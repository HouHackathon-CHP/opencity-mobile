// SPDX-License-Identifier: MIT
@file:Suppress("DEPRECATION")
package org.houhackathon.opencity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
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
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource

enum class MapCategory(
    val key: String,
    val title: String,
    val icon: ImageVector,
    val themeColor: Color,
    val activeBg: Color
) {
    ALL(
        key = "all",
        title = "Đa tầng GIS",
        icon = Icons.Default.Layers,
        themeColor = Color(0xFF1E293B),
        activeBg = Color(0xFFF1F5F9)
    ),
    REPORTS(
        key = "reports",
        title = "Phản ánh",
        icon = Icons.Default.Place,
        themeColor = Color(0xFFEF4444),
        activeBg = Color(0xFFFEF2F2)
    ),
    BUS(
        key = "bus",
        title = "Xe buýt",
        icon = Icons.Default.DirectionsBus,
        themeColor = Color(0xFF0284C7),
        activeBg = Color(0xFFEFF6FF)
    ),
    AQI(
        key = "aqi",
        title = "Vùng AQI",
        icon = Icons.Default.Air,
        themeColor = Color(0xFFD97706),
        activeBg = Color(0xFFFFFBEB)
    )
}

enum class GisLayer(
    val key: String,
    val title: String,
    val badgeLabel: String,
    val icon: ImageVector,
    val activeColor: Color,
    val activeBg: Color,
    val borderColor: Color
) {
    REPORTS(
        key = "reports",
        title = "Phản ánh",
        badgeLabel = "PHẢN ÁNH HIỆN TRƯỜNG",
        icon = Icons.Default.Place,
        activeColor = Color(0xFFEF4444),
        activeBg = Color(0xFFFEF2F2),
        borderColor = Color(0xFFFECACA)
    ),
    BUS_STOPS(
        key = "bus_stops",
        title = "Trạm buýt",
        badgeLabel = "TRẠM XE BUÝT",
        icon = Icons.Default.DirectionsBus,
        activeColor = Color(0xFF0284C7),
        activeBg = Color(0xFFEFF6FF),
        borderColor = Color(0xFFBFDBFE)
    ),
    AQI_SENSORS(
        key = "aqi_sensors",
        title = "Vùng AQI",
        badgeLabel = "VÙNG CHẤT LƯỢNG KHÔNG KHÍ",
        icon = Icons.Default.Air,
        activeColor = Color(0xFFD97706),
        activeBg = Color(0xFFFFFBEB),
        borderColor = Color(0xFFFDE68A)
    )
}

data class BusRouteInfo(
    val code: String,
    val name: String,
    val startEnd: String,
    val operatingHours: String = "05:00 - 22:30",
    val interval: String = "10 - 15 phút",
    val fare: String = "7.000 đ"
)

val HANOI_BUS_ROUTES = listOf(
    BusRouteInfo("01", "Tuyến 01", "Bến xe Gia Lâm ⇄ Bến xe Yên Nghĩa", "05:00 - 21:00", "10 - 15 phút", "7.000 đ"),
    BusRouteInfo("02", "Tuyến 02", "Bác Cổ ⇄ Bến xe Yên Nghĩa", "05:00 - 22:30", "10 phút", "7.000 đ"),
    BusRouteInfo("07", "Tuyến 07", "Cầu Giấy ⇄ Sân bay Nội Bài", "05:00 - 21:35", "10 - 15 phút", "8.000 đ"),
    BusRouteInfo("26", "Tuyến 26", "Mai Động ⇄ SVĐ Quốc Gia", "05:00 - 22:30", "10 - 12 phút", "7.000 đ"),
    BusRouteInfo("32", "Tuyến 32", "Bến xe Giáp Bát ⇄ Nhổn", "05:05 - 22:30", "8 - 12 phút", "7.000 đ"),
    BusRouteInfo("34", "Tuyến 34", "Bến xe Mỹ Đình ⇄ Bến xe Gia Lâm", "05:07 - 21:00", "10 - 15 phút", "7.000 đ"),
    BusRouteInfo("BRT01", "BRT 01", "Kim Mã ⇄ Bến xe Yên Nghĩa", "05:00 - 22:00", "5 - 10 phút", "7.000 đ"),
    BusRouteInfo("86", "Tuyến 86", "Ga Hà Nội ⇄ Sân bay Nội Bài", "05:10 - 21:45", "25 - 30 phút", "45.000 đ"),
    BusRouteInfo("E01", "VinBus E01", "Bến xe Mỹ Đình ⇄ KĐT Ocean Park", "05:00 - 22:00", "12 - 15 phút", "8.000 đ"),
    BusRouteInfo("E03", "VinBus E03", "Cầu Diễn ⇄ KĐT Ocean Park", "05:00 - 22:00", "15 - 20 phút", "9.000 đ")
)

data class GisMapPoint(
    val id: String,
    val layer: GisLayer,
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lon: Double,
    val state: String = "",
    val isAlert: Boolean = false,
    val aqiValue: Int? = null,
    val aqiLevel: String = "",
    val aqiLabel: String = "",
    val routes: List<String> = emptyList(),
    val address: String = "",
    val category: String = "",
    val pm25: Double? = null,
    val pm10: Double? = null,
    val temperature: Double? = null,
    val humidity: Double? = null
)

private val reportStates = linkedMapOf(
    "submitted" to "Mới gửi",
    "triaged" to "Đã tiếp nhận",
    "assigned" to "Đang xử lý",
    "resolved" to "Đã hoàn tất"
)

private const val MAP_STYLE_POSITRON = "https://tiles.openfreemap.org/styles/positron"

