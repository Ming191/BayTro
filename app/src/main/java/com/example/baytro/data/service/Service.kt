package com.example.baytro.data.service

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    @kotlinx.serialization.Transient
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: String = "",
    val unit: String = "",
    val icon: String = "",
    val buildingID: String = ""
)