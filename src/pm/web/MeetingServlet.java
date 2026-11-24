package pm.web;

import pm.dao.Db;
import pm.dao.LogDao;
import pm.dao.MeetingDao;
import pm.dao.ProjectDao;
import pm.dao.RemindDao;
import pm.dao.UserDao;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@MultipartConfig(fileSizeThreshold = 1048576,
        maxFileSize = 20971520, maxRequestSize = 26214400)
public class MeetingServlet extends HttpServlet {

    private static final int PAGE_SIZE = 12;

    private static final Set<String> ALLOWED = new HashSet<String>(Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "jpg", "jpeg", "png", "gif", "zip", "rar", "txt", "md"));

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        if (action.isEmpty()) {
            action = "list";
        }
        try {
            if ("addForm".equals(action)) {
                addForm(req, resp);
            } else if ("editForm".equals(action)) {
                editForm(req, resp);
            } else if ("view".equals(action)) {
                view(req, resp);
            } else if ("print".equals(action)) {
                print(req, resp);
            } else if ("printList".equals(action)) {
                printList(req, resp);
            } else if ("export".equals(action)) {
                export(req, resp);
            } else if ("attach".equals(action)) {
                attach(req, resp);
            } else {
                list(req, resp);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        try {
            if ("save".equals(action)) {
                save(req, resp);
            } else if ("cancel".equals(action)) {
                cancel(req, resp);
            } else if ("reply".equals(action)) {
                reply(req, resp);
            } else if ("deleteAttach".equals(action)) {
                deleteAttach(req, resp);
            } else {
                redirect(req, resp, "/meeting?action=list", "不支持的操作");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private void list(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String projectId = Web.p(req, "projectId");
        String userId = Web.p(req, "userId");
        String from = Web.p(req, "from");
        String to = Web.p(req, "to");
        String status = Web.p(req, "status");
        String keyword = Web.p(req, "keyword");
        int page = Math.max(1, Web.i(req, "page", 1));
        String uid = Web.uid(req);
        boolean all = Web.isAdmin(req);

        long total = MeetingDao.count(projectId, userId, from, to, status, keyword, uid, all);
        int pages = (int) Math.ceil(total / (double) PAGE_SIZE);
        if (pages < 1) {
            pages = 1;
        }
        if (page > pages) {
            page = pages;
        }
        List<Map<String, String>> rows = MeetingDao.list(projectId, userId, from, to, status,
                keyword, page, PAGE_SIZE, uid, all);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pages", pages);
        req.setAttribute("projects", ProjectDao.forSelect(uid, all));
        req.setAttribute("users", UserDao.activeUsers());
        req.setAttribute("fProjectId", projectId);
        req.setAttribute("fUserId", userId);
        req.setAttribute("fFrom", from);
        req.setAttribute("fTo", to);
        req.setAttribute("fStatus", status);
        req.setAttribute("fKeyword", keyword);
        req.getRequestDispatcher("/meeting/list.jsp").forward(req, resp);
    }

    private void addForm(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (Web.isViewer(req)) {
            redirect(req, resp, "/meeting?action=list", "查看者不能创建会议");
            return;
        }
        req.setAttribute("projects", ProjectDao.forSelect(Web.uid(req), Web.isAdmin(req)));
        req.setAttribute("users", UserDao.activeUsers());
        req.getRequestDispatcher("/meeting/form.jsp").forward(req, resp);
    }

    private void editForm(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String id = Web.p(req, "id");
        Map<String, String> row = MeetingDao.get(id, Web.uid(req));
        if (row == null) {
            redirect(req, resp, "/meeting?action=list", "会议不存在");
            return;
        }
        if (!canEdit(req, row)) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "没有权限修改该会议");
            return;
        }
        req.setAttribute("row", row);
        req.setAttribute("selectedIds", MeetingDao.attendeeIds(id));
        req.setAttribute("projects", ProjectDao.forSelect(Web.uid(req), Web.isAdmin(req)));
        req.setAttribute("users", UserDao.activeUsers());
        req.getRequestDispatcher("/meeting/form.jsp").forward(req, resp);
    }

    private void view(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String id = Web.p(req, "id");
        Map<String, String> row = MeetingDao.get(id, Web.uid(req));
        if (row == null) {
            redirect(req, resp, "/meeting?action=list", "会议不存在");
            return;
        }
        if (!canView(req, row)) {
            redirect(req, resp, "/meeting?action=list", "没有权限查看该会议");
            return;
        }
        req.setAttribute("row", row);
        req.setAttribute("attendees", MeetingDao.attendees(id));
        req.getRequestDispatcher("/meeting/view.jsp").forward(req, resp);
    }

    private void print(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String id = Web.p(req, "id");
        Map<String, String> row = MeetingDao.get(id, Web.uid(req));
        if (row == null) {
            redirect(req, resp, "/meeting?action=list", "会议不存在");
            return;
        }
        if (!canView(req, row)) {
            redirect(req, resp, "/meeting?action=list", "没有权限查看该会议");
            return;
        }
        req.setAttribute("row", row);
        req.setAttribute("attendees", MeetingDao.attendees(id));
        req.setAttribute("printTime", Db.now());
        req.getRequestDispatcher("/meeting/print.jsp").forward(req, resp);
    }

    private void printList(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String projectId = Web.p(req, "projectId");
        String userId = Web.p(req, "userId");
        String from = Web.p(req, "from");
        String to = Web.p(req, "to");
        String status = Web.p(req, "status");
        String keyword = Web.p(req, "keyword");
        String uid = Web.uid(req);
        boolean all = Web.isAdmin(req);

        List<Map<String, String>> rows = MeetingDao.listAll(projectId, userId, from, to,
                status, keyword, uid, all);
        long total = MeetingDao.count(projectId, userId, from, to, status, keyword, uid, all);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("fProjectId", projectId);
        req.setAttribute("fUserId", userId);
        req.setAttribute("fFrom", from);
        req.setAttribute("fTo", to);
        req.setAttribute("fStatus", status);
        req.setAttribute("fKeyword", keyword);
        req.setAttribute("printTime", Db.now());
        req.getRequestDispatcher("/meeting/printList.jsp").forward(req, resp);
    }

    private void export(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String projectId = Web.p(req, "projectId");
        String userId = Web.p(req, "userId");
        String from = Web.p(req, "from");
        String to = Web.p(req, "to");
        String status = Web.p(req, "status");
        String keyword = Web.p(req, "keyword");
        String uid = Web.uid(req);
        boolean all = Web.isAdmin(req);

        List<Map<String, String>> rows = MeetingDao.listAll(projectId, userId, from, to,
                status, keyword, uid, all);

        List<String> headers = Arrays.asList("会议主题", "关联项目", "开始时间", "结束时间",
                "地点", "组织人", "参会人数", "同意/请假", "状态");
        List<List<String>> data = new ArrayList<List<String>>();
        for (Map<String, String> r : rows) {
            List<String> line = new ArrayList<String>();
            line.add(r.get("topic"));
            line.add(r.get("project_name"));
            line.add(r.get("start_time"));
            line.add(r.get("end_time"));
            line.add(r.get("location"));
            line.add(r.get("organizer"));
            line.add(r.get("attendee_count"));
            line.add(r.get("accepted_count") + " / " + r.get("declined_count"));
            line.add(MeetingDao.statusText(r));
            data.add(line);
        }

        XlsxUtil.download(resp, "会议清单-" + Db.today() + ".xlsx", "会议清单", headers, data);
        LogDao.write(uid, Web.uname(req), "EXPORT", "meeting:list",
                "导出会议清单 " + rows.size() + " 条", Web.ip(req));
    }

    private void attach(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String id = Web.p(req, "id");
        Map<String, String> row = MeetingDao.get(id, Web.uid(req));
        if (row == null) {
            redirect(req, resp, "/meeting?action=list", "会议不存在");
            return;
        }
        if (!canView(req, row)) {
            redirect(req, resp, "/meeting?action=list", "没有权限查看该会议");
            return;
        }
        String path = s(row.get("attach_path"));
        if (path.isEmpty()) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "会议没有附件");
            return;
        }

        String baseDir = Conf.get("pm.upload.dir", "PM_UPLOAD_DIR",
                System.getProperty("java.io.tmpdir") + File.separator + "pm-upload");
        File base = new File(baseDir).getCanonicalFile();
        File file = new File(base, path).getCanonicalFile();
        if (!file.getPath().startsWith(base.getPath() + File.separator) || !file.isFile()) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "附件不存在");
            return;
        }

        String name = s(row.get("attach_name"));
        if (name.isEmpty()) {
            name = file.getName();
        }
        resp.setContentType("application/octet-stream");
        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(name, "UTF-8").replace("+", "%20"));
        resp.setContentLengthLong(file.length());
        Files.copy(file.toPath(), resp.getOutputStream());
    }

    private void save(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        if (Web.isViewer(req)) {
            redirect(req, resp, "/meeting?action=list", "查看者不能创建或修改会议");
            return;
        }

        String id = Web.p(req, "id");
        if (!id.isEmpty() && !Db.isId(id)) {
            redirect(req, resp, "/meeting?action=list", "参数错误");
            return;
        }
        Map<String, String> old = Db.isId(id) ? MeetingDao.get(id, Web.uid(req)) : null;
        if (Db.isId(id) && old == null) {
            redirect(req, resp, "/meeting?action=list", "会议不存在");
            return;
        }
        if (old != null && !canEdit(req, old)) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "没有权限修改该会议");
            return;
        }

        String topic = Web.p(req, "topic");
        String projectId = Web.p(req, "projectId");
        if (projectId.isEmpty()) {
            projectId = "0";
        }
        String start = normalize(Web.p(req, "startTime"));
        String end = normalize(Web.p(req, "endTime"));
        String location = Web.p(req, "location");
        String agenda = Web.p(req, "agenda");

        if (topic.isEmpty()) {
            redirect(req, resp, formBack(id), "请填写会议主题");
            return;
        }
        if (start.isEmpty() || end.isEmpty()) {
            redirect(req, resp, formBack(id), "请选择开始时间和结束时间");
            return;
        }
        if (start.compareTo(end) >= 0) {
            redirect(req, resp, formBack(id), "结束时间必须晚于开始时间");
            return;
        }
        if (!"0".equals(projectId)
                && (!Db.isId(projectId)
                || !ProjectDao.canView(projectId, Web.uid(req), Web.isAdmin(req)))) {
            redirect(req, resp, formBack(id), "无权关联该项目");
            return;
        }

        List<String> ids = parseIds(req.getParameterValues("attendeeIds"));
        if (Db.isId(Web.uid(req)) && !ids.contains(Web.uid(req))) {
            ids.add(Web.uid(req));
        }
        if (ids.isEmpty()) {
            redirect(req, resp, formBack(id), "请选择参会人");
            return;
        }

        if (!"1".equals(Web.p(req, "force"))) {
            List<Map<String, String>> conflicts = MeetingDao.conflicts(id, start, end, ids);
            if (!conflicts.isEmpty()) {
                req.setAttribute("conflicts", conflicts);
                req.setAttribute("row", formRow(id, topic, projectId, start, end, location, agenda));
                req.setAttribute("selectedIds", ids);
                req.setAttribute("projects", ProjectDao.forSelect(Web.uid(req), Web.isAdmin(req)));
                req.setAttribute("users", UserDao.activeUsers());
                req.getRequestDispatcher("/meeting/form.jsp").forward(req, resp);
                return;
            }
        }

        Map<String, String> p = new HashMap<String, String>();
        p.put("topic", topic);
        p.put("projectId", projectId);
        p.put("startTime", start);
        p.put("endTime", end);
        p.put("location", location);
        p.put("agenda", agenda);
        p.put("organizerId", old == null ? Web.uid(req) : old.get("organizer_id"));
        p.put("organizer", old == null ? Web.uname(req) : old.get("organizer"));
        p.put("creator", old == null ? Web.uname(req) : old.get("creator"));

        String mid = MeetingDao.save(id, p);
        if (mid.isEmpty()) {
            redirect(req, resp, formBack(id), "保存失败，请重试");
            return;
        }

        String event = old == null ? "创建" : "变更";
        boolean scheduleChanged = old == null
                || !start.equals(old.get("start_time"))
                || !end.equals(old.get("end_time"))
                || !s(location).equals(s(old.get("location")));

        syncAttendees(mid, p.get("organizerId"), ids, scheduleChanged);

        try {
            saveAttach(req, mid);
        } catch (Exception e) {
            Map<String, String> saved = MeetingDao.get(mid, Web.uid(req));
            if (saved != null) {
                notify(saved, event);
            }
            redirect(req, resp, "/meeting?action=view&id=" + mid,
                    "会议已保存，附件上传失败：" + e.getMessage());
            return;
        }

        Map<String, String> saved = MeetingDao.get(mid, Web.uid(req));
        if (saved != null) {
            notify(saved, event);
        }
        LogDao.write(Web.uid(req), Web.uname(req), old == null ? "ADD" : "UPDATE",
                "meeting:" + mid, (old == null ? "创建" : "修改") + "会议：" + topic, Web.ip(req));
        redirect(req, resp, "/meeting?action=view&id=" + mid,
                old == null ? "会议已创建并通知参会人" : "会议已更新并重新通知参会人");
    }

    private void cancel(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String id = Web.p(req, "id");
        Map<String, String> m = MeetingDao.get(id, Web.uid(req));
        if (m == null) {
            redirect(req, resp, "/meeting?action=list", "会议不存在");
            return;
        }
        if ("CANCELED".equals(m.get("status"))) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "会议已取消");
            return;
        }
        if (!canEdit(req, m)) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "没有权限取消该会议");
            return;
        }
        String reason = Web.p(req, "reason");
        if (reason.isEmpty()) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "取消会议必须填写理由");
            return;
        }
        if (!MeetingDao.cancel(id, reason)) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "取消失败，请刷新后重试");
            return;
        }
        Map<String, String> saved = MeetingDao.get(id, Web.uid(req));
        if (saved != null) {
            notifyCancel(saved, reason);
        }
        LogDao.write(Web.uid(req), Web.uname(req), "CANCEL", "meeting:" + id,
                "取消会议：" + m.get("topic") + "；理由：" + reason, Web.ip(req));
        redirect(req, resp, "/meeting?action=view&id=" + id, "会议已取消并通知参会人");
    }

    private void reply(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String id = Web.p(req, "id");
        Map<String, String> m = MeetingDao.get(id, Web.uid(req));
        if (m == null) {
            redirect(req, resp, "/meeting?action=list", "会议不存在");
            return;
        }
        if ("CANCELED".equals(m.get("status"))) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "会议已取消，不能回执");
            return;
        }
        if (m.get("end_time").compareTo(Db.now()) < 0) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "会议已结束，不能回执");
            return;
        }
        if (!MeetingDao.isAttendee(id, Web.uid(req))) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "你不是该会议参会人");
            return;
        }
        boolean accept = "accept".equals(Web.p(req, "decision"));
        String reason = Web.p(req, "reason");
        if (!accept && reason.isEmpty()) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "请假请填写理由");
            return;
        }
        if (!MeetingDao.reply(id, Web.uid(req), accept, reason)) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "回执失败，请重试");
            return;
        }
        LogDao.write(Web.uid(req), Web.uname(req), "REPLY", "meeting:" + id,
                (accept ? "参加" : "请假") + "会议：" + m.get("topic"), Web.ip(req));
        redirect(req, resp, "/meeting?action=view&id=" + id, "回执已提交");
    }

    private void deleteAttach(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String id = Web.p(req, "id");
        Map<String, String> m = MeetingDao.get(id, Web.uid(req));
        if (m == null) {
            redirect(req, resp, "/meeting?action=list", "会议不存在");
            return;
        }
        if (!canEdit(req, m)) {
            redirect(req, resp, "/meeting?action=view&id=" + id, "没有权限修改该会议");
            return;
        }
        String path = s(m.get("attach_path"));
        if (!path.isEmpty()) {
            String baseDir = Conf.get("pm.upload.dir", "PM_UPLOAD_DIR",
                    System.getProperty("java.io.tmpdir") + File.separator + "pm-upload");
            File base = new File(baseDir).getCanonicalFile();
            File file = new File(base, path).getCanonicalFile();
            if (file.getPath().startsWith(base.getPath() + File.separator) && file.isFile()) {
                file.delete();
            }
        }
        MeetingDao.clearAttach(id);
        LogDao.write(Web.uid(req), Web.uname(req), "UPDATE", "meeting:" + id,
                "删除附件：" + m.get("topic"), Web.ip(req));
        redirect(req, resp, "/meeting?action=view&id=" + id, "附件已删除");
    }

    private void saveAttach(HttpServletRequest req, String mid) throws Exception {
        String ct = req.getContentType();
        if (ct == null || !ct.toLowerCase().startsWith("multipart/")) {
            return;
        }
        Part part = req.getPart("attach");
        if (part == null || part.getSize() <= 0) {
            return;
        }
        String submitted = part.getSubmittedFileName();
        if (submitted == null || submitted.trim().isEmpty()) {
            return;
        }
        String name = new File(submitted).getName();
        String ext = name.contains(".")
                ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext)) {
            throw new IllegalArgumentException("附件类型不支持");
        }
        String dir = Conf.get("pm.upload.dir", "PM_UPLOAD_DIR",
                System.getProperty("java.io.tmpdir") + File.separator + "pm-upload");
        File folder = new File(dir, "meeting");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("上传目录不可写");
        }
        String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, new File(folder, stored).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        MeetingDao.updateAttach(mid, name, "meeting/" + stored, part.getSize());
    }

    private void syncAttendees(String mid, String organizerId, List<String> ids, boolean reset) {
        List<String> oldIds = MeetingDao.attendeeIds(mid);
        for (String u : ids) {
            if (!oldIds.contains(u)) {
                MeetingDao.addAttendee(mid, u, u.equals(organizerId) ? "ACCEPTED" : "PENDING");
            }
        }
        for (String u : oldIds) {
            if (!ids.contains(u) && !u.equals(organizerId)) {
                MeetingDao.removeAttendee(mid, u);
            }
        }
        MeetingDao.ensureOrganizer(mid, organizerId);
        if (reset) {
            MeetingDao.resetReplies(mid, organizerId);
        }
    }

    private void notify(Map<String, String> m, String event) {
        String title = "会议" + event + "：" + s(m.get("topic"));
        String project = s(m.get("project_name"));
        if (project.isEmpty()) {
            project = "无";
        }
        String place = s(m.get("location"));
        if (place.isEmpty()) {
            place = "待定";
        }
        String content = "项目：" + project
                + "\n时间：" + s(m.get("start_time")) + " 至 " + s(m.get("end_time"))
                + "\n地点：" + place + "\n请打开系统查看详情并回执。";
        notifyRaw(m, title, content);
    }

    private void notifyCancel(Map<String, String> m, String reason) {
        String title = "会议取消：" + s(m.get("topic"));
        String content = "原定 " + s(m.get("start_time")) + " 至 " + s(m.get("end_time"))
                + " 的会议已取消。\n取消理由：" + reason;
        notifyRaw(m, title, content);
    }

    private void notifyRaw(Map<String, String> m, String title, String content) {
        if (title.length() > 120) {
            title = title.substring(0, 120) + "…";
        }
        if (content.length() > 480) {
            content = content.substring(0, 480) + "…";
        }
        String[] wx;
        try {
            wx = WeChatNotifier.send(title, content);
        } catch (Exception e) {
            wx = new String[]{"FAILED", e.getMessage()};
        }
        for (Map<String, String> a : MeetingDao.attendees(m.get("id"))) {
            if (a.get("user_id").equals(m.get("organizer_id"))) {
                continue;
            }
            try {
                long rid = RemindDao.add(a.get("user_id"), a.get("user_name"), "MEETING",
                        title, content, "/meeting?action=view&id=" + m.get("id"), false);
                if (rid > 0) {
                    RemindDao.markWeChat(rid, wx[0], wx[1]);
                }
            } catch (Exception e) {
                // 单条提醒失败不影响其他参会人
            }
        }
    }

    private boolean canEdit(HttpServletRequest req, Map<String, String> m) {
        if ("CANCELED".equals(m.get("status"))) {
            return false;
        }
        if (Web.isAdmin(req)) {
            return true;
        }
        if (Web.uid(req).equals(m.get("organizer_id"))) {
            return true;
        }
        return Web.isPm(req) && ProjectDao.canManage(m.get("project_id"), Web.uid(req),
                Web.role(req), false);
    }

    private boolean canView(HttpServletRequest req, Map<String, String> m) {
        if (Web.isAdmin(req)) {
            return true;
        }
        if (Web.uid(req).equals(m.get("organizer_id"))) {
            return true;
        }
        return MeetingDao.isAttendee(m.get("id"), Web.uid(req));
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String url = req.getContextPath() + path;
        if (msg != null && !msg.isEmpty()) {
            url += (path.contains("?") ? "&" : "?") + "msg=" + URLEncoder.encode(msg, "UTF-8");
        }
        resp.sendRedirect(url);
    }

    private String formBack(String id) {
        return Db.isId(id) ? "/meeting?action=editForm&id=" + id : "/meeting?action=addForm";
    }

    private Map<String, String> formRow(String id, String topic, String projectId,
                                        String start, String end, String location, String agenda) {
        Map<String, String> r = new HashMap<String, String>();
        r.put("id", id);
        r.put("topic", topic);
        r.put("project_id", projectId);
        r.put("start_time", start);
        r.put("end_time", end);
        r.put("location", location);
        r.put("agenda", agenda);
        r.put("attach_name", "");
        r.put("attach_path", "");
        r.put("attach_size", "0");
        return r;
    }

    private String normalize(String v) {
        if (v == null) {
            return "";
        }
        String x = v.trim().replace('T', ' ');
        if (x.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
            x += ":00";
        }
        return x.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}") ? x : "";
    }

    private List<String> parseIds(String[] values) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        if (values != null) {
            for (String v : values) {
                if (Db.isId(v)) {
                    set.add(v);
                }
            }
        }
        return new ArrayList<String>(set);
    }

    private String s(String v) {
        return v == null ? "" : v;
    }
}