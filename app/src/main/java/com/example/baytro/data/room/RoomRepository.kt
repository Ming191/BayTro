package com.example.baytro.data.room
import dev.gitlive.firebase.firestore.FirebaseFirestore

class RoomRepository(
    private val db: FirebaseFirestore
) {
    private val collection = db.collection("rooms")

    suspend fun getAll(): List<Room> {
        val snapshot = collection.get()
        return snapshot.documents.map { 
            val room = it.data<Room>()
            room.copy(id = it.id)
        }
    }

    suspend fun getById(id: String): Room? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            val room = snapshot.data<Room>()
            room.copy(id = snapshot.id)
        } else {
            null
        }
    }

    suspend fun add(item: Room): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    suspend fun update(id: String, item: Room): Boolean {
        return try {
            collection.document(id).set(item)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun delete(id: String): Boolean {
        return try {
            collection.document(id).delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRoomsByBuildingId(buildingId: String): List<Room> {
        val allRooms = getAll()
        return allRooms.filter { it.buildingId == buildingId }
    }
}
