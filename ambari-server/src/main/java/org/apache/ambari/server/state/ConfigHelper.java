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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.google.inject.Singleton;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;

import com.google.inject.Inject;
import org.apache.ambari.server.configuration.Configuration;

/**
 * Helper class that works with config traversals.
 */
@Singleton
public class ConfigHelper {

  private Clusters clusters = null;
  private AmbariMetaInfo ambariMetaInfo = null;
  private static String DELETED = "DELETED_";
  public static final String CLUSTER_DEFAULT_TAG = "tag";
  
  @Inject
  public ConfigHelper(Clusters c, AmbariMetaInfo metaInfo) {
    clusters = c;
    ambariMetaInfo = metaInfo; 
  }
  
  /**
   * Gets the desired tags for a cluster and host
   * @param cluster the cluster
   * @param hostName the host name
   * @return a map of tag type to tag names with overrides
   * @throws AmbariException
   */
  public Map<String, Map<String, String>> getEffectiveDesiredTags(
      Cluster cluster, String hostName) throws AmbariException {
    
    Host host = clusters.getHost(hostName);
    
    return getEffectiveDesiredTags(cluster, host.getDesiredHostConfigs(cluster));
  }

  /**
   * Gets the desired tags for a cluster and overrides for a host
   * @param cluster the cluster
   * @param hostConfigOverrides the host overrides applied using config groups
   * @return a map of tag type to tag names with overrides
   * @throws AmbariException
   */
  private Map<String, Map<String, String>> getEffectiveDesiredTags(
      Cluster cluster, Map<String, HostConfig> hostConfigOverrides)
        throws AmbariException {
    
    Map<String, DesiredConfig> clusterDesired = cluster.getDesiredConfigs();
    
    Map<String, Map<String,String>> resolved = new TreeMap<String, Map<String, String>>();
    
    // Do not use host component config mappings.  Instead, the rules are:
    // 1) Use the cluster desired config
    // 2) override (1) with config-group overrides
    
    for (Entry<String, DesiredConfig> clusterEntry : clusterDesired.entrySet()) {
      String type = clusterEntry.getKey();
      String tag = clusterEntry.getValue().getVersion();

      // 1) start with cluster config
      Config config = cluster.getConfig(type, tag);
      if (null == config) {
        continue;
      }

      Map<String, String> tags = new LinkedHashMap<String, String>();

      tags.put(CLUSTER_DEFAULT_TAG, config.getVersionTag());

      // AMBARI-3672. Only consider Config groups for override tags
      // tags -> (configGroupId, versionTag)
      if (hostConfigOverrides != null) {
        HostConfig hostConfig = hostConfigOverrides.get(config.getType());
        if (hostConfig != null) {
          for (Entry<Long, String> tagEntry : hostConfig
              .getConfigGroupOverrides().entrySet()) {
            tags.put(tagEntry.getKey().toString(), tagEntry.getValue());
          }
        }
      }

      resolved.put(type, tags);
    }

    return resolved;
  }

  /**
   * Get all config properties for a cluster given a set of configType to
   * versionTags map. This helper method merges all the override tags with a
   * the properties from parent cluster config properties
   *
   * @param cluster
   * @param desiredTags
   * @return {type : {key, value}}
   */
  public Map<String, Map<String, String>> getEffectiveConfigProperties(
    Cluster cluster, Map<String, Map<String, String>> desiredTags) {

    Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();

    if (desiredTags != null) {
      for (Entry<String, Map<String, String>> entry : desiredTags.entrySet()) {
        String type = entry.getKey();
        Map<String, String> propertyMap = properties.get(type);
        if (propertyMap == null) {
          propertyMap = new HashMap<String, String>();
        }

        Map<String, String> tags = new HashMap<String, String>(entry.getValue());
        String clusterTag = tags.get(CLUSTER_DEFAULT_TAG);

        // Overrides is only supported if the config type exists at cluster
        // level
        if (clusterTag != null) {
          Config config = cluster.getConfig(type, clusterTag);
          if (config != null) {
            propertyMap.putAll(config.getProperties());
          }
          tags.remove(CLUSTER_DEFAULT_TAG);
          // Now merge overrides
          for (Entry<String, String> overrideEntry : tags.entrySet()) {
            Config overrideConfig = cluster.getConfig(type,
              overrideEntry.getValue());

            if (overrideConfig != null) {
              propertyMap = getMergedConfig(propertyMap, overrideConfig.getProperties());
            }
          }
        }
        properties.put(type, propertyMap);
      }
    }

    return properties;
  }

