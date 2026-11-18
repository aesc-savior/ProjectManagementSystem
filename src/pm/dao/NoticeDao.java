package pm.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 公告与签收。 */
public class NoticeDao {

    private static final String BASE_SELECT =
            "select n.id, n.title, n.content, n.scope, n.project_id, n.expire_time,"
                    + " n.publisher_id, n.publisher, n.create_time,"
                    + " (select count(*) from pm_notice_read r where r.notice_id=n.id) as read_count,"
                    + " (select count(*) from pm_notice_read r where r.notice_id=n.id and r.user_id=%UID%) as signed,"
                    + " ifnull(p.name,'') as project_name"
                    + " from pm_notice n left join pm_project p on p.id=n.project_id";

    public static List<Map<String, String>> list(String keyword, String uid, boolean all) {
        List<String> w = new ArrayList<String>();
        w.add("n.del_flag=0");
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = Db.esc(keyword);
            w.add("(n.title like '%" + k + "%' or n.content like '%" + k + "%')");
        }
        if (!all) {
            String scopeSql = "select p.id from pm_project p where p.del_flag=0"
                    + ProjectDao.scope(uid, false);
            w.add("(n.scope='ALL' or n.project_id in (" + scopeSql + "))");
        }
        String sql = BASE_SELECT.replace("%UID%", Db.esc(uid))
                + " where " + String.join(" and ", w) + " order by n.id desc limit 100";
        return Db.query(sql);
    }

    public static Map<String, String> get(String id, String uid) {
        if (!Db.isId(id)) {
            return null;
        }
        String sql = BASE_SELECT.replace("%UID%", Db.esc(uid)) + " where n.id=" + id;
        return Db.queryOne(sql);
    }

    public static void add(String title, String content, String scope, String projectId,
                           String expireTime, String publisherId, String publisher) {
        Db.update("insert into pm_notice(title,content,scope,project_id,expire_time,publisher_id,"
                + "publisher,create_time,del_flag) values('" + Db.esc(title) + "','" + Db.esc(content)
                + "','" + Db.esc(scope) + "'," + (Db.isId(projectId) ? projectId : "0") + ","
                + (expireTime != null && expireTime.matches("\\d{4}-\\d{2}-\\d{2}( \\d{2}:\\d{2}:\\d{2})?")
                ? "'" + expireTime + "'" : "null") + "," + Db.esc(publisherId) + ",'"
                + Db.esc(publisher) + "','" + Db.now() + "',0)");
    }

    public static void delete(String id) {
        if (Db.isId(id)) {
            Db.update("update pm_notice set del_flag=1 where id=" + id);
        }
    }

    public static void sign(String noticeId, String userId) {
        if (!Db.isId(noticeId) || !Db.isId(userId)) {
            return;
        }
        long exists = Db.queryLong("select count(*) from pm_notice_read where notice_id="
                + noticeId + " and user_id=" + userId);
        if (exists == 0) {
            Db.update("insert into pm_notice_read(notice_id,user_id,read_time,device) values("
                    + noticeId + "," + userId + ",'" + Db.now() + "','PC')");
        }
    }

    public static long unread(String uid, boolean all) {
        List<Map<String, String>> rows = list("", uid, all);
        long n = 0;
        for (Map<String, String> r : rows) {
            if ("0".equals(r.get("signed"))) {
                n++;
            }
        }
        return n;
    }
}
