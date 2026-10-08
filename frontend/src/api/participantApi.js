import api from './axios'

export function joinRoom(code, nickname) {
  return api.post(`/rooms/${code}/participants`, { nickname }).then((response) => response.data)
}

export function updateMyStation(code, { startStationName }) {
  return api
    .patch(`/rooms/${code}/participants/me`, { startStationName })
    .then((response) => response.data)
}
