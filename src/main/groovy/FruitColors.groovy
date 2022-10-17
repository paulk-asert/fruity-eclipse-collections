/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import fruit.Fruit
import groovy.swing.SwingBuilder
import groovy.transform.Field
import groovyx.gpars.GParsExecutorsPool
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

//import java.util.concurrent.Executors

import static java.awt.Color.BLACK
import static java.awt.Color.BLUE
import static java.awt.Color.GREEN
import static java.awt.Color.MAGENTA
import static java.awt.Color.ORANGE
import static java.awt.Color.RED
import static java.awt.Color.RGBtoHSB
import static java.awt.Color.WHITE
import static java.awt.Color.YELLOW
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
        Bags.immutable.withOccurrences(RED, 2, YELLOW, 1, ORANGE, 2, MAGENTA, 1)

Fruit.ALL_EMOJI.chunk(4).with {
    assert first == Lists.mutable.with('🍎', '🍑', '🍌', '🍒')
    assert last == Lists.mutable.with('🍊', '🍇')
}

// For virtual threads, replace next line with:
// GParsExecutorsPool.withExistingPool(Executors.newVirtualThreadPerTaskExecutor()) { pool ->
GParsExecutorsPool.withPool { pool ->
    var parallelFruit = Fruit.ALL.asParallel(pool, 1)
    var redFruit = parallelFruit.select(fruit -> fruit.color == RED).toList()
    assert redFruit == Lists.mutable.with(Fruit.of('🍎'), Fruit.of('🍒'))
}

var results = Fruit.ALL.collect { fruit ->
    var file = getClass().classLoader.getResource("${fruit.name()}.png").file as File
    var image = ImageIO.read(file)

    var colors = [:].withDefault { 0 }
    var ranges = [:].withDefault { 0 }
    for (x in 0..<image.width) {
        for (y in 0..<image.height) {
            def (int r, int g, int b) = rgb(image, x, y)
            float[] hsb = hsb(r, g, b)
            def (deg, range) = range(hsb)
            if (range != WHITE) { // ignore white background
                ranges[range]++
                colors[deg]++
            }
        }
    }
    var maxRange = ranges.max { e -> e.value }.key
    var maxColor = range(colors.max { e -> e.value }.key)

    int cols = 8, rows = 8
    int grid = 5 // thickness of black "grid" between subimages
    int stepX = image.width / cols
    int stepY = image.height / rows
    var splitImage = new BufferedImage(image.width + (cols - 1) * grid, image.height + (rows - 1) * grid, image.type)
    var g2a = splitImage.createGraphics()
    var pixelated = new BufferedImage(image.width + (cols - 1) * grid, image.height + (rows - 1) * grid, image.type)
    var g2b = pixelated.createGraphics()

    ranges = [:].withDefault { 0 }
    for (i in 0..<rows) {
        for (j in 0..<cols) {
            def clusterer = new KMeansPlusPlusClusterer(5, 100)
            List<DoublePoint> data = []
            for (x in 0..<stepX) {
                for (y in 0..<stepY) {
                    def (int r, int g, int b) = rgb(image, stepX * j + x, stepY * i + y)
                    var dp = new DoublePoint([r, g, b] as int[])
                    var hsb = hsb(r, g, b)
                    def (deg, col) = range(hsb)
                    data << dp
                }
            }
            var centroids = clusterer.cluster(data)
            var biggestCluster = centroids.max { ctrd -> ctrd.points.size() }
            var ctr = biggestCluster.center.point*.intValue()
            var hsb = hsb(*ctr)
            def (_, range) = range(hsb)
            if (range != WHITE) ranges[range]++
            g2a.drawImage(image, (stepX + grid) * j, (stepY + grid) * i, stepX * (j + 1) + grid * j, stepY * (i + 1) + grid * i,
                    stepX * j, stepY * i, stepX * (j + 1), stepY * (i + 1), null)
            g2b.color = new Color(*ctr)
            g2b.fillRect((stepX + grid) * j, (stepY + grid) * i, stepX, stepY)
        }
    }
    g2a.dispose()
    g2b.dispose()

    var swing = new SwingBuilder()
    var maxCentroid = ranges.max { e -> e.value }.key
    swing.edt {
        frame(title: 'Original vs Subimages vs K-Means',
                defaultCloseOperation: DISPOSE_ON_CLOSE, pack: true, show: true) {
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
    def colors = [fruit.color, maxColor, maxRange, maxCentroid].collect {
        NAME_OF[it].padRight(14)
    }.join().trim()
    println "${fruit.emoji.padRight(6)} $colors"
}

@Field public static COLOR_OF = BiMaps.immutable.ofAll([
        WHITE: WHITE, RED: RED, GREEN: GREEN, BLUE: BLUE,
        ORANGE: ORANGE, YELLOW: YELLOW, MAGENTA: MAGENTA
])
@Field public static NAME_OF = COLOR_OF.inverse()

static hsb(int r, int g, int b) {
    float[] hsb = new float[3]
    RGBtoHSB(r, g, b, hsb)
    hsb
}

static rgb(BufferedImage image, int x, int y) {
    int rgb = image.getRGB(x, y)
    int r = (rgb >> 16) & 0xFF
    int g = (rgb >> 8) & 0xFF
    int b = rgb & 0xFF
    [r, g, b]
}

static range(float[] hsb) {
    if (hsb[1] < 0.1 && hsb[2] > 0.9) return [0, WHITE]
    if (hsb[2] < 0.1) return [0, BLACK]
    int deg = (hsb[0] * 360).round()
    return [deg, range(deg)]
}

static range(int deg) {
    switch (deg) {
        case 0..<16 -> RED
        case 16..<35 -> ORANGE
        case 35..<75 -> YELLOW
        case 75..<160 -> GREEN
        case 160..<250 -> BLUE
        case 250..<330 -> MAGENTA
        default -> RED
    }
}
