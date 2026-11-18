# 项目管理系统（ProjectManagementSystem）· 可运行骨架

 **Servlet 6 + JSP + 原生 JDBC + session/filter 登录**。
配套文档：`项目管理系统-需求规格说明书（V1.0）.docx`、`项目管理系统-功能清单台账.xlsx`（PM-001～PM-123）。

> 定位：**可运行骨架**。把“登录 → 项目 → 任务 → 周报 → 审批 → 统计”的主线跑通。

---

## 一、技术栈

| 项 | 版本 / 说明 |
|---|---|
| JDK | 17 |
| Web 容器 | Tomcat 10.1（Servlet 6，`jakarta.servlet.*`） |
| 页面 | JSP + 手写 CSS（`web/assets/css/app.css`），无前端构建 |
| 数据访问 | 原生 JDBC + 拼接 SQL（课程框架风格） |
| 数据库 | MySQL 8.0，库名 `pm`，表前缀 `pm_`，utf8mb4 |
| 密码 | 随机盐 + MD5(盐+密码)，`pm.util.PasswordUtil` |
| 上传 | `Part` API，单文件 ≤200MB，落盘目录可配置 |

## 二、已实现模块（对应 SRS 条目）

| 模块 | 功能 | 主要条目 |
|---|---|---|
| 登录 / 角色 | 登录、退出、4 角色（ADMIN/PM/MEMBER/VIEWER）、按角色菜单 | PM-098～PM-111 |
| 首页概览 | 可见项目数、我的待办、待我审批、未缴罚款、项目状态图、逾期任务 | PM-096 等 |
| 项目 | 建档（编号自动）、编辑、状态流转、归档、删除、成员管理、组合查询 + 自定义排序 + 分页、详情 | PM-001～PM-013 |
| 任务 | 创建、WBS 父子任务、负责人、优先级、状态/进度、父任务进度自动汇总、组合查询、进度快更 | PM-014～PM-029 |
| 周报 | 填写、提交、撤回前修改、项目经理评语/评分/退回、按项目/人/周/状态查询 | PM-030～PM-038 |
| 文档 | 上传（类型白名单）、下载、删除、按项目/关键词查询、同名自动版本号 | PM-039～PM-048 |
| 公告 | 发布（全员/项目）、签收、签收人数、删除、查询 | PM-049～PM-055 |
| 审批 | 任务延期申请（新截止日 + 原因）、待我审批、通过/驳回（通过后自动改任务截止日） | PM-080～PM-087 |
| 罚款 | 按超期天数 ×100 元生成罚款单、模拟微信支付、台账 | PM-084～PM-087 |
| 统计 | 项目/任务/周报状态分布、任务完成率、预算、未缴罚款、近 6 个月新增项目 | PM-088～PM-097 |
| 用户 | 增删改查、启停、重置密码（ADMIN） | PM-098～PM-099 |
| 日志 | 登录、增删改、提交、审批、上传下载、支付留痕，可按动作/关键词筛 | PM-104 |
| 第三方接口 | `GET /api/projects?status=&type=`、`GET /api/projects/{id}`、`DELETE /api/projects/{id}`，Bearer Token | PM-109 |

**尚未实现（下一步按 SRS 补）**：会议与纪要（PM-056～PM-062）、资产与合同（PM-063～PM-072）、甘特图（PM-024）、个人/团队驾驶舱（PM-096/097 的图形化版本）、微信小程序端（PM-112～PM-123）、真实微信支付（当前为模拟）、气象采集、备份恢复、字典配置。

## 三、目录结构

