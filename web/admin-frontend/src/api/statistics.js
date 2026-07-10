import request from './request'

export function getOverview() {
  return request.get('/admin/statistics/overview')
}

export function getReservationStatus() {
  return request.get('/admin/statistics/reservation-status')
}

export function getTopCourses() {
  return request.get('/admin/statistics/top-courses')
}

export function getTopCampuses() {
  return request.get('/admin/statistics/top-campuses')
}

export function getConversion() {
  return request.get('/admin/statistics/conversion')
}
