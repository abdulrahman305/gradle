/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.com.h0tk3y.kotlin.staticObjectNotation.analysis

import com.h0tk3y.kotlin.staticObjectNotation.AccessFromCurrentReceiverOnly
import com.h0tk3y.kotlin.staticObjectNotation.Adding
import com.h0tk3y.kotlin.staticObjectNotation.Configuring
import com.h0tk3y.kotlin.staticObjectNotation.Restricted
import com.h0tk3y.kotlin.staticObjectNotation.analysis.ErrorReason
import com.h0tk3y.kotlin.staticObjectNotation.demo.resolve
import com.h0tk3y.kotlin.staticObjectNotation.schemaBuilder.schemaFromTypes
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class TopLevelForTest {
    @Configuring
    fun inner(f: HasAnnotatedMembers.() -> Unit) = Unit

    @Restricted
    val inner: HasAnnotatedMembers get() = TODO()
}

private class HasAnnotatedMembers {
    @Restricted
    @AccessFromCurrentReceiverOnly
    var x: Int = 0

    @Restricted
    var y: Int = 0

    @Adding
    @AccessFromCurrentReceiverOnly
    fun f(): Int = 0

    @Configuring
    fun nested(fn: Nested.() -> Unit) = Unit

    @Restricted
    val nested: Nested get() = TODO()
}

private class Nested {
    @Restricted
    var n = 1
}

class AccessInCurrentReceiverOnlyTest {
    val schema = schemaFromTypes(TopLevelForTest::class, listOf(TopLevelForTest::class, HasAnnotatedMembers::class, Nested::class))

    @Test
    fun `access on current receiver is allowed`() {
        val result = schema.resolve(
            """
            inner {
                x = 1
                y = 1
                f()
            }
            """.trimIndent()
        )

        assertTrue { result.errors.isEmpty() }
    }

    @Test
    fun `access on a property is not allowed`() {
        val result = schema.resolve(
            """
            inner.x = 1
            inner.y = 1
            inner.f()
            """.trimIndent()
        )

        val errors = result.errors
        assertEquals(2, errors.size)
        assertTrue { errors.all { it.errorReason is ErrorReason.AccessOnCurrentReceiverOnlyViolation } }
    }

    @Test
    fun `access on an outer receiver is not allowed`() {
        val result = schema.resolve(
            """
            inner {
                nested {
                    x = 1
                    y = 1
                    f()
                    n = 1
                }
            }
            """.trimIndent()
        )

        val errors = result.errors
        assertEquals(2, errors.size)
        assertTrue { errors.all { it.errorReason is ErrorReason.AccessOnCurrentReceiverOnlyViolation } }
    }
}
