package pm.dao;

import java.util.List;
import java.util.Map;

// 审批：延期申请、审批中心。本期先覆盖任务延期
public class ApprovalDao {

    private static final String BASE_SELECT =
            "select a.id, a.biz_type, a.biz_id, a.title, a.applicant_id, a.applicant,"
                    + " a.approver_id, a.status, a.detail, a.old_date, a.new_date, a.opinion,"
                    + " a.create_time, a.approve_time,"
                    + " ifnull(t.title,'') as task_title, ifnull(p.name,'') as project_name,"
                    + " ifnull(u.user_name,'') as approver_name"
                    + " from pm_approval a left join pm_task t on t.id=a.biz_id"
                    + " left join pm_project p on p.id=t.project_id"
                    + " left join pm_user u on u.id=a.approver_id";

    // 拼接 SQL 前的转义：先转义反斜杠，再处理单引号。
    // Db.esc 只处理单引号，MySQL 默认又开启反斜杠转义，只靠它挡不住 '\' 注入。
    private static String sql(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replace("\\", "\\\\").replace("'", "''");
    }

    private static boolean isDate(String s) {
        return s != null && s.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    public static List<Map<String, String>> pending(String uid, boolean all) {
        return pending(uid, all, 0, 100);
    }

    // 待我审批（分页）。
    public static List<Map<String, String>> pending(String uid, boolean all, int offset, int size) {
        String where = " where a.status='PENDING'";
        if (!all) {
            where += " and a.approver_id=" + Db.esc(uid);
        }
        return Db.query(BASE_SELECT + where + " order by a.id desc" + limit(offset, size));
    }

    public static List<Map<String, String>> mine(String uid) {
        return mine(uid, 0, 100);
    }

    // 我发起的申请（分页）。
    public static List<Map<String, String>> mine(String uid, int offset, int size) {
        return Db.query(BASE_SELECT + " where a.applicant_id=" + Db.esc(uid)
                + " order by a.id desc" + limit(offset, size));
    }

    // 已办：我是审批人且已有结论的单子（PM-081 要求的三栏之一，原先缺失）。
    // 管理员看全部已办，其他人只看自己审批过的。
    public static List<Map<String, String>> done(String uid, boolean all, int offset, int size) {
        String where = " where a.status in ('APPROVED','REJECTED')";
        if (!all) {
            where += " and a.approver_id=" + Db.esc(uid);
        }
        return Db.query(BASE_SELECT + where + " order by a.approve_time desc, a.id desc"
                + limit(offset, size));
    }

    public static long countDone(String uid, boolean all) {
        String where = all ? "" : " and approver_id=" + Db.esc(uid);
        return Db.queryLong("select count(*) from pm_approval"
                + " where status in ('APPROVED','REJECTED')" + where);
    }

    public static long countMine(String uid) {
        return Db.queryLong("select count(*) from pm_approval where applicant_id=" + Db.esc(uid));
    }

    private static String limit(int offset, int size) {
        int s = size <= 0 ? 20 : Math.min(size, 100);
        return " limit " + Math.max(0, offset) + "," + s;
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

    // 提交任务延期申请；返回错误提示，成功返回 null
    public static String applyDelay(String taskId, String newDate, String reason,
                                    String applicantId, String applicantName) {
        Map<String, String> t = TaskDao.get(taskId);
        if (t == null) {
            return "任务不存在";
        }
        String taskStatus = t.get("status");
        if ("DONE".equals(taskStatus) || "CANCELED".equals(taskStatus)) {
            return "任务已完成或已取消，无需申请延期";
        }
        if (!isDate(newDate)) {
            return "请选择新的截止日期";
        }
        if (reason == null || reason.trim().isEmpty()) {
            return "请填写延期原因";
        }

        // 新截止日必须晚于原截止日，且不能早于今天
        String oldDate = t.get("due_date");
        if (isDate(oldDate)) {
            if (newDate.compareTo(oldDate) <= 0) {
                return "新的截止日期必须晚于原截止日（" + oldDate + "）";
            }
        }
        if (newDate.compareTo(Db.today()) < 0) {
            return "新的截止日期不能早于今天（" + Db.today() + "）";
        }

        Map<String, String> p = ProjectDao.get(t.get("project_id"));
        if (p == null) {
            return "任务所属项目不存在";
        }
        if ("ARCHIVED".equals(p.get("status"))) {
            return "项目已归档，不能再提交延期申请";
        }

        // 审批人默认为项目负责人；负责人本人申请时置空，转由管理员处理，避免自己审自己
        String owner = p.get("owner_id");
        String approver = "NULL";
        if (Db.isId(owner) && !owner.equals(applicantId)) {
            approver = owner;
        }

        long dup = Db.queryLong("select count(*) from pm_approval where biz_type='TASK_DELAY'"
                + " and biz_id=" + taskId + " and status='PENDING'");
        if (dup > 0) {
            return "该任务已有待审批的延期申请";
        }

        String oldDateSql = isDate(oldDate) ? "'" + oldDate + "'" : "NULL";
        Db.update("insert into pm_approval(biz_type,biz_id,title,applicant_id,applicant,approver_id,"
                + "status,detail,old_date,new_date,create_time) values('TASK_DELAY'," + taskId
                + ",'任务延期申请：" + sql(t.get("title")) + "'," + Db.esc(applicantId)
                + ",'" + sql(applicantName) + "'," + approver + ",'PENDING','" + sql(reason)
                + "'," + oldDateSql + ",'" + newDate + "','" + Db.now() + "')");
        return null;
    }

    // 审批：pass=true 通过；通过后把任务截止日改为申请日期
    // 这里没有事务（Db 未提供），所以把「改任务截止日」放在「改审批状态」之前：
    // 前者幂等，后者带 status='PENDING' 条件。任一步失败重试都不会留下
    // 「审批已通过但截止日没改」的脏数据
    public static String decide(String id, boolean pass, String opinion, String uid, boolean admin) {
        Map<String, String> a = get(id);
        if (a == null) {
            return "审批记录不存在";
        }
        if (!"PENDING".equals(a.get("status"))) {
            return "该申请已处理";
        }
        String approverId = a.get("approver_id");
        if (!admin) {
            if (!Db.isId(approverId)) {
                return "该申请未指定审批人（申请人即项目负责人），仅管理员可处理";
            }
            if (!uid.equals(approverId)) {
                return "你不是该申请的审批人";
            }
            if (uid.equals(a.get("applicant_id"))) {
                return "不能审批自己提交的申请";
            }
        }
        if (!pass && (opinion == null || opinion.trim().isEmpty())) {
            return "驳回必须填写意见";
        }

        String newDate = a.get("new_date");
        if (pass && "TASK_DELAY".equals(a.get("biz_type"))) {
            if (!isDate(newDate)) {
                return "该申请缺少有效的新截止日，无法通过";
            }
            if (newDate.compareTo(Db.today()) < 0) {
                return "新截止日（" + newDate + "）已早于今天，请驳回并让申请人重新提交";
            }
            Db.update("update pm_task set due_date='" + newDate + "' where id=" + a.get("biz_id"));
        }

        int rows = Db.update("update pm_approval set status='" + (pass ? "APPROVED" : "REJECTED")
                + "', opinion='" + sql(opinion) + "', approve_time='" + Db.now()
                + "' where id=" + id + " and status='PENDING'");
        if (rows == 0) {
            return "该申请已被处理，请刷新后查看";
        }
        return null;
    }
}
