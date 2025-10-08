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
    ) { entry ->
        val buildingId = entry.arguments?.getString(Screens.ARG_BUILDING_ID) ?: ""
        RoomListScreen(
            navController = navController
        )
    }

    composable(
        route = Screens.AddRoom.route,
        arguments = Screens.AddRoom.arguments
    ) { entry ->
        val buildingId = entry.arguments?.getString(Screens.ARG_BUILDING_ID) ?: ""
        Log.d("AddRoomNav", "BuildingIdInNav: $buildingId")
        AddRoomScreen(
            navController = navController,
            buildingId = buildingId
        )
    }

    composable(
        route = Screens.EditRoom.route,
        arguments = Screens.EditRoom.arguments
    ) { backStackEntry ->
        val roomId = backStackEntry.arguments?.getString(Screens.ARG_ROOM_ID) ?: ""
        EditRoomScreen(
            navController = navController
        )
    }

    composable(
        route = Screens.RoomDetails.route,
        arguments = Screens.RoomDetails.arguments
    ) { backStackEntry ->
        val roomId = backStackEntry.arguments?.getString(Screens.ARG_ROOM_ID) ?: ""
        RoomDetailsScreen(
            navController = navController
        )
    }
}

