package com.haptix.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.haptix.app.ui.theme.CleanSansFontFamily
import com.haptix.app.ui.theme.HaptiXBodyMedium
import com.haptix.app.ui.theme.HaptiXShapeTokens
import com.haptix.app.ui.theme.HaptiXThemeTokens
import com.haptix.app.ui.theme.MotionTokens
import com.haptix.app.ui.theme.TechnicalMicroLabel

/**
 * Obsidian Research Lab Input Console Field.
 *
 * Dark background, precision 12dp radius, animated cyan focus line,
 * and high-contrast error states. Bypasses generic Material TextField chrome.
 */
@Composable
fun HaptiXInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    helperText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val colors = HaptiXThemeTokens.colors

    val borderColor by animateColorAsState(
        targetValue = when {
            errorMessage != null -> colors.error
            isFocused -> colors.accentPrimary
            else -> colors.borderSubtle
        },
        animationSpec = MotionTokens.colorSpring(),
        label = "inputBorderColor"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Label
        Text(
            text = label.uppercase(),
            style = TechnicalMicroLabel,
            color = if (isFocused) colors.accentPrimary else colors.textSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Input Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(HaptiXShapeTokens.input)
                .background(colors.surface)
                .border(1.dp, borderColor, HaptiXShapeTokens.input)
                .padding(horizontal = 14.dp, vertical = if (singleLine) 13.dp else 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty() && !placeholder.isNullOrBlank()) {
                Text(
                    text = placeholder,
                    style = HaptiXBodyMedium,
                    color = colors.textTertiary
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = TextStyle(
                    fontFamily = CleanSansFontFamily,
                    fontSize = HaptiXBodyMedium.fontSize,
                    color = colors.textPrimary
                ),
                cursorBrush = SolidColor(colors.accentPrimary),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine
            )
        }

        // Subtext / Error
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                style = TechnicalMicroLabel,
                color = colors.error
            )
        } else if (helperText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                style = TechnicalMicroLabel,
                color = colors.textTertiary
            )
        }
    }
}
