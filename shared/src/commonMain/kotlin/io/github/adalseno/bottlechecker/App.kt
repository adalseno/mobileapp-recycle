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

package io.github.adalseno.bottlechecker

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.adalseno.bottlechecker.scanner.BarcodeScanner
import io.github.adalseno.bottlechecker.ui.CheckerScreen
import io.github.adalseno.bottlechecker.ui.CheckerViewModel
import io.github.adalseno.bottlechecker.ui.theme.AppTheme

/** Entry point shared by Android and iOS. Each platform passes in its own [scanner]. */
@Composable
fun App(scanner: BarcodeScanner) {
    AppTheme {
        val viewModel = viewModel { CheckerViewModel.create() }
        CheckerScreen(viewModel, scanner)
    }
}
