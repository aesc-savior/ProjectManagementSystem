<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<%!
    private String e(Object v) {
        if (v == null) return "";
        String s = v.toString();
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
%>
<%
    Map<String, String> r = (Map<String, String>) request.getAttribute("row");
    if (r == null) { response.sendRedirect(request.getContextPath() + "/weekly?action=list"); return; }
    String printTime = (String) request.getAttribute("printTime");
    if (printTime == null) printTime = "";
    String ctx = request.getContextPath();
%>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>周报打印 · <%= e(r.get("week_no")) %></title>
    <style>
        * { box-sizing: border-box; }
        body {
            margin: 0; padding: 28px 32px; background: #eef2f7; color: #0f172a;
            font-family: "Microsoft YaHei", "PingFang SC", SimSun, sans-serif; font-size: 14px; line-height: 1.8;
        }
        .sheet { width: 794px; min-height: 1000px; margin: 0 auto; background: #fff; padding: 42px 48px;
            box-shadow: 0 8px 28px rgba(15, 23, 42, .12); }
        h1 { text-align: center; font-size: 22px; margin: 0 0 6px; letter-spacing: 4px; }
        .meta-line { text-align: center; color: #64748b; font-size: 12px; margin-bottom: 18px; }
        table.head { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
        table.head th, table.head td { border: 1px solid #cbd5e1; padding: 7px 10px; font-size: 13px; text-align: left; }
        table.head th { background: #f8fafc; width: 96px; color: #475569; font-weight: 600; }
        h2 { font-size: 15px; margin: 20px 0 8px; padding-bottom: 5px; border-bottom: 1px solid #e2e8f0; }
        .body-text { white-space: pre-wrap; word-break: break-word; min-height: 28px; }
        .sign { margin-top: 34px; display: flex; justify-content: space-between; color: #475569; font-size: 13px; }
        .toolbar { width: 794px; margin: 0 auto 12px; display: flex; gap: 10px; justify-content: flex-end; }
        .toolbar a, .toolbar button {
            padding: 7px 16px; border: 1px solid #cbd5e1; border-radius: 7px; background: #fff;
            color: #0f172a; font-size: 13px; cursor: pointer; text-decoration: none;
        }
        .toolbar button.primary { background: #2563eb; border-color: #2563eb; color: #fff; }
        @media print {
            body { background: #fff; padding: 0; }
            .sheet { width: auto; min-height: auto; box-shadow: none; padding: 0; }
            .toolbar { display: none; }
            h2 { break-after: avoid; page-break-after: avoid; }
            .body-text { break-inside: avoid; page-break-inside: avoid; }
        }
    </style>
</head>
<body>
<div class="toolbar">
    <a href="<%= ctx %>/weekly?action=list">返回列表</a>
    <a href="<%= ctx %>/weekly?action=export&scope=one&id=<%= e(r.get("id")) %>">导出 Excel</a>
    <a href="<%= ctx %>/weekly?action=export&scope=one&fmt=doc&id=<%= e(r.get("id")) %>">导出 Word</a>
    <button class="primary" onclick="window.print()">打印</button>
</div>

<div class="sheet">
    <h1>项目周报</h1>
    <div class="meta-line">打印时间：<%= e(printTime) %></div>

    <table class="head">
        <tr>
            <th>项目编号</th><td><%= e(r.get("project_no")) %></td>
            <th>项目名称</th><td><%= e(r.get("project_name")) %></td>
        </tr>
        <tr>
            <th>提交人</th><td><%= e(r.get("user_name")) %></td>
            <th>周次</th><td><%= e(r.get("week_no")) %></td>
        </tr>
        <tr>
            <th>工时</th><td><%= e(r.get("hours")) %> 小时</td>
            <th>状态</th><td><%= "SUBMITTED".equals(r.get("status")) ? "已提交"
                    : ("REVIEWED".equals(r.get("status")) ? "已审核"
                    : ("REJECTED".equals(r.get("status")) ? "已退回" : "草稿")) %></td>
        </tr>
        <tr>
            <th>提交时间</th><td><%= e(r.get("submit_time")) %></td>
            <th>审核人</th><td><%= e(r.get("reviewer")) %></td>
        </tr>
    </table>

    <h2>一、本周完成</h2>
    <div class="body-text"><%= e(r.get("done_text")) %></div>

    <h2>二、下周计划</h2>
    <div class="body-text"><%= e(r.get("plan_text")) %></div>

    <h2>三、问题与风险</h2>
    <div class="body-text"><%= e(r.get("issue_text")) %></div>

    <h2>四、审核意见</h2>
    <div class="body-text"><%= r.get("review").isEmpty() ? "（暂无）" : e(r.get("review")) %>
        <%= r.get("score").isEmpty() ? "" : "（评分 " + e(r.get("score")) + "）" %></div>

    <h2>五、附件清单</h2>
    <div class="body-text"><%= r.get("attach_name").isEmpty() ? "无附件"
            : e(r.get("attach_name")) + "（" + e(r.get("attach_size")) + " 字节）" %></div>

    <div class="sign">
        <span>提交人签字：____________________</span>
        <span>项目经理签字：____________________</span>
        <span>日期：<%= e(printTime) %></span>
    </div>
</div>
</body>
</html>
