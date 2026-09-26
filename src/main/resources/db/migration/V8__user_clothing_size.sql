-- Size quần áo khách hay mặc (cho người không biết số đo 3 vòng)
ALTER TABLE dtb_users ADD COLUMN clothing_size VARCHAR(4) CHECK (clothing_size IN ('S', 'M', 'L', 'XL'));
