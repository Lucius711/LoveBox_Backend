-- Đổi tên thương hiệu LoveBox → Lentique cho tài khoản chủ đồ demo (đảo lại V6)
UPDATE dtb_users SET name = 'Lentique Studio', email = 'owner@lentique.demo' WHERE google_sub = 'demo-owner';
