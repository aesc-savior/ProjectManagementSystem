const api = require('../../utils/request')
const auth = require('../../utils/auth')
const pager = require('../../utils/pager')
const fmt = require('../../utils/format')

// 注意顺序：页面自己的方法放在后面，才能覆盖分页组件里的同名方法（如下拉刷新要顺带刷统计）
Page(Object.assign(pager.methods({
  url: '/projects',
  params() {
    return { keyword: this.data.keyword }
  },
  map(rows) {
    return rows.map((p) => {
      const item = Object.assign({}, p)
      item.statusText = fmt.label('project', p.status)
      item.statusType = fmt.type('project', p.status)
      item.progress = Number(p.progress || 0)
      item.overdueText = Number(p.overdueDays || 0) > 0 ? ('超期 ' + p.overdueDays + ' 天') : ''
      item.endText = p.endDate ? fmt.date(p.endDate) : '未设置'
      return item
    })
  }
}), {
  data: pager.data({
    keyword: '',
    stats: {
      projectCount: 0,
      taskTodo: 0,
      approvalPending: 0,
      fineUnpaid: 0,
      fineAmount: 0,
      noticeUnsigned: 0
    }
  }),

  onShow() {
    if (!auth.isLogged()) {
      auth.redirectToLogin()
      return
    }
    this.loadStats()
    if (!this.data.loaded) {
      this.fetchList(true)
    }
  },

  onPullDownRefresh() {
    this.loadStats()
    this.fetchList(true).then(() => wx.stopPullDownRefresh())
  },

  loadStats() {
    api.get('/stats', {}, { silent: true })
      .then((s) => this.setData({ stats: s || {} }))
      .catch(() => null)
  },

  onKeyword(e) {
    this.setData({ keyword: e.detail.value })
    if (this.timer) {
      clearTimeout(this.timer)
    }
    this.timer = setTimeout(() => this.resetList(), 350)
  },

  onClearKeyword() {
    this.setData({ keyword: '' })
    this.resetList()
  },

  toDetail(e) {
    wx.navigateTo({ url: '/pages/project/detail?id=' + e.currentTarget.dataset.id })
  },

  toTask() {
    wx.switchTab({ url: '/pages/task/list' })
  },

  toApproval() {
    wx.switchTab({ url: '/pages/approval/list' })
  },

  toFine() {
    wx.navigateTo({ url: '/pages/fine/list' })
  },

  toNotice() {
    wx.navigateTo({ url: '/pages/notice/list' })
  }
}))
