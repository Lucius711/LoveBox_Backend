-- R2_PUBLIC_URL từng đặt nhầm là endpoint S3 API (không public) → link ảnh đã lưu không mở được.
-- Đổi mọi link ảnh sang R2_PUBLIC_URL hiện tại (placeholder r2PublicUrl, vd https://pub-xxx.r2.dev).
UPDATE dtb_product_images
SET url = '${r2PublicUrl}' || substring(url from '/products/.*$')
WHERE url ~ '/products/' AND url NOT LIKE '${r2PublicUrl}/%';
