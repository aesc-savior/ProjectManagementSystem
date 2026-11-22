<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "审批中心"; String active = "approval"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> pending = (List<Map<String, String>>) request.getAttribute("pending");
    List<Map<String, String>> mine = (List<Map<String, String>>) request.getAttribute("mine");
    if (pending == null) pending = new ArrayList<Map<String, String>>();
    if (mine == null) mine = new ArrayList<Map<String, String>>();
%>
<div class="page-title">
    <div>
        <h2>审批中心</h2>
        <div class="sub">待我审批 <b><%= pending.size() %></b> 条；驳回必须填写意见，通过后任务截止日自动更新。</div>
    </div>
</div>

<div class="card">
    <h3>待我审批</h3>
    <% if (pending.isEmpty()) { %>
    <div class="empty">没有待审批事项。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>申请</th><th>申请人</th><th>延期原因</th><th>新截止日</th><th>提交时间</th><th>处理</th></tr></thead>
        <tbody>
        <% for (Map<String, String> r : pending) { %>
        <tr>
            <td>
                <%= h(r.get("title")) %>
                <% if (!r.get("project_name").isEmpty()) { %><div class="hint"><%= h(r.get("project_name")) %></div><% } %>
            </td>
            <td><%= h(r.get("applicant")) %></td>
            <td><%= h(r.get("detail")) %></td>
            <td class="nowrap"><%= h(r.get("new_date")) %></td>
            <td class="nowrap"><%= h(r.get("create_time")) %></td>
            <td>
                <form class="flex" method="post" action="<%= ctx %>/approval?action=decide&id=<%= r.get("id") %>">
                    <input type="text" name="opinion" placeholder="审批意见" style="width:140px;padding:3px 6px">
                    <button class="btn btn-sm btn-primary" type="submit" name="decision" value="approve">通过</button>
                    <button class="btn btn-sm btn-danger" type="submit" name="decision" value="reject">驳回</button>
                </form>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>

<div class="card">
    <h3>我发起的</h3>
    <% if (mine.isEmpty()) { %>
    <div class="empty">你还没有发起过审批。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>申请</th><th>新截止日</th><th>状态</th><th>审批人</th><th>意见</th><th>审批时间</th></tr></thead>
        <tbody>
        <% for (Map<String, String> r : mine) { %>
        <tr>
            <td><%= h(r.get("title")) %></td>
            <td class="nowrap"><%= h(r.get("new_date")) %></td>
            <td><span class="<%= approvalBadge(r.get("status")) %>"><%= approvalText(r.get("status")) %></span></td>
            <td><%= h(r.get("approver_name")) %></td>
            <td><%= h(r.get("opinion")) %></td>
            <td class="nowrap"><%= h(r.get("approve_time")) %></td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
