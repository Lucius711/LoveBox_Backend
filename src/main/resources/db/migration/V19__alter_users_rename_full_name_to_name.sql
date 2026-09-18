-- V19: dtb_users.full_name → name
-- V2 created the column as full_name; entity maps @Column(name = "name")
ALTER TABLE dtb_users RENAME COLUMN full_name TO name;