@Composable
fun MapScreen(
    api: ApiClient,
    revision: Int,
    onDetail: (String, String) -> Unit,
    onNavigate: ((String) -> Unit)? = null
) {
    var allPoints by remember { mutableStateOf<List<GisMapPoint>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var reload by remember { mutableIntStateOf(0) }

    var currentCategory by remember { mutableStateOf(MapCategory.ALL) }

    var allLayersSelection by remember {
        mutableStateOf(setOf(GisLayer.REPORTS, GisLayer.BUS_STOPS, GisLayer.AQI_SENSORS))
    }

    var reportStatusFilter by remember { mutableStateOf("all") }

    var selectedBusRoute by remember { mutableStateOf<String?>(null) }
    var busRouteDirection by remember { mutableStateOf("forward") } // "forward" | "backward"
    var allBusRoutes by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var selectedRouteDetail by remember { mutableStateOf<JSONObject?>(null) }
    var loadingRouteLine by remember { mutableStateOf(false) }
    var showRouteDetailModal by remember { mutableStateOf(false) }

    var selectedRadius by remember { mutableFloatStateOf(3.0f) } // 3.0f, 5.0f, 10.0f, 25.0f (Toàn TP)

    var aqiFilterLevel by remember { mutableStateOf("all") }

    var searchQuery by remember { mutableStateOf("") }

    var selectedPoint by remember { mutableStateOf<GisMapPoint?>(null) }

    var isListExpanded by remember { mutableStateOf(false) }
    var listFilterTab by remember { mutableStateOf("all") }

    val scope = rememberCoroutineScope()
    var lastLoadedCenter by remember { mutableStateOf(LatLng(21.0285, 105.8542)) }
    var currentCameraTarget by remember { mutableStateOf(LatLng(21.0285, 105.8542)) }
    var showSearchThisArea by remember { mutableStateOf(false) }
    var isSearchingArea by remember { mutableStateOf(false) }

    var mapLibreMapRef by remember { mutableStateOf<MapLibreMap?>(null) }

    LaunchedEffect(revision, reload) {
        loading = true
        try {
            val reportList = try {
                val reps = api.list("/reports").mapNotNull { it.asReportPoint() }
                val alerts = api.list("/bulletins")
                    .filter { it.text("category") == "alert" }
                    .mapNotNull { it.asBulletinPoint() }
                reps + alerts
            } catch (_: Exception) {
                try {
                    api.list("/bulletins")
                        .filter { it.text("category") == "alert" }
                        .mapNotNull { it.asBulletinPoint() }
                } catch (_: Exception) {
                    emptyList()
                }
            }

            val busStopList = try {
                val stopsJson = api.list("/bus/stops")
                stopsJson.mapNotNull { it.asBusStopPoint() }
            } catch (_: Exception) {
                emptyList()
            }

            val aqiList = try {
                val stationsJson = api.list("/air-quality/stations")
                stationsJson.mapNotNull { it.asAqiPoint() }
            } catch (_: Exception) {
                emptyList()
            }

            allPoints = reportList + busStopList + aqiList

            val busRoutesList = try {
                api.list("/bus/routes")
            } catch (_: Exception) {
                emptyList()
            }
            allBusRoutes = busRoutesList
        } catch (_: Exception) {
        } finally {
            loading = false
        }
    }

    LaunchedEffect(selectedBusRoute) {
        if (selectedBusRoute == null || selectedBusRoute == "all") {
            selectedRouteDetail = null
            return@LaunchedEffect
        }
        loadingRouteLine = true
        try {
            val rObj = allBusRoutes.find { it.optString("route_number").equals(selectedBusRoute, ignoreCase = true) }
            val rId = rObj?.optString("id") ?: "hn-bus-${selectedBusRoute?.lowercase()}"
            val d = api.item("/bus/routes/$rId")
            selectedRouteDetail = d
        } catch (_: Exception) {
        } finally {
            loadingRouteLine = false
        }
    }

    val visiblePoints = remember(
        allPoints,
        currentCategory,
        allLayersSelection,
        reportStatusFilter,
        selectedBusRoute,
        aqiFilterLevel,
        searchQuery,
        selectedRouteDetail,
        busRouteDirection,
        selectedRadius
    ) {
        if (currentCategory == MapCategory.BUS && selectedBusRoute != null && selectedBusRoute != "all") {
            val pathStopsArr = if (busRouteDirection == "forward") {
                selectedRouteDetail?.optJSONArray("forward_stops")
            } else {
                selectedRouteDetail?.optJSONArray("backward_stops")
            }
            if (pathStopsArr != null && pathStopsArr.length() > 0) {
                val stopsList = mutableListOf<GisMapPoint>()
                for (i in 0 until pathStopsArr.length()) {
                    val s = pathStopsArr.optJSONObject(i) ?: continue
                    val sLat = s.optDouble("latitude")
                    val sLng = s.optDouble("longitude")
                    if (sLat == 0.0 || sLng == 0.0) continue
                    val order = s.optInt("stop_order", i + 1)
                    val sName = s.optString("name")
                    val sAddr = s.optString("address", "Hà Nội")
                    stopsList.add(
                        GisMapPoint(
                            id = s.optString("stop_id", "route-$selectedBusRoute-stop-$order"),
                            layer = GisLayer.BUS_STOPS,
                            title = sName,
                            subtitle = "Trạm #$order • Tuyến $selectedBusRoute",
                            lat = sLat,
                            lon = sLng,
                            address = sAddr,
                            routes = listOf(selectedBusRoute!!)
                        )
                    )
                }
                if (searchQuery.isBlank()) {
                    stopsList
                } else {
                    val q = searchQuery.trim().lowercase()
                    stopsList.filter {
                        it.title.lowercase().contains(q) ||
                            it.subtitle.lowercase().contains(q) ||
                            it.address.lowercase().contains(q)
                    }
                }
            } else {
                allPoints.filter { pt ->
                    pt.layer == GisLayer.BUS_STOPS && pt.routes.any { it.equals(selectedBusRoute, ignoreCase = true) }
                }
            }
        } else {
            allPoints.filter { pt ->
                val categoryMatches = when (currentCategory) {
                    MapCategory.ALL -> pt.layer in allLayersSelection
                    MapCategory.REPORTS -> pt.layer == GisLayer.REPORTS
                    MapCategory.BUS -> pt.layer == GisLayer.BUS_STOPS
                    MapCategory.AQI -> pt.layer == GisLayer.AQI_SENSORS
                }
                if (!categoryMatches) return@filter false

                val subFilterMatches = when (currentCategory) {
                    MapCategory.ALL -> true
                    MapCategory.REPORTS -> {
                        when (reportStatusFilter) {
                            "all" -> true
                            "alert" -> pt.isAlert
                            else -> pt.state == reportStatusFilter
                        }
                    }
                    MapCategory.BUS -> {
                        if (selectedBusRoute == null || selectedBusRoute == "all") {
                            true
                        } else {
                            pt.routes.any { it.equals(selectedBusRoute, ignoreCase = true) }
                        }
                    }
                    MapCategory.AQI -> {
                        val aqi = pt.aqiValue
                        when (aqiFilterLevel) {
                            "good" -> aqi != null && aqi <= 50
                            "moderate" -> aqi != null && aqi in 51..100
                            "unhealthy" -> aqi != null && aqi > 100
                            else -> true
                        }
                    }
                }
                if (!subFilterMatches) return@filter false

                if (selectedRadius < 25f && (currentCategory == MapCategory.ALL || currentCategory == MapCategory.BUS)) {
                    val d = calcDistanceKm(pt.lat, pt.lon, 21.0285, 105.8542)
                    if (d > selectedRadius) return@filter false
                }

                if (searchQuery.isBlank()) {
                    true
                } else {
                    val q = searchQuery.trim().lowercase()
                    pt.title.lowercase().contains(q) ||
                        pt.subtitle.lowercase().contains(q) ||
                        pt.address.lowercase().contains(q) ||
                        pt.routes.any { it.lowercase().contains(q) }
                }
            }
        }
    }

    val currentRouteCoords = remember(selectedRouteDetail, busRouteDirection, selectedBusRoute, visiblePoints, currentCategory) {
        if (currentCategory != MapCategory.BUS || selectedBusRoute == null || selectedBusRoute == "all") {
            emptyList()
        } else {
            val pathKey = if (busRouteDirection == "forward") "forward_path" else "backward_path"
            val pathArr = selectedRouteDetail?.optJSONArray(pathKey)
            if (pathArr != null && pathArr.length() > 0) {
                val list = mutableListOf<LatLng>()
                for (i in 0 until pathArr.length()) {
                    val pt = pathArr.optJSONArray(i) ?: continue
                    list.add(LatLng(pt.optDouble(1), pt.optDouble(0)))
                }
                list
            } else {
                val routeStops = visiblePoints.filter { pt ->
                    pt.layer == GisLayer.BUS_STOPS && pt.routes.any { it.equals(selectedBusRoute, ignoreCase = true) }
                }
                routeStops.map { LatLng(it.lat, it.lon) }
            }
        }
    }

    val currentRouteColor = remember(selectedRouteDetail) {
        selectedRouteDetail?.optString("color")?.takeIf { it.isNotBlank() } ?: "#0284C7"
    }

    LaunchedEffect(currentRouteCoords, mapLibreMapRef, currentCategory) {
        if (currentCategory == MapCategory.BUS && currentRouteCoords.size >= 2) {
            val map = mapLibreMapRef ?: return@LaunchedEffect
            try {
                val boundsBuilder = LatLngBounds.Builder()
                currentRouteCoords.forEach { boundsBuilder.include(it) }
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120),
                    700
                )
            } catch (_: Exception) {
            }
        }
    }

    LaunchedEffect(selectedRadius, mapLibreMapRef) {
        if (selectedBusRoute == null || selectedBusRoute == "all") {
            val map = mapLibreMapRef ?: return@LaunchedEffect
            val targetZoom = when {
                selectedRadius <= 3.5f -> 12.5
                selectedRadius <= 5.5f -> 11.8
                selectedRadius <= 12f -> 11.0
                else -> 10.2
            }
            try {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(21.0285, 105.8542), targetZoom),
                    500
                )
            } catch (_: Exception) {
            }
        }
    }

    val reportsCount = remember(allPoints) { allPoints.count { it.layer == GisLayer.REPORTS } }
    val busStopsCount = remember(allPoints) { allPoints.count { it.layer == GisLayer.BUS_STOPS } }
    val aqiSensorsCount = remember(allPoints) { allPoints.count { it.layer == GisLayer.AQI_SENSORS } }

    Box(modifier = Modifier.fillMaxSize()) {
        GisMapLibreView(
            points = visiblePoints,
            selectedPoint = selectedPoint,
            showAqiZones = (currentCategory == MapCategory.AQI),
            routeCoordinates = if (currentCategory == MapCategory.BUS) currentRouteCoords else emptyList(),
            routeColorHex = currentRouteColor,
            onMapReady = { map -> mapLibreMapRef = map },
            onMarkerClick = { point ->
                selectedPoint = point
                isListExpanded = false
            },
            onCameraMoved = { lat, lon, _ ->
                val dist = calcDistanceKm(lat, lon, lastLoadedCenter.latitude, lastLoadedCenter.longitude)
                if (selectedRadius >= 25f && dist >= 3.0 && !isSearchingArea) {
                    showSearchThisArea = true
                }
                currentCameraTarget = LatLng(lat, lon)
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = showSearchThisArea,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 220.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(enabled = !isSearchingArea) {
                        scope.launch {
                            isSearchingArea = true
                            try {
                                val tLat = currentCameraTarget.latitude
                                val tLon = currentCameraTarget.longitude
                                val stopsJson = api.list("/bus/stops?radius_km=4.5&lat=$tLat&lng=$tLon")
                                val newStops = stopsJson.mapNotNull { it.asBusStopPoint() }
                                if (newStops.isNotEmpty()) {
                                    allPoints = (allPoints + newStops).distinctBy { it.id }
                                }
                                lastLoadedCenter = currentCameraTarget
                                showSearchThisArea = false
                            } catch (_: Exception) {
                            } finally {
                                isSearchingArea = false
                            }
                        }
                    },
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                border = BorderStroke(1.2.dp, Color(0xFF0284C7))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSearchingArea) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF0284C7)
                        )
                        Text(
                            text = "Đang quét dữ liệu khu vực...",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "🔍 Tìm kiếm ở khu vực này",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = currentCategory.themeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = when (currentCategory) {
                                    MapCategory.ALL -> "Tìm trạm buýt, phản ánh, vùng AQI..."
                                    MapCategory.REPORTS -> "Tìm sự cố, địa chỉ phản ánh..."
                                    MapCategory.BUS -> "Tìm tên trạm buýt, số hiệu tuyến (01, 32, BRT...)..."
                                    MapCategory.AQI -> "Tìm vùng quan trắc, quận/huyện..."
                                },
                                fontSize = 12.sp,
                                color = OpenCityColors.TextLight
                            )
                        }
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa tìm kiếm",
                                tint = OpenCityColors.TextLight,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MapCategory.entries.forEach { cat ->
                        val isSelected = currentCategory == cat
                        val count = when (cat) {
                            MapCategory.ALL -> allPoints.size
                            MapCategory.REPORTS -> reportsCount
                            MapCategory.BUS -> busStopsCount
                            MapCategory.AQI -> aqiSensorsCount
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (cat != MapCategory.BUS) {
                                        selectedBusRoute = null
                                    }
                                    currentCategory = cat
                                    selectedPoint = null
                                    isListExpanded = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) cat.themeColor else Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else cat.themeColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = cat.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) Color.White else OpenCityColors.TextMain
                                    )
                                }
                                Text(
                                    text = "$count điểm",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else OpenCityColors.TextLight
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            when (currentCategory) {
                MapCategory.ALL -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val repOn = GisLayer.REPORTS in allLayersSelection
                            FilterPill(
                                label = "Phản ánh ($reportsCount)",
                                icon = Icons.Default.Place,
                                isEnabled = repOn,
                                activeColor = Color(0xFFEF4444),
                                onClick = {
                                    allLayersSelection = if (repOn) allLayersSelection - GisLayer.REPORTS else allLayersSelection + GisLayer.REPORTS
                                }
                            )

                            val aqiOn = GisLayer.AQI_SENSORS in allLayersSelection
                            FilterPill(
                                label = "Vùng AQI ($aqiSensorsCount)",
                                icon = Icons.Default.Air,
                                isEnabled = aqiOn,
                                activeColor = Color(0xFFD97706),
                                onClick = {
                                    allLayersSelection = if (aqiOn) allLayersSelection - GisLayer.AQI_SENSORS else allLayersSelection + GisLayer.AQI_SENSORS
                                }
                            )

                            val busOn = GisLayer.BUS_STOPS in allLayersSelection
                            val busVisibleCount = if (selectedRadius >= 25f) busStopsCount else visiblePoints.count { it.layer == GisLayer.BUS_STOPS }
                            FilterPill(
                                label = "Trạm buýt ($busVisibleCount)",
                                icon = Icons.Default.DirectionsBus,
                                isEnabled = busOn,
                                activeColor = Color(0xFF0284C7),
                                onClick = {
                                    allLayersSelection = if (busOn) allLayersSelection - GisLayer.BUS_STOPS else allLayersSelection + GisLayer.BUS_STOPS
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.95f),
                                shape = RoundedCornerShape(20.dp),
                                shadowElevation = 3.dp,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Radar, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "${visiblePoints.size} điểm (${if (selectedRadius >= 25f) "Toàn TP" else "${selectedRadius.toInt()} km"})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OpenCityColors.TextMain
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                listOf(3.0f to "3km", 5.0f to "5km", 10.0f to "10km", 25.0f to "Toàn TP").forEach { (r, label) ->
                                    val isSel = Math.abs(selectedRadius - r) < 0.1f
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { selectedRadius = r },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSel) Color(0xFF0284C7) else Color.White.copy(alpha = 0.92f),
                                        border = BorderStroke(1.dp, if (isSel) Color(0xFF0284C7) else Color(0xFFCBD5E1)),
                                        shadowElevation = if (isSel) 2.dp else 1.dp
                                    ) {
                                        Text(
                                            text = label,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                MapCategory.REPORTS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "all" to "Tất cả ($reportsCount)",
                                "submitted" to "Mới gửi",
                                "assigned" to "Đang xử lý",
                                "resolved" to "Đã hoàn tất",
                                "alert" to "🚨 Cảnh báo khẩn"
                            ).forEach { (stKey, stLabel) ->
                                val isSel = reportStatusFilter == stKey
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { reportStatusFilter = stKey },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSel) Color(0xFF0284C7) else Color.White.copy(alpha = 0.95f),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF0284C7) else Color(0xFFCBD5E1)),
                                    shadowElevation = if (isSel) 3.dp else 1.dp
                                ) {
                                    Text(
                                        text = stLabel,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else OpenCityColors.TextMain
                                    )
                                }
                            }

                            if (onNavigate != null) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { onNavigate("new-report") },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF0284C7),
                                    shadowElevation = 3.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Text(text = "Gửi phản ánh", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Place, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Tiếp nhận & Giám sát phản ánh hiện trường trực tiếp trên bản đồ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E40AF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                MapCategory.BUS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isAllSelected = selectedBusRoute == null || selectedBusRoute == "all"
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedBusRoute = null },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isAllSelected) Color(0xFF0284C7) else Color.White.copy(alpha = 0.95f),
                                border = BorderStroke(1.dp, if (isAllSelected) Color(0xFF0284C7) else Color(0xFFCBD5E1)),
                                shadowElevation = if (isAllSelected) 3.dp else 1.dp
                            ) {
                                Text(
                                    text = "Tất cả trạm",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontSize = 11.sp,
                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAllSelected) Color.White else OpenCityColors.TextMain
                                )
                            }

                            HANOI_BUS_ROUTES.forEach { r ->
                                val isSel = selectedBusRoute == r.code
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable {
                                             selectedBusRoute = if (isSel) null else r.code
                                        },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSel) Color(0xFF0284C7) else Color.White.copy(alpha = 0.95f),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF0284C7) else Color(0xFFCBD5E1)),
                                    shadowElevation = if (isSel) 3.dp else 1.dp
                                ) {
                                    Text(
                                        text = r.name,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else OpenCityColors.TextMain
                                    )
                                }
                            }
                        }

                        if (selectedBusRoute == null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.95f),
                                    shape = RoundedCornerShape(20.dp),
                                    shadowElevation = 3.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(13.dp))
                                        Text(
                                            text = "${visiblePoints.size} trạm dừng (${if (selectedRadius >= 25f) "Toàn TP" else "${selectedRadius.toInt()} km"})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OpenCityColors.TextMain
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    listOf(3.0f to "3km", 5.0f to "5km", 10.0f to "10km", 25.0f to "Toàn TP").forEach { (r, label) ->
                                        val isSel = Math.abs(selectedRadius - r) < 0.1f
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { selectedRadius = r },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSel) Color(0xFF0284C7) else Color.White.copy(alpha = 0.92f),
                                            border = BorderStroke(1.dp, if (isSel) Color(0xFF0284C7) else Color(0xFFCBD5E1)),
                                            shadowElevation = if (isSel) 2.dp else 1.dp
                                        ) {
                                            Text(
                                                text = label,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                fontSize = 10.5.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) Color.White else Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val activeRouteCode = selectedBusRoute
                        val activeRouteInfo = HANOI_BUS_ROUTES.firstOrNull { it.code == activeRouteCode }
                        if (activeRouteCode != null) {
                            val routeName = activeRouteInfo?.name ?: "Tuyến $activeRouteCode"
                            val routeStartEnd = activeRouteInfo?.startEnd ?: selectedRouteDetail?.optString("name") ?: "Lộ trình tuyến xe buýt"
                            val routeHours = activeRouteInfo?.operatingHours ?: selectedRouteDetail?.optString("operating_hours")?.let { formatBusHours(it) } ?: "05:00 - 21:00"
                            val routeFare = activeRouteInfo?.fare ?: selectedRouteDetail?.optString("fare")?.let { formatBusFare(it) } ?: "10.000 đ"
                            val fwdCount = selectedRouteDetail?.optJSONArray("forward_stops")?.length() ?: 38
                            val bwdCount = selectedRouteDetail?.optJSONArray("backward_stops")?.length() ?: 38

                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .background(Color.White, RoundedCornerShape(16.dp))
                                        .border(BorderStroke(1.dp, Color(0xFFCBD5E1)), RoundedCornerShape(16.dp))
                                        .padding(2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val isFwd = busRouteDirection == "forward"
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { busRouteDirection = "forward" },
                                        color = if (isFwd) Color(0xFF0284C7) else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "▶ Chiều đi ($fwdCount trạm)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isFwd) Color.White else Color(0xFF334155)
                                            )
                                        }
                                    }
                                    val isBwd = busRouteDirection == "backward"
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { busRouteDirection = "backward" },
                                        color = if (isBwd) Color(0xFF0284C7) else Color.Transparent,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "◀ Chiều về ($bwdCount trạm)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isBwd) Color.White else Color(0xFF334155)
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    shadowElevation = 2.dp
                                ) {
                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "$routeName: $routeStartEnd",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E40AF),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = { selectedBusRoute = null },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Bỏ lọc", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⏰ $routeHours • 🎫 $routeFare",
                                                fontSize = 10.sp,
                                                color = Color(0xFF2563EB),
                                                fontWeight = FontWeight.Medium
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { showRouteDetailModal = true },
                                                    color = Color(0xFF0284C7),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.FormatListNumbered, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                        Text(text = "Lịch trình trạm", fontSize = 9.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { onNavigate?.invoke("bus-routes") },
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.AltRoute, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(12.dp))
                                                        Text(text = "Mạng lưới Bus ➔", fontSize = 9.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                MapCategory.AQI -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "all" to "Tất cả ($aqiSensorsCount trạm)",
                                "good" to "🟢 Tốt (<50)",
                                "moderate" to "🟡 TB (51-100)",
                                "unhealthy" to "🟠 Kém (>100)"
                            ).forEach { (aqiKey, aqiLabel) ->
                                val isSel = aqiFilterLevel == aqiKey
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { aqiFilterLevel = aqiKey },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSel) Color(0xFFD97706) else Color.White.copy(alpha = 0.95f),
                                    border = BorderStroke(1.dp, if (isSel) Color(0xFFD97706) else Color(0xFFCBD5E1)),
                                    shadowElevation = if (isSel) 3.dp else 1.dp
                                ) {
                                    Text(
                                        text = aqiLabel,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) Color.White else OpenCityColors.TextMain
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(imageVector = Icons.Default.Radar, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "Vòng vùng bán kính: ~1.8 km (Đang bật)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🟢 Tốt", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                    Text("🟡 TB", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCA8A04))
                                    Text("🟠 Kém", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                                    Text("🔴 Xấu", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 68.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionMapButton(
                icon = Icons.Default.Add,
                contentDescription = "Phóng to",
                onClick = {
                    mapLibreMapRef?.animateCamera(CameraUpdateFactory.zoomIn(), 250)
                }
            )
            FloatingActionMapButton(
                icon = Icons.Default.Remove,
                contentDescription = "Thu nhỏ",
                onClick = {
                    mapLibreMapRef?.animateCamera(CameraUpdateFactory.zoomOut(), 250)
                }
            )
            FloatingActionMapButton(
                icon = Icons.Default.MyLocation,
                contentDescription = "Trung tâm Hà Nội",
                onClick = {
                    mapLibreMapRef?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(21.0285, 105.8542), 12.0),
                        400
                    )
                }
            )
            FloatingActionMapButton(
                icon = Icons.Default.Refresh,
                contentDescription = "Tải lại",
                onClick = { reload++ }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            if (selectedPoint != null) {
                GisInteractiveDetailCard(
                    point = selectedPoint!!,
                    onClose = { selectedPoint = null },
                    onNavigateDetail = {
                        val pt = selectedPoint ?: return@GisInteractiveDetailCard
                        when (pt.layer) {
                            GisLayer.REPORTS -> onDetail(if (pt.isAlert) "bulletin" else "report", pt.id)
                            GisLayer.BUS_STOPS -> onNavigate?.invoke("find-bus")
                            GisLayer.AQI_SENSORS -> onNavigate?.invoke("air-quality")
                        }
                    },
                    onSelectBusRoute = { routeCode ->
                        currentCategory = MapCategory.BUS
                        selectedBusRoute = routeCode
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            } else {
                GisContextualDrawer(
                    category = currentCategory,
                    totalVisible = visiblePoints.size,
                    points = visiblePoints,
                    isExpanded = isListExpanded,
                    onToggleExpand = { isListExpanded = !isListExpanded },
                    currentTab = listFilterTab,
                    onTabChange = { listFilterTab = it },
                    onPointClick = { pt ->
                        selectedPoint = pt
                        isListExpanded = false
                        mapLibreMapRef?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(pt.lat, pt.lon), 14.5),
                            350
                        )
                    },
                    onActionNavigate = onNavigate
                )
            }
        }

        if (loading) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xEE0F172A),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Text(
                        text = "Đang nạp dữ liệu GIS đa tầng...",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (showRouteDetailModal) {
            val activeRouteInfo = HANOI_BUS_ROUTES.firstOrNull { it.code == selectedBusRoute }
            BusRouteDetailModal(
                routeSummary = selectedRouteDetail,
                routeInfo = activeRouteInfo,
                direction = busRouteDirection,
                onDirectionChange = { busRouteDirection = it },
                onDismiss = { showRouteDetailModal = false },
                onNavigateToBusNetwork = { onNavigate?.invoke("bus-routes") },
                onSelectStop = { stopLat, stopLon, stopName ->
                    showRouteDetailModal = false
                    mapLibreMapRef?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(stopLat, stopLon), 15.5),
                        400
                    )
                    val foundStop = visiblePoints.find { it.title == stopName || (Math.abs(it.lat - stopLat) < 0.001 && Math.abs(it.lon - stopLon) < 0.001) }
                        ?: allPoints.find { it.layer == GisLayer.BUS_STOPS && (it.title == stopName || calcDistanceKm(it.lat, it.lon, stopLat, stopLon) < 0.05) }
                        ?: GisMapPoint(
                            id = "route-stop-${System.currentTimeMillis()}",
                            layer = GisLayer.BUS_STOPS,
                            title = stopName,
                            subtitle = "Trạm đón/trả • Tuyến ${selectedBusRoute ?: "01"}",
                            lat = stopLat,
                            lon = stopLon,
                            routes = listOfNotNull(selectedBusRoute)
                        )
                    selectedPoint = foundStop
                }
            )
        }
    }
}

