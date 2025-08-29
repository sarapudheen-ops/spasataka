package com.spacetec.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        
        var currentRow = 0
        var currentRowWidth = 0
        var currentRowMaxHeight = 0
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()
        val placeableRows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        placeableRows.add(mutableListOf())
        
        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width <= constraints.maxWidth) {
                placeableRows[currentRow].add(placeable)
                currentRowWidth += placeable.width
                currentRowMaxHeight = maxOf(currentRowMaxHeight, placeable.height)
            } else {
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowMaxHeight)
                currentRow++
                placeableRows.add(mutableListOf())
                placeableRows[currentRow].add(placeable)
                currentRowWidth = placeable.width
                currentRowMaxHeight = placeable.height
            }
        }
        rowWidths.add(currentRowWidth)
        rowHeights.add(currentRowMaxHeight)
        
        val totalHeight = rowHeights.sum()
        val maxWidth = rowWidths.maxOrNull() ?: 0
        
        layout(maxWidth, totalHeight) {
            var yPosition = 0
            placeableRows.forEachIndexed { rowIndex, row ->
                var xPosition = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x = xPosition, y = yPosition)
                    xPosition += placeable.width
                }
                yPosition += rowHeights[rowIndex]
            }
        }
    }
}
