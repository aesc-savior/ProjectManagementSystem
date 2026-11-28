<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "历史版本"; String active = "doc"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    String docName = (String) request.getAttribute("docName");
%>
<div class="page-title" style="display:flex; justify-content:space-between; align-items:center;">
    <div>
        <h2><%= h(docName) %>文档历史版本</h2>
        <div class="sub">共 <b><%= rows.size() %></b> 个版本</div>
    </div>
    <div>
        <a class="btn btn-primary" href="javascript:history.back()">返回文档列表</a>
    </div>
</div>

<div class="card">
    <table class="grid">
        <thead>
        <tr>
            <th>版本号</th>
            <th>文件名</th>
            <th>说明</th>
            <th>标签</th>
            <th>大小</th>
            <th>上传时间</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <% for (int i = 0; i < rows.size(); i++) {
            Map<String, String> r = rows.get(i);
            long size = 0;
            try { size = Long.parseLong(r.get("size")); } catch (Exception e) { }

            String sizeStr = "";
            if (size > 1024 * 1024) {
                sizeStr = String.format("%.1f MB", size / (1024.0 * 1024.0));
            } else {
                sizeStr = String.format("%.1f KB", size / 1024.0);
            }
        %>
        <tr>
            <td><strong>v<%= r.get("version") %></strong></td>
            <td><%= h(r.get("name")) %></td>
            <td><%= h(r.get("description")) %></td>
            <td><%= h(r.get("tags")) %></td>
            <td><%= sizeStr %></td>
            <td><%= h(r.get("create_time")) %></td>
            <td class="ops">
                <a class="btn btn-sm" href="<%= ctx %>/doc?action=preview&id=<%= r.get("id") %>" target="_blank">预览</a>
                <a class="btn btn-sm" href="<%= ctx %>/doc?action=download&id=<%= r.get("id") %>">下载</a>
                <button class="btn btn-sm" onclick="editDoc('<%= r.get("id") %>', '<%= h(r.get("name")) %>', '<%= h(r.get("description")) %>', '<%= h(r.get("tags")) %>')">编辑</button>
                <form method="post" action="<%= ctx %>/doc?action=delete&id=<%= r.get("id") %>" style="display:inline" onsubmit="return confirm('确认移入回收站？')">
                    <input type="hidden" name="returnHistory" value="1">
                    <button class="btn btn-sm btn-danger" type="submit">删除</button>
                </form>
                <form method="post" action="<%= ctx %>/doc?action=rollback&id=<%= r.get("id") %>" style="display:inline" onsubmit="return confirm('确认回滚到该版本吗？回滚后版本号将变为最新。')">
                    <button class="btn btn-sm btn-warning" type="submit">回滚</button>
                </form>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
</div>

<form id="editForm" method="post" action="<%= ctx %>/doc?action=updateInfo" style="display:none;">
    <input type="hidden" name="id" id="editId">
    <input type="hidden" name="name" id="editName">
    <input type="hidden" name="description" id="editDesc">
    <input type="hidden" name="tags" id="editTags">
    <input type="hidden" name="returnHistory" value="1"><!--提供returnHistory方便后端改完名后重定向-->
</form>

<script>
    function editDoc(id, name, desc, tags) {
        var n = prompt("文件名：", name); if (n == null) return;
        var d = prompt("说明：", desc); if (d == null) return;
        var t = prompt("标签：", tags); if (t == null) return;
        document.getElementById("editId").value = id;
        document.getElementById("editName").value = n;
        document.getElementById("editDesc").value = d;
        document.getElementById("editTags").value = t;
        document.getElementById("editForm").submit();
    }
</script>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
