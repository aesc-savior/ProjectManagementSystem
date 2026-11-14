package archive.web;

import archive.dao.ArchiveDao;
import archive.dao.LogDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 档案模块：列表 / 新增 / 编辑 / 查看 / 删除 / 归档 / 上传附件 / 下载附件。
 * 统一入口 /archive?action=xxx，页面在 web/archive/ 下。
 */
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 200L * 1024 * 1024,
        maxRequestSize = 210L * 1024 * 1024)
public class ArchiveServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        if (action.isEmpty()) {
            action = "list";
        }
        if ("list".equals(action)) {
            list(req, resp);
        } else if ("addForm".equals(action)) {
            req.setAttribute("categories", ArchiveDao.categories());
            req.getRequestDispatcher("/archive/form.jsp").forward(req, resp);
        } else if ("editForm".equals(action)) {
            Map<String, String> row = ArchiveDao.get(Web.p(req, "id"));
            if (row == null) {
                redirect(req, resp, "/archive?action=list", "档案不存在");
                return;
            }
            req.setAttribute("row", row);
            req.setAttribute("categories", ArchiveDao.categories());
            req.getRequestDispatcher("/archive/form.jsp").forward(req, resp);
        } else if ("view".equals(action)) {
            view(req, resp);
        } else if ("download".equals(action)) {
            download(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/archive?action=list");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String ctx = req.getContextPath();

        boolean staff = Web.isStaff(req);
        if (!staff && !"upload".equals(action)) {
            resp.sendRedirect(ctx + "/home/index.jsp?msg=" + Web.enc("没有操作档案的权限"));
            return;
        }

        if ("save".equals(action)) {
            String id = Web.p(req, "id");
            java.util.Map<String, String> p = new java.util.HashMap<String, String>();
            p.put("title", Web.p(req, "title"));
            p.put("categoryId", Web.p(req, "categoryId"));
            p.put("secretLevel", Web.p(req, "secretLevel"));
            p.put("keepYears", Web.p(req, "keepYears"));
            p.put("deptName", Web.p(req, "deptName"));
            p.put("ownerName", Web.p(req, "ownerName"));
            p.put("status", Web.p(req, "status"));
            p.put("archiveTime", Web.p(req, "archiveTime"));
            p.put("remark", Web.p(req, "remark"));
            p.put("creator", Web.uname(req));
            if (p.get("title").isEmpty()) {
                redirect(req, resp, "/archive?action=list", "档案名称不能为空");
                return;
            }
            String savedId = ArchiveDao.save(id, p);
            LogDao.write(Web.uid(req), Web.uname(req), id.isEmpty() ? "ADD" : "UPDATE",
                    "archive:" + savedId, "保存档案：" + p.get("title"), Web.ip(req));
            redirect(req, resp, "/archive?action=view&id=" + savedId, "保存成功");
        } else if ("delete".equals(action)) {
            String id = Web.p(req, "id");
            ArchiveDao.delete(id);
            LogDao.write(Web.uid(req), Web.uname(req), "DELETE", "archive:" + id, "删除档案", Web.ip(req));
            redirect(req, resp, "/archive?action=list", "已删除");
        } else if ("archive".equals(action)) {
            String id = Web.p(req, "id");
            ArchiveDao.archive(id);
            LogDao.write(Web.uid(req), Web.uname(req), "ARCHIVE", "archive:" + id, "归档", Web.ip(req));
            redirect(req, resp, "/archive?action=view&id=" + id, "已归档");
        } else if ("upload".equals(action)) {
            upload(req, resp);
        } else if ("deleteFile".equals(action)) {
            String fileId = Web.p(req, "fileId");
            String archiveId = Web.p(req, "id");
            ArchiveDao.deleteFile(fileId);
            LogDao.write(Web.uid(req), Web.uname(req), "DELETE", "file:" + fileId, "删除附件", Web.ip(req));
            redirect(req, resp, "/archive?action=view&id=" + archiveId, "附件已删除");
        } else {
            resp.sendRedirect(ctx + "/archive?action=list");
        }
    }

    // ---------------------------------------------------------------- 列表
    private void list(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String keyword = Web.p(req, "keyword");
        String categoryId = Web.p(req, "categoryId");
        String year = Web.p(req, "year");
        String secretLevel = Web.p(req, "secretLevel");
        String status = Web.p(req, "status");
        int page = Math.max(1, Web.i(req, "page", 1));

        long total = ArchiveDao.count(keyword, categoryId, year, secretLevel, status);
        List<Map<String, String>> rows =
                ArchiveDao.list(keyword, categoryId, year, secretLevel, status, page, PAGE_SIZE);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageSize", PAGE_SIZE);
        req.setAttribute("pages", (int) Math.ceil(total / (double) PAGE_SIZE));
        req.setAttribute("categories", ArchiveDao.categories());
        req.setAttribute("fKeyword", keyword);
        req.setAttribute("fCategoryId", categoryId);
        req.setAttribute("fYear", year);
        req.setAttribute("fSecret", secretLevel);
        req.setAttribute("fStatus", status);
        req.getRequestDispatcher("/archive/list.jsp").forward(req, resp);
    }

    // ---------------------------------------------------------------- 详情
    private void view(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String id = Web.p(req, "id");
        Map<String, String> row = ArchiveDao.get(id);
        if (row == null) {
            req.setAttribute("error", "档案不存在或已被删除");
            req.getRequestDispatcher("/archive/view.jsp").forward(req, resp);
            return;
        }
        int mySecret = Web.secret(req);
        String secret = row.get("secret_level");
        int target = 1;
        try {
            target = Integer.parseInt(secret);
        } catch (Exception ignore) {
        }
        if (target > mySecret) {
            req.setAttribute("error", "你的密级（" + mySecret + " 级）不足，无法查看该档案");
            req.getRequestDispatcher("/archive/view.jsp").forward(req, resp);
            return;
        }
        req.setAttribute("row", row);
        req.setAttribute("files", ArchiveDao.files(id));
        LogDao.write(Web.uid(req), Web.uname(req), "VIEW", "archive:" + id, "查看档案", Web.ip(req));
        req.getRequestDispatcher("/archive/view.jsp").forward(req, resp);
    }

    // ---------------------------------------------------------------- 上传
    private void upload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ctx = req.getContextPath();
        String archiveId = Web.p(req, "id");
        if (!Web.isStaff(req) || !archiveId.matches("\\d{1,18}")) {
            resp.sendRedirect(ctx + "/archive?action=list");
            return;
        }
        try {
            Part part = req.getPart("file");
            String submitted = part == null ? null : part.getSubmittedFileName();
            if (submitted == null || submitted.trim().isEmpty() || part.getSize() == 0) {
                redirect(req, resp, "/archive?action=view&id=" + archiveId, "请选择要上传的文件");
                return;
            }
            String dir = System.getProperty("archive.upload.dir",
                    System.getProperty("java.io.tmpdir") + File.separator + "archive-upload");
            File folder = new File(dir);
            if (!folder.exists() && !folder.mkdirs()) {
                redirect(req, resp, "/archive?action=view&id=" + archiveId, "上传目录不可写：" + dir);
                return;
            }
            String ext = "";
            int dot = submitted.lastIndexOf('.');
            if (dot > -1) {
                ext = submitted.substring(dot).toLowerCase();
            }
            String storeName = UUID.randomUUID().toString().replace("-", "") + ext;
            File target = new File(folder, storeName);
            try (InputStream in = part.getInputStream();
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
            ArchiveDao.addFile(archiveId, submitted, storeName, target.getAbsolutePath(),
                    part.getSize(), ext.replace(".", ""));
            LogDao.write(Web.uid(req), Web.uname(req), "UPLOAD", "archive:" + archiveId,
                    "上传附件：" + submitted, Web.ip(req));
            redirect(req, resp, "/archive?action=view&id=" + archiveId, "上传成功：" + submitted);
        } catch (Exception e) {
            redirect(req, resp, "/archive?action=view&id=" + archiveId, "上传失败：" + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- 下载
    private void download(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> f = ArchiveDao.file(Web.p(req, "fileId"));
        if (f == null) {
            resp.sendError(404, "附件不存在");
            return;
        }
        File file = new File(f.get("file_path"));
        if (!file.exists()) {
            resp.sendError(404, "文件已丢失：" + f.get("file_name"));
            return;
        }
        String name = URLEncoder.encode(f.get("file_name"), "UTF-8").replace("+", "%20");
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + name);
        resp.setContentLengthLong(file.length());
        try (InputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
        LogDao.write(Web.uid(req), Web.uname(req), "DOWNLOAD", "file:" + f.get("id"),
                "下载附件：" + f.get("file_name"), Web.ip(req));
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
