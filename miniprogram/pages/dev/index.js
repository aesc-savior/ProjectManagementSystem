const api = require('../../utils/request')
const config = require('../../utils/config')
const toast = require('../../utils/toast')

Page({
  data: {
    draft: '',
    current: '',
    offline: false,
    pinging: false,
    result: '',
    resultOk: false
  },

  onLoad() {
    this.setData({
      draft: config.apiBase(),
      current: config.apiBase(),
      offline: config.isOffline()
    })
  },

  onInput(e) {
    this.setData({ draft: e.detail.value.trim(), result: '' })
  },

  onSave() {
    const url = this.data.draft
    if (!/^https?:\/\/.+/.test(url)) {
      toast.toast('地址要以 http:// 或 https:// 开头')
      return
    }
    config.setBase(url)
    this.setData({ current: config.apiBase() })
    api.clearToken()
    toast.success('已保存，请重新登录')
    setTimeout(() => {
      wx.reLaunch({ url: '/pages/login/index' })
    }, 900)
  },

  onReset() {
    config.setBase('')
    this.setData({ draft: config.baseUrl, current: config.baseUrl })
    toast.toast('已恢复默认地址')
  },

  /** 直接打 /ping，不经过业务封装，排除登录态干扰 */
  onTest() {
    if (this.data.pinging) {
      return
    }
    this.setData({ pinging: true, result: '' })
    wx.request({
      url: this.data.draft.replace(/\/+$/, '') + '/ping',
      method: 'GET',
      timeout: 8000,
      success: (res) => {
        const ok = res.statusCode === 200 && res.data && res.data.code === 200
        this.setData({
          pinging: false,
          resultOk: ok,
          result: ok
            ? '连接成功，微信登录模式：' + (res.data.data.mock ? 'mock（未配 AppID）' : '正式')
            : '连上了但返回异常（HTTP ' + res.statusCode + '）'
        })
      },
      fail: () => {
        this.setData({
          pinging: false,
          resultOk: false,
          result: '连不上。检查：同一 Wi-Fi？Tomcat 已启动？防火墙放行 9999？地址含 /pm/api/mp？'
        })
      }
    })
  },

  onToggleOffline(e) {
    const v = e.detail.value
    config.setOffline(v)
    this.setData({ offline: v })
    toast.toast(v ? '已切到离线演示（假数据）' : '已切回真实后端')
    api.clearToken()
  }
})
