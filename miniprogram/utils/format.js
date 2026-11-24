/**
 * 展示层格式化：日期、金额、各种状态文案与配色。
 * 放在一处，页面里不再各写一套 if-else。
 */

const STATUS = {
  project: {
    PENDING: { text: '待启动', type: 'info' },
    RUNNING: { text: '进行中', type: 'primary' },
    PAUSED: { text: '已暂停', type: 'warn' },
    DONE: { text: '已完成', type: 'success' },
    CANCELED: { text: '已取消', type: 'default' },
    ARCHIVED: { text: '已归档', type: 'default' }
  },
  task: {
    TODO: { text: '待开始', type: 'info' },
    DOING: { text: '进行中', type: 'primary' },
    PAUSED: { text: '已暂停', type: 'warn' },
    DONE: { text: '已完成', type: 'success' },
    CANCELED: { text: '已取消', type: 'default' }
  },
  approval: {
    PENDING: { text: '待审批', type: 'warn' },
    APPROVED: { text: '已通过', type: 'success' },
    REJECTED: { text: '已驳回', type: 'danger' }
  },
  fine: {
    UNPAID: { text: '待缴纳', type: 'danger' },
    PAID: { text: '已缴纳', type: 'success' }
  },
  priority: {
    HIGH: { text: '高', type: 'danger' },
    MID: { text: '中', type: 'warn' },
    LOW: { text: '低', type: 'info' }
  }
}

function label(map, key) {
  const m = STATUS[map] && STATUS[map][key]
  return m ? m.text : (key || '—')
}

function type(map, key) {
  const m = STATUS[map] && STATUS[map][key]
  return m ? m.type : 'default'
}

/** 列表里一次性补上展示字段，避免 wxml 里写逻辑 */
function decorate(list, map, field) {
  const key = field || 'status'
  return (list || []).map((row) => {
    const item = Object.assign({}, row)
    item.statusText = label(map, row[key])
    item.statusType = type(map, row[key])
    item.priorityText = label('priority', row.priority)
    item.priorityType = type('priority', row.priority)
    return item
  })
}

function pad(n) {
  return n < 10 ? '0' + n : '' + n
}

/** '2026-11-19 10:05:00' → '11-19 10:05' */
function shortTime(v) {
  if (!v) {
    return '—'
  }
  return String(v).replace(/^\d{4}-/, '').substring(0, 11)
}

/** 只取日期部分 */
function date(v) {
  if (!v) {
    return '—'
  }
  return String(v).substring(0, 10)
}

/** 今天往后 n 天的日期字符串，用于日期选择器默认值 */
function plusDays(n) {
  const d = new Date()
  d.setDate(d.getDate() + (n || 0))
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate())
}

function today() {
  return plusDays(0)
}

/** 金额（后端返回的是整数元） */
function money(v) {
  const n = Number(v || 0)
  const s = n.toFixed(2)
  const parts = s.split('.')
  return parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',') + '.' + parts[1]
}

/** 超期天数 → 文案 */
function overdue(days) {
  const d = Number(days || 0)
  if (d > 0) {
    return '已超期 ' + d + ' 天'
  }
  if (d === 0) {
    return '今天到期'
  }
  return '剩余 ' + (-d) + ' 天'
}

module.exports = {
  STATUS: STATUS,
  label: label,
  type: type,
  decorate: decorate,
  shortTime: shortTime,
  date: date,
  today: today,
  plusDays: plusDays,
  money: money,
  overdue: overdue
}
