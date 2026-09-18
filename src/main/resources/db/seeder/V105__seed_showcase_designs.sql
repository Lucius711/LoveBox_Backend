-- =====================================================================
-- Love Box — Seeder V105 (Showcase designs)
-- Corresponds to: V4__create_catalog_tables.sql (dtb_showcase_designs)
-- =====================================================================

INSERT INTO dtb_showcase_designs (id, category_id, box_size_id, name, image_url, is_featured, display_order, status)
SELECT
    gen_random_uuid(),
    c.id,
    b.id,
    d.name,
    d.image_url,
    d.is_featured,
    d.display_order,
    'ACTIVE'
FROM (VALUES
    ('cute-baby',  'STANDARD', 'Bunny Dream',   'https://cdn.lovebox.vn/showcase/cute-baby-01.jpg',  1, 1),
    ('cute-baby',  'DELUXE',   'Pastel Garden', 'https://cdn.lovebox.vn/showcase/cute-baby-02.jpg',  0, 2),
    ('thanh-lich', 'STANDARD', 'Pearl White',   'https://cdn.lovebox.vn/showcase/thanh-lich-01.jpg', 1, 1),
    ('thanh-lich', 'DELUXE',   'Midnight Rose', 'https://cdn.lovebox.vn/showcase/thanh-lich-02.jpg', 0, 2),
    ('vintage',    'STANDARD', 'Rustic Bloom',  'https://cdn.lovebox.vn/showcase/vintage-01.jpg',    1, 1),
    ('vintage',    'DELUXE',   'Golden Hour',   'https://cdn.lovebox.vn/showcase/vintage-02.jpg',    0, 2),
    ('toi-gian',   'STANDARD', 'Clean Lines',   'https://cdn.lovebox.vn/showcase/toi-gian-01.jpg',   1, 1),
    ('toi-gian',   'DELUXE',   'Mono Floral',   'https://cdn.lovebox.vn/showcase/toi-gian-02.jpg',   0, 2)
) AS d(slug, box_code, name, image_url, is_featured, display_order)
JOIN dtb_product_categories c ON c.slug  = d.slug
JOIN dtb_box_sizes           b ON b.code  = d.box_code;
