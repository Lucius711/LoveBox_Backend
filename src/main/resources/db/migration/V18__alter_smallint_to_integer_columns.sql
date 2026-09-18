-- =====================================================================
-- V18: Cast SMALLINT display_order columns to INTEGER to match entities
-- =====================================================================

ALTER TABLE dtb_product_categories
    ALTER COLUMN display_order TYPE INTEGER USING display_order::INTEGER;

ALTER TABLE dtb_showcase_designs
    ALTER COLUMN display_order TYPE INTEGER USING display_order::INTEGER;
