package pm.web;

import pm.dao.LogDao;
import pm.dao.ProjectDao;
import pm.dao.TaskDao;
import pm.dao.UserDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 任务模块：/task?action=list|addForm|editForm|save|delete|progress */
public class TaskServlet extends HttpServlet {

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
            if (!Web.isPm(req) || !ProjectDao.canManage(projectId, uid, Web.role(req), all)) {
                redirect(req, resp, "/task?action=list", "只有项目经理可以在该项目下创建任务");
                return;
            }
            req.setAttribute("projectId", projectId);
            req.setAttribute("projects", ProjectDao.forSelect(uid, all));
            req.setAttribute("users", UserDao.activeUsers());
            req.setAttribute("parents", TaskDao.topTasks(projectId));
            req.getRequestDispatcher("/task/form.jsp").forward(req, resp);
            return;
        }

        if ("editForm".equals(action)) {
            Map<String, String> row = TaskDao.get(Web.p(req, "id"));
            if (row == null || !ProjectDao.canView(row.get("project_id"), uid, all)) {
                redirect(req, resp, "/task?action=list", "任务不存在或没有权限");
                return;
            }
            req.setAttribute("row", row);
            req.setAttribute("projectId", row.get("project_id"));
            req.setAttribute("projects", ProjectDao.forSelect(uid, all));
            req.setAttribute("users", UserDao.activeUsers());
            req.setAttribute("parents", TaskDao.topTasks(row.get("project_id")));
            req.getRequestDispatcher("/task/form.jsp").forward(req, resp);
            return;
        }

        String keyword = Web.p(req, "keyword");
        String projectId = Web.p(req, "projectId");
        String assigneeId = Web.p(req, "assigneeId");
        String status = Web.p(req, "status");
        String sort = Web.p(req, "sort");
        int page = Math.max(1, Web.i(req, "page", 1));

        long total = TaskDao.count(keyword, projectId, assigneeId, status, uid, all);
        List<Map<String, String>> rows = TaskDao.list(keyword, projectId, assigneeId, status, sort,
                page, PAGE_SIZE, uid, all);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pages", (int) Math.ceil(total / (double) PAGE_SIZE));
        req.setAttribute("fKeyword", keyword);
        req.setAttribute("fProjectId", projectId);
        req.setAttribute("fAssigneeId", assigneeId);
        req.setAttribute("fStatus", status);
        req.setAttribute("fSort", sort);
        req.setAttribute("projects", ProjectDao.forSelect(uid, all));
        req.setAttribute("users", UserDao.activeUsers());
        req.getRequestDispatcher("/task/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String id = Web.p(req, "id");
        String uid = Web.uid(req);
        boolean admin = Web.isAdmin(req);
        String ctx = req.getContextPath();

        if ("save".equals(action)) {
            String projectId = Web.p(req, "projectId");
            Map<String, String> old = TaskDao.get(id);
            boolean allowed;
            if (old != null) {
                allowed = (Web.isPm(req) && ProjectDao.canManage(old.get("project_id"), uid, Web.role(req), admin))
                        || uid.equals(old.get("assignee_id"));
            } else {
                allowed = Web.isPm(req) && ProjectDao.canManage(projectId, uid, Web.role(req), admin);
            }
            if (!allowed) {
                redirect(req, resp, "/task?action=list", "没有保存该任务的权限");
                return;
            }
            String title = Web.p(req, "title");
            if (title.isEmpty()) {
                redirect(req, resp, "/task?action=list", "任务标题不能为空");
                return;
            }
            if (!projectId.matches("\\d{1,18}")) {
                redirect(req, resp, "/task?action=list", "请选择所属项目");
                return;
            }
            Map<String, String> p = new HashMap<String, String>();
            p.put("projectId", projectId);
            p.put("parentId", Web.p(req, "parentId"));
            p.put("title", title);
            p.put("description", Web.p(req, "description"));
            p.put("assigneeId", Web.p(req, "assigneeId"));
            p.put("priority", Web.p(req, "priority"));
            p.put("status", Web.p(req, "status"));
            p.put("progress", Web.p(req, "progress"));
            p.put("startDate", Web.p(req, "startDate"));
            p.put("dueDate", Web.p(req, "dueDate"));
            p.put("estimateHours", Web.p(req, "estimateHours"));
            p.put("actualHours", Web.p(req, "actualHours"));
            p.put("creator", Web.uname(req));
            String savedId = TaskDao.save(id, p);
            LogDao.write(uid, Web.uname(req), id.isEmpty() ? "ADD" : "UPDATE", "task:" + savedId,
                    (id.isEmpty() ? "创建" : "修改") + "任务：" + title, Web.ip(req));
            redirect(req, resp, "/task?action=editForm&id=" + savedId, "保存成功");
        } else if ("delete".equals(action)) {
            Map<String, String> task = TaskDao.get(id);
            if (task == null || !(Web.isPm(req) && ProjectDao.canManage(task.get("project_id"), uid, Web.role(req), admin))) {
                redirect(req, resp, "/task?action=list", "没有删除权限");
                return;
            }
            String error = TaskDao.delete(id);
            if (error != null) {
                redirect(req, resp, "/task?action=list", error);
                return;
            }
            LogDao.write(uid, Web.uname(req), "DELETE", "task:" + id, "删除任务", Web.ip(req));
            redirect(req, resp, "/task?action=list", "任务已删除");
        } else if ("progress".equals(action)) {
            Map<String, String> task = TaskDao.get(id);
            if (task == null) {
                redirect(req, resp, "/task?action=list", "任务不存在");
                return;
            }
            boolean allowed = uid.equals(task.get("assignee_id"))
                    || (Web.isPm(req) && ProjectDao.canManage(task.get("project_id"), uid, Web.role(req), admin));
            if (!allowed) {
                redirect(req, resp, "/task?action=list", "只有任务负责人可以更新进度");
                return;
            }
            int progress = Web.i(req, "progress", 0);
            TaskDao.updateProgress(id, progress, Web.p(req, "actualHours"));
            LogDao.write(uid, Web.uname(req), "UPDATE", "task:" + id, "更新进度为 " + progress + "%", Web.ip(req));
            redirect(req, resp, "/task?action=editForm&id=" + id, "进度已更新");
        } else {
            resp.sendRedirect(ctx + "/task?action=list");
        }
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
