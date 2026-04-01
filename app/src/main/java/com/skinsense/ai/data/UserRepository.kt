package com.skinsense.ai.data

import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

interface UserRepository {
    suspend fun createUserProfile(user: User): Result<Boolean>
    suspend fun getUserProfile(uid: String): Result<User?>
    suspend fun updateUserProfile(user: User): Result<Boolean>
    suspend fun getDoctors(): Result<List<User>>           // Verified/approved doctors for patients
    suspend fun deleteUser(uid: String): Result<Boolean>
    suspend fun getUnverifiedDoctors(): Result<List<User>> // Admin: pending doctors
    suspend fun verifyDoctor(uid: String): Result<Boolean> // Admin: approve
    suspend fun getAllDoctors(): Result<List<User>>         // Admin: all doctors
    suspend fun getAllPatients(): Result<List<User>>        // Admin: all patients
    suspend fun setDoctorStatus(uid: String, status: String): Result<Boolean>
    suspend fun setUserActive(uid: String, collection: String, isActive: Boolean): Result<Boolean>
    
    // Connection Flow
    suspend fun sendConnectionRequest(patientId: String, doctorId: String, patientName: String): Result<Boolean>
    suspend fun getPendingConnectionRequests(doctorId: String): Result<List<ConnectionRequest>>
    suspend fun respondToConnectionRequest(requestId: String, status: ConnectionStatus): Result<Boolean>
    suspend fun getConnectedPatients(doctorId: String): Result<List<User>>
    suspend fun getConnectionStatus(patientId: String, doctorId: String): ConnectionStatus?
    suspend fun updateFcmToken(uid: String, role: UserRole, token: String): Result<Boolean>
}

