package com.civic.crowdcivics.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.civic.crowdcivics.model.User;
import com.civic.crowdcivics.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(User user) {
        System.out.println("REGISTERING USER: " + user.getEmail());
        System.out.println("MOBILE: " + user.getPhone());
        System.out.println("RAW PASSWORD: " + user.getPassword());

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        if (user.getPhone() != null && !user.getPhone().trim().isEmpty() &&
                userRepository.findByPhone(user.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Mobile number already in use");
        }

        try {
            String rawPassword = user.getPassword();
            String encodedPassword = passwordEncoder.encode(rawPassword);
            user.setPassword(encodedPassword);

            System.out.println("ENCODED PASSWORD: " + encodedPassword);
            System.out.println("ENCODED PASSWORD LENGTH: " + encodedPassword.length());

            User savedUser = userRepository.save(user);
            System.out.println("USER SAVED TO DATABASE WITH ID: " + savedUser.getId());

            Optional<User> verifiedUser = userRepository.findByEmail(user.getEmail());
            if (verifiedUser.isPresent()) {
                User dbUser = verifiedUser.get();
                System.out.println("DATABASE VERIFICATION SUCCESSFUL");
                System.out.println("Stored password length: " + dbUser.getPassword().length());
                System.out.println("Stored password starts with: " +
                        (dbUser.getPassword().startsWith("$2a$") ? "BCrypt" : "Unknown"));
            }

            return savedUser;

        } catch (Exception e) {
            System.err.println("REGISTRATION FAILED: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    public Optional<User> findByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            System.out.println("FOUND USER: " + user.get().getEmail());
        } else {
            System.out.println("USER NOT FOUND: " + email);
        }
        return user;
    }

    public Optional<User> findByPhone(String phone) {
        Optional<User> user = userRepository.findByPhone(phone);
        if (user.isPresent()) {
            System.out.println("FOUND USER WITH MOBILE: " + phone);
        } else {
            System.out.println("USER NOT FOUND WITH MOBILE: " + phone);
        }
        return user;
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void updatePassword(User user, String newPassword) {
        System.out.println("UPDATING PASSWORD FOR: " + user.getEmail());

        try {
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);

            System.out.println("NEW ENCODED PASSWORD LENGTH: " + encodedPassword.length());
            userRepository.save(user);

            System.out.println("PASSWORD UPDATED SUCCESSFULLY");
        } catch (Exception e) {
            System.err.println("PASSWORD UPDATE FAILED: " + e.getMessage());
            throw new RuntimeException("Password update failed", e);
        }
    }

    public boolean resetPassword(String email, String newPassword) {
        System.out.println("RESETTING PASSWORD FOR: " + email);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            updatePassword(user, newPassword);
            return true;
        }
        System.out.println("USER NOT FOUND FOR PASSWORD RESET");
        return false;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("LOADING USER: " + username);

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> {
                    System.out.println("USER NOT FOUND: " + username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        System.out.println("USER LOADED: " + user.getEmail());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles("USER")
                .build();
    }
}