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
import org.apache.commons.math3.ml.clustering.DoublePoint
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.plotly.Plot
import tech.tablesaw.plotly.api.Scatter3DPlot
import tech.tablesaw.plotly.components.Marker
import tech.tablesaw.plotly.traces.Scatter3DTrace

import javax.imageio.ImageIO

import static FruitColors.hsb
import static FruitColors.range
import static FruitColors.rgb
import static java.awt.Color.WHITE

Fruit.ALL.collect { fruit ->
    var file = getClass().classLoader.getResource("${fruit.name()}.png").file as File
    var image = ImageIO.read(file)

    var rgbTable = Table.create(DoubleColumn.create('r'),
            DoubleColumn.create('g'),
            DoubleColumn.create('b'),
            StringColumn.create('col'))
    var allData = []
    for (x in 0..<image.width) {
        for (y in 0..<image.height) {
            def (int r, int g, int b) = rgb(image, x, y)
            def (_, col) = range(hsb(r, g, b))
            if (col != WHITE) {
                allData << new DoublePoint([r, g, b] as int[])
                rgbTable.appendRow().with {
                    setDouble('r', r)
                    setDouble('g', g)
                    setDouble('b', b)
                    setString('col', "rgb($r,$g,$b)")
                }
            }
        }
    }

    var clusterer = new KMeansPlusPlusClusterer(3, 100)
    var centroids = clusterer.cluster(allData)
    centroids*.center.each { ctr ->
        rgbTable.appendRow().with {
            setDouble('r', ctr.point[0])
            setDouble('g', ctr.point[1])
            setDouble('b', ctr.point[2])
            setString('col', "rgb(0,0,0)")
        }
    }

    var plot = Scatter3DPlot.create("RGB in 3D for ${fruit.name()}", rgbTable, 'r', 'g', 'b')
    // override trace to obtain custom color (would be nicer if API supported this)
    var marker = Marker.builder().color(rgbTable.column('col').asObjectArray()).size(20).opacity(0.8).build()
    var trace = Scatter3DTrace.builder(rgbTable.column('r'), rgbTable.column('g'), rgbTable.column('b')).marker(marker).build()
    plot.traces[0] = trace
    Plot.show(plot)
}
