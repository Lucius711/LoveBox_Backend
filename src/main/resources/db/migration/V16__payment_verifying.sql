-- Bỏ PayOS, dùng QR tĩnh: khách bấm "Tôi đã chuyển khoản" → VERIFYING (chờ admin đối soát sao kê) → PAID
-- Bỏ CHECK cũ của payment_status (tên do Postgres tự đặt → tìm theo định nghĩa)
DO $$
DECLARE c RECORD;
BEGIN
    FOR c IN SELECT conname FROM pg_constraint
             WHERE conrelid = 'dtb_bookings'::regclass AND contype = 'c'
               AND pg_get_constraintdef(oid) LIKE '%payment_status%'
    LOOP
        EXECUTE format('ALTER TABLE dtb_bookings DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE dtb_bookings
    ADD CONSTRAINT chk_bookings_payment_status CHECK (payment_status IN ('UNPAID', 'VERIFYING', 'PAID', 'REFUNDED'));