private fun parseHexColor(hex: String, fallback: Color = Color(0xFF16A34A)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = if (clean.length == 6) {
            (0xFF000000 or clean.toLong(16)).toInt()
        } else if (clean.length == 8) {
            clean.toLong(16).toInt()
        } else {
            fallback.toArgb()
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}

private fun formatBusHours(raw: String): String {
    if (raw.isBlank()) return "05:00 - 21:00"
    return raw.split(";").first().trim()
        .replace("Mo-Su ", "")
        .replace("Mo-Sa ", "")
}

private fun formatBusFrequency(raw: String): String {
    if (raw.isBlank()) return "10 - 15p"
    if (raw.contains("phút", ignoreCase = true)) {
        return raw.replace("phút/chuyến", "p").replace("phút", "p").trim()
    }
    return raw.replace("00:", "").trim()
}

private fun formatBusFare(raw: String): String {
    if (raw.isBlank() || raw.equals("no", ignoreCase = true) || raw.equals("0", ignoreCase = true)) return "Miễn phí"
    if (raw.contains("đ", ignoreCase = true) || raw.contains("VND", ignoreCase = true)) {
        return raw.replace("VND", "đ").trim()
    }
    val numOnly = raw.filter { it.isDigit() }
    val num = numOnly.toLongOrNull() ?: return raw
    return java.text.NumberFormat.getIntegerInstance(java.util.Locale("vi", "VN")).format(num) + " đ"
}

@Composable
private fun BusRouteDetailModal(
    routeSummary: JSONObject?,
    routeInfo: BusRouteInfo?,
    direction: String,
    onDirectionChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToBusNetwork: () -> Unit,
    onSelectStop: (lat: Double, lon: Double, name: String) -> Unit
) {
    val routeNum = routeInfo?.code ?: routeSummary?.optString("route_number") ?: "01"
    val name = routeInfo?.startEnd ?: routeSummary?.optString("name") ?: "Lộ trình tuyến xe buýt"
    val brandColor = parseHexColor(routeSummary?.optString("color") ?: "#16A34A")
    val hours = routeSummary?.optString("operating_hours")?.takeIf { it.isNotBlank() } ?: (routeInfo?.operatingHours ?: "05:00 - 21:00")
    val frequency = routeSummary?.optString("frequency")?.takeIf { it.isNotBlank() } ?: (routeInfo?.interval ?: "10 - 15 phút")
    val fare = routeSummary?.optString("fare")?.takeIf { it.isNotBlank() } ?: (routeInfo?.fare ?: "10.000 đ")

    val forwardStops = remember(routeSummary) {
        val arr = routeSummary?.optJSONArray("forward_stops")
        if (arr == null) emptyList() else (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }
    val backwardStops = remember(routeSummary) {
        val arr = routeSummary?.optJSONArray("backward_stops")
        if (arr == null) emptyList() else (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
    }
    val activeStops = if (direction == "forward") forwardStops else backwardStops

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = OpenCityColors.CardBg,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = brandColor,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(56.dp)
                                .height(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = routeNum,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Tuyến $routeNum",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.TextMain
                            )
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                color = OpenCityColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = OpenCityColors.TextMuted)
                    }
                }

                HorizontalDivider(color = OpenCityColors.BorderColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OpenCityColors.SurfaceBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Giờ chạy", fontSize = 10.sp, color = OpenCityColors.TextMuted)
                        Text(formatBusHours(hours), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Giãn cách", fontSize = 10.sp, color = OpenCityColors.TextMuted)
                        Text(formatBusFrequency(frequency), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Giá vé", fontSize = 10.sp, color = OpenCityColors.TextMuted)
                        Text(formatBusFare(fare), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = brandColor)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Đơn vị", fontSize = 10.sp, color = OpenCityColors.TextMuted)
                        Text("Transerco", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextSecondary)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(36.dp)
                        .background(OpenCityColors.SurfaceBg, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (direction == "forward") brandColor else Color.Transparent)
                            .clickable { onDirectionChange("forward") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chiều đi (${forwardStops.size.takeIf { it > 0 } ?: 38} trạm)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (direction == "forward") Color.White else OpenCityColors.TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (direction == "backward") brandColor else Color.Transparent)
                            .clickable { onDirectionChange("backward") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chiều về (${backwardStops.size.takeIf { it > 0 } ?: 38} trạm)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (direction == "backward") Color.White else OpenCityColors.TextSecondary
                        )
                    }
                }

                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToBusNetwork()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(
                            text = "Mở toàn màn hình trong Mạng lưới Xe Buýt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (activeStops.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Đang tải dữ liệu trạm dừng tuyến...", fontSize = 13.sp, color = OpenCityColors.TextMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item {
                            Text(
                                text = if (direction == "forward") "Danh sách trạm đón/trả Chiều đi (chạm để xem vị trí):" else "Danh sách trạm đón/trả Chiều về (chạm để xem vị trí):",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.TextSecondary,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }

                        itemsIndexed(activeStops) { index, stop ->
                            val isFirst = index == 0
                            val isLast = index == activeStops.size - 1
                            val stopName = stop.optString("name", "Trạm đón/trả")
                            val address = stop.optString("address", "")
                            val stopLat = stop.optDouble("latitude").takeIf { it != 0.0 } ?: stop.optDouble("lat", 0.0)
                            val stopLon = stop.optDouble("longitude").takeIf { it != 0.0 } ?: stop.optDouble("lon", 0.0)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (stopLat != 0.0 && stopLon != 0.0) {
                                            onSelectStop(stopLat, stopLon, stopName)
                                        }
                                    }
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        color = if (isFirst) OpenCityColors.BrandMint else if (isLast) OpenCityColors.BrandRed else brandColor,
                                        shape = CircleShape,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    if (!isLast) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(28.dp)
                                                .background(OpenCityColors.BorderColor)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stopName,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OpenCityColors.TextMain
                                    )
                                    if (address.isNotBlank()) {
                                        Text(
                                            text = address,
                                            fontSize = 10.5.sp,
                                            color = OpenCityColors.TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    icon: ImageVector,
    isEnabled: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isEnabled) activeColor else Color.White.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, if (isEnabled) activeColor else Color(0xFFCBD5E1)),
        shadowElevation = if (isEnabled) 3.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isEnabled) Color.White else activeColor,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) Color.White else OpenCityColors.TextMain
            )
        }
    }
}

