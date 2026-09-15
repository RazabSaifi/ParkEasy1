package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

val StarGold = Color(0xFFF59E0B)
val StarGoldLight = Color(0xFFFEF3C7)

@Composable
fun StarRatingDisplay(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = 16.dp,
    activeColor: Color = StarGold,
    inactiveColor: Color = Color(0xFFCBD5E1)
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val filled = rating >= i
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (filled) activeColor else inactiveColor,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingReviewDialog(
    parkingTitle: String,
    initialRating: Float = 5f,
    initialComment: String = "",
    onDismiss: () -> Unit,
    onSubmitReview: (rating: Float, comment: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var rating by remember { mutableFloatStateOf(initialRating) }
    var comment by remember { mutableStateOf(initialComment) }
    val isDark = MaterialTheme.colorScheme.background == CharcoalBackground

    val feedbackTags = listOf(
        "🛡️ Safe & Secure",
        "🚗 Easy Entry & Exit",
        "🧹 Clean Slot",
        "💡 Well-Lit Bay",
        "⚡ EV Charger Worked",
        "👤 Helpful Host / Guard",
        "📍 Accurate Location",
        "💰 Great Value"
    )

    val ratingLabel = when {
        rating >= 4.8f -> "Outstanding Experience! 🌟"
        rating >= 4.0f -> "Great & Easy Parking! 👍"
        rating >= 3.0f -> "Satisfactory & Okay 👌"
        rating >= 2.0f -> "Could Be Better ⚠️"
        else -> "Needs Improvement 👎"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) CharcoalSurface else MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, if (isDark) CharcoalBorder else Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("rating_review_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top close button & Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Rate Parking Session",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = parkingTitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).testTag("close_rating_dialog_button")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive 5-star rating selector
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) CharcoalElevated else Slate100,
                    border = BorderStroke(1.dp, if (isDark) CharcoalBorder else Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { starIndex ->
                                val isSelected = rating >= starIndex
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.12f else 1.0f,
                                    animationSpec = spring(),
                                    label = "star_scale"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            rating = starIndex.toFloat()
                                        }
                                        .testTag("star_rate_$starIndex"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.Star else Icons.Outlined.Star,
                                        contentDescription = "$starIndex stars",
                                        tint = if (isSelected) StarGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .size(36.dp)
                                            .scale(scale)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Rating numeric score & label
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "%.1f".format(rating),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = StarGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = ratingLabel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick praise / highlight tags
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Quick Highlights (Tap to add)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        feedbackTags.forEach { tag ->
                            val isIncluded = comment.contains(tag.substringAfter(" "))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isIncluded) {
                                    if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                                } else {
                                    if (isDark) CharcoalElevated else MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isIncluded) PrimaryBlue else (if (isDark) CharcoalBorder else Slate200)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        val cleanText = tag.substringAfter(" ")
                                        comment = if (isIncluded) {
                                            comment.replace(cleanText, "").replace("  ", " ").trim()
                                        } else {
                                            if (comment.isBlank()) cleanText else "$comment, $cleanText"
                                        }
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    if (isIncluded) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = if (isIncluded) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isIncluded) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Comment input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Your Comment",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${comment.length} / 250",
                            fontSize = 11.sp,
                            color = if (comment.length > 250) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = comment,
                        onValueChange = { if (it.length <= 250) comment = it },
                        placeholder = {
                            Text(
                                "Share helpful details for other drivers: gate access, clearance, host communication, safety...",
                                fontSize = 12.sp
                            )
                        },
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) CharcoalElevated else MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = if (isDark) CharcoalElevated else MaterialTheme.colorScheme.surface,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = if (isDark) CharcoalBorder else Slate200
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("review_comment_input")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDark) CharcoalBorder else Slate200),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_review_button")
                    ) {
                        Text(
                            "Not Now",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            val finalComment = comment.ifBlank {
                                when {
                                    rating >= 4.5f -> "Great parking spot! Safe, secure, and hassle-free."
                                    rating >= 3.5f -> "Good convenient parking slot, convenient location."
                                    else -> "Average parking experience."
                                }
                            }
                            onSubmitReview(rating, finalComment)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("submit_rating_button")
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Submit Review",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
