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
import tech.tablesaw.api.DoubleColumn
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.plotly.Plot
import tech.tablesaw.plotly.api.HorizontalBarPlot
import tech.tablesaw.plotly.api.ScatterPlot
import tech.tablesaw.plotly.components.Marker
import tech.tablesaw.plotly.traces.BarTrace
import tech.tablesaw.plotly.traces.ScatterTrace

import javax.imageio.ImageIO

import static FruitColors.*
import static java.awt.Color.WHITE

Fruit.ALL.collect { fruit ->
    var file = getClass().classLoader.getResource("${fruit.name()}.png").file as File
    var image = ImageIO.read(file)

    var countTable = Table.create(DoubleColumn.create('count'),
            StringColumn.create('col'))
    var degrees = [:].withDefault { 0 }
    var ranges = [:].withDefault { 0 }
    var dColors = [:]
    var rColors = [:]
    for (x in 0..<image.width) {
        for (y in 0..<image.height) {
            def (int r, int g, int b) = rgb(image, x, y)
            def (deg, col) = range(hsb(r, g, b))
            if (col != WHITE) { // ignore white background
                ranges[col]++
                rColors[col] = "rgb($col.red,$col.green,$col.blue)"
                degrees[deg]++
                dColors[deg] = "rgb($r,$g,$b)"
            }
        }
    }

    double[] xcol = degrees.keySet()
    double[] ycol = degrees.values()
    String[] dCols = degrees.keySet().collect{ dColors[it] }
    String[] rCols = ranges.keySet().collect{ rColors[it] }
    var plot = ScatterPlot.create("Color count for ${fruit.name()}", 'Color', xcol, 'Number', ycol)
    var marker = Marker.builder().color(dCols).size(20).opacity(0.9).build()
    var trace = ScatterTrace.builder(xcol, ycol).marker(marker).build()
    plot.traces[0] = trace
    Plot.show(plot)

    ranges.entrySet().each { e ->
        countTable.appendRow().with {
            setDouble('count', e.value)
            setString('col', NAME_OF[e.key])
        }
    }

    plot = HorizontalBarPlot.create("Color range histogram for ${fruit.name()}", countTable, 'col', 'count')
    marker = Marker.builder().color(rCols).size(20).opacity(0.9).build()
    trace = BarTrace.builder(countTable.categoricalColumn('col'),
            countTable.numberColumn('count')).marker(marker).build()
    plot.traces[0] = trace
    Plot.show(plot)
}
