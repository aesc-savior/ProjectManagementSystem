package pm.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

// 登录过滤器：未登录访问业务页面统一跳登录页
// 放行：登录页、认证接口、静态资源、欢迎页、开放 API（API 自己校验 Token）
public class LoginFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String ctx = req.getContextPath();
        String path = req.getRequestURI().substring(ctx.length());
        int semicolon = path.indexOf(';');
        if (semicolon > -1) {
            path = path.substring(0, semicolon);
        }

        if (isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        Object uid = session == null ? null : session.getAttribute("uid");
        if (uid != null && !uid.toString().trim().isEmpty()) {
            chain.doFilter(request, response);
        } else {
            resp.sendRedirect(ctx + "/login.jsp?error=timeout");
        }
    }

    private boolean isPublic(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return true;
        }
        if ("/login.jsp".equals(path) || "/auth".equals(path) || "/index.jsp".equals(path)) {
            return true;
        }
        if (path.startsWith("/assets/") || path.startsWith("/api/")) {
            return true;
        }
        String lower = path.toLowerCase();
        return lower.endsWith(".css") || lower.endsWith(".js") || lower.endsWith(".png")
                || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".ico") || lower.endsWith(".svg") || lower.endsWith(".woff")
                || lower.endsWith(".woff2") || lower.endsWith(".ttf");
    }

    @Override
    public void destroy() {
    }
}
