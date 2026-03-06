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
        sendEmailOTP(email, "REGISTRATION");
    }

    @Transactional
    public void sendPasswordResetOTP(String email) {
        sendEmailOTP(email, "PASSWORD_RESET");
    }

    @Transactional
    private void sendEmailOTP(String email, String purpose) {
        try {
            System.out.println("STARTING EMAIL OTP PROCESS FOR: " + email);
            otpRepository.deleteByEmailAndPurpose(email, purpose);
            String otp = generateOTP();
            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
            OTPVerification otpVerification = new OTPVerification(email, otp, purpose, expiryDate);
            otpRepository.save(otpVerification);

            // Use EmailService to send the actual email
            emailService.sendOtpEmail(email, otp, purpose);

            System.out.println("EMAIL OTP for " + email + " generated: " + otp);
        } catch (Exception e) {
            System.err.println("EMAIL OTP PROCESS FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send OTP", e);
        }
    }

    @Transactional
    public boolean verifyOTP(String email, String otp, String purpose) {
        try {
            System.out.println("Verifying Email OTP - Email: " + email);
            Optional<OTPVerification> otpOpt = otpRepository.findByEmailAndOtpAndPurposeAndUsedFalse(email, otp,
                    purpose);
            if (otpOpt.isPresent()) {
                OTPVerification otpVerification = otpOpt.get();
                if (otpVerification.isExpired()) {
                    otpRepository.delete(otpVerification);
                    return false;
                }
                otpVerification.setUsed(true);
                otpRepository.save(otpVerification);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error verifying OTP: " + e.getMessage());
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