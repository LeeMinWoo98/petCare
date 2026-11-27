import { apiRequest } from './client'

export function listPets() {
  return apiRequest('/api/v1/pets')
}

export function getPet(petId) {
  return apiRequest(`/api/v1/pets/${petId}`)
}

export function createPet(payload) {
  return apiRequest('/api/v1/pets', {
    method: 'POST',
    body: payload,
  })
}

export function updatePet(petId, payload) {
  return apiRequest(`/api/v1/pets/${petId}`, {
    method: 'PUT',
    body: payload,
  })
}

export function deletePet(petId) {
  return apiRequest(`/api/v1/pets/${petId}`, {
    method: 'DELETE',
  })
}
