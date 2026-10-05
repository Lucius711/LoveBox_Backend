-- Xoá toàn bộ dữ liệu giả do seed-fake-traffic.sql tạo (chỉ những bản ghi có dấu seed).
BEGIN;
DELETE FROM dtb_bookings WHERE code LIKE 'SEED%';
DELETE FROM dtb_notifications WHERE user_id IN (SELECT id FROM dtb_users WHERE email LIKE 'seed-renter-%@lentique.test');
DELETE FROM dtb_users WHERE email LIKE 'seed-renter-%@lentique.test';
DELETE FROM dtb_daily_visitors WHERE visitor_id LIKE 'seed-%';
COMMIT;
