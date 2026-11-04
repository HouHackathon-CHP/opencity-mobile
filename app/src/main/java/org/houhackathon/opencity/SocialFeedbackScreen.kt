// SPDX-License-Identifier: MIT
package org.houhackathon.opencity

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SocialFeedbackScreen(api: ApiClient, revision: Int) {
    var topics by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedId by remember { mutableStateOf("") }
    var opinion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(revision, reload) {
        loading = true
        runCatching { api.list("/social-feedback/topics") }
            .onSuccess { topics = it; if (selectedId.isBlank()) selectedId = it.firstOrNull()?.text("id") ?: ""; error = "" }
            .onFailure { error = it.message ?: "Không tải được chủ đề góp ý." }
        loading = false
    }
    val selected = topics.firstOrNull { it.text("id") == selectedId }
    LaunchedEffect(selectedId, selected?.optJSONObject("my_opinion")?.optString("body")) {
        opinion = selected?.optJSONObject("my_opinion")?.optString("body") ?: ""
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Góp ý, phản biện xã hội", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold, color = OpenCityColors.TextMain)
        Text("Gửi ý kiến cho chủ đề do đơn vị vận hành địa bàn mở.", color = OpenCityColors.TextSecondary)
        TextButton(onClick = { reload++ }) { Text("Làm mới") }
        if (error.isNotBlank()) ErrorBanner(error)
        if (notice.isNotBlank()) Text(notice, color = OpenCityColors.BrandMintDark)
        if (loading) CircularProgressIndicator()
        if (!loading && topics.isEmpty()) EmptyPlaceholder("Chưa có chủ đề lấy ý kiến.")
        topics.forEach { topic ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)) {
                Column(Modifier.padding(14.dp)) {
                    Text(topic.text("title"), fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                    Text("${topic.optInt("opinion_count")} ý kiến · ${if (topic.text("status") == "open") "Đang mở" else "Đã đóng"}",
                        color = OpenCityColors.TextSecondary)
                    TextButton(onClick = { selectedId = topic.text("id"); notice = "" }) {
                        Text(if (selectedId == topic.text("id")) "Đang xem" else "Xem chủ đề")
                    }
                }
            }
        }
        selected?.let { topic ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(topic.text("title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(topic.text("description"), color = OpenCityColors.TextSecondary)
                    if (topic.text("area").isNotBlank()) Text("Khu vực: ${topic.text("area")}")
                    if (topic.text("status") == "open") {
                        OutlinedTextField(value = opinion, onValueChange = { opinion = it },
                            label = { Text("Ý kiến của bạn") }, modifier = Modifier.fillMaxWidth(),
                            minLines = 3, maxLines = 8)
                        Text("Bạn có thể sửa ý kiến khi chủ đề còn mở.", color = OpenCityColors.TextSecondary)
                        Button(enabled = !busy && opinion.trim().length in 10..3000, onClick = {
                            scope.launch {
                                busy = true; error = ""; notice = ""
                                runCatching { api.saveConsultationOpinion(selectedId, opinion) }
                                    .onSuccess { notice = "Đã lưu ý kiến của bạn."; reload++ }
                                    .onFailure { error = it.message ?: "Không lưu được ý kiến." }
                                busy = false
                            }
                        }) { Text("Lưu ý kiến") }
                    } else {
                        Text("Chủ đề đã kết thúc.", color = OpenCityColors.TextSecondary)
                        val mine = topic.optJSONObject("my_opinion")?.optString("body")
                        if (!mine.isNullOrBlank()) Text("Ý kiến của bạn: $mine")
                    }
                }
            }
        }
    }
}
