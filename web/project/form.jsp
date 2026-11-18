<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "项目编辑"; String active = "project"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    Map<String, String> row = (Map<String, String>) request.getAttribute("row");
    List<Map<String, String>> users = (List<Map<String, String>>) request.getAttribute("users");
    if (users == null) users = new ArrayList<Map<String, String>>();
    boolean edit = row != null;
    String id = edit ? row.get("id") : "";
    String vName = edit ? row.get("name") : "";
    String vType = edit ? row.get("project_type") : "RND";
    String vPrio = edit ? row.get("priority") : "MID";
    String vStatus = edit ? row.get("status") : "PENDING";
    String vStart = edit ? row.get("start_date") : "";
    String vEnd = edit ? row.get("end_date") : "";
    String myId = (String) session.getAttribute("uid");
    String vOwner = edit ? row.get("owner_id") : myId;
    if (vOwner == null || vOwner.isEmpty()) vOwner = myId;
    String vBudget = edit ? row.get("budget") : "0";
    String vDept = edit ? row.get("dept_name") : "";
    String vDesc = edit ? row.get("description") : "";
%>
<div class="page-title">
    <div>
        <h2><%= edit ? "编辑项目" : "新建项目" %></h2>
        <div class="sub">带 * 为必填；项目编号由系统按「PM-年份-4 位流水」自动生成。</div>
    </div>
    <a class="btn" href="<%= ctx %>/project?action=list">返回列表</a>
</div>

<div class="card">
    <form method="post" action="<%= ctx %>/project?action=save">
        <input type="hidden" name="id" value="<%= h(id) %>">
        <div class="form-grid">
            <div class="field full">
                <label>项目名称 *</label>
                <input type="text" name="name" value="<%= h(vName) %>" maxlength="200" placeholder="例如：官网改版项目">
            </div>
            <div class="field">
                <label>项目类型 *</label>
                <select name="projectType">
                    <option value="RND" <%= "RND".equals(vType) ? "selected" : "" %>>研发</option>
                    <option value="BUILD" <%= "BUILD".equals(vType) ? "selected" : "" %>>建设</option>
                    <option value="SERVICE" <%= "SERVICE".equals(vType) ? "selected" : "" %>>服务</option>
                    <option value="OTHER" <%= "OTHER".equals(vType) ? "selected" : "" %>>其他</option>
                </select>
            </div>
            <div class="field">
                <label>优先级 *</label>
                <select name="priority">
                    <option value="HIGH" <%= "HIGH".equals(vPrio) ? "selected" : "" %>>高</option>
                    <option value="MID" <%= "MID".equals(vPrio) ? "selected" : "" %>>中</option>
                    <option value="LOW" <%= "LOW".equals(vPrio) ? "selected" : "" %>>低</option>
                </select>
            </div>
            <div class="field">
                <label>负责人 *</label>
                <select name="ownerId">
                    <% for (Map<String, String> u : users) { %>
                    <option value="<%= u.get("id") %>" <%= u.get("id").equals(vOwner) ? "selected" : "" %>>
                        <%= h(u.get("user_name")) %>（<%= roleTextOf(u.get("role_code")) %>）
                    </option>
                    <% } %>
                </select>
            </div>
            <div class="field">
                <label>状态</label>
                <select name="status">
                    <option value="PENDING" <%= "PENDING".equals(vStatus) ? "selected" : "" %>>待启动</option>
                    <option value="RUNNING" <%= "RUNNING".equals(vStatus) ? "selected" : "" %>>进行中</option>
                    <option value="PAUSED" <%= "PAUSED".equals(vStatus) ? "selected" : "" %>>暂停</option>
                    <option value="DONE" <%= "DONE".equals(vStatus) ? "selected" : "" %>>已完成</option>
                    <option value="CANCELED" <%= "CANCELED".equals(vStatus) ? "selected" : "" %>>已取消</option>
                    <option value="ARCHIVED" <%= "ARCHIVED".equals(vStatus) ? "selected" : "" %>>已归档</option>
                </select>
            </div>
            <div class="field">
                <label>开始日期</label>
                <input type="date" name="startDate" value="<%= h(vStart) %>">
            </div>
            <div class="field">
                <label>结束日期</label>
                <input type="date" name="endDate" value="<%= h(vEnd) %>">
            </div>
            <div class="field">
                <label>预算（元）</label>
                <input type="number" name="budget" step="0.01" min="0" value="<%= h(vBudget) %>">
            </div>
            <div class="field">
                <label>所属部门</label>
                <input type="text" name="deptName" value="<%= h(vDept) %>" placeholder="例如：研发部">
            </div>
            <div class="field full">
                <label>项目描述</label>
                <textarea name="description" placeholder="目标、范围、关键交付物"><%= h(vDesc) %></textarea>
            </div>
        </div>
        <div class="mt flex">
            <button class="btn btn-primary" type="submit">保存</button>
            <a class="btn" href="<%= ctx %>/project?action=list">取消</a>
        </div>
    </form>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
