package com.example.baytro.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.tooling.preview.Preview
import com.example.baytro.ui.theme.*
@Composable
fun ServiceIconFrame(
    modifier: Modifier = Modifier,
    label: String = ""
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .background(
                color = Pink80,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.take(1).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            color = Pink40
        )
    }
}


@Preview(showBackground = true)
@Composable
fun ServiceIconFramePreview() {
    ServiceIconFrame(label = "E")
}
