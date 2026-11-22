# 项目管理系统（ProjectManagementSystem）· 可运行骨架

由原 **档案管理系统** 代码框架重构而来：同样的 **Servlet 6 + JSP + 原生 JDBC + session/filter 登录**，业务换成项目管理。
配套文档：`项目管理系统-需求规格说明书（V1.0）.docx`、`项目管理系统-功能清单台账.xlsx`（PM-001～PM-123）。

> 定位：**可运行骨架**。把“登录 → 项目 → 任务 → 周报 → 审批 → 统计”的主线跑通，其余模块可按同一模式复制。

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
| 周报 | 填写（含附件）、文件名规范化 `项目编号-YYYYWW-姓名-周报`、提交/撤回/退回后重提、评语评分（退回必填理由）、四种条件查询 + 六维排序、工时合计、未交名单标识、清单/未交名单/单份 Excel 导出（单份另含 Word 版）、打印视图、提交统计与近 6 周趋势、一键催交 + 每周五 18:00 定时提醒（站内 + 企业微信双通道） | PM-030～PM-038 |
| 站内提醒 | 周报催交提醒汇总，支持未读计数、标记已读、一键跳转处理 | 支撑 PM-038 |
| 文档 | 上传（类型白名单）、下载、删除、按项目/关键词查询、同名自动版本号 | PM-039～PM-048 |
| 公告 | 发布（全员/项目）、签收、签收人数、删除、查询 | PM-049～PM-055 |
| 审批 | 任务延期申请（新截止日 + 原因）、待我审批、通过/驳回（通过后自动改任务截止日） | PM-080～PM-087 |
| 罚款 | 按超期天数 ×100 元生成罚款单、模拟微信支付、台账 | PM-084～PM-087 |
| 统计 | 项目/任务/周报状态分布、任务完成率、预算、未缴罚款、近 6 个月新增项目 | PM-088～PM-097 |
| 用户 | 增删改查、启停、重置密码（ADMIN） | PM-098～PM-099 |
| 日志 | 登录、增删改、提交、审批、上传下载、支付留痕，可按动作/关键词筛 | PM-104 |
| 第三方接口 | `GET /api/projects?status=&type=`、`GET /api/projects/{id}`、`DELETE /api/projects/{id}`，Bearer Token | PM-109 |

**尚未实现（下一步按 SRS 补）**：资产与合同（PM-063～PM-072）、会议与纪要（PM-056～PM-062）、甘特图（PM-024）、个人/团队驾驶舱（PM-096/097 的图形化版本）、微信小程序端（PM-112～PM-123）、真实微信支付（当前为模拟）、气象采集、备份恢复、字典配置。

## 三、目录结构

