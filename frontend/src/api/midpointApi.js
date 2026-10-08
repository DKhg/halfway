import api from './axios'

export function getMidpoint(code) {
  return api.get(`/rooms/${code}/midpoint`).then((response) => response.data)
}