```
ArchivesManagementSystem（目录名暂未改，项目内容已是 PM 系统）
├─ sql/pm.sql                 建库建表 + 演示数据（4 账号、5 项目、10 任务、4 周报、2 公告）
├─ src/pm/
│  ├─ dao/                    Db / UserDao / ProjectDao / TaskDao / WeeklyDao / DocDao / NoticeDao / ApprovalDao / FineDao / LogDao
│  ├─ filter/LoginFilter.java 未登录跳登录页；/api/* 交 ApiServlet 校验 Token
│  ├─ util/PasswordUtil.java  盐 + MD5
│  └─ web/                    Auth / Project / Task / Weekly / Doc / Notice / Approval / Fine / Stat / User / Log / Api
├─ web/
│  ├─ login.jsp  index.jsp  home/index.jsp
│  ├─ project/  task/  weekly/  doc/  notice/  approval/  fine/  stat/  user/  log/
│  ├─ WEB-INF/web.xml         过滤器 + 12 个 Servlet 映射 + multipart
│  ├─ WEB-INF/inc/head.jspf   公共头（菜单按角色 + 标签辅助方法）
│  ├─ WEB-INF/inc/foot.jspf   公共尾
│  ├─ WEB-INF/classes/pm/     已编译 class（手工部署用；IDEA 会覆盖）
│  └─ assets/css/app.css
└─ 项目管理系统-需求规格说明书（V1.0）.docx
   项目管理系统-功能清单台账.xlsx
   项目管理系统-功能清单.docx
```

## 四、跑起来（三步）

### 1. 建库

```sql
-- 用 Navicat / 命令行都行
create database pm default charset utf8mb4 collate utf8mb4_general_ci;
```

然后执行 `sql/pm.sql`（会重建 `pm_*` 表并写入演示数据）。

### 2. 数据库连接

`src/pm/dao/Db.java` 默认：

```java
URL      = "jdbc:mysql://localhost:3366/pm?...";
USER     = "root";
PASSWORD = "123456";
```

也可用 JVM 参数覆盖：

```
-Dpm.db.url="jdbc:mysql://localhost:3366/pm?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
-Dpm.db.user=root
-Dpm.db.password=你的密码
-Dpm.upload.dir=D:/pm-upload
-Dpm.api.token=pm-demo-token
```

### 3. IDEA 配置

1. `Project Structure → SDK` 选 JDK 17；
2. `web/WEB-INF/lib/*.jar` 全部 `Add as Library`；
3. `Run → Edit Configurations → Tomcat Server → Local`，Deployment 加 **Artifact: war exploded**，
   **Application context 填 `/pm`**；
4. 启动后访问 `http://localhost:9999/pm/`（端口以本机 Tomcat `server.xml` 为准）。

> 手工部署：把 `web` 目录作为 docBase，在 `Tomcat/conf/Catalina/localhost/` 下建 `pm.xml`：
> ```xml
> <Context docBase="D:/apps/pm" path="/pm" reloadable="false" />
> ```
> 并带上 `web/WEB-INF/classes` 与 `web/WEB-INF/lib`。

## 五、演示账号（密码都是 `123456`）

| 账号 | 角色 | 能做什么 |
|---|---|---|
| `admin` | 系统管理员 | 全部数据、用户管理、日志、生成罚款 |
| `pm` | 项目经理 | 本人负责的项目、任务分配、审批、周报审核、罚款 |
| `member` | 项目成员 | 本人参与项目、任务进度、周报提交、文档上传、公告签收 |
| `viewer` | 查看者 | 只读可见项目（无新增/编辑/审批按钮） |

## 六、设计约定

- **数据权限**：管理员看全部；其他角色看“自己负责 + 自己参与”的项目，任务/周报/文档/公告按项目联动。
- **项目状态**：`PENDING → RUNNING → PAUSED/DONE/CANCELED → ARCHIVED`，归档后只读。
- **任务进度**：父任务进度 = 子任务平均进度；任务 100% 自动置 `DONE`。
- **周报**：同一人同一项目同一周唯一；提交后锁定，可被项目经理退回重写。
- **文档版本**：同项目同名文件重复上传，版本号自动 +1。
- **罚款**：超期天数 × 100 元；同一项目只允许一张未缴罚款单；支付为模拟回调（`trade_no=MOCK...`）。
- **日志**：写日志失败不影响主流程（`LogDao` 内部捕获）。
