import axios from 'axios';

const apiClient = axios.create({
baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

// Public routes where a 401 is expected (unauthenticated session check)
// and must NOT trigger a redirect — doing so causes an infinite reload loop
// because AuthProvider calls getMe() on every mount, which returns 401 for
// logged-out users, which would reload the page, which remounts AuthProvider…
const PUBLIC_PATHS = ['/login', '/register', '/forgot-password'];

// Global error handling: 401 on a protected page → redirect to /login
apiClient.interceptors.response.use(
  (res) => res,
  (error) => {
    const onPublicPage = PUBLIC_PATHS.some((p) =>
      window.location.pathname.startsWith(p)
    );
    if (error.response?.status === 401 && !onPublicPage) {
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default apiClient;
