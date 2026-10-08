package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haptix.app.ui.components.motion.bounceClick
import com.haptix.app.ui.theme.HaptiXBodyMedium
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.LocalReducedMotion
import com.haptix.app.ui.theme.MotionTokens
import com.haptix.app.ui.theme.TechnicalMicroLabel
import com.haptix.app.ui.theme.rememberUiHaptics

/**
 * Custom Obsidian Selection Chip.
 *
 * Inactive: #0D1118 surface with hairline border.
 * Active: cyan-tinted surface, illuminated cyan border, spring scale.
 */
@Composable
fun HaptiXChoiceChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiHaptics = rememberUiHaptics()
    val reducedMotion = LocalReducedMotion.current
    val colors = HaptiXThemeTokens.colors
    val shape = HaptiXShapeTokens.input

    val targetBg = if (isSelected) colors.focusSurface else colors.surface
    val targetBorder = if (isSelected) colors.accentPrimary else colors.borderSubtle

    val animatedBg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = MotionTokens.colorSpring(),
        label = "chipBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = targetBorder,
        animationSpec = MotionTokens.colorSpring(),
        label = "chipBorder"
    )

    val targetScale = if (isSelected && !reducedMotion) 1.02f else 1.0f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = MotionTokens.Interactive,
        label = "chipScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minWidth = 84.dp, minHeight = 44.dp)
            .clip(shape)
            .background(animatedBg)
            .border(1.dp, animatedBorder, shape)
            .bounceClick(
                role = Role.RadioButton,
                onClick = {
                    uiHaptics.selection()
                    onClick()
                }
            )
            .semantics {
                this.role = Role.RadioButton
                this.selected = isSelected
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = HaptiXBodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp
            ),
            color = if (isSelected) colors.textPrimary else colors.textSecondary
        )
    }
}

/**
 * Demographic choice group (e.g. Gender options).
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
    val colors = HaptiXThemeTokens.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = TechnicalMicroLabel,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                val isSelected = option.equals(selectedOption, ignoreCase = true)
                HaptiXChoiceChip(
                    text = option,
                    isSelected = isSelected,
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}

/**
 * Obsidian styled dropdown selector for options (e.g. Favorite Genre).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaptiXDropdown(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "SELECT OPTION"
) {
    var expanded by remember { mutableStateOf(false) }
    val uiHaptics = rememberUiHaptics()
    val colors = HaptiXThemeTokens.colors
    val shape = HaptiXShapeTokens.input

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = TechnicalMicroLabel,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                uiHaptics.tap()
                expanded = it
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .clip(shape)
                    .background(colors.surface)
                    .border(
                        1.dp,
                        if (expanded) colors.accentPrimary else colors.borderSubtle,
                        shape
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedOption.isNotBlank()) selectedOption else placeholder,
                        style = HaptiXBodyMedium,
                        color = if (selectedOption.isNotBlank()) colors.textPrimary else colors.textTertiary
                    )
                    Text(
                        text = if (expanded) "▲" else "▼",
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(colors.elevated)
                    .border(1.dp, colors.borderSubtle, HaptiXShapeTokens.card)
            ) {
                options.forEach { option ->
                    val isSelected = option.equals(selectedOption, ignoreCase = true)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                style = HaptiXBodyMedium,
                                color = if (isSelected) colors.accentPrimary else colors.textPrimary
                            )
                        },
                        onClick = {
                            uiHaptics.selection()
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
