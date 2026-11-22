package pm.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

// 企业微信群机器人推送
// 地址写在 pm-local.properties 的 pm.wechat.webhook，没配就返回 SKIPPED
public class WeChatNotifier {

    private WeChatNotifier() {
    }

    private static String webhook() {
        return Conf.get("pm.wechat.webhook", "PM_WECHAT_WEBHOOK", "");
    }

    public static boolean configured() {
        return !webhook().isEmpty();
    }

    // 发送一条文本消息
    // 返回 长度为 2 的数组：{状态, 说明}，状态取值 SENT / SKIPPED / FAILED
    public static String[] send(String title, String content) {
        if (!configured()) {
            return new String[]{"SKIPPED", "未配置企业微信 Webhook（pm.wechat.webhook）"};
        }
        HttpURLConnection conn = null;
        try {
            String body = "{\"msgtype\":\"text\",\"text\":{\"content\":"
                    + quote(title + "\n" + content) + "}}";
            URL url = new URL(webhook());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(6000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes("UTF-8"));
            out.flush();
            out.close();

            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String resp = read(in);
            if (code == 200 && resp.contains("\"errcode\":0")) {
                return new String[]{"SENT", "企业微信已接收"};
            }
            return new String[]{"FAILED", "HTTP " + code + " " + shorten(resp)};
        } catch (Exception e) {
            return new String[]{"FAILED", shorten(e.getClass().getSimpleName() + ": " + e.getMessage())};
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String read(InputStream in) throws Exception {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int len;
        while ((len = in.read(buf)) > 0) {
            bos.write(buf, 0, len);
        }
        in.close();
        return new String(bos.toByteArray(), "UTF-8");
    }

    private static String shorten(String s) {
        if (s == null) {
            return "";
        }
        String v = s.replaceAll("\\s+", " ").trim();
        return v.length() > 180 ? v.substring(0, 180) + "..." : v;
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(' ');
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
