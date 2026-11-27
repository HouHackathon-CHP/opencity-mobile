// SPDX-License-Identifier: MIT
@file:Suppress("DEPRECATION")
package org.houhackathon.opencity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume

// JSON Helpers
private fun JSONObject.intOrNull(key: String): Int? = if (has(key) && !isNull(key)) optInt(key) else null
private fun JSONObject.doubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) optDouble(key).takeIf { !it.isNaN() } else null
private fun JSONObject.objOrNull(key: String): JSONObject? = optJSONObject(key)
private fun JSONObject.arrOrNull(key: String): JSONArray? = optJSONArray(key)

private fun calcDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

private fun formatDistanceKm(distKm: Double): String {
    return if (distKm < 1.0) {
        "${(distKm * 1000).toInt()} m"
    } else {
        String.format(Locale.US, "%.1f km", distKm)
    }
}

private suspend fun fetchCurrentLocation(context: Context): Pair<Double, Double>? = withContext(Dispatchers.IO) {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!fine && !coarse) return@withContext null

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
    val provider = when {
        fine && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    } ?: return@withContext null

    val last = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
    if (last != null && System.currentTimeMillis() - last.time < 180_000) {
        return@withContext last.latitude to last.longitude
    }

    if (Build.VERSION.SDK_INT < 30) {
        return@withContext last?.let { it.latitude to it.longitude }
    }

    return@withContext suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        runCatching {
            manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { point ->
                if (continuation.isActive) {
                    continuation.resume(point?.let { it.latitude to it.longitude })
                }
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(last?.let { it.latitude to it.longitude })
        }
    }
}

private fun extractCleanNameAndDistrict(station: JSONObject): Pair<String, String> {
    val rawName = station.text("name").removePrefix("Trạm ").trim()
    var district = station.text("district").trim()
    var cleanName = rawName

    if (rawName.contains(" - ")) {
        val parts = rawName.split(" - ")
        cleanName = parts[0].trim()
        if (district.isBlank() && parts.size > 1) {
            district = parts[1].trim()
        }
    }

    if (district.isBlank()) {
        val address = station.text("address")
        val knownDistricts = listOf("Hoàn Kiếm", "Ba Đình", "Cầu Giấy", "Đống Đa", "Tây Hồ", "Hai Bà Trưng", "Hoàng Mai", "Thanh Xuân", "Long Biên", "Nam Từ Liêm", "Bắc Từ Liêm", "Hà Đông")
        for (d in knownDistricts) {
            if (address.contains(d, ignoreCase = true) || rawName.contains(d, ignoreCase = true)) {
                district = d
                break
            }
        }
    }

    return cleanName to district
}

// WAQI uses the US EPA AQI scale.
private val AqiColorGood = Color(0xFF10B981)              // 0-50: Tốt (Xanh ngọc)
private val AqiColorModerate = Color(0xFFF59E0B)          // 51-100: Trung bình (Vàng hổ phách)
private val AqiColorUnhealthySens = Color(0xFFF97316)     // 101-150: Kém (Cam)
private val AqiColorUnhealthy = Color(0xFFEF4444)         // 151-200: Xấu (Đỏ)
private val AqiColorVeryUnhealthy = Color(0xFF8B5CF6)     // 201-300: Rất xấu (Tím)
private val AqiColorHazardous = Color(0xFF7F1D1D)         // >300: Nguy hại (Nâu sẫm)

private val AqiGradientPalette = listOf(
    AqiColorGood,
    AqiColorModerate,
    AqiColorUnhealthySens,
    AqiColorUnhealthy,
    AqiColorVeryUnhealthy,
    AqiColorHazardous
)

private fun aqiColorForVal(aqi: Int): Color = when {
    aqi <= 50 -> AqiColorGood
    aqi <= 100 -> AqiColorModerate
    aqi <= 150 -> AqiColorUnhealthySens
    aqi <= 200 -> AqiColorUnhealthy
    aqi <= 300 -> AqiColorVeryUnhealthy
    else -> AqiColorHazardous
}

private fun aqiColorHex(aqi: Int): String = when {
    aqi <= 50 -> "#10B981"
    aqi <= 100 -> "#F59E0B"
    aqi <= 150 -> "#F97316"
    aqi <= 200 -> "#EF4444"
    aqi <= 300 -> "#8B5CF6"
    else -> "#7F1D1D"
}

private fun aqiLevelTitle(aqi: Int): String = when {
    aqi <= 50 -> "Tốt"
    aqi <= 100 -> "Trung bình"
    aqi <= 150 -> "Kém"
    aqi <= 200 -> "Xấu"
    aqi <= 300 -> "Rất xấu"
    else -> "Nguy hại"
}

