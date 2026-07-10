import request from './request'

export function getLeaveMessageList(params) {
  return request.get('/admin/leave-message/list', { params })
}

export function getLeaveMessage(id) {
  return request.get(`/admin/leave-message/${id}`)
}

export function handleLeaveMessage(id, data) {
  return request.put(`/admin/leave-message/${id}/handle`, data)
}
