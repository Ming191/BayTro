package com.example.baytro.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val serviceIconMap: Map<String, ImageVector> = mapOf(
    "Electricity" to Icons.Filled.Bolt,
    "Water" to Icons.Filled.WaterDrop,
    "Internet" to Icons.Filled.Wifi,
    "Cleaning" to Icons.Filled.CleaningServices,
    "Gas" to Icons.Filled.LocalGasStation,
    "Parking" to Icons.Filled.LocalParking,
    "Security" to Icons.Filled.Security,
    "Trash" to Icons.Filled.Delete,
    "TV" to Icons.Filled.Tv,
    "Maintenance" to Icons.Filled.Build,
    "Other" to Icons.AutoMirrored.Filled.Help
)
@Composable
fun ServiceIconFrame(
    modifier: Modifier = Modifier,
    serviceName: String = ""
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        val icon = serviceIconMap[serviceName] ?: serviceIconMap["Other"]
        Icon(
            imageVector = icon!!,
            contentDescription = serviceName,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}


@Preview(showBackground = true)
@Composable
fun ServiceIconFramePreview() {
    ServiceIconFrame(serviceName = "Gas")
}
