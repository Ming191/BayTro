package com.example.baytro.data

import com.example.baytro.data.service.Service
import dev.gitlive.firebase.firestore.Direction
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
        if (buildingIds.isEmpty()) {
            return emptyList()
        }

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

    suspend fun getBuildingSummariesByLandlord(landlordId: String): List<BuildingSummary> {
        val snapshot = collection
            .where {
                all(
                    "userId" equalTo landlordId,
                    "status" equalTo "ACTIVE"
                )
            }
            .orderBy("name", Direction.ASCENDING)
            .get()

        return snapshot.documents.map { doc ->
            doc.data<Building>().copy(id = doc.id).toSummary()
        }
    }


    // Get buildings by user ID (landlord)
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

    // Listen to real-time updates for building services
    fun listenToBuildingServices(buildingId: String): Flow<List<Service>> {
        return collection.document(buildingId).snapshots.map { snapshot ->
            if (snapshot.exists) {
                val building = snapshot.data<Building>()
                building.services
            } else {
                emptyList()
            }
        }
    }

    suspend fun getServicesByBuildingId(buildingId: String): List<Service> {
        val snapshot = collection.document(buildingId).get()
        return if (snapshot.exists) {
            val building = snapshot.data<Building>()
            building.services
        } else {
            emptyList()
        }
    }
}