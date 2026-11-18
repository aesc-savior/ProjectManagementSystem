package pm.web;

import pm.dao.LogDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** 操作日志（管理员/项目经理）：/log?action=list */
public class LogServlet extends HttpServlet {

    private static final int PAGE_SIZE = 20;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!(Web.isAdmin(req) || "PM".equals(Web.role(req)))) {
            resp.sendRedirect(req.getContextPath() + "/home/index.jsp?msg="
                    + Web.enc("没有查看日志的权限"));
            return;
        }
        String action = Web.p(req, "action");
        if (!action.isEmpty() && !"list".equals(action)) {
            resp.sendRedirect(req.getContextPath() + "/log?action=list");
            return;
        }
        String actionFilter = Web.p(req, "fAction");
        String keyword = Web.p(req, "keyword");
        int page = Math.max(1, Web.i(req, "page", 1));

        long total = LogDao.count(actionFilter, keyword);
        List<Map<String, String>> rows = LogDao.list(actionFilter, keyword, page, PAGE_SIZE);

        req.setAttribute("rows", rows);
        req.setAttribute("total", total);
        req.setAttribute("page", page);
        req.setAttribute("pages", (int) Math.ceil(total / (double) PAGE_SIZE));
        req.setAttribute("fAction", actionFilter);
        req.setAttribute("fKeyword", keyword);
        req.getRequestDispatcher("/log/list.jsp").forward(req, resp);
    }
}
