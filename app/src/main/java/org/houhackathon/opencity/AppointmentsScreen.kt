// SPDX-License-Identifier: MIT
package org.houhackathon.opencity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

private fun appointmentTime(value: String): String = runCatching {
    val withoutFraction = value.replace(Regex("\\.\\d+(?=[+-]\\d{2}:\\d{2}|Z$)"), "")
    val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(withoutFraction)
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN")).format(parsed!!)
}.getOrElse { value.replace("T", " ").take(16) }

private fun appointmentStatus(value: String): String = when (value) {
    "pending" -> "Chờ xác nhận"
    "confirmed" -> "Đã xác nhận"
    "rejected" -> "Đã từ chối"
    "cancelled" -> "Đã hủy"
    else -> value
}

@Composable
fun AppointmentsScreen(api: ApiClient, revision: Int) {
    var slots by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedSlot by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(revision, refreshKey) {
        loading = true
        runCatching {
            slots = api.list("/appointments/slots")
            appointments = api.list("/appointments")
        }.onSuccess { error = "" }
            .onFailure { error = it.message ?: "Không tải được lịch tiếp công dân." }
        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Đăng ký tiếp công dân", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold, color = OpenCityColors.TextMain)
        Text("Chọn khung giờ, gửi nội dung cần trao đổi và theo dõi xác nhận. Lịch này chỉ dùng trong hệ thống thử nghiệm OpenCity.",
            color = OpenCityColors.TextSecondary)
        TextButton(onClick = { refreshKey++ }) { Text("Làm mới") }
        if (error.isNotBlank()) ErrorBanner(error)
        if (notice.isNotBlank()) Text(notice, color = OpenCityColors.BrandMintDark)
        if (loading && slots.isEmpty() && appointments.isEmpty()) CircularProgressIndicator()

        SectionHeader("Khung giờ sắp tới")
        if (!loading && slots.isEmpty()) EmptyPlaceholder("Chưa có khung giờ tiếp công dân.")
        slots.forEach { slot ->
            val slotId = slot.text("id")
            val alreadyRequested = appointments.any {
                it.text("slot_id") == slotId && it.text("status") in setOf("pending", "confirmed")
            }
            Surface(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(appointmentTime(slot.text("starts_at")), fontWeight = FontWeight.Bold,
                        color = OpenCityColors.TextMain)
                    Text(slot.text("location"), color = OpenCityColors.TextSecondary)
                    Text("Còn ${slot.optInt("remaining_capacity")}/${slot.optInt("capacity")} chỗ đã xác nhận",
                        style = MaterialTheme.typography.bodySmall, color = OpenCityColors.TextMuted)
                    if (selectedSlot == slotId) {
                        OutlinedTextField(
                            value = reason, onValueChange = { reason = it },
                            label = { Text("Nội dung cần trao đổi") },
                            minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth()
                        )
                        Text("Yêu cầu sẽ được cán bộ xem xét trước khi xác nhận.",
                            style = MaterialTheme.typography.bodySmall, color = OpenCityColors.TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    busy = true; error = ""; notice = ""
                                    runCatching { api.requestAppointment(slotId, reason) }
                                        .onSuccess {
                                            selectedSlot = ""; reason = ""
                                            notice = "Đã gửi yêu cầu đăng ký."
                                            refreshKey++
                                        }.onFailure { error = it.message ?: "Không gửi được yêu cầu." }
                                    busy = false
                                }
                            }, enabled = !busy && reason.trim().length in 10..1000) { Text("Gửi yêu cầu") }
                            OutlinedButton(onClick = { selectedSlot = ""; reason = "" }) { Text("Đóng") }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { selectedSlot = slotId; reason = "" },
                            enabled = slot.optInt("remaining_capacity") > 0 && !alreadyRequested
                        ) { Text(if (alreadyRequested) "Đã đăng ký" else "Chọn khung giờ") }
                    }
                }
            }
        }

        SectionHeader("Lịch hẹn của tôi")
        if (!loading && appointments.isEmpty()) EmptyPlaceholder("Bạn chưa gửi yêu cầu tiếp công dân.")
        appointments.forEach { item ->
            Surface(
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(appointmentTime(item.text("starts_at")), fontWeight = FontWeight.Bold,
                        color = OpenCityColors.TextMain)
                    Text(appointmentStatus(item.text("status")), color = OpenCityColors.BrandAzure,
                        fontWeight = FontWeight.Bold)
                    Text(item.text("location"), color = OpenCityColors.TextSecondary)
                    Text(item.text("reason"), color = OpenCityColors.TextMain)
                    if (item.text("review_note").isNotBlank())
                        Text("Phản hồi: ${item.text("review_note")}", color = OpenCityColors.TextSecondary)
                    if (item.text("status") in setOf("pending", "confirmed")) {
                        OutlinedButton(onClick = {
                            scope.launch {
                                busy = true; error = ""; notice = ""
                                runCatching { api.cancelAppointment(item.text("id")) }
                                    .onSuccess { notice = "Đã hủy yêu cầu."; refreshKey++ }
                                    .onFailure { error = it.message ?: "Không hủy được yêu cầu." }
                                busy = false
                            }
                        }, enabled = !busy) { Text("Hủy yêu cầu") }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
