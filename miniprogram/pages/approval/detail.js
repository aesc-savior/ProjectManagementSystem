const api = require('../../utils/request')
const auth = require('../../utils/auth')
const fmt = require('../../utils/format')
const toast = require('../../utils/toast')

Page({
  data: {
    id: '',
    item: null,
    canDecide: false,
    saving: false,
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
      this.setData({ loading: false, error: '缺少审批编号' })
      return Promise.resolve()
    }
    return api.get('/approvals/' + this.data.id, {}, { silent: true })
      .then((data) => {
        const a = data.approval || {}
        const me = (auth.currentUser() || {}).userId
        a.statusText = fmt.label('approval', a.status)
        a.statusType = fmt.type('approval', a.status)
        a.timeText = fmt.shortTime(a.createTime)
        a.oldText = a.oldDate ? fmt.date(a.oldDate) : '—'
        a.newText = a.newDate ? fmt.date(a.newDate) : '—'
        const isApprover = a.approverId && String(a.approverId) === String(me)
        const isAdmin = (auth.currentUser() || {}).role === 'ADMIN'
        this.setData({
          item: a,
          canDecide: a.status === 'PENDING' && (isApprover || isAdmin),
          loading: false,
          error: ''
        })
      })
      .catch((err) => this.setData({ loading: false, error: err.message }))
  },

  onPass() {
    toast.confirm('通过后任务截止日将自动更新为 ' + this.data.item.newText + '，确认通过？', {
      title: '通过审批',
      confirmText: '通过',
      danger: false
    }).then((ok) => {
      if (ok) {
        this.decide(true, '')
      }
    })
  },

  onReject() {
    toast.prompt('驳回意见', '请说明驳回原因（必填）').then((opinion) => {
      if (opinion === null) {
        return
      }
      if (!opinion.trim()) {
        toast.toast('驳回必须填写意见')
        return
      }
      this.decide(false, opinion.trim())
    })
  },

  decide(pass, opinion) {
    if (this.data.saving) {
      return
    }
    this.setData({ saving: true })
    api.post('/approvals/' + this.data.id + '/decide', { pass: pass, opinion: opinion }, { loading: true })
      .then(() => {
        toast.success(pass ? '已通过' : '已驳回')
        this.setData({ saving: false })
        setTimeout(() => {
          const pages = getCurrentPages()
          if (pages.length > 1) {
            wx.navigateBack()
          } else {
            wx.switchTab({ url: '/pages/approval/list' })
          }
        }, 800)
      })
      .catch(() => this.setData({ saving: false }))
  },

  toTask() {
    const a = this.data.item
    if (a && a.bizId) {
      wx.navigateTo({ url: '/pages/task/detail?id=' + a.bizId })
    }
  }
})
