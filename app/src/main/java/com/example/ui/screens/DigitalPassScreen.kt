package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Booking
import com.example.data.util.LocationUtils
import com.example.ui.components.RatingReviewDialog
import com.example.ui.components.StarGold
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSubtle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryNavy
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun DigitalPassScreen(
    booking: Booking?,
    onBack: () -> Unit,
    onCompleteBooking: (Long) -> Unit = {},
    onSubmitReview: (bookingId: Long, spaceId: Long, rating: Float, comment: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background == CharcoalBackground

    if (booking == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Booking pass not found.", color = MaterialTheme.colorScheme.onSurface)
        }
        return
    }

    var isPassScanned by remember { mutableStateOf(booking.status == "Completed") }
    var showRatingDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Digital Parking Pass", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isPassScanned) (if (isDark) Color(0xFF1E2E4A) else Color(0xFFEFF6FF)) else (if (isDark) Color(0xFF143823) else Color(0xFFDCFCE7))
                ) {
                    Text(
                        text = if (isPassScanned) "CHECKED IN" else booking.status.uppercase(),
                        color = if (isPassScanned) PrimaryBlue else (if (isDark) AccentEmerald else Color(0xFF15803D)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Pass Card Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("digital_pass_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header of Pass
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryBlue)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Icon(Icons.Default.LocalParking, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("PARKEASY PASS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 1.sp)
                                    Text("Guaranteed Slot", color = Color(0xFFBBF7D0), fontSize = 10.sp)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Pass ID", color = Color(0xFFBFDBFE), fontSize = 10.sp)
                                Text(booking.bookingCode, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    // Content of Pass
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(booking.parkingTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${booking.parkingAddress}, ${booking.parkingCity}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 14.dp))

                        // Booking Specs
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("DATE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Text(booking.bookingDate, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text("SLOT TIME", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Text("${booking.startTime} - ${booking.endTime}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("DURATION", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Text("${booking.durationHours} Hours", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("VEHICLE", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Text(booking.vehicleType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text("REGISTRATION", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Text(booking.vehicleRegNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("PAID AMOUNT", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                Text("₹${booking.totalAmount.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 16.dp))

                        // QR Code Canvas Matrix
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("SCAN AT ENTRY GATE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Deterministic QR code pattern drawing (high contrast on white background for scanner readability)
                                Canvas(modifier = Modifier.size(150.dp)) {
                                    val gridCount = 21
                                    val cellSize = size.width / gridCount
                                    val hash = booking.bookingCode.hashCode()

                                    for (i in 0 until gridCount) {
                                        for (j in 0 until gridCount) {
                                            val isFinderCorner = (i < 7 && j < 7) || (i >= gridCount - 7 && j < 7) || (i < 7 && j >= gridCount - 7)
                                            val isFinderInner = (i in 2..4 && j in 2..4) || (i in gridCount - 5..gridCount - 3 && j in 2..4) || (i in 2..4 && j in gridCount - 5..gridCount - 3)
                                            val isFinderBorder = (i in 0..6 && (j == 0 || j == 6)) || (j in 0..6 && (i == 0 || i == 6)) ||
                                                    (i >= gridCount - 7 && (j == 0 || j == 6)) || (j in 0..6 && (i == gridCount - 7 || i == gridCount - 1)) ||
                                                    (i in 0..6 && (j == gridCount - 7 || j == gridCount - 1)) || (j >= gridCount - 7 && (i == 0 || i == 6))

                                            val fill = if (isFinderCorner) {
                                                isFinderInner || isFinderBorder
                                            } else {
                                                ((i * 31 + j * 17 + hash) % 3) == 0
                                            }

                                            if (fill) {
                                                drawRect(
                                                    color = Color(0xFF0F172A),
                                                    topLeft = Offset(i * cellSize, j * cellSize),
                                                    size = Size(cellSize * 0.95f, cellSize * 0.95f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(booking.bookingCode, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, letterSpacing = 2.sp)
                            Text("Show this QR pass or tell booking ID to security guard", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (isPassScanned) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AccentEmerald.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Gate Guard Verified — Vehicle Checked In", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentEmerald)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Directions & Gate Check-In Simulation
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val geoQuery = "${booking.parkingTitle}, ${booking.parkingAddress}, ${booking.parkingCity}"
                        val mapUri = android.net.Uri.parse("google.navigation:q=" + android.net.Uri.encode(geoQuery) + "&mode=d")
                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, mapUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        try {
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            val fallbackUri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(geoQuery))
                            try {
                                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, fallbackUri))
                            } catch (ignored: Exception) {}
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("pass_directions_button")
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Google Maps", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedButton(
                    onClick = {
                        isPassScanned = !isPassScanned
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isPassScanned) AccentEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isPassScanned) AccentEmerald else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("pass_simulate_scan_button")
                ) {
                    Icon(
                        imageVector = if (isPassScanned) Icons.Default.CheckCircle else Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = if (isPassScanned) AccentEmerald else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPassScanned) "Checked In ✓" else "Gate Check-In", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Session Completion & Star Rating Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) CharcoalElevated else Slate100
                ),
                border = BorderStroke(1.dp, if (isDark) CharcoalBorder else Slate200),
                modifier = Modifier.fillMaxWidth().testTag("session_completion_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (booking.status == "Completed" && booking.isReviewed) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Parking Session Completed & Reviewed ⭐",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AccentEmerald
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (booking.status == "Completed") "Rate Your Experience" else "Finished Parking?",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (booking.status == "Completed") "Leave a star rating & feedback" else "End session & rate spot",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    if (booking.status != "Completed") {
                                        onCompleteBooking(booking.id)
                                    }
                                    showRatingDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (booking.status == "Completed") StarGold else PrimaryBlue
                                ),
                                modifier = Modifier.testTag("rate_session_button")
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (booking.status == "Completed") "Rate Spot" else "End & Rate",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRatingDialog) {
        RatingReviewDialog(
            parkingTitle = booking.parkingTitle,
            onDismiss = { showRatingDialog = false },
            onSubmitReview = { rating, comment ->
                onSubmitReview(booking.id, booking.parkingSpaceId, rating, comment)
                showRatingDialog = false
            }
        )
    }
}
