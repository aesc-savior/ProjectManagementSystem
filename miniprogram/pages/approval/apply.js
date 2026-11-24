const api = require('../../utils/request')
const auth = require('../../utils/auth')
const fmt = require('../../utils/format')
const toast = require('../../utils/toast')

Page({
  data: {
    tasks: [],
    taskNames: [],
    taskIndex: -1,
    taskId: '',
    taskTitle: '',
    oldDate: '',
    newDate: '',
    minDate: '',
    reason: '',
    loading: false,
    submitting: false,
    error: ''
  },

  onLoad(options) {
    if (options.taskId) {
      const title = decodeURIComponent(options.title || '')
      this.setData({
        taskId: options.taskId,
        taskTitle: title,
        oldDate: options.dueDate || ''
      })
      this.refreshMin()
      return
    }
    this.loadTasks()
  },

  onShow() {
    if (!auth.isLogged()) {
      auth.redirectToLogin()
    }
  },

  loadTasks() {
    this.setData({ loading: true })
    api.get('/tasks', { mine: true, size: 50 }, { silent: true })
      .then((res) => {
        const tasks = (res && res.list) || []
        this.setData({
          tasks: tasks,
          taskNames: tasks.map((t) => t.title),
          loading: false
        })
      })
      .catch((err) => this.setData({ loading: false, error: err.message }))
  },

  onPickTask(e) {
    const index = Number(e.detail.value)
    const t = this.data.tasks[index]
    if (!t) {
      return
    }
    this.setData({
      taskIndex: index,
      taskId: t.id,
      taskTitle: t.title,
      oldDate: t.dueDate || '',
      newDate: ''
    })
    this.refreshMin()
  },

  /** 可选的最早日期 = max(今天, 原截止日 + 1 天) */
  refreshMin() {
    const today = fmt.today()
    let min = today
    if (this.data.oldDate) {
      const d = new Date(this.data.oldDate.replace(/-/g, '/'))
      d.setDate(d.getDate() + 1)
      const next = d.getFullYear() + '-' + ('0' + (d.getMonth() + 1)).slice(-2)
        + '-' + ('0' + d.getDate()).slice(-2)
      if (next > min) {
        min = next
      }
    }
    this.setData({ minDate: min, newDate: this.data.newDate || min })
  },

  onDate(e) {
    this.setData({ newDate: e.detail.value })
  },

  onReason(e) {
    this.setData({ reason: e.detail.value })
  },

  submit() {
    if (this.data.submitting) {
      return
    }
    if (!this.data.taskId) {
      toast.toast('请先选择要延期的任务')
      return
    }
    if (!this.data.newDate) {
      toast.toast('请选择新的截止日期')
      return
    }
    if (!this.data.reason.trim()) {
      toast.toast('请填写延期原因')
      return
    }
    this.setData({ submitting: true })
    api.post('/approvals/apply', {
      taskId: this.data.taskId,
      newDate: this.data.newDate,
      reason: this.data.reason.trim()
    }, { loading: true })
      .then(() => {
        toast.success('已提交，等待审批')
        this.setData({ submitting: false })
        setTimeout(() => {
          const pages = getCurrentPages()
          if (pages.length > 1) {
            wx.navigateBack()
          } else {
            wx.switchTab({ url: '/pages/approval/list' })
          }
        }, 800)
      })
      .catch(() => this.setData({ submitting: false }))
  }
})
