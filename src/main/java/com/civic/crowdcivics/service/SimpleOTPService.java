//package com.civic.crowdcivics.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import com.civic.crowdcivics.model.OTPVerification;
//import com.civic.crowdcivics.repository.OTPVerificationRepository;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//import java.util.Random;
//
//@Service
//public class SimpleOTPService {
//
//    @Autowired
//    private OTPVerificationRepository otpRepository;
//
//    private static final int OTP_LENGTH = 6;
//    private static final int OTP_EXPIRY_MINUTES = 10;
//
//    public String generateOTP() {
//        Random random = new Random();
//        StringBuilder otp = new StringBuilder();
//        for (int i = 0; i < OTP_LENGTH; i++) {
//            otp.append(random.nextInt(10));
//        }
//        return otp.toString();
//    }
//
//    @Transactional
//    public String sendRegistrationOTP(String email) {
//        try {
//            System.out.println("SIMPLE OTP SERVICE - REGISTRATION");
//
//            otpRepository.deleteByEmailAndPurpose(email, "REGISTRATION");
//
//            String otp = generateOTP();
//            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
//
//            OTPVerification otpVerification = new OTPVerification(email, otp, "REGISTRATION", expiryDate);
//            otpRepository.save(otpVerification);
//
//            System.out.println("YOUR OTP FOR REGISTRATION");
//            System.out.println("Email: " + email);
//            System.out.println("OTP: " + otp);
//            System.out.println("Valid for: " + OTP_EXPIRY_MINUTES + " minutes");
//
//            return otp;
//
//        } catch (Exception e) {
//            System.err.println("SIMPLE OTP FAILED: " + e.getMessage());
//            throw new RuntimeException("OTP service unavailable");
//        }
//    }
//}