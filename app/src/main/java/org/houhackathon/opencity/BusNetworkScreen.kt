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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlin.coroutines.resume
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.Locale

// JSON Helper
private fun JSONObject.text(key: String, fallback: String = ""): String =
    if (has(key) && !isNull(key)) optString(key, fallback) else fallback

private fun JSONObject.intOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

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

private fun parseHexColor(hex: String, fallback: Color = OpenCityColors.BrandAzure): Color {
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

@Composable
fun BusNetworkScreen(
    api: ApiClient,
    revision: Int
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var routes by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var stops by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var auditReport by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var isOffline by remember { mutableStateOf(false) }

    var userCoords by remember { mutableStateOf(21.0285 to 105.8542) }
    var isGpsLocked by remember { mutableStateOf(false) }
    var isLocating by remember { mutableStateOf(false) }

    var currentTab by remember { mutableStateOf("routes") } // "routes" | "stops" | "map"
    var selectedFilter by remember { mutableStateOf("all") } // "all" | "standard" | "electric" | "brt"
    var searchQuery by remember { mutableStateOf("") }

    var selectedRadiusKm by remember { mutableFloatStateOf(3.0f) }
    var stopSearchQuery by remember { mutableStateOf("") }
    var loadingStops by remember { mutableStateOf(false) }

    var selectedRouteForDetail by remember { mutableStateOf<JSONObject?>(null) }
    var selectedStopOnMap by remember { mutableStateOf<JSONObject?>(null) }

    val loadStops: (Double, Double, Float, String) -> Unit = { lat, lng, radius, query ->
        scope.launch {
            loadingStops = true
            try {
                val url = if (query.isNotBlank()) {
                    val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
                    "/bus/stops?keyword=$encoded"
                } else if (radius >= 25.0f) {
                    "/bus/stops?radius_km=30.0&lat=$lat&lng=$lng"
                } else {
                    "/bus/stops?lat=$lat&lng=$lng&radius_km=$radius"
                }
                val sList = api.list(url)
                stops = sList
            } catch (_: Exception) {
            }
            loadingStops = false
        }
    }

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
                    loadStops(loc.first, loc.second, selectedRadiusKm, stopSearchQuery)
                }
                isLocating = false
            }
        }
    }

    val loadData = {
        scope.launch {
            loading = true
            try {
                val rList = api.list("/bus/routes")
                routes = rList
                isOffline = false
            } catch (_: Exception) {
                routes = emptyList()
                isOffline = true
            }

            loadStops(userCoords.first, userCoords.second, selectedRadiusKm, stopSearchQuery)

            try {
                auditReport = api.item("/bus/audit")
            } catch (_: Exception) {}

            loading = false
        }
    }

    LaunchedEffect(revision) {
        loadData()
        scope.launch {
            val loc = fetchCurrentLocation(context)
            if (loc != null) {
                userCoords = loc
                isGpsLocked = true
                loadStops(loc.first, loc.second, selectedRadiusKm, stopSearchQuery)
            }
        }
    }

    val filteredRoutes = remember(routes, selectedFilter, searchQuery) {
        routes.filter { r ->
            val matchFilter = when (selectedFilter) {
                "standard" -> r.text("route_type") == "standard"
                "electric" -> r.text("route_type") == "electric"
                "brt" -> r.text("route_type") == "brt"
                else -> true
            }
            val matchSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                r.text("route_number").lowercase().contains(q) ||
                        r.text("name").lowercase().contains(q) ||
                        r.text("operator").lowercase().contains(q)
            }
            matchFilter && matchSearch
        }
    }

    val stopsWithDistance = remember(stops, userCoords) {
        stops.map { st ->
            val lat = st.doubleOrNull("latitude") ?: userCoords.first
            val lon = st.doubleOrNull("longitude") ?: userCoords.second
            val dist = calcDistanceKm(userCoords.first, userCoords.second, lat, lon)
            st to dist
        }.sortedBy { it.second }
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
                // Location Bar
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
                                text = if (isLocating) "Đang tìm vệ tinh GPS..."
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
                    BusTabButton(
                        title = if (isOffline && routes.isEmpty()) "Tuyến xe (Ngoại tuyến)" else "Tuyến xe (${routes.size})",
                        icon = Icons.Default.DirectionsBus,
                        isSelected = currentTab == "routes",
                        modifier = Modifier.weight(1.1f),
                        onClick = { currentTab = "routes" }
                    )
                    BusTabButton(
                        title = "Trạm gần bạn",
                        icon = Icons.Default.PinDrop,
                        isSelected = currentTab == "stops",
                        modifier = Modifier.weight(1f),
                        onClick = { currentTab = "stops" }
                    )
                    BusTabButton(
                        title = "Bản đồ",
                        icon = Icons.Default.Map,
                        isSelected = currentTab == "map",
                        modifier = Modifier.weight(0.9f),
                        onClick = { currentTab = "map" }
                    )
                }

                if (isOffline) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = OpenCityColors.BrandAmberLight,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, OpenCityColors.BrandAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = OpenCityColors.BrandAmberDark,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Đang ngoại tuyến • Mất kết nối máy chủ",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OpenCityColors.BrandAmberDark
                                )
                            }
                            Surface(
                                color = OpenCityColors.BrandAmber,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { loadData() }
                            ) {
                                Text(
                                    text = "Thử lại",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (loading && routes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OpenCityColors.BrandAzure)
            }
        } else if (currentTab == "map") {
            BusMapView(
                stops = stopsWithDistance.map { it.first },
                userCoords = userCoords,
                selectedRadius = selectedRadiusKm,
                onSelectRadius = { r ->
                    selectedRadiusKm = r
                    stopSearchQuery = ""
                    loadStops(userCoords.first, userCoords.second, r, "")
                },
                selectedStop = selectedStopOnMap,
                onSelectStop = { selectedStopOnMap = it },
                onSelectRouteFromStop = { routeNumber ->
                    val r = routes.find { it.text("route_number").equals(routeNumber, ignoreCase = true) }
                    if (r != null) {
                        selectedRouteForDetail = r
                    }
                }
            )
        } else if (currentTab == "stops") {
            NearbyStopsTab(
                stopsWithDist = stopsWithDistance,
                searchQuery = stopSearchQuery,
                isOffline = isOffline,
                onRetry = { loadData() },
                onSearchChange = { query ->
                    stopSearchQuery = query
                    loadStops(userCoords.first, userCoords.second, selectedRadiusKm, query)
                },
                selectedRadius = selectedRadiusKm,
                onSelectRadius = { r ->
                    selectedRadiusKm = r
                    stopSearchQuery = ""
                    loadStops(userCoords.first, userCoords.second, r, "")
                },
                loadingStops = loadingStops,
                onSelectStop = { selectedStopOnMap = it; currentTab = "map" },
                onSelectRoute = { routeNum ->
                    val r = routes.find { it.text("route_number").equals(routeNum, ignoreCase = true) }
                    if (r != null) {
                        selectedRouteForDetail = r
                    }
                }
            )
        } else {
            BusRoutesTab(
                routes = filteredRoutes,
                allRoutes = routes,
                selectedFilter = selectedFilter,
                searchQuery = searchQuery,
                isOffline = isOffline,
                onRetry = { loadData() },
                onFilterChange = { selectedFilter = it },
                onSearchChange = { searchQuery = it },
                onSelectRoute = { selectedRouteForDetail = it }
            )
        }
    }

    selectedRouteForDetail?.let { route ->
        BusRouteDetailDialog(
            api = api,
            routeSummary = route,
            onDismiss = { selectedRouteForDetail = null }
        )
    }
}

