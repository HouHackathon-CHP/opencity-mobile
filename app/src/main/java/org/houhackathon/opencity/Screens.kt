package org.houhackathon.opencity

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject

internal fun JSONObject.text(key: String): String = optString(key).takeUnless { it == "null" }.orEmpty()
internal fun JSONObject.number(key: String): Double? = if (isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }

// 1. WELCOME SCREEN
@Composable
fun WelcomeScreen(
    login: () -> Unit,
    register: () -> Unit = {},
    onExplore: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Identity Brand Mark
        Box(
            modifier = Modifier
                .size(86.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, OpenCityColors.BrandAzureBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            NexusPortalMark(size = 52.dp)
        }

        Spacer(Modifier.height(18.dp))

        // Ecosystem Pill
        Surface(
            color = OpenCityColors.BrandAzureLight,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(OpenCityColors.BrandAzure)
                )
                Text(
                    text = "DX-OS ECOSYSTEM • CỔNG ĐÔ THỊ THÔNG MINH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.BrandAzureDark,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Kết nối Trực tiếp\nChính quyền & Cư dân",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OpenCityColors.TextMain,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Nền tảng tiếp nhận phản ánh hiện trường, dịch vụ công trực tuyến, tra cứu giao thông xe buýt và giám sát chất lượng môi trường thời gian thực.",
            style = MaterialTheme.typography.bodyMedium,
            color = OpenCityColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(24.dp))

        // Feature Highlight Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureItem(
                icon = Icons.Default.CameraAlt,
                iconColor = OpenCityColors.BrandAzure,
                iconBg = OpenCityColors.BrandAzureLight,
                title = "Phản ánh hiện trường tức thì",
                desc = "Gửi hình ảnh và tọa độ GPS sự cố hạ tầng đô thị đến đúng đơn vị phụ trách."
            )
            FeatureItem(
                icon = Icons.Default.AccountBalance,
                iconColor = OpenCityColors.BrandNavy,
                iconBg = Color(0xFFE2E8F0),
                title = "Tra cứu dịch vụ công số",
                desc = "Khám phá danh mục thủ tục hành chính, quy trình và hướng dẫn không cần đăng nhập."
            )
            FeatureItem(
                icon = Icons.Default.Campaign,
                iconColor = OpenCityColors.BrandRed,
                iconBg = OpenCityColors.BrandRedLight,
                title = "Bản tin & Cảnh báo an toàn",
                desc = "Nhận thông báo khẩn cấp về giao thông, thời tiết và trật tự đô thị 24/7."
            )
        }

        Spacer(Modifier.height(28.dp))

        // Sign In Action Button
        Button(
            onClick = login,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(6.dp, RoundedCornerShape(16.dp)),
            colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Đăng nhập với DX-OS Citizen",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Citizen đăng ký
        OutlinedButton(
            onClick = register,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, OpenCityColors.BrandAzure),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = OpenCityColors.BrandAzure,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Đăng ký tài khoản công dân",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.BrandAzure
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onExplore,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Explore,
                    contentDescription = null,
                    tint = OpenCityColors.BrandNavy,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Khám phá ngay (Chế độ Khách)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.BrandNavy
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Trust badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = OpenCityColors.BrandMintDark,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Bảo mật chuẩn OIDC • Xác thực định danh một chạm",
                fontSize = 12.sp,
                color = OpenCityColors.TextMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    title: String,
    desc: String
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                Spacer(Modifier.height(2.dp))
                Text(text = desc, fontSize = 12.sp, color = OpenCityColors.TextSecondary, lineHeight = 16.sp)
            }
        }
    }
}


