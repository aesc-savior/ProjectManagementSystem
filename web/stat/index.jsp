<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "统计报表"; String active = "stat"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    long projectTotal = request.getAttribute("projectTotal") == null ? 0 : (Long) request.getAttribute("projectTotal");
    long taskTotal = request.getAttribute("taskTotal") == null ? 0 : (Long) request.getAttribute("taskTotal");
    long taskDone = request.getAttribute("taskDone") == null ? 0 : (Long) request.getAttribute("taskDone");
    long budget = request.getAttribute("budget") == null ? 0 : (Long) request.getAttribute("budget");
    long unpaidFine = request.getAttribute("unpaidFine") == null ? 0 : (Long) request.getAttribute("unpaidFine");
    long pendingApproval = request.getAttribute("pendingApproval") == null ? 0 : (Long) request.getAttribute("pendingApproval");
    List<Map<String, String>> projectStatus = (List<Map<String, String>>) request.getAttribute("projectStatus");
    List<Map<String, String>> taskStatus = (List<Map<String, String>>) request.getAttribute("taskStatus");
    List<Map<String, String>> weeklyStatus = (List<Map<String, String>>) request.getAttribute("weeklyStatus");
    List<Map<String, String>> monthly = (List<Map<String, String>>) request.getAttribute("monthly");
    if (projectStatus == null) projectStatus = new ArrayList<Map<String, String>>();
    if (taskStatus == null) taskStatus = new ArrayList<Map<String, String>>();
    if (weeklyStatus == null) weeklyStatus = new ArrayList<Map<String, String>>();
    if (monthly == null) monthly = new ArrayList<Map<String, String>>();
    int doneRate = taskTotal == 0 ? 0 : (int) Math.round(taskDone * 100.0 / taskTotal);
%>
<div class="page-title">
    <div>
        <h2>统计报表</h2>
        <div class="sub">数据按当前用户可见范围统计；图表为纯 CSS 条形图，打印时一并输出。</div>
    </div>
</div>

<div class="stats">
    <div class="stat"><div class="label">项目总数</div><div class="value"><%= projectTotal %></div><div class="hint">预算合计 <%= money(String.valueOf(budget)) %> 元</div></div>
    <div class="stat"><div class="label">任务完成率</div><div class="value"><%= doneRate %>%</div><div class="hint"><%= taskDone %> / <%= taskTotal %> 个任务</div></div>
    <div class="stat"><div class="label">待我审批</div><div class="value"><%= pendingApproval %></div><div class="hint"><a href="<%= ctx %>/approval?action=list">去处理 →</a></div></div>
    <div class="stat"><div class="label">未缴罚款</div><div class="value"><%= money(String.valueOf(unpaidFine)) %></div><div class="hint">元</div></div>
</div>

<div class="card">
    <h3>项目状态分布</h3>
    <% if (projectStatus.isEmpty()) { %><div class="empty">暂无数据</div><% } else {
        long max = 1;
        for (Map<String, String> r : projectStatus) { try { max = Math.max(max, Long.parseLong(r.get("cnt"))); } catch (Exception ignore) { } }
        for (Map<String, String> r : projectStatus) {
            long c = 0; try { c = Long.parseLong(r.get("cnt")); } catch (Exception ignore) { }
            int pct = (int) Math.round(c * 100.0 / max);
    %>
    <div class="bar-row">
        <div class="bar-label"><%= projStatusText(r.get("status")) %></div>
        <div class="bar-track"><div class="bar-fill" style="width:<%= Math.max(pct, 3) %>%"></div></div>
        <div class="bar-num"><%= c %></div>
    </div>
    <% } } %>
</div>

<div class="card">
    <h3>任务状态分布</h3>
    <% if (taskStatus.isEmpty()) { %><div class="empty">暂无数据</div><% } else {
        long max = 1;
        for (Map<String, String> r : taskStatus) { try { max = Math.max(max, Long.parseLong(r.get("cnt"))); } catch (Exception ignore) { } }
        for (Map<String, String> r : taskStatus) {
            long c = 0; try { c = Long.parseLong(r.get("cnt")); } catch (Exception ignore) { }
            int pct = (int) Math.round(c * 100.0 / max);
    %>
    <div class="bar-row">
        <div class="bar-label"><%= taskStatusText(r.get("status")) %></div>
        <div class="bar-track"><div class="bar-fill" style="width:<%= Math.max(pct, 3) %>%"></div></div>
        <div class="bar-num"><%= c %></div>
    </div>
    <% } } %>
</div>

<div class="card">
    <h3>周报状态分布</h3>
    <% if (weeklyStatus.isEmpty()) { %><div class="empty">暂无数据</div><% } else {
        long max = 1;
        for (Map<String, String> r : weeklyStatus) { try { max = Math.max(max, Long.parseLong(r.get("cnt"))); } catch (Exception ignore) { } }
        for (Map<String, String> r : weeklyStatus) {
            long c = 0; try { c = Long.parseLong(r.get("cnt")); } catch (Exception ignore) { }
            int pct = (int) Math.round(c * 100.0 / max);
    %>
    <div class="bar-row">
        <div class="bar-label"><%= weekStatusText(r.get("status")) %></div>
        <div class="bar-track"><div class="bar-fill" style="width:<%= Math.max(pct, 3) %>%"></div></div>
        <div class="bar-num"><%= c %></div>
    </div>
    <% } } %>
</div>

<div class="card">
    <h3>近 6 个月新增项目</h3>
    <% if (monthly.isEmpty()) { %>
    <div class="empty">近 6 个月没有新增项目。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>月份</th><th>新增项目数</th></tr></thead>
        <tbody>
        <% for (Map<String, String> r : monthly) { %>
        <tr><td><%= h(r.get("m")) %></td><td><%= h(r.get("cnt")) %></td></tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
