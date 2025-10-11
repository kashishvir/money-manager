package com.BuildMoneyManager.MoneyManager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${spring.mail.properties.mail.smtp.from}")
    private String sendersEmail;

    public void sendEmail(String to, String subject, String body) {
        String url = "https://api.brevo.com/v3/smtp/email";

        Map<String, Object> payload = Map.of(
                "sender", Map.of("email", sendersEmail, "name", "Money Manager"), // ✅ FIXED
                "to", new Object[]{Map.of("email", to)},
                "subject", subject,
                "textContent", body
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        new RestTemplate().postForEntity(url, entity, String.class);
    }
}