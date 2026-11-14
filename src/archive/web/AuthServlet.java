package archive.web;

import archive.dao.LogDao;
import archive.dao.UserDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;

/** 登录与退出：POST /auth?action=login，GET /auth?action=logout。 */
public class AuthServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String action = Web.p(req, "action");
        String ctx = req.getContextPath();
        if ("logout".equals(action)) {
            HttpSession s = req.getSession(false);
            if (s != null) {
                s.invalidate();
            }
            resp.sendRedirect(ctx + "/login.jsp?msg=" + Web.enc("已安全退出"));
            return;
        }
        resp.sendRedirect(ctx + "/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String ctx = req.getContextPath();
        String action = Web.p(req, "action");
        if (!"login".equals(action)) {
            resp.sendRedirect(ctx + "/login.jsp");
            return;
        }

        String loginId = Web.p(req, "loginId");
        String password = Web.p(req, "password");
        Map<String, String> user = UserDao.login(loginId, password);
        if (user == null) {
            resp.sendRedirect(ctx + "/login.jsp?error=1");
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("uid", user.get("id"));
        session.setAttribute("uname", user.get("user_name"));
        session.setAttribute("role", user.get("role_code"));
        session.setAttribute("dept", user.get("dept_name"));
        session.setAttribute("loginId", user.get("login_id"));
        String secret = user.get("secret_level");
        session.setAttribute("secret", secret == null || secret.isEmpty() ? "1" : secret);
        session.setMaxInactiveInterval(60 * 60);

        LogDao.write(user.get("id"), user.get("user_name"), "LOGIN", "user:" + user.get("id"),
                "登录系统", Web.ip(req));
        resp.sendRedirect(ctx + "/home/index.jsp");
    }
}
