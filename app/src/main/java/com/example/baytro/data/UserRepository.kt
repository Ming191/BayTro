package com.example.baytro.data

import dev.gitlive.firebase.firestore.FirebaseFirestore


class UserRepository(
    db: FirebaseFirestore
) : Repository<User> {
    private val collection = db.collection("users")

    override suspend fun getAll(): List<User> {
        val snapshot = collection.get()
        return snapshot.documents.map { it.data<User>()}
    }

    override suspend fun getById(id: String): User? {
        val snapshot = collection.document(id).get()
        return if (snapshot.exists) {
            val user = snapshot.data<User>()
            user.copy(id = snapshot.id)
        } else {
            null
        }
    }

    override suspend fun add(item: User): String {
        val docRef = collection.add(item)
        return docRef.id
    }

    override suspend fun addWithId(id: String, item: User) {
        collection.document(id).set(item)
    }

    override suspend fun update(id: String, item: User) {
        collection.document(id).set(item, merge = true)
    }

    override suspend fun delete(id: String) {
        collection.document(id).delete()
    }
    override suspend fun updateFields(id: String, fields: Map<String, Any?>) {
        collection.document(id).update(fields)
    }

    suspend fun updateUserProfileImageUrl(userId: String, imageUrl: String) {
        try {
            val userDocRef = collection.document(userId)
            userDocRef.update("profileImgUrl" to imageUrl)
        } catch (e: Exception) {
            throw Exception("Failed to update user profile in database.", e)
        }
    }
}