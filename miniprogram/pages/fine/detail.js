const api = require('../../utils/request')
const auth = require('../../utils/auth')
const fmt = require('../../utils/format')
const toast = require('../../utils/toast')

Page({
  data: {
    id: '',
    fine: null,
    canPay: false,
    paying: false,
    loading: true,
    error: ''
  },

  onLoad(options) {
    this.setData({ id: options.id || '' })
  },

  onShow() {
    if (!auth.isLogged()) {
      auth.redirectToLogin()
      return
    }
    this.load()
  },

  load() {
    if (!this.data.id) {
      this.setData({ loading: false, error: '缺少罚款单编号' })
      return Promise.resolve()
    }
    return api.get('/fines/' + this.data.id, {}, { silent: true })
      .then((data) => {
        const f = data.fine || {}
        f.statusText = fmt.label('fine', f.status)
        f.statusType = fmt.type('fine', f.status)
        f.amountText = fmt.money(f.amount)
        this.setData({
          fine: f,
          canPay: f.status === 'UNPAID',
          loading: false,
          error: ''
        })
      })
      .catch((err) => this.setData({ loading: false, error: err.message }))
  },

  onPay() {
    if (this.data.paying) {
      return
    }
    toast.confirm('确认缴纳 ¥' + this.data.fine.amountText + ' ？', {
      title: '缴纳罚款',
      confirmText: '确认缴纳',
      danger: false
    }).then((ok) => {
      if (!ok) {
        return
      }
      this.setData({ paying: true })
      api.post('/fines/' + this.data.id + '/pay', {}, { loading: true })
        .then(() => {
          toast.success('缴纳成功')
          this.setData({ paying: false })
          this.load()
        })
        .catch(() => this.setData({ paying: false }))
    })
  },

  toProject() {
    if (this.data.fine) {
      wx.navigateTo({ url: '/pages/project/detail?id=' + this.data.fine.projectId })
    }
  }
})
