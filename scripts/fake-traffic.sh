#!/bin/bash
# Mỗi ngày: chọn TIMES giờ ngẫu nhiên (tăng dần), mỗi giờ đó thêm 3-4 lượt truy cập giả.
# Cộng dồn đủ LIMIT lượt thì tự gỡ khỏi crontab và dừng hẳn.
# vid có dấu 'seed-cron-…' để lọc/xoá: DELETE FROM dtb_daily_visitors WHERE visitor_id LIKE 'seed-cron-%';
#   Cài trên VPS:   chmod +x ~/fake-traffic.sh && (crontab -l; echo '5 17 * * * ~/fake-traffic.sh') | crontab -
#                   (17:05 UTC = 00:05 giờ VN; VPS để giờ VN thì dùng '5 0 * * *')
#   Chạy thử ngay:  ~/fake-traffic.sh now      (bỏ hết thời gian chờ)
#   Xem đã thêm:    cat ~/.fake-traffic-count      Đếm lại từ 0: rm ~/.fake-traffic-count
#   Log:            tail ~/fake-traffic.log
LIMIT=${LIMIT:-628}
TIMES=${TIMES:-10}
API=${API:-http://localhost:7070/api/track}
COUNT_FILE=${COUNT_FILE:-$HOME/.fake-traffic-count}
exec >> "${LOG:-$HOME/fake-traffic.log}" 2>&1

count=$(cat "$COUNT_FILE" 2>/dev/null || echo 0)
if [ "$count" -ge "$LIMIT" ]; then
  crontab -l 2>/dev/null | grep -v fake-traffic.sh | crontab -   # đủ rồi → gỡ cron
  echo "Đã đủ $count/$LIMIT lượt, dừng."; exit 0
fi

# TIMES mốc giây ngẫu nhiên trong 0..23h (từ lúc cron chạy 00:05), sắp tăng dần → mốc sau luôn sau mốc trước
slots=$(shuf -i 0-82800 -n "$TIMES" | sort -n)
echo "$(date '+%F') lịch: $(for s in $slots; do date -d "+$s sec" '+%H:%M'; done | tr '\n' ' ')"

START=$(date +%s)
for s in $slots; do
  [ "$1" = now ] || { now=$(( $(date +%s) - START )); [ $((s - now)) -gt 0 ] && sleep $((s - now)); } 2>/dev/null
  for i in $(seq $(shuf -i 3-4 -n1)); do
    [ "$count" -ge "$LIMIT" ] && exit 0
    vid=seed-cron-$(head -c6 /dev/urandom | od -An -tx1 | tr -d ' \n')
    curl -sf -X POST "$API" -H 'Content-Type: application/json' -d "{\"vid\":\"$vid\"}" >/dev/null \
      && echo $((++count)) > "$COUNT_FILE" && echo "$(date '+%F %T') +1 $vid ($count/$LIMIT)"
    [ "$1" = now ] || sleep $(shuf -i 5-60 -n1)   # 3-4 người cách nhau vài giây-1 phút
  done
done
