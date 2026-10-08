import api from './axios'

export function getPlaces(code, category, subCategory, customOnly) {
  return api.get(`/rooms/${code}/places`, { params: { category, subCategory, customOnly } }).then((response) => response.data)
}

export function votePlace(code, category, placeId) {
  return api.put(`/rooms/${code}/places/vote`, null, { params: { category, placeId } })
}

export function unvotePlace(code, category, placeId) {
  return api.delete(`/rooms/${code}/places/vote`, { params: { category, placeId } })
}

export function confirmPlace(code, category, placeName) {
  return api.patch(`/rooms/${code}/places/confirm`, { category, placeName })
}

export function clearPlace(code, category) {
  return api.delete(`/rooms/${code}/places/confirm`, { params: { category } })
}

export function searchCustomPlaces(code, category, keyword) {
  return api.get(`/rooms/${code}/places/search`, { params: { category, keyword } }).then((response) => response.data)
}

export function getPoolSuggestions(code, category) {
  return api.get(`/rooms/${code}/places/pool-suggestions`, { params: { category } }).then((response) => response.data)
}

export function addCustomPlace(code, place) {
  return api.post(`/rooms/${code}/places/custom`, place)
}

export function deleteCustomPlace(code, category, placeId) {
  return api.delete(`/rooms/${code}/places/custom`, { params: { category, placeId } })
}
