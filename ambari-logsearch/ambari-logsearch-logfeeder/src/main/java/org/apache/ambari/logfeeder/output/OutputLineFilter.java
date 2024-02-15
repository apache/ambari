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
package org.apache.ambari.logfeeder.output;

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.plugin.input.cache.LRUCache;
import org.apache.ambari.logfeeder.plugin.input.Input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * Filter for outputs based on input configs, which can drop lines if the filter applies.
 */
public class OutputLineFilter {

  private static final Logger LOG = LoggerFactory.getLogger(OutputLineFilter.class);

  /**
   * Applies filter based on input cache (on service log only).
   * Get the message and in-memory timestamp for log line. If both are not empty, evaluate that log line needs to be filtered out or not.
   */
  public Boolean apply(Map<String, Object> lineMap, Input input) {
    boolean isLogFilteredOut = false;
    LRUCache inputLruCache = input.getCache();
    if (inputLruCache != null && "service".equals(input.getInputDescriptor().getRowtype())) {
      String logMessage = (String) lineMap.get(input.getCacheKeyField());
      Long timestamp = null;
      if (lineMap.containsKey((LogFeederConstants.IN_MEMORY_TIMESTAMP))) {
        timestamp = (Long) lineMap.get(LogFeederConstants.IN_MEMORY_TIMESTAMP);
      }
      if (logMessage != null && timestamp != null) {
        isLogFilteredOut = !inputLruCache.isEntryReplaceable(logMessage, timestamp);
        if (!isLogFilteredOut) {
          inputLruCache.put(logMessage, timestamp);
        } else {
          LOG.debug("Log line filtered out: {} (file: {}, dedupInterval: {}, lastDedupEnabled: {})",
            logMessage, inputLruCache.getFileName(), inputLruCache.getDedupInterval(), inputLruCache.isLastDedupEnabled());
        }
      }
    }
    if (lineMap.containsKey(LogFeederConstants.IN_MEMORY_TIMESTAMP)) {
      lineMap.remove(LogFeederConstants.IN_MEMORY_TIMESTAMP);
    }
    return isLogFilteredOut;
  }
}
