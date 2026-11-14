<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    /* 输出转义，避免把用户输入当 HTML 渲染 */
    private String h(Object v) {
        if (v == null) return "";
        String s = v.toString();
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String secretText(String v) {
        if ("2".equals(v)) return "内部";
        if ("3".equals(v)) return "秘密";
        if ("4".equals(v)) return "机密";
        return "公开";
    }

    private String keepText(String v) {
        if ("SHORT".equals(v)) return "短期";
        if ("FOREVER".equals(v)) return "永久";
        return "长期";
    }

    private String statusText(String v) {
        if ("ARCHIVED".equals(v)) return "已归档";
        if ("BORROWED".equals(v)) return "借出中";
        if ("DESTROYED".equals(v)) return "已销毁";
        return "待归档";
    }

    private String statusBadge(String v) {
        if ("ARCHIVED".equals(v)) return "badge badge-archived";
        if ("BORROWED".equals(v)) return "badge badge-borrowed";
        if ("DESTROYED".equals(v)) return "badge badge-destroyed";
        return "badge badge-pending";
    }

    private String borrowText(String v) {
        if ("APPROVED".equals(v)) return "已通过";
        if ("REJECTED".equals(v)) return "已驳回";
        if ("RETURNED".equals(v)) return "已归还";
        return "待审批";
    }

    private String borrowBadge(String v) {
        if ("APPROVED".equals(v)) return "badge badge-approved";
        if ("REJECTED".equals(v)) return "badge badge-rejected";
        if ("RETURNED".equals(v)) return "badge badge-returned";
        return "badge badge-pending";
    }

    private String roleTextOf(String v) {
        if ("ADMIN".equals(v)) return "系统管理员";
        if ("ARCHIVIST".equals(v)) return "档案管理员";
        return "部门用户";
    }
%>
<%
    String ctx = request.getContextPath();
    String curRole = (String) session.getAttribute("role");
    if (curRole == null) curRole = "USER";
    String curName = (String) session.getAttribute("uname");
    if (curName == null) curName = "";
    String roleText = "USER".equals(curRole) ? "部门用户"
            : ("ARCHIVIST".equals(curRole) ? "档案管理员" : "系统管理员");
    String flash = request.getParameter("msg");
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%= h(pageTitle) %> · 档案管理系统</title>
    <link rel="stylesheet" href="<%= ctx %>/assets/css/app.css">
</head>
<body>
<div class="app">
    <aside class="sidebar">
        <div class="brand">档案管理<small>v1.0</small></div>
        <nav class="nav">
            <div class="group">工作区</div>
            <a class="<%= "home".equals(active) ? "active" : "" %>" href="<%= ctx %>/home/index.jsp">首页概览</a>
            <a class="<%= "archive".equals(active) ? "active" : "" %>" href="<%= ctx %>/archive?action=list">档案管理</a>
            <a class="<%= "borrow".equals(active) ? "active" : "" %>" href="<%= ctx %>/borrow?action=list">借阅管理</a>
            <div class="group">系统</div>
            <% if ("ADMIN".equals(curRole)) { %>
            <a class="<%= "user".equals(active) ? "active" : "" %>" href="<%= ctx %>/user?action=list">用户管理</a>
            <% } %>
            <% if ("ADMIN".equals(curRole) || "ARCHIVIST".equals(curRole)) { %>
            <a class="<%= "log".equals(active) ? "active" : "" %>" href="<%= ctx %>/log?action=list">操作日志</a>
            <% } %>
        </nav>
    </aside>
    <div class="main">
        <header class="topbar">
            <div class="who">当前登录：<b><%= h(curName) %></b>
                <span class="muted">（<%= roleText %><%= "USER".equals(curRole) ? "" : "，" + h(session.getAttribute("dept")) %>）</span>
            </div>
            <div class="flex">
                <a class="btn btn-sm" href="<%= ctx %>/home/index.jsp">首页</a>
                <a class="btn btn-sm" href="<%= ctx %>/auth?action=logout">退出</a>
            </div>
        </header>
        <main class="content">
            <% if (flash != null && !flash.trim().isEmpty()) { %>
            <div class="alert alert-info"><%= h(flash) %></div>
            <% } %>
