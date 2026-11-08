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

package io.github.adalseno.bottlechecker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors = lightColorScheme(
    primary = BrandColors.primaryLight,
    onPrimary = BrandColors.onPrimaryLight,
    primaryContainer = BrandColors.primaryContainerLight,
    onPrimaryContainer = BrandColors.onPrimaryContainerLight,
    secondary = BrandColors.secondaryLight,
    background = BrandColors.backgroundLight,
    surface = BrandColors.surfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = BrandColors.primaryDark,
    onPrimary = BrandColors.onPrimaryDark,
    primaryContainer = BrandColors.primaryContainerDark,
    onPrimaryContainer = BrandColors.onPrimaryContainerDark,
    secondary = BrandColors.secondaryDark,
    background = BrandColors.backgroundDark,
    surface = BrandColors.surfaceDark,
)

/** Default Material typography; swap in a brand font family here if needed. */
private val AppTypography = Typography()

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
