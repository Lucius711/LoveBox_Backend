package com.lovebox.modules.booking;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Luật tính tiền + khoá lịch. Thuần tuý (không DB) để test được. */
public final class RentalRules {
    private RentalRules() {}

    /** Ngày đệm sau mỗi lượt thuê để giặt ủi / vận chuyển. */
    public static final int BUFFER_DAYS = 1;
    public static final int MAX_DAYS = 30;
    public static final long EXPRESS_FEE = 30_000;

    /** Thuê 10/10 → 12/10 = 3 ngày (tính cả ngày nhận và ngày trả). */
    public static int days(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    public static long shippingFee(String deliveryMethod) {
        return "EXPRESS".equals(deliveryMethod) ? EXPRESS_FEE : 0;
    }

    /** Khoảng bị khoá của 1 booking: [start, end + BUFFER]. Hai booking xung đột khi khoảng khoá giao nhau. */
    public static boolean overlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        return !aStart.isAfter(bEnd.plusDays(BUFFER_DAYS)) && !bStart.isAfter(aEnd.plusDays(BUFFER_DAYS));
    }

    public static List<LocalDate> blockedDays(LocalDate start, LocalDate end) {
        return start.datesUntil(end.plusDays(BUFFER_DAYS + 1)).toList();
    }

    public static void validateDates(LocalDate start, LocalDate end, LocalDate today) {
        if (start == null || end == null || start.isBefore(today) || end.isBefore(start)
                || days(start, end) > MAX_DAYS) {
            throw new IllegalArgumentException("Ngày thuê không hợp lệ (từ hôm nay, tối đa " + MAX_DAYS + " ngày)");
        }
    }

    // ── Trạng thái đơn ────────────────────────────────────────────────────
    // PENDING(Chờ xác nhận) → CONFIRMED → SHIPPING(Đang giao) → RENTED(Đang thuê) → RETURNED(Đã trả đồ)
    //   → COMPLETED(Đã hoàn cọc) | DISPUTED(Tranh chấp) → COMPLETED
    public static final Map<String, Set<String>> OWNER_TRANSITIONS = Map.of(
            "PENDING", Set.of("CONFIRMED", "CANCELLED"),
            "CONFIRMED", Set.of("SHIPPING", "RENTED"),   // RENTED trực tiếp khi khách tự đến lấy
            "SHIPPING", Set.of("RENTED"),
            "RENTED", Set.of("RETURNED"));

    public static final Map<String, Set<String>> ADMIN_EXTRA = Map.of(
            "RETURNED", Set.of("COMPLETED", "DISPUTED"),
            "DISPUTED", Set.of("COMPLETED"),
            "CONFIRMED", Set.of("CANCELLED"),
            "SHIPPING", Set.of("CANCELLED"));

    public static boolean canTransition(String from, String to, boolean admin) {
        if (OWNER_TRANSITIONS.getOrDefault(from, Set.of()).contains(to)) return true;
        return admin && ADMIN_EXTRA.getOrDefault(from, Set.of()).contains(to);
    }
}
