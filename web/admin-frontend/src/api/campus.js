import request from './request'

export function getCampusList(params) {
  return request.get('/admin/campus/list', { params })
}

export function getCampus(id) {
  return request.get(`/admin/campus/${id}`)
}

export function createCampus(data) {
  return request.post('/admin/campus', data)
}

export function updateCampus(id, data) {
  return request.put(`/admin/campus/${id}`, data)
}

export function deleteCampus(id) {
  return request.delete(`/admin/campus/${id}`)
}

export function getCampusCourses(id) {
  return request.get(`/admin/campus/${id}/courses`)
}
