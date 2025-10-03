package com.example.baytro.data.contract

import com.example.baytro.data.Repository
import dev.gitlive.firebase.firestore.FirebaseFirestore

class ContractRepository(
    private val db : FirebaseFirestore
) : Repository<Contract> {
    private val collection = db.collection("contracts")

    override suspend fun getAll(): List<Contract> {
        val snapshot = collection.get()
        return snapshot.documents.map { it.data<Contract>()}
    }

    override suspend fun getById(id: String): Contract? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            val contract = snapshot.data<Contract>()
            contract.copy(id = snapshot.id)
        } else {
            null
        }
    }

    override suspend fun add(item: Contract): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    override suspend fun addWithId(id: String, item: Contract) {
        collection.document(id).set(item)
    }

    override suspend fun update(id: String, item: Contract) {
        collection.document(id).set(item, merge = true)
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete()
    }
    override suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }
}