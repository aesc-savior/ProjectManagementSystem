const api = require('../../utils/request')
const auth = require('../../utils/auth')
const config = require('../../utils/config')

Page({
  data: {
    checking: true,
    needBind: false,
    loginId: '',
    password: '',
    error: '',
    loading: false,
    base: ''
  },

  onLoad() {
    this.setData({ base: config.apiBase() })
    this.autoLogin()
  },

  onShow() {
    // 从设置页回来，地址可能变了
    this.setData({ base: config.apiBase() })
  },

  /** 已绑定过就直接进；未绑定（409）或连不上才停在表单 */
  autoLogin() {
    this.setData({ checking: true, error: '' })
    auth.login()
      .then(() => this.enterApp())
      .catch((err) => {
        this.setData({
          checking: false,
          needBind: true,
          error: err.statusCode === 409 ? '' : err.message
        })
      })
  },

  onInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value })
  },

  onBind() {
    const loginId = (this.data.loginId || '').trim()
    const password = this.data.password || ''
    if (!loginId || !password) {
      this.setData({ error: '请输入平台账号和密码' })
      return
    }
    this.setData({ loading: true, error: '' })
    auth.bind(loginId, password)
      .then(() => this.enterApp())
      .catch((err) => this.setData({ loading: false, error: err.message }))
  },

  enterApp() {
    this.setData({ loading: false, checking: false, needBind: false })
    wx.switchTab({ url: '/pages/project/list' })
  },

  toDev() {
    wx.navigateTo({ url: '/pages/dev/index' })
  }
})
