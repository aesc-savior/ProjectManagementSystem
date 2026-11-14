package archive.dao;

import archive.util.PasswordUtil;

import java.util.List;
import java.util.Map;

/** 用户与登录。角色：ADMIN 系统管理员 / ARCHIVIST 档案管理员 / USER 部门用户。 */
public class UserDao {

    /** 登录成功返回用户行（含 role_code），失败返回 null。 */
    public static Map<String, String> login(String loginId, String password) {
        if (loginId == null || loginId.trim().isEmpty() || password == null) {
            return null;
        }
        Map<String, String> u = Db.queryOne(
                "select id, login_id, user_name, password, salt, role_code, dept_name,"
                        + " secret_level, status"
                        + " from da_user where del_flag=0 and login_id='" + Db.esc(loginId) + "'");
        if (u == null) {
            return null;
        }
        if (!"NORMAL".equals(u.get("status"))) {
            return null;
        }
        if (!PasswordUtil.matches(password, u.get("salt"), u.get("password"))) {
            return null;
        }
        return u;
    }

    public static List<Map<String, String>> list(String keyword) {
        String where = " where del_flag=0";
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = Db.esc(keyword);
            where += " and (user_name like '%" + k + "%' or login_id like '%" + k + "%'"
                    + " or dept_name like '%" + k + "%')";
        }
        return Db.query("select id, login_id, user_name, role_code, dept_name, phone, email,"
                + " secret_level, status, create_time from da_user" + where + " order by id");
    }

    public static Map<String, String> get(String id) {
        if (!Db.isId(id)) {
            return null;
        }
        return Db.queryOne("select id, login_id, user_name, role_code, dept_name, phone, email,"
                + " secret_level, status from da_user where id=" + id + " and del_flag=0");
    }

    /**
     * 新增或修改。新增必须给 loginId 与 password；修改时 password 留空表示不改。
     * 返回错误提示；成功返回 null。
     */
    public static String save(String id, Map<String, String> p) {
        String loginId = Db.esc(p.get("loginId"));
        String userName = Db.esc(p.get("userName"));
        String role = normRole(p.get("roleCode"));
        String dept = Db.esc(p.get("deptName"));
        String phone = Db.esc(p.get("phone"));
        String email = Db.esc(p.get("email"));
        String secret = normSecret(p.get("secretLevel"));
        String password = p.get("password");

        if (!Db.isId(id)) {
            if (loginId.isEmpty() || userName.isEmpty()) {
                return "登录账号与姓名不能为空";
            }
            long exists = Db.queryLong("select count(*) from da_user where login_id='" + loginId + "'");
            if (exists > 0) {
                return "登录账号 " + loginId + " 已存在";
            }
            if (password == null || password.trim().isEmpty()) {
                return "新增用户必须设置初始密码";
            }
            String salt = PasswordUtil.newSalt();
            String hash = PasswordUtil.hash(password.trim(), salt);
            Db.update("insert into da_user(login_id,user_name,password,salt,role_code,dept_name,"
                    + "phone,email,secret_level,status,create_time,del_flag) values('"
                    + loginId + "','" + userName + "','" + hash + "','" + salt + "','" + role + "','"
                    + dept + "','" + phone + "','" + email + "'," + secret + ",'NORMAL','"
                    + Db.now() + "',0)");
            return null;
        }

        StringBuilder sql = new StringBuilder("update da_user set");
        sql.append(" user_name='").append(userName).append("'");
        sql.append(", role_code='").append(role).append("'");
        sql.append(", dept_name='").append(dept).append("'");
        sql.append(", phone='").append(phone).append("'");
        sql.append(", email='").append(email).append("'");
        sql.append(", secret_level=").append(secret);
        if (password != null && !password.trim().isEmpty()) {
            String salt = PasswordUtil.newSalt();
            String hash = PasswordUtil.hash(password.trim(), salt);
            sql.append(", salt='").append(salt).append("'");
            sql.append(", password='").append(hash).append("'");
        }
        sql.append(" where id=").append(id);
        Db.update(sql.toString());
        return null;
    }

    public static void toggle(String id) {
        if (Db.isId(id)) {
            Db.update("update da_user set status=if(status='NORMAL','DISABLED','NORMAL') where id=" + id);
        }
    }

    /** 逻辑删除；管理员账号不允许通过页面删除。 */
    public static String delete(String id) {
        if (!Db.isId(id)) {
            return "用户编号不合法";
        }
        Map<String, String> u = get(id);
        if (u == null) {
            return "用户不存在";
        }
        if ("ADMIN".equals(u.get("role_code"))) {
            return "系统管理员账号不允许删除，可改为停用";
        }
        Db.update("update da_user set del_flag=1 where id=" + id);
        return null;
    }

    public static long countByRole(String role) {
        return Db.queryLong("select count(*) from da_user where del_flag=0 and role_code='"
                + Db.esc(role) + "'");
    }

    private static String normRole(String v) {
        if ("ADMIN".equals(v) || "ARCHIVIST".equals(v) || "USER".equals(v)) {
            return v;
        }
        return "USER";
    }

    private static String normSecret(String v) {
        return v != null && v.matches("[1-4]") ? v : "1";
    }
}
