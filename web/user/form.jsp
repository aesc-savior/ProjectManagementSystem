<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="pm.dao.*,java.util.*" %>
<% String pageTitle = "用户编辑"; String active = "user"; %>
<%@ include file="/WEB-INF/inc/head.jspf" %>
<%
    Map<String, String> row = (Map<String, String>) request.getAttribute("row");
    boolean edit = row != null;
    String id = edit ? row.get("id") : "";
    String vLogin = edit ? row.get("login_id") : "";
    String vName = edit ? row.get("user_name") : "";
    String vRole = edit ? row.get("role_code") : "MEMBER";
    String vDept = edit ? row.get("dept_name") : "";
    String vPhone = edit ? row.get("phone") : "";
    String vEmail = edit ? row.get("email") : "";
%>
<div class="page-title">
    <div>
        <h2><%= edit ? "编辑用户" : "新增用户" %></h2>
        <div class="sub"><%= edit ? "登录账号不可修改；密码留空表示保持原密码。" : "新增用户必须设置初始密码（建议 123456）。" %></div>
    </div>
    <a class="btn" href="<%= ctx %>/user?action=list">返回列表</a>
</div>

<div class="card">
    <form method="post" action="<%= ctx %>/user?action=save">
        <input type="hidden" name="id" value="<%= h(id) %>">
        <div class="form-grid">
            <div class="field">
                <label>登录账号 *</label>
                <input type="text" name="loginId" value="<%= h(vLogin) %>" <%= edit ? "readonly" : "" %> placeholder="字母数字，例如 zhangsan">
            </div>
            <div class="field">
                <label>姓名 *</label>
                <input type="text" name="userName" value="<%= h(vName) %>">
            </div>
            <div class="field">
                <label>角色 *</label>
                <select name="roleCode">
                    <option value="MEMBER" <%= "MEMBER".equals(vRole) ? "selected" : "" %>>项目成员</option>
                    <option value="PM" <%= "PM".equals(vRole) ? "selected" : "" %>>项目经理</option>
                    <option value="VIEWER" <%= "VIEWER".equals(vRole) ? "selected" : "" %>>查看者</option>
                    <option value="ADMIN" <%= "ADMIN".equals(vRole) ? "selected" : "" %>>系统管理员</option>
                </select>
            </div>
            <div class="field">
                <label>部门</label>
                <input type="text" name="deptName" value="<%= h(vDept) %>" placeholder="例如：研发部">
            </div>
            <div class="field">
                <label>手机</label>
                <input type="text" name="phone" value="<%= h(vPhone) %>">
            </div>
            <div class="field">
                <label>邮箱</label>
                <input type="text" name="email" value="<%= h(vEmail) %>">
            </div>
            <div class="field">
                <label><%= edit ? "重置密码（留空不改）" : "初始密码 *" %></label>
                <input type="password" name="password" placeholder="<%= edit ? "不修改请留空" : "例如 123456" %>">
            </div>
        </div>
        <div class="mt flex">
            <button class="btn btn-primary" type="submit">保存</button>
            <a class="btn" href="<%= ctx %>/user?action=list">取消</a>
        </div>
    </form>
</div>
<%@ include file="/WEB-INF/inc/foot.jspf" %>
