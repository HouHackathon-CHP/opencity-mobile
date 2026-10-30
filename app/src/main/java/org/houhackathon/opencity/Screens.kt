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
import androidx.compose.material.icons.filled.*
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

// ==========================================
// 1. WELCOME SCREEN (When citizen is not logged in)
// ==========================================
@Composable
fun WelcomeScreen(login: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Identity Brand Mark
        Box(
            modifier = Modifier
                .size(90.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, OpenCityColors.BrandAzureBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            NexusPortalMark(size = 56.dp)
        }

        Spacer(Modifier.height(20.dp))

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

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Nền tảng tiếp nhận phản ánh hiện trường, cảnh báo an toàn thời gian thực và khảo sát ý kiến cộng đồng trên địa bàn đô thị.",
            style = MaterialTheme.typography.bodyMedium,
            color = OpenCityColors.TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(28.dp))

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
                icon = Icons.Default.Campaign,
                iconColor = OpenCityColors.BrandRed,
                iconBg = OpenCityColors.BrandRedLight,
                title = "Bản tin & Cảnh báo an toàn",
                desc = "Nhận thông báo khẩn cấp về giao thông, thời tiết và trật tự đô thị 24/7."
            )
            FeatureItem(
                icon = Icons.Default.Poll,
                iconColor = OpenCityColors.BrandMint,
                iconBg = OpenCityColors.BrandMintLight,
                title = "Khảo sát ý kiến cư dân",
                desc = "Tham gia đóng góp tiếng nói vào các đề xuất và quyết sách của thành phố."
            )
        }

        Spacer(Modifier.height(32.dp))

        // Sign In Action Button
        Button(
            onClick = login,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
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

// ==========================================
// 2. HOME SCREEN (Citizen Hub)
// ==========================================
@Composable
fun HomeScreen(
    api: ApiClient,
    revision: Int,
    onNavigate: (String) -> Unit,
    onDetail: (String, String) -> Unit
) {
    var bulletins by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var reports by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(revision) {
        loading = true
        try {
            bulletins = api.list("/bulletins").take(3)
            reports = api.list("/reports").take(3)
            error = null
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
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

                    Button(
                        onClick = { onNavigate("new-report") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(text = "+ Gửi phản ánh hiện trường mới", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// 3. LIST SCREEN (Reports, Bulletins, Surveys)
// ==========================================
@Composable
fun ListScreen(
    api: ApiClient,
    path: String,
    title: String,
    revision: Int,
    onClick: (String) -> Unit,
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
        else -> "survey"
    }

    LaunchedEffect(path, revision, reload) {
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
                    text = "${filteredItems.size} mục hiển thị",
                    fontSize = 12.sp,
                    color = OpenCityColors.TextMuted
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

        Spacer(Modifier.height(24.dp))
    }
}

// ==========================================
// ITEM CARD (Modern Card Component)
// ==========================================
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
                "bulletin" -> item.text("summary").ifBlank { item.text("body") }
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
                    "bulletin" -> item.text("area").ifBlank { "Toàn thành phố" }
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
        "bulletin" -> {
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

// ==========================================
// 4. DETAIL SCREEN (Master Detail)
// ==========================================
@Composable
fun DetailScreen(
    api: ApiClient,
    kind: String,
    id: String,
    revision: Int,
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    val path = when (kind) {
        "report" -> "/reports/$id"
        "bulletin" -> "/bulletins/$id"
        else -> "/surveys/$id"
    }

    var item by remember(path) { mutableStateOf<JSONObject?>(null) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var busy by remember(path) { mutableStateOf(false) }
    var selectedOptionId by remember(path) { mutableStateOf<String?>(null) }
    var localRevision by remember(path) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(path, revision, localRevision) {
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
                imageVector = Icons.Default.ArrowBack,
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

        error?.let { ErrorBanner(it) }

        val data = item
        if (data == null) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OpenCityColors.BrandAzure)
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
                    "bulletin" -> {
                        InfoRow(icon = Icons.Default.LocationCity, label = "Khu vực áp dụng", value = data.text("area").ifBlank { "Toàn thành phố" })
                        InfoRow(icon = Icons.Default.Schedule, label = "Thời gian phát", value = data.text("created_at").take(16).replace("T", " "))
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

                // Timeline Audit Trail (Tiến trình xử lý)
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

            "bulletin" -> {
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
                            text = "Chi tiết bản tin",
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

// ==========================================
// 5. ACCOUNT SCREEN (Citizen Identity Card)
// ==========================================
@Composable
fun AccountScreen(
    signedIn: Boolean,
    login: () -> Unit,
    onLogout: () -> Unit,
    onSurveys: () -> Unit,
    onCatalog: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Hồ sơ công dân",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OpenCityColors.TextMain
        )

        Spacer(Modifier.height(14.dp))

        // Digital Citizen ID Card Mockup
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
                            color = if (signedIn) Color(0x3310B981) else Color(0x33EF4444),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (signedIn) "✓ ĐÃ ĐĂNG NHẬP DX-OS" else "CHƯA ĐĂNG NHẬP",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (signedIn) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "HỒ SƠ TÀI KHOẢN THỬ NGHIỆM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = if (signedIn) "Công dân OpenCity" else "Khách vãng lai",
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
                            text = if (signedIn) "Tài khoản DX-OS demo" else "Chưa có phiên đăng nhập",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFCBD5E1)
                        )
                        Text(
                            text = "Môi trường thử nghiệm",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
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
                    icon = Icons.Default.Category,
                    iconColor = OpenCityColors.BrandAmberDark,
                    title = "Danh mục dịch vụ công số",
                    subtitle = "Xem sơ đồ các tiện ích số trên toàn thành phố",
                    onClick = onCatalog
                )
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

// ==========================================
// 6. CATALOG SCREEN (Public Services Catalog)
// ==========================================
@Composable
fun CatalogScreen(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val catalog = remember {
        runCatching { JSONObject(context.assets.open("catalog.json").bufferedReader().use { it.readText() }) }
            .getOrDefault(JSONObject())
    }
    val categories = catalog.optJSONArray("categories")
    val services = catalog.optJSONArray("services")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
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
                imageVector = Icons.Default.ArrowBack,
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

        Text(
            text = "Danh mục Dịch vụ Công số",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = OpenCityColors.TextMain
        )
        Text(
            text = "Sơ đồ các chức năng và dịch vụ đô thị đang vận hành và thử nghiệm.",
            fontSize = 13.sp,
            color = OpenCityColors.TextSecondary
        )

        Spacer(Modifier.height(16.dp))

        if (categories != null && services != null) {
            for (i in 0 until categories.length()) {
                val category = categories.optJSONObject(i) ?: continue
                val catId = category.text("id")

                SectionHeader(title = category.text("name"))

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                ) {
                    Column {
                        var first = true
                        for (j in 0 until services.length()) {
                            val service = services.optJSONObject(j) ?: continue
                            if (service.text("category") == catId) {
                                if (!first) HorizontalDivider(color = OpenCityColors.BorderColor)
                                first = false

                                val isAvailable = service.text("status") == "available"
                                val route = service.text("route").removePrefix("/")
                                val canNavigate = isAvailable && route in setOf("reports", "news", "map", "surveys")

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = canNavigate) { onNavigate(route) }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = service.text("name"),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OpenCityColors.TextMain
                                        )
                                        if (service.text("description").isNotBlank()) {
                                            Text(
                                                text = service.text("description"),
                                                fontSize = 12.sp,
                                                color = OpenCityColors.TextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Surface(
                                        color = if (isAvailable) OpenCityColors.BrandMintLight else OpenCityColors.SurfaceBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isAvailable) "Đang hoạt động" else "Chưa tích hợp",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAvailable) OpenCityColors.BrandMintDark else OpenCityColors.TextLight
                                        )
                                    }

                                    if (canNavigate) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = OpenCityColors.BrandAzure,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ==========================================
// SHARED HELPER COMPONENTS
// ==========================================
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
