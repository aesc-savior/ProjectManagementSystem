package pm.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 延期罚款：按超期天数生成罚款单，支付走模拟回调
public class FineDao {

    public static final int PER_DAY = 100;

    private static final String BASE_SELECT =
            "select f.id, f.project_id, f.user_id, f.user_name, f.amount, f.reason, f.status,"
                    + " f.due_date, f.trade_no, f.pay_time, f.create_time, p.name as project_name,"
                    + " p.project_no"
                    + " from pm_fine f join pm_project p on p.id=f.project_id";

    private static String sql(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replace("\\", "\\\\").replace("'", "''");
    }

    private static boolean isDate(String s) {
        return s != null && s.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    // 列表：管理员看全部，其他人只看自己的罚款单。
    public static List<Map<String, String>> list(String uid, boolean all) {
        return list(uid, all, null, 0, 200);
    }

    // 按状态筛选 + 分页（小程序端用；status 传 null/空表示全部）。
    public static List<Map<String, String>> list(String uid, boolean all, String status,
                                                 int offset, int size) {
        return Db.query(BASE_SELECT + where(uid, all, status) + " order by f.id desc"
                + " limit " + Math.max(0, offset) + "," + (size <= 0 ? 20 : Math.min(size, 100)));
    }

    public static long count(String uid, boolean all, String status) {
        return Db.queryLong("select count(*) from pm_fine f" + where(uid, all, status));
    }

    private static String where(String uid, boolean all, String status) {
        String where = " where 1=1";
        if (!all) {
            where += " and f.user_id=" + Db.esc(uid);
        }
        if (status != null && status.matches("[A-Z]{3,10}")) {
            where += " and f.status='" + status + "'";
        }
        return where;
    }

    public static Map<String, String> get(String id) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(BASE_SELECT + " where f.id=" + id);
    }

    // 已有未缴罚款单的项目 id，用于在生成下拉里把它们排除掉
    public static Set<String> unpaidProjects() {
        Set<String> ids = new HashSet<String>();
        for (Map<String, String> r : Db.query(
                "select distinct project_id from pm_fine where status='UNPAID'")) {
            ids.add(r.get("project_id"));
        }
        return ids;
    }

    // 对某项目生成罚款单；返回错误提示，成功返回 null
    public static String generate(String projectId) {
        if (!Db.isId(projectId)) {
            return "项目不存在";
        }
        Map<String, String> p = ProjectDao.get(projectId);
        if (p == null) {
            return "项目不存在";
        }
        if (!isDate(p.get("end_date"))) {
            return "该项目未设置截止日期，无法计算超期天数";
        }
        if (!Db.isId(p.get("owner_id"))) {
            return "该项目未设置负责人（项目经理），无法确定罚款对象";
        }

        String endDate = p.get("end_date");
        long days = Db.queryLong("select datediff(curdate(),'" + endDate + "')");
        if (days <= 0) {
            return "该项目尚未超期，无需罚款";
        }

        // 同一项目存在未缴罚款单时，不允许再开新单
        long exists = Db.queryLong("select count(*) from pm_fine where project_id=" + projectId
                + " and status='UNPAID'");
        if (exists > 0) {
            return "该项目已有未缴纳的罚款单，请先处理";
        }

        // 同一项目、同一期（同一截止日）只允许一张罚款单：缴完也不能对同一期重复开单
        long sameTerm = Db.queryLong("select count(*) from pm_fine where project_id=" + projectId
                + " and due_date='" + endDate + "'");
        if (sameTerm > 0) {
            return "该项目本期（截止 " + endDate + "）的罚款单已生成过，不能重复开单";
        }

        long amount = days * PER_DAY;
        String reason = "项目「" + p.get("name") + "」超期 " + days + " 天，按 " + PER_DAY + " 元/天计。";
        try {
            Db.update("insert into pm_fine(project_id,user_id,user_name,amount,reason,status,"
                    + "due_date,create_time) values(" + projectId + "," + p.get("owner_id") + ",'"
                    + sql(p.get("owner_name")) + "'," + amount + ",'" + sql(reason)
                    + "','UNPAID','" + endDate + "','" + Db.now() + "')");
        } catch (RuntimeException e) {
            // 并发下可能撞唯一键 uk_project_term，当作业务提示返回，不抛 500
            String msg = String.valueOf(e.getMessage());
            if (msg.contains("uk_project_term") || msg.contains("Duplicate")) {
                return "该项目本期罚款单已生成，请勿重复提交";
            }
            throw e;
        }
        return null;
    }

    // 模拟微信支付：把状态改为已缴纳
    public static String pay(String id, String uid, boolean admin) {
        Map<String, String> f = get(id);
        if (f == null) {
            return "罚款单不存在";
        }
        if (!admin && !uid.equals(f.get("user_id"))) {
            return "只有被罚人本人可以缴纳";
        }
        if ("PAID".equals(f.get("status"))) {
            return "该罚款单已缴纳";
        }
        // 带状态条件的更新：双击/并发时只有一次能改成功
        int rows = Db.update("update pm_fine set status='PAID', trade_no='MOCK"
                + System.currentTimeMillis() + "', pay_time='" + Db.now()
                + "' where id=" + id + " and status='UNPAID'");
        if (rows == 0) {
            return "该罚款单已缴纳，请刷新后查看";
        }
        return null;
    }

    // 未缴罚款合计（元）。金额用 BigDecimal 汇总，避免 double 丢精度。
    public static long sumUnpaid(String uid, boolean all) {
        String where = all ? "" : " and user_id=" + Db.esc(uid);
        Map<String, String> row = Db.queryOne(
                "select ifnull(sum(amount),0) as total from pm_fine where status='UNPAID'" + where);
        if (row == null || row.get("total") == null || row.get("total").isEmpty()) {
            return 0L;
        }
        try {
            return new BigDecimal(row.get("total"))
                    .setScale(0, RoundingMode.HALF_UP).longValue();
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
