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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.state.PropertyInfo.PropertyType;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.utils.SecretReference;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

/**
 * Helper class that works with config traversals.
 */
@Singleton
public class ConfigHelper {

  private Clusters clusters = null;
  private AmbariMetaInfo ambariMetaInfo = null;
  private ClusterDAO clusterDAO = null;
  private static final String DELETED = "DELETED_";
  public static final String CLUSTER_DEFAULT_TAG = "tag";
  private final boolean STALE_CONFIGS_CACHE_ENABLED;
  private final int STALE_CONFIGS_CACHE_EXPIRATION_TIME;

  /**
   * Cache for storing stale config flags. Key for cache is hash of [actualConfigs, desiredConfigs, hostName, serviceName,
   * componentName].
   */
  private final Cache<Integer, Boolean> staleConfigsCache;

  private static final Logger LOG =
      LoggerFactory.getLogger(ConfigHelper.class);

  /**
   * List of property prefixes and names. Please keep in alphabetical order.
   */
  public static final String HBASE_SITE = "hbase-site";
  public static final String HDFS_SITE = "hdfs-site";
  public static final String HIVE_SITE = "hive-site";
  public static final String YARN_SITE = "yarn-site";
  public static final String CLUSTER_ENV = "cluster-env";
  public static final String CLUSTER_ENV_ALERT_REPEAT_TOLERANCE = "alerts_repeat_tolerance";
  public static final String CLUSTER_ENV_RETRY_ENABLED = "command_retry_enabled";
  public static final String CLUSTER_ENV_RETRY_COMMANDS = "commands_to_retry";
  public static final String CLUSTER_ENV_RETRY_MAX_TIME_IN_SEC = "command_retry_max_time_in_sec";
  public static final String COMMAND_RETRY_MAX_TIME_IN_SEC_DEFAULT = "600";
  public static final String CLUSTER_ENV_STACK_FEATURES_PROPERTY = "stack_features";
  public static final String CLUSTER_ENV_STACK_TOOLS_PROPERTY = "stack_tools";

  public static final String HTTP_ONLY = "HTTP_ONLY";
  public static final String HTTPS_ONLY = "HTTPS_ONLY";

  /**
   * The tag given to newly created versions.
   */
  public static final String FIRST_VERSION_TAG = "version1";

  @Inject
  public ConfigHelper(Clusters c, AmbariMetaInfo metaInfo, Configuration configuration, ClusterDAO clusterDAO) {
    clusters = c;
    ambariMetaInfo = metaInfo;
    this.clusterDAO = clusterDAO;
    STALE_CONFIGS_CACHE_ENABLED = configuration.isStaleConfigCacheEnabled();
    STALE_CONFIGS_CACHE_EXPIRATION_TIME = configuration.staleConfigCacheExpiration();
    staleConfigsCache = CacheBuilder.newBuilder().
        expireAfterWrite(STALE_CONFIGS_CACHE_EXPIRATION_TIME, TimeUnit.SECONDS).build();
  }

  /**
   * Gets the desired tags for a cluster and host
   *
   * @param cluster  the cluster
   * @param hostName the host name
   * @return a map of tag type to tag names with overrides
   * @throws AmbariException
   */
  public Map<String, Map<String, String>> getEffectiveDesiredTags(
      Cluster cluster, String hostName) throws AmbariException {

    return getEffectiveDesiredTags(cluster, hostName, null);
  }

  /**
   * Gets the desired tags for a cluster and host
   *
   * @param cluster
   *          the cluster
   * @param hostName
   *          the host name
   * @return a map of tag type to tag names with overrides
   * @throws AmbariException
   */
  public Map<String, Map<String, String>> getEffectiveDesiredTags(Cluster cluster, String hostName,
      Map<String, DesiredConfig> desiredConfigs) throws AmbariException {

    Host host = (hostName == null) ? null : clusters.getHost(hostName);
    Map<String, HostConfig> desiredHostConfigs = (host == null) ? null
        : host.getDesiredHostConfigs(cluster, desiredConfigs);

    return getEffectiveDesiredTags(cluster, desiredConfigs, desiredHostConfigs);
  }

