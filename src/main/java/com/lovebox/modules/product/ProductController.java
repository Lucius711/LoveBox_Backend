package com.lovebox.modules.product;

import com.lovebox.common.response.ApiResponse;
import com.lovebox.modules.product.ProductDtos.*;
import com.lovebox.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products/meta")
    public ApiResponse<Map<String, List<String>>> meta() {
        return ApiResponse.success(Vocab.asMap());
    }

    /** availability: available | rented; sort: new | popular | priceAsc | priceDesc */
    @GetMapping("/products")
    public ApiResponse<PageResult<Summary>> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) String style,
                                             @RequestParam(required = false) String color,
                                             @RequestParam(required = false) String size,
                                             @RequestParam(required = false) Long minPrice,
                                             @RequestParam(required = false) Long maxPrice,
                                             @RequestParam(required = false) String availability,
                                             @RequestParam(required = false) String sort,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "12") int pageSize) {
        return ApiResponse.success(productService.search(new ProductService.Filter(
                q, category, style, color, size, minPrice, maxPrice, availability, sort, page, pageSize)));
    }

    @GetMapping("/products/{id}")
    public ApiResponse<Detail> detail(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal viewer) {
        return ApiResponse.success(productService.detail(id, viewer));
    }

    @GetMapping("/products/{id}/blocked-dates")
    public ApiResponse<List<LocalDate>> blockedDates(@PathVariable UUID id) {
        return ApiResponse.success(productService.blockedDates(id));
    }

    // ── Chủ đồ (role OWNER/ADMIN — xem SecurityConfig) ─────────────────────
    @GetMapping("/owner/products")
    public ApiResponse<List<Summary>> mine(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.success(productService.mine(p.getId()));
    }

    @PostMapping("/owner/products")
    public ApiResponse<Detail> create(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ProductRequest req) {
        return ApiResponse.success("Đã gửi, chờ admin duyệt", productService.create(p.getId(), req));
    }

    @PutMapping("/owner/products/{id}")
    public ApiResponse<Detail> update(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id,
                                      @Valid @RequestBody ProductRequest req) {
        return ApiResponse.success("Đã cập nhật, chờ admin duyệt lại", productService.update(p, id, req));
    }

    @DeleteMapping("/owner/products/{id}")
    public ApiResponse<Void> hide(@AuthenticationPrincipal UserPrincipal p, @PathVariable UUID id) {
        productService.hide(p, id);
        return ApiResponse.success("Đã ẩn sản phẩm", null);
    }
}
