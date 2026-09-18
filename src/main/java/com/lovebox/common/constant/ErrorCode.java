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
    INVALID_JWT_TOKEN(4011, "JWT token không hợp lệ", HttpStatus.UNAUTHORIZED),
    EXPIRED_JWT_TOKEN(4012, "JWT token đã hết hạn", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(4013, "Refresh token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    SESSION_NOT_FOUND(4014, "Phiên đăng nhập không tồn tại", HttpStatus.UNAUTHORIZED),

    // ── User ─────────────────────────────────────────────────────────────
    USER_NOT_FOUND(4020, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),
    USER_ALREADY_DELETED(4021, "Tài khoản đã bị xóa", HttpStatus.GONE),

    // ── Catalog ──────────────────────────────────────────────────────────
    CATEGORY_NOT_FOUND(4030, "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    BOX_SIZE_NOT_FOUND(4031, "Không tìm thấy kích thước hộp", HttpStatus.NOT_FOUND),
    BOX_SIZE_INACTIVE(4032, "Kích thước hộp hiện không khả dụng", HttpStatus.BAD_REQUEST),
    SHOWCASE_DESIGN_NOT_FOUND(4033, "Không tìm thấy mẫu thiết kế", HttpStatus.NOT_FOUND),

    // ── AI ───────────────────────────────────────────────────────────────
    AI_CONVERSATION_NOT_FOUND(4040, "Không tìm thấy cuộc trò chuyện AI", HttpStatus.NOT_FOUND),
    AI_CONVERSATION_FORBIDDEN(4041, "Bạn không có quyền truy cập cuộc trò chuyện này", HttpStatus.FORBIDDEN),
    AI_IMAGE_GENERATION_FAILED(4042, "Tạo ảnh AI thất bại, vui lòng thử lại", HttpStatus.INTERNAL_SERVER_ERROR),
    AI_GENERATED_IMAGE_NOT_FOUND(4043, "Không tìm thấy ảnh AI đã tạo", HttpStatus.NOT_FOUND),
    AI_SERVICE_UNAVAILABLE(4044, "Dịch vụ AI hiện không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),

    // ── Gift Design ──────────────────────────────────────────────────────
    GIFT_DESIGN_NOT_FOUND(4050, "Không tìm thấy thiết kế hộp quà", HttpStatus.NOT_FOUND),
    GIFT_DESIGN_FORBIDDEN(4051, "Bạn không có quyền truy cập thiết kế này", HttpStatus.FORBIDDEN),
    INVALID_GIFT_DESIGN_SOURCE(4052, "Nguồn thiết kế không hợp lệ", HttpStatus.BAD_REQUEST),

    // ── Greeting Wish ─────────────────────────────────────────────────────
    GREETING_WISH_NOT_FOUND(4055, "Không tìm thấy lời chúc", HttpStatus.NOT_FOUND),
    GREETING_WISH_FORBIDDEN(4056, "Bạn không có quyền truy cập lời chúc này", HttpStatus.FORBIDDEN),
    INVALID_QR_TOKEN(4057, "QR token không hợp lệ", HttpStatus.NOT_FOUND),

    // ── Cart ─────────────────────────────────────────────────────────────
    CART_NOT_FOUND(4060, "Không tìm thấy giỏ hàng", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND(4061, "Không tìm thấy sản phẩm trong giỏ hàng", HttpStatus.NOT_FOUND),
    CART_EMPTY(4062, "Giỏ hàng trống", HttpStatus.BAD_REQUEST),
    CART_ITEM_QUANTITY_INVALID(4063, "Số lượng sản phẩm không hợp lệ", HttpStatus.BAD_REQUEST),

    // ── Order ────────────────────────────────────────────────────────────
    ORDER_NOT_FOUND(4070, "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    ORDER_FORBIDDEN(4071, "Bạn không có quyền truy cập đơn hàng này", HttpStatus.FORBIDDEN),
    ORDER_STATUS_TRANSITION_INVALID(4072, "Không thể chuyển trạng thái đơn hàng", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_CANCELLED(4073, "Đơn hàng đã bị hủy", HttpStatus.BAD_REQUEST),
    ORDER_CANNOT_CANCEL(4074, "Đơn hàng không thể hủy ở trạng thái hiện tại", HttpStatus.BAD_REQUEST),

    // ── Payment ──────────────────────────────────────────────────────────
    PAYMENT_TRANSACTION_NOT_FOUND(4080, "Không tìm thấy giao dịch thanh toán", HttpStatus.NOT_FOUND),
    PAYMENT_RECEIPT_ALREADY_UPLOADED(4081, "Biên lai đã được tải lên trước đó", HttpStatus.CONFLICT),
    PAYMENT_ALREADY_VERIFIED(4082, "Giao dịch đã được xác minh", HttpStatus.CONFLICT),
    PAYMENT_RECEIPT_REQUIRED(4083, "Vui lòng tải lên biên lai thanh toán", HttpStatus.BAD_REQUEST),

    // ── Notification ─────────────────────────────────────────────────────
    NOTIFICATION_NOT_FOUND(4090, "Không tìm thấy thông báo", HttpStatus.NOT_FOUND),
    NOTIFICATION_SEND_FAILED(4091, "Gửi thông báo thất bại", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
