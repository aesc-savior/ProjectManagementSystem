package archive.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 极简 JDBC 工具类。
 *
 * 连接参数优先级：JVM 参数 > 环境变量 > 默认值
 *   -Darchive.db.url / -Darchive.db.user / -Darchive.db.password
 *   ARCHIVE_DB_URL / ARCHIVE_DB_USER / ARCHIVE_DB_PASSWORD
 *
 * 默认连接：jdbc:mysql://localhost:3366/archive  root/123456
 */
public class Db {

    public static final String URL = pick("archive.db.url", "ARCHIVE_DB_URL",
            "jdbc:mysql://localhost:3366/archive?useUnicode=true&characterEncoding=UTF-8"
                    + "&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true");
    public static final String USER = pick("archive.db.user", "ARCHIVE_DB_USER", "root");
    public static final String PASSWORD = pick("archive.db.password", "ARCHIVE_DB_PASSWORD", "123456");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "未找到 MySQL 驱动：请把 mysql-connector-java-8.0.20.jar 放进 web/WEB-INF/lib", e);
        }
    }

    private static String pick(String prop, String env, String def) {
        String v = System.getProperty(prop);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(env);
        }
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }

    private static Connection open() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** 查询多行，列名原样作为 key（字符串便于 JSP 直接显示）。 */
    public static List<Map<String, String>> query(String sql) {
        List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
        try (Connection conn = open();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<String, String>();
                for (int i = 1; i <= n; i++) {
                    String v = rs.getString(i);
                    row.put(md.getColumnLabel(i), v == null ? "" : v);
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException("查询失败：" + sql, e);
        }
        return rows;
    }

    public static Map<String, String> queryOne(String sql) {
        List<Map<String, String>> rows = query(sql);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public static long queryLong(String sql) {
        Map<String, String> row = queryOne(sql);
        if (row == null || row.isEmpty()) {
            return 0L;
        }
        String v = row.values().iterator().next();
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return 0L;
        }
    }

    /** 执行 insert / update / delete，返回影响行数。 */
    public static int update(String sql) {
        try (Connection conn = open();
             Statement st = conn.createStatement()) {
            return st.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException("执行失败：" + sql, e);
        }
    }

    /** 单引号转义，避免拼接 SQL 时把语句打断（课程项目够用；生产应改 PreparedStatement）。 */
    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replace("'", "''");
    }

    /** 只允许纯数字主键，防止 id 参数注入。 */
    public static boolean isId(String s) {
        return s != null && s.matches("\\d{1,18}");
    }

    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }

    public static int year() {
        return Integer.parseInt(new SimpleDateFormat("yyyy").format(new Date()));
    }
}
