import api from './axios'

export function createAnonymousUser() {
  return api.post('/users').then((response) => response.data)
}
