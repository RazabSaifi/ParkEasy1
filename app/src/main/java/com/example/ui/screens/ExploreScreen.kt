package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ParkingSpace
import com.example.data.util.LocationUtils
import com.example.data.util.UserLocation
import com.example.ui.FilterState
import com.example.ui.components.InteractiveMapView
import com.example.ui.components.ParkingCard
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.CharcoalBackground
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CharcoalSubtle
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

@Composable
fun ExploreScreen(
    spaces: List<ParkingSpace>,
    filterState: FilterState,
    selectedSpaceId: Long?,
    onSelectSpace: (Long) -> Unit,
    onSearchChange: (String) -> Unit,
    onCitySelect: (String) -> Unit,
    onOpenFilterSheet: () -> Unit,
    onResetFilters: () -> Unit,
    onViewDetails: (Long) -> Unit,
    onBookNow: (Long) -> Unit,
    modifier: Modifier = Modifier,
    userLocation: UserLocation? = null,
    onToggleNearMe: () -> Unit = {},
    onSetNearbyRadius: (Double?) -> Unit = {},
    onChangeUserLocation: (UserLocation) -> Unit = {},
    onRequestGpsLocation: () -> Unit = {},
    isLocating: Boolean = false,
    getDistanceString: ((ParkingSpace) -> String)? = null,
    getWalkingTimeString: ((ParkingSpace) -> String)? = null
) {
    val strings = LocalStrings.current
    // On mobile, default to Map view matching Screen 2 of the visual reference
    var isMobileMapVisible by remember { mutableStateOf(true) }
    var showLocationDialog by remember { mutableStateOf(false) }

    val cities = listOf("All Cities", "Bengaluru", "New Delhi", "Mumbai", "Hyderabad", "Pune")

    val isDark = MaterialTheme.colorScheme.background == CharcoalBackground

    if (showLocationDialog) {
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.chooseArea, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column {
                    // Google Play Services GPS Action Card
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) CharcoalElevated else Color(0xFFEFF6FF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) PrimaryBlue.copy(alpha = 0.5f) else Color(0xFF93C5FD)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onRequestGpsLocation()
                                showLocationDialog = false
                            }
                            .testTag("explore_detect_gps_dialog_card")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "Current GPS",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isLocating) "Locking GPS Coordinates..." else "Use Device GPS Coordinates",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "Google Play Services Fused Location",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Or choose a neighborhood to simulate:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LocationUtils.popularLocations.forEach { loc ->
                        val isCurrent = loc.id == userLocation?.id
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChangeUserLocation(loc)
                                    showLocationDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = isCurrent,
                                onClick = {
                                    onChangeUserLocation(loc)
                                    showLocationDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = loc.name,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isCurrent) PrimaryBlue else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${loc.locality}, ${loc.city}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLocationDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWideScreen = maxWidth > 720.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Search & City Filter Bar (Top Header)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = filterState.searchQuery,
                                    onValueChange = onSearchChange,
                                    placeholder = { Text(strings.searchPlaceholder, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("explore_search_input")
                                )
                                if (filterState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchChange("") },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Filter Button
                        val hasActiveFilters = filterState.vehicleType != "All" ||
                                filterState.parkingType != "All" ||
                                filterState.priceTier != "All" ||
                                filterState.isCoveredOnly ||
                                filterState.hasEvChargingOnly

                        IconButton(
                            onClick = onOpenFilterSheet,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (hasActiveFilters) MaterialTheme.colorScheme.primaryContainer else if (isDark) CharcoalSubtle else Slate100,
                                    RoundedCornerShape(12.dp)
                                )
                                .testTag("explore_filter_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (hasActiveFilters) {
                                        Badge(containerColor = PrimaryBlue)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filters",
                                    tint = if (hasActiveFilters) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Location & Nearby Radius Controls Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // GPS quick action chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLocating) PrimaryBlue.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isLocating) PrimaryBlue else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .clickable { onRequestGpsLocation() }
                                .testTag("explore_gps_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                if (isLocating) {
                                    CircularProgressIndicator(
                                        color = PrimaryBlue,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = "GPS Coordinates",
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLocating) "Locating..." else "My GPS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }

                        // Current simulated GPS location switcher chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) CharcoalElevated else Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) CharcoalBorder else Color(0xFFBFDBFE)),
                            modifier = Modifier
                                .clickable { showLocationDialog = true }
                                .testTag("explore_location_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = userLocation?.name ?: "Indiranagar",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Near Me Mode Toggle Chip
                        FilterChip(
                            selected = filterState.isNearMeActive,
                            onClick = onToggleNearMe,
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = if (filterState.isNearMeActive) Color.White else AccentEmerald,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Near Me", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentEmerald,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("explore_near_me_chip")
                        )

                        // Proximity radius options
                        val radiusOptions = listOf(
                            Pair("< 500m", 0.5),
                            Pair("< 1 km", 1.0),
                            Pair("< 2 km", 2.0),
                            Pair("< 5 km", 5.0)
                        )

                        radiusOptions.forEach { (label, radius) ->
                            val isSelected = filterState.nearbyRadiusKm == radius
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) onSetNearbyRadius(null) else onSetNearbyRadius(radius)
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }

                        // City filter chips
                        cities.forEach { city ->
                            val selected = filterState.selectedCity == city
                            FilterChip(
                                selected = selected,
                                onClick = { onCitySelect(city) },
                                label = { Text(city, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = PrimaryBlue
                                )
                            )
                        }
                    }
                }
            }

            // Body: Split-screen on Desktop/Tablet, or toggleable on Mobile
            if (isWideScreen) {
                // Split Screen Desktop Experience: Left List, Right Map
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        ParkingListContent(
                            spaces = spaces,
                            onViewDetails = onViewDetails,
                            onBookNow = onBookNow,
                            onResetFilters = onResetFilters,
                            getDistanceString = getDistanceString,
                            getWalkingTimeString = getWalkingTimeString
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                    ) {
                        InteractiveMapView(
                            spaces = spaces,
                            selectedSpaceId = selectedSpaceId,
                            onSelectSpace = onSelectSpace,
                            onViewDetails = onViewDetails,
                            onBookNow = onBookNow,
                            userLocation = userLocation,
                            nearbyRadiusKm = filterState.nearbyRadiusKm,
                            onCenterToLocation = onRequestGpsLocation
                        )
                    }
                }
            } else {
                // Mobile Experience: Listings First or Full Map
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isMobileMapVisible) {
                        InteractiveMapView(
                            spaces = spaces,
                            selectedSpaceId = selectedSpaceId,
                            onSelectSpace = onSelectSpace,
                            onViewDetails = onViewDetails,
                            onBookNow = onBookNow,
                            userLocation = userLocation,
                            nearbyRadiusKm = filterState.nearbyRadiusKm,
                            onCenterToLocation = onRequestGpsLocation
                        )
                    } else {
                        ParkingListContent(
                            spaces = spaces,
                            onViewDetails = onViewDetails,
                            onBookNow = onBookNow,
                            onResetFilters = onResetFilters,
                            getDistanceString = getDistanceString,
                            getWalkingTimeString = getWalkingTimeString
                        )
                    }

                    // Floating Toggle Button ("Map" / "List")
                    ExtendedFloatingActionButton(
                        onClick = { isMobileMapVisible = !isMobileMapVisible },
                        icon = {
                            Icon(
                                imageVector = if (isMobileMapVisible) Icons.Default.List else Icons.Default.Map,
                                contentDescription = null,
                                tint = Color.White
                            )
                        },
                        text = {
                            Text(
                                text = if (isMobileMapVisible) "${strings.listView} (${spaces.size})" else strings.mapView,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        },
                        containerColor = PrimaryBlue,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .testTag("mobile_map_toggle_fab")
                    )
                }
            }
        }
    }
}

@Composable
private fun ParkingListContent(
    spaces: List<ParkingSpace>,
    onViewDetails: (Long) -> Unit,
    onBookNow: (Long) -> Unit,
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
    getDistanceString: ((ParkingSpace) -> String)? = null,
    getWalkingTimeString: ((ParkingSpace) -> String)? = null
) {
    val strings = LocalStrings.current
    if (spaces.isEmpty()) {
        // Empty State
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No parking spaces found nearby.",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Try clearing filters or expanding search radius.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onResetFilters,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("empty_expand_search_btn")
            ) {
                Text(strings.clearFilters)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            item {
                Text(
                    text = "${spaces.size} ${strings.spotsAvailable}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            items(spaces, key = { space -> space.id }) { space ->
                Box(modifier = Modifier.padding(vertical = 6.dp)) {
                    ParkingCard(
                        space = space,
                        onViewDetails = { onViewDetails(space.id) },
                        onBookNow = { onBookNow(space.id) },
                        distanceText = getDistanceString?.invoke(space),
                        walkTimeText = getWalkingTimeString?.invoke(space)
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(70.dp)) // padding for bottom nav & FAB
            }
        }
    }
}
