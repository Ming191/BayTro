package com.example.baytro.data.room

import android.util.Log
import com.example.baytro.data.service.Service
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRepository(
    db: FirebaseFirestore
) {
    private val collection = db.collection("rooms")

    companion object {
        private const val TAG = "RoomRepository"
    }

    suspend fun getAll(): List<Room> {
        return try {
            val snapshot = collection
                .where { "status" notEqualTo "ARCHIVED" }
                .get()
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
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            val b = snapshot.data<Room>()
            b.copy(id = snapshot.id)
        } else null
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
            val snapshot = collection.where {
                all(
                    "buildingId" equalTo buildingId,
                    "status" notEqualTo "ARCHIVED"
                )
            }.get()
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

    fun getRoomFlow(id: String): Flow<Room?> {
        return collection.document(id).snapshots.map { it.data<Room>() }
    }

    suspend fun getExtraServicesByRoomId(roomId: String): List<Service> {
        return try {
            val snapshot = collection.document(roomId)
                .collection("extraServices")
                .where { "status" equalTo "ACTIVE" }
                .get()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val service = doc.data<Service>()
                    service.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching extra services for room $roomId: ${e.message}")
            emptyList()
        }
    }

    fun listenToRoomExtraServices(roomId: String): Flow<List<Service>> {
        return collection.document(roomId)
            .collection("extraServices")
            .where { "status" equalTo "ACTIVE" }
            .snapshots
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val service = doc.data<Service>()
                        service.copy(id = doc.id) // nếu Service có trường id
                    } catch (_: Exception) {
                        null
                    }
                }
            }
    }

    suspend fun addExtraServiceToRoom(roomId: String, service: Service): String {
        val docRef = collection.document(roomId)
            .collection("extraServices")
            .add(service)
        return docRef.id
    }

    suspend fun getExtraServiceById(roomId: String, serviceId: String): Service? {
        val snapshot = collection.document(roomId)
            .collection("extraServices")
            .document(serviceId)
            .get()

        return if (snapshot.exists) {
            val service = snapshot.data<Service>()
            service.copy(id = serviceId)
        } else null
    }

    suspend fun updateExtraServiceInRoom(roomId: String, service: Service) {
        try {
            collection.document(roomId)
                .collection("extraServices")
                .document(service.id)
                .set(service)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating extra service in room $roomId: ${e.message}")
            throw e
        }
    }

    suspend fun removeExtraServiceFromRoom(roomId: String, serviceId: String) {
        collection.document(roomId)
            .collection("extraServices")
            .document(serviceId)
            .update("status" to "DELETE")
    }

    suspend fun hasExtraService(roomId: String, serviceId: String): Boolean {
        return try {
            val doc = collection.document(roomId)
                .collection("extraServices")
                .document(serviceId)
                .get()

            if (!doc.exists) return false

            // Check if service is active (not soft-deleted)
            val service = doc.data<Service>()
            service.status != com.example.baytro.data.service.Status.DELETE
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if room has extra service: ${e.message}")
            false
        }
    }

    /**
     * Updates room data and manages its services in a batch operation
     */
    suspend fun updateRoomAndServices(
        roomId: String,
        updatedRoomData: Room,
        servicesToAdd: List<Service>,
        servicesToUpdate: List<Service>,
        servicesToDelete: List<Service>
    ) {
        try {
            // Update room data
            update(roomId, updatedRoomData)

            // Delete services
            servicesToDelete.forEach { service ->
                removeExtraServiceFromRoom(roomId, service.id)
            }

            // Add new services
            servicesToAdd.forEach { service ->
                val serviceToAdd = service.copy(
                    id = "", // Clear temp ID
                    status = com.example.baytro.data.service.Status.ACTIVE
                )
                addExtraServiceToRoom(roomId, serviceToAdd)
            }

            // Update existing services
            servicesToUpdate.forEach { service ->
                updateExtraServiceInRoom(roomId, service)
            }

            Log.d(TAG, "Successfully updated room $roomId and its services")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating room and services: ${e.message}")
            throw e
        }
    }
}