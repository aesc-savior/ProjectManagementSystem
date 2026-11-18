package pm.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;

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
        return r.isEmpty() ? "MEMBER" : r;
    }

    public static String dept(HttpServletRequest req) {
        return str(req, "dept");
    }

    public static boolean isAdmin(HttpServletRequest req) {
        return "ADMIN".equals(role(req));
    }

    /** 管理员或项目经理：可建项目、管成员、审批、生成罚款。 */
    public static boolean isPm(HttpServletRequest req) {
        String r = role(req);
        return "ADMIN".equals(r) || "PM".equals(r);
    }

    public static boolean isViewer(HttpServletRequest req) {
        return "VIEWER".equals(role(req));
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

    public static String money(String v) {
        try {
            return new DecimalFormat("#,##0.00").format(Double.parseDouble(v));
        } catch (Exception e) {
            return "0.00";
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
