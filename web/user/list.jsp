<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<% String pageTitle = "用户管理"; String active = "user"; %>
<%@ include file="/WEB-INF/inc/head.jsp" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    String fKeyword = (String) request.getAttribute("fKeyword");
    if (fKeyword == null) fKeyword = "";
%>
<div class="page-title">
    <div>
        <h2>用户管理</h2>
        <div class="sub">共 <%= rows.size() %> 个账号；停用后无法登录，管理员账号不可删除。</div>
    </div>
    <a class="btn btn-primary" href="<%= ctx %>/user?action=addForm">+ 新增用户</a>
</div>

<div class="card">
    <form class="filters" method="get" action="<%= ctx %>/user">
        <input type="hidden" name="action" value="list">
        <div class="field grow">
            <label>关键词（姓名 / 账号 / 部门）</label>
            <input type="text" name="keyword" value="<%= h(fKeyword) %>">
        </div>
        <div class="field" style="width:100px">
            <label>&nbsp;</label>
            <button class="btn btn-primary" type="submit">查询</button>
        </div>
    </form>
</div>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">没有符合条件的用户。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr><th>姓名</th><th>登录账号</th><th>角色</th><th>部门</th><th>密级</th><th>状态</th><th>创建时间</th><th>操作</th></tr>
        </thead>
        <tbody>
        <% for (Map<String, String> u : rows) { %>
        <tr>
            <td><%= h(u.get("user_name")) %></td>
            <td><%= h(u.get("login_id")) %></td>
            <td><%= roleTextOf(u.get("role_code")) %></td>
            <td><%= h(u.get("dept_name")) %></td>
            <td><%= secretText(u.get("secret_level")) %></td>
            <td>
                <span class="<%= "NORMAL".equals(u.get("status")) ? "badge badge-normal" : "badge badge-disabled" %>">
                    <%= "NORMAL".equals(u.get("status")) ? "正常" : "停用" %>
                </span>
            </td>
            <td class="nowrap"><%= h(u.get("create_time")) %></td>
            <td class="ops">
                <a class="btn btn-sm" href="<%= ctx %>/user?action=editForm&id=<%= u.get("id") %>">编辑</a>
                <form method="post" action="<%= ctx %>/user?action=toggle&id=<%= u.get("id") %>" style="display:inline">
                    <button class="btn btn-sm" type="submit"><%= "NORMAL".equals(u.get("status")) ? "停用" : "启用" %></button>
                </form>
                <form method="post" action="<%= ctx %>/user?action=delete&id=<%= u.get("id") %>"
                      style="display:inline" onsubmit="return confirm('确认删除该用户？')">
                    <button class="btn btn-sm btn-danger" type="submit">删除</button>
                </form>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jsp" %>
