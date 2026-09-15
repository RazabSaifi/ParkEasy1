package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.data.model.Booking
import com.example.ui.components.RatingReviewDialog
import com.example.ui.components.StarGold
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSubtle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun MyBookingsScreen(
    bookings: List<Booking>,
    onViewPass: (Long) -> Unit,
    onCancelBooking: (Long) -> Unit,
    onSubmitReview: (bookingId: Long, spaceId: Long, rating: Float, comment: String) -> Unit,
    onFindParking: () -> Unit,
    onCompleteBooking: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Upcoming", "Completed", "Cancelled")

    var bookingToCancel by remember { mutableStateOf<Booking?>(null) }
    var reviewBookingTarget by remember { mutableStateOf<Booking?>(null) }

    val isDark = MaterialTheme.colorScheme.background == CharcoalBackground

    val filteredBookings = bookings.filter { b ->
        when (selectedTabIndex) {
            0 -> b.status == "Confirmed"
            1 -> b.status == "Completed"
            else -> b.status == "Cancelled"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("my_bookings_screen")
    ) {
        // Page Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "My Bookings",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage your parking reservations and digital passes.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Clean Modern Segmented Tab Navigation
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) CharcoalElevated else Slate100,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        val count = bookings.count {
                            when (index) {
                                0 -> it.status == "Confirmed"
                                1 -> it.status == "Completed"
                                else -> it.status == "Cancelled"
                            }
                        }

                        val bgColor by animateColorAsState(
                            if (isSelected) (if (isDark) CharcoalSurface else Color.White) else Color.Transparent,
                            label = "tabBg"
                        )
                        val textColor by animateColorAsState(
                            if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            label = "tabText"
                        )

                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = bgColor,
                            border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)) else null,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTabIndex = index }
                                .testTag("tab_$title")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else (if (isDark) CharcoalBorder else Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)

        // Content Area
        if (filteredBookings.isEmpty()) {
            // Clean Minimal Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = when (selectedTabIndex) {
                            0 -> "No upcoming bookings"
                            1 -> "No completed bookings"
                            else -> "No cancelled bookings"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Find a parking spot near your destination.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onFindParking,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .height(46.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Explore Parking",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Booking Cards List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredBookings, key = { it.id }) { booking ->
                    BookingCard(
                        booking = booking,
                        onViewPass = { onViewPass(booking.id) },
                        onCancel = { bookingToCancel = booking },
                        onReview = { reviewBookingTarget = booking },
                        onEndSession = {
                            onCompleteBooking(booking.id)
                            reviewBookingTarget = booking
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Cancel Booking Confirmation Dialog (Clean & Restrained)
    if (bookingToCancel != null) {
        val target = bookingToCancel!!
        AlertDialog(
            onDismissRequest = { bookingToCancel = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Cancel Booking?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to cancel booking #${target.bookingCode}? Since it is more than 1 hour before slot time, a 100% full refund of ₹${target.totalAmount.toInt()} will be credited back to your payment source.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelBooking(target.id)
                        bookingToCancel = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Confirm Cancellation", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookingToCancel = null }) {
                    Text("Keep Booking", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        )
    }

    // Rate & Review Dialog
    if (reviewBookingTarget != null) {
        val target = reviewBookingTarget!!
        RatingReviewDialog(
            parkingTitle = target.parkingTitle,
            onDismiss = { reviewBookingTarget = null },
            onSubmitReview = { rating, comment ->
                onSubmitReview(target.id, target.parkingSpaceId, rating, comment)
                reviewBookingTarget = null
            }
        )
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    onViewPass: () -> Unit,
    onCancel: () -> Unit,
    onReview: () -> Unit,
    onEndSession: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background == CharcoalBackground
    // Dynamic photo URL based on ID
    val photoUrl = when (booking.parkingSpaceId % 4L) {
        0L -> "https://images.unsplash.com/photo-1590674899484-d5640e854abe?w=400&q=80"
        1L -> "https://images.unsplash.com/photo-1506521781263-d8422e82f27a?w=400&q=80"
        2L -> "https://images.unsplash.com/photo-1617814076367-b759c7d7e738?w=400&q=80"
        else -> "https://images.unsplash.com/photo-1573348722427-f1d6819fdf98?w=400&q=80"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("booking_item_${booking.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top row: Thumbnail + Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Parking image thumbnail
                val context = LocalContext.current
                val imageRequest = remember(photoUrl, context) {
                    ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(150)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                }
                AsyncImage(
                    model = imageRequest,
                    contentDescription = booking.parkingTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Status Pill & Rating Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (booking.status) {
                                "Confirmed" -> if (isDark) Color(0xFF143823) else Color(0xFFDCFCE7)
                                "Completed" -> if (isDark) Color(0xFF1E2E4A) else Color(0xFFEFF6FF)
                                else -> if (isDark) Color(0xFF3F1D1D) else Color(0xFFFEE2E2)
                            }
                        ) {
                            Text(
                                text = booking.status.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (booking.status) {
                                    "Confirmed" -> if (isDark) AccentEmerald else Color(0xFF15803D)
                                    "Completed" -> PrimaryBlue
                                    else -> if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }

                        // Rating badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "4.8",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = booking.parkingTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${booking.parkingAddress}, ${booking.parkingCity}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Date, Time and Total Amount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Date: ${booking.bookingDate}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Time: ${booking.startTime} – ${booking.endTime}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Paid",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "₹${booking.totalAmount.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (booking.status == "Confirmed") {
                    // Restrained Destructive Cancel Action
                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("cancel_booking_btn_${booking.id}")
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // End Session & Rate Button
                    OutlinedButton(
                        onClick = onEndSession,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StarGold.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = StarGold
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("end_and_rate_btn_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = StarGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "End & Rate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Primary Blue Action: View Pass
                    Button(
                        onClick = onViewPass,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("view_pass_btn_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pass",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (booking.status == "Completed") {
                    if (!booking.isReviewed) {
                        Button(
                            onClick = onReview,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StarGold,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("rate_review_btn_${booking.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Rate & Review",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF143823) else Color(0xFFDCFCE7),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Reviewed ✓",
                                fontSize = 11.sp,
                                color = if (isDark) AccentEmerald else Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onViewPass,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) CharcoalElevated else Slate900
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("view_pass_btn_${booking.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Receipt & Pass",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // Cancelled state badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) CharcoalElevated else Slate100
                    ) {
                        Text(
                            text = "Refund Processed (₹${booking.totalAmount.toInt()})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
