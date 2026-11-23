const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '')

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      credentials: 'include',
      ...options,
      headers: { Accept: 'application/json', ...options.headers },
    })
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

export async function getCurrentUser() {
  return request('/api/auth/me')
}

export async function csrfToken() {
  const payload = await request('/api/auth/csrf')
  return payload.token
}

export async function registerAccount(data) {
  const token = await csrfToken()
  return request('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
    body: JSON.stringify(data),
  })
}

export async function loginAccount(data) {
  const token = await csrfToken()
  return request('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-XSRF-TOKEN': token },
    body: JSON.stringify(data),
  })
}

export async function logoutAccount() {
  const token = await csrfToken()
  return request('/api/auth/logout', {
    method: 'POST',
    headers: { 'X-XSRF-TOKEN': token },
  })
}