@Composable
private fun BusTabButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) OpenCityColors.BrandAzure else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else OpenCityColors.TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else OpenCityColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}

// ds tuyến
@Composable
private fun BusRoutesTab(
    routes: List<JSONObject>,
    allRoutes: List<JSONObject>,
    selectedFilter: String,
    searchQuery: String,
    isOffline: Boolean = false,
    onRetry: () -> Unit = {},
    onFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onSelectRoute: (JSONObject) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Tìm số tuyến (01, E02, BRT), bến xe, điểm đến...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = OpenCityColors.TextMuted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Xóa", tint = OpenCityColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = OpenCityColors.CardBg,
                unfocusedContainerColor = OpenCityColors.CardBg,
                focusedBorderColor = OpenCityColors.BrandAzure,
                unfocusedBorderColor = OpenCityColors.BorderColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        val standardCount = allRoutes.count { it.text("route_type") == "standard" }
        val electricCount = allRoutes.count { it.text("route_type") == "electric" }
        val brtCount = allRoutes.count { it.text("route_type") == "brt" }

        val filterOptions = listOf(
            "all" to "Tất cả (${allRoutes.size})",
            "standard" to "Buýt thường ($standardCount)",
            "electric" to "⚡ VinBus điện ($electricCount)",
            "brt" to "BRT 01 ($brtCount)"
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterOptions) { (key, label) ->
                val isSelected = selectedFilter == key
                Surface(
                    color = if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.CardBg,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.BorderColor),
                    modifier = Modifier.clickable { onFilterChange(key) }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else OpenCityColors.TextMain,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            color = if (isOffline && allRoutes.isEmpty()) OpenCityColors.BrandAmberLight else OpenCityColors.BrandAzureLight,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (isOffline && allRoutes.isEmpty()) OpenCityColors.BrandAmber.copy(alpha = 0.5f) else OpenCityColors.BrandAzureBorder.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isOffline && allRoutes.isEmpty()) Icons.Default.CloudOff else Icons.Default.DirectionsBus,
                    contentDescription = null,
                    tint = if (isOffline && allRoutes.isEmpty()) OpenCityColors.BrandAmberDark else OpenCityColors.BrandAzure,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isOffline && allRoutes.isEmpty()) "Chế độ ngoại tuyến • Chưa thể kết nối máy chủ để đồng bộ tuyến xe"
                           else "Mạng lưới xe buýt Hà Nội • ${allRoutes.size} tuyến • 5.052 trạm dừng 2 chiều",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOffline && allRoutes.isEmpty()) OpenCityColors.BrandAmberDark else OpenCityColors.BrandAzureDark
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (routes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isOffline && allRoutes.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Surface(
                            color = OpenCityColors.BrandAmberLight,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = OpenCityColors.BrandAmberDark,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = "Đang ngoại tuyến",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMain
                        )
                        Text(
                            text = "Không thể kết nối đến máy chủ để tải danh sách tuyến xe buýt. Vui lòng kiểm tra lại kết nối mạng hoặc máy chủ backend.",
                            fontSize = 12.5.sp,
                            color = OpenCityColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Thử lại kết nối", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = OpenCityColors.TextMuted, modifier = Modifier.size(36.dp))
                        Text("Không tìm thấy tuyến xe nào phù hợp", fontSize = 13.sp, color = OpenCityColors.TextMuted)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(routes) { route ->
                    BusRouteCard(route = route, onClick = { onSelectRoute(route) })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

private fun formatBusHours(raw: String): String {
    if (raw.isBlank()) return "05:00 - 21:00"
    val first = raw.split(";").first().trim()
    return first
        .replace("Mo-Su ", "")
        .replace("Mo-Sa ", "")
        .replace("Mo-Th ", "")
        .replace("Fr ", "T6 ")
        .replace("Sa-Su ", "T7-CN ")
}

private fun formatBusFrequency(raw: String): String {
    if (raw.isBlank()) return "10 - 15p"
    if (raw.contains("phút", ignoreCase = true)) {
        return raw.replace("phút/chuyến", "p").replace("phút", "p").trim()
    }
    val cleaned = raw.replace("00:", "").trim()
    return if (cleaned.isNotEmpty()) "$cleaned p" else "10 - 15p"
}

private fun formatBusFare(raw: String): String {
    if (raw.isBlank() || raw.equals("no", ignoreCase = true) || raw.equals("0", ignoreCase = true)) return "Miễn phí"
    if (raw.contains("đ", ignoreCase = true)) return raw
    val numOnly = raw.filter { it.isDigit() }
    val num = numOnly.toLongOrNull() ?: return raw
    return java.text.NumberFormat.getIntegerInstance(java.util.Locale("vi", "VN")).format(num) + " đ"
}

@Composable
private fun BusRouteCard(
    route: JSONObject,
    onClick: () -> Unit
) {
    val routeNum = route.text("route_number")
    val name = route.text("name")
    val operator = route.text("operator")
    val routeType = route.text("route_type")
    val hours = route.text("operating_hours", "05:00 - 21:00")
    val frequency = route.text("frequency", "10 - 15 phút")
    val fare = route.text("fare", "8.000 đ")
    val colorHex = route.text("color", "#2563EB")
    val brandColor = parseHexColor(colorHex)
    val stopsFwd = route.intOrNull("total_stops_forward") ?: 0
    val stopsBwd = route.intOrNull("total_stops_backward") ?: 0

    val displayHours = remember(hours) { formatBusHours(hours) }
    val displayFreq = remember(frequency) { formatBusFrequency(frequency) }
    val displayFare = remember(fare) { formatBusFare(fare) }

    Surface(
        color = OpenCityColors.CardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = brandColor,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(66.dp)
                    .height(64.dp),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = routeNum,
                        fontSize = if (routeNum.length > 4) 15.sp else 19.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        lineHeight = 20.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text(
                            text = when (routeType) {
                                "electric" -> "Điện"
                                "brt" -> "BRT"
                                else -> "Buýt"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (routeType == "electric") {
                        Surface(
                            color = OpenCityColors.BrandMintLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "VinBus",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.BrandMintDark,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    } else if (routeType == "brt") {
                        Surface(
                            color = OpenCityColors.BrandRedLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Nhanh",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.BrandRed,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = OpenCityColors.TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = displayHours,
                            fontSize = 11.5.sp,
                            color = OpenCityColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text("•", fontSize = 10.sp, color = OpenCityColors.TextLight)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = OpenCityColors.TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = displayFreq,
                            fontSize = 11.5.sp,
                            color = OpenCityColors.TextSecondary,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = OpenCityColors.BrandAzure,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = displayFare,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OpenCityColors.BrandAzure,
                            maxLines = 1
                        )
                    }

                    if (stopsFwd > 0 || stopsBwd > 0) {
                        Text("•", fontSize = 10.sp, color = OpenCityColors.TextLight)
                        Text(
                            text = "$stopsFwd trạm đi • $stopsBwd trạm về",
                            fontSize = 11.sp,
                            color = OpenCityColors.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
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

// trạm gần
@Composable
private fun NearbyStopsTab(
    stopsWithDist: List<Pair<JSONObject, Double>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedRadius: Float,
    onSelectRadius: (Float) -> Unit,
    loadingStops: Boolean,
    isOffline: Boolean = false,
    onRetry: () -> Unit = {},
    onSelectStop: (JSONObject) -> Unit,
    onSelectRoute: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Tìm trạm dừng (tên đường, bến xe, phố)...", fontSize = 13.sp, color = OpenCityColors.TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = OpenCityColors.TextMuted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Xóa", tint = OpenCityColors.TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = OpenCityColors.CardBg,
                unfocusedContainerColor = OpenCityColors.CardBg,
                focusedBorderColor = OpenCityColors.BrandAzure,
                unfocusedBorderColor = OpenCityColors.BorderColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val radiusOptions = listOf(
                3.0f to "Bán kính 3 km",
                5.0f to "5 km",
                10.0f to "10 km",
                25.0f to "Toàn Hà Nội (~5.052 trạm)"
            )
            radiusOptions.forEach { (r, label) ->
                val selected = Math.abs(selectedRadius - r) < 0.1f && searchQuery.isBlank()
                Surface(
                    onClick = { onSelectRadius(r) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) OpenCityColors.BrandAzure else OpenCityColors.CardBg,
                    border = BorderStroke(1.dp, if (selected) OpenCityColors.BrandAzure else OpenCityColors.BorderColor)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White else OpenCityColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Kết quả tìm kiếm (${stopsWithDist.size})"
                    else "Trạm dừng (${stopsWithDist.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextMain
                )
                if (loadingStops) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = OpenCityColors.BrandAzure)
                }
            }
            Text(
                text = if (searchQuery.isNotBlank()) "Toàn mạng lưới"
                else if (selectedRadius >= 25f) "Toàn Hà Nội"
                else "Bán kính ~${selectedRadius.toInt()} km",
                fontSize = 11.5.sp,
                color = OpenCityColors.TextMuted
            )
        }

        Spacer(Modifier.height(10.dp))

        if (stopsWithDist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isOffline) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Surface(
                            color = OpenCityColors.BrandAmberLight,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = OpenCityColors.BrandAmberDark,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Text(
                            text = "Đang ngoại tuyến",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMain
                        )
                        Text(
                            text = "Không thể kết nối đến máy chủ để tải dữ liệu trạm dừng xe buýt. Vui lòng kiểm tra lại kết nối mạng hoặc máy chủ backend.",
                            fontSize = 12.5.sp,
                            color = OpenCityColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Thử lại kết nối", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.PinDrop, contentDescription = null, tint = OpenCityColors.TextMuted, modifier = Modifier.size(36.dp))
                        Text("Đang tải hoặc chưa tìm thấy trạm dừng trong bán kính này", fontSize = 13.sp, color = OpenCityColors.TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(stopsWithDist) { (stop, dist) ->
                    BusStopCard(
                        stop = stop,
                        distKm = dist,
                        onClick = { onSelectStop(stop) },
                        onSelectRoute = onSelectRoute
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun BusStopCard(
    stop: JSONObject,
    distKm: Double,
    onClick: () -> Unit,
    onSelectRoute: (String) -> Unit
) {
    val name = stop.text("name")
    val address = stop.text("address")
    val hasShelter = stop.optBoolean("has_shelter", false)
    val routesArr = stop.arrOrNull("routes")
    val routesList = remember(routesArr) {
        if (routesArr == null) emptyList() else (0 until routesArr.length()).map { routesArr.getString(it) }
    }

    Surface(
        color = OpenCityColors.CardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                        color = OpenCityColors.BrandAzureLight,
                        shape = CircleShape,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = OpenCityColors.BrandAzure,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = name,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                }

                Surface(
                    color = OpenCityColors.BrandMintLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Cách ${formatDistanceKm(distKm)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OpenCityColors.BrandMintDark,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = if (hasShelter) OpenCityColors.BrandMintLight else OpenCityColors.SurfaceBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (hasShelter) "Có nhà chờ" else "Cột dừng",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasShelter) OpenCityColors.BrandMintDark else OpenCityColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (routesList.isNotEmpty()) {
                    Text("Tuyến đi qua:", fontSize = 11.sp, color = OpenCityColors.TextMuted)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(routesList) { rNum ->
                            Surface(
                                color = OpenCityColors.BrandAzureLight,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                                modifier = Modifier.clickable { onSelectRoute(rNum) }
                            ) {
                                Text(
                                    text = rNum,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.BrandAzure,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// maplibre mạng bus
@Composable
private fun BusMapView(
    stops: List<JSONObject>,
    userCoords: Pair<Double, Double>,
    selectedRadius: Float = 3.0f,
    onSelectRadius: (Float) -> Unit = {},
    selectedStop: JSONObject?,
    onSelectStop: (JSONObject) -> Unit,
    onSelectRouteFromStop: (String) -> Unit
) {
    val context = LocalContext.current
    var mapLibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(
                    Style.Builder().fromUri("https://tiles.openfreemap.org/styles/positron")
                ) {
                    mapLibreMap = map
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(userCoords.first, userCoords.second))
                        .zoom(13.2)
                        .tilt(20.0)
                        .bearing(0.0)
                        .build()

                    map.setOnMarkerClickListener { marker ->
                        if (marker.title == "Vị trí của bạn") return@setOnMarkerClickListener true
                        val found = stops.find { st ->
                            val lat = st.doubleOrNull("latitude") ?: 0.0
                            val lon = st.doubleOrNull("longitude") ?: 0.0
                            Math.abs(lat - marker.position.latitude) < 0.0005 &&
                                    Math.abs(lon - marker.position.longitude) < 0.0005
                        }
                        if (found != null) {
                            onSelectStop(found)
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15.0), 600)
                        }
                        true
                    }
                }
            }
        }
    }

    LaunchedEffect(stops, mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        map.clear()

        val userIcon = IconFactory.getInstance(context).fromBitmap(createUserLocationMarkerBitmap(context))
        map.addMarker(
            MarkerOptions()
                .position(LatLng(userCoords.first, userCoords.second))
                .title("Vị trí của bạn")
                .snippet("Tâm tìm kiếm trạm buýt")
                .icon(userIcon)
        )

        val stopIcon = IconFactory.getInstance(context).fromBitmap(createBusStopMarkerBitmap(context))
        stops.take(250).forEach { st ->
            val lat = st.doubleOrNull("latitude") ?: return@forEach
            val lon = st.doubleOrNull("longitude") ?: return@forEach
            val name = st.text("name")

            map.addMarker(
                MarkerOptions()
                    .position(LatLng(lat, lon))
                    .title(name)
                    .icon(stopIcon)
            )
        }

        map.setOnMarkerClickListener { marker ->
            if (marker.title == "Vị trí của bạn") return@setOnMarkerClickListener true
            val found = stops.find { st ->
                val lat = st.doubleOrNull("latitude") ?: 0.0
                val lon = st.doubleOrNull("longitude") ?: 0.0
                Math.abs(lat - marker.position.latitude) < 0.0005 &&
                        Math.abs(lon - marker.position.longitude) < 0.0005
            }
            if (found != null) {
                onSelectStop(found)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 15.5), 500)
            }
            true
        }
    }

    DisposableEffect(lifecycleOwner) {
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .padding(top = 12.dp, start = 12.dp)
                .align(Alignment.TopStart),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = OpenCityColors.CardBg.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, OpenCityColors.BorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = OpenCityColors.BrandAzure, modifier = Modifier.size(14.dp))
                    Text("${stops.size} trạm dừng (${if (selectedRadius >= 25f) "Toàn TP" else "${selectedRadius.toInt()} km"})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val radii = listOf(3.0f to "3km", 5.0f to "5km", 10.0f to "10km", 25.0f to "Toàn TP")
                radii.forEach { (r, label) ->
                    val isSel = Math.abs(selectedRadius - r) < 0.1f
                    Surface(
                        onClick = { onSelectRadius(r) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSel) OpenCityColors.BrandAzure else OpenCityColors.CardBg.copy(alpha = 0.92f),
                        shadowElevation = 3.dp,
                        border = BorderStroke(1.dp, if (isSel) OpenCityColors.BrandAzure else OpenCityColors.BorderColor)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else OpenCityColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomIn(), 250) },
                shape = CircleShape,
                color = OpenCityColors.CardBg,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Phóng to", tint = OpenCityColors.TextMain, modifier = Modifier.size(20.dp))
                }
            }

            Surface(
                onClick = { mapLibreMap?.animateCamera(CameraUpdateFactory.zoomOut(), 250) },
                shape = CircleShape,
                color = OpenCityColors.CardBg,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Remove, contentDescription = "Thu nhỏ", tint = OpenCityColors.TextMain, modifier = Modifier.size(20.dp))
                }
            }

            Surface(
                onClick = {
                    mapLibreMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(userCoords.first, userCoords.second), 14.5),
                        400
                    )
                },
                shape = CircleShape,
                color = OpenCityColors.CardBg,
                shadowElevation = 5.dp,
                border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Vị trí của bạn", tint = OpenCityColors.BrandAzure, modifier = Modifier.size(18.dp))
                }
            }
        }

        selectedStop?.let { st ->
            val name = st.text("name")
            val address = st.text("address")
            val dist = calcDistanceKm(userCoords.first, userCoords.second, st.doubleOrNull("latitude") ?: 21.0, st.doubleOrNull("longitude") ?: 105.8)
            val routesArr = st.arrOrNull("routes")
            val routesList = if (routesArr == null) emptyList() else (0 until routesArr.length()).map { routesArr.getString(it) }

            Surface(
                color = OpenCityColors.CardBg,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
                            Text(address.ifBlank { "Hà Nội" }, fontSize = 12.sp, color = OpenCityColors.TextMuted)
                        }
                        Surface(color = OpenCityColors.BrandMintLight, shape = RoundedCornerShape(8.dp)) {
                            Text("Cách ${formatDistanceKm(dist)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.BrandMintDark, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }

                    if (routesList.isNotEmpty()) {
                        Text("Các tuyến xe buýt đón tại trạm này:", fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = OpenCityColors.TextSecondary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(routesList) { rNum ->
                                Surface(
                                    color = OpenCityColors.BrandAzure,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { onSelectRouteFromStop(rNum) }
                                ) {
                                    Text(
                                        text = rNum,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

@Composable
private fun BusRouteDetailDialog(
    api: ApiClient,
    routeSummary: JSONObject,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var detail by remember { mutableStateOf<JSONObject?>(null) }
    var loadingDetail by remember { mutableStateOf(true) }
    var direction by remember { mutableStateOf("forward") } // "forward" (Chiều đi) | "backward" (Chiều về)
    var viewMode by remember { mutableStateOf("list") } // "list" | "map"

    val routeId = routeSummary.text("id")
    val routeNum = routeSummary.text("route_number")
    val name = routeSummary.text("name")
    val operator = routeSummary.text("operator")
    val hours = routeSummary.text("operating_hours", "05:00 - 21:00")
    val frequency = routeSummary.text("frequency", "10 - 15 phút")
    val fare = routeSummary.text("fare", "8.000 đ")
    val brandColor = parseHexColor(routeSummary.text("color", "#2563EB"))

    LaunchedEffect(routeId) {
        scope.launch {
            try {
                val d = api.item("/bus/routes/$routeId")
                detail = d
            } catch (_: Exception) {
                detail = routeSummary
            } finally {
                loadingDetail = false
            }
        }
    }

    val forwardStops = remember(detail) {
        val arr = detail?.arrOrNull("forward_stops")
        if (arr == null) emptyList() else (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    val backwardStops = remember(detail) {
        val arr = detail?.arrOrNull("backward_stops")
        if (arr == null) emptyList() else (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    val activeStops = if (direction == "forward") forwardStops else backwardStops

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = OpenCityColors.CardBg,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
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
                        .padding(horizontal = 20.dp, vertical = 10.dp),
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
                        Text(formatBusFare(fare), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.BrandAzure)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .height(36.dp)
                        .background(OpenCityColors.SurfaceBg, RoundedCornerShape(10.dp))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (direction == "forward") OpenCityColors.BrandAzure else Color.Transparent)
                            .clickable { direction = "forward" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "➡️ Chiều đi (${forwardStops.size.takeIf { it > 0 } ?: routeSummary.intOrNull("total_stops_forward") ?: 0} trạm)",
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
                            .background(if (direction == "backward") OpenCityColors.BrandAzure else Color.Transparent)
                            .clickable { direction = "backward" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⬅️ Chiều về (${backwardStops.size.takeIf { it > 0 } ?: routeSummary.intOrNull("total_stops_backward") ?: 0} trạm)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (direction == "backward") Color.White else OpenCityColors.TextSecondary
                        )
                    }
                }

                if (loadingDetail) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OpenCityColors.BrandAzure)
                    }
                } else if (activeStops.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Đang tải dữ liệu trạm dừng tuyến...", fontSize = 13.sp, color = OpenCityColors.TextMuted)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        item {
                            Text(
                                text = if (direction == "forward") "Danh sách trạm dừng chiều đi:" else "Danh sách trạm dừng chiều về:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.TextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        itemsIndexed(activeStops) { index, stop ->
                            val isFirst = index == 0
                            val isLast = index == activeStops.size - 1
                            val stopName = stop.text("name")
                            val address = stop.text("address")

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                                .height(30.dp)
                                                .background(OpenCityColors.BorderColor)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stopName,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isFirst || isLast) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isFirst || isLast) OpenCityColors.TextMain else OpenCityColors.TextSecondary
                                    )
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
                            }
                        }

                        item { Spacer(Modifier.height(20.dp)) }
                    }
                }
            }
        }
    }
}

private fun createUserLocationMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (32 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = size / 2f
    val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x332563EB
    }
    canvas.drawCircle(center, center, 14f * density, outerPaint)

    val whiteBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 8f * density, whiteBg)

    val centerDot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF2563EB.toInt()
    }
    canvas.drawCircle(center, center, 5.5f * density, centerDot)

    return bitmap
}

private fun createBusStopMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (28 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = size / 2f
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x22000000
    }
    canvas.drawCircle(center, center + 1 * density, 11f * density, shadowPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF2563EB.toInt()
    }
    canvas.drawCircle(center, center, 10f * density, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 10f * density, strokePaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 4f * density, dotPaint)

    return bitmap
}
