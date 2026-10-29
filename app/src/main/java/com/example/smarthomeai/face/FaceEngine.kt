package com.example.smarthomeai.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/** Hộp khuôn mặt, toạ độ chuẩn hoá 0..1 theo ảnh đã xoay thẳng đứng (chưa lật gương). */
data class NormBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

/** [embedding] = null khi chưa có file mô hình (chỉ phát hiện, không định danh được). */
class FaceResult(val box: NormBox, val embedding: FloatArray?)

class FaceMatch(val name: String, val score: Float)

/**
 * Phát hiện khuôn mặt bằng ML Kit, rồi (nếu có mô hình) tạo vector đặc trưng bằng
 * MobileFaceNet TFLite để so khớp với người đã đăng ký.
 *
 * Đặt file mô hình tại: app/src/main/assets/mobile_face_net.tflite
 * (đầu vào 112x112x3, đầu ra vector 192 chiều).
 */
class FaceEngine(context: Context) {

    companion object {
        const val MODEL_FILE = "mobile_face_net.tflite"
        private const val ANALYZE_INTERVAL_MS = 250L
    }

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.25f)
            .build()
    )

    private val interpreter: Interpreter? = try {
        val fd = context.assets.openFd(MODEL_FILE)
        val buffer = FileInputStream(fd.fileDescriptor).channel
            .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        Interpreter(buffer)
    } catch (e: Exception) {
        null
    }

    private val inputSize = interpreter?.getInputTensor(0)?.shape()?.getOrNull(1) ?: 112
    private val outputSize = interpreter?.getOutputTensor(0)?.shape()?.getOrNull(1) ?: 192

    @Volatile
    private var closed = false
    private var lastRun = 0L

    val modelReady: Boolean get() = interpreter != null

    /** Analyzer cho CameraX ImageAnalysis. [onResult] được gọi trên main thread. */
    fun analyzer(onResult: (FaceResult?) -> Unit) = ImageAnalysis.Analyzer { proxy ->
        val now = SystemClock.elapsedRealtime()
        if (closed || now - lastRun < ANALYZE_INTERVAL_MS) {
            proxy.close()
            return@Analyzer
        }
        lastRun = now

        val bitmap = try {
            rotate(proxy.toBitmap(), proxy.imageInfo.rotationDegrees)
        } catch (e: Exception) {
            null
        } finally {
            proxy.close()
        }
        if (bitmap == null) return@Analyzer

        detector.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { faces ->
                if (closed) return@addOnSuccessListener
                val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                if (face == null) {
                    onResult(null)
                    return@addOnSuccessListener
                }
                val r = face.boundingBox
                val box = NormBox(
                    r.left.toFloat() / bitmap.width,
                    r.top.toFloat() / bitmap.height,
                    r.right.toFloat() / bitmap.width,
                    r.bottom.toFloat() / bitmap.height
                )
                onResult(FaceResult(box, embeddingOf(bitmap, r)))
            }
            .addOnFailureListener { if (!closed) onResult(null) }
    }

    private fun embeddingOf(source: Bitmap, rect: android.graphics.Rect): FloatArray? {
        val net = interpreter ?: return null
        val left = rect.left.coerceIn(0, source.width - 1)
        val top = rect.top.coerceIn(0, source.height - 1)
        val width = rect.width().coerceAtMost(source.width - left)
        val height = rect.height().coerceAtMost(source.height - top)
        if (width < 40 || height < 40) return null

        return try {
            val crop = Bitmap.createBitmap(source, left, top, width, height)
            val scaled = Bitmap.createScaledBitmap(crop, inputSize, inputSize, true)
            val input = ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4)
                .order(ByteOrder.nativeOrder())
            val pixels = IntArray(inputSize * inputSize)
            scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            for (p in pixels) {
                input.putFloat((((p shr 16) and 0xFF) - 128f) / 128f)
                input.putFloat((((p shr 8) and 0xFF) - 128f) / 128f)
                input.putFloat(((p and 0xFF) - 128f) / 128f)
            }
            val output = Array(1) { FloatArray(outputSize) }
            synchronized(net) { net.run(input, output) }
            normalize(output[0])
        } catch (e: Exception) {
            null
        }
    }

    /** Trung bình nhiều vector khi đăng ký để ổn định hơn. */
    fun average(samples: List<FloatArray>): FloatArray {
        val sum = FloatArray(samples.first().size)
        samples.forEach { s -> for (i in sum.indices) sum[i] += s[i] }
        return normalize(sum)
    }

    fun close() {
        closed = true
        detector.close()
        interpreter?.close()
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    private fun normalize(v: FloatArray): FloatArray {
        var sum = 0f
        for (x in v) sum += x * x
        val norm = sqrt(sum).coerceAtLeast(1e-9f)
        return FloatArray(v.size) { v[it] / norm }
    }
}

/** Lưu vector khuôn mặt đã đăng ký trong SharedPreferences. */
class FaceStore(context: Context) {

    companion object {
        /** Ngưỡng cosine để coi là cùng một người. Tăng nếu nhận nhầm, giảm nếu hay báo "người lạ". */
        const val THRESHOLD = 0.70f
    }

    private val prefs = context.getSharedPreferences("face_store", Context.MODE_PRIVATE)

    fun save(name: String, embedding: FloatArray) {
        prefs.edit().putString(name, embedding.joinToString(",")).apply()
    }

    fun remove(name: String) {
        prefs.edit().remove(name).apply()
    }

    fun names(): List<String> = prefs.all.keys.sorted()

    fun match(embedding: FloatArray): FaceMatch? {
        var best: FaceMatch? = null
        for ((name, value) in prefs.all) {
            val stored = (value as? String)?.split(",")?.mapNotNull { it.toFloatOrNull() }?.toFloatArray()
                ?: continue
            if (stored.size != embedding.size) continue
            var dot = 0f
            for (i in stored.indices) dot += stored[i] * embedding[i]
            if (best == null || dot > best.score) best = FaceMatch(name, dot)
        }
        return best?.takeIf { it.score >= THRESHOLD }
    }
}
