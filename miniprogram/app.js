const api = require('./utils/request')

App({
  globalData: {
    user: null,
    // 状态文案表由 format.js 统一维护，这里不再重复
    lastError: ''
  },

  onLaunch() {
    // 有令牌就先当作已登录，具体有效性由各页 /me 校验（401 会自动回登录页）
    this.globalData.user = api.getUser()
    this.watchUpdate()
  },

  /** 小版本热更：下次冷启动生效，不打断当前操作 */
  watchUpdate() {
    if (!wx.getUpdateManager) {
      return
    }
    const manager = wx.getUpdateManager()
    manager.onUpdateReady(() => {
      wx.showModal({
        title: '发现新版本',
        content: '已为你准备好新版本，是否立即重启？',
        success(res) {
          if (res.confirm) {
            manager.applyUpdate()
          }
        }
      })
    })
  },

  onError(msg) {
    this.globalData.lastError = msg
    console.error('[app error]', msg)
  },

  /** 统一的用户信息读取，页面可直接调 */
  user() {
    return this.globalData.user || api.getUser()
  }
})
