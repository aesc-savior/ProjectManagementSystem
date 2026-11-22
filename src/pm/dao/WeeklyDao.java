package pm.dao;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// 周报：填写、提交、撤回、审核、查询、导出与按周统计
public class WeeklyDao {

    private static final String BASE_SELECT =
            "select w.id, w.project_id, w.user_id, w.week_no, w.done_text, w.plan_text, w.issue_text,"
                    + " w.hours, w.status, w.review, w.score, w.submit_time, w.create_time,"
                    + " w.reviewer, w.review_time, w.attach_name, w.attach_path, w.attach_size, w.withdraw_time,"
                    + " p.project_no, p.name as project_name, u.user_name"
                    + " from pm_weekly w join pm_project p on p.id=w.project_id"
                    + " join pm_user u on u.id=w.user_id";

    public static String currentWeek() {
        LocalDate d = LocalDate.now();
        int week = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = d.get(IsoFields.WEEK_BASED_YEAR);
        return year + "-W" + String.format("%02d", week);
    }

    // 2026-W05 → 202605，用于规范化文件名
    public static String weekCompact(String weekNo) {
        if (weekNo == null || !weekNo.matches("\\d{4}-W\\d{2}")) {
            return "";
        }
        return weekNo.substring(0, 4) + weekNo.substring(6);
    }

