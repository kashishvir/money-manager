package com.BuildMoneyManager.MoneyManager.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.sender}")
    private String senderEmail;

    /**
     * Sends a simple HTML email.
     */
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true indicates multipart message
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true indicates HTML

            mailSender.send(message);
            System.out.println("✅ Email sent successfully to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email to " + to + ": " + e.getMessage());
            // Re-throw so callers can detect and log the failure
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }

    /**
     * Sends an email with an attachment (byte[] + filename).
     */
    public void sendEmailWithAttachment(String to, String subject, String htmlContent, byte[] attachment, String filename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true indicates multipart message for attachments
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true indicates HTML
            
            // Add the attachment
            helper.addAttachment(filename, new ByteArrayResource(attachment));
            
            mailSender.send(message);
            System.out.println("✅ Email with attachment sent successfully to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email with attachment: " + e.getMessage());
        }
    }
}
