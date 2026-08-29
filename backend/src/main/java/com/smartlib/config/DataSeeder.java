package com.smartlib.config;

import com.smartlib.entity.User;
import com.smartlib.enums.Role;
import com.smartlib.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    @Bean
    CommandLineRunner seedUsers(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            // =====================================================
            // ADMIN
            // =====================================================

            String adminEmail = "admin@smartlib.com";

            if (!userRepository.existsByEmail(adminEmail)) {

                User admin = User.builder()
                        .name("SmartLib Administrator")
                        .email(adminEmail)
                        .password(
                                passwordEncoder.encode("Admin@123")
                        )
                        .role(Role.ADMIN)
                        .active(true)
                        .build();

                userRepository.save(admin);

                System.out.println(
                        "SmartLib ADMIN created: "
                                + adminEmail
                );
            }


            // =====================================================
            // TEST MEMBER
            // =====================================================

            String testMemberEmail =
                    "testmember@smartlib.com";

            User testMember =
                    userRepository
                            .findByEmail(testMemberEmail)
                            .orElse(null);

            if (testMember == null) {

                testMember = User.builder()
                        .name("Test Member")
                        .email(testMemberEmail)
                        .password(
                                passwordEncoder.encode(
                                        "Test@123"
                                )
                        )
                        .role(Role.MEMBER)
                        .active(true)
                        .build();

                userRepository.save(testMember);

                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "SmartLib TEST MEMBER created"
                );

                System.out.println(
                        "Email    : "
                                + testMemberEmail
                );

                System.out.println(
                        "Password : Test@123"
                );

                System.out.println(
                        "========================================"
                );

            } else {

                // Reset password so the credentials
                // are guaranteed to be known.
                testMember.setPassword(
                        passwordEncoder.encode(
                                "Test@123"
                        )
                );

                testMember.setRole(Role.MEMBER);
                testMember.setActive(true);

                userRepository.save(testMember);

                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "SmartLib TEST MEMBER password reset"
                );

                System.out.println(
                        "Email    : "
                                + testMemberEmail
                );

                System.out.println(
                        "Password : Test@123"
                );

                System.out.println(
                        "========================================"
                );
            }
        };
    }
}