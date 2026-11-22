<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "首页概览"; String active = "home"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    String uid = (String) session.getAttribute("uid");
    boolean all = isAdmin;
    long projCount = ProjectDao.countAll(uid, all);
    long taskTodo = Db.queryLong("select count(*) from pm_task t join pm_project p on p.id=t.project_id"
            + " where t.del_flag=0 and p.del_flag=0 and t.assignee_id=" + Db.esc(uid)
            + " and t.status in ('TODO','DOING')");
    long approvalPending = Db.queryLong("select count(*) from pm_approval where approver_id="
            + Db.esc(uid) + " and status='PENDING'");
    long fineUnpaid = Db.queryLong("select count(*) from pm_fine where user_id="
            + Db.esc(uid) + " and status='UNPAID'");
    List<Map<String, String>> recent = ProjectDao.list("", "", "", "", "new", 1, 6, uid, all);
    List<Map<String, String>> overdue = Db.query(
            "select t.id,t.title,t.due_date,t.progress,p.name as project_name from pm_task t"
                    + " join pm_project p on p.id=t.project_id"
                    + " where t.del_flag=0 and p.del_flag=0 and t.status not in ('DONE','CANCELED')"
                    + " and t.due_date is not null and t.due_date < curdate() and t.assignee_id=" + Db.esc(uid)
                    + " order by t.due_date limit 6");
    List<Map<String, String>> statusRows = ProjectDao.countByStatus(uid, all);
    long maxStatus = 1;
    for (Map<String, String> r : statusRows) {
        try { maxStatus = Math.max(maxStatus, Long.parseLong(r.get("cnt"))); } catch (Exception ignore) { }
    }
%>
<div class="page-title">
    <div>
        <h2>你好，<%= h(curName) %></h2>
        <div class="sub">这是项目概览页，数据实时来自数据库；不同角色看到的范围不同。</div>
    </div>
    <% if (isPm) { %>
    <a class="btn btn-primary" href="<%= ctx %>/project?action=addForm">+ 新建项目</a>
    <% } %>
</div>

<div class="stats">
    <div class="stat">
        <div class="label">可见项目</div>
        <div class="value"><%= projCount %></div>
        <div class="hint">管理员看全部，其他角色看自己负责/参与</div>
    </div>
    <div class="stat">
        <div class="label">我的待办任务</div>
        <div class="value"><%= taskTodo %></div>
        <div class="hint"><a href="<%= ctx %>/task?action=list&assigneeId=<%= h(uid) %>">去处理 →</a></div>
    </div>
    <div class="stat">
        <div class="label">待我审批</div>
        <div class="value"><%= approvalPending %></div>
        <div class="hint"><a href="<%= ctx %>/approval?action=list">去审批 →</a></div>
    </div>
    <div class="stat">
        <div class="label">未缴罚款</div>
        <div class="value"><%= fineUnpaid %></div>
        <div class="hint"><a href="<%= ctx %>/fine?action=list">查看罚款 →</a></div>
    </div>
</div>

<div class="card">
    <h3>项目状态分布</h3>
    <% if (statusRows.isEmpty()) { %>
    <div class="empty">还没有项目数据。</div>
    <% } else { %>
        <% for (Map<String, String> r : statusRows) {
            long c = 0;
            try { c = Long.parseLong(r.get("cnt")); } catch (Exception ignore) { }
            int pct = (int) Math.round(c * 100.0 / maxStatus);
        %>
        <div class="bar-row">
            <div class="bar-label"><%= projStatusText(r.get("status")) %></div>
            <div class="bar-track"><div class="bar-fill" style="width:<%= Math.max(pct, 3) %>%"></div></div>
            <div class="bar-num"><%= c %></div>
        </div>
        <% } %>
    <% } %>
</div>

<div class="card">
    <h3>最近项目</h3>
    <% if (recent.isEmpty()) { %>
    <div class="empty">还没有项目，<% if (isPm) { %>点“新建项目”开始<% } else { %>等待项目经理创建<% } %>。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr><th>编号</th><th>项目名称</th><th>类型</th><th>优先级</th><th>状态</th><th>进度</th><th>截止</th></tr>
        </thead>
        <tbody>
        <% for (Map<String, String> r : recent) {
            int prog = 0;
            try { prog = Integer.parseInt(r.get("progress")); } catch (Exception ignore) { }
        %>
        <tr>
            <td class="nowrap mono"><%= h(r.get("project_no")) %></td>
            <td><a href="<%= ctx %>/project?action=view&id=<%= r.get("id") %>"><%= h(r.get("name")) %></a></td>
            <td><%= typeText(r.get("project_type")) %></td>
            <td><span class="<%= prioBadge(r.get("priority")) %>"><%= prioText(r.get("priority")) %></span></td>
            <td><span class="<%= projStatusBadge(r.get("status")) %>"><%= projStatusText(r.get("status")) %></span></td>
            <td><div class="progress"><i style="width:<%= prog %>%"></i></div></td>
            <td class="nowrap"><%= h(r.get("end_date")) %></td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>

<div class="card">
    <h3>我的逾期任务</h3>
    <% if (overdue.isEmpty()) { %>
    <div class="empty">没有逾期任务，保持住。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>任务</th><th>项目</th><th>截止</th><th>进度</th></tr></thead>
        <tbody>
        <% for (Map<String, String> r : overdue) {
            int prog = 0;
            try { prog = Integer.parseInt(r.get("progress")); } catch (Exception ignore) { }
        %>
        <tr>
            <td><a href="<%= ctx %>/task?action=editForm&id=<%= r.get("id") %>"><%= h(r.get("title")) %></a></td>
            <td><%= h(r.get("project_name")) %></td>
            <td class="nowrap"><%= h(r.get("due_date")) %></td>
            <td><div class="progress"><i style="width:<%= prog %>%"></i></div></td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
