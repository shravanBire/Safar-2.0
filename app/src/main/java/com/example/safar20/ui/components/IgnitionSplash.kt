package com.example.safar20.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Royal Enfield "Key-On" ignition startup animation splash overlay.
 * Features needle sweep, warning dots lighting sequence, fading brand text, and auto-dismiss.
 */
@Composable
fun IgnitionSplash(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val needleAnim = remember { Animatable(0f) }
    val gpsLightAnim = remember { Animatable(0.3f) }
    val p2pLightAnim = remember { Animatable(0.3f) }
    val sosLightAnim = remember { Animatable(0.3f) }
    val textAlphaAnim = remember { Animatable(0f) }
    val splashAlphaAnim = remember { Animatable(1f) }

    val needlePath = remember { Path() }
    val dialFaceBrush = remember { Brush.radialGradient(listOf(GunmetalGauge, CrankcaseBlack)) }

    LaunchedEffect(Unit) {
        // Fade in brand text
        launch {
            textAlphaAnim.animateTo(1f, animationSpec = tween(600))
        }

        // Sequential warning lights lighting
        launch {
            gpsLightAnim.animateTo(1f, animationSpec = tween(200))
            delay(150)
            p2pLightAnim.animateTo(1f, animationSpec = tween(200))
            delay(150)
            sosLightAnim.animateTo(1f, animationSpec = tween(200))
        }

        // Sweep needle 0 -> max (700ms)
        needleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )

        // Hold at max (150ms)
        delay(150)

        // Dim warning lights as needle swings back
        launch {
            gpsLightAnim.animateTo(0.3f, animationSpec = tween(500))
            p2pLightAnim.animateTo(0.3f, animationSpec = tween(500))
            sosLightAnim.animateTo(0.3f, animationSpec = tween(500))
        }

        // Sweep needle back max -> 0 (700ms)
        needleAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )

        // Fade splash out (300ms) and notify
        splashAlphaAnim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300)
        )

        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = splashAlphaAnim.value }
            .background(CrankcaseBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Mini Gauge (~220.dp)
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerRadius = size.width / 2f - 4.dp.toPx()

                    // Dial Base
                    drawCircle(brush = dialFaceBrush, radius = outerRadius, center = center)
                    drawCircle(color = BronzeRim, radius = outerRadius, center = center, style = Stroke(width = 4.dp.toPx()))
                    drawCircle(color = RoyalBrass, radius = outerRadius - 3.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))

                    // Background Track Arc (135° to 405°)
                    val startAngle = 135f
                    val totalSweep = 270f
                    val tickRadius = outerRadius - 16.dp.toPx()

                    for (i in 0..160 step 20) {
                        val tickAngle = startAngle + ((i / 160f) * totalSweep)
                        val tickRad = Math.toRadians(tickAngle.toDouble())
                        val innerPt = Offset(
                            x = center.x + ((tickRadius - 8.dp.toPx()) * cos(tickRad)).toFloat(),
                            y = center.y + ((tickRadius - 8.dp.toPx()) * sin(tickRad)).toFloat()
                        )
                        val outerPt = Offset(
                            x = center.x + (tickRadius * cos(tickRad)).toFloat(),
                            y = center.y + (tickRadius * sin(tickRad)).toFloat()
                        )
                        drawLine(
                            color = if (i >= 120) SignalRed else BronzeRim,
                            start = innerPt,
                            end = outerPt,
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Tapered Needle
                    val needleAngle = startAngle + (needleAnim.value * totalSweep)
                    val needleRad = Math.toRadians(needleAngle.toDouble())
                    val perpRad = needleRad + Math.PI / 2.0
                    val needleLength = outerRadius - 22.dp.toPx()
                    val baseHalfWidth = 3.dp.toPx()

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

                    drawPath(path = needlePath, color = RoyalBrass)

                    // Hub cap
                    drawCircle(color = BronzeRim, radius = 8.dp.toPx(), center = center)
                    drawCircle(color = RoyalBrass, radius = 8.dp.toPx(), center = center, style = Stroke(width = 1.dp.toPx()))
                    drawCircle(color = RoyalBrass, radius = 3.dp.toPx(), center = center)
                }

                // Top Warning Dots inside Gauge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(PhosphorGreen.copy(alpha = gpsLightAnim.value), CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(RoyalBrass.copy(alpha = p2pLightAnim.value), CircleShape))
                    Box(modifier = Modifier.size(6.dp).background(SignalRed.copy(alpha = sosLightAnim.value), CircleShape))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Fading Brand Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer { alpha = textAlphaAnim.value }
            ) {
                Text(
                    text = "SAFAR",
                    fontFamily = BebasNeue,
                    fontSize = 42.sp,
                    color = WarmIvory,
                    letterSpacing = 8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SERVERLESS TELEMETRY",
                    fontFamily = Rajdhani,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = MutedPewter,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
