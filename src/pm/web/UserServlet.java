package pm.web;

import pm.dao.LogDao;
import pm.dao.UserDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

// 用户管理（仅系统管理员）：/user?action=list|addForm|editForm|save|toggle|delete
public class UserServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (!Web.isAdmin(req)) {
            deny(req, resp);
            return;
        }
        String action = Web.p(req, "action");
        if (action.isEmpty()) {
            action = "list";
        }
        if ("addForm".equals(action)) {
            req.getRequestDispatcher("/user/form.jsp").forward(req, resp);
        } else if ("editForm".equals(action)) {
            Map<String, String> row = UserDao.get(Web.p(req, "id"));
            if (row == null) {
                redirect(req, resp, "/user?action=list", "用户不存在");
                return;
            }
            req.setAttribute("row", row);
            req.getRequestDispatcher("/user/form.jsp").forward(req, resp);
        } else {
            String keyword = Web.p(req, "keyword");
            req.setAttribute("rows", UserDao.list(keyword));
            req.setAttribute("fKeyword", keyword);
            req.getRequestDispatcher("/user/list.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        if (!Web.isAdmin(req)) {
            deny(req, resp);
            return;
        }
        String action = Web.p(req, "action");
        String id = Web.p(req, "id");

        if ("save".equals(action)) {
            Map<String, String> p = new HashMap<String, String>();
            p.put("loginId", Web.p(req, "loginId"));
            p.put("userName", Web.p(req, "userName"));
            p.put("roleCode", Web.p(req, "roleCode"));
            p.put("deptName", Web.p(req, "deptName"));
            p.put("phone", Web.p(req, "phone"));
            p.put("email", Web.p(req, "email"));
            p.put("password", Web.p(req, "password"));
            p.put("creator", Web.uname(req));
            String error = UserDao.save(id, p);
            if (error != null) {
                redirect(req, resp, "/user?action=list", error);
                return;
            }
            LogDao.write(Web.uid(req), Web.uname(req), id.isEmpty() ? "ADD" : "UPDATE",
                    "user:" + id, "保存用户：" + p.get("userName"), Web.ip(req));
            redirect(req, resp, "/user?action=list", "保存成功");
        } else if ("toggle".equals(action)) {
            if (id.equals(Web.uid(req))) {
                redirect(req, resp, "/user?action=list", "不能停用当前登录账号");
                return;
            }
            UserDao.toggle(id);
            LogDao.write(Web.uid(req), Web.uname(req), "UPDATE", "user:" + id, "启用/停用", Web.ip(req));
            redirect(req, resp, "/user?action=list", "状态已切换");
        } else if ("delete".equals(action)) {
            if (id.equals(Web.uid(req))) {
                redirect(req, resp, "/user?action=list", "不能删除当前登录账号");
                return;
            }
            String error = UserDao.delete(id);
            if (error != null) {
                redirect(req, resp, "/user?action=list", error);
                return;
            }
            LogDao.write(Web.uid(req), Web.uname(req), "DELETE", "user:" + id, "删除用户", Web.ip(req));
            redirect(req, resp, "/user?action=list", "已删除");
        } else {
            resp.sendRedirect(req.getContextPath() + "/user?action=list");
        }
    }

    private void deny(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.sendRedirect(req.getContextPath() + "/home/index.jsp?msg="
                + URLEncoder.encode("只有系统管理员可以管理用户", "UTF-8"));
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
