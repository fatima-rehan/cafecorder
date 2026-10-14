const BASE = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080') + '/api'
const TOKEN_KEY = 'cafecorder-token'

// The login token lives in memory, with a copy in localStorage so a refresh keeps you signed in.
// localStorage can throw (private mode, blocked storage), so every access is wrapped.
let token = null
try {
  token = localStorage.getItem(TOKEN_KEY)
} catch {
  // no storage: you'll just need to log in again after a refresh
}

export function setToken(next) {
  token = next
  try {
    if (next) localStorage.setItem(TOKEN_KEY, next)
    else localStorage.removeItem(TOKEN_KEY)
  } catch {
    // same as above
  }
}

export const hasToken = () => Boolean(token)

// App registers this: a 401 on a request that carried a token means it expired or the account is gone
let onUnauthorized = () => {}
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

// open: true for public endpoints, which never need the token (and shouldn't log you out over a stale one)
async function request(path, { method = 'GET', body, open = false } = {}) {
  const sentToken = open ? null : token
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (sentToken) headers.Authorization = `Bearer ${sentToken}`

  let res
  try {
    res = await fetch(BASE + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) })
  } catch {
    const error = new Error("Can't reach CafeCorder right now. Try again in a moment.")
    error.status = 0
    error.fields = {}
    throw error
  }

  if (res.status === 204) return null
  const data = await res.json().catch(() => null)
  if (!res.ok) {
    // the backend sends ProblemDetail JSON: { detail, errors: { field: message } }
    const error = new Error(data?.detail ?? `Request failed (${res.status})`)
    error.status = res.status
    error.fields = data?.errors ?? {}
    if (res.status === 401 && sentToken) onUnauthorized()
    throw error
  }
  return data
}

export const api = {
  auth: {
    register: (data) => request('/auth/register', { method: 'POST', body: data }),
    login: (data) => request('/auth/login', { method: 'POST', body: data }),
    me: () => request('/auth/me'),
  },
  visits: {
    list: (neighbourhood, sort) => {
      const params = new URLSearchParams({ sort })
      if (neighbourhood) params.set('neighbourhood', neighbourhood)
      return request(`/visits?${params}`)
    },
    stats: () => request('/visits/stats'),
    neighbourhoods: () => request('/visits/neighbourhoods'),
    create: (visit) => request('/visits', { method: 'POST', body: visit }),
    update: (id, visit) => request(`/visits/${id}`, { method: 'PUT', body: visit }),
    remove: (id) => request(`/visits/${id}`, { method: 'DELETE' }),
  },
  feed: {
    photos: (sort, page = 0) => request(`/feed/photos?sort=${sort}&page=${page}`, { open: true }),
    topCafes: () => request('/feed/top-cafes', { open: true }),
    map: () => request('/feed/map', { open: true }),
  },
}
