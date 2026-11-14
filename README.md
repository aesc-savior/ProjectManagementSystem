# 档案管理系统 · 基础框架（archive-demo）

基于 `DeviceDemo` 的网页框架改造而来：把 device 业务换成档案业务，并补上了分类、借阅、日志、附件上传。

> 定位：**可运行的骨架**，不是成品。

---

## 一、技术栈

| 项 | 版本 / 说明 |
|---|---|
| JDK | 17（DeviceDemo 同款） |
| Web 容器 | Tomcat 10.1（Servlet 6，`jakarta.servlet.*`） |
| 页面 | JSP + 手写 CSS（不依赖 Metronic，改起来轻） |
| 数据访问 | 原生 JDBC + 拼接 SQL（与 DeviceDemo 一致，便于课程讲解） |
| 数据库 | MySQL 8.0，表前缀 `da_`，字符集 utf8mb4 |
| 密码 | 随机盐 + MD5(盐+密码)，与 DeviceDemo 的 `PasswordUtil` 算法一致 |
| 上传 | `Part` API（`@MultipartConfig` / web.xml `multipart-config`），单文件 ≤200MB |

## 二、目录结构

```
档案管理系统-基础框架/
├─ sql/archive.sql            建库建表 + 演示数据（4 个账号、7 个分类、6 条档案）
├─ src/archive/
│  ├─ dao/                    Db / ArchiveDao / UserDao / BorrowDao / LogDao
│  ├─ filter/LoginFilter.java 未登录拦到登录页
│  ├─ util/PasswordUtil.java  盐 + MD5
│  └─ web/                    AuthServlet / ArchiveServlet / UserServlet / BorrowServlet / LogServlet / Web
├─ web/
│  ├─ login.jsp               登录页
│  ├─ home/index.jsp          首页概览（统计卡 + 最近档案）
│  ├─ archive/                list / form / view（列表、新增编辑、详情+附件+借阅）
│  ├─ user/                   list / form（用户管理）
│  ├─ borrow/list.jsp         我的借阅 / 待审批 / 全部记录
│  ├─ log/list.jsp            操作日志
│  ├─ WEB-INF/
│  │  ├─ web.xml              过滤器 + 5 个 Servlet 映射 + multipart 配置
│  │  ├─ inc/head.jsp         公共头（顶栏 + 侧边菜单，按角色显示）
│  │  ├─ inc/foot.jsp         公共尾
│  │  ├─ lib/                 mysql-connector-java-8.0.20.jar
│  │  └─ classes/             已编译的 class（可直接部署；IDEA 会覆盖）
│  └─ assets/css/app.css      全站样式
└─ README.md
```

## 三、跑起来（三步）

### 1. 建库

```sql
-- 用 Navicat / 命令行都行
create database archive default charset utf8mb4 collate utf8mb4_general_ci;
```

然后执行 `sql/archive.sql`（建 6 张表 + 演示数据）。

### 2. 改数据库连接（只有一处）

打开 `src/archive/dao/Db.java`，改这三行的默认值：

```java
URL      = "jdbc:mysql://localhost:3306/archive?...";
USER     = "root";
PASSWORD = "123456";
```

不想改代码，也可以在 Tomcat 的 VM options 里加：

```
-Darchive.db.url="jdbc:mysql://localhost:3366/archive?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
-Darchive.db.user=root
-Darchive.db.password=你的密码
-Darchive.upload.dir=D:/archive-upload
```

### 3. 用 IDEA 打开并配置

1. `File → Open` 选择本目录（不要选成 DeviceDemo）。
2. `Project Structure → SDK` 选 JDK 17。
3. 把 `web/WEB-INF/lib/*.jar` 全部 `Add as Library`（DeviceDemo 的 `.iml` 也是这么配的）。
4. `Run → Edit Configurations → + Tomcat Server → Local`，Deployment 里加 **Artifact: archive-demo:war exploded**，
   Application context 填 **`/archive`**。
5. 启动，访问 `http://localhost:8080/archive/`。

> 手工部署（不用 IDEA）：把 `web` 目录整体作为 docBase，在 `conf/Catalina/localhost/` 下建 `archive.xml`：
> ```xml
> <Context docBase="D:/apps/archive" path="/archive" reloadable="false" />
> ```
> 并把 `web/WEB-INF/classes` 与 `web/WEB-INF/lib` 一起带上。

### 4. 演示账号（密码都是 `123456`）

| 账号 | 角色 | 能做什么 |
|---|---|---|
| `admin` | 系统管理员（密级 4） | 全部 + 用户管理 + 日志 |
| `archivist` | 档案管理员（密级 3） | 档案增删改、归档、上传、审批借阅、看日志 |
| `user` | 部门用户（密级 2） | 检索、查看、下载、申请借阅 |
| `wangwu` | 部门用户（密级 1） | 同上，但只能看公开级档案 |

## 四、已实现并验证过的功能

本框架已在本机（Tomcat 10.1 + MySQL 8）实际跑通以下链路，不是“写完没跑”：

