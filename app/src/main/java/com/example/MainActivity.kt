package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.ParkSpaceViewModel
import com.example.ui.components.FilterSheet
import com.example.ui.components.LanguageSelectionDialog
import com.example.ui.components.NotificationDialog
import com.example.ui.components.ParkSpaceBottomNav
import com.example.ui.components.ParkSpaceTopBar
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.getAppStrings
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AvailabilityScreen
import com.example.ui.screens.BookingFlowScreen
import com.example.ui.screens.DigitalPassScreen
import com.example.ui.screens.EarningsScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.ListMySpaceWizard
import com.example.ui.screens.MyBookingsScreen
import com.example.ui.screens.MySpacesScreen
import com.example.ui.screens.ParkingDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProviderDashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ParkSpaceViewModel = viewModel()
            val darkModePreference by viewModel.darkModePreference.collectAsStateWithLifecycle()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDark = darkModePreference ?: systemDark

            MyApplicationTheme(darkTheme = isDark) {
                ParkSpaceApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ParkSpaceApp(
    viewModel: ParkSpaceViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val filteredSpaces by viewModel.filteredSpaces.collectAsStateWithLifecycle()
    val allSpaces by viewModel.allSpaces.collectAsStateWithLifecycle()
    val providerSpaces by viewModel.providerSpaces.collectAsStateWithLifecycle()
    val userBookings by viewModel.userBookings.collectAsStateWithLifecycle()
    val providerBookings by viewModel.providerBookings.collectAsStateWithLifecycle()
    val allBookings by viewModel.allBookings.collectAsStateWithLifecycle()
    val userVehicles by viewModel.userVehicles.collectAsStateWithLifecycle()
    val userNotifications by viewModel.userNotifications.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()
    val allReviews by viewModel.allReviews.collectAsStateWithLifecycle()
    val selectedSpaceId by viewModel.selectedSpaceId.collectAsStateWithLifecycle()
    val selectedBookingId by viewModel.selectedBookingId.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()
    val darkModePreference by viewModel.darkModePreference.collectAsStateWithLifecycle()
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = darkModePreference ?: systemDark

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val strings = remember(appLanguage) { getAppStrings(appLanguage) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            viewModel.fetchCurrentGpsLocation()
        } else {
            viewModel.showMessage("Location permission denied. You can still select locations manually.")
        }
    }

    val onRequestGpsLocation: () -> Unit = {
        if (viewModel.hasLocationPermission()) {
            viewModel.fetchCurrentGpsLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val unreadNotificationsCount = userNotifications.count { !it.isRead }

    val shouldShowTopBar = currentScreen != AppScreen.LANDING &&
            currentScreen != AppScreen.BOOKING_FLOW &&
            currentScreen != AppScreen.LIST_SPACE_WIZARD

    val shouldShowBottomNav = currentScreen != AppScreen.LANDING &&
            currentScreen != AppScreen.BOOKING_FLOW &&
            currentScreen != AppScreen.DIGITAL_PASS &&
            currentScreen != AppScreen.LIST_SPACE_WIZARD

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalAppLanguage provides appLanguage
    ) {
        if (showLanguageDialog) {
            LanguageSelectionDialog(
                currentLanguage = appLanguage,
                onSelectLanguage = { viewModel.setLanguage(it) },
                onDismiss = { showLanguageDialog = false }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (shouldShowTopBar) {
                    ParkSpaceTopBar(
                        activeMode = activeMode,
                        unreadNotificationCount = unreadNotificationsCount,
                        isDarkMode = isDark,
                        currentLanguage = appLanguage,
                        onToggleDarkMode = { viewModel.toggleDarkMode(isDark) },
                        onLanguageClick = { showLanguageDialog = true },
                        onModeChange = { mode -> viewModel.setMode(mode) },
                        onNotificationsClick = { showNotificationDialog = true },
                        onProfileClick = { viewModel.navigateTo(AppScreen.PROFILE) },
                        onBrandClick = {
                            if (activeMode == "Provider") {
                                viewModel.navigateTo(AppScreen.PROVIDER_DASHBOARD)
                            } else if (activeMode == "Admin") {
                                viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD)
                            } else {
                                viewModel.navigateTo(AppScreen.HOME)
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (shouldShowBottomNav) {
                    ParkSpaceBottomNav(
                        activeMode = activeMode,
                        currentScreen = currentScreen,
                        onNavigate = { screen -> viewModel.navigateTo(screen) }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { target ->
                when (target) {
                    AppScreen.LANDING -> {
                        LandingScreen(
                            onFindParking = { viewModel.navigateTo(AppScreen.EXPLORE) },
                            onListSpace = { viewModel.navigateTo(AppScreen.LIST_SPACE_WIZARD) },
                            onSearchDestination = { location, vehicle ->
                                viewModel.updateSearchQuery(location)
                                viewModel.updateVehicleType(vehicle)
                                viewModel.navigateTo(AppScreen.EXPLORE)
                            }
                        )
                    }
                    AppScreen.HOME -> {
                        HomeScreen(
                            user = currentUser,
                            spaces = filteredSpaces.ifEmpty { allSpaces },
                            onSearch = { q ->
                                viewModel.updateSearchQuery(q)
                                viewModel.navigateTo(AppScreen.EXPLORE)
                            },
                            onSelectCategory = { cat ->
                                when (cat) {
                                    "EV" -> {
                                        viewModel.resetFilters()
                                        viewModel.toggleFeature("EV Charging")
                                    }
                                    "Residential" -> {
                                        viewModel.resetFilters()
                                        viewModel.updateParkingType("Residential")
                                    }
                                    else -> {
                                        viewModel.resetFilters()
                                        viewModel.updateVehicleType(cat)
                                    }
                                }
                                viewModel.navigateTo(AppScreen.EXPLORE)
                            },
                            onViewDetails = { spaceId -> viewModel.viewSpaceDetails(spaceId) },
                            onBookNow = { spaceId -> viewModel.startBooking(spaceId) },
                            onExploreAll = { viewModel.navigateTo(AppScreen.EXPLORE) },
                            onListSpace = { viewModel.navigateTo(AppScreen.LIST_SPACE_WIZARD) },
                            userLocation = userLocation,
                            onNearMeClick = { radius ->
                                viewModel.setNearbyRadius(radius)
                                viewModel.navigateTo(AppScreen.EXPLORE)
                            },
                            onChangeLocation = { loc -> viewModel.setUserLocation(loc) },
                            onRequestGpsLocation = onRequestGpsLocation,
                            isLocating = isLocating,
                            getDistanceString = { sp -> viewModel.getDistanceString(sp) },
                            getWalkingTimeString = { sp -> viewModel.getWalkingTimeString(sp) }
                        )
                    }
                    AppScreen.EXPLORE -> {
                        ExploreScreen(
                            spaces = filteredSpaces,
                            filterState = filters,
                            selectedSpaceId = selectedSpaceId,
                            onSelectSpace = { spaceId -> viewModel.viewSpaceDetails(spaceId) },
                            onSearchChange = { q -> viewModel.updateSearchQuery(q) },
                            onCitySelect = { city -> viewModel.updateSelectedCity(city) },
                            onOpenFilterSheet = { showFilterSheet = true },
                            onResetFilters = { viewModel.resetFilters() },
                            onViewDetails = { spaceId -> viewModel.viewSpaceDetails(spaceId) },
                            onBookNow = { spaceId -> viewModel.startBooking(spaceId) },
                            userLocation = userLocation,
                            onToggleNearMe = { viewModel.toggleNearMe() },
                            onSetNearbyRadius = { r -> viewModel.setNearbyRadius(r) },
                            onChangeUserLocation = { loc -> viewModel.setUserLocation(loc) },
                            onRequestGpsLocation = onRequestGpsLocation,
                            isLocating = isLocating,
                            getDistanceString = { sp -> viewModel.getDistanceString(sp) },
                            getWalkingTimeString = { sp -> viewModel.getWalkingTimeString(sp) }
                        )
                    }
                    AppScreen.DETAIL -> {
                        val space = allSpaces.find { it.id == selectedSpaceId } ?: filteredSpaces.firstOrNull()
                        ParkingDetailScreen(
                            space = space,
                            reviews = allReviews,
                            onBack = { viewModel.navigateTo(AppScreen.EXPLORE) },
                            onBookNow = { id -> viewModel.startBooking(id) },
                            onSubmitReview = { rating, comment ->
                                if (space != null) {
                                    viewModel.submitReview(0L, space.id, rating, comment)
                                }
                            },
                            userLocation = userLocation
                        )
                    }
                    AppScreen.BOOKING_FLOW -> {
                        val space = allSpaces.find { it.id == selectedSpaceId } ?: filteredSpaces.firstOrNull()
                        BookingFlowScreen(
                            space = space,
                            savedVehicles = userVehicles,
                            onBack = {
                                if (selectedSpaceId != null) {
                                    viewModel.navigateTo(AppScreen.DETAIL)
                                } else {
                                    viewModel.navigateTo(AppScreen.EXPLORE)
                                }
                            },
                            onCompleteBooking = { sp, vt, vr, d, st, et, h, sub, fee, tot, pay, cb ->
                                viewModel.completeBooking(sp, vt, vr, d, st, et, h, sub, fee, tot, pay, cb)
                            },
                            onViewPass = { bookingId -> viewModel.viewBookingPass(bookingId) }
                        )
                    }
                    AppScreen.DIGITAL_PASS -> {
                        val booking = (allBookings + userBookings).find { it.id == selectedBookingId } ?: userBookings.firstOrNull()
                        DigitalPassScreen(
                            booking = booking,
                            onBack = { viewModel.navigateTo(AppScreen.MY_BOOKINGS) },
                            onCompleteBooking = { bookingId -> viewModel.completeBookingSession(bookingId) },
                            onSubmitReview = { bkId, spId, rating, comment ->
                                viewModel.submitReview(bkId, spId, rating, comment)
                            }
                        )
                    }
                    AppScreen.MY_BOOKINGS -> {
                        MyBookingsScreen(
                            bookings = userBookings,
                            onViewPass = { bookingId -> viewModel.viewBookingPass(bookingId) },
                            onCancelBooking = { bookingId -> viewModel.cancelBooking(bookingId) },
                            onSubmitReview = { bkId, spId, rating, comment ->
                                viewModel.submitReview(bkId, spId, rating, comment)
                            },
                            onFindParking = { viewModel.navigateTo(AppScreen.EXPLORE) },
                            onCompleteBooking = { bookingId -> viewModel.completeBookingSession(bookingId) }
                        )
                    }
                    AppScreen.LIST_SPACE_WIZARD -> {
                        ListMySpaceWizard(
                            onRequestGpsLocation = onRequestGpsLocation,
                            userLocation = userLocation,
                            onCancel = {
                                if (activeMode == "Provider") {
                                    viewModel.navigateTo(AppScreen.PROVIDER_DASHBOARD)
                                } else {
                                    viewModel.navigateTo(AppScreen.HOME)
                                }
                            },
                            onPublish = { newSpace ->
                                viewModel.createNewSpace(newSpace) {
                                    viewModel.setMode("Provider")
                                    viewModel.navigateTo(AppScreen.PROVIDER_DASHBOARD)
                                }
                            }
                        )
                    }
                    AppScreen.PROVIDER_DASHBOARD -> {
                        ProviderDashboardScreen(
                            user = currentUser,
                            spaces = providerSpaces.ifEmpty { allSpaces.filter { it.ownerId == 1L } },
                            bookings = providerBookings.ifEmpty { userBookings },
                            onAddNewSpace = { viewModel.navigateTo(AppScreen.LIST_SPACE_WIZARD) },
                            onManageSpaces = { viewModel.navigateTo(AppScreen.MY_SPACES) },
                            onOpenCalendar = { viewModel.navigateTo(AppScreen.AVAILABILITY) },
                            onOpenEarnings = { viewModel.navigateTo(AppScreen.EARNINGS) },
                            onToggleSpaceStatus = { space -> viewModel.toggleSpaceStatus(space) },
                            onWithdrawEarnings = { amt -> viewModel.withdrawEarnings(amt) }
                        )
                    }
                    AppScreen.MY_SPACES -> {
                        MySpacesScreen(
                            spaces = providerSpaces.ifEmpty { allSpaces.filter { it.ownerId == 1L } },
                            onAddNew = { viewModel.navigateTo(AppScreen.LIST_SPACE_WIZARD) },
                            onToggleStatus = { space -> viewModel.toggleSpaceStatus(space) },
                            onBack = { viewModel.navigateTo(AppScreen.PROVIDER_DASHBOARD) }
                        )
                    }
                    AppScreen.AVAILABILITY -> {
                        AvailabilityScreen(
                            spaces = providerSpaces,
                            onBack = { viewModel.navigateTo(AppScreen.PROVIDER_DASHBOARD) }
                        )
                    }
                    AppScreen.EARNINGS -> {
                        EarningsScreen(
                            user = currentUser,
                            bookings = providerBookings.ifEmpty { userBookings },
                            onWithdraw = { amt -> viewModel.withdrawEarnings(amt) },
                            onBack = { viewModel.navigateTo(AppScreen.PROVIDER_DASHBOARD) }
                        )
                    }
                    AppScreen.PROFILE -> {
                        ProfileScreen(
                            user = currentUser,
                            vehicles = userVehicles,
                            activeMode = activeMode,
                            onModeChange = { mode -> viewModel.setMode(mode) },
                            onAddVehicle = { t, r, m -> viewModel.addVehicle(t, r, m) },
                            onDeleteVehicle = { id -> viewModel.deleteVehicle(id) },
                            onSubmitKyc = { viewModel.submitKycVerification() },
                            onUpdateProfile = { name, phone, email -> viewModel.updateUserProfile(name, phone, email) },
                            darkModePreference = darkModePreference,
                            onToggleDarkMode = { viewModel.toggleDarkMode(isDark) },
                            currentLanguage = appLanguage,
                            onSelectLanguage = { viewModel.setLanguage(it) }
                        )
                    }
                    AppScreen.ADMIN_DASHBOARD -> {
                        AdminDashboardScreen(
                            spaces = allSpaces,
                            bookings = allBookings.ifEmpty { userBookings },
                            settings = platformSettings,
                            onApproveListing = { id -> viewModel.adminApproveListing(id) },
                            onRejectListing = { id -> viewModel.adminRejectListing(id) },
                            onUpdateCommission = { comm -> viewModel.updatePlatformCommission(comm) },
                            onBack = { viewModel.navigateTo(AppScreen.HOME) }
                        )
                    }
                }
            }

            // Filter Bottom Sheet / Dialog
            if (showFilterSheet) {
                FilterSheet(
                    filterState = filters,
                    onVehicleSelect = { v -> viewModel.updateVehicleType(v) },
                    onParkingTypeSelect = { pt -> viewModel.updateParkingType(pt) },
                    onPriceTierSelect = { pt -> viewModel.updatePriceTier(pt) },
                    onSortSelect = { s -> viewModel.updateSortOption(s) },
                    onToggleFeature = { f -> viewModel.toggleFeature(f) },
                    onReset = { viewModel.resetFilters() },
                    onDismiss = { showFilterSheet = false }
                )
            }

            // Notifications Dialog
            if (showNotificationDialog) {
                NotificationDialog(
                    notifications = userNotifications,
                    onMarkRead = { id -> viewModel.markNotificationRead(id) },
                    onMarkAllRead = { viewModel.markAllNotificationsRead() },
                    onDismiss = { showNotificationDialog = false }
                )
            }
        }
    }
}
}
