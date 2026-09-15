package com.example.data.repository

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import com.example.data.remote.FirebaseSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

class ParkSpaceRepository(
    private val database: AppDatabase,
    private val coroutineScope: CoroutineScope = GlobalScope
) {

    private val userDao = database.userDao()
    private val spaceDao = database.parkingSpaceDao()
    private val bookingDao = database.bookingDao()
    private val vehicleDao = database.vehicleDao()
    private val reviewDao = database.reviewDao()
    private val notificationDao = database.notificationDao()
    private val blockedSlotDao = database.blockedSlotDao()
    private val settingsDao = database.platformSettingsDao()
    private val firebaseSyncManager = FirebaseSyncManager.getInstance(database, coroutineScope)

    suspend fun ensureDemoDataInitialized() = withContext(Dispatchers.IO) {
        val count = spaceDao.countSpaces()
        if (count == 0) {
            userDao.insertOrUpdate(DemoData.initialUser)
            settingsDao.saveSettings(DemoData.initialSettings)
            vehicleDao.insertAll(DemoData.initialVehicles)
            spaceDao.insertAll(DemoData.initialSpaces)
            bookingDao.insertAll(DemoData.initialBookings)
            reviewDao.insertAll(DemoData.initialReviews)
            notificationDao.insertAll(DemoData.initialNotifications)
        }
        // Start real-time cloud sync with Firebase Cloud Firestore
        firebaseSyncManager.startRealtimeSync()
    }

    // --- Users ---
    fun getUser(userId: Long = 1L): Flow<User?> = userDao.getUser(userId)
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun setActiveMode(userId: Long = 1L, mode: String) = withContext(Dispatchers.IO) {
        userDao.setActiveMode(userId, mode)
    }

    suspend fun updateUserProfile(userId: Long = 1L, name: String, phone: String, email: String) = withContext(Dispatchers.IO) {
        userDao.updateProfile(userId, name, phone, email)
        notificationDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Profile Updated ✨",
                message = "Your profile details (Name, Phone, Email) have been updated successfully.",
                type = "Security"
            )
        )
    }

    suspend fun updateKyc(userId: Long = 1L, idVerified: Boolean, propVerified: Boolean) = withContext(Dispatchers.IO) {
        userDao.updateKyc(userId, idVerified, propVerified)
        notificationDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "KYC Status Updated",
                message = if (idVerified && propVerified) "Your Identity and Property documents are verified! You can now receive verified badge." else "KYC verification status changed.",
                type = "Security"
            )
        )
    }

    suspend fun withdrawEarnings(userId: Long = 1L, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getUser(userId).firstOrNull() ?: return@withContext false
        if (user.availableBalance >= amount && amount > 0) {
            userDao.withdrawBalance(userId, amount)
            notificationDao.insertNotification(
                NotificationItem(
                    userId = userId,
                    title = "Withdrawal Initiated 💸",
                    message = "Payout of ₹${amount.toInt()} has been transferred to your linked UPI ID. Ref: WT-${System.currentTimeMillis() % 100000}",
                    type = "Earnings"
                )
            )
            true
        } else {
            false
        }
    }

    // --- Parking Spaces ---
    fun getAllSpaces(): Flow<List<ParkingSpace>> = spaceDao.getAllSpaces()
    fun getSpacesByOwner(ownerId: Long = 1L): Flow<List<ParkingSpace>> = spaceDao.getSpacesByOwner(ownerId)
    fun getSpaceById(id: Long): Flow<ParkingSpace?> = spaceDao.getSpaceById(id)

    suspend fun createSpace(space: ParkingSpace): Long = withContext(Dispatchers.IO) {
        val id = spaceDao.insertSpace(space)
        val createdSpace = space.copy(id = id)
        firebaseSyncManager.publishSpaceToCloud(createdSpace)
        notificationDao.insertNotification(
            NotificationItem(
                userId = space.ownerId,
                title = "Listing Published 🚗",
                message = "'${space.title}' has been submitted and is currently pending verification.",
                type = "Listing"
            )
        )
        id
    }

    suspend fun updateSpace(space: ParkingSpace) = withContext(Dispatchers.IO) {
        spaceDao.updateSpace(space)
    }

    suspend fun setSpaceStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        spaceDao.updateStatus(id, status)
    }

    suspend fun setSpaceVerificationStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        spaceDao.updateVerificationStatus(id, status)
    }

    suspend fun deleteSpace(space: ParkingSpace) = withContext(Dispatchers.IO) {
        spaceDao.deleteSpace(space)
    }

    // --- Bookings ---
    fun getAllBookings(): Flow<List<Booking>> = bookingDao.getAllBookings()
    fun getUserBookings(userId: Long = 1L): Flow<List<Booking>> = bookingDao.getBookingsByUser(userId)
    fun getProviderBookings(ownerId: Long = 1L): Flow<List<Booking>> = bookingDao.getBookingsForProvider(ownerId)
    fun getBookingById(id: Long): Flow<Booking?> = bookingDao.getBookingById(id)

    suspend fun createBooking(
        userId: Long = 1L,
        userName: String,
        space: ParkingSpace,
        vehicleType: String,
        vehicleRegNumber: String,
        bookingDate: String,
        startTime: String,
        endTime: String,
        durationHours: Int,
        subtotal: Double,
        platformFee: Double,
        totalAmount: Double,
        paymentMethod: String
    ): Booking = withContext(Dispatchers.IO) {
        val randomSuffix = (10000..99999).random()
        val bookingCode = "PS-$randomSuffix"
        val qrData = "PARKSPACE:BK-$bookingCode:SLOT-${space.id}:$bookingDate:$startTime"
        val providerEarnings = totalAmount - platformFee

        val booking = Booking(
            bookingCode = bookingCode,
            userId = userId,
            userName = userName,
            parkingSpaceId = space.id,
            parkingTitle = space.title,
            parkingAddress = space.address,
            parkingCity = space.city,
            vehicleType = vehicleType,
            vehicleRegNumber = vehicleRegNumber,
            bookingDate = bookingDate,
            startTime = startTime,
            endTime = endTime,
            durationHours = durationHours,
            subtotal = subtotal,
            platformFee = platformFee,
            totalAmount = totalAmount,
            status = "Confirmed",
            paymentMethod = paymentMethod,
            paymentStatus = "Paid",
            qrData = qrData,
            providerEarnings = providerEarnings,
            isReviewed = false
        )

        val id = bookingDao.insertBooking(booking)

        // Credit provider earnings
        userDao.addEarnings(space.ownerId, providerEarnings)

        // Seeker confirmation notification
        notificationDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Booking Confirmed 🎉",
                message = "Slot at '${space.title}' confirmed for $bookingDate ($startTime - $endTime). Pass: $bookingCode.",
                type = "Booking"
            )
        )

        // Provider notification
        notificationDao.insertNotification(
            NotificationItem(
                userId = space.ownerId,
                title = "New Booking Received 🚘",
                message = "$userName booked slot for $durationHours hrs. Earnings: ₹${providerEarnings.toInt()} credited to balance.",
                type = "Earnings"
            )
        )

        val savedBooking = booking.copy(id = id)
        firebaseSyncManager.publishBookingToCloud(savedBooking)
        savedBooking
    }

    suspend fun completeBooking(bookingId: Long) = withContext(Dispatchers.IO) {
        bookingDao.updateBookingStatus(bookingId, "Completed")
    }

    suspend fun cancelBooking(bookingId: Long, userId: Long = 1L) = withContext(Dispatchers.IO) {
        bookingDao.updateBookingStatus(bookingId, "Cancelled")
        notificationDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Booking Cancelled ℹ️",
                message = "Your booking #$bookingId has been cancelled. Refund will be credited within 24 hours.",
                type = "Booking"
            )
        )
    }

    // --- Vehicles ---
    fun getUserVehicles(userId: Long = 1L): Flow<List<Vehicle>> = vehicleDao.getVehiclesByUser(userId)

    suspend fun addVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        vehicleDao.insertVehicle(vehicle)
    }

    suspend fun deleteVehicle(id: Long) = withContext(Dispatchers.IO) {
        vehicleDao.deleteVehicle(id)
    }

    // --- Reviews ---
    fun getSpaceReviews(spaceId: Long): Flow<List<Review>> = reviewDao.getReviewsForSpace(spaceId)
    fun getAllReviews(): Flow<List<Review>> = reviewDao.getAllReviews()

    suspend fun submitReview(
        bookingId: Long,
        spaceId: Long,
        userId: Long = 1L,
        userName: String,
        rating: Float,
        comment: String
    ) = withContext(Dispatchers.IO) {
        val review = Review(
            bookingId = bookingId,
            userId = userId,
            userName = userName,
            parkingSpaceId = spaceId,
            rating = rating,
            comment = comment
        )
        reviewDao.insertReview(review)
        if (bookingId > 0) {
            bookingDao.markReviewed(bookingId)
            bookingDao.updateBookingStatus(bookingId, "Completed")
        }

        // Update space rating average
        val space = spaceDao.getSpaceById(spaceId).firstOrNull()
        if (space != null) {
            val newCount = space.reviewsCount + 1
            val newRating = ((space.rating * space.reviewsCount) + rating) / newCount
            val roundedRating = Math.round(newRating * 10f) / 10f
            spaceDao.updateSpace(space.copy(rating = roundedRating, reviewsCount = newCount))
        }

        notificationDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Review Submitted ⭐",
                message = "Thank you for sharing your feedback with the community!",
                type = "Review"
            )
        )
    }

    // --- Notifications ---
    fun getUserNotifications(userId: Long = 1L): Flow<List<NotificationItem>> = notificationDao.getNotificationsByUser(userId)

    suspend fun markNotificationRead(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsRead(userId: Long = 1L) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(userId)
    }

    // --- Blocked Slots ---
    fun getBlockedSlots(spaceId: Long): Flow<List<BlockedSlot>> = blockedSlotDao.getBlockedSlots(spaceId)

    suspend fun addBlockedSlot(slot: BlockedSlot) = withContext(Dispatchers.IO) {
        blockedSlotDao.insertBlockedSlot(slot)
    }

    suspend fun removeBlockedSlot(id: Long) = withContext(Dispatchers.IO) {
        blockedSlotDao.deleteBlockedSlot(id)
    }

    // --- Platform Settings ---
    fun getSettings(): Flow<PlatformSettings?> = settingsDao.getSettings()

    suspend fun updateSettings(settings: PlatformSettings) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(settings)
    }
}
