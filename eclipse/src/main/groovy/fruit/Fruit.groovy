package fruit

import groovy.transform.TupleConstructor
import org.eclipse.collections.api.factory.Lists
import org.eclipse.collections.api.list.ImmutableList

import java.awt.Color

@TupleConstructor
enum Fruit {
    APPLE('🍎', Color.RED, [Color.RED, Color.GREEN]),
    PEACH('🍑', Color.ORANGE, [Color.ORANGE]),
    BANANA('🍌', Color.YELLOW, [Color.YELLOW, Color.GREEN]),
    CHERRY('🍒', Color.RED, [Color.RED]),
    ORANGE('🍊', Color.ORANGE, [Color.ORANGE]),
    GRAPE('🍇', Color.MAGENTA, [Color.MAGENTA, Color.GREEN])

    public static ImmutableList<Fruit> ALL = Lists.immutable.with(values())
    public static ImmutableList<String> ALL_EMOJI = Lists.immutable.with(*values()*.emoji)
    final String emoji
    final Color color
    final List<Color> colors

    static Fruit of(String emoji) {
        values().find{it.emoji == emoji }
    }
}
