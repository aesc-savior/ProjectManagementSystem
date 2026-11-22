/**
 * 离线假数据（config.offline = true 时启用）
 *
 * 用途：没开后端、没有 AppID 时也能把 14 个页面点一遍，验收时先看交互。
 * 只覆盖小程序端用到的接口，字段和后端返回保持一致。
 */
const USER = {
  userId: '2', loginId: 'zhangming', userName: '张明', role: 'PM', roleName: '项目经理',
  dept: '研发一部', phone: '13800000002', email: 'zhangming@example.com'
}

const PROJECTS = [
  {
    id: '1', projectNo: 'PM-2026-0001', name: '智能仓储管理系统', status: 'RUNNING',
    priority: 'HIGH', projectType: 'RND', startDate: '2026-09-01', endDate: '2026-11-30',
    ownerName: '张明', dept: '研发一部', description: '面向中小仓库的 WMS，含入库、出库、盘点三大模块。',
    taskCount: 12, doneCount: 7, progress: 58, overdueDays: 0
  },
  {
    id: '2', projectNo: 'PM-2026-0002', name: '客户门户改版', status: 'PENDING',
    priority: 'MID', projectType: 'BUILD', startDate: '2026-12-01', endDate: '2027-02-28',
    ownerName: '李静', dept: '前端组', description: '门户 3.0 视觉与交互重做，接入统一登录。',
    taskCount: 5, doneCount: 0, progress: 0, overdueDays: 0
  }
]

const TASKS = [
  {
    id: '101', projectId: '1', projectName: '智能仓储管理系统', parentId: '0', parentTitle: '',
    title: '入库单批量导入', description: '支持 Excel 批量导入并做行级校验。', assigneeId: '2',
    assignee: '张明', priority: 'HIGH', status: 'DOING', progress: 60,
    startDate: '2026-11-01', dueDate: '2026-11-20', estimateHours: '16', actualHours: '10',
    overdueDays: 0
  },
  {
    id: '102', projectId: '1', projectName: '智能仓储管理系统', parentId: '0', parentTitle: '',
    title: '库存水位预警', description: '低于安全库存时推送提醒。', assigneeId: '2',
    assignee: '张明', priority: 'MID', status: 'TODO', progress: 0,
    startDate: '2026-11-10', dueDate: '2026-11-25', estimateHours: '8', actualHours: '0',
    overdueDays: 0
  },
  {
    id: '103', projectId: '1', projectName: '智能仓储管理系统', parentId: '101', parentTitle: '入库单批量导入',
    title: '导入模板下载', description: '', assigneeId: '2', assignee: '张明', priority: 'LOW',
    status: 'DONE', progress: 100, startDate: '2026-11-01', dueDate: '2026-11-08',
    estimateHours: '4', actualHours: '3', overdueDays: 0
  }
]

const NOTICES = [
  {
    id: '1', title: '关于 11 月安全生产检查的通知', scope: 'ALL', projectName: '',
    publisher: '行政部', createTime: '2026-11-18 09:12:00', expireTime: '2026-11-30 23:59:59',
    readCount: 12, signed: false,
    content: '定于 11 月 22 日（周六）上午 9:30 对各办公区进行安全生产检查，请各部门提前整理工位、清理消防通道。'
  },
  {
    id: '2', title: '智能仓储项目周会改期', scope: 'PROJECT', projectName: '智能仓储管理系统',
    publisher: '张明', createTime: '2026-11-17 16:40:00', expireTime: '', readCount: 3, signed: true,
    content: '本周周会由周四下午调整至周五上午 10:00，会议室 B-302。'
  }
]

