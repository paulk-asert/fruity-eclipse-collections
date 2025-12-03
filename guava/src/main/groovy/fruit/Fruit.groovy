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
