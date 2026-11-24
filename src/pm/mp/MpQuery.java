package pm.mp;

import pm.dao.ApprovalDao;
import pm.dao.Db;
import pm.dao.FineDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 小程序端专用的数据访问。
 *
 * 为什么不复用 ProjectDao / TaskDao / NoticeDao：
 * 那三个 DAO 属于 A/B/E 组，他们的方法签名随时可能变。这里自己写只读 SQL，
 * 小程序端只依赖表结构（pm_* 的字段），不依赖别人的 Java 方法，
 * 合并代码时不会互相卡住。
 *
 * 可见性规则和 PC 端保持一致：管理员看全部，其余人看「自己负责 + 自己参与」。
 */
public final class MpQuery {

    private MpQuery() {
    }

    // ---------------- 公共片段 ----------------

    private static String sql(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replace("\\", "\\\\").replace("'", "''");
    }

    private static String lit(String s) {
        return (s == null || s.trim().isEmpty()) ? "NULL" : "'" + sql(s) + "'";
    }

    /** 项目可见性：all=true（管理员）不加限制。 */
    private static String scope(String uid, boolean all) {
        if (all) {
            return "";
        }
        String u = Db.esc(uid);
        return " and (p.owner_id=" + u + " or exists(select 1 from pm_project_member m"
                + " where m.project_id=p.id and m.user_id=" + u + "))";
    }

    private static String limit(int offset, int size) {
        int s = size <= 0 ? 20 : Math.min(size, 100);
        return " limit " + Math.max(0, offset) + "," + s;
    }

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static final String PROJECT_SELECT =
            "select p.id, p.project_no, p.name, p.project_type, p.priority, p.status,"
                    + " p.start_date, p.end_date, p.owner_id, p.dept_name, p.description,"
                    + " p.budget, ifnull(u.user_name,'未指定') as owner_name,"
                    + " (select count(*) from pm_task t where t.project_id=p.id and t.del_flag=0) as task_count,"
                    + " (select count(*) from pm_task t where t.project_id=p.id and t.del_flag=0"
                    + "   and t.status='DONE') as done_count,"
                    + " (select ifnull(round(avg(t.progress)),0) from pm_task t"
                    + "   where t.project_id=p.id and t.del_flag=0) as progress,"
                    + " datediff(curdate(), p.end_date) as overdue_days"
                    + " from pm_project p left join pm_user u on u.id=p.owner_id";

    private static final String TASK_SELECT =
            "select t.id, t.project_id, t.parent_id, t.title, t.description, t.assignee_id,"
                    + " t.priority, t.status, t.progress, t.start_date, t.due_date,"
                    + " t.estimate_hours, t.actual_hours, t.create_time,"
                    + " p.name as project_name, p.project_no,"
                    + " ifnull(u.user_name,'未分配') as assignee,"
                    + " ifnull(pt.title,'') as parent_title,"
                    + " datediff(curdate(), t.due_date) as overdue_days"
                    + " from pm_task t join pm_project p on p.id=t.project_id"
                    + " left join pm_user u on u.id=t.assignee_id"
                    + " left join pm_task pt on pt.id=t.parent_id";

    // ---------------- 项目 ----------------

    public static List<Map<String, String>> projects(String uid, boolean all, String keyword,
                                                     String status, int offset, int size) {
        List<String> w = new ArrayList<String>();
        w.add("p.del_flag=0");
        if (!blank(keyword)) {
            String k = sql(keyword);
            w.add("(p.name like '%" + k + "%' or p.project_no like '%" + k + "%')");
        }
        if (!blank(status) && status.matches("[A-Z]{3,10}")) {
            w.add("p.status='" + status + "'");
        }
        return Db.query(PROJECT_SELECT + " where " + String.join(" and ", w)
                + scope(uid, all) + " order by p.id desc" + limit(offset, size));
    }

    public static long countProjects(String uid, boolean all, String keyword, String status) {
        List<String> w = new ArrayList<String>();
        w.add("p.del_flag=0");
        if (!blank(keyword)) {
            String k = sql(keyword);
            w.add("(p.name like '%" + k + "%' or p.project_no like '%" + k + "%')");
        }
        if (!blank(status) && status.matches("[A-Z]{3,10}")) {
            w.add("p.status='" + status + "'");
        }
        return Db.queryLong("select count(*) from pm_project p where " + String.join(" and ", w)
                + scope(uid, all));
    }

