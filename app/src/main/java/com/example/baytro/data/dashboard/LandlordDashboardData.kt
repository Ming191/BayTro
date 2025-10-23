package com.example.baytro.data.dashboard

data class LandlordDashboardData(
    val username: String = "",
    val totalPendingActions: Int = 0,
    val pendingReadingsCount: Int = 0,
    val newJoinRequestsCount: Int = 0,
    val overdueBillsCount: Int = 0,
    val totalRevenueThisMonth: Double = 0.0,
    val totalUnpaidAmount: Double = 0.0,
    val totalOccupancyRate: Double = 0.0,
    val occupiedRoomCount: Int = 0,
    val totalRoomCount: Int = 0,
    val monthlyRevenueHistory: List<RevenueDataPoint> = emptyList()
)

data class RevenueDataPoint(
    val month: String = "",
    val revenue: Double = 0.0
)