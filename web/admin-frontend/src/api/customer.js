import request from './request'

export function getCustomerList(params) {
  return request.get('/admin/customer/list', { params })
}

export function getCustomer(id) {
  return request.get(`/admin/customer/${id}`)
}

export function getCustomerReservations(id) {
  return request.get(`/admin/customer/${id}/reservations`)
}