  /**
   * Gets the desired tags for a cluster and overrides for a host
   *
   * @param cluster
   *          the cluster
   * @param hostConfigOverrides
   *          the host overrides applied using config groups
   * @param clusterDesired
   *          the desired configurations for the cluster. Obtaining these can be
   *          expensive, ans since this method could be called 10,000's of times
   *          when generating cluster/host responses. Therefore, the caller
   *          should build these once and pass them in. If {@code null}, then
   *          this method will retrieve them at runtime, incurring a performance
   *          penality.
   * @return a map of tag type to tag names with overrides
   */
  private Map<String, Map<String, String>> getEffectiveDesiredTags(
      Cluster cluster, Map<String, DesiredConfig> clusterDesired,
      Map<String, HostConfig> hostConfigOverrides) {

    if (null == cluster) {
      clusterDesired = new HashMap<>();
    }

    // per method contract, lookup if not supplied
    if (null == clusterDesired) {
      clusterDesired = cluster.getDesiredConfigs();
    }

    if (null == clusterDesired) {
      clusterDesired = new HashMap<>();
    }

    Map<String, Map<String, String>> resolved = new TreeMap<String, Map<String, String>>();

    // Do not use host component config mappings.  Instead, the rules are:
    // 1) Use the cluster desired config
    // 2) override (1) with config-group overrides

    for (Entry<String, DesiredConfig> clusterEntry : clusterDesired.entrySet()) {
      String type = clusterEntry.getKey();
      String tag = clusterEntry.getValue().getTag();

      // 1) start with cluster config
      if (cluster != null) {
        Config config = cluster.getConfig(type, tag);
        if (null == config) {
          continue;
        }

        Map<String, String> tags = new LinkedHashMap<String, String>();

        tags.put(CLUSTER_DEFAULT_TAG, config.getTag());

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
   * Get all config attributes for a cluster given a set of configType to
   * versionTags map. This helper method merges all the override tags with a
   * the attributes from parent cluster config properties
   *
   * @param cluster
   * @param desiredTags
   * @return {type : {attribute : {property, attributeValue}}
   */
  public Map<String, Map<String, Map<String, String>>> getEffectiveConfigAttributes(
      Cluster cluster, Map<String, Map<String, String>> desiredTags) {

    Map<String, Map<String, Map<String, String>>> attributes = new HashMap<String, Map<String, Map<String, String>>>();

    if (desiredTags != null) {
      for (Entry<String, Map<String, String>> entry : desiredTags.entrySet()) {

        String type = entry.getKey();
        Map<String, Map<String, String>> attributesMap = null;

        Map<String, String> tags = new HashMap<String, String>(entry.getValue());
        String clusterTag = tags.get(CLUSTER_DEFAULT_TAG);

        if (clusterTag != null) {
          Config config = cluster.getConfig(type, clusterTag);
          if (config != null) {
            attributesMap = new TreeMap<String, Map<String, String>>();
            cloneAttributesMap(config.getPropertiesAttributes(), attributesMap);
          }
          tags.remove(CLUSTER_DEFAULT_TAG);
        }
        for (Entry<String, String> overrideEntry : tags.entrySet()) {
          Config overrideConfig = cluster.getConfig(type,
              overrideEntry.getValue());
          overrideAttributes(overrideConfig, attributesMap);
        }
        if (attributesMap != null) {
          attributes.put(type, attributesMap);
        }
      }
    }

    return attributes;
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
      for (Entry<String, String> entry : override.entrySet()) {
        Boolean deleted = 0 == entry.getKey().indexOf(DELETED);
        String nameToUse = deleted ?
            entry.getKey().substring(DELETED.length()) : entry.getKey();
        if (finalConfig.containsKey(nameToUse)) {
          finalConfig.remove(nameToUse);
        }
        if (!deleted) {
          finalConfig.put(nameToUse, entry.getValue());
        }
      }
    }

    return finalConfig;
  }

  /**
   * Merge override attributes with original ones.
   * If overrideConfig#getPropertiesAttributes does not contain occurrence of override for any of
   * properties from overrideConfig#getProperties then persisted attribute should be removed.
   */
  public Map<String, Map<String, String>> overrideAttributes(Config overrideConfig,
                                                             Map<String, Map<String, String>> persistedAttributes) {
    if (overrideConfig != null && persistedAttributes != null) {
      Map<String, Map<String, String>> overrideAttributes = overrideConfig.getPropertiesAttributes();
      if (overrideAttributes != null) {
        cloneAttributesMap(overrideAttributes, persistedAttributes);
        Map<String, String> overrideProperties = overrideConfig.getProperties();
        if (overrideProperties != null) {
          Set<String> overriddenProperties = overrideProperties.keySet();
          for (String overriddenProperty : overriddenProperties) {
            for (Entry<String, Map<String, String>> persistedAttribute : persistedAttributes.entrySet()) {
              String attributeName = persistedAttribute.getKey();
              Map<String, String> persistedAttributeValues = persistedAttribute.getValue();
              Map<String, String> overrideAttributeValues = overrideAttributes.get(attributeName);
              if (overrideAttributeValues == null || !overrideAttributeValues.containsKey(overriddenProperty)) {
                persistedAttributeValues.remove(overriddenProperty);
              }
            }
          }
        }
      }
    }
    return persistedAttributes;
  }

  public void cloneAttributesMap(Map<String, Map<String, String>> sourceAttributesMap,
                                 Map<String, Map<String, String>> targetAttributesMap) {
    if (sourceAttributesMap != null && targetAttributesMap != null) {
      for (Entry<String, Map<String, String>> attributesEntry : sourceAttributesMap.entrySet()) {
        String attributeName = attributesEntry.getKey();
        if (!targetAttributesMap.containsKey(attributeName)) {
          targetAttributesMap.put(attributeName, new TreeMap<String, String>());
        }
        for (Entry<String, String> attributesValue : attributesEntry.getValue().entrySet()) {
          targetAttributesMap.get(attributeName).put(attributesValue.getKey(), attributesValue.getValue());
        }
      }
    }
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
   * The purpose of this method is to determine if a
   * {@link ServiceComponentHost}'s known actual configs are different than what
   * is set on the cluster (the desired). The following logic is applied:
   * <ul>
   * <li>Desired type does not exist on the SCH (actual)
   * <ul>
   * <li>Type does not exist on the stack: <code>false</code></li>
   * <li>Type exists on the stack: <code>true</code> if the config key is on the
   * stack. otherwise <code>false</code></li>
   * </ul>
   * </li>
   * <li>Desired type exists for the SCH
   * <ul>
   * <li>Desired tags already set for the SCH (actual): <code>false</code></li>
   * <li>Desired tags DO NOT match SCH: <code>true</code> if the changed keys
   * exist on the stack, otherwise <code>false</code></li>
   * </ul>
   * </li>
   * </ul>
   *
   * @param sch
   *          the SCH to calcualte config staleness for (not {@code null}).
   * @param requestDesiredConfigs
   *          the desired configurations for the cluster. Obtaining these can be
   *          expensive and since this method operates on SCH's, it could be
   *          called 10,000's of times when generating cluster/host responses.
   *          Therefore, the caller should build these once and pass them in. If
   *          {@code null}, then this method will retrieve them at runtime,
   *          incurring a performance penality.
   *
   * @return <code>true</code> if the actual configs are stale
   */
  public boolean isStaleConfigs(ServiceComponentHost sch, Map<String, DesiredConfig> requestDesiredConfigs)
      throws AmbariException {
    boolean stale = calculateIsStaleConfigs(sch, requestDesiredConfigs);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Cache configuration staleness for host {} and component {} as {}",
          sch.getHostName(), sch.getServiceComponentName(), stale);
    }
    return stale;
  }

  /**
   * Remove configs by type
   *
   * @param type config Type
   */
  @Transactional
  public void removeConfigsByType(Cluster cluster, String type) {
    Set<String> globalVersions = cluster.getConfigsByType(type).keySet();

    for (String version : globalVersions) {
      ClusterConfigEntity clusterConfigEntity = clusterDAO.findConfig
          (cluster.getClusterId(), type, version);

      clusterDAO.removeConfig(clusterConfigEntity);
    }
  }

  /**
   * Gets all the config dictionary where property with the given name is present in stack definitions
   *
   * @param stackId
   * @param propertyName
   */
  public Set<String> findConfigTypesByPropertyName(StackId stackId, String propertyName, String clusterName) throws AmbariException {
    StackInfo stack = ambariMetaInfo.getStack(stackId.getStackName(),
                                              stackId.getStackVersion());

    Set<String> result = new HashSet<String>();

    for (Service service : clusters.getCluster(clusterName).getServices().values()) {
      Set<PropertyInfo> stackProperties = ambariMetaInfo.getServiceProperties(stack.getName(), stack.getVersion(), service.getName());
      Set<PropertyInfo> stackLevelProperties = ambariMetaInfo.getStackProperties(stack.getName(), stack.getVersion());
      stackProperties.addAll(stackLevelProperties);

      for (PropertyInfo stackProperty : stackProperties) {
        if (stackProperty.getName().equals(propertyName)) {
          String configType = fileNameToConfigType(stackProperty.getFilename());

          result.add(configType);
        }
      }
    }

    return result;
  }

  /**
   * Gets a map of config types to password property names to password property value names,
   * that are credential store enabled.
   *
   * @param stackId
   * @param service
   * @return
   * @throws AmbariException
     */
  public Map<String, Map<String, String>> getCredentialStoreEnabledProperties(StackId stackId, Service service)
          throws AmbariException {
    PropertyType propertyType = PropertyType.PASSWORD;
    StackInfo stack = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
    Map<String, Map<String, String>> result = new HashMap<>();
    Map<String, String> passwordProperties;
    Set<PropertyInfo> serviceProperties = ambariMetaInfo.getServiceProperties(stack.getName(), stack.getVersion(), service.getName());
    for (PropertyInfo serviceProperty : serviceProperties) {
      if (serviceProperty.getPropertyTypes().contains(propertyType)) {
        if (!serviceProperty.getPropertyValueAttributes().isKeyStore())
          continue;
        String stackPropertyConfigType = fileNameToConfigType(serviceProperty.getFilename());
        passwordProperties = result.get(stackPropertyConfigType);
        if (passwordProperties == null) {
          passwordProperties = new HashMap<>();
          result.put(stackPropertyConfigType, passwordProperties);
        }
        // If the password property is used by another property, it means the password property
        // is a password value name while the use is the password alias name. If the user property
        // is from another config type, include that in the password alias name as name:type.
        if (serviceProperty.getUsedByProperties().size() > 0) {
          for (PropertyDependencyInfo usedByProperty : serviceProperty.getUsedByProperties()) {
            String propertyName = usedByProperty.getName();
            if (!StringUtils.isEmpty(usedByProperty.getType())) {
              propertyName += ':' + usedByProperty.getType();
            }
            passwordProperties.put(propertyName, serviceProperty.getName());
          }
        }
        else {
          passwordProperties.put(serviceProperty.getName(), serviceProperty.getName());
        }
      }
    }

    return result;
  }

  public Set<String> getPropertyValuesWithPropertyType(StackId stackId, PropertyType propertyType,
      Cluster cluster, Map<String, DesiredConfig> desiredConfigs) throws AmbariException {
    StackInfo stack = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
    Map<String, Config> actualConfigs = new HashMap<>();
    Set<String> result = new HashSet<String>();

    for (Map.Entry<String, DesiredConfig> desiredConfigEntry : desiredConfigs.entrySet()) {
      String configType = desiredConfigEntry.getKey();
      DesiredConfig desiredConfig = desiredConfigEntry.getValue();
      actualConfigs.put(configType, cluster.getConfig(configType, desiredConfig.getTag()));
    }

    for (Service service : cluster.getServices().values()) {
      Set<PropertyInfo> serviceProperties = ambariMetaInfo.getServiceProperties(stack.getName(), stack.getVersion(), service.getName());
      for (PropertyInfo serviceProperty : serviceProperties) {
        if (serviceProperty.getPropertyTypes().contains(propertyType)) {
          String stackPropertyConfigType = fileNameToConfigType(serviceProperty.getFilename());
          try {
            String property = actualConfigs.get(stackPropertyConfigType).getProperties().get(serviceProperty.getName());
            if (null == property){
              LOG.error(String.format("Unable to obtain property values for %s with property attribute %s. "
                  + "The property does not exist in version %s of %s configuration.",
                  serviceProperty.getName(),
                  propertyType,
                  desiredConfigs.get(stackPropertyConfigType),
                  stackPropertyConfigType
                  ));
            } else {
              result.add(property);
            }
          } catch (Exception ignored) {
          }
        }
      }
    }

    Set<PropertyInfo> stackProperties = ambariMetaInfo.getStackProperties(stack.getName(), stack.getVersion());

    for (PropertyInfo stackProperty : stackProperties) {
      if (stackProperty.getPropertyTypes().contains(propertyType)) {
        String stackPropertyConfigType = fileNameToConfigType(stackProperty.getFilename());
        result.add(actualConfigs.get(stackPropertyConfigType).getProperties().get(stackProperty.getName()));
      }
    }

    return result;
  }

  public String getPropertyValueFromStackDefinitions(Cluster cluster, String configType, String propertyName) throws AmbariException {
    StackId stackId = cluster.getCurrentStackVersion();
    StackInfo stack = ambariMetaInfo.getStack(stackId.getStackName(),
        stackId.getStackVersion());

    for (ServiceInfo serviceInfo : stack.getServices()) {
      Set<PropertyInfo> serviceProperties = ambariMetaInfo.getServiceProperties(stack.getName(), stack.getVersion(), serviceInfo.getName());
      Set<PropertyInfo> stackProperties = ambariMetaInfo.getStackProperties(stack.getName(), stack.getVersion());
      serviceProperties.addAll(stackProperties);

      for (PropertyInfo stackProperty : serviceProperties) {
        String stackPropertyConfigType = fileNameToConfigType(stackProperty.getFilename());

        if (stackProperty.getName().equals(propertyName) && stackPropertyConfigType.equals(configType)) {
          return stackProperty.getValue();
        }
      }

    }

    return null;
  }

  /**
   * Gets the configuration value referenced by the specified placeholder from
   * the cluster configuration. This will take a configuration placeholder such
   * as {{hdfs-site/foo}} and return the value of {@code foo} defined in
   * {@code hdfs-site}.
   *
   * @param cluster     the cluster to use when rendering the placeholder value (not
   *                    {@code null}).
   * @param placeholder the placeholder value, such as {{hdfs-site/foobar}} (not
   *                    {@code null} )
   * @return the configuration value, or {@code null} if none.
   * @throws AmbariException if there was a problem parsing the placeholder or retrieving the
   *                         referenced value.
   */
  public String getPlaceholderValueFromDesiredConfigurations(Cluster cluster,
                                                             String placeholder) {
    // remove the {{ and }} from the placholder
    if (placeholder.startsWith("{{") && placeholder.endsWith("}}")) {
      placeholder = placeholder.substring(2, placeholder.length() - 2).trim();
    }

    // break up hdfs-site/foobar into hdfs-site and foobar
    int delimiterPosition = placeholder.indexOf("/");
    if (delimiterPosition < 0) {
      return placeholder;
    }

    String configType = placeholder.substring(0, delimiterPosition);
    String propertyName = placeholder.substring(delimiterPosition + 1,
        placeholder.length());

    // return the value if it exists, otherwise return the placeholder
    String value = getValueFromDesiredConfigurations(cluster, configType, propertyName);
    return value != null ? value : placeholder;
  }

  public String getValueFromDesiredConfigurations(Cluster cluster, String configType, String propertyName) {
    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    DesiredConfig desiredConfig = desiredConfigs.get(configType);
    if(desiredConfig != null) {
      Config config = cluster.getConfig(configType, desiredConfig.getTag());
      Map<String, String> configurationProperties = config.getProperties();
      if (null != configurationProperties) {
        String value = configurationProperties.get(propertyName);
        if (null != value) {
          return value;
        }
      }
    }
    return null;
  }

  public ServiceInfo getPropertyOwnerService(Cluster cluster, String configType, String propertyName) throws AmbariException {
    StackId stackId = cluster.getCurrentStackVersion();
    StackInfo stack = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());

    for (ServiceInfo serviceInfo : stack.getServices()) {
      Set<PropertyInfo> serviceProperties = ambariMetaInfo.getServiceProperties(stack.getName(), stack.getVersion(), serviceInfo.getName());

      for (PropertyInfo stackProperty : serviceProperties) {
        String stackPropertyConfigType = fileNameToConfigType(stackProperty.getFilename());

        if (stackProperty.getName().equals(propertyName) && stackPropertyConfigType.equals(configType)) {
          return serviceInfo;
        }
      }

    }

    return null;
  }

  public Set<PropertyInfo> getServiceProperties(Cluster cluster, String serviceName) throws AmbariException {
    // The original implementation of this method is to return all properties regardless of whether
    // they should be excluded or not.  By setting removeExcluded to false in the method invocation
    // below, no attempt will be made to remove properties that exist in excluded types.
    return getServiceProperties(cluster.getCurrentStackVersion(), serviceName, false);
  }

  /**
   * Retrieves a Set of PropertyInfo objects containing the relevant properties for the requested
   * service.
   * <p/>
   * If <code>removeExcluded</code> is <code>true</code>, the service's excluded configuration types
   * are used to prune off PropertyInfos that should be ignored; else if <code>false</code>, all
   * PropertyInfos will be returned.
   *
   * @param stackId        a StackId declaring the relevant stack
   * @param serviceName    a String containing the requested service's name
   * @param removeExcluded a boolean value indicating whether to remove properties from excluded
   *                       configuration types (<code>true</code>) or return the complete set of properties regardless of exclusions (<code>false</code>)
   * @return a Set of PropertyInfo objects for the requested service
   * @throws AmbariException if the requested stack or the requested service is not found
   */
  public Set<PropertyInfo> getServiceProperties(StackId stackId, String serviceName, boolean removeExcluded)
      throws AmbariException {
    ServiceInfo service = ambariMetaInfo.getService(stackId.getStackName(), stackId.getStackVersion(), serviceName);
    Set<PropertyInfo> properties = new HashSet<PropertyInfo>(service.getProperties());

    if (removeExcluded) {
      Set<String> excludedConfigTypes = service.getExcludedConfigTypes();

      // excludedConfigTypes can be null since org.apache.ambari.server.state.ServiceInfo.setExcludedConfigTypes()
      // allows for null values
      if ((excludedConfigTypes != null) && !excludedConfigTypes.isEmpty()) {
        // Iterate through the set of found PropertyInfo instances and remove ones that should be
        // excluded.
        Iterator<PropertyInfo> iterator = properties.iterator();

        while (iterator.hasNext()) {
          PropertyInfo propertyInfo = iterator.next();

          // If the config type for the current PropertyInfo is containing within an excluded type,
          // remove it from the set of properties being returned
          if (excludedConfigTypes.contains(ConfigHelper.fileNameToConfigType(propertyInfo.getFilename()))) {
            iterator.remove();
          }
        }
      }
    }

    return properties;
  }

  public Set<PropertyInfo> getStackProperties(Cluster cluster) throws AmbariException {
    StackId stackId = cluster.getCurrentStackVersion();
    StackInfo stack = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());

    return ambariMetaInfo.getStackProperties(stack.getName(), stack.getVersion());
  }

