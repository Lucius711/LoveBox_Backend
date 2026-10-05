package com.lovebox.modules.admin;

import com.lovebox.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Lượt truy cập (khách duy nhất/ngày) và số người thực sự đặt thuê — màn "Truy cập & đặt thuê" của admin. */
@RestController
@RequiredArgsConstructor
public class TrafficController {

    private static final String TODAY = "(now() at time zone 'Asia/Ho_Chi_Minh')::date";
    private static final String BOOKED_DAY = "(b.created_at at time zone 'Asia/Ho_Chi_Minh')::date";

    private final JdbcTemplate jdbc;

    /** Public (SecurityConfig). FE gọi 1 lần mỗi lần mở web; trùng trong ngày thì bỏ qua. */
    // ponytail: không chống bot/spam visitor_id giả — thêm rate-limit theo IP nếu số liệu bị bơm
    @PostMapping("/track")
    public ApiResponse<Void> track(@RequestBody Map<String, String> body) {
        String vid = body.get("vid");
        if (vid != null && !vid.isBlank() && vid.length() <= 64)
            jdbc.update("insert into dtb_daily_visitors(day, visitor_id) values (" + TODAY + ", ?) on conflict do nothing", vid);
        return ApiResponse.success(null);
    }

    /** /admin/** → chỉ ADMIN. Người đặt thuê = renter có đơn đã thanh toán (PAID, không huỷ), tính theo ngày tạo đơn. */
    @GetMapping("/admin/traffic")
    public ApiResponse<Map<String, Object>> traffic(@RequestParam(defaultValue = "30") int days) {
        int n = Math.max(1, Math.min(days, 365));
        String from = TODAY + " - " + (n - 1);
        List<Map<String, Object>> daily = jdbc.queryForList("""
                select to_char(d.day, 'DD/MM/YYYY') as day,
                  (select count(*) from dtb_daily_visitors v where v.day = d.day) visitors,
                  (select count(distinct b.renter_id) from dtb_bookings b where b.payment_status = 'PAID' and b.status <> 'CANCELLED' and %2$s = d.day) renters,
                  (select count(*) from dtb_bookings b where b.payment_status = 'PAID' and b.status <> 'CANCELLED' and %2$s = d.day) bookings
                from (select generate_series(%1$s, %3$s, interval '1 day')::date as day) d
                order by d.day desc""".formatted(from, BOOKED_DAY, TODAY));
        Map<String, Object> total = jdbc.queryForMap("""
                select (select count(distinct visitor_id) from dtb_daily_visitors where day >= %1$s) visitors,
                       (select count(distinct renter_id) from dtb_bookings b where b.payment_status = 'PAID' and b.status <> 'CANCELLED' and %2$s >= %1$s) renters,
                       (select count(*) from dtb_bookings b where b.payment_status = 'PAID' and b.status <> 'CANCELLED' and %2$s >= %1$s) bookings
                """.formatted(from, BOOKED_DAY));
        return ApiResponse.success(Map.of("total", total, "daily", daily));
    }
}
