# Mỗi ngày: chờ tới 1 giờ ngẫu nhiên trong ngày rồi thêm 3-4 người truy cập giả qua POST /track.
# vid có dấu 'seed-cron-…' → clear-fake-traffic.sql (LIKE 'seed-%') xoá được, và lọc ra được khi cần số thật.
#   Cài 1 lần (Task Scheduler, 00:05 mỗi ngày):  powershell -ExecutionPolicy Bypass -File scripts\fake-daily-traffic.ps1 -Install -Api https://<domain>/api
#   Chạy thử ngay, không chờ:                     powershell -ExecutionPolicy Bypass -File scripts\fake-daily-traffic.ps1 -Api http://localhost:7070/api -NoWait
#   Gỡ:                                            Unregister-ScheduledTask FakeDailyTraffic -Confirm:$false
#   Đếm số giả:  SELECT day, count(*) FROM dtb_daily_visitors WHERE visitor_id LIKE 'seed-cron-%' GROUP BY day;
param([Parameter(Mandatory)][string]$Api, [switch]$Install, [switch]$NoWait)
$Api = $Api.TrimEnd('/')

if ($Install) {
  $action   = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$PSCommandPath`" -Api $Api"
  $trigger  = New-ScheduledTaskTrigger -Daily -At 00:05
  $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Hours 24)
  Register-ScheduledTask FakeDailyTraffic -Action $action -Trigger $trigger -Settings $settings -Force | Out-Null
  "Đã cài task FakeDailyTraffic (00:05 mỗi ngày, giờ chạy thật random trong ngày)"; return
}

if (-not $NoWait) {
  # ponytail: máy sleep/tắt thì Start-Sleep dừng theo → lượt đó có thể trễ/mất; cần chắc chắn thì chạy trên server (cron) thay vì laptop
  $left = [int]((Get-Date).Date.AddDays(1) - (Get-Date)).TotalSeconds - 3600   # chừa 1h cho 3-4 lượt
  Start-Sleep -Seconds (Get-Random -Minimum 0 -Maximum ([Math]::Max(1, $left)))
}

$n = Get-Random -Minimum 3 -Maximum 5   # 3 hoặc 4
for ($i = 1; $i -le $n; $i++) {
  if ($i -gt 1 -and -not $NoWait) { Start-Sleep -Seconds (Get-Random -Minimum 30 -Maximum 600) }   # cách nhau 0.5-10 phút
  $vid = 'seed-cron-' + [guid]::NewGuid().ToString('N').Substring(0, 12)
  Invoke-RestMethod -Method Post -Uri "$Api/track" -ContentType 'application/json' -Body (@{ vid = $vid } | ConvertTo-Json) | Out-Null
  "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') +1 $vid"
}
