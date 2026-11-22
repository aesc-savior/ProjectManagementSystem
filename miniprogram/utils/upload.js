/**
 * 拍照 / 选图上传（PM-121）
 *
 * 链路：chooseMedia → 超过 5MB 就压缩 → uploadFile 带令牌 → 返回可访问地址。
 * 后端对应 POST /api/mp/upload（单文件 <= 5MB），落盘后返回 /api/mp/files/{name}。
 */
const config = require('./config')
const api = require('./request')

const MAX_BYTES = config.maxUploadMb * 1024 * 1024

function chooseMedia() {
  return new Promise((resolve, reject) => {
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['camera', 'album'],
      sizeType: ['compressed'],
      camera: 'back',
      success(res) {
        const file = res.tempFiles && res.tempFiles[0]
        if (file) {
          resolve(file)
        } else {
          reject(new Error('没有选择文件'))
        }
      },
      fail(e) {
        reject(new Error(e.errMsg && e.errMsg.indexOf('cancel') >= 0 ? '已取消' : '选择图片失败'))
      }
    })
  })
}

function fileSize(path) {
  return new Promise((resolve) => {
    wx.getFileSystemManager().getFileInfo({
      filePath: path,
      success(res) {
        resolve(res.size || 0)
      },
      fail() {
        resolve(0)
      }
    })
  })
}

function compress(path, quality) {
  return new Promise((resolve) => {
    wx.compressImage({
      src: path,
      quality: quality || 60,
      success(res) {
        resolve(res.tempFilePath)
      },
      fail() {
        resolve(path)
      }
    })
  })
}

/** 超过限制就逐级降质，最多压 3 次，仍超标则报错 */
function fitSize(file) {
  return fileSize(file.tempFilePath).then((size) => {
    if (size <= MAX_BYTES) {
      return file.tempFilePath
    }
    return compress(file.tempFilePath, 50)
      .then((path) => fileSize(path).then((s2) => {
        if (s2 <= MAX_BYTES) {
          return path
        }
        return compress(path, 30).then((p3) => fileSize(p3).then((s3) => {
          if (s3 <= MAX_BYTES) {
            return p3
          }
          throw new Error('图片超过 ' + config.maxUploadMb + 'MB，请重新拍摄')
        }))
      }))
  })
}

function upload(path) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: api.buildUrl('/upload'),
      filePath: path,
      name: 'file',
      header: { Authorization: 'Bearer ' + api.getToken() },
      timeout: 30000,
      success(res) {
        let body = {}
        try {
          body = JSON.parse(res.data || '{}')
        } catch (e) {
          reject(new Error('上传返回异常'))
          return
        }
        if (res.statusCode === 200 && body.code === 200) {
          const data = body.data || {}
          data.fullUrl = api.fileUrl(data.url)
          resolve(data)
          return
        }
        reject(new Error(body.message || '上传失败'))
      },
      fail() {
        reject(new Error('上传失败，检查网络或服务器地址'))
      }
    })
  })
}

/** 对外唯一入口：选一张图并上传 */
function pickAndUpload() {
  return chooseMedia()
    .then((file) => fitSize(file))
    .then((path) => upload(path))
}

module.exports = { pickAndUpload: pickAndUpload, upload: upload }
