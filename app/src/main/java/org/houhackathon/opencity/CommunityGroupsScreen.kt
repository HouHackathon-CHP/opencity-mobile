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
fun CommunityGroupsScreen(api: ApiClient, revision: Int) {
    var groups by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedId by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var postBody by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(revision, reload) {
        loading = true
        runCatching { api.list("/community/groups") }
            .onSuccess { groups = it; if (selectedId.isBlank()) selectedId = it.firstOrNull()?.text("id") ?: ""; error = "" }
            .onFailure { error = it.message ?: "Không tải được nhóm cộng đồng." }
        loading = false
    }
    val selected = groups.firstOrNull { it.text("id") == selectedId }
    val joined = selected?.optBoolean("joined") == true
    LaunchedEffect(selectedId, joined, reload) {
        posts = emptyList()
        if (selectedId.isNotBlank() && joined) {
            runCatching { api.list("/community/groups/$selectedId/posts") }
                .onSuccess { posts = it; error = "" }
                .onFailure { error = it.message ?: "Không tải được bài viết." }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Nhóm cộng đồng", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold, color = OpenCityColors.TextMain)
        Text("Chọn nhóm trong địa bàn, tham gia và trao đổi cùng cư dân.", color = OpenCityColors.TextSecondary)
        TextButton(onClick = { reload++ }) { Text("Làm mới") }
        if (error.isNotBlank()) ErrorBanner(error)
        if (loading) CircularProgressIndicator()
        if (!loading && groups.isEmpty()) EmptyPlaceholder("Chưa có nhóm trong đơn vị này.")
        groups.forEach { group ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)) {
                Column(Modifier.padding(14.dp)) {
                    Text(group.text("title"), fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                    Text("${group.optInt("member_count")} thành viên", color = OpenCityColors.TextSecondary)
                    TextButton(onClick = { selectedId = group.text("id") }) {
                        Text(if (selectedId == group.text("id")) "Đang xem" else "Xem nhóm")
                    }
                }
            }
        }
        selected?.let { group ->
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = OpenCityColors.CardBg, border = BorderStroke(1.dp, OpenCityColors.BorderColor)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(group.text("title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(group.text("description"), color = OpenCityColors.TextSecondary)
                    if (group.text("area").isNotBlank()) Text("Khu vực: ${group.text("area")}")
                    Button(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true
                            runCatching {
                                if (joined) api.leaveCommunityGroup(selectedId) else api.joinCommunityGroup(selectedId)
                            }.onSuccess { reload++ }
                                .onFailure { error = it.message ?: "Không cập nhật được thành viên." }
                            busy = false
                        }
                    }) { Text(if (joined) "Rời nhóm" else "Tham gia nhóm") }
                    if (joined) {
                        Text("Bài viết trong nhóm", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = postBody, onValueChange = { postBody = it },
                            label = { Text("Chia sẻ với nhóm") }, modifier = Modifier.fillMaxWidth(),
                            minLines = 2, maxLines = 5)
                        Button(enabled = !busy && postBody.trim().length in 3..2000, onClick = {
                            scope.launch {
                                busy = true
                                runCatching { api.postToCommunityGroup(selectedId, postBody) }
                                    .onSuccess { postBody = ""; reload++ }
                                    .onFailure { error = it.message ?: "Không đăng được bài viết." }
                                busy = false
                            }
                        }) { Text("Đăng bài") }
                        if (posts.isEmpty()) Text("Chưa có bài viết.")
                        posts.forEach { post ->
                            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                                color = OpenCityColors.SurfaceBg) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(post.text("user_id"), fontWeight = FontWeight.Bold)
                                    Text(post.text("body"))
                                }
                            }
                        }
                    } else Text("Tham gia nhóm để xem và đăng bài viết.")
                }
            }
        }
    }
}
