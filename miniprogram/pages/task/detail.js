const api = require('../../utils/request')
const auth = require('../../utils/auth')
const fmt = require('../../utils/format')
const toast = require('../../utils/toast')

Page({
  data: {
    id: '',
    task: null,
    children: [],
    progress: 0,
    hours: '',
    canEdit: false,
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

  onPullDownRefresh() {
    this.load().then(() => wx.stopPullDownRefresh())
  },

  load() {
    if (!this.data.id) {
      this.setData({ loading: false, error: '缺少任务编号' })
      return Promise.resolve()
    }
    this.setData({ loading: true })
    return api.get('/tasks/' + this.data.id, {}, { silent: true })
      .then((data) => {
        const t = data.task || {}
        const me = (auth.currentUser() || {}).userId
        t.statusText = fmt.label('task', t.status)
        t.statusType = fmt.type('task', t.status)
        t.priorityText = fmt.label('priority', t.priority)
        t.priorityType = fmt.type('priority', t.priority)
        t.dueText = t.dueDate ? fmt.date(t.dueDate) : '未设置'
        t.overdueText = fmt.overdue(Number(t.overdueDays || 0))
        const children = (data.children || []).map((c) => {
          const item = Object.assign({}, c)
          item.statusText = fmt.label('task', c.status)
          item.statusType = fmt.type('task', c.status)
          return item
        })
        this.setData({
          task: t,
          children: children,
          progress: Number(t.progress || 0),
          hours: t.actualHours || '',
          canEdit: !!me && String(t.assigneeId) === String(me),
          loading: false,
          error: ''
        })
        wx.setNavigationBarTitle({ title: t.title || '任务详情' })
      })
      .catch((err) => this.setData({ loading: false, error: err.message }))
  },

  onProgress(e) {
    this.setData({ progress: e.detail.value })
  },

  onHours(e) {
    this.setData({ hours: e.detail.value })
  },

  saveProgress() {
    if (this.data.saving) {
      return
    }
    this.setData({ saving: true })
    api.post('/tasks/' + this.data.id + '/progress', {
      progress: this.data.progress,
      actualHours: this.data.hours
    }, { loading: true })
      .then(() => {
        toast.success('进度已更新')
        this.setData({ saving: false })
        this.load()
      })
      .catch(() => this.setData({ saving: false }))
  },

  quickDone() {
    this.setData({ progress: 100 })
    this.saveProgress()
  },

  applyDelay() {
    const t = this.data.task
    if (!t) {
      return
    }
    wx.navigateTo({
      url: '/pages/approval/apply?taskId=' + t.id
        + '&title=' + encodeURIComponent(t.title)
        + '&dueDate=' + (t.dueDate || '')
    })
  },

  toProject() {
    if (this.data.task) {
      wx.navigateTo({ url: '/pages/project/detail?id=' + this.data.task.projectId })
    }
  }
})
