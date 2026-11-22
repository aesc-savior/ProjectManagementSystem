package pm.mp;

import pm.dao.ApprovalDao;
import pm.dao.Db;
import pm.dao.FineDao;
import pm.dao.LogDao;
import pm.dao.UserDao;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * 小程序端唯一入口：/api/mp/*
 *
 * 鉴权统一在 MpAuthFilter 完成（/ping、/auth/login、/auth/bind 免鉴权），
 * 这里只管路由和业务。用注解注册，不动 web/WEB-INF/web.xml。
 *
 * ── 接口清单 ────────────────────────────────────────────────
 * GET  /ping                      探活
 * GET  /config                    服务端能力（罚款单价、令牌天数、是否 mock）
 * GET  /me                        当前账号
 * GET  /stats                     工作台概览
 * GET  /projects?keyword&status&page&size
 * GET  /projects/{id}             详情（含成员）
 * GET  /tasks?projectId&mine&status&keyword&page&size
 * GET  /tasks/{id}                详情（含子任务）
 * GET  /notices?page&size
 * GET  /notices/{id}
 * GET  /approvals?tab=pending|mine|done&page&size
 * GET  /approvals/{id}
 * GET  /fines?status=UNPAID|PAID&page&size
 * GET  /fines/{id}
 *
 * POST /auth/login                {code}
 * POST /auth/bind                 {code, loginId, password}
 * POST /auth/logout
 * POST /auth/unbind
 * POST /tasks/{id}/progress       {progress, actualHours}
 * POST /notices/{id}/sign
 * POST /approvals/{id}/decide     {pass, opinion}
 * POST /fines/{id}/pay
 * POST /upload                    multipart，见 MpUploadServlet
 * ────────────────────────────────────────────────────────────
 */
@WebServlet(urlPatterns = "/api/mp/*")
public class MpApiServlet extends HttpServlet {

    private static final int DEFAULT_SIZE = 10;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String[] seg = segs(req);

        if (seg.length == 0) {
            ok(resp, Json.obj().put("service", "mp-api").put("mock", WxApi.isMock())
                    .put("hint", "GET /ping 探活，POST /auth/bind 首次绑定"));
            return;
        }

        String head = seg[0];
        String id = seg.length > 1 ? seg[1] : "";

        if ("ping".equals(head)) {
            ok(resp, Json.obj().put("service", "mp-api").put("mock", WxApi.isMock())
                    .put("serverTime", Db.now()));
            return;
        }
        if ("config".equals(head)) {
            ok(resp, Json.obj()
                    .put("mock", WxApi.isMock())
                    .put("finePerDay", (long) FineDao.PER_DAY)
                    .put("tokenDays", (long) MpDao.TOKEN_DAYS)
                    .put("maxUploadMb", (long) MpUploadServlet.MAX_MB)
                    .put("serverTime", Db.now()));
            return;
        }
        if ("me".equals(head)) {
            ok(resp, Json.obj().put("user", userJson(user(req))));
            return;
        }
        if ("stats".equals(head)) {
            ok(resp, statsJson(req));
            return;
        }
        if ("projects".equals(head)) {
            projects(req, resp, id);
            return;
        }
        if ("tasks".equals(head)) {
            tasks(req, resp, id);
            return;
        }
        if ("notices".equals(head)) {
            notices(req, resp, id);
            return;
        }
        if ("approvals".equals(head)) {
            approvals(req, resp, id);
            return;
        }
        if ("fines".equals(head)) {
            fines(req, resp, id);
            return;
        }
        fail(resp, 404, 404, "接口不存在：" + req.getPathInfo());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String[] seg = segs(req);

