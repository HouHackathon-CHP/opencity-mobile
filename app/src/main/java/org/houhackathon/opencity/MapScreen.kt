package org.houhackathon.opencity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private data class Point(val id: String, val kind: String, val title: String, val lat: Double, val lon: Double)

private val osmStyle = """
{"version":8,"sources":{"openstreetmap":{"type":"raster","tiles":["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],"tileSize":256,"attribution":"© OpenStreetMap contributors"}},"layers":[{"id":"openstreetmap","type":"raster","source":"openstreetmap"}]}
""".trimIndent()

@Composable
fun MapScreen(
    api: ApiClient,
    revision: Int,
    onDetail: (String, String) -> Unit
) {
    var points by remember { mutableStateOf<List<Point>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("all") }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(revision, reload) {
        loading = true
        try {
            val reports = api.list("/reports").mapNotNull { it.asPoint("report") }
            val alerts = api.list("/bulletins").filter { it.text("category") == "alert" }.mapNotNull { it.asPoint("bulletin") }
            points = reports + alerts
            error = null
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    val visible = points.filter { filter == "all" || it.kind == filter }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OpenCityColors.SurfaceBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Bản đồ Đô thị số",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = OpenCityColors.TextMain
                )
                Text(
                    text = "Hiển thị tọa độ phản ánh hiện trường & cảnh báo an toàn",
                    fontSize = 12.sp,
                    color = OpenCityColors.TextSecondary
                )
            }

            IconButton(
                onClick = { reload++ },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.White)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Làm mới", tint = OpenCityColors.BrandAzure)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("all" to "Tất cả", "report" to "Phản ánh hiện trường", "bulletin" to "Cảnh báo khẩn").forEach { (key, label) ->
                val isSelected = filter == key
                FilterChip(
                    selected = isSelected,
                    onClick = { filter = key },
                    label = {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (key == "bulletin") OpenCityColors.BrandRed else OpenCityColors.BrandAzure,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = OpenCityColors.TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) (if (key == "bulletin") OpenCityColors.BrandRed else OpenCityColors.BrandAzure) else OpenCityColors.BorderColor
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        error?.let { ErrorBanner(it) }

        // Map View Container
        Surface(
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, OpenCityColors.BorderColor),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                MapPanel(visible, Modifier.fillMaxSize())

                // Attribution chip overlay
                Surface(
                    color = Color(0xCC0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "© OpenStreetMap contributors",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = OpenCityColors.BrandAzure)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Points List Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Các điểm ghi nhận trên bản đồ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = OpenCityColors.TextMain
            )
            Surface(
                color = OpenCityColors.BrandAzureLight,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "${visible.size} vị trí",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OpenCityColors.BrandAzureDark
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (visible.isEmpty() && !loading) {
            EmptyPlaceholder("Chưa có phản ánh hoặc cảnh báo nào có tọa độ GPS trong danh mục này.")
        } else {
            visible.forEach { point ->
                val isAlert = point.kind == "bulletin"
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { onDetail(point.kind, point.id) },
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isAlert) OpenCityColors.BrandRedBorder else OpenCityColors.BorderColor),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isAlert) OpenCityColors.BrandRedLight else OpenCityColors.BrandAzureLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isAlert) Icons.Default.Warning else Icons.Default.Place,
                                contentDescription = null,
                                tint = if (isAlert) OpenCityColors.BrandRedDark else OpenCityColors.BrandAzureDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = if (isAlert) OpenCityColors.BrandRedLight else OpenCityColors.BrandAzureLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isAlert) "CẢNH BÁO" else "PHẢN ÁNH",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAlert) OpenCityColors.BrandRedDark else OpenCityColors.BrandAzureDark
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = point.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = OpenCityColors.TextMain,
                                maxLines = 1
                            )

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = "Tọa độ: %.5f, %.5f".format(point.lat, point.lon),
                                fontSize = 11.sp,
                                color = OpenCityColors.TextLight
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Chi tiết",
                            tint = OpenCityColors.BrandAzure,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private fun JSONObject.asPoint(kind: String): Point? {
    val lat = number("latitude") ?: return null
    val lon = number("longitude") ?: return null
    return Point(text("id"), kind, text("title"), lat, lon)
}

@Composable
private fun MapPanel(points: List<Point>, modifier: Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(mapView, owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)

    LaunchedEffect(points) {
        mapView.getMapAsync { map ->
            map.setStyle(Style.Builder().fromJson(osmStyle)) {
                map.clear()
                points.forEach { point ->
                    map.addMarker(MarkerOptions().position(LatLng(point.lat, point.lon)).title(point.title))
                }
                val target = points.firstOrNull()?.let { LatLng(it.lat, it.lon) } ?: LatLng(21.0285, 105.8542)
                map.cameraPosition = CameraPosition.Builder().target(target).zoom(if (points.isEmpty()) 10.0 else 12.0).build()
            }
        }
    }
}
