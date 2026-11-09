const DEFAULT_API_BASE = 'http://localhost:8080';
const API_BASE = (import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE).replace(/\/$/, '');
const TOKEN_KEY = 'printdoc.demo.access-token';

export function getToken() { return sessionStorage.getItem(TOKEN_KEY) || ''; }
export function setToken(token) { if (token) sessionStorage.setItem(TOKEN_KEY, token); else sessionStorage.removeItem(TOKEN_KEY); }
export function clearToken() { sessionStorage.removeItem(TOKEN_KEY); }

async function request(path, { method = 'GET', body, token = getToken(), headers = {} } = {}) {
  const h = { Accept: 'application/json', ...headers };
  if (body && !(body instanceof FormData)) h['Content-Type'] = 'application/json';
  if (token) h.Authorization = `Bearer ${token}`;
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, { method, headers: h, body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined, credentials: 'omit' });
  } catch {
    const error = new Error(`Could not reach the API at ${API_BASE}. Start it with "npm run api" (or npm run dev:fullstack), then retry.`);
    error.status = 0;
    throw error;
  }
  const payload = await response.json().catch(() => null);
  if (!response.ok || payload?.success === false) {
    const error = new Error(payload?.message || `Request failed (${response.status})`);
    error.status = response.status;
    error.fields = payload?.data || null;
    throw error;
  }
  return payload?.data;
}

export const api = {
  baseUrl: API_BASE,
  login: async (email, password) => {
    const data = await request('/api/v1/auth/shop/login', { method: 'POST', body: { email, password }, token: '' });
    setToken(data.accessToken);
    return data;
  },
  logout: () => clearToken(),
  register: async ({ shopName, ownerName, email, password, shopCode, phone, printerBrand }) => {
    const data = await request('/api/v1/auth/shop/register', {
      method: 'POST',
      token: '',
      body: { shopName, ownerName, email, password, ...(shopCode ? { shopCode } : {}), ...(phone ? { phone } : {}), ...(printerBrand ? { printerBrand } : {}) },
    });
    setToken(data.accessToken);
    return data;
  },
  me: () => request('/api/v1/auth/me'),
  publicShop: (shopCode) => request(`/api/v1/public/shops/${encodeURIComponent(shopCode)}`, { token: '' }),
  plans: () => request('/api/v1/public/plans', { token: '' }),
  upload: (shopCode, file) => {
    const form = new FormData(); form.append('file', file);
    return request(`/api/v1/public/shops/${encodeURIComponent(shopCode)}/uploads`, { method: 'POST', body: form, token: '' });
  },
  createJob: (shopCode, payload) => request(`/api/v1/public/shops/${encodeURIComponent(shopCode)}/jobs`, { method: 'POST', body: payload, token: '' }),
  ownerTransition: (jobId, status, note = '') => request(`/api/v1/owner/jobs/${encodeURIComponent(jobId)}/transition`, { method: 'POST', body: { status, note } }),
  publicJobStatus: (jobId) => request(`/api/v1/public/jobs/${encodeURIComponent(jobId)}/status`, { token: '' }),
  ownerJobs: (params = {}) => request(`/api/v1/owner/jobs?${new URLSearchParams(params)}`),
  setupStatus: () => request('/api/v1/owner/setup-status'),
  agentCredentials: () => request('/api/v1/owner/devices/credentials', { method: 'POST', body: {} }),
  devices: () => request('/api/v1/owner/devices'),
  approveDevice: (deviceId) => request(`/api/v1/owner/devices/${encodeURIComponent(deviceId)}/approve`, { method: 'POST', body: {} }),
  rejectDevice: (deviceId) => request(`/api/v1/owner/devices/${encodeURIComponent(deviceId)}/reject`, { method: 'POST', body: {} }),
  revokeDevice: (deviceId) => request(`/api/v1/owner/devices/${encodeURIComponent(deviceId)}/revoke`, { method: 'POST', body: {} }),
  updateProfile: (payload) => request('/api/v1/owner/profile', { method: 'PUT', body: payload }),
};
