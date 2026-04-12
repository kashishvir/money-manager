package com.BuildMoneyManager.MoneyManager.controller;

import com.BuildMoneyManager.MoneyManager.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final EmailService emailService;

    @GetMapping({"/status", "/health"})
    public String healthCheck(){
        return "Application is running";
    }

    /**
     * Diagnostic endpoint — send a test email to verify SMTP config on Render.
     * Example: GET /api/v1.0/test-email?to=yourname@gmail.com
     * Remove or secure this endpoint once email is confirmed working.
     */
    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail(@RequestParam String to) {
        try {
            emailService.sendEmail(
                to,
                "Money Manager — SMTP Test",
                "<p>Hello! 👋</p><p>This is a test email from your Money Manager backend.</p>"
                + "<p>If you received this, your SMTP configuration is working correctly!</p>"
            );
            return ResponseEntity.ok("✅ Test email sent successfully to: " + to);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("❌ Failed to send test email: " + e.getMessage()
                    + "\n\nCause: " + (e.getCause() != null ? e.getCause().getMessage() : "unknown"));
        }
    }
}
