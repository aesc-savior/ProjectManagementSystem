<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<% String pageTitle = "操作日志"; String active = "log"; %>
<%@ include file="/WEB-INF/inc/head.jsp" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    long total = request.getAttribute("total") == null ? 0 : (Long) request.getAttribute("total");
    int pageNo = request.getAttribute("page") == null ? 1 : (Integer) request.getAttribute("page");
    int pages = request.getAttribute("pages") == null ? 1 : (Integer) request.getAttribute("pages");
    String fAction = (String) request.getAttribute("fAction");
    String fKeyword = (String) request.getAttribute("fKeyword");
    if (fAction == null) fAction = "";
    if (fKeyword == null) fKeyword = "";
%>
<div class="page-title">
    <div>
        <h2>操作日志</h2>
        <div class="sub">共 <b><%= total %></b> 条；登录、增删改、审批、上传下载都会留痕。</div>
    </div>
</div>

<div class="card">
    <form class="filters" method="get" action="<%= ctx %>/log">
        <input type="hidden" name="action" value="list">
        <div class="field">
            <label>动作</label>
            <select name="fAction">
                <option value="">全部</option>
                <option value="LOGIN" <%= "LOGIN".equals(fAction) ? "selected" : "" %>>登录</option>
                <option value="ADD" <%= "ADD".equals(fAction) ? "selected" : "" %>>新增</option>
                <option value="UPDATE" <%= "UPDATE".equals(fAction) ? "selected" : "" %>>修改</option>
                <option value="DELETE" <%= "DELETE".equals(fAction) ? "selected" : "" %>>删除</option>
                <option value="ARCHIVE" <%= "ARCHIVE".equals(fAction) ? "selected" : "" %>>归档</option>
                <option value="APPLY" <%= "APPLY".equals(fAction) ? "selected" : "" %>>借阅申请</option>
                <option value="APPROVE" <%= "APPROVE".equals(fAction) ? "selected" : "" %>>审批</option>
                <option value="UPLOAD" <%= "UPLOAD".equals(fAction) ? "selected" : "" %>>上传</option>
                <option value="DOWNLOAD" <%= "DOWNLOAD".equals(fAction) ? "selected" : "" %>>下载</option>
            </select>
        </div>
        <div class="field grow">
            <label>关键词（操作人 / 对象 / 说明）</label>
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
    <div class="empty">没有日志记录。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr><th>时间</th><th>操作人</th><th>动作</th><th>对象</th><th>说明</th><th>IP</th></tr>
        </thead>
        <tbody>
        <% for (Map<String, String> r : rows) { %>
        <tr>
            <td class="nowrap"><%= h(r.get("create_time")) %></td>
            <td><%= h(r.get("user_name")) %></td>
            <td><span class="badge badge-borrowed"><%= h(r.get("action")) %></span></td>
            <td><%= h(r.get("target")) %></td>
            <td><%= h(r.get("detail")) %></td>
            <td><%= h(r.get("ip")) %></td>
        </tr>
        <% } %>
        </tbody>
    </table>

    <div class="pager">
        <span class="info">第 <%= pageNo %> / <%= Math.max(pages, 1) %> 页</span>
        <% if (pageNo > 1) { %>
        <a href="<%= ctx %>/log?action=list&page=<%= pageNo - 1 %>&fAction=<%= fAction %>&keyword=<%= java.net.URLEncoder.encode(fKeyword, "UTF-8") %>">上一页</a>
        <% } %>
        <% if (pageNo < pages) { %>
        <a href="<%= ctx %>/log?action=list&page=<%= pageNo + 1 %>&fAction=<%= fAction %>&keyword=<%= java.net.URLEncoder.encode(fKeyword, "UTF-8") %>">下一页</a>
        <% } %>
    </div>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jsp" %>
