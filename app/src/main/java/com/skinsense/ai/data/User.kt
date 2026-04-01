package com.skinsense.ai.data

enum class UserRole {
    PATIENT,
    DOCTOR,
    ADMIN
}

enum class DoctorStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    DEACTIVATED
}

data class User(
    // Common Fields
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: UserRole = UserRole.PATIENT,
    val isVerified: Boolean = false,
    val profileImageUri: String? = null,
    val isActive: Boolean = true,
    val status: String = "approved", // "pending_approval", "approved", "rejected", "deactivated"
    val bio: String? = null,
    val location: String? = null,
    val phone: String? = null,

    // Patient-specific fields
    val username: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val language: String? = null,
    val emergencyContact: String? = null,
    val bloodGroup: String? = null,
    val allergies: String? = null,
    val skinConditions: String? = null,
    val medications: String? = null,
    val medicalNotes: String? = null,

    // Doctor-specific fields
    val medicalTitle: String? = null, // Dr., MD, etc.
    val specialization: String? = null,
    val experienceYears: Int? = null,
    val qualifications: String? = null,
    val hospitalName: String? = null,
    val consultationModes: String? = null, // e.g., "Chat, In-person"
    val licenseNumber: String? = null,
    val skinConditionsTreated: String? = null,
    val proceduresOffered: String? = null,
    val areasOfInterest: String? = null,
    val clinicAddress: String? = null,
    val consultationHours: String? = null,
    val onlineAvailability: Boolean = false,
    val licenseDocumentUri: String? = null,
    val certificatesUris: List<String> = emptyList(),
    val idProofUri: String? = null,
    val verificationNotes: String? = null,
    
    // Legacy/Stats
    val rating: Float = 0.0f,
    val reviewCount: Int = 0,
    val contactDetails: String? = null,

    // Appointment/booking fields
    val consultationFee: Int = 500,
    val availableSlots: List<String> = listOf("09:00 AM", "10:00 AM", "11:00 AM", "02:00 PM", "03:00 PM", "04:30 PM", "05:00 PM", "06:30 PM"),
    val isAvailableForBooking: Boolean = true,
    val fcmToken: String? = null
)