    // 周报文件名规范化（PM-031）：项目编号-YYYYWW-姓名-周报
    // seq>1 时追加序号，用于批量导出时避免同名覆盖
    public static String fileName(String projectNo, String weekNo, String userName, int seq, String ext) {
        String base = (projectNo == null || projectNo.isEmpty() ? "项目" : projectNo)
                + "-" + weekCompact(weekNo)
                + "-" + (userName == null || userName.isEmpty() ? "成员" : userName)
                + "-周报";
        if (seq > 1) {
            base = base + "(" + seq + ")";
        }
        return base + (ext == null || ext.isEmpty() ? "" : "." + ext);
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

    // 排序白名单，避免把前端参数拼进 order by
    private static String orderBy(String sort, String order) {
        String col;
        if ("status".equals(sort)) {
            col = "w.status";
        } else if ("project".equals(sort)) {
            col = "p.name";
        } else if ("member".equals(sort)) {
            col = "u.user_name";
        } else if ("hours".equals(sort)) {
            col = "w.hours";
        } else if ("submit".equals(sort)) {
            col = "w.submit_time";
        } else {
            col = "w.week_no";
        }
        String dir = "asc".equalsIgnoreCase(order) ? "asc" : "desc";
        return " order by " + col + " " + dir + ", w.id desc";
    }

    public static List<Map<String, String>> list(String projectId, String userId, String weekNo,
                                                 String status, int page, int size,
                                                 String uid, boolean all) {
        return list(projectId, userId, weekNo, status, page, size, uid, all, "", "");
    }

    public static List<Map<String, String>> list(String projectId, String userId, String weekNo,
                                                 String status, int page, int size,
                                                 String uid, boolean all, String sort, String order) {
        int offset = Math.max(0, (page - 1) * size);
        return Db.query(BASE_SELECT + buildWhere(projectId, userId, weekNo, status, uid, all)
                + orderBy(sort, order) + " limit " + offset + "," + size);
    }

    // 导出用：不分页
    public static List<Map<String, String>> listAll(String projectId, String userId, String weekNo,
                                                    String status, String uid, boolean all,
                                                    String sort, String order) {
        return Db.query(BASE_SELECT + buildWhere(projectId, userId, weekNo, status, uid, all)
                + orderBy(sort, order) + " limit 2000");
    }

    public static long count(String projectId, String userId, String weekNo, String status,
                             String uid, boolean all) {
        return Db.queryLong("select count(*) from pm_weekly w join pm_project p on p.id=w.project_id"
                + buildWhere(projectId, userId, weekNo, status, uid, all));
    }

    // 当前查询条件下本人工时合计（PM-030 工时自动汇总）
    public static String sumHours(String projectId, String userId, String weekNo, String status,
                                  String uid, boolean all) {
        Map<String, String> row = Db.queryOne("select ifnull(sum(w.hours),0) as h from pm_weekly w"
                + " join pm_project p on p.id=w.project_id"
                + buildWhere(projectId, userId, weekNo, status, uid, all));
        return row == null ? "0" : row.get("h");
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

    // 新增或修改；返回周报 id。仅草稿/退回态允许改写（PM-032）
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
                    + issue + "', hours=" + hours + " where id=" + id
                    + " and status in ('DRAFT','REJECTED')");
            return id;
        }
        Db.update("insert into pm_weekly(project_id,user_id,week_no,done_text,plan_text,issue_text,hours,"
                + "status,create_time) values(" + projectId + "," + userId + ",'" + weekNo + "','"
                + done + "','" + plan + "','" + issue + "'," + hours + ",'DRAFT','" + Db.now() + "')");
        Map<String, String> row = Db.queryOne("select id from pm_weekly where project_id=" + projectId
                + " and user_id=" + userId + " and week_no='" + weekNo + "'");
        return row == null ? "" : row.get("id");
    }

    public static void updateAttach(String id, String name, String path, long size) {
        if (Db.isId(id)) {
            Db.update("update pm_weekly set attach_name='" + Db.esc(name) + "', attach_path='"
                    + Db.esc(path) + "', attach_size=" + size + " where id=" + id);
        }
    }

    public static void clearAttach(String id) {
        if (Db.isId(id)) {
            Db.update("update pm_weekly set attach_name=null, attach_path=null, attach_size=0 where id=" + id);
        }
    }

    public static void submit(String id) {
        if (Db.isId(id)) {
            Db.update("update pm_weekly set status='SUBMITTED', submit_time='" + Db.now()
                    + "' where id=" + id + " and status in ('DRAFT','REJECTED')");
        }
    }

    // 撤回：已提交未审核可撤回为草稿（PM-032）
    public static boolean withdraw(String id) {
        if (!Db.isId(id)) {
            return false;
        }
        int n = Db.update("update pm_weekly set status='DRAFT', withdraw_time='" + Db.now()
                + "' where id=" + id + " and status='SUBMITTED'");
        return n > 0;
    }

    public static void review(String id, boolean pass, String review, int score) {
        review(id, pass, review, score, null);
    }

    public static void review(String id, boolean pass, String review, int score, String reviewer) {
        if (Db.isId(id)) {
            Db.update("update pm_weekly set status='" + (pass ? "REVIEWED" : "REJECTED")
                    + "', review='" + Db.esc(review) + "', score=" + (score > 0 ? score : 0)
                    + ", reviewer='" + Db.esc(reviewer) + "', review_time='" + Db.now()
                    + "' where id=" + id);
        }
    }

    // 某项目某周的应交/已交人数（用于提交率）
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

    // 未交名单与提交统计（PM-034 / PM-037）
    private static final String MEMBER_UNION =
            "select m.project_id as project_id, m.user_id as user_id from pm_project_member m"
                    + " union select p2.id as project_id, p2.owner_id as user_id from pm_project p2"
                    + " where p2.owner_id>0";

    // 应交未交名单：项目成员（含负责人、排除查看者）中，该周未提交/未审核的人
    // projectId 为空时按当前用户可见范围统计全部项目
    public static List<Map<String, String>> missing(String projectId, String weekNo,
                                                    String uid, boolean all) {
        List<String> w = new ArrayList<String>();
        w.add("p.del_flag=0");
        w.add("p.status in ('RUNNING','PAUSED','PENDING')");
        w.add("u.status='NORMAL'");
        w.add("u.del_flag=0");
        w.add("u.role_code<>'VIEWER'");
        if (Db.isId(projectId)) {
            w.add("p.id=" + projectId);
        }
        w.add("not exists(select 1 from pm_weekly w2 where w2.project_id=p.id"
                + " and w2.user_id=u.id and w2.week_no='" + Db.esc(weekNo) + "'"
                + " and w2.status in ('SUBMITTED','REVIEWED'))");
        return Db.query("select u.id as user_id, u.user_name, p.id as project_id, p.project_no,"
                + " p.name as project_name, p.owner_id"
                + " from (" + MEMBER_UNION + ") x"
                + " join pm_user u on u.id=x.user_id"
                + " join pm_project p on p.id=x.project_id"
                + " where " + String.join(" and ", w) + ProjectDao.scope(uid, all)
                + " order by p.id, u.id");
    }

    // 按项目汇总某周的应交/已交/未交数量
    public static List<Map<String, String>> statByProject(String weekNo, String uid, boolean all) {
        String wk = Db.esc(weekNo);
        return Db.query("select p.id as project_id, p.project_no, p.name as project_name,"
                + " (select count(distinct x.user_id) from (" + MEMBER_UNION + ") x"
                + "   join pm_user u2 on u2.id=x.user_id"
                + "   where x.project_id=p.id and u2.status='NORMAL' and u2.del_flag=0 and u2.role_code<>'VIEWER') as due_cnt,"
                + " (select count(distinct w2.user_id) from pm_weekly w2 where w2.project_id=p.id"
                + "   and w2.week_no='" + wk + "' and w2.status in ('SUBMITTED','REVIEWED')) as done_cnt"
                + " from pm_project p where p.del_flag=0 and p.status in ('RUNNING','PAUSED','PENDING')"
                + ProjectDao.scope(uid, all) + " order by p.id");
    }

    // 近 N 周提交趋势（PM-037 出图用）
    public static List<Map<String, String>> trend(int weeks, String uid, boolean all) {
        List<Map<String, String>> rows = Db.query("select w.week_no,"
                + " sum(case when w.status in ('SUBMITTED','REVIEWED') then 1 else 0 end) as done_cnt,"
                + " count(*) as total_cnt from pm_weekly w join pm_project p on p.id=w.project_id"
                + buildWhere("", "", "", "", uid, all)
                + " group by w.week_no order by w.week_no desc limit " + Math.max(1, weeks));
        return rows;
    }
}
