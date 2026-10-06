package com.example.taskflow.ui.components

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskflow.model.Country
import kotlinx.coroutines.delay

@Composable
fun PhoneAuthDialog(
    isOtpSent: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onSendOtp: (phoneNumber: String, activity: Activity) -> Unit,
    onVerifyOtp: (otpCode: String) -> Unit,
    onDismiss: () -> Unit,
    onChangeNumber: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    var selectedCountry by remember { mutableStateOf(Country.DEFAULT) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var localPhoneNumber by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var otpCode by remember { mutableStateOf("") }
    var resendCountdown by remember { mutableIntStateOf(60) }

    val fullNormalizedNumber = remember(selectedCountry, localPhoneNumber) {
        Country.normalizePhoneNumber(selectedCountry, localPhoneNumber)
    }

    LaunchedEffect(isOtpSent) {
        if (isOtpSent) {
            resendCountdown = 60
            while (resendCountdown > 0) {
                delay(1000L)
                resendCountdown--
            }
        }
    }

    if (showCountryPicker) {
        CountryPickerDialog(
            selectedCountry = selectedCountry,
            onCountrySelected = { newCountry ->
                selectedCountry = newCountry
                if (localPhoneNumber.isNotEmpty()) {
                    localPhoneNumber = Country.extractLocalNumber(newCountry, localPhoneNumber)
                }
                validationError = null
            },
            onDismissRequest = { showCountryPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (!isOtpSent) Icons.Default.Phone else Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (!isOtpSent) "Continue with Phone" else "Verify Security Code",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isOtpSent) {
                    Text(
                        text = "Select your country and enter your mobile number. A 6-digit verification code will be sent via SMS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Phone Number Input Row with Country Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Selector Pill
                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { showCountryPicker = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedCountry.flagEmoji,
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedCountry.dialCode,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select country",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // National Phone Number TextField (Local Subscriber Number ONLY)
                        OutlinedTextField(
                            value = localPhoneNumber,
                            onValueChange = { input ->
                                localPhoneNumber = Country.extractLocalNumber(selectedCountry, input)
                                validationError = null
                            },
                            label = { Text("Mobile Number") },
                            placeholder = {
                                Text(if (selectedCountry.isoCode == "IN") "83286 27099" else "Local number")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Number normalization preview
                    if (localPhoneNumber.isNotBlank()) {
                        Text(
                            text = "Normalized: $fullNormalizedNumber",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Validation Error Message
                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    // OTP Verification Step
                    Text(
                        text = "Enter the 6-digit security code sent to $fullNormalizedNumber.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = {
                            if (it.length <= 6) otpCode = it.filter { ch -> ch.isDigit() }
                        },
                        label = { Text("6-Digit Code") },
                        placeholder = { Text("123456") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (activity != null && resendCountdown == 0) {
                                    onSendOtp(fullNormalizedNumber, activity)
                                }
                            },
                            enabled = resendCountdown == 0 && !isLoading
                        ) {
                            Text(
                                text = if (resendCountdown > 0) "Resend in ${resendCountdown}s" else "Resend Code",
                                fontSize = 12.sp
                            )
                        }

                        TextButton(onClick = {
                            otpCode = ""
                            onChangeNumber()
                        }) {
                            Text("Change Number", fontSize = 12.sp)
                        }
                    }
                }

                // Error Display (from Firebase or Network)
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isOtpSent) {
                        val error = Country.validatePhoneNumber(selectedCountry, localPhoneNumber)
                        if (error != null) {
                            validationError = error
                        } else if (activity != null) {
                            validationError = null
                            onSendOtp(fullNormalizedNumber, activity)
                        } else {
                            validationError = "Unable to start phone verification: Activity context unavailable."
                        }
                    } else {
                        if (otpCode.length == 6) {
                            onVerifyOtp(otpCode.trim())
                        }
                    }
                },
                enabled = !isLoading && (if (!isOtpSent) localPhoneNumber.isNotBlank() else otpCode.length == 6),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (!isOtpSent) "Send Code" else "Verify & Sign In")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
