package com.example.safar20.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.data.local.UnitConverter
import com.example.safar20.ui.theme.BebasNeue
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.CrankcaseBlack
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.PhosphorGreen
import com.example.safar20.ui.theme.Rajdhani
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed
import com.example.safar20.ui.theme.WarmIvory
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE = 135f
private const val TOTAL_SWEEP = 270f

/**
 * Royal Enfield Tripper Dash inspired circular Speedometer Gauge.
 * Features radial gradient dial face, glowing progress arc, tapered mechanical needle,
 * 3-light warning strip, BebasNeue speed readout, and rolling mechanical ODO box.
 */
@Composable
fun TripperSpeedometer(
    speedKmh: Float,
    unit: String = "km/h",
    odoDistanceKm: Double = 0.0,
    gaugeSize: Dp = 260.dp,
    connectionState: ConnectionState = ConnectionState.PARKED,
    modifier: Modifier = Modifier
) {
    // Smooth bouncy spring mechanical needle sweep
    val animatedSpeed by animateFloatAsState(
        targetValue = speedKmh.coerceIn(0f, 160f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "NeedleSweep"
    )

    val convertedSpeedInt = UnitConverter.convertSpeed(animatedSpeed, unit).toInt()
    val speedUnitLabel = UnitConverter.getSpeedUnitLabel(unit)

    val convertedDistance = UnitConverter.convertDistance(odoDistanceKm, unit)
    val formattedDistance = String.format(Locale.US, "%05.1f", convertedDistance)
    val odoDigits = formattedDistance.toCharArray()
    val distanceUnitLabel = UnitConverter.getDistanceUnitLabel(unit)

    // Hoisted static allocations to prevent draw loop allocations
    val dialFaceBrush = remember {
        Brush.radialGradient(listOf(GunmetalGauge, CrankcaseBlack))
    }
    val textMeasurer = rememberTextMeasurer()
    val majorTicks = remember { listOf(0, 20, 40, 60, 80, 100, 120, 140, 160) }
    val tickTextLayouts = remember(textMeasurer) {
        majorTicks.associateWith { i ->
            val isRedline = i >= 120
            textMeasurer.measure(
                text = "$i",
                style = TextStyle(
                    fontFamily = BebasNeue,
                    fontSize = 11.sp,
                    color = if (isRedline) SignalRed else WarmIvory
                )
            )
        }
    }
    val needlePath = remember { Path() }

    Box(
        modifier = modifier.size(gaugeSize),
        contentAlignment = Alignment.Center
    ) {
        // Canvas Gauge
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 6.dp.toPx()

            // 1. Dial Face Base & Faint Inner Shadow Ring
            drawCircle(
                brush = dialFaceBrush,
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = CrankcaseBlack.copy(alpha = 0.5f),
                radius = outerRadius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Outer Metallic Bezel Rings
            drawCircle(
                color = BronzeRim,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 5.dp.toPx())
            )
            drawCircle(
                color = RoyalBrass,
                radius = outerRadius - 3.5.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Gauge Geometry (135° to 405°, total 270°)
            val arcRadius = outerRadius - 16.dp.toPx()
            val arcSize = Size(arcRadius * 2, arcRadius * 2)
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)

            // 3. Progress Arc (from 0 up to current speed, turning SignalRed > 120)
            val currentSpeedRatio = (animatedSpeed / 160f).coerceIn(0f, 1f)
            val currentSweep = currentSpeedRatio * TOTAL_SWEEP
            val arcColor = if (animatedSpeed > 120f) SignalRed else RoyalBrass

            // Glow stroke underneath
            if (currentSweep > 0f) {
                drawArc(
                    color = arcColor.copy(alpha = 0.25f),
                    startAngle = START_ANGLE,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )

                // Main progress stroke
                drawArc(
                    color = arcColor,
                    startAngle = START_ANGLE,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 2. Dial Ticks & BebasNeue Numbers
            val tickRadius = outerRadius - 20.dp.toPx()
            val labelRadius = tickRadius - 18.dp.toPx()

            for (i in 0..160 step 10) {
                val speedRatio = i / 160f
                val tickAngle = START_ANGLE + (speedRatio * TOTAL_SWEEP)
                val tickRad = Math.toRadians(tickAngle.toDouble())

                val isMajor = (i % 20 == 0)
                val isRedline = (i >= 120)

                val tickLength = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                val tickWidth = if (isMajor) 2.5.dp.toPx() else 1.0.dp.toPx()
                val tickColor = if (isRedline) SignalRed else WarmIvory.copy(alpha = 0.8f)

                val innerPt = Offset(
                    x = center.x + ((tickRadius - tickLength) * cos(tickRad)).toFloat(),
                    y = center.y + ((tickRadius - tickLength) * sin(tickRad)).toFloat()
                )
                val outerPt = Offset(
                    x = center.x + (tickRadius * cos(tickRad)).toFloat(),
                    y = center.y + (tickRadius * sin(tickRad)).toFloat()
                )

                drawLine(
                    color = tickColor,
                    start = innerPt,
                    end = outerPt,
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Round
                )

                // Text Labels for Major Ticks (0, 20, ..., 160)
                if (isMajor) {
                    val textPt = Offset(
                        x = center.x + (labelRadius * cos(tickRad)).toFloat(),
                        y = center.y + (labelRadius * sin(tickRad)).toFloat()
                    )
                    val layoutResult = tickTextLayouts[i]
                    if (layoutResult != null) {
                        drawText(
                            textLayoutResult = layoutResult,
                            topLeft = Offset(
                                textPt.x - layoutResult.size.width / 2f,
                                textPt.y - layoutResult.size.height / 2f
                            )
                        )
                    }
                }
            }

            // 4. Tapered Mechanical Needle
            val needleAngle = START_ANGLE + (animatedSpeed / 160f).coerceIn(0f, 1f) * TOTAL_SWEEP
            val needleRad = Math.toRadians(needleAngle.toDouble())
            val perpRad = needleRad + Math.PI / 2.0
            val needleLength = outerRadius - 28.dp.toPx()
            val baseHalfWidth = 3.5.dp.toPx()

            val tip = Offset(
                x = center.x + (needleLength * cos(needleRad)).toFloat(),
                y = center.y + (needleLength * sin(needleRad)).toFloat()
            )
            val baseLeft = Offset(
                x = center.x + (baseHalfWidth * cos(perpRad)).toFloat(),
                y = center.y + (baseHalfWidth * sin(perpRad)).toFloat()
            )
            val baseRight = Offset(
                x = center.x - (baseHalfWidth * cos(perpRad)).toFloat(),
                y = center.y - (baseHalfWidth * sin(perpRad)).toFloat()
            )

            needlePath.reset()
            needlePath.moveTo(baseLeft.x, baseLeft.y)
            needlePath.lineTo(tip.x, tip.y)
            needlePath.lineTo(baseRight.x, baseRight.y)
            needlePath.close()

            val needleColor = if (animatedSpeed > 120f) SignalRed else RoyalBrass
            drawPath(path = needlePath, color = needleColor)

            // Center Circular Hub Cap
            drawCircle(
                color = BronzeRim,
                radius = 9.dp.toPx(),
                center = center
            )
            drawCircle(
                color = RoyalBrass,
                radius = 9.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = RoyalBrass,
                radius = 3.5.dp.toPx(),
                center = center
            )
        }

        // Foreground Readout Overlay (Warning Strip, Speed, Mechanical Odometer)
        val isConnectionActive = connectionState == ConnectionState.P2P_DIRECT || connectionState == ConnectionState.SIMULATOR_SYNCING

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 28.dp)
        ) {
            // 6. Tiny 3-light warning strip at top inside dial
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WarningLightPill(label = "GPS", color = PhosphorGreen, isActive = isConnectionActive)
                WarningLightPill(label = "P2P", color = RoyalBrass, isActive = isConnectionActive)
                WarningLightPill(label = "SOS", color = SignalRed, isActive = false)
            }

            // 5. Center Speed & Unit Readout
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "$convertedSpeedInt",
                    color = WarmIvory,
                    fontSize = 64.sp,
                    lineHeight = 64.sp,
                    fontFamily = BebasNeue
                )
                Text(
                    text = speedUnitLabel.uppercase(),
                    color = MutedPewter,
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
            }

            // 5. Bottom Mechanical Rolling Odometer Box
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .background(GunmetalGauge, shape = RoundedCornerShape(4.dp))
                    .border(1.dp, BronzeRim, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "TRIP",
                    color = MutedPewter,
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
                odoDigits.forEach { char ->
                    Box(
                        modifier = Modifier
                            .background(CrankcaseBlack, shape = RoundedCornerShape(2.dp))
                            .border(0.5.dp, BronzeRim, shape = RoundedCornerShape(2.dp))
                            .padding(horizontal = 3.dp, vertical = 1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.toString(),
                            color = if (char == '.') MutedPewter else WarmIvory,
                            fontFamily = BebasNeue,
                            fontSize = 13.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = distanceUnitLabel,
                    color = MutedPewter,
                    fontFamily = Rajdhani,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun WarningLightPill(
    label: String,
    color: Color,
    isActive: Boolean = false
) {
    val alpha = if (isActive) 1.0f else 0.4f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color.copy(alpha = alpha), shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            color = MutedPewter.copy(alpha = alpha),
            fontFamily = Rajdhani,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
    }
}
