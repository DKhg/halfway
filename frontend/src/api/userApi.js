import api from './axios'

export function createAnonymousUser() {
  return api.post('/users').then((response) => response.data)
}

export function getMe() {
  return api.get('/users/me').then((response) => response.data)
}

export function updateHomeStation(homeStationName) {
  return api.patch('/users/me/home-station', { homeStationName })
}
