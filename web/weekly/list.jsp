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
    List<Map<String, String>> missing = (List<Map<String, String>>) request.getAttribute("missing");
    if (missing == null) missing = new ArrayList<Map<String, String>>();
    long total = request.getAttribute("total") == null ? 0 : (Long) request.getAttribute("total");
    int pageNo = request.getAttribute("page") == null ? 1 : (Integer) request.getAttribute("page");
    int pages = request.getAttribute("pages") == null ? 1 : (Integer) request.getAttribute("pages");
    String fProjectId = (String) request.getAttribute("fProjectId");
    String fUserId = (String) request.getAttribute("fUserId");
    String fWeekNo = (String) request.getAttribute("fWeekNo");
    String fStatus = (String) request.getAttribute("fStatus");
    String fSort = (String) request.getAttribute("fSort");
    String fOrder = (String) request.getAttribute("fOrder");
    String currentWeek = (String) request.getAttribute("currentWeek");
    String statWeek = (String) request.getAttribute("statWeek");
    String sumHours = (String) request.getAttribute("sumHours");
    if (fProjectId == null) fProjectId = "";
    if (fUserId == null) fUserId = "";
    if (fWeekNo == null) fWeekNo = "";
    if (fStatus == null) fStatus = "";
    if (fSort == null) fSort = "";
    if (fOrder == null) fOrder = "";
    if (currentWeek == null) currentWeek = "";
    if (statWeek == null) statWeek = currentWeek;
    if (sumHours == null) sumHours = "0";
    String uid = (String) session.getAttribute("uid");
    String qs = "&projectId=" + fProjectId + "&userId=" + fUserId + "&weekNo=" + fWeekNo
            + "&status=" + fStatus + "&sort=" + fSort + "&order=" + fOrder;
    String expQs = "&projectId=" + fProjectId + "&userId=" + fUserId + "&weekNo=" + fWeekNo + "&status=" + fStatus;
    String baseQs = expQs;
    String nextOrder = "asc".equals(fOrder) ? "desc" : "asc";
%>
<div class="page-title">
    <div>
        <h2>项目周报</h2>
        <div class="sub">共 <b><%= total %></b> 份，工时合计 <b><%= h(sumHours) %></b> 小时；当前周次 <b><%= h(currentWeek) %></b>，同一人同一项目每周一份。</div>
    </div>
    <div class="flex">
        <a class="btn" href="<%= ctx %>/weekly?action=stat&weekNo=<%= h(statWeek) %>">提交统计</a>
        <a class="btn" href="<%= ctx %>/weekly?action=export&scope=list<%= expQs %>">导出 Excel</a>
        <% if (!projects.isEmpty() && !isViewer) { %>
        <a class="btn btn-primary" href="<%= ctx %>/weekly?action=addForm&projectId=<%= fProjectId.isEmpty() ? projects.get(0).get("id") : fProjectId %>">+ 填写周报</a>
        <% } %>
    </div>
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
            <label>排序</label>
            <select name="sort">
                <option value="" <%= fSort.isEmpty() ? "selected" : "" %>>周次</option>
                <option value="project" <%= "project".equals(fSort) ? "selected" : "" %>>项目</option>
                <option value="member" <%= "member".equals(fSort) ? "selected" : "" %>>成员</option>
                <option value="hours" <%= "hours".equals(fSort) ? "selected" : "" %>>工时</option>
                <option value="status" <%= "status".equals(fSort) ? "selected" : "" %>>状态</option>
                <option value="submit" <%= "submit".equals(fSort) ? "selected" : "" %>>提交时间</option>
            </select>
        </div>
        <div class="field">
            <label>方向</label>
            <select name="order">
                <option value="desc" <%= !"asc".equals(fOrder) ? "selected" : "" %>>降序</option>
                <option value="asc" <%= "asc".equals(fOrder) ? "selected" : "" %>>升序</option>
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

