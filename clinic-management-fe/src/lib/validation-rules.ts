export const FULL_NAME_REGEX = /^[\p{L}]+(?:[\p{L} .'-]*[\p{L}])?$/u;
export const EMAIL_REGEX = /^(?=.{6,30}$)(?!.*\.\.)[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
export const PHONE_REGEX = /^0\d{9,10}$/;
export const CCCD_REGEX = /^\d{12}$/;
export const SERVICE_CODE_REGEX = /^[A-Za-z0-9._-]{1,50}$/;

export const FULL_NAME_MESSAGE = 'Họ tên không được chứa số hoặc ký tự đặc biệt.';
export const EMAIL_MESSAGE = 'Email phải đúng định dạng, dài 6-30 ký tự và không có dấu chấm liên tiếp.';
export const PHONE_MESSAGE = 'Số điện thoại phải bắt đầu bằng 0 và có 10-11 chữ số.';
export const CCCD_MESSAGE = 'CCCD phải gồm đúng 12 chữ số.';
