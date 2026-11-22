package pm.web;

import pm.dao.LogDao;
import pm.dao.ProjectDao;
import pm.dao.RemindDao;
import pm.dao.UserDao;
import pm.dao.WeeklyDao;
import pm.util.Conf;
import pm.util.WeChatNotifier;
import pm.util.XlsxUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 周报：填报、提交、撤回、审核、打印、Excel 导出、催交
// 覆盖 PM-030～PM-038
@MultipartConfig(maxFileSize = 20 * 1024 * 1024, maxRequestSize = 25 * 1024 * 1024, fileSizeThreshold = 1048576)
public class WeeklyServlet extends HttpServlet {

    private static final int PAGE_SIZE = 12;
    private static final String[] ALLOWED_EXT = {"doc", "docx", "xls", "xlsx", "pdf", "ppt", "pptx", "txt", "png", "jpg", "zip"};

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        if (action.isEmpty()) {
            action = "list";
        }
        String uid = Web.uid(req);
        boolean all = Web.isAdmin(req);

        if ("addForm".equals(action)) {
            String projectId = Web.p(req, "projectId");
            if (projectId.isEmpty()) {
                List<Map<String, String>> projects = ProjectDao.forSelect(uid, all);
                if (!projects.isEmpty()) {
                    projectId = projects.get(0).get("id");
                }
            }
            if (!ProjectDao.canView(projectId, uid, all)) {
                redirect(req, resp, "/weekly?action=list", "请选择你参与的项目");
                return;
            }
            req.setAttribute("projects", ProjectDao.forSelect(uid, all));
            req.setAttribute("projectId", projectId);
            req.setAttribute("weekNo", WeeklyDao.currentWeek());
            req.getRequestDispatcher("/weekly/form.jsp").forward(req, resp);
            return;
        }

        if ("editForm".equals(action)) {
            Map<String, String> row = WeeklyDao.get(Web.p(req, "id"));
            if (row == null || !ProjectDao.canView(row.get("project_id"), uid, all)) {
                redirect(req, resp, "/weekly?action=list", "周报不存在或没有权限");
                return;
            }
            req.setAttribute("row", row);
            req.setAttribute("projects", ProjectDao.forSelect(uid, all));
            req.setAttribute("projectId", row.get("project_id"));
            req.setAttribute("weekNo", row.get("week_no"));
            req.getRequestDispatcher("/weekly/form.jsp").forward(req, resp);
            return;
        }

