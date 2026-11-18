<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "任务编辑"; String active = "task"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    Map<String, String> row = (Map<String, String>) request.getAttribute("row");
    List<Map<String, String>> projects = (List<Map<String, String>>) request.getAttribute("projects");
    List<Map<String, String>> users = (List<Map<String, String>>) request.getAttribute("users");
    List<Map<String, String>> parents = (List<Map<String, String>>) request.getAttribute("parents");
    if (projects == null) projects = new ArrayList<Map<String, String>>();
    if (users == null) users = new ArrayList<Map<String, String>>();
    if (parents == null) parents = new ArrayList<Map<String, String>>();
    boolean edit = row != null;
    String id = edit ? row.get("id") : "";
    String vProject = edit ? row.get("project_id") : (String) request.getAttribute("projectId");
    if (vProject == null) vProject = "";
    String vParent = edit ? row.get("parent_id") : "0";
    String vTitle = edit ? row.get("title") : "";
    String vDesc = edit ? row.get("description") : "";
    String vAssignee = edit ? row.get("assignee_id") : "";
    String vPrio = edit ? row.get("priority") : "MID";
    String vStatus = edit ? row.get("status") : "TODO";
    String vProgress = edit ? row.get("progress") : "0";
    String vStart = edit ? row.get("start_date") : "";
    String vDue = edit ? row.get("due_date") : "";
    String vEstimate = edit ? row.get("estimate_hours") : "0";
    String vActual = edit ? row.get("actual_hours") : "0";
    String uid = (String) session.getAttribute("uid");
%>
<div class="page-title">
    <div>
        <h2><%= edit ? "编辑任务" : "新建任务" %></h2>
        <div class="sub">父任务下可继续拆子任务；父任务进度按子任务平均自动汇总。</div>
    </div>
    <a class="btn" href="<%= ctx %>/task?action=list">返回列表</a>
</div>

<div class="card">
    <form method="post" action="<%= ctx %>/task?action=save">
        <input type="hidden" name="id" value="<%= h(id) %>">
        <div class="form-grid">
            <div class="field">
                <label>所属项目 *</label>
                <select name="projectId">
                    <% for (Map<String, String> p : projects) { %>
                    <option value="<%= p.get("id") %>" <%= p.get("id").equals(vProject) ? "selected" : "" %>><%= h(p.get("name")) %></option>
                    <% } %>
                </select>
            </div>
            <div class="field">
                <label>父任务（WBS）</label>
                <select name="parentId">
                    <option value="0">无（顶层任务）</option>
                    <% for (Map<String, String> p : parents) {
                        if (edit && p.get("id").equals(id)) continue; %>
                    <option value="<%= p.get("id") %>" <%= p.get("id").equals(vParent) ? "selected" : "" %>><%= h(p.get("title")) %></option>
                    <% } %>
                </select>
            </div>
            <div class="field full">
                <label>任务标题 *</label>
                <input type="text" name="title" value="<%= h(vTitle) %>" maxlength="200" placeholder="例如：首页视觉设计">
            </div>
            <div class="field">
                <label>负责人</label>
                <select name="assigneeId">
                    <option value="">未分配</option>
                    <% for (Map<String, String> u : users) { %>
                    <option value="<%= u.get("id") %>" <%= u.get("id").equals(vAssignee) ? "selected" : "" %>><%= h(u.get("user_name")) %></option>
                    <% } %>
                </select>
            </div>
            <div class="field">
                <label>优先级</label>
                <select name="priority">
                    <option value="HIGH" <%= "HIGH".equals(vPrio) ? "selected" : "" %>>高</option>
                    <option value="MID" <%= "MID".equals(vPrio) ? "selected" : "" %>>中</option>
                    <option value="LOW" <%= "LOW".equals(vPrio) ? "selected" : "" %>>低</option>
                </select>
            </div>
            <div class="field">
                <label>状态</label>
                <select name="status">
                    <option value="TODO" <%= "TODO".equals(vStatus) ? "selected" : "" %>>待开始</option>
                    <option value="DOING" <%= "DOING".equals(vStatus) ? "selected" : "" %>>进行中</option>
                    <option value="PAUSED" <%= "PAUSED".equals(vStatus) ? "selected" : "" %>>暂停</option>
                    <option value="DONE" <%= "DONE".equals(vStatus) ? "selected" : "" %>>已完成</option>
                    <option value="CANCELED" <%= "CANCELED".equals(vStatus) ? "selected" : "" %>>已取消</option>
                </select>
            </div>
            <div class="field">
                <label>完成百分比</label>
                <input type="number" name="progress" min="0" max="100" value="<%= h(vProgress) %>">
            </div>
            <div class="field">
                <label>开始日期</label>
                <input type="date" name="startDate" value="<%= h(vStart) %>">
            </div>
            <div class="field">
                <label>截止日期</label>
                <input type="date" name="dueDate" value="<%= h(vDue) %>">
            </div>
            <div class="field">
                <label>预估工时（小时）</label>
                <input type="number" step="0.5" min="0" name="estimateHours" value="<%= h(vEstimate) %>">
            </div>
            <div class="field">
                <label>实际工时（小时）</label>
                <input type="number" step="0.5" min="0" name="actualHours" value="<%= h(vActual) %>">
            </div>
            <div class="field full">
                <label>任务描述</label>
                <textarea name="description" placeholder="工作内容、验收标准、依赖说明"><%= h(vDesc) %></textarea>
            </div>
        </div>
        <div class="mt flex">
            <button class="btn btn-primary" type="submit">保存</button>
            <a class="btn" href="<%= ctx %>/task?action=list">取消</a>
            <% if (edit) { %>
            <a class="btn" href="<%= ctx %>/approval?action=applyForm&taskId=<%= id %>">申请延期</a>
            <% } %>
        </div>
    </form>
</div>

<% if (edit) { %>
<div class="card">
    <h3>快速更新进度</h3>
    <form class="flex" method="post" action="<%= ctx %>/task?action=progress&id=<%= id %>">
        <div class="field" style="width:150px">
            <label>完成百分比</label>
            <input type="number" name="progress" min="0" max="100" value="<%= h(vProgress) %>">
        </div>
        <div class="field" style="width:150px">
            <label>实际工时</label>
            <input type="number" step="0.5" min="0" name="actualHours" value="<%= h(vActual) %>">
        </div>
        <div class="field" style="width:110px">
            <label>&nbsp;</label>
            <button class="btn btn-primary" type="submit">更新</button>
        </div>
    </form>
    <div class="hint mt">负责人或项目经理可更新；100% 会自动置为“已完成”，父任务进度随之汇总。</div>
</div>
<% } %>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
