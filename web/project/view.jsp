<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "项目详情"; String active = "project"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    String error = (String) request.getAttribute("error");
    Map<String, String> row = (Map<String, String>) request.getAttribute("row");
    List<Map<String, String>> members = (List<Map<String, String>>) request.getAttribute("members");
    List<Map<String, String>> tasks = (List<Map<String, String>>) request.getAttribute("tasks");
    List<Map<String, String>> users = (List<Map<String, String>>) request.getAttribute("users");
    if (members == null) members = new ArrayList<Map<String, String>>();
    if (tasks == null) tasks = new ArrayList<Map<String, String>>();
    if (users == null) users = new ArrayList<Map<String, String>>();
    long weeklyCount = request.getAttribute("weeklyCount") == null ? 0 : (Long) request.getAttribute("weeklyCount");
    long fineTotal = request.getAttribute("fineTotal") == null ? 0 : (Long) request.getAttribute("fineTotal");
    String uid = (String) session.getAttribute("uid");
%>
<% if (row == null) { %>
<div class="page-title"><h2>项目详情</h2></div>
<div class="card">
    <div class="alert alert-error"><%= h(error == null ? "项目不存在" : error) %></div>
    <a class="btn" href="<%= ctx %>/project?action=list">返回列表</a>
</div>
<% } else {
    boolean can = ProjectDao.canManage(row.get("id"), uid, curRole, isAdmin);
    int prog = 0;
    try { prog = Integer.parseInt(row.get("progress")); } catch (Exception ignore) { }
%>
<div class="page-title">
    <div>
        <h2><span class="mono"><%= h(row.get("project_no")) %></span>　<%= h(row.get("name")) %></h2>
        <div class="sub">
            <span class="<%= projStatusBadge(row.get("status")) %>"><%= projStatusText(row.get("status")) %></span>
            　<%= typeText(row.get("project_type")) %>　·　优先级
            <span class="<%= prioBadge(row.get("priority")) %>"><%= prioText(row.get("priority")) %></span>
            　·　<%= h(row.get("owner_name")) %>　·　<%= h(row.get("dept_name")) %>
        </div>
    </div>
    <div class="flex">
        <a class="btn" href="<%= ctx %>/project?action=list">返回列表</a>
        <% if (can) { %>
        <a class="btn" href="<%= ctx %>/project?action=editForm&id=<%= row.get("id") %>">编辑</a>
        <% if (!"ARCHIVED".equals(row.get("status"))) { %>
        <form method="post" action="<%= ctx %>/project?action=archive&id=<%= row.get("id") %>" style="display:inline"
              onsubmit="return confirm('归档后项目只读，确认归档？')">
            <button class="btn btn-primary" type="submit">归档</button>
        </form>
        <% } %>
        <form method="post" action="<%= ctx %>/project?action=delete&id=<%= row.get("id") %>" style="display:inline"
              onsubmit="return confirm('确认删除该项目？')">
            <button class="btn btn-danger" type="submit">删除</button>
        </form>
        <% } %>
    </div>
</div>

<div class="stats">
    <div class="stat"><div class="label">整体进度</div><div class="value"><%= prog %>%</div><div class="hint">按任务平均进度汇总</div></div>
    <div class="stat"><div class="label">任务数</div><div class="value"><%= h(row.get("task_count")) %></div><div class="hint">已完成 <%= h(row.get("done_count")) %> 个</div></div>
    <div class="stat"><div class="label">周报数</div><div class="value"><%= weeklyCount %></div><div class="hint">含草稿与已提交</div></div>
    <div class="stat"><div class="label">罚款合计</div><div class="value"><%= money(String.valueOf(fineTotal)) %></div><div class="hint">元</div></div>
</div>

<div class="card">
    <h3>基本信息</h3>
    <div class="detail-grid">
        <table class="grid">
            <tbody>
            <tr><th style="width:110px">项目编号</th><td class="mono"><%= h(row.get("project_no")) %></td></tr>
            <tr><th>项目类型</th><td><%= typeText(row.get("project_type")) %></td></tr>
            <tr><th>开始日期</th><td><%= h(row.get("start_date")) %></td></tr>
            <tr><th>结束日期</th><td><%= h(row.get("end_date")) %></td></tr>
            </tbody>
        </table>
        <table class="grid">
            <tbody>
            <tr><th style="width:110px">预算</th><td><%= money(row.get("budget")) %> 元</td></tr>
            <tr><th>负责人</th><td><%= h(row.get("owner_name")) %></td></tr>
            <tr><th>创建人</th><td><%= h(row.get("creator")) %></td></tr>
            <tr><th>创建时间</th><td><%= h(row.get("create_time")) %></td></tr>
            </tbody>
        </table>
    </div>
    <% if (!row.get("description").isEmpty()) { %>
    <div class="mt"><b>描述：</b><%= h(row.get("description")) %></div>
    <% } %>
    <% if (can) { %>
    <form class="flex mt" method="post" action="<%= ctx %>/project?action=status&id=<%= row.get("id") %>">
        <div class="field" style="width:150px">
            <label>变更状态</label>
            <select name="status">
                <option value="PENDING" <%= "PENDING".equals(row.get("status")) ? "selected" : "" %>>待启动</option>
                <option value="RUNNING" <%= "RUNNING".equals(row.get("status")) ? "selected" : "" %>>进行中</option>
                <option value="PAUSED" <%= "PAUSED".equals(row.get("status")) ? "selected" : "" %>>暂停</option>
                <option value="DONE" <%= "DONE".equals(row.get("status")) ? "selected" : "" %>>已完成</option>
                <option value="CANCELED" <%= "CANCELED".equals(row.get("status")) ? "selected" : "" %>>已取消</option>
            </select>
        </div>
        <div class="field" style="width:110px">
            <label>&nbsp;</label>
            <button class="btn" type="submit">更新状态</button>
        </div>
    </form>
    <% } %>
</div>

<div class="card">
    <h3>项目成员（<%= members.size() %>）</h3>
    <% if (members.isEmpty()) { %>
    <div class="empty">还没有成员。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>姓名</th><th>账号</th><th>部门</th><th>项目角色</th><th>加入日期</th><th>操作</th></tr></thead>
        <tbody>
        <% for (Map<String, String> m : members) { %>
        <tr>
            <td><%= h(m.get("user_name")) %></td>
            <td class="mono"><%= h(m.get("login_id")) %></td>
            <td><%= h(m.get("dept_name")) %></td>
            <td>
                <%= "MANAGER".equals(m.get("role_code")) ? "项目管理员"
                        : ("VIEWER".equals(m.get("role_code")) ? "查看者" : "成员") %>
            </td>
            <td><%= h(m.get("join_date")) %></td>
            <td class="ops">
                <% if (can) { %>
                <form method="post" action="<%= ctx %>/project?action=memberRemove&id=<%= row.get("id") %>" style="display:inline"
                      onsubmit="return confirm('确认移除该成员？')">
                    <input type="hidden" name="userId" value="<%= m.get("user_id") %>">
                    <button class="btn btn-sm btn-danger" type="submit">移除</button>
                </form>
                <% } else { %>—<% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
    <% if (can) { %>
    <form class="flex mt" method="post" action="<%= ctx %>/project?action=memberAdd&id=<%= row.get("id") %>">
        <div class="field grow">
            <label>添加成员</label>
            <select name="userId">
                <% for (Map<String, String> u : users) { %>
                <option value="<%= u.get("id") %>"><%= h(u.get("user_name")) %>（<%= roleTextOf(u.get("role_code")) %>）</option>
                <% } %>
            </select>
        </div>
        <div class="field" style="width:140px">
            <label>项目角色</label>
            <select name="memberRole">
                <option value="MEMBER">成员</option>
                <option value="MANAGER">项目管理员</option>
                <option value="VIEWER">查看者</option>
            </select>
        </div>
        <div class="field" style="width:100px">
            <label>&nbsp;</label>
            <button class="btn btn-primary" type="submit">添加</button>
        </div>
    </form>
    <% } %>
</div>

<div class="card">
    <h3>任务（最近 12 条）</h3>
    <% if (tasks.isEmpty()) { %>
    <div class="empty">还没有任务。<a href="<%= ctx %>/task?action=addForm&projectId=<%= row.get("id") %>">去创建 →</a></div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>任务</th><th>负责人</th><th>状态</th><th>进度</th><th>截止</th></tr></thead>
        <tbody>
        <% for (Map<String, String> t : tasks) {
            int tp = 0;
            try { tp = Integer.parseInt(t.get("progress")); } catch (Exception ignore) { }
        %>
        <tr>
            <td><a href="<%= ctx %>/task?action=editForm&id=<%= t.get("id") %>"><%= h(t.get("title")) %></a></td>
            <td><%= h(t.get("assignee")) %></td>
            <td><span class="<%= taskStatusBadge(t.get("status")) %>"><%= taskStatusText(t.get("status")) %></span></td>
            <td><div class="progress"><i style="width:<%= tp %>%"></i></div></td>
            <td class="nowrap"><%= h(t.get("due_date")) %></td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>
<% } %>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