@Composable
private fun FloatingActionMapButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = OpenCityColors.BrandAzure,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GisInteractiveDetailCard(
    point: GisMapPoint,
    onClose: () -> Unit,
    onNavigateDetail: () -> Unit,
    onSelectBusRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        border = BorderStroke(1.5.dp, point.layer.borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                        color = point.layer.activeBg,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, point.layer.borderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = point.layer.icon,
                                contentDescription = null,
                                tint = point.layer.activeColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = point.layer.badgeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = point.layer.activeColor
                            )
                        }
                    }

                    if (point.layer == GisLayer.REPORTS && point.state.isNotBlank()) {
                        Surface(
                            color = OpenCityColors.BrandAzureLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = reportStates[point.state] ?: point.state,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.BrandAzureDark
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng thẻ",
                        tint = OpenCityColors.TextLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = point.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = OpenCityColors.TextMain
            )

            if (point.address.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = OpenCityColors.BrandAzure,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = point.address,
                        fontSize = 11.sp,
                        color = OpenCityColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            when (point.layer) {
                GisLayer.REPORTS -> {
                    if (point.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = point.subtitle,
                            fontSize = 11.sp,
                            color = OpenCityColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "⏱️ Tiến độ xử lý SLA:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                            Text(text = "Trong hạn giải quyết (Dưới 24h)", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateDetail,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Xem chi tiết hồ sơ phản ánh", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                }

                GisLayer.BUS_STOPS -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Các tuyến xe buýt đón/trả tại trạm (chạm để vẽ lộ trình trên bản đồ):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.TextLight
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        point.routes.forEach { r ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectBusRoute(r) },
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Tuyến $r ➔",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "🏛️ Tiện ích trạm:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                            Text(text = "Nhà chờ có mái che • Bảng LED thời gian thực", fontSize = 10.sp, color = OpenCityColors.TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val firstR = point.routes.firstOrNull()
                        if (firstR != null) {
                            Button(
                                onClick = { onSelectBusRoute(firstR) },
                                modifier = Modifier.weight(1.2f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(text = "Lộ trình Tuyến $firstR", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = onNavigateDetail,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF16A34A)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF16A34A))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(text = "Mạng lưới Bus", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                GisLayer.AQI_SENSORS -> {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val aqiVal = point.aqiValue
                        val (aqiBg, aqiTextColor) = aqiVal?.let(::getAqiColors)
                            ?: (Color(0xFFE2E8F0) to Color(0xFF475569))
                        Surface(
                            color = aqiBg,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(text = aqiVal?.toString() ?: "—", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = aqiTextColor)
                                Text(text = "AQI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = aqiTextColor)
                            }
                        }

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "Chất lượng:", fontSize = 11.sp, color = OpenCityColors.TextLight)
                                Text(text = point.aqiLabel.ifBlank { "Chưa có AQI" }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = aqiTextColor)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                point.pm25?.let { Text(text = "AQI PM2.5: $it", fontSize = 10.sp, color = OpenCityColors.TextSecondary) }
                                point.pm10?.let { Text(text = "AQI PM10: $it", fontSize = 10.sp, color = OpenCityColors.TextSecondary) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                point.temperature?.let { Text(text = "Nhiệt độ: $it°C", fontSize = 10.sp, color = OpenCityColors.TextLight) }
                                point.humidity?.let { Text(text = "Độ ẩm: $it%", fontSize = 10.sp, color = OpenCityColors.TextLight) }
                            }
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    val adviceText = when {
                        point.aqiValue == null -> "Trạm chưa công bố AQI hiện tại."
                        point.aqiValue <= 50 -> "Vùng không khí sạch. Rất thuận lợi cho mọi hoạt động thể chất ngoài trời."
                        point.aqiValue <= 100 -> "Không khí ở mức chấp nhận được. Nhóm cực kỳ nhạy cảm nên chú ý nếu thấy mệt."
                        else -> "Không khí kém. Người già, trẻ nhỏ và người có bệnh hô hấp nên đeo khẩu trang chống bụi PM2.5."
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(text = "💡 Khuyến cáo:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                            Text(text = adviceText, fontSize = 10.sp, color = Color(0xFF92400E), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onNavigateDetail,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Xem dự báo AQI & Phân tích chi tiết", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GisContextualDrawer(
    category: MapCategory,
    totalVisible: Int,
    points: List<GisMapPoint>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currentTab: String,
    onTabChange: (String) -> Unit,
    onPointClick: (GisMapPoint) -> Unit,
    onActionNavigate: ((String) -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(250)),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFCBD5E1))
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(category.themeColor)
                    )
                    val label = when (category) {
                        MapCategory.ALL -> "$totalVisible điểm trên bản đồ đa tầng"
                        MapCategory.REPORTS -> "$totalVisible phản ánh hiện trường & cảnh báo"
                        MapCategory.BUS -> "$totalVisible trạm đón/trả xe buýt trên mạng lưới"
                        MapCategory.AQI -> "$totalVisible vùng quan trắc chất lượng không khí"
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpenCityColors.TextMain
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Thu gọn" else "Xem danh sách",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = category.themeColor
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = category.themeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(Modifier.height(8.dp))

                if (category == MapCategory.ALL) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "all" to "Tất cả ($totalVisible)",
                            "reports" to "Phản ánh",
                            "bus_stops" to "Trạm buýt",
                            "aqi_sensors" to "Vùng AQI"
                        ).forEach { (key, tabTitle) ->
                            val isSel = currentTab == key
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onTabChange(key) },
                                color = if (isSel) category.themeColor else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = tabTitle,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else OpenCityColors.TextSecondary
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                val filteredList = remember(points, currentTab, category) {
                    val list = if (category == MapCategory.ALL) {
                        if (currentTab == "all") points else points.filter { it.layer.key == currentTab }
                    } else {
                        points
                    }
                    list.take(150)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(filteredList, key = { it.id }) { pt ->
                        DrawerPointRow(point = pt, onClick = { onPointClick(pt) })
                    }
                }

                when (category) {
                    MapCategory.BUS -> {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onActionNavigate?.invoke("find-bus") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF16A34A))
                        ) {
                            Icon(imageVector = Icons.Default.DirectionsBus, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(text = "Mở ứng dụng Tra cứu & Lộ trình xe buýt đầy đủ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                    }
                    MapCategory.AQI -> {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onActionNavigate?.invoke("air-quality") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFD97706))
                        ) {
                            Icon(imageVector = Icons.Default.Air, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(text = "Mở Bản tin & Dự báo Không khí chuyên sâu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                    MapCategory.REPORTS -> {
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onActionNavigate?.invoke("new-report") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF0284C7))
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(text = "Tạo phản ánh hiện trường mới tại đây", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                    }
                    MapCategory.ALL -> {}
                }
            }
        }
    }
}

@Composable
private fun DrawerPointRow(
    point: GisMapPoint,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable { onClick() },
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(point.layer.activeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = point.layer.icon,
                    contentDescription = null,
                    tint = point.layer.activeColor,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = point.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (point.address.isNotBlank()) {
                    Text(
                        text = point.address,
                        fontSize = 10.sp,
                        color = OpenCityColors.TextLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (point.routes.isNotEmpty()) {
                    Text(
                        text = "Tuyến: ${point.routes.joinToString(", ")}",
                        fontSize = 9.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (point.layer == GisLayer.AQI_SENSORS && point.aqiValue != null) {
                val (bg, textColor) = getAqiColors(point.aqiValue)
                Surface(
                    color = bg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "AQI ${point.aqiValue}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor
                    )
                }
            } else if (point.layer == GisLayer.REPORTS && point.state.isNotBlank()) {
                Surface(
                    color = OpenCityColors.BrandAzureLight,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = reportStates[point.state] ?: point.state,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.BrandAzureDark
                    )
                }
            }
        }
    }
}

private fun getAqiColors(aqi: Int): Pair<Color, Color> {
    return when {
        aqi <= 50 -> Color(0xFFDCFCE7) to Color(0xFF15803D)      // Xanh lá - Tốt
        aqi <= 100 -> Color(0xFFFEF9C3) to Color(0xFFA16207)     // Vàng - Vừa phải
        aqi <= 150 -> Color(0xFFFFEDD5) to Color(0xFFC2410C)     // Cam - Kém
        aqi <= 200 -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)     // Đỏ - Xấu
        else -> Color(0xFFF3E8FF) to Color(0xFF6B21A8)           // Tím - Nguy hại
    }
}

private fun generateCircleCoords(
    lng: Double,
    lat: Double,
    radiusKm: Double = 1.8,
    numPoints: Int = 36
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
    aqiPoints: List<GisMapPoint>,
    selectedId: String? = null
): String {
    val root = JSONObject()
    root.put("type", "FeatureCollection")
    val features = JSONArray()

    aqiPoints.forEach { pt ->
        val aqi = pt.aqiValue ?: return@forEach
        val isSelected = selectedId == pt.id
        val color = when {
            aqi <= 50 -> "#22c55e"
            aqi <= 100 -> "#eab308"
            aqi <= 150 -> "#f97316"
            else -> "#ef4444"
        }

        val feature = JSONObject()
        feature.put("type", "Feature")

        val props = JSONObject()
        props.put("id", pt.id)
        props.put("color", color)
        props.put("fillOpacity", if (isSelected) 0.28 else 0.13)
        props.put("lineWidth", if (isSelected) 3.0 else 1.8)
        props.put("lineOpacity", 0.85)
        feature.put("properties", props)

        val geometry = JSONObject()
        geometry.put("type", "Polygon")
        val ring = JSONArray()
        val circleRing = generateCircleCoords(pt.lon, pt.lat, radiusKm = if (isSelected) 2.2 else 1.8)
        circleRing.forEach { (lng, ltt) ->
            val p = JSONArray()
            p.put(lng)
            p.put(ltt)
            ring.put(p)
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

private fun buildBusRouteGeoJson(coords: List<LatLng>, hexColor: String): String {
    if (coords.isEmpty()) {
        return "{\"type\":\"FeatureCollection\",\"features\":[]}"
    }
    val root = JSONObject()
    root.put("type", "FeatureCollection")
    val features = JSONArray()

    val feature = JSONObject()
    feature.put("type", "Feature")

    val props = JSONObject()
    props.put("color", hexColor)
    feature.put("properties", props)

    val geometry = JSONObject()
    geometry.put("type", "LineString")
    val coordsArr = JSONArray()
    for (c in coords) {
        val pt = JSONArray()
        pt.put(c.longitude)
        pt.put(c.latitude)
        coordsArr.put(pt)
    }
    geometry.put("coordinates", coordsArr)
    feature.put("geometry", geometry)
    features.put(feature)

    root.put("features", features)
    return root.toString()
}

private data class GisCluster(
    val centerLat: Double,
    val centerLon: Double,
    val points: List<GisMapPoint>
) {
    val isSingle: Boolean get() = points.size == 1
    val singlePoint: GisMapPoint get() = points.first()
    val count: Int get() = points.size
    val layer: GisLayer get() = points.first().layer
    val hasAlert: Boolean get() = points.any { it.isAlert }
}

private fun projectToPixel(lat: Double, lng: Double, scale: Double): Pair<Double, Double> {
    val x = (lng + 180.0) / 360.0 * scale
    val sinY = Math.sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
    val y = (0.5 - Math.log((1.0 + sinY) / (1.0 - sinY)) / (4.0 * Math.PI)) * scale
    return Pair(x, y)
}

private fun clusterGisPoints(points: List<GisMapPoint>, zoom: Double): List<GisCluster> {
    if (points.isEmpty()) return emptyList()

    if (zoom >= 14.0) {
        return points.map { GisCluster(it.lat, it.lon, listOf(it)) }
    }

    val clusterRadiusPx = when {
        zoom < 11.0 -> 55.0
        zoom < 12.5 -> 44.0
        zoom < 13.5 -> 36.0
        else -> 28.0
    }
    val clusterRadiusPx2 = clusterRadiusPx * clusterRadiusPx
    val scale = 256.0 * Math.pow(2.0, zoom)
    val result = mutableListOf<GisCluster>()
    val groupedByLayer = points.groupBy { it.layer }

    for ((layer, layerPoints) in groupedByLayer) {
        if (layer == GisLayer.AQI_SENSORS) {
            // Giữ từng trạm quan trắc trên bản đồ khi zoom xa để hiển thị đúng chỉ số của từng điểm.
            for (pt in layerPoints) {
                result.add(GisCluster(pt.lat, pt.lon, listOf(pt)))
            }
            continue
        }

        val clusters = mutableListOf<MutableList<GisMapPoint>>()
        val clusterCenters = mutableListOf<Pair<Double, Double>>()

        for (pt in layerPoints) {
            val (px, py) = projectToPixel(pt.lat, pt.lon, scale)
            var matchedIndex = -1
            var minD2 = Double.MAX_VALUE

            for (i in clusterCenters.indices) {
                val (cx, cy) = clusterCenters[i]
                val d2 = (px - cx) * (px - cx) + (py - cy) * (py - cy)
                if (d2 <= clusterRadiusPx2 && d2 < minD2) {
                    minD2 = d2
                    matchedIndex = i
                }
            }

            if (matchedIndex != -1) {
                clusters[matchedIndex].add(pt)
            } else {
                clusters.add(mutableListOf(pt))
                clusterCenters.add(Pair(px, py))
            }
        }

        for (group in clusters) {
            val avgLat = group.sumOf { it.lat } / group.size
            val avgLon = group.sumOf { it.lon } / group.size
            result.add(GisCluster(avgLat, avgLon, group))
        }
    }

    return result
}

@Composable
private fun GisMapLibreView(
    points: List<GisMapPoint>,
    selectedPoint: GisMapPoint?,
    showAqiZones: Boolean,
    routeCoordinates: List<LatLng> = emptyList(),
    routeColorHex: String = "#16A34A",
    onMapReady: (MapLibreMap) -> Unit,
    onMarkerClick: (GisMapPoint) -> Unit,
    onCameraMoved: ((Double, Double, Double) -> Unit)? = null,
    modifier: Modifier
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentZoom by remember { mutableDoubleStateOf(12.0) }
    var currentBounds by remember { mutableStateOf<org.maplibre.android.geometry.LatLngBounds?>(null) }
    val markerPointMap = remember { mutableMapOf<Long, GisMapPoint>() }
    val markerClusterMap = remember { mutableMapOf<Long, GisCluster>() }

    fun updateAqiZonesLayer(style: Style) {
        val source = style.getSourceAs<GeoJsonSource>("aqi-zones") ?: return
        val aqiList = if (showAqiZones) points.filter { it.layer == GisLayer.AQI_SENSORS } else emptyList()
        val newGeoJson = if (aqiList.isNotEmpty()) {
            buildAqiZonesGeoJson(aqiList, selectedPoint?.id)
        } else {
            "{\"type\":\"FeatureCollection\",\"features\":[]}"
        }
        source.setGeoJson(newGeoJson)
    }

    fun updateBusRouteLayer(style: Style) {
        val source = style.getSourceAs<GeoJsonSource>("bus-route-line") ?: return
        val newGeoJson = if (routeCoordinates.isNotEmpty()) {
            buildBusRouteGeoJson(routeCoordinates, routeColorHex)
        } else {
            "{\"type\":\"FeatureCollection\",\"features\":[]}"
        }
        source.setGeoJson(newGeoJson)
    }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    ) { mv ->
        if (mapLibreMap == null) {
            mv.getMapAsync { map ->
                mapLibreMap = map
                onMapReady(map)

                map.setStyle(Style.Builder().fromUri(MAP_STYLE_POSITRON)) { style ->
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(21.0285, 105.8542))
                        .zoom(12.0)
                        .build()
                    currentZoom = 12.0

                    // Cấu hình Lớp Vùng AQI GeoJSON chuẩn như AirQualityScreen (Đường nét đứt, độ mờ 0.13)
                    val aqiSource = GeoJsonSource("aqi-zones", "{\"type\":\"FeatureCollection\",\"features\":[]}")
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

                    val busRouteSource = GeoJsonSource("bus-route-line", "{\"type\":\"FeatureCollection\",\"features\":[]}")
                    style.addSource(busRouteSource)

                    val busRouteCasing = LineLayer("bus-route-line-casing", "bus-route-line").apply {
                        setProperties(
                            PropertyFactory.lineColor(android.graphics.Color.WHITE),
                            PropertyFactory.lineWidth(7.0f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            PropertyFactory.lineOpacity(0.95f)
                        )
                    }
                    style.addLayer(busRouteCasing)

                    val busRouteLayer = LineLayer("bus-route-line-layer", "bus-route-line").apply {
                        setProperties(
                            PropertyFactory.lineColor(Expression.get("color")),
                            PropertyFactory.lineWidth(4.5f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                            PropertyFactory.lineOpacity(0.98f)
                        )
                    }
                    style.addLayer(busRouteLayer)

                    updateAqiZonesLayer(style)
                    updateBusRouteLayer(style)

                    currentBounds = map.projection.visibleRegion.latLngBounds
                }

                map.addOnCameraIdleListener {
                    currentZoom = map.cameraPosition.zoom
                    currentBounds = map.projection.visibleRegion.latLngBounds
                    val target = map.cameraPosition.target
                    if (target != null) {
                        onCameraMoved?.invoke(target.latitude, target.longitude, map.cameraPosition.zoom)
                    }
                }

                map.setOnMarkerClickListener { marker ->
                    val singlePt = markerPointMap[marker.id]
                    val cluster = markerClusterMap[marker.id]
                    if (singlePt != null) {
                        onMarkerClick(singlePt)
                        true
                    } else if (cluster != null && !cluster.isSingle) {
                        val nextZoom = (map.cameraPosition.zoom + 2.3).coerceAtMost(16.5)
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(cluster.centerLat, cluster.centerLon),
                                nextZoom
                            ),
                            350
                        )
                        true
                    } else {
                        false
                    }
                }

                map.addOnMapClickListener { latLng ->
                    val screenPoint = map.projection.toScreenLocation(latLng)
                    val features = map.queryRenderedFeatures(screenPoint, "aqi-zones-fill")
                    if (features.isNotEmpty()) {
                        val id = features[0].getStringProperty("id")
                        val found = points.find { it.id == id }
                        if (found != null) {
                            onMarkerClick(found)
                            return@addOnMapClickListener true
                        }
                    }
                    false
                }
            }
        }
    }

    val zoomBucket = remember(currentZoom) {
        (Math.round(currentZoom * 2.0) / 2.0)
    }

    LaunchedEffect(showAqiZones, selectedPoint, points) {
        val style = mapLibreMap?.style
        if (style != null && style.isFullyLoaded) {
            updateAqiZonesLayer(style)
        } else {
            mapLibreMap?.getStyle { s ->
                updateAqiZonesLayer(s)
            }
        }
    }

    LaunchedEffect(routeCoordinates, routeColorHex) {
        val style = mapLibreMap?.style
        if (style != null && style.isFullyLoaded) {
            updateBusRouteLayer(style)
        } else {
            mapLibreMap?.getStyle { s ->
                updateBusRouteLayer(s)
            }
        }
    }

    val isRouteActive = routeCoordinates.isNotEmpty()
    val allClusters = remember(points, zoomBucket, isRouteActive) {
        if (isRouteActive && currentZoom >= 9.5) {
            points.map { GisCluster(it.lat, it.lon, listOf(it)) }
        } else {
            clusterGisPoints(points, currentZoom)
        }
    }

    val visibleClusters = remember(allClusters, currentBounds) {
        val bounds = currentBounds
        if (allClusters.size <= 80 || bounds == null) {
            allClusters
        } else {
            val latMargin = (bounds.latitudeNorth - bounds.latitudeSouth) * 0.35
            val lonMargin = (bounds.longitudeEast - bounds.longitudeWest) * 0.35
            val minLat = bounds.latitudeSouth - latMargin
            val maxLat = bounds.latitudeNorth + latMargin
            val minLon = bounds.longitudeWest - lonMargin
            val maxLon = bounds.longitudeEast + lonMargin
            allClusters.filter { cl ->
                cl.centerLat in minLat..maxLat && cl.centerLon in minLon..maxLon
            }
        }
    }

    LaunchedEffect(visibleClusters, mapLibreMap, routeCoordinates, routeColorHex) {
        val map = mapLibreMap ?: return@LaunchedEffect

        map.clear()
        markerPointMap.clear()
        markerClusterMap.clear()

        if (routeCoordinates.size >= 2) {
            val casingColor = android.graphics.Color.WHITE
            val routeColor = try {
                android.graphics.Color.parseColor(routeColorHex)
            } catch (_: Exception) {
                android.graphics.Color.parseColor("#0284C7")
            }
            map.addPolyline(
                PolylineOptions()
                    .addAll(routeCoordinates)
                    .color(casingColor)
                    .width(8.5f)
            )
            map.addPolyline(
                PolylineOptions()
                    .addAll(routeCoordinates)
                    .color(routeColor)
                    .width(5.5f)
            )
        }

        val iconFactory = IconFactory.getInstance(context)

        visibleClusters.forEach { cluster ->
            val iconBitmap = if (cluster.isSingle) {
                val pt = cluster.singlePoint
                when (pt.layer) {
                    GisLayer.REPORTS -> createReportMarkerBitmap(context, pt.isAlert)
                    GisLayer.BUS_STOPS -> {
                        val isOrigin = isRouteActive && points.firstOrNull()?.id == pt.id
                        val isDest = isRouteActive && points.size > 1 && points.lastOrNull()?.id == pt.id
                        createBusMarkerBitmap(context, isOrigin = isOrigin, isDestination = isDest)
                    }
                    GisLayer.AQI_SENSORS -> createAqiMarkerBitmap(context, pt.aqiValue)
                }
            } else {
                createClusterMarkerBitmap(context, cluster.count, cluster.layer, cluster.hasAlert)
            }

            val icon = iconFactory.fromBitmap(iconBitmap)
            val title = if (cluster.isSingle) cluster.singlePoint.title else "Cụm ${cluster.count} điểm ${cluster.layer.title}"
            val snippet = if (cluster.isSingle) cluster.singlePoint.subtitle else "Chạm để phóng to xem ${cluster.count} điểm chi tiết"

            val markerOptions = MarkerOptions()
                .position(LatLng(cluster.centerLat, cluster.centerLon))
                .title(title)
                .snippet(snippet)
                .icon(icon)

            val marker = map.addMarker(markerOptions)
            if (marker != null) {
                markerClusterMap[marker.id] = cluster
                if (cluster.isSingle) {
                    markerPointMap[marker.id] = cluster.singlePoint
                }
            }
        }
    }

    LaunchedEffect(selectedPoint) {
        val map = mapLibreMap ?: return@LaunchedEffect
        selectedPoint?.let { pt ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(pt.lat, pt.lon), 14.5), 350)
        }
    }
}

private fun createReportMarkerBitmap(context: Context, isAlert: Boolean): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (30 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(center, center, center - (1 * density), paint)

    paint.color = if (isAlert) android.graphics.Color.parseColor("#DC2626") else android.graphics.Color.parseColor("#EF4444")
    canvas.drawCircle(center, center, center - (3 * density), paint)

    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(center, center, 4.5f * density, paint)

    return bitmap
}

private fun createBusMarkerBitmap(
    context: Context,
    isOrigin: Boolean = false,
    isDestination: Boolean = false
): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (30 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x2E000000
    }
    canvas.drawCircle(center, center + 1.2f * density, 12f * density, shadowPaint)

    val bgColor = when {
        isOrigin -> 0xFF059669.toInt()
        isDestination -> 0xFFDC2626.toInt()
        else -> 0xFF1D4ED8.toInt()
    }

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = bgColor
    }
    canvas.drawCircle(center, center, 11f * density, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 11f * density, strokePaint)

    if (isOrigin || isDestination) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 12f * density
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val badge = if (isOrigin) "A" else "B"
        val yPos = center - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(badge, center, yPos, textPaint)
    } else {
        drawBusSilhouette(canvas, center, center, density, bgColor)
    }

    return bitmap
}

