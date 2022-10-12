import fruit.Fruit
import groovyx.gpars.GParsExecutorsPool
import org.eclipse.collections.api.factory.Bags
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Multimaps
import org.eclipse.collections.impl.factory.Sets

import static java.awt.Color.*

assert Lists.mutable.with('🍎', '🍎', '🍌', '🍌').distinct() ==
        Lists.mutable.with('🍎', '🍌')

var onlyBanana = Sets.immutable.with('🍌')

assert Fruit.ALL_EMOJI.select(onlyBanana::contains) == List.of('🍌')

assert Fruit.ALL_EMOJI.reject(onlyBanana::contains) ==
        List.of('🍎', '🍑', '🍒', '🍊', '🍇')

assert Fruit.ALL.groupBy(Fruit::getColor) ==
        Multimaps.mutable.list.empty()
                .withKeyMultiValues(RED, Fruit.of('🍎'), Fruit.of('🍒'))
                .withKeyMultiValues(YELLOW, Fruit.of('🍌'))
                .withKeyMultiValues(ORANGE, Fruit.of('🍑'), Fruit.of('🍊'))
                .withKeyMultiValues(MAGENTA, Fruit.of('🍇'))

assert Fruit.ALL.countBy(Fruit::getColor) ==
        Bags.immutable.withOccurrences(
                RED, 2,
                YELLOW, 1,
                ORANGE, 2,
                MAGENTA, 1
        )

assert Fruit.ALL_EMOJI.chunk(4).with {
    first == Lists.mutable.with('🍎', '🍑', '🍌', '🍒')
    last == Lists.mutable.with('🍊', '🍇')
}

// For virtual threads on JDK19 with preview features enabled,
// replace the GParsExecutorsPool.withPool line with the following:
// GParsPool.withExistingPool(Executors.newVirtualThreadPerTaskExecutor()) { pool ->
GParsExecutorsPool.withPool { pool ->
    var parallelFruit = Fruit.ALL.asParallel(pool, 1)
    var redFruit = parallelFruit.select(fruit -> fruit.color == RED).toList()
    assert redFruit == Lists.mutable.with(Fruit.of('🍎'), Fruit.of('🍒'))
}

