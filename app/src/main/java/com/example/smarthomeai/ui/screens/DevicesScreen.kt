package com.example.smarthomeai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.example.smarthomeai.data.DeviceType
import com.example.smarthomeai.data.SmartHomeState
import com.example.smarthomeai.ui.components.*
import com.example.smarthomeai.ui.theme.*

@Composable
fun DevicesScreen(
    state: SmartHomeState,
    onSetDevice: (DeviceType, Boolean) -> Unit,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        containerColor = Bg,
        topBar = { GradientTopBar("Điều khiển thiết bị", onBack) },
        bottomBar = { SmartBottomBar("devices", onNavigate = onNavigate) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DeviceCard(
                Icons.Filled.Lightbulb, Color.White, Color(0xFFFFB300),
                "Đèn", "Bật / Tắt đèn trong nhà", state.light, "Bật", "Tắt"
            ) { onSetDevice(DeviceType.LIGHT, it) }

            DeviceCard(
                Icons.Filled.MeetingRoom, Color.White, Color(0xFFFF7043),
                "Cửa", "Mở / Đóng cửa", state.doorOpen, "Mở", "Đóng"
            ) { onSetDevice(DeviceType.DOOR, it) }

            DeviceCard(
                Icons.Filled.NotificationsActive, Color.White, RedAlert,
                "Còi báo", "Bật / Tắt còi cảnh báo", state.alarm, "Bật", "Tắt"
            ) { onSetDevice(DeviceType.ALARM, it) }
        }
    }
}

@Composable
private fun DeviceCard(
    icon: ImageVector, iconTint: Color, iconBg: Color,
    title: String, subtitle: String, on: Boolean,
    onLabel: String, offLabel: String, onChange: (Boolean) -> Unit
) {
    WhiteCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, iconTint, iconBg, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                Text(subtitle, fontSize = 12.sp, color = TextGray)
            }
            SmartSwitch(on, onChange)
        }
        Spacer(Modifier.height(12.dp))
        OnOffButtons(on, onLabel, offLabel, onChange)
    }
}
