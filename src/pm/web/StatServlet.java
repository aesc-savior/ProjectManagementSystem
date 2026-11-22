package pm.web;

import pm.dao.ApprovalDao;
import pm.dao.Db;
import pm.dao.FineDao;
import pm.dao.ProjectDao;
import pm.dao.TaskDao;
import pm.dao.WeeklyDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// 统计：首页/报表页的数据汇总
public class StatServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String uid = Web.uid(req);
        boolean all = Web.isAdmin(req);

        long projectTotal = ProjectDao.countAll(uid, all);
        long taskTotal = TaskDao.count("", "", "", "", uid, all);
        long taskDone = Db.queryLong("select count(*) from pm_task t join pm_project p on p.id=t.project_id"
                + TaskDao.buildWhere("", "", "", "DONE", uid, all));
        long budget = 0;
        Map<String, String> b = ProjectDao.sumBudget(uid, all);
        if (b != null) {
            try { budget = (long) Double.parseDouble(b.get("total")); } catch (Exception ignore) { }
        }
        long unpaidFine = FineDao.sumUnpaid(uid, all);
        long pendingApproval = ApprovalDao.countPending(uid, all);

        req.setAttribute("projectTotal", projectTotal);
        req.setAttribute("taskTotal", taskTotal);
        req.setAttribute("taskDone", taskDone);
        req.setAttribute("budget", budget);
        req.setAttribute("unpaidFine", unpaidFine);
        req.setAttribute("pendingApproval", pendingApproval);
        req.setAttribute("projectStatus", ProjectDao.countByStatus(uid, all));
        req.setAttribute("taskStatus", TaskDao.countByStatus(uid, all));
        req.setAttribute("weeklyStatus", WeeklyDao.countByStatus(uid, all));
        List<Map<String, String>> monthly = Db.query(
                "select date_format(p.create_time,'%Y-%m') as m, count(*) as cnt from pm_project p"
                        + " where p.del_flag=0" + ProjectDao.scope(uid, all)
                        + " and p.create_time >= date_sub(curdate(), interval 6 month)"
                        + " group by date_format(p.create_time,'%Y-%m') order by m");
        req.setAttribute("monthly", monthly);
        req.getRequestDispatcher("/stat/index.jsp").forward(req, resp);
    }
}
