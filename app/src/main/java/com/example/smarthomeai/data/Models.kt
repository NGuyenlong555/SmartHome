package com.example.smarthomeai.data

import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class DeviceType(val key: String) {
    LIGHT("light"), DOOR("door"), ALARM("alarm")
}

data class LogItem(
    val kind: String,      // temp | hum | motion | light | door | alarm
    val title: String,
    val time: String
)

data class FaceRecord(
    val name: String,
    val time: String,
    val known: Boolean
)

enum class AlertType { MOTION, STRANGER, DOOR, LOGIN }

data class AlertItem(
    val type: AlertType,
    val title: String,
    val time: String
)

data class SmartHomeState(
    val temperature: Int = 28,
    val humidity: Int = 65,
    val motionDetected: Boolean = false,
    val light: Boolean = false,
    val doorOpen: Boolean = false,
    val alarm: Boolean = false,
    val logs: List<LogItem> = emptyList(),
    val faces: List<FaceRecord> = emptyList(),
    val alerts: List<AlertItem> = emptyList(),
    val wifiName: String = "Home_5G",
    val esp32Online: Boolean = false,
    val esp32Ip: String = "192.168.1.50",
    val recognizedName: String = "Chưa nhận diện",
    val recognizedAccuracy: Int = 0,
    val registeredUsers: List<String> = emptyList(),
    val autoAlarm: Boolean = true,        // tự bật còi khi có người lạ
    val notifyEnabled: Boolean = true,    // thông báo đẩy
    val userEmail: String? = null
)

fun nowString(): String =
    SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()).format(Date())

/** Dữ liệu ESP32 trả về. light/door/alarm = null nếu firmware không gửi. */
data class SensorData(
    val temp: Int,
    val hum: Int,
    val motion: Boolean,
    val light: Boolean?,
    val door: Boolean?,
    val alarm: Boolean?
)

/**
 * Gửi lệnh tới ESP32 qua HTTP. Chỉnh endpoint cho khớp firmware của bạn:
 *   GET http://<ip>/relay?dev=light&state=1
 *   GET http://<ip>/sensor  ->  {"temp":28,"hum":65,"motion":0,"light":0,"door":0,"alarm":0}
 * Nếu ESP32 không phản hồi, app vẫn chạy ở chế độ mô phỏng.
 */
object Esp32Client {
    fun sendCommand(ip: String, device: String, on: Boolean): Boolean = try {
        val url = URL("http://$ip/relay?dev=$device&state=${if (on) 1 else 0}")
        val c = url.openConnection() as HttpURLConnection
        c.connectTimeout = 1500
        c.readTimeout = 1500
        val ok = c.responseCode == 200
        c.disconnect()
        ok
    } catch (e: Exception) {
        false
    }

    fun readSensor(ip: String): SensorData? = try {
        val url = URL("http://$ip/sensor")
        val c = url.openConnection() as HttpURLConnection
        c.connectTimeout = 1500
        c.readTimeout = 1500
        val body = c.inputStream.bufferedReader().readText()
        c.disconnect()

        fun num(key: String): Int? =
            Regex("\"$key\"\\s*:\\s*(-?\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull()

        val temp = num("temp")
        val hum = num("hum")
        if (temp == null || hum == null) null
        else SensorData(
            temp = temp,
            hum = hum,
            motion = num("motion") == 1,
            light = num("light")?.let { it == 1 },
            door = num("door")?.let { it == 1 },
            alarm = num("alarm")?.let { it == 1 }
        )
    } catch (e: Exception) {
        null
    }
}
