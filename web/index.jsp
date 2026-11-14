<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String ctx = request.getContextPath();
    if (session.getAttribute("uid") == null) {
        response.sendRedirect(ctx + "/login.jsp");
    } else {
        response.sendRedirect(ctx + "/home/index.jsp");
    }
%>
