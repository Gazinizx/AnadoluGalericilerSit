package com.example.anadolugalericilersit.data.repository

import android.content.Context
import android.net.Uri
import com.example.anadolugalericilersit.data.local.LocalStore
import com.example.anadolugalericilersit.data.model.Dealer
import com.example.anadolugalericilersit.data.model.DealerStatus
import com.example.anadolugalericilersit.data.model.Role
import com.example.anadolugalericilersit.data.model.User
import com.example.anadolugalericilersit.utils.Resource
import com.example.anadolugalericilersit.utils.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class AuthRepository {

    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    fun isUserLoggedIn(): Boolean {
        if (LocalStore.currentLoggedInUid != null) return true
        val savedUid = SessionManager.getSavedUid()
        return !savedUid.isNullOrBlank()
    }

    fun getCurrentUid(): String? {
        return LocalStore.currentLoggedInUid ?: SessionManager.getSavedUid()
    }

    suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val cleanEmail = email.trim()
            val cleanPass = password.trim()
            val normalizedInput = cleanEmail.lowercase().replace(" ", "")

            if (cleanEmail.isBlank() || cleanPass.isBlank()) {
                return Resource.Error("E-posta/Telefon ve şifre boş bırakılamaz")
            }

            // Check if input matches Admin 1 (Mevlüt Admin)
            val isAdmin1Input = normalizedInput == "05054543099" ||
                    normalizedInput == "5054543099" ||
                    normalizedInput == "mevlutadmin"

            if (isAdmin1Input) {
                val isValidPass = cleanPass == "Mevlüt321." ||
                        cleanPass == "mevlut321." ||
                        cleanPass == "mevlut321" ||
                        cleanPass == "Mevlüt321"
                if (!isValidPass) {
                    return Resource.Error("Girilen şifre hatalı!")
                }

                val adminUser = User(
                    uid = "admin_mevlut",
                    email = cleanEmail,
                    password = cleanPass,
                    name = "Mevlüt Admin",
                    role = Role.SUPER_ADMIN,
                    canIssueDamga = true
                )
                LocalStore.users["admin_mevlut"] = adminUser
                LocalStore.currentLoggedInUid = adminUser.uid
                SessionManager.saveSession(adminUser.uid)

                try {
                    firestore.collection("users").document(adminUser.uid).set(adminUser).await()
                } catch (e: Exception) {
                    // ignore firestore sync errors offline
                }

                return Resource.Success(adminUser)
            }

            // Check if input matches Admin 2 (Süper Yönetim)
            val isAdmin2Input = normalizedInput == "yönetim@gmail.com" ||
                    normalizedInput == "yonetim@gmail.com" ||
                    normalizedInput == "yonetim" ||
                    normalizedInput == "yönetim" ||
                    normalizedInput == "5541027908" ||
                    normalizedInput == "05541027908" ||
                    normalizedInput == "5441027908" ||
                    normalizedInput == "05441027908" ||
                    normalizedInput == "admin@gmail.com" ||
                    normalizedInput == "admin"

            if (isAdmin2Input) {
                val isValidPass = cleanPass == "MD112233." ||
                        cleanPass == "md112233." ||
                        cleanPass == "300426" ||
                        cleanPass == "123456"
                if (!isValidPass) {
                    return Resource.Error("Girilen şifre hatalı!")
                }

                val adminUser = User(
                    uid = "admin_super_full",
                    email = cleanEmail,
                    password = cleanPass,
                    name = "Süper Yetkili Yönetici",
                    role = Role.SUPER_ADMIN,
                    canIssueDamga = true
                )
                LocalStore.users["admin_super_full"] = adminUser
                LocalStore.currentLoggedInUid = adminUser.uid
                SessionManager.saveSession(adminUser.uid)

                try {
                    firestore.collection("users").document(adminUser.uid).set(adminUser).await()
                } catch (e: Exception) {
                    // ignore firestore sync errors offline
                }

                return Resource.Success(adminUser)
            }

            // Check existing registered users in LocalStore
            val localUser = LocalStore.users.values.find {
                it.email.equals(cleanEmail, ignoreCase = true) ||
                        it.uid == cleanEmail
            }
            if (localUser != null) {
                if (localUser.password.isNotBlank() && localUser.password != cleanPass) {
                    return Resource.Error("Girilen şifre hatalı!")
                }
                LocalStore.currentLoggedInUid = localUser.uid
                SessionManager.saveSession(localUser.uid)
                return Resource.Success(localUser)
            }

            // Query Firestore users collection for real registered dealers / users
            try {
                val queryByEmail = firestore.collection("users")
                    .whereEqualTo("email", cleanEmail)
                    .get()
                    .await()

                var doc = queryByEmail.documents.firstOrNull()

                // Fallback: Query dealers collection if not found in users
                if (doc == null || !doc.exists()) {
                    val dealerQuery = firestore.collection("dealers")
                        .whereEqualTo("email", cleanEmail)
                        .get()
                        .await()
                    doc = dealerQuery.documents.firstOrNull()
                }

                if (doc != null && doc.exists()) {
                    var user = doc.toObject(User::class.java)
                    if (user != null) {
                        // Check password
                        if (user.password.isNotBlank() && user.password != cleanPass) {
                            return Resource.Error("Girilen şifre hatalı!")
                        }

                        if (user.email.contains("yonetim", ignoreCase = true) ||
                            user.email.contains("yönetim", ignoreCase = true) ||
                            user.email == "05054543099" ||
                            user.email == "5441027908" ||
                            user.email == "5541027908"
                        ) {
                            user = user.copy(role = Role.SUPER_ADMIN, canIssueDamga = true)
                        }

                        LocalStore.users[user.uid] = user
                        LocalStore.currentLoggedInUid = user.uid
                        SessionManager.saveSession(user.uid)

                        // Load dealer profile if present
                        val dealerId = user.dealerId
                        if (!dealerId.isNullOrBlank()) {
                            val dealerDoc = firestore.collection("dealers").document(dealerId).get().await()
                            if (dealerDoc.exists()) {
                                val dealer = dealerDoc.toObject(Dealer::class.java)
                                if (dealer != null) {
                                    LocalStore.dealers[dealerId] = dealer
                                }
                            }
                        }

                        return Resource.Success(user)
                    }
                }
            } catch (e: Exception) {
                // proceed if query fails
            }

            Resource.Error("Girilen e-posta adresi veya kullanıcı bulunamadı! Lütfen önce kayıt olun.")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Giriş yapılırken hata oluştu")
        }
    }

    suspend fun registerDealer(
        context: Context,
        galleryName: String,
        authorizedName: String,
        phone: String,
        email: String,
        password: String,
        city: String,
        district: String,
        address: String,
        taxNumber: String,
        iban: String,
        ibanOwnerName: String,
        description: String,
        logoUri: Uri?
    ): Resource<Dealer> {
        return try {
            val uid = UUID.randomUUID().toString()
            val dealer = Dealer(
                id = uid,
                ownerUid = uid,
                galleryName = galleryName,
                authorizedName = authorizedName,
                phone = phone,
                email = email,
                city = city.ifBlank { "Kayseri" },
                district = district.ifBlank { "Kocasinan" },
                address = address,
                taxNumber = taxNumber,
                iban = iban,
                ibanOwnerName = ibanOwnerName,
                description = description,
                logoUrl = logoUri?.toString() ?: "",
                accountStatus = DealerStatus.PENDING
            )

            val user = User(
                uid = uid,
                email = email,
                password = hashPassword(password),
                name = galleryName,
                role = Role.DEALER,
                dealerId = uid
            )

            LocalStore.users[uid] = user
            LocalStore.dealers[uid] = dealer
            LocalStore.currentLoggedInUid = uid
            SessionManager.saveSession(uid, context)

            try {
                firestore.collection("dealers").document(uid).set(dealer).await()
                firestore.collection("users").document(uid).set(user).await()
            } catch (e: Exception) {
                // ignore firestore sync errors if offline
            }

            Resource.Success(dealer)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Kayıt olunurken hata oluştu")
        }
    }

    suspend fun resetPassword(email: String): Resource<Unit> {
        return try {
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Şifre sıfırlama başarısız")
        }
    }

    suspend fun getCurrentUser(): Resource<User> {
        val uid = LocalStore.currentLoggedInUid ?: SessionManager.getSavedUid()
        if (uid.isNullOrBlank()) return Resource.Error("Oturum açık değil")
        val safeUid: String = uid

        val localUser = LocalStore.users[safeUid]
        if (localUser != null) {
            LocalStore.currentLoggedInUid = safeUid
            if (localUser.email == "05054543099" ||
                localUser.email == "5441027908" ||
                localUser.email == "5541027908" ||
                localUser.email.contains("yonetim", ignoreCase = true) ||
                localUser.email.contains("yönetim", ignoreCase = true) ||
                safeUid.startsWith("admin")
            ) {
                val updated = localUser.copy(role = Role.SUPER_ADMIN, canIssueDamga = true)
                LocalStore.users[safeUid] = updated
                return Resource.Success(updated)
            }
            return Resource.Success(localUser)
        }

        // Handle direct admin UIDs
        if (safeUid == "admin_mevlut" || safeUid == "admin_super_full") {
            val adminUser = User(
                uid = safeUid,
                email = if (safeUid == "admin_mevlut") "05054543099" else "yönetim@gmail.com",
                name = if (safeUid == "admin_mevlut") "Mevlüt Admin" else "Süper Yetkili Yönetici",
                role = Role.SUPER_ADMIN,
                canIssueDamga = true
            )
            LocalStore.users[safeUid] = adminUser
            LocalStore.currentLoggedInUid = safeUid
            return Resource.Success(adminUser)
        }

        // Query Firestore for saved UID
        return try {
            val doc = firestore.collection("users").document(safeUid).get().await()
            if (doc.exists()) {
                var user = doc.toObject(User::class.java)
                if (user != null) {
                    if (user.email.contains("yonetim", ignoreCase = true) ||
                        user.email.contains("yönetim", ignoreCase = true) ||
                        user.email == "05054543099" ||
                        user.email == "5441027908" ||
                        user.email == "5541027908"
                    ) {
                        user = user.copy(role = Role.SUPER_ADMIN, canIssueDamga = true)
                    }

                    LocalStore.users[safeUid] = user
                    LocalStore.currentLoggedInUid = safeUid
                    return Resource.Success(user)
                }
            }
            Resource.Error("Kullanıcı bulunamadı")
        } catch (e: Exception) {
            Resource.Error("Kullanıcı oturumu yüklenemedi")
        }
    }

    suspend fun getCurrentDealerProfile(): Resource<Dealer> {
        val uid = LocalStore.currentLoggedInUid ?: SessionManager.getSavedUid()
        if (uid.isNullOrBlank()) return Resource.Error("Oturum açık değil")
        val safeUid: String = uid

        val user = LocalStore.users[safeUid]
        if (user?.role == Role.SUPER_ADMIN ||
            user?.role == Role.ADMIN ||
            user?.email == "05054543099" ||
            user?.email == "5441027908" ||
            user?.email == "5541027908" ||
            safeUid.startsWith("admin")
        ) {
            return Resource.Error("Admin kullanıcısının galeri profili yoktur")
        }

        val localDealer = LocalStore.dealers[safeUid]
        if (localDealer != null) return Resource.Success(localDealer)

        return try {
            val doc = firestore.collection("dealers").document(safeUid).get().await()
            if (doc.exists()) {
                val dealer = doc.toObject(Dealer::class.java)
                if (dealer != null) {
                    LocalStore.dealers[safeUid] = dealer
                    return Resource.Success(dealer)
                }
            }
            Resource.Error("Galeri bilgisi bulunamadı")
        } catch (e: Exception) {
            Resource.Error("Galeri profili yüklenemedi")
        }
    }

    fun logout() {
        LocalStore.currentLoggedInUid = null
        SessionManager.clearSession()
    }

    suspend fun deleteAccount(): Resource<Unit> {
        val uid = LocalStore.currentLoggedInUid ?: SessionManager.getSavedUid()
        if (!uid.isNullOrBlank()) {
            LocalStore.users.remove(uid)
            LocalStore.dealers.remove(uid)
        }
        LocalStore.currentLoggedInUid = null
        SessionManager.clearSession()
        return Resource.Success(Unit)
    }

    private fun hashPassword(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input
        }
    }
}
