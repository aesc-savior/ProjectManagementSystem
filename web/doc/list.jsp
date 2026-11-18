<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "项目文档"; String active = "doc"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    List<Map<String, String>> projects = (List<Map<String, String>>) request.getAttribute("projects");
    if (projects == null) projects = new ArrayList<Map<String, String>>();
    String fProjectId = (String) request.getAttribute("fProjectId");
    String fKeyword = (String) request.getAttribute("fKeyword");
    if (fProjectId == null) fProjectId = "";
    if (fKeyword == null) fKeyword = "";
    String uid = (String) session.getAttribute("uid");
%>
<div class="page-title">
    <div>
        <h2>项目文档</h2>
        <div class="sub">共 <b><%= rows.size() %></b> 个文档（最多显示最近 200 个）；同名文件再次上传会自动递增版本。</div>
    </div>
</div>

<div class="card">
    <form class="filters" method="get" action="<%= ctx %>/doc">
        <input type="hidden" name="action" value="list">
        <div class="field">
            <label>项目</label>
            <select name="projectId">
                <option value="">全部</option>
                <% for (Map<String, String> p : projects) { %>
                <option value="<%= p.get("id") %>" <%= p.get("id").equals(fProjectId) ? "selected" : "" %>><%= h(p.get("name")) %></option>
                <% } %>
            </select>
        </div>
        <div class="field grow">
            <label>文件名关键词</label>
            <input type="text" name="keyword" value="<%= h(fKeyword) %>" placeholder="例如 需求">
        </div>
        <div class="field">
            <label>&nbsp;</label>
            <div class="flex">
                <button class="btn btn-primary" type="submit">查询</button>
                <a class="btn" href="<%= ctx %>/doc?action=list">重置</a>
            </div>
        </div>
    </form>
</div>

<% if (!projects.isEmpty() && !isViewer) { %>
<div class="card">
    <h3>上传文档</h3>
    <form class="flex" method="post" action="<%= ctx %>/doc?action=upload" enctype="multipart/form-data">
        <div class="field" style="min-width:220px">
            <label>所属项目 *</label>
            <select name="projectId">
                <% for (Map<String, String> p : projects) { %>
                <option value="<%= p.get("id") %>" <%= p.get("id").equals(fProjectId) ? "selected" : "" %>><%= h(p.get("name")) %></option>
                <% } %>
            </select>
        </div>
        <div class="field">
            <label>可见范围</label>
            <select name="visibility">
                <option value="PROJECT">项目成员</option>
                <option value="PUBLIC">公开</option>
                <option value="PRIVATE">仅自己</option>
            </select>
        </div>
        <div class="field grow">
            <label>文件 *</label>
            <input type="file" name="file">
        </div>
        <div class="field" style="width:110px">
            <label>&nbsp;</label>
            <button class="btn btn-primary" type="submit">上传</button>
        </div>
    </form>
    <div class="hint mt">支持 pdf/doc/docx/xls/xlsx/ppt/pptx/jpg/png/zip/rar/txt/md，单文件 ≤200MB。</div>
</div>
<% } %>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">还没有文档。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>文件名</th><th>项目</th><th>版本</th><th>大小</th><th>上传人</th><th>上传时间</th><th>操作</th></tr></thead>
        <tbody>
        <% for (Map<String, String> r : rows) {
            long size = 0;
            try { size = Long.parseLong(r.get("size")); } catch (Exception ignore) { }
            boolean canDelete = isAdmin || uid.equals(r.get("uploader_id"));
        %>
        <tr>
            <td><span class="tag"><%= h(r.get("ext")) %></span><%= h(r.get("name")) %></td>
            <td><a href="<%= ctx %>/project?action=view&id=<%= r.get("project_id") %>"><%= h(r.get("project_name")) %></a></td>
            <td>v<%= h(r.get("version")) %></td>
            <td><%= String.format("%.1f KB", size / 1024.0) %></td>
            <td><%= h(r.get("uploader")) %></td>
            <td class="nowrap"><%= h(r.get("create_time")) %></td>
            <td class="ops">
                <a class="btn btn-sm" href="<%= ctx %>/doc?action=download&id=<%= r.get("id") %>">下载</a>
                <% if (canDelete) { %>
                <form method="post" action="<%= ctx %>/doc?action=delete&id=<%= r.get("id") %>" style="display:inline"
                      onsubmit="return confirm('确认删除该文档？')">
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
