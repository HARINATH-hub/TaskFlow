package com.example.taskflow.data.remote

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: FirebaseUser) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    val authStateFlow: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                trySend(AuthState.Authenticated(user))
            } else {
                trySend(AuthState.Unauthenticated)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Signs in with a Google ID Token obtained from Google Play Services Sign-In
     */
    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> {
        AuthLogger.stage("GoogleAuth", "Creating GoogleAuthProvider credential from ID Token")
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            AuthLogger.stage("GoogleAuth", "Invoking FirebaseAuth.signInWithCredential")
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                AuthLogger.success("GoogleAuth", "Firebase sign-in completed successfully", "UID: ${user.uid}, email: ${user.email}")
                Result.success(user)
            } else {
                AuthLogger.error("GoogleAuth", "User details empty after sign-in", "Null FirebaseUser")
                Result.failure(Exception("Google sign-in succeeded but user details were empty."))
            }
        } catch (e: Exception) {
            val mapped = mapAuthException(e)
            AuthLogger.error("GoogleAuth", "signInWithCredential failed", mapped.message ?: "Unknown error", e)
            Result.failure(mapped)
        }
    }

    private var lastResendingToken: PhoneAuthProvider.ForceResendingToken? = null

    /**
     * Initiates Firebase Phone Number verification.
     * Note: OTP codes are strictly handled internally and never logged.
     */
    fun sendPhoneVerificationCode(
        phoneNumber: String,
        activity: Activity,
        isResend: Boolean = false,
        onCodeSent: (verificationId: String, resendToken: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationCompleted: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanNumber = phoneNumber.trim()
        if (!cleanNumber.startsWith("+") || cleanNumber.length < 8) {
            AuthLogger.error("PhoneAuth", "Phone validation failed", "Number too short or missing country code: $cleanNumber")
            onError("Please enter a valid phone number with international country code (e.g. +91 9876543210).")
            return
        }

        AuthLogger.stage("PhoneAuth", "Initiating PhoneAuthProvider.verifyPhoneNumber", "Phone: $cleanNumber, isResend: $isResend")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                AuthLogger.stage("PhoneAuth", "Instant auto-verification triggered by Play Services")
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { authResult ->
                        val user = authResult.user
                        if (user != null) {
                            AuthLogger.success("PhoneAuth", "Instant sign-in completed", "UID: ${user.uid}")
                            onVerificationCompleted(user)
                        } else {
                            onError("Sign-in succeeded but user profile was empty.")
                        }
                    }
                    .addOnFailureListener { e ->
                        val mapped = mapAuthException(e)
                        AuthLogger.error("PhoneAuth", "Instant sign-in failed", mapped.message ?: "", e)
                        onError(mapped.localizedMessage ?: "Verification failed.")
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val mapped = mapAuthException(e)
                AuthLogger.error("PhoneAuth", "PhoneAuthProvider onVerificationFailed", mapped.message ?: "", e)
                onError(mapped.localizedMessage ?: "Phone verification failed.")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                lastResendingToken = token
                AuthLogger.success("PhoneAuth", "SMS OTP dispatched by Firebase", "verificationId: $verificationId")
                onCodeSent(verificationId, token)
            }
        }

        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(cleanNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (isResend && lastResendingToken != null) {
            builder.setForceResendingToken(lastResendingToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(builder.build())
    }

    /**
     * Completes phone sign-in using the user-entered 6-digit SMS OTP.
     */
    suspend fun verifyPhoneOtp(
        verificationId: String,
        otpCode: String
    ): Result<FirebaseUser> {
        val trimmedCode = otpCode.trim()
        if (trimmedCode.length != 6) {
            AuthLogger.error("PhoneAuth", "OTP validation failed", "Length is ${trimmedCode.length}, expected 6")
            return Result.failure(Exception("Please enter the complete 6-digit verification code."))
        }

        AuthLogger.stage("PhoneAuth", "Verifying OTP code with verificationId")

        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, trimmedCode)
            AuthLogger.stage("PhoneAuth", "Calling FirebaseAuth.signInWithCredential with PhoneAuthCredential")
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                AuthLogger.success("PhoneAuth", "Phone sign-in completed successfully", "UID: ${user.uid}")
                Result.success(user)
            } else {
                AuthLogger.error("PhoneAuth", "Phone sign-in succeeded but user details were empty", "Null FirebaseUser")
                Result.failure(Exception("Phone sign-in succeeded but user profile was empty."))
            }
        } catch (e: Exception) {
            val mapped = mapAuthException(e)
            AuthLogger.error("PhoneAuth", "verifyPhoneOtp failed", mapped.message ?: "", e)
            Result.failure(mapped)
        }
    }

    fun signOut() {
        auth.signOut()
        lastResendingToken = null
    }

    private fun mapAuthException(e: Exception): Exception {
        val rawMsg = e.localizedMessage ?: e.message ?: ""
        return when {
            rawMsg.contains("API key not valid", ignoreCase = true) -> {
                Exception("Firebase configuration error: Invalid API key. Please configure your active Firebase project's google-services.json.")
            }
            rawMsg.contains("app is not authorized", ignoreCase = true) ||
            rawMsg.contains("SafetyNet", ignoreCase = true) ||
            rawMsg.contains("Play Integrity", ignoreCase = true) -> {
                Exception("Phone verification setup required: Please add your SHA-1/SHA-256 fingerprint in Firebase Console.")
            }
            rawMsg.contains("quota", ignoreCase = true) -> {
                Exception("SMS quota exceeded. Please wait a moment or configure test numbers in Firebase Console.")
            }
            rawMsg.contains("invalid-verification-code", ignoreCase = true) ||
            rawMsg.contains("code is invalid", ignoreCase = true) -> {
                Exception("The 6-digit verification code is incorrect. Please check and try again.")
            }
            rawMsg.contains("session-expired", ignoreCase = true) -> {
                Exception("Verification code has expired. Please tap 'Resend Code'.")
            }
            e is FirebaseAuthInvalidCredentialsException -> {
                Exception("Invalid credentials or verification code. Please check and try again.")
            }
            e is FirebaseTooManyRequestsException -> {
                Exception("Too many verification attempts. Please wait a few minutes before trying again.")
            }
            e is FirebaseNetworkException -> {
                Exception("Network connection failed. Please check your internet connection.")
            }
            else -> {
                Exception(e.localizedMessage ?: "An unexpected authentication error occurred.")
            }
        }
    }
}
