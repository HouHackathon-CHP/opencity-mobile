// SPDX-License-Identifier: MIT
package org.houhackathon.opencity

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable
fun DigitalWardScreen(api: ApiClient, revision: Int, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var wards by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(revision, reload) {
        loading = true
        runCatching { api.list("/digital-wards") }
            .onSuccess { wards = it; error = "" }
            .onFailure { error = it.message ?: "Không tải được xã/phường số." }
        loading = false
    }

    val visible = wards.filter { ward ->
        val needle = query.trim().lowercase()
        needle.isBlank() || listOf("code", "name", "address")
            .any { key -> ward.text(key).lowercase().contains(needle) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("Xã/phường số", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold, color = OpenCityColors.TextMain)
        Text("Thông tin điểm phục vụ do đơn vị vận hành cập nhật.", color = OpenCityColors.TextSecondary)
        OutlinedTextField(value = query, onValueChange = { query = it },
            label = { Text("Tìm tên, mã hoặc địa chỉ") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        TextButton(onClick = { reload++ }) { Text("Làm mới") }
        if (error.isNotBlank()) ErrorBanner(error)
        if (loading) CircularProgressIndicator()
        if (!loading && visible.isEmpty()) EmptyPlaceholder("Chưa có điểm phục vụ phù hợp.")

        visible.forEach { ward ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ward.text("name"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(ward.text("description"), color = OpenCityColors.TextSecondary)
                    Text("Địa chỉ: ${ward.text("address")}")
                    Text("Giờ làm việc: ${ward.text("office_hours")}")
                    val phone = ward.text("phone")
                    Button(onClick = {
                        val number = phone.filter { it.isDigit() || it == '+' }
                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))) }
                            .onFailure { error = "Không mở được ứng dụng gọi điện." }
                    }) { Text("Gọi $phone") }
                    val lat = ward.optDouble("latitude", Double.NaN)
                    val lon = ward.optDouble("longitude", Double.NaN)
                    if (lat.isFinite() && lon.isFinite()) {
                        TextButton(onClick = {
                            val geo = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) }
                                .onFailure { error = "Thiết bị chưa có ứng dụng bản đồ." }
                        }) { Text("Mở vị trí trên bản đồ") }
                    }
                    TextButton(onClick = { onNavigate("appointments") }) { Text("Đặt lịch tiếp công dân") }
                    TextButton(onClick = { onNavigate("administrative-feedback") }) { Text("Phản ánh thủ tục hành chính") }
                    TextButton(onClick = { onNavigate("hotlines") }) { Text("Danh bạ đường dây nóng") }
                }
            }
        }
    }
}
