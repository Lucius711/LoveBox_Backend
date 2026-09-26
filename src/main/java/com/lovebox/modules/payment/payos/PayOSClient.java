package com.lovebox.modules.payment.payos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Minimal PayOS API client.
 * Docs: https://payos.vn/docs/api/
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayOSClient {

    private static final String BASE_URL = "https://api-merchant.payos.vn";

    private final RestClient http = RestClient.create(BASE_URL);
    private final ObjectMapper objectMapper;

    @Value("${app.payment.payos.client-id:}") private String clientId;
    @Value("${app.payment.payos.api-key:}") private String apiKey;
    @Value("${app.payment.payos.checksum-key:}") private String checksumKey;
    @Value("${app.payment.payos.return-url}") private String returnUrl;
    @Value("${app.payment.payos.cancel-url}") private String cancelUrl;

    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank();
    }

    /**
     * Create a PayOS payment link.
     * @return PayOSPaymentData (qrCode, checkoutUrl, orderCode) or null if PayOS is not configured
     */
    public PayOSPaymentData createPaymentLink(long orderCode, long amount, String description) {
        if (!isEnabled()) return null;
        try {
            String sig = sign(Map.of(
                    "amount", String.valueOf(amount),
                    "cancelUrl", cancelUrl,
                    "description", description,
                    "orderCode", String.valueOf(orderCode),
                    "returnUrl", returnUrl));

            Map<String, Object> body = Map.of(
                    "orderCode", orderCode,
                    "amount", amount,
                    "description", description,
                    "cancelUrl", cancelUrl,
                    "returnUrl", returnUrl,
                    "signature", sig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-client-id", clientId);
            headers.set("x-api-key", apiKey);

            String raw = http.post().uri("/v2/payment-requests").headers(h -> h.addAll(headers)).body(body).retrieve().body(String.class);

            JsonNode root = objectMapper.readTree(raw);
            if (!"00".equals(root.path("code").asText())) {
                log.warn("PayOS error: {}", raw);
                return null;
            }
            JsonNode data = root.path("data");
            return new PayOSPaymentData(
                    data.path("qrCode").asText(null),
                    data.path("checkoutUrl").asText(null),
                    orderCode);
        } catch (Exception e) {
            log.error("PayOS createPaymentLink failed", e);
            return null;
        }
    }

    /**
     * Verify webhook signature.
     * Signature = HMAC-SHA256 of sorted data fields with checksumKey.
     */
    public boolean verifyWebhook(Map<String, Object> webhookData, String signature) {
        try {
            String computed = sign(flatten(webhookData));
            return computed.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("PayOS webhook signature verification failed", e);
            return false;
        }
    }

    // ── internal ──────────────────────────────────────────────────────────

    private String sign(Map<String, String> params) throws Exception {
        return sign(params, checksumKey);
    }

    private static String sign(Map<String, String> params, String key) throws Exception {
        // sort by key, concat key=value&...
        TreeMap<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> { if (sb.length() > 0) sb.append('&'); sb.append(k).append('=').append(v); });

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(raw);
    }

    /** Flatten a nested webhook data map to String→String for signing */
    @SuppressWarnings("unchecked")
    private Map<String, String> flatten(Map<String, Object> map) {
        Map<String, String> result = new TreeMap<>();
        map.forEach((k, v) -> {
            if (v instanceof Map) ((Map<String, Object>) v).forEach((k2, v2) -> result.put(k2, String.valueOf(v2)));
            else result.put(k, String.valueOf(v));
        });
        return result;
    }

    public record PayOSPaymentData(String qrCode, String checkoutUrl, long orderCode) {}
}
