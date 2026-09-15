package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DemoData
import com.example.data.model.BlockedSlot
import com.example.data.model.Booking
import com.example.data.model.NotificationItem
import com.example.data.model.ParkingSpace
import com.example.data.model.PlatformSettings
import com.example.data.model.Review
import com.example.data.model.User
import com.example.data.model.Vehicle
import com.example.data.repository.ParkSpaceRepository
import com.example.data.util.GpsLocationProvider
import com.example.data.util.LocationUtils
import com.example.data.util.UserLocation
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    LANDING,
    HOME,
    EXPLORE,
    DETAIL,
    BOOKING_FLOW,
    DIGITAL_PASS,
    MY_BOOKINGS,
    LIST_SPACE_WIZARD,
    PROVIDER_DASHBOARD,
    MY_SPACES,
    AVAILABILITY,
    EARNINGS,
    PROFILE,
    ADMIN_DASHBOARD
}

data class FilterState(
    val searchQuery: String = "",
    val selectedCity: String = "All Cities",
    val vehicleType: String = "All", // "All", "Car", "Bike", "SUV", "Van"
    val parkingType: String = "All", // "All", "Residential", "Commercial", "Open Plot", "Basement", "Private Garage"
    val priceTier: String = "All", // "All", "Under ₹20", "₹20 - ₹50", "₹50 - ₹100", "₹100+"
    val maxPrice: Double? = null,
    val distanceFilter: String = "All", // "All", "< 500m", "< 1km", "< 2km", "< 5km"
    val isNearMeActive: Boolean = false,
    val nearbyRadiusKm: Double? = null,
    val isCoveredOnly: Boolean = false,
    val hasCctvOnly: Boolean = false,
    val hasGuardOnly: Boolean = false,
    val hasEvChargingOnly: Boolean = false,
    val has24x7Only: Boolean = false,
    val sortOption: String = "Recommended" // "Recommended", "Nearest", "Cheapest", "Highest Rated"
)

class ParkSpaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ParkSpaceRepository
    private val gpsLocationProvider = GpsLocationProvider(application)

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = ParkSpaceRepository(db)
        viewModelScope.launch {
            repository.ensureDemoDataInitialized()
        }
    }

    // GPS Locating State
    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    // Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedSpaceId = MutableStateFlow<Long?>(null)
    val selectedSpaceId: StateFlow<Long?> = _selectedSpaceId.asStateFlow()

    private val _selectedBookingId = MutableStateFlow<Long?>(null)
    val selectedBookingId: StateFlow<Long?> = _selectedBookingId.asStateFlow()

    // Active User Mode: "Seeker", "Provider", "Admin"
    private val _activeMode = MutableStateFlow("Seeker")
    val activeMode: StateFlow<String> = _activeMode.asStateFlow()

    // Dark Mode Theme State (null = Follow System, true = Dark, false = Light)
    private val _darkModePreference = MutableStateFlow<Boolean?>(null)
    val darkModePreference: StateFlow<Boolean?> = _darkModePreference.asStateFlow()

    fun setDarkMode(enabled: Boolean?) {
        _darkModePreference.value = enabled
        val label = when (enabled) {
            true -> "Charcoal dark mode enabled"
            false -> "Light mode enabled"
            null -> "System theme enabled"
        }
        showMessage(label)
    }

    fun toggleDarkMode(currentIsDark: Boolean? = null) {
        val current = currentIsDark ?: _darkModePreference.value ?: true
        setDarkMode(!current)
    }

    // App Language State (English, Hindi, Kannada, Tamil, Telugu, Spanish)
    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        val msg = when (language) {
            AppLanguage.ENGLISH -> "Language switched to English 🇬🇧"
            AppLanguage.HINDI -> "भाषा बदलकर हिन्दी कर दी गई है 🇮🇳"
            AppLanguage.KANNADA -> "ಭಾಷೆಯನ್ನು ಕನ್ನಡಕ್ಕೆ ಬದಲಾಯಿಸಲಾಗಿದೆ 🇮🇳"
            AppLanguage.TAMIL -> "மொழி தமிழுக்கு மாற்றப்பட்டது 🇮🇳"
            AppLanguage.TELUGU -> "భాష తెలుగుకు మార్చబడింది 🇮🇳"
            AppLanguage.SPANISH -> "Idioma cambiado a Español 🇪🇸"
        }
        showMessage(msg)
    }

    // User Simulated GPS Location
    private val _userLocation = MutableStateFlow(LocationUtils.popularLocations[0])
    val userLocation: StateFlow<UserLocation> = _userLocation.asStateFlow()

    // Filters
    private val _filters = MutableStateFlow(FilterState())
    val filters: StateFlow<FilterState> = _filters.asStateFlow()

    // Data Streams
    val currentUser: StateFlow<User?> = repository.getUser(1L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DemoData.initialUser)

    val allSpaces: StateFlow<List<ParkingSpace>> = repository.getAllSpaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providerSpaces: StateFlow<List<ParkingSpace>> = repository.getSpacesByOwner(1L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userBookings: StateFlow<List<Booking>> = repository.getUserBookings(1L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val providerBookings: StateFlow<List<Booking>> = repository.getProviderBookings(1L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBookings: StateFlow<List<Booking>> = repository.getAllBookings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userVehicles: StateFlow<List<Vehicle>> = repository.getUserVehicles(1L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userNotifications: StateFlow<List<NotificationItem>> = repository.getUserNotifications(1L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val platformSettings: StateFlow<PlatformSettings?> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DemoData.initialSettings)

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReviews: StateFlow<List<Review>> = repository.getAllReviews()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Spaces calculation with real proximity
    val filteredSpaces: StateFlow<List<ParkingSpace>> = combine(allSpaces, _filters, _userLocation) { spaces, f, userLoc ->
        var list = spaces.filter { it.status == "Active" }

        // Search Query (Searches across all locations if query typed)
        if (f.searchQuery.isNotBlank()) {
            val q = f.searchQuery.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                it.area.lowercase().contains(q) ||
                it.city.lowercase().contains(q) ||
                it.address.lowercase().contains(q)
            }
        } else if (f.selectedCity != "All Cities") {
            list = list.filter {
                it.city.equals(f.selectedCity, ignoreCase = true) ||
                it.area.equals(f.selectedCity, ignoreCase = true) ||
                it.city.isBlank()
            }
        }

        // Vehicle Type
        if (f.vehicleType != "All") {
            list = list.filter { it.supportedVehicles.contains(f.vehicleType, ignoreCase = true) }
        }

        // Parking Type
        if (f.parkingType != "All") {
            list = list.filter { it.parkingType.equals(f.parkingType, ignoreCase = true) }
        }

        // Price Tier
        when (f.priceTier) {
            "Under ₹20" -> list = list.filter { it.hourlyPrice < 20 }
            "₹20 - ₹50" -> list = list.filter { it.hourlyPrice in 20.0..50.0 }
            "₹50 - ₹100" -> list = list.filter { it.hourlyPrice in 50.0..100.0 }
            "₹100+" -> list = list.filter { it.hourlyPrice >= 100 }
        }

        // Feature toggles
        if (f.isCoveredOnly) list = list.filter { it.isCovered }
        if (f.hasCctvOnly) list = list.filter { it.hasCctv }
        if (f.hasGuardOnly) list = list.filter { it.hasSecurityGuard }
        if (f.hasEvChargingOnly) list = list.filter { it.hasEvCharging }
        if (f.has24x7Only) list = list.filter { it.has24x7Access }

        // Proximity / Nearby Radius filtering
        val maxRadiusKm = when {
            f.nearbyRadiusKm != null -> f.nearbyRadiusKm
            f.distanceFilter == "< 500m" -> 0.5
            f.distanceFilter == "< 1km" -> 1.0
            f.distanceFilter == "< 2km" -> 2.0
            f.distanceFilter == "< 5km" -> 5.0
            f.isNearMeActive -> 5.0
            else -> null
        }

        if (maxRadiusKm != null) {
            list = list.filter { space ->
                val dist = LocationUtils.calculateDistanceKm(
                    userLoc.latitude,
                    userLoc.longitude,
                    space.latitude,
                    space.longitude
                )
                dist <= maxRadiusKm
            }
        }

        // Sorting
        when {
            f.isNearMeActive || f.sortOption == "Nearest" -> {
                list.sortedBy { space ->
                    LocationUtils.calculateDistanceKm(
                        userLoc.latitude,
                        userLoc.longitude,
                        space.latitude,
                        space.longitude
                    )
                }
            }
            f.sortOption == "Cheapest" -> list.sortedBy { it.hourlyPrice }
            f.sortOption == "Highest Rated" -> list.sortedByDescending { it.rating }
            else -> list.sortedByDescending { it.rating * 10 + it.reviewsCount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback Snackbar / Banner message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    // Navigation Actions
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun viewSpaceDetails(spaceId: Long) {
        _selectedSpaceId.value = spaceId
        _currentScreen.value = AppScreen.DETAIL
    }

    fun startBooking(spaceId: Long) {
        _selectedSpaceId.value = spaceId
        _currentScreen.value = AppScreen.BOOKING_FLOW
    }

    fun viewBookingPass(bookingId: Long) {
        _selectedBookingId.value = bookingId
        _currentScreen.value = AppScreen.DIGITAL_PASS
    }

    fun setMode(mode: String) {
        _activeMode.value = mode
        viewModelScope.launch {
            repository.setActiveMode(1L, mode)
        }
        // Direct to appropriate home
        when (mode) {
            "Provider" -> _currentScreen.value = AppScreen.PROVIDER_DASHBOARD
            "Admin" -> _currentScreen.value = AppScreen.ADMIN_DASHBOARD
            else -> _currentScreen.value = AppScreen.HOME
        }
    }

    // Filter Updates
    fun updateSearchQuery(query: String) {
        _filters.value = _filters.value.copy(searchQuery = query)
    }

    fun updateSelectedCity(city: String) {
        _filters.value = _filters.value.copy(selectedCity = city)
    }

    fun updateVehicleType(type: String) {
        _filters.value = _filters.value.copy(vehicleType = type)
    }

    fun updateParkingType(type: String) {
        _filters.value = _filters.value.copy(parkingType = type)
    }

    fun updatePriceTier(tier: String) {
        _filters.value = _filters.value.copy(priceTier = tier)
    }

    fun updateSortOption(sort: String) {
        _filters.value = _filters.value.copy(sortOption = sort)
    }

    // Nearby Feature & GPS Methods
    fun hasLocationPermission(): Boolean = gpsLocationProvider.hasLocationPermission()

    fun fetchCurrentGpsLocation(onPermissionRequired: () -> Unit = {}) {
        if (!gpsLocationProvider.hasLocationPermission()) {
            onPermissionRequired()
            return
        }

        viewModelScope.launch {
            _isLocating.value = true
            try {
                val loc = gpsLocationProvider.getCurrentGpsLocation()
                if (loc != null) {
                    val userLoc = gpsLocationProvider.reverseGeocode(loc.latitude, loc.longitude)
                    _userLocation.value = userLoc
                    _filters.value = _filters.value.copy(
                        isNearMeActive = true,
                        selectedCity = if (userLoc.city.isNotBlank()) userLoc.city else _filters.value.selectedCity,
                        sortOption = "Nearest"
                    )
                    showMessage("GPS Location locked: ${userLoc.name} 📍")
                } else {
                    showMessage("GPS signal weak or unavailable. Using default location.")
                }
            } catch (e: Exception) {
                showMessage("Location error: ${e.localizedMessage ?: "Unknown"}")
            } finally {
                _isLocating.value = false
            }
        }
    }

    fun setUserLocation(location: UserLocation) {
        _userLocation.value = location
        // Auto-align city filter if city matches or reset city
        _filters.value = _filters.value.copy(selectedCity = location.city)
        showMessage("Location updated to ${location.name}, ${location.city} 📍")
    }

    fun toggleNearMe(active: Boolean? = null) {
        val newActive = active ?: !_filters.value.isNearMeActive
        _filters.value = _filters.value.copy(
            isNearMeActive = newActive,
            sortOption = if (newActive) "Nearest" else _filters.value.sortOption
        )
        if (newActive) {
            showMessage("Showing parking spaces near ${_userLocation.value.locality} 📍")
        }
    }

    fun setNearbyRadius(radiusKm: Double?) {
        _filters.value = _filters.value.copy(
            nearbyRadiusKm = radiusKm,
            isNearMeActive = radiusKm != null,
            sortOption = if (radiusKm != null) "Nearest" else _filters.value.sortOption
        )
        val msg = if (radiusKm != null) {
            "Searching within ${LocationUtils.formatDistance(radiusKm)} of ${_userLocation.value.name}"
        } else {
            "Proximity radius cleared"
        }
        showMessage(msg)
    }

    fun getDistanceForSpace(space: ParkingSpace): Double {
        val loc = _userLocation.value
        return LocationUtils.calculateDistanceKm(
            loc.latitude,
            loc.longitude,
            space.latitude,
            space.longitude
        )
    }

    fun getDistanceString(space: ParkingSpace): String {
        return LocationUtils.formatDistance(getDistanceForSpace(space))
    }

    fun getWalkingTimeString(space: ParkingSpace): String {
        return LocationUtils.formatWalkingTime(getDistanceForSpace(space))
    }

    fun toggleFeature(feature: String) {
        val current = _filters.value
        _filters.value = when (feature) {
            "Covered" -> current.copy(isCoveredOnly = !current.isCoveredOnly)
            "CCTV" -> current.copy(hasCctvOnly = !current.hasCctvOnly)
            "Guard" -> current.copy(hasGuardOnly = !current.hasGuardOnly)
            "EV Charging" -> current.copy(hasEvChargingOnly = !current.hasEvChargingOnly)
            "24/7 Access" -> current.copy(has24x7Only = !current.has24x7Only)
            else -> current
        }
    }

    fun resetFilters() {
        _filters.value = FilterState()
    }

    // Booking Creation
    fun completeBooking(
        space: ParkingSpace,
        vehicleType: String,
        vehicleReg: String,
        date: String,
        startTime: String,
        endTime: String,
        hours: Int,
        subtotal: Double,
        fee: Double,
        total: Double,
        paymentMethod: String,
        onSuccess: (Booking) -> Unit
    ) {
        viewModelScope.launch {
            val booking = repository.createBooking(
                userId = 1L,
                userName = currentUser.value?.name ?: "Rohan Sharma",
                space = space,
                vehicleType = vehicleType,
                vehicleRegNumber = vehicleReg,
                bookingDate = date,
                startTime = startTime,
                endTime = endTime,
                durationHours = hours,
                subtotal = subtotal,
                platformFee = fee,
                totalAmount = total,
                paymentMethod = paymentMethod
            )
            _selectedBookingId.value = booking.id
            showMessage("Booking confirmed for ${space.title} 🎉")
            onSuccess(booking)
        }
    }

    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            repository.cancelBooking(bookingId, 1L)
            showMessage("Booking #$bookingId has been cancelled.")
        }
    }

    fun completeBookingSession(bookingId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.completeBooking(bookingId)
            showMessage("Parking session marked complete! Please rate your experience ⭐")
            onComplete()
        }
    }

    fun submitReview(bookingId: Long, spaceId: Long, rating: Float, comment: String) {
        viewModelScope.launch {
            repository.submitReview(
                bookingId = bookingId,
                spaceId = spaceId,
                userId = 1L,
                userName = currentUser.value?.name ?: "Rohan Sharma",
                rating = rating,
                comment = comment
            )
            showMessage("Thank you! Review submitted successfully ⭐")
        }
    }

    // Provider Space Management
    fun createNewSpace(space: ParkingSpace, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.createSpace(space)
            if (space.city.isNotBlank() && _filters.value.selectedCity != "All Cities") {
                _filters.value = _filters.value.copy(selectedCity = space.city)
            }
            showMessage("Space '${space.title}' listed and synced live to cloud! 🎉")
            onComplete()
        }
    }

    fun toggleSpaceStatus(space: ParkingSpace) {
        viewModelScope.launch {
            val newStatus = if (space.status == "Active") "Paused" else "Active"
            repository.setSpaceStatus(space.id, newStatus)
            showMessage("Listing is now $newStatus")
        }
    }

    fun withdrawEarnings(amount: Double) {
        viewModelScope.launch {
            val success = repository.withdrawEarnings(1L, amount)
            if (success) {
                showMessage("₹${amount.toInt()} transferred to your UPI account! 💸")
            } else {
                showMessage("Insufficient available balance for withdrawal.")
            }
        }
    }

    // Vehicle Management
    fun addVehicle(type: String, regNumber: String, model: String) {
        viewModelScope.launch {
            repository.addVehicle(
                Vehicle(
                    userId = 1L,
                    type = type,
                    registrationNumber = regNumber.uppercase(),
                    model = model,
                    isDefault = false
                )
            )
            showMessage("Vehicle $regNumber added.")
        }
    }

    fun deleteVehicle(id: Long) {
        viewModelScope.launch {
            repository.deleteVehicle(id)
            showMessage("Vehicle removed.")
        }
    }

    // Notifications
    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead(1L)
        }
    }

    // Profile Management
    fun updateUserProfile(name: String, phone: String, email: String) {
        viewModelScope.launch {
            repository.updateUserProfile(1L, name, phone, email)
            showMessage("Profile details updated successfully ✨")
        }
    }

    // KYC & Verification
    fun submitKycVerification() {
        viewModelScope.launch {
            repository.updateKyc(1L, idVerified = true, propVerified = true)
            showMessage("KYC verification completed! Provider verified ✓")
        }
    }

    // Admin Controls
    fun adminApproveListing(spaceId: Long) {
        viewModelScope.launch {
            repository.setSpaceVerificationStatus(spaceId, "Verified")
            repository.setSpaceStatus(spaceId, "Active")
            showMessage("Listing #$spaceId approved and verified ✓")
        }
    }

    fun adminRejectListing(spaceId: Long) {
        viewModelScope.launch {
            repository.setSpaceVerificationStatus(spaceId, "Rejected")
            repository.setSpaceStatus(spaceId, "Rejected")
            showMessage("Listing #$spaceId rejected.")
        }
    }

    fun updatePlatformCommission(commissionPercent: Double) {
        viewModelScope.launch {
            val current = platformSettings.value ?: DemoData.initialSettings
            repository.updateSettings(current.copy(commissionPercentage = commissionPercent))
            showMessage("Platform commission set to ${commissionPercent}%")
        }
    }
}
