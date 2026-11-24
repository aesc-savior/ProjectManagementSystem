/**
 * 分页列表的公共逻辑（下拉刷新 / 上拉加载 / 空态 / 错误重试）
 *
 * 用法：
 *   const pager = require('../../utils/pager')
 *   Page(Object.assign({
 *     data: pager.data({ keyword: '' }),
 *     onLoad() { this.fetchList(true) }
 *   }, pager.methods({ url: '/projects', params() { return { keyword: this.data.keyword } } })))
 */
const api = require('./request')
const config = require('./config')

function data(extra) {
  return Object.assign({
    list: [],
    page: 1,
    size: config.pageSize,
    total: 0,
    hasMore: true,
    loading: false,
    loaded: false,
    error: ''
  }, extra || {})
}

/**
 * @param {object} options
 *   url      接口路径
 *   params   function，返回附加查询参数（this 指向页面）
 *   map      function，把后端 list 转成页面要的展示结构
 */
function methods(options) {
  const url = options.url
  const paramsFn = options.params || function () { return {} }
  const mapFn = options.map || function (rows) { return rows }

  return {
    /** reset=true 重新从第 1 页加载 */
    fetchList(reset) {
      if (this.data.loading) {
        return Promise.resolve()
      }
      if (!reset && !this.data.hasMore) {
        return Promise.resolve()
      }
      const page = reset ? 1 : this.data.page + 1
      this.setData({ loading: true, error: '' })

      const query = Object.assign({ page: page, size: this.data.size }, paramsFn.call(this))
      return api.get(url, query, { silent: true })
        .then((res) => {
          const rows = mapFn.call(this, (res && res.list) || [])
          this.setData({
            list: reset ? rows : this.data.list.concat(rows),
            page: page,
            total: (res && res.total) || 0,
            hasMore: !!(res && res.hasMore),
            loading: false,
            loaded: true
          })
        })
        .catch((err) => {
          this.setData({ loading: false, loaded: true, error: err.message })
          if (err.statusCode === 401) {
            wx.reLaunch({ url: '/pages/login/index' })
          }
        })
    },

    /** 筛选条件变了：清空重来 */
    resetList() {
      this.setData({ list: [], page: 1, hasMore: true })
      return this.fetchList(true)
    },

    /** 到底了继续加载 */
    loadMore() {
      return this.fetchList(false)
    },

    onPullDownRefresh() {
      this.fetchList(true).then(() => wx.stopPullDownRefresh())
    },

    onReachBottom() {
      this.loadMore()
    }
  }
}

module.exports = { data: data, methods: methods }