  /**
   * A helper method to create a new {@link Config} for a given configuration
   * type and updates to the current values, if any. This method will perform the following tasks:
   * <ul>
   * <li>Merge the specified updates with the properties of the current version of the
   * configuration</li>
   * <li>Create a {@link Config} in the cluster for the specified type. This
   * will have the proper versions and tags set automatically.</li>
   * <li>Set the cluster's {@link DesiredConfig} to the new configuration</li>
   * <li>Create an entry in the configuration history with a note and username.</li>
   * <ul>
   *
   * @param cluster
   * @param controller
   * @param configType
   * @param updates
   * @param removals a collection of property names to remove from the configuration type
   * @param authenticatedUserName
   * @param serviceVersionNote
   * @throws AmbariException
   */
  public void updateConfigType(Cluster cluster,
                               AmbariManagementController controller, String configType,
                               Map<String, String> updates, Collection<String> removals,
                               String authenticatedUserName,
                               String serviceVersionNote) throws AmbariException {

    // Nothing to update or remove
    if (configType == null ||
      (updates == null || updates.isEmpty()) &&
      (removals == null || removals.isEmpty())) {
      return;
    }

    Config oldConfig = cluster.getDesiredConfigByType(configType);
    Map<String, String> oldConfigProperties;
    Map<String, String> properties = new HashMap<String, String>();
    Map<String, Map<String, String>> propertiesAttributes =
      new HashMap<String, Map<String, String>>();

    if (oldConfig == null) {
      oldConfigProperties = null;
    } else {
      oldConfigProperties = oldConfig.getProperties();
      if (oldConfigProperties != null) {
        properties.putAll(oldConfigProperties);
      }
      if (oldConfig.getPropertiesAttributes() != null) {
        propertiesAttributes.putAll(oldConfig.getPropertiesAttributes());
      }
    }

    if (updates != null) {
      properties.putAll(updates);
    }

    // Remove properties that need to be removed.
    if (removals != null) {
      for (String propertyName : removals) {
        properties.remove(propertyName);
        for (Map<String, String> attributesMap: propertiesAttributes.values()) {
          attributesMap.remove(propertyName);
        }
      }
    }

    if ((oldConfigProperties == null)
      || !Maps.difference(oldConfigProperties, properties).areEqual()) {
      createConfigType(cluster, controller, configType, properties,
        propertiesAttributes, authenticatedUserName, serviceVersionNote);
    }
  }