<% if (!missing.isEmpty()) { %>
<div class="card">
    <div class="flex" style="justify-content:space-between;align-items:flex-start">
        <div>
            <h3 style="margin:0 0 6px">应交未交名单 · <%= h(statWeek) %></h3>
            <div class="sub muted">共 <b><%= missing.size() %></b> 人未提交，列表中已标记「未交」。</div>
        </div>
        <div class="flex">
            <a class="btn btn-sm" href="<%= ctx %>/weekly?action=export&scope=missing&weekNo=<%= h(statWeek) %>&projectId=<%= fProjectId %>">导出未交名单</a>
            <% if (isPm) { %>
            <form method="post" action="<%= ctx %>/weekly?action=remind" style="display:inline"
                  onsubmit="return confirm('将给未交成员与项目经理各生成一条站内提醒，确认发送？')">
                <input type="hidden" name="weekNo" value="<%= h(statWeek) %>">
                <input type="hidden" name="projectId" value="<%= fProjectId %>">
                <button class="btn btn-sm btn-primary" type="submit">一键催交</button>
            </form>
            <% } %>
        </div>
    </div>
    <div class="mt flex" style="flex-wrap:wrap">
        <% int shown = 0;
           for (Map<String, String> m : missing) {
               if (shown++ >= 24) { break; } %>
        <span class="tag"><%= h(m.get("user_name")) %> · <%= h(m.get("project_no")) %></span>
        <% } %>
        <% if (missing.size() > 24) { %><span class="muted">等 <%= missing.size() %> 人</span><% } %>
    </div>
</div>
<% } %>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">还没有周报记录。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr>
            <th><a href="<%= ctx %>/weekly?action=list&sort=project&order=<%= "project".equals(fSort) ? nextOrder : "asc" %><%= baseQs %>">项目</a></th>
            <th><a href="<%= ctx %>/weekly?action=list&sort=member&order=<%= "member".equals(fSort) ? nextOrder : "asc" %><%= baseQs %>">成员</a></th>
            <th><a href="<%= ctx %>/weekly?action=list&sort=week&order=<%= "week".equals(fSort) ? nextOrder : "desc" %><%= baseQs %>">周次</a></th>
            <th><a href="<%= ctx %>/weekly?action=list&sort=hours&order=<%= "hours".equals(fSort) ? nextOrder : "desc" %><%= baseQs %>">工时</a></th>
            <th><a href="<%= ctx %>/weekly?action=list&sort=status&order=<%= "status".equals(fSort) ? nextOrder : "asc" %><%= baseQs %>">状态</a></th>
            <th><a href="<%= ctx %>/weekly?action=list&sort=submit&order=<%= "submit".equals(fSort) ? nextOrder : "desc" %><%= baseQs %>">提交时间</a></th>
            <th>评语</th>
            <th>附件</th>
            <th>操作</th>
        </tr>
        </thead>
        <tbody>
        <% for (Map<String, String> r : rows) {
            String wid = r.get("id");
            boolean mine = uid.equals(r.get("user_id"));
            boolean canReview = isPm && ProjectDao.canManage(r.get("project_id"), uid, curRole, isAdmin);
            boolean editable = mine && ("DRAFT".equals(r.get("status")) || "REJECTED".equals(r.get("status")));
            boolean withdrawable = mine && "SUBMITTED".equals(r.get("status"));
            boolean hasAttach = !r.get("attach_name").isEmpty();
        %>
        <tr>
            <td><a href="<%= ctx %>/project?action=view&id=<%= r.get("project_id") %>"><%= h(r.get("project_name")) %></a></td>
            <td><%= h(r.get("user_name")) %></td>
            <td class="mono"><%= h(r.get("week_no")) %></td>
            <td><%= h(r.get("hours")) %> h</td>
            <td><span class="<%= weekStatusBadge(r.get("status")) %>"><%= weekStatusText(r.get("status")) %></span></td>
            <td class="nowrap"><%= h(r.get("submit_time")) %></td>
            <td><%= h(r.get("review")) %><% if (!r.get("score").isEmpty()) { %> <span class="tag"><%= h(r.get("score")) %> 分</span><% } %>
                <% if (!r.get("reviewer").isEmpty()) { %><div class="muted" style="font-size:12px">审核：<%= h(r.get("reviewer")) %></div><% } %></td>
            <td>
                <% if (hasAttach) { %>
                <a class="btn btn-sm" href="<%= ctx %>/weekly?action=attach&id=<%= wid %>" title="<%= h(r.get("attach_name")) %>">下载</a>
                <% } else { %><span class="muted">无</span><% } %>
            </td>
            <td class="ops">
                <a class="btn btn-sm" href="<%= ctx %>/weekly?action=print&id=<%= wid %>" target="_blank">打印</a>
                <a class="btn btn-sm" href="<%= ctx %>/weekly?action=export&scope=one&id=<%= wid %>">导出</a>
                <% if (editable) { %>
                <a class="btn btn-sm" href="<%= ctx %>/weekly?action=editForm&id=<%= wid %>">编辑</a>
                <form method="post" action="<%= ctx %>/weekly?action=submit&id=<%= wid %>" style="display:inline"
                      onsubmit="return confirm('提交后不可直接修改，确认提交？')">
                    <button class="btn btn-sm btn-primary" type="submit">提交</button>
                </form>
                <% } %>
                <% if (withdrawable) { %>
                <form method="post" action="<%= ctx %>/weekly?action=withdraw&id=<%= wid %>" style="display:inline"
                      onsubmit="return confirm('撤回后周报回到草稿态，确认撤回？')">
                    <button class="btn btn-sm" type="submit">撤回</button>
                </form>
                <% } %>
                <% if (canReview && "SUBMITTED".equals(r.get("status"))) { %>
                <form method="post" action="<%= ctx %>/weekly?action=review&id=<%= wid %>" class="flex" style="margin-top:4px">
                    <input type="text" name="review" placeholder="评语（退回必填）" style="width:130px;padding:3px 6px">
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
