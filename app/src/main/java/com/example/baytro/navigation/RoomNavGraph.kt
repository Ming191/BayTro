package com.example.baytro.navigation

import android.util.Log
import androidx.core.os.bundleOf
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.example.baytro.view.screens.room.RoomListScreen
import com.example.baytro.view.screens.room.AddRoomScreen
import com.example.baytro.view.screens.room.EditRoomScreen
import com.example.baytro.view.screens.room.RoomDetailsScreen

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
            backToRoomListScreen = {
                navController.popBackStack()
            },
            onAddServiceClick = { roomId, buildingId ->
                navController.navigate(Screens.AddService.createRoute(roomId,buildingId, true)) {

                }
            }
        )
    }

    composable(
        route = Screens.EditRoom.route,
        arguments = Screens.EditRoom.arguments
    ) {
        EditRoomScreen(
            backToRoomListScreen = {
                navController.popBackStack()
            }
        )
    }

    composable(
        route = Screens.RoomDetails.route,
        arguments = Screens.RoomDetails.arguments
    ) {
        RoomDetailsScreen(
            onAddServiceClick = { roomId, buildingId, isFromAddRoom ->
                navController.navigate(Screens.AddService.createRoute(roomId, buildingId, isFromAddRoom))
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
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}