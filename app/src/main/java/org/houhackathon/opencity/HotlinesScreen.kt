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
fun HotlinesScreen(api: ApiClient, revision: Int) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(revision, refreshKey) {
        loading = true
        runCatching { api.list("/hotlines") }
            .onSuccess { items = it; error = "" }
            .onFailure { error = it.message ?: "Không tải được danh bạ." }
        loading = false
    }

    val visible = items.filter { item ->
        val needle = query.trim().lowercase()
        needle.isBlank() || listOf("name", "unit", "area", "phone")
            .any { key -> item.text(key).lowercase().contains(needle) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Danh bạ đường dây nóng", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold, color = OpenCityColors.TextMain)
        Text("Các số liên hệ do đơn vị vận hành OpenCity công bố. Chạm Gọi để mở ứng dụng điện thoại.",
            color = OpenCityColors.TextSecondary)
        OutlinedTextField(value = query, onValueChange = { query = it },
            label = { Text("Tìm tên, đơn vị, khu vực hoặc số") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        TextButton(onClick = { refreshKey++ }) { Text("Làm mới") }
        if (error.isNotBlank()) ErrorBanner(error)
        if (loading && items.isEmpty()) CircularProgressIndicator()
        if (!loading && visible.isEmpty()) EmptyPlaceholder("Chưa có liên hệ phù hợp.")

        visible.forEach { item ->
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(item.text("name"), fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                    Text(item.text("unit"), color = OpenCityColors.BrandAzure)
                    if (item.text("area").isNotBlank()) Text("Khu vực: ${item.text("area")}", color = OpenCityColors.TextSecondary)
                    if (item.text("hours").isNotBlank()) Text("Giờ trực: ${item.text("hours")}", color = OpenCityColors.TextSecondary)
                    if (item.text("description").isNotBlank()) Text(item.text("description"), color = OpenCityColors.TextSecondary)
                    val phone = item.text("phone")
                    Button(onClick = {
                        val dialNumber = phone.filter { it.isDigit() || it == '+' }
                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialNumber"))) }
                            .onFailure { error = "Không mở được ứng dụng gọi điện trên thiết bị." }
                    }, enabled = phone.isNotBlank()) { Text("Gọi $phone") }
                }
            }
        }
    }
}
