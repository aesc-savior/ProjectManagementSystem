/**
 * 登录态管理
 *
 * 令牌存在本地（7 天有效），每次进 tabBar 页面时由 ensureLogin 静默校验一次：
 * 过期或账号被停用，后端返回 401，这里统一跳回登录页。
 */
const api = require('./request')

function isLogged() {
  return !!api.getToken()
}

function currentUser() {
  return api.getUser()
}

function redirectToLogin() {
  wx.reLaunch({ url: '/pages/login/index' })
}

function setUser(user) {
  const app = getApp()
  if (app && app.globalData) {
    app.globalData.user = user
  }
  api.setToken(api.getToken(), user)
}

/**
 * 保证已登录，返回 Promise<user>
 * 页面 onShow 里调用，失败会自己跳登录页，页面不用再处理。
 */
function ensureLogin() {
  if (!isLogged()) {
    redirectToLogin()
    return Promise.reject(new Error('未登录'))
  }
  return api.get('/me', {}, { silent: true })
    .then((data) => {
      const user = data && data.user
      if (user) {
        setUser(user)
      }
      return user
    })
    .catch((err) => {
      if (err.statusCode === 401) {
        redirectToLogin()
      }
      throw err
    })
}

/** 账号密码 + code 绑定并登录 */
function bind(loginId, password) {
  return api.getLoginCode()
    .then((code) => api.post('/auth/bind', {
      code: code,
      loginId: loginId,
      password: password
    }, { loading: true }))
    .then((data) => {
      api.setToken(data.token, data.user)
      setUser(data.user)
      return data.user
    })
}

/** 已绑定过：静默登录 */
function login() {
  return api.getLoginCode()
    .then((code) => api.post('/auth/login', { code: code }, { silent: true }))
    .then((data) => {
      api.setToken(data.token, data.user)
      setUser(data.user)
      return data.user
    })
}

function logout() {
  return api.post('/auth/logout', {}, { silent: true })
    .catch(() => null)
    .then(() => {
      api.clearToken()
      const app = getApp()
      if (app && app.globalData) {
        app.globalData.user = null
      }
      redirectToLogin()
    })
}

module.exports = {
  isLogged: isLogged,
  currentUser: currentUser,
  ensureLogin: ensureLogin,
  setUser: setUser,
  bind: bind,
  login: login,
  logout: logout,
  redirectToLogin: redirectToLogin
}
