import { apiRequest } from './client'

export function signup(payload) {
  return apiRequest('/api/v1/auth/signup', {
    method: 'POST',
    body: payload,
    skipAuth: true,
  })
}

export function login(payload) {
  return apiRequest('/api/v1/auth/login', {
    method: 'POST',
    body: payload,
    skipAuth: true,
  })
}

export function getMyFamily() {
  return apiRequest('/api/v1/families/me')
}
