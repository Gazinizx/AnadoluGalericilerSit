package com.example.anadolugalericilersit.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object IntentUtils {

    fun openDialer(context: Context, phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Telefon numarası bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        val cleanNumber = phoneNumber.replace("\\s".toRegex(), "")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Telefon uygulaması açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsApp(context: Context, phoneNumber: String, message: String = "Merhaba, ilanınızla ilgileniyorum.") {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Telefon numarası bulunamadı", Toast.LENGTH_SHORT).show()
            return
        }
        var cleanNumber = phoneNumber.replace("[^0-9]".toRegex(), "")
        if (cleanNumber.startsWith("0")) {
            cleanNumber = "90" + cleanNumber.substring(1)
        } else if (!cleanNumber.startsWith("90")) {
            cleanNumber = "90$cleanNumber"
        }
        val url = "https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    fun openMaps(context: Context, latitude: Double, longitude: Double, label: String = "Galeri Adresi") {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser if Google Maps app is not installed
            val browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            try {
                context.startActivity(browserIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Harita uygulaması açılamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareVehicle(context: Context, vehicleTitle: String, price: String, vehicleId: String) {
        val text = "Europ Galeri'de bu harika araca göz atın:\n$vehicleTitle\nFiyat: $price TL\nİlan No: $vehicleId"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, vehicleTitle)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "İlanı Paylaş"))
    }
}
