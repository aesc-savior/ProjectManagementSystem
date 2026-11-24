/** 提示与确认框的小封装，避免每页重复写 showModal 的 success 判断 */

function toast(title) {
  wx.showToast({ title: title || '', icon: 'none', duration: 2000 })
}

function success(title) {
  wx.showToast({ title: title || '操作成功', icon: 'success', duration: 1500 })
}

/**
 * 二次确认，返回 Promise<boolean>
 * @param {string} content
 * @param {object} opts { title, confirmText, danger }
 */
function confirm(content, opts) {
  const o = opts || {}
  return new Promise((resolve) => {
    wx.showModal({
      title: o.title || '确认操作',
      content: content,
      confirmText: o.confirmText || '确定',
      cancelText: '取消',
      confirmColor: o.danger === false ? '#2f6fed' : '#dc2626',
      success(res) {
        resolve(!!res.confirm)
      },
      fail() {
        resolve(false)
      }
    })
  })
}

/** 输入框，返回 Promise<string|null> */
function prompt(title, placeholder, value) {
  return new Promise((resolve) => {
    wx.showModal({
      title: title,
      editable: true,
      placeholderText: placeholder || '',
      content: value || '',
      success(res) {
        resolve(res.confirm ? (res.content || '') : null)
      },
      fail() {
        resolve(null)
      }
    })
  })
}

module.exports = { toast: toast, success: success, confirm: confirm, prompt: prompt }
