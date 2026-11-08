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

package io.github.adalseno.bottlechecker.domain

/**
 * Checks a barcode before it is sent to the server, so obvious typos and misreads are
 * caught locally. Accepts EAN-8, UPC-E (8 digits), UPC-A (12) and EAN-13 (13).
 */
object BarcodeValidator {

    sealed interface Result {
        data class Valid(val code: String) : Result
        data class Invalid(val reason: InvalidReason) : Result
    }

    enum class InvalidReason { Empty, NotDigits, WrongLength, BadCheckDigit }

    private val validLengths = setOf(8, 12, 13)

    fun validate(raw: String): Result {
        val code = raw.filterNot { it.isWhitespace() }
        return when {
            code.isEmpty() -> Result.Invalid(InvalidReason.Empty)
            !code.all { it in '0'..'9' } -> Result.Invalid(InvalidReason.NotDigits)
            code.length !in validLengths -> Result.Invalid(InvalidReason.WrongLength)
            !hasValidCheckDigit(code) -> Result.Invalid(InvalidReason.BadCheckDigit)
            else -> Result.Valid(code)
        }
    }

    private fun hasValidCheckDigit(code: String): Boolean = when (code.length) {
        // 8 digits can be either EAN-8 or UPC-E; UPC-E's check digit is computed on its UPC-A expansion.
        8 -> isValidGtin(code) || expandUpcE(code)?.let(::isValidGtin) == true
        else -> isValidGtin(code)
    }

    /** Standard GTIN mod-10 check (EAN-8, UPC-A, EAN-13). [code] must be all digits. */
    internal fun isValidGtin(code: String): Boolean {
        val digits = code.map { it - '0' }
        val sum = digits.dropLast(1)
            .asReversed()
            .mapIndexed { i, d -> if (i % 2 == 0) d * 3 else d }
            .sum()
        return (10 - sum % 10) % 10 == digits.last()
    }

    /** Expands an 8-digit UPC-E code to its 12-digit UPC-A form, or null if it isn't UPC-E. */
    internal fun expandUpcE(code: String): String? {
        if (code.length != 8) return null
        val ns = code[0]
        if (ns != '0' && ns != '1') return null
        val d = code.substring(1, 7)
        val check = code[7]
        val body = when (d[5]) {
            '0', '1', '2' -> "${d[0]}${d[1]}${d[5]}0000${d[2]}${d[3]}${d[4]}"
            '3' -> "${d[0]}${d[1]}${d[2]}00000${d[3]}${d[4]}"
            '4' -> "${d[0]}${d[1]}${d[2]}${d[3]}00000${d[4]}"
            else -> "${d[0]}${d[1]}${d[2]}${d[3]}${d[4]}0000${d[5]}"
        }
        return "$ns$body$check"
    }
}
