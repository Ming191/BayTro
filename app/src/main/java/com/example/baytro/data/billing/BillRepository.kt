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
    db: FirebaseFirestore
) : Repository<Bill> {
    private val collection = db.collection("bills")

    override suspend fun getAll(): List<Bill> {
        return try {
            val snapshot = collection.get()
            Log.d(TAG, "getAll: Retrieved ${snapshot.documents.size} bill documents")
            snapshot.documents.mapNotNull { doc ->
                try {
                    doc.data<Bill>().copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Error decoding bill in getAll: ${doc.id}", e)
                    Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "  Error message: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error in getAll", e)
            emptyList()
        }
    }

    override suspend fun getById(id: String): Bill? {
        return try {
            val snapshot = collection.document(id).get()
            if (snapshot.exists) {
                Log.d(TAG, "getById: Decoding bill $id")
                val bill = snapshot.data<Bill>().copy(id = snapshot.id)
                Log.d(TAG, "getById: ✓ Successfully decoded bill $id")
                bill
            } else {
                Log.d(TAG, "getById: Bill $id not found")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error in getById for bill $id", e)
            Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "  Error message: ${e.message}")
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
                try {
                    Log.d(TAG, "  Decoding bill by id: $id")
                    val bill = snapshot.data<Bill>().copy(id = snapshot.id)
                    Log.d(TAG, "  ✓ observeById: Bill updated - status=${bill.status}, totalAmount=${bill.totalAmount}")
                    bill
                } catch (e: Exception) {
                    Log.e(TAG, "✗ Error decoding bill by id: $id", e)
                    Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
                    Log.e(TAG, "  Error message: ${e.message}")
                    try {
                        val rawData = snapshot.data<Map<String, Any>>()
                        Log.e(TAG, "  Document data keys: ${rawData.keys}")
                        Log.e(TAG, "  Document data: $rawData")
                    } catch (logError: Exception) {
                        Log.e(TAG, "  Could not log document data: ${logError.message}")
                    }
                    null
                }
            } else {
                Log.d(TAG, "observeById: Bill not found")
                null
            }
        }
    }

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
                snapshot.documents.mapNotNull { doc ->
                    try {
                        Log.d(TAG, "  Decoding bill document: ${doc.id}")
                        val bill = doc.data<Bill>().copy(id = doc.id)
                        Log.d(TAG, "  ✓ Successfully decoded bill: ${doc.id}")

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
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Error decoding bill document ${doc.id}", e)
                        Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
                        Log.e(TAG, "  Error message: ${e.message}")
                        try {
                            val rawData = doc.data<Map<String, Any>>()
                            Log.e(TAG, "  Document data keys: ${rawData.keys}")
                            Log.e(TAG, "  Document data: $rawData")
                        } catch (logError: Exception) {
                            Log.e(TAG, "  Could not log document data: ${logError.message}")
                        }
                        null
                    }
                }
            }
    }

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
                    try {
                        Log.d(TAG, "  Decoding current bill document: ${doc.id}")
                        val bill = doc.data<Bill>().copy(id = doc.id)
                        Log.d(TAG, "  ✓ Successfully decoded current bill: id=${bill.id}, status=${bill.status}, month=${bill.month}, year=${bill.year}")
                        bill
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Error decoding current bill document ${doc.id}", e)
                        Log.e(TAG, "  Error type: ${e.javaClass.simpleName}")
                        Log.e(TAG, "  Error message: ${e.message}")
                        try {
                            val rawData = doc.data<Map<String, Any>>()
                            Log.e(TAG, "  Document data keys: ${rawData.keys}")
                            Log.e(TAG, "  Document data: $rawData")
                        } catch (logError: Exception) {
                            Log.e(TAG, "  Could not log document data: ${logError.message}")
                        }
                        null
                    }
                }
            }
    }
}