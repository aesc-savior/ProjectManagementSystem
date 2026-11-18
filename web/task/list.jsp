<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "任务管理"; String active = "task"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    List<Map<String, String>> projects = (List<Map<String, String>>) request.getAttribute("projects");
    if (projects == null) projects = new ArrayList<Map<String, String>>();
    List<Map<String, String>> users = (List<Map<String, String>>) request.getAttribute("users");
    if (users == null) users = new ArrayList<Map<String, String>>();
    long total = request.getAttribute("total") == null ? 0 : (Long) request.getAttribute("total");
    int pageNo = request.getAttribute("page") == null ? 1 : (Integer) request.getAttribute("page");
    int pages = request.getAttribute("pages") == null ? 1 : (Integer) request.getAttribute("pages");
    String fKeyword = (String) request.getAttribute("fKeyword");
    String fProjectId = (String) request.getAttribute("fProjectId");
    String fAssigneeId = (String) request.getAttribute("fAssigneeId");
    String fStatus = (String) request.getAttribute("fStatus");
    String fSort = (String) request.getAttribute("fSort");
    if (fKeyword == null) fKeyword = "";
    if (fProjectId == null) fProjectId = "";
    if (fAssigneeId == null) fAssigneeId = "";
    if (fStatus == null) fStatus = "";
    if (fSort == null) fSort = "";
    String uid = (String) session.getAttribute("uid");
    String qs = "&keyword=" + java.net.URLEncoder.encode(fKeyword, "UTF-8")
            + "&projectId=" + fProjectId + "&assigneeId=" + fAssigneeId + "&status=" + fStatus + "&sort=" + fSort;
%>
<div class="page-title">
    <div>
        <h2>任务管理</h2>
        <div class="sub">共 <b><%= total %></b> 个任务；支持按项目、负责人、状态筛选，父任务进度由子任务自动汇总。</div>
    </div>
    <% if (isPm && !projects.isEmpty()) { %>
    <form method="get" action="<%= ctx %>/task" class="flex">
        <input type="hidden" name="action" value="addForm">
        <select name="projectId" style="min-width:220px">
            <% for (Map<String, String> p : projects) { %>
            <option value="<%= p.get("id") %>" <%= p.get("id").equals(fProjectId) ? "selected" : "" %>><%= h(p.get("name")) %></option>
            <% } %>
        </select>
        <button class="btn btn-primary" type="submit">+ 新建任务</button>
    </form>
    <% } %>
</div>

<div class="card">
    <form class="filters" method="get" action="<%= ctx %>/task">
        <input type="hidden" name="action" value="list">
        <div class="field grow">
            <label>关键词</label>
            <input type="text" name="keyword" value="<%= h(fKeyword) %>" placeholder="任务标题 / 描述">
        </div>
        <div class="field">
            <label>项目</label>
            <select name="projectId">
                <option value="">全部</option>
                <% for (Map<String, String> p : projects) { %>
                <option value="<%= p.get("id") %>" <%= p.get("id").equals(fProjectId) ? "selected" : "" %>><%= h(p.get("name")) %></option>
                <% } %>
            </select>
        </div>
        <div class="field">
            <label>负责人</label>
            <select name="assigneeId">
                <option value="">全部</option>
                <% for (Map<String, String> u : users) { %>
                <option value="<%= u.get("id") %>" <%= u.get("id").equals(fAssigneeId) ? "selected" : "" %>><%= h(u.get("user_name")) %></option>
                <% } %>
            </select>
        </div>
        <div class="field">
            <label>状态</label>
            <select name="status">
                <option value="">全部</option>
                <option value="TODO" <%= "TODO".equals(fStatus) ? "selected" : "" %>>待开始</option>
                <option value="DOING" <%= "DOING".equals(fStatus) ? "selected" : "" %>>进行中</option>
                <option value="PAUSED" <%= "PAUSED".equals(fStatus) ? "selected" : "" %>>暂停</option>
                <option value="DONE" <%= "DONE".equals(fStatus) ? "selected" : "" %>>已完成</option>
                <option value="CANCELED" <%= "CANCELED".equals(fStatus) ? "selected" : "" %>>已取消</option>
            </select>
        </div>
        <div class="field">
            <label>排序</label>
            <select name="sort">
                <option value="" <%= fSort.isEmpty() ? "selected" : "" %>>项目 + WBS</option>
                <option value="due" <%= "due".equals(fSort) ? "selected" : "" %>>截止时间最早</option>
                <option value="progress" <%= "progress".equals(fSort) ? "selected" : "" %>>进度从高到低</option>
            </select>
        </div>
        <div class="field">
            <label>&nbsp;</label>
            <div class="flex">
                <button class="btn btn-primary" type="submit">查询</button>
                <a class="btn" href="<%= ctx %>/task?action=list">重置</a>
            </div>
        </div>
    </form>
