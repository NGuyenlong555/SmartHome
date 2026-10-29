package com.example.smarthomeai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomeai.data.DeviceType
import com.example.smarthomeai.data.SmartHomeState
import com.example.smarthomeai.ui.components.*
import com.example.smarthomeai.ui.theme.*

@Composable
fun HomeScreen(
    state: SmartHomeState,
    onSetDevice: (DeviceType, Boolean) -> Unit,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        containerColor = Bg,
        bottomBar = { SmartBottomBar("home", onNavigate = onNavigate) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(HeaderBrush)
                    .statusBarsPadding()
                    .padding(bottom = 36.dp)
            ) {
                Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    IconButton(onClick = { onNavigate("alerts") }) {
                        Icon(Icons.Filled.Notifications, null, tint = Color.White)
                    }
                    IconButton(onClick = { onNavigate("settings") }) {
                        Icon(Icons.Filled.Settings, null, tint = Color.White)
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Home, null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Text("Smart Home AI", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Giám sát và điều khiển ngôi nhà của bạn", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                }
            }

            Column(
                Modifier
                    .offset(y = (-24).dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniSensorCard(Icons.Filled.Thermostat, Color(0xFFFF7043), "Nhiệt độ", "${state.temperature}°C", Modifier.weight(1f)) { onNavigate("env") }
                    MiniSensorCard(Icons.Filled.WaterDrop, Blue500, "Độ ẩm", "${state.humidity}%", Modifier.weight(1f)) { onNavigate("env") }
                }
                MotionCard(state.motionDetected) { onNavigate("env") }

                Text("Điều khiển thiết bị", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickDevice(Icons.Filled.Lightbulb, Color(0xFFFFB300), "Đèn", if (state.light) "Bật" else "Tắt", state.light, Modifier.weight(1f)) {
                        onSetDevice(DeviceType.LIGHT, it)
                    }
                    QuickDevice(Icons.Filled.MeetingRoom, Blue600, "Cửa", if (state.doorOpen) "Mở" else "Đóng", state.doorOpen, Modifier.weight(1f)) {
                        onSetDevice(DeviceType.DOOR, it)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickDevice(Icons.Filled.NotificationsActive, RedAlert, "Còi báo", if (state.alarm) "Bật" else "Tắt", state.alarm, Modifier.weight(1f)) {
                        onSetDevice(DeviceType.ALARM, it)
                    }
                    Spacer(Modifier.weight(1f))
                }

                // Banner AI
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PurpleSoft)
                        .clickable { onNavigate("camera") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(Icons.Filled.Face, Color.White, Purple, 52.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("AI nhận diện khuôn mặt", fontWeight = FontWeight.SemiBold, color = TextDark)
                        Text("Đăng ký, quét và quản lý người dùng", fontSize = 12.sp, color = TextGray)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextGray)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MiniSensorCard(
    icon: ImageVector, tint: Color, label: String, value: String,
    modifier: Modifier, onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 12.sp, color = TextGray)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
        }
    }
}

@Composable
fun MotionCard(detected: Boolean, onClick: () -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (detected) RedSoft else GreenSoft)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(Icons.AutoMirrored.Filled.DirectionsRun, Color.White, if (detected) RedAlert else GreenOk, 44.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Chuyển động", fontSize = 12.sp, color = TextGray)
            Text(
                if (detected) "Phát hiện" else "Không phát hiện",
                fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark
            )
        }
    }
}

@Composable
private fun QuickDevice(
    icon: ImageVector, tint: Color, name: String, status: String, on: Boolean,
    modifier: Modifier, onToggle: (Boolean) -> Unit
) {
    WhiteCard(modifier) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(name, fontWeight = FontWeight.SemiBold, color = TextDark)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(status, fontSize = 12.sp, color = TextGray, modifier = Modifier.weight(1f))
            SmartSwitch(on, onToggle)
        }
    }
}
