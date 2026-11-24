/**
 * 统一请求层
 *
 * 职责：拼地址、带令牌、管 loading、统一错误码、401 自动回登录页。
 * 页面只关心：成功拿 data，失败拿 err.message / err.code。
 */
const config = require('./config')
const mock = require('./mock')

const TOKEN_KEY = 'mp_token'
const USER_KEY = 'mp_user'

let loadingCount = 0

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

function getUser() {
  return wx.getStorageSync(USER_KEY) || null
}

function setToken(token, user) {
  wx.setStorageSync(TOKEN_KEY, token || '')
  if (user) {
    wx.setStorageSync(USER_KEY, user)
  }
}

function clearToken() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
}

/** 固定的 mock code：后端 mock 微信时保证 openid 稳定 */
function getLoginCode() {
  if (config.mockLoginCode) {
    return Promise.resolve(config.mockCode)
  }
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (res.code) {
          resolve(res.code)
        } else {
          reject(new Error('获取微信登录凭证失败'))
        }
      },
      fail() {
        reject(new Error('获取微信登录凭证失败'))
      }
    })
  })
}

/** 普通参数拼到 query 上，undefined / 空串自动丢掉 */
function buildUrl(path, params) {
  let url = config.apiBase() + (path.charAt(0) === '/' ? path : '/' + path)
  if (!params) {
    return url
  }
  const parts = []
  Object.keys(params).forEach((k) => {
    const v = params[k]
    if (v === undefined || v === null || v === '') {
      return
    }
    parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(v))
  })
  return parts.length ? url + (url.indexOf('?') >= 0 ? '&' : '?') + parts.join('&') : url
}

function showLoading() {
  loadingCount += 1
  if (loadingCount === 1) {
    wx.showLoading({ title: '加载中', mask: true })
  }
}

function hideLoading() {
  loadingCount = Math.max(0, loadingCount - 1)
  if (loadingCount === 0) {
    wx.hideLoading()
  }
}

function onUnauthorized() {
  clearToken()
  const app = getApp()
  if (app && app.globalData) {
    app.globalData.user = null
  }
  wx.reLaunch({ url: '/pages/login/index' })
}

/**
 * @param {string} method
 * @param {string} path
 * @param {object} data
 * @param {object} options { loading:boolean, silent:boolean }
 */
function request(method, path, data, options) {
  const opts = options || {}

  if (config.isOffline()) {
    return mock.handle(method, path, data)
  }

  if (opts.loading) {
    showLoading()
  }

  return new Promise((resolve, reject) => {
    const header = { 'content-type': 'application/json' }
    const token = getToken()
    if (token) {
      header.Authorization = 'Bearer ' + token
    }
    wx.request({
      url: buildUrl(path, method === 'GET' ? data : null),
      method: method,
      data: method === 'GET' ? {} : (data || {}),
      header: header,
      timeout: config.timeout,
      success(res) {
        const body = res.data || {}
        if (res.statusCode === 200 && body.code === 200) {
          resolve(body.data)
          return
        }
        if (res.statusCode === 401) {
          // 登录/绑定接口自身的 401 是业务失败（code 无效等），不能跳登录页，否则会打转
          if (path.indexOf('/auth/') !== 0) {
            onUnauthorized()
          }
        }
        const err = new Error(body.message || ('请求失败（' + res.statusCode + '）'))
        err.code = body.code || res.statusCode
        err.statusCode = res.statusCode
        if (!opts.silent) {
          wx.showToast({ title: err.message, icon: 'none' })
        }
        reject(err)
      },
      fail(e) {
        const err = new Error(e.errMsg && e.errMsg.indexOf('timeout') >= 0
          ? '请求超时，检查是否与电脑同一 Wi-Fi'
          : '连不上后端，请检查服务器地址')
        err.code = -1
        if (!opts.silent) {
          wx.showToast({ title: err.message, icon: 'none' })
        }
        reject(err)
      },
      complete() {
        if (opts.loading) {
          hideLoading()
        }
      }
    })
  })
}

/** 图片等需要带令牌的地址 */
function fileUrl(path) {
  if (!path) {
    return ''
  }
  if (path.indexOf('http') === 0) {
    return path
  }
  const token = getToken()
  return config.origin() + path + (path.indexOf('?') >= 0 ? '&' : '?') + 'token=' + token
}

module.exports = {
  request,
  get: (path, data, options) => request('GET', path, data, options),
  post: (path, data, options) => request('POST', path, data, options),
  getToken,
  setToken,
  clearToken,
  getUser,
  getLoginCode,
  buildUrl,
  fileUrl,
  showLoading,
  hideLoading
}
