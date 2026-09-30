const defaultBase = 'http://127.0.0.1:8000/api/v1'
let base = import.meta.env.VITE_API_BASE || defaultBase
// #ifdef H5
if (base.startsWith('//')) base = `${window.location.protocol}${base}`
// #endif
// #ifndef H5
if (base.startsWith('//')) base = `https:${base}`
// #endif
export const auth = {
  token: () => uni.getStorageSync('rollcall-token') || '',
  save: (token) => uni.setStorageSync('rollcall-token', token),
  clear: () => uni.removeStorageSync('rollcall-token'),
}
let navigating = false
export function api(path, { method = 'GET', body } = {}) {
  return new Promise((resolve, reject) => {
    uni.request({
      url: base + path,
      method,
      data: body,
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        'X-Client': 'app',
        ...(auth.token() ? { Authorization: `Bearer ${auth.token()}` } : {}),
      },
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data)
          return
        }
        if (response.statusCode === 401 && path !== '/auth/login') {
          auth.clear()
          if (!navigating) {
            navigating = true
            uni.reLaunch({
              url: '/pages/login/login',
              complete: () => {
                navigating = false
              },
            })
          }
        }
        reject(new Error(response.data?.message || `请求失败 (${response.statusCode})`))
      },
      fail() {
        reject(new Error('无法连接服务，请检查网络和后端地址'))
      },
    })
  })
}
export async function requireUser(role) {
  if (!auth.token()) {
    uni.reLaunch({ url: '/pages/login/login' })
    return null
  }
  const user = await api('/auth/me')
  if (user.role !== 'TEACHER') {
    auth.clear()
    uni.reLaunch({ url: '/pages/login/login' })
    throw new Error('目前前台暂时仅支持老师账号登录')
  }
  if (role && user.role !== role) {
    uni.reLaunch({ url: '/pages/activities/activities' })
    return null
  }
  return user
}
export function notify(error) {
  uni.showToast({ title: error.message || String(error), icon: 'none', duration: 2500 })
}
export function confirm(title, content) {
  return new Promise((resolve) =>
    uni.showModal({
      title,
      content,
      confirmColor: '#38694c',
      success: (r) => resolve(r.confirm),
      fail: () => resolve(false),
    }),
  )
}
