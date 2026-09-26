package com.lovebox.common.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ── Generic ──────────────────────────────────────────────────────────
    INTERNAL_SERVER_ERROR(5000, "Lỗi hệ thống, vui lòng thử lại sau", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(4000, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(4004, "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),
    FORBIDDEN(4003, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    UNAUTHORIZED(4001, "Vui lòng đăng nhập để tiếp tục", HttpStatus.UNAUTHORIZED),

    // ── Auth ─────────────────────────────────────────────────────────────
    INVALID_GOOGLE_TOKEN(4010, "Google token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(4013, "Refresh token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),

    // ── User ─────────────────────────────────────────────────────────────
    USER_NOT_FOUND(4020, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USER_ALREADY_DELETED(4021, "Tài khoản đã bị xóa", HttpStatus.GONE),

    // ── Product ──────────────────────────────────────────────────────────
    PRODUCT_NOT_FOUND(4030, "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_AVAILABLE(4031, "Sản phẩm chưa được duyệt hoặc đã ẩn", HttpStatus.BAD_REQUEST),
    INVALID_FILE(4032, "File ảnh không hợp lệ (chỉ nhận JPG/PNG/WEBP, tối đa 5MB)", HttpStatus.BAD_REQUEST),

    // ── Booking ──────────────────────────────────────────────────────────
    BOOKING_NOT_FOUND(4070, "Không tìm thấy đơn thuê", HttpStatus.NOT_FOUND),
    BOOKING_DATE_CONFLICT(4071, "Ngày bạn chọn đã có người thuê (kèm 1 ngày giặt ủi). Vui lòng chọn ngày khác", HttpStatus.CONFLICT),
    BOOKING_INVALID_DATES(4072, "Ngày thuê không hợp lệ", HttpStatus.BAD_REQUEST),
    BOOKING_STATUS_TRANSITION_INVALID(4073, "Không thể chuyển trạng thái đơn thuê", HttpStatus.BAD_REQUEST),
    BOOKING_OWN_PRODUCT(4074, "Bạn không thể thuê đồ của chính mình", HttpStatus.BAD_REQUEST),
    REVIEW_NOT_ALLOWED(4075, "Chỉ đánh giá được sau khi đã trả đồ, mỗi đơn 1 lần", HttpStatus.BAD_REQUEST),
    REFUND_FAILED(5020, "Chưa thể hoàn tiền", HttpStatus.BAD_REQUEST);   // vd khách chưa có STK nhận hoàn

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