        if (seg.length >= 2 && "auth".equals(seg[0])) {
            String action = seg[1];
            if ("login".equals(action)) {
                login(req, resp);
                return;
            }
            if ("bind".equals(action)) {
                bind(req, resp);
                return;
            }
            if ("logout".equals(action)) {
                MpDao.logout((String) req.getAttribute(MpAuthFilter.ATTR_TOKEN));
                ok(resp, Json.obj().put("logout", true));
                return;
            }
            if ("unbind".equals(action)) {
                Map<String, String> u = user(req);
                MpDao.unbind(u.get("user_id"));
                LogDao.write(u.get("user_id"), u.get("user_name"), "UPDATE",
                        "mp:bind:" + u.get("user_id"), "小程序解绑微信", ip(req));
                ok(resp, Json.obj().put("unbind", true));
                return;
            }
        }

        // 提交延期申请：/approvals/apply
        if (seg.length == 2 && "approvals".equals(seg[0]) && "apply".equals(seg[1])) {
            Map<String, String> u = user(req);
            String error = ApprovalDao.applyDelay(param(req, "taskId"), param(req, "newDate"),
                    param(req, "reason"), u.get("user_id"), u.get("user_name"));
            if (error != null) {
                fail(resp, 400, 400, error);
                return;
            }
            LogDao.write(u.get("user_id"), u.get("user_name"), "UPDATE",
                    "mp:approval:apply", "提交任务延期申请（小程序）", ip(req));
            ok(resp, Json.obj().put("applied", true));
            return;
        }

