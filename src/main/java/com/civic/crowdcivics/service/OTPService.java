package com.civic.crowdcivics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.civic.crowdcivics.model.OTPVerification;
import com.civic.crowdcivics.repository.OTPVerificationRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OTPService {

    @Autowired
    private OTPVerificationRepository otpRepository;

    @Autowired
    private EmailService emailService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;

    public String generateOTP() {
        Random random = new Random();
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    @Transactional
    public void sendRegistrationOTP(String email) {
        try {
            System.out.println("STARTING OTP PROCESS FOR: " + email);

            otpRepository.deleteByEmailAndPurpose(email, "REGISTRATION");

            String otp = generateOTP();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

            OTPVerification otpVerification = new OTPVerification(email, otp, "REGISTRATION", expiryDate);
            OTPVerification savedOtp = otpRepository.save(otpVerification);

            System.out.println("OTP generated: " + otp + " for email: " + email);
            System.out.println("OTP saved with ID: " + savedOtp.getId());

            emailService.sendOtpEmail(email, otp, "account registration");
            System.out.println("OTP process completed successfully");

        } catch (Exception e) {
            System.err.println("OTP PROCESS FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send OTP: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void sendPasswordResetOTP(String email) {
        try {
            System.out.println("STARTING PASSWORD RESET OTP FOR: " + email);

            otpRepository.deleteByEmailAndPurpose(email, "PASSWORD_RESET");

            String otp = generateOTP();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

            OTPVerification otpVerification = new OTPVerification(email, otp, "PASSWORD_RESET", expiryDate);
            otpRepository.save(otpVerification);

            System.out.println("RESET OTP: " + otp + " for email: " + email);

            emailService.sendOtpEmail(email, otp, "password reset");
            System.out.println("Password reset OTP sent successfully");

        } catch (Exception e) {
            System.err.println("PASSWORD RESET OTP FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send reset OTP", e);
        }
    }

    @Transactional
    public boolean verifyOTP(String email, String otp, String purpose) {
        try {
            System.out.println("Verifying OTP - Email: " + email + ", OTP: " + otp + ", Purpose: " + purpose);

            Optional<OTPVerification> otpOpt = otpRepository.findByEmailAndOtpAndPurposeAndUsedFalse(email, otp, purpose);

            if (otpOpt.isPresent()) {
                OTPVerification otpVerification = otpOpt.get();
                System.out.println("OTP found in database: " + otpVerification.getOtp());
                System.out.println("OTP ID: " + otpVerification.getId());
                System.out.println("OTP Expiry: " + otpVerification.getExpiryDate());
                System.out.println("OTP Used: " + otpVerification.isUsed());

                if (otpVerification.isExpired()) {
                    System.out.println("OTP expired for: " + email);
                    otpRepository.delete(otpVerification);
                    return false;
                }

                otpVerification.setUsed(true);
                otpRepository.save(otpVerification);
                System.out.println("OTP marked as used and saved");
                System.out.println("OTP verified successfully for: " + email);
                return true;
            } else {
                System.out.println("OTP not found in database for email: " + email + " and OTP: " + otp);

                Optional<OTPVerification> anyOtp = otpRepository.findByEmailAndPurposeAndUsedFalse(email, purpose);
                if (anyOtp.isPresent()) {
                    OTPVerification foundOtp = anyOtp.get();
                    System.out.println("But found different OTP in database: " + foundOtp.getOtp());
                    System.out.println("This OTP used status: " + foundOtp.isUsed());
                    System.out.println("This OTP expired: " + foundOtp.isExpired());
                } else {
                    System.out.println("No OTP found for this email and purpose");
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("Error verifying OTP: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void checkOtpStatus(String email, String purpose) {
        try {
            Optional<OTPVerification> otpOpt = otpRepository.findByEmailAndPurposeAndUsedFalse(email, purpose);
            if (otpOpt.isPresent()) {
                OTPVerification otp = otpOpt.get();
                System.out.println("OTP STATUS - Email: " + email);
                System.out.println("OTP: " + otp.getOtp());
                System.out.println("Purpose: " + otp.getPurpose());
                System.out.println("Expiry: " + otp.getExpiryDate());
                System.out.println("Used: " + otp.isUsed());
                System.out.println("Expired: " + otp.isExpired());
            } else {
                System.out.println("No active OTP found for email: " + email + " and purpose: " + purpose);
            }
        } catch (Exception e) {
            System.err.println("Error checking OTP status: " + e.getMessage());
        }
    }
}