package com.ajimsjames.wearbaroalt.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import java.util.Locale
import kotlin.math.*

enum class BaroTab {
    BAROMETER,
    ALTIMETER,
    STORM_ALERT,
    SETTINGS
}

@Composable
fun BaroAltScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(BaroTab.BAROMETER) }

    var pressureHpa by remember { mutableStateOf(1013.25f) }
    var seaLevelQnhHpa by remember { mutableStateOf(1013.25f) }
    var pressureHistory by remember { mutableStateOf(listOf(1014.5f, 1014.2f, 1013.9f, 1013.6f, 1013.25f)) }
    var showQnhDialog by remember { mutableStateOf(false) }

    // Altitude computations
    val currentAltitudeMeters = SensorManager.getAltitude(seaLevelQnhHpa, pressureHpa)
    val currentAltitudeFeet = currentAltitudeMeters * 3.28084f
    val pressureInHg = pressureHpa * 0.02953f

    // Severe Storm Warning Detector (> 2.5 hPa drop in recent history)
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
                        if (pressureHistory.isEmpty() || abs(pressureHistory.last() - pressureHpa) > 0.1f) {
                            pressureHistory = (pressureHistory.takeLast(24) + pressureHpa)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        baroSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI) }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E11))
    ) {
        // Main Content Scroll Area
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(top = 36.dp, bottom = 44.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedTab) {
                BaroTab.BAROMETER -> {
                    // Aero Circular Gauge
                    item {
                        Box(
                            modifier = Modifier
                                .size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 8.dp.toPx()
                                // Background Arc
                                drawArc(
                                    color = Color(0x33FFAB00),
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                // Active Progress Arc (norm 950 - 1050 hPa)
                                val progress = ((pressureHpa - 950f) / 100f).coerceIn(0f, 1f)
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(Color(0xFFFFD54F), Color(0xFFFF6D00))),
                                    startAngle = 135f,
                                    sweepAngle = 270f * progress,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.1f", pressureHpa),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("hPa / mbar", color = Color(0xFFFFAB00), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 24h Pressure Sparkline Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF16181D))
                                .padding(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("📈 24h Trend", color = Color(0xFF90CAF9), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (pressureDrop > 0.5f) "Falling (-%.1f)".format(pressureDrop) else "Stable",
                                    color = if (isStormAlert) Color(0xFFFF1744) else Color(0xFF00E676),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (pressureHistory.size > 1) {
                                        val maxP = (pressureHistory.maxOrNull() ?: 1020f) + 0.5f
                                        val minP = (pressureHistory.minOrNull() ?: 1000f) - 0.5f
                                        val rangeP = (maxP - minP).coerceAtLeast(0.5f)

                                        val path = Path()
                                        val stepX = size.width / (pressureHistory.size - 1)

                                        pressureHistory.forEachIndexed { idx, valP ->
                                            val x = idx * stepX
                                            val normY = (valP - minP) / rangeP
                                            val y = size.height - (normY * (size.height - 6f)) - 3f
                                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        }

                                        drawPath(
                                            path = path,
                                            color = Color(0xFFFFAB00),
                                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // InHg Telemetry Card
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF16181D))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mercury:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format(Locale.US, "%.2f inHg", pressureInHg), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                BaroTab.ALTIMETER -> {
                    // Altitude Dial Gauge
                    item {
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 8.dp.toPx()
                                drawArc(
                                    color = Color(0x3300E5FF),
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                val progress = ((currentAltitudeMeters.coerceIn(0f, 3000f)) / 3000f)
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(Color(0xFF00E5FF), Color(0xFF00E676))),
                                    startAngle = 135f,
                                    sweepAngle = 270f * progress,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.0f", currentAltitudeMeters),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("METERS", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Imperial Feet & QNH Reference Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF16181D))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Elevation (Feet):", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format(Locale.US, "%.0f ft", currentAltitudeFeet), color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("QNH Datum:", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format(Locale.US, "%.1f hPa", seaLevelQnhHpa), color = Color(0xFFFFAB00), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Calibrate QNH Sea-Level Pressure Button
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF21242D))
                                .clickable { showQnhDialog = true }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙️ Calibrate Sea-Level QNH", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                BaroTab.STORM_ALERT -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isStormAlert) Color(0xFF3B0B11) else Color(0xFF0B291A))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (isStormAlert) "⚠️ RAPID DROP DETECTED!" else "🌤️ PRESSURE STABLE",
                                color = if (isStormAlert) Color(0xFFFF1744) else Color(0xFF00E676),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isStormAlert)
                                    "Pressure dropped by %.1f hPa. Rain or severe weather expected.".format(pressureDrop)
                                else
                                    "Atmosphere is steady at %.1f hPa. No severe storms forecasted.".format(pressureHpa),
                                color = Color.LightGray,
                                fontSize = 9.5.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                BaroTab.SETTINGS -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF16181D))
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎈 WearBaroAlt v2.1.0", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Material 3 Aero Edition", color = Color(0xFFFFAB00), fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• LPS28DFW Direct Sensor Telemetry", color = Color.Gray, fontSize = 8.5.sp)
                            Text("• 24h Real-time Sparkline Buffer", color = Color.Gray, fontSize = 8.5.sp)
                            Text("• QNH Altimeter Calibrator", color = Color.Gray, fontSize = 8.5.sp)
                        }
                    }
                }
            }
        }

        // Top Curved Bezel Title / Status
        CurvedLayout(
            anchor = 270f,
            anchorType = androidx.wear.compose.foundation.AnchorType.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            curvedComposable {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xEE16181D))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (selectedTab) {
                            BaroTab.BAROMETER -> "🎈 Baro • %.1f hPa".format(pressureHpa)
                            BaroTab.ALTIMETER -> "⛰️ Alt • %.0f m".format(currentAltitudeMeters)
                            BaroTab.STORM_ALERT -> "⛈️ Storm Warning"
                            BaroTab.SETTINGS -> "⚙️ Settings"
                        },
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Bottom Curved Bezel Navigation Buttons
        CurvedLayout(
            anchor = 90f,
            anchorType = androidx.wear.compose.foundation.AnchorType.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            curvedComposable {
                MaterialNavPill("Baro", selected = selectedTab == BaroTab.BAROMETER) { selectedTab = BaroTab.BAROMETER }
            }
            curvedComposable { Spacer(modifier = Modifier.width(4.dp)) }
            curvedComposable {
                MaterialNavPill("Alt", selected = selectedTab == BaroTab.ALTIMETER) { selectedTab = BaroTab.ALTIMETER }
            }
            curvedComposable { Spacer(modifier = Modifier.width(4.dp)) }
            curvedComposable {
                MaterialNavPill("Storm", selected = selectedTab == BaroTab.STORM_ALERT) { selectedTab = BaroTab.STORM_ALERT }
            }
            curvedComposable { Spacer(modifier = Modifier.width(4.dp)) }
            curvedComposable {
                MaterialNavPill("Info", selected = selectedTab == BaroTab.SETTINGS) { selectedTab = BaroTab.SETTINGS }
            }
        }

        // QNH Stepper Calibration Dialog
        if (showQnhDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF0000000))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF16181D))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("⚙️ Calibrate QNH", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Text(
                        text = "%.1f hPa".format(seaLevelQnhHpa),
                        color = Color(0xFFFFAB00),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF21242D))
                                .clickable { seaLevelQnhHpa -= 1f },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("−", color = Color(0xFF00E5FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF21242D))
                                .clickable { seaLevelQnhHpa += 1f },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color(0xFF00E5FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF00E5FF))
                            .clickable { showQnhDialog = false }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Done", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun MaterialNavPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFFFAB00) else Color(0xFF21242D))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color(0xFFB0B3B8),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

