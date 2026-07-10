import request from './request'

export function getReservationList(params) {
  return request.get('/admin/reservation/list', { params })
}

export function getReservation(id) {
  return request.get(`/admin/reservation/${id}`)
}

export function confirmReservation(id) {
  return request.put(`/admin/reservation/${id}/confirm`)
}

export function cancelReservation(id) {
  return request.put(`/admin/reservation/${id}/cancel`)
}

export function completeReservation(id) {
  return request.put(`/admin/reservation/${id}/complete`)
}
