package pm.dao;

import java.util.List;
import java.util.Map;

/** 延期罚款：按超期天数生成罚款单，支付走模拟回调。 */
public class FineDao {

    public static final int PER_DAY = 100;

    private static final String BASE_SELECT =
            "select f.id, f.project_id, f.user_id, f.user_name, f.amount, f.reason, f.status,"
                    + " f.trade_no, f.pay_time, f.create_time, p.name as project_name, p.project_no"
                    + " from pm_fine f join pm_project p on p.id=f.project_id";

    public static List<Map<String, String>> list(String uid, boolean all) {
        String where = " where 1=1";
        if (!all) {
            where += " and f.user_id=" + Db.esc(uid);
        }
        return Db.query(BASE_SELECT + where + " order by f.id desc limit 200");
    }

    public static Map<String, String> get(String id) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne(BASE_SELECT + " where f.id=" + id);
    }

    /** 对某项目生成罚款单；返回错误提示，成功返回 null。 */
    public static String generate(String projectId) {
        Map<String, String> p = ProjectDao.get(projectId);
        if (p == null) {
            return "项目不存在";
        }
        long days = Db.queryLong("select datediff(curdate(),'" + Db.esc(p.get("end_date")) + "')");
        if (days <= 0) {
            return "该项目尚未超期，无需罚款";
        }
        long exists = Db.queryLong("select count(*) from pm_fine where project_id=" + projectId
                + " and status='UNPAID'");
        if (exists > 0) {
            return "该项目已有未缴纳的罚款单，请先处理";
        }
        long amount = days * PER_DAY;
        String reason = "项目「" + p.get("name") + "」超期 " + days + " 天，按 " + PER_DAY + " 元/天计。";
        Db.update("insert into pm_fine(project_id,user_id,user_name,amount,reason,status,create_time)"
                + " values(" + projectId + "," + Db.esc(p.get("owner_id")) + ",'"
                + Db.esc(p.get("owner_name")) + "'," + amount + ",'" + Db.esc(reason)
                + "','UNPAID','" + Db.now() + "')");
        return null;
    }

    /** 模拟微信支付：把状态改为已缴纳。 */
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
        String tradeNo = "MOCK" + System.currentTimeMillis();
        Db.update("update pm_fine set status='PAID', trade_no='" + tradeNo + "', pay_time='"
                + Db.now() + "' where id=" + id);
        return null;
    }

    public static long sumUnpaid(String uid, boolean all) {
        String where = all ? "" : " and user_id=" + Db.esc(uid);
        Map<String, String> row = Db.queryOne(
                "select ifnull(sum(amount),0) as total from pm_fine where status='UNPAID'" + where);
        try {
            return Math.round(Double.parseDouble(row.get("total")));
        } catch (Exception e) {
            return 0;
        }
    }
}
