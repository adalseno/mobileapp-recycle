/*
 * Copyright 2026 adalseno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.adalseno.bottlechecker.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.adalseno.bottlechecker.data.BcrsLinks
import io.github.adalseno.bottlechecker.domain.BarcodeValidator.InvalidReason
import io.github.adalseno.bottlechecker.domain.ErrorKind
import io.github.adalseno.bottlechecker.resources.Res
import io.github.adalseno.bottlechecker.resources.*
import io.github.adalseno.bottlechecker.scanner.BarcodeScanner
import io.github.adalseno.bottlechecker.scanner.UnavailableReason
import io.github.adalseno.bottlechecker.ui.theme.LocalStatusColors
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Max content width, so tablets and iPads get a centred column instead of a stretched phone layout. */
private val MaxContentWidth = 560.dp

@Composable
fun CheckerScreen(viewModel: CheckerViewModel, scanner: BarcodeScanner) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    CheckerContent(
        state = state,
        onScan = { scope.launch { viewModel.onScanResult(scanner.scan()) } },
        onInputChange = viewModel::onInputChange,
        onCheck = viewModel::onCheckInput,
        onRetry = viewModel::onRetry,
        onFindReturnPoint = { uriHandler.openUri(BcrsLinks.RETURN_POINTS) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckerContent(
    state: CheckerUiState,
    onScan: () -> Unit,
    onInputChange: (String) -> Unit,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    onFindReturnPoint: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val loading = state.check is CheckState.Loading
    val submit = {
        focusManager.clearFocus()
        onCheck()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(stringResource(Res.string.app_name)) })
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onScan()
                    },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Icon(BarcodeIcon, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(Res.string.scan_button), style = MaterialTheme.typography.titleMedium)
                }

                state.scanNotice?.let { notice ->
                    Text(
                        text = stringResource(notice.message()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                OrDivider()

                OutlinedTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(Res.string.barcode_label)) },
                    placeholder = { Text(stringResource(Res.string.barcode_placeholder)) },
                    singleLine = true,
                    isError = state.inputError != null,
                    supportingText = state.inputError?.let { reason ->
                        { Text(stringResource(reason.message())) }
                    },
                    trailingIcon = if (state.input.isNotEmpty()) {
                        {
                            IconButton(onClick = { onInputChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = stringResource(Res.string.clear))
                            }
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth(),
                )

                FilledTonalButton(
                    onClick = submit,
                    enabled = !loading && state.input.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Text(stringResource(Res.string.check_button))
                }

                AnimatedContent(
                    targetState = state.check,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    contentKey = { it::class },
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                ) { check ->
                    ResultArea(check, onRetry)
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = onFindReturnPoint,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(Res.string.find_return_point))
                }
                Text(
                    text = stringResource(Res.string.find_return_point_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.or_type),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        HorizontalDivider(Modifier.weight(1f))
    }
}

@Composable
private fun ResultArea(check: CheckState, onRetry: () -> Unit) {
    val status = LocalStatusColors.current
    when (check) {
        CheckState.Idle -> Unit

        is CheckState.Loading -> Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            Spacer(Modifier.size(12.dp))
            Text(stringResource(Res.string.checking, check.barcode))
        }

        is CheckState.Done -> if (check.eligible) {
            ResultCard(
                icon = Icons.Filled.CheckCircle,
                iconTint = status.eligibleIcon,
                container = status.eligibleContainer,
                content = status.onEligibleContainer,
                title = stringResource(Res.string.eligible_title),
                body = stringResource(Res.string.eligible_body),
                barcode = check.barcode,
            )
        } else {
            ResultCard(
                icon = Icons.Filled.Close,
                iconTint = status.notEligibleIcon,
                container = status.notEligibleContainer,
                content = status.onNotEligibleContainer,
                title = stringResource(Res.string.not_eligible_title),
                body = stringResource(Res.string.not_eligible_body),
                barcode = check.barcode,
            )
        }

        is CheckState.Failed -> ResultCard(
            icon = Icons.Filled.Warning,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            container = MaterialTheme.colorScheme.surfaceVariant,
            content = MaterialTheme.colorScheme.onSurfaceVariant,
            title = stringResource(Res.string.error_title),
            body = stringResource(check.kind.message()),
            barcode = check.barcode,
        ) {
            OutlinedButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(Res.string.retry))
            }
        }
    }
}

@Composable
private fun ResultCard(
    icon: ImageVector,
    iconTint: Color,
    container: Color,
    content: Color,
    title: String,
    body: String,
    barcode: String,
    action: (@Composable () -> Unit)? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(64.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Text(
                text = stringResource(Res.string.barcode_shown, barcode),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
            action?.invoke()
        }
    }
}

private fun InvalidReason.message(): StringResource = when (this) {
    InvalidReason.Empty -> Res.string.error_empty
    InvalidReason.NotDigits -> Res.string.error_not_digits
    InvalidReason.WrongLength -> Res.string.error_length
    InvalidReason.BadCheckDigit -> Res.string.error_check_digit
}

private fun ErrorKind.message(): StringResource = when (this) {
    ErrorKind.Network -> Res.string.error_network
    ErrorKind.Timeout -> Res.string.error_timeout
    ErrorKind.Server -> Res.string.error_server
    ErrorKind.UnexpectedResponse -> Res.string.error_unexpected
}

private fun ScanNotice.message(): StringResource = when (this) {
    ScanNotice.Failed -> Res.string.scan_failed
    is ScanNotice.Unavailable -> when (reason) {
        UnavailableReason.NotSupported -> Res.string.scan_not_supported
        UnavailableReason.PermissionDenied -> Res.string.scan_permission_denied
        UnavailableReason.Downloading -> Res.string.scan_downloading
    }
}
