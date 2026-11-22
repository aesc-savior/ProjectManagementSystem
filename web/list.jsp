<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "延期罚款"; String active = "fine"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    List<Map<String, String>> overdue = (List<Map<String, String>>) request.getAttribute("overdue");
    if (overdue == null) overdue = new ArrayList<Map<String, String>>();
    long unpaid = request.getAttribute("unpaid") == null ? 0 : (Long) request.getAttribute("unpaid");
    java.util.Set<String> unpaidIds = (java.util.Set<String>) request.getAttribute("unpaidIds");
    if (unpaidIds == null) unpaidIds = new java.util.HashSet<String>();
    String uid = (String) session.getAttribute("uid");
%>
<div class="page-title">
    <div>
        <h2>延期罚款</h2>
        <div class="sub">项目超期按 <%= FineDao.PER_DAY %> 元/天生成罚款单，罚款对象为项目经理；支付为模拟微信支付。</div>
    </div>
</div>

<div class="stats">
    <div class="stat">
        <div class="label">未缴罚款</div>
        <div class="value"><%= money(String.valueOf(unpaid)) %></div>
        <div class="hint">元（<%= isAdmin ? "全部" : "本人" %>）</div>
    </div>
    <div class="stat">
        <div class="label">超期项目</div>
        <div class="value"><%= overdue.size() %></div>
        <div class="hint">可生成罚款单</div>
    </div>
    <div class="stat">
        <div class="label">罚款记录</div>
        <div class="value"><%= rows.size() %></div>
        <div class="hint">最近 200 条</div>
    </div>
</div>

<%
    // 已经有未缴罚款单的项目不再出现在下拉里，避免反复提交同一个项目
    List<Map<String, String>> canGen = new ArrayList<Map<String, String>>();
    for (Map<String, String> p : overdue) {
        if (!unpaidIds.contains(p.get("id"))) canGen.add(p);
    }
%>
<% if (isPm && !canGen.isEmpty()) { %>
<div class="card">
    <h3>生成罚款单</h3>
    <form class="flex" method="post" action="<%= ctx %>/fine?action=generate"
          onsubmit="return confirm('确认按超期天数生成罚款单？')">
        <div class="field grow">
            <label>超期项目</label>
            <select name="projectId">
                <% for (Map<String, String> p : canGen) { %>
                <option value="<%= p.get("id") %>"><%= h(p.get("project_no")) %>　<%= h(p.get("name")) %>（截止 <%= h(p.get("end_date")) %>）</option>
                <% } %>
            </select>
        </div>
        <div class="field" style="width:120px">
            <label>&nbsp;</label>
            <button class="btn btn-primary" type="submit">生成</button>
        </div>
    </form>
</div>
<% } else if (isPm && !overdue.isEmpty()) { %>
<div class="card">
    <h3>生成罚款单</h3>
    <div class="empty">当前超期项目都已有未缴罚款单，先缴纳后才能再开单。</div>
</div>
<% } %>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">还没有罚款记录。</div>
    <% } else { %>
    <table class="grid">
        <thead><tr><th>项目</th><th>被罚人</th><th>金额</th><th>超期截止日</th><th>原因</th><th>状态</th><th>支付时间</th><th>操作</th></tr></thead>
        <tbody>
        <% for (Map<String, String> r : rows) {
            boolean canPay = isAdmin || uid.equals(r.get("user_id"));
        %>
        <tr>
            <td><a href="<%= ctx %>/project?action=view&id=<%= r.get("project_id") %>"><%= h(r.get("project_name")) %></a></td>
            <td><%= h(r.get("user_name")) %></td>
            <td><b><%= money(r.get("amount")) %></b> 元</td>
            <td class="nowrap"><%= r.get("due_date").isEmpty() ? "—" : h(r.get("due_date")) %></td>
            <td><%= h(r.get("reason")) %></td>
            <td><span class="<%= fineBadge(r.get("status")) %>"><%= fineText(r.get("status")) %></span></td>
            <td class="nowrap"><%= h(r.get("pay_time")) %></td>
            <td class="ops">
                <% if ("UNPAID".equals(r.get("status")) && canPay) { %>
                <form method="post" action="<%= ctx %>/fine?action=pay&id=<%= r.get("id") %>" style="display:inline"
                      onsubmit="return confirm('模拟微信支付，确认缴纳？')">
                    <button class="btn btn-sm btn-primary" type="submit">微信支付</button>
                </form>
                <% } else { %>—<% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
