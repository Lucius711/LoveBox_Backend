package com.lovebox.modules.stylist;

import com.lovebox.modules.product.Product;
import com.lovebox.modules.product.Vocab;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Logic thuần của trợ lý AI: quy đổi size, bóc từ khoá dự phòng, chấm điểm độ khớp. */
public final class StylistMatcher {
    private StylistMatcher() {}

    public record Criteria(List<String> categories, List<String> colors, List<String> styles,
                           List<String> occasions, List<String> features, Long maxPrice) {
        public Criteria withColors(List<String> c) { return new Criteria(categories, c, styles, occasions, features, maxPrice); }
        /** Giá trong câu chat thắng; không nhắc giá thì lấy ngân sách trong hồ sơ. */
        public Criteria orMaxPrice(Long p) { return maxPrice != null ? this : new Criteria(categories, colors, styles, occasions, features, p); }
    }

    /** Sở thích trong hồ sơ — chỉ dùng để XẾP HẠNG (ưu tiên), không loại đồ. */
    public record Profile(List<String> occasions, List<String> styles, List<String> colors, List<String> fitFeatures) {
        public static Profile of(List<String> occasions, List<String> styles, List<String> colors, String fitNote) {
            return new Profile(occasions, styles, colors, extractLocal(fitNote).features());
        }
    }

    /** Số điểm trùng giữa món đồ và sở thích trong hồ sơ (dịp, phong cách, màu, lưu ý dáng như "che bắp tay"). */
    public static int affinity(Product p, Profile f) {
        return overlap(f.occasions(), p.tagValues(Product.OCCASION)) + overlap(f.styles(), p.tagValues(Product.STYLE))
                + overlap(f.colors(), p.tagValues(Product.COLOR)) + overlap(f.fitFeatures(), p.tagValues(Product.FEATURE));
    }

    private static int overlap(List<String> a, List<String> b) {
        return (int) a.stream().filter(b::contains).count();
    }

    public record Body(Integer heightCm, Integer weightKg, Integer bust, Integer waist, Integer hip) {}

    private static final List<String> SIZES = Vocab.SIZES;

    /** Chiều cao + cân nặng → size chuẩn (bảng size nữ phổ biến ở VN). */
    public static String sizeFor(Integer heightCm, Integer weightKg) {
        if (weightKg == null) return null;
        int i = weightKg <= 47 ? 0 : weightKg <= 54 ? 1 : weightKg <= 62 ? 2 : 3;
        if (heightCm != null && heightCm >= 168 && i < 3) i++;   // người cao → lên 1 size cho đủ dài
        return SIZES.get(i);
    }

    /** Có số đo 3 vòng → so với số đo tối đa của đồ; không có → so size (vừa hoặc rộng hơn 1 size). */
    public static boolean fits(Product p, Body b, String size) {
        if (b.bust() != null || b.waist() != null || b.hip() != null) {
            return (b.bust() == null || p.getBustMax() >= b.bust())
                    && (b.waist() == null || p.getWaistMax() >= b.waist())
                    && (b.hip() == null || p.getHipMax() >= b.hip());
        }
        if (size == null) return true;
        int diff = SIZES.indexOf(p.getSize()) - SIZES.indexOf(size);
        return diff == 0 || diff == 1;
    }

    /** Độ khớp 0..1 = trung bình trên các tiêu chí khách có nhắc tới. */
    public static double score(Product p, Criteria c) {
        List<Double> parts = new ArrayList<>();
        if (!c.categories().isEmpty()) parts.add(c.categories().contains(p.getCategory()) ? 1.0 : 0.0);
        addAny(parts, c.colors(), p.tagValues(Product.COLOR));
        addAny(parts, c.styles(), p.tagValues(Product.STYLE));
        addAny(parts, c.occasions(), p.tagValues(Product.OCCASION));
        if (!c.features().isEmpty()) {   // nhiều yêu cầu kiểu dáng → tính theo tỉ lệ đáp ứng
            List<String> have = p.tagValues(Product.FEATURE);
            parts.add(c.features().stream().filter(have::contains).count() / (double) c.features().size());
        }
        return parts.isEmpty() ? 1.0 : parts.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
    }

    private static void addAny(List<Double> parts, List<String> wanted, List<String> have) {
        if (!wanted.isEmpty()) parts.add(wanted.stream().anyMatch(have::contains) ? 1.0 : 0.0);
    }

