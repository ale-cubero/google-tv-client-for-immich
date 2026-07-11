package com.immichtv.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun SetupScreen(
    onConfigured: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val urlFocus = remember { FocusRequester() }

    // Overscan-safe padding (~5%).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 96.dp, vertical = 54.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.width(640.dp),
        ) {
            Text(
                text = "Connect to Immich",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            TvTextField(
                label = "Server URL",
                value = state.url,
                onValueChange = viewModel::onUrlChange,
                keyboardType = KeyboardType.Uri,
                modifier = Modifier.focusRequester(urlFocus),
            )
            TvTextField(
                label = "API Key",
                value = state.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                keyboardType = KeyboardType.Password,
                mask = true,
            )

            InsecureToggle(
                checked = state.allowInsecure,
                onToggle = { viewModel.onInsecureChange(!state.allowInsecure) },
            )

            Button(
                onClick = { viewModel.validateAndSave(onConfigured) },
                enabled = !state.validating,
            ) {
                Text(if (state.validating) "Validating…" else "Connect")
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFFF6E6E),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { urlFocus.requestFocus() }
}

/** D-pad-focusable checkbox row for opting into an insecure (self-signed / http://) connection. */
@Composable
private fun InsecureToggle(checked: Boolean, onToggle: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val borderColor =
        if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.border
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onToggle) // handles D-pad OK when focused
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = if (checked) "☑" else "☐",
            color = if (checked) MaterialTheme.colorScheme.primary else Color(0xFF9AA4AD),
            fontSize = 26.sp,
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = "Allow insecure connection",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Trust self-signed certificates and allow http:// — for self-hosted servers without a valid certificate. The API key may travel unencrypted.",
                color = Color(0xFF9AA4AD),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** Minimal D-pad-focusable single-line text field (tv-material has no TextField). */
@Composable
private fun TvTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    mask: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val borderColor =
        if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.border

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelLarge,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            // Mask the API key so it isn't exposed on screen / in screenshots.
            visualTransformation = if (mask) PasswordVisualTransformation() else VisualTransformation.None,
            textStyle = TextStyle(color = Color.White, fontSize = 20.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Next,
            ),
            modifier = modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                // A focused BasicTextField swallows D-pad up/down on TV; explicitly release
                // vertical focus so the remote can move between fields and the button.
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Down)
                        Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Up)
                        else -> false
                    }
                }
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                )
                .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(PaddingValues(horizontal = 16.dp, vertical = 14.dp)),
        )
    }
}
