/**
 * 小程序端配置
 *
 * ── 手机直连（当前方案）─────────────────────────────
 * 1. 电脑和手机连同一个 Wi-Fi；
 * 2. 在电脑上查局域网 IP（Windows: ipconfig / macOS: ifconfig | grep inet），
 *    把下面的 192.168.1.100 换成那个 IP，端口保持 Tomcat 的 9999；
 * 3. 微信开发者工具点「预览」，扫码后**先点右上角 ... → 开启调试**，
 *    真机调试模式下微信不校验 https 域名，http 也能用；
 * 4. 连不上先排查：Windows 防火墙是否放行 9999、Tomcat 是否监听 0.0.0.0。
 *
 * 运行时不改代码也能换地址：「我的 → 服务器设置」会写进本地缓存，优先级高于这里。
 */
const config = {
  // 后端接口根地址（注意带上下文路径 /pm）
  baseUrl: 'http://192.168.1.100:9999/pm/api/mp',

  // true = 完全离线（走 utils/mock.js 假数据），用于没开后端时演示界面
  offline: false,

  // 后端没配 -Dpm.wx.appid/secret 时走 mock 微信：openid = 'mock_' + code。
  // 这种情况下 code 必须固定，否则每次登录 openid 都变，永远绑不上。
  // 拿到 AppID 后把这里改成 false，就会走真 wx.login。
  mockLoginCode: true,
  mockCode: 'dev-openid-001',

  timeout: 15000,
  pageSize: 10,
  maxUploadMb: 5
}

const BASE_KEY = 'mp_base_url'
const OFFLINE_KEY = 'mp_offline'

/** 实际使用的接口根地址：本地缓存优先（服务器设置页写这个键） */
function apiBase() {
  return (wx.getStorageSync(BASE_KEY) || config.baseUrl).replace(/\/+$/, '')
}

/** 是否离线：设置页可以运行时切换，没切换过就用 config.offline */
function isOffline() {
  const v = wx.getStorageSync(OFFLINE_KEY)
  return v === '' || v === null || v === undefined ? !!config.offline : !!v
}

function setOffline(v) {
  wx.setStorageSync(OFFLINE_KEY, !!v)
}

/** http://ip:port/pm（去掉 /api/mp），拼图片地址用 */
function origin() {
  return apiBase().replace(/\/api\/mp$/, '')
}

function setBase(url) {
  if (url) {
    wx.setStorageSync(BASE_KEY, url.replace(/\/+$/, ''))
  } else {
    wx.removeStorageSync(BASE_KEY)
  }
}

module.exports = {
  baseUrl: config.baseUrl,
  offline: config.offline,
  mockLoginCode: config.mockLoginCode,
  mockCode: config.mockCode,
  timeout: config.timeout,
  pageSize: config.pageSize,
  maxUploadMb: config.maxUploadMb,
  apiBase: apiBase,
  origin: origin,
  setBase: setBase,
  isOffline: isOffline,
  setOffline: setOffline,
  BASE_KEY: BASE_KEY,
  OFFLINE_KEY: OFFLINE_KEY
}
