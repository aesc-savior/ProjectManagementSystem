<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "会议表单"; String active = "meeting"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%!
  private String nvl(String v) {
    return v == null ? "" : v;
  }
%>
<%
  Map<String, String> row = (Map<String, String>) request.getAttribute("row");
  boolean hasRow = row != null;
  String vId = hasRow ? nvl(row.get("id")) : "";
  boolean edit = !vId.isEmpty();

  String vTopic = hasRow ? nvl(row.get("topic")) : "";
  String vProjectId = hasRow ? nvl(row.get("project_id")) : "0";
  if (vProjectId.isEmpty()) vProjectId = "0";
  String vStart = hasRow ? nvl(row.get("start_time")) : "";
  String vEnd = hasRow ? nvl(row.get("end_time")) : "";
  String vLocation = hasRow ? nvl(row.get("location")) : "";
  String vAgenda = hasRow ? nvl(row.get("agenda")) : "";
  String vAttach = hasRow ? nvl(row.get("attach_name")) : "";

  List<Map<String, String>> projects = (List<Map<String, String>>) request.getAttribute("projects");
  if (projects == null) projects = new ArrayList<Map<String, String>>();
  List<Map<String, String>> users = (List<Map<String, String>>) request.getAttribute("users");
  if (users == null) users = new ArrayList<Map<String, String>>();
  List<String> selectedIds = (List<String>) request.getAttribute("selectedIds");
  if (selectedIds == null) selectedIds = new ArrayList<String>();
  List<Map<String, String>> conflicts =
          (List<Map<String, String>>) request.getAttribute("conflicts");
%>

<div class="page-title">
  <div>
    <h2><%= edit ? "编辑会议" : "新建会议" %></h2>
    <div class="sub">创建、变更、取消后会自动通知参会人</div>
  </div>
  <div class="flex">
    <a class="btn" href="<%= ctx %>/meeting?action=list">返回列表</a>
  </div>
</div>

<% if (conflicts != null && !conflicts.isEmpty()) { %>
<div class="alert alert-error">
  <b>时间冲突：</b>
  <ul>
    <% for (Map<String, String> c : conflicts) { %>
    <li><%= h(c.get("user_name")) %>：<%= h(c.get("topic")) %>
      （<%= h(c.get("start_time")) %> ~ <%= h(c.get("end_time")) %>）</li>
    <% } %>
  </ul>
  确认继续请点「仍然保存」。
</div>
<% } %>

<form method="post" action="<%= ctx %>/meeting?action=save" enctype="multipart/form-data">
  <input type="hidden" name="id" value="<%= h(vId) %>">
  <div class="form-grid">
    <div class="field full">
      <label>会议主题 *</label>
      <input type="text" name="topic" required maxlength="200" value="<%= h(vTopic) %>">
    </div>
    <div class="field">
      <label>关联项目</label>
      <select name="projectId">
        <option value="0">不关联</option>
        <% for (Map<String, String> p : projects) { %>
        <option value="<%= p.get("id") %>"
                <%= p.get("id").equals(vProjectId) ? "selected" : "" %>>
          <%= h(p.get("name")) %>
        </option>
        <% } %>
      </select>
    </div>
    <div class="field">
      <label>地点</label>
      <input type="text" name="location" maxlength="200" value="<%= h(vLocation) %>">
    </div>
    <div class="field">
      <label>开始时间 *</label>
      <input type="datetime-local" name="startTime" required
             value="<%= h(MeetingDao.inputTime(vStart)) %>">
    </div>
    <div class="field">
      <label>结束时间 *</label>
      <input type="datetime-local" name="endTime" required
             value="<%= h(MeetingDao.inputTime(vEnd)) %>">
    </div>
    <div class="field full">
      <label>参会人 *</label>
      <select name="attendeeIds" multiple size="10">
        <% for (Map<String, String> u : users) { %>
        <option value="<%= u.get("id") %>"
                <%= selectedIds.contains(u.get("id")) ? "selected" : "" %>>
          <%= h(u.get("user_name")) %>（<%= h(u.get("dept_name")) %>）
        </option>
        <% } %>
      </select>
      <div class="hint">按住 Ctrl 多选；组织者默认参会。</div>
    </div>
    <div class="field full">
      <label>会议议程</label>
      <textarea name="agenda" rows="6"><%= h(vAgenda) %></textarea>
    </div>
    <div class="field full">
      <label>附件</label>
      <input type="file" name="attach">
      <% if (edit && !vAttach.isEmpty()) { %>
      <div class="hint">
        已有附件：<%= h(vAttach) %>；重新上传会覆盖。
        <button class="btn btn-sm btn-danger" type="submit"
                formaction="<%= ctx %>/meeting?action=deleteAttach&id=<%= h(vId) %>"
                formenctype="application/x-www-form-urlencoded"
                formnovalidate
                onclick="return confirm('确定删除附件？')">删除附件</button>
      </div>
      <% } %>
    </div>
  </div>
  <div class="flex" style="margin-top:16px">
    <button class="btn btn-primary" type="submit">保存</button>
    <% if (conflicts != null && !conflicts.isEmpty()) { %>
    <button class="btn btn-danger" type="submit" name="force" value="1">仍然保存</button>
    <% } %>
    <a class="btn" href="<%= ctx %>/meeting?action=list">取消</a>
  </div>
</form>