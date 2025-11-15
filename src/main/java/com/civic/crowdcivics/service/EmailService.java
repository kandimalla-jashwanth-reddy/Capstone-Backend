package com.civic.crowdcivics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        try {
            System.out.println("=== EMAIL SERVICE DEBUG ===");
            System.out.println("To: " + toEmail);
            System.out.println("OTP: " + otp);
            System.out.println("Purpose: " + purpose);
            System.out.println("MailSender available: " + (mailSender != null));

            if (mailSender == null) {
                System.out.println("MailSender is NULL - Email configuration issue!");
                System.out.println("OTP for " + purpose + ": " + otp + " (Email not sent)");
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("kandimallajashwanthreddy594@gmail.com");
            message.setTo(toEmail);
            message.setSubject("Your OTP for " + purpose + " - CrowdCivics");
            message.setText(
                    "Dear User,\n\n" +
                            "Your OTP for " + purpose + " is: " + otp + "\n\n" +
                            "This OTP is valid for 10 minutes.\n\n" +
                            "If you didn't request this, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "CrowdCivics Team"
            );

            mailSender.send(message);
            System.out.println("REAL EMAIL SENT SUCCESSFULLY to: " + toEmail);
            System.out.println("OTP: " + otp);

        } catch (Exception e) {
            System.err.println("EMAIL SENDING FAILED: " + e.getMessage());
            System.out.println("OTP for " + purpose + ": " + otp + " (Email failed)");
            e.printStackTrace();

            System.out.println("=".repeat(50));
            System.out.println("YOUR OTP FOR " + purpose.toUpperCase());
            System.out.println("Email: " + toEmail);
            System.out.println("OTP: " + otp);
            System.out.println("Valid for: 10 minutes");
            System.out.println("=".repeat(50));
        }
    }
}