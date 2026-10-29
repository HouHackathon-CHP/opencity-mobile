package org.houhackathon.opencity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.connectivity.ConnectionBuilder
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

private const val SESSION_KEY = "opencity-session"

/** Keeps OAuth tokens encrypted by the Android Keystore, outside backup. */
private class SessionStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("private_session", Context.MODE_PRIVATE)
    private val key: SecretKey by lazy {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(SESSION_KEY, null) as? SecretKey) ?: run {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder(SESSION_KEY,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            generator.generateKey()
        }
    }
    fun read(): String? = try {
        val raw = Base64.decode(prefs.getString("state", null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, raw.copyOfRange(0, 12)))
        String(cipher.doFinal(raw.copyOfRange(12, raw.size)), Charsets.UTF_8)
    } catch (_: Exception) { clear(); null }
    fun write(value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val blob = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("state", Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
    }
    fun clear() { prefs.edit().remove("state").apply() }
}

class AuthManager(context: Context) {
    private val store = SessionStore(context.applicationContext)
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder().callTimeout(5, TimeUnit.SECONDS).build()
    private val configuration = AuthorizationServiceConfiguration(
        Uri.parse("${BuildConfig.IDENTITY_URL}/realms/dxos/protocol/openid-connect/auth"),
        Uri.parse("${BuildConfig.IDENTITY_URL}/realms/dxos/protocol/openid-connect/token")
    )
    // HTTP is accepted only for the local emulator demo. Production URLs must use HTTPS.
    private val service = AuthorizationService(context, AppAuthConfiguration.Builder()
        .setConnectionBuilder(ConnectionBuilder { uri ->
            require(uri.scheme == "https" || (BuildConfig.DEBUG && uri.host in listOf("127.0.0.1", "localhost")))
            URL(uri.toString()).openConnection() as HttpURLConnection
        }).build())
    private var state = try { store.read()?.let { AuthState.jsonDeserialize(it) } ?: AuthState() }
        catch (_: Exception) { AuthState() }
    var signedIn by mutableStateOf(state.isAuthorized)
        private set

    fun signInIntent(): Intent {
        val request = AuthorizationRequest.Builder(configuration, "dxos-mobile", ResponseTypeValues.CODE,
            Uri.parse("opencity://auth")).setScope("openid profile email").build()
        return service.getAuthorizationRequestIntent(request)
    }

    fun completeSignIn(intent: Intent?, done: (String?) -> Unit) {
        if (intent == null) { done("Đăng nhập đã bị hủy."); return }
        val response = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)
        if (response == null) { done(error?.errorDescription ?: "Đăng nhập chưa hoàn tất."); return }
        state = AuthState(response, error)
        service.performTokenRequest(response.createTokenExchangeRequest()) { token, failure ->
            state.update(token, failure)
            if (token == null || !state.isAuthorized) done(failure?.errorDescription ?: "Không lấy được phiên đăng nhập.")
            else { store.write(state.jsonSerializeString()); signedIn = true; done(null) }
        }
    }

    suspend fun accessToken(): String = suspendCancellableCoroutine { continuation ->
        if (!state.isAuthorized) { continuation.resumeWith(Result.failure(IllegalStateException("Vui lòng đăng nhập."))); return@suspendCancellableCoroutine }
        val requestedState = state
        requestedState.performActionWithFreshTokens(service) { token, _, error ->
            if (state !== requestedState) {
                continuation.resumeWith(Result.failure(IllegalStateException("Đã đăng xuất.")))
                return@performActionWithFreshTokens
            }
            if (token == null) continuation.resumeWith(Result.failure(IllegalStateException(error?.errorDescription ?: "Phiên đăng nhập đã hết hạn.")))
            else { store.write(state.jsonSerializeString()); continuation.resume(token) }
        }
    }

    fun signOut() {
        val refreshToken = state.refreshToken
        state = AuthState()
        store.clear()
        signedIn = false
        if (refreshToken != null) background.launch {
            runCatching {
                val body = FormBody.Builder().add("client_id", "dxos-mobile")
                    .add("token", refreshToken).add("token_type_hint", "refresh_token").build()
                val req = Request.Builder().url("${BuildConfig.IDENTITY_URL}/realms/dxos/protocol/openid-connect/revoke")
                    .post(body).build()
                http.newCall(req).execute().close()
            }
        }
    }
    fun close() { background.cancel(); service.dispose() }
}
