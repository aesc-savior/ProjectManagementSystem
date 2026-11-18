package pm.web;

import pm.dao.DocDao;
import pm.dao.LogDao;
import pm.dao.ProjectDao;
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

/** 文档：/doc?action=list|upload|download|delete */
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 200L * 1024 * 1024,
        maxRequestSize = 210L * 1024 * 1024)
public class DocServlet extends HttpServlet {

    private static final String[] ALLOWED = {"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "jpg", "jpeg", "png", "gif", "zip", "rar", "txt", "md"};

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        if ("download".equals(action)) {
            download(req, resp);
            return;
        }
        String projectId = Web.p(req, "projectId");
        String keyword = Web.p(req, "keyword");
        List<Map<String, String>> rows = DocDao.list(projectId, keyword, Web.uid(req), Web.isAdmin(req));
        req.setAttribute("rows", rows);
        req.setAttribute("projects", ProjectDao.forSelect(Web.uid(req), Web.isAdmin(req)));
        req.setAttribute("fProjectId", projectId);
        req.setAttribute("fKeyword", keyword);
        req.getRequestDispatcher("/doc/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String ctx = req.getContextPath();
        if ("upload".equals(action)) {
            upload(req, resp);
        } else if ("delete".equals(action)) {
            Map<String, String> d = DocDao.get(Web.p(req, "id"));
            if (d == null) {
                resp.sendRedirect(ctx + "/doc?action=list");
                return;
            }
            boolean allowed = Web.isAdmin(req) || Web.uid(req).equals(d.get("uploader_id"))
                    || (Web.isPm(req) && ProjectDao.canManage(d.get("project_id"), Web.uid(req), Web.role(req), false));
            if (!allowed) {
                redirect(req, resp, "/doc?action=list", "没有删除权限");
                return;
            }
            DocDao.delete(d.get("id"));
            LogDao.write(Web.uid(req), Web.uname(req), "DELETE", "doc:" + d.get("id"),
                    "删除文档：" + d.get("name"), Web.ip(req));
            redirect(req, resp, "/doc?action=list", "文档已删除");
        } else {
            resp.sendRedirect(ctx + "/doc?action=list");
        }
    }

    private void upload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ctx = req.getContextPath();
        String projectId = Web.p(req, "projectId");
        if (!ProjectDao.canView(projectId, Web.uid(req), Web.isAdmin(req))) {
            redirect(req, resp, "/doc?action=list", "请选择你参与的项目");
            return;
        }
        try {
            Part part = req.getPart("file");
            String submitted = part == null ? null : part.getSubmittedFileName();
            if (submitted == null || submitted.trim().isEmpty() || part.getSize() == 0) {
                redirect(req, resp, "/doc?action=list&projectId=" + projectId, "请选择要上传的文件");
                return;
            }
            String ext = "";
            int dot = submitted.lastIndexOf('.');
            if (dot > -1) {
                ext = submitted.substring(dot + 1).toLowerCase();
            }
            boolean ok = false;
            for (String a : ALLOWED) {
                if (a.equals(ext)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                redirect(req, resp, "/doc?action=list&projectId=" + projectId,
                        "不支持的文件类型：" + (ext.isEmpty() ? "无扩展名" : ext));
                return;
            }
            String dir = System.getProperty("pm.upload.dir",
                    System.getProperty("java.io.tmpdir") + File.separator + "pm-upload");
            File folder = new File(dir);
            if (!folder.exists() && !folder.mkdirs()) {
                redirect(req, resp, "/doc?action=list&projectId=" + projectId, "上传目录不可写：" + dir);
                return;
            }
            String storeName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            File target = new File(folder, storeName);
            try (InputStream in = part.getInputStream();
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
            DocDao.add(projectId, Web.p(req, "taskId"), submitted, ext, part.getSize(),
                    target.getAbsolutePath(), Web.p(req, "visibility"), Web.uid(req));
            LogDao.write(Web.uid(req), Web.uname(req), "UPLOAD", "project:" + projectId,
                    "上传文档：" + submitted, Web.ip(req));
            redirect(req, resp, "/doc?action=list&projectId=" + projectId, "上传成功：" + submitted);
        } catch (Exception e) {
            redirect(req, resp, "/doc?action=list&projectId=" + projectId, "上传失败：" + e.getMessage());
        }
    }

    private void download(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> d = DocDao.get(Web.p(req, "id"));
        if (d == null) {
            resp.sendError(404, "文档不存在");
            return;
        }
        if (!ProjectDao.canView(d.get("project_id"), Web.uid(req), Web.isAdmin(req))) {
            resp.sendError(403, "没有下载权限");
            return;
        }
        File file = new File(d.get("path"));
        if (!file.exists()) {
            resp.sendError(404, "文件已丢失：" + d.get("name"));
            return;
        }
        String name = URLEncoder.encode(d.get("name"), "UTF-8").replace("+", "%20");
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
        LogDao.write(Web.uid(req), Web.uname(req), "DOWNLOAD", "doc:" + d.get("id"),
                "下载文档：" + d.get("name"), Web.ip(req));
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
