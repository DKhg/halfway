import api from './axios'

export function createRoom({ name, mode, fairnessType, fixedStationName, includesFood, includesCafe, meetingDate }) {
  return api.post('/rooms', { name, mode, fairnessType, fixedStationName, includesFood, includesCafe, meetingDate })
    .then((response) => response.data)
}

export function getRoom(code) {
  return api.get(`/rooms/${code}`).then((response) => response.data)
}

export function getMyRooms() {
  return api.get('/rooms/mine').then((response) => response.data)
}

export function deleteRoom(code) {
  return api.delete(`/rooms/${code}`)
}

export function updateRoomFairness(code, fairnessType) {
  return api.patch(`/rooms/${code}`, { fairnessType }).then((response) => response.data)
}

export function updateRoomMeetingDate(code, meetingDate) {
  return api.patch(`/rooms/${code}/meeting-date`, { meetingDate }).then((response) => response.data)
}