    // ── Dự phòng khi không có / lỗi Gemini: so chuỗi với bộ nhãn chuẩn ─────
    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("dạ hội", "Đầm dạ hội"), Map.entry("vest", "Veston"), Map.entry("cosplay", "Đồ Cosplay/Sự kiện"),
            Map.entry("cưới", "Đám cưới"), Map.entry("tiệc", "Đi tiệc"), Map.entry("ngoại cảnh", "Chụp ảnh ngoại cảnh"),
            Map.entry("cute", "Dễ thương"), Map.entry("elegant", "Thanh lịch"), Map.entry("edgy", "Cá tính"),
            Map.entry("kín", "Kín đáo"), Map.entry("nổi", "Nổi bật"), Map.entry("che tay", "Che bắp tay"),
            Map.entry("bắp tay", "Che bắp tay"), Map.entry("đỏ rượu", "Đỏ đô"), Map.entry("burgundy", "Đỏ đô"),
            Map.entry("hồng phấn", "Hồng pastel"), Map.entry("navy", "Xanh navy"), Map.entry("xẻ", "Xẻ tà"));

    private static final Pattern PRICE = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(k|nghìn|ngàn|tr|triệu)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public static Criteria extractLocal(String text) {
        String t = text == null ? "" : java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        return new Criteria(find(t, Vocab.CATEGORIES), find(t, Vocab.COLORS), find(t, Vocab.STYLES),
                find(t, Vocab.OCCASIONS), find(t, Vocab.FEATURES), parsePrice(t));
    }

    private static List<String> find(String t, List<String> vocab) {
        Set<String> hits = new LinkedHashSet<>();
        for (String v : vocab) if (t.contains(v.toLowerCase(Locale.ROOT))) hits.add(v);
        SYNONYMS.forEach((k, v) -> { if (vocab.contains(v) && t.contains(k)) hits.add(v); });
        // "đỏ đô" cũng chứa "đỏ" → bỏ giá trị ngắn nằm trong giá trị dài hơn đã khớp
        hits.removeIf(h -> hits.stream().anyMatch(o -> !o.equals(h)
                && o.toLowerCase(Locale.ROOT).contains(h.toLowerCase(Locale.ROOT))));
        return List.copyOf(hits);
    }

    private static final Pattern HEIGHT = Pattern.compile("(?<![\\d.])(1[3-9]\\d)\\s*cm|\\b1m\\s*(\\d{1,2})\\b");
    private static final Pattern WEIGHT = Pattern.compile("(?<![\\d.])(\\d{2,3})\\s*kg");

    /** "160cm" / "1m6" / "1m65" → cm. */
    public static Integer parseHeight(String t) {
        if (t == null) return null;
        Matcher m = HEIGHT.matcher(t.toLowerCase(Locale.ROOT));
        if (!m.find()) return null;
        if (m.group(1) != null) return Integer.parseInt(m.group(1));
        String d = m.group(2);
        return 100 + Integer.parseInt(d.length() == 1 ? d + "0" : d);
    }

    public static Integer parseWeight(String t) {
        if (t == null) return null;
        Matcher m = WEIGHT.matcher(t.toLowerCase(Locale.ROOT));
        if (!m.find()) return null;
        int w = Integer.parseInt(m.group(1));
        return w >= 30 && w <= 150 ? w : null;
    }

    static Long parsePrice(String t) {
        Matcher m = PRICE.matcher(t);
        if (!m.find()) return null;
        double n = Double.parseDouble(m.group(1).replace(',', '.'));
        String unit = m.group(2).toLowerCase(Locale.ROOT);
        return Math.round(n * (unit.startsWith("t") ? 1_000_000 : 1_000));
    }

    /** Giữ lại đúng các giá trị có trong bộ nhãn (AI có thể bịa). */
    public static List<String> clean(Object raw, List<String> vocab) {
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream().map(String::valueOf).filter(vocab::contains).distinct().toList();
    }

    public static List<String> alternativeColors(List<String> colors) {
        Set<String> alt = new LinkedHashSet<>();
        colors.forEach(c -> alt.addAll(Vocab.COLOR_FAMILY.getOrDefault(c, List.of())));
        alt.removeAll(colors);
        return List.copyOf(alt);
    }
}
