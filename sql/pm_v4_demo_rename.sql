-- 恢复演示数据的原始名称，把「例1」这类占位写法换回具体案例名
-- 只改名称和说明文字，编号、日期、金额、状态都不动
-- 可重复执行

USE pm;

-- 项目
UPDATE pm_project SET name = '官网改版项目', description = '官网视觉与信息架构重做，含 PC 与移动端' WHERE id = 1;
UPDATE pm_project SET name = '数据中心机房建设', description = '二期机房扩容与配电改造' WHERE id = 2;
UPDATE pm_project SET name = '客户成功服务包', description = '重点客户年度服务与巡检' WHERE id = 3;
UPDATE pm_project SET name = '移动端重构', description = 'App 与小程序统一技术栈，等待预算审批' WHERE id = 4;
UPDATE pm_project SET name = '年度审计支持', description = '配合外部审计提供资料与访谈' WHERE id = 5;
UPDATE pm_project SET name = '部署验收测试项目', description = '自动化部署验证：中文与写入链路检查' WHERE id = 6;

-- 任务
UPDATE pm_task SET title = '需求调研', description = '访谈业务方，输出需求清单' WHERE id = 1;
UPDATE pm_task SET title = '原型设计', description = '低保真 + 高保真原型' WHERE id = 2;
UPDATE pm_task SET title = '前端开发', description = '首页、产品页、案例页' WHERE id = 3;
UPDATE pm_task SET title = '后端接口', description = 'CMS 接口与表单服务' WHERE id = 4;
UPDATE pm_task SET title = '页面开发', description = '首页与产品页' WHERE id = 5;
UPDATE pm_task SET title = '组件库', description = '按钮、表单、卡片组件' WHERE id = 6;
UPDATE pm_task SET title = '联调测试', description = '接口联调与回归' WHERE id = 7;
UPDATE pm_task SET title = '土建施工', description = '机房地面与隔断' WHERE id = 8;
UPDATE pm_task SET title = '设备采购', description = 'UPS、机柜与空调' WHERE id = 9;
UPDATE pm_task SET title = '上线发布', description = '生产环境发布与回滚预案' WHERE id = 10;

-- 周报
UPDATE pm_weekly SET done_text = '完成首页视觉稿与 3 个产品页切图；组件库完成按钮与表单。', plan_text = '完成案例页与组件库剩余组件。', issue_text = '设计稿变更两次，进度略延后。' WHERE id = 1;
UPDATE pm_weekly SET done_text = '完成首页改版与联调', plan_text = '推进上线', issue_text = '等接口' WHERE id = 2;
UPDATE pm_weekly SET done_text = '后端接口完成项目与任务模块。', plan_text = '完成周报与文档接口。', issue_text = '测试环境数据库不稳定。' WHERE id = 3;
UPDATE pm_weekly SET done_text = '土建完成 50%，配电进场。', plan_text = '完成机柜到货验收。', issue_text = '物业施工时间受限。' WHERE id = 4;
UPDATE pm_weekly SET done_text = '完成组件库开发与联调', plan_text = '推进上线发布', issue_text = '第三方接口联调延迟' WHERE id = 5;

-- 公告
UPDATE pm_notice SET title = '关于 11 月项目例会的通知', content = '本周五 14:00 在三楼会议室召开项目例会，请各项目经理准备进度与风险清单。' WHERE id = 1;
UPDATE pm_notice SET title = '官网改版项目进入联调阶段', content = '官网改版项目已完成页面开发 80%，下周起进入接口联调，请相关同学预留时间。' WHERE id = 2;

-- 审批
UPDATE pm_approval SET title = '任务延期申请：前端开发', detail = '设计稿两次变更，需重做案例页。' WHERE id = 1;
UPDATE pm_approval SET title = '任务延期申请：联调测试', detail = '后端接口延期。' WHERE id = 2;
UPDATE pm_approval SET title = '任务延期申请：上线发布', detail = '依赖第三方接口未就绪' WHERE id = 3;

-- 罚款
UPDATE pm_fine SET reason = '项目「年度审计支持」超期 138 天，按 100 元/天计。' WHERE id = 1;

-- 文档
UPDATE pm_doc SET name = '上传测试.txt' WHERE id IN (2, 3);

-- 日志
UPDATE pm_log SET detail = '创建项目：官网改版项目' WHERE id = 2;
UPDATE pm_log SET detail = '创建项目：部署验收测试项目' WHERE id = 5;
UPDATE pm_log SET detail = '上传文档：上传测试.txt' WHERE detail LIKE '上传文档：%';
UPDATE pm_log SET detail = '下载文档：上传测试.txt' WHERE detail LIKE '下载文档：%';
