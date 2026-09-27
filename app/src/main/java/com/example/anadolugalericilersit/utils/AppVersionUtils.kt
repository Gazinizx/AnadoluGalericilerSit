package com.example.anadolugalericilersit.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.example.anadolugalericilersit.data.model.AppVersionConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object AppVersionUtils {

    /**
     * Gets current app versionCode installed on device.
     */
    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Gets current app versionName installed on device.
     */
    fun getCurrentVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    /**
     * Fetches version configuration from Firestore collection "app_config", document "version".
     */
    suspend fun checkAppVersion(): AppVersionConfig? {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("app_config")
                .document("version")
                .get()
                .await()

            if (doc.exists()) {
                doc.toObject(AppVersionConfig::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Opens the APK download URL or store page in browser.
     */
    fun openUpdateUrl(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // fallback
        }
    }
}
