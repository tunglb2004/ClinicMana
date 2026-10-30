import { AxiosError } from 'axios';
import { ApiResponse, ErrorCode, isErrorCode } from '@/types/api.types';

/**
 * Extract error message from backend response
 * ALWAYS prioritize backend message over hardcoded ones
 * 
 * Backend response structure: { success, message, data, error, timestamp }
 * 
 * @param error - The error object from catch block
 * @returns Error message from backend or generic fallback
 */
export const extractErrorMessage = (error: unknown): string => {
  // Check if it's an Axios error with response
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as AxiosError<ApiResponse>;
    
    // Priority 1: Backend message from response.data.message
    if (axiosError.response?.data?.message) {
      return axiosError.response.data.message;
    }
    
    // Priority 2: HTTP status text (rare case)
    if (axiosError.response?.statusText) {
      return axiosError.response.statusText;
    }
  }
  
  // Priority 3: JavaScript Error message
  if (error instanceof Error) {
    return error.message;
  }
  
  // Priority 4: Generic fallback (only if backend truly doesn't return anything)
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
};

/**
 * Extract error code from backend response
 * Use this for conditional logic instead of checking message strings
 * 
 * @param error - The error object from catch block
 * @returns ErrorCode enum value or null
 */
export const extractErrorCode = (error: unknown): ErrorCode | null => {
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as AxiosError<ApiResponse>;
    const errorCode = axiosError.response?.data?.error;
    
    if (errorCode && isErrorCode(errorCode)) {
      return errorCode;
    }
  }
  
  return null;
};

/**
 * Check if error matches a specific error code
 * 
 * @param error - The error object from catch block
 * @param code - The ErrorCode to check against
 * @returns true if error matches the code, false otherwise
 */
export const isErrorCodeMatch = (error: unknown, code: ErrorCode): boolean => {
  return extractErrorCode(error) === code;
};

/**
 * Extract field-level validation errors from backend
 * Used for 400 Bad Request with field errors in data property
 * 
 * Backend response structure: { success, message, data: { field1: 'error1', field2: 'error2' }, error, timestamp }
 * 
 * @param error - The error object from catch block
 * @returns Object mapping field names to error messages
 */
export const extractFieldErrors = (error: unknown): Record<string, string> => {
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as AxiosError<ApiResponse>;
    
    // Check for 400 status with data object containing field errors
    if (axiosError.response?.status === 400) {
      const data = axiosError.response.data?.data;
      
      if (data && typeof data === 'object') {
        return data as Record<string, string>;
      }
    }
  }
  
  return {};
};

/**
 * Check if error contains field-level validation errors
 * 
 * @param error - The error object from catch block
 * @returns true if error has field errors, false otherwise
 */
export const hasFieldErrors = (error: unknown): boolean => {
  const fieldErrors = extractFieldErrors(error);
  return Object.keys(fieldErrors).length > 0;
};

/**
 * Extract success message from backend response
 * Used for ApiResponse type
 * 
 * @param response - Response object with message property
 * @returns Success message from backend
 */
export const extractSuccessMessage = (response: { message?: string } | ApiResponse): string => {
  return response.message || 'Thao tác thành công';
};

/**
 * Check if response is successful
 * Backend returns: { success, message, data, error, timestamp }
 * 
 * @param response - Response object with success property
 * @returns true if operation was successful, false otherwise
 */
export const isResponseSuccess = (response: ApiResponse): boolean => {
  return response.success === true;
};
