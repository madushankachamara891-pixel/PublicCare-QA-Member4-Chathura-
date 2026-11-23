import { csrfToken } from './authApi.js'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

async function request(path, options = {}, csrf = false) {
  const headers = { Accept: 'application/json', ...options.headers }
  if (csrf) headers['X-XSRF-TOKEN'] = await csrfToken()
  if (options.body && !(typeof FormData !== 'undefined' && options.body instanceof FormData)) headers['Content-Type'] = 'application/json'
  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { credentials: 'include', ...options, headers })
  } catch {
    throw new Error('Unable to reach PublicCare. Check that the backend is running and try again.')
  }
  if (response.status === 204) return null
  const payload = await response.json().catch(() => ({}))
  if (!response.ok) {
    const error = new Error(payload.error || `Request failed (${response.status}).`)
    error.status = response.status
    throw error
  }
  return payload
}

const queryString = (params) => {
  const query = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => { if (value !== '' && value != null) query.set(key, value) })
  return query.toString()
}

export const reportStatusLabel = (status) => ({ PENDING: 'Pending', IN_PROGRESS: 'In progress', RESOLVED: 'Resolved' })[status] || status

export function getPublicReports(params = {}) { return request(`/api/public/reports?${queryString(params)}`) }
export function getPublicReport(id) { return request(`/api/public/reports/${encodeURIComponent(id)}`) }
export function getMyReports(params = {}) { return request(`/api/reports/mine?${queryString(params)}`) }
export function getMyReport(id) { return request(`/api/reports/${encodeURIComponent(id)}`) }
async function getReportImage(path) {
  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      credentials: 'include',
      headers: { Accept: 'image/*' },
    })
  } catch {
    throw new Error('Unable to reach PublicCare. Check that the backend is running and try again.')
  }
  if (!response.ok) {
    const payload = await response.json().catch(() => ({}))
    const error = new Error(payload.error || `Request failed (${response.status}).`)
    error.status = response.status
    throw error
  }
  return response.blob()
}
export function getMyReportImage(id) { return getReportImage(`/api/reports/${encodeURIComponent(id)}/image`) }
export function getAdminReportImage(id) { return getReportImage(`/api/admin/reports/${encodeURIComponent(id)}/image`) }
export function createReport(data) { return request('/api/reports', { method: 'POST', body: JSON.stringify(data) }, true) }
export function uploadReportImage(id, file) {
  const body = new FormData()
  body.append('image', file)
  return request(`/api/reports/${encodeURIComponent(id)}/image`, { method: 'POST', body }, true)
}
export function updateMyReport(id, data) { return request(`/api/reports/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(data) }, true) }
export function getAdminReports(params = {}) { return request(`/api/admin/reports?${queryString(params)}`) }
export function getAdminSummary(params = {}) { return request(`/api/admin/reports/summary?${queryString(params)}`) }
export function getAdminReport(id) { return request(`/api/admin/reports/${encodeURIComponent(id)}`) }
export function deleteAdminReport(id) { return request(`/api/admin/reports/${encodeURIComponent(id)}`, { method: 'DELETE' }, true) }
export function updateReportStatus(id, data) { return request(`/api/admin/reports/${encodeURIComponent(id)}/status`, { method: 'PATCH', body: JSON.stringify(data) }, true) }
export function getMyProgressUpdates(id) { return request(`/api/reports/${encodeURIComponent(id)}/progress-updates`) }
export function getAdminProgressUpdates(id) { return request(`/api/admin/reports/${encodeURIComponent(id)}/progress-updates`) }
export function getMyProgressImage(id, updateId) { return getReportImage(`/api/reports/${encodeURIComponent(id)}/progress-updates/${encodeURIComponent(updateId)}/image`) }
export function getAdminProgressImage(id, updateId) { return getReportImage(`/api/admin/reports/${encodeURIComponent(id)}/progress-updates/${encodeURIComponent(updateId)}/image`) }
export function addAdminProgressUpdate(id, data) {
  const body = new FormData()
  body.append('status', data.status)
  body.append('note', data.note)
  if (data.image) body.append('image', data.image)
  return request(`/api/admin/reports/${encodeURIComponent(id)}/progress-updates`, { method: 'POST', body }, true)
}
export function setAdminReportPublicApproval(id, approved) {
  return request(`/api/admin/reports/${encodeURIComponent(id)}/public-approval`, { method: 'PATCH', body: JSON.stringify({ approved }) }, true)
}
export function setAdminProgressPublicApproval(id, updateId, approved) {
  return request(`/api/admin/reports/${encodeURIComponent(id)}/progress-updates/${encodeURIComponent(updateId)}/public-approval`, { method: 'PATCH', body: JSON.stringify({ approved }) }, true)
}
export function setAdminProgressNotePublicApproval(id, updateId, approved) {
  return request(`/api/admin/reports/${encodeURIComponent(id)}/progress-updates/${encodeURIComponent(updateId)}/note-public-approval`, { method: 'PATCH', body: JSON.stringify({ approved }) }, true)
}
