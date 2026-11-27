import { apiRequest } from './client'

export function searchMedicineCatalog(keyword = '') {
  const query = keyword.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : ''
  return apiRequest(`/api/v1/medicines/search${query}`, { skipAuth: true })
}

export function listPetMedicines(petId, date) {
  const query = date ? `?date=${encodeURIComponent(date)}` : ''
  return apiRequest(`/api/v1/pets/${petId}/medicines${query}`)
}

export function createPetMedicine(petId, payload) {
  return apiRequest(`/api/v1/pets/${petId}/medicines`, {
    method: 'POST',
    body: payload,
  })
}

export function updatePetMedicine(petId, medicineId, payload) {
  return apiRequest(`/api/v1/pets/${petId}/medicines/${medicineId}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deletePetMedicine(petId, medicineId) {
  return apiRequest(`/api/v1/pets/${petId}/medicines/${medicineId}`, {
    method: 'DELETE',
  })
}

export function recordMedicationDose(petId, medicineId, payload = null) {
  return apiRequest(`/api/v1/pets/${petId}/medicines/${medicineId}/logs`, {
    method: 'POST',
    body: payload ?? {},
  })
}

export function cancelMedicationLog(logId) {
  return apiRequest(`/api/v1/medication-logs/${logId}`, {
    method: 'DELETE',
  })
}
