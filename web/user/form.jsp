<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<% String pageTitle = "用户编辑"; String active = "user"; %>
<%@ include file="/WEB-INF/inc/head.jsp" %>
<%
    Map<String, String> row = (Map<String, String>) request.getAttribute("row");
    boolean edit = row != null;
    String id = edit ? row.get("id") : "";
    String vLogin = edit ? row.get("login_id") : "";
    String vName = edit ? row.get("user_name") : "";
    String vRole = edit ? row.get("role_code") : "USER";
    String vDept = edit ? row.get("dept_name") : "";
    String vPhone = edit ? row.get("phone") : "";
    String vEmail = edit ? row.get("email") : "";
    String vSecret = edit ? row.get("secret_level") : "1";
%>
<div class="page-title">
    <div>
        <h2><%= edit ? "编辑用户" : "新增用户" %></h2>
        <div class="sub">
            <%= edit ? "登录账号不可修改；密码留空表示保持原密码。" : "新增用户必须设置初始密码（建议 123456，首次登录后修改）。" %>
        </div>
    </div>
    <a class="btn" href="<%= ctx %>/user?action=list">返回列表</a>
</div>

<div class="card">
    <form method="post" action="<%= ctx %>/user?action=save">
        <input type="hidden" name="id" value="<%= h(id) %>">
        <div class="form-grid">
            <div class="field">
                <label>登录账号 *</label>
                <input type="text" name="loginId" value="<%= h(vLogin) %>" <%= edit ? "readonly" : "" %>
                       placeholder="字母数字，例如 zhangsan">
            </div>
            <div class="field">
                <label>姓名 *</label>
                <input type="text" name="userName" value="<%= h(vName) %>">
            </div>
            <div class="field">
                <label>角色 *</label>
                <select name="roleCode">
                    <option value="USER" <%= "USER".equals(vRole) ? "selected" : "" %>>部门用户</option>
                    <option value="ARCHIVIST" <%= "ARCHIVIST".equals(vRole) ? "selected" : "" %>>档案管理员</option>
                    <option value="ADMIN" <%= "ADMIN".equals(vRole) ? "selected" : "" %>>系统管理员</option>
                </select>
            </div>
            <div class="field">
                <label>可访问密级</label>
                <select name="secretLevel">
                    <option value="1" <%= "1".equals(vSecret) ? "selected" : "" %>>公开（1 级）</option>
                    <option value="2" <%= "2".equals(vSecret) ? "selected" : "" %>>内部（2 级）</option>
                    <option value="3" <%= "3".equals(vSecret) ? "selected" : "" %>>秘密（3 级）</option>
                    <option value="4" <%= "4".equals(vSecret) ? "selected" : "" %>>机密（4 级）</option>
                </select>
            </div>
            <div class="field">
                <label>部门</label>
                <input type="text" name="deptName" value="<%= h(vDept) %>" placeholder="例如：财务处">
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
<div class="card">
    <h3>角色说明</h3>
    <table class="grid">
        <thead><tr><th>角色</th><th>能做什么</th></tr></thead>
        <tbody>
        <tr><td>系统管理员</td><td>全部档案操作 + 用户管理 + 系统配置 + 日志审计</td></tr>
        <tr><td>档案管理员</td><td>档案增删改、归档、上传附件、审批借阅、查看日志</td></tr>
        <tr><td>部门用户</td><td>检索、查看、下载、申请借阅</td></tr>
        </tbody>
    </table>
</div>
<%@ include file="/WEB-INF/inc/foot.jsp" %>
