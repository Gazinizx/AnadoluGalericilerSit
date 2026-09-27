package com.example.anadolugalericilersit.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.anadolugalericilersit.MainActivity
import com.example.anadolugalericilersit.R
import com.example.anadolugalericilersit.data.local.LocalStore
import com.example.anadolugalericilersit.data.local.TransactionType
import com.example.anadolugalericilersit.data.model.Dealer
import com.example.anadolugalericilersit.data.model.NotificationItem
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

object NotificationUtils {

    private const val CHANNEL_ID = "anadolu_galeri_notifications"
    private const val CHANNEL_NAME = "Anadolu Galericiler Bildirimleri"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Borç, Ödeme ve Sistem Bildirimleri"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a system status bar notification.
     */
    fun showSystemNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (_: Exception) {
            // Permission missing or security restriction
        }
    }

    /**
     * Triggers notifications for debt and payment transactions.
     * Both status bar notification and in-app NotificationItem entries.
     */
    fun sendDebtOrPaymentNotification(
        context: Context,
        dealer: Dealer,
        type: TransactionType,
        amount: Double
    ) {
        val amountInt = amount.toInt()
        val isPayment = type == TransactionType.PAYMENT

        // Dealer Notification Texts
        val dealerTitle = if (isPayment) "Borç Ödemesi Alındı" else "Hesap Borç Güncellemesi"
        val dealerMessage = if (isPayment) {
            "Borcunuzdan $amountInt TL düşüldü."
        } else {
            "Hesabınıza $amountInt TL eklendi."
        }

        // Admin Notification Texts
        val adminTitle = if (isPayment) "Ödeme Alındı Bilgisi" else "Yeni Borç Eklendi"
        val adminMessage = if (isPayment) {
            "${dealer.galleryName} $amountInt TL ödeme yaptı."
        } else {
            "${dealer.galleryName} firmasına $amountInt TL borç eklendi."
        }

        val baseId = System.currentTimeMillis().toInt()

        // 1. Trigger Status Bar Notification on system notification bar
        val currentUid = LocalStore.currentLoggedInUid
        val isCurrentDealer = (currentUid == dealer.ownerUid) || (currentUid == dealer.id)

        if (isCurrentDealer) {
            // Show dealer status bar notification
            showSystemNotification(context, dealerTitle, dealerMessage, baseId)
        } else {
            // Admin is logged in: show admin status bar notification, and also show dealer notification
            showSystemNotification(context, adminTitle, adminMessage, baseId)
            showSystemNotification(context, "Galerici Bildirimi: $dealerTitle", dealerMessage, baseId + 1)
        }

        // 2. Save In-App Notification for Dealer
        val dealerUserId = dealer.ownerUid.ifBlank { dealer.id }
        val dealerNotification = NotificationItem(
            id = UUID.randomUUID().toString(),
            userId = dealerUserId,
            title = dealerTitle,
            message = dealerMessage,
            type = if (isPayment) "PAYMENT" else "DEBT",
            createdAt = System.currentTimeMillis()
        )

        val dealerList = LocalStore.notifications.getOrPut(dealerUserId) { mutableListOf() }
        dealerList.add(0, dealerNotification)

        // 3. Save In-App Notification for Admin
        val adminNotification = NotificationItem(
            id = UUID.randomUUID().toString(),
            userId = "admin_mevlut",
            title = adminTitle,
            message = adminMessage,
            type = if (isPayment) "PAYMENT" else "DEBT",
            createdAt = System.currentTimeMillis()
        )

        val adminList = LocalStore.notifications.getOrPut("admin_mevlut") { mutableListOf() }
        adminList.add(0, adminNotification)

        // Firestore async save
        try {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("notifications").document(dealerNotification.id).set(dealerNotification)
            firestore.collection("notifications").document(adminNotification.id).set(adminNotification)
        } catch (_: Exception) {
            // ignore offline
        }
    }
}


