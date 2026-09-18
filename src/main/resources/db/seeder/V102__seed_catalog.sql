-- =====================================================================
-- Love Box — Seeder V102 (Catalog master data)
-- Corresponds to: V4__create_catalog_tables.sql
-- =====================================================================

-- 1. Product Categories
INSERT INTO dtb_product_categories (id, name, slug, description, display_order)
VALUES
    (gen_random_uuid(), 'Cute Baby',   'cute-baby',   'Phong cách dễ thương, màu pastel, tươi sáng',       1),
    (gen_random_uuid(), 'Thanh lịch',  'thanh-lich',  'Phong cách tối giản, elegant, màu trung tính',      2),
    (gen_random_uuid(), 'Vintage',     'vintage',     'Phong cách cổ điển, tone màu ấm, hoài niệm',        3),
    (gen_random_uuid(), 'Tối giản',    'toi-gian',    'Minimalist, đường nét sạch sẽ, hiện đại',           4);

-- 2. Box Sizes
INSERT INTO dtb_box_sizes (id, code, name, price, description, is_active)
VALUES
    (gen_random_uuid(), 'STANDARD', 'Standard Box', 250000,
     'Hộp tiêu chuẩn — hoa thủ công + nến thơm + thiệp QR', 1),
    (gen_random_uuid(), 'DELUXE',   'Deluxe Box',   350000,
     'Hộp cao cấp — thêm phụ kiện và gói quà đặc biệt',     1);
