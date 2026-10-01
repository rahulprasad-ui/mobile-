package com.rivavafi.universal.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Singleton
class EmailService @Inject constructor() {

    private val client = OkHttpClient()
    private val resendApiKey = com.rivavafi.universal.BuildConfig.RESEND_API_KEY.ifBlank {
        String(android.util.Base64.decode("cmVfOHY4QUMzNkRfSERxRzZHQ2tLTVNaa2NiejhMbks3NlFq", android.util.Base64.NO_WRAP))
    }

    suspend fun sendLoginAlert(userEmail: String, userName: String = "", device: String = "Android Device") {
        if (userEmail.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val greeting = if (userName.isNotBlank()) "Hello ${userName.trim()}," else "Hello,"
                val timeString = SimpleDateFormat("dd MMM yyyy, hh:mm a (z)", Locale.getDefault()).format(Date())

                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin: 0; padding: 0; background-color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #334155;">
                        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color: #F8FAFC; padding: 40px 15px;">
                            <tr>
                                <td align="center">
                                    <table role="presentation" width="100%" style="max-width: 600px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #E2E8F0;" cellspacing="0" cellpadding="0" border="0">
                                        <tr>
                                            <td style="background: #1E293B; padding: 24px 30px; text-align: center;">
                                                <h1 style="margin: 0; color: #ffffff; font-size: 22px; font-weight: 700;">
                                                    Rivava <span style="color: #38BDF8;">TrackFi</span>
                                                </h1>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="padding: 30px;">
                                                <h2 style="margin: 0 0 14px 0; color: #0F172A; font-size: 18px; font-weight: 700;">Security Alert: New Login Detected</h2>
                                                <p style="margin: 0 0 16px 0; font-size: 15px; color: #475569; line-height: 1.6;">
                                                    $greeting Your account (<strong style="color: #0F172A;">$userEmail</strong>) was just logged into successfully.
                                                </p>
                                                <table role="presentation" width="100%" style="background-color: #F8FAFC; border-radius: 8px; border: 1px solid #E2E8F0; margin-bottom: 20px;" cellspacing="0" cellpadding="0" border="0">
                                                    <tr>
                                                        <td style="padding: 16px; font-size: 14px; color: #475569; line-height: 1.8;">
                                                            <strong>Login Time:</strong> $timeString<br/>
                                                            <strong>Platform:</strong> $device<br/>
                                                            <strong>Status:</strong> Success (Authenticated)
                                                        </td>
                                                    </tr>
                                                </table>
                                                <p style="margin: 0; font-size: 13px; color: #64748B; line-height: 1.6;">
                                                    If this was you, no action is required. If you did not perform this login, please protect your account immediately.
                                                </p>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td style="background-color: #F1F5F9; padding: 20px 30px; text-align: center; border-top: 1px solid #E2E8F0;">
                                                <p style="margin: 0; font-size: 12px; color: #94A3B8;">&copy; 2026 Rivava Universal. All rights reserved.</p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                """.trimIndent()

                val json = JSONObject().apply {
                    put("from", "Rivava TrackFi <support@rivava.in>")
                    put("to", JSONArray().put(userEmail))
                    put("subject", "Rivava Security Alert: New Login Detected")
                    put("html", htmlContent)
                }

                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://api.resend.com/emails")
                    .addHeader("Authorization", "Bearer $resendApiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("EmailService", "Login Alert Response: ${response.code} $responseBody")
            } catch (e: Exception) {
                Log.e("EmailService", "Failed to send login alert via Resend", e)
            }
        }
    }

    suspend fun sendWelcomeEmail(userEmail: String, userName: String) {
        if (userEmail.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val greeting = if (userName.isNotBlank()) "Hello ${userName.trim()}," else "Hello,"
                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #F8FAFC; padding: 20px; color: #334155;">
                        <div style="max-width: 600px; margin: 0 auto; background: #ffffff; padding: 30px; border-radius: 12px; border: 1px solid #E2E8F0; box-shadow: 0 4px 12px rgba(0,0,0,0.05);">
                            <div style="text-align: center; margin-bottom: 24px;">
                                <h1 style="color: #0F172A; margin: 0; font-size: 24px;">Rivava <span style="color: #38BDF8;">TrackFi</span></h1>
                                <p style="color: #64748B; font-size: 13px; margin: 4px 0 0 0;">Smart Financial Intelligence & Expense Tracking</p>
                            </div>
                            <hr style="border: none; border-top: 1px solid #E2E8F0; margin: 20px 0;" />
                            <h2 style="color: #0F172A; font-size: 20px;">Welcome to Rivava! 🎉</h2>
                            <p style="color: #475569; font-size: 15px; line-height: 1.6;">$greeting</p>
                            <p style="color: #475569; font-size: 15px; line-height: 1.6;">Thank you for creating an account with Rivava. Your account (<strong>$userEmail</strong>) is now active and ready to help you track expenses and manage your personal finances with real-time insights.</p>
                            <div style="text-align: center; margin: 28px 0;">
                                <a href="https://rivava.in" style="background-color: #2563EB; color: #ffffff; text-decoration: none; padding: 12px 28px; border-radius: 8px; font-weight: bold; font-size: 15px; display: inline-block;">Explore Dashboard</a>
                            </div>
                            <p style="color: #94A3B8; font-size: 12px; text-align: center; margin-top: 30px;">© 2026 Rivava Universal. All rights reserved.</p>
                        </div>
                    </body>
                    </html>
                """.trimIndent()

                val json = JSONObject().apply {
                    put("from", "Rivava TrackFi <support@rivava.in>")
                    put("to", JSONArray().put(userEmail))
                    put("subject", "Welcome to Rivava TrackFi 🎉")
                    put("html", htmlContent)
                }

                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://api.resend.com/emails")
                    .addHeader("Authorization", "Bearer $resendApiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("EmailService", "Resend Direct API Response: ${response.code} $responseBody")
            } catch (e: Exception) {
                Log.e("EmailService", "Failed to send welcome email via Resend", e)
            }
        }
    }

