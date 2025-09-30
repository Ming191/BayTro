package com.example.baytro.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.baytro.data.user.User

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TenantsSection(tenants: List<User>, onAddTenantClick: () -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        maxItemsInEachRow = 3
    ) {
        tenants.forEach { tenant ->
            TenantItem(tenant = tenant)
        }
        AddTenantButton(onClick = onAddTenantClick)
    }
}
@Composable
fun TenantItem(tenant: User) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        AsyncImage(
            model = tenant.profileImgUrl,
            contentDescription = tenant.fullName,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Text(text = tenant.fullName, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@Composable
fun AddTenantButton(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Tenant", tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@Composable
fun AddFirstTenantPrompt(onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Contract is pending activation", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text("Add the first tenant to activate this contract, manage invoices, and track payments.", textAlign = TextAlign.Center)
            Button(onClick = onClick) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Invite First Tenant")
            }
        }
    }
}