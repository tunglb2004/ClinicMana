package g2.be.entity;

import g2.be.util.ClinicTime;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 20, unique = true)
    private String cccd;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "avatar_url")
    private String avatarUrl;

    /**
     * Mã nhân viên - tự động sinh khi Admin tạo tài khoản.
     * Format: NV-XXXXXXXX (8 ký tự hex viết hoa)
     * Dùng để định danh nhân viên trong hệ thống.
     * Chỉ nhân viên (Doctor, Nurse,...) mới có. Patient không có.
     */
    @Column(name = "staff_id", unique = true, length = 20)
    private String staffId;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    /**
     * Cờ yêu cầu đổi mật khẩu lần đầu đăng nhập.
     * - true: User phải đổi mật khẩu ngay sau khi login
     * - false: User có thể sử dụng bình thường
     * 
     * Khi Admin tạo tài khoản, mật khẩu random được gửi qua email,
     * mustChangePassword = true. Sau khi đổi MK thành công, set = false.
     */
    @Builder.Default
    @Column(name = "must_change_password")
    private Boolean mustChangePassword = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Doctor doctor;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Patient patient;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Nurse nurse;

    @PrePersist
    protected void onCreate() {
        createdAt = ClinicTime.now();
        updatedAt = ClinicTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ClinicTime.now();
    }

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
