package com.lovebox.modules.stylist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lovebox.common.constant.ErrorCode;
import com.lovebox.common.exception.AppException;
import com.lovebox.modules.ai.config.AiProperties;
import com.lovebox.modules.product.Product;
import com.lovebox.modules.product.ProductDtos.PageResult;
import com.lovebox.modules.product.ProductDtos.Summary;
import com.lovebox.modules.product.ProductService;
import com.lovebox.modules.product.Vocab;
import com.lovebox.modules.stylist.StylistMatcher.Body;
import com.lovebox.modules.stylist.StylistMatcher.Criteria;
import com.lovebox.modules.stylist.StylistMatcher.Profile;
import com.lovebox.modules.user.entity.User;
import com.lovebox.modules.user.service.UserService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;

/**
 * Trợ lý AI tìm đồ: form (dịp, số đo, ngân sách) + câu mô tả tự do
 * → Gemini bóc từ khoá theo bộ nhãn chuẩn → so khớp Tags trong DB → 3-5 bộ khớp nhất (≥80%).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StylistService {

    static final double MIN_MATCH = 0.8;
    static final int MAX_RESULTS = 5;

    private final ProductService productService;
    private final UserService userService;
    private final RestClient geminiChatClient;
    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final ChatSessionRepository chatRepository;

    public record Request(@NotBlank @Size(max = 1000) String prompt, UUID chatId) {}
    public record ChatReply(UUID chatId, Result result) {}
    public record ChatSummary(UUID id, String title, boolean archived, Instant updatedAt) {
        static ChatSummary of(ChatSession s) { return new ChatSummary(s.getId(), s.getTitle(), s.isArchived(), s.getUpdatedAt()); }
    }
    public record ChatDetail(UUID id, String title, boolean archived, List<Map<String, Object>> messages) {}
    public record ArchiveRequest(boolean archived) {}

    public record Match(Summary product, int matchRate, boolean alternative) {}

    /** missing: thông tin hồ sơ còn thiếu (khách bỏ qua onboarding) → bot hỏi lại mỗi lượt. */
    public record Result(String size, Criteria criteria, boolean usedAi, String message, List<Match> results, List<String> missing) {
        Result(String size, Criteria criteria, boolean usedAi, String message, List<Match> results) {
            this(size, criteria, usedAi, message, results, List.of());
        }
    }

    /** Gợi ý cho riêng người dùng (trang chủ) = tìm với câu trống, xếp theo hồ sơ. */
    public List<Match> recommend(UUID userId) {
        return search(userId, null).results();
    }

    /**
     * Số đo + ngân sách lấy từ hồ sơ onboarding (lọc cứng); câu chat là tiêu chí khớp;
     * sở thích trong hồ sơ dùng để xếp hạng.
     */
    public Result search(UUID userId, String prompt) {
        User u = userService.get(userId);
        // Hồ sơ trống → lấy tạm số đo khách gõ trong chat ("1m6, 50kg")
        Integer h = u.getHeightCm() != null ? u.getHeightCm() : StylistMatcher.parseHeight(prompt);
        Integer w = u.getWeightKg() != null ? u.getWeightKg() : StylistMatcher.parseWeight(prompt);
        String size = u.getClothingSize() != null ? u.getClothingSize() : StylistMatcher.sizeFor(h, w);
        Body body = new Body(h, w, u.getBust(), u.getWaist(), u.getHip());
        Profile profile = Profile.of(u.getFavOccasions(), u.getFavStyles(), u.getFavColors(), u.getFitNote());

        Criteria extracted = extractWithAi(prompt);
        boolean usedAi = extracted != null;
        if (extracted == null) extracted = StylistMatcher.extractLocal(prompt);
        Criteria c = extracted.orMaxPrice(u.getBudgetMax());

        List<String> missing = new ArrayList<>();
        if (u.getClothingSize() == null) {   // đã khai size thì không cần hỏi chiều cao / cân nặng
            if (h == null) missing.add("chiều cao");
            if (w == null) missing.add("cân nặng");
        }
        if (c.maxPrice() == null) missing.add("ngân sách thuê mỗi ngày");
        Result r = find(size, body, profile, c, usedAi);
        if (missing.isEmpty()) return r;
        String ask = " Để chọn vừa người hơn, bạn cho mình biết thêm " + String.join(", ", missing)
                + " (vd: 1m60, 50kg, dưới 300k) nhé. Lưu vào Hồ sơ thì lần sau mình không phải hỏi lại!";
        return new Result(r.size(), r.criteria(), r.usedAi(), r.message() + ask, r.results(), missing);
    }

    // ── Lịch sử chat ─────────────────────────────────────────────────────
    /** Gộp câu mới vào các câu trước của cuộc chat → tìm → lưu cả câu hỏi lẫn kết quả. */
    @Transactional
    public ChatReply chat(UUID userId, UUID chatId, String raw) {
        String msg = raw.strip();
        ChatSession s = chatId == null ? new ChatSession() : own(userId, chatId);
        if (s.getId() == null) {
            s.setUserId(userId);
            s.setTitle(msg.length() > 120 ? msg.substring(0, 117) + "..." : msg);
        }
        String prompt = s.getPrompt().isBlank() ? msg : s.getPrompt() + ". " + msg;
        Result r = search(userId, prompt);
        List<Map<String, Object>> msgs = new ArrayList<>(s.getMessages());   // list mới → Hibernate chắc chắn thấy thay đổi
        msgs.add(Map.of("from", "user", "text", msg));
        msgs.add(Map.of("from", "bot", "result", objectMapper.convertValue(r, new TypeReference<Map<String, Object>>() {})));
        s.setMessages(msgs);
        s.setPrompt(prompt);
        s.setArchived(false);   // nói tiếp cuộc đã lưu trữ → đưa về danh sách chính
        return new ChatReply(chatRepository.save(s).getId(), r);
    }

    public PageResult<ChatSummary> chats(UUID userId, boolean archived, int page, int size) {
        int n = Math.min(Math.max(size, 1), 50);
        Page<ChatSession> p = chatRepository.findByUserIdAndArchivedOrderByUpdatedAtDesc(userId, archived,
                PageRequest.of(Math.max(page, 1) - 1, n));
        return new PageResult<>(p.getContent().stream().map(ChatSummary::of).toList(),
                p.getNumber() + 1, n, p.getTotalElements(), p.getTotalPages());
    }

    public ChatDetail chatDetail(UUID userId, UUID id) {
        ChatSession s = own(userId, id);
        return new ChatDetail(s.getId(), s.getTitle(), s.isArchived(), s.getMessages());
    }

    @Transactional
    public ChatSummary archive(UUID userId, UUID id, boolean archived) {
        ChatSession s = own(userId, id);
        s.setArchived(archived);
        return ChatSummary.of(chatRepository.saveAndFlush(s));
    }

    private ChatSession own(UUID userId, UUID id) {
        return chatRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Result find(String size, Body body, Profile profile, Criteria c, boolean usedAi) {
        Long max = c.maxPrice();

        List<Product> pool = productService.approved().stream()
                .filter(p -> StylistMatcher.fits(p, body, size))
                .filter(p -> max == null || max == 0 || p.getRentPricePerDay() <= max)
                .toList();
        if (pool.isEmpty())
            return new Result(size, c, usedAi, "Hiện chưa có bộ nào vừa số đo và ngân sách trong hồ sơ của bạn. "
                    + "Bạn thử nới ngân sách hoặc cập nhật số đo trong Hồ sơ nhé!", List.of());

        List<Match> exact = rank(pool, c, profile, Set.of(), false, MIN_MATCH);
        String what = describe(c);
        if (exact.size() >= 3 || c.colors().isEmpty())
            return exact.isEmpty() ? closest(size, c, profile, usedAi, pool)
                    : new Result(size, c, usedAi, "Mình tìm được " + exact.size() + " bộ " + what + " rất hợp với bạn nè!", exact);

        // Hết màu khách muốn → tự đề xuất màu gần nhất
        List<String> alt = StylistMatcher.alternativeColors(c.colors());
        Set<UUID> seen = new HashSet<>(exact.stream().map(m -> m.product().id()).toList());
        List<Match> alternatives = rank(pool, c.withColors(alt), profile, seen, true, MIN_MATCH);
        List<Match> merged = new ArrayList<>(exact);
        merged.addAll(alternatives.subList(0, Math.min(alternatives.size(), MAX_RESULTS - exact.size())));
        if (merged.isEmpty()) return closest(size, c, profile, usedAi, pool);
        if (alternatives.isEmpty())
            return new Result(size, c, usedAi, "Mình tìm được " + exact.size() + " bộ " + what + " hợp với bạn nè!", exact);

        String wanted = String.join(" / ", c.colors()).toLowerCase();
        String found = merged.stream().filter(Match::alternative).flatMap(m -> m.product().colors().stream())
                .filter(alt::contains).distinct().limit(2).map(String::toLowerCase).reduce((a, b) -> a + " hoặc " + b).orElse("");
        String shape = c.features().isEmpty() ? "" : " dáng " + String.join(", ", c.features()).toLowerCase();
        String msg = exact.isEmpty()
                ? "Hiện kho đồ màu " + wanted + " đang hết, nhưng mình thấy có mẫu màu " + found + shape + " rất hợp với bạn, bạn xem thử nhé!"
                : "Mình tìm được " + exact.size() + " bộ màu " + wanted + ", kèm vài mẫu màu " + found + shape + " cũng rất hợp với bạn nè!";
        return new Result(size, c, usedAi, msg, merged);
    }

    private List<Match> rank(List<Product> pool, Criteria c, Profile profile, Set<UUID> exclude, boolean alternative, double minScore) {
        return pool.stream().filter(p -> !exclude.contains(p.getId()))
                .map(p -> Map.entry(p, StylistMatcher.score(p, c)))
                .filter(e -> e.getValue() >= minScore)
                .sorted(Comparator.<Map.Entry<Product, Double>>comparingDouble(Map.Entry::getValue).reversed()
                        .thenComparing(e -> -StylistMatcher.affinity(e.getKey(), profile))
                        .thenComparing(e -> -e.getKey().getRentCount()))
                .limit(MAX_RESULTS)
                .map(e -> new Match(Summary.from(e.getKey()), (int) Math.round(e.getValue() * 100), alternative))
                .toList();
    }

    /** Không có bộ nào ≥80% → vẫn gợi ý 3 bộ gần nhất, nói rõ cho khách. */
    private Result closest(String size, Criteria c, Profile profile, boolean usedAi, List<Product> pool) {
        List<Match> near = rank(pool, c, profile, Set.of(), true, 0).stream().limit(3).toList();
        return new Result(size, c, usedAi, "Chưa có bộ nào khớp hoàn toàn yêu cầu của bạn, đây là " + near.size()
                + " mẫu gần nhất mình nghĩ bạn sẽ thích. Bạn có thể mô tả thêm để mình tìm lại nhé!", near);
    }

    private static String describe(Criteria c) {
        List<String> bits = new ArrayList<>();
        if (!c.categories().isEmpty()) bits.add(String.join("/", c.categories()).toLowerCase());
        if (!c.colors().isEmpty()) bits.add("màu " + String.join("/", c.colors()).toLowerCase());
        if (!c.styles().isEmpty()) bits.add(String.join(", ", c.styles()).toLowerCase());
        return String.join(" ", bits);
    }

    // ── Gemini: bóc tách từ khoá ─────────────────────────────────────────
    private static final String SYSTEM = """
            Bạn là trợ lý bóc tách yêu cầu thuê trang phục. Đọc câu của khách và trả về DUY NHẤT một JSON:
            {"categories":[],"colors":[],"styles":[],"occasions":[],"features":[],"maxPrice":null}
            - Chỉ dùng giá trị NGUYÊN VĂN trong các danh sách dưới; không có thì để mảng rỗng.
            - "features" là kiểu dáng/tính năng (vd: trễ vai, che bắp tay). "che bắp tay to" → "Che bắp tay".
            - Ý "kín đáo", "nổi bật" → styles. "Tone đỏ hoặc đen" → colors ["Đỏ","Đen"].
            - maxPrice: giá thuê tối đa bằng VND (vd "dưới 300k" → 300000), không nhắc thì null.
            categories: %s
            colors: %s
            styles: %s
            occasions: %s
            features: %s
            """.formatted(Vocab.CATEGORIES, Vocab.COLORS, Vocab.STYLES, Vocab.OCCASIONS, Vocab.FEATURES);

    private Criteria extractWithAi(String prompt) {
        if (prompt == null || prompt.isBlank() || aiProperties.getGeminiApiKey() == null
                || aiProperties.getGeminiApiKey().isBlank()) return null;
        for (String model : List.of(aiProperties.getChatModel(), aiProperties.getFallbackChatModel())) {
            try {
                Map<?, ?> res = geminiChatClient.post().uri("/chat/completions").body(Map.of(
                        "model", model,
                        "temperature", 0,
                        "response_format", Map.of("type", "json_object"),
                        "messages", List.of(Map.of("role", "system", "content", SYSTEM),
                                Map.of("role", "user", "content", prompt))))
                        .retrieve().body(Map.class);
                String content = (String) ((Map<?, ?>) ((Map<?, ?>) ((List<?>) res.get("choices")).get(0)).get("message")).get("content");
                Map<?, ?> j = objectMapper.readValue(content.replaceAll("(?s)^```(json)?|```$", "").trim(), Map.class);
                Object price = j.get("maxPrice");
                return new Criteria(
                        StylistMatcher.clean(j.get("categories"), Vocab.CATEGORIES),
                        StylistMatcher.clean(j.get("colors"), Vocab.COLORS),
                        StylistMatcher.clean(j.get("styles"), Vocab.STYLES),
                        StylistMatcher.clean(j.get("occasions"), Vocab.OCCASIONS),
                        StylistMatcher.clean(j.get("features"), Vocab.FEATURES),
                        price instanceof Number n && n.longValue() > 0 ? n.longValue() : null);
            } catch (Exception e) {
                log.warn("[Stylist] Gemini {} lỗi, thử model khác / dùng so khớp cục bộ: {}", model, e.getMessage());
            }
        }
        return null;
    }
}
