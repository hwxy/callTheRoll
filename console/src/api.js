const configuredBase = import.meta.env.VITE_API_BASE || '/api/v1'
const base = configuredBase.startsWith('//')
  ? `${window.location.protocol}${configuredBase}`
  : configuredBase
export const auth = {
  token: () => sessionStorage.getItem('rollcall-token') || '',
  save: (value) => sessionStorage.setItem('rollcall-token', value),
  clear: () => sessionStorage.removeItem('rollcall-token'),
}
export async function api(path, { method = 'GET', body, blob = false } = {}) {
  const headers = { 'X-Client': 'console' }
  if (auth.token()) headers.Authorization = `Bearer ${auth.token()}`
  if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json'
  let response
  try {
    response = await fetch(base + path, {
      method,
      headers,
      body: body ? (body instanceof FormData ? body : JSON.stringify(body)) : undefined,
      signal: AbortSignal.timeout(20000),
    })
  } catch {
    throw new Error('无法连接服务，请检查网络与后端服务')
  }
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    if (response.status === 401 && path !== '/auth/login') {
      auth.clear()
      window.dispatchEvent(new Event('session-expired'))
    }
    throw new Error(error.message || `请求失败 (${response.status})`)
  }
  if (blob) return response.blob()
  const text = await response.text()
  return text ? JSON.parse(text) : null
}
