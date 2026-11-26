package com.example.cortex_camping.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class AdminRepository {
    private val db = Firebase.firestore
    private val adminsRef = db.collection("admins")
    private val _admins = mutableStateListOf<Admin>()

    val admins: List<Admin>
        get() = _admins

    // true once Firestore has answered the first time, so the app knows who this device is
    var loaded by mutableStateOf(false)
        private set

    fun saveOrUpdateAdmin(deviceId: String, admin: Admin) {
        val adminWithId = admin.copy(id = deviceId)
        adminsRef.document(deviceId).set(adminWithId)
    }

    init {
        adminsRef.addSnapshotListener {snapshot, error ->
            loaded = true
            if (error != null) {
                android.util.Log.e("AdminRepository", "Couldn't load admins", error)
                return@addSnapshotListener
            }
            _admins.clear()
            snapshot?.documents?.forEach { document ->
                val admin = document.toObject(Admin::class.java)
                if (admin != null) {
                    _admins.add(admin)
                }
            }
        }
    }
}