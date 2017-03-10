/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logfeeder.input.cache;

import com.google.common.collect.EvictingQueue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU cache for handle de-duplications per input files.
 * It won't put already existing entries into the cache map if de-duplication interval not higher then a specific value
 * or if the new value is the most recently used one (in case of lastDedupEnabled is true)
 */
public class LRUCache {
  private final LinkedHashMap<String, Long> keyValueMap;
  private final String fileName;
  private final long dedupInterval;
  private final boolean lastDedupEnabled;
  private final EvictingQueue<String> mostRecentLogs;

  public LRUCache(final int limit, final String fileName, final long dedupInterval, boolean lastDedupEnabled) {
    this.fileName = fileName;
    this.dedupInterval = dedupInterval;
    this.lastDedupEnabled = lastDedupEnabled;
    this.mostRecentLogs = EvictingQueue.create(1); // for now, we will just store 1 mru entry
    keyValueMap = new LinkedHashMap<String, Long>(16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(final Map.Entry<String, Long> eldest) {
        return size() > limit;
      }
    };
  }

  public boolean isEntryReplaceable(String key, Long value) {
    boolean result = true;
    Long existingValue = keyValueMap.get(key);
    if (existingValue == null) {
      result = true;
    } else if (lastDedupEnabled && mostRecentLogs.contains(key)) { // TODO: get peek element if mostRecentLogs will contain more than 1 element
      result = false;
    } else if (Math.abs(value - existingValue) < dedupInterval) {
      result = false;
    }
    mostRecentLogs.add(key);
    return result;
  }

  public void put(String key, Long value) {
    if (isEntryReplaceable(key, value)) {
      keyValueMap.put(key, value);
    }
  }

  public Long get(String key) {
    mostRecentLogs.add(key);
    return keyValueMap.get(key);
  }

  public String getMRUKey() {
    return mostRecentLogs.peek();
  }

  public int size() {
    return keyValueMap.size();
  }

  public long getDedupInterval() {
    return dedupInterval;
  }

  public boolean containsKey(String key) {
    return keyValueMap.containsKey(key);
  }

  public String getFileName() {
    return this.fileName;
  }

  public boolean isLastDedupEnabled() {
    return lastDedupEnabled;
  }
}
