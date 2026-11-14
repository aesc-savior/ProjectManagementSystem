<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<% String pageTitle = "借阅管理"; String active = "borrow"; %>
<%@ include file="/WEB-INF/inc/head.jsp" %>
<%
    List<Map<String, String>> mine = (List<Map<String, String>>) request.getAttribute("mine");
    List<Map<String, String>> pending = (List<Map<String, String>>) request.getAttribute("pending");
    List<Map<String, String>> all = (List<Map<String, String>>) request.getAttribute("all");
    if (mine == null) mine = new ArrayList<Map<String, String>>();
    boolean staff = "ADMIN".equals(curRole) || "ARCHIVIST".equals(curRole);
%>
<div class="page-title">
    <div>
        <h2>借阅管理</h2>
        <div class="sub">申请 → 审批 → 借出 → 归还；审批通过后档案状态自动变为“借出中”。</div>
    </div>
</div>

<% if (staff && pending != null && !pending.isEmpty()) { %>
<div class="card">
    <h3>待我审批（<%= pending.size() %>）</h3>
    <table class="grid">
        <thead>
        <tr><th>档案</th><th>申请人</th><th>原因</th><th>期限</th><th>提交时间</th><th>操作</th></tr>
        </thead>
        <tbody>
        <% for (Map<String, String> b : pending) { %>
        <tr>
            <td><a href="<%= ctx %>/archive?action=view&id=<%= b.get("archive_id") %>"><%= h(b.get("archive_no")) %>　<%= h(b.get("title")) %></a></td>
            <td><%= h(b.get("user_name")) %></td>
            <td><%= h(b.get("reason")) %></td>
            <td class="nowrap"><%= h(b.get("start_date")) %> ～ <%= h(b.get("end_date")) %></td>
            <td class="nowrap"><%= h(b.get("create_time")) %></td>
            <td class="ops">
                <form method="post" action="<%= ctx %>/borrow?action=approve&id=<%= b.get("id") %>" style="display:inline">
                    <button class="btn btn-sm btn-primary" type="submit">通过</button>
                </form>
                <form method="post" action="<%= ctx %>/borrow?action=reject&id=<%= b.get("id") %>"
                      style="display:inline" onsubmit="return confirm('确认驳回该申请？')">
                    <button class="btn btn-sm btn-danger" type="submit">驳回</button>
                </form>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
</div>
<% } %>

<div class="card">
    <h3>我的借阅（<%= mine.size() %>）</h3>
    <% if (mine.isEmpty()) { %>
    <div class="empty">还没有借阅记录。到档案详情页点击“申请借阅”。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr><th>档案</th><th>原因</th><th>期限</th><th>状态</th><th>审批人</th><th>归还时间</th><th>操作</th></tr>
        </thead>
        <tbody>
        <% for (Map<String, String> b : mine) { %>
        <tr>
            <td><a href="<%= ctx %>/archive?action=view&id=<%= b.get("archive_id") %>"><%= h(b.get("archive_no")) %>　<%= h(b.get("title")) %></a></td>
            <td><%= h(b.get("reason")) %></td>
            <td class="nowrap"><%= h(b.get("start_date")) %> ～ <%= h(b.get("end_date")) %>
                <% if ("1".equals(b.get("overdue"))) { %><span class="badge badge-rejected">逾期</span><% } %>
            </td>
            <td><span class="<%= borrowBadge(b.get("status")) %>"><%= borrowText(b.get("status")) %></span></td>
            <td><%= h(b.get("approver")) %></td>
            <td class="nowrap"><%= h(b.get("return_time")) %></td>
            <td class="ops">
                <% if (staff && "APPROVED".equals(b.get("status"))) { %>
                <form method="post" action="<%= ctx %>/borrow?action=return&id=<%= b.get("id") %>"
                      style="display:inline" onsubmit="return confirm('确认登记归还？')">
                    <button class="btn btn-sm" type="submit">登记归还</button>
                </form>
                <% } else { %>—<% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>

<% if (staff && all != null) { %>
<div class="card">
    <h3>全部借阅记录（最近 200 条）</h3>
    <table class="grid">
        <thead>
        <tr><th>档案</th><th>借阅人</th><th>期限</th><th>状态</th><th>审批人</th><th>审批时间</th><th>归还时间</th></tr>
        </thead>
        <tbody>
        <% for (Map<String, String> b : all) { %>
        <tr>
            <td><%= h(b.get("archive_no")) %>　<%= h(b.get("title")) %></td>
            <td><%= h(b.get("user_name")) %></td>
            <td class="nowrap"><%= h(b.get("start_date")) %> ～ <%= h(b.get("end_date")) %></td>
            <td><span class="<%= borrowBadge(b.get("status")) %>"><%= borrowText(b.get("status")) %></span></td>
            <td><%= h(b.get("approver")) %></td>
            <td class="nowrap"><%= h(b.get("approve_time")) %></td>
            <td class="nowrap"><%= h(b.get("return_time")) %></td>
        </tr>
        <% } %>
        </tbody>
    </table>
</div>
<% } %>
<%@ include file="/WEB-INF/inc/foot.jsp" %>