        if ("print".equals(action)) {
            Map<String, String> row = WeeklyDao.get(Web.p(req, "id"));
            if (row == null || !ProjectDao.canView(row.get("project_id"), uid, all)) {
                redirect(req, resp, "/weekly?action=list", "周报不存在或没有权限");
                return;
            }
            req.setAttribute("row", row);
            req.setAttribute("printTime", new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));
            req.getRequestDispatcher("/weekly/print.jsp").forward(req, resp);
            return;
        }

        if ("stat".equals(action)) {
            String weekNo = Web.p(req, "weekNo");
            if (!weekNo.matches("\\d{4}-W\\d{2}")) {
                weekNo = WeeklyDao.currentWeek();
            }
            req.setAttribute("weekNo", weekNo);
            req.setAttribute("statRows", WeeklyDao.statByProject(weekNo, uid, all));
            req.setAttribute("missing", WeeklyDao.missing("", weekNo, uid, all));
            req.setAttribute("trend", WeeklyDao.trend(6, uid, all));
            req.setAttribute("projects", ProjectDao.forSelect(uid, all));
            req.getRequestDispatcher("/weekly/stat.jsp").forward(req, resp);
            return;
        }

        if ("export".equals(action)) {
            String scope = Web.p(req, "scope");
            if ("one".equals(scope)) {
                exportOne(req, resp, uid, all);
            } else if ("missing".equals(scope)) {
                exportMissing(req, resp, uid, all);
            } else {
                exportList(req, resp, uid, all);
            }
            return;
        }

        if ("attach".equals(action)) {
            downloadAttach(req, resp, uid, all);
            return;
        }

        // 列表
        String projectId = Web.p(req, "projectId");
        String userId = Web.p(req, "userId");
        String weekNo = Web.p(req, "weekNo");
        String status = Web.p(req, "status");
        String sort = Web.p(req, "sort");
        String order = Web.p(req, "order");
        int page = Math.max(1, Web.i(req, "page", 1));

        long total = WeeklyDao.count(projectId, userId, weekNo, status, uid, all);
        List<Map<String, String>> rows = WeeklyDao.list(projectId, userId, weekNo, status,
                page, PAGE_SIZE, uid, all, sort, order);

        // 未交名单以筛选周次为准，未筛选时看当前周（PM-034 未交标识）
        String statWeek = weekNo.matches("\\d{4}-W\\d{2}") ? weekNo : WeeklyDao.currentWeek();
        req.setAttribute("missing", WeeklyDao.missing(projectId, statWeek, uid, all));
        req.setAttribute("statWeek", statWeek);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("sumHours", WeeklyDao.sumHours(projectId, userId, weekNo, status, uid, all));
        req.setAttribute("page", page);
        req.setAttribute("pages", (int) Math.ceil(total / (double) PAGE_SIZE));
        req.setAttribute("fProjectId", projectId);
        req.setAttribute("fUserId", userId);
        req.setAttribute("fWeekNo", weekNo);
        req.setAttribute("fStatus", status);
        req.setAttribute("fSort", sort);
        req.setAttribute("fOrder", order);
        req.setAttribute("projects", ProjectDao.forSelect(uid, all));
        req.setAttribute("users", UserDao.activeUsers());
        req.setAttribute("currentWeek", WeeklyDao.currentWeek());
        req.getRequestDispatcher("/weekly/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String id = Web.p(req, "id");
        String uid = Web.uid(req);
        boolean all = Web.isAdmin(req);
        String ctx = req.getContextPath();

        if ("save".equals(action)) {
            String projectId = Web.p(req, "projectId");
            String weekNo = Web.p(req, "weekNo");
            if (!ProjectDao.canView(projectId, uid, all)) {
                redirect(req, resp, "/weekly?action=list", "没有该项目权限");
                return;
            }
            if (!weekNo.matches("\\d{4}-W\\d{2}")) {
                redirect(req, resp, "/weekly?action=list", "周次格式应为 2026-W46");
                return;
            }
            // 已提交/已审核的周报不允许直接改写（PM-032）
            if (!id.isEmpty()) {
                Map<String, String> old = WeeklyDao.get(id);
                if (old == null) {
                    redirect(req, resp, "/weekly?action=list", "周报不存在");
                    return;
                }
                String st = old.get("status");
                if ("SUBMITTED".equals(st) || "REVIEWED".equals(st)) {
                    redirect(req, resp, "/weekly?action=list", "该周报已提交，请先撤回再修改");
                    return;
                }
            }
            String userId = uid;
            if (Web.isPm(req) && DbId(Web.p(req, "userId"))) {
                userId = Web.p(req, "userId");
            }
            if (WeeklyDao.exists(projectId, userId, weekNo, id)) {
                redirect(req, resp, "/weekly?action=list", "该成员这一周已经有一份周报了");
                return;
            }
            Map<String, String> p = new HashMap<String, String>();
            p.put("projectId", projectId);
            p.put("userId", userId);
            p.put("weekNo", weekNo);
            p.put("done", Web.p(req, "done"));
            p.put("plan", Web.p(req, "plan"));
            p.put("issue", Web.p(req, "issue"));
            p.put("hours", Web.p(req, "hours"));
            String savedId = WeeklyDao.save(id, p);
            handleAttach(req, savedId, uid);
            LogDao.write(uid, Web.uname(req), id.isEmpty() ? "ADD" : "UPDATE", "weekly:" + savedId,
                    "保存周报：" + weekNo + "，工时 " + Web.p(req, "hours"), Web.ip(req));
            redirect(req, resp, "/weekly?action=editForm&id=" + savedId, "保存成功");
        } else if ("submit".equals(action)) {
            Map<String, String> w = WeeklyDao.get(id);
            if (w == null || !uid.equals(w.get("user_id"))) {
                redirect(req, resp, "/weekly?action=list", "只能提交自己的周报");
                return;
            }
            if (!("DRAFT".equals(w.get("status")) || "REJECTED".equals(w.get("status")))) {
                redirect(req, resp, "/weekly?action=list", "当前状态不可提交");
                return;
            }
            WeeklyDao.submit(id);
            LogDao.write(uid, Web.uname(req), "SUBMIT", "weekly:" + id,
                    "提交 " + w.get("week_no") + " 周报", Web.ip(req));
            redirect(req, resp, "/weekly?action=list", "周报已提交");
        } else if ("withdraw".equals(action)) {
            Map<String, String> w = WeeklyDao.get(id);
            if (w == null || !uid.equals(w.get("user_id"))) {
                redirect(req, resp, "/weekly?action=list", "只能撤回自己的周报");
                return;
            }
            if (WeeklyDao.withdraw(id)) {
                LogDao.write(uid, Web.uname(req), "WITHDRAW", "weekly:" + id,
                        "撤回 " + w.get("week_no") + " 周报", Web.ip(req));
                redirect(req, resp, "/weekly?action=editForm&id=" + id, "已撤回为草稿，可继续修改");
            } else {
                redirect(req, resp, "/weekly?action=list", "只有已提交且未审核的周报可以撤回");
            }
        } else if ("review".equals(action)) {
            Map<String, String> w = WeeklyDao.get(id);
            if (w == null || !ProjectDao.canManage(w.get("project_id"), uid, Web.role(req), all)) {
                redirect(req, resp, "/weekly?action=list", "只有项目经理可以审核");
                return;
            }
            boolean pass = "approve".equals(Web.p(req, "decision"));
            String review = Web.p(req, "review");
            // 退回必须填写理由（PM-033）
            if (!pass && review.isEmpty()) {
                redirect(req, resp, "/weekly?action=list", "退回必须填写理由");
                return;
            }
            int score = Web.i(req, "score", 0);
            if (score < 0 || score > 100) {
                redirect(req, resp, "/weekly?action=list", "评分应在 0~100 之间");
                return;
            }
            WeeklyDao.review(id, pass, review, score, Web.uname(req));
            LogDao.write(uid, Web.uname(req), pass ? "APPROVE" : "REJECT", "weekly:" + id,
                    (pass ? "审核通过" : "退回周报：" + review), Web.ip(req));
            redirect(req, resp, "/weekly?action=list", pass ? "审核完成" : "已退回");
        } else if ("deleteAttach".equals(action)) {
            Map<String, String> w = WeeklyDao.get(id);
            if (w == null || !uid.equals(w.get("user_id"))) {
                redirect(req, resp, "/weekly?action=list", "没有权限");
                return;
            }
            if ("SUBMITTED".equals(w.get("status")) || "REVIEWED".equals(w.get("status"))) {
                redirect(req, resp, "/weekly?action=list", "已提交的周报不可删除附件");
                return;
            }
            WeeklyDao.clearAttach(id);
            LogDao.write(uid, Web.uname(req), "DELETE", "weekly:" + id, "删除周报附件", Web.ip(req));
            redirect(req, resp, "/weekly?action=editForm&id=" + id, "附件已删除");
        } else if ("remind".equals(action)) {
            if (!Web.isPm(req)) {
                redirect(req, resp, "/weekly?action=list", "只有项目经理可以发起催交");
                return;
            }
            String weekNo = Web.p(req, "weekNo");
            if (!weekNo.matches("\\d{4}-W\\d{2}")) {
                weekNo = WeeklyDao.currentWeek();
            }
            int sent = remindMissing(weekNo, Web.p(req, "projectId"), uid, all, Web.uname(req));
            LogDao.write(uid, Web.uname(req), "REMIND", "weekly:" + weekNo,
                    "发起周报催交，覆盖 " + sent + " 人", Web.ip(req));
            redirect(req, resp, "/weekly?action=stat&weekNo=" + weekNo, "已生成 " + sent + " 条催交提醒");
        } else {
            resp.sendRedirect(ctx + "/weekly?action=list");
        }
    }

    // 催交：给未交成员与项目经理各生成一条站内提醒（PM-038）
    public static int remindMissing(String weekNo, String projectId, String operator,
                                    boolean all, String operatorName) {
        List<Map<String, String>> missing = WeeklyDao.missing(projectId, weekNo, operator, all);
        int sent = 0;
        StringBuilder summary = new StringBuilder();
        for (Map<String, String> m : missing) {
            String title = "周报待提交提醒 " + weekNo;
            String content = "项目 " + m.get("project_no") + "（" + m.get("project_name") + "）"
                    + weekNo + " 周报尚未提交，请在本周五 18:00 前完成。";
            long rid = RemindDao.add(m.get("user_id"), m.get("user_name"), "WEEKLY", title, content,
                    "/weekly?action=addForm&projectId=" + m.get("project_id"));
            if (rid > 0) {
                String[] r = WeChatNotifier.send(title, content);
                RemindDao.markWeChat(rid, r[0], r[1]);
            }
            sent++;
            summary.append(m.get("user_name")).append("(").append(m.get("project_no")).append(") ");
        }
        if (sent > 0) {
            for (Map<String, String> mgr : RemindDao.managers()) {
                String t = "催交汇总 " + weekNo;
                String c = "本周共有 " + sent + " 人未提交周报：" + summary;
                long rid = RemindDao.add(mgr.get("id"), mgr.get("user_name"), "WEEKLY",
                        t, c, "/weekly?action=stat&weekNo=" + weekNo);
                if (rid > 0) {
                    String[] r = WeChatNotifier.send(t, c);
                    RemindDao.markWeChat(rid, r[0], r[1]);
                }
            }
        }
        return sent;
    }

    // 附件
    private void handleAttach(HttpServletRequest req, String weeklyId, String uid)
            throws IOException, ServletException {
        if (weeklyId == null || weeklyId.isEmpty()) {
            return;
        }
        Part part;
        try {
            part = req.getPart("file");
        } catch (Exception e) {
            return;
        }
        if (part == null || part.getSize() <= 0) {
            return;
        }
        String original = part.getSubmittedFileName();
        if (original == null || original.trim().isEmpty()) {
            return;
        }
        original = original.replace("\\", "/");
        if (original.contains("/")) {
            original = original.substring(original.lastIndexOf('/') + 1);
        }
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot > -1) {
            ext = original.substring(dot + 1).toLowerCase();
        }
        boolean ok = false;
        for (String e : ALLOWED_EXT) {
            if (e.equals(ext)) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            return;
        }
        String dir = Conf.get("pm.upload.dir", "PM_UPLOAD_DIR",
                System.getProperty("java.io.tmpdir") + File.separator + "pm-upload");
        File folder = new File(dir, "weekly");
        if (!folder.exists() && !folder.mkdirs()) {
            return;
        }
        String store = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File target = new File(folder, store);
        try (InputStream in = part.getInputStream(); OutputStream out = new java.io.FileOutputStream(target)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        // 只存相对文件名，避免 Windows 反斜杠入库被当作转义符
        WeeklyDao.updateAttach(weeklyId, original, "weekly/" + store, part.getSize());
        LogDao.write(uid, "", "UPLOAD", "weekly:" + weeklyId, "上传周报附件：" + original, "");
    }

    private void downloadAttach(HttpServletRequest req, HttpServletResponse resp, String uid, boolean all)
            throws IOException {
        Map<String, String> row = WeeklyDao.get(Web.p(req, "id"));
        if (row == null || !ProjectDao.canView(row.get("project_id"), uid, all)) {
            resp.sendError(404);
            return;
        }
        String rel = row.get("attach_path");
        if (rel == null || rel.trim().isEmpty()) {
            resp.sendError(404, "该周报没有附件");
            return;
        }
        String dir = Conf.get("pm.upload.dir", "PM_UPLOAD_DIR",
                System.getProperty("java.io.tmpdir") + File.separator + "pm-upload");
        File file = new File(dir, rel.replace("\\", "/"));
        if (!file.isFile()) {
            resp.sendError(404, "附件文件不存在");
            return;
        }
        String ext = row.get("attach_name");
        int dot = ext == null ? -1 : ext.lastIndexOf('.');
        String suffix = dot > -1 ? ext.substring(dot + 1) : "";
        String name = WeeklyDao.fileName(row.get("project_no"), row.get("week_no"),
                row.get("user_name"), 1, suffix);
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment; filename=\"attach." + suffix + "\"; filename*=UTF-8''"
                + URLEncoder.encode(name, "UTF-8").replace("+", "%20"));
        try (InputStream in = new FileInputStream(file); OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
        LogDao.write(uid, Web.uname(req), "DOWNLOAD", "weekly:" + row.get("id"), "下载周报附件：" + name, "");
    }

    // 导出
    private void exportList(HttpServletRequest req, HttpServletResponse resp, String uid, boolean all)
            throws IOException {
        String projectId = Web.p(req, "projectId");
        String userId = Web.p(req, "userId");
        String weekNo = Web.p(req, "weekNo");
        String status = Web.p(req, "status");
        List<Map<String, String>> rows = WeeklyDao.listAll(projectId, userId, weekNo, status,
                uid, all, Web.p(req, "sort"), Web.p(req, "order"));
        // 导出列与页面表格保持一致（PM-035）
        List<String> headers = Arrays.asList("项目编号", "项目名称", "成员", "周次", "工时(h)",
                "状态", "提交时间", "审核人", "评语", "评分", "附件");
        List<List<String>> data = new ArrayList<List<String>>();
        for (Map<String, String> r : rows) {
            data.add(Arrays.asList(r.get("project_no"), r.get("project_name"), r.get("user_name"),
                    r.get("week_no"), r.get("hours"), statusText(r.get("status")),
                    r.get("submit_time"), r.get("reviewer"), r.get("review"), r.get("score"),
                    r.get("attach_name")));
        }
        String name = "周报清单-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".xlsx";
        XlsxUtil.download(resp, name, "周报清单", headers, data);
        LogDao.write(uid, Web.uname(req), "EXPORT", "weekly:list", "导出周报清单 " + rows.size() + " 条（xlsx）", "");
    }

    private void exportMissing(HttpServletRequest req, HttpServletResponse resp, String uid, boolean all)
            throws IOException {
        String weekNo = Web.p(req, "weekNo");
        if (!weekNo.matches("\\d{4}-W\\d{2}")) {
            weekNo = WeeklyDao.currentWeek();
        }
        List<Map<String, String>> rows = WeeklyDao.missing(Web.p(req, "projectId"), weekNo, uid, all);
        List<String> headers = Arrays.asList("项目编号", "项目名称", "应交人", "周次", "状态");
        List<List<String>> data = new ArrayList<List<String>>();
        for (Map<String, String> r : rows) {
            data.add(Arrays.asList(r.get("project_no"), r.get("project_name"),
                    r.get("user_name"), weekNo, "未交"));
        }
        XlsxUtil.download(resp, "周报未交名单-" + weekNo + ".xlsx", "未交名单-" + weekNo, headers, data);
        LogDao.write(uid, Web.uname(req), "EXPORT", "weekly:missing", "导出未交名单 " + rows.size() + " 人（xlsx）", "");
    }

    // 单份导出为 Excel（PM-035），文件名按 PM-031 规范化，含正文与附件清单
    private void exportOne(HttpServletRequest req, HttpServletResponse resp, String uid, boolean all)
            throws IOException {
        Map<String, String> r = WeeklyDao.get(Web.p(req, "id"));
        if (r == null || !ProjectDao.canView(r.get("project_id"), uid, all)) {
            resp.sendError(404);
            return;
        }
        if ("doc".equals(Web.p(req, "fmt"))) {
            exportOneDoc(resp, r, uid, req);
            return;
        }
        String fileName = WeeklyDao.fileName(r.get("project_no"), r.get("week_no"), r.get("user_name"), 1, "xlsx");
        List<List<String>> data = new ArrayList<List<String>>();
        data.add(Arrays.asList("项目编号", r.get("project_no")));
        data.add(Arrays.asList("项目名称", r.get("project_name")));
        data.add(Arrays.asList("提交人", r.get("user_name")));
        data.add(Arrays.asList("周次", r.get("week_no")));
        data.add(Arrays.asList("工时", r.get("hours") + " 小时"));
        data.add(Arrays.asList("状态", statusText(r.get("status"))));
        data.add(Arrays.asList("提交时间", r.get("submit_time")));
        data.add(Arrays.asList("审核人", r.get("reviewer")));
        data.add(Arrays.asList("", ""));
        data.add(Arrays.asList("一、本周完成", r.get("done_text")));
        data.add(Arrays.asList("二、下周计划", r.get("plan_text")));
        data.add(Arrays.asList("三、问题与风险", r.get("issue_text")));
        data.add(Arrays.asList("四、审核意见", withScore(r.get("review"), r.get("score"))));
        data.add(Arrays.asList("", ""));
        data.add(Arrays.asList("五、附件清单", ""));
        if (r.get("attach_name") == null || r.get("attach_name").isEmpty()) {
            data.add(Arrays.asList("无附件", ""));
        } else {
            data.add(Arrays.asList(r.get("attach_name"), r.get("attach_size") + " 字节"));
        }
        XlsxUtil.download(resp, fileName, "项目周报", Arrays.asList("项目", "内容"), data);
        LogDao.write(uid, Web.uname(req), "EXPORT", "weekly:" + r.get("id"), "导出周报：" + fileName, "");
    }

    private static String withScore(String review, String score) {
        String text = review == null || review.trim().isEmpty() ? "（暂无）" : review;
        return score == null || score.isEmpty() ? text : text + "（评分 " + score + "）";
    }

    // 保留 Word 版单份导出，供需要纸质/盖章场景使用
    private void exportOneDoc(HttpServletResponse resp, Map<String, String> r, String uid,
                              HttpServletRequest req) throws IOException {
        String fileName = WeeklyDao.fileName(r.get("project_no"), r.get("week_no"), r.get("user_name"), 1, "doc");
        resp.setContentType("application/msword;charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=\"weekly.doc\"; filename*=UTF-8''"
                + URLEncoder.encode(fileName, "UTF-8").replace("+", "%20"));
        PrintWriter w = resp.getWriter();
        w.println("<html><head><meta charset=\"UTF-8\"><title>" + esc(fileName) + "</title>");
        w.println("<style>body{font-family:\"Microsoft YaHei\",SimSun;font-size:12pt;line-height:1.8}"
                + "h2{text-align:center}table{border-collapse:collapse;width:100%}"
                + "td,th{border:1px solid #999;padding:6px 8px;font-size:10.5pt}"
                + "h3{font-size:12pt;margin:16px 0 6px}p{white-space:pre-wrap}</style></head><body>");
        w.println("<h2>项目周报</h2>");
        w.println("<table>");
        w.println("<tr><th>项目编号</th><td>" + esc(r.get("project_no")) + "</th><th>项目名称</th><td>"
                + esc(r.get("project_name")) + "</td></tr>");
        w.println("<tr><th>提交人</th><td>" + esc(r.get("user_name")) + "</td><th>周次</th><td>"
                + esc(r.get("week_no")) + "</td></tr>");
        w.println("<tr><th>工时</th><td>" + esc(r.get("hours")) + " 小时</td><th>状态</th><td>"
                + esc(statusText(r.get("status"))) + "</td></tr>");
        w.println("<tr><th>提交时间</th><td>" + esc(r.get("submit_time")) + "</td><th>审核人</th><td>"
                + esc(r.get("reviewer")) + "</td></tr>");
        w.println("</table>");
        w.println("<h3>一、本周完成</h3><p>" + esc(r.get("done_text")) + "</p>");
        w.println("<h3>二、下周计划</h3><p>" + esc(r.get("plan_text")) + "</p>");
        w.println("<h3>三、问题与风险</h3><p>" + esc(r.get("issue_text")) + "</p>");
        w.println("<h3>四、审核意见</h3><p>" + esc(r.get("review"))
                + (r.get("score").isEmpty() ? "" : "（评分 " + esc(r.get("score")) + "）") + "</p>");
        w.println("<h3>五、附件清单</h3><p>"
                + (r.get("attach_name").isEmpty() ? "无附件"
                : esc(r.get("attach_name")) + "（" + esc(r.get("attach_size")) + " 字节）")
                + "</p>");
        w.println("</body></html>");
        w.flush();
        LogDao.write(uid, Web.uname(req), "EXPORT", "weekly:" + r.get("id"), "导出周报：" + fileName, "");
    }

    private static String esc(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String statusText(String v) {
        if ("SUBMITTED".equals(v)) {
            return "已提交";
        }
        if ("REVIEWED".equals(v)) {
            return "已审核";
        }
        if ("REJECTED".equals(v)) {
            return "已退回";
        }
        return "草稿";
    }

    private boolean DbId(String s) {
        return s != null && s.matches("\\d{1,18}");
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
