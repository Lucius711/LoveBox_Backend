-- Thanh lý đồ cũ: mỗi món hoặc cho thuê (RENT) hoặc bán đứt (SALE). Đơn mua dùng chung bảng bookings (kind = SALE).
ALTER TABLE dtb_products
    ADD COLUMN listing_type VARCHAR(8) NOT NULL DEFAULT 'RENT' CHECK (listing_type IN ('RENT', 'SALE')),
    ADD COLUMN sale_price   BIGINT     NOT NULL DEFAULT 0 CHECK (sale_price >= 0);

-- Bỏ CHECK cũ của status + rent_price_per_day (tên do Postgres tự đặt → tìm theo định nghĩa)
DO $$
DECLARE c record;
BEGIN
    FOR c IN SELECT conname FROM pg_constraint
             WHERE conrelid = 'dtb_products'::regclass AND contype = 'c'
               AND (pg_get_constraintdef(oid) LIKE '%rent_price_per_day%' OR pg_get_constraintdef(oid) LIKE '%(status)%')
    LOOP
        EXECUTE format('ALTER TABLE dtb_products DROP CONSTRAINT %I', c.conname);
    END LOOP;
END $$;

ALTER TABLE dtb_products
    ADD CONSTRAINT chk_products_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'HIDDEN', 'SOLD')),
    ADD CONSTRAINT chk_products_price CHECK (CASE listing_type WHEN 'SALE' THEN sale_price > 0 ELSE rent_price_per_day > 0 END);

ALTER TABLE dtb_bookings
    ADD COLUMN kind VARCHAR(8) NOT NULL DEFAULT 'RENT' CHECK (kind IN ('RENT', 'SALE'));
CREATE INDEX idx_products_listing ON dtb_products(listing_type, status);
