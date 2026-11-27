const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')
const SESSION_KEY = 'petcare_session_v1'

export class ApiError extends Error {
  constructor(status, code, message, fieldErrors = null) {
    super(message || '요청 처리 중 오류가 발생했습니다.')
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

function getStoredMemberId() {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    return parsed?.memberId ?? null
  } catch {
    return null
  }
}

export async function apiRequest(path, options = {}) {
  const { method = 'GET', body, headers = {}, skipAuth = false } = options
  const requestHeaders = {
    Accept: 'application/json',
    ...headers,
  }

  if (body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json'
  }

  if (!skipAuth) {
    const memberId = getStoredMemberId()
    if (memberId != null) {
      requestHeaders['X-Member-Id'] = String(memberId)
    }
  }

  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      headers: requestHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(
      0,
      'NETWORK_ERROR',
      '서버에 연결할 수 없습니다. 백엔드 서버 실행 상태를 확인해주세요.',
    )
  }

  if (response.status === 204) {
    return null
  }

  const text = await response.text()
  let data = null
  if (text) {
    try {
      data = JSON.parse(text)
    } catch {
      data = { message: text }
    }
  }

  if (!response.ok) {
    throw new ApiError(
      response.status,
      data?.code || 'HTTP_ERROR',
      data?.message || `요청이 실패했습니다. (${response.status})`,
      data?.fieldErrors || null,
    )
  }

  return data
}
