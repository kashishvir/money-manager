package com.BuildMoneyManager.MoneyManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String senderEmail;

    /**
     * Sends a simple email.
     */
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            String url = "https://api.brevo.com/v3/smtp/email";
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", senderEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            System.out.println("✅ Email sent successfully to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Sends an email with an attachment (byte[] + filename).
     */
    public void sendEmailWithAttachment(String to, String subject, String htmlContent, byte[] attachment, String filename) {
        try {
            String url = "https://api.brevo.com/v3/smtp/email";
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            // Convert byte[] to Base64 string
            String base64File = Base64.getEncoder().encodeToString(attachment);

            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", senderEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlContent,
                    "attachment", List.of(Map.of(
                            "name", filename,
                            "content", base64File
                    ))
            );

            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            System.out.println("✅ Email with attachment sent successfully to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email with attachment: " + e.getMessage());
        }
    }
}