const APPROVALS = [
  {
    id: '1', bizType: 'TASK_DELAY', bizId: '102', title: '任务延期申请：库存水位预警',
    applicantId: '5', applicant: '王强', approverId: '2', approverName: '张明',
    status: 'PENDING', detail: '第三方短信通道对接延期，预计晚 5 天完成。',
    oldDate: '2026-11-25', newDate: '2026-11-30', opinion: '',
    createTime: '2026-11-19 10:05:00', approveTime: '',
    taskTitle: '库存水位预警', projectName: '智能仓储管理系统'
  },
  {
    id: '2', bizType: 'TASK_DELAY', bizId: '101', title: '任务延期申请：入库单批量导入',
    applicantId: '2', applicant: '张明', approverId: '', approverName: '',
    status: 'PENDING', detail: '客户补充了 3 类单据格式，需要重新联调。',
    oldDate: '2026-11-20', newDate: '2026-11-27', opinion: '',
    createTime: '2026-11-19 09:30:00', approveTime: '',
    taskTitle: '入库单批量导入', projectName: '智能仓储管理系统'
  },
  {
    id: '3', bizType: 'TASK_DELAY', bizId: '103', title: '任务延期申请：导入模板下载',
    applicantId: '5', applicant: '王强', approverId: '2', approverName: '张明',
    status: 'APPROVED', detail: '模板样式调整。', oldDate: '2026-11-08', newDate: '2026-11-12',
    opinion: '同意，注意同步更新说明文档。', createTime: '2026-11-05 14:20:00',
    approveTime: '2026-11-05 18:02:00', taskTitle: '导入模板下载',
    projectName: '智能仓储管理系统'
  }
]

const FINES = [
  {
    id: '1', projectId: '1', projectName: '智能仓储管理系统', projectNo: 'PM-2026-0001',
    userId: '2', userName: '张明', amount: 300, reason: '项目「智能仓储管理系统」超期 3 天，按 100 元/天计。',
    status: 'UNPAID', dueDate: '2026-11-16', tradeNo: '', payTime: '',
    createTime: '2026-11-19 11:00:00'
  },
  {
    id: '2', projectId: '2', projectName: '客户门户改版', projectNo: 'PM-2026-0002',
    userId: '2', userName: '张明', amount: 100, reason: '项目「客户门户改版」超期 1 天，按 100 元/天计。',
    status: 'PAID', dueDate: '2026-10-31', tradeNo: 'MOCK20261101093000',
    payTime: '2026-11-01 09:30:00', createTime: '2026-11-01 09:00:00'
  }
]

function page(rows, query) {
  const p = Number(query.page || 1)
  const s = Number(query.size || 10)
  const start = (p - 1) * s
  return {
    list: rows.slice(start, start + s),
    page: p,
    size: s,
    total: rows.length,
    hasMore: start + s < rows.length
  }
}

function byId(rows, id) {
  for (let i = 0; i < rows.length; i += 1) {
    if (String(rows[i].id) === String(id)) {
      return rows[i]
    }
  }
  return null
}

function match(rows, keyword, fields) {
  if (!keyword) {
    return rows
  }
  const k = String(keyword).toLowerCase()
  return rows.filter((r) => fields.some((f) => String(r[f] || '').toLowerCase().indexOf(k) >= 0))
}

/** 返回 Promise，模拟真实网络延迟 */
function delay(data, ms) {
  return new Promise((resolve) => {
    setTimeout(() => resolve(data), ms || 120)
  })
}

