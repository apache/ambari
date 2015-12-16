/**
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * Class that assists with merging configuration values across stacks.
 */
@Singleton
public class ConfigMergeHelper {

  private static final Pattern HEAP_PATTERN = Pattern.compile("(\\d+)([mgMG])");
  private static final Logger LOG = LoggerFactory.getLogger(ConfigMergeHelper.class);

  @Inject
  private Provider<Clusters> m_clusters;

  @Inject
  private Provider<AmbariMetaInfo> m_ambariMetaInfo;

  @SuppressWarnings("unchecked")
  public Map<String, Map<String, ThreeWayValue>> getConflicts(String clusterName, StackId targetStack) throws AmbariException {
    Cluster cluster = m_clusters.get().getCluster(clusterName);
    StackId oldStack = cluster.getCurrentStackVersion();

    Map<String, Map<String, String>> oldMap = new HashMap<String, Map<String, String>>();
    Map<String, Map<String, String>> newMap = new HashMap<String, Map<String, String>>();

    // Add service properties for old and new stack
    for (String serviceName : cluster.getServices().keySet()) {
      Set<PropertyInfo> oldStackProperties = m_ambariMetaInfo.get().getServiceProperties(
          oldStack.getStackName(), oldStack.getStackVersion(), serviceName);
      addToMap(oldMap, oldStackProperties);

      Set<PropertyInfo> newStackProperties = m_ambariMetaInfo.get().getServiceProperties(
          targetStack.getStackName(), targetStack.getStackVersion(), serviceName);
      addToMap(newMap, newStackProperties);
    }

    // Add stack properties for old and new stack
    Set<PropertyInfo> set = m_ambariMetaInfo.get().getStackProperties(
        oldStack.getStackName(), oldStack.getStackVersion());
    addToMap(oldMap, set);

    set = m_ambariMetaInfo.get().getStackProperties(
        targetStack.getStackName(), targetStack.getStackVersion());
    addToMap(newMap, set);

    // Final result after merging.
    Map<String, Map<String, ThreeWayValue>> result =
        new HashMap<String, Map<String, ThreeWayValue>>();

    for (Entry<String, Map<String, String>> entry : oldMap.entrySet()) {
      if (!newMap.containsKey(entry.getKey())) {
        LOG.info("Stack {} does not have an equivalent config type {} in {}",
            oldStack.getStackId(), entry.getKey(), targetStack.getStackId());
        continue;
      }

      Map<String, String> oldPairs = entry.getValue();
      Map<String, String> newPairs = newMap.get(entry.getKey());
      Collection<String> customValueKeys = null;

      Config config = cluster.getDesiredConfigByType(entry.getKey());
      if (null != config) {
        Set<String> valueKeys = config.getProperties().keySet();

        customValueKeys = CollectionUtils.subtract(valueKeys, oldPairs.keySet());
      }

      // Keep properties with custom values (i.e., changed from default value in old stack)
      if (null != customValueKeys) {
        for (String prop : customValueKeys) {
          String newVal = newPairs.get(prop);
          String savedVal = config.getProperties().get(prop);
          if (null != newVal && null != savedVal && !newVal.equals(savedVal)) {
            ThreeWayValue twv = new ThreeWayValue();
            twv.oldStackValue = null;
            twv.newStackValue = normalizeValue(savedVal, newVal.trim());
            twv.savedValue = savedVal.trim();

            if (!result.containsKey(entry.getKey())) {
              result.put(entry.getKey(), new HashMap<String, ThreeWayValue>());
            }

            result.get(entry.getKey()).put(prop, twv);
          }
        }
      }

      Collection<String> common = CollectionUtils.intersection(newPairs.keySet(),
          oldPairs.keySet());

      for (String prop : common) {
        String oldStackVal = oldPairs.get(prop);
        String newStackVal = newPairs.get(prop);
        String savedVal = "";
        if (null != config) {
          savedVal = config.getProperties().get(prop);
        }

        // If values are not defined in stack (null), we skip them
        // Or if values in old stack and in new stack are the same, and value
        // in current config is different, skip it
        if (!(newStackVal == null && oldStackVal == null)
                && !newStackVal.equals(savedVal) &&
            (!oldStackVal.equals(newStackVal) || !oldStackVal.equals(savedVal))) {
          ThreeWayValue twv = new ThreeWayValue();
          twv.oldStackValue = normalizeValue(savedVal, oldStackVal.trim());
          twv.newStackValue = normalizeValue(savedVal, newStackVal.trim());
          twv.savedValue = (null == savedVal) ? null : savedVal.trim();

          if (!result.containsKey(entry.getKey())) {
            result.put(entry.getKey(), new HashMap<String, ThreeWayValue>());
          }

          result.get(entry.getKey()).put(prop, twv);
        }
      }
    }

    return result;
  }

  private void addToMap(Map<String, Map<String, String>> map, Set<PropertyInfo> stackProperties) {
    for (PropertyInfo pi : stackProperties) {
      String type = ConfigHelper.fileNameToConfigType(pi.getFilename());

      if (!map.containsKey(type)) {
        map.put(type, new HashMap<String, String>());
      }
      map.get(type).put(pi.getName(), pi.getValue());
    }

  }
  /**
   * Represents the three different config values for merging.
   */
  public static class ThreeWayValue {
    /**
     * The previous stack-defined value.
     */
    public String oldStackValue;
    /**
     * The new stack-defined value.
     */
    public String newStackValue;
    /**
     * The saved stack value, possibly changed from previous stack-defined value.
     */
    public String savedValue;
  }

  private static String normalizeValue(String templateValue, String newRawValue) {
    if (null == templateValue) {
      return newRawValue;
    }

    Matcher m = HEAP_PATTERN.matcher(templateValue);

    if (m.matches()) {
      return newRawValue + m.group(2);
    }

    return newRawValue;
  }

}
