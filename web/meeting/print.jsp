<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<%!
  private String e(Object o) {
    if (o == null) return "";
    return String.valueOf(o).replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
  }

  private String replyText(String v) {
    if ("ACCEPTED".equals(v)) return "参加";
    if ("DECLINED".equals(v)) return "请假";
    return "待回执";
  }
%>
<%
  Map<String, String> row = (Map<String, String>) request.getAttribute("row");
  List<Map<String, String>> attendees =
          (List<Map<String, String>>) request.getAttribute("attendees");
  if (attendees == null) attendees = new ArrayList<Map<String, String>>();
  String st = MeetingDao.statusText(row);
%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>会议通知 - <%= e(row.get("topic")) %></title>
  <style>
    * { box-sizing: border-box; }
    body { margin: 0; background: #f2f4f7; font-family: "Microsoft YaHei", Arial, sans-serif; color: #222; }
    .toolbar { padding: 10px 16px; background: #fff; border-bottom: 1px solid #ddd; }
    .toolbar button, .toolbar a {
      display: inline-block; margin-right: 8px; padding: 6px 14px; border: 1px solid #ccc;
      border-radius: 4px; background: #fff; color: #333; text-decoration: none; cursor: pointer;
    }
    .toolbar button:hover, .toolbar a:hover { background: #f5f5f5; }
    .sheet { width: 794px; min-height: 1000px; margin: 16px auto; padding: 40px 46px;
      background: #fff; box-shadow: 0 0 8px rgba(0,0,0,.08); }
    h1 { text-align: center; font-size: 22px; margin: 0 0 8px; }
    .sub { text-align: center; color: #666; font-size: 12px; margin-bottom: 22px; }
    table { width: 100%; border-collapse: collapse; font-size: 13px; }
    th, td { border: 1px solid #bbb; padding: 7px 8px; text-align: left; vertical-align: top; }
    th { background: #f5f5f5; width: 110px; font-weight: normal; }
    .section-title { margin: 20px 0 8px; font-size: 15px; font-weight: bold; }
    .agenda { border: 1px solid #bbb; padding: 10px; min-height: 90px;
      white-space: pre-wrap; font-size: 13px; line-height: 1.7; }
    .sign { margin-top: 34px; display: flex; justify-content: space-between; font-size: 13px; }
    @media print {
      body { background: #fff; }
      .toolbar { display: none; }
      .sheet { width: auto; min-height: auto; margin: 0; padding: 0; box-shadow: none; }
      @page { size: A4 portrait; margin: 14mm; }
    }
  </style>
</head>
<body>
<div class="toolbar">
  <button onclick="window.print()">打印</button>
  <a href="<%= request.getContextPath() %>/meeting?action=view&id=<%= row.get("id") %>">返回详情</a>
</div>
<div class="sheet">
  <h1>会议通知</h1>
  <div class="sub">打印时间：<%= e(request.getAttribute("printTime")) %></div>

  <table>
    <tr><th>会议主题</th><td colspan="3"><%= e(row.get("topic")) %></td></tr>
    <tr><th>关联项目</th><td><%= e(row.get("project_name")) %></td>
      <th>状态</th><td><%= e(st) %></td></tr>
    <tr><th>开始时间</th><td><%= e(row.get("start_time")) %></td>
      <th>结束时间</th><td><%= e(row.get("end_time")) %></td></tr>
    <tr><th>地点</th><td><%= e(row.get("location")) %></td>
      <th>组织人</th><td><%= e(row.get("organizer")) %></td></tr>
    <tr><th>参会人数</th>
      <td colspan="3">应到 <%= e(row.get("attendee_count")) %>，
        同意 <%= e(row.get("accepted_count")) %>，
        请假 <%= e(row.get("declined_count")) %></td></tr>
    <% if ("CANCELED".equals(row.get("status"))) { %>
    <tr><th>取消理由</th><td colspan="3"><%= e(row.get("cancel_reason")) %></td></tr>
    <% } %>
  </table>

  <div class="section-title">会议议程</div>
  <div class="agenda"><%= e(row.get("agenda")) %></div>

  <div class="section-title">参会人回执</div>
  <table>
    <thead>
    <tr><th>参会人</th><th>部门</th><th>回执</th><th>理由</th><th>回执时间</th></tr>
    </thead>
    <tbody>
    <% for (Map<String, String> a : attendees) { %>
    <tr>
      <td><%= e(a.get("user_name")) %></td>
      <td><%= e(a.get("dept_name")) %></td>
      <td><%= replyText(a.get("reply_status")) %></td>
      <td><%= e(a.get("reply_reason")) %></td>
      <td><%= e(a.get("reply_time")) %></td>
    </tr>
    <% } %>
    </tbody>
  </table>

  <div class="sign">
    <span>组织人签字：________________</span>
    <span>日期：________________</span>
  </div>
</div>
</body>
</html>