private fun drawBusSilhouette(canvas: Canvas, cx: Float, cy: Float, density: Float, windowColor: Int) {
    val busPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = windowColor
        style = Paint.Style.FILL
    }

    val bW = 12f * density
    val bH = 13.5f * density
    val bL = cx - bW / 2f
    val bT = cy - bH / 2f
    val bR = cx + bW / 2f
    val bB = cy + bH / 2f - 1.2f * density
    val corner = 2f * density

    val body = RectF(bL, bT, bR, bB)
    canvas.drawRoundRect(body, corner, corner, busPaint)

    val wheelR = 1.1f * density
    canvas.drawCircle(bL + bW * 0.25f, bB + wheelR * 0.6f, wheelR, busPaint)
    canvas.drawCircle(bR - bW * 0.25f, bB + wheelR * 0.6f, wheelR, busPaint)

    val winW = bW * 0.74f
    val winH = bH * 0.35f
    val winRect = RectF(cx - winW / 2f, bT + 1.5f * density, cx + winW / 2f, bT + 1.5f * density + winH)
    canvas.drawRoundRect(winRect, 1.2f * density, 1.2f * density, winPaint)

    val headLightR = 0.7f * density
    val lightY = bB - 1.8f * density
    canvas.drawCircle(bL + bW * 0.24f, lightY, headLightR, winPaint)
    canvas.drawCircle(bR - bW * 0.24f, lightY, headLightR, winPaint)
}

