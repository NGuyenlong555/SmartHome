package com.example.smarthomeai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomeai.data.AlertItem
import com.example.smarthomeai.data.AlertType
import com.example.smarthomeai.data.SmartHomeState
import com.example.smarthomeai.ui.components.*
import com.example.smarthomeai.ui.theme.*

@Composable
fun AlertsScreen(state: SmartHomeState, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Scaffold(
        containerColor = Bg,
        topBar = { GradientTopBar("Cảnh báo", onBack) },
        bottomBar = { SmartBottomBar("alerts", third = "alerts", onNavigate = onNavigate) }
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.alerts.isEmpty()) item { EmptyHint("Chưa có cảnh báo nào") }
            else items(state.alerts) { AlertRow(it) }
        }
    }
}

@Composable
private fun AlertRow(a: AlertItem) {
    val (icon, tint, bg) = when (a.type) {
        AlertType.MOTION -> Triple(Icons.AutoMirrored.Filled.DirectionsRun, RedAlert, RedSoft)
        AlertType.STRANGER -> Triple(Icons.Filled.PersonSearch, OrangeWarn, YellowSoft)
        AlertType.DOOR -> Triple(Icons.Filled.MeetingRoom, Blue600, BlueSoft)
        AlertType.LOGIN -> Triple(Icons.Filled.CheckCircle, GreenOk, GreenSoft)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, Color.White, tint, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextDark)
            Text(a.time, fontSize = 11.sp, color = TextGray)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextGray)
    }
}

@Composable
fun SettingsScreen(
    state: SmartHomeState,
    onIpChange: (String) -> Unit,
    onAutoAlarm: (Boolean) -> Unit,
    onNotify: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    var showIp by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var ipText by remember(state.esp32Ip) { mutableStateOf(state.esp32Ip) }

    Scaffold(
        containerColor = Bg,
        topBar = { GradientTopBar("Cài đặt", onBack) },
        bottomBar = { SmartBottomBar("settings", onNavigate = onNavigate) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Group("Kết nối") {
                SettingRow(Icons.Filled.Wifi, Blue600, "Wi-Fi", "Đã kết nối: ${state.wifiName}")
                SettingRow(
                    Icons.Filled.Memory, GreenOk, "ESP32",
                    if (state.esp32Online) "Online • ${state.esp32Ip}" else "Offline • ${state.esp32Ip}"
                ) { showIp = true }
            }
            Group("Tài khoản") {
                SettingRow(Icons.Filled.Person, Blue600, "Thông tin cá nhân", state.userEmail ?: "Chưa đăng nhập") { showProfile = true }
                SettingRow(Icons.Filled.Group, Blue600, "Quản lý người dùng", "${state.registeredUsers.size} người")
            }
            Group("Ứng dụng") {
                SettingRow(
                    Icons.Filled.Notifications, Blue600, "Thông báo", "Báo khi có chuyển động, người lạ",
                    trailing = { SmartSwitch(state.notifyEnabled, onNotify) }
                )
                SettingRow(
                    Icons.Filled.NotificationsActive, RedAlert, "Tự bật còi khi có người lạ", "Còi kêu ngay khi camera thấy người lạ",
                    trailing = { SmartSwitch(state.autoAlarm, onAutoAlarm) }
                )
                SettingRow(Icons.Filled.Contrast, Blue600, "Giao diện", "Sáng")
            }
            Group("Hệ thống") {
                SettingRow(Icons.Filled.Info, Blue600, "Giới thiệu", "v1.0.0")
                SettingRow(Icons.AutoMirrored.Filled.Logout, RedAlert, "Đăng xuất") { onLogout() }
            }
        }
    }

    if (showProfile) {
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = { Text("Thông tin cá nhân") },
            text = { Text("Tài khoản: ${state.userEmail ?: "Chưa đăng nhập"}\nNgười dùng đã đăng ký khuôn mặt: ${state.registeredUsers.size}") },
            confirmButton = { TextButton(onClick = { showProfile = false }) { Text("Đóng") } }
        )
    }

    if (showIp) {
        AlertDialog(
            onDismissRequest = { showIp = false },
            title = { Text("Địa chỉ IP ESP32") },
            text = {
                OutlinedTextField(ipText, { ipText = it }, singleLine = true, label = { Text("192.168.x.x") })
            },
            confirmButton = {
                TextButton(onClick = { onIpChange(ipText.trim()); showIp = false }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { showIp = false }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark, modifier = Modifier.padding(top = 6.dp))
    WhiteCard(Modifier.fillMaxWidth(), content = content)
}

@Composable
private fun SettingRow(
    icon: ImageVector, tint: Color, title: String, value: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = TextDark)
            if (value != null) Text(value, fontSize = 11.sp, color = TextGray)
        }
        if (trailing != null) trailing()
        else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextGray)
    }
}
