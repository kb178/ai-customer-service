import request from './request'

export function getFaqList(params) {
  return request.get('/admin/faq/list', { params })
}

export function getFaq(id) {
  return request.get(`/admin/faq/${id}`)
}

export function createFaq(data) {
  return request.post('/admin/faq', data)
}

export function updateFaq(id, data) {
  return request.put(`/admin/faq/${id}`, data)
}

export function deleteFaq(id) {
  return request.delete(`/admin/faq/${id}`)
}
