# Smart Home AI (Android – Kotlin + Jetpack Compose)

## Cách chạy
1. Android Studio (Koala trở lên) → **File > Open** → chọn thư mục `SmartHomeAI`.
2. Chờ Gradle sync (cần JDK 17; Android Studio có sẵn).
3. Chọn máy ảo / điện thoại → **Run ▶**.

## Cấu trúc
- `ui/theme/Theme.kt` – màu, theme
- `ui/components/Components.kt` – header, card, switch, bottom bar
- `ui/screens/` – Login, Home, Devices, Environment, Camera (+ Đăng ký khuôn mặt), Alerts, Settings
- `data/SmartHomeViewModel.kt` – trạng thái app, đăng nhập demo, điều khiển thiết bị, đọc cảm biến
- `data/Models.kt` – model + `Esp32Client` (HTTP)

## Phần cứng ESP32
Firmware: `esp32/SmartHomeESP32/SmartHomeESP32.ino` (Arduino IDE, board ESP32 Dev Module).
1. Cài thư viện: *DHT sensor library* (Adafruit), *Adafruit Unified Sensor*, *ESP32Servo*.
2. Sửa `WIFI_SSID`, `WIFI_PASS` trong file, nạp code, mở Serial Monitor (115200) để xem IP.
3. Trong app: **Cài đặt > ESP32** nhập IP đó (điện thoại và ESP32 cùng Wi-Fi).

| Linh kiện | Chân ESP32 |
|---|---|
| DHT22 (data) | GPIO4 (VCC 3.3V) |
| PIR HC-SR501 (OUT) | GPIO27 (VCC 5V) |
| Relay đèn (IN) | GPIO26 (VCC 5V) |
| Servo cửa (signal) | GPIO18 (nguồn 5V ngoài) |
| Buzzer chủ động | GPIO25 |

Tất cả GND nối chung. Nếu relay đóng/ngắt ngược thì đặt `RELAY_ACTIVE_LOW = false`.

API app gọi:
- `GET http://<ip>/relay?dev=light|door|alarm&state=1|0`
- `GET http://<ip>/sensor` -> `{"temp":28,"hum":65,"motion":0,...}` (poll mỗi 5 giây)

Nếu ESP32 không phản hồi, app vẫn chạy với dữ liệu mô phỏng và Cài đặt hiện Offline.

## Nhận diện khuôn mặt
- Phát hiện khuôn mặt: ML Kit (chạy ngay, không cần thêm file).
- Định danh người dùng: MobileFaceNet TFLite, **đã kèm sẵn** tại `app/src/main/assets/mobile_face_net.tflite`
  (đầu vào 112x112x3, đầu ra 192 chiều). Mô hình lấy từ repo công khai
  github.com/estebanuri/face_recognition (repo không ghi giấy phép; nếu nộp bài hãy ghi nguồn).
- Đăng ký: màn Camera > Đăng ký người dùng > nhập tên > Bắt đầu quét (lấy 8 mẫu, lưu trên máy).
- Ngưỡng so khớp: `FaceStore.THRESHOLD` (mặc định 0.70) trong `face/FaceEngine.kt`.
  Nhận nhầm người thì tăng, hay báo "Người lạ" với người đã đăng ký thì giảm.

## Đăng nhập (Firebase)
Mặc định app chạy **chế độ demo** (mọi tài khoản không rỗng đều vào được). Để đăng nhập thật:
1. Vào console.firebase.google.com > tạo project > **Add app > Android**, package name `com.example.smarthomeai`.
2. Lấy mã SHA-1: trong Android Studio mở Gradle > app > Tasks > android > **signingReport**, copy SHA-1 của `debug`, dán vào Firebase (bắt buộc cho Google Sign-In).
3. Tải **google-services.json** về, đặt vào thư mục `app/` (cạnh `app/build.gradle.kts`).
4. Firebase > Authentication > Sign-in method: bật **Email/Password** và **Google**. Sau khi bật Google, tải lại google-services.json.
5. Sync Gradle, chạy lại. Màn hình đăng nhập sẽ hết dòng "Chế độ demo".

## Tự bật còi, thông báo, lưu dữ liệu
- Cài đặt > **Tự bật còi khi có người lạ**: camera thấy người lạ thì app bật còi (gửi lệnh `alarm` tới ESP32).
- Cài đặt > **Thông báo**: báo khi có chuyển động hoặc người lạ. Android 13+ sẽ hỏi quyền thông báo lần đầu.
  Thông báo chỉ hoạt động khi app đang chạy (chưa có dịch vụ chạy nền).
- Lịch sử, cảnh báo, trạng thái thiết bị, IP ESP32 và các cài đặt được lưu trên máy.
- Khi ESP32 online, app đồng bộ trạng thái đèn/cửa/còi theo dữ liệu `/sensor` gửi về.