  private void createConfigType(Cluster cluster,
                               AmbariManagementController controller,
                               String configType, Map<String, String> properties,
                               Map<String, Map<String, String>> propertyAttributes,
                               String authenticatedUserName,
                               String serviceVersionNote) throws AmbariException {

    // create the configuration history entry
    Config baseConfig = createConfig(cluster, controller, configType, FIRST_VERSION_TAG, properties,
        propertyAttributes);

    if (baseConfig != null) {
      cluster.addDesiredConfig(authenticatedUserName,
          Collections.singleton(baseConfig), serviceVersionNote);
    }
  }

  /**
   * A helper method to create a new {@link Config} for a given configuration
   * type. This method will perform the following tasks:
   * <ul>
   * <li>Create a {@link Config} in the cluster for the specified type. This
   * will have the proper versions and tags set automatically.</li>
   * <li>Set the cluster's {@link DesiredConfig} to the new configuration</li>
   * <li>Create an entry in the configuration history with a note and username.</li>
   * <ul>
   *
   * @param cluster
   * @param controller
   * @param configType
   * @param properties
   * @param authenticatedUserName
   * @param serviceVersionNote
   * @throws AmbariException
   */
  public void createConfigType(Cluster cluster,
                               AmbariManagementController controller,
                               String configType, Map<String, String> properties,
                               String authenticatedUserName,
                               String serviceVersionNote) throws AmbariException {
    createConfigType(cluster, controller, configType, properties,
      new HashMap<String, Map<String, String>>(), authenticatedUserName,
      serviceVersionNote);
  }

