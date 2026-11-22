package pm.web;

import pm.dao.RemindDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;

// 站内提醒：/remind?action=list|read|readAll|go
public class RemindServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String uid = Web.uid(req);
        String ctx = req.getContextPath();

        if ("read".equals(action) || "go".equals(action)) {
            String id = Web.p(req, "id");
            RemindDao.markRead(id, uid);
            String url = Web.p(req, "url");
            if (!url.isEmpty() && url.startsWith("/") && !url.startsWith("//")) {
                resp.sendRedirect(ctx + url);
            } else {
                resp.sendRedirect(ctx + "/remind?action=list");
            }
            return;
        }

        if ("readAll".equals(action)) {
            RemindDao.markAllRead(uid);
            resp.sendRedirect(ctx + "/remind?action=list&msg="
                    + URLEncoder.encode("已全部标记为已读", "UTF-8"));
            return;
        }

        String type = Web.p(req, "type");
        req.setAttribute("rows", RemindDao.list(uid, type));
        req.setAttribute("fType", type);
        req.setAttribute("unread", RemindDao.unreadCount(uid));
        req.getRequestDispatcher("/remind/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        if ("readAll".equals(action)) {
            RemindDao.markAllRead(Web.uid(req));
        }
        resp.sendRedirect(req.getContextPath() + "/remind?action=list");
    }
}
