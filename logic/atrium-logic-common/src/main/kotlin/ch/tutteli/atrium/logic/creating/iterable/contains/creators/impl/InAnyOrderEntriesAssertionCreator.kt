package ch.tutteli.atrium.logic.creating.iterable.contains.creators.impl

import ch.tutteli.atrium.assertions.Assertion
import ch.tutteli.atrium.assertions.AssertionGroup
import ch.tutteli.atrium.assertions.DefaultListAssertionGroupType
import ch.tutteli.atrium.assertions.DefaultSummaryAssertionGroupType
import ch.tutteli.atrium.assertions.builders.assertionBuilder
import ch.tutteli.atrium.core.None
import ch.tutteli.atrium.core.Some
import ch.tutteli.atrium.core.getOrElse
import ch.tutteli.atrium.creating.AssertionContainer
import ch.tutteli.atrium.creating.CollectingExpect
import ch.tutteli.atrium.creating.Expect
import ch.tutteli.atrium.logic.creating.basic.contains.creators.impl.ContainsAssertionCreator
import ch.tutteli.atrium.logic.creating.iterable.contains.IterableLikeContains
import ch.tutteli.atrium.logic.creating.iterable.contains.searchbehaviours.InAnyOrderSearchBehaviour
import ch.tutteli.atrium.logic.creating.iterable.contains.searchbehaviours.NotSearchBehaviour
import ch.tutteli.atrium.logic.creating.typeutils.IterableLike
import ch.tutteli.atrium.logic.impl.allCreatedAssertionsHold
import ch.tutteli.atrium.logic.impl.createExplanatoryAssertionGroup
import ch.tutteli.atrium.logic.impl.createHasElementAssertion
import ch.tutteli.atrium.reporting.translating.Translatable
import ch.tutteli.atrium.translations.DescriptionIterableAssertion
import ch.tutteli.atrium.translations.DescriptionIterableAssertion.AN_ELEMENT_WHICH

/**
 * Represents a creator of a sophisticated `contains` assertions for [Iterable] where an expected entry can appear
 * in any order and is identified by holding a group of assertions, created by an assertion creator lambda.
 *
 * @param T The type of the subject of the assertion for which the `contains` assertion is be build.
 *
 * @property searchBehaviour The search behaviour -- in this case representing `in any order` which is used to
 *   decorate the description (a [Translatable]) which is used for the [AssertionGroup].
 *
 * @constructor Represents a creator of a sophisticated `contains` assertions for [Iterable] where expected entries
 *   can appear in any order and are identified by holding a group of assertions, created by an assertion
 *   creator lambda.
 * @param searchBehaviour The search behaviour -- in this case representing `in any order` which is used to
 *   decorate the description (a [Translatable]) which is used for the [AssertionGroup].
 * @param checkers The checkers which create assertions based on the search result.
 */
class InAnyOrderEntriesAssertionCreator<E : Any, T : IterableLike>(
    private val converter: (T) -> Iterable<E?>,
    searchBehaviour: InAnyOrderSearchBehaviour,
    checkers: List<IterableLikeContains.Checker>
) : ContainsAssertionCreator<T, List<E?>, (Expect<E>.() -> Unit)?, IterableLikeContains.Checker>(
    searchBehaviour,
    checkers
),
    IterableLikeContains.Creator<T, (Expect<E>.() -> Unit)?> {

    override val descriptionContains: Translatable = DescriptionIterableAssertion.CONTAINS

    override fun makeSubjectMultipleTimesConsumable(container: AssertionContainer<T>): AssertionContainer<List<E?>> =
        turnSubjectToList(container, converter)

    override fun searchAndCreateAssertion(
        multiConsumableContainer: AssertionContainer<List<E?>>,
        searchCriterion: (Expect<E>.() -> Unit)?,
        featureFactory: (Int, Translatable) -> AssertionGroup
    ): AssertionGroup {
        val iterator = multiConsumableContainer.maybeSubject.getOrElse { emptyList() }.iterator()
        val hasElementAssertion = createHasElementAssertion(iterator)
        val (explanatoryGroup, count) =
            createExplanatoryAssertionsAndMatchingCount(iterator, searchCriterion)

        val featureAssertion = featureFactory(count, DescriptionIterableAssertion.NUMBER_OF_OCCURRENCES)
        val assertions = mutableListOf<Assertion>(explanatoryGroup, featureAssertion)

        val groupType = if (searchBehaviour is NotSearchBehaviour) {
            assertions.add(hasElementAssertion)
            addEmptyAssertionCreatorLambdaIfNecessary(assertions, searchCriterion, count)
            DefaultSummaryAssertionGroupType
        } else {
            DefaultListAssertionGroupType
        }

        return assertionBuilder.customType(groupType)
            .withDescriptionAndEmptyRepresentation(AN_ELEMENT_WHICH)
            .withAssertions(assertions)
            .build()
    }

    private fun createExplanatoryAssertionsAndMatchingCount(
        itr: Iterator<E?>,
        assertionCreatorOrNull: (Expect<E>.() -> Unit)?
    ): Pair<AssertionGroup, Int> {
        val (firstNonNullOrNull, sequence) = if (itr.hasNext() && assertionCreatorOrNull != null) {
            // we search the first non-null element in order that feature assertions
            // which are based on the subject can be showed properly in the explanation
            getFirstNonNullAndSequence(itr, sequenceOf())
        } else {
            null to itr.asSequence()
        }
        val group =
            createExplanatoryAssertionGroup(assertionCreatorOrNull) { firstNonNullOrNull?.let { Some(it) } ?: None }
        val count = sequence.count { allCreatedAssertionsHold(it, assertionCreatorOrNull) }
        return group to count
    }

    private tailrec fun getFirstNonNullAndSequence(itr: Iterator<E?>, sequence: Sequence<E?>): Pair<E?, Sequence<E?>> {
        return if (itr.hasNext()) {
            val first = itr.next()
            if (first != null) {
                first to sequence + sequenceOf(first) + itr.asSequence()
            } else {
                getFirstNonNullAndSequence(itr, sequence + sequenceOf(first))
            }
        } else {
            null to sequence
        }
    }

    private fun addEmptyAssertionCreatorLambdaIfNecessary(
        assertions: MutableList<Assertion>,
        searchCriterion: (Expect<E>.() -> Unit)?,
        count: Int
    ) {
        if (searchCriterion != null && count == 0) {
            val collectingExpect = CollectingExpect<E>(None)
            // not using addAssertionsCreatedBy on purpose so that we don't append a failing assertion
            collectingExpect.searchCriterion()
            val collectedAssertions = collectingExpect.getAssertions()
            if (collectedAssertions.isEmpty()) {
                assertions.addAll(
                    CollectingExpect<E>(None)
                        .addAssertionsCreatedBy(searchCriterion)
                        .getAssertions()
                )
            }
        }
    }
}
