import api from './axios'

export function getLastTrain(code, homeStationName, dayType) {
  return api
    .get(`/rooms/${code}/last-train`, { params: { homeStationName, dayType } })
    .then((response) => response.data)
}
