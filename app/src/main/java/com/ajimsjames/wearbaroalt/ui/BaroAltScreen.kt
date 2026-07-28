package com.ajimsjames.wearbaroalt.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material.Text
import java.util.Locale

enum class BaroTab {
    BAROMETER,
    ALTIMETER,
    STORM_ALERT,
    ABOUT
}

@Composable
fun BaroAltScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(BaroTab.BAROMETER) }

    var pressureHpa by remember { mutableStateOf(1013.25f) }
    var seaLevelQnhHpa by remember { mutableStateOf(1013.25f) }
    var pressureHistory by remember { mutableStateOf(listOf(1015.2f, 1014.8f, 1014.1f, 1013.8f, 1013.25f)) }

    val currentAltitudeMeters = SensorManager.getAltitude(seaLevelQnhHpa, pressureHpa)
    val currentAltitudeFeet = currentAltitudeMeters * 3.28084f

    val pressureInHg = pressureHpa * 0.02953f

    // Pressure Trend Check (Storm Alert detector: > 2.5 hPa drop)
    val pressureDrop = if (pressureHistory.size >= 2) pressureHistory.first() - pressureHistory.last() else 0f
    val isStormAlert = pressureDrop > 2.5f

    // Sensor Listener
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val baroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let { e ->
                    if (e.sensor.type == Sensor.TYPE_PRESSURE && e.values.isNotEmpty()) {
                        pressureHpa = e.values[0]
                        if (pressureHistory.isEmpty() || Math.abs(pressureHistory.last() - pressureHpa) > 0.1f) {
                            pressureHistory = (pressureHistory.takeLast(19) + pressureHpa)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        baroSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 40.dp, bottom = 24.dp)
        ) {
            when (selectedTab) {
                BaroTab.BAROMETER -> {
                    item {
                        Text(
                            text = "🎈 LPS28DFW Barometer",
                            color = Color(0xFFFFB300),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        // Live Pressure Circle Gauge
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF241D10)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", pressureHpa),
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("hPa / mbar", color = Color(0xFFFFB300), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }

                    item {
                        // Barometer Trend Graph
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(4.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                if (pressureHistory.size > 1) {
                                    val maxP = (pressureHistory.maxOrNull() ?: 1020f) + 1f
                                    val minP = (pressureHistory.minOrNull() ?: 1000f) - 1f
                                    val rangeP = (maxP - minP).coerceAtLeast(1f)

                                    val path = Path()
                                    val stepX = size.width / (pressureHistory.size - 1)

                                    pressureHistory.forEachIndexed { idx, valP ->
                                        val x = idx * stepX
                                        val normY = (valP - minP) / rangeP
                                        val y = size.height - (normY * (size.height - 8f)) - 4f
                                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                    }

                                    drawPath(
                                        path = path,
                                        color = Color(0xFFFFB300),
                                        style = Stroke(width = 2.5.dp.toPx())
                                    )
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Inches of Mercury:", color = Color.Gray, fontSize = 9.sp)
                                Text(String.format(Locale.US, "%.2f inHg", pressureInHg), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("3-Hour Trend:", color = Color.Gray, fontSize = 9.sp)
                                Text(
                                    text = if (pressureDrop > 0.5f) "Falling (-${String.format(Locale.US, "%.1f", pressureDrop)} hPa)" else "Stable",
                                    color = if (isStormAlert) Color(0xFFFF1744) else Color(0xFF00E676),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                BaroTab.ALTIMETER -> {
                    item {
                        Text(
                            text = "⛰️ QNH Sea-Level Altimeter",
                            color = Color(0xFF81D4FA),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF101B24)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.0f", currentAltitudeMeters),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("METERS", color = Color(0xFF81D4FA), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(6.dp)) }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Altitude (Feet):", color = Color.Gray, fontSize = 9.5.sp)
                                Text(String.format(Locale.US, "%.0f ft", currentAltitudeFeet), color = Color(0xFF00E676), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("QNH Sea Pressure:", color = Color.Gray, fontSize = 9.5.sp)
                                Text(String.format(Locale.US, "%.2f hPa", seaLevelQnhHpa), color = Color(0xFFFFB300), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                BaroTab.STORM_ALERT -> {
                    item {
                        Text(
                            text = "⛈️ Storm Drop Warning",
                            color = if (isStormAlert) Color(0xFFFF1744) else Color(0xFF00E676),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isStormAlert) Color(0xFF33080A) else Color(0xFF0A2214))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isStormAlert) "⚠️ RAPID PRESSURE DROP DETECTED!" else "🌤️ STABLE ATMOSPHERIC PRESSURE",
                                color = if (isStormAlert) Color(0xFFFF5252) else Color(0xFF00E676),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isStormAlert)
                                    "Pressure dropped by ${String.format(Locale.US, "%.1f", pressureDrop)} hPa. Approaching rain/storm expected."
                                else
                                    "Atmospheric pressure is steady at ${String.format(Locale.US, "%.1f", pressureHpa)} hPa.",
                                color = Color.LightGray,
                                fontSize = 9.5.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                BaroTab.ABOUT -> {
                    item {
                        Text(
                            text = "⚙️ About App",
                            color = Color(0xFFFFB300),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎈 WearBaroAlt v1.0.0", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("By Aju George", color = Color.Gray, fontSize = 9.5.sp, modifier = Modifier.padding(bottom = 6.dp))

                            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
                                Text("• LPS28DFW Hardware Barometer Polling", color = Color.LightGray, fontSize = 8.5.sp)
                                Text("• QNH Sea-Level Altimeter Engine", color = Color.LightGray, fontSize = 8.5.sp)
                                Text("• Rapid Storm Drop Pressure Warning", color = Color.LightGray, fontSize = 8.5.sp)
                                Text("• Target: Samsung Galaxy Watch 6", color = Color(0xFFFFB300), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Curved Bezel Top Navigation Bar
        CurvedLayout(
            anchor = 270f,
            modifier = Modifier.fillMaxSize()
        ) {
            curvedComposable {
                BezelTabPill("📉 Baro", selected = selectedTab == BaroTab.BAROMETER) { selectedTab = BaroTab.BAROMETER }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                BezelTabPill("⛰️ Alt", selected = selectedTab == BaroTab.ALTIMETER) { selectedTab = BaroTab.ALTIMETER }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                BezelTabPill("⛈️ Storm", selected = selectedTab == BaroTab.STORM_ALERT) { selectedTab = BaroTab.STORM_ALERT }
            }
            curvedComposable { Spacer(modifier = Modifier.width(3.dp)) }
            curvedComposable {
                BezelTabPill("⚙️ About", selected = selectedTab == BaroTab.ABOUT) { selectedTab = BaroTab.ABOUT }
            }
        }
    }
}

@Composable
fun BezelTabPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFFFB300) else Color(0xFF2C2C2E))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.Gray,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
