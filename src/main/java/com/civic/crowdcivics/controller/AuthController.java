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
            System.out.println("SENDING REGISTRATION EMAIL OTP TO: " + request.getEmail());
            if (userService.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already registered");
            }
            otpService.sendRegistrationOTP(request.getEmail());
            return ResponseEntity.ok("OTP sent successfully to email!");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Failed to send OTP: " + ex.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest.UserRegistrationRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }

            if (userService.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already registered");
            }
            if (userService.findByPhone(request.getPhone()).isPresent()) {
                return ResponseEntity.badRequest().body("Mobile number already registered");
            }

            boolean otpValid = otpService.verifyOTP(request.getEmail(), request.getOtp(), "REGISTRATION");
            if (!otpValid) {
                return ResponseEntity.badRequest().body("Invalid or expired Email OTP. Please request a new OTP.");
            }

            String role = request.getRole() != null ? request.getRole().toUpperCase() : "CITIZEN";
            User user = new User();

            if ("ADMIN".equals(role)) {
                if (request.getAdminId() == null || !request.getAdminId().matches("\\d{8}")) {
                    return ResponseEntity.badRequest().body("Admin ID must be exactly 8 digits");
                }
                user.setAdminId(request.getAdminId());
                user.setDepartment(request.getDepartment());
            }

            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setPhone(request.getPhone());
            user.setPassword(request.getPassword());
            user.setRole(role);

            userService.register(user);
            return ResponseEntity.ok("Registration successful!");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Registration failed: " + ex.getMessage());
        }
    }

    @PostMapping("/send-reset-otp")
    public ResponseEntity<?> sendResetOtp(@RequestBody AuthRequest.EmailRequest request) {
        try {
            System.out.println("SENDING RESET EMAIL OTP TO: " + request.getEmail());
            if (userService.findByEmail(request.getEmail()).isEmpty()) {
                return ResponseEntity.badRequest().body("Email not registered");
            }
            otpService.sendPasswordResetOTP(request.getEmail());
            return ResponseEntity.ok("OTP sent successfully to email!");
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Failed to send OTP");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody AuthRequest.PasswordResetRequest request) {
        try {
            boolean otpValid = otpService.verifyOTP(request.getEmail(), request.getOtp(), "PASSWORD_RESET");
            if (!otpValid) {
                return ResponseEntity.badRequest().body("Invalid or expired Email OTP");
            }

            Optional<User> userContent = userService.findByEmail(request.getEmail());
            if (userContent.isPresent()) {
                userService.resetPassword(request.getEmail(), request.getNewPassword());
                return ResponseEntity.ok("Password reset successful!");
            } else {
                return ResponseEntity.badRequest().body("User not found");
            }
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Password reset failed");
        }
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody AuthRequest.PasswordUpdateRequest request) {
        try {
            Optional<User> userOpt = userService.findByEmail(request.getEmail());

            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("User not found");
            }

            User user = userOpt.get();

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body("Incorrect current password.");
            }

            userService.resetPassword(request.getEmail(), request.getNewPassword());
            return ResponseEntity.ok("Password updated successfully!");

        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Failed to update password: " + ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest.LoginRequest request) {
        System.out.println("LOGIN ATTEMPT FOR: " + request.getEmail() + " AS ROLE: " + request.getRole());

        try {
            User user = null;

            if ("ADMIN".equalsIgnoreCase(request.getRole())) {
                Optional<User> adminUser = userService.findByAdminId(request.getEmail());
                if (adminUser.isPresent() && "ADMIN".equalsIgnoreCase(adminUser.get().getRole())) {
                    user = adminUser.get();
                }
            } else {
                Optional<User> userContent = userService.findByEmail(request.getEmail());
                if (userContent.isPresent() && !"ADMIN".equalsIgnoreCase(userContent.get().getRole())) {
                    user = userContent.get();
                }
            }

            if (user != null) {
                boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());

                if (passwordMatches) {
                    System.out.println("LOGIN SUCCESSFUL FOR: " + user.getEmail());
                    java.util.Map<String, Object> response = new java.util.HashMap<>();
                    response.put("message", "Login successful");
                    response.put("role", user.getRole());
                    response.put("userId", user.getId());
                    response.put("email", user.getEmail());
                    response.put("name", user.getName());
                    return ResponseEntity.ok(response);
                } else {
                    System.out.println("INVALID PASSWORD FOR: " + request.getEmail());
                    return ResponseEntity.badRequest().body("Invalid credentials");
                }
            } else {
                return ResponseEntity.badRequest().body("User not found");
            }
        } catch (Exception ex) {
            System.err.println("LOGIN ERROR FOR: " + request.getEmail() + " - " + ex.getMessage());
            return ResponseEntity.badRequest().body("Login failed: " + ex.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUser(@RequestParam String email) {
        Optional<User> user = userService.findByEmail(email);
        if (user.isPresent()) {
            User userData = user.get();
            userData.setPassword("HIDDEN");

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", userData.getId());
            response.put("name", userData.getName());
            response.put("email", userData.getEmail());
            response.put("phone", userData.getPhone());
            response.put("role", userData.getRole());

            if ("ADMIN".equals(userData.getRole())) {
                response.put("adminId", userData.getAdminId());
                response.put("department", userData.getDepartment());
            }

            return ResponseEntity.ok(response);
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