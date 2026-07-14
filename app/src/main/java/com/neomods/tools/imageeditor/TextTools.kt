package com.neomods.tools.imageeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val textColors = listOf(
    Color.White, Color.Black, Color.Red, Color.Green, Color.Blue,
    Color.Yellow, Color.Cyan, Color.Magenta, Color(0xFFFF6B35),
    Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF14B8A6),
)

@Composable
fun TextTools(
    onAddText: (String, Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.White) }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Text", style = MaterialTheme.typography.titleSmall)

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            label = { Text("Enter text") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Color palette
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(textColors) { c ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(c)
                        .then(
                            if (selectedColor == c) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier
                        )
                        .clickable { selectedColor = c }
                )
            }
        }

        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    onAddText(textInput, selectedColor)
                    textInput = ""
                }
            },
            enabled = textInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Text Layer")
        }
    }
}
