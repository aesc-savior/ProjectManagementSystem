package pm.web;

import pm.dao.Db;
import pm.dao.LogDao;
import pm.dao.ProjectDao;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

// 给第三方调的 REST 接口，只有项目查询和删除
// 请求头带 Authorization: Bearer <token> 或 X-Token，token 在 pm-local.properties 里配
public class ApiServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!authorized(req, resp)) {
            return;
        }
        String path = req.getPathInfo();
        if (path == null || "/".equals(path) || "/projects".equals(path)) {
            String status = req.getParameter("status") == null ? "" : req.getParameter("status").trim();
            String type = req.getParameter("type") == null ? "" : req.getParameter("type").trim();
            List<Map<String, String>> rows = ProjectDao.list("", status, type, "", "new", 1, 100,
                    "0", true);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < rows.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                Map<String, String> r = rows.get(i);
                sb.append("{\"id\":").append(r.get("id"))
                        .append(",\"projectNo\":\"").append(esc(r.get("project_no"))).append("\"")
                        .append(",\"name\":\"").append(esc(r.get("name"))).append("\"")
                        .append(",\"type\":\"").append(esc(r.get("project_type"))).append("\"")
                        .append(",\"priority\":\"").append(esc(r.get("priority"))).append("\"")
                        .append(",\"status\":\"").append(esc(r.get("status"))).append("\"")
                        .append(",\"owner\":\"").append(esc(r.get("owner_name"))).append("\"")
                        .append(",\"endDate\":\"").append(esc(r.get("end_date"))).append("\"")
                        .append(",\"progress\":").append(r.get("progress").isEmpty() ? "0" : r.get("progress"))
                        .append("}");
            }
            sb.append("]");
            write(resp, 200, "{\"code\":200,\"message\":\"ok\",\"data\":" + sb + "}");
            return;
        }
        String id = path.startsWith("/projects/") ? path.substring("/projects/".length()) : "";
        if (id.matches("\\d{1,18}")) {
            Map<String, String> r = ProjectDao.get(id);
            if (r == null) {
                write(resp, 404, "{\"code\":404,\"message\":\"project not found\",\"data\":null}");
                return;
            }
            write(resp, 200, "{\"code\":200,\"message\":\"ok\",\"data\":{\"id\":" + r.get("id")
                    + ",\"projectNo\":\"" + esc(r.get("project_no")) + "\",\"name\":\"" + esc(r.get("name"))
                    + "\",\"status\":\"" + esc(r.get("status")) + "\",\"owner\":\""
                    + esc(r.get("owner_name")) + "\",\"budget\":" + (r.get("budget").isEmpty() ? "0" : r.get("budget"))
                    + "}}");
            return;
        }
        write(resp, 404, "{\"code\":404,\"message\":\"not found\",\"data\":null}");
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!authorized(req, resp)) {
            return;
        }
        String path = req.getPathInfo();
        String id = path != null && path.startsWith("/projects/") ? path.substring("/projects/".length()) : "";
        if (!id.matches("\\d{1,18}")) {
            write(resp, 400, "{\"code\":400,\"message\":\"id required\",\"data\":null}");
            return;
        }
        Map<String, String> r = ProjectDao.get(id);
        if (r == null) {
            write(resp, 404, "{\"code\":404,\"message\":\"project not found\",\"data\":null}");
            return;
        }
        ProjectDao.delete(id);
        LogDao.write("0", "api", "DELETE", "project:" + id, "第三方接口删除项目", req.getRemoteAddr());
        write(resp, 200, "{\"code\":200,\"message\":\"deleted\",\"data\":null}");
    }

    private boolean authorized(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String auth = req.getHeader("Authorization");
        String token = req.getHeader("X-Token");
        if (auth != null && auth.startsWith("Bearer ")) {
            token = auth.substring(7).trim();
        }
        if (Db.API_TOKEN.equals(token)) {
            return true;
        }
        write(resp, 401, "{\"code\":401,\"message\":\"invalid token\",\"data\":null}");
        return false;
    }

    private void write(HttpServletResponse resp, int status, String json) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.write(json);
    }

    private String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "");
    }
}
