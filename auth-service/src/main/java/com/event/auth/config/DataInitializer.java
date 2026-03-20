package com.event.auth.config;

import com.event.auth.entity.User;
import com.event.auth.entity.UserProfile;
import com.event.auth.repository.UserProfileRepository;
import com.event.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, 
                           UserProfileRepository userProfileRepository, 
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void run(String... args) throws Exception {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@event.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .isActive(true)
                    .build();
            admin = userRepository.save(admin);

            UserProfile profile = UserProfile.builder()
                    .user(admin)
                    .fullName("System Administrator")
                    .build();
            userProfileRepository.save(profile);
            
            System.out.println("Default Admin user created: admin / admin123");
        }
    }
}
