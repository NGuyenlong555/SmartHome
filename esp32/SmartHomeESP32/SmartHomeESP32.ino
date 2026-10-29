/*
 * Smart Home AI - firmware ESP32
 * Khớp với app Android:
 *   GET /relay?dev=light|door|alarm&state=1|0   -> "OK"
 *   GET /sensor -> {"temp":28,"hum":65,"motion":0,"light":0,"door":0,"alarm":0}
 *
 * Thư viện cần cài (Library Manager):
 *   - "DHT sensor library" (Adafruit) + "Adafruit Unified Sensor"
 *   - "ESP32Servo"
 * Board: ESP32 Dev Module
 *
 * Nối chân:
 *   DHT22 data  -> GPIO4   (VCC 3.3V, GND)
 *   PIR OUT     -> GPIO27  (VCC 5V, GND)  - PIR cần 30-60s khởi động
 *   Relay IN    -> GPIO26  (VCC 5V, GND)  - đèn nối qua COM / NO của relay
 *   Servo cửa   -> GPIO18  (VCC 5V ngoài, GND chung với ESP32)
 *   Buzzer      -> GPIO25  (buzzer chủ động 5V, GND)
 */

#include <WiFi.h>
#include <WebServer.h>
#include <DHT.h>
#include <ESP32Servo.h>

// ====== CẤU HÌNH ======
const char* WIFI_SSID = "TEN_WIFI";
const char* WIFI_PASS = "MAT_KHAU";

#define DHT_PIN     4
#define DHT_TYPE    DHT22      // đổi thành DHT11 nếu dùng DHT11
#define PIR_PIN     27
#define RELAY_PIN   26
#define SERVO_PIN   18
#define BUZZER_PIN  25

const bool RELAY_ACTIVE_LOW = true;   // đa số module relay kích mức thấp; nếu ngược thì đặt false
const int  DOOR_OPEN_ANGLE   = 90;
const int  DOOR_CLOSED_ANGLE = 0;
const unsigned long DHT_INTERVAL_MS = 2500;  // DHT22 cần >= 2s giữa 2 lần đọc

// ====== TRẠNG THÁI ======
WebServer server(80);
DHT dht(DHT_PIN, DHT_TYPE);
Servo doorServo;

bool lightOn = false;
bool doorOpen = false;
bool alarmOn = false;
float lastTemp = 0;
float lastHum = 0;
unsigned long lastDhtRead = 0;

// ====== ĐIỀU KHIỂN THIẾT BỊ ======
void setLight(bool on) {
  lightOn = on;
  bool level = RELAY_ACTIVE_LOW ? !on : on;
  digitalWrite(RELAY_PIN, level ? HIGH : LOW);
}

void setDoor(bool open) {
  doorOpen = open;
  doorServo.write(open ? DOOR_OPEN_ANGLE : DOOR_CLOSED_ANGLE);
}

void setAlarm(bool on) {
  alarmOn = on;
  digitalWrite(BUZZER_PIN, on ? HIGH : LOW);
}

// ====== HANDLER HTTP ======
void handleRelay() {
  if (!server.hasArg("dev") || !server.hasArg("state")) {
    server.send(400, "text/plain", "Missing dev/state");
    return;
  }
  String dev = server.arg("dev");
  bool on = server.arg("state") == "1";

  if (dev == "light")      setLight(on);
  else if (dev == "door")  setDoor(on);
  else if (dev == "alarm") setAlarm(on);
  else {
    server.send(404, "text/plain", "Unknown device");
    return;
  }
  server.send(200, "text/plain", "OK");
}

void readDht() {
  if (millis() - lastDhtRead < DHT_INTERVAL_MS) return;
  lastDhtRead = millis();
  float t = dht.readTemperature();
  float h = dht.readHumidity();
  if (!isnan(t)) lastTemp = t;   // lỗi đọc -> giữ giá trị cũ
  if (!isnan(h)) lastHum = h;
}

void handleSensor() {
  readDht();
  int motion = digitalRead(PIR_PIN) == HIGH ? 1 : 0;
  String json = "{\"temp\":" + String((int)round(lastTemp)) +
                ",\"hum\":" + String((int)round(lastHum)) +
                ",\"motion\":" + String(motion) +
                ",\"light\":" + String(lightOn ? 1 : 0) +
                ",\"door\":" + String(doorOpen ? 1 : 0) +
                ",\"alarm\":" + String(alarmOn ? 1 : 0) + "}";
  server.send(200, "application/json", json);
}

void handleRoot() {
  server.send(200, "text/plain", "Smart Home AI ESP32 - OK");
}

// ====== WIFI ======
void connectWifi() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.print("Dang ket noi WiFi");
  unsigned long start = millis();
  while (WiFi.status() != WL_CONNECTED && millis() - start < 20000) {
    delay(500);
    Serial.print(".");
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.println();
    Serial.print("Da ket noi. IP ESP32: ");
    Serial.println(WiFi.localIP());   // nhập IP này vào app: Cài đặt > ESP32
  } else {
    Serial.println("\nKhong ket noi duoc WiFi, se thu lai...");
  }
}

void setup() {
  Serial.begin(115200);

  pinMode(PIR_PIN, INPUT);
  pinMode(RELAY_PIN, OUTPUT);
  pinMode(BUZZER_PIN, OUTPUT);

  setLight(false);
  setAlarm(false);

  doorServo.setPeriodHertz(50);
  doorServo.attach(SERVO_PIN, 500, 2400);
  setDoor(false);

  dht.begin();
  connectWifi();

  server.on("/", handleRoot);
  server.on("/relay", handleRelay);
  server.on("/sensor", handleSensor);
  server.begin();
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWifi();
  }
  server.handleClient();
  readDht();
}
