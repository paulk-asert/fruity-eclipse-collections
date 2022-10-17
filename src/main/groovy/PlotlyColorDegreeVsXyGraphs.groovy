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
import tech.tablesaw.plotly.api.Scatter3DPlot
import tech.tablesaw.plotly.components.Marker
import tech.tablesaw.plotly.traces.Scatter3DTrace

import javax.imageio.ImageIO

import static FruitColors.*
import static java.awt.Color.WHITE

Fruit.ALL.collect { fruit ->
    var file = getClass().classLoader.getResource("${fruit.name()}.png").file as File
    var image = ImageIO.read(file)

    def xyTable = Table.create(DoubleColumn.create('x'),
            DoubleColumn.create('y'),
            DoubleColumn.create('deg'),
            StringColumn.create('col'))
    for (x in 0..<image.width) {
        for (y in 0..<image.height) {
            def (int r, int g, int b) = rgb(image, x, y)
            def (deg, col) = range(hsb(r, g, b))
            if (col != WHITE) {
                xyTable.appendRow().with {
                    setDouble('x', x)
                    setDouble('y', y)
                    setDouble('deg', (deg + 100) % 360)
                    setString('col', NAME_OF[col])
                }
            }
        }
    }

    var plot = Scatter3DPlot.create('Color vs xy', xyTable, 'x', 'y', 'deg', 'col')
    // this is particularly ugly since the Tablesaw plotly integration is missing numerous functionality
    var tables = xyTable.splitOn(xyTable.categoricalColumn('col'))
    var tableList = tables.asTableList()
    for (i in 0..<tables.size()) {
        var t = plot.traces[i]
        var n = t.name()
        var color = COLOR_OF[n]
        var rgb = "rgb($color.red,$color.green,$color.blue)"
        var marker = Marker.builder().color(rgb).size(12).opacity(0.8).build()
        var table = tableList.get(i)
        plot.traces[i] =
                Scatter3DTrace.builder(
                        table.numberColumn('x'),
                        table.numberColumn('y'),
                        table.numberColumn('deg'))
                        .showLegend(true)
                        .name(n)
                        .marker(marker)
                        .build()
    }

    Plot.show(plot)
}
