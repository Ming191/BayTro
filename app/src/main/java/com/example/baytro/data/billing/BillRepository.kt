package com.example.baytro.data.billing

import com.example.baytro.data.BuildingSummary
import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BillRepository(
    private val db: FirebaseFirestore
) : Repository<Bill> {
    private val collection = db.collection("bills")

    override suspend fun getAll(): List<Bill> {
        val snapshot = collection.get()
        return snapshot.documents.map {
            it.data<Bill>().copy(id = it.id)
        }
    }

    override suspend fun getById(id: String): Bill? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            snapshot.data<Bill>().copy(id = snapshot.id)
        } else {
            null
        }
    }

    override suspend fun add(item: Bill): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    override suspend fun addWithId(id: String, item: Bill) {
        collection.document(id).set(item)
    }

    override suspend fun update(id: String, item: Bill) {
        collection.document(id).set(item, merge = true)
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete()
    }

    override suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }

    // Listen for bills by building and month (for landlord dashboard with building filter)
    fun listenForBillsByBuildingAndMonth(
        landlordId: String,
        buildingId: String,
        month: Int,
        year: Int,
        buildings: List<BuildingSummary>
    ): Flow<List<BillSummary>> {
        return collection
            .where {
                all(
                    "landlordId" equalTo landlordId,
                    "buildingId" equalTo buildingId,
                    "month" equalTo month,
                    "year" equalTo year
                )
            }
            .orderBy("roomName", Direction.ASCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    val bill = doc.data<Bill>().copy(id = doc.id)

                    val buildingName = buildings.find { it.id == bill.buildingId }?.name ?: "Unknown Building"

                    BillSummary(
                        id = bill.id,
                        roomName = bill.roomName,
                        buildingName = buildingName,
                        totalAmount = bill.totalAmount,
                        status = bill.status,
                        month = bill.month,
                        year = bill.year,
                        paymentDueDate = bill.paymentDueDate
                    )
                }
            }
    }

    // Listen for current bill for a tenant (most recent UNPAID or OVERDUE)
    fun listenForBillsByContractAndMonth(contractId: String, month: Int, year: Int): Flow<List<BillSummary>> {
        return collection
            .where {
                all(
                    "contractId" equalTo contractId,
                    "month" equalTo month,
                    "year" equalTo year
                )
            }
            .orderBy("issuedDate", Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.data<Bill>().copy(id = doc.id).toSummary()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }

    fun listenForCurrentBillByContract(contractId: String): Flow<Bill?> {
        return collection
            .where { "contractId" equalTo contractId }
            .where { "status" inArray listOf(BillStatus.UNPAID, BillStatus.OVERDUE) }
            .orderBy("issuedDate", Direction.DESCENDING)
            .limit(1)
            .snapshots
            .map { snapshot ->
                snapshot.documents.firstOrNull()?.let { doc ->
                    doc.data<Bill>().copy(id = doc.id)
                }
            }
    }

    fun listenForBillsByTenantAndMonth(tenantId: String, month: Int, year: Int): Flow<List<BillSummary>> {
        return collection
            .where {
                all(
                    "tenantId" equalTo tenantId,
                    "month" equalTo month,
                    "year" equalTo year
                )
            }
            .orderBy("issuedDate", Direction.DESCENDING)
            .snapshots
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.data<Bill>().copy(id = doc.id).toSummary()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
    }
}
