package com.example.vprofile.emailservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.base-url}")
    private String baseUrl;

    @Async
    @SuppressWarnings("UseSpecificCatch")
    public void sendVerificationEmail(String to, String token, String entityType) {
        try {
            String subject = "Verify Your Email for Wezume";
            String verificationLink = "";

            if ("user".equalsIgnoreCase(entityType)) {
                verificationLink = baseUrl + "/api/verify-email?token=" + token;
            } else if ("PlacementLogin".equalsIgnoreCase(entityType)) {
                verificationLink = baseUrl + "/api/verify/placement/" + token;
            }

            Context context = new Context();
            context.setVariable("subject", subject);
            context.setVariable("verificationLink", verificationLink);

            String htmlContent = templateEngine.process("verification-email.html", context);
            String plainTextContent = "Welcome! Please verify your email by visiting this link: " + verificationLink;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("no-reply@wezume.in", "Wezume Team");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainTextContent, htmlContent);

            mailSender.send(message);
            logger.info("Verification email sent successfully to {}.", to);

        } catch (Exception e) {
            logger.error("Error sending verification email to {}", to, e);
        }
    }
}