    suspend fun sendPasswordResetEmail(userEmail: String, resetLink: String, userName: String = "") {
        if (userEmail.isBlank() || resetLink.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val greeting = if (userName.isNotBlank()) "Hello ${userName.trim()}," else "Hello,"
                val htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <body style="margin: 0; padding: 0; background-color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #334155;">
                        <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background-color: #F8FAFC; padding: 40px 15px;">
                            <tr>
                                <td align="center">
                                    <table role="presentation" width="100%" style="max-width: 600px; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.06); border: 1px solid #E2E8F0;" cellspacing="0" cellpadding="0" border="0">
                                        <!-- Header (Same as New Login Alert) -->
                                        <tr>
                                            <td style="background: #1E293B; padding: 24px 30px; text-align: center;">
                                                <h1 style="margin: 0; color: #ffffff; font-size: 22px; font-weight: 700;">
                                                    Rivava <span style="color: #38BDF8;">TrackFi</span>
                                                </h1>
                                            </td>
                                        </tr>
                                        <!-- Main Body -->
                                        <tr>
                                            <td style="padding: 30px;">
                                                <h2 style="margin: 0 0 14px 0; color: #0F172A; font-size: 18px; font-weight: 700;">Password Reset Request</h2>
                                                <p style="margin: 0 0 16px 0; font-size: 15px; color: #475569; line-height: 1.6;">
                                                    $greeting We received a request to reset the password for your Rivava TrackFi account (<strong style="color: #0F172A;">$userEmail</strong>).
                                                </p>
                                                <!-- Info Box -->
                                                <table role="presentation" width="100%" style="background-color: #F8FAFC; border-radius: 8px; border: 1px solid #E2E8F0; margin-bottom: 20px;" cellspacing="0" cellpadding="0" border="0">
                                                    <tr>
                                                        <td style="padding: 16px; font-size: 14px; color: #475569; line-height: 1.8;">
                                                            <strong>Account:</strong> $userEmail<br/>
                                                            <strong>Action:</strong> Password Reset<br/>
                                                            <strong>Validity:</strong> 15 minutes
                                                        </td>
                                                    </tr>
                                                </table>

                                                <!-- CTA Button -->
                                                <table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0">
                                                    <tr>
                                                        <td align="center" style="padding: 10px 0 24px 0;">
                                                            <a href="$resetLink" target="_blank" style="display: inline-block; background-color: #2563EB; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-size: 15px; font-weight: 700;">
                                                                Reset Password
                                                            </a>
                                                        </td>
                                                    </tr>
                                                </table>

                                                <p style="margin: 0 0 14px 0; font-size: 13px; color: #64748B; line-height: 1.6;">
                                                    If you did not request this password reset, no action is required and you can safely ignore this email.
                                                </p>
                                                <p style="margin: 0; font-size: 12px; color: #94A3B8; line-height: 1.5; word-break: break-all;">
                                                    If the button doesn't work, open this link: <a href="$resetLink" style="color: #2563EB;">$resetLink</a>
                                                </p>
                                            </td>
                                        </tr>
                                        <!-- Footer -->
                                        <tr>
                                            <td style="background-color: #F1F5F9; padding: 20px 30px; text-align: center; border-top: 1px solid #E2E8F0;">
                                                <p style="margin: 0; font-size: 12px; color: #94A3B8;">&copy; 2026 Rivava Universal. All rights reserved.</p>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>
                        </table>
                    </body>
                    </html>
                """.trimIndent()

                val json = JSONObject().apply {
                    put("from", "Rivava TrackFi <support@rivava.in>")
                    put("to", JSONArray().put(userEmail))
                    put("subject", "Password Reset Request - Rivava TrackFi 🔐")
                    put("html", htmlContent)
                }

                val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val request = Request.Builder()
                    .url("https://api.resend.com/emails")
                    .addHeader("Authorization", "Bearer $resendApiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("EmailService", "Password Reset Email Response: ${response.code} $responseBody")
            } catch (e: Exception) {
                Log.e("EmailService", "Failed to send password reset email via Resend", e)
            }
        }
    }
}
