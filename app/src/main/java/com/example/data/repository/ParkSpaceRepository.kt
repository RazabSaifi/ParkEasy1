package com.example.data.repository

import android.content.Context
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
    private val context: Context? = null,
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
    private val firebaseSyncManager = FirebaseSyncManager.getInstance(database, coroutineScope, context)

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
        // Ensure any local spaces are synced to cloud if missing
        firebaseSyncManager.syncAllLocalSpacesToCloud()
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
                    message = "Payout of â‚¹${amount.toInt()} has been transferred to your linked UPI ID. Ref: WT-${System.currentTimeMillis() % 100000}",
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
                message = "'${space.title}' has been submitted and is live on the cloud.",
                type = "Listing"
            )
        )
        id
    }

    suspend fun updateSpace(space: ParkingSpace) = withContext(Dispatchers.IO) {
        spaceDao.updateSpace(space)
        firebaseSyncManager.publishSpaceToCloud(space)
    }

    suspend fun setSpaceStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        spaceDao.updateStatus(id, status)
        firebaseSyncManager.updateSpaceStatusInCloud(id, status)
    }

    suspend fun setSpaceVerificationStatus(id: Long, status: String) = withContext(Dispatchers.IO) {
        spaceDao.updateVerificationStatus(id, status)
        val space = spaceDao.getSpaceById(id).firstOrNull()
        if (space != null) {
            firebaseSyncManager.publishSpaceToCloud(space.copy(verificationStatus = status))
        }
    }

    suspend fun deleteSpace(space: ParkingSpace) = withContext(Dispatchers.IO) {
        spaceDao.deleteSpace(space)
        firebaseSyncManager.deleteSpaceFromCloud(space.id)
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
                message = "$userName booked slot for $durationHours hrs. Earnings: â‚¹${providerEarnings.toInt()} credited to balance.",
                type = "Earnings"
            )
        )

        val savedBooking = booking.copy(id = id)
        firebaseSyncManager.publishBookingToCloud(savedBooking)
        savedBooking
    }

    suspend fun completeBooking(bookingId: Long) = withContext(Dispatchers.IO) {
        bookingDao.updateBookingStatus(bookingId, "Completed")
        val booking = bookingDao.getBookingById(bookingId).firstOrNull()
        if (booking != null) {
            firebaseSyncManager.updateBookingStatusInCloud(booking.bookingCode, "Completed")
        }
    }

    suspend fun cancelBooking(bookingId: Long, userId: Long = 1L) = withContext(Dispatchers.IO) {
        bookingDao.updateBookingStatus(bookingId, "Cancelled")
        val booking = bookingDao.getBookingById(bookingId).firstOrNull()
        if (booking != null) {
            firebaseSyncManager.updateBookingStatusInCloud(booking.bookingCode, "Cancelled")
        }
        notificationDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Booking Cancelled â„¹ï¸",
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
    fun getReviewsForSpace(spaceId: Long): Flow<List<Review>> = reviewDao.getReviewsForSpace(spaceId)

    suspend fun addReview(
        spaceId: Long,
        userId: Long = 1L,
        userName: String,
        rating: Float,
        comment: String
    ) = withContext(Dispatchers.IO) {
        val review = Review(
            parkingSpaceId = spaceId,
            userId = userId,
            userName = userName,
            rating = rating,
            comment = comment
        )
        reviewDao.insertReview(review)

        // Recalculate average rating
        val allReviews = reviewDao.getReviewsForSpace(spaceId).firstOrNull() ?: listOf(review)
        val newAvgRating = allReviews.map { it.rating }.average().toFloat()
        spaceDao.updateRating(spaceId, newAvgRating, allReviews.size)

        // Publish updated rating to cloud
        val space = spaceDao.getSpaceById(spaceId).firstOrNull()
        if (space != null) {
            firebaseSyncManager.publishSpaceToCloud(space.copy(rating = newAvgRating, reviewsCount = allReviews.size))
        }

        notificationDao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Review Submitted 🌟",
                message = "Thank you for rating your parking experience with $rating stars!",
                type = "Review"
            )
        )
    }

    // --- Notifications ---
    fun getNotifications(userId: Long = 1L): Flow<List<NotificationItem>> = notificationDao.getNotificationsForUser(userId)
    fun getUnreadNotificationsCount(userId: Long = 1L): Flow<Int> = notificationDao.getUnreadCount(userId)

    suspend fun markNotificationAsRead(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead(userId: Long = 1L) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(userId)
    }

    // --- Blocked Slots ---
    fun getBlockedSlots(spaceId: Long): Flow<List<BlockedSlot>> = blockedSlotDao.getBlockedSlotsForSpace(spaceId)

    suspend fun blockSlot(spaceId: Long, date: String, slotNumber: Int, reason: String) = withContext(Dispatchers.IO) {
        blockedSlotDao.insertBlockedSlot(
            BlockedSlot(
                parkingSpaceId = spaceId,
                date = date,
                slotNumber = slotNumber,
                reason = reason
            )
        )
    }

    suspend fun unblockSlot(id: Long) = withContext(Dispatchers.IO) {
        blockedSlotDao.deleteBlockedSlot(id)
    }

    // --- Platform Settings ---
    fun getPlatformSettings(): Flow<PlatformSettings?> = settingsDao.getSettings()

    suspend fun savePlatformSettings(settings: PlatformSettings) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(settings)
    }
}