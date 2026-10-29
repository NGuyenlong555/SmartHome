package com.example.smarthomeai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smarthomeai.ui.theme.*

/**
 * @param demoMode true khi chưa cấu hình Firebase (mọi tài khoản không rỗng đều vào được)
 * @param onSubmit (email, mật khẩu, đăng ký?, callback lỗi/null)
 * @param onGoogle (callback lỗi/null)
 */
@Composable
fun LoginScreen(
    demoMode: Boolean,
    onSubmit: (String, String, Boolean, (String?) -> Unit) -> Unit,
    onGoogle: ((String?) -> Unit) -> Unit,
    onSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var registerMode by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val finish: (String?) -> Unit = { err ->
        loading = false
        error = err
        if (err == null) onSuccess()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(LoginBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))
        Icon(Icons.Filled.Home, null, tint = Color.White, modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(8.dp))
        Text("Smart Home AI", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Ngôi nhà thông minh trong tầm tay", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
        ) {
            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("Email", color = TextGray) },
                leadingIcon = { Icon(Icons.Filled.Person, null, tint = Blue600) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(color = OffGray)
            TextField(
                value = pass,
                onValueChange = { pass = it },
                placeholder = { Text("Mật khẩu", color = TextGray) },
                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Blue600) },
                trailingIcon = {
                    IconButton(onClick = { show = !show }) {
                        Icon(if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = TextGray)
                    }
                },
                visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                colors = fieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = Color(0xFFFFE0E0), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        if (demoMode) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Chế độ demo: chưa cấu hình Firebase nên mọi tài khoản đều vào được",
                color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                loading = true
                error = null
                onSubmit(email, pass, registerMode, finish)
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue600)
        ) {
            Text(
                when {
                    loading -> "Đang xử lý..."
                    registerMode -> "Đăng ký"
                    else -> "Đăng nhập"
                },
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(12.dp))
        Text("Hoặc", color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                loading = true
                error = null
                onGoogle(finish)
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = TextDark),
            border = null
        ) {
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text("Đăng nhập bằng Google", fontSize = 15.sp)
        }

        Spacer(Modifier.height(28.dp))
        Text(
            if (registerMode) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký",
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable(enabled = !loading) {
                    registerMode = !registerMode
                    error = null
                }
                .padding(8.dp)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)
