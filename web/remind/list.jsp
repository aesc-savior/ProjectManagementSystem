<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<% String pageTitle = "站内提醒"; String active = "remind"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    Object unreadObj = request.getAttribute("unread");
    long unread = unreadObj == null ? 0 : ((Number) unreadObj).longValue();
    String fType = (String) request.getAttribute("fType");
    if (fType == null) fType = "";
%>
<div class="page-title">
    <div>
        <h2>站内提醒</h2>
        <div class="sub">未读 <b><%= unread %></b> 条；周报催交提醒、会议通知汇总在此。</div>
    </div>
    <div class="flex">
        <a class="btn" href="<%= ctx %>/remind?action=list">全部</a>
        <a class="btn" href="<%= ctx %>/remind?action=list&type=WEEKLY">周报</a>
        <a class="btn" href="<%= ctx %>/remind?action=list&type=MEETING">会议</a>
        <a class="btn btn-primary" href="<%= ctx %>/remind?action=readAll">全部已读</a>
    </div>
</div>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">暂无提醒。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>类型</th><th>标题</th><th>内容</th><th>时间</th><th>站内</th><th>微信通道</th><th>操作</th></tr></thead>
        <tbody>
        <% for (Map<String, String> r : rows) {
            String type = r.get("type");
            String badge = "MEETING".equals(type) ? "badge badge-borrowed" : "badge badge-pending";
            String typeText = "MEETING".equals(type) ? "会议" : "周报";
            boolean unreadRow = "0".equals(r.get("read_flag"));
            String url = r.get("target_url");
        %>
        <tr>
            <td><span class="<%= badge %>"><%= typeText %></span></td>
            <td><% if (unreadRow) { %><b><%= h(r.get("title")) %></b><% } else { %><%= h(r.get("title")) %><% } %></td>
            <td><%= h(r.get("content")) %></td>
            <td class="nowrap mono"><%= h(r.get("create_time")) %></td>
            <td><% if (unreadRow) { %><span class="badge badge-rejected">未读</span><% } else { %><span class="badge badge-normal">已读</span><% } %></td>
            <td><%
                String ws = r.get("wechat_status") == null ? "" : r.get("wechat_status");
                String wd = r.get("wechat_detail") == null ? "" : r.get("wechat_detail");
                if ("SENT".equals(ws)) {
            %><span class="badge badge-normal" title="<%= h(wd) %>">已发送</span><%
                } else if ("FAILED".equals(ws)) {
            %><span class="badge badge-rejected" title="<%= h(wd) %>">发送失败</span><%
                } else if ("SKIPPED".equals(ws)) {
            %><span class="badge badge-pending" title="<%= h(wd) %>">未配置</span><%
                } else {
            %>—<%
                }
            %></td>
            <td class="ops">
                <% if (!url.isEmpty()) { %>
                <a class="btn btn-sm btn-primary" href="<%= ctx %>/remind?action=go&id=<%= r.get("id") %>&url=<%= java.net.URLEncoder.encode(url, "UTF-8") %>">去处理</a>
                <% } %>
                <% if (unreadRow) { %>
                <a class="btn btn-sm" href="<%= ctx %>/remind?action=read&id=<%= r.get("id") %>">标记已读</a>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
