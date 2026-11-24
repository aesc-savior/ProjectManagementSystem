# 微信小程序端 · 完整地基说明（v2）

> 归属：F 组 · 第十二章「微信小程序端」（PM-112～PM-123）
> 本轮完成：**表 + `/api/mp/*` 全部接口 + 小程序 14 个页面 + 手机直连方案**。
> 约束：不修改 `web.xml`、`Db.java`、`ProjectDao`、`TaskDao`、`NoticeDao`、`pm.sql` 与任何 PC 端 JSP。

---

## 一、页面与功能对照

| 页面 | 路径 | 台账条目 |
|---|---|---|
| 登录 / 绑定 | `pages/login/index` | PM-112 扫码绑定（账号密码 + openid） |
| 项目列表（含工作台概览） | `pages/project/list` | PM-114 我的项目 |
| 项目详情 | `pages/project/detail` | PM-114 |
| 任务列表 | `pages/task/list` | PM-115 我的任务 |
| 任务详情 + 进度上报 | `pages/task/detail` | PM-116 任务进度更新 |
| 审批中心（待办 / 我发起 / 已办） | `pages/approval/list` | PM-118 待办审批 |
| 审批详情（通过 / 驳回） | `pages/approval/detail` | PM-119 审批处理 |
| 申请延期 | `pages/approval/apply` | PM-118 |
| 罚款台账 | `pages/fine/list` | PM-120 罚款查看 |
| 罚款详情 + 缴纳 | `pages/fine/detail` | PM-122 缴纳罚款 |
| 公告列表 | `pages/notice/list` | PM-117 公告签收 |
| 公告详情 + 签收 | `pages/notice/detail` | PM-117 |
| 我的 | `pages/mine/index` | 个人信息、退出、解绑 |
| 服务器设置 | `pages/dev/index` | 手机直连调试用 |

tabBar 顺序：**项目 / 任务 / 审批 / 我的**（按任务模块的排法）。

---

## 二、手机直连（当前方案，无需 AppID）

### 1. 后端先能被手机访问

```bash
# 1) 电脑上查局域网 IP
ipconfig                     # Windows，看 IPv4 地址
ifconfig | grep "inet "      # macOS

# 2) 用手机浏览器访问下面这个地址，能返回 JSON 就说明通了
http://192.168.1.100:9999/pm/api/mp/ping
```

返回内容类似：

```json
{"code":200,"message":"ok","data":{"service":"mp-api","mock":true,"serverTime":"2026-11-19 17:40:00"}}
```

`mock: true` 表示后端没配 AppID，走的是模拟微信登录（openid 由固定 code 生成），完全够开发与演示。

### 2. 小程序里填地址

两种方式，任选：

- **改代码**：`miniprogram/utils/config.js` 的 `baseUrl`
- **不改代码**：「我的 → 服务器设置」输入地址并保存（存本地缓存，优先级更高），页面上有「测试连接」按钮

### 3. 真机预览

1. 微信开发者工具 → 项目用 **测试号（touristappid）** 打开；
2. 点「预览」，二维码弹出后**先勾「调试」**再扫码 —— 调试模式下微信不校验 https 域名，`http://192.168.x.x` 才能用；
3. 手机与电脑必须同一个 Wi-Fi，Windows 防火墙放行 9999。

> 正式发布（体验版/正式版）必须换成已备案的 https 域名并在微信后台配置 request 合法域名，这是平台硬约束，代码层面绕不过去。

### 4. 完全没有后端时

「服务器设置」里打开**离线演示模式**，所有接口走 `utils/mock.js` 的内置假数据，14 个页面都能点通，用于先看交互。

---

## 三、接口契约

统一前缀 `/api/mp`，统一返回：

```json
{"code":200,"message":"ok","data":{...}}
```

列表接口的 `data` 固定为：

```json
{"list":[],"page":1,"size":10,"total":0,"hasMore":true}
```

鉴权：`Authorization: Bearer <token>`；图片等无法带头的场景用 `?token=<token>`。
`/ping`、`/auth/login`、`/auth/bind` 免鉴权。

### 3.1 登录与账号

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| GET | `/ping` | — | 探活，返回是否 mock |
| GET | `/config` | — | 罚款单价、令牌天数、上传上限 |
| GET | `/me` | — | 当前账号 |
| GET | `/stats` | — | 工作台概览（项目数/待办任务/待审批/未缴罚款/未签公告） |
| POST | `/auth/login` | `{code}` | 已绑定直接登录；未绑定返回 **409** |
| POST | `/auth/bind` | `{code, loginId, password}` | 首次绑定并登录 |
| POST | `/auth/logout` | — | 吊销当前令牌 |
| POST | `/auth/unbind` | — | 解绑微信并吊销全部令牌 |

### 3.2 项目 / 任务

