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
    private val prefix = "${BuildConfig.GATEWAY_URL}/api/opencity"

    private suspend fun request(path: String, method: String = "GET", body: okhttp3.RequestBody? = null): String =
        withContext(Dispatchers.IO) {
            val token = auth.accessToken()
            val req = Request.Builder().url(prefix + path).header("Authorization", "Bearer $token")
                .method(method, body).build()
            http.newCall(req).execute().use { response ->
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
