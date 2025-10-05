package com.example.baytro.view.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.w3c.dom.Text
@Composable
fun ButtonComponent(
    text : String = "",
    onButtonClick : () -> Unit
) {
    Button(
        onClick = onButtonClick,
        modifier = Modifier
            .width(180.dp)
            .height(50.dp)
    ) {
        Text(text)
    }
}