private fun aqiQuickAdvice(aqi: Int): Pair<String, List<String>> = when {
    aqi <= 50 -> "Không khí trong lành, thời tiết lý tưởng cho mọi hoạt động ngoài trời!" to
            listOf("Tập thể dục ngoài trời thoải mái", "Mở cửa đón gió thông thoáng", "Không cần đeo khẩu trang")
    aqi <= 100 -> "Chất lượng không khí chấp nhận được. Người nhạy cảm lưu ý khi vận động mạnh." to
            listOf("Đi lại, làm việc bình thường", "Người nhạy cảm chú ý hô hấp", "Đeo khẩu trang khi đường đông")
    aqi <= 150 -> "Bụi mịn tăng cao! Trẻ nhỏ, người lớn tuổi và bệnh hô hấp nên hạn chế ra đường." to
            listOf("Đeo khẩu trang N95 khi ra ngoài", "Hạn chế chạy bộ thể dục ngoài phố", "Đóng bớt cửa sổ giờ cao điểm")
    aqi <= 200 -> "Không khí ô nhiễm xấu! Bắt buộc đeo khẩu trang chống bụi khi ra đường." to
            listOf("Đeo khẩu trang lọc bụi mịn", "Tránh thể dục ngoài trời", "Bật máy lọc không khí trong nhà")
    else -> "Ô nhiễm nguy hại! Hạn chế tối đa ra ngoài trời, đóng kín cửa sổ và lọc khí liên tục." to
            listOf("Ở trong nhà, đóng kín cửa", "Bật máy lọc khí tối đa", "Tránh ra ngoài")
}

