/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.discovery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetricMetadata;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sync metadata info with the store
 */
public class TimelineMetricMetadataSync implements Runnable {
  private static final Log LOG = LogFactory.getLog(TimelineMetricMetadataSync.class);

  private final TimelineMetricMetadataManager cacheManager;

  public TimelineMetricMetadataSync(TimelineMetricMetadataManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @Override
  public void run() {
    List<TimelineMetricMetadata> metadataToPersist = new ArrayList<>();
    // Find all entries to persist
    for (TimelineMetricMetadata metadata : cacheManager.getMetadataCache().values()) {
      if (!metadata.isPersisted()) {
        metadataToPersist.add(metadata);
      }
    }
    boolean markSuccess = false;
    if (!metadataToPersist.isEmpty()) {
      try {
        cacheManager.persistMetadata(metadataToPersist);
        markSuccess = true;
      } catch (SQLException e) {
        LOG.warn("Error persisting metadata.", e);
      }
    }
    // Mark corresponding entries as persisted to skip on next run
    if (markSuccess) {
      for (TimelineMetricMetadata metadata : metadataToPersist) {
        TimelineMetricMetadataKey key = new TimelineMetricMetadataKey(
          metadata.getMetricName(), metadata.getAppId()
        );

        // Mark entry as being persisted
        metadata.setIsPersisted(true);
        // Update cache
        cacheManager.getMetadataCache().put(key, metadata);
      }
    }
    // Sync hosted apps data is needed
    if (cacheManager.syncHostedAppsMetadata()) {
      Map<String, Set<String>> persistedData = null;
      try {
        persistedData = cacheManager.getPersistedHostedAppsData();
      } catch (SQLException e) {
        LOG.warn("Failed on fetching hosted apps data from store.", e);
        return; // Something wrong with store
      }

      Map<String, Set<String>> cachedData = cacheManager.getHostedAppsCache();
      Map<String, Set<String>> dataToSync = new HashMap<>();
      if (cachedData != null && !cachedData.isEmpty()) {
        for (Map.Entry<String, Set<String>> cacheEntry : cachedData.entrySet()) {
          // No persistence / stale data in store
          if (persistedData == null || persistedData.isEmpty() ||
              !persistedData.containsKey(cacheEntry.getKey()) ||
              !persistedData.get(cacheEntry.getKey()).containsAll(cacheEntry.getValue())) {
            dataToSync.put(cacheEntry.getKey(), cacheEntry.getValue());
          }
        }
        try {
          cacheManager.persistHostedAppsMetadata(dataToSync);
          cacheManager.markSuccessOnSyncHostedAppsMetadata();

        } catch (SQLException e) {
          LOG.warn("Error persisting hosted apps metadata.", e);
        }
      }

    }
  }
}
