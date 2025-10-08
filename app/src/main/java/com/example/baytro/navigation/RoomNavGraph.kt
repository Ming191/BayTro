package com.example.baytro.navigation

import android.util.Log
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
        AddRoomScreen(navController = navController)
    }

    composable(
        route = Screens.EditRoom.route,
        arguments = Screens.EditRoom.arguments
    ) {
        EditRoomScreen(navController = navController)
    }

    composable(
        route = Screens.RoomDetails.route,
        arguments = Screens.RoomDetails.arguments
    ) {
        RoomDetailsScreen(navController = navController)
    }
}