  /**
   * Merge override with original, if original property doesn't exist,
   * add it to the properties
   *
   * @param persistedClusterConfig
   * @param override
   * @return
   */
  public Map<String, String> getMergedConfig(Map<String,
      String> persistedClusterConfig, Map<String, String> override) {

    Map<String, String> finalConfig = new HashMap<String, String>(persistedClusterConfig);

    if (override != null && override.size() > 0) {
      for (String overrideKey : override.keySet()) {
        Boolean deleted = 0 == overrideKey.indexOf(DELETED);
        String nameToUse = deleted ? overrideKey.substring(DELETED.length()) : overrideKey;
        if (finalConfig.containsKey(nameToUse)) {
          finalConfig.remove(nameToUse);
        }
        if (!deleted) {
          finalConfig.put(nameToUse, override.get(overrideKey));
        }
      }
    }

    return finalConfig;
  }

  public void applyCustomConfig(Map<String, Map<String, String>> configurations,
      String type, String name, String value, Boolean deleted) {
    if (!configurations.containsKey(type)) {
      configurations.put(type, new HashMap<String, String>());
    }
    String nameToUse = deleted ? DELETED + name : name;
    Map<String, String> properties = configurations.get(type);
    if (properties.containsKey(nameToUse)) {
      properties.remove(nameToUse);
    }
    properties.put(nameToUse, value);
  }
  
