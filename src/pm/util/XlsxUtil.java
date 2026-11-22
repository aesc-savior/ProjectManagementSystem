package pm.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;

// 极简 xlsx 写入器，按 OOXML 规则直接打 zip，不依赖 POI
// 只做单表：表头加粗、列宽按内容估算、数字单元格、冻结首行
public class XlsxUtil {

    private XlsxUtil() {
    }

    // 输出下载响应，中文文件名按 RFC 5987 编码
    public static void download(HttpServletResponse resp, String fileName,
                                String sheetName, List<String> headers, List<List<String>> rows)
            throws IOException {
        byte[] bytes = build(sheetName, headers, rows);
        String encoded = URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition",
                "attachment; filename=\"export.xlsx\"; filename*=UTF-8''" + encoded);
        resp.setContentLength(bytes.length);
        OutputStream out = resp.getOutputStream();
        out.write(bytes);
        out.flush();
    }

    // 生成 xlsx 字节内容
    public static byte[] build(String sheetName, List<String> headers, List<List<String>> rows)
            throws IOException {
        String sheet = sheetXml(headers, rows);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bos);
        put(zip, "[Content_Types].xml", CONTENT_TYPES);
        put(zip, "_rels/.rels", RELS);
        put(zip, "xl/workbook.xml", workbook(safeSheetName(sheetName)));
        put(zip, "xl/_rels/workbook.xml.rels", WB_RELS);
        put(zip, "xl/styles.xml", STYLES);
        put(zip, "xl/worksheets/sheet1.xml", sheet);
        zip.finish();
        zip.close();
        return bos.toByteArray();
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes("UTF-8"));
        zip.closeEntry();
    }

    private static String sheetXml(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">");

        int cols = headers.size();
        sb.append("<dimension ref=\"A1:").append(colName(Math.max(cols - 1, 0)))
                .append(rows.size() + 1).append("\"/>");

        // 冻结表头（CT_Worksheet 要求 sheetViews 在 cols/sheetData 之前）
        sb.append("<sheetViews><sheetView workbookViewId=\"0\">")
                .append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                .append("</sheetView></sheetViews>");
        sb.append("<sheetFormatPr defaultRowHeight=\"15\"/>");

        // 列宽：按表头与首 50 行内容的最大显示宽度估算（中文按 2 字符计）
        int[] width = new int[cols];
        for (int i = 0; i < cols; i++) {
            width[i] = visualWidth(headers.get(i));
            for (int r = 0; r < rows.size() && r < 50; r++) {
                List<String> row = rows.get(r);
                if (i < row.size()) {
                    width[i] = Math.max(width[i], visualWidth(row.get(i)));
                }
            }
        }
        sb.append("<cols>");
        for (int i = 0; i < cols; i++) {
            int w = Math.min(Math.max(width[i] + 2, 8), 52);
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                    .append("\" width=\"").append(w).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols>");

        sb.append("<sheetData>");
        sb.append("<row r=\"1\">");
        for (int i = 0; i < cols; i++) {
            sb.append(cell(colName(i) + "1", headers.get(i), 1));
        }
        sb.append("</row>");

        for (int r = 0; r < rows.size(); r++) {
            int rowNo = r + 2;
            sb.append("<row r=\"").append(rowNo).append("\">");
            List<String> row = rows.get(r);
            for (int i = 0; i < cols; i++) {
                String v = i < row.size() ? row.get(i) : "";
                sb.append(cell(colName(i) + rowNo, v, 0));
            }
            sb.append("</row>");
        }
        sb.append("</sheetData>");
        sb.append("</worksheet>");
        return sb.toString();
    }

    private static String cell(String ref, String value, int style) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            return "<c r=\"" + ref + "\" s=\"" + style + "\"/>";
        }
        boolean numeric = v.matches("-?\\d{1,15}(\\.\\d{1,4})?");
        StringBuilder sb = new StringBuilder();
        sb.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\"");
        if (numeric) {
            sb.append("><v>").append(v).append("</v></c>");
        } else {
            sb.append(" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(esc(v.replace("\r", "").replace("\n", " ")))
                    .append("</t></is></c>");
        }
        return sb.toString();
    }

    private static int visualWidth(String s) {
        if (s == null) {
            return 0;
        }
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += s.charAt(i) > 127 ? 2 : 1;
        }
        return w;
    }

    private static String colName(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        }
        return sb.toString();
    }

    private static String safeSheetName(String name) {
        String n = name == null || name.trim().isEmpty() ? "Sheet1" : name.trim();
        n = n.replaceAll("[\\\\/*\\[\\]:?]", " ");
        return n.length() > 31 ? n.substring(0, 31) : n;
    }

    private static String workbook(String sheetName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
                + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"" + esc(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:
                    // 过滤 XML 1.0 非法控制字符，否则 Excel 打不开
                    if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                        sb.append(' ');
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static final String CONTENT_TYPES =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
                    + "</Types>";

    private static final String RELS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>";

    private static final String WB_RELS =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                    + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
                    + "</Relationships>";

    // 单元格样式：0 常规，1 表头加粗
    private static final String STYLES =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                    + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                    + "<fonts count=\"2\">"
                    + "<font><sz val=\"11\"/><name val=\"微软雅黑\"/></font>"
                    + "<font><b/><sz val=\"11\"/><name val=\"微软雅黑\"/></font>"
                    + "</fonts>"
                    + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>"
                    + "<fill><patternFill patternType=\"gray125\"/></fill></fills>"
                    + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
                    + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
                    + "<cellXfs count=\"2\">"
                    + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\">"
                    + "<alignment vertical=\"top\" wrapText=\"1\"/></xf>"
                    + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\">"
                    + "<alignment vertical=\"center\" wrapText=\"1\"/></xf>"
                    + "</cellXfs>"
                    + "</styleSheet>";
}
