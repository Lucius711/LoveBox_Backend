package com.lovebox;

import com.lovebox.modules.booking.RentalRules;
import com.lovebox.modules.product.Product;
import com.lovebox.modules.stylist.StylistMatcher;
import com.lovebox.modules.stylist.StylistMatcher.Body;
import com.lovebox.modules.stylist.StylistMatcher.Criteria;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Check logic lõi: khoá lịch + ngày đệm, tiền, chuyển trạng thái, quy đổi size, bóc từ khoá, chấm điểm. */
class RentalLogicTest {

    static LocalDate d(int day) { return LocalDate.of(2026, 10, day); }

    @Test
    void calendarBlocksOneBufferDay() {
        // Khách A thuê 10→12/10 → khoá 10→13/10
        assertEquals(List.of(d(10), d(11), d(12), d(13)), RentalRules.blockedDays(d(10), d(12)));
        assertTrue(RentalRules.overlaps(d(10), d(12), d(13), d(15)));   // B nhận 13/10: ngày giặt ủi → trùng
        assertFalse(RentalRules.overlaps(d(10), d(12), d(14), d(15)));  // B nhận 14/10: OK
        assertTrue(RentalRules.overlaps(d(10), d(12), d(7), d(9)));     // B trả 9/10, cần đệm 10/10 → trùng
        assertFalse(RentalRules.overlaps(d(10), d(12), d(5), d(8)));
    }

    @Test
    void money() {
        assertEquals(3, RentalRules.days(d(10), d(12)));
        assertEquals(1, RentalRules.days(d(10), d(10)));
        assertEquals(30_000, RentalRules.shippingFee("EXPRESS"));
        assertEquals(0, RentalRules.shippingFee("PICKUP"));
        Product p = product("S", 84, 64, 90);
        p.setRetailPrice(3_000_000); p.setDepositPercent(60);
        assertEquals(1_800_000, p.deposit());
        assertThrows(IllegalArgumentException.class, () -> RentalRules.validateDates(d(12), d(10), d(1)));
        assertThrows(IllegalArgumentException.class, () -> RentalRules.validateDates(d(1), d(3), d(2)));
    }

    @Test
    void statusTransitions() {
        assertTrue(RentalRules.canTransition("PENDING", "CONFIRMED", false));
        assertFalse(RentalRules.canTransition("RETURNED", "COMPLETED", false));  // hoàn cọc chỉ admin
        assertTrue(RentalRules.canTransition("RETURNED", "COMPLETED", true));
        assertFalse(RentalRules.canTransition("COMPLETED", "CANCELLED", true));
    }

    @Test
    void sizeFromHeightWeight() {
        assertEquals("S", StylistMatcher.sizeFor(155, 45));
        assertEquals("M", StylistMatcher.sizeFor(160, 50));
        assertEquals("L", StylistMatcher.sizeFor(170, 50));   // cao → lên size
        assertEquals("XL", StylistMatcher.sizeFor(165, 70));
        assertNull(StylistMatcher.sizeFor(160, null));
    }

    @Test
    void localKeywordExtraction() {
        Criteria c = StylistMatcher.extractLocal("Mình muốn tìm đầm màu đỏ đô, trễ vai, che được bắp tay to, giá thuê dưới 300k");
        assertEquals(List.of("Đỏ đô"), c.colors());
        assertTrue(c.features().containsAll(List.of("Trễ vai", "Che bắp tay")));
        assertEquals(300_000L, c.maxPrice());
        assertEquals(List.of("Đỏ", "Đen"), StylistMatcher.extractLocal("tone màu đỏ hoặc đen").colors());
        assertEquals(1_500_000L, StylistMatcher.extractLocal("tầm 1.5tr").maxPrice());
        assertEquals(160, StylistMatcher.parseHeight("mình 1m6, 50kg"));
        assertEquals(165, StylistMatcher.parseHeight("cao 165 cm"));
        assertEquals(50, StylistMatcher.parseWeight("mình 1m6, 50kg"));
        assertNull(StylistMatcher.parseHeight("dưới 300k"));
        assertNull(StylistMatcher.extractLocal("50kg").maxPrice());
    }

    @Test
    void scoringAndFit() {
        Product p = product("M", 88, 70, 94);
        p.getTags().addAll(List.of(new Product.Tag("COLOR", "Đỏ"), new Product.Tag("FEATURE", "Trễ vai"),
                new Product.Tag("FEATURE", "Che bắp tay"), new Product.Tag("OCCASION", "Prom")));
        Criteria want = new Criteria(List.of(), List.of("Đỏ đô"), List.of(), List.of("Prom"), List.of("Trễ vai", "Che bắp tay"), null);
        assertEquals(2 / 3.0, StylistMatcher.score(p, want), 1e-9);                       // sai màu
        assertEquals(1.0, StylistMatcher.score(p, want.withColors(StylistMatcher.alternativeColors(want.colors()))), 1e-9);
        assertTrue(StylistMatcher.fits(p, new Body(null, null, null, null, null), "M"));
        assertTrue(StylistMatcher.fits(p, new Body(null, null, null, null, null), "S"));   // rộng hơn 1 size vẫn được
        assertFalse(StylistMatcher.fits(p, new Body(null, null, null, null, null), "L"));
        assertFalse(StylistMatcher.fits(p, new Body(null, null, 86, 72, null), "M"));     // eo 72 > eo tối đa 70
    }

    @Test
    void profilePersonalization() {
        Product p = product("M", 88, 70, 94);
        p.getTags().addAll(List.of(new Product.Tag("STYLE", "Nàng thơ"), new Product.Tag("FEATURE", "Che bắp tay")));
        var prof = StylistMatcher.Profile.of(List.of("Prom"), List.of("Nàng thơ"), List.of(), "bắp tay hơi to, muốn che bắp tay");
        assertEquals(2, StylistMatcher.affinity(p, prof));
        Criteria none = StylistMatcher.extractLocal(null);
        assertEquals(1.0, StylistMatcher.score(p, none), 1e-9);                            // câu trống → không loại món nào
        assertEquals(500_000L, none.orMaxPrice(500_000L).maxPrice());                       // lấy ngân sách hồ sơ
        assertEquals(300_000L, StylistMatcher.extractLocal("dưới 300k").orMaxPrice(500_000L).maxPrice()); // câu chat thắng
    }

    static Product product(String size, int bust, int waist, int hip) {
        Product p = new Product();
        p.setSize(size); p.setBustMax(bust); p.setWaistMax(waist); p.setHipMax(hip);
        return p;
    }
}
