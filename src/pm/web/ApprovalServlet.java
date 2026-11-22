package pm.web;

import pm.dao.ApprovalDao;
import pm.dao.LogDao;
import pm.dao.ProjectDao;
import pm.dao.TaskDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

// 审批：/approval?action=list|applyForm|apply|decide
public class ApprovalServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        if (action.isEmpty()) {
            action = "list";
        }
        String uid = Web.uid(req);
        boolean admin = Web.isAdmin(req);

        if ("applyForm".equals(action)) {
            Map<String, String> task = TaskDao.get(Web.p(req, "taskId"));
            if (task == null) {
                redirect(req, resp, "/approval?action=list", "任务不存在");
                return;
            }
            boolean allowed = uid.equals(task.get("assignee_id"))
                    || (Web.isPm(req) && ProjectDao.canManage(task.get("project_id"), uid, Web.role(req), admin));
            if (!allowed) {
                redirect(req, resp, "/approval?action=list", "只有任务负责人可以申请延期");
                return;
            }
            req.setAttribute("task", task);
            req.getRequestDispatcher("/approval/form.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("pending", ApprovalDao.pending(uid, admin));
        req.setAttribute("mine", ApprovalDao.mine(uid));
        req.getRequestDispatcher("/approval/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String uid = Web.uid(req);
        boolean admin = Web.isAdmin(req);
        String ctx = req.getContextPath();

        if ("apply".equals(action)) {
            String taskId = Web.p(req, "taskId");
            Map<String, String> task = TaskDao.get(taskId);
            if (task == null) {
                redirect(req, resp, "/approval?action=list", "任务不存在");
                return;
            }
            boolean allowed = uid.equals(task.get("assignee_id"))
                    || (Web.isPm(req) && ProjectDao.canManage(task.get("project_id"), uid, Web.role(req), admin));
            if (!allowed) {
                redirect(req, resp, "/approval?action=list", "没有申请权限");
                return;
            }
            String error = ApprovalDao.applyDelay(taskId, Web.p(req, "newDate"),
                    Web.p(req, "reason"), uid, Web.uname(req));
            if (error != null) {
                redirect(req, resp, "/approval?action=applyForm&taskId=" + taskId, error);
                return;
            }
            LogDao.write(uid, Web.uname(req), "ADD", "approval:task:" + taskId,
                    "提交任务延期申请", Web.ip(req));
            redirect(req, resp, "/approval?action=list", "延期申请已提交");
        } else if ("decide".equals(action)) {
            boolean pass = "approve".equals(Web.p(req, "decision"));
            String error = ApprovalDao.decide(Web.p(req, "id"), pass, Web.p(req, "opinion"), uid, admin);
            if (error != null) {
                redirect(req, resp, "/approval?action=list", error);
                return;
            }
            LogDao.write(uid, Web.uname(req), "APPROVE", "approval:" + Web.p(req, "id"),
                    pass ? "审批通过" : "审批驳回", Web.ip(req));
            redirect(req, resp, "/approval?action=list", "审批完成");
        } else {
            resp.sendRedirect(ctx + "/approval?action=list");
        }
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
