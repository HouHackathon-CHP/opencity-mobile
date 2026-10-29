package org.houhackathon.opencity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONObject

internal fun JSONObject.text(key: String): String = optString(key).takeUnless { it == "null" }.orEmpty()
internal fun JSONObject.number(key: String): Double? = if (isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }

@Composable
fun WelcomeScreen(login: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Kết nối với thành phố", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Theo dõi tin cảnh báo, gửi phản ánh hiện trường và tham gia khảo sát cộng đồng.")
        Spacer(Modifier.height(24.dp))
        Button(onClick = login, modifier = Modifier.fillMaxWidth()) { Text("Đăng nhập") }
        Text("Sử dụng tài khoản DX-OS để tiếp tục.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun HomeScreen(api: ApiClient, revision: Int, onNavigate: (String) -> Unit,
               onDetail: (String, String) -> Unit) {
    var bulletins by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var reports by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(revision) {
        try {
            bulletins = api.list("/bulletins").take(3)
            reports = api.list("/reports").take(3)
            error = null
        } catch (e: Exception) { error = e.message }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Chào mừng đến OpenCity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Một nơi để theo dõi và đóng góp cho đô thị của bạn.")
        Spacer(Modifier.height(20.dp))
        Button(onClick = { onNavigate("new-report") }, Modifier.fillMaxWidth()) { Text("+ Gửi phản ánh hiện trường") }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { onNavigate("surveys") }) { Text("Khảo sát") }
            OutlinedButton(onClick = { onNavigate("catalog") }) { Text("Danh mục") }
        }
        error?.let { ErrorText(it) }
        SectionTitle("Tin mới & cảnh báo")
        if (bulletins.isEmpty()) Text("Chưa có bản tin.")
        bulletins.forEach { ItemCard(it, "bulletin") { onDetail("bulletin", it.text("id")) } }
        TextButton(onClick = { onNavigate("news") }) { Text("Xem tất cả tin tức") }
        SectionTitle("Phản ánh gần đây")
        if (reports.isEmpty()) Text("Bạn chưa có phản ánh.")
        reports.forEach { ItemCard(it, "report") { onDetail("report", it.text("id")) } }
        TextButton(onClick = { onNavigate("reports") }) { Text("Xem tất cả phản ánh") }
    }
}

