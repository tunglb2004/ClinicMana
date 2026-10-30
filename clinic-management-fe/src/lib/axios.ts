import axios, { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { ApiResponse, ErrorCode } from '@/types/api.types';

// 1. Lấy URL từ biến môi trường (Rule #1)
const baseURL = process.env.NEXT_PUBLIC_API_URL;

const axiosClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Flag to prevent multiple refresh token calls
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (reason?: unknown) => void;
}> = [];

const processQueue = (error: AxiosError | null, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });

  failedQueue = [];
};

// 2. Interceptor cho Request: Tự động gắn Token
axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // Lấy token từ localStorage (Zustand persist)
    if (typeof window !== 'undefined') {
      const authStorage = localStorage.getItem('auth-storage');
      if (authStorage) {
        try {
          const { state } = JSON.parse(authStorage);
          const token = state?.accessToken;
          if (token && config.headers) {
            config.headers.Authorization = `Bearer ${token}`;
          }
        } catch (error) {
          console.error('Failed to parse auth storage:', error);
        }
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 3. Interceptor cho Response: Xử lý Token hết hạn & Auto Refresh
axiosClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // Unwrap response: Extract data from ApiResponse wrapper
    // This allows services to work with data directly without checking success/message
    if (response.data && typeof response.data === 'object' && 'success' in response.data) {
      // Return the data field directly for easier consumption
      // Services can access response.data directly instead of response.data.data
      return {
        ...response,
        data: response.data.data,
        // Keep original response for metadata if needed
        _originalResponse: response.data,
      } as AxiosResponse;
    }
    return response;
  },
  async (error: AxiosError<ApiResponse>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Handle specific error codes from backend
    const errorCode = error.response?.data?.error;
    
    // Check for token expiration errors
    if (
      errorCode === ErrorCode.REFRESH_TOKEN_EXPIRED || 
      errorCode === ErrorCode.REFRESH_TOKEN_INVALID ||
      error.response?.status === 401
    ) {
      if (!originalRequest._retry) {
        if (isRefreshing) {
          // Nếu đang refresh, đợi trong queue
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject });
          })
            .then((token) => {
              if (originalRequest.headers) {
                originalRequest.headers.Authorization = `Bearer ${token}`;
              }
              return axiosClient(originalRequest);
            })
            .catch((err) => Promise.reject(err));
        }

        originalRequest._retry = true;
        isRefreshing = true;

        if (typeof window !== 'undefined') {
          const authStorage = localStorage.getItem('auth-storage');
          if (authStorage) {
            try {
              const { state } = JSON.parse(authStorage);
              const refreshToken = state?.refreshToken;

              if (refreshToken) {
                // Call refresh token API
                const response = await axios.post<ApiResponse<{ accessToken: string; refreshToken: string }>>(
                  `${baseURL}/auth/refresh-token`,
                  { refreshToken }
                );

                // Extract tokens from response.data (already unwrapped by backend)
                const tokens = response.data.data;
                if (tokens) {
                  const { accessToken: newAccessToken, refreshToken: newRefreshToken } = tokens;

                  // Update localStorage
                  const updatedState = {
                    ...state,
                    accessToken: newAccessToken,
                    refreshToken: newRefreshToken,
                  };
                  localStorage.setItem('auth-storage', JSON.stringify({ state: updatedState }));

                  // Update original request header
                  if (originalRequest.headers) {
                    originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
                  }

                  processQueue(null, newAccessToken);
                  isRefreshing = false;

                  return axiosClient(originalRequest);
                }
              }
            } catch (refreshError) {
              processQueue(refreshError as AxiosError, null);
              isRefreshing = false;

              // Clear storage and redirect to login
              localStorage.removeItem('auth-storage');
              window.location.href = '/login';
              return Promise.reject(refreshError);
            }
          }

          // No refresh token found, redirect to login
          localStorage.removeItem('auth-storage');
          window.location.href = '/login';
        }
      }
    }

    // Handle account locked error
    if (errorCode === ErrorCode.LOGIN_ACCOUNT_LOCKED) {
      // Clear storage and redirect to login with error message
      if (typeof window !== 'undefined') {
        localStorage.removeItem('auth-storage');
        window.location.href = '/login?error=account_locked';
      }
    }

    return Promise.reject(error);
  }
);

export default axiosClient;