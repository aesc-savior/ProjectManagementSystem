<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "我的文件夹"; String active = "doc"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    List<Map<String, String>> treeProjects = (List<Map<String, String>>) request.getAttribute("treeProjects");
    if (treeProjects == null) treeProjects = new ArrayList<Map<String, String>>();
    List<Map<String, String>> allTasks = (List<Map<String, String>>) request.getAttribute("allTasks");
    if (allTasks == null) allTasks = new ArrayList<Map<String, String>>();

    //从请求参数取得用户的请求点进了那一层文件夹
    String status = request.getParameter("status"); if (status == null) status = "";
    String projectId = request.getParameter("projectId"); if (projectId == null) projectId = "";
    String taskId = request.getParameter("taskId"); if (taskId == null) taskId = "";
    //状态
    String[] statuses = {"RUNNING", "PENDING", "PAUSED", "DONE", "CANCELED", "ARCHIVED"};
    String[] statusNames = {"进行中", "待启动", "暂停", "已完成", "已取消", "已归档"};
%>
<div class="page-title" style="display:flex; justify-content:space-between; align-items:center;">
    <div><h2>我的文件夹</h2></div>
    <div><a class="btn btn-primary" href="<%= ctx %>/doc?action=list">返回文档管理</a></div>
</div>

<div class="card">
    <div style="margin-bottom: 15px; font-size: 14px; padding: 10px; background: #f8f9fa; border: 1px solid #ddd; border-radius: 4px;">
        当前位置:
        <a href="<%= ctx %>/doc?action=folder" style="color: #007bff; text-decoration: none;">全部文件</a>
        <% if (!"".equals(status)) {//判断用户是否进入了一个状态级文件夹
            String stName = "";//将用户请求(点入)的状态与所有状态逐一比对得到用户点进的是哪个文件夹
            for(int i=0; i<statuses.length; i++){ if(statuses[i].equals(status)) stName = statusNames[i]; }
        %>
        &gt;<!--  (“文件夹”路径)全部文件 > -->
        <a href="<%= ctx %>/doc?action=folder&status=<%=status%>" style="color: #007bff; text-decoration: none;"><%= stName %></a><!--  全部文件 > 状态 > -->
        <% } %>
        <% if (!"".equals(projectId)) {//判断用户是否进入一个项目级文件夹
            String pName = "";//根据项目ID找到用户点入的项目名
            for (int i = 0; i < treeProjects.size(); i++) {
                Map<String, String> p = treeProjects.get(i);
                if (p.get("id").equals(projectId)) {
                    pName = p.get("name");
                    break;
                }
            }
        %>
        &gt; <!--  全部文件 > 状态 > -->
        <a href="<%= ctx %>/doc?action=folder&status=<%=status%>&projectId=<%=projectId%>" style="color: #007bff; text-decoration: none;"><%= h(pName) %></a>
        <% } %>
        <% if (!"".equals(taskId)) {//判断是否进入任务级文件夹
            String tName = "";
            for (int i = 0; i < allTasks.size(); i++) {
                Map<String, String> t = allTasks.get(i);
                if (t.get("id").equals(taskId)) {
                    tName = t.get("title");
                    break;
                }
            }
        %>
        &gt; <!--  全部文件 > 状态 > 项目 > -->
        <span style="color: #333; font-weight: bold;"><%= h(tName) %></span><!--  全部文件 > 状态 > 项目 > 任务-->
        <% } %>
    </div>

    <!-- 文件夹与文档共有的表头 -->
    <table class="grid">
        <thead>
        <tr>
            <th>名称</th>
            <th>大小</th>
            <th>修改时间 / 上传时间</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <%
            boolean hasFolders = false;//判断有没有子文件夹
            if ("".equals(projectId)) {
                if ("".equals(status)) {
                    //无项目ID和状态，是根目录“全部文件”，需要展示状态文件夹
                    for (int s = 0; s < statuses.length; s++) {//循环6种状态
                        String st = statuses[s];
                        long totalSize = 0; //计算状态文件夹的总大小
                        String maxTime = "";//计算状态文件夹的最后更新时间

                        for (int pi = 0; pi < treeProjects.size(); pi++) {//依次读取根目录当前正在画的状态下的各个项目的信息
                            Map<String, String> p = treeProjects.get(pi);
                            if (st.equals(p.get("status"))) {//如果选择的这个项目确实属于当前正在画的状态的文件夹
                                //为正在画的状态文件夹累加上大小并比对是否为当前文件夹里更新时间最靠后的
                                try { totalSize += Long.parseLong(p.get("total_size")); } catch(Exception e){}
                                String t = p.get("last_update");
                                if (t != null && t.compareTo(maxTime) > 0) maxTime = t;
                            }
                        }
                        //大小为0，空文件夹隐藏不显示
                        if (totalSize <= 0) continue;

                        hasFolders = true;//有子文件夹，也需要显示
                        String sizeStr = "";//根据大小判断应当用什么单位
                        if (totalSize > 1024 * 1024) {
                            sizeStr = String.format("%.1f MB", totalSize / (1024.0 * 1024.0));
                        } else {
                            sizeStr = String.format("%.1f KB", totalSize / 1024.0);
                        }
        %>
        <!-- 画出根目录的状态文件夹 -->
        <tr style="background: #fcfcfc;">
            <td style="font-size: 16px; font-weight: bold;">📁 <%= statusNames[s] %></td>
            <td><%= sizeStr %></td>
            <td><%= h(maxTime) %></td>
            <td><a class="btn btn-sm" href="<%= ctx %>/doc?action=folder&status=<%=st%>">打开</a></td>
        </tr>
        <% } } else {//如果无项目ID有状态信息，在状态文件夹内
            // 项目文件夹
            for (int pi = 0; pi < treeProjects.size(); pi++) {//遍历的还是项目表
                Map<String, String> p = treeProjects.get(pi);
                if (status.equals(p.get("status"))) {//如果这个项目在现在这个状态文件夹内则计算并画出这个项目文件夹
                    long ts = 0; try { ts = Long.parseLong(p.get("total_size")); } catch(Exception e){}
                    // 大小为0，隐藏
                    if (ts <= 0) continue;

                    hasFolders = true;
                    String sizeStr = "";
                    if (ts > 1024 * 1024) {
                        sizeStr = String.format("%.1f MB", ts / (1024.0 * 1024.0));
                    } else {
                        sizeStr = String.format("%.1f KB", ts / 1024.0);
                    }
        %>
        <!-- 画出状态文件夹下的项目文件夹 -->
        <tr style="background: #fcfcfc;">
            <td style="font-size: 16px; font-weight: bold;">📁 <%= h(p.get("name")) %></td>
            <td><%= sizeStr %></td>
            <td><%= h(p.get("last_update")) %></td>
            <td><a class="btn btn-sm" href="<%= ctx %>/doc?action=folder&status=<%=status%>&projectId=<%=p.get("id")%>">打开</a></td>
        </tr>
        <%         }
        }
        }
        } else if ("".equals(taskId)) {//有项目ID但是没有任务ID，在项目文件夹
            // 任务文件夹
            for (int ti = 0; ti < allTasks.size(); ti++) {
                Map<String, String> t = allTasks.get(ti);
                if (projectId.equals(t.get("project_id"))) {
                    long ts = 0;
                    String taskSizeStr = t.get("total_size");
                    if (taskSizeStr == null) {
                        taskSizeStr = "0";
                    }
                    try { ts = Long.parseLong(taskSizeStr); } catch(Exception e){}
                    // 大小为0，隐藏
                    if (ts <= 0) continue;

                    hasFolders = true;
                    String sizeStr = "";
                    if (ts > 1024 * 1024) {
                        sizeStr = String.format("%.1f MB", ts / (1024.0 * 1024.0));
                    } else {
                        sizeStr = String.format("%.1f KB", ts / 1024.0);
                    }
        %>
        <!-- 画出项目下的任务文件夹 -->
        <tr style="background: #fcfcfc;">
            <td style="font-size: 16px; font-weight: bold;">📁 <%= h(t.get("title")) %></td>
            <td><%= sizeStr %></td>
            <td><%= h(t.get("last_update")) %></td>
            <td><a class="btn btn-sm" href="<%= ctx %>/doc?action=folder&status=<%=status%>&projectId=<%=projectId%>&taskId=<%= t.get("id") %>">打开</a></td>
        </tr>
        <%         }
        }
        }
        %>

        <!-- 文件夹并列的所有文档 -->
        <%
            if (rows.isEmpty() && !hasFolders) {//没有子文件夹也没有直属文档
        %>
        <tr><td colspan="4" style="text-align:center; color:#999; padding: 30px;">此文件夹为空</td></tr>
        <% } else {//servlet里List<Map<String, String>> rows = DocDao.list(projectId, taskId, status, keyword, orderBy, isRecycle, Web.uid(req), Web.isAdmin(req));
                //已经筛选好需要的文档了，只需要画出即可
            for (int ri = 0; ri < rows.size(); ri++) {
                Map<String, String> r = rows.get(ri);
                long size = 0; try { size = Long.parseLong(r.get("size")); } catch (Exception e) { }

                String sizeStr = "";
                if (size > 1024 * 1024) {
                    sizeStr = String.format("%.1f MB", size / (1024.0 * 1024.0));
                } else {
                    sizeStr = String.format("%.1f KB", size / 1024.0);
                }
        %>
        <tr>
            <!-- 画文档 -->
            <td style="font-size: 15px;">📄 <%= h(r.get("name")) %></td>
            <td><%= sizeStr %></td>
            <td><%= h(r.get("create_time")) %></td>
            <td>
                <a class="btn btn-sm" href="<%= ctx %>/doc?action=preview&id=<%= r.get("id") %>" target="_blank">预览</a>
                <a class="btn btn-sm" href="<%= ctx %>/doc?action=download&id=<%= r.get("id") %>">下载</a>
            </td>
        </tr>
        <% } } %>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>