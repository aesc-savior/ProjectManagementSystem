<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="archive.dao.*,java.util.*" %>
<% String pageTitle = "首页概览"; String active = "home"; %>
<%@ include file="/WEB-INF/inc/head.jsp" %>
<%
    Map<String, String> s = ArchiveDao.stats();
    List<Map<String, String>> recent = ArchiveDao.list("", "", "", "", "", 1, 6);
%>
<div class="page-title">
    <div>
        <h2>你好，<%= h(curName) %></h2>
        <div class="sub">这是档案管理系统的概览页，数据实时来自数据库。</div>
    </div>
    <% if ("ADMIN".equals(curRole) || "ARCHIVIST".equals(curRole)) { %>
    <a class="btn btn-primary" href="<%= ctx %>/archive?action=addForm">+ 新增档案</a>
    <% } %>
</div>

<div class="stats">
    <div class="stat">
        <div class="label">档案总量</div>
        <div class="value"><%= s.get("total") %></div>
        <div class="hint">含待归档、已归档、借出中</div>
    </div>
    <div class="stat">
        <div class="label">已归档</div>
        <div class="value"><%= s.get("archived") %></div>
        <div class="hint">可借阅、可下载</div>
    </div>
    <div class="stat">
        <div class="label">借出中</div>
        <div class="value"><%= s.get("borrowed") %></div>
        <div class="hint">待归还</div>
    </div>
    <div class="stat">
        <div class="label">待审批借阅</div>
        <div class="value"><%= s.get("borrowPending") %></div>
        <div class="hint"><a href="<%= ctx %>/borrow?action=list">去处理 →</a></div>
    </div>
</div>

<div class="card">
    <h3>最近档案</h3>
    <% if (recent.isEmpty()) { %>
    <div class="empty">还没有档案，先新增一条试试。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr>
            <th>档号</th><th>档案名称</th><th>分类</th><th>密级</th><th>保管期限</th><th>状态</th><th>归档时间</th>
        </tr>
        </thead>
        <tbody>
        <% for (Map<String, String> r : recent) { %>
        <tr>
            <td><a href="<%= ctx %>/archive?action=view&id=<%= r.get("id") %>"><%= h(r.get("archive_no")) %></a></td>
            <td><%= h(r.get("title")) %></td>
            <td><%= h(r.get("category_name")) %></td>
            <td><%= secretText(r.get("secret_level")) %></td>
            <td><%= keepText(r.get("keep_years")) %></td>
            <td><span class="<%= statusBadge(r.get("status")) %>"><%= statusText(r.get("status")) %></span></td>
            <td class="nowrap"><%= h(r.get("archive_time")) %></td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <div class="mt right"><a href="<%= ctx %>/archive?action=list">查看全部 →</a></div>
    <% } %>
</div>

<div class="card">
    <h3>当前角色的能力</h3>
    <table class="grid">
        <thead><tr><th>角色</th><th>可以做什么</th><th>不能做什么</th></tr></thead>
        <tbody>
        <tr><td>系统管理员</td><td>用户管理、系统配置、查看全部日志、所有档案操作</td><td>—</td></tr>
        <tr><td>档案管理员</td><td>新增/编辑/归档档案、上传附件、审批借阅、查看日志</td><td>管理用户与角色</td></tr>
        <tr><td>部门用户</td><td>检索、查看、下载（有权限时）、申请借阅</td><td>增删改档案、审批</td></tr>
        </tbody>
    </table>
    <div class="hint mt">说明：菜单与按钮的显隐由 JSP 按角色控制，真正的拦截在 Servlet 与过滤器里，改前端绕不过去。</div>
</div>
<%@ include file="/WEB-INF/inc/foot.jsp" %>
