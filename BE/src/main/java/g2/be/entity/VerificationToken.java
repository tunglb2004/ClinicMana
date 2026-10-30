package g2.be.entity;

import jakarta.persistence.*;
import lombok.*;
import g2.be.util.ClinicTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TokenType type;

    @Column(name = "is_used")
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
    }

    public enum TokenType {
        REGISTER,
        RESET_PASSWORD,
        CHANGE_EMAIL
    }
}
