-- =====================================================================
-- V14: Sync remaining tables with Hibernate entity definitions
-- Covers: dtb_cart_items, dtb_showcase_designs, dtb_gift_designs,
--         dtb_greeting_wishes, dtb_order_items, dtb_shipping_info,
--         dtb_payment_transactions
-- =====================================================================

-- ─────────────────────────────────────────────
-- 1. dtb_cart_items — add unit_price
-- ─────────────────────────────────────────────
ALTER TABLE dtb_cart_items
    ADD COLUMN IF NOT EXISTS unit_price NUMERIC(12, 2) NOT NULL DEFAULT 0;

-- ─────────────────────────────────────────────
-- 2. dtb_showcase_designs — add thumbnail_url, is_active
--    (entity uses thumbnail_url instead of image_url,
--     and is_active SMALLINT instead of is_featured+status)
-- ─────────────────────────────────────────────
ALTER TABLE dtb_showcase_designs
    ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS is_active     SMALLINT NOT NULL DEFAULT 1;

-- Back-fill thumbnail_url from existing image_url so data is not lost
UPDATE dtb_showcase_designs
SET thumbnail_url = image_url
WHERE thumbnail_url IS NULL AND image_url IS NOT NULL;

-- Make thumbnail_url NOT NULL after back-fill
ALTER TABLE dtb_showcase_designs
    ALTER COLUMN thumbnail_url SET NOT NULL;

-- ─────────────────────────────────────────────
-- 3. dtb_gift_designs — add ai_generated_image_id, showcase_design_id,
--    customization_data, status
--    (entity replaced generation_id→ai_generated_image_id,
--     showcase_id→showcase_design_id, dropped image_url/title)
-- ─────────────────────────────────────────────
ALTER TABLE dtb_gift_designs
    ADD COLUMN IF NOT EXISTS ai_generated_image_id UUID,
    ADD COLUMN IF NOT EXISTS showcase_design_id    UUID,
    ADD COLUMN IF NOT EXISTS customization_data    TEXT,
    ADD COLUMN IF NOT EXISTS status                VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

-- Back-fill new FK columns from old ones
UPDATE dtb_gift_designs SET ai_generated_image_id = generation_id WHERE generation_id IS NOT NULL;
UPDATE dtb_gift_designs SET showcase_design_id    = showcase_id   WHERE showcase_id IS NOT NULL;

-- Add FK constraints on the new columns (referencing existing tables)
ALTER TABLE dtb_gift_designs
    ADD CONSTRAINT fk_gift_design_ai_gen_image
        FOREIGN KEY (ai_generated_image_id)
        REFERENCES dtb_ai_generated_images(id) ON DELETE SET NULL;

ALTER TABLE dtb_gift_designs
    ADD CONSTRAINT fk_gift_design_showcase_design
        FOREIGN KEY (showcase_design_id)
        REFERENCES dtb_showcase_designs(id) ON DELETE SET NULL;

-- ─────────────────────────────────────────────
-- 4. dtb_greeting_wishes — add recipient_name, sender_name
-- ─────────────────────────────────────────────
ALTER TABLE dtb_greeting_wishes
    ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS sender_name    VARCHAR(255);

-- Make recipient_name NOT NULL after adding with NULL (no existing data assumed dev)
ALTER TABLE dtb_greeting_wishes
    ALTER COLUMN recipient_name SET NOT NULL;

-- ─────────────────────────────────────────────
-- 5. dtb_order_items — add box_size_id, line_total
--    (unit_price, box_size_code, box_size_name already in V8)
-- ─────────────────────────────────────────────
ALTER TABLE dtb_order_items
    ADD COLUMN IF NOT EXISTS box_size_id UUID,
    ADD COLUMN IF NOT EXISTS line_total  NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE dtb_order_items
    ADD CONSTRAINT fk_order_item_box_size
        FOREIGN KEY (box_size_id) REFERENCES dtb_box_sizes(id) ON DELETE SET NULL;

-- ─────────────────────────────────────────────
-- 6. dtb_shipping_info — add address_line1, address_line2, city, delivery_note
--    (district, ward already exist; address→address_line1 back-fill)
-- ─────────────────────────────────────────────
ALTER TABLE dtb_shipping_info
    ADD COLUMN IF NOT EXISTS address_line1 VARCHAR(500),
    ADD COLUMN IF NOT EXISTS address_line2 VARCHAR(500),
    ADD COLUMN IF NOT EXISTS city          VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delivery_note VARCHAR(500);

-- Back-fill address_line1 from legacy address column
UPDATE dtb_shipping_info
SET address_line1 = address
WHERE address_line1 IS NULL AND address IS NOT NULL;

-- Back-fill city from province if empty (dev data safety)
UPDATE dtb_shipping_info SET city = province WHERE city IS NULL AND province IS NOT NULL;
UPDATE dtb_shipping_info SET city = '' WHERE city IS NULL;

-- Make address_line1 NOT NULL after back-fill
ALTER TABLE dtb_shipping_info
    ALTER COLUMN address_line1 SET NOT NULL;

-- ─────────────────────────────────────────────
-- 7. dtb_payment_transactions — add currency, payment_method,
--    vietqr_payload, receipt_image_url, receipt_uploaded_at, rejection_reason
--    (verified_at, verified_by already exist; receipt_url→receipt_image_url back-fill)
-- ─────────────────────────────────────────────
ALTER TABLE dtb_payment_transactions
    ADD COLUMN IF NOT EXISTS currency           VARCHAR(10)   NOT NULL DEFAULT 'VND',
    ADD COLUMN IF NOT EXISTS payment_method     VARCHAR(50)   NOT NULL DEFAULT 'VIETQR',
    ADD COLUMN IF NOT EXISTS vietqr_payload     TEXT,
    ADD COLUMN IF NOT EXISTS receipt_image_url  VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS receipt_uploaded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS rejection_reason   TEXT;

-- Back-fill receipt_image_url from legacy receipt_url column
UPDATE dtb_payment_transactions
SET receipt_image_url = receipt_url
WHERE receipt_image_url IS NULL AND receipt_url IS NOT NULL;
