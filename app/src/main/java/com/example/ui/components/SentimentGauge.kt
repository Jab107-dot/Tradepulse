package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MarketSentimentDetail
import com.example.ui.theme.BearishRed
import com.example.ui.theme.BullishGreen
import com.example.ui.theme.CryptoCyan
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalSurfaceBorder
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SentimentGauge(
    sentiment: MarketSentimentDetail,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sentiment_gauge_card"),
        shape = RoundedCornerShape(16.dp),
        color = TerminalSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, TerminalSurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Market Sentiment Composite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Multi-Factor Fear & Greed Engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val badgeColor = when {
                    sentiment.compositeScore >= 75 -> BullishGreen
                    sentiment.compositeScore >= 55 -> CryptoCyan
                    sentiment.compositeScore >= 45 -> GoldAccent
                    else -> BearishRed
                }

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        sentiment.label.uppercase(),
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speedometer Arc Gauge Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                SpeedometerDialCanvas(score = sentiment.compositeScore)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        "${sentiment.compositeScore}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "OUT OF 100",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fear to Greed Spectrum Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 Extreme Fear", fontSize = 10.sp, color = BearishRed)
                Text("50 Neutral", fontSize = 10.sp, color = GoldAccent)
                Text("100 Extreme Greed", fontSize = 10.sp, color = BullishGreen)
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Positioning Breakdown (Retail vs Institutional)
            Text(
                "Market Positioning Distribution",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Retail Bar
            PositioningSplitBar(
                title = "Retail Crowd Positioning",
                longPct = sentiment.retailLongPercent,
                shortPct = sentiment.retailShortPercent,
                longLabel = "Long",
                shortLabel = "Short"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Institutional Bar
            PositioningSplitBar(
                title = "Institutional Positioning (Smart Money)",
                longPct = sentiment.institutionalLongPercent,
                shortPct = sentiment.institutionalShortPercent,
                longLabel = "Long",
                shortLabel = "Short"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Sentiment Metric Indicators Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniCard(
                    title = "Social Velocity",
                    value = if (sentiment.socialSentimentScore >= 0) "+${sentiment.socialSentimentScore}" else "${sentiment.socialSentimentScore}",
                    accentColor = if (sentiment.socialSentimentScore >= 0) BullishGreen else BearishRed,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = "Implied Vol (VIX)",
                    value = "${sentiment.volatilityIndex}",
                    accentColor = if (sentiment.volatilityIndex < 18.0) BullishGreen else BearishRed,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    title = "Put / Call Ratio",
                    value = "${sentiment.putCallRatio}",
                    accentColor = if (sentiment.putCallRatio < 0.8) BullishGreen else BearishRed,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Narrative Summary Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = sentiment.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SpeedometerDialCanvas(score: Int) {
    val animatedScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 900),
        label = "sentiment_gauge_anim"
    )

    Canvas(modifier = Modifier.size(240.dp, 130.dp)) {
        val width = size.width
        val height = size.height
        val radius = width / 2.2f
        val center = Offset(width / 2f, height - 10f)

        // Arc sweep is 180 degrees from 180 (left) to 0 (right)
        val startAngle = 180f
        val sweepAngle = 180f

        // Draw background track
        drawArc(
            color = Color(0xFF1E293B),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 18f, cap = StrokeCap.Round)
        )

        // Gradient Arc: Red -> Yellow -> Green
        val progressSweep = (animatedScore / 100f) * 180f
        val gradientBrush = Brush.horizontalGradient(
            colors = listOf(BearishRed, GoldAccent, CryptoCyan, BullishGreen)
        )

        drawArc(
            brush = gradientBrush,
            startAngle = startAngle,
            sweepAngle = progressSweep.coerceIn(2f, 180f),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 18f, cap = StrokeCap.Round)
        )

        // Draw indicator needle dot
        val needleAngleRad = Math.toRadians((startAngle + progressSweep).toDouble())
        val needleX = center.x + (radius * cos(needleAngleRad)).toFloat()
        val needleY = center.y + (radius * sin(needleAngleRad)).toFloat()

        drawCircle(
            color = Color.White,
            radius = 7f,
            center = Offset(needleX, needleY)
        )
    }
}

@Composable
private fun PositioningSplitBar(
    title: String,
    longPct: Int,
    shortPct: Int,
    longLabel: String,
    shortLabel: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "$longLabel $longPct% / $shortLabel $shortPct%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(5.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight((longPct.toFloat() / 100f).coerceAtLeast(0.01f))
                    .height(10.dp)
                    .background(BullishGreen, RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
            )
            Box(
                modifier = Modifier
                    .weight((shortPct.toFloat() / 100f).coerceAtLeast(0.01f))
                    .height(10.dp)
                    .background(BearishRed, RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp))
            )
        }
    }
}

@Composable
private fun MetricMiniCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}
