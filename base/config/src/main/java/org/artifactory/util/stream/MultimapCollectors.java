/*
 *
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2016 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.artifactory.util.stream;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author Dan Feldman
 */
public class MultimapCollectors {

    /**
     * Maps an entryset of a map to multimap
     */
    public static <K, V, A extends Multimap<K, V>> Collector<Map.Entry<K, V>, A, A> multimapFromEntrySet(Supplier<A> supplier) {
        return Collector.of(supplier, (acc, entry) -> acc.put(entry.getKey(), entry.getValue()), (map1, map2) -> {
            map1.putAll(map2);
            return map1;
        });
    }

    /**
     * Collector that maps a given object stream to a multimap of the form: key -> value where you can supply the key
     * and value mappers.
     */
    public static <T, K, V> MultimapCollector<T, K, V> toMultimap(Function<T, K> keyMapper, Function<T, V> valueMapper) {
        return new MultimapCollector<>(keyMapper, valueMapper);
    }

    /**
     * Collector that maps a given object stream to a multimap of the form: key -> object where you can supply the key mapper.
     */
    public static <T, V> MultimapCollector<T, V, T> toMultimap(Function<T, V> keyMapper) {
        return new MultimapCollector<>(keyMapper, v -> v);
    }

    public static class MultimapCollector<T, K, V> implements Collector<T, Multimap<K, V>, Multimap<K, V>> {

        private final Function<T, K> keyMapper;
        private final Function<T, V> valueMapper;

        MultimapCollector(Function<T, K> keyMapper, Function<T, V> valueMapper) {
            this.keyMapper = keyMapper;
            this.valueMapper = valueMapper;
        }

        @Override
        public Supplier<Multimap<K, V>> supplier() {
            return HashMultimap::create;
        }

        @Override
        public BiConsumer<Multimap<K, V>, T> accumulator() {
            return (map, element) -> map.put(keyMapper.apply(element), valueMapper.apply(element));
        }

        @Override
        public BinaryOperator<Multimap<K, V>> combiner() {
            return (target, source) -> {
                target.putAll(source);
                return target;
            };
        }

        @Override
        public Function<Multimap<K, V>, Multimap<K, V>> finisher() {
            return map -> map;
        }

        @Override
        public Set<Characteristics> characteristics() {
            return ImmutableSet.of(Characteristics.IDENTITY_FINISH);
        }
    }
}