```
ArchivesManagementSystem（目录名暂未改，项目内容已是 PM 系统）
├─ sql/pm.sql                 建库建表 + 演示数据（4 账号、5 项目、10 任务、5 周报）
├─ sql/pm_v2_weekly.sql      增量升级脚本（已有库补周报附件字段和提醒表，不删数据）
├─ src/pm/
│  ├─ dao/                    Db / UserDao / ProjectDao / TaskDao / WeeklyDao / DocDao / NoticeDao / ApprovalDao / FineDao / LogDao / RemindDao
│  ├─ filter/LoginFilter.java 未登录跳登录页；/api/* 交 ApiServlet 校验 Token
│  ├─ util/PasswordUtil.java  盐 + MD5
│  ├─ util/WeeklyRemindJob.java  周报催交定时任务（每周五 18:00，-Dpm.remind.enabled=false 可关）
│  └─ web/                    Auth / Project / Task / Weekly / Doc / Notice / Approval / Fine / Stat / User / Log / Remind / Api
├─ web/
│  ├─ login.jsp  index.jsp  home/index.jsp
│  ├─ project/  task/  weekly/  doc/  notice/  approval/  fine/  stat/  user/  log/  remind/
│  ├─ WEB-INF/web.xml         过滤器 + 15 个 Servlet 映射 + listener + multipart
│  ├─ WEB-INF/inc/head.jspf   公共头（菜单按角色 + 标签辅助方法 + 未读提醒计数）
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

**已有库升级**：若库里已有业务数据，不要重跑 `pm.sql`，改执行增量脚本：

```bash
mysql -uroot -p --default-character-set=utf8mb4 < sql/pm_v2_weekly.sql
```

它只做两件事：给 `pm_weekly` 补附件与审核字段、新建站内提醒表；不删除任何既有记录。

### 2. 数据库连接（推荐用配置文件，别用 -D 参数）

配置写在 **`src/pm-local.properties`**（构建后进 `WEB-INF/classes`，随应用一起加载），无需改代码：

```properties
pm.db.url=jdbc:mysql://localhost:3306/pm?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
pm.db.user=root
pm.db.password=你的密码
pm.upload.dir=D:/pm-upload
pm.api.token=pm-demo-token
#pm.wechat.webhook=https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=你的key
```

取值优先级：`-D` 参数 ＞ 环境变量（`PM_DB_PASSWORD` 等）＞ `pm-local.properties` ＞ 代码默认值。

其他存放位置也会被自动找到（前面找不到才看后面）：
`-Dpm.config=路径` → `${catalina.base}/conf/` → 用户主目录 → 运行目录。

注意：连接串里有 `&`，不要写进 Windows 命令行、`.bat` 或 IDEA 的 VM options，
cmd 会把 `&` 当命令分隔符拆开，报一堆 `'characterEncoding' 不是内部或外部命令`。
命令行只留 `-Dfile.encoding=UTF-8`，其他参数都写进这个文件。

微信通道（企业微信群机器人）不配也能用，只是提醒列表里会标「未配置」，不会假装发送成功。

### 3. IDEA 配置

1. `Project Structure → SDK` 选 JDK 17；
2. `web/WEB-INF/lib/*.jar` 全部 `Add as Library`；
3. `Run → Edit Configurations → Tomcat Server → Local`，Deployment 加 **Artifact: war exploded**，
   **Application context 填 `/pm`**；
4. **VM options 只留** `-Dfile.encoding=UTF-8`（数据库账号密码交给 `pm-local.properties`，
   不要往这里粘带 `&` 的连接串）；
5. 启动后访问 `http://localhost:9999/pm/`（端口以本机 Tomcat `server.xml` 为准）。

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

## 六、第三方接口示例

```bash
# 查询（≥2 个组合条件）
curl -H "Authorization: Bearer pm-demo-token" \
     "http://localhost:9999/pm/api/projects?status=RUNNING&type=RND"

# 删除（逻辑删除）
curl -X DELETE -H "Authorization: Bearer pm-demo-token" \
     "http://localhost:9999/pm/api/projects/1"
```

未带 Token 或 Token 错误返回 `401`；返回体统一为 `{code,message,data}`。

## 七、设计约定

- **数据权限**：管理员看全部；其他角色看“自己负责 + 自己参与”的项目，任务/周报/文档/公告按项目联动。
- **项目状态**：`PENDING → RUNNING → PAUSED/DONE/CANCELED → ARCHIVED`，归档后只读。
- **任务进度**：父任务进度 = 子任务平均进度；任务 100% 自动置 `DONE`。
- **周报**：同一人同一项目同一周唯一；提交后锁定，可被项目经理退回重写。
- **文档版本**：同项目同名文件重复上传，版本号自动 +1。
- **罚款**：超期天数 × 100 元；同一项目只允许一张未缴罚款单；支付为模拟回调（`trade_no=MOCK...`）。
- **日志**：写日志失败不影响主流程（`LogDao` 内部捕获）。

## 八、已知限制

- 资产与合同、会议与纪要、甘特图、驾驶舱、小程序尚未实现；
- 微信支付、订阅消息为模拟，未接入真实商户号/AppID；
- SQL 为字符串拼接（课程项目风格），生产应改 PreparedStatement；
- 上传目录默认在系统临时目录，正式部署请设置 `-Dpm.upload.dir`；
- 旧档案系统的表 `archive` 未被删除，如需清理请自行 `drop database archive`。
