package com.example.cortex_camping.model

import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class AdminRepository {
    private val db = Firebase.firestore
    private val adminsRef = db.collection("admins")
    private val _admins = mutableStateListOf<Admin>()

    val admins: List<Admin>
        get() = _admins

    fun saveOrUpdateAdmin(deviceId: String, admin: Admin) {
        val adminWithId = admin.copy(id = deviceId)
        adminsRef.document(deviceId).set(adminWithId)
    }

    init {
        adminsRef.addSnapshotListener {snapshot, error ->
            if (error != null) return@addSnapshotListener
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