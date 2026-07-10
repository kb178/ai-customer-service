import request from './request'

export function getConversationList(params) {
  return request.get('/admin/conversation/list', { params })
}

export function getSessionDetail(sessionId) {
  return request.get(`/admin/conversation/session/${sessionId}`)
}
