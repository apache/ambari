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
    LOG.debug("Persisting metric metadata...");
    persistMetricMetadata();
    LOG.debug("Persisting hosted apps metadata...");
    persistHostAppsMetadata();
    LOG.debug("Persisting hosted instance metadata...");
    persistHostInstancesMetadata();
    if (cacheManager.isDistributedModeEnabled()) {
      LOG.debug("Refreshing metric metadata...");
      refreshMetricMetadata();
      LOG.debug("Refreshing hosted apps metadata...");
      refreshHostAppsMetadata();
      LOG.debug("Refreshing hosted instances metadata...");
      refreshHostedInstancesMetadata();
    }
  }

  /**
   * Find metrics not persisted to store and persist them
   */
  private void persistMetricMetadata() {
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
  }

  /**
   * Read all metric metadata and update cached values - HA mode
   */
  private void refreshMetricMetadata() {
    Map<TimelineMetricMetadataKey, TimelineMetricMetadata> metadataFromStore = null;
    try {
      metadataFromStore = cacheManager.getMetadataFromStore();
    } catch (SQLException e) {
      LOG.warn("Error refreshing metadata from store.", e);
    }
    if (metadataFromStore != null) {
      Map<TimelineMetricMetadataKey, TimelineMetricMetadata> cachedMetadata =
        cacheManager.getMetadataCache();

      for (Map.Entry<TimelineMetricMetadataKey, TimelineMetricMetadata> metadataEntry : metadataFromStore.entrySet()) {
        if (!cachedMetadata.containsKey(metadataEntry.getKey())) {
          cachedMetadata.put(metadataEntry.getKey(), metadataEntry.getValue());
        }
      }
    }
  }

  /**
   * Sync hosted apps data if needed
   */
  private void persistHostAppsMetadata() {
    if (cacheManager.syncHostedAppsMetadata()) {
      Map<String, Set<String>> persistedData = null;
      try {
        persistedData = cacheManager.getHostedAppsFromStore();
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

  /**
   * Sync apps instances data if needed
   */
  private void persistHostInstancesMetadata() {
    if (cacheManager.syncHostedInstanceMetadata()) {
      Map<String, Set<String>> persistedData = null;
      try {
        persistedData = cacheManager.getHostedInstancesFromStore();
      } catch (SQLException e) {
        LOG.warn("Failed on fetching hosted instances data from store.", e);
        return; // Something wrong with store
      }

      Map<String, Set<String>> cachedData = cacheManager.getHostedInstanceCache();
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
          cacheManager.persistHostedInstanceMetadata(dataToSync);
          cacheManager.markSuccessOnSyncHostedInstanceMetadata();

        } catch (SQLException e) {
          LOG.warn("Error persisting hosted apps metadata.", e);
        }
      }

    }
  }
  /**
   * Read all hosted apps metadata and update cached values - HA
   */
  private void refreshHostAppsMetadata() {
    Map<String, Set<String>> hostedAppsDataFromStore = null;
    try {
      hostedAppsDataFromStore = cacheManager.getHostedAppsFromStore();
    } catch (SQLException e) {
      LOG.warn("Error refreshing metadata from store.", e);
    }
    if (hostedAppsDataFromStore != null) {
      Map<String, Set<String>> cachedData = cacheManager.getHostedAppsCache();

      for (Map.Entry<String, Set<String>> storeEntry : hostedAppsDataFromStore.entrySet()) {
        if (!cachedData.containsKey(storeEntry.getKey())) {
          cachedData.put(storeEntry.getKey(), storeEntry.getValue());
        }
      }
    }
  }

  private void refreshHostedInstancesMetadata() {
    Map<String, Set<String>> hostedInstancesFromStore = null;
    try {
      hostedInstancesFromStore = cacheManager.getHostedInstancesFromStore();
    } catch (SQLException e) {
      LOG.warn("Error refreshing metadata from store.", e);
    }
    if (hostedInstancesFromStore != null) {
      Map<String, Set<String>> cachedData = cacheManager.getHostedInstanceCache();

      for (Map.Entry<String, Set<String>> storeEntry : hostedInstancesFromStore.entrySet()) {
        if (!cachedData.containsKey(storeEntry.getKey())) {
          cachedData.put(storeEntry.getKey(), storeEntry.getValue());
        }
      }
    }
  }
}
