<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "通知公告"; String active = "notice"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    String fKeyword = (String) request.getAttribute("fKeyword");
    if (fKeyword == null) fKeyword = "";
%>
<div class="page-title">
    <div>
        <h2>通知公告</h2>
        <div class="sub">共 <b><%= rows.size() %></b> 条；发布后需成员签收，签收情况可在列表中查看。</div>
    </div>
    <% if (isPm) { %>
    <a class="btn btn-primary" href="<%= ctx %>/notice?action=addForm">+ 发布公告</a>
    <% } %>
</div>

<div class="card">
    <form class="filters" method="get" action="<%= ctx %>/notice">
        <input type="hidden" name="action" value="list">
        <div class="field grow">
            <label>关键词</label>
            <input type="text" name="keyword" value="<%= h(fKeyword) %>" placeholder="标题 / 内容">
        </div>
        <div class="field" style="width:100px">
            <label>&nbsp;</label>
            <button class="btn btn-primary" type="submit">查询</button>
        </div>
    </form>
</div>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">还没有公告。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>标题</th><th>范围</th><th>发布人</th><th>发布时间</th><th>签收</th><th>操作</th></tr></thead>
        <tbody>
        <% for (Map<String, String> r : rows) {
            boolean signed = !"0".equals(r.get("signed"));
        %>
        <tr>
            <td><a href="<%= ctx %>/notice?action=view&id=<%= r.get("id") %>"><%= h(r.get("title")) %></a></td>
            <td><%= "ALL".equals(r.get("scope")) ? "全员" : ("PROJECT".equals(r.get("scope")) ? "项目：" + h(r.get("project_name")) : "指定人员") %></td>
            <td><%= h(r.get("publisher")) %></td>
            <td class="nowrap"><%= h(r.get("create_time")) %></td>
            <td>
                <%= signed ? "已签收" : "未签收" %>
                <span class="hint">（<%= h(r.get("read_count")) %> 人）</span>
            </td>
            <td class="ops">
                <a class="btn btn-sm" href="<%= ctx %>/notice?action=view&id=<%= r.get("id") %>">查看</a>
                <% if (!signed) { %>
                <form method="post" action="<%= ctx %>/notice?action=sign&id=<%= r.get("id") %>" style="display:inline">
                    <button class="btn btn-sm btn-primary" type="submit">签收</button>
                </form>
                <% } %>
                <% if (isPm) { %>
                <form method="post" action="<%= ctx %>/notice?action=delete&id=<%= r.get("id") %>" style="display:inline"
                      onsubmit="return confirm('确认删除该公告？')">
                    <button class="btn btn-sm btn-danger" type="submit">删除</button>
                </form>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
