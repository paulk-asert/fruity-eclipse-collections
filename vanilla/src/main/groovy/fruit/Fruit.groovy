package fruit

import groovy.transform.TupleConstructor

import java.awt.Color

@TupleConstructor
enum Fruit {
    APPLE('🍎', Color.RED, [Color.RED, Color.GREEN]),
    PEACH('🍑', Color.ORANGE, [Color.ORANGE]),
    BANANA('🍌', Color.YELLOW, [Color.YELLOW, Color.GREEN]),
    CHERRY('🍒', Color.RED, [Color.RED]),
    ORANGE('🍊', Color.ORANGE, [Color.ORANGE]),
    GRAPE('🍇', Color.MAGENTA, [Color.MAGENTA, Color.GREEN])

    final String emoji
    final Color color
    final List<Color> colors

    static Fruit of(String emoji) {
        values().find{it.emoji == emoji }
    }
}
