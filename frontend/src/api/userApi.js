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

export function updateNickname(nickname) {
  return api.patch('/users/me/nickname', { nickname })
}

export function deleteMyAccount() {
  return api.delete('/users/me')
}
