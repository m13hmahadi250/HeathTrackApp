package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.salat.CompassAccuracy
import com.example.salat.QiblaCalculator
import com.example.salat.QiblaCompassManager
import com.example.salat.QiblaData
import com.example.util.HapticsHelper
import kotlin.math.*

@Composable
fun SalatQiblaCompass(
    qiblaData: QiblaData,
    hapticEnabled: Boolean = true,
    onOpenLocationPicker: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val compassManager = remember {
        QiblaCompassManager(
            context = context,
            userLatitude = qiblaData.latitude,
            userLongitude = qiblaData.longitude
        )
    }

    LaunchedEffect(qiblaData.latitude, qiblaData.longitude) {
        compassManager.setLocation(qiblaData.latitude, qiblaData.longitude)
    }

    DisposableEffect(Unit) {
        compassManager.startListening()
        onDispose {
            compassManager.stopListening()
        }
    }

    val compassState by compassManager.compassState.collectAsStateWithLifecycle()

    // Smooth device heading with shortest-angle transition
    var previousHeading by remember { mutableFloatStateOf(compassState.deviceHeading) }
    var continuousHeading by remember { mutableFloatStateOf(compassState.deviceHeading) }

    LaunchedEffect(compassState.deviceHeading) {
        val diff = ((compassState.deviceHeading - previousHeading + 540f) % 360f) - 180f
        continuousHeading += diff
        previousHeading = compassState.deviceHeading
    }

    val animatedHeading by animateFloatAsState(
        targetValue = continuousHeading,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "compass_heading"
    )

    // Current normalized heading (0..360, where 0 is True North)
    val currentTrueHeading = ((animatedHeading % 360f) + 360f) % 360f
    val qiblaBearing = qiblaData.bearingDegrees

    // Angle difference from current device heading to Qibla:
    // positive: Kaaba is to user's right by diff degrees
    // negative: Kaaba is to user's left by abs(diff) degrees
    val angleDifference = ((qiblaBearing - currentTrueHeading + 540f) % 360f) - 180f
    val isFacingQibla = abs(angleDifference) <= 4.0f

    var wasFacingQibla by remember { mutableStateOf(false) }
    LaunchedEffect(isFacingQibla) {
        if (isFacingQibla && !wasFacingQibla && hapticEnabled) {
            HapticsHelper.performClick(context, true)
        }
        wasFacingQibla = isFacingQibla
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("salat_qibla_compass_section"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Location & Makkah Distance Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Qibla Direction (Makkah)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (onOpenLocationPicker != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = onOpenLocationPicker,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditLocation,
                                    contentDescription = "Change Location",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${qiblaData.locationName} • Bearing: ${qiblaData.bearingDegrees.roundToInt()}° (${qiblaData.cardinalDirection})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Lat: %.4f°, Lon: %.4f°".format(qiblaData.latitude, qiblaData.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "${qiblaData.bearingDegrees.roundToInt()}°",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${qiblaData.distanceKm} km",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Sensor Alert / Calibration Banner if needed
        if (!compassState.isSensorAvailable) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SensorsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Hardware compass sensor not active on this device. Align your phone toward ${qiblaData.bearingDegrees.roundToInt()}° (${qiblaData.cardinalDirection}) using the fixed bearing marker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        } else if (compassState.needsCalibration) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.RotateRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Calibrating compass: Move away from magnets/metal and wave device in a figure-8.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Direction Guidance Status Badge
        val statusColor by animateColorAsState(
            targetValue = if (isFacingQibla) Color(0xFF10B981) else MaterialTheme.colorScheme.secondaryContainer,
            label = "status_color"
        )
        val statusTextColor by animateColorAsState(
            targetValue = if (isFacingQibla) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
            label = "status_text_color"
        )

        val guidanceText = when {
            isFacingQibla -> "✓ Aligned with the Holy Kaaba (Qibla)"
            angleDifference > 0 -> "Turn ${angleDifference.roundToInt()}° Right ➔"
            else -> "Turn ${abs(angleDifference).roundToInt()}° Left ⬅"
        }

        Surface(
            color = statusColor,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(vertical = 4.dp),
            shadowElevation = if (isFacingQibla) 4.dp else 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isFacingQibla) Icons.Default.CheckCircle else if (angleDifference > 0) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = statusTextColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = guidanceText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusTextColor
                )
            }
        }

        // Compass Visualizer Dial (Fixed pointer at 12 o'clock, Dial rotates with True North)
        Box(
            modifier = Modifier
                .size(290.dp)
                .testTag("compass_dial_visualizer"),
            contentAlignment = Alignment.Center
        ) {
            val dialRotation = -animatedHeading
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outlineVariant
            val alignedGreen = Color(0xFF10B981)
            val northRed = Color(0xFFEF4444)

            // Outer dial with ticks and Kaaba position on rim
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(dialRotation)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 16.dp.toPx()

                // Outer border circle
                drawCircle(
                    color = if (isFacingQibla) alignedGreen else outlineColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = if (isFacingQibla) 4.dp.toPx() else 2.dp.toPx())
                )

                // Draw 360-degree ticks aligned correctly with North at angle 0 (Top)
                for (angle in 0 until 360 step 5) {
                    val angleFromNorthRad = Math.toRadians((angle - 90.0))
                    val isCardinal = angle % 90 == 0
                    val isSemiCardinal = angle % 45 == 0
                    val isMajorTick = angle % 15 == 0

                    val tickLen = when {
                        isCardinal -> 16.dp.toPx()
                        isSemiCardinal -> 12.dp.toPx()
                        isMajorTick -> 8.dp.toPx()
                        else -> 4.dp.toPx()
                    }

                    val tickColor = when {
                        angle == 0 -> northRed
                        isCardinal -> primaryColor
                        isSemiCardinal -> primaryColor.copy(alpha = 0.8f)
                        else -> outlineColor
                    }

                    val startX = center.x + (radius - tickLen) * cos(angleFromNorthRad).toFloat()
                    val startY = center.y + (radius - tickLen) * sin(angleFromNorthRad).toFloat()
                    val endX = center.x + radius * cos(angleFromNorthRad).toFloat()
                    val endY = center.y + radius * sin(angleFromNorthRad).toFloat()

                    drawLine(
                        color = tickColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (isCardinal) 3.5.dp.toPx() else if (isMajorTick) 2.dp.toPx() else 1.dp.toPx()
                    )
                }

                // Draw Kaaba indicator point directly on the rotating dial circle at qiblaBearing
                val kaabaAngleRad = Math.toRadians((qiblaBearing.toDouble() - 90.0))
                val kaabaMarkerX = center.x + (radius - 22.dp.toPx()) * cos(kaabaAngleRad).toFloat()
                val kaabaMarkerY = center.y + (radius - 22.dp.toPx()) * sin(kaabaAngleRad).toFloat()

                drawCircle(
                    color = if (isFacingQibla) alignedGreen else Color(0xFFD97706),
                    radius = 7.dp.toPx(),
                    center = Offset(kaabaMarkerX, kaabaMarkerY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(kaabaMarkerX, kaabaMarkerY)
                )
            }

            // Cardinal Letters (North, East, South, West) rotating with dial
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(dialRotation),
                contentAlignment = Alignment.Center
            ) {
                // North (Top, 0°)
                Text(
                    text = "N",
                    fontWeight = FontWeight.ExtraBold,
                    color = northRed,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 22.dp)
                )
                // South (Bottom, 180°)
                Text(
                    text = "S",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp)
                )
                // East (Right, 90°)
                Text(
                    text = "E",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 22.dp)
                )
                // West (Left, 270°)
                Text(
                    text = "W",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 22.dp)
                )
            }

            // Qibla Target Arrow (Points from device top toward Kaaba)
            // When device faces Qibla (heading == qiblaBearing), qiblaRelativeDegrees == 0 (points UP)
            val qiblaRelativeDegrees = qiblaBearing - animatedHeading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(qiblaRelativeDegrees),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val tipRadius = size.minDimension / 2f - 24.dp.toPx()

                    // Draw needle pointing to Qibla
                    val needleColor = if (isFacingQibla) alignedGreen else Color(0xFFD97706)
                    val path = Path().apply {
                        moveTo(center.x, center.y - tipRadius) // arrow tip
                        lineTo(center.x - 14.dp.toPx(), center.y - tipRadius + 32.dp.toPx())
                        lineTo(center.x - 5.dp.toPx(), center.y - tipRadius + 26.dp.toPx())
                        lineTo(center.x - 5.dp.toPx(), center.y)
                        lineTo(center.x + 5.dp.toPx(), center.y)
                        lineTo(center.x + 5.dp.toPx(), center.y - tipRadius + 26.dp.toPx())
                        lineTo(center.x + 14.dp.toPx(), center.y - tipRadius + 32.dp.toPx())
                        close()
                    }
                    drawPath(path = path, color = needleColor)

                    // Draw counter-tail (needle base)
                    val tailPath = Path().apply {
                        moveTo(center.x - 4.dp.toPx(), center.y)
                        lineTo(center.x, center.y + 24.dp.toPx())
                        lineTo(center.x + 4.dp.toPx(), center.y)
                        close()
                    }
                    drawPath(path = tailPath, color = needleColor.copy(alpha = 0.4f))
                }

                // Kaaba Mosque Badge at tip of needle
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isFacingQibla) alignedGreen else Color(0xFFD97706),
                        modifier = Modifier.size(32.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = "Kaaba Direction",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Fixed Top Heading Marker (Points to phone's current direction)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 16.dp.toPx()
                // Small triangle at 12 o'clock showing phone's line of sight
                val topMarkerPath = Path().apply {
                    moveTo(center.x, center.y - radius - 8.dp.toPx())
                    lineTo(center.x - 6.dp.toPx(), center.y - radius - 16.dp.toPx())
                    lineTo(center.x + 6.dp.toPx(), center.y - radius - 16.dp.toPx())
                    close()
                }
                drawPath(topMarkerPath, color = if (isFacingQibla) alignedGreen else primaryColor)
            }

            // Center Compass Hub
            Surface(
                modifier = Modifier.size(60.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = if (isFacingQibla) alignedGreen else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${currentTrueHeading.roundToInt()}°",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isFacingQibla) alignedGreen else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "TRUE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Heading & Bearing Details Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Phone Heading",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentTrueHeading.roundToInt()}° True North",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Makkah Qibla",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${qiblaData.bearingDegrees.roundToInt()}° (${qiblaData.cardinalDirection})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Offline & Geodesic Calculation notice
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SignalWifiOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "100% Offline Great-Circle Geodesic (Kaaba: 21.4225° N, 39.8262° E)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