// 2. HOME SCREEN
@Composable
fun HomeScreen(
    api: ApiClient,
    revision: Int,
    signedIn: Boolean,
    onNavigate: (String) -> Unit,
    onDetail: (String, String) -> Unit
) {
    var bulletins by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var reports by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var airSummary by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(revision, signedIn) {
        loading = true
        try {
            bulletins = runCatching { api.list("/bulletins").take(3) }.getOrDefault(emptyList())
            reports = if (signedIn) runCatching { api.list("/reports").take(3) }.getOrDefault(emptyList()) else emptyList()
            airSummary = runCatching { api.item("/air-quality/summary") }.getOrElse {
                JSONObject("""{"average_aqi":82,"classification":{"label":"Trung bình","level":"moderate"}}""")
            }
            error = null
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
        error = null
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Citizen Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(OpenCityColors.BrandNavyDark, Color(0xFF1E293B))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0x3310B981),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(OpenCityColors.BrandMint)
                                )
                                Text(
                                    text = "Đô thị số trực tuyến",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }

                        Text(
                            text = "OpenCity Demo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Cùng chung tay xây dựng\nđô thị văn minh & hiện đại",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 24.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigate("new-report") },
                            modifier = Modifier.weight(1.3f),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text(text = "+ Gửi phản ánh", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { onNavigate("dashboard") },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Text(text = "Thống kê", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Quick Shortcut Tiles
        Text(
            text = "Dịch vụ trực tuyến",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = OpenCityColors.TextMain
        )

        Spacer(Modifier.height(10.dp))

        // Grid hàng 1: Phản ánh, Bản đồ số, Khảo sát
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.List,
                iconColor = OpenCityColors.BrandAzure,
                iconBg = OpenCityColors.BrandAzureLight,
                label = "Phản ánh",
                onClick = { onNavigate("reports") }
            )
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Map,
                iconColor = OpenCityColors.BrandCyan,
                iconBg = OpenCityColors.BrandCyanLight,
                label = "Bản đồ số",
                onClick = { onNavigate("map") }
            )
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Poll,
                iconColor = OpenCityColors.BrandMintDark,
                iconBg = OpenCityColors.BrandMintLight,
                label = "Khảo sát",
                onClick = { onNavigate("surveys") }
            )
        }

        Spacer(Modifier.height(10.dp))

        // Grid hàng 2: Không khí (AQI), Xe buýt, Danh mục
        val avgAqi = airSummary?.let { air ->
            if (air.has("average_aqi") && !air.isNull("average_aqi")) air.optInt("average_aqi") else null
        }
        val cls = airSummary?.optJSONObject("classification")
        val level = cls?.optString("level")?.takeUnless { it == "null" } ?: "moderate"
        val aqiColor = when (level.lowercase()) {
            "good" -> Color(0xFF10B981)
            "moderate" -> Color(0xFFF59E0B)
            "unhealthy_sensitive" -> Color(0xFFF97316)
            "unhealthy" -> Color(0xFFEF4444)
            "very_unhealthy" -> Color(0xFF8B5CF6)
            "hazardous" -> Color(0xFF7F1D1D)
            else -> Color(0xFFF59E0B)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Air,
                iconColor = aqiColor,
                iconBg = aqiColor.copy(alpha = 0.12f),
                label = "Không khí",
                badgeText = if (avgAqi != null) "$avgAqi" else "83",
                badgeColor = aqiColor,
                onClick = { onNavigate("air-quality") }
            )
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.DirectionsBus,
                iconColor = OpenCityColors.BrandAzure,
                iconBg = OpenCityColors.BrandAzureLight,
                label = "Xe buýt",
                badgeText = "184",
                badgeColor = OpenCityColors.BrandAzure,
                onClick = { onNavigate("find-bus") }
            )
            ShortcutTile(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Category,
                iconColor = OpenCityColors.BrandAmberDark,
                iconBg = OpenCityColors.BrandAmberLight,
                label = "Danh mục",
                onClick = { onNavigate("catalog") }
            )
        }

        error?.let {
            Spacer(Modifier.height(14.dp))
            ErrorBanner(it)
        }

        Spacer(Modifier.height(24.dp))

        // Section: Tin mới & Cảnh báo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cảnh báo & Bản tin đô thị",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OpenCityColors.TextMain
            )
            TextButton(onClick = { onNavigate("news") }) {
                Text(
                    text = "Xem tất cả →",
                    color = OpenCityColors.BrandAzure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        if (loading && bulletins.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OpenCityColors.BrandAzure)
            }
        } else if (bulletins.isEmpty()) {
            EmptyPlaceholder("Hiện chưa có bản tin hoặc cảnh báo mới.")
        } else {
            bulletins.forEach { bulletin ->
                ItemCard(
                    item = bulletin,
                    kind = "bulletin",
                    onClick = { onDetail("bulletin", bulletin.text("id")) }
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Section: Phản ánh gần đây
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Phản ánh hiện trường gần đây",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OpenCityColors.TextMain
            )
            TextButton(onClick = { onNavigate("reports") }) {
                Text(
                    text = "Xem tất cả →",
                    color = OpenCityColors.BrandAzure,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        if (loading && reports.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OpenCityColors.BrandAzure)
            }
        } else if (reports.isEmpty()) {
            EmptyPlaceholder("Bạn chưa gửi phản ánh hiện trường nào.")
        } else {
            reports.forEach { report ->
                ItemCard(
                    item = report,
                    kind = "report",
                    onClick = { onDetail("report", report.text("id")) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ShortcutTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    label: String,
    badgeText: String? = null,
    badgeColor: Color = OpenCityColors.BrandAzure,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(20.dp))
                }
                if (badgeText != null) {
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.offset(x = 6.dp, y = (-4).dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 3. LIST SCREEN
@Composable
fun ListScreen(
    api: ApiClient,
    path: String,
    title: String,
    revision: Int,
    onClick: (String) -> Unit,
    onLogin: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    var rawItems by remember(path) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember(path) { mutableStateOf(true) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var reload by remember(path) { mutableIntStateOf(0) }
    var selectedFilter by remember(path) { mutableStateOf("all") }

    val kind = when (path) {
        "/reports" -> "report"
        "/bulletins" -> "bulletin"
        "/communications" -> "communication"
        else -> "survey"
    }

    LaunchedEffect(path, revision, reload) {
        if (!api.auth.signedIn && !api.isPublicRead(path)) {
            loading = false
            error = "Yêu cầu đăng nhập tài khoản công dân DX-OS để sử dụng tính năng này."
            return@LaunchedEffect
        }
        loading = true
        try {
            rawItems = api.list(path)
            error = null
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val filterOptions = when (kind) {
        "report" -> listOf(
            "all" to "Tất cả",
            "submitted" to "Chờ tiếp nhận",
            "in_progress" to "Đang xử lý",
            "resolved" to "Hoàn tất"
        )
        "bulletin" -> listOf(
            "all" to "Tất cả",
            "alert" to "Cảnh báo khẩn",
            "news" to "Bản tin đô thị"
        )
        "communication" -> listOf("all" to "Tất cả")
        else -> listOf(
            "all" to "Tất cả",
            "open" to "Đang mở",
            "closed" to "Đã đóng"
        )
    }

    val filteredItems = remember(rawItems, selectedFilter) {
        if (selectedFilter == "all") rawItems
        else rawItems.filter { item ->
            when (kind) {
                "report" -> {
                    val st = item.text("state").lowercase()
                    when (selectedFilter) {
                        "submitted" -> st in setOf("submitted", "new", "triage", "open")
                        "in_progress" -> st in setOf("in_progress", "processing", "assigned")
                        "resolved" -> st in setOf("resolved", "completed", "closed")
                        else -> true
                    }
                }
                "bulletin" -> item.text("category").lowercase() == selectedFilter
                else -> item.text("status").lowercase() == selectedFilter
            }
        }
    }

    val isAuthError = (!api.auth.signedIn && !api.isPublicRead(path)) ||
            error?.contains("đăng nhập", ignoreCase = true) == true ||
            error?.contains("token", ignoreCase = true) == true ||
            error?.contains("xác thực", ignoreCase = true) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OpenCityColors.TextMain
                )
                Text(
                    text = if (isAuthError) "Yêu cầu xác thực tài khoản" else "${filteredItems.size} mục hiển thị",
                    fontSize = 12.sp,
                    color = if (isAuthError) OpenCityColors.BrandAzure else OpenCityColors.TextMuted
                )
            }

            IconButton(
                onClick = { reload++ },
                colors = IconButtonDefaults.iconButtonColors(containerColor = OpenCityColors.CardBg)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Làm mới",
                    tint = OpenCityColors.BrandAzure
                )
            }
        }

        action?.let {
            Spacer(Modifier.height(14.dp))
            it()
        }

        Spacer(Modifier.height(14.dp))

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterOptions) { (key, label) ->
                val isSelected = selectedFilter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = key },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OpenCityColors.BrandAzure,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = OpenCityColors.TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.BorderColor
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (isAuthError) {
            val gateTitle = when (kind) {
                "survey" -> "Đăng nhập để tham gia khảo sát ý kiến"
                "report" -> "Đăng nhập để theo dõi hồ sơ phản ánh"
                else -> "Yêu cầu đăng nhập tài khoản Công dân"
            }
            val gateDesc = when (kind) {
                "survey" -> "Mỗi tài khoản công dân tham gia biểu quyết một lần cho từng chủ trương. Dữ liệu tổng hợp được phân tách theo phạm vi quản lý của đơn vị bạn."
                "report" -> "Hồ sơ phản ánh hiện trường được liên kết với định danh tài khoản công dân DX-OS để bảo mật thông tin và theo dõi tiến độ xử lý từ cán bộ đô thị."
                else -> "Tính năng này yêu cầu định danh tài khoản công dân DX-OS để xem thông tin và tham gia tương tác."
            }
            LoginGateCard(
                title = gateTitle,
                description = gateDesc,
                onLogin = onLogin
            )
        } else {
            error?.let { ErrorBanner(it) }

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OpenCityColors.BrandAzure)
                }
            } else if (filteredItems.isEmpty()) {
                EmptyPlaceholder("Không tìm thấy dữ liệu phù hợp với bộ lọc hiện tại.")
            } else {
                filteredItems.forEach { item ->
                    ItemCard(item = item, kind = kind) { onClick(item.text("id")) }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ItemCard(item: JSONObject, kind: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Category & State Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val categoryName = when (kind) {
                    "report" -> item.text("category").ifBlank { "Hạ tầng" }
                    "bulletin" -> if (item.text("category") == "alert") "CẢNH BÁO" else "BẢN TIN"
                    "communication" -> "TRUYỀN THÔNG"
                    else -> "KHẢO SÁT"
                }

                Surface(
                    color = if (categoryName == "CẢNH BÁO") OpenCityColors.BrandRedLight else OpenCityColors.BrandAzureLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = categoryName.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (categoryName == "CẢNH BÁO") OpenCityColors.BrandRedDark else OpenCityColors.BrandAzureDark
                    )
                }

                StatusPill(item, kind)
            }

            Spacer(Modifier.height(10.dp))

            // Card Title
            Text(
                text = item.text("title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextMain,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Card Subtitle / Description
            val subtitle = when (kind) {
                "report" -> item.text("address")
                "bulletin", "communication" -> item.text("summary").ifBlank { item.text("body") }
                else -> item.text("description")
            }

            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (kind == "report") {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = OpenCityColors.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = subtitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = OpenCityColors.TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // Card Footer
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = OpenCityColors.BorderColor, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val meta = when (kind) {
                    "report" -> item.text("created_at").take(16).replace("T", " ")
                    "bulletin", "communication" -> item.text("area").ifBlank { "Toàn thành phố" }
                    else -> "${item.optJSONArray("options")?.length() ?: 0} lựa chọn"
                }
                Text(
                    text = meta,
                    fontSize = 11.sp,
                    color = OpenCityColors.TextLight
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Chi tiết",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.BrandAzure
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = OpenCityColors.BrandAzure,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(item: JSONObject, kind: String) {
    val (label, bg, fg) = when (kind) {
        "report" -> {
            val state = item.text("state").lowercase()
            when (state) {
                "submitted", "new" -> Triple("Mới gửi", OpenCityColors.BrandAzureLight, OpenCityColors.BrandAzureDark)
                "acknowledged", "triaged" -> Triple("Đã tiếp nhận", Color(0xFFEFF6FF), Color(0xFF1D4ED8))
                "in_progress", "processing" -> Triple("Đang xử lý", OpenCityColors.BrandAmberLight, OpenCityColors.BrandAmberDark)
                "resolved", "completed", "closed" -> Triple("Hoàn tất", OpenCityColors.BrandMintLight, OpenCityColors.BrandMintDark)
                "rejected" -> Triple("Từ chối", OpenCityColors.BrandRedLight, OpenCityColors.BrandRedDark)
                else -> Triple(item.text("state").ifBlank { "Ghi nhận" }, OpenCityColors.SurfaceBg, OpenCityColors.TextSecondary)
            }
        }
        "bulletin", "communication" -> {
            if (item.text("category") == "alert") Triple("Khẩn cấp", OpenCityColors.BrandRedLight, OpenCityColors.BrandRedDark)
            else Triple("Công khai", OpenCityColors.BrandMintLight, OpenCityColors.BrandMintDark)
        }
        else -> {
            if (item.text("status") == "open") Triple("Đang mở", OpenCityColors.BrandMintLight, OpenCityColors.BrandMintDark)
            else Triple("Đã kết thúc", Color(0xFFF1F5F9), Color(0xFF64748B))
        }
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

// 4. màn hình thông tin chi tiết
@Composable
fun DetailScreen(
    api: ApiClient,
    kind: String,
    id: String,
    revision: Int,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onLogin: (() -> Unit)? = null
) {
    val path = when (kind) {
        "report" -> "/reports/$id"
        "bulletin" -> "/bulletins/$id"
        "communication" -> "/communications/$id"
        else -> "/surveys/$id"
    }

    var item by remember(path) { mutableStateOf<JSONObject?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var busy by remember(path) { mutableStateOf(false) }
    var selectedOptionId by remember(path) { mutableStateOf<String?>(null) }
    var localRevision by remember(path) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(path, revision, localRevision) {
        if (!api.auth.signedIn && !api.isPublicRead(path)) {
            error = "Yêu cầu đăng nhập tài khoản công dân DX-OS để xem chi tiết."
            return@LaunchedEffect
        }
        try {
            item = api.item(path)
            error = null
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Back Link
        Row(
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Quay lại",
                tint = OpenCityColors.BrandAzure,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Quay lại danh sách",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.BrandAzure
            )
        }

        Spacer(Modifier.height(12.dp))

        val isAuthError = (!api.auth.signedIn && !api.isPublicRead(path)) ||
                error?.contains("đăng nhập", ignoreCase = true) == true ||
                error?.contains("token", ignoreCase = true) == true ||
                error?.contains("xác thực", ignoreCase = true) == true

        if (isAuthError) {
            LoginGateCard(
                title = "Đăng nhập để xem nội dung chi tiết",
                description = "Nội dung này yêu cầu định danh tài khoản công dân DX-OS để bảo mật thông tin và kiểm tra quyền hạn truy cập.",
                onLogin = onLogin,
                onBack = onBack
            )
            return@Column
        }

        error?.let {
            ErrorBanner(it)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { localRevision++ },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenCityColors.BrandAzure),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Thử tải lại")
            }
        }

        val data = item
        if (data == null) {
            if (error == null) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OpenCityColors.BrandAzure)
                }
            }
            return@Column
        }

        // Header Card with Title & Badge
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val badgeText = when (kind) {
                        "report" -> "HỒ SƠ PHẢN ÁNH #${id.take(8).uppercase()}"
                        "bulletin" -> if (data.text("category") == "alert") "⚠️ CẢNH BÁO AN TOÀN" else "📰 BẢN TIN ĐÔ THỊ"
                        "communication" -> "THÔNG TIN TRUYỀN THÔNG"
                        else -> "📊 KHẢO SÁT Ý KIẾN"
                    }
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.BrandAzureDark
                    )

                    StatusPill(data, kind)
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = data.text("title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OpenCityColors.TextMain
                )

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = OpenCityColors.BorderColor)
                Spacer(Modifier.height(14.dp))

                when (kind) {
                    "report" -> {
                        InfoRow(icon = Icons.Default.Category, label = "Lĩnh vực", value = data.text("category").ifBlank { "Chưa phân loại" })
                        InfoRow(icon = Icons.Default.Place, label = "Địa điểm", value = data.text("address").ifBlank { "Theo định vị GPS" })
                        val assignedUnit = when (data.text("assigned_unit")) {
                            "infrastructure" -> "Hạ tầng & chiếu sáng"
                            "environment" -> "Môi trường & cây xanh"
                            "transport" -> "Giao thông"
                            "intake" -> "Tiếp nhận chung"
                            else -> "Chưa phân công"
                        }
                        InfoRow(icon = Icons.Default.Groups, label = "Đơn vị xử lý", value = assignedUnit)
                        if (data.text("due_at").isNotBlank()) {
                            InfoRow(icon = Icons.Default.Schedule, label = "Hạn xử lý",
                                value = data.text("due_at").take(16).replace("T", " "))
                        }
                        val lat = data.number("latitude")
                        val lon = data.number("longitude")
                        if (lat != null && lon != null) {
                            InfoRow(icon = Icons.Default.MyLocation, label = "Tọa độ GPS", value = "%.5f, %.5f".format(lat, lon))
                        }
                    }
                    "bulletin", "communication" -> {
                        InfoRow(icon = Icons.Default.LocationCity, label = "Khu vực áp dụng", value = data.text("area").ifBlank { "Toàn thành phố" })
                        InfoRow(icon = Icons.Default.Schedule, label = "Thời gian phát", value = data.text("published_at").take(16).replace("T", " "))
                    }
                    else -> {
                        InfoRow(icon = Icons.Default.CheckCircle, label = "Tình trạng", value = if (data.text("status") == "open") "Đang lấy ý kiến cư dân" else "Đã đóng biểu quyết")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Content Section Card
        when (kind) {
            "report" -> {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Nội dung phản ánh chi tiết",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMain
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = data.text("description"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OpenCityColors.TextSecondary,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Tiến trình xử lý
                val history = data.optJSONArray("history")
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Timeline, contentDescription = null, tint = OpenCityColors.BrandAzure)
                            Text(
                                text = "Tiến trình xử lý của cơ quan chức năng",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.TextMain
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        if (history == null || history.length() == 0) {
                            Text(
                                text = "Hồ sơ đã được gửi lên hệ thống và đang chờ cán bộ phân loại, tiếp nhận.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OpenCityColors.TextMuted
                            )
                        } else {
                            for (i in 0 until history.length()) {
                                val event = history.optJSONObject(i) ?: continue
                                val isLast = i == history.length() - 1
                                TimelineNode(
                                    action = event.text("action"),
                                    timestamp = event.text("created_at"),
                                    note = listOfNotNull(
                                        event.text("to_unit").takeIf { it.isNotBlank() }?.let { "Đơn vị: $it" },
                                        event.text("note").takeIf { it.isNotBlank() }
                                    ).joinToString(" · "),
                                    isLast = isLast
                                )
                            }
                        }
                    }
                }
            }

            "bulletin", "communication" -> {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        if (data.text("summary").isNotBlank()) {
                            Surface(
                                color = OpenCityColors.SurfaceBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = data.text("summary"),
                                    modifier = Modifier.padding(14.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = OpenCityColors.TextSecondary,
                                    lineHeight = 20.sp
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                        }

                        Text(
                            text = if (kind == "communication") "Nội dung truyền thông" else "Chi tiết bản tin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMain
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = data.text("body"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OpenCityColors.TextSecondary,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            else -> { // Survey
                val mine = data.text("my_option_id")
                val isOpen = data.text("status") == "open"
                val options = data.optJSONArray("options")

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Mục đích khảo sát",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMain
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = data.text("description"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OpenCityColors.TextSecondary,
                            lineHeight = 22.sp
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = if (mine.isNotBlank() || !isOpen) "Kết quả biểu quyết" else "Chọn ý kiến của bạn",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OpenCityColors.TextMain
                        )

                        Spacer(Modifier.height(12.dp))

                        var totalVotes = 0
                        if (options != null) {
                            for (i in 0 until options.length()) {
                                totalVotes += options.optJSONObject(i)?.optInt("votes") ?: 0
                            }
                        }

                        if (options != null) {
                            for (i in 0 until options.length()) {
                                val opt = options.optJSONObject(i) ?: continue
                                val optId = opt.text("id")
                                val label = opt.text("label")
                                val votes = opt.optInt("votes")
                                val pct = if (totalVotes > 0) (votes * 100) / totalVotes else 0
                                val isMyChoice = mine == optId
                                val isSelected = selectedOptionId == optId

                                if (mine.isNotBlank() || !isOpen) {
                                    // Result Progress Bar View
                                    Surface(
                                        color = if (isMyChoice) OpenCityColors.BrandAzureLight else OpenCityColors.SurfaceBg,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isMyChoice) OpenCityColors.BrandAzureBorder else OpenCityColors.BorderColor
                                        ),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontWeight = if (isMyChoice) FontWeight.ExtraBold else FontWeight.Medium,
                                                        color = if (isMyChoice) OpenCityColors.BrandAzureDark else OpenCityColors.TextMain,
                                                        fontSize = 14.sp
                                                    )
                                                    if (isMyChoice) {
                                                        Surface(
                                                            color = OpenCityColors.BrandAzure,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "Lựa chọn của bạn",
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "$votes phiếu ($pct%)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OpenCityColors.TextMuted
                                                )
                                            }

                                            Spacer(Modifier.height(8.dp))

                                            LinearProgressIndicator(
                                                progress = { if (totalVotes > 0) votes.toFloat() / totalVotes.toFloat() else 0f },
                                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                                color = if (isMyChoice) OpenCityColors.BrandAzure else OpenCityColors.BrandCyan,
                                                trackColor = OpenCityColors.BorderColor
                                            )
                                        }
                                    }
                                } else {
                                    // Interactive voting selection card
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp)
                                            .clickable { selectedOptionId = optId },
                                        color = if (isSelected) OpenCityColors.BrandAzureLight else Color.White,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(
                                            1.5.dp,
                                            if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.BorderColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedOptionId = optId },
                                                colors = RadioButtonDefaults.colors(selectedColor = OpenCityColors.BrandAzure)
                                            )
                                            Text(
                                                text = label,
                                                modifier = Modifier.weight(1f),
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = OpenCityColors.TextMain
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (mine.isBlank() && isOpen) {
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val optId = selectedOptionId ?: return@Button
                                    busy = true
                                    scope.launch {
                                        try {
                                            api.answer(id, optId)
                                            localRevision++
                                            onChanged()
                                        } catch (e: Exception) {
                                            error = e.message
                                        } finally {
                                            busy = false
                                        }
                                    }
                                },
                                enabled = !busy && selectedOptionId != null,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(
                                    text = if (busy) "Đang gửi biểu quyết..." else "Gửi biểu quyết khảo sát",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        if (mine.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                color = OpenCityColors.BrandMintLight,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = OpenCityColors.BrandMintDark)
                                    Text(
                                        text = "Bạn đã hoàn thành biểu quyết khảo sát ý kiến này.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OpenCityColors.BrandMintDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = OpenCityColors.TextMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, modifier = Modifier.width(110.dp), fontSize = 13.sp, color = OpenCityColors.TextSecondary)
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = OpenCityColors.TextMain
        )
    }
}

@Composable
private fun TimelineNode(action: String, timestamp: String, note: String?, isLast: Boolean) {
    val label = when (action) {
        "start" -> "Người dân gửi phản ánh"
        "triage" -> "Đã tiếp nhận"
        "assign", "assigned" -> "Đã phân công đơn vị"
        "accepted" -> "Đơn vị đã nhận việc"
        "progress" -> "Cập nhật tiến độ"
        "transfer_requested" -> "Đề nghị chuyển đơn vị"
        "transfer_rejected" -> "Từ chối chuyển đơn vị"
        "reassigned" -> "Đã chuyển đơn vị"
        "resolve", "resolved" -> "Đã hoàn tất xử lý"
        else -> action
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(OpenCityColors.BrandAzure)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(OpenCityColors.BrandAzureBorder)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 12.dp)) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
            if (timestamp.isNotBlank()) {
                Text(text = timestamp.take(16).replace("T", " "), fontSize = 11.sp, color = OpenCityColors.TextLight)
            }
            if (!note.isNullOrBlank()) {
                Text(text = note, fontSize = 12.sp, color = OpenCityColors.TextSecondary)
            }
        }
    }
}

// 5. ACCOUNT SCREEN
@Composable
fun AccountScreen(
    signedIn: Boolean,
    userName: String? = null,
    isDispatcher: Boolean = false,
    roleBadge: String = "guest",
    roleTitle: String = "Khách vãng lai",
    login: () -> Unit,
    register: () -> Unit = {},
    onLogout: () -> Unit,
    onSurveys: () -> Unit,
    onCatalog: () -> Unit,
    onBusinessFeedback: () -> Unit = {},
    onDashboard: () -> Unit = {},
    onDispatcherHub: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (isDispatcher) "Hồ sơ Cán bộ tác nghiệp" else "Hồ sơ công dân",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OpenCityColors.TextMain
        )

        Spacer(Modifier.height(14.dp))

        // Digital Citizen
        Surface(
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(OpenCityColors.BrandNavyDark, Color(0xFF1E293B))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NexusPortalMark(size = 32.dp)
                        Surface(
                            color = when {
                                isDispatcher -> Color(0x33818CF8)
                                signedIn -> Color(0x3310B981)
                                else -> Color(0x333B82F6)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = when {
                                    isDispatcher -> "✓ CÁN BỘ ĐIỀU PHỐI DX-OS"
                                    signedIn -> "✓ ĐÃ ĐỊNH DANH DX-OS"
                                    else -> "CHẾ ĐỘ KHÁCH"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isDispatcher -> Color(0xFFA5B4FC)
                                    signedIn -> Color(0xFF6EE7B7)
                                    else -> Color(0xFF93C5FD)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = when {
                            isDispatcher -> "HỒ SƠ CÁN BỘ ĐIỀU PHỐI & TÁC NGHIỆP"
                            signedIn -> "HỒ SƠ TÀI KHOẢN ĐỊNH DANH"
                            else -> "PHIÊN DUYỆT CÔNG KHAI"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = if (signedIn) (userName?.takeIf { it.isNotBlank() } ?: "Công dân OpenCity") else "Khách vãng lai",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (signedIn) "Keycloak OIDC Single Sign-On" else "Tra cứu dịch vụ công không cần đăng nhập",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = roleBadge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isDispatcher -> Color(0xFFA5B4FC)
                                signedIn -> Color(0xFF6EE7B7)
                                else -> Color(0xFF93C5FD)
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Quick Navigation Menu
        Text(
            text = "Dịch vụ liên kết",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OpenCityColors.TextMain
        )

        Spacer(Modifier.height(10.dp))

        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                MenuItem(
                    icon = Icons.Default.Poll,
                    iconColor = OpenCityColors.BrandMintDark,
                    title = "Khảo sát ý kiến cư dân",
                    subtitle = "Xem và tham gia các cuộc khảo sát công khai",
                    onClick = onSurveys
                )
                HorizontalDivider(color = OpenCityColors.BorderColor)
                MenuItem(
                    icon = Icons.Default.BusinessCenter,
                    iconColor = OpenCityColors.BrandAzure,
                    title = "Doanh nghiệp kiến nghị",
                    subtitle = "Nộp hồ sơ tháo gỡ khó khăn (thuế, mặt bằng...) và theo dõi tiến trình",
                    onClick = onBusinessFeedback
                )
                HorizontalDivider(color = OpenCityColors.BorderColor)
                MenuItem(
                    icon = Icons.Default.Speed,
                    iconColor = OpenCityColors.BrandCyan,
                    title = "Dashboard Thống kê Đô thị",
                    subtitle = "Biểu đồ tiến độ xử lý phản ánh theo quận/huyện, thời gian TB & hài lòng",
                    onClick = onDashboard
                )
                HorizontalDivider(color = OpenCityColors.BorderColor)
                MenuItem(
                    icon = Icons.Default.Category,
                    iconColor = OpenCityColors.BrandAmberDark,
                    title = "Danh mục dịch vụ công số",
                    subtitle = "Xem sơ đồ các tiện ích số trên toàn thành phố",
                    onClick = onCatalog
                )
                if (isDispatcher) {
                    HorizontalDivider(color = OpenCityColors.BorderColor)
                    MenuItem(
                        icon = Icons.Default.SupportAgent,
                        iconColor = Color(0xFF2563EB),
                        title = "Tác nghiệp Cán bộ",
                        subtitle = "Phân luồng hồ sơ hiện trường bằng AI & Duyệt lịch tiếp dân trực tuyến",
                        onClick = onDispatcherHub
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Auth Management Button
        if (signedIn) {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, OpenCityColors.BrandRedBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenCityColors.BrandRedDark)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(text = "Đăng xuất tài khoản DX-OS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        } else {
            Surface(
                color = OpenCityColors.BrandAzureLight,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OpenCityColors.BrandAzure),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Chế độ Khách tra cứu", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OpenCityColors.BrandAzureDark)
                        Text(
                            "Bạn có thể tra cứu thông tin dịch vụ công, giao thông, môi trường mà không cần tài khoản. Đăng nhập hoặc đăng ký để gửi phản ánh hiện trường.",
                            fontSize = 11.sp,
                            color = OpenCityColors.TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Button(
                onClick = login,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(text = "Đăng nhập với DX-OS Citizen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = register,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, OpenCityColors.BrandAzure)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = OpenCityColors.BrandAzure, modifier = Modifier.size(18.dp))
                    Text(text = "Đăng ký tài khoản công dân mới", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = OpenCityColors.BrandAzure)
                }
            }
        }

        Spacer(Modifier.height(30.dp))

        // System Footer
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "OpenCity Mobile v1.0.0",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextLight
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Hệ sinh thái Đô thị thông minh DX-OS • Native Jetpack Compose",
                fontSize = 11.sp,
                color = OpenCityColors.TextLight
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(OpenCityColors.SurfaceBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
            Text(text = subtitle, fontSize = 12.sp, color = OpenCityColors.TextSecondary)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = OpenCityColors.TextLight,
            modifier = Modifier.size(16.dp)
        )
    }
}

// 6. CATALOG SCREEN
@Composable
private fun getServiceIconOutlined(iconName: String, categoryId: String): ImageVector {
    return when (iconName) {
        "MapPin", "MapPinned" -> Icons.Outlined.Place
        "AddLocationAlt" -> Icons.Outlined.AddLocationAlt
        "Layers" -> Icons.Outlined.Layers
        "Bell" -> Icons.Outlined.Notifications
        "Newspaper" -> Icons.AutoMirrored.Outlined.Article
        "Megaphone" -> Icons.Outlined.Campaign
        "TriangleAlert", "FileWarning", "ShieldAlert" -> Icons.Outlined.WarningAmber
        "BusFront", "Route" -> Icons.Outlined.DirectionsBus
        "CalendarDays", "CalendarCheck" -> Icons.Outlined.CalendarToday
        "Headset" -> Icons.Outlined.SupportAgent
        "BriefcaseBusiness" -> Icons.Outlined.WorkOutline
        "Landmark" -> Icons.Outlined.AccountBalance
        "Building2", "House" -> Icons.Outlined.LocationCity
        "UsersRound", "MessageCircle", "MessagesSquare" -> Icons.Outlined.Groups
        "ShieldCheck" -> Icons.Outlined.Shield
        "Camera" -> Icons.Outlined.PhotoCamera
        "Ticket" -> Icons.Outlined.ConfirmationNumber
        "Plane" -> Icons.Outlined.Flight
        "ParkingCircle" -> Icons.Outlined.LocalParking
        "TrafficCone" -> Icons.Outlined.Traffic
        "ClipboardPlus", "ClipboardCheck" -> Icons.Outlined.Assignment
        "HeartPulse", "Hospital", "Globe" -> Icons.Outlined.LocalHospital
        "HeartHandshake" -> Icons.Outlined.FavoriteBorder
        "Pill" -> Icons.Outlined.Medication
        "School", "GraduationCap" -> Icons.Outlined.School
        "BadgeCheck", "BadgeInfo" -> Icons.Outlined.Verified
        "BookOpen", "BookOpenCheck" -> Icons.Outlined.MenuBook
        "Map", "Compass" -> Icons.Outlined.Explore
        "Castle", "Heritage" -> Icons.Outlined.AccountBalance
        "Store", "ShoppingBag" -> Icons.Outlined.Storefront
        "ScanQrCode" -> Icons.Outlined.QrCode
        "Flame" -> Icons.Outlined.LocalFireDepartment
        "Gift" -> Icons.Outlined.CardGiftcard
        "Wind" -> Icons.Outlined.Air
        "PhoneCall" -> Icons.Outlined.Phone
        "Wallet" -> Icons.Outlined.AccountBalanceWallet
        "Tv" -> Icons.Outlined.Tv
        "Lightbulb" -> Icons.Outlined.Lightbulb
        "ListChecks" -> Icons.Outlined.Poll
        "FolderOpen" -> Icons.Outlined.Folder
        else -> when (categoryId) {
            "feedback" -> Icons.Outlined.Place
            "transport" -> Icons.Outlined.DirectionsBus
            "news" -> Icons.AutoMirrored.Outlined.Article
            "planning" -> Icons.Outlined.Layers
            "utilities" -> Icons.Outlined.Widgets
            "health" -> Icons.Outlined.LocalHospital
            "education" -> Icons.Outlined.School
            "community" -> Icons.Outlined.Groups
            "government", "public-service" -> Icons.Outlined.AccountBalance
            else -> Icons.Outlined.Category
        }
    }
}

@Composable
private fun CatalogServiceItem(
    service: JSONObject,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    onClick: () -> Unit
) {
    val catId = service.text("category")
    val isOffline = CatalogCacheManager.isOfflineCapable(service)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getServiceIconOutlined(service.text("icon"), catId),
                contentDescription = null,
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = service.text("name"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (isOffline) {
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(0.5.dp, Color(0xFFBBF7D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A))
                            )
                            Text(
                                text = "Offline",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            if (service.text("description").isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = service.text("description"),
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Professional Bookmark Icon Button
        IconButton(
            onClick = onTogglePin,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (isPinned) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (isPinned) "Bỏ lưu" else "Lưu dịch vụ",
                tint = if (isPinned) Color(0xFF0284C7) else Color(0xFF94A3B8),
                modifier = Modifier.size(19.dp)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = "Xem",
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
fun CatalogScreen(
    signedIn: Boolean = false,
    isDispatcher: Boolean = false,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogin: () -> Unit = {},
    onRegister: () -> Unit = {}
) {
    val context = LocalContext.current
    var cacheRevision by remember { mutableIntStateOf(0) }
    val catalog = remember(cacheRevision) {
        CatalogCacheManager.getCatalog(context)
    }
    val lastSyncedTime = remember(cacheRevision) {
        CatalogCacheManager.getLastSyncedTimeFormatted(context)
    }
    var pinnedIds by remember(cacheRevision) {
        mutableStateOf(HashSet(CatalogCacheManager.getPinnedIds(context)))
    }

    val categories = catalog.optJSONArray("categories")
    val services = catalog.optJSONArray("services")

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("all") }
    var selectedFilter by remember { mutableStateOf("all") } // "all", "available", "offline", "planned"
    var inspectService by remember { mutableStateOf<JSONObject?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Quay lại",
                tint = OpenCityColors.BrandAzure,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Quay lại",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.BrandAzure
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Danh mục Dịch vụ Công số",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Hệ thống tra cứu quy trình, dữ liệu và tiện ích đô thị số.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Refined Offline Sync Status Bar
        Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Column {
                        Text(
                            text = "Bộ nhớ đệm ngoại tuyến (Offline Ready)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Đồng bộ: $lastSyncedTime • Dữ liệu sẵn sàng tra cứu không cần mạng",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        CatalogCacheManager.refreshCacheFromAssets(context)
                        cacheRevision++
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.Sync,
                        contentDescription = "Đồng bộ lại",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (!signedIn) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = Color(0xFFF0F9FF),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                    Text(
                        text = "Chế độ Khách: Tra cứu thông tin mọi dịch vụ công không cần đăng nhập",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0369A1)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Search Bar
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isNotBlank() && selectedCategory != "all") {
                            selectedCategory = "all"
                        }
                    },
                    placeholder = { Text("Tìm kiếm dịch vụ, quy hoạch, xe buýt, AQI...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Xóa", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Filter Row 1: Status Filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text("Tất cả", fontSize = 12.sp, fontWeight = if (selectedFilter == "all") FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0F172A),
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "available",
                    onClick = { selectedFilter = "available" },
                    label = { Text("Trực tuyến", fontSize = 12.sp, fontWeight = if (selectedFilter == "available") FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF0FDF4),
                        selectedLabelColor = Color(0xFF15803D)
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "offline",
                    onClick = { selectedFilter = "offline" },
                    label = { Text("Sẵn sàng ngoại tuyến", fontSize = 12.sp, fontWeight = if (selectedFilter == "offline") FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF0F9FF),
                        selectedLabelColor = Color(0xFF0369A1)
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "planned",
                    onClick = { selectedFilter = "planned" },
                    label = { Text("Sắp triển khai", fontSize = 12.sp, fontWeight = if (selectedFilter == "planned") FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFFFBEB),
                        selectedLabelColor = Color(0xFFB45309)
                    )
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Filter Row 2: Category Filters with Dynamic Count Badges (Accounts for Search Query & Status Filter)
        if (categories != null && services != null) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    var totalMatching = 0
                    for (j in 0 until services.length()) {
                        val s = services.optJSONObject(j) ?: continue
                        when (selectedFilter) {
                            "available" -> if (s.text("status") != "available") continue
                            "offline" -> if (!CatalogCacheManager.isOfflineCapable(s)) continue
                            "planned" -> if (s.text("status") != "planned") continue
                        }
                        if (searchQuery.isNotBlank()) {
                            val q = searchQuery.trim().lowercase()
                            val nameMatch = s.text("name").lowercase().contains(q)
                            val descMatch = s.text("description").lowercase().contains(q)
                            val idMatch = s.text("id").lowercase().contains(q)
                            if (!nameMatch && !descMatch && !idMatch) continue
                        }
                        totalMatching++
                    }
                    FilterChip(
                        selected = selectedCategory == "all",
                        onClick = { selectedCategory = "all" },
                        label = { Text("Tất cả ($totalMatching)", fontSize = 11.5.sp, fontWeight = if (selectedCategory == "all") FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE2E8F0),
                            selectedLabelColor = Color(0xFF0F172A)
                        )
                    )
                }
                for (i in 0 until categories.length()) {
                    val cat = categories.optJSONObject(i) ?: continue
                    val cId = cat.text("id")
                    val cName = cat.text("name")
                    var count = 0
                    for (j in 0 until services.length()) {
                        val s = services.optJSONObject(j) ?: continue
                        if (s.text("category") != cId) continue
                        when (selectedFilter) {
                            "available" -> if (s.text("status") != "available") continue
                            "offline" -> if (!CatalogCacheManager.isOfflineCapable(s)) continue
                            "planned" -> if (s.text("status") != "planned") continue
                        }
                        if (searchQuery.isNotBlank()) {
                            val q = searchQuery.trim().lowercase()
                            val nameMatch = s.text("name").lowercase().contains(q)
                            val descMatch = s.text("description").lowercase().contains(q)
                            val idMatch = s.text("id").lowercase().contains(q)
                            if (!nameMatch && !descMatch && !idMatch) continue
                        }
                        count++
                    }
                    item {
                        FilterChip(
                            selected = selectedCategory == cId,
                            onClick = {
                                if (count == 0 && searchQuery.isNotBlank()) {
                                    searchQuery = ""
                                }
                                selectedCategory = cId
                            },
                            label = { Text("$cName ($count)", fontSize = 11.5.sp, fontWeight = if (selectedCategory == cId) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE2E8F0),
                                selectedLabelColor = Color(0xFF0F172A)
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Main Services Scroll List
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (categories != null && services != null) {
                // Pinned / Bookmarked Section
                if (pinnedIds.isNotEmpty() && searchQuery.isBlank() && selectedCategory == "all" && selectedFilter == "all") {
                    val pinnedServices = mutableListOf<JSONObject>()
                    for (j in 0 until services.length()) {
                        val s = services.optJSONObject(j) ?: continue
                        if (s.text("id") in pinnedIds) {
                            pinnedServices.add(s)
                        }
                    }

                    if (pinnedServices.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Bookmark,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "TIỆN ÍCH ĐÃ LƯU TRUY CẬP NHANH",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Text(
                                text = "${pinnedServices.size} mục",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }

                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                        ) {
                            Column {
                                pinnedServices.forEachIndexed { index, service ->
                                    if (index > 0) HorizontalDivider(color = Color(0xFFF1F5F9))
                                    val sId = service.text("id")
                                    CatalogServiceItem(
                                        service = service,
                                        isPinned = true,
                                        onTogglePin = {
                                            CatalogCacheManager.togglePin(context, sId)
                                            pinnedIds = HashSet(CatalogCacheManager.getPinnedIds(context))
                                        },
                                        onClick = { inspectService = service }
                                    )
                                }
                            }
                        }
                    }
                }

                var totalFound = 0
                for (i in 0 until categories.length()) {
                    val category = categories.optJSONObject(i) ?: continue
                    val catId = category.text("id")

                    if (selectedCategory != "all" && selectedCategory != catId) continue

                    val matchedServices = mutableListOf<JSONObject>()
                    for (j in 0 until services.length()) {
                        val s = services.optJSONObject(j) ?: continue
                        if (s.text("category") != catId) continue

                        // Filter check
                        when (selectedFilter) {
                            "available" -> if (s.text("status") != "available") continue
                            "offline" -> if (!CatalogCacheManager.isOfflineCapable(s)) continue
                            "planned" -> if (s.text("status") != "planned") continue
                        }

                        // Search query check
                        if (searchQuery.isNotBlank()) {
                            val q = searchQuery.trim().lowercase()
                            val nameMatch = s.text("name").lowercase().contains(q)
                            val descMatch = s.text("description").lowercase().contains(q)
                            val idMatch = s.text("id").lowercase().contains(q)
                            if (!nameMatch && !descMatch && !idMatch) continue
                        }
                        matchedServices.add(s)
                    }

                    if (matchedServices.isEmpty()) continue
                    totalFound += matchedServices.size

                    // Refined Category Section Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category.text("name").uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "${matchedServices.size} dịch vụ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column {
                            matchedServices.forEachIndexed { index, service ->
                                if (index > 0) HorizontalDivider(color = Color(0xFFF1F5F9))
                                val sId = service.text("id")
                                CatalogServiceItem(
                                    service = service,
                                    isPinned = sId in pinnedIds,
                                    onTogglePin = {
                                        CatalogCacheManager.togglePin(context, sId)
                                        pinnedIds = HashSet(CatalogCacheManager.getPinnedIds(context))
                                    },
                                    onClick = { inspectService = service }
                                )
                            }
                        }
                    }
                }

                if (totalFound == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp, start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(44.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() && selectedCategory != "all")
                                    "Không có dịch vụ \"$searchQuery\" trong danh mục này"
                                else if (searchQuery.isNotBlank())
                                    "Không tìm thấy kết quả cho \"$searchQuery\""
                                else
                                    "Không có dịch vụ nào phù hợp bộ lọc",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (searchQuery.isNotBlank() && selectedCategory != "all")
                                    "Từ khóa \"$searchQuery\" có thể thuộc danh mục khác (như Giao thông hoặc Phản ánh)."
                                else
                                    "Vui lòng thử từ khóa khác hoặc thiết lập lại bộ lọc.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(4.dp))

                            if (selectedCategory != "all") {
                                Button(
                                    onClick = { selectedCategory = "all" },
                                    colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Tìm \"$searchQuery\" trong Tất cả danh mục", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (searchQuery.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Xóa từ khóa tìm kiếm", fontSize = 12.sp, color = Color(0xFF334155))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Inspect Service Dialog
    inspectService?.let { service ->
        val isAvailable = service.text("status") == "available"
        val route = service.text("route").removePrefix("/")
        val isDispatcherOnly = route == "dispatcher-hub"
        val canNavigate = isAvailable && (
            route in setOf(
                "reports", "news", "map", "surveys", "appointments",
                "administrative-feedback", "air-quality", "find-bus", "bus-routes", "hotlines",
                "communications", "community-groups", "social-feedback", "digital-wards",
                "new-report", "notifications", "business-feedback", "dashboard"
            ) || (isDispatcherOnly && isDispatcher)
        )
        val requiresAuth = route in setOf("reports", "appointments", "administrative-feedback", "new-report", "dispatcher-hub")
        val isOffline = CatalogCacheManager.isOfflineCapable(service)
        val sId = service.text("id")
        val isPinned = sId in pinnedIds

        AlertDialog(
            onDismissRequest = { inspectService = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getServiceIconOutlined(service.text("icon"), service.text("category")),
                                contentDescription = null,
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = service.text("name"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                CatalogCacheManager.togglePin(context, sId)
                                pinnedIds = HashSet(CatalogCacheManager.getPinnedIds(context))
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isPinned) "Bỏ lưu" else "Lưu",
                                tint = if (isPinned) Color(0xFF0284C7) else Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { inspectService = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "Đóng", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (service.text("status")) {
                                "available" -> Color(0xFFF0FDF4)
                                "planned" -> Color(0xFFFFFBEB)
                                else -> Color(0xFFF8FAFC)
                            },
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                0.5.dp,
                                when (service.text("status")) {
                                    "available" -> Color(0xFFBBF7D0)
                                    "planned" -> Color(0xFFFDE68A)
                                    else -> Color(0xFFE2E8F0)
                                }
                            )
                        ) {
                            Text(
                                text = when (service.text("status")) {
                                    "available" -> "Trực tuyến"
                                    "planned" -> "Sắp triển khai"
                                    else -> "Cổng DVC Quốc gia"
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (service.text("status")) {
                                    "available" -> Color(0xFF15803D)
                                    "planned" -> Color(0xFFB45309)
                                    else -> Color(0xFF475569)
                                }
                            )
                        }

                        if (isOffline) {
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.5.dp, Color(0xFFBBF7D0))
                            ) {
                                Text(
                                    text = "Offline Ready",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Mã: ${service.text("id")}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    if (service.text("description").isNotBlank()) {
                        Text(
                            text = service.text("description"),
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }

                    if (isOffline) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(9.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.CloudDone, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Dữ liệu tiện ích này đã được lưu trong bộ nhớ đệm. Bạn có thể tra cứu bình thường ngay cả khi không có kết nối Internet.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text(
                        text = "Quy trình & Hướng dẫn giải quyết:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F172A)
                    )

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row {
                                Text("• Cơ quan thực hiện: ", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                Text("UBND Thành phố / Đơn vị chuyên trách", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                            Row {
                                Text("• Đối tượng tiếp nhận: ", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                Text("Công dân, tổ chức, doanh nghiệp trên địa bàn", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                            Row {
                                Text("• Cách thức: ", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                Text(if (canNavigate) "Trực tuyến qua ứng dụng OpenCity" else "Cổng DVC / Trực tiếp tại bộ phận một cửa", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                            Row {
                                Text("• Lệ phí: ", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF0F172A))
                                Text("Miễn phí / Theo quy định hiện hành", fontSize = 11.sp, color = Color(0xFF15803D))
                            }
                        }
                    }

                    if (isDispatcherOnly && !isDispatcher) {
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Dịch vụ tác nghiệp nội bộ dành riêng cho Cán bộ điều phối hiện trường (reports_dispatcher). Vui lòng đăng nhập với tài khoản Cán bộ để truy cập.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    } else if (!signedIn && requiresAuth) {
                        Surface(
                            color = Color(0xFFFFFBEB),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Bạn đang tra cứu ở Chế độ Khách. Để gửi hồ sơ hoặc tạo yêu cầu, vui lòng Đăng ký hoặc Đăng nhập tài khoản.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB45309),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isDispatcherOnly && !isDispatcher) {
                    Button(
                        onClick = {
                            inspectService = null
                            onLogin()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Đăng nhập Cán bộ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (canNavigate) {
                    if (!signedIn && requiresAuth) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    inspectService = null
                                    onRegister()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Đăng ký", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    inspectService = null
                                    onLogin()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Đăng nhập", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                inspectService = null
                                onNavigate(route)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Truy cập tiện ích →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { inspectService = null },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Đã hiểu", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                if (canNavigate && !signedIn && requiresAuth) {
                    TextButton(
                        onClick = {
                            inspectService = null
                            onNavigate(route)
                        }
                    ) {
                        Text("Xem thử màn hình", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }
        )
    }
}

// SHARED HELPER COMPONENTS
@Composable
internal fun SectionTitle(title: String) {
    SectionHeader(title = title)
}

@Composable
internal fun SectionHeader(title: String) {
    Spacer(Modifier.height(14.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = OpenCityColors.TextMain
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun ErrorBanner(message: String) {
    Surface(
        color = OpenCityColors.BrandRedLight,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OpenCityColors.BrandRedBorder),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = OpenCityColors.BrandRedDark)
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OpenCityColors.BrandRedDark
            )
        }
    }
}

@Composable
internal fun ErrorText(message: String) {
    ErrorBanner(message = message)
}

@Composable
internal fun EmptyPlaceholder(message: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = OpenCityColors.TextLight,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OpenCityColors.TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun LoginGateCard(
    title: String = "Yêu cầu đăng nhập tài khoản Công dân",
    description: String = "Tính năng này yêu cầu định danh tài khoản công dân DX-OS để xem thông tin và tham gia tương tác.",
    onLogin: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = OpenCityColors.BrandAzure.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = OpenCityColors.BrandAzure,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Text(
                    text = "XÁC THỰC TÀI KHOẢN CÔNG DÂN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextMain,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                fontSize = 13.sp,
                color = OpenCityColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(4.dp))

            if (onLogin != null) {
                Button(
                    onClick = onLogin,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Đăng nhập tài khoản DX-OS", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            if (onBack != null) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Quay lại danh sách", fontSize = 13.sp, color = OpenCityColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
internal fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.Medium, color = OpenCityColors.TextSecondary, fontSize = 13.sp)
        Text(text = value, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain, fontSize = 13.sp)
    }
    HorizontalDivider(color = OpenCityColors.BorderColor)
}
