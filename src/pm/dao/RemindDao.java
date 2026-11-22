package pm.dao;

import java.util.List;
import java.util.Map;

// 站内提醒：周报催交（PM-038）
public class RemindDao {

    // 同一个人、同类型、同标题一天只写一条，免得定时任务刷屏；返回新 id，0 表示没写
    public static long add(String userId, String userName, String type,
                           String title, String content, String targetUrl) {
        if (!Db.isId(userId)) {
            return 0L;
        }
        long dup = Db.queryLong("select count(*) from pm_remind where user_id=" + userId
                + " and type='" + Db.esc(type) + "' and title='" + Db.esc(title) + "'"
                + " and date(create_time)=curdate()");
        if (dup > 0) {
            return 0L;
        }
        Db.update("insert into pm_remind(user_id,user_name,type,title,content,target_url,read_flag,create_time)"
                + " values(" + userId + ",'" + Db.esc(userName) + "','" + Db.esc(type) + "','"
                + Db.esc(title) + "','" + Db.esc(content) + "','" + Db.esc(targetUrl) + "',0,'"
                + Db.now() + "')");
        return Db.queryLong("select max(id) from pm_remind where user_id=" + userId);
    }

    // 回写微信通道的推送结果（PM-038：站内与微信各一条）
    public static void markWeChat(long remindId, String status, String detail) {
        if (remindId <= 0) {
            return;
        }
        Db.update("update pm_remind set wechat_status='" + Db.esc(status) + "', wechat_detail='"
                + Db.esc(detail) + "' where id=" + remindId);
    }

    public static List<Map<String, String>> list(String uid, String type) {
        String where = " where user_id=" + Db.esc(uid);
        if (type != null && type.matches("[A-Z]{3,10}")) {
            where += " and type='" + type + "'";
        }
        return Db.query("select id,user_id,user_name,type,title,content,target_url,read_flag,create_time,"
                + "wechat_status,wechat_detail from pm_remind" + where
                + " order by read_flag asc, id desc limit 100");
    }

    public static long unreadCount(String uid) {
        if (!Db.isId(uid)) {
            return 0;
        }
        return Db.queryLong("select count(*) from pm_remind where user_id=" + uid + " and read_flag=0");
    }

    public static void markRead(String id, String uid) {
        if (Db.isId(id)) {
            Db.update("update pm_remind set read_flag=1 where id=" + id + " and user_id=" + Db.esc(uid));
        }
    }

    public static void markAllRead(String uid) {
        if (Db.isId(uid)) {
            Db.update("update pm_remind set read_flag=1 where user_id=" + uid);
        }
    }

    // 取项目经理（ADMIN/PM）账号，用于把催交提醒同时推给他们
    public static List<Map<String, String>> managers() {
        return Db.query("select id, user_name from pm_user where del_flag=0 and status='NORMAL'"
                + " and role_code in ('ADMIN','PM') order by id");
    }
}
