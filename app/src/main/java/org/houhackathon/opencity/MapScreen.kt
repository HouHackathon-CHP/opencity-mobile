package org.houhackathon.opencity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
fun MapScreen(api: ApiClient, revision: Int, onDetail: (String, String) -> Unit) {
    var points by remember { mutableStateOf<List<Point>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by remember { mutableStateOf("all") }
    LaunchedEffect(revision) {
        try {
            val reports = api.list("/reports").mapNotNull { it.asPoint("report") }
            val alerts = api.list("/bulletins").filter { it.text("category") == "alert" }.mapNotNull { it.asPoint("bulletin") }
            points = reports + alerts
            error = null
        } catch (e: Exception) { error = e.message }
    }
    val visible = points.filter { filter == "all" || it.kind == filter }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Bản đồ đô thị", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("all" to "Tất cả", "report" to "Phản ánh", "bulletin" to "Cảnh báo").forEach { (key, label) ->
                FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label) })
            }
        }
        error?.let { ErrorText(it) }
        MapPanel(visible, Modifier.fillMaxWidth().height(390.dp))
        Text("© OpenStreetMap contributors", style = MaterialTheme.typography.labelSmall)
        SectionTitle("Điểm trên bản đồ")
        if (visible.isEmpty()) Text("Chưa có phản ánh hoặc cảnh báo có tọa độ.")
        visible.forEach { point ->
            ElevatedCard(Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onDetail(point.kind, point.id) }) {
                Column(Modifier.padding(12.dp)) {
                    Text(point.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (point.kind == "report") "Phản ánh của tôi" else "Cảnh báo")
                }
            }
        }
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
        onDispose { owner.lifecycle.removeObserver(observer); mapView.onPause(); mapView.onStop(); mapView.onDestroy() }
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
