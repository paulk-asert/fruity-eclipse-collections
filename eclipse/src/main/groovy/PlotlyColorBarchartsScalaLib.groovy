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

import plotly.*
import plotly.element.*
import plotly.layout.*
import static scala.collection.JavaConverters.asScala

import fruit.Fruit
import org.apache.commons.math3.ml.clustering.DoublePoint
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer

import javax.imageio.ImageIO

import static FruitColors.hsb
import static FruitColors.range
import static FruitColors.rgb
import static java.awt.Color.WHITE

def defaultConfig = Plotly.plot$default$4()

Fruit.ALL.collect { fruit ->
    var file = getClass().classLoader.getResource("${fruit.name()}.png").file as File
    var image = ImageIO.read(file)

    var allData = []
    for (x in 0..<image.width) {
        for (y in 0..<image.height) {
            def (int r, int g, int b) = rgb(image, x, y)
            def (_, col) = range(hsb(r, g, b))
            if (col != WHITE) {
                allData << new DoublePoint([r, g, b] as int[])
            }
        }
    }

    var clusterer = new KMeansPlusPlusClusterer(3, 100)
    var centroids = clusterer.cluster(allData)
    var sizes = centroids*.points*.size()
    var colors = centroids*.center*.point.collect{ new Color.RGB(*it as int[]) }
//    var tmpDir =
    var path = "/tmp/plotly/${fruit}.html"

    var trace = new Bar(intSeq([1, 2, 3]), intSeq(sizes))
            .withMarker(new Marker().withColor(oneOrSeq(colors)))

    var traces = asScala([trace]).toSeq()

    var layout = new Layout()
            .withTitle("Centroid sizes for $fruit")
            .withShowlegend(false)
            .withHeight(600)
            .withWidth(800)

    Plotly.plot(path, traces, layout, defaultConfig, false, false, true)
}

static oneOrSeq(List list) {
    OneOrSeq.fromSeq(seq(list))
}

static intSeq(List<Integer> integers) {
    Sequence.fromIntSeq(seq(integers))
}

static seq(List list) {
    asScala(list).toSeq()
}
