package com.example.baytro.data.billing

import android.util.Log
import com.example.baytro.data.BuildingSummary
import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val TAG = "BillRepository"

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

    fun observeById(id: String): Flow<Bill?> {
        Log.d(TAG, "observeById: id=$id")
        return collection.document(id).snapshots.map { snapshot ->
            if (snapshot.exists) {
                val bill = snapshot.data<Bill>().copy(id = snapshot.id)
                Log.d(TAG, "observeById: Bill updated - status=${bill.status}, totalAmount=${bill.totalAmount}")
                bill
            } else {
                Log.d(TAG, "observeById: Bill not found")
                null
            }
        }
    }

    // Listen for bills by building and month (for landlord dashboard with building filter)
    fun listenForBillsByBuildingAndMonth(
        landlordId: String,
        buildingId: String,
        month: Int,
        year: Int,
        buildings: List<BuildingSummary>
    ): Flow<List<BillSummary>> {
        Log.d(TAG, "listenForBillsByBuildingAndMonth: landlordId=$landlordId, buildingId=$buildingId, month=$month, year=$year")
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
                Log.d(TAG, "listenForBillsByBuildingAndMonth: received ${snapshot.documents.size} documents")
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
        Log.d(TAG, "listenForBillsByContractAndMonth: contractId=$contractId, month=$month, year=$year")
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
                Log.d(TAG, "listenForBillsByContractAndMonth: received ${snapshot.documents.size} documents")
                snapshot.documents.mapNotNull { doc ->
                    try {
                        val bill = doc.data<Bill>().copy(id = doc.id)
                        Log.d(TAG, "  Bill: id=${bill.id}, contractId=${bill.contractId}, month=${bill.month}, year=${bill.year}, status=${bill.status}")
                        bill.toSummary()
                    } catch (e: Exception) {
                        Log.e(TAG, "  Error parsing bill document ${doc.id}", e)
                        null
                    }
                }
            }
    }

    fun listenForCurrentBillByContract(contractId: String): Flow<Bill?> {
        Log.d(TAG, "listenForCurrentBillByContract: contractId=$contractId")
        return collection
            .where { "contractId" equalTo contractId }
            .where { "status" inArray listOf(BillStatus.UNPAID, BillStatus.OVERDUE) }
            .orderBy("issuedDate", Direction.DESCENDING)
            .limit(1)
            .snapshots
            .map { snapshot ->
                Log.d(TAG, "listenForCurrentBillByContract: received ${snapshot.documents.size} documents")
                snapshot.documents.firstOrNull()?.let { doc ->
                    val bill = doc.data<Bill>().copy(id = doc.id)
                    Log.d(TAG, "  Current bill: id=${bill.id}, status=${bill.status}, month=${bill.month}, year=${bill.year}")
                    bill
                }
            }
    }

}