const api = require('../../utils/request')
const auth = require('../../utils/auth')
const pager = require('../../utils/pager')
const fmt = require('../../utils/format')

const TABS = [
  { key: '', text: '全部' },
  { key: 'TODO', text: '待开始' },
  { key: 'DOING', text: '进行中' },
  { key: 'DONE', text: '已完成' }
]

Page(Object.assign({
  data: pager.data({
    tabs: TABS,
    status: '',
    mine: false,
    keyword: '',
    projectId: '',
    projectName: ''
  }),

  onShow() {
    if (!auth.isLogged()) {
      auth.redirectToLogin()
      return
    }
    const app = getApp()
    const filter = app.globalData && app.globalData.taskFilter
    if (filter) {
      app.globalData.taskFilter = null
      this.setData({
        projectId: filter.projectId || '',
        projectName: filter.projectName || '',
        status: '',
        mine: false
      })
      this.resetList()
      return
    }
    if (!this.data.loaded) {
      this.fetchList(true)
    }
  },

  onTab(e) {
    const key = e.currentTarget.dataset.key || ''
    if (key === this.data.status) {
      return
    }
    this.setData({ status: key })
    this.resetList()
  },

  onToggleMine() {
    this.setData({ mine: !this.data.mine })
    this.resetList()
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
    if (this.timer) {
      clearTimeout(this.timer)
    }
    this.timer = setTimeout(() => this.resetList(), 350)
  },

  clearProject() {
    this.setData({ projectId: '', projectName: '' })
    this.resetList()
  },

  toDetail(e) {
    wx.navigateTo({ url: '/pages/task/detail?id=' + e.currentTarget.dataset.id })
  }
}, pager.methods({
  url: '/tasks',
  params() {
    return {
      projectId: this.data.projectId,
      status: this.data.status,
      mine: this.data.mine,
      keyword: this.data.keyword
    }
  },
  map(rows) {
    return rows.map((t) => {
      const item = Object.assign({}, t)
      item.statusText = fmt.label('task', t.status)
      item.statusType = fmt.type('task', t.status)
      item.priorityText = fmt.label('priority', t.priority)
      item.priorityType = fmt.type('priority', t.priority)
      item.progress = Number(t.progress || 0)
      item.dueText = t.dueDate ? fmt.date(t.dueDate) : '未设置'
      item.overdueText = Number(t.overdueDays || 0) > 0 ? ('超期 ' + t.overdueDays + ' 天') : ''
      return item
    })
  }
})))
