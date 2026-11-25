package org.houhackathon.opencity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Duration
import java.time.Instant

enum class DispatcherTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TRIAGE("Phân luồng hồ sơ", Icons.AutoMirrored.Filled.Assignment),
    APPOINTMENTS("Duyệt lịch hẹn", Icons.Default.CalendarMonth),
    METRICS("Chỉ số tác nghiệp", Icons.Default.Insights)
}

data class HandlingUnit(val id: String, val name: String, val iconColor: Color)

val MUNICIPAL_UNITS = listOf(
    HandlingUnit("infrastructure", "Đội Hạ tầng Đô thị & Cấp thoát nước", Color(0xFF2563EB)),
    HandlingUnit("transport", "Đội Giao thông & Trật tự Đô thị", Color(0xFF16A34A)),
    HandlingUnit("environment", "Đội Vệ sinh & Môi trường Đô thị", Color(0xFFD97706)),
    HandlingUnit("intake", "Bộ phận Một cửa & Tiếp nhận Hành chính", Color(0xFF9333EA))
)

@Composable
fun DispatcherHubScreen(
    api: ApiClient,
    revision: Int,
    isDispatcher: Boolean = true,
    onNavigateBack: () -> Unit,
    onViewReportDetail: (String) -> Unit = {}
) {
    if (!isDispatcher) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(OpenCityColors.SurfaceBg)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF2F2),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = "Quyền truy cập bị từ chối (403)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.TextMain
                    )
                    Text(
                        text = "Chức năng Tác nghiệp Cán bộ chỉ dành riêng cho Cán bộ Điều phối hiện trường và Ban quản lý có quyền 'reports_dispatcher'. Tài khoản công dân thông thường không được phép truy cập.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OpenCityColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quay lại màn hình Tài khoản", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    var selectedTab by remember { mutableStateOf(DispatcherTab.TRIAGE) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    var reports by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var metrics by remember { mutableStateOf<JSONObject?>(null) }

    var loadingReports by remember { mutableStateOf(true) }
    var loadingAppointments by remember { mutableStateOf(true) }
    var loadingMetrics by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(revision, refreshKey) {
        loadingReports = true
        loadingAppointments = true
        loadingMetrics = true
        errorMessage = null

        try {
            val list = api.list("/reports")
            reports = list
        } catch (e: Exception) {
            reports = emptyList()
            errorMessage = e.message ?: "Không thể kết nối đến máy chủ"
        } finally {
            loadingReports = false
        }

        try {
            val apptList = api.list("/appointments")
            appointments = apptList
        } catch (_: Exception) {
            appointments = emptyList()
        } finally {
            loadingAppointments = false
        }

        try {
            metrics = api.get("/reports/triage-metrics")
        } catch (_: Exception) {
            metrics = null
        } finally {
            loadingMetrics = false
        }
    }

    val pendingReportsCount = remember(reports) {
        reports.count {
            val st = it.optString("state", it.optString("status", "submitted"))
            st in listOf("submitted", "triage_ready", "pending")
        }
    }

    val pendingApptsCount = remember(appointments) {
        appointments.count {
            val st = it.optString("status", "requested")
            st in listOf("requested", "pending")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Surface(
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Điều phối hiện trường AI & Duyệt tiếp dân",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OpenCityColors.BrandNavy
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = "DX-OS LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Hệ thống điều hành tác nghiệp thông minh dành cho cán bộ",
                            fontSize = 11.sp,
                            color = OpenCityColors.TextSecondary
                        )
                    }
                    IconButton(onClick = { refreshKey++ }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = OpenCityColors.BrandNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.White,
                    contentColor = OpenCityColors.BrandNavy,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = OpenCityColors.BrandNavy,
                            height = 3.dp
                        )
                    }
                ) {
                    DispatcherTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedTab == tab) OpenCityColors.BrandNavy else Color(0xFF64748B)
                                    )
                                    Text(
                                        text = tab.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == tab) OpenCityColors.BrandNavy else Color(0xFF64748B)
                                    )
                                    val badge = when (tab) {
                                        DispatcherTab.TRIAGE -> if (pendingReportsCount > 0) pendingReportsCount else null
                                        DispatcherTab.APPOINTMENTS -> if (pendingApptsCount > 0) pendingApptsCount else null
                                        else -> null
                                    }
                                    if (badge != null) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (tab == DispatcherTab.TRIAGE) Color(0xFFDC2626) else Color(0xFF2563EB)
                                        ) {
                                            Text(
                                                text = badge.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        actionMessage?.let { msg ->
            Surface(
                color = Color(0xFFECFDF5),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = msg, fontSize = 12.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = { actionMessage = null }, modifier = Modifier.size(20.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFF065F46), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        if (errorMessage != null && reports.isEmpty()) {
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Thông báo kết nối tác nghiệp", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF991B1B))
                    }
                    Text(
                        text = "Không thể tải danh sách điều phối: $errorMessage. Vui lòng đảm bảo bạn đang đăng nhập với tài khoản Cán bộ DX-OS (dxos-dispatcher).",
                        fontSize = 12.sp,
                        color = Color(0xFF7F1D1D)
                    )
                    OutlinedButton(
                        onClick = { refreshKey++ },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Thử kết nối lại", fontSize = 12.sp)
                    }
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                DispatcherTab.TRIAGE -> TriageTabContent(
                    api = api,
                    reports = reports,
                    loading = loadingReports,
                    onUpdateReports = { updated -> reports = updated },
                    onShowMessage = { msg -> actionMessage = msg },
                    onViewReportDetail = onViewReportDetail
                )
                DispatcherTab.APPOINTMENTS -> AppointmentsTabContent(
                    api = api,
                    appointments = appointments,
                    loading = loadingAppointments,
                    onUpdateAppointments = { updated -> appointments = updated },
                    onShowMessage = { msg -> actionMessage = msg }
                )
                DispatcherTab.METRICS -> MetricsTabContent(
                    metrics = metrics,
                    reports = reports,
                    loading = loadingMetrics
                )
            }
        }
    }
}

