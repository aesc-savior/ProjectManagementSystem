package pm.dao;

import java.util.List;
import java.util.Map;

// 操作日志：登录、增删改、提交、审批、上传下载、支付等关键动作留痕
public class LogDao {

    // 写日志失败不影响主流程，只打印到控制台
    public static void write(String userId, String userName, String action,
                             String target, String detail, String ip) {
        try {
            Db.update("insert into pm_log(user_id,user_name,action,target,detail,ip,create_time)"
                    + " values('" + Db.esc(userId) + "','" + Db.esc(userName) + "','"
                    + Db.esc(action) + "','" + Db.esc(target) + "','" + Db.esc(detail) + "','"
                    + Db.esc(ip) + "','" + Db.now() + "')");
        } catch (Exception e) {
            System.out.println("[LogDao] 写日志失败：" + e.getMessage());
        }
    }

    public static List<Map<String, String>> list(String action, String keyword, int page, int size) {
        String where = buildWhere(action, keyword);
        int offset = Math.max(0, (page - 1) * size);
        return Db.query("select id,user_id,user_name,action,target,detail,ip,create_time from pm_log"
                + where + " order by id desc limit " + offset + "," + size);
    }

    public static long count(String action, String keyword) {
        return Db.queryLong("select count(*) from pm_log" + buildWhere(action, keyword));
    }

    private static String buildWhere(String action, String keyword) {
        StringBuilder w = new StringBuilder(" where 1=1");
        if (action != null && action.matches("[A-Z_]{3,20}")) {
            w.append(" and action='").append(action).append("'");
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = Db.esc(keyword);
            w.append(" and (user_name like '%").append(k).append("%' or target like '%")
                    .append(k).append("%' or detail like '%").append(k).append("%')");
        }
        return w.toString();
    }
}
