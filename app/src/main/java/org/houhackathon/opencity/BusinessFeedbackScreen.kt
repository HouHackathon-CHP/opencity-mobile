// SPDX-License-Identifier: MIT
package org.houhackathon.opencity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

private val sectorNames = linkedMapOf(
    "tax" to "Thuế & Tài chính",
    "land" to "Mặt bằng & Đất đai",
    "investment" to "Thủ tục Đầu tư",
    "labor" to "Lao động & BHXH",
    "other" to "Khó khăn khác"
)

private val sectorDescriptions = mapOf(
    "tax" to "Vướng mắc về chính sách thuế, thủ tục hóa đơn điện tử, hoàn thuế và các gói ưu đãi tài chính.",
    "land" to "Kiến nghị về cấp giấy phép xây dựng, thuê mặt bằng sản xuất kinh doanh, giải phóng mặt bằng.",
    "investment" to "Hồ sơ đăng ký chủ trương đầu tư, cấp phép dự án mới, thẩm định công nghệ và liên thông thủ tục.",
    "labor" to "Thủ tục cấp phép lao động nước ngoài, đóng bảo hiểm xã hội, an toàn vệ sinh lao động.",
    "other" to "Các vấn đề phối hợp liên ngành hoặc thủ tục hành chính chuyên ngành khác."
)

private fun petitionStatusLabel(status: String) = when (status) {
    "submitted" -> "Mới tiếp nhận"
    "in_review" -> "Đang thẩm định & xử lý"
    "needs_info", "info_requested" -> "Yêu cầu bổ sung hồ sơ"
    "info_provided" -> "Đã bổ sung thông tin"
    "resolved" -> "Đã tháo gỡ & giải quyết"
    "reopened" -> "Đang xem xét lại"
    "closed" -> "Đã đóng kết thúc"
    else -> status
}

private fun petitionStatusColors(status: String): Pair<Color, Color> = when (status) {
    "submitted" -> Pair(OpenCityColors.BrandAzureLight, OpenCityColors.BrandAzureDark)
    "in_review" -> Pair(OpenCityColors.BrandAmberLight, OpenCityColors.BrandAmberDark)
    "needs_info", "info_requested" -> Pair(Color(0xFFFFEDD5), Color(0xFFC2410C))
    "info_provided" -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
    "resolved" -> Pair(OpenCityColors.BrandMintLight, OpenCityColors.BrandMintDark)
    "reopened" -> Pair(Color(0xFFFFE4E6), Color(0xFFBE123C))
    "closed" -> Pair(Color(0xFFF1F5F9), Color(0xFF64748B))
    else -> Pair(OpenCityColors.SurfaceBg, OpenCityColors.TextSecondary)
}

private fun petitionActionLabel(action: String) = when (action) {
    "submitted" -> "Doanh nghiệp đã nộp hồ sơ kiến nghị"
    "review" -> "Cơ quan chuyên môn đã tiếp nhận và đang thẩm định"
    "request_info" -> "Cơ quan thẩm quyền yêu cầu bổ sung tài liệu giải trình"
    "provide_info" -> "Doanh nghiệp đã nộp tài liệu bổ sung"
    "resolve" -> "Cơ quan thẩm quyền đã ban hành kết luận giải quyết"
    "reopen" -> "Doanh nghiệp yêu cầu xem xét lại hồ sơ"
    "close" -> "Hồ sơ kiến nghị đã đóng kết thúc"
    else -> action
}

