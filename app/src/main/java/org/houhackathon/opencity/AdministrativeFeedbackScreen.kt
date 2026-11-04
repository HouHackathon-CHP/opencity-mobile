// SPDX-License-Identifier: MIT
package org.houhackathon.opencity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
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

private val issueNames = linkedMapOf(
    "delay" to "Chậm xử lý", "instructions" to "Hướng dẫn chưa rõ",
    "conduct" to "Thái độ phục vụ", "other" to "Vấn đề khác"
)

private fun feedbackStatus(status: String) = when (status) {
    "submitted" -> "Mới gửi"
    "in_review" -> "Đang xem xét"
    "answered" -> "Đã trả lời"
    "closed" -> "Đã kết thúc"
    else -> status
}

private fun feedbackAction(action: String) = when (action) {
    "submitted" -> "Đã gửi phản ánh"
    "start_review" -> "Cán bộ đã tiếp nhận"
    "answer" -> "Cán bộ đã trả lời"
    "clarification" -> "Đã yêu cầu làm rõ"
    "close" -> "Đã kết thúc hồ sơ"
    else -> action
}

@Composable
fun AdministrativeFeedbackScreen(api: ApiClient, revision: Int) {
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedId by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var procedure by remember { mutableStateOf("") }
    var agency by remember { mutableStateOf("") }
    var issueType by remember { mutableStateOf("delay") }
    var description by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val selected = items.firstOrNull { it.text("id") == selectedId }

    LaunchedEffect(revision, refreshKey) {
        loading = true
        runCatching { api.list("/administrative-feedback") }
            .onSuccess { list ->
                items = list
                if (selectedId.isBlank() || list.none { it.text("id") == selectedId })
                    selectedId = list.firstOrNull()?.text("id").orEmpty()
                error = ""
            }.onFailure { error = it.message ?: "Không tải được hồ sơ." }
        loading = false
    }

    fun sendAction(action: String) {
        val caseId = selectedId
        if (caseId.isBlank() || busy) return
        scope.launch {
            busy = true; error = ""; notice = ""
            runCatching { api.administrativeFeedbackAction(caseId, action, note) }
                .onSuccess { note = ""; notice = "Đã cập nhật trạng thái hồ sơ."; refreshKey++ }
                .onFailure { error = it.message ?: "Không cập nhật được hồ sơ." }
            busy = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Phản ánh thủ tục hành chính", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold, color = OpenCityColors.TextMain)
        Text("Gửi ý kiến về quy trình hoặc chất lượng phục vụ và theo dõi phản hồi ngay trong OpenCity.",
            color = OpenCityColors.TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showForm = !showForm }) { Text(if (showForm) "Đóng biểu mẫu" else "Gửi phản ánh") }
            TextButton(onClick = { refreshKey++ }) { Text("Làm mới") }
        }
        if (error.isNotBlank()) ErrorBanner(error)
        if (notice.isNotBlank()) Text(notice, color = OpenCityColors.BrandMintDark)
        if (loading && items.isEmpty()) CircularProgressIndicator()

        if (showForm) Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader("Nội dung phản ánh")
                OutlinedTextField(title, { title = it }, label = { Text("Tiêu đề") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(procedure, { procedure = it }, label = { Text("Thủ tục liên quan") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(agency, { agency = it }, label = { Text("Cơ quan, bộ phận tiếp nhận") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Nhóm vấn đề", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    issueNames.forEach { (key, label) ->
                        FilterChip(selected = issueType == key, onClick = { issueType = key }, label = { Text(label) })
                    }
                }
                OutlinedTextField(description, { description = it }, label = { Text("Mô tả chi tiết") },
                    modifier = Modifier.fillMaxWidth(), minLines = 4, maxLines = 8)
                Button(onClick = {
                    scope.launch {
                        busy = true; error = ""; notice = ""
                        runCatching { api.createAdministrativeFeedback(title, procedure, agency, issueType, description) }
                            .onSuccess {
                                selectedId = it.text("id")
                                title = ""; procedure = ""; agency = ""; description = ""
                                showForm = false; notice = "Đã gửi phản ánh."; refreshKey++
                            }.onFailure { error = it.message ?: "Không gửi được phản ánh." }
                        busy = false
                    }
                }, enabled = !busy && title.trim().length in 5..120 && procedure.trim().length in 3..120 &&
                    agency.trim().length in 3..120 && description.trim().length in 20..2000) {
                    Text("Gửi phản ánh")
                }
            }
        }

        SectionHeader("Phản ánh của tôi")
        if (!loading && items.isEmpty()) EmptyPlaceholder("Bạn chưa gửi phản ánh thủ tục hành chính.")
        items.forEach { item ->
            OutlinedButton(onClick = { selectedId = item.text("id"); note = "" },
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text(item.text("title"), fontWeight = FontWeight.Bold)
                    Text(feedbackStatus(item.text("status")), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (selected != null) Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(selected.text("title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Trạng thái: ${feedbackStatus(selected.text("status"))}", color = OpenCityColors.BrandAzure)
                Text("Thủ tục: ${selected.text("procedure")}")
                Text("Bộ phận: ${selected.text("agency")}")
                Text("Vấn đề: ${issueNames[selected.text("issue_type")] ?: "Khác"}")
                Text(selected.text("description"))
                if (selected.text("response").isNotBlank()) {
                    SectionHeader("Phản hồi gần nhất")
                    Text(selected.text("response"))
                }
                if (selected.text("status") == "answered") {
                    OutlinedTextField(note, { note = it }, label = { Text("Nội dung cần làm rõ thêm") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { sendAction("clarification") },
                            enabled = !busy && note.trim().length in 10..2000) { Text("Yêu cầu làm rõ") }
                        Button(onClick = { sendAction("close") }, enabled = !busy) { Text("Kết thúc") }
                    }
                }
                SectionHeader("Lịch sử xử lý")
                val history = selected.optJSONArray("history")
                if (history != null) for (index in 0 until history.length()) {
                    val entry = history.getJSONObject(index)
                    Text("${feedbackAction(entry.text("action"))} · ${entry.text("created_at").replace("T", " ").take(16)}",
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    if (entry.text("note").isNotBlank()) Text(entry.text("note"))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
