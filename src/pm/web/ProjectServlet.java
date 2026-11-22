package pm.web;

import pm.dao.LogDao;
import pm.dao.ProjectDao;
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

// 项目：列表、详情、保存、删除、归档、状态流转、成员维护
// 页面在 web/project/ 下
public class ProjectServlet extends HttpServlet {

    private static final int PAGE_SIZE = 10;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        if (action.isEmpty()) {
            action = "list";
        }
        if ("addForm".equals(action)) {
            if (!Web.isPm(req)) {
                deny(req, resp);
                return;
            }
            req.setAttribute("users", UserDao.activeUsers());
            req.getRequestDispatcher("/project/form.jsp").forward(req, resp);
        } else if ("editForm".equals(action)) {
            Map<String, String> row = ProjectDao.get(Web.p(req, "id"));
            if (row == null || !ProjectDao.canManage(row.get("id"), Web.uid(req), Web.role(req), Web.isAdmin(req))) {
                redirect(req, resp, "/project?action=list", "项目不存在或没有编辑权限");
                return;
            }
            req.setAttribute("row", row);
            req.setAttribute("users", UserDao.activeUsers());
            req.getRequestDispatcher("/project/form.jsp").forward(req, resp);
        } else if ("view".equals(action)) {
            view(req, resp);
        } else {
            list(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        if (!Web.isPm(req)) {
            deny(req, resp);
            return;
        }
        String action = Web.p(req, "action");
        String id = Web.p(req, "id");
        String ctx = req.getContextPath();

        if ("save".equals(action)) {
            boolean edit = !id.isEmpty();
            if (edit && !ProjectDao.canManage(id, Web.uid(req), Web.role(req), Web.isAdmin(req))) {
                redirect(req, resp, "/project?action=list", "只有项目负责人可以编辑该项目");
                return;
            }
            String name = Web.p(req, "name");
            if (name.isEmpty()) {
                redirect(req, resp, edit ? "/project?action=editForm&id=" + id : "/project?action=addForm", "项目名称不能为空");
                return;
            }
            String start = Web.p(req, "startDate");
            String end = Web.p(req, "endDate");
            if (!start.isEmpty() && !end.isEmpty() && start.compareTo(end) > 0) {
                redirect(req, resp, edit ? "/project?action=editForm&id=" + id : "/project?action=addForm",
                        "结束时间不能早于开始时间");
                return;
            }
            Map<String, String> p = new HashMap<String, String>();
            p.put("name", name);
            p.put("projectType", Web.p(req, "projectType"));
            p.put("priority", Web.p(req, "priority"));
            p.put("status", Web.p(req, "status"));
            p.put("startDate", start);
            p.put("endDate", end);
            p.put("ownerId", Web.p(req, "ownerId"));
            p.put("budget", Web.p(req, "budget"));
            p.put("deptName", Web.p(req, "deptName"));
            p.put("description", Web.p(req, "description"));
            p.put("creator", Web.uname(req));
            String savedId = ProjectDao.save(id, p);
            LogDao.write(Web.uid(req), Web.uname(req), edit ? "UPDATE" : "ADD", "project:" + savedId,
                    (edit ? "修改" : "创建") + "项目：" + name, Web.ip(req));
            redirect(req, resp, "/project?action=view&id=" + savedId, "保存成功");
        } else if ("delete".equals(action)) {
            if (!ProjectDao.canManage(id, Web.uid(req), Web.role(req), Web.isAdmin(req))) {
                redirect(req, resp, "/project?action=list", "没有删除权限");
                return;
            }
            ProjectDao.delete(id);
            LogDao.write(Web.uid(req), Web.uname(req), "DELETE", "project:" + id, "删除项目", Web.ip(req));
            redirect(req, resp, "/project?action=list", "项目已删除（逻辑删除）");
        } else if ("archive".equals(action)) {
            if (!ProjectDao.canManage(id, Web.uid(req), Web.role(req), Web.isAdmin(req))) {
                redirect(req, resp, "/project?action=list", "没有归档权限");
                return;
            }
            ProjectDao.archive(id);
            LogDao.write(Web.uid(req), Web.uname(req), "ARCHIVE", "project:" + id, "归档项目", Web.ip(req));
            redirect(req, resp, "/project?action=view&id=" + id, "已归档");
        } else if ("status".equals(action)) {
            if (!ProjectDao.canManage(id, Web.uid(req), Web.role(req), Web.isAdmin(req))) {
                redirect(req, resp, "/project?action=view&id=" + id, "没有变更状态的权限");
                return;
            }
            String status = Web.p(req, "status");
            ProjectDao.changeStatus(id, status);
            LogDao.write(Web.uid(req), Web.uname(req), "UPDATE", "project:" + id, "状态变更为 " + status, Web.ip(req));
            redirect(req, resp, "/project?action=view&id=" + id, "状态已更新");
        } else if ("memberAdd".equals(action)) {
            if (!ProjectDao.canManage(id, Web.uid(req), Web.role(req), Web.isAdmin(req))) {
                redirect(req, resp, "/project?action=view&id=" + id, "没有成员管理权限");
                return;
            }
            String error = ProjectDao.addMember(id, Web.p(req, "userId"), Web.p(req, "memberRole"));
            if (error != null) {
                redirect(req, resp, "/project?action=view&id=" + id, error);
                return;
            }
            LogDao.write(Web.uid(req), Web.uname(req), "ADD", "project:" + id, "添加项目成员", Web.ip(req));
            redirect(req, resp, "/project?action=view&id=" + id, "成员已添加");
        } else if ("memberRemove".equals(action)) {
            if (!ProjectDao.canManage(id, Web.uid(req), Web.role(req), Web.isAdmin(req))) {
                redirect(req, resp, "/project?action=view&id=" + id, "没有成员管理权限");
                return;
            }
            ProjectDao.removeMember(id, Web.p(req, "userId"));
            LogDao.write(Web.uid(req), Web.uname(req), "DELETE", "project:" + id, "移除项目成员", Web.ip(req));
            redirect(req, resp, "/project?action=view&id=" + id, "成员已移除");
        } else {
            resp.sendRedirect(ctx + "/project?action=list");
        }
    }

    // 列表
    private void list(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String keyword = Web.p(req, "keyword");
        String status = Web.p(req, "status");
        String type = Web.p(req, "projectType");
        String priority = Web.p(req, "priority");
        String sort = Web.p(req, "sort");
        int page = Math.max(1, Web.i(req, "page", 1));
        boolean all = Web.isAdmin(req);
        String uid = Web.uid(req);

        long total = ProjectDao.count(keyword, status, type, priority, uid, all);
        List<Map<String, String>> rows =
                ProjectDao.list(keyword, status, type, priority, sort, page, PAGE_SIZE, uid, all);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pageSize", PAGE_SIZE);
        req.setAttribute("pages", (int) Math.ceil(total / (double) PAGE_SIZE));
        req.setAttribute("fKeyword", keyword);
        req.setAttribute("fStatus", status);
        req.setAttribute("fType", type);
        req.setAttribute("fPriority", priority);
        req.setAttribute("fSort", sort);
        req.getRequestDispatcher("/project/list.jsp").forward(req, resp);
    }

    // 详情
    private void view(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String id = Web.p(req, "id");
        Map<String, String> row = ProjectDao.get(id);
        if (row == null || !ProjectDao.canView(id, Web.uid(req), Web.isAdmin(req))) {
            req.setAttribute("error", "项目不存在，或你没有查看权限");
            req.getRequestDispatcher("/project/view.jsp").forward(req, resp);
            return;
        }
        req.setAttribute("row", row);
        req.setAttribute("members", ProjectDao.members(id));
        req.setAttribute("tasks", pm.dao.Db.query(
                "select t.id,t.title,t.status,t.progress,t.due_date,ifnull(u.user_name,'未分配') as assignee"
                        + " from pm_task t left join pm_user u on u.id=t.assignee_id"
                        + " where t.project_id=" + id + " and t.del_flag=0 order by t.id desc limit 12"));
        req.setAttribute("users", UserDao.activeUsers());
        req.setAttribute("weeklyCount", pm.dao.Db.queryLong(
                "select count(*) from pm_weekly where project_id=" + id));
        req.setAttribute("fineTotal", pm.dao.Db.queryLong(
                "select ifnull(sum(amount),0) from pm_fine where project_id=" + id));
        req.getRequestDispatcher("/project/view.jsp").forward(req, resp);
    }

    private void deny(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/home/index.jsp?msg="
                + URLEncoder.encode("只有系统管理员或项目经理可以维护项目", "UTF-8"));
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
