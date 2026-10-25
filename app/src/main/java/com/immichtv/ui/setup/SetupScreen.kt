/*
 * TV client for Immich — a read-only Android TV client for a self-hosted Immich server.
 * Copyright (C) 2026 Alejandro Cubero
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import com.immichtv.ui.common.brandButtonColors
import com.immichtv.ui.theme.AppColors
import com.immichtv.ui.theme.AppDimens

@Composable
fun SetupScreen(
    onConfigured: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val str = com.immichtv.ui.i18n.LocalStrings.current
    val urlFocus = remember { FocusRequester() }
    val connectFocus = remember { FocusRequester() }
    var showKey by remember { mutableStateOf(false) }

    // Overscan-safe padding, shared with the rest of the app (~5% at TV density).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = AppDimens.overscanH, vertical = AppDimens.overscanV),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.width(640.dp),
        ) {
            Text(
                text = str.setupTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )

            TvTextField(
                label = str.setupServerUrl,
                value = state.url,
                onValueChange = viewModel::onUrlChange,
                keyboardType = KeyboardType.Uri,
                modifier = Modifier.focusRequester(urlFocus),
            )
            TvTextField(
                label = str.setupApiKey,
                value = state.apiKey,
                onValueChange = viewModel::onApiKeyChange,
                keyboardType = KeyboardType.Password,
                mask = !showKey,
                imeAction = ImeAction.Done,
            )

            ToggleRow(
                label = str.setupShowKey,
                checked = showKey,
                onToggle = { showKey = !showKey },
            )

            ToggleRow(
                label = str.setupAllowHttp,
                subtitle = str.setupAllowHttpSubtitle,
                checked = state.allowHttp,
                onToggle = { viewModel.onAllowHttpChange(!state.allowHttp) },
            )

            Button(
                onClick = { viewModel.validateAndSave(onConfigured) },
                enabled = !state.validating,
                colors = brandButtonColors(),
                modifier = Modifier.focusRequester(connectFocus),
            ) {
                Text(if (state.validating) str.setupValidating else str.setupConnect)
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = AppColors.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }

    state.certPrompt?.let { prompt ->
        CertTrustDialog(
            prompt = prompt,
            onTrust = { viewModel.trustCertificate(onConfigured) },
            onReject = viewModel::rejectCertificate,
        )
    }
    // When the certificate dialog closes, return focus to Connect (not the first text field, which
    // would pop the on-screen keyboard over the result message).
    var promptWasShown by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(state.certPrompt) {
        if (state.certPrompt != null) {
            promptWasShown = true
        } else if (promptWasShown) {
            promptWasShown = false
            runCatching { connectFocus.requestFocus() }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) { urlFocus.requestFocus() }
}

/**
 * Shown when the platform rejects the server's certificate: what the certificate is, its SHA-256
 * fingerprint to compare against the server, and an explicit choice. Focus starts on "Don't trust"
 * so trusting is always a deliberate step; Back also rejects. A real dialog window (not an overlay)
 * so D-pad focus can't wander to the setup controls behind it.
 */
@Composable
private fun CertTrustDialog(prompt: CertPrompt, onTrust: () -> Unit, onReject: () -> Unit) {
    val str = com.immichtv.ui.i18n.LocalStrings.current
    val rejectFocus = remember { FocusRequester() }
    var dialogHasFocus by remember { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onReject, // Back
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .onFocusChanged { dialogHasFocus = it.hasFocus }
                .background(AppColors.panel, RoundedCornerShape(16.dp))
                .border(1.dp, AppColors.outline, RoundedCornerShape(16.dp))
                .padding(horizontal = 36.dp, vertical = 30.dp),
        ) {
            Text(
                text = str.certTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = str.certIntro(prompt.host),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary,
            )
            CertField(str.certSubject, prompt.subject)
            CertField(str.certIssuer, prompt.issuer)
            CertField(str.certValidUntil, prompt.validUntil)
            CertField(str.certFingerprint, prompt.fingerprint, monospace = true)
            Text(
                text = str.certWarning,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 6.dp)) {
                Button(
                    onClick = onReject,
                    colors = brandButtonColors(),
                    modifier = Modifier.focusRequester(rejectFocus),
                ) { Text(str.certCancel) }
                OutlinedButton(onClick = onTrust) { Text(str.certTrust) }
            }
        }
    }
    // Pull focus into the dialog (onto "Don't trust"). A single request can land before the buttons
    // are attached — e.g. while the Connect button behind is still settling after validation — so
    // retry until the dialog actually holds focus; never fight focus already inside it.
    androidx.compose.runtime.LaunchedEffect(prompt) {
        repeat(20) {
            if (dialogHasFocus) return@LaunchedEffect
            runCatching { rejectFocus.requestFocus() }
            kotlinx.coroutines.delay(30)
        }
    }
}

@Composable
private fun CertField(label: String, value: String, monospace: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = AppColors.textSecondary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.let {
                if (monospace) it.copy(fontFamily = FontFamily.Monospace) else it
            },
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** D-pad-focusable checkbox row (OK toggles). */
@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    subtitle: String? = null,
) {
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
        Icon(
            imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else AppColors.textSecondary,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.padding(start = 14.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = AppColors.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
    imeAction: ImeAction = ImeAction.Next,
) {
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
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
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            // IME action hops to the next field / closes the keyboard, so the remote's "Next"/"Done"
            // key behaves as expected instead of leaving the keyboard up to swallow D-pad presses.
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
                onDone = { keyboard?.hide(); focusManager.moveFocus(FocusDirection.Down) },
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