@Composable
fun AirQualityScreen(
    api: ApiClient,
    revision: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var summary by remember { mutableStateOf<JSONObject?>(null) }
    var stations by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedStationForDetail by remember { mutableStateOf<JSONObject?>(null) }

    var userCoords by remember { mutableStateOf<Pair<Double, Double>>(21.0285 to 105.8542) }
    var isGpsLocked by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf("overview") }

    var selectedZone by remember { mutableStateOf("near") }

    val askLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            scope.launch {
                isLocating = true
                val loc = fetchCurrentLocation(context)
                if (loc != null) {
                    userCoords = loc
                    isGpsLocked = true
                }
                isLocating = false
            }
        }
    }

    val loadData = {
        scope.launch {
            try {
                val sum = api.item("/air-quality/summary")
                val stList = api.list("/air-quality/stations")
                summary = sum
                stations = stList
            } catch (_: Exception) {
                summary = null
                stations = emptyList()
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(revision) {
        loadData()
        scope.launch {
            val loc = fetchCurrentLocation(context)
            if (loc != null) {
                userCoords = loc
                isGpsLocked = true
            }
        }
    }

    val stationsWithDistance = remember(stations, userCoords) {
        stations.map { st ->
            val lat = st.doubleOrNull("latitude") ?: 21.0285
            val lon = st.doubleOrNull("longitude") ?: 105.8542
            val dist = calcDistanceKm(userCoords.first, userCoords.second, lat, lon)
            st to dist
        }.sortedBy { it.second }
    }

    val nearestStation = remember(stationsWithDistance) {
        stationsWithDistance.firstOrNull { (station, _) ->
            station.objOrNull("latest_reading") != null
        }?.first
    }

    val heroStation = remember(selectedZone, nearestStation, stations) {
        when (selectedZone) {
            "near" -> nearestStation ?: stations.firstOrNull()
            "all" -> nearestStation ?: stations.firstOrNull()
            else -> stations.find {
                val (_, dist) = extractCleanNameAndDistrict(it)
                dist.contains(selectedZone, ignoreCase = true) || it.text("name").contains(selectedZone, ignoreCase = true)
            } ?: nearestStation ?: stations.firstOrNull()
        }
    }

    val heroDistance = remember(heroStation, userCoords) {
        heroStation?.let { st ->
            val lat = st.doubleOrNull("latitude") ?: 21.0285
            val lon = st.doubleOrNull("longitude") ?: 105.8542
            calcDistanceKm(userCoords.first, userCoords.second, lat, lon)
        }
    }

    val displayedStations = remember(selectedZone, stationsWithDistance) {
        if (selectedZone == "near" || selectedZone == "all") {
            stationsWithDistance
        } else {
            stationsWithDistance.filter { (st, _) ->
                val (_, dist) = extractCleanNameAndDistrict(st)
                dist.contains(selectedZone, ignoreCase = true) || st.text("name").contains(selectedZone, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
    ) {
        Surface(
            color = OpenCityColors.CardBg,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, OpenCityColors.BorderColor.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = if (isGpsLocked) OpenCityColors.BrandMintLight else OpenCityColors.BrandAzureLight,
                            shape = CircleShape,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isGpsLocked) Icons.Default.GpsFixed else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isGpsLocked) OpenCityColors.BrandMintDark else OpenCityColors.BrandAzure,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (isGpsLocked) "Vị trí GPS của bạn" else "Khu vực trung tâm Hà Nội",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.TextMain
                            )
                            Text(
                                text = if (isLocating) "Đang tìm tín hiệu vệ tinh GPS..."
                                else if (isGpsLocked) "Đã cập nhật theo vị trí thực tế"
                                else "Mặc định trung tâm • Chạm để định vị",
                                fontSize = 11.sp,
                                color = if (isGpsLocked) OpenCityColors.BrandMintDark else OpenCityColors.TextMuted
                            )
                        }
                    }

                    Surface(
                        color = OpenCityColors.BrandAzureLight,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.clickable {
                            askLocationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Định vị",
                                tint = OpenCityColors.BrandAzure,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isLocating) "Đang tìm..." else "Định vị",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.BrandAzure
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(OpenCityColors.SurfaceBg, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentTab == "overview") OpenCityColors.BrandAzure else Color.Transparent)
                            .clickable { currentTab = "overview" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = if (currentTab == "overview") Color.White else OpenCityColors.TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Quanh bạn & Vùng",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTab == "overview") Color.White else OpenCityColors.TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentTab == "map") OpenCityColors.BrandAzure else Color.Transparent)
                            .clickable { currentTab = "map" },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = if (currentTab == "map") Color.White else OpenCityColors.TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Bản đồ 3D",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentTab == "map") Color.White else OpenCityColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        if (loading && stations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OpenCityColors.BrandAzure)
            }
        } else if (currentTab == "map") {
            AirQualityMapView(
                stations = stationsWithDistance.map { it.first },
                userCoords = userCoords,
                onStationClick = { selectedStationForDetail = it }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ZoneSelectorBar(
                    selectedZone = selectedZone,
                    onSelectZone = { selectedZone = it }
                )

                heroStation?.takeIf { it.objOrNull("latest_reading") != null }?.let { st ->
                    HeroZoneAirCard(
                        station = st,
                        distanceKm = heroDistance,
                        isGpsLocked = isGpsLocked,
                        onClickDetail = { selectedStationForDetail = st }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trạm quan trắc xung quanh (${displayedStations.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.TextMain
                    )
                    Text(
                        text = "Gần nhất xếp trước",
                        fontSize = 11.sp,
                        color = OpenCityColors.TextMuted
                    )
                }

                displayedStations.forEach { (st, dist) ->
                    NearbyStationCard(
                        station = st,
                        distKm = dist,
                        onClick = { selectedStationForDetail = st }
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    selectedStationForDetail?.let { station ->
        StationDetailDialog(
            station = station,
            onDismiss = { selectedStationForDetail = null }
        )
    }
}

@Composable
private fun ZoneSelectorBar(
    selectedZone: String,
    onSelectZone: (String) -> Unit
) {
    val zones = listOf(
        "near" to "Gần tôi nhất",
        "all" to "Tất cả vùng",
        "Hoàn Kiếm" to "Hoàn Kiếm",
        "Ba Đình" to "Ba Đình",
        "Cầu Giấy" to "Cầu Giấy",
        "Đống Đa" to "Đống Đa",
        "Tây Hồ" to "Tây Hồ",
        "Hai Bà Trưng" to "Hai Bà Trưng"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(zones) { (key, label) ->
            val isSelected = selectedZone == key
            val isNear = key == "near"
            Surface(
                color = if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.CardBg,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.BorderColor),
                modifier = Modifier.clickable { onSelectZone(key) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    if (isNear) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else OpenCityColors.BrandAzure,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else OpenCityColors.TextMain
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroZoneAirCard(
    station: JSONObject,
    distanceKm: Double?,
    isGpsLocked: Boolean,
    onClickDetail: () -> Unit
) {
    val (cleanName, district) = extractCleanNameAndDistrict(station)
    val reading = station.objOrNull("latest_reading")
    val aqi = reading?.intOrNull("aqi") ?: 75
    val pm25 = reading?.doubleOrNull("pm25")
    val pm10 = reading?.doubleOrNull("pm10")
    val temp = reading?.doubleOrNull("temperature")
    val humidity = reading?.doubleOrNull("humidity")

    val color = aqiColorForVal(aqi)
    val levelLabel = aqiLevelTitle(aqi)

    Surface(
        color = OpenCityColors.CardBg,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickDetail() }
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.10f),
                            OpenCityColors.CardBg
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = color.copy(alpha = 0.18f),
                        shape = CircleShape,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Text(
                        text = if (district.isNotBlank()) "KHU VỰC: ${district.uppercase()}" else "KHÔNG KHÍ GẦN BẠN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        letterSpacing = 0.5.sp
                    )
                }

                distanceKm?.let { dist ->
                    Surface(
                        color = color.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isGpsLocked) "Cách ${formatDistanceKm(dist)} • R 2 km" else "Bán kính ~2.0 km",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = color,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$aqi",
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Black,
                            color = color,
                            lineHeight = 46.sp
                        )
                        Text(
                            text = "AQI (US EPA)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMuted,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text = "Trạm đo: $cleanName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = OpenCityColors.TextSecondary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = color,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 1.dp
                    ) {
                        Text(
                            text = levelLabel.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = when {
                            aqi <= 50 -> "Rất an toàn"
                            aqi <= 100 -> "Chấp nhận được"
                            aqi <= 150 -> "Nhạy cảm cẩn trọng"
                            aqi <= 200 -> "Có hại sức khỏe"
                            else -> "Nguy hại cao"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = OpenCityColors.TextSecondary
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AqiLinearGaugeBar(aqi = aqi)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("0 Tốt", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AqiColorGood, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                    Text("100 TB", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AqiColorModerate, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("150 Kém", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AqiColorUnhealthySens, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("200 Xấu", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AqiColorUnhealthy, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("300+ Nguy hại", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = AqiColorHazardous, modifier = Modifier.weight(1.3f), textAlign = TextAlign.End)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionQuickCard(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    iconTint = OpenCityColors.BrandAzure,
                    iconBg = OpenCityColors.BrandAzureLight,
                    title = "Thể thao",
                    status = when {
                        aqi <= 50 -> "Lý tưởng"
                        aqi <= 100 -> "Bình thường"
                        aqi <= 150 -> "Hạn chế"
                        else -> "Tránh"
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionQuickCard(
                    icon = Icons.Default.Masks,
                    iconTint = if (aqi <= 100) OpenCityColors.BrandMintDark else OpenCityColors.BrandAmberDark,
                    iconBg = if (aqi <= 100) OpenCityColors.BrandMintLight else OpenCityColors.BrandAmberLight,
                    title = "Khẩu trang",
                    status = when {
                        aqi <= 50 -> "Chưa cần"
                        aqi <= 100 -> "Tùy ý"
                        aqi <= 150 -> "Nên đeo"
                        else -> "Bắt buộc"
                    },
                    modifier = Modifier.weight(1f)
                )
                ActionQuickCard(
                    icon = Icons.Default.Window,
                    iconTint = if (aqi <= 100) OpenCityColors.BrandCyan else OpenCityColors.BrandRed,
                    iconBg = if (aqi <= 100) OpenCityColors.BrandCyanLight else OpenCityColors.BrandRedLight,
                    title = "Cửa sổ",
                    status = when {
                        aqi <= 50 -> "Mở thoáng"
                        aqi <= 100 -> "Mở tốt"
                        aqi <= 150 -> "Khép bớt"
                        else -> "Đóng kín"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniMetricCard(
                    icon = Icons.Default.Grain,
                    iconTint = if (pm25 != null && pm25 <= 50.0) OpenCityColors.BrandMintDark else OpenCityColors.BrandAmberDark,
                    label = "Bụi PM2.5",
                    value = pm25?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                    unit = "AQI",
                    modifier = Modifier.weight(1f)
                )
                MiniMetricCard(
                    icon = Icons.Default.Grain,
                    iconTint = if (pm10 != null && pm10 <= 50.0) OpenCityColors.BrandCyan else OpenCityColors.BrandAmberDark,
                    label = "Bụi PM10",
                    value = pm10?.let { String.format(Locale.US, "%.1f", it) } ?: "—",
                    unit = "AQI",
                    modifier = Modifier.weight(1f)
                )
                MiniMetricCard(
                    icon = Icons.Default.Thermostat,
                    iconTint = Color(0xFFF97316),
                    label = "Nhiệt độ",
                    value = temp?.toInt()?.toString() ?: "—",
                    unit = "°C",
                    modifier = Modifier.weight(1f)
                )
                MiniMetricCard(
                    icon = Icons.Default.WaterDrop,
                    iconTint = OpenCityColors.BrandAzure,
                    label = "Độ ẩm",
                    value = humidity?.toInt()?.toString() ?: "—",
                    unit = "%",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chạm để xem bảng chỉ số chi tiết tất cả khí đo →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OpenCityColors.BrandAzure
                )
            }
        }
    }
}

@Composable
private fun ActionQuickCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = OpenCityColors.CardBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        shadowElevation = 0.5.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = iconBg,
                shape = CircleShape,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = OpenCityColors.TextSecondary
            )
            Text(
                text = status,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextMain
            )
        }
    }
}

@Composable
private fun MiniMetricCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = OpenCityColors.CardBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        modifier = modifier.height(68.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 7.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = OpenCityColors.TextMuted,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextMain
                )
                if (unit.isNotBlank()) {
                    Text(
                        text = if (unit == "°C" || unit == "%") unit else " $unit",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Normal,
                        color = OpenCityColors.TextMuted,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AqiLinearGaugeBar(aqi: Int) {
    val fraction = (aqi.toFloat() / 300f).coerceIn(0.04f, 0.96f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(AqiGradientPalette))
        )

        Box(
            modifier = Modifier.fillMaxWidth(fraction)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(Color.White)
                    .shadow(2.dp, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(aqiColorForVal(aqi))
                )
            }
        }
    }
}

@Composable
private fun NearbyStationCard(
    station: JSONObject,
    distKm: Double,
    onClick: () -> Unit
) {
    val (cleanName, district) = extractCleanNameAndDistrict(station)
    val address = station.text("address")
    val reading = station.objOrNull("latest_reading")
    val aqi = reading?.intOrNull("aqi")
    val pm25 = reading?.doubleOrNull("pm25")
    val color = aqi?.let(::aqiColorForVal) ?: Color(0xFF64748B)
    val label = aqi?.let(::aqiLevelTitle) ?: "Chưa có AQI"

    Surface(
        color = OpenCityColors.CardBg,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = color,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(66.dp)
                    .height(62.dp),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = aqi?.toString() ?: "—",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 21.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = cleanName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (district.isNotBlank()) {
                        Surface(
                            color = OpenCityColors.BrandAzureLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = district,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OpenCityColors.BrandAzure,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = OpenCityColors.BrandMintDark,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Cách ${formatDistanceKm(distKm)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = OpenCityColors.BrandMintDark
                        )
                    }

                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = OpenCityColors.TextLight
                    )

                    Text(
                        text = "AQI PM2.5: ${pm25?.let { String.format(Locale.US, "%.1f", it) } ?: "—"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = OpenCityColors.TextSecondary
                    )
                }

                if (address.isNotBlank()) {
                    Text(
                        text = address,
                        fontSize = 11.sp,
                        color = OpenCityColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                color = OpenCityColors.SurfaceBg,
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = OpenCityColors.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// tương tác MapLibre Map View
@Composable
private fun AirQualityMapView(
    stations: List<JSONObject>,
    userCoords: Pair<Double, Double>,
    onStationClick: (JSONObject) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedStationOnMap by remember { mutableStateOf<JSONObject?>(null) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var showRadiusZones by remember { mutableStateOf(true) }

    fun updateZonesGeoJson(style: Style) {
        val source = style.getSourceAs<GeoJsonSource>("aqi-zones")
        val newGeoJson = if (showRadiusZones) {
            buildAqiZonesGeoJson(stations, selectedStationOnMap?.text("code"), userCoords)
        } else {
            "{\"type\":\"FeatureCollection\",\"features\":[]}"
        }
        source?.setGeoJson(newGeoJson)
    }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            val mapViewForCamera = this
            getMapAsync { map ->
                map.setStyle(
                    Style.Builder().fromUri("https://tiles.openfreemap.org/styles/positron")
                ) { style ->
                    mapLibreMap = map
                    val initialCenter = LatLng(userCoords.first, userCoords.second)
                    map.cameraPosition = CameraPosition.Builder()
                        .target(initialCenter)
                        .zoom(12.6)
                        .tilt(28.0)
                        .bearing(-10.0)
                        .build()

                    val initialGeoJson = buildAqiZonesGeoJson(stations, selectedStationOnMap?.text("code"), userCoords)
                    val aqiSource = GeoJsonSource("aqi-zones", initialGeoJson)
                    style.addSource(aqiSource)

                    val fillLayer = FillLayer("aqi-zones-fill", "aqi-zones").apply {
                        setProperties(
                            PropertyFactory.fillColor(Expression.get("color")),
                            PropertyFactory.fillOpacity(Expression.get("fillOpacity"))
                        )
                    }
                    style.addLayer(fillLayer)

                    val lineLayer = LineLayer("aqi-zones-line", "aqi-zones").apply {
                        setProperties(
                            PropertyFactory.lineColor(Expression.get("color")),
                            PropertyFactory.lineWidth(Expression.get("lineWidth")),
                            PropertyFactory.lineOpacity(Expression.get("lineOpacity")),
                            PropertyFactory.lineDasharray(arrayOf(3f, 2f))
                        )
                    }
                    style.addLayer(lineLayer)

                    val userIcon = IconFactory.getInstance(context).fromBitmap(createUserLocationMarkerBitmap(context))
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(userCoords.first, userCoords.second))
                            .title("Vị trí của bạn")
                            .snippet("Bán kính quan sát quanh bạn")
                            .icon(userIcon)
                    )

                    stations.forEach { st ->
                        val lat = st.doubleOrNull("latitude") ?: return@forEach
                        val lon = st.doubleOrNull("longitude") ?: return@forEach
                        val rd = st.objOrNull("latest_reading")
                        val aqi = rd?.intOrNull("aqi")
                        val (cleanName, _) = extractCleanNameAndDistrict(st)

                        val markerBitmap = createAqiMarkerBitmap(context, aqi)
                        val icon = IconFactory.getInstance(context).fromBitmap(markerBitmap)

                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(lat, lon))
                                .title(cleanName)
                                .snippet(aqi?.let { "$it AQI" } ?: "Chưa có AQI")
                                .icon(icon)
                        )
                    }

                    if (stations.size >= 2) {
                        val bounds = LatLngBounds.Builder()
                        stations.forEach { st ->
                            val lat = st.doubleOrNull("latitude")
                            val lon = st.doubleOrNull("longitude")
                            if (lat != null && lon != null) bounds.include(LatLng(lat, lon))
                        }
                        mapViewForCamera.post {
                            runCatching {
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100), 700)
                            }
                        }
                    }

                    map.setOnMarkerClickListener { marker ->
                        if (marker.title == "Vị trí của bạn") return@setOnMarkerClickListener true
                        val found = stations.find { st ->
                            val lat = st.doubleOrNull("latitude") ?: 0.0
                            val lon = st.doubleOrNull("longitude") ?: 0.0
                            Math.abs(lat - marker.position.latitude) < 0.001 &&
                                    Math.abs(lon - marker.position.longitude) < 0.001
                        }
                        if (found != null) {
                            selectedStationOnMap = found
                        }
                        true
                    }

                    map.addOnMapClickListener { latLng ->
                        val point = map.projection.toScreenLocation(latLng)
                        val features = map.queryRenderedFeatures(point, "aqi-zones-fill")
                        if (features.isNotEmpty()) {
                            val code = features[0].getStringProperty("code")
                            if (!code.isNullOrBlank() && code != "user-location-zone") {
                                val found = stations.find { it.text("code") == code }
                                if (found != null) {
                                    selectedStationOnMap = found
                                    return@addOnMapClickListener true
                                }
                            }
                        }
                        false
                    }
                }
            }
        }
    }

    LaunchedEffect(selectedStationOnMap, showRadiusZones) {
        mapLibreMap?.getStyle { style ->
            updateZonesGeoJson(style)
        }
        selectedStationOnMap?.let { st ->
            val lat = st.doubleOrNull("latitude")
            val lon = st.doubleOrNull("longitude")
            if (lat != null && lon != null) {
                mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 13.5))
            }
        }
    }

    LaunchedEffect(userCoords) {
        mapLibreMap?.getStyle { style ->
            updateZonesGeoJson(style)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Surface(
            color = Color.White.copy(alpha = 0.94f),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, OpenCityColors.BrandAzure.copy(alpha = 0.25f)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .clickable { showRadiusZones = !showRadiusZones }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = if (showRadiusZones) OpenCityColors.BrandAzure else OpenCityColors.TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (showRadiusZones) "Vòng vùng bán kính: ~1.8 km (Đang bật)" else "Vòng vùng: Đã ẩn",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (showRadiusZones) OpenCityColors.BrandAzure else OpenCityColors.TextMuted
                )
            }
        }

        Surface(
            color = Color.White.copy(alpha = 0.92f),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 3.dp,
            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp, top = 56.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Vùng chất lượng",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = AqiColorGood, label = "Tốt")
                    LegendDot(color = AqiColorModerate, label = "TB")
                    LegendDot(color = AqiColorUnhealthySens, label = "Kém")
                    LegendDot(color = AqiColorUnhealthy, label = "Xấu")
                }
            }
        }

        selectedStationOnMap?.let { st ->
            val (cleanName, district) = extractCleanNameAndDistrict(st)
            val address = st.text("address")
            val rd = st.objOrNull("latest_reading")
            val aqi = rd?.intOrNull("aqi")
            val color = aqi?.let(::aqiColorForVal) ?: Color(0xFF64748B)
            val label = aqi?.let(::aqiLevelTitle) ?: "Chưa có AQI"
            val dist = calcDistanceKm(userCoords.first, userCoords.second, st.doubleOrNull("latitude") ?: 21.0, st.doubleOrNull("longitude") ?: 105.8)

            Surface(
                color = OpenCityColors.CardBg,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clickable { onStationClick(st) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = color,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .width(54.dp)
                            .height(54.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(aqi?.toString() ?: "—", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(cleanName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (district.isNotBlank()) {
                                Surface(color = OpenCityColors.BrandAzureLight, shape = RoundedCornerShape(4.dp)) {
                                    Text(district, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.BrandAzure, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(
                            text = "Cách bạn ${formatDistanceKm(dist)} • Bán kính vùng phủ 1.8 km",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OpenCityColors.BrandMintDark
                        )
                        Text(address, fontSize = 11.sp, color = OpenCityColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    Button(
                        onClick = { onStationClick(st) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Chi tiết", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = OpenCityColors.TextMain
        )
    }
}

private fun generateCircleCoords(
    lng: Double,
    lat: Double,
    radiusKm: Double = 1.8,
    numPoints: Int = 32
): List<Pair<Double, Double>> {
    val points = ArrayList<Pair<Double, Double>>(numPoints + 1)
    val latRadius = radiusKm / 111.0
    val lngRadius = radiusKm / (111.0 * Math.cos(Math.toRadians(lat)))
    for (i in 0..numPoints) {
        val angle = 2.0 * Math.PI * i / numPoints
        val x = lng + lngRadius * Math.cos(angle)
        val y = lat + latRadius * Math.sin(angle)
        points.add(Pair(x, y))
    }
    return points
}

private fun buildAqiZonesGeoJson(
    stations: List<JSONObject>,
    selectedCode: String?,
    userCoords: Pair<Double, Double>?
): String {
    val root = JSONObject()
    root.put("type", "FeatureCollection")
    val features = JSONArray()

    stations.forEach { st ->
        val lat = st.doubleOrNull("latitude") ?: return@forEach
        val lon = st.doubleOrNull("longitude") ?: return@forEach
        val rd = st.objOrNull("latest_reading")
        val aqi = rd?.intOrNull("aqi") ?: return@forEach
        val code = st.text("code")
        val isSelected = selectedCode == code
        val color = aqiColorHex(aqi)

        val feature = JSONObject()
        feature.put("type", "Feature")

        val props = JSONObject()
        props.put("code", code)
        props.put("color", color)
        props.put("fillOpacity", if (isSelected) 0.28 else 0.13)
        props.put("lineWidth", if (isSelected) 3.0 else 1.8)
        props.put("lineOpacity", 0.85)
        feature.put("properties", props)

        val geometry = JSONObject()
        geometry.put("type", "Polygon")
        val ring = JSONArray()
        val circleRing = generateCircleCoords(lon, lat, radiusKm = if (isSelected) 2.2 else 1.8)
        circleRing.forEach { (lng, ltt) ->
            val pt = JSONArray()
            pt.put(lng)
            pt.put(ltt)
            ring.put(pt)
        }
        val coords = JSONArray()
        coords.put(ring)
        geometry.put("coordinates", coords)
        feature.put("geometry", geometry)

        features.put(feature)
    }

    userCoords?.let { (userLat, userLon) ->
        val feature = JSONObject()
        feature.put("type", "Feature")
        val props = JSONObject()
        props.put("code", "user-location-zone")
        props.put("color", "#0284c7")
        props.put("fillOpacity", 0.10)
        props.put("lineWidth", 1.8)
        props.put("lineOpacity", 0.80)
        feature.put("properties", props)

        val geometry = JSONObject()
        geometry.put("type", "Polygon")
        val ring = JSONArray()
        val circleRing = generateCircleCoords(userLon, userLat, radiusKm = 1.2)
        circleRing.forEach { (lng, ltt) ->
            val pt = JSONArray()
            pt.put(lng)
            pt.put(ltt)
            ring.put(pt)
        }
        val coords = JSONArray()
        coords.put(ring)
        geometry.put("coordinates", coords)
        feature.put("geometry", geometry)

        features.put(feature)
    }

    root.put("features", features)
    return root.toString()
}

private fun createUserLocationMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (34 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x330284C7.toInt()
    }
    canvas.drawCircle(center, center, center, glowPaint)

    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF0284C7.toInt()
    }
    canvas.drawCircle(center, center, 9 * density, circlePaint)

    val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 9 * density, outlinePaint)

    val centerDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 3f * density, centerDot)

    return bitmap
}

private fun createAqiMarkerBitmap(context: Context, aqi: Int?): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (38 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = (aqi?.let(::aqiColorForVal) ?: Color(0xFF64748B)).toArgb()
    }

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33000000
    }

    val radius = size / 2f
    canvas.drawCircle(radius, radius + 2 * density, radius - 2 * density, shadowPaint)
    canvas.drawCircle(radius, radius, radius - 2 * density, paint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(radius, radius, radius - 3.5f * density, strokePaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 13f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    val yPos = radius - (textPaint.descent() + textPaint.ascent()) / 2
    canvas.drawText(aqi?.toString() ?: "—", radius, yPos, textPaint)

    return bitmap
}

@Composable
private fun StationDetailDialog(
    station: JSONObject,
    onDismiss: () -> Unit
) {
    val (cleanName, district) = extractCleanNameAndDistrict(station)
    val address = station.text("address")
    val reading = station.objOrNull("latest_reading")
    val aqi = reading?.intOrNull("aqi")
    val pm25 = reading?.doubleOrNull("pm25")
    val pm10 = reading?.doubleOrNull("pm10")
    val temp = reading?.doubleOrNull("temperature")
    val humidity = reading?.doubleOrNull("humidity")
    val co = reading?.doubleOrNull("co")
    val no2 = reading?.doubleOrNull("no2")
    val so2 = reading?.doubleOrNull("so2")
    val o3 = reading?.doubleOrNull("o3")

    val color = aqi?.let(::aqiColorForVal) ?: Color(0xFF64748B)
    val levelLabel = aqi?.let(::aqiLevelTitle) ?: "Chưa có AQI"
    val (adviceHeadline, tips) = aqi?.let(::aqiQuickAdvice)
        ?: ("Trạm chưa công bố AQI hiện tại." to emptyList())
    fun measured(value: Double?, digits: Int = 1): String =
        value?.let { String.format(Locale.US, "%.${digits}f", it) } ?: "—"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = OpenCityColors.CardBg,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cleanName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                        Text(if (district.isNotBlank()) "$district • $address" else address, fontSize = 12.sp, color = OpenCityColors.TextMuted)
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = OpenCityColors.TextLight)
                    }
                }

                Surface(
                    color = color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("CHỈ SỐ KHÔNG KHÍ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                            Text(aqi?.let { "$it AQI" } ?: "Chưa có AQI", fontSize = 32.sp, fontWeight = FontWeight.Black, color = color)
                        }

                        Surface(color = color, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = levelLabel.uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Text(adviceHeadline, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OpenCityColors.TextMain)

                reading?.optString("note")?.takeIf { it.isNotBlank() }?.let { source ->
                    Text(source, fontSize = 11.sp, color = OpenCityColors.TextMuted)
                }

                HorizontalDivider(color = OpenCityColors.BorderColor)

                Text("Chỉ số AQI thành phần và thời tiết", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailParamBox(Modifier.weight(1f), "PM2.5", measured(pm25), "AQI", color)
                    DetailParamBox(Modifier.weight(1f), "PM10", measured(pm10), "AQI", OpenCityColors.BrandCyan)
                    DetailParamBox(Modifier.weight(1f), "Nhiệt độ", temp?.toInt()?.toString() ?: "—", "°C", Color(0xFFF97316))
                    DetailParamBox(Modifier.weight(1f), "Độ ẩm", humidity?.toInt()?.toString() ?: "—", "%", OpenCityColors.BrandAzure)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailParamBox(Modifier.weight(1f), "CO", measured(co, 2), "AQI", OpenCityColors.TextSecondary)
                    DetailParamBox(Modifier.weight(1f), "NO₂", measured(no2), "AQI", OpenCityColors.TextSecondary)
                    DetailParamBox(Modifier.weight(1f), "SO₂", measured(so2), "AQI", OpenCityColors.TextSecondary)
                    DetailParamBox(Modifier.weight(1f), "O₃", measured(o3), "AQI", OpenCityColors.TextSecondary)
                }

                // Khuyến nghị y tế
                Text("Khuyến nghị y tế:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                tips.forEach { tip ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OpenCityColors.BrandMintDark, modifier = Modifier.size(14.dp))
                        Text(tip, fontSize = 12.sp, color = OpenCityColors.TextSecondary)
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Đã hiểu", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailParamBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    accentColor: Color
) {
    Surface(
        color = OpenCityColors.SurfaceBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        modifier = modifier.height(78.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = OpenCityColors.TextMuted,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextMain,
                maxLines = 1
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (unit.isNotBlank()) unit else " ",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Normal,
                color = OpenCityColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}
