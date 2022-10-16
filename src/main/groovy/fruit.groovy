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
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.plotly.components.Axis
import tech.tablesaw.plotly.components.Figure
import tech.tablesaw.plotly.components.Layout
import tech.tablesaw.plotly.components.Marker

import tech.tablesaw.plotly.Plot
import tech.tablesaw.plotly.api.Scatter3DPlot
import tech.tablesaw.plotly.components.threeD.Scene
import tech.tablesaw.plotly.traces.Scatter3DTrace

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage
//import java.util.concurrent.Executors

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
    var image = ImageIO.read(new File("resources/${fruit.name()}.png"))

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
    def maxRange = ranges.max { e -> e.value }.key
    def maxColor = range(colors.max { e -> e.value }.key)

    int cols = 8, rows = 8
    int stepX = image.width / cols
    int stepY = image.height / rows
    var splitImage = new BufferedImage(image.width + (cols - 1) * 5, image.height + (rows - 1) * 5, image.type)
    var g2a = splitImage.createGraphics()
    var pixelated = new BufferedImage(image.width + (cols - 1) * 5, image.height + (rows - 1) * 5, image.type)
    var g2b = pixelated.createGraphics()

    ranges = [:].withDefault { 0 }
    def xyTable = Table.create(DoubleColumn.create('x'),
            DoubleColumn.create('y'),
            DoubleColumn.create('deg'),
            StringColumn.create('col'))
    def rgbTable = Table.create(DoubleColumn.create('r'),
            DoubleColumn.create('g'),
            DoubleColumn.create('b'),
            StringColumn.create('col'))
    List<DoublePoint> allData = []
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
                    if (col != WHITE) {
                        allData << dp
                    }
                    if (col != WHITE) {
                        rgbTable.appendRow().with {
                            setDouble('r', r)
                            setDouble('g', g)
                            setDouble('b', b)
                            setString('col', "rgb($r,$g,$b)")
                        }
                    }
                    xyTable.appendRow().with {
                        setDouble('x', stepX * j + x)
                        setDouble('y', stepY * i + y)
                        setDouble('deg', (deg + 100) % 360)
                        setString('col', COLOR_NAMES[col])
                    }
                }
            }
            var centroids = clusterer.cluster(data)
            var biggestCluster = centroids.max { ctrd -> ctrd.points.size() }
            var ctr = biggestCluster.center.point*.intValue()
            var hsb = hsb(*ctr)
            def (_, range) = range(hsb)
            if (range != WHITE) ranges[range]++
            g2a.drawImage(image, (stepX + 5) * j, (stepY + 5) * i, stepX * (j + 1) + 5 * j, stepY * (i + 1) + 5 * i,
                    stepX * j, stepY * i, stepX * (j + 1), stepY * (i + 1), null)
            g2b.setColor(new Color(*ctr))
            g2b.fillRect((stepX + 5) * j, (stepY + 5) * i, stepX, stepY)
        }
    }
    g2a.dispose()
    g2b.dispose()

    def clusterer = new KMeansPlusPlusClusterer(3, 100)
    var centroids = clusterer.cluster(allData)
    centroids*.center.each { ctr ->
        rgbTable.appendRow().with {
            setDouble('r', ctr.point[0])
            setDouble('g', ctr.point[1])
            setDouble('b', ctr.point[2])
            setString('col', "rgb(0,0,0)")
        }

    }

    var marker = Marker.builder().color(rgbTable.column('col').asObjectArray()).size(20).opacity(0.8).build()
    var trace = Scatter3DTrace.builder(rgbTable.column('r'), rgbTable.column('g'), rgbTable.column('b')).marker(marker).build()
    Layout layout = standardLayout("RGB in 3D for ${fruit.name()}", 'r', 'g', 'b', false);

    Plot.show(Figure.builder().addTraces(trace).layout(layout).build())
    Plot.show(Scatter3DPlot.create('Color vs xy', xyTable, 'x', 'y', 'deg', 'col'))

    var swing = new SwingBuilder()
    var maxCentroid = ranges.max { e -> e.value }.key
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
    def colors = [fruit.color, maxColor, maxRange, maxCentroid].collect {
        COLOR_NAMES[it].padRight(14)
    }.join().trim()
    println "${fruit.emoji.padRight(6)} $colors"
}

@Field COLOR_NAMES = BiMaps.immutable.ofAll(
        [WHITE: WHITE, RED: RED, ORANGE: ORANGE, GREEN: GREEN,
         BLUE : BLUE, YELLOW: YELLOW, MAGENTA: MAGENTA]
).inverse()

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
    if (hsb[1] < 0.1 && hsb[2] > 0.9) return [0, WHITE]
    if (hsb[2] < 0.1) return [0, BLACK]
    int deg = (hsb[0] * 360).round()
    return [deg, range(deg)]
}

def range(int deg) {
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

def standardLayout(String title, String xCol, String yCol, String zCol, boolean showLegend) {
    Layout.builder()
            .title(title)
            .height(800)
            .width(1000)
            .showLegend(showLegend)
            .scene(
                    Scene.sceneBuilder()
                            .xAxis(Axis.builder().title(xCol).build())
                            .yAxis(Axis.builder().title(yCol).build())
                            .zAxis(Axis.builder().title(zCol).build())
                            .build())
            .build()
}
