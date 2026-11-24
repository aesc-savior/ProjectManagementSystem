const api = require('../../utils/request')
const auth = require('../../utils/auth')
const toast = require('../../utils/toast')

Page({
  data: {
    id: '',
    notice: null,
    signing: false,
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
      this.setData({ loading: false, error: '缺少公告编号' })
      return Promise.resolve()
    }
    return api.get('/notices/' + this.data.id, {}, { silent: true })
      .then((data) => {
        this.setData({ notice: data.notice || {}, loading: false, error: '' })
        wx.setNavigationBarTitle({ title: '公告详情' })
      })
      .catch((err) => this.setData({ loading: false, error: err.message }))
  },

  onSign() {
    if (this.data.signing) {
      return
    }
    this.setData({ signing: true })
    api.post('/notices/' + this.data.id + '/sign', {}, { loading: true })
      .then(() => {
        toast.success('已签收')
        this.setData({ signing: false })
        this.load()
      })
      .catch(() => this.setData({ signing: false }))
  }
})