  /**
   * Create configurations and assign them for services.
   * @param cluster               the cluster
   * @param controller            the controller
   * @param batchProperties       the type->config map batch of properties
   * @param authenticatedUserName the user that initiated the change
   * @param serviceVersionNote    the service version note
   * @throws AmbariException
   */
  public void createConfigTypes(Cluster cluster,
      AmbariManagementController controller,
      Map<String, Map<String, String>> batchProperties, String authenticatedUserName,
      String serviceVersionNote) throws AmbariException {

    Map<String, Set<Config>> serviceMapped = new HashMap<String, Set<Config>>();

    for (Map.Entry<String, Map<String, String>> entry : batchProperties.entrySet()) {
      String type = entry.getKey();
      Map<String, String> properties = entry.getValue();

      Config baseConfig = createConfig(cluster, controller, type, FIRST_VERSION_TAG, properties,
        Collections.<String, Map<String,String>>emptyMap());

      if (null != baseConfig) {
        try {
          String service = cluster.getServiceForConfigTypes(Collections.singleton(type));
          if (!serviceMapped.containsKey(service)) {
            serviceMapped.put(service, new HashSet<Config>());
          }
          serviceMapped.get(service).add(baseConfig);

        } catch (Exception e) {
          // !!! ignore
        }
      }
    }

    // create the configuration history entries
    for (Set<Config> configs : serviceMapped.values()) {
      if (!configs.isEmpty()) {
        cluster.addDesiredConfig(authenticatedUserName, configs, serviceVersionNote);
      }
    }

  }

