# 📌 Army2 Client - LibGDX

👉 Đây là Project **Army2 Client** được viết lại trên nền tảng **LibGDX**, mô phỏng chính xác 100% cơ chế căn góc của MobiArmy 2.

## 🔹 Giới thiệu
- **Army2 Client** được phát triển lại từ đầu với nền tảng **LibGDX**, giúp trải nghiệm game mượt mà hơn.
- Đây là bản **chính xác 100% về cơ chế căn góc** của MobiArmy 2, một tựa game huyền thoại của **Teamobi**.
- Vì dự án vẫn đang trong quá trình phát triển, bạn có thể gặp một số lỗi trong quá trình chơi. Nếu gặp lỗi, hãy tự tìm hiểu và sửa chữa hoặc đóng góp cho dự án.

### 🛠 Thông tin phát triển
- **Tác giả viết lại:** Văn Tú
- **Dựa trên game gốc:** [MobiArmy 2 của Teamobi](http://teamobi.com/home/game/MobiArmy-2-4.html)

## 📸 Hình ảnh minh họa
Dưới đây là một số hình ảnh về client:

![Ảnh 1](assets/anh1.png)
![Ảnh 2](assets/anh2.png)
![Ảnh 3](assets/anh3.png)
![Ảnh 4](assets/anh4.png)

## 💡 Hướng dẫn cài đặt & chạy

Client desktop dùng Java/LibGDX và có thể chạy trực tiếp trên Windows hoặc Linux.

### Yêu cầu

- Cài **JDK 21**, kiểm tra cả `java -version` và `javac -version` đều trả về phiên bản 21.
- Nếu có biến môi trường `JAVA_HOME`, hãy trỏ nó tới thư mục JDK 21.
- Có kết nối Internet ở lần chạy đầu để tải Gradle và các thư viện.
- Linux cần phiên desktop có màn hình đồ họa; không chạy giao diện game trong terminal headless.

Dự án có sẵn Gradle Wrapper 8.5 nên không cần cài Gradle riêng. Chạy các lệnh dưới đây tại thư mục gốc repository, nơi có `gradlew` và `gradlew.bat`.

### Windows

Mở PowerShell, tải mã nguồn và vào thư mục project (bỏ qua bước clone nếu đã có):

```powershell
git clone https://github.com/vantu03/MobiArmy2-Client.git
cd MobiArmy2-Client

java -version
javac -version
.\gradlew.bat :desktop:run
```

Nếu dùng Command Prompt (CMD), chạy `gradlew.bat :desktop:run` tại cùng thư mục.

### Linux

Mở terminal trong phiên desktop:

```bash
git clone https://github.com/vantu03/MobiArmy2-Client.git
cd MobiArmy2-Client

java -version
javac -version
bash gradlew :desktop:run
```

Nếu đã có mã nguồn, chỉ cần vào thư mục project rồi chạy `bash gradlew :desktop:run`. Cách gọi qua `bash` không yêu cầu cấp quyền thực thi cho file `gradlew`.

### Chỉ kiểm tra build

Để biên dịch client mà không mở cửa sổ game:

```powershell
# Windows PowerShell
.\gradlew.bat :desktop:classes
```

```bash
# Linux
bash gradlew :desktop:classes
```

Bạn cũng có thể mở dự án trong NetBeans hoặc IntelliJ IDEA và chạy task Gradle `:desktop:run`.

## 📢 Kết nối với Server

1. Khởi động [MobiArmy2 Server](https://github.com/vantu03/MobiArmy2-Server) và database theo README của server.
2. Nếu server chạy trên cùng máy với client, dùng cổng TCP `8122`. Danh sách máy chủ đi kèm client đã có **Localhost**, trỏ tới `127.0.0.1:8122`.
3. Chạy client, chọn **Localhost** rồi nhấn **Enter**.
4. Ở phần giới thiệu, nhấn **F2** (**Qua nhanh**) để tới form đăng nhập.
5. Nhập tài khoản game có trong database server, đăng nhập rồi thử vào phòng/trận để kiểm tra luồng chơi.

Nếu server nằm trên máy khác, cần cấu hình danh sách máy chủ của client với IP máy đó; `127.0.0.1` luôn trỏ về máy đang chạy client. Nếu server chạy trong Docker trên cùng máy, cần publish cổng `8122` ra host.

Đã kiểm tra trên Linux với JDK 21: build thành công, hiển thị form đăng nhập, kết nối TCP và nhận đúng phản hồi bắt tay Army2 (command `-27`) tại `127.0.0.1:8122`. Chưa kiểm thử đăng nhập/vào trận và chưa chạy kiểm chứng trên Windows.

## 📣 SEO & Đưa Project Đến Nhiều Người Hơn
- **Từ khóa liên quan:** MobiArmy 2, căn góc chuẩn, Teamobi, game bắn súng tọa độ, LibGDX game, client MobiArmy 2.
- **Chia sẻ trên các cộng đồng:** Facebook Group, Reddit, Discord, diễn đàn game.
- **Viết bài trên blog hoặc Medium:** Hướng dẫn cách chạy game, cách chỉnh sửa client.

---

💚 Nếu bạn có bất kỳ góp ý nào, hãy mở **Issues** hoặc tạo **Pull Request** để cải thiện dự án! 🚀
