package pm.dao;

import java.util.List;
import java.util.Map;

/** 审批：延期申请、审批中心。本期先覆盖任务延期。 */
public class ApprovalDao {

    private static final String BASE_SELECT =
            "select a.id, a.biz_type, a.biz_id, a.title, a.applicant_id, a.applicant,"
                    + " a.approver_id, a.status, a.detail, a.new_date, a.opinion,"
                    + " a.create_time, a.approve_time,"
                    + " ifnull(t.title,'') as task_title, ifnull(p.name,'') as project_name,"
                    + " ifnull(u.user_name,'') as approver_name"
                    + " from pm_approval a left join pm_task t on t.id=a.biz_id"
                    + " left join pm_project p on p.id=t.project_id"
                    + " left join pm_user u on u.id=a.approver_id";

    public static List<Map<String, String>> pending(String uid, boolean all) {
        String where = " where a.status='PENDING'";
        if (!all) {
            where += " and a.approver_id=" + Db.esc(uid);
        }
        return Db.query(BASE_SELECT + where + " order by a.id desc limit 100");
    }

    public static List<Map<String, String>> mine(String uid) {
        return Db.query(BASE_SELECT + " where a.applicant_id=" + Db.esc(uid)
                + " order by a.id desc limit 100");
    }

    public static long countPending(String uid, boolean all) {
        String where = all ? "" : " and approver_id=" + Db.esc(uid);
        return Db.queryLong("select count(*) from pm_approval where status='PENDING'" + where);
    }

    public static Map<String, String> get(String id) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(BASE_SELECT + " where a.id=" + id);
    }

    /** 提交任务延期申请；返回错误提示，成功返回 null。 */
    public static String applyDelay(String taskId, String newDate, String reason,
                                    String applicantId, String applicantName) {
        Map<String, String> t = TaskDao.get(taskId);
        if (t == null) {
            return "任务不存在";
        }
        if (newDate == null || !newDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return "请选择新的截止日期";
        }
        if (reason == null || reason.trim().isEmpty()) {
            return "请填写延期原因";
        }
        Map<String, String> p = ProjectDao.get(t.get("project_id"));
        String approver = p == null ? "" : p.get("owner_id");
        if (!Db.isId(approver)) {
            return "项目未设置负责人，无法提交审批";
        }
        long dup = Db.queryLong("select count(*) from pm_approval where biz_type='TASK_DELAY'"
                + " and biz_id=" + taskId + " and status='PENDING'");
        if (dup > 0) {
            return "该任务已有待审批的延期申请";
        }
        Db.update("insert into pm_approval(biz_type,biz_id,title,applicant_id,applicant,approver_id,"
                + "status,detail,new_date,create_time) values('TASK_DELAY'," + taskId + ",'任务延期申请："
                + Db.esc(t.get("title")) + "'," + Db.esc(applicantId) + ",'" + Db.esc(applicantName)
                + "'," + approver + ",'PENDING','" + Db.esc(reason) + "','" + newDate + "','"
                + Db.now() + "')");
        return null;
    }

    /** 审批：pass=true 通过；通过后把任务截止日改为申请日期。 */
    public static String decide(String id, boolean pass, String opinion, String uid, boolean admin) {
        Map<String, String> a = get(id);
        if (a == null) {
            return "审批记录不存在";
        }
        if (!"PENDING".equals(a.get("status"))) {
            return "该申请已处理";
        }
        if (!admin && !uid.equals(a.get("approver_id"))) {
            return "你不是该申请的审批人";
        }
        if (!pass && (opinion == null || opinion.trim().isEmpty())) {
            return "驳回必须填写意见";
        }
        Db.update("update pm_approval set status='" + (pass ? "APPROVED" : "REJECTED")
                + "', opinion='" + Db.esc(opinion) + "', approve_time='" + Db.now()
                + "' where id=" + id);
        if (pass && "TASK_DELAY".equals(a.get("biz_type")) && a.get("new_date") != null
                && a.get("new_date").matches("\\d{4}-\\d{2}-\\d{2}")) {
            Db.update("update pm_task set due_date='" + a.get("new_date") + "' where id=" + a.get("biz_id"));
        }
        return null;
    }
}
