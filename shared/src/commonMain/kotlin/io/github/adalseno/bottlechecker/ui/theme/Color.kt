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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Brand palette. A rebrand should only need to change the values in this file
// (and Typography in Theme.kt).
// ---------------------------------------------------------------------------

internal object BrandColors {
    val primaryLight = Color(0xFF0F766E)
    val onPrimaryLight = Color(0xFFFFFFFF)
    val primaryContainerLight = Color(0xFFB2F0E6)
    val onPrimaryContainerLight = Color(0xFF00201C)
    val secondaryLight = Color(0xFF4A635F)
    val backgroundLight = Color(0xFFF6FAF8)
    val surfaceLight = Color(0xFFF6FAF8)

    val primaryDark = Color(0xFF7FD8CB)
    val onPrimaryDark = Color(0xFF003731)
    val primaryContainerDark = Color(0xFF005048)
    val onPrimaryContainerDark = Color(0xFFB2F0E6)
    val secondaryDark = Color(0xFFB1CCC6)
    val backgroundDark = Color(0xFF0E1513)
    val surfaceDark = Color(0xFF0E1513)
}

/**
 * Colours that carry meaning (eligible / not eligible) rather than brand.
 * Kept separate so a rebrand doesn't accidentally make "not eligible" green.
 */
@Immutable
data class StatusColors(
    val eligibleContainer: Color,
    val onEligibleContainer: Color,
    val eligibleIcon: Color,
    val notEligibleContainer: Color,
    val onNotEligibleContainer: Color,
    val notEligibleIcon: Color,
)

internal val LightStatusColors = StatusColors(
    eligibleContainer = Color(0xFFD7F5DC),
    onEligibleContainer = Color(0xFF0B3D17),
    eligibleIcon = Color(0xFF1B7F35),
    notEligibleContainer = Color(0xFFFFDAD6),
    onNotEligibleContainer = Color(0xFF410002),
    notEligibleIcon = Color(0xFFBA1A1A),
)

internal val DarkStatusColors = StatusColors(
    eligibleContainer = Color(0xFF15451F),
    onEligibleContainer = Color(0xFFC4F0CC),
    eligibleIcon = Color(0xFF7EDC92),
    notEligibleContainer = Color(0xFF5C1513),
    onNotEligibleContainer = Color(0xFFFFDAD6),
    notEligibleIcon = Color(0xFFFFB4AB),
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }
