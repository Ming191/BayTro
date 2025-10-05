package com.example.baytro.data.service

import android.util.Log
import dev.gitlive.firebase.firestore.FirebaseFirestore

/**
 * Repository cho Service collection (Firebase KMP)
 */
class ServiceRepository(
    private val db: FirebaseFirestore
) {
    private val collection = db.collection("services")

    suspend fun getAll(): List<Service> {
        val snapshot = collection.get().documents
        return snapshot.map { it.data<Service>().copy(id = it.id) }
    }

    suspend fun getById(id: String): Service? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) snapshot.data<Service>().copy(id = snapshot.id) else null
    }

    suspend fun add(item: Service): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    suspend fun addWithId(id: String, item: Service) {
        collection.document(id).set(item)
    }

    suspend fun update(id: String, item: Service) {
        collection.document(id).set(item, merge = true)
    }

    suspend fun delete(id: String) {
        collection.document(id).delete()
    }

    suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }

    suspend fun getServicesByBuildingId(buildingId: String): List<Service> {
        Log.d("ServiceRepository", "Fetching services for building $buildingId")
        val snapshot = collection.where { "buildingID" equalTo buildingId }.get()
        return snapshot.documents.mapNotNull { doc ->
            try {
                val service = doc.data<Service>()
                service.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
    }
}
