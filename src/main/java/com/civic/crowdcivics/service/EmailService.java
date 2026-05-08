package com.civic.crowdcivics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            System.out.println("=== EMAIL SERVICE DEBUG ===");
            System.out.println("To: " + toEmail);
            System.out.println("OTP: " + otp);
            System.out.println("Purpose: " + purpose);
            System.out.println("MailSender available: " + (mailSender != null));

            if (mailSender == null) {
                System.out.println("MailSender is NULL - Email configuration issue!");
                throw new RuntimeException("MailSender is not configured");
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(toEmail);
            message.setSubject("Your OTP for " + purpose + " - CrowdCivics");
            message.setText(
                    "Dear User,\n\n" +
                            "Your OTP for " + purpose + " is: " + otp + "\n\n" +
                            "This OTP is valid for 10 minutes.\n\n" +
                            "If you didn't request this, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "CrowdCivics Team");

            mailSender.send(message);
            System.out.println("OTP email sent successfully");
        } catch (Exception e) {
            System.out.println("========== MAIL ERROR ==========");
            e.printStackTrace();

            System.out.println("Message: " + e.getMessage());
            System.out.println("Cause: " + e.getCause());

            throw new RuntimeException("Failed to send OTP: " + e.getMessage());
        }
    }
}