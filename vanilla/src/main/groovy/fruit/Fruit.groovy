package fruit

import java.awt.Color

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

    Fruit(String emoji, Color color, List<Color> colors) {
        this.emoji = emoji
        this.color = color
        this.colors = colors
    }

    static Fruit of(String emoji) {
        values().find{it.emoji == emoji }
    }
}
