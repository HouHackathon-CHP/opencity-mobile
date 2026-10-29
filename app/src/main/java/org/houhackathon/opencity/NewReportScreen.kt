package org.houhackathon.opencity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

private val categories = listOf("Hạ tầng", "Môi trường", "Giao thông", "Chiếu sáng", "Cây xanh", "Khác")
private const val DRAFT = "report-draft"

@Composable
fun NewReportScreen(api: ApiClient, context: Context, onCreated: (String) -> Unit) {
    val prefs = remember { context.getSharedPreferences("drafts", Context.MODE_PRIVATE) }
    val saved = remember { runCatching { JSONObject(prefs.getString(DRAFT, "{}") ?: "{}") }.getOrDefault(JSONObject()) }
    var title by remember { mutableStateOf(saved.optString("title")) }
    var description by remember { mutableStateOf(saved.optString("description")) }
    var address by remember { mutableStateOf(saved.optString("address")) }
    var category by remember { mutableStateOf(saved.optString("category", categories.first())) }
    var latitude by remember { mutableStateOf(saved.number("latitude")) }
    var longitude by remember { mutableStateOf(saved.number("longitude")) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo = it }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { if (it) photo = cameraUri }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) scope.launch {
            val point = withTimeoutOrNull(10_000) { currentLocation(context) }
            if (point == null) message = "Không lấy được GPS. Bạn vẫn có thể nhập địa chỉ bằng tay."
            else { latitude = point.first; longitude = point.second; message = "Đã gắn tọa độ GPS." }
        } else message = "Chưa được cấp quyền vị trí."
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Gửi phản ánh hiện trường", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(title, { title = it }, label = { Text("Tiêu đề *") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("Lĩnh vực")
        categories.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { name -> FilterChip(selected = category == name, onClick = { category = name }, label = { Text(name) }) }
            }
        }
        OutlinedTextField(description, { description = it }, label = { Text("Mô tả *") },
            modifier = Modifier.fillMaxWidth(), minLines = 4)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(address, { address = it }, label = { Text("Địa chỉ *") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }) { Text(if (latitude == null) "Lấy vị trí hiện tại" else "GPS: %.5f, %.5f".format(latitude, longitude)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { pickPhoto.launch("image/*") }) { Text("Chọn ảnh") }
            OutlinedButton(onClick = {
                val file = File(context.cacheDir, "photos/capture-${System.currentTimeMillis()}.jpg")
                file.parentFile?.mkdirs()
                cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                takePhoto.launch(cameraUri!!)
            }) { Text("Chụp ảnh") }
        }
        photo?.let { Text("Đã chọn ảnh: ${it.lastPathSegment.orEmpty()}") }
        message?.let { ErrorText(it) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = {
            val draft = JSONObject().put("title", title).put("description", description)
                .put("category", category).put("address", address)
            if (latitude != null && longitude != null) { draft.put("latitude", latitude); draft.put("longitude", longitude) }
            prefs.edit().putString(DRAFT, draft.toString()).apply()
            message = "Đã lưu bản nháp. Ảnh cần chọn lại khi gửi."
        }) { Text("Lưu bản nháp") }
        Button(enabled = !busy, onClick = {
            if (title.trim().length < 5 || description.trim().length < 15 || address.isBlank()) {
                message = "Tiêu đề tối thiểu 5 ký tự, mô tả tối thiểu 15 ký tự và cần địa chỉ."
            } else {
                busy = true; message = null
                scope.launch {
                    try {
                        val result = api.createReport(title, description, category, address, latitude, longitude, photo)
                        prefs.edit().remove(DRAFT).apply()
                        onCreated(result.text("id"))
                    } catch (e: Exception) { message = e.message ?: "Không gửi được phản ánh." }
                    finally { busy = false }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Đang gửi..." else "Gửi phản ánh") }
    }
}

private suspend fun currentLocation(context: Context): Pair<Double, Double>? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = when {
        fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return null
    }
    val last = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    if (last != null && System.currentTimeMillis() - last.time < 120_000) return last.latitude to last.longitude
    if (Build.VERSION.SDK_INT < 30) return last?.let { it.latitude to it.longitude }
    return suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { point ->
            if (continuation.isActive) continuation.resume(point?.let { it.latitude to it.longitude })
        }
    }
}
