package com.example.smarthomeai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.smarthomeai.data.FaceRecord
import com.example.smarthomeai.data.SmartHomeState
import com.example.smarthomeai.face.FaceEngine
import com.example.smarthomeai.face.FaceResult
import com.example.smarthomeai.face.FaceStore
import com.example.smarthomeai.ui.components.*
import com.example.smarthomeai.ui.theme.*
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private const val REGISTER_SAMPLES = 8
private const val LOG_COOLDOWN_MS = 15_000L
private const val SCAN_TIMEOUT_MS = 20_000L

/** Kết quả hiển thị trên khung camera. */
private class LiveFace(val result: FaceResult, val name: String?, val score: Int)

/**
 * Xin quyền camera và hiển thị preview CameraX.
 * Nếu truyền [engine] + [onFace] thì mỗi khung hình được phân tích khuôn mặt.
 */
@Composable
fun CameraPreviewBox(
    modifier: Modifier = Modifier,
    front: Boolean = true,
    engine: FaceEngine? = null,
    onFace: ((FaceResult?) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFace by rememberUpdatedState(onFace)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val providerHolder = remember { arrayOfNulls<ProcessCameraProvider>(1) }

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }

    DisposableEffect(Unit) {
        onDispose {
            providerHolder[0]?.unbindAll()
            executor.shutdown()
        }
    }

    Box(modifier.background(Color(0xFF263040)), contentAlignment = Alignment.Center) {
        if (granted) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val view = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        val provider = future.get()
                        providerHolder[0] = provider
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                        val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            provider.unbindAll()
                            if (engine != null) {
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { a ->
                                        a.setAnalyzer(executor, engine.analyzer { r -> currentOnFace?.invoke(r) })
                                    }
                                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                            } else {
                                provider.bindToLifecycle(lifecycleOwner, selector, preview)
                            }
                        } catch (_: Exception) {
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    view
                }
            )
        } else {
            Text("Cần cấp quyền camera", color = Color.White)
        }
    }
}

@Composable
fun CameraScreen(
    state: SmartHomeState,
    onRecognized: (name: String?, accuracy: Int) -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val engine = remember { FaceEngine(context) }
    val store = remember { FaceStore(context) }
    DisposableEffect(Unit) { onDispose { engine.close() } }

    var live by remember { mutableStateOf<LiveFace?>(null) }
    var lastLogKey by remember { mutableStateOf("") }
    var lastLogTime by remember { mutableLongStateOf(0L) }

    val handleFace: (FaceResult?) -> Unit = { r ->
        if (r == null) {
            live = null
        } else {
            val emb = r.embedding
            val match = emb?.let { store.match(it) }
            live = LiveFace(r, match?.name, ((match?.score ?: 0f) * 100).toInt().coerceIn(0, 100))
            if (emb != null) {
                val key = match?.name ?: "#stranger"
                val now = SystemClock.elapsedRealtime()
                if (key != lastLogKey || now - lastLogTime > LOG_COOLDOWN_MS) {
                    lastLogKey = key
                    lastLogTime = now
                    onRecognized(match?.name, ((match?.score ?: 0f) * 100).toInt().coerceIn(0, 100))
                }
            }
        }
    }

    Scaffold(
        containerColor = Bg,
        topBar = { GradientTopBar("Camera & AI", onBack) },
        bottomBar = { SmartBottomBar("camera", onNavigate = onNavigate) }
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    CameraPreviewBox(Modifier.fillMaxSize(), engine = engine, onFace = handleFace)
                    FaceOverlay(live, engine.modelReady, mirror = true, modifier = Modifier.fillMaxSize())
                    StatusChip(live, engine.modelReady, Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp))
                }
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GreenSoft)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconBadge(Icons.Filled.Person, Color.White, GreenOk, 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Người dùng", fontSize = 11.sp, color = TextGray)
                        Text(state.recognizedName, fontWeight = FontWeight.Bold, color = TextDark)
                        Text("Độ chính xác: ${state.recognizedAccuracy}%", fontSize = 12.sp, color = TextGray)
                    }
                }
            }
            if (!engine.modelReady) {
                item {
                    Text(
                        "Chưa có mô hình nhận diện. Chép file ${FaceEngine.MODEL_FILE} vào app/src/main/assets " +
                            "để định danh người dùng (hiện chỉ phát hiện khuôn mặt).",
                        color = RedAlert, fontSize = 12.sp
                    )
                }
            }
            item { PrimaryButton("Đăng ký người dùng", onClick = onRegister) }
            item { SectionTitle("Lịch sử nhận diện", action = "Xem tất cả") }
            if (state.faces.isEmpty()) item { EmptyHint("Chưa có lần nhận diện nào") }
            else items(state.faces) { FaceRow(it) }
        }
    }
}

