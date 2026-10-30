package g2.be.service;

import g2.be.exception.AppException;
import g2.be.exception.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;
import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    // Không inject JavaMailSender từ container nữa vì ta sẽ tự tạo instance cho mỗi
    // lần gửi (để set token)
    // Hoặc nếu muốn dùng bean thì phải cẩn thận concurrency.
    // Tốt nhất là dùng 1 bean cha, nhưng ở đây để đơn giản và an toàn thread, ta
    // tạo mới hoặc cấu hình lại 1 instance riêng.
    // Tuy nhiên, để tận dụng DI, ta có thể inject JavaMailSenderImpl gốc để lấy
    // config host/port, rồi clone ra.
    private final JavaMailSender defaultMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.refresh.token}")
    private String refreshToken;

    // URL lấy token của Google
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    /**
     * Lấy Access Token mới từ Refresh Token
     */
    private String getAccessToken() {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", clientId);
            map.add("client_secret", clientSecret);
            map.add("refresh_token", refreshToken);
            map.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("access_token");
            }
        } catch (Exception e) {
            log.error("Failed to refresh access token", e);
        }
        throw new AppException(ErrorCode.GOOGLE_TOKEN_FAILED);
    }

    /**
     * Tạo Sender mới với Access Token
     */
    private JavaMailSender createAuthenticatedSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        // Copy cấu hình từ default sender (được inject từ properties)
        if (defaultMailSender instanceof JavaMailSenderImpl defaultImpl) {
            sender.setHost(defaultImpl.getHost());
            sender.setPort(defaultImpl.getPort());
            sender.setJavaMailProperties(defaultImpl.getJavaMailProperties());
        }

        // Cấu hình auth
        sender.setUsername(fromEmail);
        sender.setPassword(getAccessToken()); // Password chính là Access Token

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth.mechanisms", "XOAUTH2"); // Quan trọng: dùng XOAUTH2

        return sender;
    }

    /**
     * Gửi email đơn giản (text)
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            JavaMailSender mailSender = createAuthenticatedSender();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /**
     * Gửi email HTML (đẹp hơn)
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            JavaMailSender mailSender = createAuthenticatedSender();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            try {
                helper.setFrom(fromEmail, "Phòng Khám FBT");
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(fromEmail);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /**
     * Gửi OTP cho đăng ký
     */
    @Async
    public void sendOtpEmail(String to, String otpCode, String purpose) {
        String subject = getOtpSubject(purpose);
        String htmlContent = buildOtpEmailTemplate(otpCode, purpose);
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Gửi email thông tin tài khoản mới cho bệnh nhân
     */
    @Async
    public void sendAccountCreationEmail(String to, String fullName, String username, String password) {
        String subject = "Tài khoản của bạn đã được tạo - Phòng Khám Đa Khoa";
        String htmlContent = buildAccountCreationEmailTemplate(fullName, username, password);
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Gửi email thông báo hóa đơn điện tử đã phát hành thành công.
     */
    @Async
    public void sendReceiptIssuedEmail(String to,
                                       String fullName,
                                       String invoiceId,
                                       String eInvoiceCode,
                                       String eInvoiceUrl,
                                       LocalDateTime receiptIssuedAt,
                                       BigDecimal customerPaid) {
        String subject = "Hóa đơn điện tử của bạn đã được phát hành";
        String htmlContent = buildReceiptIssuedEmailTemplate(
                fullName,
                invoiceId,
                eInvoiceCode,
                eInvoiceUrl,
                receiptIssuedAt,
                customerPaid
        );
        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * Lấy subject dựa vào mục đích
     */
    private String getOtpSubject(String purpose) {
        return switch (purpose.toUpperCase()) {
            case "REGISTER" -> "Xác thực tài khoản - Mã OTP đăng ký";
            case "RESET_PASSWORD" -> "Đặt lại mật khẩu - Mã OTP xác thực";
            default -> "Mã xác thực OTP";
        };
    }

    private String buildReceiptIssuedEmailTemplate(String fullName,
                                                   String invoiceId,
                                                   String eInvoiceCode,
                                                   String eInvoiceUrl,
                                                   LocalDateTime receiptIssuedAt,
                                                   BigDecimal customerPaid) {
        String displayName = fullName != null && !fullName.isBlank() ? fullName : "Quý khách";
        String issuedAtText = receiptIssuedAt != null
                ? receiptIssuedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "N/A";
        String amountText = customerPaid != null ? customerPaid.toPlainString() + " VND" : "N/A";

        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f4f4f4;
                            margin: 0;
                            padding: 0;
                        }
                        .email-container {
                            max-width: 640px;
                            margin: 40px auto;
                            background-color: #ffffff;
                            border-radius: 8px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #0f766e 0%%, #115e59 100%%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 24px;
                            font-weight: 600;
                        }
                        .content {
                            padding: 36px 30px;
                        }
                        .content p {
                            color: #444444;
                            line-height: 1.6;
                            margin-bottom: 18px;
                        }
                        .info-box {
                            background-color: #f8fafc;
                            border: 1px solid #dbeafe;
                            border-radius: 8px;
                            padding: 22px;
                            margin: 24px 0;
                        }
                        .info-row {
                            display: flex;
                            justify-content: space-between;
                            gap: 16px;
                            padding: 10px 0;
                            border-bottom: 1px solid #e5e7eb;
                        }
                        .info-row:last-child {
                            border-bottom: none;
                        }
                        .label {
                            color: #6b7280;
                            font-size: 14px;
                        }
                        .value {
                            color: #111827;
                            font-size: 15px;
                            font-weight: 600;
                            text-align: right;
                        }
                        .cta {
                            display: inline-block;
                            margin-top: 10px;
                            padding: 14px 22px;
                            background-color: #0f766e;
                            color: #ffffff !important;
                            text-decoration: none;
                            border-radius: 6px;
                            font-weight: 600;
                        }
                        .note {
                            margin-top: 24px;
                            color: #6b7280;
                            font-size: 13px;
                        }
                        .footer {
                            background-color: #f8f9fa;
                            padding: 20px;
                            text-align: center;
                            color: #888888;
                            font-size: 12px;
                            border-top: 1px solid #e0e0e0;
                        }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="header">
                            <h1>Hóa đơn điện tử đã phát hành</h1>
                        </div>
                        <div class="content">
                            <p>Xin chào <strong>%s</strong>,</p>
                            <p>Hóa đơn điện tử cho lần thanh toán của bạn đã được phát hành thành công. Bạn có thể truy cập ngay bằng nút bên dưới.</p>

                            <div class="info-box">
                                <div class="info-row">
                                    <span class="label">Mã hóa đơn</span>
                                    <span class="value">%s</span>
                                </div>
                                <div class="info-row">
                                    <span class="label">Mã hóa đơn điện tử</span>
                                    <span class="value">%s</span>
                                </div>
                                <div class="info-row">
                                    <span class="label">Thời gian phát hành</span>
                                    <span class="value">%s</span>
                                </div>
                                <div class="info-row">
                                    <span class="label">Số tiền khách thanh toán</span>
                                    <span class="value">%s</span>
                                </div>
                            </div>

                            <a class="cta" href="%s" target="_blank" rel="noreferrer">Mở hóa đơn điện tử</a>

                            <p class="note">Nếu bạn không thực hiện giao dịch này hoặc cần hỗ trợ thêm, vui lòng liên hệ phòng khám để được kiểm tra.</p>
                        </div>
                        <div class="footer">
                            <p><strong>Phòng Khám Đa Khoa</strong></p>
                            <p>Email: support@clinic.com | Hotline: 1900-xxxx</p>
                            <p style="margin-top: 15px;">© 2026 Clinic Management System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """,
                displayName,
                invoiceId != null ? invoiceId : "N/A",
                eInvoiceCode != null ? eInvoiceCode : "N/A",
                issuedAtText,
                amountText,
                eInvoiceUrl != null ? eInvoiceUrl : "#"
        );
    }

    /**
     * Template HTML đẹp cho email OTP
     */
    private String buildOtpEmailTemplate(String otpCode, String purpose) {
        String title = purpose.equalsIgnoreCase("REGISTER")
                ? "Xác thực tài khoản"
                : "Đặt lại mật khẩu";

        String description = purpose.equalsIgnoreCase("REGISTER")
                ? "Cảm ơn bạn đã đăng ký tài khoản! Vui lòng sử dụng mã OTP dưới đây để hoàn tất đăng ký:"
                : "Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng sử dụng mã OTP dưới đây:";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f4f4f4;
                            margin: 0;
                            padding: 0;
                        }
                        .email-container {
                            max-width: 600px;
                            margin: 40px auto;
                            background-color: #ffffff;
                            border-radius: 8px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 24px;
                            font-weight: 600;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .content p {
                            color: #555555;
                            line-height: 1.6;
                            margin-bottom: 20px;
                        }
                        .otp-box {
                            background-color: #f8f9fa;
                            border: 2px dashed #667eea;
                            border-radius: 8px;
                            padding: 20px;
                            text-align: center;
                            margin: 30px 0;
                        }
                        .otp-code {
                            font-size: 36px;
                            font-weight: bold;
                            color: #667eea;
                            letter-spacing: 8px;
                            margin: 10px 0;
                        }
                        .otp-label {
                            color: #888888;
                            font-size: 14px;
                            margin-bottom: 10px;
                        }
                        .warning {
                            background-color: #fff3cd;
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            color: #856404;
                            font-size: 14px;
                        }
                        .footer {
                            background-color: #f8f9fa;
                            padding: 20px;
                            text-align: center;
                            color: #888888;
                            font-size: 12px;
                            border-top: 1px solid #e0e0e0;
                        }
                        .footer p {
                            margin: 5px 0;
                        }
                        .security-tips {
                            background-color: #e8f4f8;
                            border-left: 4px solid #17a2b8;
                            padding: 15px;
                            margin: 20px 0;
                            font-size: 14px;
                            color: #0c5460;
                        }
                        .security-tips ul {
                            margin: 10px 0;
                            padding-left: 20px;
                        }
                        .security-tips li {
                            margin: 5px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="header">
                            <h1>🏥 Phòng Khám Đa Khoa</h1>
                        </div>
                        <div class="content">
                            <h2 style="color: #333333; margin-top: 0;">""" + title + """
                </h2>
                            <p>Xin chào,</p>
                            <p>""" + description + """
                </p>

                            <div class="otp-box">
                                <div class="otp-label">MÃ XÁC THỰC CỦA BẠN</div>
                                <div class="otp-code">""" + otpCode
                + """
                        </div>
                                        <p style="margin: 10px 0 0 0; color: #888; font-size: 14px;">
                                            Mã có hiệu lực trong <strong>5 phút</strong>
                                        </p>
                                    </div>

                                    <div class="warning">
                                        <strong>⚠️ Lưu ý:</strong> Mã OTP này chỉ có hiệu lực trong 5 phút và chỉ sử dụng được một lần.
                                    </div>

                                    <div class="security-tips">
                                        <strong>🔐 Bảo mật thông tin:</strong>
                                        <ul>
                                            <li>Không chia sẻ mã OTP này với bất kỳ ai</li>
                                            <li>Nhân viên của chúng tôi sẽ không bao giờ yêu cầu mã OTP qua điện thoại</li>
                                            <li>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email</li>
                                        </ul>
                                    </div>

                                    <p style="margin-top: 30px; color: #888888;">
                                        Nếu bạn không yêu cầu mã này, vui lòng bỏ qua email này hoặc liên hệ với chúng tôi ngay.
                                    </p>
                                </div>
                                <div class="footer">
                                    <p><strong>Phòng Khám Đa Khoa</strong></p>
                                    <p>Email: support@clinic.com | Hotline: 1900-xxxx</p>
                                    <p style="margin-top: 15px;">© 2026 Clinic Management System. All rights reserved.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """;
    }

    /**
     * Template HTML cho email tạo tài khoản mới
     */
    private String buildAccountCreationEmailTemplate(String fullName, String username, String password) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            background-color: #f4f4f4;
                            margin: 0;
                            padding: 0;
                        }
                        .email-container {
                            max-width: 600px;
                            margin: 40px auto;
                            background-color: #ffffff;
                            border-radius: 8px;
                            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                            overflow: hidden;
                        }
                        .header {
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            padding: 30px 20px;
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 24px;
                            font-weight: 600;
                        }
                        .content {
                            padding: 40px 30px;
                        }
                        .content p {
                            color: #555555;
                            line-height: 1.6;
                            margin-bottom: 20px;
                        }
                        .credentials-box {
                            background-color: #f8f9fa;
                            border: 2px solid #667eea;
                            border-radius: 8px;
                            padding: 25px;
                            margin: 30px 0;
                        }
                        .credential-row {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            padding: 15px 0;
                            border-bottom: 1px solid #e0e0e0;
                        }
                        .credential-row:last-child {
                            border-bottom: none;
                        }
                        .credential-label {
                            color: #888888;
                            font-size: 14px;
                            font-weight: 500;
                        }
                        .credential-value {
                            font-size: 18px;
                            font-weight: bold;
                            color: #667eea;
                            font-family: 'Courier New', monospace;
                        }
                        .warning {
                            background-color: #fff3cd;
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            color: #856404;
                            font-size: 14px;
                        }
                        .info-box {
                            background-color: #e8f4f8;
                            border-left: 4px solid #17a2b8;
                            padding: 15px;
                            margin: 20px 0;
                            font-size: 14px;
                            color: #0c5460;
                        }
                        .info-box ul {
                            margin: 10px 0;
                            padding-left: 20px;
                        }
                        .info-box li {
                            margin: 5px 0;
                        }
                        .button {
                            display: inline-block;
                            background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                            color: white;
                            padding: 12px 30px;
                            text-decoration: none;
                            border-radius: 5px;
                            margin: 20px 0;
                            font-weight: 600;
                        }
                        .footer {
                            background-color: #f8f9fa;
                            padding: 20px;
                            text-align: center;
                            color: #888888;
                            font-size: 12px;
                            border-top: 1px solid #e0e0e0;
                        }
                        .footer p {
                            margin: 5px 0;
                        }
                    </style>
                </head>
                <body>
                    <div class="email-container">
                        <div class="header">
                            <h1>🏥 Phòng Khám Đa Khoa</h1>
                        </div>
                        <div class="content">
                            <h2 style="color: #333333; margin-top: 0;">Chào mừng bạn đến với hệ thống!</h2>
                            <p>Xin chào <strong>%s</strong></p>
                            <p>Tài khoản của bạn đã được tạo thành công tại Phòng Khám Đa Khoa. Dưới đây là thông tin đăng nhập của bạn:</p>

                            <div class="credentials-box">
                                <div class="credential-row">
                                    <span class="credential-label">👤 Tên đăng nhập:</span>
                                    <span class="credential-value">%s</span>
                                </div>
                                <div class="credential-row">
                                    <span class="credential-label">🔑 Mật khẩu tạm thời:</span>
                                    <span class="credential-value">%s</span>
                                </div>
                            </div>

                            <div class="warning">
                                <strong>⚠️ Quan trọng:</strong> Đây là mật khẩu tạm thời. Vui lòng đổi mật khẩu ngay sau lần đăng nhập đầu tiên để bảo mật tài khoản.
                            </div>

                            <div class="info-box">
                                <strong>📝 Hướng dẫn đăng nhập:</strong>
                                <ul>
                                    <li>Truy cập trang đăng nhập của hệ thống</li>
                                    <li>Nhập tên đăng nhập và mật khẩu tạm thời</li>
                                    <li>Hệ thống sẽ yêu cầu bạn đổi mật khẩu mới</li>
                                    <li>Chọn mật khẩu mới có độ dài tối thiểu 8 ký tự</li>
                                </ul>
                            </div>

                            <div style="text-align: center;">
                                <a href="http://localhost:3000/login" class="button">Đăng nhập ngay</a>
                            </div>

                            <div class="info-box" style="margin-top: 30px;">
                                <strong>🔐 Bảo mật tài khoản:</strong>
                                <ul>
                                    <li>Không chia sẻ thông tin đăng nhập với bất kỳ ai</li>
                                    <li>Sử dụng mật khẩu mạnh kết hợp chữ, số và ký tự đặc biệt</li>
                                    <li>Đăng xuất sau mỗi phiên làm việc</li>
                                </ul>
                            </div>

                            <p style="margin-top: 30px; color: #888888;">
                                Nếu bạn không yêu cầu tạo tài khoản này, vui lòng liên hệ với chúng tôi ngay lập tức.
                            </p>
                        </div>
                        <div class="footer">
                            <p><strong>Phòng Khám Đa Khoa</strong></p>
                            <p>Email: support@clinic.com | Hotline: 1900-xxxx</p>
                            <p style="margin-top: 15px;">© 2026 Clinic Management System. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, fullName, username, password);
    }
}
