package g2.be.repository;

import g2.be.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByEmailAndOtpCodeAndTypeAndIsUsed(
            String email,
            String otpCode,
            VerificationToken.TokenType type,
            Boolean isUsed
    );

    Optional<VerificationToken> findByEmailAndType(String email, VerificationToken.TokenType type);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiryDate < :now")
    void deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.email = :email AND vt.type = :type")
    void deleteByEmailAndType(String email, VerificationToken.TokenType type);
}