function handle(method, path, data) {
  const q = data || {}
  const seg = path.split('/').filter((x) => x !== '')

  if (path === '/me') {
    return delay({ user: USER })
  }
  if (path === '/auth/login' || path === '/auth/bind') {
    return delay({ token: 'offline-token', expireAt: '', mock: true, user: USER })
  }
  if (path === '/auth/logout' || path === '/auth/unbind') {
    return delay({ ok: true })
  }
  if (path === '/stats') {
    return delay({
      projectCount: PROJECTS.length,
      taskTodo: TASKS.filter((t) => t.status === 'TODO' || t.status === 'DOING').length,
      approvalPending: APPROVALS.filter((a) => a.status === 'PENDING').length,
      fineUnpaid: FINES.filter((f) => f.status === 'UNPAID').length,
      fineAmount: FINES.filter((f) => f.status === 'UNPAID').reduce((s, f) => s + f.amount, 0),
      noticeUnsigned: NOTICES.filter((n) => !n.signed).length
    })
  }

  if (seg[0] === 'projects') {
    if (seg[1]) {
      const p = byId(PROJECTS, seg[1])
      return p ? delay({ project: p, members: [{ user_id: '2', role_code: 'MANAGER', user_name: '张明', dept_name: '研发一部' }] })
        : Promise.reject(new Error('项目不存在'))
    }
    return delay(page(match(PROJECTS, q.keyword, ['name', 'projectNo']), q))
  }

  if (seg[0] === 'tasks') {
    if (seg[1] && seg[2] === 'progress') {
      const t = byId(TASKS, seg[1])
      if (!t) {
        return Promise.reject(new Error('任务不存在'))
      }
      t.progress = Number(q.progress)
      t.status = t.progress >= 100 ? 'DONE' : (t.progress > 0 ? 'DOING' : 'TODO')
      return delay({ task: t })
    }
    if (seg[1]) {
      const t = byId(TASKS, seg[1])
      return t ? delay({ task: t, children: TASKS.filter((x) => String(x.parentId) === String(t.id)) })
        : Promise.reject(new Error('任务不存在'))
    }
    let rows = TASKS
    if (q.projectId) {
      rows = rows.filter((t) => String(t.projectId) === String(q.projectId))
    }
    if (q.mine === true || q.mine === 'true') {
      rows = rows.filter((t) => String(t.assigneeId) === USER.userId)
    }
    if (q.status) {
      rows = rows.filter((t) => t.status === q.status)
    }
    return delay(page(match(rows, q.keyword, ['title']), q))
  }

  if (seg[0] === 'notices') {
    if (seg[1] && seg[2] === 'sign') {
      const n = byId(NOTICES, seg[1])
      if (n) {
        n.signed = true
        n.readCount += 1
      }
      return delay({ signed: true })
    }
    if (seg[1]) {
      const n = byId(NOTICES, seg[1])
      return n ? delay({ notice: n }) : Promise.reject(new Error('公告不存在'))
    }
    return delay(page(NOTICES, q))
  }

  if (seg[0] === 'approvals') {
    if (seg[1] && seg[2] === 'decide') {
      const a = byId(APPROVALS, seg[1])
      if (!a) {
        return Promise.reject(new Error('审批不存在'))
      }
      if (a.status !== 'PENDING') {
        return Promise.reject(new Error('该申请已被处理，请刷新'))
      }
      a.status = q.pass ? 'APPROVED' : 'REJECTED'
      a.opinion = q.opinion || ''
      a.approveTime = '2026-11-19 17:40:00'
      return delay({ pass: !!q.pass, status: a.status })
    }
    if (seg[1]) {
      const a = byId(APPROVALS, seg[1])
      return a ? delay({ approval: a }) : Promise.reject(new Error('审批不存在'))
    }
    const tab = q.tab || 'pending'
    let rows = APPROVALS
    if (tab === 'pending') {
      rows = APPROVALS.filter((a) => a.status === 'PENDING' && a.approverId === USER.userId)
    } else if (tab === 'mine') {
      rows = APPROVALS.filter((a) => a.applicantId === USER.userId)
    } else {
      rows = APPROVALS.filter((a) => a.status !== 'PENDING')
    }
    return delay(page(rows, q))
  }

  if (seg[0] === 'fines') {
    if (seg[1] && seg[2] === 'pay') {
      const f = byId(FINES, seg[1])
      if (!f) {
        return Promise.reject(new Error('罚款单不存在'))
      }
      if (f.status === 'PAID') {
        return Promise.reject(new Error('该罚款单已缴纳'))
      }
      f.status = 'PAID'
      f.tradeNo = 'MOCK' + Date.now()
      f.payTime = '2026-11-19 17:41:00'
      return delay({ paid: true, fine: f })
    }
    if (seg[1]) {
      const f = byId(FINES, seg[1])
      return f ? delay({ fine: f }) : Promise.reject(new Error('罚款单不存在'))
    }
    let rows = FINES
    if (q.status) {
      rows = rows.filter((f) => f.status === q.status)
    }
    return delay(page(rows, q))
  }

  return Promise.reject(new Error('离线模式未实现的接口：' + method + ' ' + path))
}

module.exports = { handle }
