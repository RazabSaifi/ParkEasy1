package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Booking
import com.example.data.model.ParkingSpace
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Real-Time Firebase Cloud Firestore Synchronization Manager.
 * Syncs parking space listings and booking passes across multiple phones and the web app in real-time.
 */
class FirebaseSyncManager private constructor(
    private val db: AppDatabase,
    private val coroutineScope: CoroutineScope,
    private val context: Context? = null
) {
    private var firestore: FirebaseFirestore? = null
    private var isListening = false

    init {
        ensureFirebaseInitialized()
    }

    private fun ensureFirebaseInitialized(): FirebaseFirestore? {
        if (firestore != null) return firestore
        try {
            if (context != null && FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:437275182001:android:e1f69b3bfe57556d777390")
                    .setApiKey("AIzaSyAOfp5nHZhRg4csd5O0pGDunTCNDq49jD0")
                    .setProjectId("parkeasy-20d83")
                    .setStorageBucket("parkeasy-20d83.firebasestorage.app")
                    .setGcmSenderId("437275182001")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "FirebaseApp initialized programmatically for parkeasy-20d83 [Firebase]")
            }
            firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firebase Firestore connected successfully [Firebase]")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization notice: ${e.localizedMessage}")
        }
        return firestore
    }

    /**
     * Start listening to live cloud updates on "parking_spaces" and "bookings"
     */
    fun startRealtimeSync() {
        val fs = ensureFirebaseInitialized() ?: run {
            Log.w(TAG, "Firestore not available, skipping realtime sync listeners")
            return
        }
        if (isListening) return
        isListening = true

        try {
            // 1. Listen to real-time additions/modifications to parking spaces
            fs.collection(COLLECTION_SPACES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to parking_spaces Firestore collection: ${error.localizedMessage}", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val remoteSpaces = mutableListOf<ParkingSpace>()
                            for (doc in snapshot.documents) {
                                try {
                                    val space = docToParkingSpace(doc.id, doc.data ?: emptyMap())
                                    if (space != null) {
                                        remoteSpaces.add(space)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed parsing document ${doc.id}", e)
                                }
                            }
                            if (remoteSpaces.isNotEmpty()) {
                                db.parkingSpaceDao().insertAll(remoteSpaces)
                                Log.d(TAG, "Realtime Sync: Inserted/Updated ${remoteSpaces.size} spaces into local DB")
                            }
                        }
                    }
                }

            // 2. Listen to real-time additions/modifications to bookings
            fs.collection(COLLECTION_BOOKINGS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to bookings Firestore collection: ${error.localizedMessage}", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && !snapshot.isEmpty) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val remoteBookings = mutableListOf<Booking>()
                            for (doc in snapshot.documents) {
                                try {
                                    val booking = docToBooking(doc.id, doc.data ?: emptyMap())
                                    if (booking != null) {
                                        // Deduplicate with existing local booking if present
                                        val existing = db.bookingDao().getBookingByCode(booking.bookingCode)
                                        val resolvedBooking = if (existing != null) {
                                            booking.copy(id = existing.id)
                                        } else {
                                            booking
                                        }
                                        remoteBookings.add(resolvedBooking)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed parsing booking document ${doc.id}", e)
                                }
                            }
                            if (remoteBookings.isNotEmpty()) {
                                db.bookingDao().insertAll(remoteBookings)
                                Log.d(TAG, "Realtime Sync: Inserted/Updated ${remoteBookings.size} bookings into local DB")
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting realtime sync", e)
        }
    }

    /**
     * Publish a new or updated parking space to Cloud Firestore
     */
    suspend fun publishSpaceToCloud(space: ParkingSpace) = withContext(Dispatchers.IO) {
        val fs = ensureFirebaseInitialized() ?: return@withContext
        try {
            val spaceMap = spaceToMap(space)
            val docId = if (space.id > 0) space.id.toString() else System.currentTimeMillis().toString()
            Tasks.await(
                fs.collection(COLLECTION_SPACES).document(docId).set(spaceMap, SetOptions.merge())
            )
            Log.d(TAG, "Published space #${space.id} '${space.title}' to Firestore successfully! docId=$docId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish space to Firestore: ${e.localizedMessage}", e)
        }
    }

    /**
     * Update parking space status in Cloud Firestore
     */
    suspend fun updateSpaceStatusInCloud(spaceId: Long, status: String) = withContext(Dispatchers.IO) {
        val fs = ensureFirebaseInitialized() ?: return@withContext
        try {
            val docId = spaceId.toString()
            val updates = mapOf(
                "status" to status,
                "isOnline" to (status.equals("Active", ignoreCase = true)),
                "updatedAt" to System.currentTimeMillis()
            )
            Tasks.await(
                fs.collection(COLLECTION_SPACES).document(docId).update(updates)
            )
            Log.d(TAG, "Updated space #$spaceId status to $status in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed updating space status in Firestore: ${e.localizedMessage}", e)
        }
    }

    /**
     * Delete parking space from Cloud Firestore
     */
    suspend fun deleteSpaceFromCloud(spaceId: Long) = withContext(Dispatchers.IO) {
        val fs = ensureFirebaseInitialized() ?: return@withContext
        try {
            val docId = spaceId.toString()
            Tasks.await(
                fs.collection(COLLECTION_SPACES).document(docId).delete()
            )
            Log.d(TAG, "Deleted space #$spaceId from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete space from Firestore: ${e.localizedMessage}", e)
        }
    }

    /**
     * Publish a booking pass to Cloud Firestore
     */
    suspend fun publishBookingToCloud(booking: Booking) = withContext(Dispatchers.IO) {
        val fs = ensureFirebaseInitialized() ?: return@withContext
        try {
            val bookingMap = bookingToMap(booking)
            val docId = if (booking.bookingCode.isNotBlank()) booking.bookingCode else booking.id.toString()
            Tasks.await(
                fs.collection(COLLECTION_BOOKINGS).document(docId).set(bookingMap, SetOptions.merge())
            )
            Log.d(TAG, "Published booking ${booking.bookingCode} to Firestore! docId=$docId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish booking to Firestore: ${e.localizedMessage}", e)
        }
    }

    /**
     * Update booking status (Completed / Cancelled) in Cloud Firestore
     */
    suspend fun updateBookingStatusInCloud(bookingCode: String, status: String) = withContext(Dispatchers.IO) {
        val fs = ensureFirebaseInitialized() ?: return@withContext
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            if (status.equals("Completed", ignoreCase = true)) {
                updates["completedAt"] = System.currentTimeMillis()
            }
            Tasks.await(
                fs.collection(COLLECTION_BOOKINGS).document(bookingCode).update(updates)
            )
            Log.d(TAG, "Updated booking $bookingCode status to $status in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update booking status in Firestore: ${e.localizedMessage}", e)
        }
    }

    /**
     * Sync all local spaces to Cloud Firestore if cloud does not have them yet
     */
    suspend fun syncAllLocalSpacesToCloud() = withContext(Dispatchers.IO) {
        val fs = ensureFirebaseInitialized() ?: return@withContext
        try {
            val localSpaces = db.parkingSpaceDao().getAllSpaces().firstOrNull() ?: emptyList()
            for (space in localSpaces) {
                val docId = space.id.toString()
                val docRef = fs.collection(COLLECTION_SPACES).document(docId)
                val snapshot = Tasks.await(docRef.get())
                if (!snapshot.exists()) {
                    val map = spaceToMap(space)
                    Tasks.await(docRef.set(map))
                    Log.d(TAG, "Synced local space #${space.id} '${space.title}' to Firestore")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing local spaces to Firestore: ${e.localizedMessage}", e)
        }
    }

    private fun spaceToMap(s: ParkingSpace): Map<String, Any> {
        return mapOf(
            "id" to s.id,
            "ownerId" to s.ownerId,
            "ownerName" to s.ownerName.trim(),
            "ownerPhone" to s.ownerPhone.trim(),
            "title" to s.title.trim(),
            "description" to s.description.trim(),
            "address" to s.address.trim(),
            "area" to s.area.trim(),
            "city" to s.city.trim(),
            "state" to s.state.trim(),
            "pincode" to s.pincode.trim(),
            "latitude" to s.latitude,
            "longitude" to s.longitude,
            "parkingType" to s.parkingType.trim(),
            "vehicleCapacity" to s.vehicleCapacity,
            "supportedVehicles" to s.supportedVehicles.trim(),
            "hourlyPrice" to s.hourlyPrice,
            "dailyPrice" to s.dailyPrice,
            "monthlyPrice" to s.monthlyPrice,
            "status" to s.status.trim(),
            "isOnline" to (s.status.trim().equals("Active", ignoreCase = true)),
            "verificationStatus" to s.verificationStatus.trim(),
            "isCovered" to s.isCovered,
            "hasCctv" to s.hasCctv,
            "hasSecurityGuard" to s.hasSecurityGuard,
            "hasEvCharging" to s.hasEvCharging,
            "hasLighting" to s.hasLighting,
            "has24x7Access" to s.has24x7Access,
            "rating" to s.rating.toDouble(),
            "reviewsCount" to s.reviewsCount,
            "parkingPhoto" to s.parkingPhoto.trim(),
            "updatedAt" to System.currentTimeMillis()
        )
    }

    private fun docToParkingSpace(docId: String, data: Map<String, Any>): ParkingSpace? {
        if (data.isEmpty()) return null
        val id = (data["id"] as? Number)?.toLong()
            ?: (data["id"] as? String)?.trim()?.toLongOrNull()
            ?: docId.trim().toLongOrNull()
            ?: abs(docId.hashCode().toLong())

        val rawTitle = data["title"] as? String ?: data["name"] as? String ?: return null
        val title = rawTitle.trim()
        if (title.isEmpty()) return null

        val city = (data["city"] as? String)?.trim() ?: "Bengaluru"
        val area = (data["area"] as? String)?.trim() ?: ""
        val state = (data["state"] as? String)?.trim() ?: ""
        val address = (data["address"] as? String)?.trim() ?: ""
        val pincode = (data["pincode"] as? String)?.trim() ?: "560038"

        val hourlyPrice = (data["hourlyPrice"] as? Number)?.toDouble()
            ?: (data["hourlyPrice"] as? String)?.trim()?.toDoubleOrNull() ?: 40.0
        val dailyPrice = (data["dailyPrice"] as? Number)?.toDouble()
            ?: (data["dailyPrice"] as? String)?.trim()?.toDoubleOrNull() ?: (hourlyPrice * 6)
        val monthlyPrice = (data["monthlyPrice"] as? Number)?.toDouble()
            ?: (data["monthlyPrice"] as? String)?.trim()?.toDoubleOrNull() ?: (dailyPrice * 12)

        val vehicleCapacity = (data["vehicleCapacity"] as? Number)?.toInt()
            ?: (data["vehicleCapacity"] as? String)?.trim()?.toIntOrNull() ?: 2
        val rating = (data["rating"] as? Number)?.toFloat()
            ?: (data["rating"] as? String)?.trim()?.toFloatOrNull() ?: 4.8f
        val reviewsCount = (data["reviewsCount"] as? Number)?.toInt()
            ?: (data["reviewsCount"] as? String)?.trim()?.toIntOrNull() ?: 12

        return ParkingSpace(
            id = id,
            ownerId = (data["ownerId"] as? Number)?.toLong() ?: 1L,
            ownerName = (data["ownerName"] as? String)?.trim() ?: "Arjun Patel",
            ownerPhone = (data["ownerPhone"] as? String)?.trim() ?: "+91 98234 56789",
            title = title,
            description = (data["description"] as? String)?.trim() ?: "",
            address = address,
            area = area,
            city = city,
            state = state,
            pincode = pincode,
            latitude = (data["latitude"] as? Number)?.toDouble() ?: 12.9716,
            longitude = (data["longitude"] as? Number)?.toDouble() ?: 77.5946,
            parkingType = (data["parkingType"] as? String)?.trim() ?: "Residential",
            vehicleCapacity = vehicleCapacity,
            supportedVehicles = (data["supportedVehicles"] as? String)?.trim() ?: "Car, Bike",
            hourlyPrice = hourlyPrice,
            dailyPrice = dailyPrice,
            monthlyPrice = monthlyPrice,
            status = (data["status"] as? String)?.trim() ?: "Active",
            verificationStatus = (data["verificationStatus"] as? String)?.trim() ?: "Verified",
            isCovered = data["isCovered"] as? Boolean ?: true,
            hasCctv = data["hasCctv"] as? Boolean ?: true,
            hasSecurityGuard = data["hasSecurityGuard"] as? Boolean ?: false,
            hasEvCharging = data["hasEvCharging"] as? Boolean ?: false,
            hasLighting = data["hasLighting"] as? Boolean ?: true,
            has24x7Access = data["has24x7Access"] as? Boolean ?: true,
            rating = rating,
            reviewsCount = reviewsCount,
            parkingPhoto = (data["parkingPhoto"] as? String)?.trim() ?: ""
        )
    }

    private fun bookingToMap(b: Booking): Map<String, Any> {
        return mapOf(
            "id" to b.id,
            "bookingCode" to b.bookingCode.trim(),
            "userId" to b.userId,
            "userName" to b.userName.trim(),
            "parkingSpaceId" to b.parkingSpaceId,
            "parkingTitle" to b.parkingTitle.trim(),
            "parkingAddress" to b.parkingAddress.trim(),
            "parkingCity" to b.parkingCity.trim(),
            "vehicleType" to b.vehicleType.trim(),
            "vehicleRegNumber" to b.vehicleRegNumber.trim(),
            "bookingDate" to b.bookingDate.trim(),
            "startTime" to b.startTime.trim(),
            "endTime" to b.endTime.trim(),
            "durationHours" to b.durationHours,
            "subtotal" to b.subtotal,
            "platformFee" to b.platformFee,
            "totalAmount" to b.totalAmount,
            "status" to b.status.trim(),
            "paymentMethod" to b.paymentMethod.trim(),
            "paymentStatus" to b.paymentStatus.trim(),
            "qrData" to b.qrData.trim(),
            "providerEarnings" to b.providerEarnings,
            "createdAt" to b.createdAt
        )
    }

    private fun docToBooking(docId: String, data: Map<String, Any>): Booking? {
        if (data.isEmpty()) return null
        val id = (data["id"] as? Number)?.toLong()
            ?: (data["id"] as? String)?.trim()?.toLongOrNull()
            ?: docId.trim().toLongOrNull()
            ?: abs(docId.hashCode().toLong())

        val bookingCode = (data["bookingCode"] as? String)?.trim() ?: docId.trim()
        val spaceId = (data["parkingSpaceId"] as? Number)?.toLong()
            ?: (data["parkingSpaceId"] as? String)?.trim()?.toLongOrNull()
            ?: 1L

        val durationHours = (data["durationHours"] as? Number)?.toInt()
            ?: (data["durationHours"] as? String)?.trim()?.toIntOrNull() ?: 2
        val totalAmount = (data["totalAmount"] as? Number)?.toDouble()
            ?: (data["totalAmount"] as? String)?.trim()?.toDoubleOrNull() ?: 90.0
        val subtotal = (data["subtotal"] as? Number)?.toDouble()
            ?: (data["subtotal"] as? String)?.trim()?.toDoubleOrNull() ?: (totalAmount - 10.0)
        val platformFee = (data["platformFee"] as? Number)?.toDouble()
            ?: (data["platformFee"] as? String)?.trim()?.toDoubleOrNull() ?: 10.0

        return Booking(
            id = id,
            bookingCode = bookingCode,
            userId = (data["userId"] as? Number)?.toLong() ?: 1L,
            userName = (data["userName"] as? String)?.trim() ?: "Commuter",
            parkingSpaceId = spaceId,
            parkingTitle = (data["parkingTitle"] as? String)?.trim() ?: "Parking Slot",
            parkingAddress = (data["parkingAddress"] as? String)?.trim() ?: "",
            parkingCity = (data["parkingCity"] as? String)?.trim() ?: "Bengaluru",
            vehicleType = (data["vehicleType"] as? String)?.trim() ?: "Car",
            vehicleRegNumber = (data["vehicleRegNumber"] as? String)?.trim() ?: "KA-01-AB-1234",
            bookingDate = (data["bookingDate"] as? String)?.trim() ?: "Today",
            startTime = (data["startTime"] as? String)?.trim() ?: "10:00 AM",
            endTime = (data["endTime"] as? String)?.trim() ?: "12:00 PM",
            durationHours = durationHours,
            subtotal = subtotal,
            platformFee = platformFee,
            totalAmount = totalAmount,
            status = (data["status"] as? String)?.trim() ?: "Confirmed",
            paymentMethod = (data["paymentMethod"] as? String)?.trim() ?: "UPI",
            paymentStatus = (data["paymentStatus"] as? String)?.trim() ?: "Paid",
            qrData = (data["qrData"] as? String)?.trim() ?: "PARKSPACE:$bookingCode",
            providerEarnings = (data["providerEarnings"] as? Number)?.toDouble() ?: (totalAmount - platformFee),
            isReviewed = data["isReviewed"] as? Boolean ?: false,
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    companion object {
        private const val TAG = "FirebaseSyncManager"
        private const val COLLECTION_SPACES = "parking_spaces"
        private const val COLLECTION_BOOKINGS = "bookings"

        @Volatile
        private var instance: FirebaseSyncManager? = null

        fun getInstance(db: AppDatabase, coroutineScope: CoroutineScope, context: Context? = null): FirebaseSyncManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseSyncManager(db, coroutineScope, context).also { instance = it }
            }
        }
    }
}