package com.example.baytro.data.service

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Repository cho Service collection
 */
class ServiceRepository(
    private val db: FirebaseFirestore
) {
    private val collection = db.collection("services")

    suspend fun getAll(): List<Service> {
        val snapshot = collection.get().await()
        return snapshot.documents.mapNotNull { it.toObject(Service::class.java)?.copy(id = it.id) }
    }

    suspend fun getById(id: String): Service? {
        val snapshot = collection.document(id).get().await()
        return if (snapshot.exists()) {
            snapshot.toObject(Service::class.java)?.copy(id = snapshot.id)
        } else null
    }

    suspend fun add(item: Service): String {
        val docRef = collection.add(item).await()
        return docRef.id
    }

    suspend fun addWithId(id: String, item: Service) {
        collection.document(id).set(item).await()
    }

    suspend fun update(id: String, item: Service) {
        collection.document(id).set(item, SetOptions.merge()).await()
    }

    suspend fun delete(id: String) {
        collection.document(id).delete().await()
    }

    suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields).await()
    }

    // 🔑 Dùng trong ServiceListVM
    suspend fun getServicesByRoomId(roomId: String): List<Service> {
        val snapshot = collection.whereEqualTo("roomId", roomId).get().await()
        return snapshot.documents.mapNotNull { it.toObject(Service::class.java)?.copy(id = it.id) }
    }
}