| 功能 | 入口 | 验证结果 |
|---|---|---|
| 登录 / 退出 / 会话 | `/auth` | admin 登录后跳首页；未登录访问业务页 302 到登录页 |
| 角色控制 | 各 Servlet + JSP | 部门用户访问 `/user` 被 302 回首页并提示“只有系统管理员可以管理用户” |
| 密级控制 | 档案详情 | 密级 2 的用户打开 4 级档案，页面显示“密级不足”且不返回正文 |
| 档案列表 + 组合查询 | `/archive?action=list` | 分类/年度/密级/状态/关键词 5 个条件，分页 10 条 |
| 新增档案 | `/archive?action=save` | 自动生成 `DA-2026-0007`，保存后跳详情 |
| 编辑 / 归档 / 逻辑删除 | `/archive?action=editForm|archive|delete` | 归档后状态变“已归档”并写归档时间 |
| 附件上传 / 下载 / 删除 | `/archive?action=upload|download|deleteFile` | 上传后落盘、详情可见、可下载 |
| 借阅申请 → 审批 → 归还 | `/borrow` | 申请后出现在待审批；通过后档案变“借出中”；日志记录 APPROVE |
| 用户管理 | `/user` | 新增/编辑/停用/删除；管理员不可删除、不能停用自己 |
| 操作日志 | `/log` | 登录、增删改、审批、上传下载全部留痕，可按动作与关键词筛 |

对应关系（功能清单编号 → 本框架）：

| 功能清单 | 状态 |
|---|---|
| F-001、F-004、F-009、F-013、F-016～F-018、F-025、F-028、F-041～F-043、F-050～F-053、F-056、F-064、F-079 | 已实现骨架 |
| F-002 批量导入、F-007 断点续传、F-019 全文检索、F-032～F-035 完整借阅提醒、F-044/045 到期与延期、F-046～F-049 销毁、F-072 图表 | 待补 |
| 小程序、驾驶舱、统计图表 | 未开始（建议按 W2 计划排） |

## 五、代码怎么读（一次请求的完整路径）

以“新增档案”为例：

1. `web/archive/form.jsp` 表单 POST 到 `/archive?action=save`。
2. `ArchiveServlet.doPost()` 判断 `action`，收集参数，调 `ArchiveDao.save()`。
3. `ArchiveDao.save()` 拼接 SQL：新增时先 `nextArchiveNo()` 生成档号，再 `insert`。
4. `Db.update()` 打开连接、执行、关闭（try-with-resources）。
5. 保存后 `sendRedirect` 到详情页；`LogDao.write()` 记一条 ADD 日志。
6. 详情页 `view.jsp` 用 `ArchiveDao.files()` 取附件，按角色显示“编辑/归档/上传/借阅”。

> 这套“页面 → Servlet 分发 action → DAO 拼 SQL → Db 执行”的结构，就是 DeviceDemo 的结构，
> 只是把 `device` 换成 `archive`、把 JSON 返回换成了服务端渲染。

## 六、下一步建议（按优先级）

1. **补统计接口**：`ArchiveDao.stats()` 已经有 7 个指标，接到 ECharts 就是一页统计（F-064/072）。
2. **补借阅提醒**：加一个定时任务扫描 `da_borrow` 的 `end_date`，到期前 1 天写消息（F-035）。
3. **补批量导入**：用 POI 读 Excel（DeviceDemo 的 `WEB-INF/lib` 里有 poi-3.8）写 `ArchiveDao.importExcel()`（F-002）。
4. **补权限细化**：现在是“角色 + 密级”，可再加 `da_menu`/`da_role_menu` 做菜单级配置（F-051 完整版）。
5. **换 UI**：如果答辩要求好看，把 `assets/css/app.css` 换成 Metronic 的样式即可，页面结构不用动。

## 七、常见问题

| 现象 | 原因 / 处理 |
|---|---|
| 启动报 `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | `WEB-INF/lib` 里缺 mysql-connector，或没 Add as Library |
| 500 且日志 `Unknown database 'archive'` | 没建库，或 `Db.java` 里的库名不对 |
| 500 `Access denied for user` | 账号密码不对；注意 MySQL 8 需要 `allowPublicKeyRetrieval=true` |
| 中文乱码 | 连接串带 `characterEncoding=UTF-8`；JSP 已声明 `pageEncoding="UTF-8"` |
| 上传报 `FileSizeLimitExceeded` | 超过 200MB；改 `web.xml` 的 `max-file-size` 与 `@MultipartConfig` |
| 页面 404 `/archive/` | IDEA 的 Application context 没设成 `/archive` |
| JSP 改了不生效 | `reloadable` 为 false 时需重启；开发期可改成 true |

## 八、与 DeviceDemo 的对应关系

| DeviceDemo | 本框架 | 说明 |
|---|---|---|
| `src/device/dao/Db.java` | `src/archive/dao/Db.java` | 连接参数改为可配置，补 try-with-resources |
| `src/device/dao/DeviceDao.java` | `src/archive/dao/ArchiveDao.java` | 表从 project_todo 换成 da_archive，加了分类/附件/统计 |
| `src/device/dao/Data.java`（JSON 包装） | 不再需要 | 改服务端渲染，直接用 Map 传参 |
| `src/user/center/ServletAction.java` | `src/archive/web/AuthServlet.java` | 去掉 JSON 分支，直接重定向 |
| `src/user/filter/LoginFilter.java` | `src/archive/filter/LoginFilter.java` | 放行规则保留，路径改为本项目的 |
| `src/user/util/PasswordUtil.java` | `src/archive/util/PasswordUtil.java` | 算法完全一致 |
| `web/device/file/*.jsp` | `web/archive/*.jsp` | 从简单页升级为统一布局 |
| `web/home/frame/*.jsp` | `web/WEB-INF/inc/head.jsp`、`foot.jsp` | 菜单从数据库驱动改成按角色静态渲染，少一层依赖 |

---

**一句话**：把这份骨架 clone 下来，改 `Db.java` 的库名密码、跑一遍 SQL、配好 Tomcat，就能登录进系统看到 6 条档案、上传附件、走一遍借阅审批。剩下的按功能清单往里填模块即可。
