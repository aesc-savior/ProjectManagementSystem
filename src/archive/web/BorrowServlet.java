package archive.web;

import archive.dao.ArchiveDao;
import archive.dao.BorrowDao;
import archive.dao.LogDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

/** 借阅：/borrow?action=list|apply|approve|reject|return */
public class BorrowServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String ctx = req.getContextPath();
        String action = Web.p(req, "action");
        if (!action.isEmpty() && !"list".equals(action)) {
            resp.sendRedirect(ctx + "/borrow?action=list");
            return;
        }
        String uid = Web.uid(req);
        req.setAttribute("mine", BorrowDao.mine(uid));
        if (Web.isStaff(req)) {
            req.setAttribute("pending", BorrowDao.pending());
            req.setAttribute("all", BorrowDao.all());
        }
        req.getRequestDispatcher("/borrow/list.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        String action = Web.p(req, "action");
        String id = Web.p(req, "id");
        String ctx = req.getContextPath();

        if ("apply".equals(action)) {
            String archiveId = Web.p(req, "archiveId");
            String days = Web.p(req, "days");
            String reason = Web.p(req, "reason");
            String error = BorrowDao.apply(archiveId, Web.uid(req), Web.uname(req), reason, days);
            if (error != null) {
                redirect(req, resp, "/archive?action=view&id=" + archiveId, error);
                return;
            }
            LogDao.write(Web.uid(req), Web.uname(req), "APPLY", "archive:" + archiveId,
                    "申请借阅 " + days + " 天", Web.ip(req));
            redirect(req, resp, "/borrow?action=list", "借阅申请已提交，等待审批");
            return;
        }

        if (!Web.isStaff(req)) {
            redirect(req, resp, "/borrow?action=list", "只有档案管理员可以审批");
            return;
        }

        if ("approve".equals(action) || "reject".equals(action)) {
            String error = BorrowDao.approve(id, Web.uname(req), "approve".equals(action));
            if (error != null) {
                redirect(req, resp, "/borrow?action=list", error);
                return;
            }
            LogDao.write(Web.uid(req), Web.uname(req), "APPROVE", "borrow:" + id,
                    "approve".equals(action) ? "通过借阅" : "驳回借阅", Web.ip(req));
            redirect(req, resp, "/borrow?action=list", "审批完成");
        } else if ("return".equals(action)) {
            String error = BorrowDao.giveBack(id);
            if (error != null) {
                redirect(req, resp, "/borrow?action=list", error);
                return;
            }
            LogDao.write(Web.uid(req), Web.uname(req), "RETURN", "borrow:" + id, "登记归还", Web.ip(req));
            redirect(req, resp, "/borrow?action=list", "归还登记完成");
        } else {
            resp.sendRedirect(ctx + "/borrow?action=list");
        }
    }

    private void redirect(HttpServletRequest req, HttpServletResponse resp,
                          String path, String msg) throws IOException {
        String sep = path.contains("?") ? "&" : "?";
        resp.sendRedirect(req.getContextPath() + path + sep + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }
}
