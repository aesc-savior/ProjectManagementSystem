package pm.web;

import pm.dao.Db;
import pm.dao.DocDao;
import pm.dao.LogDao;
import pm.dao.ProjectDao;
import pm.util.Conf;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
//上传的文档最大200MB
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 200L * 1024 * 1024, maxRequestSize = 210L * 1024 * 1024)
public class DocServlet extends HttpServlet {

    private static final String[] ALLOWED = {"pdf", "doc", "docx", "xls", "xlsx", "csv", "ppt", "pptx",
            "jpg", "jpeg", "png", "gif", "zip", "rar", "txt", "md"};

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException { //无法处理的错误
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
//调用Web.java
// public static String p(HttpServletRequest req, String name) {
//        String v = req.getParameter(name);
//        return v == null ? "" : v.trim();
//    }
        if ("download".equals(action)) { download(req, resp); return; }
        if ("downloadZip".equals(action)) { downloadZip(req, resp); return; }
        if ("preview".equals(action)) { preview(req, resp); return; }
        if ("export".equals(action)) { exportExcel(req, resp); return; }

        String projectId = Web.p(req, "projectId");
        String taskId = Web.p(req, "taskId");
        String status = Web.p(req, "status");
        String keyword = Web.p(req, "keyword");
        String orderBy = Web.p(req, "orderBy");
        String isRecycle = Web.p(req, "isRecycle");
        if (isRecycle == null) isRecycle = "0";

        String uploadProjectId = Web.p(req, "uploadProjectId");
        String uploadTaskId = Web.p(req, "uploadTaskId");

        List<Map<String, String>> rows = DocDao.list(projectId, taskId, status, keyword, orderBy, isRecycle, Web.uid(req), Web.isAdmin(req));
        //用条件筛选出文档
        req.setAttribute("rows", rows);
        //根据具体用户筛选其参加的项目方便上传下拉框
        req.setAttribute("projects", ProjectDao.forSelect(Web.uid(req), Web.isAdmin(req)));

        if ("folder".equals(action)) {
            List<Map<String, String>> treeProjects = DocDao.listTreeProjects(Web.uid(req), Web.isAdmin(req));
            req.setAttribute("treeProjects", treeProjects);

            List<Map<String, String>> allTasks = new ArrayList<Map<String, String>>();
            for(int i = 0; i < treeProjects.size(); i++) {
                allTasks.addAll(DocDao.listTreeTasks(treeProjects.get(i).get("id")));
            }//任务属于哪个项目可以在任务表里查，项目属于哪个状态可以在项目表里查
            req.setAttribute("allTasks", allTasks);//将当前用户可访问到的任务全列出来

            req.getRequestDispatcher("/doc/folder.jsp").forward(req, resp);//执行显示folder页面
            return;
        }

        //搜索区任务下拉框，如果搜索区选中了一个项目则列出项目里有的任务
        List<Map<String, String>> searchTasks = new ArrayList<Map<String, String>>();
        if (projectId != null && !projectId.isEmpty()) {
            searchTasks = DocDao.listTreeTasks(projectId);
        }
        req.setAttribute("searchTasks", searchTasks);

        List<Map<String, String>> uploadTasks = new ArrayList<Map<String, String>>();
        List<Map<String, String>> uploadMembers = new ArrayList<Map<String, String>>();
            if (uploadProjectId != null && !uploadProjectId.trim().isEmpty()) {
            uploadTasks = DocDao.listTreeTasks(uploadProjectId);
            // 如果在上传区选了具体任务，就查任务相关人员；否则查全项目成员(用于指定人员才能查看文档的权限设置)
            if (uploadTaskId != null && !uploadTaskId.trim().isEmpty()) {
                uploadMembers = DocDao.listTaskMembers(uploadProjectId, uploadTaskId, Web.uid(req));
            } else {
                uploadMembers = DocDao.listProjectMembers(uploadProjectId, Web.uid(req));
            }
        }

        req.setAttribute("uploadTasks", uploadTasks);
        req.setAttribute("uploadMembers", uploadMembers);

        req.getRequestDispatcher("/doc/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String contextPath = req.getContextPath();  //获取上下文路径，跳转用

        if ("upload".equals(action)) {
            upload(req, resp);
        }
        else if ("delete".equals(action)) {
            Map<String, String> d = DocDao.get(Web.p(req, "id"));
            if (d != null && (Web.isAdmin(req)  //管理员、对应项目的项目经理和上传者才可以删除
                    || Web.uid(req).equals(d.get("uploader_id"))
                    || (Web.isPm(req) && ProjectDao.canManage(d.get("project_id"), Web.uid(req), Web.role(req), false)))) {
                DocDao.delete(d.get("id"));//根据id移入回收站
                LogDao.write(Web.uid(req), Web.uname(req), "DELETE", "doc:" + d.get("id"), "移入回收站：" + d.get("name"), Web.ip(req));
                redirect(req, resp, "/doc?action=list", "操作成功");
            } else {
                redirect(req, resp, "/doc?action=list", "权限不足，无法删除");
            }
        }
        else if ("restore".equals(action)) {//从回收站恢复
            Map<String, String> d = DocDao.get(Web.p(req, "id"));
            if (d != null && (Web.isAdmin(req)  //管理员、对应项目的项目经理和上传者才可以恢复
                    || Web.uid(req).equals(d.get("uploader_id"))
                    || (Web.isPm(req) && ProjectDao.canManage(d.get("project_id"), Web.uid(req), Web.role(req), false)))) {
                DocDao.restore(d.get("id"));
                redirect(req, resp, "/doc?action=list&isRecycle=1", "文档已恢复");
            } else {
                redirect(req, resp, "/doc?action=list&isRecycle=1", "权限不足，无法恢复");
            }
        }
        else if ("forceDelete".equals(action)) {//调用彻底删除
            Map<String, String> d = DocDao.get(Web.p(req, "id"));
            if (d != null && (Web.isAdmin(req)//同删除条件判断，管理员、对应项目的项目经理和上传者      (是否冗余？本来就看不到不在权限的回收站内文档？)
                    || Web.uid(req).equals(d.get("uploader_id"))
                    || (Web.isPm(req) && ProjectDao.canManage(d.get("project_id"), Web.uid(req), Web.role(req), false)))) {
                DocDao.forceDelete(d.get("id"));
                LogDao.write(Web.uid(req), Web.uname(req), "DELETE", "doc:" + d.get("id"), "彻底删除：" + d.get("name"), Web.ip(req));
                redirect(req, resp, "/doc?action=list&isRecycle=1", "文档已彻底删除");
            } else {
                redirect(req, resp, "/doc?action=list&isRecycle=1", "权限不足，无法彻底删除");
            }
        }
        else if ("updateInfo".equals(action)) { //编辑功能
            Map<String, String> d = DocDao.get(Web.p(req, "id"));
            if (d != null && (Web.isAdmin(req)  //管理员、对应项目的项目经理和上传者才可以编辑
                    || Web.uid(req).equals(d.get("uploader_id"))
                    || (Web.isPm(req) && ProjectDao.canManage(d.get("project_id"), Web.uid(req), Web.role(req), false)))) {
                DocDao.updateInfo(Web.p(req, "id"), Web.p(req, "name"), Web.p(req, "description"), Web.p(req, "tags"));
                LogDao.write(Web.uid(req), Web.uname(req), "UPDATE", "doc:" + d.get("id"), "修改文档：" + d.get("name"), Web.ip(req));
                redirect(req, resp, "/doc?action=list", "文档信息已更新");
            } else {
                redirect(req, resp, "/doc?action=list", "权限不足，无法修改");
            }
        }
        else {  //回文档管理首页
            resp.sendRedirect(contextPath + "/doc?action=list");
        }
    }

    private void upload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String contextPath = req.getContextPath();
        String projectId = Web.p(req, "projectId");

        if (!ProjectDao.canView(projectId, Web.uid(req), Web.isAdmin(req))) {//调用校验权限：是不是项目成员
            redirect(req, resp, "/doc?action=list", "这不是你参与的项目");
            return;
        }
        try {
            Part part = req.getPart("file");
            String submitted = null;
            if (part != null) {
                submitted = part.getSubmittedFileName();//上传的文件名
            }
            if (submitted == null || submitted.trim().isEmpty() || part.getSize() == 0) {//未选择上传的文件
                redirect(req, resp, "/doc?action=list&uploadProjectId=" + projectId, "请选择要上传的文件");
                return;
            }
            String ext = "";
            int dot = submitted.lastIndexOf('.');
            if (dot > -1) ext = submitted.substring(dot + 1).toLowerCase();//提取后缀并转为全小写(适配白名单的格式)

            boolean ok = false;
            for (int i = 0; i < ALLOWED.length; i++) {//将后缀与白名单逐一比对
                String a = ALLOWED[i];
                if (a.equals(ext)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                redirect(req, resp, "/doc?action=list&uploadProjectId=" + projectId, "不支持的文件类型：" + ext);
                return;
            }

            //暂存入临时文件夹\pm-upload方便读取
            String dir = Conf.get("pm.upload.dir", "PM_UPLOAD_DIR", System.getProperty("java.io.tmpdir") + File.separator + "pm-upload");
            File folder = new File(dir);
            if (!folder.exists()) folder.mkdirs();//创建文件夹

            String storeName = UUID.randomUUID().toString().replace("-", "") + "." + ext;//文件随机起名方便回滚避免上传相互覆盖
            File target = new File(folder, storeName);//在存文件的文件夹里创上传文档同名的文档
            //然后将数据慢慢搬进新建的同名文档
            try (InputStream in = part.getInputStream(); OutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192]; int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            }

            //写入数据库
            String taskId = Web.p(req, "taskId");
            DocDao.add(projectId, taskId, submitted, ext, part.getSize(),
                    target.getAbsolutePath(), Web.p(req, "visibility"), Web.uid(req),
                    Web.p(req, "description"), Web.p(req, "tags"));

            String[] allowedUsers = req.getParameterValues("allowedUsers");//指定人员
            if ("PRIVATE".equals(Web.p(req, "visibility"))) {//如果设置为只供指定人员看
                //就写入权限表：刚刚上传(ID最新)的文档允许这个(allowedUsers)访问(每个人写一条记录)
                Map<String, String> latestDoc = Db.queryOne("select id from pm_doc order by id desc limit 1");
                DocDao.addPermissions(latestDoc.get("id"), allowedUsers);
            }

            //调用写日志
            LogDao.write(Web.uid(req), Web.uname(req), "UPLOAD", "project:" + projectId, "上传：" + submitted, Web.ip(req));
            //上传结果提示
            redirect(req, resp, "/doc?action=list&uploadProjectId=" + projectId + (Db.isId(taskId) ? "&uploadTaskId=" + taskId : ""), "上传成功：" + submitted);
        } catch (Exception e) {
            redirect(req, resp, "/doc?action=list&uploadProjectId=" + projectId, "上传失败：" + e.getMessage());
        }
    }

