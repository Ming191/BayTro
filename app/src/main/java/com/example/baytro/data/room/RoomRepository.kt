package com.example.baytro.data.room

import android.util.Log
import dev.gitlive.firebase.firestore.FirebaseFirestore

class RoomRepository(
    db: FirebaseFirestore
) {
    private val collection = db.collection("rooms")

    companion object {
        private const val TAG = "RoomRepository"
    }

    suspend fun getAll(): List<Room> {
        return try {
            val snapshot = collection.get()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val room = doc.data<Room>()
                    room.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing room ${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all rooms: ${e.message}")
            emptyList()
        }
    }

    suspend fun getById(id: String): Room? {
        return try {
            val snapshot = collection.document(id).get()
            if (snapshot.exists) {
                val room = snapshot.data<Room>()
                room.copy(id = snapshot.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching room $id: ${e.message}")
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
            Log.e(TAG, "Error updating room $id: ${e.message}")
            false
        }
    }

    suspend fun delete(id: String): Boolean {
        return try {
            collection.document(id).delete()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting room $id: ${e.message}")
            false
        }
    }

    suspend fun getRoomsByBuildingId(buildingId: String): List<Room> {
        return try {
            val snapshot = collection.where { "buildingId" equalTo buildingId }.get()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val room = doc.data<Room>()
                    room.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing room ${doc.id} for building $buildingId: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching rooms for building $buildingId: ${e.message}")
            emptyList()
        }
    }
}
