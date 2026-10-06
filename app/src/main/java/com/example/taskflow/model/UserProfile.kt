package com.example.taskflow.model

data class UserProfile(
    val uid: String = "",
    val displayName: String = "Productivity Pro",
    val email: String? = null,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val totalXp: Int = 0,
    val streakDays: Int = 0,
    val lastCompletionDate: String = "",
    val themeMode: String = "SYSTEM",
    val authProvider: String = "anonymous"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "photoUrl" to photoUrl,
            "createdAt" to createdAt,
            "totalXp" to totalXp,
            "streakDays" to streakDays,
            "lastCompletionDate" to lastCompletionDate,
            "themeMode" to themeMode,
            "authProvider" to authProvider
        )
    }

    companion object {
        fun fromMap(data: Map<String, Any?>): UserProfile {
            return UserProfile(
                uid = data["uid"] as? String ?: "",
                displayName = data["displayName"] as? String ?: "Productivity Pro",
                email = data["email"] as? String,
                phoneNumber = data["phoneNumber"] as? String,
                photoUrl = data["photoUrl"] as? String,
                createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                totalXp = (data["totalXp"] as? Number)?.toInt() ?: 0,
                streakDays = (data["streakDays"] as? Number)?.toInt() ?: 0,
                lastCompletionDate = data["lastCompletionDate"] as? String ?: "",
                themeMode = data["themeMode"] as? String ?: "SYSTEM",
                authProvider = data["authProvider"] as? String ?: "unknown"
            )
        }
    }
}