        // 写操作：/资源/{id}/动作
        if (seg.length >= 3) {
            String head = seg[0];
            String id = seg[1];
            String action = seg[2];

            if ("tasks".equals(head) && "progress".equals(action)) {
                String error = MpQuery.updateProgress(id, intParam(req, "progress", -1),
                        param(req, "actualHours"), uid(req), all(req));
                if (error != null) {
                    fail(resp, 400, 400, error);
                    return;
                }
                ok(resp, Json.obj().put("task", taskJson(MpQuery.task(id, uid(req), all(req)))));
                return;
            }
            if ("notices".equals(head) && "sign".equals(action)) {
                String error = MpQuery.sign(id, uid(req));
                if (error != null) {
                    fail(resp, 400, 400, error);
                    return;
                }
                ok(resp, Json.obj().put("signed", true)
                        .put("readCount", longOf(String.valueOf(
                                Db.queryLong("select count(*) from pm_notice_read where notice_id="
                                        + Db.esc(id))))));
                return;
            }
            if ("approvals".equals(head) && "decide".equals(action)) {
                boolean pass = boolParam(req, "pass", false);
                String error = ApprovalDao.decide(id, pass, param(req, "opinion"),
                        uid(req), all(req));
                if (error != null) {
                    fail(resp, 400, 400, error);
                    return;
                }
                Map<String, String> u = user(req);
                LogDao.write(u.get("user_id"), u.get("user_name"), "UPDATE",
                        "mp:approval:" + id, (pass ? "通过" : "驳回") + "审批（小程序）", ip(req));
                ok(resp, Json.obj().put("pass", pass)
                        .put("status", pass ? "APPROVED" : "REJECTED"));
                return;
            }
            if ("fines".equals(head) && "pay".equals(action)) {
                String error = FineDao.pay(id, uid(req), all(req));
                if (error != null) {
                    fail(resp, 400, 400, error);
                    return;
                }
                Map<String, String> u = user(req);
                LogDao.write(u.get("user_id"), u.get("user_name"), "UPDATE",
                        "mp:fine:" + id, "缴纳罚款（小程序）", ip(req));
                ok(resp, Json.obj().put("paid", true).put("fine", fineJson(FineDao.get(id))));
                return;
            }
        }
        fail(resp, 404, 404, "接口不存在：" + req.getPathInfo());
    }

    // ================= 业务：读 =================

    private void projects(HttpServletRequest req, HttpServletResponse resp, String id) throws IOException {
        if (!id.isEmpty()) {
            Map<String, String> p = MpQuery.project(id, uid(req), all(req));
            if (p == null) {
                fail(resp, 404, 404, "项目不存在或无权查看");
                return;
            }
            ok(resp, Json.obj()
                    .put("project", projectJson(p))
                    .putRows("members", MpQuery.members(id)));
            return;
        }
        int page = Math.max(1, intParam(req, "page", 1));
        int size = size(req);
        String keyword = param(req, "keyword");
        String status = param(req, "status");
        List<Map<String, String>> rows = MpQuery.projects(uid(req), all(req), keyword, status,
                (page - 1) * size, size);
        long total = MpQuery.countProjects(uid(req), all(req), keyword, status);
        paged(resp, rows, total, page, size);
    }

    private void tasks(HttpServletRequest req, HttpServletResponse resp, String id) throws IOException {
        if (!id.isEmpty()) {
            Map<String, String> t = MpQuery.task(id, uid(req), all(req));
            if (t == null) {
                fail(resp, 404, 404, "任务不存在或无权查看");
                return;
            }
            ok(resp, Json.obj().put("task", taskJson(t))
                    .putRows("children", MpQuery.children(id)));
            return;
        }
        int page = Math.max(1, intParam(req, "page", 1));
        int size = size(req);
        String projectId = param(req, "projectId");
        boolean mine = boolParam(req, "mine", false);
        String status = param(req, "status");
        String keyword = param(req, "keyword");
        List<Map<String, String>> rows = MpQuery.tasks(uid(req), all(req), projectId, mine,
                status, keyword, (page - 1) * size, size);
        long total = MpQuery.countTasks(uid(req), all(req), projectId, mine, status, keyword);
        paged(resp, rows, total, page, size);
    }

    private void notices(HttpServletRequest req, HttpServletResponse resp, String id) throws IOException {
        if (!id.isEmpty()) {
            Map<String, String> n = MpQuery.notice(id, uid(req), all(req));
            if (n == null) {
                fail(resp, 404, 404, "公告不存在或无权查看");
                return;
            }
            ok(resp, Json.obj().put("notice", noticeJson(n, true)));
            return;
        }
        int page = Math.max(1, intParam(req, "page", 1));
        int size = size(req);
        List<Map<String, String>> rows = MpQuery.notices(uid(req), all(req), (page - 1) * size, size);
        paged(resp, rows, MpQuery.countNotices(uid(req), all(req)), page, size);
    }

    private void approvals(HttpServletRequest req, HttpServletResponse resp, String id) throws IOException {
        if (!id.isEmpty()) {
            Map<String, String> a = ApprovalDao.get(id);
            if (a == null) {
                fail(resp, 404, 404, "审批记录不存在");
                return;
            }
            // 申请人、审批人、管理员可以看；approver_id 为空（申请人=负责人）时管理员兜底
            String me = uid(req);
            boolean allowed = all(req) || me.equals(a.get("applicant_id"))
                    || me.equals(a.get("approver_id"));
            if (!allowed) {
                fail(resp, 403, 403, "无权查看该审批");
                return;
            }
            ok(resp, Json.obj().put("approval", approvalJson(a)));
            return;
        }
        int page = Math.max(1, intParam(req, "page", 1));
        int size = size(req);
        String tab = param(req, "tab");
        if (tab.isEmpty()) {
            tab = "pending";
        }
        int offset = (page - 1) * size;
        List<Map<String, String>> rows;
        long total;
        if ("mine".equals(tab)) {
            rows = ApprovalDao.mine(uid(req), offset, size);
            total = ApprovalDao.countMine(uid(req));
        } else if ("done".equals(tab)) {
            rows = ApprovalDao.done(uid(req), all(req), offset, size);
            total = ApprovalDao.countDone(uid(req), all(req));
        } else {
            rows = ApprovalDao.pending(uid(req), all(req), offset, size);
            total = ApprovalDao.countPending(uid(req), all(req));
        }
        paged(resp, rows, total, page, size);
    }

    private void fines(HttpServletRequest req, HttpServletResponse resp, String id) throws IOException {
        if (!id.isEmpty()) {
            Map<String, String> f = FineDao.get(id);
            if (f == null) {
                fail(resp, 404, 404, "罚款单不存在");
                return;
            }
            if (!all(req) && !uid(req).equals(f.get("user_id"))) {
                fail(resp, 403, 403, "只能查看自己的罚款单");
                return;
            }
            ok(resp, Json.obj().put("fine", fineJson(f)));
            return;
        }
        int page = Math.max(1, intParam(req, "page", 1));
        int size = size(req);
        String status = param(req, "status");
        List<Map<String, String>> rows = FineDao.list(uid(req), all(req), status,
                (page - 1) * size, size);
        paged(resp, rows, FineDao.count(uid(req), all(req), status), page, size);
    }

    private Json statsJson(HttpServletRequest req) {
        Map<String, Object> s = MpQuery.overview(uid(req), all(req));
        Json data = Json.obj();
        for (Map.Entry<String, Object> e : s.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Number) {
                data.put(e.getKey(), ((Number) v).longValue());
            } else {
                data.put(e.getKey(), String.valueOf(v));
            }
        }
        return data;
    }

    // ================= 业务：登录 =================

    /** 已绑定：换令牌直接登录。未绑定：返回 409 + needBind，让前端跳绑定页。 */
    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String openid;
        try {
            openid = WxApi.codeToOpenid(param(req, "code"));
        } catch (IOException e) {
            fail(resp, 401, 401, e.getMessage());
            return;
        }

        Map<String, String> binding = MpDao.bindingByOpenid(openid);
        if (binding == null) {
            fail(resp, 409, 409, "该微信尚未绑定平台账号");
            return;
        }
        Map<String, String> user = MpDao.activeUser(binding.get("user_id"));
        if (user == null) {
            fail(resp, 403, 403, "账号已停用或已删除，请联系管理员");
            return;
        }

        String nickName = param(req, "nickName");
        String avatar = param(req, "avatarUrl");
        if (!nickName.isEmpty() || !avatar.isEmpty()) {
            MpDao.updateProfile(openid, nickName, avatar);
        }

        MpDao.purgeExpired();
        String token = MpDao.newToken(user.get("id"), openid);
        LogDao.write(user.get("id"), user.get("user_name"), "LOGIN",
                "mp:user:" + user.get("id"), "小程序登录", ip(req));
        tokenResponse(resp, token, user);
    }

    /** 首次使用：账号密码 + openid 建立绑定，同时返回令牌。 */
    private void bind(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String loginId = param(req, "loginId");
        String password = param(req, "password");
        if (loginId.isEmpty() || password.isEmpty()) {
            fail(resp, 400, 400, "请输入平台账号和密码");
            return;
        }
        Map<String, String> account = UserDao.login(loginId, password);
        if (account == null) {
            fail(resp, 401, 401, "账号或密码不正确，或账号已停用");
            return;
        }
        String openid;
        try {
            openid = WxApi.codeToOpenid(param(req, "code"));
        } catch (IOException e) {
            fail(resp, 401, 401, e.getMessage());
            return;
        }

        String error = MpDao.bind(account.get("id"), openid,
                param(req, "nickName"), param(req, "avatarUrl"));
        if (error != null) {
            fail(resp, 400, 400, error);
            return;
        }

        MpDao.purgeExpired();
        String token = MpDao.newToken(account.get("id"), openid);
        Map<String, String> user = MpDao.activeUser(account.get("id"));
        LogDao.write(account.get("id"), account.get("user_name"), "UPDATE",
                "mp:bind:" + account.get("id"), "小程序绑定微信账号", ip(req));
        tokenResponse(resp, token, user);
    }

    private void tokenResponse(HttpServletResponse resp, String token, Map<String, String> user)
            throws IOException {
        resp.setStatus(200);
        write(resp, Json.obj().put("code", 200).put("message", "ok")
                .raw("data", Json.obj()
                        .put("token", token)
                        .put("expireAt", MpDao.expireAt(token))
                        .put("mock", WxApi.isMock())
                        .put("user", userJson(user))
                        .done()).done());
    }

    // ================= 输出模型 =================

    private Json userJson(Map<String, String> u) {
        if (u == null) {
            return Json.obj();
        }
        return Json.obj()
                .put("userId", u.get("user_id") == null ? u.get("id") : u.get("user_id"))
                .put("loginId", u.get("login_id"))
                .put("userName", u.get("user_name"))
                .put("role", u.get("role_code"))
                .put("roleName", roleName(u.get("role_code")))
                .put("dept", u.get("dept_name"))
                .put("phone", u.get("phone"))
                .put("email", u.get("email"));
    }

    private Json projectJson(Map<String, String> p) {
        return Json.obj()
                .put("id", p.get("id"))
                .put("projectNo", p.get("project_no"))
                .put("name", p.get("name"))
                .put("status", p.get("status"))
                .put("priority", p.get("priority"))
                .put("projectType", p.get("project_type"))
                .put("startDate", p.get("start_date"))
                .put("endDate", p.get("end_date"))
                .put("ownerName", p.get("owner_name"))
                .put("dept", p.get("dept_name"))
                .put("description", p.get("description"))
                .put("taskCount", longOf(p.get("task_count")))
                .put("doneCount", longOf(p.get("done_count")))
                .put("progress", longOf(p.get("progress")))
                .put("overdueDays", longOf(p.get("overdue_days")));
    }

    private Json taskJson(Map<String, String> t) {
        if (t == null) {
            return Json.obj();
        }
        return Json.obj()
                .put("id", t.get("id"))
                .put("projectId", t.get("project_id"))
                .put("projectName", t.get("project_name"))
                .put("parentId", t.get("parent_id"))
                .put("parentTitle", t.get("parent_title"))
                .put("title", t.get("title"))
                .put("description", t.get("description"))
                .put("assigneeId", t.get("assignee_id"))
                .put("assignee", t.get("assignee"))
                .put("priority", t.get("priority"))
                .put("status", t.get("status"))
                .put("progress", longOf(t.get("progress")))
                .put("startDate", t.get("start_date"))
                .put("dueDate", t.get("due_date"))
                .put("estimateHours", t.get("estimate_hours"))
                .put("actualHours", t.get("actual_hours"))
                .put("overdueDays", longOf(t.get("overdue_days")));
    }

    private Json noticeJson(Map<String, String> n, boolean withContent) {
        Json j = Json.obj()
                .put("id", n.get("id"))
                .put("title", n.get("title"))
                .put("scope", n.get("scope"))
                .put("projectName", n.get("project_name"))
                .put("publisher", n.get("publisher"))
                .put("createTime", n.get("create_time"))
                .put("expireTime", n.get("expire_time"))
                .put("readCount", longOf(n.get("read_count")))
                .put("signed", longOf(n.get("signed")) > 0);
        if (withContent) {
            j.put("content", n.get("content"));
        }
        return j;
    }

    private Json approvalJson(Map<String, String> a) {
        return Json.obj()
                .put("id", a.get("id"))
                .put("bizType", a.get("biz_type"))
                .put("bizId", a.get("biz_id"))
                .put("title", a.get("title"))
                .put("applicantId", a.get("applicant_id"))
                .put("applicant", a.get("applicant"))
                .put("approverId", a.get("approver_id"))
                .put("approverName", a.get("approver_name"))
                .put("status", a.get("status"))
                .put("detail", a.get("detail"))
                .put("oldDate", a.get("old_date"))
                .put("newDate", a.get("new_date"))
                .put("opinion", a.get("opinion"))
                .put("createTime", a.get("create_time"))
                .put("approveTime", a.get("approve_time"))
                .put("taskTitle", a.get("task_title"))
                .put("projectName", a.get("project_name"));
    }

    private Json fineJson(Map<String, String> f) {
        long amount = longOf(f.get("amount"));
        return Json.obj()
                .put("id", f.get("id"))
                .put("projectId", f.get("project_id"))
                .put("projectName", f.get("project_name"))
                .put("projectNo", f.get("project_no"))
                .put("userId", f.get("user_id"))
                .put("userName", f.get("user_name"))
                .put("amount", amount)
                .put("reason", f.get("reason"))
                .put("status", f.get("status"))
                .put("dueDate", f.get("due_date"))
                .put("tradeNo", f.get("trade_no"))
                .put("payTime", f.get("pay_time"))
                .put("createTime", f.get("create_time"));
    }

    private String roleName(String role) {
        if ("ADMIN".equals(role)) {
            return "系统管理员";
        }
        if ("PM".equals(role)) {
            return "项目经理";
        }
        if ("VIEWER".equals(role)) {
            return "查看者";
        }
        return "项目成员";
    }

    // ================= 工具 =================

    private Map<String, String> user(HttpServletRequest req) {
        @SuppressWarnings("unchecked")
        Map<String, String> u = (Map<String, String>) req.getAttribute(MpAuthFilter.ATTR_USER);
        return u;
    }

    private String uid(HttpServletRequest req) {
        Map<String, String> u = user(req);
        return u == null ? "" : u.get("user_id");
    }

    /** 管理员看全部数据；其余人只看自己负责/参与的范围。 */
    private boolean all(HttpServletRequest req) {
        Map<String, String> u = user(req);
        return u != null && "ADMIN".equals(u.get("role_code"));
    }

    private int size(HttpServletRequest req) throws IOException {
        int size = intParam(req, "size", DEFAULT_SIZE);
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        return Math.min(size, 50);
    }

    private long longOf(String s) {
        if (s == null || s.trim().isEmpty() || "null".equals(s)) {
            return 0L;
        }
        try {
            return (long) Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String[] segs(HttpServletRequest req) {
        String p = req.getPathInfo();
        if (p == null) {
            return new String[0];
        }
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        while (p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p.isEmpty() ? new String[0] : p.split("/");
    }

    private int intParam(HttpServletRequest req, String key, int def) throws IOException {
        String v = param(req, key);
        if (v == null || v.trim().isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private boolean boolParam(HttpServletRequest req, String key, boolean def) throws IOException {
        String v = param(req, key);
        if (v == null || v.trim().isEmpty()) {
            return def;
        }
        String s = v.trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }

    /** 取参数：优先表单/查询串，其次 JSON body（body 只读一次，缓存在 attribute 里）。 */
    private String param(HttpServletRequest req, String key) throws IOException {
        String v = req.getParameter(key);
        if (v != null && !v.trim().isEmpty()) {
            return v.trim();
        }
        String json = Json.pick(body(req), key);
        return json == null ? "" : json.trim();
    }

    private String body(HttpServletRequest req) throws IOException {
        Object cached = req.getAttribute("mpBody");
        if (cached != null) {
            return (String) cached;
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader br = req.getReader();
        char[] buf = new char[512];
        int n;
        while ((n = br.read(buf)) > 0) {
            sb.append(buf, 0, n);
        }
        String s = sb.toString();
        req.setAttribute("mpBody", s);
        return s;
    }

    private String ip(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private void paged(HttpServletResponse resp, List<Map<String, String>> rows, long total,
                       int page, int size) throws IOException {
        Json data = Json.obj()
                .put("page", (long) page)
                .put("size", (long) size)
                .put("total", total)
                .put("hasMore", (long) page * size < total)
                .putRows("list", rows);
        ok(resp, data);
    }

    private void ok(HttpServletResponse resp, Json data) throws IOException {
        resp.setStatus(200);
        write(resp, Json.obj().put("code", 200).put("message", "ok")
                .raw("data", data.done()).done());
    }

    private void fail(HttpServletResponse resp, int httpStatus, int code, String msg) throws IOException {
        resp.setStatus(httpStatus);
        write(resp, Json.obj().put("code", code).put("message", msg)
                .put("data", (String) null).done());
    }

    private void write(HttpServletResponse resp, String json) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.write(json);
        out.flush();
    }
}
