package pm.web;

import pm.dao.Db;
import pm.dao.FineDao;
import pm.dao.LogDao;
import pm.dao.ProjectDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 罚款：/fine?action=list|generate|pay
public class FineServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String uid = Web.uid(req);
        boolean admin = Web.isAdmin(req);
        List<Map<String, String>> rows = FineDao.list(uid, admin);
        req.setAttribute("rows", rows);
        req.setAttribute("overdue", manageableOverdue(req, admin));
        req.setAttribute("unpaidIds", FineDao.unpaidProjects());
        req.setAttribute("unpaid", FineDao.sumUnpaid(uid, admin));
        req.getRequestDispatcher("/fine/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String ctx = req.getContextPath();
        String uid = Web.uid(req);
        boolean admin = Web.isAdmin(req);

        if ("generate".equals(action)) {
            if (!Web.isPm(req)) {
                redirect(req, resp, "/fine?action=list", "只有项目经理可以生成罚款单");
                return;
            }
            String projectId = Web.p(req, "projectId");
            // 只能给自己负责/管理的项目开罚单，避免越权罚别人的项目
            if (!ProjectDao.canManage(projectId, uid, Web.role(req), admin)) {
                redirect(req, resp, "/fine?action=list", "只能为本人负责的项目生成罚款单");
                return;
            }
            String error = FineDao.generate(projectId);
            if (error != null) {
                redirect(req, resp, "/fine?action=list", error);
                return;
            }
            LogDao.write(uid, Web.uname(req), "ADD", "fine:project:" + projectId,
                    "生成罚款单", Web.ip(req));
            redirect(req, resp, "/fine?action=list", "罚款单已生成");
        } else if ("pay".equals(action)) {
            String error = FineDao.pay(Web.p(req, "id"), uid, admin);
            if (error != null) {
                redirect(req, resp, "/fine?action=list", error);
                return;
            }
            LogDao.write(uid, Web.uname(req), "PAY", "fine:" + Web.p(req, "id"),
                    "缴纳罚款（模拟微信支付）", Web.ip(req));
            redirect(req, resp, "/fine?action=list", "支付成功（模拟）");
        } else {
            resp.sendRedirect(ctx + "/fine?action=list");
        }
    }

    // 当前用户可开罚单的超期项目：管理员全部，PM 仅本人负责或项目内 MANAGER 的项目，
    // 其他角色为空。原来直接返回全库超期项目，属于越权（能看到也能罚别人的项目）。
    private List<Map<String, String>> manageableOverdue(HttpServletRequest req, boolean admin) {
        List<Map<String, String>> all = ProjectDao.overdue();
        if (admin) {
            return all;
        }
        if (!Web.isPm(req)) {
            return new ArrayList<Map<String, String>>();
        }
        Set<String> manageIds = new HashSet<String>();
        String uid = Web.uid(req);
        if (Db.isId(uid)) {
            List<Map<String, String>> mine = Db.query(
                    "select p.id from pm_project p where p.del_flag=0 and (p.owner_id=" + uid
                            + " or exists(select 1 from pm_project_member m where m.project_id=p.id"
                            + " and m.user_id=" + uid + " and m.role_code='MANAGER'))");
            for (Map<String, String> r : mine) {
                manageIds.add(r.get("id"));
            }
        }
        List<Map<String, String>> out = new ArrayList<Map<String, String>>();
        for (Map<String, String> p : all) {
            if (manageIds.contains(p.get("id"))) {
                out.add(p);
            }
        }
        return out;
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
