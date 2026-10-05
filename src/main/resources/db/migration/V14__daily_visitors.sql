-- Người truy cập web: mỗi trình duyệt (visitor_id ngẫu nhiên lưu ở localStorage) tính 1 lần/ngày (giờ VN).
CREATE TABLE dtb_daily_visitors (
    day        DATE        NOT NULL,
    visitor_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (day, visitor_id)
);
CREATE INDEX idx_bookings_created ON dtb_bookings(created_at);
