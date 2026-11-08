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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Simple barcode glyph (material-icons-core has no barcode/camera icon). */
internal val BarcodeIcon: ImageVector by lazy {
    // (x, width) of each bar in a 24×24 viewport.
    val bars = listOf(2f to 2f, 5f to 1f, 7f to 2f, 10f to 1f, 12f to 3f, 16f to 1f, 18f to 1f, 20f to 2f)
    ImageVector.Builder(
        name = "Barcode",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            for ((x, w) in bars) {
                moveTo(x, 5f)
                horizontalLineTo(x + w)
                verticalLineTo(19f)
                horizontalLineTo(x)
                close()
            }
        }
    }.build()
}
