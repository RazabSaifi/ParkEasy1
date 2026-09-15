package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ParkingSpace
import com.example.data.model.Review
import com.example.data.util.LocationUtils
import com.example.data.util.UserLocation
import com.example.ui.components.ParkingMapPinBadge
import com.example.ui.components.RatingReviewDialog
import com.example.ui.components.StarGold
import com.example.ui.components.StarRatingDisplay
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryBlue
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSubtle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun ParkingDetailScreen(
    space: ParkingSpace?,
    reviews: List<Review>,
    onBack: () -> Unit,
    onBookNow: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSubmitReview: ((rating: Float, comment: String) -> Unit)? = null,
    userLocation: UserLocation? = null
) {
    if (space == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Parking listing not found.", color = Slate500)
        }
        return
    }

    var isFavorite by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background == CharcoalBackground

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("parking_detail_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 84.dp) // space for sticky bottom bar
                .verticalScroll(rememberScrollState())
        ) {
            // Top Image Header with floating back and action buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                // Parking Photo
                AsyncImage(
                    model = space.parkingPhoto.ifEmpty { "https://images.unsplash.com/photo-1590674899484-d5640e854abe?w=800&q=80" },
                    contentDescription = space.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Subtle gradient overlay at top for icons visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)
                            )
                        )
                )

                // Top Controls: Back, Share, Favorite
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = (if (isDark) CharcoalSurface else Color.White).copy(alpha = 0.95f),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable(onClick = onBack)
                            .testTag("detail_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = (if (isDark) CharcoalSurface else Color.White).copy(alpha = 0.95f),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { isFavorite = !isFavorite }
                                .testTag("detail_fav_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Main Details Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Title
                Text(
                    text = space.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Location line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${space.address}, ${space.area}, ${space.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Rating line: ★ 4.8 (112 reviews)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${space.rating}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " (${space.reviewsCount} reviews)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Key stats row (4 cards matching Screen 3 of the visual reference)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stat 1: Price
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) CharcoalElevated else Slate100,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "₹${space.hourlyPrice.toInt()}/hr",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Stat 2: Type
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) CharcoalElevated else Slate100,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Type", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (space.isCovered) "Covered" else "Open",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Stat 3: Security
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) CharcoalElevated else Slate100,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Security", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (space.hasSecurityGuard) "Guard" else "CCTV",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Stat 4: Access
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) CharcoalElevated else Slate100,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Access", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (space.has24x7Access) "24/7" else "Day",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // Section 1: Amenities
                Text(
                    text = "Amenities",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                val amenityList = listOf(
                    Pair("CCTV Surveillance", Icons.Default.Videocam),
                    Pair("Security Guard", Icons.Default.Shield),
                    Pair("EV Charging", Icons.Default.ElectricCar),
                    Pair("Covered Roof", Icons.Default.Roofing)
                )

                amenityList.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) CharcoalElevated else Slate100,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = item.second,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.first,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // Section 2: Location & Google Maps
                val context = LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Location & Map",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = {
                            LocationUtils.openGoogleMaps(
                                context = context,
                                lat = space.latitude,
                                lng = space.longitude,
                                label = space.title,
                                address = space.address.ifEmpty { "${space.area}, ${space.city}" }
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("detail_google_maps_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google Maps", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) CharcoalElevated else Slate100,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${space.address}, ${space.area}, ${space.city}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (userLocation != null) {
                            val distKm = LocationUtils.calculateDistanceKm(
                                userLocation.latitude,
                                userLocation.longitude,
                                space.latitude,
                                space.longitude
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${LocationUtils.formatDistance(distKm)} away (${LocationUtils.formatWalkingTime(distKm)})",
                                fontSize = 12.sp,
                                color = AccentEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Google Maps Compose Embedded Map Preview
                        val spotPosition = remember(space.latitude, space.longitude) {
                            LatLng(space.latitude, space.longitude)
                        }
                        val miniMapCameraState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(spotPosition, 15f)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .testTag("detail_google_map_preview")
                        ) {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize(),
                                cameraPositionState = miniMapCameraState,
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = false,
                                    myLocationButtonEnabled = false,
                                    compassEnabled = false,
                                    scrollGesturesEnabled = false,
                                    zoomGesturesEnabled = false,
                                    tiltGesturesEnabled = false,
                                    rotationGesturesEnabled = false,
                                    mapToolbarEnabled = false
                                ),
                                onMapClick = {
                                    LocationUtils.openGoogleMaps(
                                        context = context,
                                        lat = space.latitude,
                                        lng = space.longitude,
                                        label = space.title
                                    )
                                }
                            ) {
                                MarkerComposable(
                                    state = rememberMarkerState(
                                        key = "detail_spot_${space.id}",
                                        position = spotPosition
                                    ),
                                    title = space.title
                                ) {
                                    ParkingMapPinBadge(
                                        space = space,
                                        isSelected = true,
                                        isDark = isDark
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Rules
                Text(
                    text = "Rules",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                val rules = listOf(
                    "Max vehicle height 2.1m",
                    "Show booking QR at entry",
                    "Park only in designated numbered bays"
                )

                rules.forEach { rule ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = rule,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // Section 3: Host
                Text(
                    text = "Host",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isDark) CharcoalElevated else Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = space.ownerName.take(1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = space.ownerName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = AccentEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Member since 2024 · Verified Host",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(20.dp))

                // Section 4: Ratings & Reviews
                val spaceReviews = remember(reviews, space.id) {
                    reviews.filter { it.parkingSpaceId == space.id }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Driver Ratings & Reviews",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${space.reviewsCount} verified community ratings",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (onSubmitReview != null) {
                        OutlinedButton(
                            onClick = { showRatingDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StarGold.copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StarGold),
                            modifier = Modifier.testTag("write_review_button")
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = StarGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rate Spot", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Aggregate Score Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) CharcoalElevated else Slate100
                    ),
                    border = BorderStroke(1.dp, if (isDark) CharcoalBorder else Slate200),
                    modifier = Modifier.fillMaxWidth().testTag("rating_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "%.1f".format(space.rating),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    StarRatingDisplay(rating = space.rating, starSize = 16.dp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Out of 5.0 stars",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = AccentEmerald.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "100% Verified",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of reviews
                if (spaceReviews.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) CharcoalElevated.copy(alpha = 0.5f) else Slate100.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, if (isDark) CharcoalBorder else Slate200),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = StarGold.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No driver comments yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Park at this bay and share your star rating & feedback with other drivers!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("reviews_list")
                    ) {
                        spaceReviews.forEach { review ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) CharcoalElevated else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, if (isDark) CharcoalBorder else Slate200),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDark) CharcoalBorder else Slate200),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = review.userName.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = review.userName,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Verified Session",
                                                    fontSize = 10.sp,
                                                    color = AccentEmerald,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        StarRatingDisplay(
                                            rating = review.rating,
                                            starSize = 14.dp
                                        )
                                    }

                                    if (review.comment.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = review.comment,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Bar: Left: ₹40/hour, Right: Book Spot button
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "₹${space.hourlyPrice.toInt()}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "/hour",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Text(
                        text = "Free cancellation up to 1 hr before",
                        fontSize = 11.sp,
                        color = AccentEmerald,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { onBookNow(space.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) CharcoalElevated else Slate900,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .width(150.dp)
                        .testTag("detail_book_now_button")
                ) {
                    Text(
                        text = "Book Spot",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showRatingDialog && onSubmitReview != null) {
        RatingReviewDialog(
            parkingTitle = space.title,
            onDismiss = { showRatingDialog = false },
            onSubmitReview = { rating, comment ->
                onSubmitReview(rating, comment)
                showRatingDialog = false
            }
        )
    }
}
