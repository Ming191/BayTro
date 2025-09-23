package com.example.baytro.view.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale


@Composable
fun Logo(
    modifier: Modifier
) {
    Image(
        painter = androidx.compose.ui.res.painterResource(id = com.example.baytro.R.drawable.logo),
        contentDescription = "Baytro Logo",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}