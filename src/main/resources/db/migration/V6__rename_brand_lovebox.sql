-- Đổi tên thương hiệu Lentique → LoveBox cho tài khoản chủ đồ demo (không sửa V1/V2 vì Flyway kiểm tra checksum).
UPDATE dtb_users SET name = 'LoveBox Studio', email = 'owner@lovebox.demo' WHERE google_sub = 'demo-owner';
