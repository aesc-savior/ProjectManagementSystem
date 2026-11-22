package pm.mp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 微信登录凭证校验（code2session）。
 *
 * 配了 appid/secret 就调真接口，没配就走 mock：
 * mock 模式下 openid = "mock_" + code，所以小程序端要发一个固定 code
 * （见 miniprogram/utils/config.js 的 mockCode），否则每次登录 openid 都变，绑不上。
 *
 * 启动参数：
 *   -Dpm.wx.appid=wx...      小程序 AppID
 *   -Dpm.wx.secret=...       小程序 AppSecret
 *   -Dpm.wx.mock=true        强制 mock（默认：没配 appid 时自动 mock）
 */
public final class WxApi {

    private static final String APPID = pick("pm.wx.appid", "PM_WX_APPID", "");
    private static final String SECRET = pick("pm.wx.secret", "PM_WX_SECRET", "");
    private static final boolean FORCE_MOCK =
            "true".equalsIgnoreCase(pick("pm.wx.mock", "PM_WX_MOCK", ""));

    private WxApi() {
    }

    public static boolean isMock() {
        return FORCE_MOCK || APPID.isEmpty() || SECRET.isEmpty();
    }

    /**
     * code 换 openid。失败抛 IOException，消息可直接返回给前端。
     */
    public static String codeToOpenid(String code) throws IOException {
        String c = code == null ? "" : code.trim();
        if (c.isEmpty()) {
            throw new IOException("缺少 code");
        }
        if (isMock()) {
            // 用稳定映射，便于本地无 AppID 时反复登录同一个"微信"
            return "mock_" + c;
        }
        StringBuilder url = new StringBuilder("https://api.weixin.qq.com/sns/jscode2session");
        url.append("?appid=").append(enc(APPID))
                .append("&secret=").append(enc(SECRET))
                .append("&js_code=").append(enc(c))
                .append("&grant_type=authorization_code");

        String resp = get(url.toString());
        String openid = Json.pick(resp, "openid");
        if (openid != null && !openid.isEmpty()) {
            return openid;
        }
        String errcode = Json.pick(resp, "errcode");
        String errmsg = Json.pick(resp, "errmsg");
        throw new IOException("微信登录失败（" + (errcode == null ? "unknown" : errcode) + " "
                + (errmsg == null ? "" : errmsg) + "）");
    }

    private static String get(String url) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            int status = conn.getResponseCode();
            InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            StringBuilder sb = new StringBuilder();
            if (in != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                char[] buf = new char[1024];
                int n;
                while ((n = br.read(buf)) > 0) {
                    sb.append(buf, 0, n);
                }
                br.close();
            }
            if (status >= 400) {
                throw new IOException("微信接口返回 HTTP " + status);
            }
            return sb.toString();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private static String pick(String prop, String env, String def) {
        String v = System.getProperty(prop);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(env);
        }
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }
}
