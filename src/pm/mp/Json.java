package pm.mp;

/**
 * 极简 JSON 工具：只服务于小程序接口层，零第三方依赖。
 *
 * 为什么不用现成的库：web/WEB-INF/lib 里的 json.jar 版本与包名不确定，
 * 而且 PC 端 ApiServlet 本来就是手拼 JSON。等接口稳定后建议统一换成
 * org.json / Gson，这个类可以整体删掉。
 */
public final class Json {

    private final StringBuilder sb;
    private final char close;
    private boolean first = true;

    private Json(char open, char close) {
        this.sb = new StringBuilder().append(open);
        this.close = close;
    }

    public static Json obj() {
        return new Json('{', '}');
    }

    public static Json arr() {
        return new Json('[', ']');
    }

    private void sep() {
        if (!first) {
            sb.append(',');
        }
        first = false;
    }

    public Json put(String key, String val) {
        sep();
        sb.append(quote(key)).append(':').append(val == null ? "null" : quote(val));
        return this;
    }

    public Json put(String key, long val) {
        sep();
        sb.append(quote(key)).append(':').append(val);
        return this;
    }

    public Json put(String key, boolean val) {
        sep();
        sb.append(quote(key)).append(':').append(val);
        return this;
    }

    /** 金额等小数：保留两位，避免 300.0 这种前端不好看的输出。 */
    public Json put(String key, double val) {
        sep();
        sb.append(quote(key)).append(':')
                .append(java.math.BigDecimal.valueOf(val)
                        .setScale(2, java.math.RoundingMode.HALF_UP).toPlainString());
        return this;
    }

    /** 数组里挂一个对象。 */
    public Json add(Json val) {
        sep();
        sb.append(val == null ? "null" : val.done());
        return this;
    }

    /** 数组里挂一个列表（List&lt;Map&gt; 直接转成 JSON 数组）。 */
    public Json addRows(java.util.List<java.util.Map<String, String>> rows) {
        sep();
        sb.append(rowsOf(rows));
        return this;
    }

    public Json putRows(String key, java.util.List<java.util.Map<String, String>> rows) {
        return raw(key, rowsOf(rows));
    }

    /** List&lt;Map&lt;String,String&gt;&gt; → JSON 数组字符串；值全是字符串，null 保留为 null。 */
    public static String rowsOf(java.util.List<java.util.Map<String, String>> rows) {
        StringBuilder out = new StringBuilder("[");
        if (rows != null) {
            boolean firstRow = true;
            for (java.util.Map<String, String> row : rows) {
                if (!firstRow) {
                    out.append(',');
                }
                firstRow = false;
                Json one = obj();
                for (java.util.Map.Entry<String, String> e : row.entrySet()) {
                    one.put(e.getKey(), e.getValue());
                }
                out.append(one.done());
            }
        }
        return out.append(']').toString();
    }

    /** 直接挂一段已经拼好的 JSON（对象或数组）。 */
    public Json raw(String key, String json) {
        sep();
        sb.append(quote(key)).append(':').append(json == null ? "null" : json);
        return this;
    }

    public Json put(String key, Json val) {
        return raw(key, val == null ? null : val.done());
    }

    public Json add(String val) {
        sep();
        sb.append(val == null ? "null" : quote(val));
        return this;
    }

    public String done() {
        return sb.append(close).toString();
    }

    @Override
    public String toString() {
        return done();
    }

    public static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(s.length() + 16).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':
                    out.append("\\\"");
                    break;
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.append('"').toString();
    }

    /**
     * 从扁平 JSON 文本里取字符串字段。
     * 只支持 {"k":"v"} / {"k":123} 这种一层结构，够小程序端提交参数用；
     * 需要嵌套结构时请换成标准 JSON 库。
     */
    public static String pick(String body, String key) {
        if (body == null || key == null) {
            return null;
        }
        int i = body.indexOf("\"" + key + "\"");
        if (i < 0) {
            return null;
        }
        int colon = body.indexOf(':', i + key.length() + 2);
        if (colon < 0) {
            return null;
        }
        int p = colon + 1;
        while (p < body.length() && Character.isWhitespace(body.charAt(p))) {
            p++;
        }
        if (p >= body.length()) {
            return null;
        }
        char c = body.charAt(p);
        if (c != '"') {
            int end = p;
            while (end < body.length() && ",}] \t\r\n".indexOf(body.charAt(end)) < 0) {
                end++;
            }
            String raw = body.substring(p, end).trim();
            return "null".equals(raw) || raw.isEmpty() ? null : raw;
        }
        StringBuilder out = new StringBuilder();
        for (int k = p + 1; k < body.length(); k++) {
            char ch = body.charAt(k);
            if (ch == '\\' && k + 1 < body.length()) {
                char n = body.charAt(++k);
                if (n == 'n') {
                    out.append('\n');
                } else if (n == 'r') {
                    out.append('\r');
                } else if (n == 't') {
                    out.append('\t');
                } else if (n == 'u' && k + 4 < body.length()) {
                    try {
                        out.append((char) Integer.parseInt(body.substring(k + 1, k + 5), 16));
                        k += 4;
                    } catch (NumberFormatException e) {
                        out.append(n);
                    }
                } else {
                    out.append(n);
                }
            } else if (ch == '"') {
                return out.toString();
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }
}
