<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*,java.text.DecimalFormat" %>
<% String pageTitle = "周报提交统计"; String active = "weekly"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> statRows = (List<Map<String, String>>) request.getAttribute("statRows");
    if (statRows == null) statRows = new ArrayList<Map<String, String>>();
    List<Map<String, String>> missing = (List<Map<String, String>>) request.getAttribute("missing");
    if (missing == null) missing = new ArrayList<Map<String, String>>();
    List<Map<String, String>> trend = (List<Map<String, String>>) request.getAttribute("trend");
    if (trend == null) trend = new ArrayList<Map<String, String>>();
    String weekNo = (String) request.getAttribute("weekNo");
    if (weekNo == null) weekNo = WeeklyDao.currentWeek();
    String fProjectId = (String) request.getAttribute("fProjectId");
    if (fProjectId == null) fProjectId = "";

    long dueAll = 0, doneAll = 0;
    for (Map<String, String> r : statRows) {
        try { dueAll += Long.parseLong(r.get("due_cnt")); } catch (Exception ignore) { }
        try { doneAll += Long.parseLong(r.get("done_cnt")); } catch (Exception ignore) { }
    }
    long missAll = Math.max(0, dueAll - doneAll);
    double rate = dueAll == 0 ? 0 : doneAll * 100.0 / dueAll;
    DecimalFormat df = new DecimalFormat("0.0");
%>
<div class="page-title">
    <div>
        <h2>周报提交统计</h2>
        <div class="sub">周次 <b><%= h(weekNo) %></b>；应交 <b><%= dueAll %></b> 人，已交 <b><%= doneAll %></b> 人，未交 <b><%= missAll %></b> 人。</div>
    </div>
    <div class="flex">
        <a class="btn" href="<%= ctx %>/weekly?action=list&weekNo=<%= h(weekNo) %>">返回列表</a>
        <a class="btn" href="<%= ctx %>/weekly?action=export&scope=missing&weekNo=<%= h(weekNo) %>">导出未交名单</a>
        <% if (isPm) { %>
        <form method="post" action="<%= ctx %>/weekly?action=remind" style="display:inline"
              onsubmit="return confirm('将给未交成员与项目经理各生成一条站内提醒，确认发送？')">
            <input type="hidden" name="weekNo" value="<%= h(weekNo) %>">
            <button class="btn btn-primary" type="submit">一键催交</button>
        </form>
        <% } %>
    </div>
</div>

<div class="stats">
    <div class="stat">
        <div class="label">应交人数</div>
        <div class="value"><%= dueAll %></div>
        <div class="hint">进行中/暂停项目的成员（不含查看者）</div>
    </div>
    <div class="stat">
        <div class="label">已交人数</div>
        <div class="value"><%= doneAll %></div>
        <div class="hint">已提交或已审核</div>
    </div>
    <div class="stat">
        <div class="label">未交人数</div>
        <div class="value"><%= missAll %></div>
        <div class="hint">需催交</div>
    </div>
    <div class="stat">
        <div class="label">提交率</div>
        <div class="value"><%= df.format(rate) %>%</div>
        <div class="hint"><%= "≥ 90% 视为达标" %></div>
    </div>
</div>

<div class="card">
    <h3>按项目提交情况</h3>
    <% if (statRows.isEmpty()) { %>
    <div class="empty">当前没有进行中的项目。</div>
    <% } else { %>
    <% for (Map<String, String> r : statRows) {
        long due = 0, done = 0;
        try { due = Long.parseLong(r.get("due_cnt")); } catch (Exception ignore) { }
        try { done = Long.parseLong(r.get("done_cnt")); } catch (Exception ignore) { }
        int pct = due == 0 ? 0 : (int) Math.round(done * 100.0 / due);
    %>
    <div class="bar-row">
        <div class="bar-label" title="<%= h(r.get("project_name")) %>"><%= h(r.get("project_no")) %></div>
        <div class="bar-track"><div class="bar-fill" style="width:<%= pct %>%"></div></div>
        <div class="bar-num"><%= done %>/<%= due %></div>
        <div style="width:76px;text-align:right">
            <a class="btn btn-sm" href="<%= ctx %>/weekly?action=list&projectId=<%= r.get("project_id") %>&weekNo=<%= h(weekNo) %>">明细</a>
        </div>
    </div>
    <% } %>
    <% } %>
</div>

<div class="card">
    <h3>近 6 周提交趋势</h3>
    <% if (trend.isEmpty()) { %>
    <div class="empty">暂无数据。</div>
    <% } else { %>
    <% for (Map<String, String> t : trend) {
        long done = 0, tot = 0;
        try { done = Long.parseLong(t.get("done_cnt")); } catch (Exception ignore) { }
        try { tot = Long.parseLong(t.get("total_cnt")); } catch (Exception ignore) { }
        int pct = tot == 0 ? 0 : (int) Math.round(done * 100.0 / tot);
    %>
    <div class="bar-row">
        <div class="bar-label mono"><%= h(t.get("week_no")) %></div>
        <div class="bar-track"><div class="bar-fill" style="width:<%= pct %>%"></div></div>
        <div class="bar-num"><%= done %>/<%= tot %></div>
    </div>
    <% } %>
    <% } %>
</div>

<div class="card">
    <h3>未交名单（<%= missing.size() %> 人）</h3>
    <% if (missing.isEmpty()) { %>
    <div class="empty">该周全部成员均已提交。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>项目编号</th><th>项目名称</th><th>应交人</th><th>周次</th><th>状态</th><th>操作</th></tr></thead>
        <tbody>
        <% for (Map<String, String> m : missing) { %>
        <tr>
            <td class="mono"><%= h(m.get("project_no")) %></td>
            <td><%= h(m.get("project_name")) %></td>
            <td><%= h(m.get("user_name")) %></td>
            <td class="mono"><%= h(weekNo) %></td>
            <td><span class="badge badge-rejected">未交</span></td>
            <td class="ops">
                <a class="btn btn-sm" href="<%= ctx %>/weekly?action=list&projectId=<%= m.get("project_id") %>&userId=<%= m.get("user_id") %>&weekNo=<%= h(weekNo) %>">查看记录</a>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
