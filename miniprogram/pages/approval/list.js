const api = require('../../utils/request')
const auth = require('../../utils/auth')
const pager = require('../../utils/pager')
const fmt = require('../../utils/format')

const TABS = [
  { key: 'pending', text: '待我审批' },
  { key: 'mine', text: '我发起' },
  { key: 'done', text: '已办' }
]

Page(Object.assign(pager.methods({
  url: '/approvals',
  params() {
    return { tab: this.data.tab }
  },
  map(rows) {
    return rows.map((a) => {
      const item = Object.assign({}, a)
      item.statusText = fmt.label('approval', a.status)
      item.statusType = fmt.type('approval', a.status)
      item.timeText = fmt.shortTime(a.createTime)
      item.oldText = a.oldDate ? fmt.date(a.oldDate) : '—'
      item.newText = a.newDate ? fmt.date(a.newDate) : '—'
      return item
    })
  }
}), {
  data: pager.data({
    tabs: TABS,
    tab: 'pending',
    counts: { pending: 0 }
  }),

  onShow() {
    if (!auth.isLogged()) {
      auth.redirectToLogin()
      return
    }
    this.loadCounts()
    // 处理完一条返回时要看到最新状态，所以每次都重拉第一页
    this.fetchList(true)
  },

  onPullDownRefresh() {
    this.loadCounts()
    this.fetchList(true).then(() => wx.stopPullDownRefresh())
  },

  loadCounts() {
    api.get('/stats', {}, { silent: true })
      .then((s) => this.setData({ counts: { pending: (s && s.approvalPending) || 0 } }))
      .catch(() => null)
  },

  onTab(e) {
    const key = e.currentTarget.dataset.key
    if (key === this.data.tab) {
      return
    }
    this.setData({ tab: key, list: [], page: 1, hasMore: true })
    this.fetchList(true)
  },

  toDetail(e) {
    wx.navigateTo({ url: '/pages/approval/detail?id=' + e.currentTarget.dataset.id })
  },

  toApply() {
    wx.navigateTo({ url: '/pages/approval/apply' })
  }
}))
