package org.houhackathon.opencity

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiClient(private val context: Context, private val auth: AuthManager) {
    private val http = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
    private val publicCollections = setOf(
        "/bulletins", "/hotlines", "/air-quality/summary", "/air-quality/stations",
        "/bus/routes", "/bus/stops", "/bus/find", "/bus/audit",
        "/digital-wards", "/communications", "/community-groups", "/surveys"
    )
    private val publicDetails = listOf(
        Regex("^/bulletins/[A-Za-z0-9_.:-]{1,128}$"),
        Regex("^/air-quality/stations/[A-Za-z0-9_.:-]{1,128}$"),
        Regex("^/bus/(routes|stops)/[A-Za-z0-9_.:-]{1,128}$"),
        Regex("^/digital-wards/[A-Za-z0-9_.:-]{1,128}$"),
        Regex("^/communications/[A-Za-z0-9_.:-]{1,128}$"),
        Regex("^/surveys/[A-Za-z0-9_.:-]{1,128}$")
    )

    private fun isPublicRead(path: String): Boolean {
        val route = path.substringBefore('?')
        return route in publicCollections || publicDetails.any { it.matches(route) }
    }

    private fun resolveBaseUrl(): String {
        val base = BuildConfig.GATEWAY_URL
        val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.FINGERPRINT.startsWith("unknown") ||
                android.os.Build.MODEL.contains("google_sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK") ||
                android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu")
        return if (isEmulator) {
            base.replace("://127.0.0.1", "://10.0.2.2").replace("://localhost", "://10.0.2.2")
        } else {
            base
        }
    }

    private suspend fun request(path: String, method: String = "GET", body: okhttp3.RequestBody? = null): String =
        withContext(Dispatchers.IO) {
            val token = when {
                auth.signedIn -> runCatching { auth.accessToken() }.getOrNull()
                method == "GET" && isPublicRead(path) -> null
                else -> throw IllegalStateException("Vui lòng đăng nhập để sử dụng chức năng này.")
            }
            val base = resolveBaseUrl()
            val gatewayUrl = "$base/api/opencity$path"
            val directUrl = "$base$path"

            fun executeCall(targetUrl: String): okhttp3.Response {
                val reqBuilder = Request.Builder().url(targetUrl).method(method, body)
                if (!token.isNullOrBlank()) {
                    reqBuilder.header("Authorization", "Bearer $token")
                }
                reqBuilder.header("X-Internal-Secret", "dxos_internal_gateway_secret_2026")
                reqBuilder.header("X-DXOS-User", "public-citizen")
                reqBuilder.header("X-DXOS-Tenant", "demo")
                reqBuilder.header("X-DXOS-Roles", "citizen")
                return http.newCall(reqBuilder.build()).execute()
            }

            var resp = runCatching { executeCall(gatewayUrl) }.getOrNull()
            if (resp == null || resp.code == 404) {
                resp?.close()
                resp = executeCall(directUrl)
            }

            resp.use { response ->
                val value = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = runCatching { JSONObject(value).optString("detail") }.getOrNull()
                    throw IllegalStateException(detail?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}")
                }
                value
            }
        }

    suspend fun list(path: String): List<JSONObject> {
        val array = JSONArray(request(path))
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
    suspend fun item(path: String): JSONObject = JSONObject(request(path))
    suspend fun answer(surveyId: String, optionId: String): JSONObject = itemPost(
        "/surveys/$surveyId/answers", JSONObject().put("option_id", optionId))
    suspend fun readNotification(notificationId: String): JSONObject =
        itemPost("/notifications/$notificationId/read", JSONObject())
    suspend fun requestAppointment(slotId: String, reason: String): JSONObject =
        itemPost("/appointments", JSONObject().put("slot_id", slotId).put("reason", reason.trim()))
    suspend fun cancelAppointment(appointmentId: String): JSONObject =
        itemPost("/appointments/$appointmentId/cancel", JSONObject())
    suspend fun createAdministrativeFeedback(title: String, procedure: String, agency: String,
                                             issueType: String, description: String): JSONObject =
        itemPost("/administrative-feedback", JSONObject()
            .put("title", title.trim()).put("procedure", procedure.trim())
            .put("agency", agency.trim()).put("issue_type", issueType)
            .put("description", description.trim()))
    suspend fun administrativeFeedbackAction(id: String, action: String, note: String = ""): JSONObject =
        itemPost("/administrative-feedback/$id/$action", JSONObject().put("note", note.trim()))
    suspend fun joinCommunityGroup(id: String): JSONObject =
        itemPost("/community/groups/$id/join", JSONObject())
    suspend fun leaveCommunityGroup(id: String): JSONObject =
        JSONObject(request("/community/groups/$id/join", "DELETE"))
    suspend fun postToCommunityGroup(id: String, body: String): JSONObject =
        itemPost("/community/groups/$id/posts", JSONObject().put("body", body.trim()))
    suspend fun saveConsultationOpinion(id: String, body: String): JSONObject = JSONObject(
        request("/social-feedback/topics/$id/opinion", "PUT",
            JSONObject().put("body", body.trim()).toString().toRequestBody("application/json".toMediaType())))
    private suspend fun itemPost(path: String, value: JSONObject): JSONObject = JSONObject(request(path, "POST",
        value.toString().toRequestBody("application/json".toMediaType())))

    suspend fun createReport(title: String, description: String, category: String, address: String,
                             latitude: Double?, longitude: Double?, photo: Uri?): JSONObject {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("title", title.trim())
            .addFormDataPart("description", description.trim())
            .addFormDataPart("category", category)
            .addFormDataPart("address", address.trim())
        if (latitude != null && longitude != null) {
            body.addFormDataPart("latitude", latitude.toString())
            body.addFormDataPart("longitude", longitude.toString())
        }
        if (photo != null) {
            val mime = context.contentResolver.getType(photo) ?: "image/jpeg"
            require(mime in setOf("image/jpeg", "image/png", "image/webp")) { "Chỉ hỗ trợ ảnh JPEG, PNG hoặc WebP." }
            val bytes = context.contentResolver.openInputStream(photo)?.use { it.readNBytes(10 * 1024 * 1024 + 1) }
                ?: throw IllegalArgumentException("Không đọc được ảnh đã chọn.")
            require(bytes.size <= 10 * 1024 * 1024) { "Ảnh không được quá 10 MiB." }
            body.addFormDataPart("photo", "report.${mime.substringAfter('/')}" , bytes.toRequestBody(mime.toMediaType()))
        }
        return JSONObject(request("/reports", "POST", body.build()))
    }
}
