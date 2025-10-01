package com.example.baytro.data.room

import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.FirebaseFirestore

class RoomRepository(
    db: FirebaseFirestore
) : Repository<Room> {
    private val collection = db.collection("rooms_test")

    override suspend fun getAll(): List<Room> {
        val snapshot = collection.get()
        return snapshot.documents.map { it.data<Room>()}
    }

    override suspend fun getById(id: String): Room? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            val room = snapshot.data<Room>()
            room.copy(id = snapshot.id)
        } else {
            null
        }
    }

    override suspend fun add(item: Room): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    override suspend fun addWithId(id: String, item: Room) {
        collection.document(id).set(item)
    }

    override suspend fun update(id: String, item: Room) {
        collection.document(id).set(item, merge = true)
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete()
    }
    override suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }

    suspend fun getRoomsByBuildingId(buildingId: String): List<Room> {
        val snapshot = collection.where { "buildingId" equalTo buildingId }.get()
        return snapshot.documents.mapNotNull { doc ->
            try {
                val room = doc.data<Room>()
                room.copy(id = doc.id)
            } catch (_: Exception) {
                null
            }
        }
    }

}