    public static Map<String, String> project(String id, String uid, boolean all) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(PROJECT_SELECT + " where p.id=" + id + " and p.del_flag=0" + scope(uid, all));
    }

    public static boolean canViewProject(String projectId, String uid, boolean all) {
        return project(projectId, uid, all) != null;
    }

    public static List<Map<String, String>> members(String projectId) {
        if (!Db.isId(projectId)) {
            return new ArrayList<Map<String, String>>();
        }
        return Db.query("select m.user_id, m.role_code, ifnull(u.user_name,'') as user_name,"
                + " ifnull(u.dept_name,'') as dept_name"
                + " from pm_project_member m join pm_user u on u.id=m.user_id"
                + " where m.project_id=" + projectId + " and u.del_flag=0 order by m.id");
    }

    // ---------------- 任务 ----------------

    public static List<Map<String, String>> tasks(String uid, boolean all, String projectId,
                                                  boolean mineOnly, String status, String keyword,
                                                  int offset, int size) {
        return Db.query(TASK_SELECT + taskWhere(uid, all, projectId, mineOnly, status, keyword)
                + " order by t.due_date is null, t.due_date asc, t.id desc" + limit(offset, size));
    }

    public static long countTasks(String uid, boolean all, String projectId, boolean mineOnly,
                                  String status, String keyword) {
        return Db.queryLong("select count(*) from pm_task t join pm_project p on p.id=t.project_id"
                + taskWhere(uid, all, projectId, mineOnly, status, keyword));
    }

    private static String taskWhere(String uid, boolean all, String projectId, boolean mineOnly,
                                    String status, String keyword) {
        List<String> w = new ArrayList<String>();
        w.add("t.del_flag=0 and p.del_flag=0");
        if (Db.isId(projectId)) {
            w.add("t.project_id=" + projectId);
        }
        if (mineOnly) {
            w.add("t.assignee_id=" + Db.esc(uid));
        }
        if (!blank(status) && status.matches("[A-Z]{3,10}")) {
            w.add("t.status='" + status + "'");
        }
        if (!blank(keyword)) {
            String k = sql(keyword);
            w.add("(t.title like '%" + k + "%' or t.description like '%" + k + "%')");
        }
        return " where " + String.join(" and ", w) + scope(uid, all);
    }

    public static Map<String, String> task(String id, String uid, boolean all) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(TASK_SELECT + " where t.id=" + id + " and t.del_flag=0" + scope(uid, all));
    }

    /** 任务下的子任务（详情页展示 WBS 一层）。 */
    public static List<Map<String, String>> children(String taskId) {
        if (!Db.isId(taskId)) {
            return new ArrayList<Map<String, String>>();
        }
        return Db.query(TASK_SELECT + " where t.parent_id=" + taskId + " and t.del_flag=0"
                + " order by t.id");
    }

    /**
     * 上报进度（PM-117）：只有任务负责人本人、项目负责人或管理员可以改。
     * 状态跟着进度走，并向上汇总父任务进度（与 PC 端规则一致）。
     */
    public static String updateProgress(String taskId, int progress, String actualHours,
                                       String uid, boolean all) {
        Map<String, String> t = task(taskId, uid, all);
        if (t == null) {
            return "任务不存在或无权访问";
        }
        if (!all && !uid.equals(t.get("assignee_id"))) {
            boolean manager = Db.queryLong("select count(*) from pm_project p where p.id="
                    + t.get("project_id") + " and p.del_flag=0 and p.owner_id=" + Db.esc(uid)) > 0;
            if (!manager) {
                return "只有任务负责人或项目负责人可以更新进度";
            }
        }
        if (progress < 0 || progress > 100) {
            return "进度必须在 0 ~ 100 之间";
        }
        String hours = (actualHours != null && actualHours.matches("\\d{1,6}(\\.\\d{1,2})?"))
                ? actualHours : null;
        String status = progress >= 100 ? "DONE" : (progress > 0 ? "DOING" : "TODO");

        StringBuilder set = new StringBuilder("set progress=").append(progress)
                .append(", status='").append(status).append("'");
        if (hours != null) {
            set.append(", actual_hours=").append(hours);
        }
        Db.update("update pm_task " + set + " where id=" + taskId);
        rollup(t.get("parent_id"));
        return null;
    }

    private static void rollup(String parentId) {
        if (!Db.isId(parentId) || "0".equals(parentId)) {
            return;
        }
        Map<String, String> agg = Db.queryOne("select count(*) as n, ifnull(round(avg(progress)),0) as p"
                + " from pm_task where parent_id=" + parentId + " and del_flag=0");
        if (agg == null || "0".equals(agg.get("n"))) {
            return;
        }
        int p;
        try {
            p = Integer.parseInt(agg.get("p"));
        } catch (NumberFormatException e) {
            p = 0;
        }
        String status = p >= 100 ? "DONE" : (p > 0 ? "DOING" : "TODO");
        Db.update("update pm_task set progress=" + p + ", status='" + status + "' where id=" + parentId);
        Map<String, String> t = Db.queryOne("select parent_id from pm_task where id=" + parentId);
        if (t != null) {
            rollup(t.get("parent_id"));
        }
    }

    // ---------------- 公告 ----------------

    private static String noticeSelect(String uid) {
        return "select n.id, n.title, n.content, n.scope, n.project_id, n.expire_time,"
                + " n.publisher, n.create_time, ifnull(p.name,'') as project_name,"
                + " (select count(*) from pm_notice_read r where r.notice_id=n.id) as read_count,"
                + " (select count(*) from pm_notice_read r where r.notice_id=n.id"
                + "   and r.user_id=" + Db.esc(uid) + ") as signed"
                + " from pm_notice n left join pm_project p on p.id=n.project_id";
    }

    public static List<Map<String, String>> notices(String uid, boolean all, int offset, int size) {
        return Db.query(noticeSelect(uid) + noticeWhere(uid, all) + " order by n.id desc"
                + limit(offset, size));
    }

    public static long countNotices(String uid, boolean all) {
        return Db.queryLong("select count(*) from pm_notice n" + noticeWhere(uid, all));
    }

    /** 未签收数量（工作台红点用）。 */
    public static long unsignedNotices(String uid, boolean all) {
        return Db.queryLong("select count(*) from pm_notice n where n.del_flag=0"
                + (all ? "" : noticeScope(uid))
                + " and not exists(select 1 from pm_notice_read r"
                + " where r.notice_id=n.id and r.user_id=" + Db.esc(uid) + ")");
    }

    private static String noticeWhere(String uid, boolean all) {
        return " where n.del_flag=0" + (all ? "" : noticeScope(uid));
    }

    private static String noticeScope(String uid) {
        String u = Db.esc(uid);
        return " and (n.scope='ALL' or n.project_id in (select p.id from pm_project p"
                + " where p.del_flag=0 and (p.owner_id=" + u + " or exists("
                + "select 1 from pm_project_member m where m.project_id=p.id and m.user_id=" + u + "))))";
    }

    public static Map<String, String> notice(String id, String uid, boolean all) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(noticeSelect(uid) + " where n.id=" + id + " and n.del_flag=0"
                + (all ? "" : noticeScope(uid)));
    }

    /** 签收（PM-118）：唯一键保证重复点击只记一次。 */
    public static String sign(String noticeId, String uid) {
        Map<String, String> n = notice(noticeId, uid, true);
        if (n == null) {
            return "公告不存在";
        }
        long exists = Db.queryLong("select count(*) from pm_notice_read"
                + " where notice_id=" + noticeId + " and user_id=" + Db.esc(uid));
        if (exists > 0) {
            return null;
        }
        try {
            Db.update("insert into pm_notice_read(notice_id,user_id,read_time,device)"
                    + " values(" + noticeId + "," + Db.esc(uid) + ",now(),'MP')");
        } catch (RuntimeException e) {
            if (String.valueOf(e.getMessage()).contains("uk_notice_user")) {
                return null;
            }
            throw e;
        }
        return null;
    }

    // ---------------- 工作台概览 ----------------

    /** PM-123 概览：我的项目数、待办任务、待我审批、未缴罚款、未签收公告。 */
    public static Map<String, Object> overview(String uid, boolean all) {
        Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        m.put("projectCount", countProjects(uid, all, "", ""));
        m.put("taskTodo", countTasks(uid, all, "", true, "TODO", "")
                + countTasks(uid, all, "", true, "DOING", ""));
        m.put("approvalPending", ApprovalDao.countPending(uid, all));
        m.put("fineUnpaid", FineDao.count(uid, all, "UNPAID"));
        m.put("fineAmount", FineDao.sumUnpaid(uid, all));
        m.put("noticeUnsigned", unsignedNotices(uid, all));
        return m;
    }

    /** 业务表取单条时统一校验归属：小程序里所有详情接口都走这里。 */
    public static boolean ownedBy(String table, String id, String uid) {
        if (!Db.isId(id)) {
            return false;
        }
        return Db.queryLong("select count(*) from " + table + " where id=" + id
                + " and user_id=" + Db.esc(uid)) > 0;
    }

    static String quote(String s) {
        return lit(s);
    }
}
