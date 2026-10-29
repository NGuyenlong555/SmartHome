package com.example.smarthomeai.data

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Đăng nhập bằng Firebase Auth (Email/Mật khẩu và Google).
 * Chỉ hoạt động khi có app/google-services.json; nếu không, [configured] = false
 * và ViewModel chuyển sang chế độ demo.
 *
 * Mọi callback trả về `null` nếu thành công, hoặc thông báo lỗi tiếng Việt.
 */
class AuthRepository(private val context: Context) {

    val configured: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty()

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun isSignedIn(): Boolean = configured && auth.currentUser != null

    val currentEmail: String?
        get() = if (configured) auth.currentUser?.let { it.email ?: it.displayName } else null

    fun login(email: String, password: String, onResult: (String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { onResult(it.exception?.let(::describe)) }
    }

    fun register(email: String, password: String, onResult: (String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { onResult(it.exception?.let(::describe)) }
    }

    fun signInWithGoogle(activity: Activity, done: (String?) -> Unit) {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId == 0) {
            done("Chưa bật Google Sign-In trong Firebase (thiếu web client ID)")
            return
        }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(resId))
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        CredentialManager.create(activity).getCredentialAsync(
            activity, request, CancellationSignal(), ContextCompat.getMainExecutor(activity),
            object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                override fun onResult(result: GetCredentialResponse) {
                    val credential = result.credential
                    if (credential is CustomCredential &&
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    ) {
                        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                            .addOnCompleteListener { done(it.exception?.let(::describe)) }
                    } else {
                        done("Không lấy được tài khoản Google")
                    }
                }

                override fun onError(e: GetCredentialException) {
                    done(
                        if (e is GetCredentialCancellationException) "Đã hủy đăng nhập Google"
                        else "Đăng nhập Google thất bại: ${e.message ?: e.type}"
                    )
                }
            }
        )
    }

    fun signOut() {
        if (configured) auth.signOut()
    }

    private fun describe(e: Exception): String = when (e) {
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Sai email hoặc mật khẩu"
        is FirebaseAuthUserCollisionException -> "Email này đã được đăng ký"
        is FirebaseAuthWeakPasswordException -> "Mật khẩu tối thiểu 6 ký tự"
        is FirebaseNetworkException -> "Không có kết nối mạng"
        else -> e.message ?: "Có lỗi xảy ra, vui lòng thử lại"
    }
}
