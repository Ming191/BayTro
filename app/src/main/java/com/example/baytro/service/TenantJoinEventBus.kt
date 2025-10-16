package com.example.baytro.service

import kotlinx.coroutines.flow.MutableSharedFlow

object TenantJoinEventBus {
    private val _contractConfirmed = MutableSharedFlow<String>()
    val contractConfirmed = _contractConfirmed

    private val _tenantJoinRequest = MutableSharedFlow<String>()
    val tenantJoinRequest = _tenantJoinRequest

    suspend fun emitContractConfirmed(contractId: String) {
        _contractConfirmed.emit(contractId)
    }

    suspend fun emitTenantJoinRequest(contractId: String) {
        _tenantJoinRequest.emit(contractId)
    }
}