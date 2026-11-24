const api = require('../../utils/request')
const auth = require('../../utils/auth')
const pager = require('../../utils/pager')
const fmt = require('../../utils/format')

Page(Object.assign({
  data: pager.data({
    unsigned: 0
  }),

  onShow() {
    if (!auth.isLogged()) {
      auth.redirectToLogin()
      return
    }
    this.fetchList(true)
    api.get('/stats', {}, { silent: true })
      .then((s) => this.setData({ unsigned: (s && s.noticeUnsigned) || 0 }))
      .catch(() => null)
  },

  toDetail(e) {
    wx.navigateTo({ url: '/pages/notice/detail?id=' + e.currentTarget.dataset.id })
  }
}, pager.methods({
  url: '/notices',
  map(rows) {
    return rows.map((n) => {
      const item = Object.assign({}, n)
      item.signedText = n.signed ? '已签收' : '待签收'
      item.signedType = n.signed ? 'success' : 'warn'
      item.timeText = fmt.shortTime(n.createTime)
      item.scopeText = n.scope === 'ALL' ? '全员' : (n.projectName || '项目')
      return item
    })
  }
})))
