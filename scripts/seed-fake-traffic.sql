-- DỮ LIỆU GIẢ cho màn admin "Truy cập & đặt thuê" — CHỈ chạy trên DB local / staging, KHÔNG chạy trên production.
-- Không nằm trong db/migration nên Flyway không tự chạy. Xoá sạch bằng clear-fake-traffic.sql.
--   psql -h localhost -U postgres -d lovebox_data -f scripts/seed-fake-traffic.sql
-- Mọi bản ghi giả đều có dấu: visitor 'seed-…', user 'seed-renter-…@lentique.test', mã đơn 'SEED…'.
-- Chỉnh số liệu ở các hằng số trong khối \set bên dưới.
\set days 30
\set renters 40
\set visitors_min 50
\set visitors_max 200
\set paid_max 8

BEGIN;

-- 1. Người truy cập: mỗi ngày :visitors_min..:visitors_max khách
INSERT INTO dtb_daily_visitors (day, visitor_id)
SELECT d.day, 'seed-' || d.day || '-' || n
FROM (SELECT generate_series((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - (:days - 1),
                             (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, interval '1 day')::date AS day) d,
     LATERAL generate_series(1, :visitors_min + floor(random() * (:visitors_max - :visitors_min + 1))::int + 0 * extract(doy FROM d.day)::int) n
ON CONFLICT DO NOTHING;

-- 2. Khách thuê giả
INSERT INTO dtb_users (google_sub, email, name, role, phone, address, onboarded_at)
SELECT 'seed-renter-' || i, 'seed-renter-' || i || '@lentique.test', 'Khách giả ' || i, 'RENTER', '0900000000', 'Seed', now()
FROM generate_series(1, :renters) i
ON CONFLICT (email) DO NOTHING;

-- 3. Đơn: mỗi ngày 0..:paid_max đơn ĐÃ TRẢ + vài đơn COD chưa trả + vài đơn huỷ (2 loại sau không được đếm)
--    Ngày thuê đặt ở năm 2040 (mỗi đơn 1 khung 3 ngày riêng) để không đụng lịch đồ thật.
WITH d AS (
  SELECT generate_series((now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date - (:days - 1),
                         (now() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date, interval '1 day')::date AS day
), plan AS (
  SELECT d.day, v.kind
  FROM d, LATERAL (VALUES ('PAID',   floor(random() * (:paid_max + 1))::int + 0 * extract(doy FROM d.day)::int),
                          ('UNPAID', floor(random() * 3)::int + 0 * extract(doy FROM d.day)::int),
                          ('CANCEL', floor(random() * 2)::int + 0 * extract(doy FROM d.day)::int)) v(kind, n),
       LATERAL generate_series(1, v.n)
), n AS (
  SELECT plan.*, row_number() OVER () AS i,
         coalesce((SELECT max(substr(code, 5)::int) FROM dtb_bookings WHERE code LIKE 'SEED%'), 0) AS base
  FROM plan
), p AS (
  SELECT id, rent_price_per_day, retail_price, deposit_percent,
         row_number() OVER (ORDER BY id) - 1 AS pi, count(*) OVER () AS pc
  FROM dtb_products WHERE status = 'APPROVED'
), u AS (
  SELECT id, row_number() OVER (ORDER BY email) - 1 AS ui, count(*) OVER () AS uc
  FROM dtb_users WHERE email LIKE 'seed-renter-%@lentique.test'
)
INSERT INTO dtb_bookings (code, checkout_code, product_id, renter_id, start_date, end_date, days,
  rent_amount, deposit_amount, total_amount, status, payment_method, payment_status, deposit_status,
  recipient_name, phone, address, delivery_method, note, created_at)
SELECT 'SEED' || lpad((n.base + n.i)::text, 6, '0'), 9000000000 + n.base + n.i, p.id, u.id,
       date '2040-01-01' + ((n.base + n.i) * 3)::int, date '2040-01-02' + ((n.base + n.i) * 3)::int, 2,
       p.rent_price_per_day * 2, p.retail_price * p.deposit_percent / 100,
       p.rent_price_per_day * 2 + p.retail_price * p.deposit_percent / 100,
       CASE n.kind WHEN 'PAID' THEN 'CONFIRMED' WHEN 'UNPAID' THEN 'PENDING' ELSE 'CANCELLED' END,
       CASE n.kind WHEN 'UNPAID' THEN 'COD' ELSE 'PAYOS' END,
       CASE n.kind WHEN 'UNPAID' THEN 'UNPAID' ELSE 'PAID' END,
       CASE n.kind WHEN 'PAID' THEN 'HELD' ELSE 'PENDING' END,
       'Khách giả', '0900000000', 'Seed', 'PICKUP', 'seed',
       (n.day + time '08:00' + random() * interval '14 hours') AT TIME ZONE 'Asia/Ho_Chi_Minh'
FROM n
JOIN p ON p.pi = n.i % p.pc
JOIN u ON u.ui = (n.i * 7) % u.uc;   -- 1 khách có thể có nhiều đơn → số người < số đơn

COMMIT;

SELECT (SELECT count(*) FROM dtb_daily_visitors WHERE visitor_id LIKE 'seed-%') AS seed_visitors,
       (SELECT count(*) FROM dtb_bookings WHERE code LIKE 'SEED%' AND payment_status = 'PAID' AND status <> 'CANCELLED') AS seed_paid_bookings,
       (SELECT count(*) FROM dtb_bookings WHERE code LIKE 'SEED%') AS seed_all_bookings;
