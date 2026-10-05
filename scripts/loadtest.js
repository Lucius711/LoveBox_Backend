// Load test bằng k6: giả lập nhiều người cùng mở web → xem danh sách đồ → xem chi tiết.
// CHỈ chạy vào local / staging. Chạy vào production sẽ bơm số "người truy cập" thật trên màn admin.
//
//   winget install k6                                   (cài 1 lần)
//   k6 run scripts/loadtest.js                          (backend local)
//   k6 run -e API=https://staging.../api scripts/loadtest.js
//   k6 run -e PEAK=500 scripts/loadtest.js              (đổi số người dùng đồng thời, mặc định 200)
//
// Dọn dữ liệu sau khi chạy:
//   DELETE FROM dtb_daily_visitors WHERE visitor_id LIKE 'loadtest-%';
import http from 'k6/http';
import { check, sleep } from 'k6';

const API = (__ENV.API || 'http://localhost:7070/api').replace(/\/+$/, '');
const PEAK = Number(__ENV.PEAK || 200);
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

export const options = {
  stages: [
    { duration: '30s', target: Math.ceil(PEAK / 4) }, // khởi động
    { duration: '1m', target: PEAK },                 // tăng lên đỉnh
    { duration: '1m', target: PEAK },                 // giữ ở đỉnh
    { duration: '30s', target: 0 },                   // hạ về 0
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],   // lỗi < 1%
    http_req_duration: ['p(95)<500'], // 95% request < 500ms
    checks: ['rate>0.99'],
  },
};

// Chặn chạy nhầm vào production (domain lentique.shop) trừ khi cố ý đặt ALLOW_PROD=1
export function setup() {
  if (/lentique\.shop/i.test(API) && __ENV.ALLOW_PROD !== '1')
    throw new Error(`API = ${API} là production → dừng. Chạy vào local/staging (hoặc đặt ALLOW_PROD=1 nếu thật sự muốn).`);
  const meta = http.get(`${API}/products/meta`);
  if (meta.status !== 200) throw new Error(`Backend chưa chạy ở ${API} (status ${meta.status})`);
  const list = http.get(`${API}/products?page=0&size=12`).json('data.items') || [];
  return { ids: list.map((p) => p.id) };
}

export default function ({ ids }) {
  // 1. Mở web → đếm truy cập (1 người dùng ảo = 1 người truy cập, lặp lại thì như F5 — không đếm thêm)
  const track = http.post(`${API}/track`, JSON.stringify({ vid: `loadtest-${__VU}` }), JSON_HEADERS);
  check(track, { 'track 200': (r) => r.status === 200 });
  sleep(1 + Math.random() * 2);

  // 2. Xem danh sách đồ (trang ngẫu nhiên 0-2)
  const list = http.get(`${API}/products?page=${Math.floor(Math.random() * 3)}&size=12`);
  check(list, { 'products 200': (r) => r.status === 200 });
  sleep(1 + Math.random() * 3);

  // 3. Xem chi tiết 1 món + lịch đã thuê
  if (ids.length) {
    const id = ids[Math.floor(Math.random() * ids.length)];
    check(http.get(`${API}/products/${id}`), { 'detail 200': (r) => r.status === 200 });
    check(http.get(`${API}/products/${id}/blocked-dates`), { 'blocked-dates 200': (r) => r.status === 200 });
  }
  sleep(2 + Math.random() * 3);
}