  Config createConfig(Cluster cluster, AmbariManagementController controller, String type, String tag,
                      Map<String, String> properties, Map<String, Map<String, String>> propertyAttributes) throws AmbariException {
    if (cluster.getConfigsByType(type) != null) {
      tag = "version" + System.currentTimeMillis();
    }

    Map<PropertyType, Set<String>> propertiesTypes = cluster.getConfigPropertiesTypes(type);
    if(propertiesTypes.containsKey(PropertyType.PASSWORD)) {
      for(String passwordProperty : propertiesTypes.get(PropertyType.PASSWORD)) {
        if(properties.containsKey(passwordProperty)) {
          String passwordPropertyValue = properties.get(passwordProperty);
          if (!SecretReference.isSecret(passwordPropertyValue)) {
            continue;
          }
          SecretReference ref = new SecretReference(passwordPropertyValue, cluster);
          String refValue = ref.getValue();
          properties.put(passwordProperty, refValue);
        }
      }
    }

    return controller.createConfig(cluster, type, properties, tag, propertyAttributes);
  }

  /**
   * Gets the default properties from the specified stack and services when a
   * cluster is first installed.
   *
   * @param stack
   *          the stack to pull stack-values from (not {@code null})
   * @param cluster
   *          the cluster to use when determining which services default
   *          configurations to include (not {@code null}).
   * @param onStackUpgradeFilter if true skip {@code <on-stack-upgrade merge="false"/>} properties
   * @return a mapping of configuration type to map of key/value pairs for the
   *         default configurations.
   * @throws AmbariException
   */
  public Map<String, Map<String, String>> getDefaultProperties(StackId stack, Cluster cluster, boolean onStackUpgradeFilter)
      throws AmbariException {
    Map<String, Map<String, String>> defaultPropertiesByType = new HashMap<String, Map<String, String>>();

    // populate the stack (non-service related) properties first
    Set<org.apache.ambari.server.state.PropertyInfo> stackConfigurationProperties = ambariMetaInfo.getStackProperties(
        stack.getStackName(), stack.getStackVersion());

    for (PropertyInfo stackDefaultProperty : stackConfigurationProperties) {
      String type = ConfigHelper.fileNameToConfigType(stackDefaultProperty.getFilename());

      if (!defaultPropertiesByType.containsKey(type)) {
        defaultPropertiesByType.put(type, new HashMap<String, String>());
      }
      if (!onStackUpgradeFilter || stackDefaultProperty.getPropertyStackUpgradeBehavior().isMerge()) {
        defaultPropertiesByType.get(type).put(stackDefaultProperty.getName(),
            stackDefaultProperty.getValue());
      }
    }

    // for every installed service, populate the default service properties
    for (String serviceName : cluster.getServices().keySet()) {
      Set<org.apache.ambari.server.state.PropertyInfo> serviceConfigurationProperties = ambariMetaInfo.getServiceProperties(
          stack.getStackName(), stack.getStackVersion(), serviceName);

      // !!! use new stack as the basis
      for (PropertyInfo serviceDefaultProperty : serviceConfigurationProperties) {
        String type = ConfigHelper.fileNameToConfigType(serviceDefaultProperty.getFilename());

        if (!defaultPropertiesByType.containsKey(type)) {
          defaultPropertiesByType.put(type, new HashMap<String, String>());
        }
        if (!onStackUpgradeFilter || serviceDefaultProperty.getPropertyStackUpgradeBehavior().isMerge()) {
          defaultPropertiesByType.get(type).put(serviceDefaultProperty.getName(),
              serviceDefaultProperty.getValue());
        }
      }
    }

    return defaultPropertiesByType;
  }

