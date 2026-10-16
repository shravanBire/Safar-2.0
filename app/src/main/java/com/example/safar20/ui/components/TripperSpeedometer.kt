package com.example.safar20.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.safar20.data.local.UnitConverter
import com.example.safar20.ui.theme.BronzeRim
import com.example.safar20.ui.theme.GunmetalGauge
import com.example.safar20.ui.theme.MutedPewter
import com.example.safar20.ui.theme.RoyalBrass
import com.example.safar20.ui.theme.SignalRed
import com.example.safar20.ui.theme.WarmIvory
import kotlin.math.cos
import kotlin.math.sin

/**
 * Royal Enfield Tripper Dash inspired circular Speedometer Gauge.
 * Features double metallic outer bezel, 0-160 km/h dial ticks with redline above 120 km/h,
 * bouncy mechanical needle animation, digital speed readout, and rolling ODO counter box.
 */
@Composable
fun TripperSpeedometer(
    speedKmh: Float,
    unit: String = "km/h",
    odoDistanceKm: Double = 0.0,
    gaugeSize: Dp = 260.dp,
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
    val formattedOdo = UnitConverter.formatDistance(odoDistanceKm, unit)

    Box(
        modifier = modifier.size(gaugeSize),
        contentAlignment = Alignment.Center
    ) {
        // Canvas Gauge (Outer bezel, dial face, ticks, and sweeping needle)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 8.dp.toPx()

            // 1. Dial Face Base
            drawCircle(
                color = GunmetalGauge,
                radius = outerRadius,
                center = center
            )

            // 2. Outer Double Metallic Bezel
            drawCircle(
                color = BronzeRim,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )
            drawCircle(
                color = RoyalBrass,
                radius = outerRadius - 4.dp.toPx(),
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 3. Gauge Background Arcs (135° to 405°, total 270°)
            val startAngle = 135f
            val totalSweep = 270f
            val normalSweep = totalSweep * (120f / 160f) // 0 to 120 km/h
            val redlineSweep = totalSweep * (40f / 160f)  // 120 to 160 km/h

            val arcRadius = outerRadius - 16.dp.toPx()
            val arcSize = Size(arcRadius * 2, arcRadius * 2)
            val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)

            // Normal Arc (0 - 120)
            drawArc(
                color = BronzeRim,
                startAngle = startAngle,
                sweepAngle = normalSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Redline Arc (120 - 160)
            drawArc(
                color = SignalRed,
                startAngle = startAngle + normalSweep,
                sweepAngle = redlineSweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )

            // 4. Dial Ticks & Numbers
            drawDialTicksAndLabels(
                center = center,
                radius = outerRadius - 22.dp.toPx(),
                startAngle = startAngle,
                totalSweep = totalSweep
            )

            // 5. Sweeping Mechanical Needle
            val needleAngle = startAngle + (animatedSpeed / 160f) * totalSweep
            val needleRad = Math.toRadians(needleAngle.toDouble())
            val needleLength = outerRadius - 32.dp.toPx()

            val needleEnd = Offset(
                x = center.x + (needleLength * cos(needleRad)).toFloat(),
                y = center.y + (needleLength * sin(needleRad)).toFloat()
            )

            drawLine(
                color = RoyalBrass,
                start = center,
                end = needleEnd,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Needle Center Cap
            drawCircle(
                color = GunmetalGauge,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = RoyalBrass,
                radius = 12.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Digital Readout & Bottom Mechanical ODO Box
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 18.dp)
        ) {
            Text(
                text = "$convertedSpeedInt",
                color = WarmIvory,
                fontSize = 46.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = speedUnitLabel.uppercase(),
                color = RoyalBrass,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Mechanical Odometer Box
            Box(
                modifier = Modifier
                    .background(GunmetalGauge, shape = RoundedCornerShape(4.dp))
                    .border(1.dp, BronzeRim, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "TRIP $formattedOdo",
                    color = WarmIvory,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun DrawScope.drawDialTicksAndLabels(
    center: Offset,
    radius: Float,
    startAngle: Float,
    totalSweep: Float
) {
    for (i in 0..160 step 10) {
        val speedRatio = i / 160f
        val tickAngle = startAngle + (speedRatio * totalSweep)
        val tickRad = Math.toRadians(tickAngle.toDouble())

        val isMajor = (i % 20 == 0)
        val isRedline = (i >= 120)

        val tickLength = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
        val tickWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx()
        val tickColor = if (isRedline) SignalRed else if (isMajor) RoyalBrass else BronzeRim

        val innerPt = Offset(
            x = center.x + ((radius - tickLength) * cos(tickRad)).toFloat(),
            y = center.y + ((radius - tickLength) * sin(tickRad)).toFloat()
        )
        val outerPt = Offset(
            x = center.x + (radius * cos(tickRad)).toFloat(),
            y = center.y + (radius * sin(tickRad)).toFloat()
        )

        drawLine(
            color = tickColor,
            start = innerPt,
            end = outerPt,
            strokeWidth = tickWidth,
            cap = StrokeCap.Round
        )

        // Text Labels for Major Ticks (0, 20, 40, ..., 160)
        if (isMajor) {
            val textPt = Offset(
                x = center.x + ((radius - 22.dp.toPx()) * cos(tickRad)).toFloat(),
                y = center.y + ((radius - 22.dp.toPx()) * sin(tickRad)).toFloat()
            )

            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = if (isRedline) android.graphics.Color.parseColor("#EF4444")
                    else android.graphics.Color.parseColor("#9CA3AF")
                    textSize = 10.dp.toPx()
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    typeface = Typeface.MONOSPACE
                }
                drawText(
                    "$i",
                    textPt.x,
                    textPt.y + 3.dp.toPx(),
                    paint
                )
            }
        }
    }
}
