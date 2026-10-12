import api from './axios'

export function startVoting(code, considerHome = false) {
  return api.post(`/rooms/${code}/voting`, null, { params: { considerHome } })
}

export function cancelVoting(code) {
  return api.delete(`/rooms/${code}/voting`)
}

export function getCandidates(code) {
  return api.get(`/rooms/${code}/candidates`).then((response) => response.data)
}

export function voteCandidate(code, candidateId) {
  return api.put(`/rooms/${code}/candidates/${candidateId}/vote`)
}

export function unvoteCandidate(code, candidateId) {
  return api.delete(`/rooms/${code}/candidates/${candidateId}/vote`)
}

export function confirmCandidate(code, candidateId) {
  return api.post(`/rooms/${code}/candidates/${candidateId}/confirm`)
}

export function unconfirmPlace(code) {
  return api.delete(`/rooms/${code}/confirm`)
}

export function updateFixedStation(code, stationName) {
  return api.patch(`/rooms/${code}/fixed-station`, { stationName })
}