    //下载文档
    private void download(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> d = DocDao.get(Web.p(req, "id"));

        if (d == null) { resp.sendError(404, "不存在"); return; }        //数据库查不到
        if (!canAccess(d, req)) { resp.sendError(403, "没有权限"); return; }//没权限

        File file = new File(d.get("path"));
        if (!file.exists()) { resp.sendError(404, "文档不见了"); return; }//跟着路径找不到文档

        String name = URLEncoder.encode(d.get("name"), "UTF-8").replace("+", "%20");//从数据库里读取文件名并转码(因为之前上传时服务器存的是不一样的名字)
        resp.setContentType("application/octet-stream");//写入resp
        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + name);
        try (InputStream in = new FileInputStream(file); OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192]; int len;
            while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
        }
        LogDao.write(Web.uid(req), Web.uname(req), "DOWNLOAD", "doc:" + d.get("id"), "下载：" + d.get("name"), Web.ip(req));
    }

    //打包下载
    private void downloadZip(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String ids = Web.p(req, "ids");//获取的需要打包的文档ID是一个用逗号分隔的字符串

        String userName = Web.uname(req);
        //拼成最终的文件名如 "张三_20261124_153022.zip"
        String timeStr = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        String zipFileName = userName + "_" + timeStr + ".zip";
        String encodedName = URLEncoder.encode(zipFileName, "UTF-8").replace("+", "%20");
        resp.setContentType("application/zip");
        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        ZipOutputStream outZip = new ZipOutputStream(resp.getOutputStream(), Charset.forName("UTF-8"));
        try {
            String[] idArray = ids.split(",");//提取出用逗号分割的各个id
            for (int i = 0; i < idArray.length; i++) {//依次放入压缩包
                String id = idArray[i];
                Map<String, String> d = DocDao.get(id);
                if (d != null && canAccess(d, req)) {//权限校验
                    File file = new File(d.get("path"));
                    if (file.exists()) {
                        outZip.putNextEntry(new ZipEntry(d.get("name")));//在压缩包中创建同名文档
                        try (FileInputStream inZip = new FileInputStream(file)) {//写入压缩包
                            byte[] buf = new byte[8192]; int len;
                            while ((len = inZip.read(buf)) != -1) outZip.write(buf, 0, len);
                        }
                        outZip.closeEntry();
                    }
                }
            }
        } finally {
            outZip.close();
        }
    }

    //预览
    private void preview(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, String> d = DocDao.get(Web.p(req, "id"));
        if (d == null) { resp.sendError(404); return; }//对应ID无记录
        if (!canAccess(d, req)) { resp.sendError(403, "权限不足"); return; }

        String ext = d.get("ext"); //后缀名
        File file = new File(d.get("path"));
        if (!file.exists()) { resp.sendError(404); return; }//按路径发现不存在

        //pdf和图片可以直接在浏览器上预览
        if ("pdf".equals(ext) || "jpg".equals(ext) || "png".equals(ext) || "jpeg".equals(ext) || "gif".equals(ext)) {
            if ("pdf".equals(ext)) resp.setContentType("application/pdf");
            else resp.setContentType("image/" + ext);
            try (InputStream in = new FileInputStream(file); OutputStream out = resp.getOutputStream()) {
                byte[] buf = new byte[8192]; int len;
                while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
            }
            return;
        }

        //如果是word或excel文件，先变成html预览。如果需要pdf则点击跳转到打印界面展示pdf的预览
        resp.setContentType("text/html;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>预览 - " + d.get("name") + "</title>");
        //在html模拟PDF预览的纸张效果：灰底背景、主题内容白底、加阴影模拟纸张效果，透明度0.1像微微凸起
        //打印时只展示中央主体部分，去掉灰底背景换为纯白并且去掉阴影
        out.println("<style>body{background:#f4f6f9; padding:20px;} .preview-container{background:#fff; box-shadow:0 2px 10px rgba(0,0,0,0.1); max-width:1000px; margin:0 auto; padding:20px;} @media print { .no-print { display: none !important; } body { background: #fff; } .preview-container { box-shadow: none; padding: 0; } }</style>");
        out.println("</head><body>");
        //在新的html预览页面允许用户点开预览pdf
        //按钮蓝底白字，手型
        out.println("<div style='text-align:center; margin-bottom:20px;' class='no-print'><button onclick='window.print()' style='padding:8px 15px; background:#007bff; color:#fff; border:none; cursor:pointer;'>PDF预览</button></div>");
        out.println("<div class='preview-container'>");

        try {
            if ("doc".equals(ext)) {
                //用POI打开读取word
                InputStream is = new FileInputStream(file);
                org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(is);
                org.apache.poi.hwpf.usermodel.Range range = doc.getRange();
                for (int i = 0; i < range.numParagraphs(); i++) {//逐段提取
                    String text = range.getParagraph(i).text();
                    if (text != null) {
                        //将危险字符转义防止预览的html出现没想到的错误
                        text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                        //将提取出的文本加上<p>渲染到网页里
                        out.println("<p style='line-height:1.6;'>" + text + "</p>");
                    }
                }
                is.close();
            } else if ("xls".equals(ext)) {
                InputStream xls = new FileInputStream(file);
                org.apache.poi.hssf.usermodel.HSSFWorkbook workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook(xls);

                int numberOfSheets = workbook.getNumberOfSheets();//xls文件内数据表数量

                for (int i = 0; i < numberOfSheets; i++) {//逐个数据表写入
                    org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(i);
                    String sheetName = workbook.getSheetName(i); //获取该页表名
                    out.println("<h3 style='margin-top:20px; color:#333;'>" + sheetName + "</h3>");
                    out.println("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse: collapse; width:100%; text-align:left;'>");
                    for (int j = 0; j <= sheet.getLastRowNum(); j++) {//逐行写入
                        org.apache.poi.ss.usermodel.Row row = sheet.getRow(j);
                        if (row == null) continue;
                        out.println("<tr>");//新起一行
                        for (int k = 0; k < row.getLastCellNum(); k++) {//逐个单元格写入
                            org.apache.poi.ss.usermodel.Cell cell = row.getCell(k);
                            String cellValue = "";
                            if (cell != null) {
                                cellValue = cell.toString();//单元格内内容强制转为字符串写入
                            }
                            //表头加灰底与其他内容区别开
                            if (j == 0) out.println("<th style='background:#eee;'>" + cellValue + "</th>");
                            else out.println("<td>" + cellValue + "</td>");
                        }
                        out.println("</tr>");//行结束
                    }
                    out.println("</table>"); //当前页数据表结束
                }
                xls.close();

            } else if ("csv".equals(ext)) {
                java.io.FileInputStream fileInput = new java.io.FileInputStream(file);
                byte[] fileBytes = new byte[fileInput.available()];
                fileInput.read(fileBytes);//读取文档内容
                fileInput.close();
                String allText = new String(fileBytes, "UTF-8");//将读取到的文档内容转为正确的UTG-8格式
                String[] lines = allText.split("\n");//根据csv的特点以换行符划分行
                out.println("<table border='1' cellspacing='0' cellpadding='6' style='border-collapse: collapse; width:100%; text-align:left;'>");

            for (int i = 0; i < lines.length; i++) {//逐行写入
                String line = lines[i].trim();//获取要写入的整行的文本内容
                if (line.isEmpty()) continue;
                out.println("<tr>");//行开头
                String[] cells = line.split(",");//用逗号分隔的各个单元格
                for (int k = 0; k < cells.length; k++) {//逐个单元格写入
                    String cell = cells[k];
                    if (i == 0) out.println("<th style='background:#eee;'>" + cell.trim() + "</th>");//表头
                    else out.println("<td>" + cell.trim() + "</td>");
                }
                out.println("</tr>");
            }
            out.println("</table>");
        } else {
                out.println("<div style='text-align:center; padding:50px; color:#666;'><h3>该格式的文档暂不支持在线渲染</h3><p>请直接使用下载功能。</p></div>");
            }
        } catch (Exception e) {
            out.println("<div style='color:red; text-align:center;'>解析失败：" + e.getMessage() + "</div>");
        }

        out.println("</div></body></html>");
        out.flush();
    }

    //判断是否为系统管理员/上传者/项目经理/公开文档/是项目成员可见文档的项目成员/是被指定可见的成员
    private boolean canAccess(Map<String, String> d, HttpServletRequest req) {
        String visibility = d.get("visibility");//PUBLIC 公开、PROJECT 项目成员、PRIVATE 指定人员
        return Web.isAdmin(req)
                || Web.uid(req).equals(d.get("uploader_id"))
                || (Web.isPm(req) && ProjectDao.canManage(d.get("project_id"), Web.uid(req), Web.role(req), Web.isAdmin(req)))
                || "PUBLIC".equals(visibility)
                || ("PROJECT".equals(visibility) && ProjectDao.canView(d.get("project_id"), Web.uid(req), Web.isAdmin(req)))
                || ("PRIVATE".equals(visibility) && DocDao.hasPermission(d.get("id"), Web.uid(req)));
    }

    //导出搜索到的文档列表，以csv格式
    private void exportExcel(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Map<String, String>> rows = DocDao.list(Web.p(req, "projectId"), Web.p(req, "taskId"), Web.p(req, "status"), Web.p(req, "keyword"), "id_desc", "0", Web.uid(req), Web.isAdmin(req));
        resp.setContentType("application/vnd.ms-excel;charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=doc_list.csv");
        PrintWriter out = resp.getWriter();
        out.write('\ufeff');//UTF-8的BOM标记
        out.println("文件名,所属项目,上传人,大小(KB),上传时间,说明,标签");//表头
        for (int i = 0; i < rows.size(); i++) {//逐行(逐个文档)
            Map<String, String> r = rows.get(i);
            long size = 0;//文档大小
            try {
                size = Long.parseLong(r.get("size"));
            } catch (Exception e) {
            }
            String description = "";//文档描述
            if (r.get("description") != null) {
                description = r.get("description");
            }
            String tags = "";//文档标签
            if (r.get("tags") != null) {
                tags = r.get("tags");
            }

            out.println(r.get("name") + ","
                    + r.get("project_name") + ","
                    + r.get("uploader") + ","
                    + String.format("%.1f", size / 1024.0) + ","//单位：KB
                    + r.get("create_time") + ","
                    + description + ","
                    + tags);
        }
        out.flush();
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp, String path, String msg) throws IOException {
        String a = "?";
        if (path.contains("?")) {
            a = "&";
        }
        String encodedMsg = URLEncoder.encode(msg, "UTF-8");//提示编码为中午防止乱码
        String fullUrl = req.getContextPath() + path + a + "msg=" + encodedMsg;//根目录+路径+提示文字

        // 如果权限不足，弹窗提示并跳转
        if (msg != null && msg.contains("权限不足")) {
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.println("<script type='text/javascript'>");
            out.println("alert('" + msg + "');");
            out.println("window.location.href='" + fullUrl + "';");
            out.println("</script>");
            out.flush();
            return;
        }

        resp.sendRedirect(fullUrl);
    }
}