package com.example.baytro.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.baytro.data.service.Service

@Composable
fun ServiceActionButton(
    icon: ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                color = backgroundColor.copy(alpha = 0.2f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor
        )
    }
}

@Composable
fun ServiceCard(
    service: Service,
    onEdit: (Service) -> Unit,
    onDelete: (Service) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ServiceIconFrame(
                    label = service.name.take(1)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = service.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = service.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ServiceActionButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit",
                    backgroundColor = MaterialTheme.colorScheme.tertiary,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    onClick = { onEdit(service) }
                )
                ServiceActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete",
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    iconColor = MaterialTheme.colorScheme.primary,
                    onClick = { onDelete(service) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Price: ${service.price} VND/${service.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ServiceCardPreview() {
    val mockService = Service(
        id = "1",
        name = "Electricity",
        description = "Based on meter reading",
        price = "4.000 VND",
        unit = "kWh",
        icon = "electricity",
        buildingID = "1"
    )

    ServiceCard(
        service = mockService,
        onEdit = {},
        onDelete = {}
    )
}