| 方法 | 路径 | 入参 |
|---|---|---|
| GET | `/projects` | `keyword,status,page,size` |
| GET | `/projects/{id}` | 返回 `project` + `members` |
| GET | `/tasks` | `projectId,mine,status,keyword,page,size` |
| GET | `/tasks/{id}` | 返回 `task` + `children` |
| POST | `/tasks/{id}/progress` | `{progress, actualHours}` |

进度规则与 PC 端一致：`100→DONE`、`>0→DOING`、`0→TODO`，并向上汇总父任务。

### 3.3 审批（你负责的模块）

| 方法 | 路径 | 入参 |
|---|---|---|
| GET | `/approvals` | `tab=pending|mine|done`、`page,size` |
| GET | `/approvals/{id}` | 申请人、审批人、管理员可看 |
| POST | `/approvals/apply` | `{taskId, newDate, reason}` |
| POST | `/approvals/{id}/decide` | `{pass, opinion}` |

服务端已有校验：新截止日必须晚于原截止日且不早于今天；不能自己审自己（申请人即负责人时转管理员）；驳回必填意见；重复处理返回业务提示。

### 3.4 罚款（你负责的模块）

| 方法 | 路径 | 入参 |
|---|---|---|
| GET | `/fines` | `status=UNPAID|PAID`、`page,size` |
| GET | `/fines/{id}` | 只能看自己的（管理员除外） |
| POST | `/fines/{id}/pay` | 条件更新，双击不会重复扣 |

### 3.5 公告与附件

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/notices` | `page,size` |
| GET | `/notices/{id}` | 含正文 |
| POST | `/notices/{id}/sign` | 签收，唯一键防重复 |
| POST | `/upload` | multipart，字段 `file`，≤5MB，返回 `{name,size,url}` |
| GET | `/files/{name}` | 读取附件，令牌走 `?token=` |

上传只落盘不写 `pm_doc` —— 项目文档表属 D 组，不越界。返回的相对地址由前端带到业务请求里。

---

## 四、数据表（本轮唯一新增的两张）

| 表 | 作用 | 约束 |
|---|---|---|
| `pm_mp_user` | openid ↔ 平台账号 | `UNIQUE(openid)`、`UNIQUE(user_id)` |
| `pm_mp_token` | 登录态，64 位 hex，7 天 | 可按令牌吊销，过期用数据库 `now()` 判 |

执行顺序：`sql/pm.sql` → `sql/upgrade_approval_fine.sql` → `sql/upgrade_mp.sql`。

---

## 五、验交流程（建议按这个顺序演示）

1. 开发者工具打开，`pm / 123456` 绑定 → 进项目列表，顶部四个统计数字来自 `/stats`；
2. 点项目 → 看任务 → 点任务 → 拖动进度条到 60% → 保存 → 返回列表看进度条变化；
3. 任务详情点「申请延期」，选一个晚于原截止日的日期，填原因提交；
4. 用另一个账号（项目负责人）登录 → 审批中心看到待办 → 进入详情，看到「原截止日 → 申请改为」对比 → 通过 → 回到任务看截止日已变；
5. 「我发起」「已办」两个 tab 都能看到刚才那条；
6. 罚款台账 → 立即缴纳 → 合计金额归零；
7. 公告 → 签收 → 返回列表状态变「已签收」；
8. 「我的」→ 退出登录 → 重新进来自动登录（说明令牌链路通）。

---

## 六、已知限制（写在周报里，避免被当成漏做）

| 限制 | 原因 | 后续 |
|---|---|---|
| 无 AppID，登录走 mock | 后端 `WxApi` 无 appid 时 `openid = mock_ + code` | 拿到 AppID 后：`-Dpm.wx.appid/secret` + `config.mockLoginCode=false` |
| 支付是模拟 | `FineDao.pay` 直接改 `PAID` | 真支付需 `out_trade_no`、中间态、回调验签，属独立一轮 |
| 无订阅消息推送 | 需要模板 ID 与 appid | 审批结果/罚款提醒接 `subscribeMessage` |
| 手机必须开调试模式 | 微信真机默认只允许 https 合法域名 | 部署 https 域名后无需调试模式 |
| PC 扫码登录（PM-113）未做 | 要改 `login.jsp`，属 E 组登录模块 | 先与 E 组约定入口 |

---

## 七、和队友的边界

| 内容 | 归属 |
|---|---|
| `src/pm/mp/*`、`miniprogram/*`、`sql/upgrade_mp.sql`、本文件 | F 组（你） |
| `web.xml`、`Db`、`ProjectDao`、`TaskDao`、`NoticeDao`、`pm.sql` | 未改动 |
| 小程序端的项目/任务/公告查询 | 在 `MpQuery` 里自写只读 SQL，不依赖别人的 DAO 方法签名 |
| 任务进度写入、公告签收 | 也是 `MpQuery` 内实现，不走他们的 DAO |