class FirestoreUserRepository : UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val patientsCollection = firestore.collection("patients")
    private val doctorsCollection = firestore.collection("doctors")
    private val adminsCollection = firestore.collection("admins")

    override suspend fun createUserProfile(user: User): Result<Boolean> {
        return try {
            Log.d("SkinSense", "createUserProfile called for ${user.uid} role=${user.role}")
            val updatedUser = when {
                user.email.equals("admin@skinsense.ai", ignoreCase = true) ->
                    user.copy(role = UserRole.ADMIN, isVerified = true, status = "approved", isActive = true)
                user.role == UserRole.ADMIN ->
                    user.copy(isVerified = true, status = "approved", isActive = true)
                user.role == UserRole.PATIENT ->
                    user.copy(isVerified = true, status = "approved", isActive = true)
                user.role == UserRole.DOCTOR ->
                    user.copy(isVerified = false, status = "pending_approval", isActive = true)
                else -> user
            }

            val collection = when (updatedUser.role) {
                UserRole.DOCTOR -> doctorsCollection
                UserRole.ADMIN -> adminsCollection
                else -> patientsCollection
            }

            // Build explicit map so role is stored as a string, not an enum ordinal
            val dataMap = mutableMapOf(
                "uid" to updatedUser.uid,
                "email" to updatedUser.email,
                "displayName" to updatedUser.displayName,
                "role" to updatedUser.role.name,
                "isVerified" to updatedUser.isVerified,
                "isActive" to updatedUser.isActive,
                "status" to updatedUser.status,
                "phone" to updatedUser.phone,
                "dateOfBirth" to updatedUser.dateOfBirth,
                "gender" to updatedUser.gender,
                "specialization" to updatedUser.specialization,
                "experienceYears" to updatedUser.experienceYears,
                "hospitalName" to updatedUser.hospitalName,
                "contactDetails" to updatedUser.contactDetails,
                "licenseNumber" to updatedUser.licenseNumber,
                "rating" to updatedUser.rating,
                "reviewCount" to updatedUser.reviewCount,
                "bio" to updatedUser.bio,
                "profileImageUri" to updatedUser.profileImageUri,
                "location" to updatedUser.location,
                "username" to updatedUser.username,
                "language" to updatedUser.language,
                "emergencyContact" to updatedUser.emergencyContact,
                "bloodGroup" to updatedUser.bloodGroup,
                "allergies" to updatedUser.allergies,
                "skinConditions" to updatedUser.skinConditions,
                "medications" to updatedUser.medications,
                "medicalNotes" to updatedUser.medicalNotes,
                "medicalTitle" to updatedUser.medicalTitle,
                "qualifications" to updatedUser.qualifications,
                "consultationModes" to updatedUser.consultationModes,
                "skinConditionsTreated" to updatedUser.skinConditionsTreated,
                "proceduresOffered" to updatedUser.proceduresOffered,
                "areasOfInterest" to updatedUser.areasOfInterest,
                "clinicAddress" to updatedUser.clinicAddress,
                "consultationHours" to updatedUser.consultationHours,
                "onlineAvailability" to updatedUser.onlineAvailability,
                "licenseDocumentUri" to updatedUser.licenseDocumentUri,
                "certificatesUris" to updatedUser.certificatesUris,
                "idProofUri" to updatedUser.idProofUri,
                "verificationNotes" to updatedUser.verificationNotes
            )

            withTimeout(10000) {
                collection.document(updatedUser.uid).set(dataMap).await()
            }
            Log.d("SkinSense", "createUserProfile Success for ${updatedUser.uid}")
            Result.success(true)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.e("SkinSense", "createUserProfile Timeout for ${user.uid}")
            Result.failure(Exception("Profile creation timed out. Please check your connection."))
        } catch (e: Exception) {
            Log.e("SkinSense", "createUserProfile Error for ${user.uid}: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getUserProfile(uid: String): Result<User?> {
        Log.d("SkinSense", "getUserProfile called for UID: $uid")
        return try {
            // Check patients collection first
            var snapshot = withTimeout(8000) { patientsCollection.document(uid).get().await() }
            if (snapshot.exists()) {
                return Result.success(snapshotToUser(snapshot))
            }
            // Check doctors collection
            snapshot = withTimeout(8000) { doctorsCollection.document(uid).get().await() }
            if (snapshot.exists()) {
                return Result.success(snapshotToUser(snapshot))
            }
            // Check admins collection
            snapshot = withTimeout(8000) { adminsCollection.document(uid).get().await() }
            if (snapshot.exists()) {
                return Result.success(snapshotToUser(snapshot))
            }
            Log.d("SkinSense", "No profile found for $uid")
            Result.success(null)
        } catch (e: Exception) {
            Log.e("SkinSense", "getUserProfile error for $uid", e)
            Result.failure(e)
        }
    }

    override suspend fun updateFcmToken(uid: String, role: UserRole, token: String): Result<Boolean> {
        return try {
            val collection = when (role) {
                UserRole.DOCTOR -> doctorsCollection
                UserRole.ADMIN -> adminsCollection
                else -> patientsCollection
            }
            collection.document(uid).update("fcmToken", token).await()
            Log.d("SkinSense", "updateFcmToken Success for $uid")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("SkinSense", "updateFcmToken Error for $uid: ${e.message}")
            Result.failure(e)
        }
    }

    /** Safely converts a Firestore document snapshot to a User */
    private fun snapshotToUser(snapshot: com.google.firebase.firestore.DocumentSnapshot): User? {
        return try {
            val roleStr = snapshot.getString("role") ?: "PATIENT"
            val role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.PATIENT }
            User(
                uid = snapshot.getString("uid") ?: "",
                email = snapshot.getString("email") ?: "",
                displayName = snapshot.getString("displayName") ?: "",
                role = role,
                isVerified = snapshot.getBoolean("isVerified") ?: false,
                isActive = snapshot.getBoolean("isActive") ?: true,
                status = snapshot.getString("status") ?: "approved",
                phone = snapshot.getString("phone"),
                dateOfBirth = snapshot.getString("dateOfBirth"),
                gender = snapshot.getString("gender"),
                specialization = snapshot.getString("specialization"),
                experienceYears = snapshot.getLong("experienceYears")?.toInt(),
                hospitalName = snapshot.getString("hospitalName"),
                contactDetails = snapshot.getString("contactDetails"),
                licenseNumber = snapshot.getString("licenseNumber"),
                rating = snapshot.getDouble("rating")?.toFloat() ?: 0f,
                reviewCount = snapshot.getLong("reviewCount")?.toInt() ?: 0,
                bio = snapshot.getString("bio"),
                profileImageUri = snapshot.getString("profileImageUri"),
                location = snapshot.getString("location"),
                username = snapshot.getString("username"),
                language = snapshot.getString("language"),
                emergencyContact = snapshot.getString("emergencyContact"),
                bloodGroup = snapshot.getString("bloodGroup"),
                allergies = snapshot.getString("allergies"),
                skinConditions = snapshot.getString("skinConditions"),
                medications = snapshot.getString("medications"),
                medicalNotes = snapshot.getString("medicalNotes"),
                medicalTitle = snapshot.getString("medicalTitle"),
                qualifications = snapshot.getString("qualifications"),
                consultationModes = snapshot.getString("consultationModes"),
                skinConditionsTreated = snapshot.getString("skinConditionsTreated"),
                proceduresOffered = snapshot.getString("proceduresOffered"),
                areasOfInterest = snapshot.getString("areasOfInterest"),
                clinicAddress = snapshot.getString("clinicAddress"),
                consultationHours = snapshot.getString("consultationHours"),
                onlineAvailability = snapshot.getBoolean("onlineAvailability") ?: false,
                licenseDocumentUri = snapshot.getString("licenseDocumentUri"),
                certificatesUris = (snapshot.get("certificatesUris") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                idProofUri = snapshot.getString("idProofUri"),
                verificationNotes = snapshot.getString("verificationNotes"),
                fcmToken = snapshot.getString("fcmToken")
            )
        } catch (e: Exception) {
            Log.e("SkinSense", "snapshotToUser failed: ${e.message}")
            null
        }
    }

    override suspend fun updateUserProfile(user: User): Result<Boolean> {
        return try {
            val collection = when (user.role) {
                UserRole.DOCTOR -> doctorsCollection
                UserRole.ADMIN -> adminsCollection
                else -> patientsCollection
            }
            
            // Build manual map to ensure proper serialization (especially enums)
            val dataMap = mutableMapOf(
                "uid" to user.uid,
                "email" to user.email,
                "displayName" to user.displayName,
                "role" to user.role.name,
                "isVerified" to user.isVerified,
                "isActive" to user.isActive,
                "status" to user.status,
                "phone" to user.phone,
                "dateOfBirth" to user.dateOfBirth,
                "gender" to user.gender,
                "specialization" to user.specialization,
                "experienceYears" to user.experienceYears,
                "hospitalName" to user.hospitalName,
                "contactDetails" to user.contactDetails,
                "licenseNumber" to user.licenseNumber,
                "rating" to user.rating,
                "reviewCount" to user.reviewCount,
                "bio" to user.bio,
                "profileImageUri" to user.profileImageUri,
                "location" to user.location,
                "username" to user.username,
                "language" to user.language,
                "emergencyContact" to user.emergencyContact,
                "bloodGroup" to user.bloodGroup,
                "allergies" to user.allergies,
                "skinConditions" to user.skinConditions,
                "medications" to user.medications,
                "medicalNotes" to user.medicalNotes,
                "medicalTitle" to user.medicalTitle,
                "qualifications" to user.qualifications,
                "consultationModes" to user.consultationModes,
                "skinConditionsTreated" to user.skinConditionsTreated,
                "proceduresOffered" to user.proceduresOffered,
                "areasOfInterest" to user.areasOfInterest,
                "clinicAddress" to user.clinicAddress,
                "consultationHours" to user.consultationHours,
                "onlineAvailability" to user.onlineAvailability,
                "licenseDocumentUri" to user.licenseDocumentUri,
                "certificatesUris" to user.certificatesUris,
                "idProofUri" to user.idProofUri,
                "verificationNotes" to user.verificationNotes
            )

            collection.document(user.uid).set(dataMap).await()
            Log.d("SkinSense", "updateUserProfile: Success for ${user.uid}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e("SkinSense", "updateUserProfile: Failed for ${user.uid}", e)
            Result.failure(e)
        }
    }

    /** Returns only approved+active doctors (for patient-facing "Find Doctor") */
    override suspend fun getDoctors(): Result<List<User>> {
        return try {
            Log.d("SkinSense", "Fetching approved doctors...")
            val snapshot = withTimeout(10000) {
                doctorsCollection
                    .whereEqualTo("status", "approved")
                    .whereEqualTo("isActive", true)
                    .get().await()
            }
            val doctors = snapshot.documents.mapNotNull { snapshotToUser(it) }
            Log.d("SkinSense", "Found ${doctors.size} approved doctors.")
            Result.success(doctors)
        } catch (e: Exception) {
            Log.e("SkinSense", "getDoctors failed: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(uid: String): Result<Boolean> {
        return try {
            patientsCollection.document(uid).delete().await()
            doctorsCollection.document(uid).delete().await()
            adminsCollection.document(uid).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin: get pending doctors */
    override suspend fun getUnverifiedDoctors(): Result<List<User>> {
        return try {
            val snapshot = withTimeout(10000) {
                doctorsCollection.whereEqualTo("status", "pending_approval").get().await()
            }
            val doctors = snapshot.documents.mapNotNull { snapshotToUser(it) }
            Result.success(doctors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin: approve a doctor */
    override suspend fun verifyDoctor(uid: String): Result<Boolean> {
        return setDoctorStatus(uid, "approved")
    }

    /** Admin: all doctors (any status) */
    override suspend fun getAllDoctors(): Result<List<User>> {
        return try {
            val snapshot = withTimeout(10000) { doctorsCollection.get().await() }
            val doctors = snapshot.documents.mapNotNull { snapshotToUser(it) }
            Result.success(doctors)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Admin: all patients */
    override suspend fun getAllPatients(): Result<List<User>> {
        return try {
            val snapshot = withTimeout(10000) { patientsCollection.get().await() }
            val patients = snapshot.documents.mapNotNull { snapshotToUser(it) }
            Result.success(patients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setDoctorStatus(uid: String, status: String): Result<Boolean> {
        return try {
            val isVerified = status == "approved"
            doctorsCollection.document(uid).update(
                mapOf("status" to status, "isVerified" to isVerified)
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setUserActive(uid: String, collection: String, isActive: Boolean): Result<Boolean> {
        return try {
            val col = when (collection) {
                "doctors" -> doctorsCollection
                "admins" -> adminsCollection
                else -> patientsCollection
            }
            col.document(uid).update("isActive", isActive).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Connection Flow Implementation
    private val connectionsCollection = firestore.collection("connections")

    override suspend fun sendConnectionRequest(patientId: String, doctorId: String, patientName: String): Result<Boolean> {
        return try {
            val id = connectionsCollection.document().id
            val request = ConnectionRequest(
                id = id,
                patientId = patientId,
                patientName = patientName,
                doctorId = doctorId,
                status = ConnectionStatus.PENDING,
                timestamp = System.currentTimeMillis()
            )
            connectionsCollection.document(id).set(request).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingConnectionRequests(doctorId: String): Result<List<ConnectionRequest>> {
        return try {
            val snapshot = connectionsCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            
            val requests = snapshot.documents.mapNotNull { doc ->
                try {
                    val statusStr = doc.getString("status") ?: "PENDING"
                    val status = ConnectionStatus.valueOf(statusStr)
                    ConnectionRequest(
                        id = doc.getString("id") ?: "",
                        patientId = doc.getString("patientId") ?: "",
                        patientName = doc.getString("patientName") ?: "",
                        doctorId = doc.getString("doctorId") ?: "",
                        status = status,
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                } catch (e: Exception) { 
                    null 
                }
            }
            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun respondToConnectionRequest(requestId: String, status: ConnectionStatus): Result<Boolean> {
        return try {
            connectionsCollection.document(requestId).update("status", status.name).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConnectedPatients(doctorId: String): Result<List<User>> {
        return try {
            // 1. Get all accepted connection requests for this doctor
            val snapshot = connectionsCollection
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
            
            val patientIds = snapshot.documents.mapNotNull { it.getString("patientId") }.distinct()
            
            if (patientIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // 2. Fetch User profiles for these patients
            val patients = mutableListOf<User>()
            for (pid in patientIds) {
                val pDoc = patientsCollection.document(pid).get().await()
                snapshotToUser(pDoc)?.let { patients.add(it) }
            }
            
            Result.success(patients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getConnectionStatus(patientId: String, doctorId: String): ConnectionStatus? {
        return try {
            val snapshot = connectionsCollection
                .whereEqualTo("patientId", patientId)
                .whereEqualTo("doctorId", doctorId)
                .get()
                .await()
            
            if (snapshot.isEmpty) return null
            
            // Get the most recent or any if multiple exist (though ideally only one)
            val doc = snapshot.documents.first()
            val statusStr = doc.getString("status") ?: "PENDING"
            ConnectionStatus.valueOf(statusStr)
        } catch (e: Exception) {
            Log.e("SkinSense", "getConnectionStatus failed: ${e.message}")
            null
        }
    }
}
