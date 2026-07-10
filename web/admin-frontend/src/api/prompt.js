import request from './request'

export function getActivePrompt() {
  return request.get('/admin/prompt')
}

export function getPromptHistory() {
  return request.get('/admin/prompt/history')
}

export function updatePrompt(data) {
  return request.put('/admin/prompt', data)
}

export function rollbackPrompt(versionId) {
  return request.put(`/admin/prompt/rollback/${versionId}`)
}
