package org.atriumlib.samples.mocha

import ch.tutteli.atrium.api.fluent.en_GB.*
import ch.tutteli.atrium.api.verbs.expect
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic._logic
import ch.tutteli.atrium.logic.createDescriptiveAssertion
import ch.tutteli.atrium.reporting.Text
import ch.tutteli.atrium.translations.DescriptionBasic.IS
import kotlin.test.Test

class SampleJsTest {
    @Test
    fun toEqual() {
        expect(1).toEqual(1)
    }

    @Test
    fun expectAnExceptionOccurred() {
        expect {
            throw IllegalArgumentException()
        }.toThrow<IllegalArgumentException>()
    }


    @Test
    fun expectAnExceptionWithAMessageOccurred() {
        expect {
            throw IllegalArgumentException("oho... hello btw")
        }.toThrow<IllegalArgumentException> {
            messageToContain("hello")
        }
    }

    @Test
    fun useOwnFunction() {
        // isEven is defined in the common module
        expect(2).toBeEven()
    }
}

fun Expect<Int>.toBeEven() =
    //TODO 0.19.0 replace with _logic.createAndAppend(IS, Text("an even number")) { it % 2 == 0 }
    _logic.append(_logic.createDescriptiveAssertion(IS, Text("an even number")) { it % 2 == 0 })
