<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="archive.dao.*,java.util.*" %>
<% String pageTitle = "档案编辑"; String active = "archive"; %>
<%@ include file="../WEB-INF/inc/head.jsp" %>
<%
    Map<String, String> row = (Map<String, String>) request.getAttribute("row");
    List<Map<String, String>> cats = (List<Map<String, String>>) request.getAttribute("categories");
    if (cats == null) cats = new ArrayList<Map<String, String>>();
    boolean edit = row != null;
    String id = edit ? row.get("id") : "";
    String vTitle = edit ? row.get("title") : "";
    String vCategory = edit ? row.get("category_id") : "";
    String vSecret = edit ? row.get("secret_level") : "1";
    String vKeep = edit ? row.get("keep_years") : "LONG";
    String vDept = edit ? row.get("dept_name") : "";
    String vOwner = edit ? row.get("owner_name") : "";
    String vStatus = edit ? row.get("status") : "PENDING";
    String vTime = edit ? row.get("archive_time") : "";
    String vRemark = edit ? row.get("remark") : "";
%>
<div class="page-title">
    <div>
        <h2><%= edit ? "编辑档案" : "新增档案" %></h2>
        <div class="sub">带 * 为必填；档号由系统按「DA-年份-流水」自动生成，不需要手工填。</div>
    </div>
    <a class="btn" href="<%= ctx %>/archive?action=list">返回列表</a>
</div>

<div class="card">
    <form method="post" action="<%= ctx %>/archive?action=save">
        <input type="hidden" name="id" value="<%= h(id) %>">
        <div class="form-grid">
            <div class="field full">
                <label>档案名称 *</label>
                <input type="text" name="title" value="<%= h(vTitle) %>" maxlength="200"
                       placeholder="例如：2023 年度财务决算报告">
            </div>
            <div class="field">
                <label>分类 *</label>
                <select name="categoryId">
                    <option value="0">未分类</option>
                    <% for (Map<String, String> c : cats) {
                        String cid = c.get("id");
                        String prefix = "0".equals(c.get("parent_id")) ? "" : "└ ";
                    %>
                    <option value="<%= cid %>" <%= cid.equals(vCategory) ? "selected" : "" %>>
                        <%= prefix + h(c.get("name")) %>
                    </option>
                    <% } %>
                </select>
            </div>
            <div class="field">
                <label>密级 *</label>
                <select name="secretLevel">
                    <option value="1" <%= "1".equals(vSecret) ? "selected" : "" %>>公开（1 级）</option>
                    <option value="2" <%= "2".equals(vSecret) ? "selected" : "" %>>内部（2 级）</option>
                    <option value="3" <%= "3".equals(vSecret) ? "selected" : "" %>>秘密（3 级）</option>
                    <option value="4" <%= "4".equals(vSecret) ? "selected" : "" %>>机密（4 级）</option>
                </select>
            </div>
            <div class="field">
                <label>保管期限 *</label>
                <select name="keepYears">
                    <option value="SHORT" <%= "SHORT".equals(vKeep) ? "selected" : "" %>>短期（5 年）</option>
                    <option value="LONG" <%= "LONG".equals(vKeep) ? "selected" : "" %>>长期（30 年）</option>
                    <option value="FOREVER" <%= "FOREVER".equals(vKeep) ? "selected" : "" %>>永久</option>
                </select>
            </div>
            <div class="field">
                <label>责任部门</label>
                <input type="text" name="deptName" value="<%= h(vDept) %>" placeholder="例如：财务处">
            </div>
            <div class="field">
                <label>责任者</label>
                <input type="text" name="ownerName" value="<%= h(vOwner) %>" placeholder="例如：财务处">
            </div>
            <div class="field">
                <label>状态</label>
                <select name="status">
                    <option value="PENDING" <%= "PENDING".equals(vStatus) ? "selected" : "" %>>待归档</option>
                    <option value="ARCHIVED" <%= "ARCHIVED".equals(vStatus) ? "selected" : "" %>>已归档</option>
                    <option value="DESTROYED" <%= "DESTROYED".equals(vStatus) ? "selected" : "" %>>已销毁</option>
                </select>
            </div>
            <div class="field">
                <label>归档时间</label>
                <input type="datetime-local" name="archiveTime"
                       value="<%= h(vTime).replace(' ', 'T') %>">
                <span class="hint">留空则由系统在归档时自动写入</span>
            </div>
            <div class="field full">
                <label>备注</label>
                <textarea name="remark" placeholder="摘要、关联项目、经办说明等"><%= h(vRemark) %></textarea>
            </div>
        </div>
        <div class="mt flex">
            <button class="btn btn-primary" type="submit">保存</button>
            <a class="btn" href="<%= ctx %>/archive?action=list">取消</a>
        </div>
    </form>
</div>
<%@ include file="../WEB-INF/inc/foot.jsp" %>
