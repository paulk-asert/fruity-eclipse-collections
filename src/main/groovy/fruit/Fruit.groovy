package fruit

import org.eclipse.collections.api.factory.Lists
import org.eclipse.collections.api.list.ImmutableList

import java.awt.Color

enum Fruit {
    APPLE('🍎', Color.RED),
    PEACH('🍑', Color.ORANGE),
    BANANA('🍌', Color.YELLOW),
    CHERRY('🍒', Color.RED),
    ORANGE('🍊', Color.ORANGE),
    GRAPE('🍇', Color.MAGENTA)

    public static ImmutableList<Fruit> ALL = Lists.immutable.with(values())
    public static ImmutableList<String> ALL_EMOJI = Lists.immutable.with(*values()*.emoji)
    final String emoji
    final Color color

    Fruit(String emoji, Color color) {
        this.emoji = emoji
        this.color = color
    }

    static Fruit of(String emoji) {
        values().find{it.emoji == emoji }
    }
}
