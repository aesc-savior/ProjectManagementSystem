const api = require('../../utils/request')
const auth = require('../../utils/auth')
const pager = require('../../utils/pager')
const fmt = require('../../utils/format')
const toast = require('../../utils/toast')

const TABS = [
  { key: '', text: '全部' },
  { key: 'UNPAID', text: '待缴纳' },
  { key: 'PAID', text: '已缴纳' }
]

Page(Object.assign(pager.methods({
  url: '/fines',
  params() {
    return { status: this.data.status }
  },
  map(rows) {
    return rows.map((f) => {
      const item = Object.assign({}, f)
      item.statusText = fmt.label('fine', f.status)
      item.statusType = fmt.type('fine', f.status)
      item.amountText = fmt.money(f.amount)
      item.timeText = fmt.shortTime(f.payTime || f.createTime)
      item.canPay = f.status === 'UNPAID'
      return item
    })
  }
}), {
  data: pager.data({
    tabs: TABS,
    status: '',
    summary: { count: 0, amount: 0 }
  }),

  onShow() {
    if (!auth.isLogged()) {
      auth.redirectToLogin()
      return
    }
    this.loadSummary()
    this.fetchList(true)
  },

  onPullDownRefresh() {
    this.loadSummary()
    this.fetchList(true).then(() => wx.stopPullDownRefresh())
  },

  loadSummary() {
    api.get('/stats', {}, { silent: true })
      .then((s) => this.setData({
        summary: { count: (s && s.fineUnpaid) || 0, amount: (s && s.fineAmount) || 0 }
      }))
      .catch(() => null)
  },

  onTab(e) {
    const key = e.currentTarget.dataset.key || ''
    if (key === this.data.status) {
      return
    }
    this.setData({ status: key, list: [], page: 1, hasMore: true })
    this.fetchList(true)
  },

  toDetail(e) {
    wx.navigateTo({ url: '/pages/fine/detail?id=' + e.currentTarget.dataset.id })
  },

  /** 列表里直接缴，演示时少点一层 */
  onPay(e) {
    const id = e.currentTarget.dataset.id
    const amount = e.currentTarget.dataset.amount
    toast.confirm('确认缴纳 ¥' + amount + ' ？演示环境走模拟支付，不产生真实扣款。', {
      title: '缴纳罚款',
      confirmText: '确认缴纳',
      danger: false
    }).then((ok) => {
      if (!ok) {
        return
      }
      api.post('/fines/' + id + '/pay', {}, { loading: true })
        .then(() => {
          toast.success('缴纳成功')
          this.loadSummary()
          this.fetchList(true)
        })
        .catch(() => null)
    })
  }
}))
