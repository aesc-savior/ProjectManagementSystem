package pm.dao;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 周报：填写、提交、审核、按周统计。 */
public class WeeklyDao {

    private static final String BASE_SELECT =
            "select w.id, w.project_id, w.user_id, w.week_no, w.done_text, w.plan_text, w.issue_text,"
                    + " w.hours, w.status, w.review, w.score, w.submit_time, w.create_time,"
                    + " p.project_no, p.name as project_name, u.user_name"
                    + " from pm_weekly w join pm_project p on p.id=w.project_id"
                    + " join pm_user u on u.id=w.user_id";

    public static String currentWeek() {
        LocalDate d = LocalDate.now();
        int week = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = d.get(IsoFields.WEEK_BASED_YEAR);
        return year + "-W" + String.format("%02d", week);
    }

    public static String buildWhere(String projectId, String userId, String weekNo, String status,
                                    String uid, boolean all) {
        List<String> w = new ArrayList<String>();
        w.add("p.del_flag=0");
        if (Db.isId(projectId)) {
            w.add("w.project_id=" + projectId);
        }
        if (Db.isId(userId)) {
            w.add("w.user_id=" + userId);
        }
        if (weekNo != null && weekNo.matches("\\d{4}-W\\d{2}")) {
            w.add("w.week_no='" + weekNo + "'");
        }
        if (status != null && status.matches("[A-Z]{3,10}")) {
            w.add("w.status='" + status + "'");
        }
        return " where " + String.join(" and ", w) + ProjectDao.scope(uid, all);
    }

    public static List<Map<String, String>> list(String projectId, String userId, String weekNo,
                                                 String status, int page, int size,
                                                 String uid, boolean all) {
        int offset = Math.max(0, (page - 1) * size);
        return Db.query(BASE_SELECT + buildWhere(projectId, userId, weekNo, status, uid, all)
                + " order by w.week_no desc, w.id desc limit " + offset + "," + size);
    }

    public static long count(String projectId, String userId, String weekNo, String status,
                             String uid, boolean all) {
        return Db.queryLong("select count(*) from pm_weekly w join pm_project p on p.id=w.project_id"
                + buildWhere(projectId, userId, weekNo, status, uid, all));
    }

    public static Map<String, String> get(String id) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(BASE_SELECT + " where w.id=" + id);
    }

    public static boolean exists(String projectId, String userId, String weekNo, String excludeId) {
        String sql = "select count(*) from pm_weekly where project_id=" + Db.esc(projectId)
                + " and user_id=" + Db.esc(userId) + " and week_no='" + Db.esc(weekNo) + "'";
        if (Db.isId(excludeId)) {
            sql += " and id<>" + excludeId;
        }
        return Db.queryLong(sql) > 0;
    }

    /** 新增或修改；返回周报 id。 */
    public static String save(String id, Map<String, String> p) {
        String projectId = Db.isId(p.get("projectId")) ? p.get("projectId") : "0";
        String userId = Db.isId(p.get("userId")) ? p.get("userId") : "0";
        String weekNo = Db.esc(p.get("weekNo"));
        String done = Db.esc(p.get("done"));
        String plan = Db.esc(p.get("plan"));
        String issue = Db.esc(p.get("issue"));
        String hours = p.get("hours") == null || p.get("hours").trim().isEmpty() ? "0" : Db.esc(p.get("hours"));
        if (!hours.matches("\\d{1,6}(\\.\\d{1,2})?")) {
            hours = "0";
        }
        if (Db.isId(id)) {
            Db.update("update pm_weekly set done_text='" + done + "', plan_text='" + plan + "', issue_text='"
                    + issue + "', hours=" + hours + " where id=" + id);
            return id;
        }
        Db.update("insert into pm_weekly(project_id,user_id,week_no,done_text,plan_text,issue_text,hours,"
                + "status,create_time) values(" + projectId + "," + userId + ",'" + weekNo + "','"
                + done + "','" + plan + "','" + issue + "'," + hours + ",'DRAFT','" + Db.now() + "')");
        Map<String, String> row = Db.queryOne("select id from pm_weekly where project_id=" + projectId
                + " and user_id=" + userId + " and week_no='" + weekNo + "'");
        return row == null ? "" : row.get("id");
    }

    public static void submit(String id) {
        if (Db.isId(id)) {
            Db.update("update pm_weekly set status='SUBMITTED', submit_time='" + Db.now()
                    + "' where id=" + id + " and status in ('DRAFT','REJECTED')");
        }
    }

    public static void review(String id, boolean pass, String review, int score) {
        if (Db.isId(id)) {
            Db.update("update pm_weekly set status='" + (pass ? "REVIEWED" : "REJECTED")
                    + "', review='" + Db.esc(review) + "', score=" + (score > 0 ? score : 0)
                    + " where id=" + id);
        }
    }

    /** 某项目某周的应交/已交人数（用于提交率）。 */
    public static Map<String, String> submission(String projectId, String weekNo) {
        if (!Db.isId(projectId)) {
            return null;
        }
        return Db.queryOne("select"
                + " (select count(*) from pm_project_member where project_id=" + projectId + ") as members,"
                + " (select count(*) from pm_weekly where project_id=" + projectId + " and week_no='"
                + Db.esc(weekNo) + "' and status in ('SUBMITTED','REVIEWED')) as submitted");
    }

    public static List<Map<String, String>> countByStatus(String uid, boolean all) {
        return Db.query("select w.status, count(*) as cnt from pm_weekly w join pm_project p on p.id=w.project_id"
                + buildWhere("", "", "", "", uid, all) + " group by w.status order by cnt desc");
    }
}
