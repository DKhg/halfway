import api from './axios'

export function startSettlement(code) {
  return api.post(`/rooms/${code}/settlement`)
}

export function getSettlement(code) {
  return api.get(`/rooms/${code}/settlement`).then((response) => response.data)
}

export function cancelSettlement(code) {
  return api.delete(`/rooms/${code}/settlement`)
}

export function updateSettlementAccount(code, bank, accountNumber) {
  return api.patch(`/rooms/${code}/settlement/account`, { bank, accountNumber })
}

export function addSettlementItem(code, { name, amount, participantIds }) {
  return api.post(`/rooms/${code}/settlement/items`, { name, amount, participantIds })
}

export function updateSettlementItem(code, itemId, { name, amount, participantIds }) {
  return api.patch(`/rooms/${code}/settlement/items/${itemId}`, { name, amount, participantIds })
}

export function deleteSettlementItem(code, itemId) {
  return api.delete(`/rooms/${code}/settlement/items/${itemId}`)
}
