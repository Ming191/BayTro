package com.example.baytro.view.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.baytro.data.room.Room

@Composable
fun RoomSelectionList(
    rooms: List<Room>,
    selectedRooms: Set<String>,
    onRoomToggle: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rooms) { room ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Room icon",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Room ${room.roomNumber}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Checkbox(
                    checked = selectedRooms.contains(room.id),
                    onCheckedChange = { onRoomToggle(room.id) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RoomSelectionListPreview() {
    val rooms = listOf(
        Room("1", "101", "101"),
        Room("2", "102", "102"),
        Room("3", "103", "103"),
        Room("4", "201", "201"),
        Room("5", "202", "202"),
        Room("6", "203", "203"),
        Room("7", "301", "301"),
        Room("8", "302", "302"),
        Room("9", "303", "303")
    )
    RoomSelectionList(
        rooms = rooms,
        selectedRooms = setOf("1", "2", "4"), // các phòng đang được tick
        onRoomToggle = {}
    )
}
