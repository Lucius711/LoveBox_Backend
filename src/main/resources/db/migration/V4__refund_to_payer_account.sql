-- Checkout không hỏi ngân hàng nữa: hoàn tiền về đúng tài khoản đã quét QR (PayOS webhook trả về),
-- COD / webhook không có thông tin → dùng STK trong hồ sơ người thuê lúc hoàn.
ALTER TABLE dtb_bookings
    ALTER COLUMN refund_bank_account DROP NOT NULL,
    ALTER COLUMN refund_bank_name DROP NOT NULL,
    ADD COLUMN refund_bank_bin VARCHAR(10);
