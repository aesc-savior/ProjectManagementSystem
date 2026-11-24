<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "会议详情"; String active = "meeting"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
  String uid = (String) session.getAttribute("uid");
  if (uid == null) uid = "";
  Map<String, String> row = (Map<String, String>) request.getAttribute("row");
  List<Map<String, String>> attendees =
          (List<Map<String, String>>) request.getAttribute("attendees");
  if (attendees == null) attendees = new ArrayList<Map<String, String>>();

  String vId = row.get("id");
  String st = MeetingDao.statusText(row);
  boolean canceled = "CANCELED".equals(row.get("status"));
  boolean ended = "已结束".equals(st);
  String myReply = row.get("my_reply") == null ? "" : row.get("my_reply");
  String myReplyReason = row.get("my_reply_reason") == null ? "" : row.get("my_reply_reason");

  boolean canEdit = !canceled
          && (isAdmin || uid.equals(row.get("organizer_id"))
          || (isPm && ProjectDao.canManage(row.get("project_id"), uid, curRole, false)));
  boolean canReply = "PENDING".equals(myReply) && !canceled && !ended
          && row.get("end_time").compareTo(Db.now()) >= 0;
%>

<div class="page-title">
  <div>
    <h2><%= h(row.get("topic")) %></h2>
    <div class="sub">
      <span class="<%= meetingBadge(st) %>"><%= st %></span>
      <span class="muted">　<%= h(row.get("start_time")) %> ~ <%= h(row.get("end_time")) %></span>
    </div>
  </div>
  <div class="flex">
    <% if (canEdit) { %>
    <a class="btn" href="<%= ctx %>/meeting?action=editForm&id=<%= vId %>">编辑</a>
    <% } %>
    <a class="btn" href="<%= ctx %>/meeting?action=print&id=<%= vId %>" target="_blank">打印</a>
    <a class="btn" href="<%= ctx %>/meeting?action=list">返回</a>
  </div>
</div>

<% if (canceled) { %>
<div class="alert alert-error">
  <b>会议已取消。</b>
  理由：<%= h(row.get("cancel_reason")) %>　
  时间：<%= h(row.get("cancel_time")) %>
</div>
<% } %>

<div class="card">
  <h3>基本信息</h3>
  <table class="grid">
    <tr><th style="width:120px">关联项目</th><td><%= h(row.get("project_name")) %></td></tr>
    <tr><th>组织人</th><td><%= h(row.get("organizer")) %></td></tr>
    <tr><th>地点</th><td><%= h(row.get("location")) %></td></tr>
    <tr><th>参会人数</th>
      <td>应到 <%= h(row.get("attendee_count")) %>，
        同意 <%= h(row.get("accepted_count")) %>，
        请假 <%= h(row.get("declined_count")) %></td></tr>
    <tr><th>附件</th>
      <td>
        <% if (row.get("attach_name") != null && !row.get("attach_name").isEmpty()) { %>
        <a href="<%= ctx %>/meeting?action=attach&id=<%= vId %>"><%= h(row.get("attach_name")) %></a>
        <% } else { %>
        <span class="muted">无</span>
        <% } %>
      </td></tr>
  </table>
</div>

<div class="card">
  <h3>会议议程</h3>
  <div style="white-space:pre-wrap"><%= h(row.get("agenda")) %></div>
</div>

<div class="card">
  <h3>参会人回执</h3>
  <table class="grid">
    <thead>
    <tr><th>参会人</th><th>部门</th><th>回执</th><th>理由</th><th>回执时间</th></tr>
    </thead>
    <tbody>
    <% for (Map<String, String> a : attendees) { %>
    <tr>
      <td><%= h(a.get("user_name")) %></td>
      <td><%= h(a.get("dept_name")) %></td>
      <td><span class="<%= replyBadge(a.get("reply_status")) %>"><%= replyText(a.get("reply_status")) %></span></td>
      <td><%= h(a.get("reply_reason")) %></td>
      <td class="nowrap"><%= h(a.get("reply_time")) %></td>
    </tr>
    <% } %>
    </tbody>
  </table>
</div>

<% if (canReply) { %>
<div class="card">
  <h3>会议回执</h3>
  <form method="post" action="<%= ctx %>/meeting?action=reply&id=<%= vId %>" class="filters">
    <div class="field grow">
      <label>请假理由（请假必填）</label>
      <input type="text" name="reason" maxlength="200">
    </div>
    <button class="btn btn-primary" type="submit" name="decision" value="accept">参加</button>
    <button class="btn btn-danger" type="submit" name="decision" value="decline">请假</button>
  </form>
</div>
<% } else if ("ACCEPTED".equals(myReply)) { %>
<div class="alert alert-ok">你已回执：参加</div>
<% } else if ("DECLINED".equals(myReply)) { %>
<div class="alert alert-info">你已回执：请假（<%= h(myReplyReason) %>）</div>
<% } %>

<% if (canEdit) { %>
<div class="card">
  <h3>取消会议</h3>
  <form method="post" action="<%= ctx %>/meeting?action=cancel&id=<%= vId %>"
        class="filters" onsubmit="return confirm('取消后不可恢复，确定？')">
    <div class="field grow">
      <label>取消理由 *</label>
      <input type="text" name="reason" required maxlength="200">
    </div>
    <button class="btn btn-danger" type="submit">取消会议</button>
  </form>
</div>
<% } %>