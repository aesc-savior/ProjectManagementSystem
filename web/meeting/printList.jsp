<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<%!
  private String e(Object o) {
    if (o == null) return "";
    return String.valueOf(o).replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
  }
%>
<%
  List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
  if (rows == null) rows = new ArrayList<Map<String, String>>();
  long total = request.getAttribute("total") == null ? 0L
          : ((Number) request.getAttribute("total")).longValue();
  String fProjectId = (String) request.getAttribute("fProjectId");
  String fUserId = (String) request.getAttribute("fUserId");
  String fFrom = (String) request.getAttribute("fFrom");
  String fTo = (String) request.getAttribute("fTo");
  String fStatus = (String) request.getAttribute("fStatus");
  String fKeyword = (String) request.getAttribute("fKeyword");
  if (fProjectId == null) fProjectId = "";
  if (fUserId == null) fUserId = "";
  if (fFrom == null) fFrom = "";
  if (fTo == null) fTo = "";
  if (fStatus == null) fStatus = "";
  if (fKeyword == null) fKeyword = "";
%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>会议清单</title>
  <style>
    * { box-sizing: border-box; }
    body { margin: 0; background: #f2f4f7; font-family: "Microsoft YaHei", Arial, sans-serif; color: #222; }
    .toolbar { padding: 10px 16px; background: #fff; border-bottom: 1px solid #ddd; }
    .toolbar button, .toolbar a {
      display: inline-block; margin-right: 8px; padding: 6px 14px; border: 1px solid #ccc;
      border-radius: 4px; background: #fff; color: #333; text-decoration: none; cursor: pointer;
    }
    .sheet { margin: 16px; padding: 20px; background: #fff; box-shadow: 0 0 8px rgba(0,0,0,.08); }
    h1 { font-size: 20px; margin: 0 0 6px; }
    .meta { color: #555; font-size: 12px; margin-bottom: 12px; }
    table { width: 100%; border-collapse: collapse; font-size: 12px; }
    th, td { border: 1px solid #999; padding: 6px 7px; text-align: left; vertical-align: top; }
    th { background: #f0f0f0; }
    @media print {
      body { background: #fff; }
      .toolbar { display: none; }
      .sheet { margin: 0; padding: 0; box-shadow: none; }
      @page { size: A4 landscape; margin: 10mm; }
      thead { display: table-header-group; }
    }
  </style>
</head>
<body>
<div class="toolbar">
  <button onclick="window.print()">打印</button>
  <a href="<%= request.getContextPath() %>/meeting?action=list">返回列表</a>
</div>
<div class="sheet">
  <h1>会议清单</h1>
  <div class="meta">
    共 <%= total %> 场　
    项目：<%= e(fProjectId.isEmpty() ? "全部" : fProjectId) %>　
    参会人ID：<%= e(fUserId.isEmpty() ? "全部" : fUserId) %>　
    日期：<%= e(fFrom.isEmpty() ? "不限" : fFrom) %> ~ <%= e(fTo.isEmpty() ? "不限" : fTo) %>　
    状态：<%= e(fStatus.isEmpty() ? "全部" : fStatus) %>　
    关键词：<%= e(fKeyword.isEmpty() ? "无" : fKeyword) %>　
    打印时间：<%= e(request.getAttribute("printTime")) %>
  </div>
  <table>
    <thead>
    <tr>
      <th>会议主题</th>
      <th>关联项目</th>
      <th>开始时间</th>
      <th>结束时间</th>
      <th>地点</th>
      <th>组织人</th>
      <th>参会人数</th>
      <th>同意/请假</th>
      <th>状态</th>
    </tr>
    </thead>
    <tbody>
    <% for (Map<String, String> r : rows) { %>
    <tr>
      <td><%= e(r.get("topic")) %></td>
      <td><%= e(r.get("project_name")) %></td>
      <td><%= e(r.get("start_time")) %></td>
      <td><%= e(r.get("end_time")) %></td>
      <td><%= e(r.get("location")) %></td>
      <td><%= e(r.get("organizer")) %></td>
      <td><%= e(r.get("attendee_count")) %></td>
      <td><%= e(r.get("accepted_count")) %> / <%= e(r.get("declined_count")) %></td>
      <td><%= e(MeetingDao.statusText(r)) %></td>
    </tr>
    <% } %>
    </tbody>
  </table>
</div>
</body>
</html>