package pm.web;

import pm.dao.LogDao;
import pm.dao.NoticeDao;
import pm.dao.ProjectDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

// 公告：/notice?action=list|view|addForm|save|delete|sign
public class NoticeServlet extends HttpServlet {

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

        if ("view".equals(action)) {
            Map<String, String> row = NoticeDao.get(Web.p(req, "id"), uid);
            if (row == null) {
                redirect(req, resp, "/notice?action=list", "公告不存在");
                return;
            }
            if (!all && "PROJECT".equals(row.get("scope"))
                    && !ProjectDao.canView(row.get("project_id"), uid, false)) {
                redirect(req, resp, "/notice?action=list", "你没有该公告的查看权限");
                return;
            }
            req.setAttribute("row", row);
            req.getRequestDispatcher("/notice/view.jsp").forward(req, resp);
            return;
        }

        if ("addForm".equals(action)) {
            if (!Web.isPm(req)) {
                redirect(req, resp, "/notice?action=list", "只有项目经理可以发布公告");
                return;
            }
            req.setAttribute("projects", ProjectDao.forSelect(uid, all));
            req.getRequestDispatcher("/notice/form.jsp").forward(req, resp);
            return;
        }

        List<Map<String, String>> rows = NoticeDao.list(Web.p(req, "keyword"), uid, all);
        req.setAttribute("rows", rows);
        req.setAttribute("fKeyword", Web.p(req, "keyword"));
        req.getRequestDispatcher("/notice/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String id = Web.p(req, "id");
        String uid = Web.uid(req);
        String ctx = req.getContextPath();

        if ("save".equals(action)) {
            if (!Web.isPm(req)) {
                redirect(req, resp, "/notice?action=list", "只有项目经理可以发布公告");
                return;
            }
            String title = Web.p(req, "title");
            if (title.isEmpty()) {
                redirect(req, resp, "/notice?action=addForm", "公告标题不能为空");
                return;
            }
            String scope = Web.p(req, "scope");
            if (!"ALL".equals(scope) && !"PROJECT".equals(scope)) {
                scope = "ALL";
            }
            NoticeDao.add(title, Web.p(req, "content"), scope, Web.p(req, "projectId"),
                    Web.p(req, "expireTime"), uid, Web.uname(req));
            LogDao.write(uid, Web.uname(req), "ADD", "notice", "发布公告：" + title, Web.ip(req));
            redirect(req, resp, "/notice?action=list", "公告已发布");
        } else if ("delete".equals(action)) {
            if (!Web.isPm(req)) {
                redirect(req, resp, "/notice?action=list", "没有删除权限");
                return;
            }
            NoticeDao.delete(id);
            LogDao.write(uid, Web.uname(req), "DELETE", "notice:" + id, "删除公告", Web.ip(req));
            redirect(req, resp, "/notice?action=list", "公告已删除");
        } else if ("sign".equals(action)) {
            NoticeDao.sign(id, uid);
            LogDao.write(uid, Web.uname(req), "UPDATE", "notice:" + id, "签收公告", Web.ip(req));
            redirect(req, resp, "/notice?action=view&id=" + id, "已签收");
        } else {
            resp.sendRedirect(ctx + "/notice?action=list");
        }
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
