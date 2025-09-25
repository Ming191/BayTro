package com.example.baytro.data

interface Repository<T : Any> {
    suspend fun getAll(): List<T>
    suspend fun getById(id: String): T?
    suspend fun add(item: T): String
    suspend fun addWithId(id: String, item: T)
    suspend fun update(id: String, item: T)
    suspend fun updateFields(id: String, fields: Map<String, Any?>)
    suspend fun delete(id: String)
}