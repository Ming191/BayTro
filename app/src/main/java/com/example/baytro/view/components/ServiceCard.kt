package com.example.baytro.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baytro.data.service.Service
import com.example.baytro.data.service.Status
import com.example.baytro.utils.Utils

@Composable
fun ServiceIconFrame(iconName: String) {
    val icon = mapNameToIcon(iconName)
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$iconName icon",
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun StatusBadge(status: Status, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor) = when (status) {
        Status.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        Status.INACTIVE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color.Transparent to Color.Transparent
    }

    if (status == Status.ACTIVE || status == Status.INACTIVE) {
        Box(
            modifier = modifier
                .background(backgroundColor, RoundedCornerShape(16))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = status.name.lowercase().replaceFirstChar { it.uppercase() },
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}


@Composable
fun ServiceActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ServiceCard(
    service: Service,
    onEdit: ((Service) -> Unit)?,
    onDelete: ((Service) -> Unit)?
) {
    OutlinedCard (
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Icon
            ServiceIconFrame(iconName = service.name.lowercase())

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Tên và giá
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Price: ${Utils.formatCurrency(service.price.toString())}/${service.metric.toString().lowercase()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
            ) {
                StatusBadge(status = service.status)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onEdit != null) {
                        ServiceActionButton(
                            icon = Icons.Filled.Edit,
                            contentDescription = "Edit Service",
                            onClick = { onEdit(service) }
                        )
                    }
                    if (onDelete != null && !service.isDefault) {
                        ServiceActionButton(
                            icon = Icons.Filled.Delete,
                            contentDescription = "Delete Service",
                            onClick = { onDelete(service) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun mapNameToIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "water" -> Icons.Default.WaterDrop
        "electricity" -> Icons.Default.ElectricBolt
        "wifi" -> Icons.Default.Wifi
        "gas" -> Icons.Default.LocalFireDepartment
        "internet" -> Icons.Default.Router
        else -> Icons.Default.MiscellaneousServices
    }
}