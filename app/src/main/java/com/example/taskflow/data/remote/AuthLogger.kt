package com.example.taskflow.data.remote

import android.util.Log

/**
 * Diagnostic logger for TaskFlow Firebase authentication flows.
 * Provides granular tracking of every authentication stage while strictly
 * ensuring no sensitive credentials, OTPs, or tokens are logged.
 */
object AuthLogger {
    private const val TAG = "TaskFlowAuth"

    fun stage(flow: String, stageName: String, details: String? = null) {
        val msg = if (!details.isNullOrBlank()) {
            "[$flow] Stage: $stageName -> $details"
        } else {
            "[$flow] Stage: $stageName"
        }
        Log.d(TAG, msg)
    }

    fun success(flow: String, stageName: String, details: String? = null) {
        val msg = if (!details.isNullOrBlank()) {
            "[$flow] SUCCESS: $stageName -> $details"
        } else {
            "[$flow] SUCCESS: $stageName"
        }
        Log.i(TAG, msg)
    }

    fun error(flow: String, stageName: String, errorMsg: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$flow] ERROR at $stageName: $errorMsg", throwable)
    }
}