  /**
   * Gets whether configurations are stale for a given service host component.
   *
   * @param sch
   *          the SCH to calcualte config staleness for (not {@code null}).
   * @param desiredConfigs
   *          the desired configurations for the cluster. Obtaining these can be
   *          expensive and since this method operates on SCH's, it could be
   *          called 10,000's of times when generating cluster/host responses.
   *          Therefore, the caller should build these once and pass them in. If
   *          {@code null}, then this method will retrieve them at runtime,
   *          incurring a performance penality.
   * @return
   * @throws AmbariException
   */
  private boolean calculateIsStaleConfigs(ServiceComponentHost sch,
      Map<String, DesiredConfig> desiredConfigs) throws AmbariException {

    if (sch.isRestartRequired()) {
      return true;
    }

    Map<String, HostConfig> actual = sch.getActualConfigs();
    if (null == actual || actual.isEmpty()) {
      return false;
    }

    Cluster cluster = clusters.getClusterById(sch.getClusterId());

    Map<String, Map<String, String>> desired = getEffectiveDesiredTags(cluster, sch.getHostName(),
        desiredConfigs);

    Boolean stale = null;
    int staleHash = 0;
    if (STALE_CONFIGS_CACHE_ENABLED){
      staleHash = Objects.hashCode(actual.hashCode(),
          desired.hashCode(),
          sch.getHostName(),
          sch.getServiceComponentName(),
          sch.getServiceName());
      stale = staleConfigsCache.getIfPresent(staleHash);
      if(stale != null) {
        return stale;
      }
    }

    stale = false;

    StackId stackId = cluster.getDesiredStackVersion();

    ServiceInfo serviceInfo = ambariMetaInfo.getService(stackId.getStackName(),
        stackId.getStackVersion(), sch.getServiceName());

    ComponentInfo componentInfo = serviceInfo.getComponentByName(sch.getServiceComponentName());
    // Configs are considered stale when:
    // - desired type DOES NOT exist in actual
    // --- desired type DOES NOT exist in stack: not_stale
    // --- desired type DOES exist in stack: check stack for any key: stale
    // - desired type DOES exist in actual
    // --- desired tags DO match actual tags: not_stale
    // --- desired tags DO NOT match actual tags
    // ---- merge values, determine changed keys, check stack: stale

    Iterator<Entry<String, Map<String, String>>> it = desired.entrySet().iterator();

    while (it.hasNext() && !stale) {
      Entry<String, Map<String, String>> desiredEntry = it.next();

      String type = desiredEntry.getKey();
      Map<String, String> tags = desiredEntry.getValue();

      if (!actual.containsKey(type)) {
        // desired is set, but actual is not
        if (!serviceInfo.hasConfigDependency(type)) {
          stale = componentInfo != null && componentInfo.hasConfigType(type);
        } else {
          stale = true;
        }
      } else {
        // desired and actual both define the type
        HostConfig hc = actual.get(type);
        Map<String, String> actualTags = buildTags(hc);

        if (!isTagChanged(tags, actualTags, hasGroupSpecificConfigsForType(cluster, sch.getHostName(), type))) {
          stale = false;
        } else {
          stale = serviceInfo.hasConfigDependency(type) || componentInfo.hasConfigType(type);
        }
      }
    }
    if (STALE_CONFIGS_CACHE_ENABLED) {
      staleConfigsCache.put(staleHash, stale);
    }
    return stale;
  }