@Composable
private fun TriageTabContent(
    api: ApiClient,
    reports: List<JSONObject>,
    loading: Boolean,
    onUpdateReports: (List<JSONObject>) -> Unit,
    onShowMessage: (String) -> Unit,
    onViewReportDetail: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var filterStatus by remember { mutableStateOf("pending") } // pending, assigned, resolved, all

    var assigningReport by remember { mutableStateOf<JSONObject?>(null) }
    var selectedUnitId by remember { mutableStateOf("infrastructure") }
    var assignmentUrgency by remember { mutableStateOf("normal") }
    var assignmentNote by remember { mutableStateOf("") }

    val filteredList = remember(reports, filterStatus) {
        reports.filter { item ->
            val st = item.optString("state", item.optString("status", "submitted"))
            when (filterStatus) {
                "pending" -> st in listOf("submitted", "triage_ready", "pending")
                "assigned" -> st in listOf("assigned", "in_progress", "processing")
                "resolved" -> st in listOf("resolved", "completed", "closed")
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pendingCount = reports.count {
                it.optString("state", it.optString("status")) in listOf("submitted", "triage_ready", "pending")
            }
            val assignedCount = reports.count {
                it.optString("state", it.optString("status")) in listOf("assigned", "in_progress", "processing")
            }
            val resolvedCount = reports.count {
                it.optString("state", it.optString("status")) in listOf("resolved", "completed", "closed")
            }

            DispatcherCounterCard(
                title = "Chờ phân luồng",
                count = pendingCount,
                color = Color(0xFFDC2626),
                bg = Color(0xFFFEF2F2),
                modifier = Modifier.weight(1f)
            )
            DispatcherCounterCard(
                title = "Đang xử lý",
                count = assignedCount,
                color = Color(0xFF2563EB),
                bg = Color(0xFFEFF6FF),
                modifier = Modifier.weight(1f)
            )
            DispatcherCounterCard(
                title = "Đã hoàn thành",
                count = resolvedCount,
                color = Color(0xFF16A34A),
                bg = Color(0xFFF0FDF4),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterStatus == "pending",
                onClick = { filterStatus = "pending" },
                label = { Text("Chờ phân luồng (${reports.count { it.optString("state", it.optString("status")) in listOf("submitted", "triage_ready", "pending") }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OpenCityColors.BrandNavy,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = filterStatus == "assigned",
                onClick = { filterStatus = "assigned" },
                label = { Text("Đang thụ lý (${reports.count { it.optString("state", it.optString("status")) in listOf("assigned", "in_progress") }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OpenCityColors.BrandNavy,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = filterStatus == "resolved",
                onClick = { filterStatus = "resolved" },
                label = { Text("Đã xử lý xong (${reports.count { it.optString("state", it.optString("status")) in listOf("resolved", "completed") }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OpenCityColors.BrandNavy,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = filterStatus == "all",
                onClick = { filterStatus = "all" },
                label = { Text("Tất cả (${reports.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = OpenCityColors.BrandNavy,
                    selectedLabelColor = Color.White
                )
            )
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OpenCityColors.BrandNavy)
            }
        } else if (filteredList.isEmpty()) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Không có hồ sơ nào trong trạng thái này",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = OpenCityColors.TextMain
                    )
                    Text(
                        text = "Toàn bộ phản ánh đã được phân luồng xử lý kịp thời theo quy trình DX-OS.",
                        fontSize = 12.sp,
                        color = OpenCityColors.TextSecondary
                    )
                }
            }
        } else {
            filteredList.forEach { reportItem ->
                ReportTriageCard(
                    report = reportItem,
                    onAssign = {
                        assigningReport = reportItem
                        selectedUnitId = reportItem.optString("suggested_unit").takeIf { it.isNotBlank() } ?: "infrastructure"
                        assignmentUrgency = reportItem.optString("urgency").takeIf { it.isNotBlank() } ?: "normal"
                        assignmentNote = "Kính chuyển đơn vị thụ lý kiểm tra hiện trường theo thẩm quyền."
                    },
                    onAccept = {
                        scope.launch {
                            val repId = reportItem.optString("id")
                            try {
                                api.post("/reports/$repId/accept", JSONObject())
                                onShowMessage("Đã tiếp nhận hồ sơ #${repId.take(8)} thành công!")
                                onUpdateReports(reports.map {
                                    if (it.optString("id") == repId) {
                                        JSONObject(it.toString()).apply {
                                            put("state", "assigned")
                                            put("status", "assigned")
                                        }
                                    } else it
                                })
                            } catch (e: Exception) {
                                onShowMessage("Lỗi khi tiếp nhận: ${e.message}")
                            }
                        }
                    },
                    onViewDetail = { onViewReportDetail(reportItem.optString("id")) }
                )
            }
        }
    }

    if (assigningReport != null) {
        val rep = assigningReport!!
        val repId = rep.optString("id")
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) assigningReport = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = OpenCityColors.BrandNavy)
                    Spacer(Modifier.width(8.dp))
                    Text("Phân công đơn vị xử lý", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = rep.optString("title", "Hồ sơ phản ánh"),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = OpenCityColors.TextMain
                    )
                    Text(
                        text = "Mã hồ sơ: #${repId.take(8)}",
                        fontSize = 11.sp,
                        color = OpenCityColors.TextSecondary
                    )

                    HorizontalDivider(color = OpenCityColors.BorderColor)

                    Text(
                        text = "Chọn Đơn vị chuyên trách:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = OpenCityColors.BrandNavy
                    )

                    MUNICIPAL_UNITS.forEach { unit ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedUnitId == unit.id) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                if (selectedUnitId == unit.id) 2.dp else 1.dp,
                                if (selectedUnitId == unit.id) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedUnitId = unit.id }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(unit.iconColor)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = unit.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedUnitId == unit.id) FontWeight.Bold else FontWeight.Normal,
                                    color = OpenCityColors.TextMain
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Mức độ ưu tiên:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("urgent" to "Khẩn cấp", "high" to "Ưu tiên cao", "normal" to "Bình thường").forEach { (key, label) ->
                            FilterChip(
                                selected = assignmentUrgency == key,
                                onClick = { assignmentUrgency = key },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = assignmentNote,
                        onValueChange = { assignmentNote = it },
                        label = { Text("Ghi chú chỉ đạo / Yêu cầu thời hạn") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            try {
                                val dueAt = Instant.now().plus(Duration.ofDays(3)).toString()
                                val body = JSONObject().apply {
                                    put("unit_id", selectedUnitId)
                                    put("due_at", dueAt)
                                    put("note", assignmentNote.ifBlank { "Kính chuyển đơn vị thụ lý kiểm tra hiện trường theo thẩm quyền." })
                                    val sugId = rep.optString("suggestion_id").takeIf { it.length == 36 }
                                    if (sugId != null) {
                                        put("suggestion_id", sugId)
                                    }
                                }
                                api.post("/reports/$repId/dispatch", body)
                                val unitName = MUNICIPAL_UNITS.firstOrNull { it.id == selectedUnitId }?.name ?: selectedUnitId
                                onShowMessage("Đã phân công hồ sơ #${repId.take(8)} cho $unitName!")
                                onUpdateReports(reports.map {
                                    if (it.optString("id") == repId) {
                                        JSONObject(it.toString()).apply {
                                            put("state", "assigned")
                                            put("status", "assigned")
                                            put("assigned_unit", selectedUnitId)
                                            put("urgency", assignmentUrgency)
                                        }
                                    } else it
                                })
                            } catch (e: Exception) {
                                onShowMessage("Lỗi điều phối: ${e.message}")
                            } finally {
                                isSubmitting = false
                                assigningReport = null
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandNavy)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Xác nhận Phân công")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { assigningReport = null }, enabled = !isSubmitting) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun ReportTriageCard(
    report: JSONObject,
    onAssign: () -> Unit,
    onAccept: () -> Unit,
    onViewDetail: () -> Unit
) {
    val title = report.optString("title", "Phản ánh hiện trường")
    val desc = report.optString("description", "")
    val address = report.optString("address", report.optString("location", "Hà Nội"))
    val status = report.optString("state", report.optString("status", "submitted"))
    val urgency = report.optString("urgency", "normal")
    val id = report.optString("id", "")
    val category = report.optString("category", "")
    val suggestedUnit = report.optString("suggested_unit").takeIf { it.isNotBlank() }
    val assignedUnit = report.optString("assigned_unit").takeIf { it.isNotBlank() }
    val topSimilarity = report.optDouble("top_similarity", 0.0)
    val hasDuplicate = report.optBoolean("has_duplicate", false)

    val statusBadgeColor = when (status) {
        "submitted", "triage_ready", "pending" -> Color(0xFFDC2626)
        "assigned", "in_progress", "processing" -> Color(0xFF2563EB)
        "resolved", "completed", "closed" -> Color(0xFF16A34A)
        else -> Color(0xFF6B7280)
    }

    val statusBadgeText = when (status) {
        "submitted", "triage_ready", "pending" -> "Chờ phân luồng"
        "assigned", "in_progress", "processing" -> "Đang xử lý"
        "resolved", "completed", "closed" -> "Đã giải quyết"
        else -> status
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusBadgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusBadgeText.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusBadgeColor
                        )
                    }

                    if (category.isNotBlank()) {
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = category,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                if (urgency in listOf("urgent", "high")) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = if (urgency == "urgent") "KHẨN CẤP" else "ƯU TIÊN CAO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextMain
            )

            if (desc.isNotBlank()) {
                Text(
                    text = desc,
                    fontSize = 12.sp,
                    color = OpenCityColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (address.isNotBlank()) {
                Text(
                    text = "📍 $address",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            if (suggestedUnit != null) {
                val unitObj = MUNICIPAL_UNITS.firstOrNull { it.id == suggestedUnit }
                Surface(
                    color = Color(0xFFF5F3FF),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "AI GỢI Ý PHÂN LUỒNG",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF7C3AED)
                                )
                            }
                            if (topSimilarity > 0) {
                                Text(
                                    text = "Độ tương đồng: ${(topSimilarity * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6D28D9)
                                )
                            }
                        }
                        Text(
                            text = "Đơn vị đề xuất: ${unitObj?.name ?: suggestedUnit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4C1D95)
                        )
                        if (hasDuplicate) {
                            Text(
                                text = "⚠️ Cảnh báo: AI phát hiện nội dung có thể trùng với phản ánh khác gần khu vực.",
                                fontSize = 10.sp,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            } else if (assignedUnit != null) {
                val unitObj = MUNICIPAL_UNITS.firstOrNull { it.id == assignedUnit }
                Surface(
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Engineering, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Đơn vị đang thụ lý: ${unitObj?.name ?: assignedUnit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (status in listOf("submitted", "triage_ready", "pending")) {
                    Button(
                        onClick = onAssign,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Phân công", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (status in listOf("assigned", "in_progress")) {
                    OutlinedButton(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Tiếp nhận", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onViewDetail,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenCityColors.BrandNavy)
                ) {
                    Text("Chi tiết", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun AppointmentsTabContent(
    api: ApiClient,
    appointments: List<JSONObject>,
    loading: Boolean,
    onUpdateAppointments: (List<JSONObject>) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var decidingAppointment by remember { mutableStateOf<JSONObject?>(null) }
    var decisionType by remember { mutableStateOf("confirmed") }
    var decisionNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Yêu cầu Đăng ký Tiếp Dân Trực tuyến",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = OpenCityColors.TextMain
        )
        Text(
            text = "Duyệt yêu cầu gặp gỡ, hướng dẫn thủ tục hành chính và đối thoại trực tiếp theo quy trình DX-OS.",
            fontSize = 12.sp,
            color = OpenCityColors.TextSecondary
        )

        if (loading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OpenCityColors.BrandNavy)
            }
        } else if (appointments.isEmpty()) {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = OpenCityColors.BrandNavy,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Hiện chưa có lịch hẹn tiếp dân nào",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = OpenCityColors.TextMain
                    )
                    Text(
                        text = "Khi công dân đăng ký lịch hẹn trực tuyến qua ứng dụng OpenCity, hồ sơ sẽ hiển thị tại đây để cán bộ xem xét phê duyệt.",
                        fontSize = 12.sp,
                        color = OpenCityColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            appointments.forEach { appt ->
                AppointmentApprovalCard(
                    appointment = appt,
                    onApprove = {
                        decidingAppointment = appt
                        decisionType = "confirmed"
                        decisionNote = "Chấp thuận lịch hẹn tại Phòng tiếp dân - Tầng 1 UBND Quận."
                    },
                    onReject = {
                        decidingAppointment = appt
                        decisionType = "rejected"
                        decisionNote = "Trùng lịch họp đột xuất của lãnh đạo. Đề nghị công dân chọn khung giờ khác."
                    }
                )
            }
        }
    }

    if (decidingAppointment != null) {
        val appt = decidingAppointment!!
        val apptId = appt.optString("id")
        val isConfirm = decisionType == "confirmed"
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) decidingAppointment = null },
            title = {
                Text(
                    text = if (isConfirm) "Phê duyệt Lịch hẹn" else "Từ chối Lịch hẹn",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isConfirm) Color(0xFF16A34A) else Color(0xFFDC2626)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Nội dung: ${appt.optString("reason", "")}",
                        fontSize = 12.sp,
                        color = OpenCityColors.TextMain
                    )
                    OutlinedTextField(
                        value = decisionNote,
                        onValueChange = { decisionNote = it },
                        label = { Text(if (isConfirm) "Địa điểm / Phòng tiếp dân / Ghi chú" else "Lý do từ chối") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            try {
                                val body = JSONObject().apply {
                                    put("decision", decisionType)
                                    put("note", decisionNote)
                                }
                                api.post("/appointments/$apptId/decision", body)
                                onShowMessage(if (isConfirm) "Đã duyệt lịch hẹn thành công!" else "Đã từ chối lịch hẹn.")
                                onUpdateAppointments(appointments.map {
                                    if (it.optString("id") == apptId) {
                                        JSONObject(it.toString()).apply {
                                            put("status", decisionType)
                                            put("decision_note", decisionNote)
                                        }
                                    } else it
                                })
                            } catch (e: Exception) {
                                onShowMessage("Lỗi xử lý lịch hẹn: ${e.message}")
                            } finally {
                                isSubmitting = false
                                decidingAppointment = null
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConfirm) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (isConfirm) "Xác nhận Duyệt" else "Xác nhận Từ chối")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { decidingAppointment = null }, enabled = !isSubmitting) {
                    Text("Đóng")
                }
            }
        )
    }
}

@Composable
private fun AppointmentApprovalCard(
    appointment: JSONObject,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val id = appointment.optString("id")
    val citizenName = appointment.optString("citizen_name", "Công dân Hà Nội")
    val reason = appointment.optString("reason", "Trao đổi hướng dẫn thủ tục hành chính")
    val status = appointment.optString("status", "requested")

    val (badgeText, badgeColor) = when (status) {
        "confirmed" -> "ĐÃ PHÊ DUYỆT" to Color(0xFF16A34A)
        "rejected" -> "ĐÃ TỪ CHỐI" to Color(0xFFDC2626)
        else -> "CHỜ DUYỆT" to Color(0xFFD97706)
    }

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                Text(
                    text = "Mã: #${id.take(8)}",
                    fontSize = 11.sp,
                    color = OpenCityColors.TextSecondary
                )
            }

            Text(
                text = "👤 $citizenName",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextMain
            )

            Text(
                text = "Nội dung: $reason",
                fontSize = 12.sp,
                color = OpenCityColors.TextSecondary
            )

            if (status == "requested" || status == "pending") {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Phê duyệt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Từ chối", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricsTabContent(
    metrics: JSONObject?,
    reports: List<JSONObject>,
    loading: Boolean
) {
    val totalGenerated = metrics?.optInt("generated", 0) ?: 0
    val totalReviewed = metrics?.optInt("reviewed", 0) ?: 0
    val totalAccepted = metrics?.optInt("accepted", 0) ?: 0
    val totalOverridden = metrics?.optInt("overridden", 0) ?: 0

    val accuracyRate = if (totalReviewed > 0) {
        "${(totalAccepted * 100) / totalReviewed}%"
    } else {
        "100%"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Hiệu suất Phân luồng & Tác nghiệp Cán bộ",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = OpenCityColors.TextMain
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiMetricBox(
                title = "Tỷ lệ chuẩn xác AI",
                value = accuracyRate,
                subtitle = "Khớp phân công của cán bộ ($totalAccepted/$totalReviewed)",
                color = Color(0xFF7C3AED),
                modifier = Modifier.weight(1f)
            )
            KpiMetricBox(
                title = "Hồ sơ AI gợi ý",
                value = "$totalGenerated",
                subtitle = "Tự động phân luồng",
                color = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiMetricBox(
                title = "Đã rà soát",
                value = "$totalReviewed",
                subtitle = "Hồ sơ qua cán bộ duyệt",
                color = Color(0xFF2563EB),
                modifier = Modifier.weight(1f)
            )
            KpiMetricBox(
                title = "Tổng số hồ sơ",
                value = "${reports.size}",
                subtitle = "Hiện diện trong hệ thống",
                color = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = "Phân bổ theo Đơn vị chuyên trách",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = OpenCityColors.TextMain
        )

        val totalReports = reports.size.coerceAtLeast(1)

        MUNICIPAL_UNITS.forEach { unit ->
            val count = reports.count {
                it.optString("assigned_unit") == unit.id ||
                (it.optString("assigned_unit").isBlank() && it.optString("unit_id") == unit.id) ||
                (it.optString("assigned_unit").isBlank() && it.optString("suggested_unit") == unit.id)
            }
            val percent = (count * 100) / totalReports

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(unit.iconColor))
                            Spacer(Modifier.width(8.dp))
                            Text(text = unit.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                        }
                        Text(text = "$count ($percent%)", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = unit.iconColor)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percent / 100f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(unit.iconColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMetricBox(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = OpenCityColors.TextSecondary)
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
private fun DispatcherCounterCard(
    title: String,
    count: Int,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = count.toString(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