@Composable
fun BusinessFeedbackScreen(
    api: ApiClient,
    revision: Int,
    signedIn: Boolean,
    onLogin: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var petitions by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedPetitionId by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Form states
    var companyName by remember { mutableStateOf("") }
    var taxCode by remember { mutableStateOf("") }
    var representative by remember { mutableStateOf("") }
    var selectedSector by remember { mutableStateOf("tax") }
    var petitionTitle by remember { mutableStateOf("") }
    var petitionDescription by remember { mutableStateOf("") }
    var requestedResolution by remember { mutableStateOf("") }

    var supplementalNote by remember { mutableStateOf("") }
    var showSupplementalDialog by remember { mutableStateOf(false) }
    var reopenReason by remember { mutableStateOf("") }
    var showReopenDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Fetch petitions
    LaunchedEffect(signedIn, revision, refreshTrigger) {
        if (signedIn) {
            loading = true
            runCatching { api.list("/business-feedback") }
                .onSuccess { list ->
                    petitions = list
                    errorMessage = null
                    if (selectedPetitionId != null && list.none { it.text("id") == selectedPetitionId }) {
                        selectedPetitionId = list.firstOrNull()?.text("id")
                    }
                }
                .onFailure {
                    errorMessage = it.message ?: "Không thể tải danh sách kiến nghị doanh nghiệp."
                }
            loading = false
        } else {
            petitions = emptyList()
            selectedPetitionId = null
        }
    }

    val selectedPetition = petitions.firstOrNull { it.text("id") == selectedPetitionId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
    ) {
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .clickable(onClick = {
                                if (selectedPetition != null) {
                                    selectedPetitionId = null
                                } else {
                                    onBack()
                                }
                            })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = OpenCityColors.BrandAzure,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (selectedPetition != null) "Danh sách hồ sơ" else "Trang chủ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.BrandAzure
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (signedIn) {
                            IconButton(
                                onClick = { refreshTrigger++ },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Làm mới",
                                    tint = OpenCityColors.BrandNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OpenCityColors.BrandAzureLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BusinessCenter,
                            contentDescription = null,
                            tint = OpenCityColors.BrandAzure,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Doanh nghiệp Kiến nghị",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OpenCityColors.TextMain
                        )
                        Text(
                            text = "Tháo gỡ khó khăn về thuế, mặt bằng, đầu tư & thủ tục",
                            fontSize = 12.sp,
                            color = OpenCityColors.TextSecondary
                        )
                    }
                }

                if (selectedPetition == null) {
                    Spacer(Modifier.height(10.dp))
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = OpenCityColors.BrandAzure,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = OpenCityColors.BrandAzure
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = if (signedIn) "Hồ sơ của tôi (${petitions.size})" else "Hồ sơ của tôi",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = "Nộp kiến nghị mới",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Text(
                                    text = "Quy trình",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            errorMessage?.let { msg ->
                Surface(
                    color = OpenCityColors.BrandRedLight,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BrandRedBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            color = OpenCityColors.BrandRedDark,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = OpenCityColors.BrandRedDark, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            successNotice?.let { msg ->
                Surface(
                    color = OpenCityColors.BrandMintLight,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BrandMintDark.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OpenCityColors.BrandMintDark, modifier = Modifier.size(18.dp))
                            Text(text = msg, color = OpenCityColors.BrandMintDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { successNotice = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = OpenCityColors.BrandMintDark, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (!signedIn) {
                Surface(
                    color = OpenCityColors.BrandAmberLight,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BrandAmberDark.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = OpenCityColors.BrandAmberDark, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Chế độ Khách tra cứu",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = OpenCityColors.BrandAmberDark
                            )
                        }
                        Text(
                            text = "Để nộp hồ sơ tháo gỡ khó khăn và theo dõi tiến trình xử lý của cơ quan chức năng, quý doanh nghiệp vui lòng đăng nhập tài khoản định danh.",
                            fontSize = 12.sp,
                            color = OpenCityColors.BrandAmberDark,
                            lineHeight = 17.sp
                        )
                        Button(
                            onClick = onLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đăng nhập tài khoản Doanh nghiệp / Công dân", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (selectedPetition != null) {
                val status = selectedPetition.text("status")
                val (statusBg, statusFg) = petitionStatusColors(status)
                val sector = selectedPetition.text("sector")
                val sectorName = sectorNames[sector] ?: sector
                val history = selectedPetition.optJSONArray("history")

                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = statusBg,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = petitionStatusLabel(status),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusFg,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                color = OpenCityColors.SurfaceBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Lĩnh vực: $sectorName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OpenCityColors.TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = selectedPetition.text("title"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMain
                        )

                        HorizontalDivider(color = OpenCityColors.BorderColor)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• Doanh nghiệp: ${selectedPetition.text("company_name")}", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OpenCityColors.TextMain)
                            Text("• Mã số thuế: ${selectedPetition.text("tax_code")}", fontSize = 12.sp, color = OpenCityColors.TextSecondary)
                            Text("• Người đại diện: ${selectedPetition.text("representative")}", fontSize = 12.sp, color = OpenCityColors.TextSecondary)
                            Text("• Thời gian nộp: ${selectedPetition.text("created_at").replace("T", " ").take(16)}", fontSize = 12.sp, color = OpenCityColors.TextLight)
                        }

                        HorizontalDivider(color = OpenCityColors.BorderColor)

                        Text("Nội dung vướng mắc, khó khăn:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.BrandNavy)
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedPetition.text("description"),
                                fontSize = 13.sp,
                                color = OpenCityColors.TextMain,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Text("Đề xuất hướng tháo gỡ / giải quyết:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.BrandNavy)
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedPetition.text("requested_resolution"),
                                fontSize = 13.sp,
                                color = Color(0xFF166534),
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        val officialResponse = selectedPetition.text("response")
                        if (officialResponse.isNotBlank()) {
                            Text("Kết luận tháo gỡ của cơ quan thẩm quyền:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                                        Text("Văn bản kết luận / Hướng giải quyết chính thức", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                    }
                                    Text(
                                        text = officialResponse,
                                        fontSize = 13.sp,
                                        color = Color(0xFF14532D),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        if (status in setOf("info_requested", "needs_info")) {
                            Surface(
                                color = Color(0xFFFFF7ED),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "⚠️ Cơ quan chuyên môn đang yêu cầu bổ sung thông tin.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC2410C)
                                    )
                                    Button(
                                        onClick = { showSupplementalDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("+ Bổ sung thông tin giải trình", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (status in setOf("resolved", "closed")) {
                            OutlinedButton(
                                onClick = { showReopenDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, OpenCityColors.BrandAzure),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Đề nghị xem xét lại / Mở lại hồ sơ", color = OpenCityColors.BrandAzure, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = OpenCityColors.BrandNavy, modifier = Modifier.size(18.dp))
                            Text("Tiến trình thụ lý & giải quyết", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = OpenCityColors.BrandNavy)
                        }

                        if (history != null && history.length() > 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                for (i in 0 until history.length()) {
                                    val ev = history.optJSONObject(i) ?: continue
                                    val action = ev.text("action")
                                    val actor = ev.text("actor")
                                    val time = ev.text("created_at").replace("T", " ").take(16)
                                    val note = ev.text("note")

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(OpenCityColors.BrandAzure)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = petitionActionLabel(action),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OpenCityColors.TextMain
                                            )
                                            Text(
                                                text = "Thực hiện bởi: $actor · $time",
                                                fontSize = 11.sp,
                                                color = OpenCityColors.TextLight
                                            )
                                            if (note.isNotBlank()) {
                                                Surface(
                                                    color = Color(0xFFF1F5F9),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                ) {
                                                    Text(
                                                        text = note,
                                                        fontSize = 12.sp,
                                                        color = OpenCityColors.TextSecondary,
                                                        modifier = Modifier.padding(8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text("Chưa có sự kiện nào được ghi nhận.", fontSize = 12.sp, color = OpenCityColors.TextLight)
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        if (loading && petitions.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = OpenCityColors.BrandAzure)
                            }
                        } else if (petitions.isEmpty()) {
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = OpenCityColors.TextLight,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = "Chưa có hồ sơ kiến nghị nào",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = OpenCityColors.TextMain
                                    )
                                    Text(
                                        text = "Quý doanh nghiệp có thể nộp kiến nghị tháo gỡ khó khăn về các thủ tục hành chính, thuế, mặt bằng để được chính quyền hỗ trợ.",
                                        fontSize = 12.sp,
                                        color = OpenCityColors.TextSecondary,
                                        lineHeight = 17.sp
                                    )
                                    Button(
                                        onClick = {
                                            if (!signedIn) onLogin() else selectedTab = 1
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("+ Nộp hồ sơ kiến nghị mới", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Danh sách hồ sơ đã nộp (${petitions.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = OpenCityColors.TextMain
                                )
                                TextButton(onClick = { selectedTab = 1 }) {
                                    Text("+ Nộp hồ sơ mới", color = OpenCityColors.BrandAzure, fontWeight = FontWeight.Bold)
                                }
                            }

                            petitions.forEach { pet ->
                                val status = pet.text("status")
                                val (statusBg, statusFg) = petitionStatusColors(status)
                                val sector = pet.text("sector")
                                val sectorName = sectorNames[sector] ?: sector

                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPetitionId = pet.text("id")
                                        }
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
                                                color = statusBg,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = petitionStatusLabel(status),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusFg,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = pet.text("created_at").replace("T", " ").take(10),
                                                fontSize = 11.sp,
                                                color = OpenCityColors.TextLight
                                            )
                                        }

                                        Text(
                                            text = pet.text("title"),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OpenCityColors.TextMain,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = pet.text("description"),
                                            fontSize = 12.sp,
                                            color = OpenCityColors.TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        HorizontalDivider(color = OpenCityColors.BorderColor.copy(alpha = 0.5f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f).padding(end = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.BusinessCenter,
                                                    contentDescription = null,
                                                    tint = OpenCityColors.TextLight,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "${pet.text("company_name")} · MST: ${pet.text("tax_code")}",
                                                    fontSize = 11.sp,
                                                    color = OpenCityColors.TextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                color = OpenCityColors.BrandAzure.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "Xem tiến trình →",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = OpenCityColors.BrandAzure,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Thông tin Đăng ký Kiến nghị",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.BrandNavy
                                )
                                Text(
                                    text = "Vui lòng nhập đầy đủ thông tin pháp nhân và mô tả chi tiết vướng mắc thực tế để cơ quan chuyên môn liên hệ và xử lý.",
                                    fontSize = 12.sp,
                                    color = OpenCityColors.TextSecondary
                                )

                                OutlinedTextField(
                                    value = companyName,
                                    onValueChange = { companyName = it },
                                    label = { Text("Tên doanh nghiệp / Hộ kinh doanh *") },
                                    placeholder = { Text("Ví dụ: Công ty Cổ phần Công nghệ Mở Hà Nội") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OpenCityColors.BrandAzure
                                    )
                                )

                                OutlinedTextField(
                                    value = taxCode,
                                    onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 13) taxCode = it },
                                    label = { Text("Mã số thuế doanh nghiệp (10 hoặc 13 số) *") },
                                    placeholder = { Text("Ví dụ: 0102030405") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OpenCityColors.BrandAzure
                                    )
                                )

                                OutlinedTextField(
                                    value = representative,
                                    onValueChange = { representative = it },
                                    label = { Text("Người đại diện pháp luật / Cán bộ đầu mối *") },
                                    placeholder = { Text("Họ và tên người đại diện") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OpenCityColors.BrandAzure
                                    )
                                )

                                Text("Lĩnh vực vướng mắc, khó khăn *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    sectorNames.forEach { (key, label) ->
                                        FilterChip(
                                            selected = selectedSector == key,
                                            onClick = { selectedSector = key },
                                            label = { Text(label, fontSize = 12.sp, fontWeight = if (selectedSector == key) FontWeight.Bold else FontWeight.Normal) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = OpenCityColors.BrandAzureLight,
                                                selectedLabelColor = OpenCityColors.BrandAzureDark
                                            )
                                        )
                                    }
                                }

                                sectorDescriptions[selectedSector]?.let { desc ->
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "ℹ $desc",
                                            fontSize = 11.sp,
                                            color = OpenCityColors.TextSecondary,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = petitionTitle,
                                    onValueChange = { petitionTitle = it },
                                    label = { Text("Tiêu đề kiến nghị *") },
                                    placeholder = { Text("Tóm tắt ngắn gọn vấn đề cần tháo gỡ") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OpenCityColors.BrandAzure
                                    )
                                )

                                OutlinedTextField(
                                    value = petitionDescription,
                                    onValueChange = { petitionDescription = it },
                                    label = { Text("Mô tả chi tiết khó khăn, vướng mắc thực tế *") },
                                    placeholder = { Text("Nêu rõ địa điểm, văn bản liên quan, cơ quan đang phụ trách, mức độ ảnh hưởng đến hoạt động sản xuất kinh doanh...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4,
                                    maxLines = 8,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OpenCityColors.BrandAzure
                                    )
                                )
                                Text(
                                    text = "Đã nhập: ${petitionDescription.trim().length} ký tự (tối thiểu 20 ký tự)",
                                    fontSize = 11.sp,
                                    color = if (petitionDescription.trim().length >= 20) OpenCityColors.BrandMintDark else OpenCityColors.TextLight
                                )

                                OutlinedTextField(
                                    value = requestedResolution,
                                    onValueChange = { requestedResolution = it },
                                    label = { Text("Đề xuất hướng tháo gỡ / giải quyết kiến nghị *") },
                                    placeholder = { Text("Doanh nghiệp mong muốn cơ quan chức năng hỗ trợ những biện pháp, hướng dẫn hoặc giải pháp cụ thể nào...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 6,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = OpenCityColors.BrandAzure
                                    )
                                )
                                Text(
                                    text = "Đã nhập: ${requestedResolution.trim().length} ký tự (tối thiểu 10 ký tự)",
                                    fontSize = 11.sp,
                                    color = if (requestedResolution.trim().length >= 10) OpenCityColors.BrandMintDark else OpenCityColors.TextLight
                                )

                                val isTaxValid = taxCode.matches(Regex("""^\d{10}(\d{3})?$"""))
                                val isCompanyValid = companyName.trim().length >= 3
                                val isRepValid = representative.trim().length >= 3
                                val isTitleValid = petitionTitle.trim().length >= 5
                                val isDescValid = petitionDescription.trim().length >= 20
                                val isResValid = requestedResolution.trim().length >= 10
                                val canSubmit = isTaxValid && isCompanyValid && isRepValid && isTitleValid && isDescValid && isResValid

                                Button(
                                    onClick = {
                                        if (!signedIn) {
                                            onLogin()
                                            return@Button
                                        }
                                        scope.launch {
                                            busy = true
                                            errorMessage = null
                                            successNotice = null
                                            runCatching {
                                                api.createBusinessPetition(
                                                    companyName = companyName,
                                                    taxCode = taxCode,
                                                    representative = representative,
                                                    sector = selectedSector,
                                                    title = petitionTitle,
                                                    description = petitionDescription,
                                                    requestedResolution = requestedResolution
                                                )
                                            }.onSuccess { created ->
                                                companyName = ""
                                                taxCode = ""
                                                representative = ""
                                                petitionTitle = ""
                                                petitionDescription = ""
                                                requestedResolution = ""
                                                successNotice = "Hồ sơ kiến nghị đã được tiếp nhận thành công. Mã hồ sơ: ${created.text("id").take(8)}..."
                                                selectedPetitionId = created.text("id")
                                                refreshTrigger++
                                            }.onFailure { err ->
                                                errorMessage = err.message ?: "Không thể gửi hồ sơ kiến nghị."
                                            }
                                            busy = false
                                        }
                                    },
                                    enabled = !busy && (canSubmit || !signedIn),
                                    colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    if (busy) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Text(
                                                text = if (!signedIn) "Đăng nhập để gửi hồ sơ" else "Gửi Hồ sơ Kiến nghị Doanh nghiệp",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Quy trình Tiếp nhận & Tháo gỡ Khó khăn",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.BrandNavy
                                )
                                Text(
                                    text = "Cam kết giải quyết công khai, minh bạch theo Đề án Hỗ trợ Doanh nghiệp Đô thị 2026.",
                                    fontSize = 12.sp,
                                    color = OpenCityColors.TextSecondary
                                )

                                HorizontalDivider(color = OpenCityColors.BorderColor)

                                val steps = listOf(
                                    Triple("1", "Nộp hồ sơ trực tuyến", "Doanh nghiệp khai báo thông tin pháp nhân, mã số thuế và phản ánh cụ thể vướng mắc qua cổng OpenCity."),
                                    Triple("2", "Phân loại & Thẩm định", "Tổ công tác tháo gỡ khó khăn đô thị tiếp nhận hồ sơ trong 24 giờ và phân công đơn vị chủ trì."),
                                    Triple("3", "Đối thoại & Bổ sung", "Trường hợp cần làm rõ, cơ quan gửi yêu cầu trực tiếp qua ứng dụng để doanh nghiệp bổ sung giải trình."),
                                    Triple("4", "Ban hành kết luận & Giám sát", "Cơ quan chuyên môn ban hành văn bản giải quyết, doanh nghiệp theo dõi toàn bộ tiến trình xử lý.")
                                )

                                steps.forEach { (num, title, desc) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(OpenCityColors.BrandAzure),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(num, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = OpenCityColors.TextMain)
                                            Text(desc, fontSize = 12.sp, color = OpenCityColors.TextSecondary, lineHeight = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    if (showSupplementalDialog && selectedPetition != null) {
        AlertDialog(
            onDismissRequest = { showSupplementalDialog = false },
            title = {
                Text("Bổ sung thông tin giải trình", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Nhập nội dung giải trình hoặc tài liệu làm rõ theo yêu cầu của cơ quan thụ lý:",
                        fontSize = 12.sp,
                        color = OpenCityColors.TextSecondary
                    )
                    OutlinedTextField(
                        value = supplementalNote,
                        onValueChange = { supplementalNote = it },
                        placeholder = { Text("Nội dung giải trình bổ sung...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )
                    Text(
                        "Tối thiểu 10 ký tự (Đã nhập: ${supplementalNote.trim().length})",
                        fontSize = 11.sp,
                        color = if (supplementalNote.trim().length >= 10) OpenCityColors.BrandMintDark else OpenCityColors.TextLight
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val petitionId = selectedPetition.text("id")
                        scope.launch {
                            busy = true
                            runCatching {
                                api.provideBusinessPetitionInfo(petitionId, supplementalNote)
                            }.onSuccess {
                                showSupplementalDialog = false
                                supplementalNote = ""
                                successNotice = "Đã gửi thông tin giải trình bổ sung thành công."
                                refreshTrigger++
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Không gửi được thông tin bổ sung."
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && supplementalNote.trim().length >= 10,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C))
                ) {
                    Text("Gửi bổ sung", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupplementalDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showReopenDialog && selectedPetition != null) {
        AlertDialog(
            onDismissRequest = { showReopenDialog = false },
            title = {
                Text("Đề nghị mở lại / Xem xét lại", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Vui lòng nêu rõ lý do hoặc các điểm chưa được tháo gỡ thỏa đáng:",
                        fontSize = 12.sp,
                        color = OpenCityColors.TextSecondary
                    )
                    OutlinedTextField(
                        value = reopenReason,
                        onValueChange = { reopenReason = it },
                        placeholder = { Text("Lý do yêu cầu xem xét lại...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    Text(
                        "Tối thiểu 10 ký tự (Đã nhập: ${reopenReason.trim().length})",
                        fontSize = 11.sp,
                        color = if (reopenReason.trim().length >= 10) OpenCityColors.BrandMintDark else OpenCityColors.TextLight
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val petitionId = selectedPetition.text("id")
                        scope.launch {
                            busy = true
                            runCatching {
                                api.reopenBusinessPetition(petitionId, reopenReason)
                            }.onSuccess {
                                showReopenDialog = false
                                reopenReason = ""
                                successNotice = "Đã gửi yêu cầu mở lại hồ sơ kiến nghị."
                                refreshTrigger++
                            }.onFailure { err ->
                                errorMessage = err.message ?: "Không thể mở lại hồ sơ kiến nghị."
                            }
                            busy = false
                        }
                    },
                    enabled = !busy && reopenReason.trim().length >= 10,
                    colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure)
                ) {
                    Text("Xác nhận mở lại", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReopenDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
