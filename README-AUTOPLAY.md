# Auto-play hai client

Hướng dẫn đầy đủ và báo cáo nằm trong repo server:
[Hướng dẫn Auto-play](../MobiArmy2-Server/docs/testing/client-autoplay.md).

```bash
bash scripts/test-autoplay.sh
export AUTO_USER_A=test_a AUTO_USER_B=test_b
read -rsp 'Mật khẩu A: ' AUTO_PASSWORD_A; echo
read -rsp 'Mật khẩu B: ' AUTO_PASSWORD_B; echo
export AUTO_PASSWORD_A AUTO_PASSWORD_B
export AUTO_HOST=127.0.0.1 AUTO_PORT=8122 AUTO_MATCHES=3
bash scripts/run-two-clients.sh
unset AUTO_PASSWORD_A AUTO_PASSWORD_B
```

Cần server đang chạy, hai tài khoản Gunner riêng và phòng PvP trống.
Nếu tên nhân vật khác username, đặt AUTO_NAME_A/AUTO_NAME_B.
Kết quả trong thư mục test-results được in khi chạy; timeout là thất bại.
F8 bật/dừng, F9 dừng. Runner sẽ đóng cả hai cửa sổ khi kiểm thử kết thúc.
Auto-play mặc định tắt khi mở game bình thường.

Kiểm thử bot server: đặt `AUTO_BOT_IDS` là danh sách ID âm chính xác (ví dụ
`-2147483648,-2147483647`). A sẽ mời các bot đó, chờ đủ người và sẵn sàng.
Dùng hai bot với hai client để đáp ứng luật chia đội cân bằng. Mặc định không đặt
biến này vẫn từ chối người/bot ngoài hai tài khoản. `AUTO_STEP_TIMEOUT_SECONDS=90`
cho phép profile mới có thêm thời gian tải tài nguyên; timeout vẫn tính là thất bại.
Log bổ sung ACTOR/POSITION/ITEM_RECEIVED/HP_RECEIVED/SHOT_RECEIVED để đối chiếu hai client.

Kiểm thử bot nhiều địa hình: `AUTO_MAP_ID=2` chọn Căn cứ thép hoặc `AUTO_MAP_ID=20`
chọn Mê cung. Mặc định `-1` giữ map phòng; chỉ cho ID PvP 0–29, host gửi yêu cầu trước
khi start và chờ map phòng khớp. Log `MATCH_MAP` ghi ID map thực nhận lúc vào trận;
phải kiểm tra log, không coi việc gửi yêu cầu là đã đổi map thành công.

## Reconnect sau trận — hồi quy 20/09/2026

Đã sửa `Session_ME.Sender`: hàng đợi gửi dùng `ConcurrentLinkedQueue.poll()` thay
cho chuỗi size/get/remove rời rạc. Khi đóng kết nối, vô hiệu hóa và ngắt worker cũ;
worker gửi/nhận cũ không tiếp tục phục vụ phiên mới. Việc gửi và đóng stream phối
hợp trên cùng khóa session.

```bash
bash scripts/test-send-queue.sh
bash scripts/test-autoplay.sh
```

Bài thử hàng đợi chạy Sender thật với 100.000 lần thêm/xóa đồng thời, kiểm tra FIFO,
worker cũ thoát và ngắt worker; chỉ thay logger bằng stub để không khởi tạo texture.
Nghiệm thu mạng bằng hai client nằm trong báo cáo D2/D3 của repo server.
