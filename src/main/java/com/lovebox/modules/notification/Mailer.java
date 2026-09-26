package com.lovebox.modules.notification;

import com.lovebox.common.util.AfterCommit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Gửi email qua Resend — chỉ gửi SAU khi transaction commit, chạy nền, lỗi chỉ log (không làm hỏng nghiệp vụ). */
@Slf4j
@Component
public class Mailer {

    private final RestClient resend;
    private final String from;
    private final String frontendUrl;
    private final boolean enabled;

    public Mailer(@Value("${app.notification.resend-api-key:}") String apiKey,
                  @Value("${app.notification.from-email:noreply@lentique.vn}") String from,
                  @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.enabled = !apiKey.isBlank();
        this.from = from;
        this.frontendUrl = frontendUrl;
        this.resend = RestClient.builder().baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey).build();
    }

    public String link(String path) { return frontendUrl + path; }

    public void send(String to, String subject, String html) {
        if (!enabled || to == null) { log.info("[Mail] skip ({}): {}", to, subject); return; }
        Runnable job = () -> CompletableFuture.runAsync(() -> {
            try {
                resend.post().uri("/emails")
                        .body(Map.of("from", from, "to", List.of(to), "subject", subject, "html", wrap(html)))
                        .retrieve().toBodilessEntity();
            } catch (Exception e) {
                log.warn("[Mail] gửi tới {} lỗi: {}", to, e.getMessage());
            }
        });
        AfterCommit.run(job);
    }

    private static String wrap(String body) {
        return "<div style=\"font-family:Arial,sans-serif;font-size:15px;line-height:1.6;color:#1c1917;max-width:520px\">"
                + "<p style=\"font-size:22px;font-weight:bold\">Lentique<span style=\"color:#a8404f\">.</span></p>"
                + body + "<p style=\"color:#78716c;font-size:12px\">Email tự động từ Lentique — vui lòng không trả lời.</p></div>";
    }
}
