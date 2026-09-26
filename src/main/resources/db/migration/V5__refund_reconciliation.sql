-- Đối soát lệnh chi hoàn tiền với PayOS: PROCESSING → SUCCEEDED | FAILED
ALTER TABLE dtb_bookings
    ADD COLUMN refund_status   VARCHAR(16) CHECK (refund_status IN ('PROCESSING', 'SUCCEEDED', 'FAILED')),
    ADD COLUMN refund_attempts INT NOT NULL DEFAULT 0;
CREATE INDEX idx_bookings_refund_processing ON dtb_bookings(refund_status) WHERE refund_status = 'PROCESSING';
-- Lệnh chi đã tạo trước khi có đối soát → đưa vào hàng đợi kiểm tra
UPDATE dtb_bookings SET refund_status = 'PROCESSING', refund_attempts = 1 WHERE refund_ref IS NOT NULL;
