package pm.mp;

import pm.dao.Db;

import java.security.SecureRandom;
import java.util.Map;

/** 小程序端数据访问：openid 绑定 + 登录态。只读写 pm_mp_user / pm_mp_token。 */
public final class MpDao {

    /** 登录态有效期（天）。 */
    public static final int TOKEN_DAYS = 7;

    private static final SecureRandom RANDOM = new SecureRandom();

    private MpDao() {
    }

    private static String sql(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replace("\\", "\\\\").replace("'", "''");
    }

    /** 字符串字面量：空串按 NULL 存，避免把空昵称写成 ''。 */
    private static String lit(String s) {
        return (s == null || s.trim().isEmpty()) ? "NULL" : "'" + sql(s) + "'";
    }

    private static String hex(byte[] bytes) {
        char[] hex = "0123456789abcdef".toCharArray();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(hex[(b >> 4) & 0x0f]).append(hex[b & 0x0f]);
        }
        return sb.toString();
    }

    // ---------------- 绑定 ----------------

    public static Map<String, String> bindingByOpenid(String openid) {
        if (openid == null || openid.trim().isEmpty()) {
            return null;
        }
        return Db.queryOne("select id, user_id, openid, nick_name, avatar_url, bind_time"
                + " from pm_mp_user where openid='" + sql(openid) + "'");
    }

    public static Map<String, String> bindingByUser(String userId) {
        if (!Db.isId(userId)) {
            return null;
        }
        return Db.queryOne("select id, user_id, openid, nick_name, avatar_url, bind_time"
                + " from pm_mp_user where user_id=" + userId);
    }

    /** 建立绑定；返回错误提示，成功返回 null。重复绑定同一对微信-账号视为成功（幂等）。 */
    public static String bind(String userId, String openid, String nickName, String avatarUrl) {
        if (!Db.isId(userId) || openid == null || openid.trim().isEmpty()) {
            return "绑定参数不完整";
        }
        Map<String, String> byOpenid = bindingByOpenid(openid);
        if (byOpenid != null && !userId.equals(byOpenid.get("user_id"))) {
            return "该微信已绑定其他账号，请先在原账号下解绑";
        }
        Map<String, String> byUser = bindingByUser(userId);
        if (byUser != null) {
            if (openid.equals(byUser.get("openid"))) {
                return null;
            }
            return "该账号已绑定其他微信，请先解绑后再换绑";
        }
        try {
            Db.update("insert into pm_mp_user(user_id,openid,nick_name,avatar_url,bind_time)"
                    + " values(" + userId + ",'" + sql(openid) + "'," + lit(nickName) + ","
                    + lit(avatarUrl) + ",now())");
        } catch (RuntimeException e) {
            String msg = String.valueOf(e.getMessage());
            if (msg.contains("uk_openid") || msg.contains("uk_user") || msg.contains("Duplicate")) {
                return "该微信或账号已被绑定，请刷新后重试";
            }
            throw e;
        }
        return null;
    }

    /** 解绑：同时吊销该账号的全部登录态。 */
    public static void unbind(String userId) {
        if (!Db.isId(userId)) {
            return;
        }
        Db.update("delete from pm_mp_token where user_id=" + userId);
        Db.update("delete from pm_mp_user where user_id=" + userId);
    }

    /** 取一个可登录的账号（未删除、未停用）；停用后小程序端应立刻失去访问权。 */
    public static Map<String, String> activeUser(String userId) {
        if (!Db.isId(userId)) {
            return null;
        }
        return Db.queryOne("select id, login_id, user_name, role_code, dept_name, phone, email"
                + " from pm_user where id=" + userId + " and del_flag=0 and status='NORMAL'");
    }

    /** 每次登录顺手更新微信昵称/头像，失败不影响登录。 */
    public static void updateProfile(String openid, String nickName, String avatarUrl) {
        if (openid == null || openid.trim().isEmpty()) {
            return;
        }
        StringBuilder set = new StringBuilder();
        if (nickName != null && !nickName.trim().isEmpty()) {
            set.append("nick_name='").append(sql(nickName)).append("'");
        }
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            if (set.length() > 0) {
                set.append(',');
            }
            set.append("avatar_url='").append(sql(avatarUrl)).append("'");
        }
        if (set.length() == 0) {
            return;
        }
        Db.update("update pm_mp_user set " + set + " where openid='" + sql(openid) + "'");
    }

    // ---------------- 登录态 ----------------

    public static String newToken(String userId, String openid) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = hex(bytes);
        Db.update("insert into pm_mp_token(token,user_id,openid,create_time,expire_time,last_active)"
                + " values('" + token + "'," + userId + ",'" + sql(openid) + "',now(),"
                + "date_add(now(), interval " + TOKEN_DAYS + " day),now())");
        return token;
    }

    /** token 对应的账号信息；过期、账号停用或已删除都返回 null。 */
    public static Map<String, String> tokenUser(String token) {
        if (token == null || !token.matches("[0-9a-f]{64}")) {
            return null;
        }
        return Db.queryOne("select t.openid, u.id as user_id, u.login_id, u.user_name,"
                + " u.role_code, u.dept_name, u.phone, u.email"
                + " from pm_mp_token t join pm_user u on u.id=t.user_id"
                + " where t.token='" + token + "' and t.expire_time > now()"
                + " and u.del_flag=0 and u.status='NORMAL'");
    }

    /** 过期时间（给前端展示），用数据库时间算，避免 JVM 时区差异。 */
    public static String expireAt(String token) {
        if (token == null || !token.matches("[0-9a-f]{64}")) {
            return "";
        }
        Map<String, String> row = Db.queryOne("select date_format(expire_time,'%Y-%m-%d %H:%i:%s') as e"
                + " from pm_mp_token where token='" + token + "'");
        return row == null ? "" : row.get("e");
    }

    public static void touch(String token) {
        if (token == null || !token.matches("[0-9a-f]{64}")) {
            return;
        }
        Db.update("update pm_mp_token set last_active=now() where token='" + token + "'");
    }

    public static void logout(String token) {
        if (token == null || !token.matches("[0-9a-f]{64}")) {
            return;
        }
        Db.update("delete from pm_mp_token where token='" + token + "'");
    }

    /** 清理过期登录态，可在登录时顺手调用。 */
    public static int purgeExpired() {
        return Db.update("delete from pm_mp_token where expire_time <= now()");
    }
}
