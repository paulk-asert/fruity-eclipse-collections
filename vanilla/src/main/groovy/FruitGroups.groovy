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

import java.awt.Color
import java.util.stream.Collectors

import static java.awt.Color.*

var expected = [
        (GREEN)   : [Fruit.of('🍎'), Fruit.of('🍌'), Fruit.of('🍇')],
        (RED)     : [Fruit.of('🍎'), Fruit.of('🍒')],
        (ORANGE)  : [Fruit.of('🍑'), Fruit.of('🍊')],
        (YELLOW)  : [Fruit.of('🍌')],
        (MAGENTA) : [Fruit.of('🍇')]
]

assert expected == Fruit.values()
        .collectMany(f -> f.colors.collect{ c -> [c, f] })
        .groupBy{ c, f -> c }
        .collectEntries{ k, v -> [k, v*.get(1)] }

// alternative using combinations
var allColors = Fruit.values()*.colors.sum().toSet()
assert expected == [allColors, Fruit.values()].combinations()
    .findAll{ c, f -> c in f.colors }
    .groupBy{ c, f -> c }
    .collectEntries{ k, v -> [k, v*.get(1)] }

// alternative using streams
assert expected == Fruit.values().stream()
    .mapMulti((fruit, consumer) -> {
        for (Color color : fruit.colors) {
            consumer.accept(Map.entry(color, fruit))
        }
    })
    .collect(Collectors.groupingBy(
            Map.Entry::getKey,
            Collectors.mapping(Map.Entry::getValue, Collectors.toList())
    ))

assert expected == GQL {
    from f in Fruit.values()
    crossjoin c in Fruit.values()*.colors.sum().toSet()
    where c in f.colors
    groupby c
    select c, list(f)
}.collectEntries()

assert expected == Fruit.values().groupByMany(Fruit::getColors)

assert Fruit.values().groupByMany(Fruit::getEmoji, Fruit::getColors) == [
    (GREEN)   : ['🍎', '🍌', '🍇'],
    (RED)     : ['🍎', '🍒'],
    (ORANGE)  : ['🍑', '🍊'],
    (YELLOW)  : ['🍌'],
    (MAGENTA) : ['🍇']
]

var vowels = 'AEIOU'.toSet()
var vowelsOf = { String word -> word.toSet().intersect(vowels) }
assert Fruit.values().groupByMany(Fruit::getEmoji, fruit -> vowelsOf(fruit.name())) == [
    A: ['🍎', '🍑', '🍌', '🍊', '🍇'],
    E: ['🍎', '🍑', '🍒', '🍊', '🍇'],
    O: ['🍊']
]

var availability = [
    '🍎': ['Spring'],
    '🍌': ['Spring', 'Summer', 'Autumn', 'Winter'],
    '🍇': ['Spring', 'Autumn'],
    '🍒': ['Autumn'],
    '🍑': ['Spring']
]
assert availability.groupByMany() == [
    Winter: ['🍌'],
    Autumn: ['🍌', '🍇', '🍒'],
    Summer: ['🍌'],
    Spring: ['🍎', '🍌', '🍇', '🍑']
]
