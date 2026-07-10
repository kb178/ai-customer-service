import request from './request'

export function getCourseList(params) {
  return request.get('/admin/course/list', { params })
}

export function getCourse(id) {
  return request.get(`/admin/course/${id}`)
}

export function createCourse(data) {
  return request.post('/admin/course', data)
}

export function updateCourse(id, data) {
  return request.put(`/admin/course/${id}`, data)
}

export function deleteCourse(id) {
  return request.delete(`/admin/course/${id}`)
}

export function getCategories() {
  return request.get('/admin/course-category/list')
}