  /**
   * Determines if the hostname has group specific configs for the type specified
   *
   * @param cluster
   * @param hostname of the host to look for
   * @param type     the type to look for (e.g. flume-conf)
   * @return <code>true</code> if the hostname has group specific configuration for the type
   */
  private boolean hasGroupSpecificConfigsForType(Cluster cluster, String hostname, String type) {
    try {
      Map<Long, ConfigGroup> configGroups = cluster.getConfigGroupsByHostname(hostname);
      if (configGroups != null && !configGroups.isEmpty()) {
        for (ConfigGroup configGroup : configGroups.values()) {
          Config config = configGroup.getConfigurations().get(type);
          if (config != null) {
            return true;
          }
        }
      }
    } catch (AmbariException ambariException) {
      LOG.warn("Could not determine group configuration for host. Details: " + ambariException.getMessage());
    }
    return false;
  }

  /**
   * @return <code>true</code> if any service on the stack defines a property
   * for the type.
   */
  private boolean hasPropertyFor(StackId stack, String type,
                                 Collection<String> keys) throws AmbariException {

    for (ServiceInfo svc : ambariMetaInfo.getServices(stack.getStackName(),
        stack.getStackVersion()).values()) {

      if (svc.hasDependencyAndPropertyFor(type, keys)) {
        return true;
      }

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
      if (null != config) {
        desiredValues.putAll(config.getProperties());
      }
    }

    for (String tag : actualTags) {
      Config config = cluster.getConfig(type, tag);
      if (null != config) {
        actualValues.putAll(config.getProperties());
      }
    }

    List<String> keys = new ArrayList<String>();

    for (Entry<String, String> entry : desiredValues.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (!actualValues.containsKey(key)) {
        keys.add(key);
      } else if (!actualValues.get(key).equals(value)) {
        keys.add(key);
      }
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
  private boolean isTagChanged(Map<String, String> desiredTags, Map<String, String> actualTags, boolean groupSpecificConfigs) {
    if (!actualTags.get(CLUSTER_DEFAULT_TAG).equals(desiredTags.get(CLUSTER_DEFAULT_TAG))) {
      return true;
    }

    // cluster level configs are already compared for staleness, now they match
    // if the host has group specific configs for type we should ignore the cluster level configs and compare specifics
    if (groupSpecificConfigs) {
      actualTags.remove(CLUSTER_DEFAULT_TAG);
      desiredTags.remove(CLUSTER_DEFAULT_TAG);
    }

    Set<String> desiredSet = new HashSet<String>(desiredTags.values());
    Set<String> actualSet = new HashSet<String>(actualTags.values());

    // Both desired and actual should be exactly the same
    return !desiredSet.equals(actualSet);
  }

  /**
   * @return the list of combined config property names
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


  public static String fileNameToConfigType(String filename) {
    int extIndex = filename.indexOf(AmbariMetaInfo.SERVICE_CONFIG_FILE_NAME_POSTFIX);
    return filename.substring(0, extIndex);
  }

  /**
   * Removes properties from configurations that marked as hidden for specified component.
   * @param configurations cluster configurations
   * @param attributes configuration attributes
   * @param componentName component name
   * @param configDownload indicates if config must be downloaded
   */
  public static void processHiddenAttribute(Map<String, Map<String, String>> configurations,
                                            Map<String, Map<String, Map<String, String>>> attributes,
                                            String componentName, boolean configDownload){
    if (configurations != null && attributes != null && componentName != null) {
      for(Map.Entry<String, Map<String,String>> confEntry : configurations.entrySet()){
        String configTag = confEntry.getKey();
        Map<String,String> confProperties = confEntry.getValue();
        if(attributes.containsKey(configTag)){
          Map<String, Map<String, String>> configAttributes = attributes.get(configTag);
          if(configAttributes.containsKey("hidden")){
            Map<String,String> hiddenProperties = configAttributes.get("hidden");
            if(hiddenProperties != null) {
              for (Map.Entry<String, String> hiddenEntry : hiddenProperties.entrySet()) {
                String propertyName = hiddenEntry.getKey();
                String components = hiddenEntry.getValue();
                // hide property if we are downloading config & CONFIG_DOWNLOAD defined,
                // otherwise - check if we have matching component name
                if ((configDownload ? components.contains("CONFIG_DOWNLOAD") : components.contains(componentName))
                    && confProperties.containsKey(propertyName)) {
                  confProperties.remove(propertyName);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Merge one attribute map to another.
   * @param attributes original map
   * @param additionalAttributes map with additional attributes
   */
  public static void mergeConfigAttributes(Map<String, Map<String, String>> attributes, Map<String, Map<String, String>> additionalAttributes){
    for(Map.Entry<String, Map<String, String>> attrEntry: additionalAttributes.entrySet()){
      String attributeName = attrEntry.getKey();
      Map<String, String> attributeProperties = attrEntry.getValue();
      if(!attributes.containsKey(attributeName)) {
        attributes.put(attributeName, attributeProperties);
      } else {
        attributes.get(attributeName).putAll(attributeProperties);
      }
    }
  }

}
