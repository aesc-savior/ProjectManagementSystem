<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "填写周报"; String active = "weekly"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    Map<String, String> row = (Map<String, String>) request.getAttribute("row");
    List<Map<String, String>> projects = (List<Map<String, String>>) request.getAttribute("projects");
    if (projects == null) projects = new ArrayList<Map<String, String>>();
    boolean edit = row != null;
    String id = edit ? row.get("id") : "";
    String vProject = edit ? row.get("project_id") : (String) request.getAttribute("projectId");
    if (vProject == null) vProject = "";
    String vWeek = edit ? row.get("week_no") : (String) request.getAttribute("weekNo");
    if (vWeek == null) vWeek = "";
    String vDone = edit ? row.get("done_text") : "";
    String vPlan = edit ? row.get("plan_text") : "";
    String vIssue = edit ? row.get("issue_text") : "";
    String vHours = edit ? row.get("hours") : "0";
    String status = edit ? row.get("status") : "DRAFT";
%>
<div class="page-title">
    <div>
        <h2><%= edit ? "编辑周报" : "填写周报" %></h2>
        <div class="sub">状态：<span class="<%= weekStatusBadge(status) %>"><%= weekStatusText(status) %></span>；提交后需项目经理退回才能修改。</div>
    </div>
    <a class="btn" href="<%= ctx %>/weekly?action=list">返回列表</a>
</div>

<div class="card">
    <form method="post" action="<%= ctx %>/weekly?action=save">
        <input type="hidden" name="id" value="<%= h(id) %>">
        <div class="form-grid">
            <div class="field">
                <label>所属项目 *</label>
                <select name="projectId" <%= edit ? "disabled" : "" %>>
                    <% for (Map<String, String> p : projects) { %>
                    <option value="<%= p.get("id") %>" <%= p.get("id").equals(vProject) ? "selected" : "" %>><%= h(p.get("name")) %></option>
                    <% } %>
                </select>
                <% if (edit) { %><input type="hidden" name="projectId" value="<%= h(vProject) %>"><% } %>
            </div>
            <div class="field">
                <label>周次 *</label>
                <input type="text" name="weekNo" value="<%= h(vWeek) %>" placeholder="2026-W46" pattern="\d{4}-W\d{2}">
                <span class="hint">格式：年份-W周序号，例如 2026-W46</span>
            </div>
            <div class="field">
                <label>本人工时（小时）</label>
                <input type="number" step="0.5" min="0" name="hours" value="<%= h(vHours) %>">
            </div>
            <div class="field full">
                <label>本周完成 *</label>
                <textarea name="done" placeholder="完成的任务、里程碑与交付物"><%= h(vDone) %></textarea>
            </div>
            <div class="field full">
                <label>下周计划</label>
                <textarea name="plan" placeholder="下周要推进的事项与目标"><%= h(vPlan) %></textarea>
            </div>
            <div class="field full">
                <label>问题与风险</label>
                <textarea name="issue" placeholder="阻塞、依赖、需要协调的资源"><%= h(vIssue) %></textarea>
            </div>
        </div>
        <div class="mt flex">
            <button class="btn btn-primary" type="submit">保存草稿</button>
            <a class="btn" href="<%= ctx %>/weekly?action=list">取消</a>
        </div>
    </form>
</div>

<% if (edit && ("DRAFT".equals(status) || "REJECTED".equals(status))) { %>
<div class="card">
    <h3>提交</h3>
    <form method="post" action="<%= ctx %>/weekly?action=submit&id=<%= id %>"
          onsubmit="return confirm('提交后不可直接修改，确认提交？')">
        <button class="btn btn-primary" type="submit">提交周报</button>
    </form>
</div>
<% } %>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
