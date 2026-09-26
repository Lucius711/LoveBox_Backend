package com.lovebox.modules.product;

import java.util.List;
import java.util.Map;

/**
 * Bộ nhãn chuẩn — form đăng đồ bắt buộc chọn từ đây, AI cũng chỉ trích xuất trong phạm vi này
 * nên việc so khớp (matching) luôn là so sánh chuỗi chính xác.
 */
public final class Vocab {
    private Vocab() {}

    public static final List<String> CATEGORIES = List.of(
            "Áo dài", "Đầm dạ hội", "Váy dự tiệc", "Veston", "Đồ Cosplay/Sự kiện", "Áo cưới", "Set công sở", "Đầm công sở");
    public static final List<String> STYLES = List.of(
            "Dễ thương", "Thanh lịch", "Cá tính", "Vintage", "Sexy", "Nàng thơ", "Sang trọng", "Kín đáo", "Nổi bật");
    public static final List<String> COLORS = List.of(
            "Đỏ", "Đỏ đô", "Hồng", "Hồng đất", "Hồng pastel", "Cam", "Vàng", "Be", "Nâu", "Xanh lá",
            "Xanh mint", "Xanh dương", "Xanh navy", "Tím", "Đen", "Trắng", "Xám", "Bạc", "Kem", "Nhiều màu");
    public static final List<String> OCCASIONS = List.of(
            "Đi tiệc", "Kỷ yếu", "Prom", "Đám cưới", "Chụp ảnh ngoại cảnh", "Sinh nhật", "Thuyết trình", "Lễ hội/Sự kiện");
    public static final List<String> FEATURES = List.of(
            "Trễ vai", "Hở lưng", "Cổ yếm", "Cúp ngực", "Tay dài", "Tay phồng", "Che bắp tay", "Xẻ tà",
            "Dáng xoè", "Dáng suông", "Ôm body", "Đuôi cá", "Dài chấm gót", "Ngắn trên gối", "Đính kết", "Ren");
    public static final List<String> CONDITIONS = List.of(
            "Mới 100%", "Mới 99%", "Đã mặc 1 lần", "Đã mặc vài lần", "Còn tốt 90%");
    public static final List<String> SIZES = List.of("S", "M", "L", "XL");

    /** Ngân hàng nhận hoàn cọc → mã BIN NAPAS (lưu kèm STK để admin chuyển khoản tay). Thêm ngân hàng: thêm 1 dòng. */
    public static final Map<String, String> BANKS = new java.util.LinkedHashMap<>();
    static {
        BANKS.put("Vietcombank", "970436"); BANKS.put("VietinBank", "970415"); BANKS.put("BIDV", "970418");
        BANKS.put("Agribank", "970405"); BANKS.put("Techcombank", "970407"); BANKS.put("MB Bank", "970422");
        BANKS.put("ACB", "970416"); BANKS.put("VPBank", "970432"); BANKS.put("TPBank", "970423");
        BANKS.put("Sacombank", "970403"); BANKS.put("HDBank", "970437"); BANKS.put("VIB", "970441");
        BANKS.put("SHB", "970443"); BANKS.put("OCB", "970448"); BANKS.put("MSB", "970426");
        BANKS.put("SeABank", "970440"); BANKS.put("Eximbank", "970431"); BANKS.put("LPBank", "970449");
    }

    /** Màu gần nhau — dùng để AI đề xuất thay thế khi hết màu khách muốn. */
    public static final Map<String, List<String>> COLOR_FAMILY = Map.ofEntries(
            Map.entry("Đỏ đô", List.of("Đỏ", "Hồng đất", "Nâu")),
            Map.entry("Đỏ", List.of("Đỏ đô", "Hồng", "Cam")),
            Map.entry("Hồng", List.of("Hồng pastel", "Hồng đất", "Đỏ")),
            Map.entry("Hồng đất", List.of("Đỏ đô", "Hồng", "Nâu")),
            Map.entry("Hồng pastel", List.of("Hồng", "Kem", "Trắng")),
            Map.entry("Cam", List.of("Vàng", "Đỏ", "Hồng đất")),
            Map.entry("Vàng", List.of("Cam", "Kem", "Be")),
            Map.entry("Be", List.of("Kem", "Nâu", "Trắng")),
            Map.entry("Nâu", List.of("Be", "Đỏ đô", "Hồng đất")),
            Map.entry("Xanh lá", List.of("Xanh mint", "Xanh dương")),
            Map.entry("Xanh mint", List.of("Xanh lá", "Xanh dương", "Trắng")),
            Map.entry("Xanh dương", List.of("Xanh navy", "Xanh mint", "Tím")),
            Map.entry("Xanh navy", List.of("Xanh dương", "Đen", "Tím")),
            Map.entry("Tím", List.of("Hồng", "Xanh navy", "Xanh dương")),
            Map.entry("Đen", List.of("Xanh navy", "Xám", "Đỏ đô")),
            Map.entry("Trắng", List.of("Kem", "Be", "Bạc")),
            Map.entry("Xám", List.of("Bạc", "Đen", "Trắng")),
            Map.entry("Bạc", List.of("Xám", "Trắng")),
            Map.entry("Kem", List.of("Trắng", "Be", "Vàng")),
            Map.entry("Nhiều màu", List.of()));

    public static Map<String, List<String>> asMap() {
        return Map.of("categories", CATEGORIES, "styles", STYLES, "colors", COLORS, "occasions", OCCASIONS,
                "features", FEATURES, "conditions", CONDITIONS, "sizes", SIZES, "banks", List.copyOf(BANKS.keySet()));
    }
}
