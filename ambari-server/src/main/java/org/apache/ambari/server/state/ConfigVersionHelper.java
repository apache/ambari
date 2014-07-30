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

package org.apache.ambari.server.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class to manage config versions for cluster
 */
public class ConfigVersionHelper {

  ConcurrentMap<String, AtomicLong> versionCounters = new ConcurrentHashMap<String, AtomicLong>();

  public ConfigVersionHelper(Map<String, Long> configTypeLastVersions) {
    for (Map.Entry<String, Long> entry : configTypeLastVersions.entrySet()) {
      String type = entry.getKey();
      Long version = entry.getValue();
      versionCounters.put(type, new AtomicLong(version));
    }
  }

  public long getNextVersion(String key) {
    AtomicLong version = versionCounters.get(key);
    if (version == null) {
      version = new AtomicLong();
      AtomicLong tmp = versionCounters.putIfAbsent(key, version);
      if (tmp != null) {
        version = tmp;
      }
    }
    return version.incrementAndGet();
  }
}
