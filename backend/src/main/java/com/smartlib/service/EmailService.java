package com.smartlib.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(
            String toEmail,
            String resetToken) {

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject(
                "SmartLib Password Reset"
        );

        message.setText(
                "Hello,\n\n" +
                "We received a request to reset your SmartLib password.\n\n" +
                "Your password reset token is:\n\n" +
                resetToken + "\n\n" +
                "This token is valid for 15 minutes.\n\n" +
                "If you did not request this password reset, " +
                "you can safely ignore this email.\n\n" +
                "Regards,\n" +
                "SmartLib Team"
        );

        mailSender.send(message);
    }
}