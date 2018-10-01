/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.utils;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Utilities for collections
 */
public class CollectionUtils {

  /**
   * Returns a singleton instance of an empty immutable {@link ConcurrentMap}
   * @param <K> key type
   * @param <V> value type
   * @return a singleton instance of an empty immutable {@link ConcurrentMap}
   */
  public static <K, V> ConcurrentMap<K, V> emptyConcurrentMap() {
    return (ConcurrentMap<K, V>)EmptyConcurrentMap.INSTANCE;
  }


  private static class EmptyConcurrentMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    @SuppressWarnings({"rawtypes"})
    private static final EmptyConcurrentMap INSTANCE = new EmptyConcurrentMap();

    @Override
    public Set<Entry<K, V>> entrySet() {
      return Collections.emptySet();
    }

    @Override
    public boolean remove(Object key, Object value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public V replace(K key, V newValue) {
      throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(K key, V value) {
      throw new UnsupportedOperationException();
    }
  }
}
