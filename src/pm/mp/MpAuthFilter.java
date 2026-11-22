package pm.mp;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * 小程序接口鉴权：/api/mp/* 下除登录/绑定/探活外，都必须带登录令牌。
 *
 * 令牌获取顺序：Authorization: Bearer <token> → X-Token → ?token=
 * 校验通过后把账号放进 request attribute，Servlet 直接用，避免重复查库。
 *
 * 用注解注册，不改 web/WEB-INF/web.xml，避免和其他模块的改动冲突。
 * 注意：PC 端的 LoginFilter 已经放行了 /api/*，所以这里不会和 session 登录互相干扰。
 */
@WebFilter(urlPatterns = "/api/mp/*")
public class MpAuthFilter implements Filter {

    public static final String ATTR_TOKEN = "mpToken";
    public static final String ATTR_USER = "mpUser";

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        req.setCharacterEncoding("UTF-8");

        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        String token = token(req);
        Map<String, String> user = MpDao.tokenUser(token);
        if (user == null) {
            fail(resp, HttpServletResponse.SC_UNAUTHORIZED, 401, "登录已失效，请重新登录");
            return;
        }

        MpDao.touch(token);
        req.setAttribute(ATTR_TOKEN, token);
        req.setAttribute(ATTR_USER, user);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    /** 免鉴权：探活、登录、绑定（登录前本来就没有令牌）。 */
    private boolean isPublic(String path) {
        return path.endsWith("/ping")
                || path.endsWith("/auth/login")
                || path.endsWith("/auth/bind");
    }

    private String token(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        String x = req.getHeader("X-Token");
        if (x != null && !x.trim().isEmpty()) {
            return x.trim();
        }
        String q = req.getParameter("token");
        return q == null ? null : q.trim();
    }

    private void fail(HttpServletResponse resp, int httpStatus, int code, String msg) throws IOException {
        resp.setStatus(httpStatus);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.write(Json.obj().put("code", code).put("message", msg)
                .put("data", (String) null).done());
        out.flush();
    }
}
