package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.theme.EditorialMetadataLabel
import com.haptix.app.ui.theme.LocalSpacing
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.TechnicalValueLabel


/**
 * Clean Apple-style selection pills for demographic options (e.g. Gender).
 * Features rounded geometry, filled subtle surfaces, and large 48dp touch targets.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HaptiXChoiceGroup(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                val isSelected = option.equals(selectedOption, ignoreCase = true)

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(150),
                    label = "chipBg"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = tween(150),
                    label = "chipText"
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 32.dp),
                            role = Role.RadioButton,
                            onClick = { onOptionSelected(option) }
                        )
                        .semantics {
                            this.role = Role.RadioButton
                            this.selected = isSelected
                            this.contentDescription = "$label: $option"
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


/**
 * Precision 1 to 5 scale selector for research metrics with 14dp rounded cards.
 */
@Composable
fun LikertScaleSelector(
    rating: Int,
    onRatingSelected: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLabel: String = "1 (Low)",
    maxLabel: String = "5 (High)"
) {
    val spacing = LocalSpacing.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = TechnicalMicroLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            (1..5).forEach { value ->
                val isSelected = rating == value

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    animationSpec = tween(200),
                    label = "likertBg"
                )

                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    animationSpec = tween(200),
                    label = "likertContent"
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    },
                    animationSpec = tween(200),
                    label = "likertBorder"
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, borderColor),
                    color = backgroundColor,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.RadioButton) { onRatingSelected(value) }
                        .semantics {
                            this.role = Role.RadioButton
                            this.selected = isSelected
                            this.contentDescription = "Rating $value out of 5"
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "0$value",
                            style = TechnicalValueLabel.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = contentColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.xs))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = minLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = maxLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Standard accessible dropdown selection component with Apple-inspired styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaptiXDropdown(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Select option..."
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedOption.ifEmpty { "" },
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