  /**
   * The purpose of this method is to determine if a {@link ServiceComponentHost}'s
   * known actual configs are different than what is set on the cluster (the desired).
   * The following logic is applied:
   * <ul>
   *   <li>Desired type does not exist on the SCH (actual)
   *     <ul>
   *       <li>Type does not exist on the stack: <code>false</code></li>
   *       <li>Type exists on the stack: <code>true</code> if the config key is on the stack.
   *         otherwise <code>false</code></li>
   *     </ul>
   *   </li>
   *   <li> Desired type exists for the SCH
   *     <ul>
   *       <li>Desired tags already set for the SCH (actual): <code>false</code></li>
   *       <li>Desired tags DO NOT match SCH: <code>true</code> if the changed keys
   *         exist on the stack, otherwise <code>false</code></li>
   *     </ul>
   *   </li>
   * </ul>
   * @param @ServiceComponentHost
   * @return <code>true</code> if the actual configs are stale
   */
  public boolean isStaleConfigs(ServiceComponentHost sch) throws AmbariException {

    Map<String, HostConfig> actual = sch.getActualConfigs();
    if (null == actual || actual.isEmpty())
      return false;
    
    Cluster cluster = clusters.getClusterById(sch.getClusterId());
    StackId stackId = cluster.getDesiredStackVersion();
    
    Map<String, Map<String, String>> desired = getEffectiveDesiredTags(cluster,
        sch.getHostName());
    
    ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), sch.getServiceName());

    // Configs are considered stale when:
    // - desired type DOES NOT exist in actual
    // --- desired type DOES NOT exist in stack: not_stale
    // --- desired type DOES exist in stack: check stack for any key: stale
    // - desired type DOES exist in actual
    // --- desired tags DO match actual tags: not_stale
    // --- desired tags DO NOT match actual tags
    // ---- merge values, determine changed keys, check stack: stale
    
    boolean stale = false;

    Iterator<Entry<String, Map<String, String>>> it = desired.entrySet().iterator();
    
    while (it.hasNext() && !stale) {
      Entry<String, Map<String, String>> desiredEntry = it.next();
      
      String type = desiredEntry.getKey();
      Map<String, String> tags = desiredEntry.getValue();
      
      if (!actual.containsKey(type)) {
        // desired is set, but actual is not
        if (!serviceInfo.hasConfigType(type)) {
          stale = false;
        } else if (type.equals(Configuration.GLOBAL_CONFIG_TAG)) {
          // find out if the keys are stale by first checking the target service,
          // then all services
          Collection<String> keys = mergeKeyNames(cluster, type, tags.values());
          
          if (serviceInfo.hasPropertyFor(type, keys) || !hasPropertyFor(stackId, type, keys)) {
            stale = true;
          }
        } else {
          stale = true;
        }
      } else {
        // desired and actual both define the type
        HostConfig hc = actual.get(type);
        Map<String, String> actualTags = buildTags(hc);
        
        if (!isTagChanged(tags, actualTags)) {
          stale = false;
        } else if (type.equals(Configuration.GLOBAL_CONFIG_TAG)) {
          // tags are changed, need to find out what has changed,
          // and if it applies
          // to the service
          Collection<String> changed = findChangedKeys(cluster, type, tags.values(), actualTags.values());
          if (serviceInfo.hasPropertyFor(type, changed)) {
            stale = true;
          }
        } else if (!serviceInfo.hasConfigType(type)) {
          stale = false;
        } else {
          stale = true;
        }
      }
    }
    return stale;
  }

  /**
   * @return <code>true</code> if any service on the stack defines a property
   * for the type.
   */

  private boolean hasPropertyFor(StackId stack, String type,
      Collection<String> keys) throws AmbariException {

    for (ServiceInfo svc : ambariMetaInfo.getServices(stack.getStackName(),
        stack.getStackVersion()).values()) {
      
      if (svc.hasPropertyFor(type, keys))
        return true;
      
    }
    
    return false;
  }
  
  /**
   * @return the keys that have changed values
   */
  private Collection<String> findChangedKeys(Cluster cluster, String type,
      Collection<String> desiredTags, Collection<String> actualTags) {
    
    Map<String, String> desiredValues = new HashMap<String, String>();
    Map<String, String> actualValues = new HashMap<String, String>();
    
    for (String tag : desiredTags) {
      Config config = cluster.getConfig(type, tag);
      if (null != config)
        desiredValues.putAll(config.getProperties());
    }
    
    for (String tag : actualTags) {
      Config config = cluster.getConfig(type, tag);
      if (null != config)
        actualValues.putAll(config.getProperties());
    }
    
    List<String> keys = new ArrayList<String>();
    
    for (Entry<String, String> entry : desiredValues.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      
      if (!actualValues.containsKey(key))
        keys.add(key);
      else if (!actualValues.get(key).equals(value))
        keys.add(key);
    }
    
    return keys;
  }
  
  /**
   * @return the map of tags for a desired config
   */
  private Map<String, String> buildTags(HostConfig hc) {
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put(CLUSTER_DEFAULT_TAG, hc.getDefaultVersionTag());
    if (hc.getConfigGroupOverrides() != null) {
      for (Entry<Long, String> entry : hc.getConfigGroupOverrides().entrySet()) {
        map.put(entry.getKey().toString(), entry.getValue());
      }
    }
    return map;
  }
  
  /**
   * @return true if the tags are different in any way, even if not-specified
   */
  private boolean isTagChanged(Map<String, String> desiredTags, Map<String, String> actualTags) {
    if (!actualTags.get(CLUSTER_DEFAULT_TAG).equals(desiredTags.get(CLUSTER_DEFAULT_TAG)))
      return true;

    Set<String> desiredSet = new HashSet<String>(desiredTags.keySet());
    Set<String> actualSet = new HashSet<String>(actualTags.keySet());

    desiredSet.removeAll(actualSet);

    if (!desiredSet.isEmpty())
      return true;

    return false;
  }

  /**
   * @return  the list of combined config property names
   */
  private Collection<String> mergeKeyNames(Cluster cluster, String type, Collection<String> tags) {
    Set<String> names = new HashSet<String>();
    
    for (String tag : tags) {
      Config config = cluster.getConfig(type, tag);
      if (null != config) {
        names.addAll(config.getProperties().keySet());
      }
    }
    
    return names;
  }

  
  
}
