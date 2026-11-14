package archive.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 借阅：申请、审批、归还。状态 PENDING → APPROVED / REJECTED → RETURNED。 */
public class BorrowDao {

    private static final String BASE_SELECT =
            "select b.id, b.archive_id, b.user_id, b.user_name, b.reason, b.start_date, b.end_date,"
                    + " b.status, b.approver, b.approve_time, b.return_time, b.create_time,"
                    + " a.title, a.archive_no,"
                    + " case when b.status='APPROVED' and b.end_date < curdate() then 1 else 0 end as overdue"
                    + " from da_borrow b left join da_archive a on a.id = b.archive_id";

    /** 提交借阅申请。返回错误提示，成功返回 null。 */
    public static String apply(String archiveId, String userId, String userName,
                               String reason, String days) {
        if (!Db.isId(archiveId)) {
            return "档案编号不合法";
        }
        Map<String, String> a = ArchiveDao.get(archiveId);
        if (a == null) {
            return "档案不存在";
        }
        if (!"ARCHIVED".equals(a.get("status"))) {
            return "只有“已归档”的档案可以借阅，当前状态：" + a.get("status");
        }
        long dup = Db.queryLong("select count(*) from da_borrow where archive_id=" + archiveId
                + " and user_id='" + Db.esc(userId) + "' and status in ('PENDING','APPROVED')");
        if (dup > 0) {
            return "你已提交过该档案的借阅申请，请勿重复提交";
        }
        int d = 30;
        try {
            d = Integer.parseInt(days);
        } catch (Exception ignore) {
        }
        if (d <= 0 || d > 30) {
            return "借阅期限需在 1～30 天之间";
        }
        String start = LocalDate.now().toString();
        String end = LocalDate.now().plusDays(d).toString();
        Db.update("insert into da_borrow(archive_id,user_id,user_name,reason,start_date,end_date,"
                + "status,create_time) values(" + archiveId + ",'" + Db.esc(userId) + "','"
                + Db.esc(userName) + "','" + Db.esc(reason) + "','" + start + "','" + end
                + "','PENDING','" + Db.now() + "')");
        return null;
    }

    public static List<Map<String, String>> mine(String userId) {
        return Db.query(BASE_SELECT + " where b.user_id='" + Db.esc(userId)
                + "' order by b.id desc limit 100");
    }

    public static List<Map<String, String>> pending() {
        return Db.query(BASE_SELECT + " where b.status='PENDING' order by b.id");
    }

    public static List<Map<String, String>> all() {
        return Db.query(BASE_SELECT + " order by b.id desc limit 200");
    }

    /** 审批：pass=true 通过，同时把档案置为借出中；false 驳回。 */
    public static String approve(String id, String approver, boolean pass) {
        if (!Db.isId(id)) {
            return "借阅编号不合法";
        }
        Map<String, String> b = Db.queryOne("select * from da_borrow where id=" + id);
        if (b == null) {
            return "借阅记录不存在";
        }
        if (!"PENDING".equals(b.get("status"))) {
            return "该申请已处理，不能重复审批";
        }
        Db.update("update da_borrow set status='" + (pass ? "APPROVED" : "REJECTED")
                + "', approver='" + Db.esc(approver) + "', approve_time='" + Db.now()
                + "' where id=" + id);
        if (pass) {
            Db.update("update da_archive set status='BORROWED' where id=" + b.get("archive_id"));
        }
        return null;
    }

    /** 归还登记：借阅记录改 RETURNED，档案回到 ARCHIVED。 */
    public static String giveBack(String id) {
        if (!Db.isId(id)) {
            return "借阅编号不合法";
        }
        Map<String, String> b = Db.queryOne("select * from da_borrow where id=" + id);
        if (b == null) {
            return "借阅记录不存在";
        }
        if (!"APPROVED".equals(b.get("status"))) {
            return "只有审批通过的借阅才能登记归还";
        }
        Db.update("update da_borrow set status='RETURNED', return_time='" + Db.now()
                + "' where id=" + id);
        Db.update("update da_archive set status='ARCHIVED' where id=" + b.get("archive_id"));
        return null;
    }

    public static long countPending() {
        return Db.queryLong("select count(*) from da_borrow where status='PENDING'");
    }

    public static long countMine(String userId) {
        return Db.queryLong("select count(*) from da_borrow where user_id='" + Db.esc(userId) + "'");
    }
}
