<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "项目周报"; String active = "weekly"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    List<Map<String, String>> projects = (List<Map<String, String>>) request.getAttribute("projects");
    if (projects == null) projects = new ArrayList<Map<String, String>>();
    List<Map<String, String>> users = (List<Map<String, String>>) request.getAttribute("users");
    if (users == null) users = new ArrayList<Map<String, String>>();
    long total = request.getAttribute("total") == null ? 0 : (Long) request.getAttribute("total");
    int pageNo = request.getAttribute("page") == null ? 1 : (Integer) request.getAttribute("page");
    int pages = request.getAttribute("pages") == null ? 1 : (Integer) request.getAttribute("pages");
    String fProjectId = (String) request.getAttribute("fProjectId");
    String fUserId = (String) request.getAttribute("fUserId");
    String fWeekNo = (String) request.getAttribute("fWeekNo");
    String fStatus = (String) request.getAttribute("fStatus");
    String currentWeek = (String) request.getAttribute("currentWeek");
    if (fProjectId == null) fProjectId = "";
    if (fUserId == null) fUserId = "";
    if (fWeekNo == null) fWeekNo = "";
    if (fStatus == null) fStatus = "";
    if (currentWeek == null) currentWeek = "";
    String uid = (String) session.getAttribute("uid");
    String qs = "&projectId=" + fProjectId + "&userId=" + fUserId + "&weekNo=" + fWeekNo + "&status=" + fStatus;
%>
<div class="page-title">
    <div>
        <h2>项目周报</h2>
        <div class="sub">共 <b><%= total %></b> 份；当前周次 <b><%= h(currentWeek) %></b>，同一人同一项目每周一份。</div>
    </div>
    <% if (!projects.isEmpty() && !isViewer) { %>
    <a class="btn btn-primary" href="<%= ctx %>/weekly?action=addForm&projectId=<%= fProjectId.isEmpty() ? projects.get(0).get("id") : fProjectId %>">+ 填写周报</a>
    <% } %>
</div>

<div class="card">
    <form class="filters" method="get" action="<%= ctx %>/weekly">
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
        <div class="field">
            <label>成员</label>
            <select name="userId">
                <option value="">全部</option>
                <% for (Map<String, String> u : users) { %>
                <option value="<%= u.get("id") %>" <%= u.get("id").equals(fUserId) ? "selected" : "" %>><%= h(u.get("user_name")) %></option>
                <% } %>
            </select>
        </div>
        <div class="field">
            <label>周次</label>
            <input type="text" name="weekNo" value="<%= h(fWeekNo) %>" placeholder="2026-W46" style="width:110px">
        </div>
        <div class="field">
            <label>状态</label>
            <select name="status">
                <option value="">全部</option>
                <option value="DRAFT" <%= "DRAFT".equals(fStatus) ? "selected" : "" %>>草稿</option>
                <option value="SUBMITTED" <%= "SUBMITTED".equals(fStatus) ? "selected" : "" %>>已提交</option>
                <option value="REVIEWED" <%= "REVIEWED".equals(fStatus) ? "selected" : "" %>>已审核</option>
                <option value="REJECTED" <%= "REJECTED".equals(fStatus) ? "selected" : "" %>>已退回</option>
            </select>
        </div>
        <div class="field">
            <label>&nbsp;</label>
            <div class="flex">
                <button class="btn btn-primary" type="submit">查询</button>
                <a class="btn" href="<%= ctx %>/weekly?action=list">重置</a>
            </div>
        </div>
    </form>
</div>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">还没有周报记录。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr><th>项目</th><th>成员</th><th>周次</th><th>工时</th><th>状态</th><th>提交时间</th><th>评语</th><th>操作</th></tr>
        </thead>
        <tbody>
        <% for (Map<String, String> r : rows) {
            String wid = r.get("id");
            boolean mine = uid.equals(r.get("user_id"));
            boolean canReview = isPm && ProjectDao.canManage(r.get("project_id"), uid, curRole, isAdmin);
            boolean editable = mine && ("DRAFT".equals(r.get("status")) || "REJECTED".equals(r.get("status")));
        %>
        <tr>
            <td><a href="<%= ctx %>/project?action=view&id=<%= r.get("project_id") %>"><%= h(r.get("project_name")) %></a></td>
            <td><%= h(r.get("user_name")) %></td>
            <td class="mono"><%= h(r.get("week_no")) %></td>
            <td><%= h(r.get("hours")) %> h</td>
            <td><span class="<%= weekStatusBadge(r.get("status")) %>"><%= weekStatusText(r.get("status")) %></span></td>
            <td class="nowrap"><%= h(r.get("submit_time")) %></td>
            <td><%= h(r.get("review")) %><% if (!r.get("score").isEmpty()) { %> <span class="tag"><%= h(r.get("score")) %> 分</span><% } %></td>
            <td class="ops">
                <% if (editable) { %>
                <a class="btn btn-sm" href="<%= ctx %>/weekly?action=editForm&id=<%= wid %>">编辑</a>
                <form method="post" action="<%= ctx %>/weekly?action=submit&id=<%= wid %>" style="display:inline"
                      onsubmit="return confirm('提交后不可直接修改，确认提交？')">
                    <button class="btn btn-sm btn-primary" type="submit">提交</button>
                </form>
                <% } %>
                <% if (canReview && "SUBMITTED".equals(r.get("status"))) { %>
                <form method="post" action="<%= ctx %>/weekly?action=review&id=<%= wid %>" class="flex" style="margin-top:4px">
                    <input type="text" name="review" placeholder="评语" style="width:120px;padding:3px 6px">
                    <input type="number" name="score" min="0" max="100" placeholder="分" style="width:52px;padding:3px 6px">
                    <button class="btn btn-sm btn-primary" type="submit" name="decision" value="approve">通过</button>
                    <button class="btn btn-sm btn-danger" type="submit" name="decision" value="reject">退回</button>
                </form>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <div class="pager">
        <span class="info">第 <%= pageNo %> / <%= Math.max(pages, 1) %> 页，共 <%= total %> 条</span>
        <% if (pageNo > 1) { %>
        <a href="<%= ctx %>/weekly?action=list&page=<%= pageNo - 1 %><%= qs %>">上一页</a>
        <% } %>
        <% if (pageNo < pages) { %>
        <a href="<%= ctx %>/weekly?action=list&page=<%= pageNo + 1 %><%= qs %>">下一页</a>
        <% } %>
    </div>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
