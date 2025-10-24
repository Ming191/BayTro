package com.example.baytro.navigation

import android.util.Log
import androidx.core.os.bundleOf
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.data.service.Service
import com.example.baytro.view.screens.room.RoomListScreen
import com.example.baytro.view.screens.room.AddRoomScreen
import com.example.baytro.view.screens.room.EditRoomScreen
import com.example.baytro.view.screens.room.RoomDetailsScreen
import kotlinx.serialization.json.Json

fun NavGraphBuilder.roomNavGraph(navController: NavHostController) {
    composable(
        route = Screens.RoomList.route,
        arguments = Screens.RoomList.arguments
    ) {
        RoomListScreen(navController = navController)
    }

    composable(
        route = Screens.AddRoom.route,
        arguments = Screens.AddRoom.arguments
    ) {
        AddRoomScreen(
            getNewExtraService = { lifecycleOwner, onServiceReceived ->
                val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
                savedStateHandle?.getLiveData<String>("newService")?.observe(lifecycleOwner) { json ->
                    val service = Json.decodeFromString<Service>(json)
                    onServiceReceived(service)
                    savedStateHandle.remove<String>("newService")
                }
            },
            backToRoomListScreen = {
                navController.popBackStack()
            },
            onAddServiceClick = { roomId, buildingId ->
                navController.navigate(Screens.AddService.createRoute(roomId,buildingId,true)) {
                }
            }
        )
    }

    composable(
        route = Screens.EditRoom.route,
        arguments = Screens.EditRoom.arguments
    ) {
        EditRoomScreen(
            onEditExtraServiceClick = { roomId, serviceId ->
                navController.navigate(Screens.EditService.createRouteFromRoom(roomId, serviceId))
            },
            onDeleteServiceClick = {
                navController.navigate(Screens.ServiceList.route)
            },
            onBackClick = {
                navController.popBackStack()
            }
        )
    }

    composable(
        route = Screens.RoomDetails.route,
        arguments = Screens.RoomDetails.arguments
    ) {
        RoomDetailsScreen(
            onAddServiceClick = { roomId, buildingId ->
                navController.navigate(Screens.AddService.createRoute(roomId, buildingId, false))
            },
            onAddContractClick = { roomId ->
                navController.navigate(Screens.AddContract.createRoute(roomId))
            },
            onViewContractClick = { contractId ->
                navController.navigate(Screens.ContractDetails.createRoute(contractId))
            },
            onEditRoomOnClick = { roomId ->
                navController.navigate(Screens.EditRoom.createRoute(roomId))
            },
            onEditServiceClick = { serviceId, buildingId ->
                navController.navigate(Screens.EditService.createRoute(buildingId, serviceId))
            },
            onEditExtraServiceClick = { serviceId, roomId ->
                navController.navigate(Screens.EditService.createRouteFromRoom(roomId, serviceId))
            },
            onDeleteServiceClick = { service ->
                navController.navigate(Screens.ServiceList.route)
            },
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}