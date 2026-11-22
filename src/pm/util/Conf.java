package pm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

// 读配置：先看 -D 参数，再看环境变量，最后读 pm-local.properties
// 连接串里有 &，不能写在命令行里，所以统一放这个文件
public final class Conf {

    // 找到的配置文件，没找到就是 null
    private static final File FILE;
    private static final Properties PROPS = new Properties();
    private static final String SOURCE;

    static {
        File f = locate();
        FILE = f;
        if (f != null) {
            load(f);
            SOURCE = f.getAbsolutePath();
        } else {
            loadFromClasspath();
            SOURCE = PROPS.isEmpty() ? "未找到配置，使用代码默认值" : "classpath:pm-local.properties";
        }
        System.out.println("[Conf] 配置来源：" + SOURCE);
    }

    private Conf() {
    }

    private static File locate() {
        String explicit = System.getProperty("pm.config");
        if (notBlank(explicit)) {
            File f = new File(explicit.trim());
            if (f.isFile()) {
                return f;
            }
        }
        String base = System.getProperty("catalina.base");
        if (notBlank(base)) {
            File f = new File(base, "conf/pm-local.properties");
            if (f.isFile()) {
                return f;
            }
        }
        String home = System.getProperty("user.home");
        if (notBlank(home)) {
            File f = new File(home, "pm-local.properties");
            if (f.isFile()) {
                return f;
            }
        }
        File f = new File("pm-local.properties");
        if (f.isFile()) {
            return f;
        }
        return null;
    }

    private static void load(File f) {
        // 先按 UTF-8 读，读不到中文再退回 GBK（Windows 记事本另存的常见编码）
        for (String cs : new String[]{"UTF-8", "GBK"}) {
            try (InputStream in = new FileInputStream(f);
                 InputStreamReader r = new InputStreamReader(in, cs)) {
                Properties p = new Properties();
                p.load(r);
                PROPS.putAll(p);
                return;
            } catch (Exception ignore) {
                // 换下一个编码重试
            }
        }
    }

    // 类路径里的同名配置（WEB-INF/classes/pm-local.properties），文件系统没找到时兜底
    private static void loadFromClasspath() {
        if (FILE != null) {
            return;
        }
        InputStream in = Conf.class.getClassLoader().getResourceAsStream("pm-local.properties");
        if (in == null) {
            return;
        }
        try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            PROPS.load(r);
        } catch (Exception ignore) {
            // 忽略
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    // 文件里是否配置了该项
    public static boolean has(String key) {
        if (PROPS.isEmpty()) {
            loadFromClasspath();
        }
        return notBlank(PROPS.getProperty(key));
    }

    // 按优先级取配置
    // prop：JVM 参数名，如 pm.db.password
    // env：环境变量名，如 PM_DB_PASSWORD，可为 null
    // def：默认值
    public static String get(String prop, String env, String def) {
        String v = System.getProperty(prop);
        if (notBlank(v)) {
            return v.trim();
        }
        if (env != null) {
            v = System.getenv(env);
            if (notBlank(v)) {
                return v.trim();
            }
        }
        if (PROPS.isEmpty()) {
            loadFromClasspath();
        }
        v = PROPS.getProperty(prop);
        return notBlank(v) ? v.trim() : def;
    }

    public static int getInt(String prop, String env, int def) {
        try {
            return Integer.parseInt(get(prop, env, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static boolean getBool(String prop, String env, boolean def) {
        return Boolean.parseBoolean(get(prop, env, String.valueOf(def)));
    }

    // 启动时打在日志里，说明配置是从哪读的
    public static String source() {
        return SOURCE;
    }
}
