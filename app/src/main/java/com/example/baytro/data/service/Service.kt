package com.example.baytro.data.service

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val id: String = "",
    val name: String,
    val description: String,
    val price: String,
    val unit: String,
    val icon: String = "" // tên icon hoặc URL icon
)