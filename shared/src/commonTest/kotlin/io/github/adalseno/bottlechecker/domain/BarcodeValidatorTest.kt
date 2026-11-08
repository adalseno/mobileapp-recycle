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

import io.github.adalseno.bottlechecker.domain.BarcodeValidator.InvalidReason
import io.github.adalseno.bottlechecker.domain.BarcodeValidator.Result
import kotlin.test.Test
import kotlin.test.assertEquals

class BarcodeValidatorTest {

    private fun invalid(reason: InvalidReason) = Result.Invalid(reason)

    @Test
    fun validEan13() = assertEquals(Result.Valid("5449000000996"), BarcodeValidator.validate("5449000000996"))

    @Test
    fun whitespaceIsIgnored() =
        assertEquals(Result.Valid("5449000000996"), BarcodeValidator.validate(" 5449 000000 996 "))

    @Test
    fun validEan8() = assertEquals(Result.Valid("96385074"), BarcodeValidator.validate("96385074"))

    @Test
    fun validUpcA() = assertEquals(Result.Valid("036000291452"), BarcodeValidator.validate("036000291452"))

    @Test
    fun validUpcE() {
        // UPC-E 04252614 expands to UPC-A 042100005264.
        assertEquals("042100005264", BarcodeValidator.expandUpcE("04252614"))
        assertEquals(Result.Valid("04252614"), BarcodeValidator.validate("04252614"))
    }

    @Test
    fun upcEExpansionRules() {
        assertEquals("01200000345", BarcodeValidator.expandUpcE("01234506")?.take(11))
        assertEquals("01230000045", BarcodeValidator.expandUpcE("01234537")?.take(11))
        assertEquals("01234000005", BarcodeValidator.expandUpcE("01234548")?.take(11))
        assertEquals("01234500005", BarcodeValidator.expandUpcE("01234555")?.take(11))
        assertEquals(null, BarcodeValidator.expandUpcE("21234555"))
    }

    @Test
    fun empty() = assertEquals(invalid(InvalidReason.Empty), BarcodeValidator.validate("  "))

    @Test
    fun letters() = assertEquals(invalid(InvalidReason.NotDigits), BarcodeValidator.validate("54490000009A6"))

    @Test
    fun wrongLength() = assertEquals(invalid(InvalidReason.WrongLength), BarcodeValidator.validate("123456789"))

    @Test
    fun badCheckDigit() {
        // The made-up code used in PLAN.md fails the check digit (it should end in 8).
        assertEquals(invalid(InvalidReason.BadCheckDigit), BarcodeValidator.validate("1234567890123"))
        assertEquals(Result.Valid("1234567890128"), BarcodeValidator.validate("1234567890128"))
    }
}