</div>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">没有符合条件的任务。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr><th>任务</th><th>项目</th><th>负责人</th><th>优先级</th><th>状态</th><th>进度</th><th>截止</th><th>操作</th></tr>
        </thead>
        <tbody>
        <% for (Map<String, String> r : rows) {
            int prog = 0;
            try { prog = Integer.parseInt(r.get("progress")); } catch (Exception ignore) { }
            boolean sub = !"0".equals(r.get("parent_id"));
            String tid = r.get("id");
            boolean canEdit = isPm || uid.equals(r.get("assignee_id"));
        %>
        <tr>
            <td>
                <%= sub ? "└ " : "" %><a href="<%= ctx %>/task?action=editForm&id=<%= tid %>"><%= h(r.get("title")) %></a>
                <% if (sub) { %><div class="hint">父任务：<%= h(r.get("parent_title")) %></div><% } %>
            </td>
            <td><a href="<%= ctx %>/project?action=view&id=<%= r.get("project_id") %>"><%= h(r.get("project_name")) %></a></td>
            <td><%= h(r.get("assignee")) %></td>
            <td><span class="<%= prioBadge(r.get("priority")) %>"><%= prioText(r.get("priority")) %></span></td>
            <td><span class="<%= taskStatusBadge(r.get("status")) %>"><%= taskStatusText(r.get("status")) %></span></td>
            <td><div class="progress"><i style="width:<%= prog %>%"></i></div><div class="hint"><%= prog %>%</div></td>
            <td class="nowrap"><%= h(r.get("due_date")) %></td>
            <td class="ops">
                <% if (canEdit) { %>
                <a class="btn btn-sm" href="<%= ctx %>/task?action=editForm&id=<%= tid %>">编辑</a>
                <% } %>
                <% if (isPm) { %>
                <form method="post" action="<%= ctx %>/task?action=delete&id=<%= tid %>" style="display:inline"
                      onsubmit="return confirm('确认删除该任务？')">
                    <button class="btn btn-sm btn-danger" type="submit">删除</button>
                </form>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <div class="pager">
        <span class="info">第 <%= pageNo %> / <%= Math.max(pages, 1) %> 页，共 <%= total %> 条</span>
        <% if (pageNo > 1) { %>
        <a href="<%= ctx %>/task?action=list&page=<%= pageNo - 1 %><%= qs %>">上一页</a>
        <% } %>
        <% for (int p = Math.max(1, pageNo - 2); p <= Math.min(pages, pageNo + 2); p++) { %>
        <% if (p == pageNo) { %><span class="cur"><%= p %></span><% } else { %>
        <a href="<%= ctx %>/task?action=list&page=<%= p %><%= qs %>"><%= p %></a><% } %>
        <% } %>
        <% if (pageNo < pages) { %>
        <a href="<%= ctx %>/task?action=list&page=<%= pageNo + 1 %><%= qs %>">下一页</a>
        <% } %>
    </div>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