private fun createAqiMarkerBitmap(context: Context, aqi: Int?): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (38 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val color = when {
        aqi == null -> android.graphics.Color.parseColor("#64748b")
        aqi <= 50 -> android.graphics.Color.parseColor("#16a34a")
        aqi <= 100 -> android.graphics.Color.parseColor("#d97706")
        aqi <= 150 -> android.graphics.Color.parseColor("#ea580c")
        else -> android.graphics.Color.parseColor("#dc2626")
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = 0x33000000
    }

    val radius = size / 2f
    canvas.drawCircle(radius, radius + 2 * density, radius - 2 * density, shadowPaint)
    canvas.drawCircle(radius, radius, radius - 2 * density, paint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        this.color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(radius, radius, radius - 3.5f * density, strokePaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        textSize = 13f * density
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val yPos = radius - (textPaint.descent() + textPaint.ascent()) / 2
    canvas.drawText(aqi?.toString() ?: "—", radius, yPos, textPaint)

    return bitmap
}

private fun createClusterMarkerBitmap(
    context: Context,
    count: Int,
    layer: GisLayer,
    hasAlert: Boolean
): Bitmap {
    val density = context.resources.displayMetrics.density
    val (radiusDp, textSizeSp) = when {
        count < 15 -> 18f to 12f
        count < 40 -> 22f to 13f
        count < 100 -> 25f to 14f
        else -> 28f to 15f
    }

    val totalDiameter = ((radiusDp * 2 + 8) * density).toInt()
    val bitmap = Bitmap.createBitmap(totalDiameter, totalDiameter, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = totalDiameter / 2f
    val r = radiusDp * density

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33000000
    }
    canvas.drawCircle(center, center + 1.8f * density, r, shadowPaint)

    val bgColor = when {
        hasAlert -> {
            when {
                count < 15 -> 0xFFF87171.toInt()
                count < 40 -> 0xFFEF4444.toInt()
                else -> 0xFFDC2626.toInt()
            }
        }
        layer == GisLayer.BUS_STOPS -> {
            when {
                count < 15 -> 0xFF0284C7.toInt()
                count < 40 -> 0xFF2563EB.toInt()
                count < 100 -> 0xFF1D4ED8.toInt()
                else -> 0xFF1E40AF.toInt()
            }
        }
        layer == GisLayer.AQI_SENSORS -> {
            when {
                count < 15 -> 0xFFF59E0B.toInt()
                count < 40 -> 0xFFD97706.toInt()
                else -> 0xFFEA580C.toInt()
            }
        }
        else -> {
            when {
                count < 15 -> 0xFF0284C7.toInt()
                count < 40 -> 0xFF2563EB.toInt()
                count < 100 -> 0xFF4F46E5.toInt()
                else -> 0xFF4338CA.toInt()
            }
        }
    }
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = bgColor
    }
    canvas.drawCircle(center, center, r, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.8f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, r, strokePaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = textSizeSp * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textY = center - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(count.toString(), center, textY, textPaint)

    return bitmap
}

private fun JSONObject.asReportPoint(): GisMapPoint? {
    val id = optString("id", "")
    if (id.isBlank()) return null
    val rawLat = optDouble("latitude", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
        ?: return null
    val rawLon = optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("lon", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("lng", Double.NaN).takeIf { !it.isNaN() }
        ?: return null

    // Phân tán bán kính nhẹ xác định cho các điểm trùng tọa độ chính xác
    val hash = kotlin.math.abs(id.hashCode())
    val angle = (hash % 12) * (2.0 * Math.PI / 12.0)
    val distanceDeg = ((hash / 12) % 4) * 0.00025
    val lat = rawLat + distanceDeg * kotlin.math.cos(angle)
    val lon = rawLon + distanceDeg * kotlin.math.sin(angle)

    return GisMapPoint(
        id = id,
        layer = GisLayer.REPORTS,
        title = optString("title", "Phản ánh hiện trường"),
        subtitle = optString("description", optString("category", "Hạ tầng")),
        lat = lat,
        lon = lon,
        state = optString("state", "submitted"),
        isAlert = false,
        address = optString("address", optString("location", "Hà Nội")),
        category = optString("category", "Hạ tầng")
    )
}

private fun JSONObject.asBulletinPoint(): GisMapPoint? {
    val id = optString("id", "")
    if (id.isBlank()) return null
    val rawLat = optDouble("latitude", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
        ?: return null
    val rawLon = optDouble("longitude", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("lon", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("lng", Double.NaN).takeIf { !it.isNaN() }
        ?: return null

    // Với các cảnh báo có cùng tọa độ trong một khu vực (ví dụ các bản tin cùng phát tại KCN Hòa Lạc hoặc cùng trạm),
    // áp dụng phân tán bán kính nhẹ xác định (~70m - 250m) theo id.hashCode()
    // giúp các marker không bị đè khít lên nhau khi zoom gần (zoom >= 14), hiển thị tách bạch từng điểm để người dùng chọn xem chi tiết
    val hash = kotlin.math.abs(id.hashCode())
    val angle = (hash % 16) * (2.0 * Math.PI / 16.0)
    val distanceDeg = 0.0006 + ((hash / 16) % 5) * 0.00035 // ~70m - 250m
    val lat = rawLat + distanceDeg * kotlin.math.cos(angle)
    val lon = rawLon + distanceDeg * kotlin.math.sin(angle)

    val title = optString("title", "Cảnh báo khẩn cấp")
    val summary = optString("summary", optString("body", optString("content", "")))
    val address = optString("area", optString("location", "Hà Nội"))

    return GisMapPoint(
        id = id,
        layer = GisLayer.REPORTS,
        title = title,
        subtitle = summary,
        lat = lat,
        lon = lon,
        state = "alert",
        isAlert = true,
        address = address,
        category = "Cảnh báo"
    )
}

private fun JSONObject.asBusStopPoint(): GisMapPoint? {
    val id = optString("id", "")
    if (id.isBlank()) return null
    val lat = optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("latitude", Double.NaN).takeIf { !it.isNaN() } ?: return null
    val lon = optDouble("lon", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("lng", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("longitude", Double.NaN).takeIf { !it.isNaN() } ?: return null

    val routesArray = optJSONArray("routes")
    val routeList = mutableListOf<String>()
    if (routesArray != null) {
        for (i in 0 until routesArray.length()) {
            val r = routesArray.optString(i)
            if (r.isNotBlank()) routeList.add(r)
        }
    }

    return GisMapPoint(
        id = id,
        layer = GisLayer.BUS_STOPS,
        title = optString("name", "Trạm xe buýt"),
        subtitle = optString("description", "Điểm đón/trả khách"),
        lat = lat,
        lon = lon,
        routes = routeList,
        address = optString("address", "")
    )
}

private fun JSONObject.asAqiPoint(): GisMapPoint? {
    val id = optString("id", optString("code", optString("station_code", "")))
    if (id.isBlank()) return null
    val lat = optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("latitude", Double.NaN).takeIf { !it.isNaN() } ?: return null
    val lon = optDouble("lon", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("lng", Double.NaN).takeIf { !it.isNaN() }
        ?: optDouble("longitude", Double.NaN).takeIf { !it.isNaN() } ?: return null

    val latest = optJSONObject("latest_reading")
    val classification = optJSONObject("classification")

    val aqi = latest?.optInt("aqi") ?: if (has("aqi") && !isNull("aqi")) optInt("aqi") else null
    val level = classification?.optString("level") ?: optString("level", "unknown")
    val label = classification?.optString("label") ?: optString("label", "Chưa có AQI")

    val pm25Val = latest?.optDouble("pm25", Double.NaN)?.takeIf { !it.isNaN() }
        ?: if (has("pm25") && !isNull("pm25")) optDouble("pm25") else null
    val pm10Val = latest?.optDouble("pm10", Double.NaN)?.takeIf { !it.isNaN() }
        ?: if (has("pm10") && !isNull("pm10")) optDouble("pm10") else null
    val tempVal = latest?.optDouble("temperature", Double.NaN)?.takeIf { !it.isNaN() }
        ?: if (has("temperature") && !isNull("temperature")) optDouble("temperature") else null
    val humVal = latest?.optDouble("humidity", Double.NaN)?.takeIf { !it.isNaN() }
        ?: if (has("humidity") && !isNull("humidity")) optDouble("humidity") else null

    return GisMapPoint(
        id = id,
        layer = GisLayer.AQI_SENSORS,
        title = optString("station_name", optString("name", "Trạm quan trắc")),
        subtitle = optString("district", optString("area_type", "Hà Nội")),
        lat = lat,
        lon = lon,
        aqiValue = aqi,
        aqiLevel = level,
        aqiLabel = label,
        address = optString("address", optString("location", "")),
        pm25 = pm25Val,
        pm10 = pm10Val,
        temperature = tempVal,
        humidity = humVal
    )
}

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
