// SPDX-License-Identifier: MIT
package org.houhackathon.opencity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
fun NotificationsScreen(api: ApiClient, revision: Int, onReport: (String) -> Unit) {
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(revision, refreshKey) {
        loading = true
        error = ""
        runCatching { api.list("/notifications") }
            .onSuccess { items = it }
            .onFailure { error = it.message ?: "Không thể tải thông báo." }
        loading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Thông báo của bạn", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold, color = OpenCityColors.TextMain)
            TextButton(onClick = { refreshKey++ }) { Text("Làm mới") }
        }
        Text("${items.count { it.text("read_at").isBlank() }} chưa đọc",
            color = OpenCityColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        if (error.isNotBlank()) Text(error, color = OpenCityColors.BrandRedDark)
        if (loading && items.isEmpty()) CircularProgressIndicator()
        if (!loading && items.isEmpty() && error.isBlank()) {
            Surface(shape = RoundedCornerShape(16.dp), color = OpenCityColors.CardBg,
                border = BorderStroke(1.dp, OpenCityColors.BorderColor)) {
                Text("Chưa có thông báo. Tiến độ xử lý phản ánh sẽ xuất hiện tại đây.",
                    modifier = Modifier.padding(18.dp), color = OpenCityColors.TextSecondary)
            }
        }
        items.forEach { item ->
            val unread = item.text("read_at").isBlank()
            Surface(
                modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch {
                        error = ""
                        runCatching {
                            if (unread) api.readNotification(item.text("id")) else null
                        }.onSuccess { result ->
                            if (result != null) items = items.map { existing ->
                                if (existing.text("id") == item.text("id"))
                                    JSONObject(existing.toString()).put("read_at", result.text("read_at"))
                                else existing
                            }
                            onReport(item.text("report_id"))
                        }.onFailure { error = it.message ?: "Không thể mở thông báo." }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = if (unread) OpenCityColors.BrandAzureLight else OpenCityColors.CardBg,
                border = BorderStroke(1.dp, if (unread) OpenCityColors.BrandAzureBorder else OpenCityColors.BorderColor)
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (unread) Icons.Default.Notifications else Icons.Default.CheckCircle,
                        contentDescription = null, tint = OpenCityColors.BrandAzure,
                        modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(item.text("title"), fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMain)
                        Text(item.text("body"), style = MaterialTheme.typography.bodyMedium,
                            color = OpenCityColors.TextSecondary)
                        Text(item.text("created_at").take(16).replace("T", " "),
                            style = MaterialTheme.typography.bodySmall, color = OpenCityColors.TextLight)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
