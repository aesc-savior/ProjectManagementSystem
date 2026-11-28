<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "项目文档"; String active = "doc"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    List<Map<String, String>> projects = (List<Map<String, String>>) request.getAttribute("projects");
    if (projects == null) projects = new ArrayList<Map<String, String>>();

    List<Map<String, String>> searchTasks = (List<Map<String, String>>) request.getAttribute("searchTasks");
    if (searchTasks == null) searchTasks = new ArrayList<Map<String, String>>();
    List<Map<String, String>> uploadTasks = (List<Map<String, String>>) request.getAttribute("uploadTasks");
    if (uploadTasks == null) uploadTasks = new ArrayList<Map<String, String>>();
    List<Map<String, String>> uploadMembers = (List<Map<String, String>>) request.getAttribute("uploadMembers");
    if (uploadMembers == null) uploadMembers = new ArrayList<Map<String, String>>();

    String projectId = request.getParameter("projectId"); if (projectId == null) projectId = "";
    String taskId = request.getParameter("taskId"); if (taskId == null) taskId = "";
    String keyword = request.getParameter("keyword"); if (keyword == null) keyword = "";
    String orderBy = request.getParameter("orderBy"); if (orderBy == null) orderBy = "id_desc";
    String isRecycleStr = request.getParameter("isRecycle"); if (isRecycleStr == null) isRecycleStr = "0";

    String uploadProjectId = request.getParameter("uploadProjectId"); if (uploadProjectId == null) uploadProjectId = "";
    String uploadTaskId = request.getParameter("uploadTaskId"); if (uploadTaskId == null) uploadTaskId = "";

    String uid = (String) session.getAttribute("uid");
%>
<div class="page-title" style="display:flex; justify-content:space-between; align-items:center;">
    <div>
        <h2>项目文档 <% if ("1".equals(isRecycleStr)) { %> - 回收站 <% } %></h2>
        <div class="sub">共 <b><%= rows.size() %></b> 个文档</div>
    </div>
    <div>
        <% if (!"1".equals(isRecycleStr)) { %>
        <!-- 文档管理首页的三个按钮 -->
        <a class="btn btn-primary" href="<%= ctx %>/doc?action=folder">我的文件夹</a>
        <a class="btn" href="<%= ctx %>/doc?action=export&projectId=<%=projectId%>&taskId=<%=taskId%>&keyword=<%=keyword%>">导出 Excel</a>
        <a class="btn btn-warning" href="<%= ctx %>/doc?action=list&isRecycle=1">回收站</a>
        <% } else { %>
        <!-- 回收站按钮 -->
        <a class="btn btn-primary" href="<%= ctx %>/doc?action=list">返回文档管理</a>
        <% } %>
    </div>
</div>

<!-- 搜索表单 -->
<div class="card">
    <form class="filters" method="get" action="<%= ctx %>/doc">
        <input type="hidden" name="action" value="list">
        <input type="hidden" name="isRecycle" value="<%= isRecycleStr %>">

        <div class="field">
            <label>项目筛选</label><!-- 项目下拉框 -->
            <select name="projectId" onchange="location.href='<%= ctx %>/doc?action=list&isRecycle=<%=isRecycleStr%>&projectId=' + this.value;">
                <option value="">全部项目</option><!-- 项目筛选的第一项 -->
                <!--其他公开文档的下拉框按钮，传值public给后端-->
                <option value="public" <% if ("public".equals(projectId)) { %>selected<% } %>>其他公开文档</option>
                <% for (int i = 0; i < projects.size(); i++) {
                    Map<String, String> p = projects.get(i); %><!-- 根据权限循环为项目筛选提供选项 -->
                <option value="<%= p.get("id") %>" <% if (p.get("id").equals(projectId)) { %>selected<% } %>><%= h(p.get("name")) %></option>
                <% } %>
            </select>
        </div>

        <div class="field">
            <label>任务筛选</label>
            <select name="taskId" onchange="location.href='<%= ctx %>/doc?action=list&isRecycle=<%=isRecycleStr%>&projectId=<%=projectId%>&taskId=' + this.value;">
                <option value="">全部任务</option>
                <% for (int i = 0; i < searchTasks.size(); i++) {
                    Map<String, String> t = searchTasks.get(i); %><!-- 根据权限循环为任务筛选提供选项 -->
                <option value="<%= t.get("id") %>" <% if (t.get("id").equals(taskId)) { %>selected<% } %>><%= h(t.get("title")) %></option>
                <% } %>
            </select>
        </div>

        <div class="field grow">
            <label>关键词搜索</label>
            <input type="text" name="keyword" value="<%= h(keyword) %>">
        </div>

        <div class="field">
            <label>排序</label>
            <select name="orderBy">
                <option value="id_desc" <% if ("id_desc".equals(orderBy)) { %>selected<% } %>>时间降序</option>
                <option value="time_asc" <% if ("time_asc".equals(orderBy)) { %>selected<% } %>>时间升序</option>
                <option value="name_asc" <% if ("name_asc".equals(orderBy)) { %>selected<% } %>>文件名升序</option>
            </select>
        </div>

        <div class="field">
            <label>&nbsp;</label>
            <div class="flex">
                <!-- 查询按钮，点击后提交GET请求，把选中的条件拼到URL后面。 -->
                <button class="btn btn-primary" type="submit">查询</button>
                <!-- 重置按钮，点击后跳转回首页/doc?action=list并清空所有搜索条件 -->
                <!-- 添加了条件isRecycle=isRecycleStr，修正在回收站点击重置会回到首页的bug -->
                <a class="btn" href="<%= ctx %>/doc?action=list&isRecycle=<%=isRecycleStr%>">重置</a>
            </div>
        </div>
    </form>
