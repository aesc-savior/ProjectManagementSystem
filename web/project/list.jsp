<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "项目管理"; String active = "project"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    long total = request.getAttribute("total") == null ? 0 : (Long) request.getAttribute("total");
    int pageNo = request.getAttribute("page") == null ? 1 : (Integer) request.getAttribute("page");
    int pages = request.getAttribute("pages") == null ? 1 : (Integer) request.getAttribute("pages");
    String fKeyword = (String) request.getAttribute("fKeyword");
    String fStatus = (String) request.getAttribute("fStatus");
    String fType = (String) request.getAttribute("fType");
    String fPriority = (String) request.getAttribute("fPriority");
    String fSort = (String) request.getAttribute("fSort");
    if (fKeyword == null) fKeyword = "";
    if (fStatus == null) fStatus = "";
    if (fType == null) fType = "";
    if (fPriority == null) fPriority = "";
    if (fSort == null) fSort = "";
    String uid = (String) session.getAttribute("uid");
    String qs = "&keyword=" + java.net.URLEncoder.encode(fKeyword, "UTF-8")
            + "&status=" + fStatus + "&projectType=" + fType + "&priority=" + fPriority + "&sort=" + fSort;
%>
<div class="page-title">
    <div>
        <h2>项目管理</h2>
        <div class="sub">共 <b><%= total %></b> 个项目；支持组合查询与自定义排序（排序不依赖表头点击）。</div>
    </div>
    <% if (isPm) { %>
    <a class="btn btn-primary" href="<%= ctx %>/project?action=addForm">+ 新建项目</a>
    <% } %>
</div>

<div class="card">
    <form class="filters" method="get" action="<%= ctx %>/project">
        <input type="hidden" name="action" value="list">
        <div class="field grow">
            <label>关键词（名称 / 编号）</label>
            <input type="text" name="keyword" value="<%= h(fKeyword) %>" placeholder="例如 官网">
        </div>
        <div class="field">
            <label>状态</label>
            <select name="status">
                <option value="">全部</option>
                <option value="PENDING" <%= "PENDING".equals(fStatus) ? "selected" : "" %>>待启动</option>
                <option value="RUNNING" <%= "RUNNING".equals(fStatus) ? "selected" : "" %>>进行中</option>
                <option value="PAUSED" <%= "PAUSED".equals(fStatus) ? "selected" : "" %>>暂停</option>
                <option value="DONE" <%= "DONE".equals(fStatus) ? "selected" : "" %>>已完成</option>
                <option value="CANCELED" <%= "CANCELED".equals(fStatus) ? "selected" : "" %>>已取消</option>
                <option value="ARCHIVED" <%= "ARCHIVED".equals(fStatus) ? "selected" : "" %>>已归档</option>
            </select>
        </div>
        <div class="field">
            <label>类型</label>
            <select name="projectType">
                <option value="">全部</option>
                <option value="RND" <%= "RND".equals(fType) ? "selected" : "" %>>研发</option>
                <option value="BUILD" <%= "BUILD".equals(fType) ? "selected" : "" %>>建设</option>
                <option value="SERVICE" <%= "SERVICE".equals(fType) ? "selected" : "" %>>服务</option>
                <option value="OTHER" <%= "OTHER".equals(fType) ? "selected" : "" %>>其他</option>
            </select>
        </div>
        <div class="field">
            <label>优先级</label>
            <select name="priority">
                <option value="">全部</option>
                <option value="HIGH" <%= "HIGH".equals(fPriority) ? "selected" : "" %>>高</option>
                <option value="MID" <%= "MID".equals(fPriority) ? "selected" : "" %>>中</option>
                <option value="LOW" <%= "LOW".equals(fPriority) ? "selected" : "" %>>低</option>
            </select>
        </div>
        <div class="field">
            <label>排序</label>
            <select name="sort">
                <option value="" <%= fSort.isEmpty() ? "selected" : "" %>>优先级 + 截止时间</option>
                <option value="end" <%= "end".equals(fSort) ? "selected" : "" %>>截止时间最早</option>
                <option value="budget" <%= "budget".equals(fSort) ? "selected" : "" %>>预算从高到低</option>
                <option value="name" <%= "name".equals(fSort) ? "selected" : "" %>>名称 A→Z</option>
                <option value="new" <%= "new".equals(fSort) ? "selected" : "" %>>最新创建</option>
            </select>
        </div>
        <div class="field">
            <label>&nbsp;</label>
            <div class="flex">
                <button class="btn btn-primary" type="submit">查询</button>
                <a class="btn" href="<%= ctx %>/project?action=list">重置</a>
            </div>
        </div>
    </form>
</div>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">没有符合条件的项目。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr>
            <th>编号</th><th>项目名称</th><th>类型</th><th>优先级</th><th>状态</th>
            <th>负责人</th><th>进度</th><th>截止</th><th>操作</th>
        </tr>
        </thead>
        <tbody>
        <% for (Map<String, String> r : rows) {
            int prog = 0;
            try { prog = Integer.parseInt(r.get("progress")); } catch (Exception ignore) { }
            String pid = r.get("id");
            boolean can = ProjectDao.canManage(pid, uid, curRole, isAdmin);
        %>
        <tr>
            <td class="nowrap mono"><%= h(r.get("project_no")) %></td>
            <td><a href="<%= ctx %>/project?action=view&id=<%= pid %>"><%= h(r.get("name")) %></a></td>
            <td><%= typeText(r.get("project_type")) %></td>
            <td><span class="<%= prioBadge(r.get("priority")) %>"><%= prioText(r.get("priority")) %></span></td>
            <td><span class="<%= projStatusBadge(r.get("status")) %>"><%= projStatusText(r.get("status")) %></span></td>
            <td><%= h(r.get("owner_name")) %></td>
            <td><div class="progress"><i style="width:<%= prog %>%"></i></div></td>
            <td class="nowrap"><%= h(r.get("end_date")) %></td>
            <td class="ops">
                <a class="btn btn-sm" href="<%= ctx %>/project?action=view&id=<%= pid %>">查看</a>
                <% if (can) { %>
                <a class="btn btn-sm" href="<%= ctx %>/project?action=editForm&id=<%= pid %>">编辑</a>
                <% if (!"ARCHIVED".equals(r.get("status"))) { %>
                <form method="post" action="<%= ctx %>/project?action=archive&id=<%= pid %>" style="display:inline"
                      onsubmit="return confirm('归档后项目只读，确认归档？')">
                    <button class="btn btn-sm" type="submit">归档</button>
                </form>
                <% } %>
                <form method="post" action="<%= ctx %>/project?action=delete&id=<%= pid %>" style="display:inline"
                      onsubmit="return confirm('确认删除该项目？')">
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
        <a href="<%= ctx %>/project?action=list&page=<%= pageNo - 1 %><%= qs %>">上一页</a>
        <% } %>
        <% for (int p = Math.max(1, pageNo - 2); p <= Math.min(pages, pageNo + 2); p++) { %>
        <% if (p == pageNo) { %><span class="cur"><%= p %></span><% } else { %>
        <a href="<%= ctx %>/project?action=list&page=<%= p %><%= qs %>"><%= p %></a><% } %>
        <% } %>
        <% if (pageNo < pages) { %>
        <a href="<%= ctx %>/project?action=list&page=<%= pageNo + 1 %><%= qs %>">下一页</a>
        <% } %>
    </div>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
