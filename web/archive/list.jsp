<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="archive.dao.*,java.util.*" %>
<% String pageTitle = "档案管理"; String active = "archive"; %>
<%@ include file="/WEB-INF/inc/head.jsp" %>
<%
    List<Map<String, String>> rows = (List<Map<String, String>>) request.getAttribute("rows");
    List<Map<String, String>> cats = (List<Map<String, String>>) request.getAttribute("categories");
    if (rows == null) rows = new ArrayList<Map<String, String>>();
    if (cats == null) cats = new ArrayList<Map<String, String>>();
    long total = request.getAttribute("total") == null ? 0 : (Long) request.getAttribute("total");
    int pageNo = request.getAttribute("page") == null ? 1 : (Integer) request.getAttribute("page");
    int pages = request.getAttribute("pages") == null ? 1 : (Integer) request.getAttribute("pages");
    String fKeyword = (String) request.getAttribute("fKeyword");
    String fCategoryId = (String) request.getAttribute("fCategoryId");
    String fYear = (String) request.getAttribute("fYear");
    String fSecret = (String) request.getAttribute("fSecret");
    String fStatus = (String) request.getAttribute("fStatus");
    if (fKeyword == null) fKeyword = "";
    if (fCategoryId == null) fCategoryId = "";
    if (fYear == null) fYear = "";
    if (fSecret == null) fSecret = "";
    if (fStatus == null) fStatus = "";
    boolean staff = "ADMIN".equals(curRole) || "ARCHIVIST".equals(curRole);
    String qs = "&keyword=" + java.net.URLEncoder.encode(fKeyword, "UTF-8")
            + "&categoryId=" + fCategoryId + "&year=" + fYear
            + "&secretLevel=" + fSecret + "&status=" + fStatus;
%>
<div class="page-title">
    <div>
        <h2>档案管理</h2>
        <div class="sub">共 <b><%= total %></b> 条，按组合条件检索；点击档号查看详情。</div>
    </div>
    <% if (staff) { %>
    <a class="btn btn-primary" href="<%= ctx %>/archive?action=addForm">+ 新增档案</a>
    <% } %>
</div>

<div class="card">
    <form class="filters" method="get" action="<%= ctx %>/archive">
        <input type="hidden" name="action" value="list">
        <div class="field grow">
            <label>关键词（名称 / 档号）</label>
            <input type="text" name="keyword" value="<%= h(fKeyword) %>" placeholder="例如 决算">
        </div>
        <div class="field">
            <label>分类</label>
            <select name="categoryId">
                <option value="">全部</option>
                <% for (Map<String, String> c : cats) {
                    String cid = c.get("id");
                    String prefix = "0".equals(c.get("parent_id")) ? "" : "└ ";
                %>
                <option value="<%= cid %>" <%= cid.equals(fCategoryId) ? "selected" : "" %>>
                    <%= prefix + h(c.get("name")) %>
                </option>
                <% } %>
            </select>
        </div>
        <div class="field">
            <label>年度</label>
            <select name="year">
                <option value="">全部</option>
                <% for (int y = 2026; y >= 2020; y--) { %>
                <option value="<%= y %>" <%= String.valueOf(y).equals(fYear) ? "selected" : "" %>><%= y %></option>
                <% } %>
            </select>
        </div>
        <div class="field">
            <label>密级</label>
            <select name="secretLevel">
                <option value="">全部</option>
                <option value="1" <%= "1".equals(fSecret) ? "selected" : "" %>>公开</option>
                <option value="2" <%= "2".equals(fSecret) ? "selected" : "" %>>内部</option>
                <option value="3" <%= "3".equals(fSecret) ? "selected" : "" %>>秘密</option>
                <option value="4" <%= "4".equals(fSecret) ? "selected" : "" %>>机密</option>
            </select>
        </div>
        <div class="field">
            <label>状态</label>
            <select name="status">
                <option value="">全部</option>
                <option value="PENDING" <%= "PENDING".equals(fStatus) ? "selected" : "" %>>待归档</option>
                <option value="ARCHIVED" <%= "ARCHIVED".equals(fStatus) ? "selected" : "" %>>已归档</option>
                <option value="BORROWED" <%= "BORROWED".equals(fStatus) ? "selected" : "" %>>借出中</option>
            </select>
        </div>
        <div class="field">
            <label>&nbsp;</label>
            <div class="flex">
                <button class="btn btn-primary" type="submit">查询</button>
                <a class="btn" href="<%= ctx %>/archive?action=list">重置</a>
            </div>
        </div>
    </form>
</div>

<div class="card">
    <% if (rows.isEmpty()) { %>
    <div class="empty">没有符合条件的档案。换个条件试试，或新增一条。</div>
    <% } else { %>
    <table class="grid">
        <thead>
        <tr>
            <th>档号</th><th>档案名称</th><th>分类</th><th>密级</th><th>保管期限</th>
            <th>状态</th><th>归档时间</th><th>操作</th>
        </tr>
        </thead>
        <tbody>
        <% for (Map<String, String> r : rows) { %>
        <tr>
            <td class="nowrap"><%= h(r.get("archive_no")) %></td>
            <td><a href="<%= ctx %>/archive?action=view&id=<%= r.get("id") %>"><%= h(r.get("title")) %></a></td>
            <td><%= h(r.get("category_name")) %></td>
            <td><%= secretText(r.get("secret_level")) %></td>
            <td><%= keepText(r.get("keep_years")) %></td>
            <td><span class="<%= statusBadge(r.get("status")) %>"><%= statusText(r.get("status")) %></span></td>
            <td class="nowrap"><%= h(r.get("archive_time")) %></td>
            <td class="ops">
                <a class="btn btn-sm" href="<%= ctx %>/archive?action=view&id=<%= r.get("id") %>">查看</a>
                <% if (staff) { %>
                <a class="btn btn-sm" href="<%= ctx %>/archive?action=editForm&id=<%= r.get("id") %>">编辑</a>
                <form method="post" action="<%= ctx %>/archive?action=delete&id=<%= r.get("id") %>"
                      style="display:inline" onsubmit="return confirm('确认删除该档案？删除后可在数据库中恢复（逻辑删除）。')">
                    <button class="btn btn-sm btn-danger" type="submit">删除</button>
                </form>
                <% } %>
            </td>
        </tr>
        <% } %>
        </tbody>
    </table>

    <div class="pager">
        <span class="info">第 <%= pageNo %> / <%= Math.max(pages, 1) %> 页，共 <%= total %> 条</span>
        <% if (pageNo > 1) { %>
        <a href="<%= ctx %>/archive?action=list&page=<%= pageNo - 1 %><%= qs %>">上一页</a>
        <% } %>
        <% for (int p = Math.max(1, pageNo - 2); p <= Math.min(pages, pageNo + 2); p++) { %>
        <% if (p == pageNo) { %><span class="cur"><%= p %></span><% } else { %>
        <a href="<%= ctx %>/archive?action=list&page=<%= p %><%= qs %>"><%= p %></a><% } %>
        <% } %>
        <% if (pageNo < pages) { %>
        <a href="<%= ctx %>/archive?action=list&page=<%= pageNo + 1 %><%= qs %>">下一页</a>
        <% } %>
    </div>
    <% } %>
</div>
<%@ include file="/WEB-INF/inc/foot.jsp" %>
