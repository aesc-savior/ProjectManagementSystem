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
    String attachName = edit ? row.get("attach_name") : "";
    boolean locked = "SUBMITTED".equals(status) || "REVIEWED".equals(status);
%>
<div class="page-title">
    <div>
        <h2><%= edit ? "编辑周报" : "填写周报" %></h2>
        <div class="sub">状态：<span class="<%= weekStatusBadge(status) %>"><%= weekStatusText(status) %></span>
            ；提交后需先撤回或由项目经理退回才能修改。</div>
    </div>
    <div class="flex">
        <% if (edit) { %>
        <a class="btn" href="<%= ctx %>/weekly?action=print&id=<%= id %>" target="_blank">打印</a>
        <a class="btn" href="<%= ctx %>/weekly?action=export&scope=one&id=<%= id %>">导出</a>
        <% } %>
        <a class="btn" href="<%= ctx %>/weekly?action=list">返回列表</a>
    </div>
</div>

<% if (edit && !row.get("review").isEmpty()) { %>
<div class="alert <%= "REJECTED".equals(status) ? "alert-error" : "alert-ok" %>">
    审核意见：<%= h(row.get("review")) %>
    <% if (!row.get("score").isEmpty()) { %>（评分 <%= h(row.get("score")) %>）<% } %>
    <% if (!row.get("reviewer").isEmpty()) { %> — <%= h(row.get("reviewer")) %> <%= h(row.get("review_time")) %><% } %>
</div>
<% } %>

<div class="card">
    <form method="post" action="<%= ctx %>/weekly?action=save" enctype="multipart/form-data">
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
                <input type="text" name="weekNo" value="<%= h(vWeek) %>" placeholder="2026-W46" pattern="\d{4}-W\d{2}" <%= locked ? "readonly" : "" %>>
                <span class="hint">格式：年份-W周序号，例如 2026-W46</span>
            </div>
            <div class="field">
                <label>本人工时（小时）</label>
                <input type="number" step="0.5" min="0" name="hours" value="<%= h(vHours) %>" <%= locked ? "readonly" : "" %>>
                <span class="hint">保存后列表按当前筛选条件自动汇总工时合计</span>
            </div>
            <div class="field">
                <label>附件<%= edit && !attachName.isEmpty() ? "（已上传）" : "" %></label>
                <% if (edit && !attachName.isEmpty()) { %>
                <div class="flex">
                    <a class="btn btn-sm" href="<%= ctx %>/weekly?action=attach&id=<%= id %>"><%= h(attachName) %></a>
                    <span class="muted"><%= h(row.get("attach_size")) %> 字节</span>
                </div>
                <% } %>
                <% if (!locked) { %>
                <input type="file" name="file">
                <span class="hint">支持 doc/docx/xls/xlsx/pdf/ppt/pptx/txt/png/jpg/zip，单个不超过 20MB；重新上传会覆盖原附件</span>
                <% } %>
            </div>
            <div class="field full">
                <label>本周完成 *</label>
                <textarea name="done" placeholder="完成的任务、里程碑与交付物" <%= locked ? "readonly" : "" %>><%= h(vDone) %></textarea>
            </div>
            <div class="field full">
                <label>下周计划</label>
                <textarea name="plan" placeholder="下周要推进的事项与目标" <%= locked ? "readonly" : "" %>><%= h(vPlan) %></textarea>
            </div>
            <div class="field full">
                <label>问题与风险</label>
                <textarea name="issue" placeholder="阻塞、依赖、需要协调的资源" <%= locked ? "readonly" : "" %>><%= h(vIssue) %></textarea>
            </div>
        </div>
        <div class="mt flex">
            <% if (!locked) { %>
            <button class="btn btn-primary" type="submit">保存草稿</button>
            <% } else { %>
            <span class="muted">周报已提交，内容只读；如需修改请先撤回。</span>
            <% } %>
            <a class="btn" href="<%= ctx %>/weekly?action=list">取消</a>
        </div>
    </form>
</div>

<% if (edit && !attachName.isEmpty() && !locked) { %>
<div class="card">
    <h3>附件管理</h3>
    <form method="post" action="<%= ctx %>/weekly?action=deleteAttach&id=<%= id %>"
          onsubmit="return confirm('确认删除该附件？')">
        <button class="btn btn-danger" type="submit">删除附件</button>
    </form>
</div>
<% } %>

<% if (edit && ("DRAFT".equals(status) || "REJECTED".equals(status))) { %>
<div class="card">
    <h3>提交</h3>
    <div class="muted mb">提交后内容锁定，项目经理审核前可撤回。</div>
    <form method="post" action="<%= ctx %>/weekly?action=submit&id=<%= id %>"
          onsubmit="return confirm('提交后不可直接修改，确认提交？')">
        <button class="btn btn-primary" type="submit">提交周报</button>
    </form>
</div>
<% } %>

<% if (edit && "SUBMITTED".equals(status)) { %>
<div class="card">
    <h3>撤回</h3>
    <div class="muted mb">撤回后周报回到草稿态，可继续修改后重新提交。</div>
    <form method="post" action="<%= ctx %>/weekly?action=withdraw&id=<%= id %>"
          onsubmit="return confirm('确认撤回本次提交？')">
        <button class="btn" type="submit">撤回提交</button>
    </form>
</div>
<% } %>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
