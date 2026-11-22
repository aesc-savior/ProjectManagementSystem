<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "发布公告"; String active = "notice"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    List<Map<String, String>> projects = (List<Map<String, String>>) request.getAttribute("projects");
    if (projects == null) projects = new ArrayList<Map<String, String>>();
%>
<div class="page-title">
    <div>
        <h2>发布公告</h2>
        <div class="sub">全员公告对所有人生效；项目公告只对项目成员可见。</div>
    </div>
    <a class="btn" href="<%= ctx %>/notice?action=list">返回列表</a>
</div>

<div class="card">
    <form method="post" action="<%= ctx %>/notice?action=save">
        <div class="form-grid">
            <div class="field full">
                <label>公告标题 *</label>
                <input type="text" name="title" maxlength="200" placeholder="例如：11 月项目例会通知">
            </div>
            <div class="field">
                <label>可见范围</label>
                <select name="scope">
                    <option value="ALL">全员</option>
                    <option value="PROJECT">指定项目</option>
                </select>
            </div>
            <div class="field">
                <label>关联项目（范围为“指定项目”时生效）</label>
                <select name="projectId">
                    <option value="0">不关联</option>
                    <% for (Map<String, String> p : projects) { %>
                    <option value="<%= p.get("id") %>"><%= h(p.get("name")) %></option>
                    <% } %>
                </select>
            </div>
            <div class="field">
                <label>有效期至</label>
                <input type="datetime-local" name="expireTime">
            </div>
            <div class="field full">
                <label>正文 *</label>
                <textarea name="content" rows="6" placeholder="公告正文"></textarea>
            </div>
        </div>
        <div class="mt flex">
            <button class="btn btn-primary" type="submit">发布</button>
            <a class="btn" href="<%= ctx %>/notice?action=list">取消</a>
        </div>
    </form>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