@Composable
fun ListScreen(api: ApiClient, path: String, title: String, revision: Int,
               onClick: (String) -> Unit, action: (@Composable () -> Unit)? = null) {
    var items by remember(path) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember(path) { mutableStateOf(true) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var reload by remember(path) { mutableIntStateOf(0) }
    LaunchedEffect(path, revision, reload) {
        loading = true
        try { items = api.list(path); error = null } catch (e: Exception) { error = e.message }
        loading = false
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        action?.invoke()
        TextButton(onClick = { reload++ }) { Text("Làm mới") }
        if (loading) CircularProgressIndicator()
        error?.let { ErrorText(it) }
        if (!loading && error == null && items.isEmpty()) Text("Chưa có dữ liệu.")
        val kind = when (path) { "/reports" -> "report"; "/bulletins" -> "bulletin"; else -> "survey" }
        items.forEach { item -> ItemCard(item, kind) { onClick(item.text("id")) } }
    }
}

@Composable
private fun ItemCard(item: JSONObject, kind: String, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp)) {
            Text(item.text("title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val subtitle = when (kind) { "report" -> item.text("address"); "bulletin" -> item.text("summary"); else -> item.text("description") }
            Spacer(Modifier.height(5.dp))
            Text(subtitle, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(6.dp))
            Text(when (kind) { "report" -> item.text("state"); "bulletin" -> if (item.text("category") == "alert") "Cảnh báo" else "Tin tức"; else -> if (item.text("status") == "open") "Đang lấy ý kiến" else "Đã đóng" },
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun DetailScreen(api: ApiClient, kind: String, id: String, revision: Int,
                 onBack: () -> Unit, onChanged: () -> Unit) {
    val path = when (kind) { "report" -> "/reports/$id"; "bulletin" -> "/bulletins/$id"; else -> "/surveys/$id" }
    var item by remember(path) { mutableStateOf<JSONObject?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var busy by remember(path) { mutableStateOf(false) }
    var localRevision by remember(path) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(path, revision, localRevision) {
        try { item = api.item(path); error = null } catch (e: Exception) { error = e.message }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Quay lại") }
        error?.let { ErrorText(it) }
        val data = item
        if (data == null) { CircularProgressIndicator(); return@Column }
        Text(data.text("title"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        when (kind) {
            "report" -> {
                InfoLine("Trạng thái", data.text("state"))
                InfoLine("Lĩnh vực", data.text("category"))
                InfoLine("Địa chỉ", data.text("address"))
                Text(data.text("description"))
                val history = data.optJSONArray("history")
                if (history != null && history.length() > 0) {
                    SectionTitle("Tiến trình xử lý")
                    for (i in 0 until history.length()) {
                        val event = history.optJSONObject(i) ?: continue
                        Text("• ${event.text("action")}  ${event.text("at").ifBlank { event.text("timestamp") }}")
                    }
                }
            }
            "bulletin" -> {
                InfoLine("Loại", if (data.text("category") == "alert") "Cảnh báo" else "Tin tức")
                InfoLine("Khu vực", data.text("area"))
                Text(data.text("body"))
            }
            else -> {
                Text(data.text("description"))
                InfoLine("Trạng thái", if (data.text("status") == "open") "Đang lấy ý kiến" else "Đã đóng")
                val mine = data.text("my_option_id")
                val options = data.optJSONArray("options")
                if (options != null) {
                    SectionTitle("Lựa chọn")
                    for (i in 0 until options.length()) {
                        val option = options.optJSONObject(i) ?: continue
                        val optionId = option.text("id")
                        val selected = mine == optionId
                        ElevatedCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(option.text("label") + if (selected) " ✓" else "", Modifier.weight(1f))
                                if (!option.isNull("votes")) Text("${option.optInt("votes")} phiếu")
                                if (mine.isBlank() && data.text("status") == "open") {
                                    TextButton(enabled = !busy, onClick = {
                                        busy = true
                                        scope.launch {
                                            try { api.answer(id, optionId); localRevision++; onChanged() }
                                            catch (e: Exception) { error = e.message }
                                            finally { busy = false }
                                        }
                                    }) { Text("Chọn") }
                                }
                            }
                        }
                    }
                }
                if (mine.isNotBlank()) Text("Bạn đã tham gia khảo sát này.")
            }
        }
    }
}

@Composable
fun AccountScreen(signedIn: Boolean, login: () -> Unit, onLogout: () -> Unit,
                  onSurveys: () -> Unit, onCatalog: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Tài khoản", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(if (signedIn) "Đã đăng nhập qua DX-OS" else "Chưa đăng nhập")
        Spacer(Modifier.height(16.dp))
        if (signedIn) OutlinedButton(onClick = onLogout) { Text("Đăng xuất") }
        else Button(onClick = login) { Text("Đăng nhập") }
        HorizontalDivider()
        TextButton(onClick = onSurveys) { Text("Khảo sát ý kiến →") }
        TextButton(onClick = onCatalog) { Text("Danh mục dịch vụ →") }
    }
}

@Composable
fun CatalogScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val catalog = remember {
        runCatching { JSONObject(context.assets.open("catalog.json").bufferedReader().use { it.readText() }) }
            .getOrDefault(JSONObject())
    }
    val categories = catalog.optJSONArray("categories")
    val services = catalog.optJSONArray("services")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        TextButton(onClick = onBack) { Text("← Quay lại") }
        Text("Danh mục dịch vụ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Các mục chưa có API chỉ hiển thị trạng thái, không mở trang giả.")
        if (categories != null && services != null) for (i in 0 until categories.length()) {
            val category = categories.optJSONObject(i) ?: continue
            SectionTitle(category.text("name"))
            for (j in 0 until services.length()) {
                val service = services.optJSONObject(j) ?: continue
                if (service.text("category") == category.text("id")) {
                    val route = service.text("route").removePrefix("/")
                    if (service.text("status") == "available" && route in setOf("reports", "news", "map", "surveys")) {
                        TextButton(onClick = { onNavigate(route) }) { Text("${service.text("name")} →") }
                    } else InfoLine(service.text("name"), if (service.text("status") == "available") "Đang hoạt động" else "Chưa tích hợp")
                }
            }
        }
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Spacer(Modifier.height(18.dp))
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(10.dp))
}
@Composable
internal fun ErrorText(message: String) {
    Text(message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
}
@Composable
internal fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        Text(value, Modifier.weight(1f))
    }
    HorizontalDivider()
}
