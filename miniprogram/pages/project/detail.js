const api = require('../../utils/request')
const auth = require('../../utils/auth')
const fmt = require('../../utils/format')

Page({
  data: {
    id: '',
    project: null,
    members: [],
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
      this.setData({ loading: false, error: '缺少项目编号' })
      return Promise.resolve()
    }
    this.setData({ loading: true })
    return api.get('/projects/' + this.data.id, {}, { silent: true })
      .then((data) => {
        const p = data.project || {}
        p.statusText = fmt.label('project', p.status)
        p.statusType = fmt.type('project', p.status)
        p.priorityText = fmt.label('priority', p.priority)
        p.typeText = { RND: '研发', BUILD: '建设', SERVICE: '服务', OTHER: '其他' }[p.projectType] || '—'
        const members = (data.members || []).map((m) => {
          const row = Object.assign({}, m)
          row.initial = (m.user_name || '?').charAt(0)
          return row
        })
        this.setData({
          project: p,
          members: members,
          loading: false,
          error: ''
        })
        wx.setNavigationBarTitle({ title: p.name || '项目详情' })
      })
      .catch((err) => this.setData({ loading: false, error: err.message }))
  },

  toTasks() {
    const p = this.data.project
    if (!p) {
      return
    }
    // tabBar 页面不能带参数，用 globalData 传一次筛选条件
    const app = getApp()
    app.globalData.taskFilter = { projectId: p.id, projectName: p.name }
    wx.switchTab({ url: '/pages/task/list' })
  },

  toNotice() {
    wx.navigateTo({ url: '/pages/notice/list?projectId=' + this.data.id })
  },

  callOwner() {
    // 占位：成员手机号不在项目详情里返回，后续接通讯录再做
    wx.showToast({ title: '联系人功能待接入', icon: 'none' })
  }
})
