export const PASSWORD_REGEX =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?])\S{8,100}$/;

export const PASSWORD_RULE_MESSAGE =
  'Mật khẩu phải có 8-100 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt.';

export function isValidPassword(password: string) {
  return PASSWORD_REGEX.test(password);
}
