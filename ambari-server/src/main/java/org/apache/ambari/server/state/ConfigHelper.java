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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;

import com.google.inject.Inject;

/**
 * Helper class that works with config traversals.
 */
public class ConfigHelper {

  private Clusters clusters = null;
  private AmbariMetaInfo ambariMetaInfo = null;
  
  @Inject
  public ConfigHelper(Clusters c, AmbariMetaInfo metaInfo) {
    clusters = c;
    ambariMetaInfo = metaInfo; 
  }
  
  /**
   * Gets the desired tags for a cluster and host
   * @param cluster the cluster
   * @param serviceName the optional service name
   * @param hostName the host name
   * @return a map of tag type to tag names with overrides
   * @throws AmbariException
   */
  public Map<String, Map<String, String>> getEffectiveDesiredTags(
      Cluster cluster, String serviceName, String hostName) throws AmbariException {
    
    Host host = clusters.getHost(hostName);
    Map<String, DesiredConfig> hostDesired = host.getDesiredConfigs(cluster.getClusterId());
    
    return getEffectiveDesiredTags(cluster, serviceName, hostDesired);
  }

  /**
   * Gets the desired tags for a cluster and host
   * @param cluster the cluster
   * @param serviceName the optional service name
   * @param hostDesired the optional host desired configs
   * @return a map of tag type to tag names with overrides
   * @throws AmbariException
   */
  public Map<String, Map<String, String>> getEffectiveDesiredTags(
      Cluster cluster, String serviceName, Map<String, DesiredConfig> hostDesired) throws AmbariException {
    
    Map<String, DesiredConfig> clusterDesired = cluster.getDesiredConfigs();
    
    Map<String, Map<String,String>> resolved = new TreeMap<String, Map<String, String>>();
    
    // Do not use host component config mappings.  Instead, the rules are:
    // 1) Use the cluster desired config
    // 2) override (1) with service-specific overrides
    // 3) override (2) with host-specific overrides
    
    for (Entry<String, DesiredConfig> clusterEntry : clusterDesired.entrySet()) {
        String type = clusterEntry.getKey();
        String tag = clusterEntry.getValue().getVersion();
        
        // 1) start with cluster config
        Config config = cluster.getConfig(type, tag);
        if (null == config) {
          continue;
        }

        Map<String, String> tags = new LinkedHashMap<String, String>();
        
        tags.put("tag", config.getVersionTag());
        
        // 2) apply the service overrides, if any are defined with different tags
        if (null != serviceName) {
          Service service = cluster.getService(serviceName);
          Config svcConfig = service.getDesiredConfigs().get(type);
          if (null != svcConfig && !svcConfig.getVersionTag().equals(tag)) {
            tags.put("service_override_tag", svcConfig.getVersionTag());
          }
        }

        if (null != hostDesired) {
          // 3) apply the host overrides, if any
          DesiredConfig dc = hostDesired.get(type);
  
          if (null != dc) {
            Config hostConfig = cluster.getConfig(type, dc.getVersion());
            if (null != hostConfig) {
              tags.put("host_override_tag", hostConfig.getVersionTag());
            }
          }
        }
        
        resolved.put(type, tags);
      }
    
    return resolved;
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
   * @param serviceComponentHostImpl
   * @return <code>true</code> if the actual configs are stale
   */
  public boolean isStaleConfigs(ServiceComponentHost sch) throws AmbariException {

    Map<String, DesiredConfig> actual = sch.getActualConfigs();
    if (null == actual || actual.isEmpty())
      return false;
    
    Cluster cluster = clusters.getClusterById(sch.getClusterId());
    StackId stackId = cluster.getDesiredStackVersion();
    
    Map<String, Map<String, String>> desired = getEffectiveDesiredTags(cluster,
        sch.getServiceName(), sch.getHostName());
    
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
        } else {
          // find out if the keys are stale by first checking the target service,
          // then all services
          Collection<String> keys = mergeKeyNames(cluster, type, tags.values());
          
          if (serviceInfo.hasPropertyFor(type, keys) || !hasPropertyFor(stackId, type, keys))
            stale = true;
        }
      } else {
        // desired and actual both define the type
        DesiredConfig dc = actual.get(type);
        Map<String, String> actualTags = buildTags(dc);
        
        if (!isTagChange(tags, actualTags)) {
          stale = false;
        } else {
          // tags are change, need to find out what has changed, and if it applies
          // to the service
          Collection<String> changed = findChangedKeys(cluster, type, tags.values(), actualTags.values());
          if (serviceInfo.hasPropertyFor(type, changed)) {
            stale = true;
          }
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
  private Map<String, String> buildTags(DesiredConfig dc) {
    Map<String, String> map = new LinkedHashMap<String, String>();
    map.put("tag", dc.getVersion());
    if (null != dc.getServiceName())
      map.put("service_override_tag", dc.getServiceName());
    if (0 != dc.getHostOverrides().size())
      map.put("host_override_tag", dc.getHostOverrides().get(0).getVersionTag());
    
    return map;
  }
  
  /**
   * @return true if the tags are different in any way, even if not-specified
   */
  private boolean isTagChange(Map<String, String> desiredTags, Map<String, String> actualTags) {
    if (!actualTags.get("tag").equals (desiredTags.get("tag")))
      return true;
    
    String tag0 = actualTags.get("service_override_tag"); 
    String tag1 = desiredTags.get("service_override_tag");
    tag0 = (null == tag0) ? "" : tag0;
    tag1 = (null == tag1) ? "" : tag1;
    if (!tag0.equals(tag1))
      return true;
    
    // desired config can only have one value here since it's from the HC.
    tag0 = actualTags.get("host_override_tag");
    tag1 = desiredTags.get("host_override_tag");
    tag0 = (null == tag0) ? "" : tag0;
    tag1 = (null == tag1) ? "" : tag1;
    if (!tag0.equals(tag1))
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
