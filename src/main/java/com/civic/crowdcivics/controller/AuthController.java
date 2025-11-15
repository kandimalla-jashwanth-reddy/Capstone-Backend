package com.civic.crowdcivics.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.civic.crowdcivics.model.User;
import com.civic.crowdcivics.service.OTPService;
import com.civic.crowdcivics.service.UserService;
import com.civic.crowdcivics.repository.OTPVerificationRepository;
import com.civic.crowdcivics.model.OTPVerification;
import com.civic.crowdcivics.dto.AuthRequest;
import com.civic.crowdcivics.dto.OTPStatusResponse;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private OTPService otpService;

    @Autowired
    private OTPVerificationRepository otpRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/test")
    public String test() {
        return "Backend is working!";
    }

    @PostMapping("/send-registration-otp")
    public ResponseEntity<?> sendRegistrationOtp(@RequestBody AuthRequest.EmailRequest request) {
        try {
            System.out.println("SENDING REGISTRATION OTP TO: " + request.getEmail());

            if (userService.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already registered");
            }

            otpService.sendRegistrationOTP(request.getEmail());
            return ResponseEntity.ok("OTP sent successfully! Check console for OTP code.");

        } catch (Exception ex) {
            System.err.println("OTP SEND ERROR: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to send OTP: " + ex.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest.UserRegistrationRequest request) {
        try {
            System.out.println("REGISTRATION ATTEMPT FOR: " + request.getEmail());
            System.out.println("MOBILE: " + request.getPhone());
            System.out.println("OTP PROVIDED: " + request.getOtp());
            System.out.println("NAME: " + request.getName());

            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                Optional<User> existingUserWithMobile = userService.findByPhone(request.getPhone());
                if (existingUserWithMobile.isPresent()) {
                    return ResponseEntity.badRequest().body("Mobile number already registered");
                }
            }

            System.out.println("CHECKING OTP STATUS BEFORE VERIFICATION:");
            Optional<OTPVerification> existingOtp = otpRepository.findByEmailAndPurposeAndUsedFalse(request.getEmail(), "REGISTRATION");
            if (existingOtp.isPresent()) {
                OTPVerification otp = existingOtp.get();
                System.out.println("FOUND OTP IN DB: " + otp.getOtp());
                System.out.println("EXPIRY: " + otp.getExpiryDate());
                System.out.println("USED: " + otp.isUsed());
                System.out.println("EXPIRED: " + otp.isExpired());
            } else {
                System.out.println("NO OTP FOUND IN DATABASE FOR: " + request.getEmail());
            }

            boolean otpValid = otpService.verifyOTP(request.getEmail(), request.getOtp(), "REGISTRATION");
            System.out.println("OTP VERIFICATION RESULT: " + otpValid);

            if (!otpValid) {
                System.out.println("OTP VERIFICATION FAILED - CHECKING DATABASE AGAIN:");
                Optional<OTPVerification> failedOtp = otpRepository.findByEmailAndPurposeAndUsedFalse(request.getEmail(), "REGISTRATION");
                if (failedOtp.isPresent()) {
                    OTPVerification otp = failedOtp.get();
                    System.out.println("STILL FOUND OTP: " + otp.getOtp());
                    System.out.println("USED STATUS: " + otp.isUsed());
                }
                return ResponseEntity.badRequest().body("Invalid or expired OTP. Please request a new OTP.");
            }

            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setPassword(request.getPassword());

            User registeredUser = userService.register(user);
            System.out.println("REGISTRATION SUCCESSFUL FOR: " + request.getEmail());
            return ResponseEntity.ok("Registration successful!");

        } catch (IllegalArgumentException ex) {
            System.err.println("REGISTRATION ERROR: " + ex.getMessage());
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            System.err.println("UNEXPECTED REGISTRATION ERROR: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.badRequest().body("Registration failed: " + ex.getMessage());
        }
    }

    @PostMapping("/send-reset-otp")
    public ResponseEntity<?> sendResetOtp(@RequestBody AuthRequest.EmailRequest request) {
        try {
            System.out.println("SENDING RESET OTP TO: " + request.getEmail());

            if (userService.findByEmail(request.getEmail()).isEmpty()) {
                return ResponseEntity.badRequest().body("Email not registered");
            }

            otpService.sendPasswordResetOTP(request.getEmail());
            return ResponseEntity.ok("OTP sent successfully! Check console for OTP code.");

        } catch (Exception ex) {
            System.err.println("RESET OTP SEND ERROR: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to send OTP");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody AuthRequest.PasswordResetRequest request) {
        System.out.println("PASSWORD RESET FOR: " + request.getEmail());
        System.out.println("OTP PROVIDED: " + request.getOtp());

        try {
            System.out.println("CHECKING OTP STATUS BEFORE PASSWORD RESET:");
            Optional<OTPVerification> existingOtp = otpRepository.findByEmailAndPurposeAndUsedFalse(request.getEmail(), "PASSWORD_RESET");
            if (existingOtp.isPresent()) {
                OTPVerification otp = existingOtp.get();
                System.out.println("FOUND RESET OTP IN DB: " + otp.getOtp());
                System.out.println("EXPIRY: " + otp.getExpiryDate());
                System.out.println("USED: " + otp.isUsed());
            }

            boolean otpValid = otpService.verifyOTP(request.getEmail(), request.getOtp(), "PASSWORD_RESET");
            System.out.println("RESET OTP VERIFICATION RESULT: " + otpValid);

            if (!otpValid) {
                return ResponseEntity.badRequest().body("Invalid or expired OTP. Please request a new OTP.");
            }

            boolean success = userService.resetPassword(request.getEmail(), request.getNewPassword());
            if (success) {
                System.out.println("PASSWORD RESET SUCCESSFUL FOR: " + request.getEmail());
                return ResponseEntity.ok("Password reset successful!");
            } else {
                System.out.println("PASSWORD RESET FAILED - USER NOT FOUND: " + request.getEmail());
                return ResponseEntity.badRequest().body("User not found");
            }
        } catch (Exception ex) {
            System.err.println("PASSWORD RESET ERROR: " + ex.getMessage());
            ex.printStackTrace();
            return ResponseEntity.badRequest().body("Password reset failed");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest.LoginRequest request) {
        System.out.println("LOGIN ATTEMPT FOR: " + request.getEmail());

        try {
            UserDetails userDetails = userService.loadUserByUsername(request.getEmail());

            boolean passwordMatches = passwordEncoder.matches(request.getPassword(), userDetails.getPassword());

            if (passwordMatches) {
                System.out.println("LOGIN SUCCESSFUL FOR: " + request.getEmail());
                return ResponseEntity.ok("Login successful");
            } else {
                System.out.println("INVALID PASSWORD FOR: " + request.getEmail());
                return ResponseEntity.badRequest().body("Invalid credentials");
            }
        } catch (Exception ex) {
            System.err.println("LOGIN ERROR FOR: " + request.getEmail() + " - " + ex.getMessage());
            return ResponseEntity.badRequest().body("Invalid credentials");
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUser(@RequestParam String email) {
        Optional<User> user = userService.findByEmail(email);
        if (user.isPresent()) {
            User userData = user.get();
            userData.setPassword("HIDDEN");
            return ResponseEntity.ok(userData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/check-mobile")
    public ResponseEntity<?> checkMobileAvailability(@RequestParam String phone) {
        try {
            if (phone == null || phone.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Mobile number is required");
            }

            boolean mobileExists = userService.findByPhone(phone).isPresent();

            if (mobileExists) {
                return ResponseEntity.ok("Mobile number already in use");
            } else {
                return ResponseEntity.ok("Mobile number available");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking mobile number: " + e.getMessage());
        }
    }

    @GetMapping("/debug/otp-status")
    public ResponseEntity<?> debugOtpStatus(@RequestParam String email, @RequestParam String purpose) {
        try {
            Optional<OTPVerification> otpOpt = otpRepository.findByEmailAndPurposeAndUsedFalse(email, purpose);
            if (otpOpt.isPresent()) {
                OTPVerification otp = otpOpt.get();
                OTPStatusResponse response = new OTPStatusResponse();
                response.setEmail(otp.getEmail());
                response.setOtp(otp.getOtp());
                response.setPurpose(otp.getPurpose());
                response.setExpiryDate(otp.getExpiryDate());
                response.setUsed(otp.isUsed());
                response.setExpired(otp.isExpired());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok("No active OTP found for email: " + email + " and purpose: " + purpose);
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error checking OTP status: " + e.getMessage());
        }
    }

    @GetMapping("/test-otp")
    public ResponseEntity<?> testOtp(@RequestParam String email) {
        try {
            otpService.sendRegistrationOTP(email);

            Optional<OTPVerification> otpOpt = otpRepository.findByEmailAndPurposeAndUsedFalse(email, "REGISTRATION");
            if (otpOpt.isPresent()) {
                return ResponseEntity.ok("OTP sent and saved: " + otpOpt.get().getOtp());
            } else {
                return ResponseEntity.badRequest().body("OTP not saved in database");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/test-password")
    public ResponseEntity<?> testPassword(@RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        return ResponseEntity.ok("Raw: " + password + " | Encoded: " + encoded);
    }
}