</div>

<!-- 上传文档部分页面 -->
<% if (!projects.isEmpty() && !isViewer && !"1".equals(isRecycleStr)) { %><!-- 不是游客且有项目且在首页(不在回收站) -->
<div class="card">
    <h3>上传文档</h3>
    <form class="flex" method="post" action="<%= ctx %>/doc?action=upload" enctype="multipart/form-data" style="flex-wrap: wrap; gap: 10px;">
        <div class="field" style="min-width:180px">
            <label>所属项目 *</label><!-- 文档必须背靠一个上传者参与的项目，必选 -->
            <!-- 根据已选中的项目ID刷新页面(包括下拉框的选项回显、关联任务下拉框选择范围) -->
            <select name="projectId" onchange="location.href='<%= ctx %>/doc?action=list&projectId=<%=projectId%>&uploadProjectId=' + this.value;">
                <% for (int i = 0; i < projects.size(); i++) {//循环所有项目以生成可选项
                    Map<String, String> p = projects.get(i); %>
                <option value="<%= p.get("id") %>" <% if (p.get("id").equals(uploadProjectId)) { %>selected<% } %>><%= h(p.get("name")) %></option>
                <% } %>
            </select>
        </div>

        <div class="field" style="min-width:150px">
            <label>关联任务</label><!-- 文档可以关联一个上传者选择背靠的项目中参与的一个任务 -->
            <!-- 根据已选中的任务ID刷新页面(包括下拉框的选项回显、指定人员下拉框选择范围) -->
            <select name="taskId" onchange="location.href='<%= ctx %>/doc?action=list&projectId=<%=projectId%>&uploadProjectId=<%=uploadProjectId%>&uploadTaskId=' + this.value;">
                <option value="">不关联</option><!-- 也可以不关联 -->
                <% for (int i = 0; i < uploadTasks.size(); i++) {
                    Map<String, String> t = uploadTasks.get(i); %>
                <option value="<%= t.get("id") %>" <% if (t.get("id").equals(uploadTaskId)) { %>selected<% } %>><%= h(t.get("title")) %></option>
                <% } %>
            </select>
        </div>

        <div class="field">
            <label>可见范围</label>
            <!-- 根据已选中可见范围刷新页面(如果是PRIVATE则需要“仅指定人员”的下拉框) -->
            <!-- 选中PRIVATE时将id="userSelectDiv"的div下拉框显示出来，否则隐藏 -->
            <select name="visibility" id="visSelect" onchange="if (this.value == 'PRIVATE') { document.getElementById('userSelectDiv').style.display = 'block'; } else { document.getElementById('userSelectDiv').style.display = 'none'; }">
                <option value="PROJECT">项目成员</option>
                <option value="PUBLIC">公开</option>
                <option value="PRIVATE">仅指定人员</option>
            </select>
        </div>

        <div class="field" id="userSelectDiv" style="display:none;">
            <label>指定人员(多选)</label><!-- 根据文档背靠的项目和关联的任务，即使由上传者指定人员可查看，指定人员也必须是文档的项目/任务参与者 -->
            <select name="allowedUsers" multiple style="height:60px; width:150px;">

                <!-- 循环根据项目和任务查人员，生成选项 -->
                <% for (int i = 0; i < uploadMembers.size(); i++) {
                    Map<String, String> m = uploadMembers.get(i); %>
                <option value="<%= m.get("id") %>"><%= h(m.get("user_name")) %></option>
                <% } %>
            </select>
        </div>

        <div class="field grow" style="min-width:150px">
            <label>文件 *</label>
            <input type="file" name="file" required>
        </div>

        <div class="field grow">
            <label>说明</label>
            <input type="text" name="description">
        </div>

        <div class="field">
            <label>标签</label>
            <input type="text" name="tags">
        </div>

        <div class="field" style="width:110px">
            <label>&nbsp;</label>
            <button class="btn btn-primary" type="submit">上传</button>
        </div>
    </form>
</div>
<% } %>

