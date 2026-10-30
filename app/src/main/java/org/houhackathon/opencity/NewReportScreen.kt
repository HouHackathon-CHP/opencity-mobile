package org.houhackathon.opencity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import kotlin.coroutines.resume

private val categories = listOf("Hạ tầng", "Môi trường", "Giao thông", "Chiếu sáng", "Cây xanh", "Khác")
private const val DRAFT = "report-draft"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewReportScreen(
    api: ApiClient,
    context: Context,
    onCreated: (String) -> Unit
) {
    val prefs = remember { context.getSharedPreferences("drafts", Context.MODE_PRIVATE) }
    val saved = remember {
        runCatching { JSONObject(prefs.getString(DRAFT, "{}") ?: "{}") }.getOrDefault(JSONObject())
    }

    var title by remember { mutableStateOf(saved.optString("title")) }
    var description by remember { mutableStateOf(saved.optString("description")) }
    var address by remember { mutableStateOf(saved.optString("address")) }
    var category by remember { mutableStateOf(saved.optString("category", categories.first())) }
    var latitude by remember { mutableStateOf(saved.number("latitude")) }
    var longitude by remember { mutableStateOf(saved.number("longitude")) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSuccessMessage by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { photo = it }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) photo = cameraUri
    }

    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) {
            locating = true
            scope.launch {
                val point = withTimeoutOrNull(10_000) { currentLocation(context) }
                locating = false
                if (point == null) {
                    message = "Không lấy được GPS tự động. Bạn vẫn có thể nhập địa chỉ bằng tay."
                    isSuccessMessage = false
                } else {
                    latitude = point.first
                    longitude = point.second
                    message = "Đã lấy tọa độ GPS hiện trường chính xác."
                    isSuccessMessage = true
                }
            }
        } else {
            message = "Ứng dụng cần được cấp quyền vị trí để tự động gắn tọa độ GPS."
            isSuccessMessage = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Form Intro Card
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(OpenCityColors.BrandAzureLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = OpenCityColors.BrandAzure,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "Gửi phản ánh hiện trường",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpenCityColors.TextMain
                    )
                    Text(
                        text = "Thông tin sẽ được chuyển tiếp trực tiếp đến cơ quan tiếp nhận chuyên trách.",
                        fontSize = 12.sp,
                        color = OpenCityColors.TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Feedback / Error Banner
        message?.let { msg ->
            Surface(
                color = if (isSuccessMessage) OpenCityColors.BrandMintLight else OpenCityColors.BrandRedLight,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSuccessMessage) OpenCityColors.BrandMintBorder else OpenCityColors.BrandRedBorder),
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isSuccessMessage) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isSuccessMessage) OpenCityColors.BrandMintDark else OpenCityColors.BrandRedDark,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = msg,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSuccessMessage) OpenCityColors.BrandMintDark else OpenCityColors.BrandRedDark
                    )
                    IconButton(onClick = { message = null }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng", tint = OpenCityColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Section 1: Thông tin phân loại & Tiêu đề
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Lĩnh vực phản ánh *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextMain
                )

                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { name ->
                        val isSelected = category == name
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = name },
                            label = {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = OpenCityColors.BrandAzure,
                                selectedLabelColor = Color.White,
                                containerColor = OpenCityColors.SurfaceBg,
                                labelColor = OpenCityColors.TextMain
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.BorderColor
                            )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Tiêu đề phản ánh *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextMain
                )

                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Ví dụ: Hố ga mất nắp trước số 45 Tràng Tiền", fontSize = 13.sp, color = OpenCityColors.TextLight) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OpenCityColors.BrandAzure,
                        unfocusedBorderColor = OpenCityColors.BorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    supportingText = {
                        Text(
                            text = "${title.trim().length}/5 ký tự tối thiểu",
                            fontSize = 11.sp,
                            color = if (title.trim().length >= 5) OpenCityColors.BrandMintDark else OpenCityColors.TextLight
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Section 2: Chi tiết sự việc
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Mô tả chi tiết hiện trường *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextMain
                )

                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            "Mô tả cụ thể mức độ hư hại, nguy cơ an toàn hoặc yêu cầu xử lý...",
                            fontSize = 13.sp,
                            color = OpenCityColors.TextLight
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OpenCityColors.BrandAzure,
                        unfocusedBorderColor = OpenCityColors.BorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    supportingText = {
                        Text(
                            text = "${description.trim().length}/15 ký tự tối thiểu",
                            fontSize = 11.sp,
                            color = if (description.trim().length >= 15) OpenCityColors.BrandMintDark else OpenCityColors.TextLight
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Section 3: Vị trí & GPS
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3. Địa điểm xảy ra sự việc *",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextMain
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("Số nhà, ngõ/ngách, tên đường, phường/xã...", fontSize = 13.sp, color = OpenCityColors.TextLight) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = OpenCityColors.BrandAzure)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OpenCityColors.BrandAzure,
                        unfocusedBorderColor = OpenCityColors.BorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(Modifier.height(12.dp))

                // GPS Coordinate status / action
                if (latitude != null && longitude != null) {
                    Surface(
                        color = OpenCityColors.BrandMintLight,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OpenCityColors.BrandMintBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = OpenCityColors.BrandMintDark, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "GPS: %.5f, %.5f".format(latitude, longitude),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.BrandMintDark
                                )
                            }
                            TextButton(
                                onClick = {
                                    askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                }
                            ) {
                                Text("Lấy lại", fontSize = 11.sp, color = OpenCityColors.BrandMintDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        },
                        enabled = !locating,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = OpenCityColors.BrandAzure)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (locating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = OpenCityColors.BrandAzure)
                                Text("Đang định vị vệ tinh GPS...", fontSize = 13.sp)
                            } else {
                                Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Lấy tọa độ GPS tự động", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Section 4: Ảnh minh chứng hiện trường
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "4. Hình ảnh minh chứng hiện trường",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextMain
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { pickPhoto.launch("image/*") },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpenCityColors.BorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = OpenCityColors.BrandAzure, modifier = Modifier.size(16.dp))
                            Text("Thư viện", fontSize = 13.sp, color = OpenCityColors.TextMain, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val file = File(context.cacheDir, "photos/capture-${System.currentTimeMillis()}.jpg")
                            file.parentFile?.mkdirs()
                            cameraUri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                            takePhoto.launch(cameraUri!!)
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpenCityColors.BorderColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null, tint = OpenCityColors.BrandCyan, modifier = Modifier.size(16.dp))
                            Text("Chụp ảnh", fontSize = 13.sp, color = OpenCityColors.TextMain, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                photo?.let { uri ->
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = OpenCityColors.BrandAzureLight,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = OpenCityColors.BrandAzure, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(text = "Đã đính kèm ảnh minh chứng", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.BrandAzureDark)
                                    Text(text = uri.lastPathSegment ?: "photo.jpg", fontSize = 10.sp, color = OpenCityColors.TextSecondary)
                                }
                            }
                            IconButton(onClick = { photo = null }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa ảnh", tint = OpenCityColors.BrandRedDark, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val draft = JSONObject()
                        .put("title", title)
                        .put("description", description)
                        .put("category", category)
                        .put("address", address)
                    if (latitude != null && longitude != null) {
                        draft.put("latitude", latitude)
                        draft.put("longitude", longitude)
                    }
                    prefs.edit().putString(DRAFT, draft.toString()).apply()
                    message = "Đã lưu bản nháp vào thiết bị an toàn."
                    isSuccessMessage = true
                },
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OpenCityColors.BorderColor)
            ) {
                Text("Lưu nháp", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = OpenCityColors.TextSecondary)
            }

            Button(
                onClick = {
                    if (title.trim().length < 5 || description.trim().length < 15 || address.isBlank()) {
                        message = "Vui lòng nhập: Tiêu đề (tối thiểu 5 ký tự), Mô tả (tối thiểu 15 ký tự) và Địa chỉ."
                        isSuccessMessage = false
                    } else {
                        busy = true
                        message = null
                        scope.launch {
                            try {
                                val result = api.createReport(title, description, category, address, latitude, longitude, photo)
                                prefs.edit().remove(DRAFT).apply()
                                onCreated(result.text("id"))
                            } catch (e: Exception) {
                                message = e.message ?: "Không gửi được phản ánh hiện trường. Vui lòng kiểm tra lại."
                                isSuccessMessage = false
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.weight(2f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Đang gửi...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Gửi phản ánh ngay", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

private suspend fun currentLocation(context: Context): Pair<Double, Double>? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return null

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = when {
        fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return null
    }

    val last = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    if (last != null && System.currentTimeMillis() - last.time < 120_000) {
        return last.latitude to last.longitude
    }

    if (Build.VERSION.SDK_INT < 30) {
        return last?.let { it.latitude to it.longitude }
    }

    return suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { point ->
            if (continuation.isActive) {
                continuation.resume(point?.let { it.latitude to it.longitude })
            }
        }
    }
}
