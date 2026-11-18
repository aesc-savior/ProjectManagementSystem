package pm.web;

import pm.dao.FineDao;
import pm.dao.LogDao;
import pm.dao.ProjectDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/** 罚款：/fine?action=list|generate|pay */
public class FineServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String uid = Web.uid(req);
        boolean admin = Web.isAdmin(req);
        List<Map<String, String>> rows = FineDao.list(uid, admin);
        req.setAttribute("rows", rows);
        req.setAttribute("overdue", ProjectDao.overdue());
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

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
