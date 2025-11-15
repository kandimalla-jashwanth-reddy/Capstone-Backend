package com.civic.crowdcivics.repository;

import com.civic.crowdcivics.model.OTPVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OTPVerificationRepository extends JpaRepository<OTPVerification, Long> {

    Optional<OTPVerification> findByEmailAndOtpAndPurposeAndUsedFalse(String email, String otp, String purpose);

    Optional<OTPVerification> findByEmailAndPurposeAndUsedFalse(String email, String purpose);

    @Query("SELECT COUNT(o) FROM OTPVerification o WHERE o.email = :email AND o.purpose = :purpose")
    long countByEmailAndPurpose(@Param("email") String email, @Param("purpose") String purpose);

    @Modifying
    @Query("DELETE FROM OTPVerification o WHERE o.email = :email AND o.purpose = :purpose")
    void deleteByEmailAndPurpose(@Param("email") String email, @Param("purpose") String purpose);

    @Modifying
    @Query("DELETE FROM OTPVerification o WHERE o.expiryDate < CURRENT_TIMESTAMP")
    void deleteExpiredOtps();
}