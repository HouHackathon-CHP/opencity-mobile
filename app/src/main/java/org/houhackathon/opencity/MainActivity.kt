package org.houhackathon.opencity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val auth by lazy { AuthManager(this) }
    private val api by lazy { ApiClient(this, auth) }
    private var authMessage by mutableStateOf<String?>(null)
    private val signIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        auth.completeSignIn(result.data) { authMessage = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OpenCityApp(auth, api, { authMessage = null; signIn.launch(auth.signInIntent()) },
            authMessage, { authMessage = null }) }
    }
    override fun onDestroy() { auth.close(); super.onDestroy() }
}

private val blue = Color(0xFF164C9C)
private val mint = Color(0xFF00A98F)
private val surface = Color(0xFFF5F8FC)

@Composable
private fun OpenCityApp(auth: AuthManager, api: ApiClient, login: () -> Unit,
                        authMessage: String?, clearAuthMessage: () -> Unit) {
    var page by remember { mutableStateOf("home") }
    var detailId by remember { mutableStateOf("") }
    var detailKind by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    val signedIn = auth.signedIn
    val rootPages = setOf("home", "reports", "news", "map", "account")
    val tabs = listOf(
        Triple("home", "Trang chủ", Icons.Default.Home),
        Triple("reports", "Phản ánh", Icons.AutoMirrored.Filled.List),
        Triple("news", "Tin tức", Icons.AutoMirrored.Filled.Article),
        Triple("map", "Bản đồ", Icons.Default.Map),
        Triple("account", "Tài khoản", Icons.Default.AccountCircle)
    )
    MaterialTheme(colorScheme = lightColorScheme(primary = blue, secondary = mint, background = surface)) {
        Scaffold(
            topBar = {
                Surface(color = blue) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 15.dp)) {
                        Text("OPENCITY", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(if (page == "home") "Thành phố trong tay bạn" else pageTitle(page),
                            style = MaterialTheme.typography.bodySmall, color = Color(0xFFD5E9FF))
                    }
                }
            },
            bottomBar = {
                if (page in rootPages) NavigationBar {
                    tabs.forEach { (key, label, icon) ->
                        NavigationBarItem(selected = page == key, onClick = { page = key }, icon = {
                            Icon(icon, contentDescription = label)
                        }, label = { Text(label) })
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (authMessage != null) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(authMessage, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                            TextButton(onClick = clearAuthMessage) { Text("Đóng") }
                        }
                    }
                }
                if (!signedIn && page != "account") {
                    WelcomeScreen(login, Modifier.fillMaxSize())
                } else when (page) {
                    "home" -> HomeScreen(api, revision, onNavigate = { page = it }, onDetail = { kind, id ->
                        detailKind = kind; detailId = id; page = "detail"
                    })
                    "reports" -> ListScreen(api, "/reports", "Phản ánh của tôi", revision,
                        onClick = { detailKind = "report"; detailId = it; page = "detail" },
                        action = { Button(onClick = { page = "new-report" }) { Text("+ Gửi phản ánh") } })
                    "news" -> ListScreen(api, "/bulletins", "Tin tức & cảnh báo", revision,
                        onClick = { detailKind = "bulletin"; detailId = it; page = "detail" })
                    "surveys" -> ListScreen(api, "/surveys", "Khảo sát ý kiến", revision,
                        onClick = { detailKind = "survey"; detailId = it; page = "detail" })
                    "map" -> MapScreen(api, revision) { kind, id -> detailKind = kind; detailId = id; page = "detail" }
                    "account" -> AccountScreen(signedIn, login, onLogout = { auth.signOut(); page = "home" },
                        onSurveys = { page = "surveys" }, onCatalog = { page = "catalog" })
                    "catalog" -> CatalogScreen(onBack = { page = "home" }, onNavigate = { page = it })
                    "new-report" -> NewReportScreen(api, LocalContext.current) { id ->
                        detailKind = "report"; detailId = id; revision++; page = "detail"
                    }
                    "detail" -> DetailScreen(api, detailKind, detailId, revision,
                        onBack = { page = when (detailKind) { "report" -> "reports"; "bulletin" -> "news"; else -> "surveys" } },
                        onChanged = { revision++ })
                }
            }
        }
    }
}

private fun pageTitle(page: String) = when (page) {
    "reports" -> "Phản ánh"; "news" -> "Tin tức & cảnh báo"; "map" -> "Bản đồ đô thị"
    "account" -> "Tài khoản"; "surveys" -> "Khảo sát"; "new-report" -> "Gửi phản ánh"
    "catalog" -> "Danh mục dịch vụ"; else -> "Chi tiết"
}
