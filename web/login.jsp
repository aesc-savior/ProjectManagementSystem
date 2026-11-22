<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
    if (session.getAttribute("uid") != null) {
        response.sendRedirect(ctx + "/home/index.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>登录 · 项目管理系统</title>
    <link rel="stylesheet" href="<%= ctx %>/assets/css/app.css">
</head>
<body>
<div class="login-page">
    <div class="login-card">
        <h1>项目管理系统</h1>
        <div class="sub">项目 · 任务 · 周报 · 审批 · 统计</div>

        <% if ("1".equals(request.getParameter("error"))) { %>
        <div class="alert alert-error">账号或密码错误，或被停用。</div>
        <% } else if ("timeout".equals(request.getParameter("error"))) { %>
        <div class="alert alert-error">登录已过期，请重新登录。</div>
        <% } %>
        <% if (request.getParameter("msg") != null) { %>
        <div class="alert alert-info"><%= request.getParameter("msg") %></div>
        <% } %>

        <form method="post" action="<%= ctx %>/auth?action=login">
            <div class="field">
                <label>登录账号</label>
                <input type="text" name="loginId" autofocus placeholder="例如 admin">
            </div>
            <div class="field">
                <label>登录密码</label>
                <input type="password" name="password" placeholder="初始密码 123456">
            </div>
            <button class="btn btn-primary" type="submit">登 录</button>
        </form>

        <div class="demo">
            演示账号：admin / pm / member / viewer，密码均为 123456<br>
            管理员、项目经理、成员、查看者看到的菜单与按钮不同
        </div>
    </div>
</div>
</body>
</html>
