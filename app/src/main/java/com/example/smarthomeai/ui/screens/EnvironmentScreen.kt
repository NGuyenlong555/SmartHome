package com.example.smarthomeai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomeai.data.LogItem
import com.example.smarthomeai.data.SmartHomeState
import com.example.smarthomeai.ui.components.*
import com.example.smarthomeai.ui.theme.*

@Composable
fun EnvironmentScreen(state: SmartHomeState, onBack: () -> Unit, onNavigate: (String) -> Unit) {
    Scaffold(
        containerColor = Bg,
        topBar = { GradientTopBar("Giám sát môi trường", onBack) },
        bottomBar = { SmartBottomBar("env", onNavigate = onNavigate) }
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigSensor(Icons.Filled.Thermostat, RedAlert, TempSoft, "Nhiệt độ", "${state.temperature}°C", Color(0xFFE5532D), Modifier.weight(1f))
                    BigSensor(Icons.Filled.WaterDrop, Blue500, HumSoft, "Độ ẩm", "${state.humidity}%", Blue600, Modifier.weight(1f))
                }
            }
            item { MotionCard(state.motionDetected) }
            item {
                Spacer(Modifier.height(4.dp))
                SectionTitle("Lịch sử dữ liệu", action = "Xem tất cả")
            }
            if (state.logs.isEmpty()) item { EmptyHint("Chưa có dữ liệu. Lịch sử sẽ xuất hiện khi bạn điều khiển thiết bị hoặc ESP32 gửi dữ liệu.") }
            else items(state.logs) { log -> LogRow(log) }
        }
    }
}

@Composable
private fun BigSensor(
    icon: ImageVector, tint: Color, bg: Color, label: String, value: String,
    valueColor: Color, modifier: Modifier
) {
    Column(
        modifier
            .height(150.dp)
            .background(bg, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, Color.White, tint, 40.dp)
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, color = TextDark)
        }
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = valueColor, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun LogRow(log: LogItem) {
    val (icon, tint, bg) = when (log.kind) {
        "temp" -> Triple(Icons.Filled.Thermostat, RedAlert, TempSoft)
        "hum" -> Triple(Icons.Filled.WaterDrop, Blue500, HumSoft)
        "motion" -> Triple(Icons.AutoMirrored.Filled.DirectionsRun, GreenOk, GreenSoft)
        "light" -> Triple(Icons.Filled.Lightbulb, OrangeWarn, YellowSoft)
        "alarm" -> Triple(Icons.Filled.NotificationsActive, RedAlert, RedSoft)
        else -> Triple(Icons.Filled.MeetingRoom, Blue600, BlueSoft)
    }
    WhiteCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, tint, bg, 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(log.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark)
                Text(log.time, fontSize = 11.sp, color = TextGray)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextGray)
        }
    }
}
