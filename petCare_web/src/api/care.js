import { apiRequest } from './client'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

export function getDailyCareLogs(petId, date) {
  const query = date ? `?date=${encodeURIComponent(date)}` : ''
  return apiRequest(`/api/v1/pets/${petId}/care-logs${query}`)
}

export function createCareLog(petId, payload) {
  return apiRequest(`/api/v1/pets/${petId}/care-logs`, {
    method: 'POST',
    body: payload,
  })
}

export function deleteCareLog(logId) {
  return apiRequest(`/api/v1/care-logs/${logId}`, {
    method: 'DELETE',
  })
}

export function openFamilyEventStream(memberId) {
  const url = `${API_BASE_URL}/api/v1/families/me/events?memberId=${encodeURIComponent(memberId)}`
  return new EventSource(url)
}
