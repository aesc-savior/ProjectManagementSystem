const api = require('../../utils/request')
const auth = require('../../utils/auth')
const config = require('../../utils/config')
const toast = require('../../utils/toast')

Page({
  data: {
    user: null,
    stats: {},
    env: '',
    initial: '',
    loading: true
  },

  onShow() {
    if (!auth.isLogged()) {
      auth.redirectToLogin()
      return
    }
    const u = auth.currentUser()
    this.setData({
      user: u,
      initial: (u && u.userName ? u.userName.charAt(0) : '我'),
      env: config.isOffline() ? '离线演示模式' : config.apiBase()
    })
    this.load()
  },

  load() {
    auth.ensureLogin()
      .then((u) => this.setData({
        user: u,
        initial: (u && u.userName ? u.userName.charAt(0) : '我'),
        loading: false
      }))
      .catch(() => this.setData({ loading: false }))
    api.get('/stats', {}, { silent: true })
      .then((s) => this.setData({ stats: s || {} }))
      .catch(() => null)
  },

  toApproval() {
    wx.switchTab({ url: '/pages/approval/list' })
  },

  toFine() {
    wx.navigateTo({ url: '/pages/fine/list' })
  },

  toNotice() {
    wx.navigateTo({ url: '/pages/notice/list' })
  },

  toDev() {
    wx.navigateTo({ url: '/pages/dev/index' })
  },

  onLogout() {
    toast.confirm('退出后需要重新绑定微信才能登录，确认退出？', { title: '退出登录' })
      .then((ok) => {
        if (ok) {
          auth.logout()
        }
      })
  },

  onUnbind() {
    toast.confirm('解绑后这台微信将无法自动登录，需要重新用账号密码绑定。确定解绑？', {
      title: '解绑微信'
    }).then((ok) => {
      if (!ok) {
        return
      }
      api.post('/auth/unbind', {}, { loading: true })
        .then(() => {
          api.clearToken()
          toast.success('已解绑')
          setTimeout(() => auth.redirectToLogin(), 800)
        })
        .catch(() => null)
    })
  }
})
