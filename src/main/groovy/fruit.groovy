import fruit.Fruit
import groovy.swing.SwingBuilder
import groovy.transform.Field
import org.apache.commons.math3.ml.clustering.DoublePoint
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
import org.eclipse.collections.api.factory.Bags
import org.eclipse.collections.api.factory.BiMaps
import org.eclipse.collections.impl.factory.Lists
import org.eclipse.collections.impl.factory.Multimaps
import org.eclipse.collections.impl.factory.Sets

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage

import static java.awt.Color.*
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE

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
//GParsExecutorsPool.withPool { pool ->
//    var parallelFruit = Fruit.ALL.asParallel(pool, 1)
//    var redFruit = parallelFruit.select(fruit -> fruit.color == RED).toList()
//    assert redFruit == Lists.mutable.with(Fruit.of('🍎'), Fruit.of('🍒'))
//}

var results = Fruit.ALL.collect { fruit ->
    var image = ImageIO.read(new File("resources/${fruit.name()}.png"))

    var colors = [:].withDefault{ 0 }
    var ranges = [:].withDefault{ 0 }
    for (x in 0..<image.width) {
        for (y in 0..<image.height) {
            def (int r, int g, int b) = rgb(image, x, y)
            float[] hsb = hsb(r, g, b)
            def (deg, range) = range(hsb)
            if (range !in [WHITE, BLACK]) {
                ranges[range]++
                colors[deg]++
            }
        }
    }
    def maxRange = ranges.max { e -> e.value }.key
    def maxColor = range(colors.max { e -> e.value }.key)

    int cols = 8, rows = 8
    int stepX = image.width/cols
    int stepY = image.height/rows
    var splitImage = new BufferedImage(image.width + (cols - 1) * 5, image.height + (rows - 1) * 5, image.type)
    var g2a = splitImage.createGraphics()
    var pixelated = new BufferedImage(image.width + (cols - 1) * 5, image.height + (rows - 1) * 5, image.type)
    var g2b = pixelated.createGraphics()

    ranges = [:].withDefault{ 0 }
    for (i in 0..<rows) {
        for (j in 0..<cols) {
            def clusterer = new KMeansPlusPlusClusterer(3, 100)
            List<DoublePoint> data = []
            for (x in 0..<stepX) {
                for (y in 0..<stepY) {
                    def (int r, int g, int b) = rgb(image, stepX * j + x, stepY * i + y)
                    data << new DoublePoint([r, g, b] as int[])
                }
            }
            var centroids = clusterer.cluster(data)
            var biggestCluster = centroids.max{ctrd -> ctrd.points.size() }
            int[] ctr = biggestCluster.center.point*.intValue()
            float[] hsb = hsb(*ctr)
            def (_, range) = range(hsb)
            if (range != WHITE) ranges[range]++
            g2a.drawImage(image, (stepX + 5) * j, (stepY + 5) * i, stepX * (j+1) + 5 * j, stepY * (i+1) + 5 * i,
                    stepX * j, stepY * i, stepX * (j+1), stepY * (i+1), null)
            g2b.setColor(new Color(*ctr))
            g2b.fillRect((stepX + 5) * j, (stepY + 5) * i, stepX, stepY)
        }
    }
    g2a.dispose()
    g2b.dispose()
    var maxCentroid = ranges.max { e -> e.value }.key
    var swing = new SwingBuilder()
    swing.edt {
        frame(title: 'Original', defaultCloseOperation: DISPOSE_ON_CLOSE, pack: true, show: true) {
            flowLayout()
            label(icon: imageIcon(image))
            label(icon: imageIcon(splitImage))
            label(icon: imageIcon(pixelated))
        }
    }

    [fruit, maxRange, maxColor, maxCentroid]
}

println "Fruit  Expected      By max color  By max range  By k-means"
results.each { fruit, maxRange, maxColor, maxCentroid ->
    println "${fruit.emoji.padRight(6)} ${name(fruit.color)}${name(maxRange)}${name(maxColor)}${name(maxCentroid)}"
}

@Field COLOR_NAMES = BiMaps.immutable.ofAll(
        [WHITE: WHITE, RED: RED, ORANGE: ORANGE, GREEN: GREEN,
         BLUE: BLUE, YELLOW: YELLOW, MAGENTA: MAGENTA]
).inverse()
def name(color) {
    COLOR_NAMES[color].padRight(14)
}

def hsb(int r, int g, int b) {
    float[] hsb = new float[3]
    RGBtoHSB(r, g, b, hsb)
    hsb
}

def rgb(BufferedImage image, int x, int y) {
    int rgb = image.getRGB(x, y)
    int r = (rgb >> 16) & 0xFF
    int g = (rgb >> 8) & 0xFF
    int b = rgb & 0xFF
    [r, g, b]
}

def range(float[] hsb) {
    if (hsb[1] < 0.1 && hsb[2] > 0.9) return [-1, WHITE]
    if (hsb[2] < 0.1) return [-1, BLACK]
    long deg = (hsb[0] * 360).round()
    return [deg, range(deg)]
}

def range(long deg) {
    switch (deg) {
        case { deg >= 0 && deg < 16 } -> RED
        case { deg >= 16 && deg < 35 } -> ORANGE
        case { deg >= 35 && deg < 75 } -> YELLOW
        case { deg >= 75 && deg < 160 } -> GREEN
        case { deg >= 160 && deg < 250 } -> BLUE
        case { deg >= 250 && deg < 330 } -> MAGENTA
        default -> RED
    }
}
