<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="archive.dao.*,java.util.*" %>
<% String pageTitle = "档案详情"; String active = "archive"; %>
<%@ include file="/WEB-INF/inc/head.jsp" %>
<%
    String error = (String) request.getAttribute("error");
    Map<String, String> row = (Map<String, String>) request.getAttribute("row");
    List<Map<String, String>> files = (List<Map<String, String>>) request.getAttribute("files");
    if (files == null) files = new ArrayList<Map<String, String>>();
    boolean staff = "ADMIN".equals(curRole) || "ARCHIVIST".equals(curRole);
%>
<% if (row == null) { %>
<div class="page-title"><h2>档案详情</h2></div>
<div class="card">
    <div class="alert alert-error"><%= h(error == null ? "档案不存在" : error) %></div>
    <a class="btn" href="<%= ctx %>/archive?action=list">返回列表</a>
</div>
<% } else { %>
<div class="page-title">
    <div>
        <h2><%= h(row.get("archive_no")) %>　<%= h(row.get("title")) %></h2>
        <div class="sub">
            <span class="<%= statusBadge(row.get("status")) %>"><%= statusText(row.get("status")) %></span>
            　<%= secretText(row.get("secret_level")) %>　·　<%= keepText(row.get("keep_years")) %>　·　
            <%= h(row.get("category_name")) %>
        </div>
    </div>
    <div class="flex">
        <a class="btn" href="<%= ctx %>/archive?action=list">返回列表</a>
        <% if (staff) { %>
        <a class="btn" href="<%= ctx %>/archive?action=editForm&id=<%= row.get("id") %>">编辑</a>
        <% if (!"ARCHIVED".equals(row.get("status"))) { %>
        <form method="post" action="<%= ctx %>/archive?action=archive&id=<%= row.get("id") %>"
              onsubmit="return confirm('归档后档案将锁定，确认归档？')">
            <button class="btn btn-primary" type="submit">归档</button>
        </form>
        <% } %>
        <form method="post" action="<%= ctx %>/archive?action=delete&id=<%= row.get("id") %>"
              onsubmit="return confirm('确认删除该档案？')">
            <button class="btn btn-danger" type="submit">删除</button>
        </form>
        <% } %>
    </div>
</div>

<div class="card">
    <h3>元数据</h3>
    <table class="grid">
        <tbody>
        <tr><th style="width:120px">档号</th><td><%= h(row.get("archive_no")) %></td>
            <th style="width:120px">分类</th><td><%= h(row.get("category_name")) %></td></tr>
        <tr><th>密级</th><td><%= secretText(row.get("secret_level")) %></td>
            <th>保管期限</th><td><%= keepText(row.get("keep_years")) %></td></tr>
        <tr><th>责任部门</th><td><%= h(row.get("dept_name")) %></td>
            <th>责任者</th><td><%= h(row.get("owner_name")) %></td></tr>
        <tr><th>归档时间</th><td><%= h(row.get("archive_time")) %></td>
            <th>录入时间</th><td><%= h(row.get("create_time")) %>（<%= h(row.get("creator")) %>）</td></tr>
        <tr><th>备注</th><td colspan="3"><%= h(row.get("remark")) %></td></tr>
        </tbody>
    </table>
</div>

<div class="card">
    <h3>附件（<%= files.size() %>）</h3>
    <% if (files.isEmpty()) { %>
    <div class="empty">暂无附件<% if (staff) { %>，可在下方上传<% } %>。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>文件名</th><th>大小</th><th>上传时间</th><th>操作</th></tr></thead>
        <tbody>
        <% for (Map<String, String> f : files) {
            long size = 0;
            try { size = Long.parseLong(f.get("file_size")); } catch (Exception ignore) { }
        %>
        <tr>
            <td><%= h(f.get("file_name")) %></td>
            <td><%= String.format("%.1f KB", size / 1024.0) %></td>
            <td class="nowrap"><%= h(f.get("create_time")) %></td>
            <td class="ops">
                <a class="btn btn-sm" href="<%= ctx %>/archive?action=download&fileId=<%= f.get("id") %>">下载</a>
                <% if (staff) { %>
                <form method="post" action="<%= ctx %>/archive?action=deleteFile&id=<%= row.get("id") %>&fileId=<%= f.get("id") %>"
                      style="display:inline" onsubmit="return confirm('确认删除该附件？')">
                    <button class="btn btn-sm btn-danger" type="submit">删除</button>
                </form>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>

    <% if (staff) { %>
    <form class="flex mt" method="post" action="<%= ctx %>/archive?action=upload&id=<%= row.get("id") %>"
          enctype="multipart/form-data">
        <input type="file" name="file" class="grow">
        <button class="btn btn-primary" type="submit">上传附件</button>
    </form>
    <div class="hint mt">单个文件 ≤200MB；上传后落盘到 upload 目录，数据库只存路径与元数据。</div>
    <% } %>
</div>

<% if ("ARCHIVED".equals(row.get("status"))) { %>
<div class="card">
    <h3>申请借阅</h3>
    <form class="flex" method="post" action="<%= ctx %>/borrow?action=apply">
        <input type="hidden" name="archiveId" value="<%= row.get("id") %>">
        <div class="field" style="width:120px">
            <label>借阅天数</label>
            <input type="number" name="days" value="7" min="1" max="30">
        </div>
        <div class="field grow">
            <label>借阅原因</label>
            <input type="text" name="reason" placeholder="例如：年度决算审计需要核对原始凭证">
        </div>
        <div class="field" style="width:110px">
            <label>&nbsp;</label>
            <button class="btn btn-primary" type="submit">提交申请</button>
        </div>
    </form>
    <div class="hint mt">期限 1～30 天；提交后进入审批，审批通过档案状态变为“借出中”。</div>
</div>
<% } %>
<% } %>
<%@ include file="/WEB-INF/inc/foot.jsp" %>
