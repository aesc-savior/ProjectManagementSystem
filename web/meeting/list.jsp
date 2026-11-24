<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "会议管理"; String active = "meeting"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
  List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
  if (rows == null) rows = new ArrayList<Map<String, String>>();
  List<Map<String, String>> projects = (List<Map<String, String>>) request.getAttribute("projects");
  if (projects == null) projects = new ArrayList<Map<String, String>>();
  List<Map<String, String>> users = (List<Map<String, String>>) request.getAttribute("users");
  if (users == null) users = new ArrayList<Map<String, String>>();

  long total = request.getAttribute("total") == null ? 0L
          : ((Number) request.getAttribute("total")).longValue();
  int pageNo = request.getAttribute("page") == null ? 1
          : ((Number) request.getAttribute("page")).intValue();
  int pages = request.getAttribute("pages") == null ? 1
          : ((Number) request.getAttribute("pages")).intValue();
  if (pages < 1) pages = 1;

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

  String uid = (String) session.getAttribute("uid");
  if (uid == null) uid = "";
  String qs = "&projectId=" + fProjectId + "&userId=" + fUserId
          + "&from=" + fFrom + "&to=" + fTo + "&status=" + fStatus
          + "&keyword=" + fKeyword;
%>

<div class="page-title">
  <div>
    <h2>会议管理</h2>
    <div class="sub">共 <b><%= total %></b> 场</div>
  </div>
  <div class="flex">
    <a class="btn" href="<%= ctx %>/meeting?action=export<%= qs %>">导出 Excel</a>
    <a class="btn" href="<%= ctx %>/meeting?action=printList<%= qs %>" target="_blank">打印清单</a>
    <% if (!isViewer) { %>
    <a class="btn btn-primary" href="<%= ctx %>/meeting?action=addForm">+ 新建会议</a>
    <% } %>
  </div>
</div>

<form class="filters" method="get" action="<%= ctx %>/meeting">
  <input type="hidden" name="action" value="list">
  <div class="field">
    <label>项目</label>
    <select name="projectId">
      <option value="">全部</option>
      <% for (Map<String, String> p : projects) { %>
      <option value="<%= p.get("id") %>" <%= p.get("id").equals(fProjectId) ? "selected" : "" %>>
        <%= h(p.get("name")) %>
      </option>
      <% } %>
    </select>
  </div>
  <div class="field">
    <label>参会人</label>
    <select name="userId">
      <option value="">全部</option>
      <% for (Map<String, String> u : users) { %>
      <option value="<%= u.get("id") %>" <%= u.get("id").equals(fUserId) ? "selected" : "" %>>
        <%= h(u.get("user_name")) %>
      </option>
      <% } %>
    </select>
  </div>
  <div class="field">
    <label>开始日期</label>
    <input type="date" name="from" value="<%= h(fFrom) %>">
  </div>
  <div class="field">
    <label>结束日期</label>
    <input type="date" name="to" value="<%= h(fTo) %>">
  </div>
  <div class="field">
    <label>状态</label>
    <select name="status">
      <option value="">全部</option>
      <option value="PUBLISHED" <%= "PUBLISHED".equals(fStatus) ? "selected" : "" %>>已发布</option>
      <option value="CANCELED" <%= "CANCELED".equals(fStatus) ? "selected" : "" %>>已取消</option>
    </select>
  </div>
  <div class="field grow">
    <label>关键词</label>
    <input type="text" name="keyword" value="<%= h(fKeyword) %>" placeholder="主题/地点/议程">
  </div>
  <button class="btn btn-primary" type="submit">查询</button>
  <a class="btn" href="<%= ctx %>/meeting?action=list">重置</a>
</form>

<table class="grid">
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
    <th>操作</th>
  </tr>
  </thead>
  <tbody>
  <% if (rows.isEmpty()) { %>
  <tr><td colspan="10" class="empty">暂无会议</td></tr>
  <% } %>
  <% for (Map<String, String> r : rows) {
    String st = MeetingDao.statusText(r);
    boolean canceled = "CANCELED".equals(r.get("status"));
    boolean ended = "已结束".equals(st);
    boolean canEditRow = !canceled
            && (isAdmin || uid.equals(r.get("organizer_id"))
            || (isPm && ProjectDao.canManage(r.get("project_id"), uid, curRole, false)));
  %>
  <tr>
    <td><a href="<%= ctx %>/meeting?action=view&id=<%= r.get("id") %>"><%= h(r.get("topic")) %></a></td>
    <td><%= h(r.get("project_name")) %></td>
    <td class="nowrap"><%= h(r.get("start_time")) %></td>
    <td class="nowrap"><%= h(r.get("end_time")) %></td>
    <td><%= h(r.get("location")) %></td>
    <td><%= h(r.get("organizer")) %></td>
    <td><%= h(r.get("attendee_count")) %></td>
    <td><%= h(r.get("accepted_count")) %> / <%= h(r.get("declined_count")) %></td>
    <td><span class="<%= meetingBadge(st) %>"><%= st %></span></td>
    <td class="ops">
      <a href="<%= ctx %>/meeting?action=view&id=<%= r.get("id") %>">详情</a>
      <% if (canEditRow) { %>
      <a href="<%= ctx %>/meeting?action=editForm&id=<%= r.get("id") %>">编辑</a>
      <form method="post" action="<%= ctx %>/meeting?action=cancel&id=<%= r.get("id") %>"
            style="display:inline" onsubmit="return confirm('取消后不可恢复，确定？')">
        <input type="text" name="reason" required maxlength="200"
               placeholder="取消理由（必填）" style="width:140px">
        <button class="btn btn-sm btn-danger" type="submit">取消</button>
      </form>
      <% } %>
      <% if ("PENDING".equals(r.get("my_reply")) && !canceled && !ended) { %>
      <a href="<%= ctx %>/meeting?action=view&id=<%= r.get("id") %>">去回执</a>
      <% } %>
    </td>
  </tr>
  <% } %>
  </tbody>
</table>

<% if (pages > 1) { %>
<div class="pager">
  <% if (pageNo > 1) { %>
  <a href="<%= ctx %>/meeting?action=list&page=<%= pageNo - 1 %><%= qs %>">上一页</a>
  <% } %>
  <span>第 <%= pageNo %> / <%= pages %> 页</span>
  <% if (pageNo < pages) { %>
  <a href="<%= ctx %>/meeting?action=list&page=<%= pageNo + 1 %><%= qs %>">下一页</a>
  <% } %>
</div>
<% } %>