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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import org.maplibre.android.annotations.PolylineOptions
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
    revision: Int,
    initialTab: String = "plan"
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

    var currentTab by remember { mutableStateOf(initialTab) } // "routes" | "plan" | "stops" | "map"
    var selectedFilter by remember { mutableStateOf("all") } // "all" | "standard" | "electric" | "brt"
    var searchQuery by remember { mutableStateOf("") }

    var selectedRadiusKm by remember { mutableFloatStateOf(3.0f) }
    var stopSearchQuery by remember { mutableStateOf("") }
    var loadingStops by remember { mutableStateOf(false) }

    var selectedRouteForDetail by remember { mutableStateOf<JSONObject?>(null) }
    var selectedStopOnMap by remember { mutableStateOf<JSONObject?>(null) }
    var selectedTripPlanForMap by remember { mutableStateOf<JSONObject?>(null) }

    var plannerFromQuery by remember { mutableStateOf("Vị trí của bạn") }
    var plannerToQuery by remember { mutableStateOf("") }
    var plannerIsUsingMyLocation by remember { mutableStateOf(true) }
    var plannerFromCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var plannerToCoords by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var mapPickingMode by remember { mutableStateOf<String?>(null) } // null | "origin" | "destination"
    var autoTriggerSearchInPlanner by remember { mutableStateOf(false) }

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
                    val hanoiLoc = if (loc.first in 20.0..22.0 && loc.second in 105.0..107.0) loc else (21.0285 to 105.8542)
                    userCoords = hanoiLoc
                    isGpsLocked = true
                    loadStops(hanoiLoc.first, hanoiLoc.second, selectedRadiusKm, stopSearchQuery)
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
                val hanoiLoc = if (loc.first in 20.0..22.0 && loc.second in 105.0..107.0) loc else (21.0285 to 105.8542)
                userCoords = hanoiLoc
                isGpsLocked = true
                loadStops(hanoiLoc.first, hanoiLoc.second, selectedRadiusKm, stopSearchQuery)
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
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, OpenCityColors.BorderColor.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isGpsLocked) Color(0xFF10B981) else Color(0xFF94A3B8))
                        )
                        Text(
                            text = if (isLocating) "Đang tìm GPS..." else if (isGpsLocked) "Hà Nội • GPS đã đồng bộ" else "Khu vực trung tâm Hà Nội",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = OpenCityColors.TextSecondary
                        )
                    }

                    Text(
                        text = if (isLocating) "Đang tìm..." else "Định vị lại",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.BrandAzure,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                askLocationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BusFeatureSquircleTile(
                        title = "Tra cứu",
                        icon = Icons.Default.DirectionsBus,
                        gradient = Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))),
                        accentColor = Color(0xFF2563EB),
                        isSelected = currentTab == "routes",
                        modifier = Modifier.weight(1f),
                        onClick = { currentTab = "routes" }
                    )
                    BusFeatureSquircleTile(
                        title = "Tìm đường",
                        icon = Icons.Default.AltRoute,
                        gradient = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706))),
                        accentColor = Color(0xFFF59E0B),
                        isSelected = currentTab == "plan",
                        modifier = Modifier.weight(1f),
                        onClick = { currentTab = "plan" }
                    )
                    BusFeatureSquircleTile(
                        title = "Trạm dừng",
                        icon = Icons.Default.PinDrop,
                        gradient = Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED))),
                        accentColor = Color(0xFF8B5CF6),
                        isSelected = currentTab == "stops",
                        modifier = Modifier.weight(1f),
                        onClick = { currentTab = "stops" }
                    )
                    BusFeatureSquircleTile(
                        title = "Bản đồ",
                        icon = Icons.Default.Map,
                        gradient = Brush.verticalGradient(listOf(Color(0xFF14B8A6), Color(0xFF0F766E))),
                        accentColor = Color(0xFF0D9488),
                        isSelected = currentTab == "map",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            mapPickingMode = null
                            selectedTripPlanForMap = null
                            currentTab = "map"
                        }
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
        } else if (currentTab == "plan") {
            TripPlannerTab(
                api = api,
                userCoords = userCoords,
                isOffline = isOffline,
                allRoutes = routes,
                fromQuery = plannerFromQuery,
                onFromQueryChange = { q, coords ->
                    plannerFromQuery = q
                    plannerFromCoords = coords
                    if (q != "Vị trí của bạn") plannerIsUsingMyLocation = false
                },
                toQuery = plannerToQuery,
                onToQueryChange = { q, coords ->
                    plannerToQuery = q
                    plannerToCoords = coords
                },
                isUsingMyLocation = plannerIsUsingMyLocation,
                onUsingMyLocationChange = { plannerIsUsingMyLocation = it },
                fromStopCoords = plannerFromCoords,
                toStopCoords = plannerToCoords,
                autoTriggerSearch = autoTriggerSearchInPlanner,
                onClearAutoTrigger = { autoTriggerSearchInPlanner = false },
                onPickOnMap = { mode ->
                    mapPickingMode = mode
                    selectedTripPlanForMap = null
                    selectedStopOnMap = null
                    currentTab = "map"
                },
                onViewRouteOnMap = { rec ->
                    selectedTripPlanForMap = rec
                    currentTab = "map"
                },
                onSelectRouteDetail = { routeNumber ->
                    val r = routes.find { it.text("route_number").equals(routeNumber, ignoreCase = true) }
                    if (r != null) {
                        selectedRouteForDetail = r
                    }
                }
            )
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
                },
                tripPlan = selectedTripPlanForMap,
                onClearTripPlan = {
                    selectedTripPlanForMap = null
                    currentTab = "plan"
                },
                pickingMode = mapPickingMode,
                onCancelPicking = { mapPickingMode = null },
                onSetAsOrigin = { name, coords ->
                    plannerFromQuery = name
                    plannerFromCoords = coords
                    plannerIsUsingMyLocation = false
                    mapPickingMode = null
                    selectedStopOnMap = null
                    selectedTripPlanForMap = null
                    currentTab = "plan"
                },
                onSetAsDestination = { name, coords ->
                    plannerToQuery = name
                    plannerToCoords = coords
                    mapPickingMode = null
                    selectedStopOnMap = null
                    selectedTripPlanForMap = null
                    autoTriggerSearchInPlanner = true
                    currentTab = "plan"
                },
                api = api,
                originCoords = if (plannerIsUsingMyLocation) userCoords else (plannerFromCoords ?: userCoords),
                destinationCoords = plannerToCoords
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
            onDismiss = { selectedRouteForDetail = null },
            onViewOnMap = { mapPlanObj ->
                selectedTripPlanForMap = mapPlanObj
                selectedRouteForDetail = null
                currentTab = "map"
            }
        )
    }
}
@Composable
private fun StopsAutocompleteDropdown(
    suggestions: List<JSONObject>,
    isLoading: Boolean,
    onSelectStop: (JSONObject) -> Unit
) {
    if (isLoading) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                color = OpenCityColors.BrandAzure,
                modifier = Modifier.size(13.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Đang tìm trạm dừng phù hợp...",
                fontSize = 11.5.sp,
                color = OpenCityColors.TextMuted
            )
        }
    } else if (suggestions.isNotEmpty()) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                suggestions.forEachIndexed { index, stop ->
                    val rawName = stop.optString("name")
                    val displayName = if (rawName.startsWith("[Trạm]")) rawName else "[Trạm] $rawName"
                    val address = stop.optString("address", "Hà Nội")

                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelectStop(stop) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = OpenCityColors.BrandAzureLight,
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBus,
                                        contentDescription = null,
                                        tint = OpenCityColors.BrandAzure,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.TextMain,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (address.isNotBlank() && address != "Hà Nội") {
                                    Text(
                                        text = address,
                                        fontSize = 10.5.sp,
                                        color = OpenCityColors.TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.NorthWest,
                                contentDescription = "Chọn",
                                tint = OpenCityColors.TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (index < suggestions.size - 1) {
                        HorizontalDivider(
                            color = OpenCityColors.BorderColor.copy(alpha = 0.35f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripPlannerTab(
    api: ApiClient,
    userCoords: Pair<Double, Double>,
    isOffline: Boolean,
    allRoutes: List<JSONObject>,
    fromQuery: String,
    onFromQueryChange: (String, Pair<Double, Double>?) -> Unit,
    toQuery: String,
    onToQueryChange: (String, Pair<Double, Double>?) -> Unit,
    isUsingMyLocation: Boolean,
    onUsingMyLocationChange: (Boolean) -> Unit,
    fromStopCoords: Pair<Double, Double>?,
    toStopCoords: Pair<Double, Double>?,
    autoTriggerSearch: Boolean = false,
    onClearAutoTrigger: () -> Unit = {},
    onPickOnMap: (String) -> Unit,
    onViewRouteOnMap: (JSONObject) -> Unit,
    onSelectRouteDetail: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var recommendations by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var expandedTripIndex by remember { mutableStateOf<Int?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    var fromSuggestions by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var toSuggestions by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var isLoadingFromSuggestions by remember { mutableStateOf(false) }
    var isLoadingToSuggestions by remember { mutableStateOf(false) }

    var justSelectedFrom by remember { mutableStateOf(false) }
    var justSelectedTo by remember { mutableStateOf(false) }

    LaunchedEffect(fromQuery, isUsingMyLocation, fromStopCoords) {
        if (justSelectedFrom || fromStopCoords != null) {
            justSelectedFrom = false
            fromSuggestions = emptyList()
            return@LaunchedEffect
        }
        if (!isUsingMyLocation && fromQuery.trim().length >= 2) {
            kotlinx.coroutines.delay(200)
            isLoadingFromSuggestions = true
            try {
                val enc = java.net.URLEncoder.encode(fromQuery.trim(), "UTF-8")
                val res = withContext(Dispatchers.IO) {
                    api.list("/bus/stops?keyword=$enc")
                }
                fromSuggestions = res.take(5)
            } catch (_: Exception) {
                fromSuggestions = emptyList()
            } finally {
                isLoadingFromSuggestions = false
            }
        } else {
            fromSuggestions = emptyList()
        }
    }

    LaunchedEffect(toQuery, toStopCoords) {
        if (justSelectedTo || toStopCoords != null) {
            justSelectedTo = false
            toSuggestions = emptyList()
            return@LaunchedEffect
        }
        if (toQuery.trim().length >= 2) {
            kotlinx.coroutines.delay(200)
            isLoadingToSuggestions = true
            try {
                val enc = java.net.URLEncoder.encode(toQuery.trim(), "UTF-8")
                val res = withContext(Dispatchers.IO) {
                    api.list("/bus/stops?keyword=$enc")
                }
                toSuggestions = res.take(5)
            } catch (_: Exception) {
                toSuggestions = emptyList()
            } finally {
                isLoadingToSuggestions = false
            }
        } else {
            toSuggestions = emptyList()
        }
    }

    val popularTrips = remember {
        listOf(
            "Long Biên" to "Mỹ Đình",
            "Sóc Sơn" to "Hoàn Kiếm",
            "Bách Khoa" to "Cầu Giấy",
            "Long Biên" to "Hà Đông",
            "Nội Bài" to "Kim Mã"
        )
    }

    val doSearch: (String, String, Boolean, Pair<Double, Double>?, Pair<Double, Double>?) -> Unit = { origin, dest, useLoc, originCoords, destCoords ->
        if (dest.isBlank()) {
            searchError = "Vui lòng nhập điểm đến (tên trạm hoặc địa chỉ)"
        } else {
            scope.launch {
                isSearching = true
                searchError = null
                hasSearched = true
                expandedTripIndex = null
                try {
                    val url = buildString {
                        append("/bus/find?")
                        if (useLoc || origin.trim() == "Vị trí của bạn") {
                            append("from_lat=${userCoords.first}&from_lng=${userCoords.second}")
                        } else if (originCoords != null) {
                            append("from_lat=${originCoords.first}&from_lng=${originCoords.second}")
                        } else {
                            val encOrigin = java.net.URLEncoder.encode(origin.trim(), "UTF-8")
                            append("from_query=$encOrigin")
                        }

                        append("&")
                        if (destCoords != null) {
                            append("to_lat=${destCoords.first}&to_lng=${destCoords.second}")
                        } else if (dest.trim() == "Vị trí của bạn") {
                            append("to_lat=${userCoords.first}&to_lng=${userCoords.second}")
                        } else if (dest.trim() == "Vị trí của bạn") {
                            append("to_lat=${userCoords.first}&to_lng=${userCoords.second}")
                        } else {
                            val encDest = java.net.URLEncoder.encode(dest.trim(), "UTF-8")
                            append("to_query=$encDest")
                        }
                    }
                    val res = api.list(url)
                    recommendations = res
                    if (res.isEmpty()) {
                        searchError = "Không tìm thấy lộ trình phù hợp giữa 2 điểm này. Vui lòng thử chọn tên trạm hoặc địa danh lân cận."
                    }
                } catch (e: Exception) {
                    val localDirect = allRoutes.filter { r ->
                        val name = r.text("name").lowercase()
                        val num = r.text("route_number").lowercase()
                        val q = dest.trim().lowercase()
                        name.contains(q) || num == q
                    }.take(3).map { r ->
                        JSONObject().apply {
                            put("type", "direct")
                            put("route_number", r.text("route_number"))
                            put("route_name", r.text("name"))
                            put("operator", r.text("operator"))
                            put("color", r.optString("color", "#2563EB"))
                            put("stops_count", 15)
                            put("estimated_minutes", 35)
                            put("co2_saved_grams", 1250)
                            put("is_electric", r.text("route_number").startsWith("E"))
                            put("board_stop", JSONObject("""{"name":"Trạm đón gần bạn","address":"Khu vực trung tâm Hà Nội"}"""))
                            put("alight_stop", JSONObject("""{"name":"${dest.trim()}","address":"Điểm đến"}"""))
                        }
                    }
                    if (localDirect.isNotEmpty()) {
                        recommendations = localDirect
                        searchError = "Đang ngoại tuyến • Gợi ý lộ trình từ cơ sở dữ liệu cục bộ"
                    } else {
                        searchError = "Không thể kết nối máy chủ để tìm lộ trình: ${e.message}"
                    }
                } finally {
                    isSearching = false
                }
            }
        }
    }

    LaunchedEffect(autoTriggerSearch) {
        if (autoTriggerSearch && toQuery.isNotBlank()) {
            onClearAutoTrigger()
            doSearch(fromQuery, toQuery, isUsingMyLocation, fromStopCoords, toStopCoords)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(Modifier.height(4.dp))
            Surface(
                color = OpenCityColors.CardBg,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Tìm đường xe buýt Thủ đô",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpenCityColors.TextMain
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                            }
                        }

                        if (isUsingMyLocation) {
                            Surface(
                                color = OpenCityColors.SurfaceBg,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onUsingMyLocationChange(false)
                                        onFromQueryChange("", null)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            Icons.Default.MyLocation,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Vị trí của bạn (Tâm Hà Nội)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OpenCityColors.TextMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Đổi",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OpenCityColors.BrandAzure
                                        )
                                        IconButton(
                                            onClick = { onPickOnMap("origin") },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Map,
                                                contentDescription = "Chọn điểm đi trên bản đồ",
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = fromQuery,
                                onValueChange = { onFromQueryChange(it, null) },
                                placeholder = { Text("Nhập điểm xuất phát...", fontSize = 13.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        val firstSug = fromSuggestions.firstOrNull()
                                        if (firstSug != null) {
                                            val chosenName = firstSug.optString("name")
                                            val sLat = firstSug.optDouble("latitude", Double.NaN)
                                            val sLng = firstSug.optDouble("longitude", Double.NaN)
                                            val coords = if (!sLat.isNaN() && !sLng.isNaN()) Pair(sLat, sLng) else null
                                            justSelectedFrom = true
                                            onFromQueryChange(chosenName, coords)
                                            fromSuggestions = emptyList()
                                        }
                                        if (toQuery.isNotBlank()) {
                                            doSearch(fromQuery, toQuery, isUsingMyLocation, fromStopCoords, toStopCoords)
                                        }
                                    }
                                ),
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (fromQuery.isNotBlank()) {
                                            IconButton(onClick = {
                                                onFromQueryChange("", null)
                                                fromSuggestions = emptyList()
                                            }) {
                                                Icon(Icons.Default.Close, contentDescription = "Xóa", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        IconButton(onClick = { onPickOnMap("origin") }) {
                                            Icon(Icons.Default.Map, contentDescription = "Chọn điểm đi trên bản đồ", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            )
                        }

                        Surface(
                            onClick = {
                                val oldFrom = if (isUsingMyLocation) "Vị trí của bạn" else fromQuery
                                val oldFromCoords = if (isUsingMyLocation) userCoords else fromStopCoords
                                val oldUseLoc = isUsingMyLocation
                                val oldTo = toQuery
                                val oldToCoords = toStopCoords

                                val newFrom = oldTo
                                val newFromCoords = oldToCoords
                                val newUseLoc = (newFrom == "Vị trí của bạn")
                                val newTo = if (oldUseLoc) "Vị trí của bạn" else oldFrom
                                val newToCoords = if (oldUseLoc) userCoords else oldFromCoords

                                onUsingMyLocationChange(newUseLoc)
                                onFromQueryChange(newFrom, newFromCoords)
                                onToQueryChange(newTo, newToCoords)

                                justSelectedFrom = true
                                justSelectedTo = true
                                fromSuggestions = emptyList()
                                toSuggestions = emptyList()
                                keyboardController?.hide()
                                focusManager.clearFocus()

                                if (newFrom.isNotBlank() && newTo.isNotBlank()) {
                                    doSearch(newFrom, newTo, newUseLoc, newFromCoords, newToCoords)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = OpenCityColors.BrandAzureLight,
                            border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Đổi chiều điểm đi và điểm đến",
                                    tint = OpenCityColors.BrandAzure,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    if (fromSuggestions.isNotEmpty() || isLoadingFromSuggestions) {
                        StopsAutocompleteDropdown(
                            suggestions = fromSuggestions,
                            isLoading = isLoadingFromSuggestions,
                            onSelectStop = { stop ->
                                val chosenName = stop.optString("name")
                                val lat = stop.optDouble("latitude", Double.NaN)
                                val lng = stop.optDouble("longitude", Double.NaN)
                                val coords = if (!lat.isNaN() && !lng.isNaN()) Pair(lat, lng) else null
                                justSelectedFrom = true
                                onFromQueryChange(chosenName, coords)
                                fromSuggestions = emptyList()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFFEF4444).copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = toQuery,
                            onValueChange = { onToQueryChange(it, null) },
                            placeholder = { Text("Nhập điểm đến (ví dụ: Mỹ Đình, Bờ Hồ...)", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    val firstSug = toSuggestions.firstOrNull()
                                    if (firstSug != null) {
                                        val chosenName = firstSug.optString("name")
                                        val sLat = firstSug.optDouble("latitude", Double.NaN)
                                        val sLng = firstSug.optDouble("longitude", Double.NaN)
                                        val coords = if (!sLat.isNaN() && !sLng.isNaN()) Pair(sLat, sLng) else null
                                        justSelectedTo = true
                                        onToQueryChange(chosenName, coords)
                                        toSuggestions = emptyList()
                                        doSearch(fromQuery, chosenName, isUsingMyLocation, fromStopCoords, coords)
                                    } else if (toQuery.isNotBlank()) {
                                        toSuggestions = emptyList()
                                        doSearch(fromQuery, toQuery, isUsingMyLocation, fromStopCoords, toStopCoords)
                                    }
                                }
                            ),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (toQuery.isNotBlank()) {
                                        IconButton(onClick = { onToQueryChange("", null); toSuggestions = emptyList() }) {
                                            Icon(Icons.Default.Close, contentDescription = "Xóa", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                    IconButton(onClick = { onPickOnMap("destination") }) {
                                        Icon(Icons.Default.Map, contentDescription = "Chọn điểm đến trên bản đồ", tint = OpenCityColors.BrandAzure, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    }

                    if (toSuggestions.isNotEmpty() || isLoadingToSuggestions) {
                        StopsAutocompleteDropdown(
                            suggestions = toSuggestions,
                            isLoading = isLoadingToSuggestions,
                            onSelectStop = { stop ->
                                val chosenName = stop.optString("name")
                                val lat = stop.optDouble("latitude", Double.NaN)
                                val lng = stop.optDouble("longitude", Double.NaN)
                                val coords = if (!lat.isNaN() && !lng.isNaN()) Pair(lat, lng) else null
                                justSelectedTo = true
                                onToQueryChange(chosenName, coords)
                                toSuggestions = emptyList()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                doSearch(fromQuery, chosenName, isUsingMyLocation, fromStopCoords, coords)
                            }
                        )
                    }

                    Surface(
                        onClick = { onPickOnMap("destination") },
                        color = OpenCityColors.BrandAzureLight.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = OpenCityColors.BrandAzure,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chọn trên bản đồ",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.BrandAzure
                                )
                                Text(
                                    text = "Chạm trực tiếp vào trạm xe buýt hoặc địa điểm",
                                    fontSize = 10.5.sp,
                                    color = OpenCityColors.TextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = OpenCityColors.BrandAzure,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Điểm đến phổ biến:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OpenCityColors.TextSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val popularDestList = listOf(
                                Triple("Bến xe Giáp Bát", 20.9804, 105.8415),
                                Triple("Bến xe Mỹ Đình", 21.0284, 105.7785),
                                Triple("Long Biên", 21.0425, 105.8496),
                                Triple("Cầu Giấy", 21.0336, 105.7997),
                                Triple("Bách Khoa", 21.0048, 105.8432),
                                Triple("Bờ Hồ", 21.0285, 105.8542)
                            )
                            items(popularDestList) { (chipDest, dLat, dLng) ->
                                Surface(
                                    onClick = {
                                        justSelectedTo = true
                                        toSuggestions = emptyList()
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        val coords = Pair(dLat, dLng)
                                        onToQueryChange(chipDest, coords)
                                        doSearch(fromQuery, chipDest, isUsingMyLocation, fromStopCoords, coords)
                                    },
                                    color = OpenCityColors.BrandAzureLight,
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = chipDest,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OpenCityColors.BrandAzure,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            doSearch(fromQuery, toQuery, isUsingMyLocation, fromStopCoords, toStopCoords)
                        },
                        enabled = toQuery.isNotBlank() && !isSearching,
                        colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text("Đang tìm lộ trình tối ưu...", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.AltRoute, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Tìm lộ trình xe buýt", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Gợi ý hành trình phổ biến (1 chạm tìm ngay):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.TextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(popularTrips) { (origin, dest) ->
                        Surface(
                            onClick = {
                                onUsingMyLocationChange(false)
                                justSelectedFrom = true
                                justSelectedTo = true
                                justSelectedFrom = true
                                justSelectedTo = true
                                fromSuggestions = emptyList()
                                toSuggestions = emptyList()
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                onFromQueryChange(origin, null)
                                onToQueryChange(dest, null)
                                doSearch(origin, dest, false, null, null)
                            },
                            color = OpenCityColors.CardBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "$origin → $dest",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OpenCityColors.BrandAzure
                                )
                            }
                        }
                    }
                }
            }
        }

        searchError?.let { err ->
            item {
                Surface(
                    color = if (err.contains("ngoại tuyến", ignoreCase = true)) OpenCityColors.BrandAmberLight else Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (err.contains("ngoại tuyến", ignoreCase = true)) OpenCityColors.BrandAmber.copy(alpha = 0.5f) else Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            if (err.contains("ngoại tuyến", ignoreCase = true)) Icons.Default.CloudOff else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (err.contains("ngoại tuyến", ignoreCase = true)) OpenCityColors.BrandAmberDark else Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (err.contains("ngoại tuyến", ignoreCase = true)) OpenCityColors.BrandAmberDark else Color(0xFF991B1B)
                        )
                    }
                }
            }
        }

        if (recommendations.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val directCount = recommendations.count { it.optString("type") == "direct" }
                    val transferCount = recommendations.count { it.optString("type") == "transfer" }

                    Text(
                        text = "Tìm thấy ${recommendations.size} lộ trình tối ưu",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpenCityColors.TextMain
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (directCount > 0) {
                            Surface(
                                color = OpenCityColors.BrandMintLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$directCount Đi thẳng",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.BrandMintDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (transferCount > 0) {
                            Surface(
                                color = OpenCityColors.BrandAmberLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$transferCount Đổi xe",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.BrandAmberDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(recommendations) { index, rec ->
                TripRecommendationCard(
                    rec = rec,
                    isExpanded = expandedTripIndex == index,
                    onToggleExpand = {
                        expandedTripIndex = if (expandedTripIndex == index) null else index
                    },
                    onViewOnMap = { onViewRouteOnMap(rec) },
                    onSelectRouteDetail = onSelectRouteDetail
                )
            }
        } else if (!hasSearched && !isSearching) {
            item {
                Surface(
                    color = OpenCityColors.CardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AltRoute,
                            contentDescription = null,
                            tint = OpenCityColors.BrandAzure,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Lên kế hoạch di chuyển xanh",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.TextMain
                        )
                        Text(
                            text = "Hệ thống hỗ trợ tìm tuyến đi thẳng hoặc đổi xe 1 lần (1-Transfer) tối ưu thời gian và tính toán lượng phát thải CO₂ tiết kiệm được.",
                            fontSize = 12.sp,
                            color = OpenCityColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TripRecommendationCard(
    rec: JSONObject,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onViewOnMap: () -> Unit,
    onSelectRouteDetail: (String) -> Unit
) {
    val isTransfer = rec.optString("type") == "transfer"
    val routeNum = rec.optString("route_number")
    val routeName = rec.optString("route_name")
    val estimatedMins = rec.optInt("estimated_minutes", 30)
    val stopsCount = rec.optInt("stops_count", 12)
    val co2Grams = rec.optInt("co2_saved_grams", stopsCount * 95)
    val isElectric = rec.optBoolean("is_electric", routeNum.startsWith("E"))

    val boardStop = rec.optJSONObject("board_stop")
    val alightStop = rec.optJSONObject("alight_stop")
    val transferStop = rec.optJSONObject("transfer_stop")
    val secondRouteNum = rec.optString("second_route_number")

    Surface(
        color = OpenCityColors.CardBg,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, if (isTransfer) OpenCityColors.BrandAmber.copy(alpha = 0.4f) else OpenCityColors.BrandAzure.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isTransfer) OpenCityColors.BrandAmberLight else OpenCityColors.BrandMintLight,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isTransfer) "Đổi xe 1 lần (1-Transfer)" else "Đi thẳng không đổi xe",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTransfer) OpenCityColors.BrandAmberDark else OpenCityColors.BrandMintDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = OpenCityColors.BrandAzure, modifier = Modifier.size(15.dp))
                    Text(
                        text = "~$estimatedMins phút",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = OpenCityColors.BrandAzure
                    )
                }
            }

            if (!isTransfer) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (isElectric) Color(0xFF10B981) else OpenCityColors.BrandAzure,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onSelectRouteDetail(routeNum) }
                    ) {
                        Text(
                            text = routeNum,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = routeName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = OpenCityColors.BrandAzure,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onSelectRouteDetail(routeNum) }
                    ) {
                        Text(
                            text = routeNum,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = OpenCityColors.TextMuted, modifier = Modifier.size(14.dp))

                    Surface(
                        color = OpenCityColors.BrandAmberLight,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, OpenCityColors.BrandAmber.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Đổi tại ${transferStop?.optString("name")?.take(18) ?: "trạm nối"}...",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.BrandAmberDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = OpenCityColors.TextMuted, modifier = Modifier.size(14.dp))

                    Surface(
                        color = OpenCityColors.BrandAzure,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.clickable { onSelectRouteDetail(secondRouteNum) }
                    ) {
                        Text(
                            text = secondRouteNum,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OpenCityColors.SurfaceBg, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Text(
                        text = "Đón: ${boardStop?.optString("name") ?: "Trạm xuất phát"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OpenCityColors.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isTransfer && transferStop != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                        Text(
                            text = "Đổi xe: ${transferStop.optString("name")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OpenCityColors.BrandAmberDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Text(
                        text = "Đến: ${alightStop?.optString("name") ?: "Trạm đích"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OpenCityColors.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(color = OpenCityColors.BorderColor, thickness = 0.5.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$stopsCount trạm dừng",
                        fontSize = 11.5.sp,
                        color = OpenCityColors.TextSecondary
                    )
                    Text(
                        text = if (isTransfer) "Vé: ~16.000 đ" else "Vé: ~8.000 đ",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.BrandAzure
                    )
                }
            }

            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Eco,
                            contentDescription = null,
                            tint = Color(0xFF166534),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Tiết kiệm ~${co2Grams}g CO₂ so với xe máy",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    }

                    if (isElectric) {
                        Surface(
                            color = Color(0xFF16A34A),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Net Zero",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            if (isExpanded) {
                TripStepByStepTimeline(rec = rec)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = OpenCityColors.BrandAzure,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isExpanded) "Thu gọn trạm dừng" else "Xem toàn bộ $stopsCount trạm",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OpenCityColors.BrandAzure
                        )
                    }
                }

                Button(
                    onClick = onViewOnMap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Xem bản đồ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripStepByStepTimeline(rec: JSONObject) {
    val isTransfer = rec.optString("type") == "transfer"
    val routeNum = rec.optString("route_number")
    val routeName = rec.optString("route_name")
    val secondRouteNum = rec.optString("second_route_number")
    val boardStop = rec.optJSONObject("board_stop")
    val alightStop = rec.optJSONObject("alight_stop")
    val transferStop = rec.optJSONObject("transfer_stop")
    val tripStopsArr = rec.optJSONArray("trip_stops")
    var showAllStops by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Hướng dẫn chi tiết từng bước di chuyển:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OpenCityColors.TextMain
        )

        TimelineStepRow(
            dotColor = Color(0xFF10B981),
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            title = "Bước 1: Đến trạm đón",
            description = "Trạm: ${boardStop?.optString("name") ?: ""}\nĐịa chỉ: ${boardStop?.optString("address") ?: "Khu vực đón xe"}"
        )

        TimelineStepRow(
            dotColor = OpenCityColors.BrandAzure,
            icon = Icons.Default.DirectionsBus,
            title = "Bước 2: Lên tuyến xe $routeNum",
            description = "Tuyến: $routeName\nLên xe và theo dõi hành trình qua các trạm dừng."
        )

        if (tripStopsArr != null && tripStopsArr.length() > 2) {
            Surface(
                onClick = { showAllStops = !showAllStops },
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                modifier = Modifier.fillMaxWidth().padding(start = 28.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (showAllStops) "Ẩn danh sách ${tripStopsArr.length()} trạm" else "Xem danh sách ${tripStopsArr.length()} trạm trên lộ trình",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OpenCityColors.BrandAzure
                    )
                    Icon(
                        if (showAllStops) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = OpenCityColors.BrandAzure,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            if (showAllStops) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until tripStopsArr.length()) {
                        val s = tripStopsArr.optJSONObject(i) ?: continue
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(OpenCityColors.TextMuted))
                            Text(
                                text = s.optString("name"),
                                fontSize = 11.sp,
                                color = OpenCityColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (isTransfer && transferStop != null) {
            TimelineStepRow(
                dotColor = Color(0xFFF59E0B),
                icon = Icons.Default.AltRoute,
                title = "Bước 3: Xuống xe tại trạm chuyển tuyến",
                description = "Trạm: ${transferStop.optString("name")}\nĐịa chỉ: ${transferStop.optString("address")}\nChờ tại trạm và chuyển sang tuyến $secondRouteNum."
            )

            TimelineStepRow(
                dotColor = OpenCityColors.BrandAzure,
                icon = Icons.Default.DirectionsBus,
                title = "Bước 4: Lên tuyến xe $secondRouteNum",
                description = "Lên xe tuyến $secondRouteNum để di chuyển đến điểm đích."
            )
        }

        TimelineStepRow(
            dotColor = Color(0xFFEF4444),
            icon = Icons.Default.Place,
            title = if (isTransfer) "Bước 5: Xuống xe tại điểm đến" else "Bước 3: Xuống xe tại điểm đến",
            description = "Trạm đích: ${alightStop?.optString("name") ?: ""}\nHoàn thành hành trình! Cảm ơn bạn đã lựa chọn phương tiện công cộng xanh."
        )
    }
}

@Composable
private fun TimelineStepRow(
    dotColor: Color,
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = dotColor.copy(alpha = 0.15f),
            shape = CircleShape,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = dotColor, modifier = Modifier.size(13.dp))
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.TextMain)
            Text(text = description, fontSize = 11.5.sp, color = OpenCityColors.TextSecondary, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun BusFeatureSquircleTile(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    accentColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            shadowElevation = if (isSelected) 5.dp else 1.5.dp,
            modifier = Modifier.size(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) OpenCityColors.TextMain else OpenCityColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(accentColor)
            )
        } else {
            Spacer(Modifier.height(3.dp))
        }
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
            "electric" to "VinBus điện ($electricCount)",
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

private suspend fun fetchWalkingRoute(
    api: ApiClient?,
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double
): Pair<List<LatLng>, Double> = withContext(Dispatchers.IO) {
    if (Math.abs(fromLat - toLat) < 0.00005 && Math.abs(fromLng - toLng) < 0.00005) {
        return@withContext Pair(listOf(LatLng(fromLat, fromLng)), 0.0)
    }

    if (api != null) {
        try {
            val json = api.item("/bus/walk-route?from_lat=$fromLat&from_lng=$fromLng&to_lat=$toLat&to_lng=$toLng")
            val coordsArr = json.optJSONArray("coordinates")
            val distM = json.optDouble("distance_m", 0.0)
            if (coordsArr != null && coordsArr.length() > 0) {
                val list = ArrayList<LatLng>(coordsArr.length())
                for (i in 0 until coordsArr.length()) {
                    val pt = coordsArr.optJSONArray(i) ?: continue
                    val lng = pt.optDouble(0)
                    val lat = pt.optDouble(1)
                    if (lat != 0.0 && lng != 0.0) {
                        list.add(LatLng(lat, lng))
                    }
                }
                if (list.size >= 2) {
                    return@withContext Pair(list, distM)
                }
            }
        } catch (_: Exception) {}
    }

    try {
        val url = java.net.URL("https://router.project-osrm.org/route/v1/foot/$fromLng,$fromLat;$toLng,$toLat?overview=full&geometries=geojson")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "OpenCityBusApp/1.0")
        if (conn.responseCode == 200) {
            val stream = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(stream)
            val routes = root.optJSONArray("routes")
            if (routes != null && routes.length() > 0) {
                val r0 = routes.getJSONObject(0)
                val distM = r0.optDouble("distance", 0.0)
                val geom = r0.optJSONObject("geometry")
                val coords = geom?.optJSONArray("coordinates")
                if (coords != null && coords.length() > 0) {
                    val list = ArrayList<LatLng>(coords.length())
                    for (i in 0 until coords.length()) {
                        val pt = coords.optJSONArray(i) ?: continue
                        list.add(LatLng(pt.optDouble(1), pt.optDouble(0)))
                    }
                    if (list.size >= 2) {
                        return@withContext Pair(list, distM)
                    }
                }
            }
        }
    } catch (_: Exception) {}

    val directDist = calcDistanceKm(fromLat, fromLng, toLat, toLng) * 1000.0
    Pair(listOf(LatLng(fromLat, fromLng), LatLng(toLat, toLng)), directDist)
}

private data class BusStopCluster(
    val centerLat: Double,
    val centerLng: Double,
    val stops: List<JSONObject>
)

private fun projectToPixel(lat: Double, lng: Double, zoom: Double): Pair<Double, Double> {
    val scale = 256.0 * Math.pow(2.0, zoom)
    val x = (lng + 180.0) / 360.0 * scale
    val sinY = Math.sin(Math.toRadians(lat)).coerceIn(-0.9999, 0.9999)
    val y = (0.5 - Math.log((1.0 + sinY) / (1.0 - sinY)) / (4.0 * Math.PI)) * scale
    return Pair(x, y)
}

private fun clusterStops(stops: List<JSONObject>, zoom: Double): List<BusStopCluster> {
    if (zoom >= 14.0) {
        return stops.mapNotNull { st ->
            val lat = st.doubleOrNull("latitude") ?: return@mapNotNull null
            val lng = st.doubleOrNull("longitude") ?: return@mapNotNull null
            if (lat == 0.0 || lng == 0.0) return@mapNotNull null
            BusStopCluster(centerLat = lat, centerLng = lng, stops = listOf(st))
        }
    }

    val clusterRadiusPx = 50.0
    val clusterRadiusPx2 = clusterRadiusPx * clusterRadiusPx
    val clusters = mutableListOf<MutableList<JSONObject>>()
    val clusterCenters = mutableListOf<Pair<Double, Double>>()

    for (st in stops) {
        val lat = st.doubleOrNull("latitude") ?: continue
        val lng = st.doubleOrNull("longitude") ?: continue
        if (lat == 0.0 || lng == 0.0) continue

        val (px, py) = projectToPixel(lat, lng, zoom)
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
            clusters[matchedIndex].add(st)
        } else {
            clusters.add(mutableListOf(st))
            clusterCenters.add(Pair(px, py))
        }
    }

    return clusters.map { group ->
        val avgLat = group.mapNotNull { it.doubleOrNull("latitude") }.average()
        val avgLng = group.mapNotNull { it.doubleOrNull("longitude") }.average()
        BusStopCluster(centerLat = avgLat, centerLng = avgLng, stops = group)
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
    onSelectStop: (JSONObject?) -> Unit,
    onSelectRouteFromStop: (String) -> Unit,
    tripPlan: JSONObject? = null,
    onClearTripPlan: () -> Unit = {},
    pickingMode: String? = null,
    onCancelPicking: () -> Unit = {},
    onSetAsOrigin: (String, Pair<Double, Double>?) -> Unit = { _, _ -> },
    onSetAsDestination: (String, Pair<Double, Double>?) -> Unit = { _, _ -> },
    api: ApiClient? = null,
    originCoords: Pair<Double, Double> = userCoords,
    destinationCoords: Pair<Double, Double>? = null
) {
    val context = LocalContext.current
    var mapLibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var currentMapZoom by remember { mutableStateOf(13.2) }

    var walkingToBoardPath by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var walkingToDestPath by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var walkingToBoardDist by remember { mutableStateOf(0) }
    var walkingToDestDist by remember { mutableStateOf(0) }

    LaunchedEffect(tripPlan, originCoords, destinationCoords) {
        if (tripPlan == null) {
            walkingToBoardPath = emptyList()
            walkingToDestPath = emptyList()
            walkingToBoardDist = 0
            walkingToDestDist = 0
            return@LaunchedEffect
        }

        val boardStop = tripPlan.optJSONObject("board_stop")
        val tripStopsArr = tripPlan.optJSONArray("trip_stops")
        val firstStop = if (tripStopsArr != null && tripStopsArr.length() > 0) tripStopsArr.optJSONObject(0) else boardStop
        val bLat = firstStop?.optDouble("latitude") ?: 0.0
        val bLng = firstStop?.optDouble("longitude") ?: 0.0

        if (bLat != 0.0 && bLng != 0.0 && originCoords.first != 0.0 && originCoords.second != 0.0) {
            val (pts, distM) = fetchWalkingRoute(api, originCoords.first, originCoords.second, bLat, bLng)
            walkingToBoardPath = pts
            walkingToBoardDist = distM.toInt()
        } else {
            walkingToBoardPath = emptyList()
            walkingToBoardDist = 0
        }

        if (destinationCoords != null && destinationCoords.first != 0.0 && destinationCoords.second != 0.0) {
            val alightStop = tripPlan.optJSONObject("alight_stop")
            val lastStop = if (tripStopsArr != null && tripStopsArr.length() > 0) tripStopsArr.optJSONObject(tripStopsArr.length() - 1) else alightStop
            val aLat = lastStop?.optDouble("latitude") ?: 0.0
            val aLng = lastStop?.optDouble("longitude") ?: 0.0
            if (aLat != 0.0 && aLng != 0.0) {
                val (pts, distM) = fetchWalkingRoute(api, aLat, aLng, destinationCoords.first, destinationCoords.second)
                walkingToDestPath = pts
                walkingToDestDist = distM.toInt()
            } else {
                walkingToDestPath = emptyList()
                walkingToDestDist = 0
            }
        } else {
            walkingToDestPath = emptyList()
            walkingToDestDist = 0
        }
    }

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

                    map.addOnCameraIdleListener {
                        val z = map.cameraPosition.zoom
                        if (Math.abs(z - currentMapZoom) >= 0.25 || (z >= 14.0 && currentMapZoom < 14.0) || (z < 14.0 && currentMapZoom >= 14.0)) {
                            currentMapZoom = z
                        }
                    }

                    map.setOnMarkerClickListener { marker ->
                        if (marker.title == "Vị trí của bạn" || marker.title?.contains("Vị trí của bạn") == true) return@setOnMarkerClickListener true
                        if (marker.title?.startsWith("Cluster:") == true) {
                            val nextZoom = (map.cameraPosition.zoom + 2.4).coerceAtMost(16.5)
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, nextZoom), 450)
                            return@setOnMarkerClickListener true
                        }
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

    LaunchedEffect(stops, mapLibreMap, tripPlan, selectedStop, pickingMode, walkingToBoardPath, walkingToDestPath, currentMapZoom) {
        val map = mapLibreMap ?: return@LaunchedEffect
        map.clear()

        if (tripPlan != null) {
            if (walkingToBoardPath.isNotEmpty()) {
                val walkPoly = PolylineOptions()
                    .addAll(walkingToBoardPath)
                    .color(android.graphics.Color.parseColor("#10B981"))
                    .width(5.0f)
                map.addPolyline(walkPoly)
            }

            val pathArray = tripPlan.optJSONArray("path_segment")
            val latLngs = mutableListOf<LatLng>()
            if (pathArray != null) {
                for (i in 0 until pathArray.length()) {
                    val pt = pathArray.optJSONArray(i) ?: continue
                    latLngs.add(LatLng(pt.optDouble(1), pt.optDouble(0)))
                }
            }
            if (latLngs.isNotEmpty()) {
                val poly = PolylineOptions()
                    .addAll(latLngs)
                    .color(android.graphics.Color.parseColor("#2563EB"))
                    .width(6.0f)
                map.addPolyline(poly)
            }

            if (walkingToDestPath.isNotEmpty()) {
                val destWalkPoly = PolylineOptions()
                    .addAll(walkingToDestPath)
                    .color(android.graphics.Color.parseColor("#10B981"))
                    .width(5.0f)
                map.addPolyline(destWalkPoly)
            }

            val userIcon = IconFactory.getInstance(context).fromBitmap(createUserLocationMarkerBitmap(context))
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(originCoords.first, originCoords.second))
                    .title(if (originCoords == userCoords) "Vị trí của bạn (Bắt đầu)" else "Điểm xuất phát")
                    .snippet(if (walkingToBoardDist > 0) "Đi bộ ~$walkingToBoardDist m đến trạm đón" else "Điểm bắt đầu hành trình")
                    .icon(userIcon)
            )

            if (originCoords != userCoords && userCoords.first != 0.0 && userCoords.second != 0.0) {
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(userCoords.first, userCoords.second))
                        .title("Vị trí GPS của bạn")
                        .snippet("Tọa độ hiện tại")
                        .icon(userIcon)
                )
            }

            if (destinationCoords != null && destinationCoords.first != 0.0 && destinationCoords.second != 0.0) {
                map.addMarker(
                    MarkerOptions()
                        .position(LatLng(destinationCoords.first, destinationCoords.second))
                        .title("Điểm đến của bạn")
                        .snippet(if (walkingToDestDist > 0) "Đi bộ ~$walkingToDestDist m từ trạm xuống" else "Đích đến hành trình")
                        .icon(IconFactory.getInstance(context).fromBitmap(createDestinationMarkerBitmap(context)))
                )
            }

            val tripStopsArr = tripPlan.optJSONArray("trip_stops")
            val boardStop = tripPlan.optJSONObject("board_stop")
            val alightStop = tripPlan.optJSONObject("alight_stop")
            val transferStop = tripPlan.optJSONObject("transfer_stop")

            if (tripStopsArr != null && tripStopsArr.length() > 0) {
                for (i in 0 until tripStopsArr.length()) {
                    val st = tripStopsArr.optJSONObject(i) ?: continue
                    val sLat = st.optDouble("latitude")
                    val sLng = st.optDouble("longitude")
                    if (sLat == 0.0 || sLng == 0.0) continue

                    val sName = st.optString("name")
                    val sAddr = st.optString("address")
                    val isFirst = (i == 0)
                    val isLast = (i == tripStopsArr.length() - 1)
                    val isTransfer = transferStop != null && (
                        (transferStop.optString("name").isNotBlank() && transferStop.optString("name") == sName) ||
                        (Math.abs(transferStop.optDouble("latitude") - sLat) < 0.0003 && Math.abs(transferStop.optDouble("longitude") - sLng) < 0.0003)
                    )

                    val markerBitmap = createIntermediateStopMarkerBitmap(context, i + 1)

                    val prefix = when {
                        isFirst -> "1. Trạm đón: "
                        isLast -> "${i + 1}. Trạm xuống: "
                        isTransfer -> "${i + 1}. Chuyển tuyến: "
                        else -> "${i + 1}. "
                    }

                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(sLat, sLng))
                            .title("$prefix$sName")
                            .snippet(sAddr.ifBlank { "Trạm xe buýt" })
                            .icon(IconFactory.getInstance(context).fromBitmap(markerBitmap))
                    )
                }
            } else {
                val busStopIcon = IconFactory.getInstance(context).fromBitmap(createBusStopMarkerBitmap(context))
                if (boardStop != null) {
                    val bLat = boardStop.optDouble("latitude")
                    val bLng = boardStop.optDouble("longitude")
                    if (bLat != 0.0 && bLng != 0.0) {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(bLat, bLng))
                                .title("Trạm đón: " + boardStop.optString("name"))
                                .snippet(boardStop.optString("address"))
                                .icon(busStopIcon)
                        )
                    }
                }

                if (transferStop != null) {
                    val tLat = transferStop.optDouble("latitude")
                    val tLng = transferStop.optDouble("longitude")
                    if (tLat != 0.0 && tLng != 0.0) {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(tLat, tLng))
                                .title("Chuyển tuyến tại: " + transferStop.optString("name"))
                                .snippet(transferStop.optString("address"))
                                .icon(busStopIcon)
                        )
                    }
                }

                if (alightStop != null) {
                    val aLat = alightStop.optDouble("latitude")
                    val aLng = alightStop.optDouble("longitude")
                    if (aLat != 0.0 && aLng != 0.0) {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(aLat, aLng))
                                .title("Trạm xuống: " + alightStop.optString("name"))
                                .snippet(alightStop.optString("address"))
                                .icon(busStopIcon)
                        )
                    }
                }
            }

            val boundsBuilder = LatLngBounds.Builder()
            var hasPoints = false

            boundsBuilder.include(LatLng(originCoords.first, originCoords.second))
            hasPoints = true

            if (originCoords != userCoords && userCoords.first != 0.0 && userCoords.second != 0.0) {
                boundsBuilder.include(LatLng(userCoords.first, userCoords.second))
            }

            walkingToBoardPath.forEach { boundsBuilder.include(it); hasPoints = true }
            latLngs.forEach { boundsBuilder.include(it); hasPoints = true }
            walkingToDestPath.forEach { boundsBuilder.include(it); hasPoints = true }

            if (destinationCoords != null && destinationCoords.first != 0.0 && destinationCoords.second != 0.0) {
                boundsBuilder.include(LatLng(destinationCoords.first, destinationCoords.second))
                hasPoints = true
            }

            if (hasPoints) {
                runCatching {
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120), 800)
                }
            }
        } else {
            val userIcon = IconFactory.getInstance(context).fromBitmap(createUserLocationMarkerBitmap(context))
            map.addMarker(
                MarkerOptions()
                    .position(LatLng(userCoords.first, userCoords.second))
                    .title("Vị trí của bạn")
                    .snippet("Tâm tìm kiếm trạm buýt")
                    .icon(userIcon)
            )

            if (pickingMode == null) {
                val clusters = clusterStops(stops.take(250), currentMapZoom)
                val stopIcon = IconFactory.getInstance(context).fromBitmap(createBusStopMarkerBitmap(context))

                clusters.forEach { cl ->
                    if (cl.stops.size == 1) {
                        val st = cl.stops[0]
                        val lat = st.doubleOrNull("latitude") ?: return@forEach
                        val lon = st.doubleOrNull("longitude") ?: return@forEach
                        val name = st.text("name")

                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(lat, lon))
                                .title(name)
                                .snippet(st.text("address"))
                                .icon(stopIcon)
                        )
                    } else {
                        val count = cl.stops.size
                        val clusterIcon = IconFactory.getInstance(context).fromBitmap(createClusterMarkerBitmap(context, count))
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(cl.centerLat, cl.centerLng))
                                .title("Cluster: $count trạm dừng")
                                .snippet("Nhấn để phóng to xem chi tiết")
                                .icon(clusterIcon)
                        )
                    }
                }
            }

            selectedStop?.let { sel ->
                val sLat = sel.doubleOrNull("latitude")
                val sLon = sel.doubleOrNull("longitude")
                if (sLat != null && sLon != null) {
                    val pin = when (pickingMode) {
                        "origin" -> createOriginMarkerBitmap(context)
                        "destination" -> createDestinationMarkerBitmap(context)
                        else -> createSelectedBusStopMarkerBitmap(context)
                    }
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(sLat, sLon))
                            .title(if (pickingMode != null) (if (pickingMode == "origin") "Điểm đi đã chọn" else "Điểm đến đã chọn") else sel.text("name"))
                            .snippet(sel.text("address"))
                            .icon(IconFactory.getInstance(context).fromBitmap(pin))
                    )
                }
            }

            map.setOnMarkerClickListener { marker ->
                if (marker.title == "Vị trí của bạn" || marker.title?.contains("Vị trí của bạn") == true) return@setOnMarkerClickListener true

                if (marker.title?.startsWith("Cluster:") == true) {
                    val nextZoom = (map.cameraPosition.zoom + 2.4).coerceAtMost(16.5)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, nextZoom), 450)
                    return@setOnMarkerClickListener true
                }

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

            map.addOnMapClickListener { latLng ->
                if (tripPlan != null) return@addOnMapClickListener false

                val closestStop = stops.minByOrNull { st ->
                    val sLat = st.doubleOrNull("latitude") ?: 0.0
                    val sLng = st.doubleOrNull("longitude") ?: 0.0
                    calcDistanceKm(latLng.latitude, latLng.longitude, sLat, sLng)
                }
                val distKm = closestStop?.let { st ->
                    calcDistanceKm(latLng.latitude, latLng.longitude, st.doubleOrNull("latitude") ?: 0.0, st.doubleOrNull("longitude") ?: 0.0)
                } ?: 999.0

                if (distKm < 0.4 && closestStop != null) {
                    val rawName = closestStop.optString("name")
                    val displayName = if (rawName.startsWith("[Trạm]")) rawName else "[Trạm] $rawName"
                    val stopObj = JSONObject().apply {
                        put("name", displayName)
                        put("address", closestStop.optString("address", "Hà Nội"))
                        put("latitude", closestStop.optDouble("latitude"))
                        put("longitude", closestStop.optDouble("longitude"))
                        put("routes", closestStop.optJSONArray("routes"))
                    }
                    onSelectStop(stopObj)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(closestStop.optDouble("latitude"), closestStop.optDouble("longitude")), 16.0), 400)
                } else {
                    val latStr = String.format(java.util.Locale.US, "%.4f", latLng.latitude)
                    val lngStr = String.format(java.util.Locale.US, "%.4f", latLng.longitude)
                    val customLoc = JSONObject().apply {
                        put("name", "Vị trí đã chọn ($latStr, $lngStr)")
                        put("address", String.format(java.util.Locale.US, "Tọa độ: %.5f, %.5f", latLng.latitude, latLng.longitude))
                        put("latitude", latLng.latitude)
                        put("longitude", latLng.longitude)
                    }
                    onSelectStop(customLoc)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.5), 400)
                }
                true
            }
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

    var isDrawerExpanded by remember { mutableStateOf(false) }
    var selectedStopIndex by remember { mutableStateOf<Int?>(null) }
    val tripStopsList = remember(tripPlan) {
        val arr = tripPlan?.optJSONArray("trip_stops")
        if (arr == null) emptyList() else (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        if (pickingMode != null) {
            Surface(
                color = if (pickingMode == "origin") Color(0xFF065F46) else OpenCityColors.BrandAzure,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp, start = 14.dp, end = 14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (pickingMode == "origin") Icons.Default.MyLocation else Icons.Default.Place,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (pickingMode == "origin") "Chạm trạm hoặc vị trí để chọn Điểm đi" else "Chạm trạm hoặc vị trí để chọn Điểm đến",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        onClick = onCancelPicking,
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Hủy",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        if (tripPlan != null) {
            val isTransfer = tripPlan.optString("type") == "transfer"
            val routeNum = tripPlan.optString("route_number")
            val secondRouteNum = tripPlan.optString("second_route_number")
            val estMins = tripPlan.optInt("estimated_minutes", 30)
            val stCount = tripStopsList.size.takeIf { it > 0 } ?: tripPlan.optInt("stops_count", 12)
            val co2 = tripPlan.optInt("co2_saved_grams", stCount * 95)
            val totalWalkDist = walkingToBoardDist + walkingToDestDist
            val walkMins = Math.max(1, (totalWalkDist / 75.0).toInt())

            Surface(
                color = OpenCityColors.CardBg,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = if (isTransfer) Color(0xFFF59E0B) else Color(0xFF2563EB),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isTransfer) "$routeNum → $secondRouteNum" else routeNum,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "~$estMins phút • $stCount trạm",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.TextMain
                                )
                                if (totalWalkDist > 0) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                                contentDescription = null,
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "Đi bộ ~${totalWalkDist}m (~$walkMins')",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Giảm ~${co2}g CO₂",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onClearTripPlan,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = OpenCityColors.BrandAzure, modifier = Modifier.size(14.dp))
                            Text("Quay lại", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = OpenCityColors.BrandAzure)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(top = 74.dp, end = 14.dp)
                    .align(Alignment.TopEnd),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = {
                        val map = mapLibreMap ?: return@Surface
                        val bBuilder = LatLngBounds.Builder()
                        var hasPts = false
                        bBuilder.include(LatLng(originCoords.first, originCoords.second)); hasPts = true
                        if (originCoords != userCoords && userCoords.first != 0.0 && userCoords.second != 0.0) {
                            bBuilder.include(LatLng(userCoords.first, userCoords.second))
                        }
                        walkingToBoardPath.forEach { bBuilder.include(it); hasPts = true }
                        val pathArr = tripPlan.optJSONArray("path_segment")
                        if (pathArr != null) {
                            for (i in 0 until pathArr.length()) {
                                val pt = pathArr.optJSONArray(i) ?: continue
                                bBuilder.include(LatLng(pt.optDouble(1), pt.optDouble(0)))
                                hasPts = true
                            }
                        }
                        walkingToDestPath.forEach { bBuilder.include(it); hasPts = true }
                        if (destinationCoords != null && destinationCoords.first != 0.0 && destinationCoords.second != 0.0) {
                            bBuilder.include(LatLng(destinationCoords.first, destinationCoords.second))
                            hasPts = true
                        }
                        if (hasPts) {
                            runCatching {
                                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bBuilder.build(), 120), 600)
                            }
                        }
                    },
                    shape = CircleShape,
                    color = OpenCityColors.CardBg,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Toàn cảnh lộ trình", tint = OpenCityColors.BrandAzure, modifier = Modifier.size(20.dp))
                    }
                }

                Surface(
                    onClick = {
                        mapLibreMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(userCoords.first, userCoords.second), 16.0),
                            500
                        )
                    },
                    shape = CircleShape,
                    color = OpenCityColors.CardBg,
                    shadowElevation = 5.dp,
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Vị trí của bạn", tint = Color(0xFF047857), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Surface(
                color = OpenCityColors.CardBg,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                shadowElevation = 14.dp,
                border = BorderStroke(1.dp, OpenCityColors.BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(OpenCityColors.BorderColor)
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = OpenCityColors.BrandAzureLight,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.FormatListNumbered,
                                        contentDescription = null,
                                        tint = OpenCityColors.BrandAzure,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Toàn bộ $stCount trạm dừng trên tuyến",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.TextMain
                                )
                                Text(
                                    text = "Chạm vào trạm để xem trên bản đồ",
                                    fontSize = 11.sp,
                                    color = OpenCityColors.TextMuted
                                )
                            }
                        }

                        Surface(
                            onClick = { isDrawerExpanded = !isDrawerExpanded },
                            color = OpenCityColors.SurfaceBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, OpenCityColors.BorderColor)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isDrawerExpanded) "Thu gọn" else "Xem trạm",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.BrandAzure
                                )
                                Icon(
                                    if (isDrawerExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                    contentDescription = null,
                                    tint = OpenCityColors.BrandAzure,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    if (isDrawerExpanded && tripStopsList.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = OpenCityColors.BorderColor.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (walkingToBoardDist > 0) {
                                item {
                                    val walkM = Math.max(1, (walkingToBoardDist / 75.0).toInt())
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                                contentDescription = null,
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Đi bộ ~$walkingToBoardDist m (~$walkM phút) từ vị trí của bạn đến trạm đón",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }
                                }
                            }

                            itemsIndexed(tripStopsList) { idx, stopObj ->
                                val isFirst = idx == 0
                                val isLast = idx == tripStopsList.size - 1
                                val sLat = stopObj.optDouble("latitude")
                                val sLng = stopObj.optDouble("longitude")
                                val sName = stopObj.optString("name")
                                val sAddress = stopObj.optString("address")
                                val isSelected = selectedStopIndex == idx

                                Surface(
                                    onClick = {
                                        selectedStopIndex = idx
                                        if (sLat != 0.0 && sLng != 0.0) {
                                            mapLibreMap?.animateCamera(
                                                CameraUpdateFactory.newLatLngZoom(LatLng(sLat, sLng), 16.5),
                                                500
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) OpenCityColors.BrandAzureLight else OpenCityColors.SurfaceBg,
                                    border = if (isSelected) BorderStroke(1.dp, OpenCityColors.BrandAzure) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = OpenCityColors.BrandAzure,
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${idx + 1}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OpenCityColors.TextMain,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (sAddress.isNotBlank()) {
                                                Text(
                                                    text = sAddress,
                                                    fontSize = 10.5.sp,
                                                    color = OpenCityColors.TextMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.GpsFixed,
                                            contentDescription = "Xem",
                                            tint = if (isSelected) OpenCityColors.BrandAzure else OpenCityColors.TextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }

                            if (destinationCoords != null && walkingToDestDist > 0) {
                                item {
                                    val walkDestM = Math.max(1, (walkingToDestDist / 75.0).toInt())
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                                contentDescription = null,
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Đi bộ ~$walkingToDestDist m (~$walkDestM phút) từ trạm xuống đến điểm đến",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (pickingMode == null) {
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
            val sLat = st.doubleOrNull("latitude") ?: 21.0
            val sLng = st.doubleOrNull("longitude") ?: 105.8
            val dist = calcDistanceKm(userCoords.first, userCoords.second, sLat, sLng)
            val routesArr = st.arrOrNull("routes")
            val routesList = if (routesArr == null) emptyList() else (0 until routesArr.length()).map { routesArr.getString(it) }

            Surface(
                color = OpenCityColors.CardBg,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, OpenCityColors.BrandAzureBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.TextMain,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = address.ifBlank { "Hà Nội" },
                                fontSize = 11.5.sp,
                                color = OpenCityColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = OpenCityColors.BrandMintLight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Cách ${formatDistanceKm(dist)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OpenCityColors.BrandMintDark,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                onClick = { onSelectStop(null) },
                                shape = CircleShape,
                                color = OpenCityColors.SurfaceBg,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Đóng",
                                        tint = OpenCityColors.TextMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (pickingMode == null && routesList.isNotEmpty()) {
                        Text(
                            text = "Các tuyến xe buýt đón tại trạm này:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OpenCityColors.TextSecondary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(routesList) { rNum ->
                                Surface(
                                    color = OpenCityColors.BrandAzure,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { onSelectRouteFromStop(rNum) }
                                ) {
                                    Text(
                                        text = rNum,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = OpenCityColors.BorderColor.copy(alpha = 0.5f))

                    if (pickingMode == "origin") {
                        Button(
                            onClick = { onSetAsOrigin(name, Pair(sLat, sLng)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Xác nhận Điểm xuất phát",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else if (pickingMode == "destination") {
                        Button(
                            onClick = { onSetAsDestination(name, Pair(sLat, sLng)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenCityColors.BrandAzure)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Xác nhận Điểm đến",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onSetAsOrigin(name, Pair(sLat, sLng)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981).copy(alpha = 0.12f),
                                    contentColor = Color(0xFF047857)
                                ),
                                border = BorderStroke(1.dp, Color(0xFF10B981))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = Color(0xFF047857),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Đi từ đây",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                }
                            }

                            Button(
                                onClick = { onSetAsDestination(name, Pair(sLat, sLng)) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = OpenCityColors.BrandAzure
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Đến đây",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
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
    onDismiss: () -> Unit,
    onViewOnMap: (JSONObject) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var detail by remember { mutableStateOf<JSONObject?>(null) }
    var loadingDetail by remember { mutableStateOf(true) }
    var direction by remember { mutableStateOf("forward") } // chieu di va chieu ve
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
                            text = "Chiều đi (${forwardStops.size.takeIf { it > 0 } ?: routeSummary.intOrNull("total_stops_forward") ?: 0} trạm)",
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
                            text = "Chiều về (${backwardStops.size.takeIf { it > 0 } ?: routeSummary.intOrNull("total_stops_backward") ?: 0} trạm)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (direction == "backward") Color.White else OpenCityColors.TextSecondary
                        )
                    }
                }

                Button(
                    onClick = {
                        val activeStopsList = if (direction == "forward") forwardStops else backwardStops
                        val pathArr = if (direction == "forward") detail?.arrOrNull("forward_path") else detail?.arrOrNull("backward_path")
                        val stopsArr = JSONArray().apply {
                            activeStopsList.forEach { put(it) }
                        }
                        val mapPlanObj = JSONObject().apply {
                            put("type", "direct")
                            put("route_number", routeNum)
                            put("route_name", name)
                            put("estimated_minutes", (activeStopsList.size.takeIf { it > 0 } ?: 15) * 2)
                            put("stops_count", activeStopsList.size)
                            put("co2_saved_grams", activeStopsList.size * 95)
                            put("path_segment", pathArr ?: JSONArray())
                            put("trip_stops", stopsArr)
                            if (activeStopsList.isNotEmpty()) {
                                put("board_stop", activeStopsList.first())
                                put("alight_stop", activeStopsList.last())
                            }
                        }
                        onDismiss()
                        onViewOnMap(mapPlanObj)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Xem toàn bộ ${activeStops.size} trạm trên bản đồ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
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
    val size = (30 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x2E000000
    }
    canvas.drawCircle(center, center + 1.2f * density, 12f * density, shadowPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF1D4ED8.toInt()
    }
    canvas.drawCircle(center, center, 11f * density, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 11f * density, strokePaint)

    val busPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1D4ED8.toInt()
        style = Paint.Style.FILL
    }

    val bW = 12f * density
    val bH = 14f * density
    val bL = center - bW / 2f
    val bT = center - bH / 2f
    val bR = center + bW / 2f
    val bB = center + bH / 2f - 1.2f * density
    val corner = 2.2f * density

    val body = android.graphics.RectF(bL, bT, bR, bB)
    canvas.drawRoundRect(body, corner, corner, busPaint)

    val wheelR = 1.2f * density
    canvas.drawCircle(bL + bW * 0.25f, bB + wheelR * 0.6f, wheelR, busPaint)
    canvas.drawCircle(bR - bW * 0.25f, bB + wheelR * 0.6f, wheelR, busPaint)

    val winW = bW * 0.74f
    val winH = bH * 0.35f
    val winRect = android.graphics.RectF(center - winW / 2f, bT + 1.8f * density, center + winW / 2f, bT + 1.8f * density + winH)
    canvas.drawRoundRect(winRect, 1.2f * density, 1.2f * density, winPaint)

    val headLightR = 0.8f * density
    val lightY = bB - 2.2f * density
    canvas.drawCircle(bL + bW * 0.24f, lightY, headLightR, winPaint)
    canvas.drawCircle(bR - bW * 0.24f, lightY, headLightR, winPaint)

    return bitmap
}

private fun createClusterMarkerBitmap(context: Context, count: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val (bgColor, radiusDp, textSizeSp) = when {
        count < 15 -> Triple(0xFF0284C7.toInt(), 18f, 12f)
        count < 40 -> Triple(0xFF2563EB.toInt(), 22f, 13f)
        count < 100 -> Triple(0xFF1D4ED8.toInt(), 25f, 14f)
        else -> Triple(0xFF7C3AED.toInt(), 28f, 15f)
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

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = bgColor
    }
    canvas.drawCircle(center, center, r, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, r, strokePaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = textSizeSp * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val textY = center - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(count.toString(), center, textY, textPaint)

    return bitmap
}

private fun createSelectedBusStopMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (38 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x332563EB
    }
    canvas.drawCircle(center, center, 17f * density, glowPaint)

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x38000000
    }
    canvas.drawCircle(center, center + 1.5f * density, 14f * density, shadowPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF1E40AF.toInt()
    }
    canvas.drawCircle(center, center, 13f * density, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 13f * density, strokePaint)

    val busPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val winPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1E40AF.toInt()
        style = Paint.Style.FILL
    }

    val bW = 14f * density
    val bH = 16f * density
    val bL = center - bW / 2f
    val bT = center - bH / 2f
    val bR = center + bW / 2f
    val bB = center + bH / 2f - 1.5f * density
    val corner = 2.5f * density

    val body = android.graphics.RectF(bL, bT, bR, bB)
    canvas.drawRoundRect(body, corner, corner, busPaint)

    val wheelR = 1.4f * density
    canvas.drawCircle(bL + bW * 0.25f, bB + wheelR * 0.6f, wheelR, busPaint)
    canvas.drawCircle(bR - bW * 0.25f, bB + wheelR * 0.6f, wheelR, busPaint)

    val winW = bW * 0.74f
    val winH = bH * 0.35f
    val winRect = android.graphics.RectF(center - winW / 2f, bT + 2f * density, center + winW / 2f, bT + 2f * density + winH)
    canvas.drawRoundRect(winRect, 1.3f * density, 1.3f * density, winPaint)

    val headLightR = 0.9f * density
    val lightY = bB - 2.5f * density
    canvas.drawCircle(bL + bW * 0.24f, lightY, headLightR, winPaint)
    canvas.drawCircle(bR - bW * 0.24f, lightY, headLightR, winPaint)

    return bitmap
}

private fun createIntermediateStopMarkerBitmap(context: Context, number: Int): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (26 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x26000000
    }
    canvas.drawCircle(center, center + 1f * density, 11f * density, shadowPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 10f * density, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = 0xFF2563EB.toInt()
    }
    canvas.drawCircle(center, center, 10f * density, strokePaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF2563EB.toInt()
        textSize = (if (number >= 100) 8.5f else if (number >= 10) 9.5f else 11.5f) * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val yOffset = (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(number.toString(), center, center - yOffset, textPaint)

    return bitmap
}

private fun createOriginMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (32 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33000000
    }
    canvas.drawCircle(center, center + 1 * density, 13f * density, shadowPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF10B981.toInt()
    }
    canvas.drawCircle(center, center, 12f * density, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 12f * density, strokePaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 4.5f * density, dotPaint)

    return bitmap
}

private fun createDestinationMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (32 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33000000
    }
    canvas.drawCircle(center, center + 1 * density, 13f * density, shadowPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFEF4444.toInt()
    }
    canvas.drawCircle(center, center, 12f * density, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 12f * density, strokePaint)

    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 4.5f * density, dotPaint)

    return bitmap
}

private fun createTransferMarkerBitmap(context: Context): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (34 * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x33000000
    }
    canvas.drawCircle(center, center + 1.5f * density, 14f * density, shadowPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFF59E0B.toInt()
    }
    canvas.drawCircle(center, center, 13f * density, bgPaint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 13f * density, strokePaint)

    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = android.graphics.Color.WHITE
    }
    canvas.drawCircle(center, center, 5f * density, innerPaint)

    return bitmap
}
