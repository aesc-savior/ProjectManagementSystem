package archive.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** Servlet 共用的小工具：取参数、取当前用户、拼跳转地址。 */
public final class Web {

    private Web() {
    }

    public static String p(HttpServletRequest req, String name) {
        String v = req.getParameter(name);
        return v == null ? "" : v.trim();
    }

    public static int i(HttpServletRequest req, String name, int def) {
        try {
            return Integer.parseInt(p(req, name));
        } catch (Exception e) {
            return def;
        }
    }

    public static String uid(HttpServletRequest req) {
        return str(req, "uid");
    }

    public static String uname(HttpServletRequest req) {
        return str(req, "uname");
    }

    public static String role(HttpServletRequest req) {
        String r = str(req, "role");
        return r.isEmpty() ? "USER" : r;
    }

    public static String dept(HttpServletRequest req) {
        return str(req, "dept");
    }

    /** 当前登录用户的可访问密级（1～4），用于档案详情做密级校验。 */
    public static int secret(HttpServletRequest req) {
        try {
            return Integer.parseInt(str(req, "secret"));
        } catch (Exception e) {
            return 1;
        }
    }

    public static boolean isAdmin(HttpServletRequest req) {
        return "ADMIN".equals(role(req));
    }

    /** 管理员或档案员：可审批、可看全部日志。 */
    public static boolean isStaff(HttpServletRequest req) {
        String r = role(req);
        return "ADMIN".equals(r) || "ARCHIVIST".equals(r);
    }

    public static String ip(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    public static String enc(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String str(HttpServletRequest req, String key) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return "";
        }
        Object v = session.getAttribute(key);
        return v == null ? "" : v.toString();
    }
}