<!-- 文档管理首页筛选搜索出的文档列表 -->
<div class="card">
    <% if (!"1".equals(isRecycleStr) && !rows.isEmpty()) { %>
    <div style="margin-bottom: 10px;">
        <button class="btn btn-primary" onclick="batchDownload()">打包下载选中文件</button>
    </div>
    <% } %>

    <table class="grid">
        <thead>
        <tr>
            <% if (!"1".equals(isRecycleStr)) { %><!-- 文档管理首页 -->
            <th style="width:40px;"><input type="checkbox" onclick="checkAll(this)"></th><!-- 全选 -->
            <% } %>
            <th>文件名</th>
            <th>版本号</th>
            <th>说明</th><th>标签</th><th>项目</th><th>大小</th><th>上传人</th><th>操作</th>
        </tr>
        </thead>
        <tbody>
        <% for (int i = 0; i < rows.size(); i++) {//循环遍历所有文档
            Map<String, String> r = rows.get(i);
            long size = 0; //把文件大小转为数字
            try { size = Long.parseLong(r.get("size")); } catch (Exception e) { }

            String sizeStr = "";
            if (size > 1024 * 1024) {
                sizeStr = String.format("%.1f MB", size / (1024.0 * 1024.0));
            } else {
                sizeStr = String.format("%.1f KB", size / 1024.0);
            }
        %>
        <tr>
            <% if (!"1".equals(isRecycleStr)) { %>
            <td><input type="checkbox" name="docId" value="<%= r.get("id") %>"></td>
            <% } %>
            <td><%= h(r.get("name")) %></td>
            <td>v<%= r.get("version") %></td>
            <td><%= h(r.get("description")) %></td>
            <td><%= h(r.get("tags")) %></td>
            <td><%= h(r.get("project_name")) %></td>
            <td><%= sizeStr %></td>
            <td><%= h(r.get("uploader")) %></td>
            <td class="ops">
                <% if ("1".equals(isRecycleStr)) {//如果在回收站 %>
                <form method="post" action="<%= ctx %>/doc?action=restore&id=<%= r.get("id") %>" style="display:inline">
                    <button class="btn btn-sm btn-success" type="submit">恢复</button>
                </form>
                <form method="post" action="<%= ctx %>/doc?action=forceDelete&id=<%= r.get("id") %>" style="display:inline" onsubmit="return confirm('彻底删除后不可恢复，确定？')">
                    <button class="btn btn-sm btn-danger" type="submit">彻底删除</button>
                </form>
                <% } else { %>
                <a class="btn btn-sm" href="<%= ctx %>/doc?action=preview&id=<%= r.get("id") %>" target="_blank">预览</a>
                <a class="btn btn-sm" href="<%= ctx %>/doc?action=download&id=<%= r.get("id") %>">下载</a>
                <a class="btn btn-sm" href="<%= ctx %>/doc?action=history&id=<%= r.get("id") %>">历史版本</a>
                <button class="btn btn-sm" onclick="editDoc('<%= r.get("id") %>', '<%= h(r.get("name")) %>', '<%= h(r.get("description")) %>', '<%= h(r.get("tags")) %>')">编辑</button>
                <form method="post" action="<%= ctx %>/doc?action=delete&id=<%= r.get("id") %>" style="display:inline" onsubmit="return confirm('确认移入回收站？')">
                    <button class="btn btn-sm btn-danger" type="submit">删除</button>
                </form>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
</div>
<!-- 将修改后的数据POST到后端，可能有新文档ID、新文件名、新说明和新标签 -->
<form id="editForm" method="post" action="<%= ctx %>/doc?action=updateInfo" style="display:none;">
    <input type="hidden" name="id" id="editId"><input type="hidden" name="name" id="editName">
    <input type="hidden" name="description" id="editDesc"><input type="hidden" name="tags" id="editTags">
</form>

<script>
    //全选框功能实现
    function checkAll(obj) { var bs = document.getElementsByName("docId"); for (var i=0; i<bs.length; i++) bs[i].checked = obj.checked; }
    //处理批量打包下载zip请求：将要一起下载的文档的ID拼起来提交给后端方便后端执行
    function batchDownload() {
        var bs = document.getElementsByName("docId"), ids = "";
        for (var i=0; i<bs.length; i++) { if (bs[i].checked) { if (ids!="") ids += ","; ids += bs[i].value; } }
        if (ids == "") { alert("请勾选要下载的文档！"); return; }
        window.location.href = "<%= ctx %>/doc?action=downloadZip&ids=" + ids;
    }
    //将文档信息编辑结果提交给后端让后端修改
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