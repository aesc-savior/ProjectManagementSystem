package pm.mp;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * 小程序端附件上传（PM-121 拍照上传）。
 *
 *   POST /api/mp/upload        表单字段 file，单文件 ≤ 5MB
 *   GET  /api/mp/files/{name}  读取（图片用 &lt;image&gt; 展示，令牌走 ?token=）
 *
 * 只落盘、不写 pm_doc：项目文档表属于 D 组，这里不越界。返回的相对地址
 * 由前端带到后续业务请求里（进度备注、审批附件等）。
 *
 * 文件名统一重写为 32 位随机 hex + 原扩展名，杜绝路径穿越和中文乱码；
 * 读取时再用白名单正则卡一次，双保险。
 */
@WebServlet(urlPatterns = {"/api/mp/upload", "/api/mp/files/*"})
@MultipartConfig(maxFileSize = MpUploadServlet.MAX_BYTES,
        maxRequestSize = MpUploadServlet.MAX_BYTES + 1024 * 1024,
        fileSizeThreshold = 512 * 1024)
public class MpUploadServlet extends HttpServlet {

    public static final int MAX_MB = 5;
    public static final long MAX_BYTES = MAX_MB * 1024L * 1024L;

    private static final String[] ALLOWED = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf"};

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        Part part;
        try {
            part = req.getPart("file");
        } catch (ServletException | IllegalStateException e) {
            fail(resp, 413, 413, "文件超过 " + MAX_MB + "MB，请压缩后重试");
            return;
        }
        if (part == null || part.getSize() <= 0) {
            fail(resp, 400, 400, "请选择要上传的文件");
            return;
        }
        if (part.getSize() > MAX_BYTES) {
            fail(resp, 413, 413, "文件超过 " + MAX_MB + "MB，请压缩后重试");
            return;
        }

        String submitted = part.getSubmittedFileName();
        String ext = ext(submitted);
        if (ext == null) {
            fail(resp, 400, 400, "只支持 " + String.join(" / ", ALLOWED) + " 格式");
            return;
        }

        File folder = folder();
        if (!folder.exists() && !folder.mkdirs()) {
            fail(resp, 500, 500, "上传目录不可写：" + folder.getAbsolutePath());
            return;
        }

        String name = hex() + ext;
        File target = new File(folder, name);
        part.write(target.getAbsolutePath());

        resp.setStatus(200);
        write(resp, Json.obj().put("code", 200).put("message", "ok")
                .raw("data", Json.obj()
                        .put("name", name)
                        .put("size", part.getSize())
                        .put("url", "/api/mp/files/" + name)
                        .done()).done());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String name = req.getPathInfo();
        if (name == null || name.length() < 2) {
            fail(resp, 404, 404, "文件不存在");
            return;
        }
        name = name.substring(1);
        // 只接受本服务生成的文件名，挡住 ../ 之类
        if (!name.matches("[0-9a-f]{32}\\.(jpg|jpeg|png|gif|webp|pdf)")) {
            fail(resp, 404, 404, "文件不存在");
            return;
        }
        File file = new File(folder(), name);
        if (!file.exists() || !file.isFile()) {
            fail(resp, 404, 404, "文件不存在");
            return;
        }

        resp.setContentType(mime(name));
        resp.setContentLengthLong(file.length());
        resp.setHeader("Cache-Control", "private, max-age=86400");
        try (FileInputStream in = new FileInputStream(file);
             OutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
        }
    }

    // ---------------- 工具 ----------------

    private File folder() {
        String dir = System.getProperty("pm.upload.dir",
                System.getProperty("java.io.tmpdir") + File.separator + "pm-upload");
        return new File(dir, "mp");
    }

    private String ext(String name) {
        if (name == null) {
            return null;
        }
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String ext = name.substring(dot).toLowerCase(Locale.ROOT);
        for (String a : ALLOWED) {
            if (a.equals(ext)) {
                return ext;
            }
        }
        return null;
    }

    private String mime(String name) {
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        return "image/jpeg";
    }

    private String hex() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        char[] hex = "0123456789abcdef".toCharArray();
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) {
            sb.append(hex[(b >> 4) & 0x0f]).append(hex[b & 0x0f]);
        }
        return sb.toString();
    }

    private void fail(HttpServletResponse resp, int httpStatus, int code, String msg)
            throws IOException {
        resp.setStatus(httpStatus);
        write(resp, Json.obj().put("code", code).put("message", msg)
                .put("data", (String) null).done());
    }

    private void write(HttpServletResponse resp, String json) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.write(json);
        out.flush();
    }
}
