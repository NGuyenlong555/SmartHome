package com.example.smarthomeai.data

import android.app.Activity
import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SmartHomeViewModel(app: Application) : AndroidViewModel(app) {

    private companion object {
        const val MAX_ITEMS = 50
        const val POLL_INTERVAL_MS = 5_000L
        const val SENSOR_LOG_INTERVAL_MS = 60_000L
        const val SYNC_PAUSE_AFTER_COMMAND_MS = 3_000L
    }

    private val storage = Storage(app)
    private val notifier = Notifier(app)
    private val auth = AuthRepository(app)

    private val _state = MutableStateFlow(storage.load().copy(userEmail = auth.currentEmail))
    val state: StateFlow<SmartHomeState> = _state.asStateFlow()

    private val _loggedIn = MutableStateFlow(auth.isSignedIn())
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    val firebaseConfigured: Boolean get() = auth.configured

    private var lastSensorLog = 0L
    private var lastCommandAt = 0L

    init {
        startSensorPolling()
        // Lưu xuống máy sau khi trạng thái ổn định 0,5 giây
        viewModelScope.launch {
            _state.collectLatest {
                delay(500)
                storage.save(it)
            }
        }
    }

    // ---------- Đăng nhập ----------
    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult("Vui lòng nhập email và mật khẩu")
            return
        }
        if (!auth.configured) {          // chế độ demo
            finishLogin(email.trim())
            onResult(null)
            return
        }
        auth.login(email.trim(), password) { error ->
            if (error == null) finishLogin(auth.currentEmail ?: email.trim())
            onResult(error)
        }
    }

    fun register(email: String, password: String, onResult: (String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult("Vui lòng nhập email và mật khẩu")
            return
        }
        if (password.length < 6) {
            onResult("Mật khẩu tối thiểu 6 ký tự")
            return
        }
        if (!auth.configured) {          // chế độ demo
            finishLogin(email.trim())
            onResult(null)
            return
        }
        auth.register(email.trim(), password) { error ->
            if (error == null) finishLogin(auth.currentEmail ?: email.trim())
            onResult(error)
        }
    }

    fun loginWithGoogle(activity: Activity, onResult: (String?) -> Unit) {
        if (!auth.configured) {
            onResult("Chưa cấu hình Firebase (thiếu google-services.json)")
            return
        }
        auth.signInWithGoogle(activity) { error ->
            if (error == null) finishLogin(auth.currentEmail ?: "Google")
            onResult(error)
        }
    }

    private fun finishLogin(email: String) {
        _state.update { it.copy(userEmail = email) }
        _loggedIn.value = true
    }

    fun logout() {
        auth.signOut()
        _state.update { it.copy(userEmail = null) }
        _loggedIn.value = false
    }

    // ---------- Điều khiển thiết bị ----------
    fun setDevice(type: DeviceType, on: Boolean) {
        if (isDeviceOn(_state.value, type) == on) return

        val (title, status) = when (type) {
            DeviceType.LIGHT -> "Đèn" to if (on) "Bật" else "Tắt"
            DeviceType.DOOR -> "Cửa" to if (on) "Mở" else "Đóng"
            DeviceType.ALARM -> "Còi báo" to if (on) "Bật" else "Tắt"
        }
        lastCommandAt = SystemClock.elapsedRealtime()
        _state.update {
            val logs = (listOf(LogItem(type.key, "$title: $status", nowString())) + it.logs).take(MAX_ITEMS)
            when (type) {
                DeviceType.LIGHT -> it.copy(light = on, logs = logs)
                DeviceType.DOOR -> it.copy(doorOpen = on, logs = logs)
                DeviceType.ALARM -> it.copy(alarm = on, logs = logs)
            }
        }
        val ip = _state.value.esp32Ip
        viewModelScope.launch(Dispatchers.IO) {
            Esp32Client.sendCommand(ip, type.key, on)
        }
    }

    private fun isDeviceOn(s: SmartHomeState, type: DeviceType) = when (type) {
        DeviceType.LIGHT -> s.light
        DeviceType.DOOR -> s.doorOpen
        DeviceType.ALARM -> s.alarm
    }

    // ---------- Cảm biến ----------
    private fun startSensorPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val data = Esp32Client.readSensor(_state.value.esp32Ip)
                if (data == null) {
                    // Không có ESP32 -> đánh dấu offline, giữ dữ liệu cũ
                    _state.update { it.copy(esp32Online = false) }
                } else {
                    applySensor(data)
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun applySensor(d: SensorData) {
        val now = SystemClock.elapsedRealtime()
        val prev = _state.value
        val time = nowString()
        val motionStarted = d.motion && !prev.motionDetected
        val motionChanged = d.motion != prev.motionDetected
        val logSensors = now - lastSensorLog > SENSOR_LOG_INTERVAL_MS
        if (logSensors) lastSensorLog = now
        val syncDevices = now - lastCommandAt > SYNC_PAUSE_AFTER_COMMAND_MS

        _state.update { s ->
            val newLogs = buildList {
                if (motionChanged) add(LogItem("motion", "Chuyển động: ${if (d.motion) "Phát hiện" else "Không phát hiện"}", time))
                if (logSensors) {
                    add(LogItem("temp", "Nhiệt độ: ${d.temp}°C", time))
                    add(LogItem("hum", "Độ ẩm: ${d.hum}%", time))
                }
            }
            s.copy(
                temperature = d.temp,
                humidity = d.hum,
                motionDetected = d.motion,
                esp32Online = true,
                light = if (syncDevices) d.light ?: s.light else s.light,
                doorOpen = if (syncDevices) d.door ?: s.doorOpen else s.doorOpen,
                alarm = if (syncDevices) d.alarm ?: s.alarm else s.alarm,
                logs = (newLogs + s.logs).take(MAX_ITEMS),
                alerts = if (motionStarted)
                    (listOf(AlertItem(AlertType.MOTION, "Phát hiện chuyển động", time)) + s.alerts).take(MAX_ITEMS)
                else s.alerts
            )
        }
        if (motionStarted) pushNotification("Phát hiện chuyển động", "Cảm biến vừa phát hiện chuyển động trong nhà")
    }

    private fun pushNotification(title: String, text: String) {
        if (_state.value.notifyEnabled) notifier.notify(title, text)
    }

    // ---------- Khuôn mặt ----------
    fun setRegisteredUsers(names: List<String>) {
        _state.update { it.copy(registeredUsers = names) }
    }

    fun registerFace(name: String) {
        _state.update { it.copy(registeredUsers = (it.registeredUsers + name).distinct()) }
    }

    /** Gọi khi nhận diện xong. [name] = null nghĩa là người lạ. */
    fun onFaceRecognized(name: String?, accuracy: Int) {
        val time = nowString()
        _state.update {
            val record = FaceRecord(name ?: "Người lạ", time, name != null)
            val alerts = if (name == null)
                (listOf(AlertItem(AlertType.STRANGER, "Người lạ xuất hiện", time)) + it.alerts).take(MAX_ITEMS)
            else it.alerts
            it.copy(
                faces = (listOf(record) + it.faces).take(MAX_ITEMS),
                alerts = alerts,
                recognizedName = name ?: "Người lạ",
                recognizedAccuracy = accuracy
            )
        }
        if (name == null) {
            pushNotification("Cảnh báo an ninh", "Người lạ xuất hiện trước camera")
            if (_state.value.autoAlarm) setDevice(DeviceType.ALARM, true)
        }
    }

    // ---------- Cài đặt ----------
    fun setEsp32Ip(ip: String) {
        _state.update { it.copy(esp32Ip = ip) }
    }

    fun setAutoAlarm(enabled: Boolean) {
        _state.update { it.copy(autoAlarm = enabled) }
    }

    fun setNotify(enabled: Boolean) {
        _state.update { it.copy(notifyEnabled = enabled) }
    }
}
