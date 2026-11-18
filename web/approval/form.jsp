<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "申请延期"; String active = "approval"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    Map<String, String> task = (Map<String, String>) request.getAttribute("task");
    if (task == null) {
%>
<div class="card"><div class="alert alert-error">任务不存在</div>
    <a class="btn" href="<%= ctx %>/approval?action=list">返回审批中心</a></div>
<% } else { %>
<div class="page-title">
    <div>
        <h2>任务延期申请</h2>
        <div class="sub"><%= h(task.get("title")) %>　·　当前截止日 <%= h(task.get("due_date")) %></div>
    </div>
    <a class="btn" href="<%= ctx %>/task?action=editForm&id=<%= task.get("id") %>">返回任务</a>
</div>

<div class="card">
    <form method="post" action="<%= ctx %>/approval?action=apply">
        <input type="hidden" name="taskId" value="<%= task.get("id") %>">
        <div class="form-grid">
            <div class="field">
                <label>新的截止日期 *</label>
                <input type="date" name="newDate" required>
            </div>
            <div class="field full">
                <label>延期原因 *</label>
                <textarea name="reason" required placeholder="说明延期原因、影响范围与补救措施"></textarea>
            </div>
        </div>
        <div class="mt flex">
            <button class="btn btn-primary" type="submit">提交审批</button>
            <a class="btn" href="<%= ctx %>/task?action=editForm&id=<%= task.get("id") %>">取消</a>
        </div>
    </form>
</div>
<% } %>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
