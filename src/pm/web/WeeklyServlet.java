package pm.web;

import pm.dao.LogDao;
import pm.dao.ProjectDao;
import pm.dao.UserDao;
import pm.dao.WeeklyDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 周报：/weekly?action=list|addForm|editForm|save|submit|review */
public class WeeklyServlet extends HttpServlet {

    private static final int PAGE_SIZE = 12;

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

        String projectId = Web.p(req, "projectId");
        String userId = Web.p(req, "userId");
        String weekNo = Web.p(req, "weekNo");
        String status = Web.p(req, "status");
        int page = Math.max(1, Web.i(req, "page", 1));

        long total = WeeklyDao.count(projectId, userId, weekNo, status, uid, all);
        List<Map<String, String>> rows = WeeklyDao.list(projectId, userId, weekNo, status,
                page, PAGE_SIZE, uid, all);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pages", (int) Math.ceil(total / (double) PAGE_SIZE));
        req.setAttribute("fProjectId", projectId);
        req.setAttribute("fUserId", userId);
        req.setAttribute("fWeekNo", weekNo);
        req.setAttribute("fStatus", status);
        req.setAttribute("projects", ProjectDao.forSelect(uid, all));
        req.setAttribute("users", UserDao.activeUsers());
        req.setAttribute("currentWeek", WeeklyDao.currentWeek());
        req.getRequestDispatcher("/weekly/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
            LogDao.write(uid, Web.uname(req), id.isEmpty() ? "ADD" : "UPDATE", "weekly:" + savedId,
                    "保存周报：" + weekNo, Web.ip(req));
            redirect(req, resp, "/weekly?action=editForm&id=" + savedId, "保存成功");
        } else if ("submit".equals(action)) {
            Map<String, String> w = WeeklyDao.get(id);
            if (w == null || !uid.equals(w.get("user_id"))) {
                redirect(req, resp, "/weekly?action=list", "只能提交自己的周报");
                return;
            }
            WeeklyDao.submit(id);
            LogDao.write(uid, Web.uname(req), "SUBMIT", "weekly:" + id,
                    "提交 " + w.get("week_no") + " 周报", Web.ip(req));
            redirect(req, resp, "/weekly?action=list", "周报已提交");
        } else if ("review".equals(action)) {
            Map<String, String> w = WeeklyDao.get(id);
            if (w == null || !ProjectDao.canManage(w.get("project_id"), uid, Web.role(req), all)) {
                redirect(req, resp, "/weekly?action=list", "只有项目经理可以审核");
                return;
            }
            boolean pass = "approve".equals(Web.p(req, "decision"));
            int score = Web.i(req, "score", 0);
            WeeklyDao.review(id, pass, Web.p(req, "review"), score);
            LogDao.write(uid, Web.uname(req), "APPROVE", "weekly:" + id,
                    pass ? "审核通过" : "退回周报", Web.ip(req));
            redirect(req, resp, "/weekly?action=list", pass ? "审核完成" : "已退回");
        } else {
            resp.sendRedirect(ctx + "/weekly?action=list");
        }
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