@Composable
private fun FaceOverlay(live: LiveFace?, modelReady: Boolean, mirror: Boolean, modifier: Modifier) {
    val face = live ?: return
    val color = when {
        !modelReady -> OrangeWarn
        face.name != null -> GreenOk
        else -> RedAlert
    }
    Canvas(modifier) {
        val b = face.result.box
        val l = if (mirror) 1f - b.right else b.left
        val r = if (mirror) 1f - b.left else b.right
        drawRoundRect(
            color = color,
            topLeft = Offset(l * size.width, b.top * size.height),
            size = Size((r - l) * size.width, (b.bottom - b.top) * size.height),
            cornerRadius = CornerRadius(16.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

@Composable
private fun StatusChip(live: LiveFace?, modelReady: Boolean, modifier: Modifier) {
    val (text, color) = when {
        live == null -> "Không thấy khuôn mặt" to Color(0xFF596B83)
        !modelReady -> "Phát hiện khuôn mặt" to OrangeWarn
        live.name != null -> "Đã nhận diện: ${live.name} (${live.score}%)" to GreenOk
        else -> "Người lạ" to RedAlert
    }
    Text(
        text,
        color = Color.White, fontSize = 12.sp,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun FaceRow(f: FaceRecord) {
    WhiteCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                Icons.Filled.Person, Color.White,
                if (f.known) Blue600 else RedAlert, 40.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(f.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextDark)
                Text(f.time, fontSize = 11.sp, color = TextGray)
            }
            Text(
                if (f.known) "Đã nhận diện" else "Cảnh báo",
                fontSize = 11.sp,
                color = if (f.known) GreenOk else RedAlert,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (f.known) GreenSoft else RedSoft)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun FaceRegisterScreen(onBack: () -> Unit, onRegistered: (String) -> Unit) {
    val context = LocalContext.current
    val engine = remember { FaceEngine(context) }
    val store = remember { FaceStore(context) }
    DisposableEffect(Unit) { onDispose { engine.close() } }

    var name by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var collected by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var faceVisible by remember { mutableStateOf(false) }
    val samples = remember { mutableListOf<FloatArray>() }

    val onFace: (FaceResult?) -> Unit = { r ->
        val emb = r?.embedding
        if (scanning) faceVisible = r != null
        if (scanning && emb != null && samples.size < REGISTER_SAMPLES) {
            samples.add(emb)
            collected = samples.size
        }
    }

    // Quá 20 giây mà chưa đủ mẫu -> dừng và báo lỗi, không để treo ở trạng thái đang quét
    LaunchedEffect(scanning) {
        if (scanning) {
            delay(SCAN_TIMEOUT_MS)
            if (scanning && collected < REGISTER_SAMPLES) {
                scanning = false
                samples.clear()
                collected = 0
                message = "Không nhận diện được khuôn mặt. Hãy đưa mặt vào giữa khung, " +
                    "ở nơi đủ sáng, bỏ khẩu trang/kính rồi thử lại."
            }
        }
    }

    LaunchedEffect(collected, scanning) {
        if (scanning && collected >= REGISTER_SAMPLES) {
            val finalName = name.trim()
            store.save(finalName, engine.average(samples))
            onRegistered(finalName)
            scanning = false
            onBack()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = { GradientTopBar("Đăng ký khuôn mặt", onBack) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .size(230.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6ECF5)),
                contentAlignment = Alignment.Center
            ) {
                if (scanning) {
                    CameraPreviewBox(Modifier.fillMaxSize(), engine = engine, onFace = onFace)
                } else {
                    Icon(Icons.Filled.Face, null, tint = Blue600, modifier = Modifier.size(110.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                when {
                    !scanning -> "Vui lòng nhìn thẳng vào camera"
                    !faceVisible -> "Chưa thấy khuôn mặt, hãy đưa mặt vào giữa khung"
                    else -> "Đang quét $collected/$REGISTER_SAMPLES... xoay nhẹ đầu"
                },
                fontWeight = FontWeight.Medium, color = TextDark
            )
            if (scanning) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { collected / REGISTER_SAMPLES.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = Blue600
                )
            }
            message?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = RedAlert, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Tên người dùng") },
                singleLine = true,
                enabled = !scanning,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            WhiteCard(Modifier.fillMaxWidth()) {
                listOf(
                    "Đảm bảo khuôn mặt được chiếu sáng tốt",
                    "Không đeo khẩu trang, kính râm",
                    "Giữ khuôn mặt trong khung hình"
                ).forEach {
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = GreenOk, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(it, fontSize = 13.sp, color = TextDark)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(if (scanning) "Hủy quét" else "Bắt đầu quét") {
                if (scanning) {
                    scanning = false
                    samples.clear()
                    collected = 0
                } else when {
                    name.isBlank() -> message = "Vui lòng nhập tên người dùng"
                    !engine.modelReady -> message =
                        "Thiếu file ${FaceEngine.MODEL_FILE} trong app/src/main/assets"
                    else -> {
                        message = null
                        samples.clear()
                        collected = 0
                        faceVisible = false
                        scanning = true
                    }
                }
            }
        }
    }
}
