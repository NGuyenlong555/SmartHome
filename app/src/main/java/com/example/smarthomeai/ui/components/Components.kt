package com.example.smarthomeai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomeai.ui.theme.*

/** Thanh tiêu đề xanh có nút quay lại. */
@Composable
fun GradientTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(HeaderBrush)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
        } else Spacer(Modifier.width(12.dp))
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, color = Blue600, fontSize = 13.sp, modifier = Modifier.clickable { onAction() })
        }
    }
}

@Composable
fun WhiteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .shadow(2.dp, RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .padding(14.dp),
        content = content
    )
}

@Composable
fun IconBadge(icon: ImageVector, tint: Color, bg: Color, size: Dp = 44.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun SmartSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = GreenOk,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFC9CFD8),
            uncheckedBorderColor = Color.Transparent,
            checkedBorderColor = Color.Transparent
        )
    )
}

@Composable
fun OnOffButtons(
    isOn: Boolean,
    onLabel: String,
    offLabel: String,
    onClick: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SegButton(onLabel, isOn, Modifier.weight(1f)) { onClick(true) }
        SegButton(offLabel, !isOn, Modifier.weight(1f)) { onClick(false) }
    }
}

@Composable
private fun SegButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Blue600 else OffGray)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else TextGray,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue600)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
    }
}

/** Thanh điều hướng dưới. [third] = "camera" hoặc "alerts". */
@Composable
fun SmartBottomBar(current: String, third: String = "camera", onNavigate: (String) -> Unit) {
    data class Item(val route: String, val label: String, val icon: ImageVector)

    val thirdItem = if (third == "alerts")
        Item("alerts", "Cảnh báo", Icons.Filled.Notifications)
    else Item("camera", "Camera", Icons.Filled.Videocam)

    val items = listOf(
        Item("home", "Trang chủ", Icons.Filled.Home),
        Item("devices", "Thiết bị", Icons.Filled.Devices),
        thirdItem,
        Item("settings", "Cài đặt", Icons.Filled.Settings)
    )
    NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
        items.forEach { item ->
            NavigationBarItem(
                selected = current == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, null) },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Blue600,
                    selectedTextColor = Blue600,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = TextGray,
                    unselectedTextColor = TextGray
                )
            )
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text,
        color = TextGray,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    )
}
