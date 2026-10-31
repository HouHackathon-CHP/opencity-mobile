package org.houhackathon.opencity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val auth by lazy { AuthManager(this) }
    private val api by lazy { ApiClient(this, auth) }
    private var authMessage by mutableStateOf<String?>(null)
    private val signIn = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        auth.completeSignIn(result.data) { authMessage = it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenCityApp(
                auth = auth,
                api = api,
                login = {
                    authMessage = null
                    signIn.launch(auth.signInIntent())
                },
                authMessage = authMessage,
                clearAuthMessage = { authMessage = null }
            )
        }
    }

    override fun onDestroy() {
        auth.close()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenCityApp(
    auth: AuthManager,
    api: ApiClient,
    login: () -> Unit,
    authMessage: String?,
    clearAuthMessage: () -> Unit
) {
    var page by remember { mutableStateOf("home") }
    var detailId by remember { mutableStateOf("") }
    var detailKind by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var notificationReturnPage by remember { mutableStateOf("home") }
    var unreadNotifications by remember { mutableIntStateOf(0) }
    val signedIn = auth.signedIn
    val rootPages = setOf("home", "reports", "news", "map", "account")

    val tabs = listOf(
        Triple("home", "Khám phá", Icons.Default.Home),
        Triple("reports", "Phản ánh", Icons.AutoMirrored.Filled.List),
        Triple("news", "Tin tức", Icons.AutoMirrored.Filled.Article),
        Triple("map", "Bản đồ", Icons.Default.Map),
        Triple("account", "Tài khoản", Icons.Default.AccountCircle)
    )

    LaunchedEffect(signedIn, page, revision) {
        unreadNotifications = if (signedIn) runCatching {
            api.list("/notifications").count { it.text("read_at").isBlank() }
        }.getOrDefault(0) else 0
    }

    OpenCityTheme {
        Scaffold(
            topBar = {
                Surface(
                    color = OpenCityColors.CardBg,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (page in rootPages) {
                            OpenCityLogo(size = 36.dp, showSubtitle = true)

                            // Notification inbox and profile shortcuts
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (signedIn) {
                                    IconButton(onClick = {
                                        notificationReturnPage = page
                                        page = "notifications"
                                    }) {
                                        BadgedBox(badge = {
                                            if (unreadNotifications > 0) Badge {
                                                Text(if (unreadNotifications > 99) "99+" else "$unreadNotifications")
                                            }
                                        }) {
                                            Icon(Icons.Default.Notifications, contentDescription = "Thông báo",
                                                tint = OpenCityColors.BrandAzure)
                                        }
                                    }
                                }
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(OpenCityColors.BrandAzureLight)
                                    .clickable { page = "account" },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Tài khoản",
                                    tint = OpenCityColors.BrandAzure,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        page = when (page) {
                                            "detail" -> when (detailKind) {
                                                "report" -> "reports"
                                                "bulletin" -> "news"
                                                else -> "surveys"
                                            }
                                            "new-report" -> "reports"
                                            "catalog" -> "home"
                                            "surveys" -> "home"
                                            "notifications" -> notificationReturnPage
                                            else -> "home"
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Quay lại",
                                        tint = OpenCityColors.TextMain
                                    )
                                }
                                Text(
                                    text = pageTitle(page, detailKind),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OpenCityColors.TextMain
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (page in rootPages) {
                    NavigationBar(
                        containerColor = OpenCityColors.CardBg,
                        tonalElevation = 8.dp,
                        modifier = Modifier.shadow(8.dp)
                    ) {
                        tabs.forEach { (key, label, icon) ->
                            val isSelected = page == key
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { page = key },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.TextMuted
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.TextMuted
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = OpenCityColors.BrandAzureLight
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OpenCityColors.SurfaceBg)
                    .padding(padding)
            ) {
                // Auth Feedback Notification Banner
                authMessage?.let { msg ->
                    Surface(
                        color = OpenCityColors.BrandAmberLight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                modifier = Modifier.weight(1f),
                                color = OpenCityColors.BrandAmberDark,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(onClick = clearAuthMessage) {
                                Text("Đóng", color = OpenCityColors.BrandAmberDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Main Navigation Routing
                if (!signedIn && page != "account") {
                    WelcomeScreen(login = login, modifier = Modifier.fillMaxSize())
                } else {
                    when (page) {
                        "home" -> HomeScreen(
                            api = api,
                            revision = revision,
                            onNavigate = { page = it },
                            onDetail = { kind, id ->
                                detailKind = kind
                                detailId = id
                                page = "detail"
                            }
                        )
                        "reports" -> ListScreen(
                            api = api,
                            path = "/reports",
                            title = "Phản ánh hiện trường",
                            revision = revision,
                            onClick = {
                                detailKind = "report"
                                detailId = it
                                page = "detail"
                            },
                            action = {
                                Button(
                                    onClick = { page = "new-report" },
                                    colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("+ Gửi phản ánh hiện trường mới", fontWeight = FontWeight.Bold)
                                }
                            }
                        )
                        "news" -> ListScreen(
                            api = api,
                            path = "/bulletins",
                            title = "Tin tức & cảnh báo",
                            revision = revision,
                            onClick = {
                                detailKind = "bulletin"
                                detailId = it
                                page = "detail"
                            }
                        )
                        "surveys" -> ListScreen(
                            api = api,
                            path = "/surveys",
                            title = "Khảo sát ý kiến cư dân",
                            revision = revision,
                            onClick = {
                                detailKind = "survey"
                                detailId = it
                                page = "detail"
                            }
                        )
                        "map" -> MapScreen(
                            api = api,
                            revision = revision,
                            onDetail = { kind, id ->
                                detailKind = kind
                                detailId = id
                                page = "detail"
                            }
                        )
                        "account" -> AccountScreen(
                            signedIn = signedIn,
                            login = login,
                            onLogout = {
                                auth.signOut()
                                page = "home"
                            },
                            onSurveys = { page = "surveys" },
                            onCatalog = { page = "catalog" }
                        )
                        "catalog" -> CatalogScreen(
                            onBack = { page = "home" },
                            onNavigate = { page = it }
                        )
                        "notifications" -> NotificationsScreen(
                            api = api,
                            revision = revision,
                            onReport = { id ->
                                detailKind = "report"
                                detailId = id
                                revision++
                                page = "detail"
                            }
                        )
                        "new-report" -> NewReportScreen(
                            api = api,
                            context = LocalContext.current,
                            onCreated = { id ->
                                detailKind = "report"
                                detailId = id
                                revision++
                                page = "detail"
                            }
                        )
                        "detail" -> DetailScreen(
                            api = api,
                            kind = detailKind,
                            id = detailId,
                            revision = revision,
                            onBack = {
                                page = when (detailKind) {
                                    "report" -> "reports"
                                    "bulletin" -> "news"
                                    else -> "surveys"
                                }
                            },
                            onChanged = { revision++ }
                        )
                    }
                }
            }
        }
    }
}

private fun pageTitle(page: String, detailKind: String = ""): String = when (page) {
    "reports" -> "Hồ sơ phản ánh"
    "news" -> "Tin tức & Cảnh báo"
    "map" -> "Bản đồ đô thị số"
    "account" -> "Tài khoản công dân"
    "surveys" -> "Khảo sát ý kiến"
    "new-report" -> "Tạo phản ánh mới"
    "catalog" -> "Danh mục dịch vụ"
    "notifications" -> "Thông báo"
    "detail" -> when (detailKind) {
        "report" -> "Chi tiết phản ánh"
        "bulletin" -> "Chi tiết bản tin"
        "survey" -> "Biểu quyết khảo sát"
        else -> "Chi tiết"
    }
    else -> "OpenCity"
}
