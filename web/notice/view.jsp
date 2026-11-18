<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "公告详情"; String active = "notice"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    Map<String, String> row = (Map<String, String>) request.getAttribute("row");
    if (row == null) {
%>
<div class="card"><div class="alert alert-error">公告不存在</div>
    <a class="btn" href="<%= ctx %>/notice?action=list">返回列表</a></div>
<% } else {
    boolean signed = !"0".equals(row.get("signed"));
%>
<div class="page-title">
    <div>
        <h2><%= h(row.get("title")) %></h2>
        <div class="sub">
            <%= h(row.get("publisher")) %> 发布于 <%= h(row.get("create_time")) %>
            　·　<%= "ALL".equals(row.get("scope")) ? "全员" : "项目：" + h(row.get("project_name")) %>
            　·　已签收 <%= h(row.get("read_count")) %> 人
        </div>
    </div>
    <a class="btn" href="<%= ctx %>/notice?action=list">返回列表</a>
</div>

<div class="card">
    <div><%= h(row.get("content")).replace("\n", "<br>") %></div>
    <% if (!row.get("expire_time").isEmpty()) { %>
    <div class="hint mt">有效期至：<%= h(row.get("expire_time")) %></div>
    <% } %>
</div>

<div class="card">
    <h3>签收</h3>
    <% if (signed) { %>
    <div class="alert alert-info">你已签收该公告。</div>
    <% } else { %>
    <form method="post" action="<%= ctx %>/notice?action=sign&id=<%= row.get("id") %>">
        <button class="btn btn-primary" type="submit">确认签收</button>
    </form>
    <% } %>
</div>
<% } %>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
