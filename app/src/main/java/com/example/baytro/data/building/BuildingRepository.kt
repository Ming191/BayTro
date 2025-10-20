package com.example.baytro.data

import com.example.baytro.data.service.Service
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BuildingRepository(
    db: FirebaseFirestore
) : Repository<Building> {
    private val collection = db.collection("buildings")

    override suspend fun getAll(): List<Building> {
        val snapshot = collection.get()
        return snapshot.documents.map { doc ->
            val b = doc.data<Building>()
            b.copy(id = doc.id)
        }
    }

    override suspend fun getById(id: String): Building? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            val b = snapshot.data<Building>()
            b.copy(id = snapshot.id)
        } else null
    }

    override suspend fun add(item: Building): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    override suspend fun addWithId(id: String, item: Building) {
        collection.document(id).set(item)
    }

    override suspend fun update(id: String, item: Building) {
        collection.document(id).set(item, merge = true)
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete()
    }

    override suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }

    suspend fun getBuildingsByIds(buildingIds: List<String>): List<Building> {
        if (buildingIds.isEmpty()) return emptyList()

        return try {
            val batches = buildingIds.chunked(10)
            val buildings = mutableListOf<Building>()

            batches.forEach { batch ->
                val snapshot = collection.where {
                    FieldPath.documentId inArray batch
                }.get()

                snapshot.documents.mapNotNullTo(buildings) { doc ->
                    try {
                        val building = doc.data<Building>()
                        building.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            buildings
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getBuildingsByUserId(userId: String): List<Building> {
        val snapshot = collection.where { "userId" equalTo userId }.get()
        return snapshot.documents.mapNotNull { doc ->
            try {
                val b = doc.data<Building>()
                b.copy(id = doc.id)
            } catch (_: Exception) {
                null
            }
        }
    }

    // ==============================
    //     SERVICES SUBCOLLECTION
    // ==============================

    fun listenToBuildingServices(buildingId: String): Flow<List<Service>> {
        return collection.document(buildingId)
            .collection("services")
            .snapshots
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    try {
                        val service = doc.data<Service>()
                        service.copy(id = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                }
            }
    }

    suspend fun getServicesByBuildingId(buildingId: String): List<Service> {
        return try {
            val snapshot = collection.document(buildingId)
                .collection("services")
                .get()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val service = doc.data<Service>()
                    service.copy(id = doc.id)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun addServiceToBuilding(buildingId: String, service: Service): String {
        val docRef = collection.document(buildingId)
            .collection("services")
            .add(service)
        return docRef.id
    }

    suspend fun getServiceById(buildingId: String, serviceId: String): Service? {
        val snapshot = collection.document(buildingId)
            .collection("services")
            .document(serviceId)
            .get()

        return if (snapshot.exists) {
            val service = snapshot.data<Service>()
            service.copy(id = serviceId)
        } else null
    }

    suspend fun updateServiceInBuilding(buildingId: String, service: Service) {
        collection.document(buildingId)
            .collection("services")
            .document(service.id)
            .set(service)
    }

    suspend fun deleteServiceFromBuilding(buildingId: String, serviceId: String) {
       collection.document(buildingId)
            .collection("services")
            .document(serviceId)
            .delete()
    }
}
