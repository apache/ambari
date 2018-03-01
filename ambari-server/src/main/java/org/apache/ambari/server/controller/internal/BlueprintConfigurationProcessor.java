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

package org.apache.ambari.server.controller.internal;


import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.AdvisedConfiguration;
import org.apache.ambari.server.topology.Cardinality;
import org.apache.ambari.server.topology.ClusterTopology;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.validators.UnitValidatedProperty;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Updates configuration properties based on cluster topology.  This is done when exporting
 * a blueprint and when a cluster is provisioned via a blueprint.
 */
// TODO move to topology package
public class BlueprintConfigurationProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(BlueprintConfigurationProcessor.class);

  /**
   * Compiled regex for "%HOSTGROUP::...%" token.
   */
  public static final Pattern HOST_GROUP_PLACEHOLDER_PATTERN = Pattern.compile("%HOSTGROUP::(\\S+?)%");

  private final static String COMMAND_RETRY_ENABLED_PROPERTY_NAME = "command_retry_enabled";

  private final static String COMMANDS_TO_RETRY_PROPERTY_NAME = "commands_to_retry";

  private final static String COMMAND_RETRY_MAX_TIME_IN_SEC_PROPERTY_NAME = "command_retry_max_time_in_sec";

  private final static String COMMAND_RETRY_ENABLED_DEFAULT = "true";

  private final static String COMMANDS_TO_RETRY_DEFAULT = "INSTALL,START";

  private final static String COMMAND_RETRY_MAX_TIME_IN_SEC_DEFAULT = "600";

  private final static String CLUSTER_ENV_CONFIG_TYPE_NAME = "cluster-env";

  private final static String HBASE_SITE_HBASE_COPROCESSOR_MASTER_CLASSES = "hbase.coprocessor.master.classes";
  private final static String HBASE_SITE_HBASE_COPROCESSOR_REGION_CLASSES = "hbase.coprocessor.region.classes";

  private final static String HAWQ_SITE_HAWQ_STANDBY_ADDRESS_HOST = "hawq_standby_address_host";
  private final static String HAWQSTANDBY = "HAWQSTANDBY";

  /**
   * Single host topology updaters
   */
  protected static Map<String, Map<String, PropertyUpdater>> singleHostTopologyUpdaters =
    new HashMap<>();

  /**
   * Multi host topology updaters
   */
  private static Map<String, Map<String, PropertyUpdater>> multiHostTopologyUpdaters =
    new HashMap<>();

  /**
   * Database host topology updaters
   */
  private static Map<String, Map<String, PropertyUpdater>> dbHostTopologyUpdaters =
    new HashMap<>();

  /**
   * Updaters for properties which need 'm' appended
   */
  private static Map<String, Map<String, PropertyUpdater>> mPropertyUpdaters =
    new HashMap<>();

  /**
   * Non topology related updaters
   */
  private static Map<String, Map<String, PropertyUpdater>> nonTopologyUpdaters =
    new HashMap<>();

  /**
   * Updaters that preserve the original property value, functions
   * as a placeholder for DB-related properties that need to be
   * removed from export, but do not require an update during
   * cluster creation
   */
  private Map<String, Map<String, PropertyUpdater>> removePropertyUpdaters =
    new HashMap<>();

  /**
   * Collection of all updaters
   */
  private static Collection<Map<String, Map<String, PropertyUpdater>>> allUpdaters =
    new ArrayList<>();

  /**
   * Compiled regex for hostgroup token with port information.
   */
  private static Pattern HOSTGROUP_PORT_REGEX = Pattern.compile("%HOSTGROUP::(\\S+?)%:?(\\d+)?");

  /**
   * Compiled regex for hostgroup token with port information.
   */
  private static Pattern LOCALHOST_PORT_REGEX = Pattern.compile("localhost:?(\\d+)?");

  /**
   * Compiled regex for placeholder
   */
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{.*\\}\\}");

  /**
   * Special network address
   */
  private static String BIND_ALL_IP_ADDRESS = "0.0.0.0";

  /**
   * Statically-defined set of properties that can support HA using a nameservice name
   *   in the configuration, rather than just a host name.
   *   This set also contains other HA properties that will be exported if the
   *   expected hostname information is not found.
   */
  private static Set<String> configPropertiesWithHASupport =
    new HashSet<>(Arrays.asList("fs.defaultFS", "hbase.rootdir", "instance.volumes", "policymgr_external_url", "xasecure.audit.destination.hdfs.dir"));

  public static boolean isNameNodeHAEnabled(Map<String, Map<String, String>> configurationProperties) {
    return configurationProperties.containsKey("hdfs-site") &&
           (configurationProperties.get("hdfs-site").containsKey("dfs.nameservices") ||
            configurationProperties.get("hdfs-site").containsKey("dfs.internal.nameservices"));
  }

  /**
   * Static convenience function to determine if Yarn ResourceManager HA is enabled
   * @param configProperties configuration properties for this cluster
   * @return true if Yarn ResourceManager HA is enabled
   *         false if Yarn ResourceManager HA is not enabled
   */
  public static boolean isYarnResourceManagerHAEnabled(Map<String, Map<String, String>> configProperties) {
    return configProperties.containsKey("yarn-site") && configProperties.get("yarn-site").containsKey("yarn.resourcemanager.ha.enabled")
      && configProperties.get("yarn-site").get("yarn.resourcemanager.ha.enabled").equals("true");
  }

  /**
   * Statically-defined list of filters to apply on property exports.
   * This will initially be used to filter out the Ranger Passwords, but
   * could be extended in the future for more generic purposes.
   */
  private PropertyFilter[] getExportPropertyFilters (Map<Long, Set<String>> authToLocalPerClusterMap)
    {
      return new PropertyFilter[] {
      new PasswordPropertyFilter(),
      new SimplePropertyNameExportFilter("tez.tez-ui.history-url.base", "tez-site"),
      new SimplePropertyNameExportFilter("admin_server_host", "kerberos-env"),
      new SimplePropertyNameExportFilter("kdc_hosts", "kerberos-env"),
      new SimplePropertyNameExportFilter("master_kdc", "kerberos-env"),
      new SimplePropertyNameExportFilter("realm", "kerberos-env"),
      new SimplePropertyNameExportFilter("kdc_type", "kerberos-env"),
      new SimplePropertyNameExportFilter("ldap-url", "kerberos-env"),
      new SimplePropertyNameExportFilter("container_dn", "kerberos-env"),
      new SimplePropertyNameExportFilter("domains", "krb5-conf"),
      new SimplePropertyNameExportFilter("dfs_ha_initial_namenode_active", "hadoop-env"),
      new SimplePropertyNameExportFilter("dfs_ha_initial_namenode_standby", "hadoop-env"),
      new StackPropertyTypeFilter(),
      new KerberosAuthToLocalRulesFilter(authToLocalPerClusterMap)};
    }

  /**
   * Statically-defined list of filters to apply on cluster config
   * property updates.
   *
   * This will initially be used to filter out properties that do not
   * need to be set, due to a given dependency property not having
   * an expected value.
   *
   * The UI uses the Recommendations/StackAdvisor APIs to accomplish this, but
   * Blueprints will use filters in the short-term, and hopefully move to a more
   * unified approach in the next release.
   *
   * This filter approach will also be used to remove properties in a given component
   * that are not valid in a High-Availability deployment (example: HDFS NameNode HA).
   */
  private static final PropertyFilter[] clusterUpdatePropertyFilters =
    { new DependencyEqualsFilter("hbase.security.authorization", "hbase-site", "true"),
      new DependencyNotEqualsFilter("hive.server2.authentication", "hive-site", "NONE"),
      /* Temporary solution related to HBASE/Phoenix issue PHOENIX-3360, to remove hbase.rpc.controllerfactory
       * .class from hbase-site. */
      new ConditionalPropertyFilter("hbase-site", "hbase.rpc.controllerfactory.class",
        "org.apache.hadoop.hbase.ipc.controller.ServerRpcControllerFactory"),
      new HDFSNameNodeHAFilter(),
      new HawqHAFilter() };

  private ClusterTopology clusterTopology;


  public BlueprintConfigurationProcessor(ClusterTopology clusterTopology) {
    this.clusterTopology = clusterTopology;
    initRemovePropertyUpdaters();
  }

  public Map<String, Map<String, PropertyUpdater>> getRemovePropertyUpdaters() {
    return removePropertyUpdaters;
  }

  public void initRemovePropertyUpdaters() {

    if (containsHostFromHostGroups("oozie-site", "oozie.service.JPAService.jdbc.url")) {
      Map<String, PropertyUpdater> oozieSiteUpdaters = singleHostTopologyUpdaters.get("oozie-site");
      Map<String, PropertyUpdater> oozieEnvUpdaters = singleHostTopologyUpdaters.get("oozie-env");
      if (oozieSiteUpdaters == null) {
        oozieSiteUpdaters = new HashMap<>();
      }
      if (oozieEnvUpdaters == null) {
        oozieEnvUpdaters = new HashMap<>();
      }
      oozieEnvUpdaters.put("oozie_existing_mysql_host", new SingleHostTopologyUpdater("OOZIE_SERVER"));
      oozieEnvUpdaters.put("oozie_existing_oracle_host", new SingleHostTopologyUpdater("OOZIE_SERVER"));
      oozieEnvUpdaters.put("oozie_existing_postgresql_host", new SingleHostTopologyUpdater("OOZIE_SERVER"));
      oozieEnvUpdaters.put("oozie_existing_oracle_host", new SingleHostTopologyUpdater("OOZIE_SERVER"));
      oozieEnvUpdaters.put("oozie_existing_postgresql_host", new SingleHostTopologyUpdater("OOZIE_SERVER"));
      oozieSiteUpdaters.put("oozie.service.JPAService.jdbc.url",  new SingleHostTopologyUpdater("OOZIE_SERVER"));

      singleHostTopologyUpdaters.put("oozie-env", oozieEnvUpdaters);
      singleHostTopologyUpdaters.put("oozie-site", oozieSiteUpdaters);
    } else {
      Map<String, PropertyUpdater> oozieEnvOriginalValueMap = new HashMap<>();
      Map<String, PropertyUpdater> oozieSiteOriginalValueMap = new HashMap<>();
      // register updaters for Oozie properties that may point to an external DB
      oozieEnvOriginalValueMap.put("oozie_existing_mysql_host", new OriginalValuePropertyUpdater());
      oozieEnvOriginalValueMap.put("oozie_existing_oracle_host", new OriginalValuePropertyUpdater());
      oozieEnvOriginalValueMap.put("oozie_existing_postgresql_host", new OriginalValuePropertyUpdater());
      oozieEnvOriginalValueMap.put("oozie_existing_oracle_host", new OriginalValuePropertyUpdater());
      oozieEnvOriginalValueMap.put("oozie_existing_postgresql_host", new OriginalValuePropertyUpdater());
      oozieSiteOriginalValueMap.put("oozie.service.JPAService.jdbc.url", new OriginalValuePropertyUpdater());

      removePropertyUpdaters.put("oozie-env", oozieEnvOriginalValueMap);
      removePropertyUpdaters.put("oozie-site", oozieSiteOriginalValueMap);
    }

    Map<String, PropertyUpdater> hiveEnvOriginalValueMap = new HashMap<>();
    // register updaters for Hive properties that may point to an external DB
    hiveEnvOriginalValueMap.put("hive_existing_oracle_host", new OriginalValuePropertyUpdater());
    hiveEnvOriginalValueMap.put("hive_existing_mssql_server_2_host", new OriginalValuePropertyUpdater());
    hiveEnvOriginalValueMap.put("hive_existing_mssql_server_host", new OriginalValuePropertyUpdater());
    hiveEnvOriginalValueMap.put("hive_existing_postgresql_host", new OriginalValuePropertyUpdater());
    hiveEnvOriginalValueMap.put("hive_existing_mysql_host", new OriginalValuePropertyUpdater());

    removePropertyUpdaters.put("hive-env", hiveEnvOriginalValueMap);

  }

  private boolean containsHostFromHostGroups(String configType, String propertyName) {
    String propertyValue = clusterTopology.getConfiguration().getPropertyValue(configType, propertyName);
    if (StringUtils.isEmpty(propertyValue)) {
      return false;
    }
    // check fir bp import
    Matcher m = HOST_GROUP_PLACEHOLDER_PATTERN.matcher(propertyValue);
    if (m.find()) {
      return true;
    }

    // check for bp export
    for (HostGroupInfo groupInfo : clusterTopology.getHostGroupInfo().values()) {
      Collection<String> hosts = groupInfo.getHostNames();
      for (String host : hosts) {
        if (propertyValue.contains(host)) {
          return true;
        }
      }
    }
    return false;
  }

  public Collection<String> getRequiredHostGroups() {
    Collection<String> requiredHostGroups = new HashSet<>();

    for (Map<String, Map<String, PropertyUpdater>> updaterMap : createCollectionOfUpdaters()) {
      for (Map.Entry<String, Map<String, PropertyUpdater>> entry : updaterMap.entrySet()) {
        String type = entry.getKey();
        for (Map.Entry<String, PropertyUpdater> updaterEntry : entry.getValue().entrySet()) {
          String propertyName = updaterEntry.getKey();
          PropertyUpdater updater = updaterEntry.getValue();

          // cluster scoped configuration which also includes all default and BP properties
          Map<String, Map<String, String>> clusterProps = clusterTopology.getConfiguration().getFullProperties();
          Map<String, String> typeMap = clusterProps.get(type);
          if (typeMap != null && typeMap.containsKey(propertyName) && typeMap.get(propertyName) != null) {
            requiredHostGroups.addAll(updater.getRequiredHostGroups(
                propertyName, typeMap.get(propertyName), clusterProps, clusterTopology));
          }

          // host group configs
          for (HostGroupInfo groupInfo : clusterTopology.getHostGroupInfo().values()) {
            Map<String, Map<String, String>> hgConfigProps = groupInfo.getConfiguration().getProperties();
            Map<String, String> hgTypeMap = hgConfigProps.get(type);
            if (hgTypeMap != null && hgTypeMap.containsKey(propertyName)) {
              requiredHostGroups.addAll(updater.getRequiredHostGroups(
                  propertyName, hgTypeMap.get(propertyName), hgConfigProps, clusterTopology));
            }
          }
        }
      }
    }
    return requiredHostGroups;
  }

  /**
   * Update properties for cluster creation.  This involves updating topology related properties with
   * concrete topology information.
   *
   * @return Set of config type names that were updated by this update call
   */
  public Set<String> doUpdateForClusterCreate() throws ConfigurationTopologyException {
      Set<String> configTypesUpdated = new HashSet<>();
    Configuration clusterConfig = clusterTopology.getConfiguration();
    Map<String, HostGroupInfo> groupInfoMap = clusterTopology.getHostGroupInfo();

    doRecommendConfigurations(clusterConfig, configTypesUpdated);

    // filter out any properties that should not be included, based on the dependencies
    // specified in the stacks, and the filters defined in this class
    doFilterPriorToClusterUpdate(clusterConfig, configTypesUpdated);

    // this needs to be called after doFilterPriorToClusterUpdate() to ensure that the returned
    // set of properties (copy) doesn't include the removed properties.  If an updater
    // removes a property other than the property it is registered for then we will
    // have an issue as it won't be removed from the clusterProps map as it is a copy.
    Map<String, Map<String, String>> configProperties = clusterConfig.getFullProperties();
    Map<String, Map<String, String>> clusterProps = configProperties;
    for (Map<String, Map<String, PropertyUpdater>> updaterMap : createCollectionOfUpdaters()) {
      for (Map.Entry<String, Map<String, PropertyUpdater>> entry : updaterMap.entrySet()) {
        String type = entry.getKey();
        for (Map.Entry<String, PropertyUpdater> updaterEntry : entry.getValue().entrySet()) {
          String propertyName = updaterEntry.getKey();
          PropertyUpdater updater = updaterEntry.getValue();

          // topo cluster scoped configuration which also includes all default and BP properties
          Map<String, String> typeMap = clusterProps.get(type);
          if (typeMap != null && typeMap.containsKey(propertyName) && typeMap.get(propertyName) != null) {
            final String originalValue = typeMap.get(propertyName);
            final String updatedValue =
              updater.updateForClusterCreate(propertyName, originalValue, clusterProps, clusterTopology);

            if(updatedValue == null ) {
              continue;
            }

            if (!updatedValue.equals(originalValue)) {
              configTypesUpdated.add(type);
            }

            clusterConfig.setProperty(type, propertyName, updatedValue);
          }

          // host group configs
          for (HostGroupInfo groupInfo : groupInfoMap.values()) {
            Configuration hgConfig = groupInfo.getConfiguration();
            Map<String, Map<String, String>> hgConfigProps = hgConfig.getFullProperties(1);
            Map<String, String> hgTypeMap = hgConfigProps.get(type);
            if (hgTypeMap != null && hgTypeMap.containsKey(propertyName)) {
              final String originalValue = hgTypeMap.get(propertyName);
              final String updatedValue =
                updater.updateForClusterCreate(propertyName, originalValue, hgConfigProps, clusterTopology);

              if (!updatedValue.equals(originalValue)) {
                configTypesUpdated.add(type);
              }

              hgConfig.setProperty(type, propertyName, updatedValue);
            }
          }
        }
      }
    }

    //todo: lots of hard coded HA rules included here
    if (isNameNodeHAEnabled(configProperties)) {

      // add "dfs.internal.nameservices" if it's not specified
      Map<String, String> hdfsSiteConfig = configProperties.get("hdfs-site");
      String nameservices = hdfsSiteConfig.get("dfs.nameservices");
      String int_nameservices = hdfsSiteConfig.get("dfs.internal.nameservices");
      if(int_nameservices == null && nameservices != null) {
        clusterConfig.setProperty("hdfs-site", "dfs.internal.nameservices", nameservices);
      }

      // if the active/stanbdy namenodes are not specified, assign them automatically
      if (! isNameNodeHAInitialActiveNodeSet(clusterProps) && ! isNameNodeHAInitialStandbyNodeSet(clusterProps)) {
        Collection<String> nnHosts = clusterTopology.getHostAssignmentsForComponent("NAMENODE");
        if (nnHosts.size() != 2) {
          throw new ConfigurationTopologyException("NAMENODE HA requires exactly 2 hosts running NAMENODE but there are: " +
              nnHosts.size() + " Hosts: " + nnHosts);
        }

        // set the properties that configure which namenode is active,
        // and which is a standby node in this HA deployment
        Iterator<String> nnHostIterator = nnHosts.iterator();
        clusterConfig.setProperty("hadoop-env", "dfs_ha_initial_namenode_active", nnHostIterator.next());
        clusterConfig.setProperty("hadoop-env", "dfs_ha_initial_namenode_standby", nnHostIterator.next());

        configTypesUpdated.add("hadoop-env");
      }
    }

    // Explicitly set any properties that are required but not currently provided in the stack definition.
    setStackToolsAndFeatures(clusterConfig, configTypesUpdated);
    setRetryConfiguration(clusterConfig, configTypesUpdated);
    setupHDFSProxyUsers(clusterConfig, configTypesUpdated);
    addExcludedConfigProperties(clusterConfig, configTypesUpdated, clusterTopology.getStack());

    trimProperties(clusterConfig, clusterTopology);

    return configTypesUpdated;
  }

  private void trimProperties(Configuration clusterConfig, ClusterTopology clusterTopology) {
    StackDefinition stack = clusterTopology.getStack();

    Map<String, Map<String, String>> configTypes = clusterConfig.getFullProperties();
    for (String configType : configTypes.keySet()) {
      Map<String,String> properties = configTypes.get(configType);
      for (String propertyName : properties.keySet()) {
        trimPropertyValue(clusterConfig, stack, configType, properties, propertyName);
      }
    }
  }

  private void trimPropertyValue(Configuration clusterConfig, StackDefinition stack, String configType, Map<String, String> properties, String propertyName) {
    if (propertyName != null && properties.get(propertyName) != null) {

      TrimmingStrategy trimmingStrategy =
        PropertyValueTrimmingStrategyDefiner.defineTrimmingStrategy(stack, propertyName, configType);
      String oldValue = properties.get(propertyName);
      String newValue = trimmingStrategy.trim(oldValue);

      if (!newValue.equals(oldValue)){
        LOG.debug("Changing value for config {} property {} from [{}] to [{}]", configType, propertyName, oldValue, newValue);
        clusterConfig.setProperty(configType, propertyName, newValue);
      }
    }
  }

  /**
   * Returns true if property should be retained with default value instead of deleting
   * TODO: This is a temporary work-around till BP integrates with stack advisor
   * @param propertyName
   * @return
   */
  private static boolean shouldPropertyBeStoredWithDefault(String propertyName) {
    if (!StringUtils.isBlank(propertyName) &&
        (HBASE_SITE_HBASE_COPROCESSOR_MASTER_CLASSES.equals(propertyName) ||
         HBASE_SITE_HBASE_COPROCESSOR_REGION_CLASSES.equals(propertyName))) {
      return true;
    }

    return false;
  }

  /**
   * Update properties for blueprint export.
   * This involves converting concrete topology information to host groups.
   */
  public void doUpdateForBlueprintExport() {
    // HA configs are only processed in cluster configuration, not HG configurations
    Map<String, Map<String, String>> configProperties = clusterTopology.getConfiguration().getFullProperties();
    if (isNameNodeHAEnabled(configProperties)) {
      doNameNodeHAUpdate();
    }

    if (isYarnResourceManagerHAEnabled(configProperties)) {
      doYarnResourceManagerHAUpdate();
    }

    if (isOozieServerHAEnabled(configProperties)) {
      doOozieServerHAUpdate();
    }

    Collection<Configuration> allConfigs = new ArrayList<>();
    allConfigs.add(clusterTopology.getConfiguration());
    for (HostGroupInfo groupInfo : clusterTopology.getHostGroupInfo().values()) {
      Configuration hgConfiguration = groupInfo.getConfiguration();
      if (! hgConfiguration.getFullProperties(1).isEmpty()) {
        // create new configuration which only contains properties specified in host group and BP host group
        allConfigs.add(new Configuration(hgConfiguration.getProperties(), null,
            new Configuration(hgConfiguration.getParentConfiguration().getProperties(), null)));
      }
    }

    for (Configuration configuration : allConfigs) {
      doSingleHostExportUpdate(singleHostTopologyUpdaters, configuration);
      doSingleHostExportUpdate(dbHostTopologyUpdaters, configuration);

      doMultiHostExportUpdate(multiHostTopologyUpdaters, configuration);

      doNonTopologyUpdate(nonTopologyUpdaters, configuration);

      doRemovePropertyExport(removePropertyUpdaters, configuration);

      doFilterPriorToExport(configuration);
    }
  }

  /**
   * This method iterates over the properties passed in, and applies a
   * list of filters to the properties.
   *
   * If any filter implementations indicate that the property should
   * not be included in a collection (a Blueprint export in this case),
   * then the property is removed prior to the export.
   *
   * @param configuration  configuration being processed
   */
  private void doFilterPriorToExport(Configuration configuration) {
    Map<String, Map<String, String>> properties = configuration.getFullProperties();
    Map<Long, Set<String>> authToLocalPerClusterMap = null;
    try {
      String clusterName = clusterTopology.getAmbariContext().getClusterName(clusterTopology.getClusterId());
      Cluster cluster = clusterTopology.getAmbariContext().getController().getClusters().getCluster(clusterName);
      authToLocalPerClusterMap = new HashMap<>();
      authToLocalPerClusterMap.put(clusterTopology.getClusterId(), clusterTopology.getAmbariContext().getController().getKerberosHelper().getKerberosDescriptor(cluster, false).getAllAuthToLocalProperties());
      } catch (AmbariException e) {
        LOG.error("Error while getting authToLocal properties. ", e);
    }
    PropertyFilter [] exportPropertyFilters = getExportPropertyFilters(authToLocalPerClusterMap);
    for (Map.Entry<String, Map<String, String>> configEntry : properties.entrySet()) {
      String type = configEntry.getKey();
      try {
          clusterTopology.getStack().getServiceForConfigType(type);
        } catch (IllegalArgumentException illegalArgumentException) {
            LOG.error(new StringBuilder(String.format("Error encountered while trying to obtain the service name for config type [%s]. ", type))
            .append("Further processing on this config type will be skipped. ")
            .append("This usually means that a service's definitions have been manually removed from the Ambari stack definitions. ")
            .append("If the stack definitions have not been changed manually, this may indicate a stack definition error in Ambari. ").toString(), illegalArgumentException);
            continue;
        }
      Map<String, String> typeProperties = configEntry.getValue();

      for (Map.Entry<String, String> propertyEntry : typeProperties.entrySet()) {
        String propertyName = propertyEntry.getKey();
        String propertyValue = propertyEntry.getValue();
        if (shouldPropertyBeExcludedForBlueprintExport(propertyName, propertyValue, type, clusterTopology, exportPropertyFilters)) {
          configuration.removeProperty(type, propertyName);
        }
      }
    }
  }

  private void doFilterPriorToClusterUpdate(Configuration configuration, Set<String> configTypesUpdated) {
    // getFullProperties returns a copy so changes to it are not reflected in config properties
    Map<String, Map<String, String>> properties = configuration.getFullProperties();
    for (Map.Entry<String, Map<String, String>> configEntry : properties.entrySet()) {
      String configType = configEntry.getKey();
      Map<String, String> configPropertiesPerType = configEntry.getValue();

      for (Map.Entry<String, String> propertyEntry : configPropertiesPerType.entrySet()) {
        String propName = propertyEntry.getKey();
        if (shouldPropertyBeExcludedForClusterUpdate(propName, propertyEntry.getValue(), configType, clusterTopology)) {
          configuration.removeProperty(configType, propName);
          configTypesUpdated.add(configType);
        }
      }
    }
  }

  /**
   * Update configuration properties from recommended configurations of the stack advisor based on
   * {@link ConfigRecommendationStrategy}
   * @param configuration configuration being processed
   * @param configTypesUpdated updated config types
   */
  private void doRecommendConfigurations(Configuration configuration, Set<String> configTypesUpdated) {
    ConfigRecommendationStrategy configRecommendationStrategy = clusterTopology.getConfigRecommendationStrategy();
    Map<String, AdvisedConfiguration> advisedConfigurations = clusterTopology.getAdvisedConfigurations();
    LOG.info("Config recommendation strategy being used is {})", configRecommendationStrategy);

    if (ConfigRecommendationStrategy.ONLY_STACK_DEFAULTS_APPLY.equals(configRecommendationStrategy)) {
      LOG.info("Filter out recommended configurations. Keep only the stack defaults.");
      doFilterStackDefaults(advisedConfigurations);
    }
    if (!ConfigRecommendationStrategy.NEVER_APPLY.equals(configRecommendationStrategy)) {
      for (Map.Entry<String, AdvisedConfiguration> advConfEntry : advisedConfigurations.entrySet()) {
        String configType = advConfEntry.getKey();
        AdvisedConfiguration advisedConfig = advConfEntry.getValue();
        LOG.info("Update '{}' configurations with recommended configurations provided by the stack advisor.", configType);
        if (advisedConfig.getProperties() != null) {
          doReplaceProperties(configuration, configType, advisedConfig, configTypesUpdated);
        }
        if (advisedConfig.getPropertyValueAttributes() != null) {
          doRemovePropertiesIfNeeded(configuration, configType, advisedConfig, configTypesUpdated);
        }
      }
    } else {
      LOG.info("No recommended configurations are applied. (strategy: {})", ConfigRecommendationStrategy.NEVER_APPLY);
    }
  }

  /**
   * Drop every configuration property from advised configuration that is not found in the stack defaults.
   * @param advisedConfigurations advised configuration instance
   */
  private void doFilterStackDefaults(Map<String, AdvisedConfiguration> advisedConfigurations) {
    Configuration stackDefaults = clusterTopology.getStack().getConfiguration(clusterTopology.getServices());
    Map<String, Map<String, String>> stackDefaultProps = stackDefaults.getProperties();
    for (Map.Entry<String, AdvisedConfiguration> adConfEntry : advisedConfigurations.entrySet()) {
      AdvisedConfiguration advisedConfiguration = adConfEntry.getValue();
      if (stackDefaultProps.containsKey(adConfEntry.getKey())) {
        Map<String, String> defaultProps = stackDefaultProps.get(adConfEntry.getKey());
        if (advisedConfiguration.getProperties() != null) {
          Map<String, String> outFilteredProps = Maps.filterKeys(advisedConfiguration.getProperties(),
            Predicates.not(Predicates.in(defaultProps.keySet())));
          advisedConfiguration.getProperties().keySet().removeAll(Sets.newCopyOnWriteArraySet(outFilteredProps.keySet()));
        }

        if (advisedConfiguration.getPropertyValueAttributes() != null) {
          Map<String, ValueAttributesInfo> outFilteredValueAttrs = Maps.filterKeys(advisedConfiguration.getPropertyValueAttributes(),
            Predicates.not(Predicates.in(defaultProps.keySet())));
          advisedConfiguration.getPropertyValueAttributes().keySet().removeAll(
            Sets.newCopyOnWriteArraySet(outFilteredValueAttrs.keySet()));
        }
      } else {
        advisedConfiguration.getProperties().clear();
      }
    }
  }

  /**
   * Update configuration properties based on advised configuration properties.
   * @param configuration configuration being processed
   * @param configType type of configuration. e.g.: yarn-site
   * @param advisedConfig advised configuration instance
   * @param configTypesUpdated updated config types
   */
  private void doReplaceProperties(Configuration configuration, String configType,
                                   AdvisedConfiguration advisedConfig, Set<String> configTypesUpdated) {
    for (Map.Entry<String, String> propEntry : advisedConfig.getProperties().entrySet()) {
      String originalValue = configuration.getPropertyValue(configType, propEntry.getKey());
      configuration.setProperty(configType, propEntry.getKey(), propEntry.getValue());
      if (!propEntry.getValue().equals(originalValue)) {
        configTypesUpdated.add(configType);
      }
    }
  }

  /**
   * Remove properties that are flagged with 'delete' value attribute.
   * @param configuration configuration being processed
   * @param configType type of configuration. e.g.: yarn-site
   * @param advisedConfig advised configuration instance
   * @param configTypesUpdated updated config types
   */
  private void doRemovePropertiesIfNeeded(Configuration configuration,
                                          String configType, AdvisedConfiguration advisedConfig, Set<String> configTypesUpdated) {
    if (advisedConfig.getPropertyValueAttributes() != null) {
      for (Map.Entry<String, ValueAttributesInfo> valueAttrEntry :
        advisedConfig.getPropertyValueAttributes().entrySet()) {
        if ("true".equalsIgnoreCase(valueAttrEntry.getValue().getDelete())) {
          if(null != configuration.removeProperty(configType, valueAttrEntry.getKey())) {
            configTypesUpdated.add(configType);
          }
        }
      }
    }
  }

  /**
   * Creates a Collection of PropertyUpdater maps that will handle the configuration
   *   update for this cluster.
   *
   *   If NameNode HA is enabled, then updater instances will be added to the
   *   collection, in addition to the default list of Updaters that are statically defined.
   *
   *   Similarly, if Yarn ResourceManager HA is enabled, then updater instances specific
   *   to Yarn HA will be added to the default list of Updaters that are statically defined.
   *
   * @return Collection of PropertyUpdater maps used to handle cluster config update
   */
  private Collection<Map<String, Map<String, PropertyUpdater>>> createCollectionOfUpdaters() {
    Collection<Map<String, Map<String, PropertyUpdater>>> updaters = allUpdaters;

    Map<String, Map<String, String>> configProperties = clusterTopology.getConfiguration().getFullProperties();
    if (isNameNodeHAEnabled(configProperties)) {
      updaters = addNameNodeHAUpdaters(updaters);
    }

    if (isYarnResourceManagerHAEnabled(configProperties)) {
      updaters = addYarnResourceManagerHAUpdaters(updaters);
    }

    if (isOozieServerHAEnabled(configProperties)) {
      updaters = addOozieServerHAUpdaters(updaters);
    }

    return updaters;
  }

  /**
   * Creates a Collection of PropertyUpdater maps that include the NameNode HA properties, and
   *   adds these to the list of updaters used to process the cluster configuration.  The HA
   *   properties are based on the names of the HA namservices and name nodes, and so must
   *   be registered at runtime, rather than in the static list.  This new Collection includes
   *   the statically-defined updaters, in addition to the HA-related updaters.
   *
   * @param updaters a Collection of updater maps to be included in the list of updaters for
   *                   this cluster config update
   * @return A Collection of PropertyUpdater maps to handle the cluster config update
   */
  private Collection<Map<String, Map<String, PropertyUpdater>>> addNameNodeHAUpdaters(Collection<Map<String, Map<String, PropertyUpdater>>> updaters) {
    Collection<Map<String, Map<String, PropertyUpdater>>> highAvailabilityUpdaters =
      new LinkedList<>();

    // always add the statically-defined list of updaters to the list to use
    // in processing cluster configuration
    highAvailabilityUpdaters.addAll(updaters);

    // add the updaters for the dynamic HA properties, based on the HA config in hdfs-site
    highAvailabilityUpdaters.add(createMapOfNameNodeHAUpdaters());

    return highAvailabilityUpdaters;
  }

  /**
   * Creates a Collection of PropertyUpdater maps that include the Yarn ResourceManager HA properties, and
   *   adds these to the list of updaters used to process the cluster configuration.  The HA
   *   properties are based on the names of the Resource Manager instances defined in
   *   yarn-site, and so must be registered at runtime, rather than in the static list.
   *
   *   This new Collection includes the statically-defined updaters,
   *   in addition to the HA-related updaters.
   *
   * @param updaters a Collection of updater maps to be included in the list of updaters for
   *                   this cluster config update
   * @return A Collection of PropertyUpdater maps to handle the cluster config update
   */
  private Collection<Map<String, Map<String, PropertyUpdater>>> addYarnResourceManagerHAUpdaters(Collection<Map<String, Map<String, PropertyUpdater>>> updaters) {
    Collection<Map<String, Map<String, PropertyUpdater>>> highAvailabilityUpdaters =
      new LinkedList<>();

    // always add the statically-defined list of updaters to the list to use
    // in processing cluster configuration
    highAvailabilityUpdaters.addAll(updaters);

    // add the updaters for the dynamic HA properties, based on the HA config in hdfs-site
    highAvailabilityUpdaters.add(createMapOfYarnResourceManagerHAUpdaters());

    return highAvailabilityUpdaters;
  }


  /**
   * Creates a Collection of PropertyUpdater maps that include the OozieServer HA properties, and
   *   adds these to the list of updaters used to process the cluster configuration.

   *   This new Collection includes the statically-defined updaters,
   *   in addition to the HA-related updaters.
   *
   * @param updaters a Collection of updater maps to be included in the list of updaters for
   *                   this cluster config update
   * @return A Collection of PropertyUpdater maps to handle the cluster config update
   */
  private Collection<Map<String, Map<String, PropertyUpdater>>> addOozieServerHAUpdaters(Collection<Map<String, Map<String, PropertyUpdater>>> updaters) {
    Collection<Map<String, Map<String, PropertyUpdater>>> highAvailabilityUpdaters =
      new LinkedList<>();

    // always add the statically-defined list of updaters to the list to use
    // in processing cluster configuration
    highAvailabilityUpdaters.addAll(updaters);

    // add the updaters for the Oozie HA properties not defined in stack, but
    // required to be present/updated in oozie-site
    highAvailabilityUpdaters.add(createMapOfOozieServerHAUpdaters());

    return highAvailabilityUpdaters;
  }

  /**
   * Performs export update for the set of properties that do not
   * require update during cluster setup, but should be removed
   * during a Blueprint export.
   *
   * In the case of a service referring to an external DB, any
   * properties that contain external host information should
   * be removed from the configuration that will be available in
   * the exported Blueprint.
   *
   * @param updaters       set of updaters for properties that should
   *                       always be removed during a Blueprint export
   * @param configuration  configuration being processed
   */
  private void doRemovePropertyExport(Map<String, Map<String, PropertyUpdater>> updaters,
                                      Configuration configuration) {

    Map<String, Map<String, String>> properties = configuration.getProperties();
    for (Map.Entry<String, Map<String, PropertyUpdater>> entry : updaters.entrySet()) {
      String type = entry.getKey();
      for (String propertyName : entry.getValue().keySet()) {
        Map<String, String> typeProperties = properties.get(type);
        if ( (typeProperties != null) && (typeProperties.containsKey(propertyName)) ) {
          configuration.removeProperty(type, propertyName);
        }
      }
    }
  }

  /**
   * Perform export update processing for HA configuration for NameNodes.  The HA NameNode property
   *   names are based on the nameservices defined when HA is enabled via the Ambari UI, so this method
   *   dynamically determines the property names, and registers PropertyUpdaters to handle the masking of
   *   host names in these configuration items.
   *
   */
  public void doNameNodeHAUpdate() {
    Map<String, Map<String, PropertyUpdater>> highAvailabilityUpdaters = createMapOfNameNodeHAUpdaters();

    // perform a single host update on these dynamically generated property names
    if (highAvailabilityUpdaters.get("hdfs-site").size() > 0) {
      doSingleHostExportUpdate(highAvailabilityUpdaters, clusterTopology.getConfiguration());
    }
  }

  /**
   * Perform export update processing for HA configuration for Yarn ResourceManagers.  The HA ResourceManager
   * property names are based on the ResourceManager names defined when HA is enabled via the Ambari UI, so this method
   * dynamically determines the property names, and registers PropertyUpdaters to handle the masking of
   * host names in these configuration items.
   *
   */
  public void doYarnResourceManagerHAUpdate() {
    Map<String, Map<String, PropertyUpdater>> highAvailabilityUpdaters = createMapOfYarnResourceManagerHAUpdaters();

    // perform a single host update on these dynamically generated property names
    if (highAvailabilityUpdaters.get("yarn-site").size() > 0) {
      doSingleHostExportUpdate(highAvailabilityUpdaters, clusterTopology.getConfiguration());
    }
  }

  /**
   * Perform export update processing for HA configuration for Oozie servers.  The properties used
   * in Oozie HA are not defined in the stack, but need to be added at runtime during an HA
   * deployment in order to support exporting/redeploying clusters with Oozie HA config.
   *
   */
  public void doOozieServerHAUpdate() {
    Map<String, Map<String, PropertyUpdater>> highAvailabilityUpdaters = createMapOfOozieServerHAUpdaters();

    if (highAvailabilityUpdaters.get("oozie-site").size() > 0) {
      doMultiHostExportUpdate(highAvailabilityUpdaters, clusterTopology.getConfiguration());
    }
  }


  /**
   * Creates map of PropertyUpdater instances that are associated with
   *   NameNode High Availability (HA).  The HA configuration property
   *   names are dynamic, and based on other HA config elements in
   *   hdfs-site.  This method registers updaters for the required
   *   properties associated with each nameservice and namenode.
   *
   * @return a Map of registered PropertyUpdaters for handling HA properties in hdfs-site
   */
  private Map<String, Map<String, PropertyUpdater>> createMapOfNameNodeHAUpdaters() {
    Map<String, Map<String, PropertyUpdater>> highAvailabilityUpdaters = new HashMap<>();
    Map<String, PropertyUpdater> hdfsSiteUpdatersForAvailability = new HashMap<>();
    highAvailabilityUpdaters.put("hdfs-site", hdfsSiteUpdatersForAvailability);

    //todo: Do we need to call this for HG configurations?
    Map<String, String> hdfsSiteConfig = clusterTopology.getConfiguration().getFullProperties().get("hdfs-site");
    // generate the property names based on the current HA config for the NameNode deployments
    for (String nameService : parseNameServices(hdfsSiteConfig)) {
      for (String nameNode : parseNameNodes(nameService, hdfsSiteConfig)) {
        final String httpsPropertyName = "dfs.namenode.https-address." + nameService + "." + nameNode;
        hdfsSiteUpdatersForAvailability.put(httpsPropertyName, new SingleHostTopologyUpdater("NAMENODE"));
        final String httpPropertyName = "dfs.namenode.http-address." + nameService + "." + nameNode;
        hdfsSiteUpdatersForAvailability.put(httpPropertyName, new SingleHostTopologyUpdater("NAMENODE"));
        final String rpcPropertyName = "dfs.namenode.rpc-address." + nameService + "." + nameNode;
        hdfsSiteUpdatersForAvailability.put(rpcPropertyName, new SingleHostTopologyUpdater("NAMENODE"));
      }
    }
    return highAvailabilityUpdaters;
  }


  /**
   * Creates map of PropertyUpdater instances that are associated with
   *   Yarn ResourceManager High Availability (HA).  The HA configuration property
   *   names are dynamic, and based on other HA config elements in
   *   yarn-site.  This method registers updaters for the required
   *   properties associated with each ResourceManager.
   *
   * @return a Map of registered PropertyUpdaters for handling HA properties in yarn-site
   */
  private Map<String, Map<String, PropertyUpdater>> createMapOfYarnResourceManagerHAUpdaters() {
    Map<String, Map<String, PropertyUpdater>> highAvailabilityUpdaters = new HashMap<>();
    Map<String, PropertyUpdater> yarnSiteUpdatersForAvailability = new HashMap<>();
    highAvailabilityUpdaters.put("yarn-site", yarnSiteUpdatersForAvailability);

    Map<String, String> yarnSiteConfig = clusterTopology.getConfiguration().getFullProperties().get("yarn-site");
    // generate the property names based on the current HA config for the ResourceManager deployments
    for (String resourceManager : parseResourceManagers(yarnSiteConfig)) {
      SingleHostTopologyUpdater updater = new SingleHostTopologyUpdater("RESOURCEMANAGER");
      yarnSiteUpdatersForAvailability.put("yarn.resourcemanager.hostname." + resourceManager, updater);
      yarnSiteUpdatersForAvailability.put("yarn.resourcemanager.address." + resourceManager, updater);
      yarnSiteUpdatersForAvailability.put("yarn.resourcemanager.admin.address." + resourceManager, updater);
      yarnSiteUpdatersForAvailability.put("yarn.resourcemanager.resource-tracker.address." + resourceManager, updater);
      yarnSiteUpdatersForAvailability.put("yarn.resourcemanager.scheduler.address." + resourceManager, updater);
      yarnSiteUpdatersForAvailability.put("yarn.resourcemanager.webapp.address." + resourceManager, updater);
      yarnSiteUpdatersForAvailability.put("yarn.resourcemanager.webapp.https.address." + resourceManager, updater);
    }

    return highAvailabilityUpdaters;
  }

  /**
   * Creates map of PropertyUpdater instances that are associated with
   *   Oozie Server High Availability (HA).
   *
   * @return a Map of registered PropertyUpdaters for handling HA properties in oozie-site
   */
  private Map<String, Map<String, PropertyUpdater>> createMapOfOozieServerHAUpdaters() {
    Map<String, Map<String, PropertyUpdater>> highAvailabilityUpdaters = new HashMap<>();
    Map<String, PropertyUpdater> oozieSiteUpdatersForAvailability = new HashMap<>();
    highAvailabilityUpdaters.put("oozie-site", oozieSiteUpdatersForAvailability);

    // register a multi-host property updater for this Oozie property.
    // this property is not defined in the stacks, since HA is not supported yet
    // by the stack definition syntax.  This property should only be considered in
    // an Oozie HA cluster.
    oozieSiteUpdatersForAvailability.put("oozie.zookeeper.connection.string", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));

    return highAvailabilityUpdaters;

  }

  /**
   * Static convenience function to determine if Oozie HA is enabled
   * @param configProperties configuration properties for this cluster
   * @return true if Oozie HA is enabled
   *         false if Oozie HA is not enabled
   */
  //todo: pass in configuration
  static boolean isOozieServerHAEnabled(Map<String, Map<String, String>> configProperties) {
    return configProperties.containsKey("oozie-site") && configProperties.get("oozie-site").containsKey("oozie.services.ext")
      && configProperties.get("oozie-site").get("oozie.services.ext").contains("org.apache.oozie.service.ZKLocksService");
  }

  /**
   * Static convenience function to determine if HiveServer HA is enabled
   * @param configProperties configuration properties for this cluster
   * @return true if HiveServer HA is enabled
   *         false if HiveServer HA is not enabled
   */
  static boolean isHiveServerHAEnabled(Map<String, Map<String, String>> configProperties) {
    return configProperties.containsKey("hive-site") && configProperties.get("hive-site").containsKey("hive.server2.support.dynamic.service.discovery")
      && configProperties.get("hive-site").get("hive.server2.support.dynamic.service.discovery").equals("true");
  }

  /**
   * Convenience method to examine the current configuration, to determine
   * if the hostname of the initial active namenode in an HA deployment has
   * been included.
   *
   * @param configProperties the configuration for this cluster
   * @return true if the initial active namenode property has been configured
   *         false if the initial active namenode property has not been configured
   */
  static boolean isNameNodeHAInitialActiveNodeSet(Map<String, Map<String, String>> configProperties) {
    return configProperties.containsKey("hadoop-env") && configProperties.get("hadoop-env").containsKey("dfs_ha_initial_namenode_active");
  }


  /**
   * Convenience method to examine the current configuration, to determine
   * if the hostname of the initial standby namenode in an HA deployment has
   * been included.
   *
   * @param configProperties the configuration for this cluster
   * @return true if the initial standby namenode property has been configured
   *         false if the initial standby namenode property has not been configured
   */
  static boolean isNameNodeHAInitialStandbyNodeSet(Map<String, Map<String, String>> configProperties) {
    return configProperties.containsKey("hadoop-env") && configProperties.get("hadoop-env").containsKey("dfs_ha_initial_namenode_standby");
  }


  /**
   * Parses out the list of nameservices associated with this HDFS configuration.
   *
   * @param properties config properties for this cluster
   *
   * @return array of Strings that indicate the nameservices for this cluster
   */
  static String[] parseNameServices(Map<String, String> properties) {
    String nameServices = properties.get("dfs.internal.nameservices");
    if (nameServices == null) {
      nameServices = properties.get("dfs.nameservices");
    }
    return splitAndTrimStrings(nameServices);
  }

  /**
   * Parses out the list of resource managers associated with this yarn-site configuration.
   *
   * @param properties config properties for this cluster
   *
   * @return array of Strings that indicate the ResourceManager names for this HA cluster
   */
  static String[] parseResourceManagers(Map<String, String> properties) {
    final String resourceManagerNames = properties.get("yarn.resourcemanager.ha.rm-ids");
    return splitAndTrimStrings(resourceManagerNames);
  }

  /**
   * Parses out the list of name nodes associated with a given HDFS
   *   NameService, based on a given HDFS configuration.
   *
   * @param nameService the nameservice used for this parsing
   * @param properties config properties for this cluster
   *
   * @return array of Strings that indicate the name nodes associated
   *           with this nameservice
   */
  static String[] parseNameNodes(String nameService, Map<String, String> properties) {
    final String nameNodes = properties.get("dfs.ha.namenodes." + nameService);
    return splitAndTrimStrings(nameNodes);
  }

  /**
   * Iterates over the list of registered filters for this config processor, and
   * queries each filter to determine if a given property should be included
   * in a property collection.  If any filters return false for the isPropertyIncluded()
   * query, then the property should be excluded.
   *
   * @param propertyName config property name
   * @param propertyValue config property value
   * @param propertyType config type that contains this property
   * @param topology cluster topology instance
   * @return true if the property should be excluded
   *         false if the property should not be excluded
   */
  private boolean shouldPropertyBeExcludedForBlueprintExport(String propertyName, String propertyValue, String propertyType, ClusterTopology topology, PropertyFilter [] exportPropertyFilters ) {
    for(PropertyFilter filter : exportPropertyFilters) {
      if (!filter.isPropertyIncluded(propertyName, propertyValue, propertyType, topology)) {
        return true;
      }
    }

    // if no filters require that the property be excluded,
    // then allow it to be included in the property collection
    return false;
  }

  /**
   * Convenience method to iterate over the cluster update filters, and determine if a given property
   * should be excluded from a collection.
   *
   * @param propertyName name of property to examine
   * @param propertyValue value of the current property
   * @param propertyType configuration type that contains this property
   * @param topology the cluster topology instance
   * @return true if the given property should be excluded
   *         false if the given property should be included
   */
  private static boolean shouldPropertyBeExcludedForClusterUpdate(String propertyName,
                                                                  String propertyValue,
                                                                  String propertyType,
                                                                  ClusterTopology topology) {

    for(PropertyFilter filter : clusterUpdatePropertyFilters) {
      try {
        if (!filter.isPropertyIncluded(propertyName, propertyValue, propertyType, topology)) {
          if (!shouldPropertyBeStoredWithDefault(propertyName)) {
            return true;
          }
        }
      } catch (Throwable throwable) {
        // if any error occurs during a filter execution, just log it
        LOG.warn("Error occurred while attempting to process the property '" + propertyName + "' with a filter.  This may indicate a config error.", throwable);
      }
    }

    // if no filters require that the property be excluded,
    // then allow it to be included in the property collection
    return false;
  }

  /**
   * Update single host topology configuration properties for blueprint export.
   *
   * @param updaters       registered updaters
   * @param configuration  configuration being processed
   */
  private void doSingleHostExportUpdate(Map<String, Map<String, PropertyUpdater>> updaters, Configuration configuration) {
    Map<String, Map<String, String>> properties = configuration.getFullProperties();
    for (Map.Entry<String, Map<String, PropertyUpdater>> entry : updaters.entrySet()) {
      String type = entry.getKey();
      for (String propertyName : entry.getValue().keySet()) {
        boolean matchedHost = false;

        Map<String, String> typeProperties = properties.get(type);
        if (typeProperties != null && typeProperties.containsKey(propertyName)) {
          String propValue = typeProperties.get(propertyName);

          for (HostGroupInfo groupInfo : clusterTopology.getHostGroupInfo().values()) {
            Collection<String> hosts = groupInfo.getHostNames();
            for (String host : hosts) {
              //todo: need to use regular expression to avoid matching a host which is a superset.
              if (propValue.contains(host)) {
                matchedHost = true;
                configuration.setProperty(type, propertyName,
                    propValue.replace(host, "%HOSTGROUP::" + groupInfo.getHostGroupName() + "%"));
                break;
              }
            }
            if (matchedHost) {
              break;
            }
          }
          // remove properties that do not contain hostnames,
          // except in the case of HA-related properties, that
          // can contain nameservice references instead of hostnames (Fix for Bug AMBARI-7458).
          // also will not remove properties that reference the special 0.0.0.0 network
          // address or properties with undefined hosts
          if (! matchedHost &&
              ! isNameServiceProperty(propertyName) &&
              ! isSpecialNetworkAddress(propValue)  &&
              ! isUndefinedAddress(propValue) &&
              ! isPlaceholder(propValue)) {

            configuration.removeProperty(type, propertyName);
          }
        }
      }
    }
  }

  /**
   * Determine if a property is a placeholder
   *
   * @param propertyValue  property value
   *
   * @return true if the property has format "{{%s}}"
   */
  private static boolean isPlaceholder(String propertyValue) {
    return PLACEHOLDER.matcher(propertyValue).find();
  }

  /**
   * Determines if a given property name's value can include
   *   nameservice references instead of host names.
   *
   * @param propertyName name of the property
   *
   * @return true if this property can support using nameservice names
   *         false if this property cannot support using nameservice names
   */
  private static boolean isNameServiceProperty(String propertyName) {
    return configPropertiesWithHASupport.contains(propertyName);
  }

  /**
   * Queries a property value to determine if the value contains
   *   a host address with all zeros (0.0.0.0).  This is a special
   *   address that signifies that the service is available on
   *   all network interfaces on a given machine.
   *
   * @param propertyValue the property value to inspect
   *
   * @return true if the 0.0.0.0 address is included in this string
   *         false if the 0.0.0.0 address is not included in this string
   */
  private static boolean isSpecialNetworkAddress(String propertyValue) {
    return propertyValue.contains(BIND_ALL_IP_ADDRESS);
  }

  /**
   * Determine if a property has an undefined host.
   *
   * @param propertyValue  property value
   *
   * @return true if the property value contains "undefined"
   */
  private static boolean isUndefinedAddress(String propertyValue) {
    return propertyValue.contains("undefined");
  }

  /**
   * Update multi host topology configuration properties for blueprint export.
   *
   * @param updaters       registered updaters
   * @param configuration  configuration being processed
   */
  private void doMultiHostExportUpdate(Map<String, Map<String, PropertyUpdater>> updaters, Configuration configuration) {
    Map<String, Map<String, String>> properties = configuration.getFullProperties();
    for (Map.Entry<String, Map<String, PropertyUpdater>> entry : updaters.entrySet()) {
      String type = entry.getKey();
      for (String propertyName : entry.getValue().keySet()) {
        Map<String, String> typeProperties = properties.get(type);
        if (typeProperties != null && typeProperties.containsKey(propertyName)) {
          String propValue = typeProperties.get(propertyName);
          for (HostGroupInfo groupInfo : clusterTopology.getHostGroupInfo().values()) {
            Collection<String> hosts = groupInfo.getHostNames();
            for (String host : hosts) {
              propValue = propValue.replaceAll(host + "\\b", "%HOSTGROUP::" +
                  groupInfo.getHostGroupName() + "%");
            }
          }
          Collection<String> addedGroups = new HashSet<>();
          String[] toks = propValue.split(",");
          boolean inBrackets = propValue.startsWith("[");

          StringBuilder sb = new StringBuilder();
          if (inBrackets) {
            sb.append('[');
          }
          boolean firstTok = true;
          for (String tok : toks) {
            tok = tok.replaceAll("[\\[\\]]", "");

            if (addedGroups.add(tok)) {
              if (! firstTok) {
                sb.append(',');
              }
              sb.append(tok);
            }
            firstTok = false;
          }

          if (inBrackets) {
            sb.append(']');
          }
          configuration.setProperty(type, propertyName, sb.toString());
        }
      }
    }
  }

  /**
   * Convert a property value which includes a host group topology token to a physical host.
   *
   *
   * @param val       value to be converted
   * @param topology  cluster topology
   *
   * @return updated value with physical host name
   */
  //todo: replace this with parseHostGroupToken which would return a hostgroup or null
  private static Collection<String> getHostStrings(String val, ClusterTopology topology) {

    Collection<String> hosts = new LinkedHashSet<>();
    Matcher m = HOSTGROUP_PORT_REGEX.matcher(val);
    while (m.find()) {
      String groupName = m.group(1);
      String port = m.group(2);

      HostGroupInfo hostGroupInfo = topology.getHostGroupInfo().get(groupName);
      if (hostGroupInfo == null) {
        throw new IllegalArgumentException(
            "Unable to match blueprint host group token to a host group: " + groupName);
      }

      for (String host : hostGroupInfo.getHostNames()) {
        if (port != null) {
          host += ":" + port;
        }
        hosts.add(host);
      }
    }
    return hosts;
  }

  /**
   * Convenience method for splitting out the HA-related properties, while
   *   also removing leading/trailing whitespace.
   *
   * @param propertyName property name to parse
   *
   * @return an array of Strings that represent the comma-separated
   *         elements in this property
   */
  private static String[] splitAndTrimStrings(String propertyName) {
    if(propertyName != null) {
      List<String> namesWithoutWhitespace = new LinkedList<>();
      for (String service : propertyName.split(",")) {
        namesWithoutWhitespace.add(service.trim());
      }

      return namesWithoutWhitespace.toArray(new String[namesWithoutWhitespace.size()]);
    } else {
      return new String[0];
    }
  }

  /**
   * Update non topology related configuration properties for blueprint export.
   *
   * @param updaters       registered non topology updaters
   * @param configuration  configuration being processed
   */
  private void doNonTopologyUpdate(Map<String, Map<String, PropertyUpdater>> updaters, Configuration configuration) {
    Map<String, Map<String, String>> properties = configuration.getFullProperties();
    for (Map.Entry<String, Map<String, PropertyUpdater>> entry : updaters.entrySet()) {
      String type = entry.getKey();
      for (String propertyName : entry.getValue().keySet()) {
        NonTopologyUpdater npu = (NonTopologyUpdater) entry.getValue().get(propertyName);
        Map<String, String> typeProperties = properties.get(type);

        if (typeProperties != null && typeProperties.containsKey(propertyName)) {
          String newValue = npu.updateForBlueprintExport(propertyName, typeProperties.get(propertyName), properties, clusterTopology);
          configuration.setProperty(type, propertyName, newValue);
        }
      }
    }
  }

  /**
   * Provides functionality to update a property value.
   */
  public interface PropertyUpdater {
    /**
     * Update a property value.
     *
     * @param propertyName    property name
     * @param origValue       original value of property
     * @param properties      all properties
     * @param topology        cluster topology
     *
     * @return new property value
     */
    String updateForClusterCreate(String propertyName,
                                         String origValue,
                                         Map<String, Map<String, String>> properties,
                                         ClusterTopology topology);

    /**
     * Determine the required host groups for the provided property.
     *
     * @param propertyName    property name
     * @param origValue       original value of property
     * @param properties      all properties
     * @param topology        cluster topology
     *
     * @return new property value
     */
    Collection<String> getRequiredHostGroups(String propertyName,
                                             String origValue,
                                             Map<String, Map<String, String>> properties,
                                             ClusterTopology topology);
  }

  private static class HostGroupUpdater implements PropertyUpdater {

    public static final PropertyUpdater INSTANCE = new HostGroupUpdater();

    @Override
    public String updateForClusterCreate(String propertyName,
      String origValue,
      Map<String, Map<String, String>> properties,
      ClusterTopology topology) {

      //todo: getHostStrings
      Matcher m = HOST_GROUP_PLACEHOLDER_PATTERN.matcher(origValue);
      if (m.find()) {
        String hostGroupName = m.group(1);

        HostGroupInfo groupInfo = topology.getHostGroupInfo().get(hostGroupName);
        if (groupInfo == null) {
          //todo: this should be validated in configuration validation
          throw new RuntimeException(
            "Encountered a host group token in configuration which couldn't be matched to a host group: "
              + hostGroupName);
        }

        //todo: warn if > hosts
        return origValue.replace(m.group(0), groupInfo.getHostNames().iterator().next());
      }

      return origValue;
    }

    @Override
    public Collection<String> getRequiredHostGroups(String propertyName,
      String origValue,
      Map<String, Map<String, String>> properties,
      ClusterTopology topology) {
      //todo: getHostStrings
      Matcher m = HOST_GROUP_PLACEHOLDER_PATTERN.matcher(origValue);
      if (m.find()) {
        String hostGroupName = m.group(1);
        return Collections.singleton(hostGroupName);
      }
      return Collections.emptySet();
    }
  }

  /**
   * Topology based updater which replaces the original host name of a property with the host name
   * which runs the associated (master) component in the new cluster.
   */
  private static class SingleHostTopologyUpdater extends HostGroupUpdater {
    /**
     * Component name
     */
    private final String component;

    /**
     * Constructor.
     *
     * @param component  component name associated with the property
     */
    public SingleHostTopologyUpdater(String component) {
      this.component = component;
    }

    /**
     * Update the property with the new host name which runs the associated component.
     *
     * @param propertyName  name of property
     * @param origValue     original value of property
     * @param properties    all properties
     * @param topology      cluster topology
     *
     * @return updated property value with old host name replaced by new host name
     */
    @Override
    public String updateForClusterCreate(String propertyName,
                                         String origValue,
                                         Map<String, Map<String, String>> properties,
                                         ClusterTopology topology)  {

      String replacedValue = super.updateForClusterCreate(propertyName, origValue, properties, topology);
      if (!Objects.equals(origValue, replacedValue)) {
        return replacedValue;
      } else {
        int matchingGroupCount = topology.getHostGroupsForComponent(component).size();
        if (matchingGroupCount == 1) {
          //todo: warn if > 1 hosts
          return replacePropertyValue(origValue,
              topology.getHostAssignmentsForComponent(component).iterator().next(), properties);
        } else {
          //todo: extract all hard coded HA logic
          Cardinality cardinality = topology.getStack().getCardinality(component);
          // if no matching host groups are found for a component whose configuration
          // is handled by this updater, check the stack first to determine if
          // zero is a valid cardinality for this component.  This is necessary
          // in the case of a component in "technical preview" status, since it
          // may be valid to have 0 or 1 instances of such a component in the cluster
          if (matchingGroupCount == 0 && cardinality.isValidCount(0)) {
            return origValue;
          } else {
            Map<String, Map<String, String>> configProperties = topology.getConfiguration().getFullProperties();
            if (isComponentNameNode() && (matchingGroupCount == 2) && isNameNodeHAEnabled(configProperties)) {
              // if this is the defaultFS property, it should reflect the nameservice name,
              // rather than a hostname (used in non-HA scenarios)
              if (properties.containsKey("core-site") && properties.get("core-site").get("fs.defaultFS").equals(origValue)) {
                return origValue;
              }

              if (properties.containsKey("hbase-site") && properties.get("hbase-site").get("hbase.rootdir").equals(origValue)) {
                // hbase-site's reference to the namenode is handled differently in HA mode, since the
                // reference must point to the logical nameservice, rather than an individual namenode
                return origValue;
              }

              if (properties.containsKey("accumulo-site") && properties.get("accumulo-site").get("instance.volumes").equals(origValue)) {
                // accumulo-site's reference to the namenode is handled differently in HA mode, since the
                // reference must point to the logical nameservice, rather than an individual namenode
                return origValue;
              }

              if (!origValue.contains("localhost")) {
                // if this NameNode HA property is a FDQN, then simply return it
                return origValue;
              }

            }

            if (isComponentSecondaryNameNode() && (matchingGroupCount == 0) && isNameNodeHAEnabled(configProperties)) {
              // if HDFS HA is enabled, then no replacement is necessary for properties that refer to the SECONDARY_NAMENODE
              // eventually this type of information should be encoded in the stacks
              return origValue;
            }

            if (isComponentResourceManager() && (matchingGroupCount == 2) && isYarnResourceManagerHAEnabled(configProperties)) {
              if (!origValue.contains("localhost")) {
                // if this Yarn property is a FQDN, then simply return it
                return origValue;
              }
            }

            if ((isOozieServerHAEnabled(properties)) && isComponentOozieServer() && (matchingGroupCount > 1))     {
              if (!origValue.contains("localhost")) {
                // if this Oozie property is a FQDN, then simply return it
                return origValue;
              }
            }

            if ((isHiveServerHAEnabled(properties)) && isComponentHiveServer() && (matchingGroupCount > 1)) {
              if (!origValue.contains("localhost")) {
                // if this Hive property is a FQDN, then simply return it
                return origValue;
              }
            }

            if ((isComponentHiveMetaStoreServer()) && matchingGroupCount > 1) {
              if (!origValue.contains("localhost")) {
                // if this Hive MetaStore property is a FQDN, then simply return it
                return origValue;
              }
            }

            if (isRangerAdmin() && matchingGroupCount > 1) {
              if (origValue != null && !origValue.contains("localhost")) {
                // if this Ranger admin property is a FQDN then simply return it
                return origValue;
              }
            }

            if ((isComponentAppTimelineServer() || isComponentHistoryServer()) &&
              (matchingGroupCount > 1 && origValue != null && !origValue.contains("localhost"))) {
                // in case of multiple component instances of AppTimelineServer or History Server leave custom value
                // if set
                return origValue;
            }

            throw new IllegalArgumentException(
                String.format("Unable to update configuration property '%s' with topology information. " +
                    "Component '%s' is mapped to an invalid number of hosts '%s'.", propertyName, component, matchingGroupCount));
          }
        }
      }
    }

    public String replacePropertyValue(String origValue, String host, Map<String, Map<String, String>> properties) {
      return origValue.replace("localhost", host);
    }

    @Override
    public Collection<String> getRequiredHostGroups(String propertyName,
                                                    String origValue,
                                                    Map<String, Map<String, String>> properties,
                                                    ClusterTopology topology) {
      Collection<String> result = super.getRequiredHostGroups(propertyName, origValue, properties, topology);
      if (!result.isEmpty()) {
        return result;
      } else {
        Collection<String> matchingGroups = topology.getHostGroupsForComponent(component);
        int matchingGroupCount = matchingGroups.size();
        if (matchingGroupCount != 0) {
          return new HashSet<>(matchingGroups);
        } else {
          Cardinality cardinality = topology.getStack().getCardinality(component);
          // if no matching host groups are found for a component whose configuration
          // is handled by this updater, return an empty set
          if (! cardinality.isValidCount(0)) {
            LOG.warn("The property '{}' is associated with the component '{}' which isn't mapped to any host group. " +
                    "This may affect configuration topology resolution.", propertyName, component);
          }
          return Collections.emptySet();
        }
      }
    }

    /**
     * Utility method to determine if the component associated with this updater
     * instance is an HDFS NameNode
     *
     * @return true if the component associated is a NameNode
     *         false if the component is not a NameNode
     */
    private boolean isComponentNameNode() {
      return component.equals("NAMENODE");
    }


    /**
     * Utility method to determine if the component associated with this updater
     * instance is an HDFS Secondary NameNode
     *
     * @return true if the component associated is a Secondary NameNode
     *         false if the component is not a Secondary NameNode
     */
    private boolean isComponentSecondaryNameNode() {
      return component.equals("SECONDARY_NAMENODE");
    }

    /**
     * Utility method to determine if the component associated with this updater
     * instance is a Yarn ResourceManager
     *
     * @return true if the component associated is a Yarn ResourceManager
     *         false if the component is not a Yarn ResourceManager
     */
    private boolean isComponentResourceManager() {
      return component.equals("RESOURCEMANAGER");
    }

    /**
     * Utility method to determine if the component associated with this updater
     * instance is an Oozie Server
     *
     * @return true if the component associated is an Oozie Server
     *         false if the component is not an Oozie Server
     */
    private boolean isComponentOozieServer() {
      return component.equals("OOZIE_SERVER");
    }

    /**
     * Utility method to determine if the component associated with this updater
     * instance is a Hive Server
     *
     * @return true if the component associated is a Hive Server
     *         false if the component is not a Hive Server
     */
    private boolean isComponentHiveServer() {
      return component.equals("HIVE_SERVER");
    }

    /**
     * Utility method to determine if the component associated with this updater
     * instance is a Hive MetaStore Server
     *
     * @return true if the component associated is a Hive MetaStore Server
     *         false if the component is not a Hive MetaStore Server
     */
    private boolean isComponentHiveMetaStoreServer() {
      return component.equals("HIVE_METASTORE");
    }

    /**
     * Utility method to determine if the component associated with this updater
     * instance is Ranger Admin
     *
     * @return true if the component associated is Ranger Admin
     *         false if the component is not Ranger Admin
     */
    private boolean isRangerAdmin() {
      return component.equals("RANGER_ADMIN");
    }


    /**
     * Utility method to determine if the component associated with this updater
     * instance is a History Server
     *
     * @return true if the component associated is a History Server
     *         false if the component is not a History Server
     */
    private boolean isComponentHistoryServer() {
      return component.equals("HISTORYSERVER");
    }


    /**
     * Utility method to determine if the component associated with this updater
     * instance is a AppTimeline Server
     *
     * @return true if the component associated is a AppTimeline Server
     *         false if the component is not a AppTimeline Server
     */
    private boolean isComponentAppTimelineServer() {
      return component.equals("APP_TIMELINE_SERVER");
    }


    /**
     * Provides access to the name of the component associated
     *   with this updater instance.
     *
     * @return component name for this updater
     */
    public String getComponentName() {
      return component;
    }
  }

  /**
   * Extension of SingleHostTopologyUpdater that supports the
   * notion of an optional service.  An example: the Storm
   * service has config properties that require the location
   * of the Ganglia server when Ganglia is deployed, but Storm
   * should also start properly without Ganglia.
   *
   * This updater detects the case when the specified component
   * is not found, and returns the original property value.
   *
   */
  private static class OptionalSingleHostTopologyUpdater extends SingleHostTopologyUpdater {

    public OptionalSingleHostTopologyUpdater(String component) {
      super(component);
    }

    @Override
    public String updateForClusterCreate(String propertyName,
                                         String origValue,
                                         Map<String, Map<String, String>> properties,
                                         ClusterTopology topology) {
      try {
        return super.updateForClusterCreate(propertyName, origValue, properties, topology);
      } catch (IllegalArgumentException illegalArgumentException) {
        // return the original value, since the optional component is not available in this cluster
        return origValue;
      }
    }

    @Override
    public Collection<String> getRequiredHostGroups(String propertyName,
                                                    String origValue,
                                                    Map<String, Map<String, String>> properties,
                                                    ClusterTopology topology) {

      try {
        return super.getRequiredHostGroups(propertyName, origValue, properties, topology);
      } catch (IllegalArgumentException e) {
        return Collections.emptySet();
      }
    }
  }

  /**
   * Topology based updater which replaces the original host name of a database property with the host name
   * where the DB is deployed in the new cluster.  If an existing database is specified, the original property
   * value is returned.
   */
  private static class DBTopologyUpdater extends SingleHostTopologyUpdater {
    /**
     * Property type (global, core-site ...) for property which is used to determine if DB is external.
     */
    private final String configPropertyType;

    /**
     * Name of property which is used to determine if DB is new or existing (exernal).
     */
    private final String conditionalPropertyName;

    /**
     * Constructor.
     *
     * @param component                component to get hot name if new DB
     * @param conditionalPropertyType  config type of property used to determine if DB is external
     * @param conditionalPropertyName  name of property which is used to determine if DB is external
     */
    private DBTopologyUpdater(String component, String conditionalPropertyType,
                              String conditionalPropertyName) {
      super(component);
      configPropertyType = conditionalPropertyType;
      this.conditionalPropertyName = conditionalPropertyName;
    }

    /**
     * If database is a new managed database, update the property with the new host name which
     * runs the associated component.  If the database is external (non-managed), return the
     * original value.
     *
     * @param propertyName  property name
     * @param origValue     original value of property
     * @param properties    all properties
     * @param topology      cluster topology
     *
     * @return updated property value with old host name replaced by new host name or original value
     *         if the database is external
     */
    @Override

    public String updateForClusterCreate(String propertyName,
                                         String origValue,
                                         Map<String, Map<String, String>> properties,
                                         ClusterTopology topology) {

      if (isDatabaseManaged(properties)) {
        return super.updateForClusterCreate(propertyName, origValue, properties, topology);
      } else {
        return origValue;
      }
    }

    @Override
    public Collection<String> getRequiredHostGroups(String propertyName,
                                                    String origValue,
                                                    Map<String, Map<String, String>> properties,
                                                    ClusterTopology topology) {
      if (isDatabaseManaged(properties)) {
        return super.getRequiredHostGroups(propertyName, origValue, properties, topology);
      } else {
        return Collections.emptySet();
      }
    }

    /**
     * Determine if database is managed, meaning that it is a component in the cluster topology.
     *
     * @return true if the DB is managed; false otherwise
     */
    private boolean isDatabaseManaged(Map<String, Map<String, String>> properties) {
      // conditional property should always exist since it is required to be specified in the stack
      return properties.get(configPropertyType).
          get(conditionalPropertyName).startsWith("New");
    }
  }

  /**
   * Topology based updater which replaces original host names (possibly more than one) contained in a property
   * value with the host names which runs the associated component in the new cluster.
   */
  protected static class MultipleHostTopologyUpdater implements PropertyUpdater {

    private static final Character DEFAULT_SEPARATOR = ',';

    /**
     * Component name
     */
    private final String component;

    /**
     * Separator for multiple property values
     */
    private final Character separator;

    /**
     * Flag to determine if a URL scheme detected as
     * a prefix in the property should be repeated across
     * all hosts in the property
     */
    private final boolean usePrefixForEachHost;

    private final boolean useSuffixForEachHost;

    private final boolean usePortForEachHost;

    /**
     * Constructor.
     *
     * @param component  component name associated with the property
     */
    public MultipleHostTopologyUpdater(String component) {
      this(component, DEFAULT_SEPARATOR, false, false, true);
    }

    /**
     * Constructor
     *
     * @param component component name associated with this property
     * @param separator the separator character to use when multiple hosts
     *                  are specified in a property or URL
     */
    public MultipleHostTopologyUpdater(String component, Character separator, boolean usePrefixForEachHost, boolean useSuffixForEachHost, boolean usePortForEachHost) {
      this.component = component;
      this.separator = separator;
      this.usePrefixForEachHost = usePrefixForEachHost;
      this.useSuffixForEachHost = useSuffixForEachHost;
      this.usePortForEachHost = usePortForEachHost;
    }

    /**
     * Update all host names included in the original property value with new host names which run the associated
     * component.
     *
     * @param propertyName property name
     * @param origValue    original value of property
     * @param properties   all properties
     * @param topology     cluster topology
     * @return updated property value with old host names replaced by new host names
     */
    @Override
    public String updateForClusterCreate(String propertyName,
                                         String origValue,
                                         Map<String, Map<String, String>> properties,
                                         ClusterTopology topology) {

      StringBuilder sb = new StringBuilder();

      if (!origValue.contains("%HOSTGROUP") && (!origValue.contains("localhost"))) {
        // this property must contain FQDNs specified directly by the user
        // of the Blueprint, so the processor should not attempt to update them
        return origValue;
      }

      Collection<String> hostStrings = getHostStrings(origValue, topology);
      hostStrings.addAll(getHostStringsFromLocalhost(origValue, topology));

      return resolveHostGroupPlaceholder(origValue, hostStrings);
    }

    /**
     * Gets the prefix for hosts
     * @param value property value
     * @return prefix
     */
    private String getPrefix(String value) {
      Matcher localhostMatcher = LOCALHOST_PORT_REGEX.matcher(value);
      Matcher hostGroupMatcher = HOSTGROUP_PORT_REGEX.matcher(value);
      String prefixCandidate = null;

      if(localhostMatcher.find()) {
        prefixCandidate = value.substring(0,localhostMatcher.start());
      } else if(hostGroupMatcher.find()) {
        prefixCandidate = value.substring(0,hostGroupMatcher.start());
      } else {
        return prefixCandidate;
      }

      // remove YAML array notation
      if(prefixCandidate.startsWith("[")) {
        prefixCandidate = prefixCandidate.substring(1);
      }
      // remove YAML string notation
      if(prefixCandidate.startsWith("'")) {
        prefixCandidate = prefixCandidate.substring(1);
      }

      return prefixCandidate;
    }

    /**
     * Gets the suffix for hosts
     * @param value property value
     * @return suffix
     */
    private String getSuffix(String value) {
      Matcher localhostMatcher = LOCALHOST_PORT_REGEX.matcher(value);
      Matcher hostGroupMatcher = HOSTGROUP_PORT_REGEX.matcher(value);


      Matcher activeMatcher = null;

      if(localhostMatcher.find()) {
        activeMatcher = localhostMatcher;
      } else if(hostGroupMatcher.find()) {
        activeMatcher = hostGroupMatcher;
      } else {
        return null;
      }

      String suffixCandidate = null;
      int indexOfEnd;
      do {
        indexOfEnd = activeMatcher.end();
      } while (activeMatcher.find());
      suffixCandidate = value.substring(indexOfEnd);

      // remove YAML array notation
      if(suffixCandidate.endsWith("]")) {
        suffixCandidate = suffixCandidate.substring(0, suffixCandidate.length()-1);
      }
      // remove YAML string notation
      if(suffixCandidate.endsWith("'")) {
        suffixCandidate = suffixCandidate.substring(0, suffixCandidate.length()-1);
      }

      return suffixCandidate;
    }

    /**
     * Resolves localhost value to "host:port" elements (port is optional)
     * @param origValue property value
     * @param topology cluster topology
     * @return list of hosts that have the given components
     */
    private Collection<String> getHostStringsFromLocalhost(String origValue, ClusterTopology topology) {
      Set<String> hostStrings = new HashSet<>();
      if(origValue.contains("localhost")) {
        Matcher localhostMatcher = LOCALHOST_PORT_REGEX.matcher(origValue);
        String port = null;
        if(localhostMatcher.find()) {
          port = calculatePort(localhostMatcher.group());
        }
        for (String host : topology.getHostAssignmentsForComponent(component)) {
          if (port != null) {
            host += ":" + port;
          }
          hostStrings.add(host);
        }
      }
      return hostStrings;
    }

    /**
     * Resolves the host group place holders in the passed in original value.
     * @param originalValue The original value containing the place holders to be resolved.
     * @param hostStrings The collection of host names that are mapped to the host groups to be resolved
     * @return The new value with place holders resolved.
     */
    protected String resolveHostGroupPlaceholder(String originalValue, Collection<String> hostStrings) {
      String prefix = getPrefix(originalValue);
      String suffix = getSuffix(originalValue);
      String port = removePorts(hostStrings);

      String sep = (useSuffixForEachHost ? suffix : "") + separator + (usePrefixForEachHost ? prefix : "");
      String combinedHosts = (usePrefixForEachHost ? prefix : "") + StringUtils.join(hostStrings, sep);

      return (usePrefixForEachHost ? "" : prefix) + combinedHosts + (usePortForEachHost || port == null ? "" : ":" + port) + suffix;
    }

    /**
     * Removes "port" part of the hosts and returns it
     * @param hostStrings list of "host:port" strings (port is optional)
     * @return the port
     */
    private String removePorts(Collection<String> hostStrings) {
      String port = null;
      if(!usePortForEachHost && !hostStrings.isEmpty()) {
        Set<String> temp = new HashSet<>();

        // extract port
        Iterator<String> i = hostStrings.iterator();
        do {
          port = calculatePort(i.next());
        } while (i.hasNext() && port == null);

        // update hosts
        if(port != null) {
          for(String host : hostStrings) {
            temp.add(host.replace(":"+port,""));
          }
        }
        hostStrings.clear();
        hostStrings.addAll(temp);
      }
      return port;
    }

    private static String calculatePort(String origValue) {
      if (origValue.contains(":")) {
        //todo: currently assuming all hosts are using same port
        return origValue.substring(origValue.indexOf(":") + 1);
      }

      return null;
    }

    @Override
    public Collection<String> getRequiredHostGroups(String propertyName,
                                                    String origValue,
                                                    Map<String, Map<String, String>> properties,
                                                    ClusterTopology topology) {

      Collection<String> requiredHostGroups = new HashSet<>();

      // add all host groups specified in host group tokens
      Matcher m = HOSTGROUP_PORT_REGEX.matcher(origValue);
      while (m.find()) {
        String groupName = m.group(1);

        if (!topology.getHostGroupInfo().containsKey(groupName)) {
          throw new IllegalArgumentException(
              "Unable to match blueprint host group token to a host group: " + groupName);
        }
        requiredHostGroups.add(groupName);
      }

      //todo: for now assuming that we will either have HG tokens or standard replacement but not both
      //todo: as is done in updateForClusterCreate
      if (requiredHostGroups.isEmpty()) {
        requiredHostGroups.addAll(topology.getHostGroupsForComponent(component));
      }

      return requiredHostGroups;
    }
  }

  /**
   * Class to facilitate special formatting needs of property values.
   */
  private abstract static class AbstractPropertyValueDecorator implements PropertyUpdater {
    PropertyUpdater propertyUpdater;

    /**
     * Constructor.
     *
     * @param propertyUpdater  wrapped updater
     */
    public AbstractPropertyValueDecorator(PropertyUpdater propertyUpdater) {
      this.propertyUpdater = propertyUpdater;
    }

    /**
     * Return decorated form of the updated input property value.
     *
     * @param propertyName  property name
     * @param origValue     original value of property
     * @param properties    all properties
     * @param topology      cluster topology
     *
     * @return Formatted output string
     */
    @Override
    public String updateForClusterCreate(String propertyName,
                                         String origValue,
                                         Map<String, Map<String, String>> properties,
                                         ClusterTopology topology) {

      // return customer-supplied properties without updating them
      if (isFQDNValue(origValue)) {
        return origValue;
      }

      return doFormat(propertyUpdater.updateForClusterCreate(propertyName, origValue, properties, topology));
    }

    /**
     * Transform input string to required output format.
     *
     * @param originalValue  original value of property
     *
     * @return formatted output string
     */
    public abstract String doFormat(String originalValue);

    @Override
    public Collection<String> getRequiredHostGroups(String propertyName,
                                                    String origValue,
                                                    Map<String, Map<String, String>> properties,
                                                    ClusterTopology topology) {

      return propertyUpdater.getRequiredHostGroups(propertyName, origValue, properties, topology);
    }

    /**
     * Convenience method to determine if a property value is a
     * customer-specified FQDN.
     *
     * @param value property value to examine
     * @return true if the property represents an FQDN value
     *         false if the property does not represent an FQDN value
     */
    public boolean isFQDNValue(String value) {
      return !value.contains("%HOSTGROUP") &&
            !value.contains("localhost");
    }
  }

  /**
   * Return properties of the form ['value']
   */
   static class YamlMultiValuePropertyDecorator extends AbstractPropertyValueDecorator {

    // currently, only plain and single-quoted Yaml flows are supported by this updater
    enum FlowStyle {
      SINGLE_QUOTED,
      PLAIN
    }

    /**
     * Regexp to extract the inner part of a string enclosed in []
     */
    private static Pattern REGEX_IN_BRACKETS = Pattern.compile("\\s*\\[(?<INNER>.*)\\]\\s*");
    /**
     * Regexp to extract the inner part of a string enclosed in ''
     */
    private static Pattern REGEX_IN_QUOTES = Pattern.compile("\\s*'(?<INNER>.*)'\\s*");

    private final FlowStyle flowStyle;

    public YamlMultiValuePropertyDecorator(PropertyUpdater propertyUpdater) {
      // single-quote style is considered default by this updater
      this(propertyUpdater, FlowStyle.SINGLE_QUOTED);
    }

    protected YamlMultiValuePropertyDecorator(PropertyUpdater propertyUpdater, FlowStyle flowStyle) {
      super(propertyUpdater);
      this.flowStyle = flowStyle;
    }

    /**
     * Format input String of the form, str1,str2 to ['str1','str2']
     * If the input string is already surrounded by [] ignore those
     * and process the part from within the square brackets.
     * @param origValue  input string
     *
     * @return formatted string
     */
    @Override
    public String doFormat(String origValue) {
      StringBuilder sb = new StringBuilder();

      Matcher m = REGEX_IN_BRACKETS.matcher(origValue);
      if (m.matches()) {
        origValue = m.group("INNER");
      }

      if (origValue != null) {
        sb.append("[");
        boolean isFirst = true;
        for (String value : origValue.split(",")) {

          m = REGEX_IN_QUOTES.matcher(value);
          if (m.matches()) {
            value = m.group("INNER");
          }

          if (!isFirst) {
            sb.append(",");
          } else {
            isFirst = false;
          }

          if (flowStyle == FlowStyle.SINGLE_QUOTED) {
            sb.append("'");
          }

          sb.append(value);

          if (flowStyle == FlowStyle.SINGLE_QUOTED) {
            sb.append("'");
          }

        }
        sb.append("]");
      }
      return sb.toString();
    }
  }

  /**
   * PropertyUpdater implementation that will always return the original
   *   value for the updateForClusterCreate() method.
   *   This updater type should only be used in cases where a given
   *   property requires no updates, but may need to be considered
   *   during the Blueprint export process.
   */
  private static class OriginalValuePropertyUpdater implements PropertyUpdater {

    @Override
    public String updateForClusterCreate(String propertyName,
                                         String origValue,
                                         Map<String, Map<String, String>> properties,
                                         ClusterTopology topology) {
      // always return the original value, since these properties do not require update handling
      return origValue;
    }

    @Override
    public Collection<String> getRequiredHostGroups(String propertyName,
                                                    String origValue,
                                                    Map<String, Map<String, String>> properties,
                                                    ClusterTopology topology) {

      return Collections.emptySet();
    }
  }

  /**
   * Custom PropertyUpdater that handles the parsing and updating of the
   * "templeton.hive.properties" configuration property for WebHCat.
   * This particular configuration property uses a format of
   * comma-separated key/value pairs.  The Values in the case of the
   * hive.metastores.uri property can also contain commas, and so character
   * escaping with a backslash (\) must take place during substitution.
   *
   */
  private static class TempletonHivePropertyUpdater implements PropertyUpdater {

    private Map<String, PropertyUpdater> mapOfKeysToUpdaters =
      new HashMap<>();

    TempletonHivePropertyUpdater() {
      // the only known property that requires hostname substitution is hive.metastore.uris,
      // but this updater should be flexible enough for other properties in the future.
      mapOfKeysToUpdaters.put("hive.metastore.uris", new MultipleHostTopologyUpdater("HIVE_METASTORE", ',', true, false, true));
    }

    @Override
    public String updateForClusterCreate(String propertyName,
                                         String origValue,
                                         Map<String, Map<String, String>> properties,
                                         ClusterTopology topology) {

      // short-circuit out any custom property values defined by the deployer
      if (!origValue.contains("%HOSTGROUP") &&
        (!origValue.contains("localhost"))) {
        // this property must contain FQDNs specified directly by the user
        // of the Blueprint, so the processor should not attempt to update them
        return origValue;
      }

      StringBuilder updatedResult = new StringBuilder();
      // split out the key/value pairs
      String[] keyValuePairs = origValue.split(",");
      boolean firstValue = true;
      for (String keyValuePair : keyValuePairs) {
        keyValuePair = keyValuePair.trim();
        if (!firstValue) {
          updatedResult.append(",");
        } else {
          firstValue = false;
        }

        String key = keyValuePair.split("=")[0].trim();
        if (mapOfKeysToUpdaters.containsKey(key)) {
          String result = mapOfKeysToUpdaters.get(key).updateForClusterCreate(
              key, keyValuePair.split("=")[1].trim(), properties, topology);
          // append the internal property result, escape out any commas in the internal property,
          // this is required due to the specific syntax of templeton.hive.properties
          updatedResult.append(key);
          updatedResult.append("=");
          updatedResult.append(result.replaceAll(",", Matcher.quoteReplacement("\\,")));
        } else {
          updatedResult.append(keyValuePair);
        }
      }

      return updatedResult.toString();
    }

    @Override
    public Collection<String> getRequiredHostGroups(String propertyName,
                                                    String origValue,
                                                    Map<String, Map<String, String>> properties,
                                                    ClusterTopology topology) {

      // short-circuit out any custom property values defined by the deployer
      if (!origValue.contains("%HOSTGROUP") &&
          (!origValue.contains("localhost"))) {
        // this property must contain FQDNs specified directly by the user
        // of the Blueprint, so the processor should not attempt to update them
        return Collections.emptySet();
      }

      Collection<String> requiredGroups = new HashSet<>();
      // split out the key/value pairs
      String[] keyValuePairs = origValue.split(",");
      for (String keyValuePair : keyValuePairs) {
        String key = keyValuePair.split("=")[0];
        if (mapOfKeysToUpdaters.containsKey(key)) {
          requiredGroups.addAll(mapOfKeysToUpdaters.get(key).getRequiredHostGroups(
              propertyName, keyValuePair.split("=")[1], properties, topology));
        }
      }
      return requiredGroups;
    }
  }

  /**
   * A topology independent updater which provides a default implementation of getRequiredHostGroups
   * since no topology related information is required by the updater.
   */
  private static abstract class NonTopologyUpdater implements PropertyUpdater {
    @Override
    public Collection<String> getRequiredHostGroups(String propertyName,
                                                    String origValue,
                                                    Map<String, Map<String, String>> properties,
                                                    ClusterTopology topology) {
      return Collections.emptyList();
    }

    public String updateForBlueprintExport(String propertyName,
                                           String origValue,
                                           Map<String, Map<String, String>> properties,
                                           ClusterTopology topology) {
      return origValue;
    }
  }


  // Register updaters for configuration properties.
  static {

    allUpdaters.add(singleHostTopologyUpdaters);
    allUpdaters.add(multiHostTopologyUpdaters);
    allUpdaters.add(dbHostTopologyUpdaters);
    allUpdaters.add(mPropertyUpdaters);
    allUpdaters.add(nonTopologyUpdaters);

    Map<String, PropertyUpdater> amsSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> druidCommon = new HashMap<>();
    Map<String, PropertyUpdater> hdfsSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> mapredSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> coreSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> hbaseSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> yarnSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> hiveSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> hiveSiteNonTopologyMap = new HashMap<>();
    Map<String, PropertyUpdater> hiveEnvOriginalValueMap = new HashMap<>();
    Map<String, PropertyUpdater> oozieSiteOriginalValueMap = new HashMap<>();
    Map<String, PropertyUpdater> oozieSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> stormSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> stormSiteNonTopologyMap = new HashMap<>();
    Map<String, PropertyUpdater> accumuloSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> falconStartupPropertiesMap = new HashMap<>();
    Map<String, PropertyUpdater> kafkaBrokerMap = new HashMap<>();
    Map<String, PropertyUpdater> kafkaBrokerNonTopologyMap = new HashMap<>();
    Map<String, PropertyUpdater> atlasPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> mapredEnvMap = new HashMap<>();
    Map<String, PropertyUpdater> mHadoopEnvMap = new HashMap<>();
    Map<String, PropertyUpdater> shHadoopEnvMap = new HashMap<>();
    Map<String, PropertyUpdater> hbaseEnvMap = new HashMap<>();
    Map<String, PropertyUpdater> hiveEnvMap = new HashMap<>();
    Map<String, PropertyUpdater> hiveInteractiveEnvMap = new HashMap<>();
    Map<String, PropertyUpdater> hiveInteractiveSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> oozieEnvMap = new HashMap<>();
    Map<String, PropertyUpdater> oozieEnvHeapSizeMap = new HashMap<>();
    Map<String, PropertyUpdater> multiWebhcatSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> multiHbaseSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> multiStormSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> multiCoreSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> multiHdfsSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> multiHiveSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> multiKafkaBrokerMap = new HashMap<>();
    Map<String, PropertyUpdater> multiSliderClientMap = new HashMap<>();
    Map<String, PropertyUpdater> multiYarnSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> multiOozieSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> multiAccumuloSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> multiRangerKmsSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> dbHiveSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerAdminPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerEnvPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerYarnAuditPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerHdfsAuditPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerHbaseAuditPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerHiveAuditPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerKnoxAuditPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerKafkaAuditPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerStormAuditPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> rangerAtlasAuditPropsMap = new HashMap<>();
    Map<String, PropertyUpdater> hawqSiteMap = new HashMap<>();
    Map<String, PropertyUpdater> zookeeperEnvMap = new HashMap<>();

    singleHostTopologyUpdaters.put("ams-site", amsSiteMap);
    singleHostTopologyUpdaters.put("druid-common", druidCommon);
    singleHostTopologyUpdaters.put("hdfs-site", hdfsSiteMap);
    singleHostTopologyUpdaters.put("mapred-site", mapredSiteMap);
    singleHostTopologyUpdaters.put("core-site", coreSiteMap);
    singleHostTopologyUpdaters.put("hbase-site", hbaseSiteMap);
    singleHostTopologyUpdaters.put("yarn-site", yarnSiteMap);
    singleHostTopologyUpdaters.put("hive-site", hiveSiteMap);
    singleHostTopologyUpdaters.put("hive-interactive-env", hiveInteractiveEnvMap);
    singleHostTopologyUpdaters.put("storm-site", stormSiteMap);
    singleHostTopologyUpdaters.put("accumulo-site", accumuloSiteMap);
    singleHostTopologyUpdaters.put("falcon-startup.properties", falconStartupPropertiesMap);
    singleHostTopologyUpdaters.put("hive-env", hiveEnvMap);
    singleHostTopologyUpdaters.put("oozie-env", oozieEnvMap);
    singleHostTopologyUpdaters.put("kafka-broker", kafkaBrokerMap);
    singleHostTopologyUpdaters.put("application-properties", atlasPropsMap);
    singleHostTopologyUpdaters.put("admin-properties", rangerAdminPropsMap);
    singleHostTopologyUpdaters.put("ranger-env", rangerEnvPropsMap);
    singleHostTopologyUpdaters.put("ranger-yarn-audit", rangerYarnAuditPropsMap);
    singleHostTopologyUpdaters.put("ranger-hdfs-audit", rangerHdfsAuditPropsMap);
    singleHostTopologyUpdaters.put("ranger-hbase-audit", rangerHbaseAuditPropsMap);
    singleHostTopologyUpdaters.put("ranger-hive-audit", rangerHiveAuditPropsMap);
    singleHostTopologyUpdaters.put("ranger-knox-audit", rangerKnoxAuditPropsMap);
    singleHostTopologyUpdaters.put("ranger-kafka-audit", rangerKafkaAuditPropsMap);
    singleHostTopologyUpdaters.put("ranger-storm-audit", rangerStormAuditPropsMap);
    singleHostTopologyUpdaters.put("ranger-atlas-audit", rangerAtlasAuditPropsMap);
    singleHostTopologyUpdaters.put("hadoop-env", shHadoopEnvMap);

    singleHostTopologyUpdaters.put("hawq-site", hawqSiteMap);
    singleHostTopologyUpdaters.put("zookeeper-env", zookeeperEnvMap);


    mPropertyUpdaters.put("hadoop-env", mHadoopEnvMap);
    mPropertyUpdaters.put("hbase-env", hbaseEnvMap);
    mPropertyUpdaters.put("mapred-env", mapredEnvMap);
    mPropertyUpdaters.put("oozie-env", oozieEnvHeapSizeMap);

    multiHostTopologyUpdaters.put("webhcat-site", multiWebhcatSiteMap);
    multiHostTopologyUpdaters.put("hbase-site", multiHbaseSiteMap);
    multiHostTopologyUpdaters.put("storm-site", multiStormSiteMap);
    multiHostTopologyUpdaters.put("core-site", multiCoreSiteMap);
    multiHostTopologyUpdaters.put("hdfs-site", multiHdfsSiteMap);
    multiHostTopologyUpdaters.put("hive-site", multiHiveSiteMap);
    multiHostTopologyUpdaters.put("hive-interactive-site", hiveInteractiveSiteMap);
    multiHostTopologyUpdaters.put("kafka-broker", multiKafkaBrokerMap);
    multiHostTopologyUpdaters.put("slider-client", multiSliderClientMap);
    multiHostTopologyUpdaters.put("yarn-site", multiYarnSiteMap);
    multiHostTopologyUpdaters.put("oozie-site", multiOozieSiteMap);
    multiHostTopologyUpdaters.put("accumulo-site", multiAccumuloSiteMap);
    multiHostTopologyUpdaters.put("kms-site", multiRangerKmsSiteMap);

    dbHostTopologyUpdaters.put("hive-site", dbHiveSiteMap);

    nonTopologyUpdaters.put("hive-site", hiveSiteNonTopologyMap);
    nonTopologyUpdaters.put("kafka-broker", kafkaBrokerNonTopologyMap);
    nonTopologyUpdaters.put("storm-site", stormSiteNonTopologyMap);

    //todo: Need to change updaters back to being static
    //todo: will need to pass ClusterTopology in as necessary


    // NAMENODE
    hdfsSiteMap.put("dfs.http.address", new SingleHostTopologyUpdater("NAMENODE"));
    hdfsSiteMap.put("dfs.https.address", new SingleHostTopologyUpdater("NAMENODE"));
    coreSiteMap.put("fs.default.name", new SingleHostTopologyUpdater("NAMENODE"));
    hdfsSiteMap.put("dfs.namenode.http-address", new SingleHostTopologyUpdater("NAMENODE"));
    hdfsSiteMap.put("dfs.namenode.https-address", new SingleHostTopologyUpdater("NAMENODE"));
    hdfsSiteMap.put("dfs.namenode.rpc-address", new SingleHostTopologyUpdater("NAMENODE"));
    coreSiteMap.put("fs.defaultFS", new SingleHostTopologyUpdater("NAMENODE"));
    hbaseSiteMap.put("hbase.rootdir", new SingleHostTopologyUpdater("NAMENODE"));
    accumuloSiteMap.put("instance.volumes", new SingleHostTopologyUpdater("NAMENODE"));
    // HDFS shared.edits JournalNode Quorum URL uses semi-colons as separators
    multiHdfsSiteMap.put("dfs.namenode.shared.edits.dir", new MultipleHostTopologyUpdater("JOURNALNODE", ';', false, false, true));
    multiHdfsSiteMap.put("dfs.encryption.key.provider.uri", new MultipleHostTopologyUpdater("RANGER_KMS_SERVER", ';', false, false, false));
    // Explicit initial primary/secondary node assignment in HA
    shHadoopEnvMap.put("dfs_ha_initial_namenode_active", new SingleHostTopologyUpdater("NAMENODE"));
    shHadoopEnvMap.put("dfs_ha_initial_namenode_standby", new SingleHostTopologyUpdater("NAMENODE"));

    // SECONDARY_NAMENODE
    hdfsSiteMap.put("dfs.secondary.http.address", new SingleHostTopologyUpdater("SECONDARY_NAMENODE"));
    hdfsSiteMap.put("dfs.namenode.secondary.http-address", new SingleHostTopologyUpdater("SECONDARY_NAMENODE"));

    // JOBTRACKER
    mapredSiteMap.put("mapred.job.tracker", new SingleHostTopologyUpdater("JOBTRACKER"));
    mapredSiteMap.put("mapred.job.tracker.http.address", new SingleHostTopologyUpdater("JOBTRACKER"));
    mapredSiteMap.put("mapreduce.history.server.http.address", new SingleHostTopologyUpdater("JOBTRACKER"));
    mapredSiteMap.put("mapreduce.job.hdfs-servers", new SingleHostTopologyUpdater("NAMENODE"));


    // HISTORY_SERVER
    yarnSiteMap.put("yarn.log.server.url", new SingleHostTopologyUpdater("HISTORYSERVER"));
    mapredSiteMap.put("mapreduce.jobhistory.webapp.address", new SingleHostTopologyUpdater("HISTORYSERVER"));
    mapredSiteMap.put("mapreduce.jobhistory.address", new SingleHostTopologyUpdater("HISTORYSERVER"));

    // RESOURCEMANAGER
    yarnSiteMap.put("yarn.resourcemanager.hostname", new SingleHostTopologyUpdater("RESOURCEMANAGER"));
    yarnSiteMap.put("yarn.resourcemanager.resource-tracker.address", new SingleHostTopologyUpdater("RESOURCEMANAGER"));
    yarnSiteMap.put("yarn.resourcemanager.webapp.address", new SingleHostTopologyUpdater("RESOURCEMANAGER"));
    yarnSiteMap.put("yarn.resourcemanager.scheduler.address", new SingleHostTopologyUpdater("RESOURCEMANAGER"));
    yarnSiteMap.put("yarn.resourcemanager.address", new SingleHostTopologyUpdater("RESOURCEMANAGER"));
    yarnSiteMap.put("yarn.resourcemanager.admin.address", new SingleHostTopologyUpdater("RESOURCEMANAGER"));
    yarnSiteMap.put("yarn.resourcemanager.webapp.https.address", new SingleHostTopologyUpdater("RESOURCEMANAGER"));

    // APP_TIMELINE_SERVER
    yarnSiteMap.put("yarn.timeline-service.address", new SingleHostTopologyUpdater("APP_TIMELINE_SERVER"));
    yarnSiteMap.put("yarn.timeline-service.webapp.address", new SingleHostTopologyUpdater("APP_TIMELINE_SERVER"));
    yarnSiteMap.put("yarn.timeline-service.webapp.https.address", new SingleHostTopologyUpdater("APP_TIMELINE_SERVER"));
    yarnSiteMap.put("yarn.log.server.web-service.url", new SingleHostTopologyUpdater("APP_TIMELINE_SERVER"));

    // HIVE_SERVER
    hiveSiteMap.put("hive.server2.authentication.ldap.url", new SingleHostTopologyUpdater("HIVE_SERVER2"));
    multiHiveSiteMap.put("hive.metastore.uris", new MultipleHostTopologyUpdater("HIVE_METASTORE", ',', true, true, true));
    dbHiveSiteMap.put("javax.jdo.option.ConnectionURL",
        new DBTopologyUpdater("MYSQL_SERVER", "hive-env", "hive_database"));
    multiCoreSiteMap.put("hadoop.proxyuser.hive.hosts", new MultipleHostTopologyUpdater("HIVE_SERVER"));
    multiCoreSiteMap.put("hadoop.proxyuser.HTTP.hosts", new MultipleHostTopologyUpdater("WEBHCAT_SERVER"));
    multiCoreSiteMap.put("hadoop.proxyuser.hcat.hosts", new MultipleHostTopologyUpdater("WEBHCAT_SERVER"));
    multiCoreSiteMap.put("hadoop.proxyuser.yarn.hosts", new MultipleHostTopologyUpdater("RESOURCEMANAGER"));
    multiCoreSiteMap.put("hadoop.security.key.provider.path", new MultipleHostTopologyUpdater("RANGER_KMS_SERVER", ';', false, false, true));
    multiWebhcatSiteMap.put("templeton.hive.properties", new TempletonHivePropertyUpdater());
    multiHiveSiteMap.put("hive.zookeeper.quorum", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    multiHiveSiteMap.put("hive.cluster.delegation.token.store.zookeeper.connectString", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));

    // HIVE Interactive Server
    hiveInteractiveEnvMap.put("hive_server_interactive_host", new SingleHostTopologyUpdater("HIVE_SERVER_INTERACTIVE"));
    hiveInteractiveSiteMap.put("hive.llap.zk.sm.connectionString", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));

    // HIVE Atlas integration
    hiveSiteNonTopologyMap.put("hive.exec.post.hooks", new NonTopologyUpdater() {
      @Override
      public String updateForClusterCreate(String propertyName,
                                           String origValue,
                                           Map<String, Map<String, String>> properties,
                                           ClusterTopology topology) {
        String atlasHookClass = "org.apache.atlas.hive.hook.HiveHook";
        String[] hiveHooks = origValue.split(",");

        List<String> hiveHooksClean = new ArrayList<>();
        for(String hiveHook : hiveHooks) {
          if (!StringUtils.isBlank(hiveHook.trim())) {
            hiveHooksClean.add(hiveHook.trim());
          }
        }

        boolean isAtlasInCluster = topology.getServices().contains("ATLAS");
        boolean isAtlasHiveHookEnabled = Boolean.parseBoolean(properties.get("hive-env").get("hive.atlas.hook"));

        // Append atlas hook if not already present.
        if (isAtlasInCluster || isAtlasHiveHookEnabled) {
          if (!hiveHooksClean.contains(atlasHookClass)) {
            hiveHooksClean.add(atlasHookClass);
          }
        } else {
          // Remove the atlas hook since Atlas service is not present.
          while (hiveHooksClean.contains(atlasHookClass)) {
            hiveHooksClean.remove(atlasHookClass);
          }
        }

        if (!hiveHooksClean.isEmpty()) {
          return StringUtils.join(hiveHooksClean, ",");
        } else {
          return " ";
        }
      }
    });

    // TODO AMBARI-17782, remove this property from hive-site only in HDP 2.5 and higher.
    hiveSiteNonTopologyMap.put("atlas.cluster.name", new NonTopologyUpdater() {
      @Override
      public String updateForClusterCreate(String propertyName,
                                           String origValue,
                                           Map<String, Map<String, String>> properties,
                                           ClusterTopology topology) {

        if (topology.getServices().contains("ATLAS")) {
          // if original value is not set or is the default "primary" set the cluster id
          if (origValue == null || origValue.trim().isEmpty() || origValue.equals("primary")) {
            //use cluster id because cluster name may change
            return String.valueOf(topology.getClusterId());
          } else {
            // if explicitly set by user, don't override
            return origValue;
          }
        } else {
          return origValue;
        }
      }

      @Override
      public String updateForBlueprintExport(String propertyName,
                                            String origValue,
                                            Map<String, Map<String, String>> properties,
                                            ClusterTopology topology) {

        // if the value is the cluster id, then update to primary
        if (origValue.equals(String.valueOf(topology.getClusterId()))) {
          return "primary";
        }
        return origValue;
      }
    });

    // TODO AMBARI-17782, remove this property only from HDP 2.5 and higher.
    hiveSiteMap.put("atlas.rest.address", new SingleHostTopologyUpdater("ATLAS_SERVER") {
      @Override
      public String updateForClusterCreate(String propertyName,
                                           String origValue,
                                           Map<String, Map<String, String>> properties,
                                           ClusterTopology topology) {
        if (topology.getServices().contains("ATLAS")) {
          String host = topology.getHostAssignmentsForComponent("ATLAS_SERVER").iterator().next();

          boolean tlsEnabled = Boolean.parseBoolean(properties.get("application-properties").get("atlas.enableTLS"));
          String scheme;
          String port;
          if (tlsEnabled) {
            scheme = "https";
            port = properties.get("application-properties").get("atlas.server.https.port");
          } else {
            scheme = "http";
            port = properties.get("application-properties").get("atlas.server.http.port");
          }

          return String.format("%s://%s:%s", scheme, host, port);
        }
        return origValue;
      }
    });


    // OOZIE_SERVER
    Map<String, PropertyUpdater> oozieStringPropertyUpdaterMap = singleHostTopologyUpdaters.get("oozie-site");
    if (oozieStringPropertyUpdaterMap == null) {
      oozieStringPropertyUpdaterMap = new HashMap<>();
    }
    oozieStringPropertyUpdaterMap.put("oozie.base.url", new SingleHostTopologyUpdater("OOZIE_SERVER"));
    singleHostTopologyUpdaters.put("oozie-site", oozieStringPropertyUpdaterMap);

    multiCoreSiteMap.put("hadoop.proxyuser.oozie.hosts", new MultipleHostTopologyUpdater("OOZIE_SERVER"));

    // ZOOKEEPER_SERVER
    multiHbaseSiteMap.put("hbase.zookeeper.quorum", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    multiWebhcatSiteMap.put("templeton.zookeeper.hosts", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    multiCoreSiteMap.put("ha.zookeeper.quorum", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    multiYarnSiteMap.put("hadoop.registry.zk.quorum", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    multiYarnSiteMap.put("yarn.resourcemanager.zk-address", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    multiSliderClientMap.put("slider.zookeeper.quorum", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    multiKafkaBrokerMap.put("zookeeper.connect", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    multiAccumuloSiteMap.put("instance.zookeeper.host", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));

    // STORM
    stormSiteMap.put("nimbus.host", new SingleHostTopologyUpdater("NIMBUS"));
    stormSiteMap.put("nimbus_hosts", new SingleHostTopologyUpdater("NIMBUS"));
    stormSiteMap.put("drpc_server_host", new SingleHostTopologyUpdater("DRPC_SERVER"));
    stormSiteMap.put("drpc.servers", new SingleHostTopologyUpdater("DRPC_SERVER"));
    stormSiteMap.put("storm_ui_server_host", new SingleHostTopologyUpdater("STORM_UI_SERVER"));
    stormSiteMap.put("worker.childopts", new OptionalSingleHostTopologyUpdater("GANGLIA_SERVER"));
    stormSiteMap.put("supervisor.childopts", new OptionalSingleHostTopologyUpdater("GANGLIA_SERVER"));
    stormSiteMap.put("nimbus.childopts", new OptionalSingleHostTopologyUpdater("GANGLIA_SERVER"));
    // Storm AMS integration
    stormSiteNonTopologyMap.put("metrics.reporter.register", new NonTopologyUpdater() {
      @Override
      public String updateForClusterCreate(String propertyName,
                                           String origValue,
                                           Map<String, Map<String, String>> properties,
                                           ClusterTopology topology) {

        if (topology.getServices().contains("AMBARI_METRICS")) {
          final String amsReporterClass = "org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter";
          if (origValue == null || origValue.isEmpty()) {
            return amsReporterClass;
          }
        }
        return origValue;
      }
    });

    multiStormSiteMap.put("supervisor_hosts",
        new YamlMultiValuePropertyDecorator(new MultipleHostTopologyUpdater("SUPERVISOR")));
    multiStormSiteMap.put("storm.zookeeper.servers",
        new YamlMultiValuePropertyDecorator(new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER")));
    multiStormSiteMap.put("nimbus.seeds",
        new YamlMultiValuePropertyDecorator(new MultipleHostTopologyUpdater("NIMBUS"), YamlMultiValuePropertyDecorator.FlowStyle.PLAIN));


    // FALCON
    falconStartupPropertiesMap.put("*.broker.url", new SingleHostTopologyUpdater("FALCON_SERVER"));

    // KAFKA
    kafkaBrokerMap.put("kafka.ganglia.metrics.host", new OptionalSingleHostTopologyUpdater("GANGLIA_SERVER"));
    // KAFKA AMS integration
    kafkaBrokerNonTopologyMap.put("kafka.metrics.reporters", new NonTopologyUpdater() {
      @Override
      public String updateForClusterCreate(String propertyName,
                                           String origValue,
                                           Map<String, Map<String, String>> properties,
                                           ClusterTopology topology) {

        if (topology.getServices().contains("AMBARI_METRICS")) {
          final String amsReportesClass = "org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter";
          if (origValue == null || origValue.isEmpty()) {
            return amsReportesClass;
          } else if (!origValue.contains(amsReportesClass)) {
            return String.format("%s,%s", origValue, amsReportesClass);
          }
        }
        return origValue;
      }
    });

    // KNOX
    multiCoreSiteMap.put("hadoop.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));
    multiWebhcatSiteMap.put("webhcat.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));
    multiOozieSiteMap.put("hadoop.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));
    multiOozieSiteMap.put("oozie.service.ProxyUserService.proxyuser.knox.hosts", new MultipleHostTopologyUpdater("KNOX_GATEWAY"));

    // ATLAS
    atlasPropsMap.put("atlas.server.bind.address", new SingleHostTopologyUpdater("ATLAS_SERVER"));
    atlasPropsMap.put("atlas.kafka.bootstrap.servers", new MultipleHostTopologyUpdater("KAFKA_BROKER"));
    atlasPropsMap.put("atlas.kafka.zookeeper.connect", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    atlasPropsMap.put("atlas.graph.index.search.solr.zookeeper-url", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER", ',', false, true, true));
    atlasPropsMap.put("atlas.graph.storage.hostname", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    atlasPropsMap.put("atlas.audit.hbase.zookeeper.quorum", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));

    // RANGER_ADMIN
    rangerAdminPropsMap.put("policymgr_external_url", new SingleHostTopologyUpdater("RANGER_ADMIN"));

    // RANGER ENV
    List<Map<String, PropertyUpdater>> configsWithRangerHdfsAuditDirProperty = ImmutableList.of(
      rangerEnvPropsMap,
      rangerYarnAuditPropsMap,
      rangerHdfsAuditPropsMap,
      rangerHbaseAuditPropsMap,
      rangerHiveAuditPropsMap,
      rangerKnoxAuditPropsMap,
      rangerKafkaAuditPropsMap,
      rangerStormAuditPropsMap,
      rangerAtlasAuditPropsMap
    );
    for (Map<String, PropertyUpdater> rangerAuditPropsMap: configsWithRangerHdfsAuditDirProperty) {
      rangerAuditPropsMap.put("xasecure.audit.destination.hdfs.dir", new OptionalSingleHostTopologyUpdater("NAMENODE"));
       // the same prop updater must be used as for fs.defaultFS in core-site
    }

    // RANGER KMS
    multiRangerKmsSiteMap.put("hadoop.kms.authentication.signer.secret.provider.zookeeper.connection.string",
      new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
    // Required due to AMBARI-4933.  These no longer seem to be required as the default values in the stack
    // are now correct but are left here in case an existing blueprint still contains an old value.
    addUnitPropertyUpdaters();

    hawqSiteMap.put("hawq_master_address_host", new SingleHostTopologyUpdater("HAWQMASTER"));
    hawqSiteMap.put("hawq_standby_address_host", new SingleHostTopologyUpdater("HAWQSTANDBY"));
    hawqSiteMap.put("hawq_dfs_url", new SingleHostTopologyUpdater("NAMENODE"));

    // AMS
    amsSiteMap.put("timeline.metrics.service.webapp.address", new SingleHostTopologyUpdater("METRICS_COLLECTOR") {
      @Override
      public String updateForClusterCreate(String propertyName, String origValue, Map<String, Map<String, String>> properties, ClusterTopology topology) {
        if (!origValue.startsWith(BIND_ALL_IP_ADDRESS)) {
          return origValue.replace(origValue.split(":")[0], BIND_ALL_IP_ADDRESS);
        } else {
          return origValue;
        }
      }
    });

    // DRUID
    druidCommon.put("metastore_hostname", HostGroupUpdater.INSTANCE);
    druidCommon.put("druid.metadata.storage.connector.connectURI", HostGroupUpdater.INSTANCE);
    druidCommon.put("druid.zk.service.host", new MultipleHostTopologyUpdater("ZOOKEEPER_SERVER"));
  }

  private static void addUnitPropertyUpdaters() {
    Map<String, List<UnitValidatedProperty>> propsPerConfigType = UnitValidatedProperty.ALL
      .stream()
      .collect(groupingBy(UnitValidatedProperty::getConfigType));
    for (String configType : propsPerConfigType.keySet()) {
      Map<String, PropertyUpdater> unitUpdaters = new HashMap<>();
      for (UnitValidatedProperty each : propsPerConfigType.get(configType)) {
        unitUpdaters.put(each.getPropertyName(), new UnitUpdater(each.getServiceName(), each.getConfigType()));
      }
      mPropertyUpdaters.put(configType, unitUpdaters);
    }
  }

  private Collection<String> setupHDFSProxyUsers(Configuration configuration, Set<String> configTypesUpdated) {
    // AMBARI-5206
    final Map<String , String> userProps = new HashMap<>();

    Collection<String> services = clusterTopology.getServices();
    if (services.contains("HDFS")) {
      // only add user properties to the map for
      // services actually included in the blueprint definition
      if (services.contains("OOZIE")) {
        userProps.put("oozie_user", "oozie-env");
      }

      if (services.contains("HIVE")) {
        userProps.put("hive_user", "hive-env");
        userProps.put("webhcat_user", "hive-env");
      }

      if (services.contains("HBASE")) {
        userProps.put("hbase_user", "hbase-env");
      }

      if (services.contains("FALCON")) {
        userProps.put("falcon_user", "falcon-env");
      }

      String proxyUserHosts = "hadoop.proxyuser.%s.hosts";
      String proxyUserGroups = "hadoop.proxyuser.%s.groups";

      Map<String, Map<String, String>> existingProperties = configuration.getFullProperties();
      for (String property : userProps.keySet()) {
        String configType = userProps.get(property);
        Map<String, String> configs = existingProperties.get(configType);
        if (configs != null) {
          String user = configs.get(property);
          if (user != null && !user.isEmpty()) {
            ensureProperty(configuration, "core-site", String.format(proxyUserHosts, user), "*", configTypesUpdated);
            ensureProperty(configuration, "core-site", String.format(proxyUserGroups, user), "*", configTypesUpdated);
          }
        } else {
          LOG.debug("setMissingConfigurations: no user configuration found for type = {}.  This may be caused by an error in the blueprint configuration.",
            configType);
        }

      }
    }
    return services;
  }

  /**
   * Adds properties from excluded config files (marked as excluded in service metainfo.xml) like Falcon related properties
   * from oozie-site.xml defined in FALCON/configuration. (AMBARI-13017)
   *
   * In case the excluded config-type related service is not present in the blueprint, excluded configs are ignored
   * @param configuration
   * @param configTypesUpdated
   * @param stack
   */
  private void addExcludedConfigProperties(Configuration configuration, Set<String> configTypesUpdated, StackDefinition stack) {
    Collection<String> blueprintServices = clusterTopology.getServices();

    LOG.debug("Handling excluded properties for blueprint services: {}", blueprintServices);

    for (String blueprintService : blueprintServices) {

      LOG.debug("Handling excluded properties for blueprint service: {}", blueprintService);
      Set<String> excludedConfigTypes = stack.getExcludedConfigurationTypes(blueprintService);

      if (excludedConfigTypes.isEmpty()) {
        LOG.debug("There are no excluded config types for blueprint service: {}", blueprintService);
        continue;
      }

      for(String configType: excludedConfigTypes) {
        LOG.debug("Handling excluded config type [{}] for blueprint service: [{}]", configType, blueprintService);

        String blueprintServiceForExcludedConfig;

        try {
          blueprintServiceForExcludedConfig = stack.getServiceForConfigType(configType);
        } catch (IllegalArgumentException illegalArgumentException) {
          LOG.warn("Error encountered while trying to obtain the service name for config type [" + configType +
            "].  Further processing on this excluded config type will be skipped.  " +
            "This usually means that a service's definitions have been manually removed from the Ambari stack definitions.  " +
            "If the stack definitions have not been changed manually, this may indicate a stack definition error in Ambari.  ", illegalArgumentException);
          // skip this type for any further processing
          continue;
        }


        if (!blueprintServices.contains(blueprintServiceForExcludedConfig)) {
          LOG.debug("Service [{}] for excluded config type [{}] is not present in the blueprint. " +
              "Ignoring excluded config entries.", blueprintServiceForExcludedConfig, configType);
          continue;
        }

        Map<String, String> configProperties = stack.getConfigurationProperties(blueprintService, configType);
        for(Map.Entry<String, String> entry: configProperties.entrySet()) {
          LOG.debug("ADD property {} {} {}", configType, entry.getKey(), entry.getValue());
          ensureProperty(configuration, configType, entry.getKey(), entry.getValue(), configTypesUpdated);
        }
      }
    }
  }

  /**
   * This method ensures that Ambari command retry is enabled if not explicitly overridden in
   * cluster-env by the Blueprint or Cluster Creation template.  The new dynamic provisioning model
   * requires that retry be enabled for most multi-node clusters, to this method sets reasonable defaults
   * in order to preserve backwards compatibility and to simplify Blueprint creation.
   *
   * If the retry-specific properties in cluster-env are not set, then the config processor
   * will set these values to defaults in cluster-env.
   *
   * @param configuration cluster configuration
   */
  private static void setRetryConfiguration(Configuration configuration, Set<String> configTypesUpdated) {
    boolean wasUpdated = false;

    if (configuration.getPropertyValue(CLUSTER_ENV_CONFIG_TYPE_NAME, COMMAND_RETRY_ENABLED_PROPERTY_NAME) == null) {
      configuration.setProperty(CLUSTER_ENV_CONFIG_TYPE_NAME, COMMAND_RETRY_ENABLED_PROPERTY_NAME, COMMAND_RETRY_ENABLED_DEFAULT);
      wasUpdated = true;
    }

    if (configuration.getPropertyValue(CLUSTER_ENV_CONFIG_TYPE_NAME, COMMANDS_TO_RETRY_PROPERTY_NAME) == null) {
      configuration.setProperty(CLUSTER_ENV_CONFIG_TYPE_NAME, COMMANDS_TO_RETRY_PROPERTY_NAME, COMMANDS_TO_RETRY_DEFAULT);
      wasUpdated = true;
    }

    if (configuration.getPropertyValue(CLUSTER_ENV_CONFIG_TYPE_NAME, COMMAND_RETRY_MAX_TIME_IN_SEC_PROPERTY_NAME) == null) {
      configuration.setProperty(CLUSTER_ENV_CONFIG_TYPE_NAME, COMMAND_RETRY_MAX_TIME_IN_SEC_PROPERTY_NAME, COMMAND_RETRY_MAX_TIME_IN_SEC_DEFAULT);
      wasUpdated = true;
    }

    if (wasUpdated) {
      configTypesUpdated.add(CLUSTER_ENV_CONFIG_TYPE_NAME);
    }
  }


  /**
   * Sets the read-only properties for stack features & tools, overriding
   * anything provided in the blueprint.
   *
   * @param configuration
   *          the configuration to update with values from the stack.
   * @param configTypesUpdated
   *          the list of configuration types updated (cluster-env will be added
   *          to this).
   */
  private void setStackToolsAndFeatures(Configuration configuration, Set<String> configTypesUpdated)
      throws ConfigurationTopologyException {
    ConfigHelper configHelper = clusterTopology.getAmbariContext().getConfigHelper();

    Set<String> properties = ImmutableSet.of(
      ConfigHelper.CLUSTER_ENV_STACK_NAME_PROPERTY,
      ConfigHelper.CLUSTER_ENV_STACK_ROOT_PROPERTY,
      ConfigHelper.CLUSTER_ENV_STACK_TOOLS_PROPERTY,
      ConfigHelper.CLUSTER_ENV_STACK_FEATURES_PROPERTY,
      ConfigHelper.CLUSTER_ENV_STACK_PACKAGES_PROPERTY
    );

    try {
      for (StackId stackId : clusterTopology.getStackIds()) {
        Map<String, Map<String, String>> defaultStackProperties = configHelper.getDefaultStackProperties(stackId);
        if (defaultStackProperties.containsKey(CLUSTER_ENV_CONFIG_TYPE_NAME)) {
          Map<String, String> clusterEnvDefaultProperties = defaultStackProperties.get(CLUSTER_ENV_CONFIG_TYPE_NAME);

          for (String property : properties) {
            if (clusterEnvDefaultProperties.containsKey(property)) {
              configuration.setProperty(CLUSTER_ENV_CONFIG_TYPE_NAME, property,
                clusterEnvDefaultProperties.get(property)
              );

              // make sure to include the configuration type as being updated
              configTypesUpdated.add(CLUSTER_ENV_CONFIG_TYPE_NAME);
            }
          }

          break;
        }
      }
    } catch (AmbariException e) {
      throw new ConfigurationTopologyException("Unable to retrieve the stack tools and features", e);
    }
  }

  /**
   * Ensure that the specified property exists.
   * If not, set a default value.
   *
   * @param configuration  configuration being processed
   * @param type           config type
   * @param property       property name
   * @param defaultValue   default value
   */
  private void ensureProperty(Configuration configuration, String type, String property, String defaultValue, Set<String> configTypesUpdated) {
    if (configuration.getPropertyValue(type, property) == null) {
      configuration.setProperty(type, property, defaultValue);
      configTypesUpdated.add(type);
    }
  }


  /**
   * Defines an interface for querying a filter to determine
   * if a given property should be included in an external
   * collection of properties.
   */
  private interface PropertyFilter {

    /**
     * Query to determine if a given property should be included in a collection of
     * properties.
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @param configType config type that contains this property
     * @param topology cluster topology instance
     * @return true if the property should be included
     *         false if the property should not be included
     */
    boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology);
  }

  /**
   * A Filter that excludes properties if the property name matches
   * a pattern of "*PASSWORD" (case-insensitive).
   *
   */
  private static class PasswordPropertyFilter implements PropertyFilter {

    private static final Pattern PASSWORD_NAME_REGEX = Pattern.compile("\\S+(PASSWORD|SECRET)", Pattern.CASE_INSENSITIVE);

    /**
     * Query to determine if a given property should be included in a collection of
     * properties.
     *
     * This implementation uses a regular expression to determine if
     * a given property name ends with "PASSWORD", using a case-insensitive match.
     * This will be used to filter out Ranger passwords that are not considered "required"
     * passwords by the stack metadata. This could potentially also
     * be useful in filtering out properties that are added to
     * stacks, but not directly marked as the PASSWORD type, even if they
     * are indeed passwords.
     *
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @param configType config type that contains this property
     * @param topology cluster topology instance
     *
     * @return true if the property should be included
     *         false if the property should not be included
     */
    @Override
    public boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology) {
      return !PASSWORD_NAME_REGEX.matcher(propertyName).matches();
    }
  }
  /**
   * A Filter that excludes properties if in stack a property is marked as password property or kerberos principal
   *
   */
  private static class StackPropertyTypeFilter implements PropertyFilter {

    /**
     * Query to determine if a given property should be included in a collection of
     * properties.
     *
     * This implementation filters property if in stack configuration is the property type is password or kerberos principal.
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @param configType config type that contains this property
     * @param topology cluster topology instance
     *
     * @return true if the property should be included
     *         false if the property should not be included
     */
    @Override
    public boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology) {
        StackDefinition stack = topology.getStack();
        final String serviceName = stack.getServiceForConfigType(configType);
        return !(stack.isPasswordProperty(serviceName, configType, propertyName) ||
                stack.isKerberosPrincipalNameProperty(serviceName, configType, propertyName));
    }
  }

  /**
   * A Filter that excludes Kerberos auth_to_local rules properties.
   */
  private static class KerberosAuthToLocalRulesFilter implements PropertyFilter {

    /**
     * Query to determine if a given property should be included in a collection of
     * properties.
     *
     * This implementation filters Kerberos auth_to_local rules properties.
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @param configType config type that contains this property
     * @param topology cluster topology instance
     *
     * @return true if the property should be included
     *         false if the property should not be included
     */
    Map<Long, Set<String>> authToLocalPerClusterMap = null;
    KerberosAuthToLocalRulesFilter (Map<Long, Set<String>> authToLocalPerClusterMap) {
      this.authToLocalPerClusterMap = authToLocalPerClusterMap;
    }
    @Override
    public boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology) {
      return (authToLocalPerClusterMap == null || authToLocalPerClusterMap.get(topology.getClusterId()) == null || !authToLocalPerClusterMap.get(topology.getClusterId()).contains(String.format("%s/%s", configType, propertyName)));
    }
  }

  /**
   * Simple filter implementation used to remove named properties from
   * a Blueprint export.  Some properties with hostname information set
   * by the UI do not have straightforward mappings to hosts, so these properties
   * cannot be exported via the default HOSTGROUP mechanism.
   */
  private static class SimplePropertyNameExportFilter implements PropertyFilter {

    private final String propertyName;

    private final String propertyConfigType;

    SimplePropertyNameExportFilter(String propertyName, String propertyConfigType) {
      this.propertyName = propertyName;
      this.propertyConfigType = propertyConfigType;
    }

    @Override
    public boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology) {
      return !(propertyConfigType.equals(configType) &&
             this.propertyName.equals(propertyName));
    }
  }


  /**
   * Filter implementation that determines if a property should be included in
   * a collection by inspecting the configuration dependencies included in the
   * stack definitions for a given property.
   *
   * The DependencyFilter is initialized with a given property that is listed
   * as a dependency of some properties in the stacks. If the dependency is found,
   * it must match a given condition (implemented in concrete subclasses) in
   * order to be included in a collection.
   */
  private static abstract class DependencyFilter implements PropertyFilter {

    private final String dependsOnPropertyName;

    private final String dependsOnConfigType;

    DependencyFilter(String dependsOnPropertyName, String dependsOnConfigType) {
      this.dependsOnPropertyName = dependsOnPropertyName;
      this.dependsOnConfigType = dependsOnConfigType;
    }


    /**
     * Inspects stack dependencies to determine if a given property
     * should be included in a collection.
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @param configType config type that contains this property
     * @param topology cluster topology instance
     *
     * @return true if the property should be included
     *         false if the property should not be included
     */
    @Override
    public boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology) {
      StackDefinition stack = topology.getStack();
      Configuration configuration = topology.getConfiguration();

      final String serviceName = stack.getServiceForConfigType(configType);
      Map<String, Stack.ConfigProperty> typeProperties =
        stack.getConfigurationPropertiesWithMetadata(serviceName, configType);

      Stack.ConfigProperty configProperty = typeProperties.get(propertyName);
      if (configProperty != null) {
        Set<PropertyDependencyInfo> dependencyInfos = configProperty.getDependsOnProperties();
        if (dependencyInfos != null) {
          // iterate over the dependencies specified for this property in the stack
          for (PropertyDependencyInfo propertyDependencyInfo : dependencyInfos) {
            if (propertyDependencyInfo.getName().equals(dependsOnPropertyName) && (propertyDependencyInfo.getType().equals(dependsOnConfigType))) {
              // this property depends upon one of the registered dependency properties
              Map<String, Map<String, String>> clusterConfig = configuration.getFullProperties();
              Map<String, String> configByType = clusterConfig.get(dependsOnConfigType);
              return isConditionSatisfied(dependsOnPropertyName, configByType.get(dependsOnPropertyName), dependsOnConfigType);
            }
          }
        }
      }

      // always include properties by default, unless a defined
      // filter is found and the condition specified by the filter
      // is not satisfied
      return true;
    }

    /**
     * Abstract method used to determine if the value of a given dependency property
     * meets a given condition.
     *
     * @param propertyName name of property
     * @param propertyValue value of property
     * @param propertyType configuration type of contains this property
     * @return  true if the condition is satisfied for this property
     *          false if the condition is not satisfied for this property
     */
    public abstract boolean isConditionSatisfied(String propertyName, String propertyValue, String propertyType);

  }

  /**
   * DependencyFilter subclass that requires that the specified
   * dependency have a specific value in order for properties that
   * depend on it to be included in a collection.
   */
  private static class DependencyEqualsFilter extends DependencyFilter {

    private final String value;

    DependencyEqualsFilter(String dependsOnPropertyName, String dependsOnConfigType, String value) {
      super(dependsOnPropertyName, dependsOnConfigType);

      this.value = value;
    }

    /**
     *
     * @param propertyName name of property
     * @param propertyValue value of property
     * @param propertyType configuration type of contains this property
     * @return true if the property is equal to the expected value
     *         false if the property does not equal the expected value
     */
    @Override
    public boolean isConditionSatisfied(String propertyName, String propertyValue, String propertyType) {
      return value.equals(propertyValue);
    }
  }

  /**
   * DependencyFilter subclass that requires that the specified
   * dependency not have the specified value in order for properties that
   * depend on it to be included in a collection.
   */
  private static class DependencyNotEqualsFilter extends DependencyFilter {

    private final String value;

    DependencyNotEqualsFilter(String dependsOnPropertyName, String dependsOnConfigType, String value) {
      super(dependsOnPropertyName, dependsOnConfigType);

      this.value = value;
    }

    /**
     *
     * @param propertyName name of property
     * @param propertyValue value of property
     * @param propertyType configuration type of contains this property
     * @return true if the property is not equal to the expected value
     *         false if the property is equal to the expected value
     *
     */
    @Override
    public boolean isConditionSatisfied(String propertyName, String propertyValue, String propertyType) {
      return !value.equals(propertyValue);
    }
  }

  /**
   * Filter implementation that scans for HDFS NameNode properties that should be
   * removed/ignored when HDFS NameNode HA is enabled.
   */
  private static class HDFSNameNodeHAFilter implements PropertyFilter {

    /**
     * Set of HDFS Property names that are only valid in a non-HA scenario.
     * In an HA setup, the property names include the names of the nameservice and
     * namenode.
     */
    private final Set<String> setOfHDFSPropertyNamesNonHA =
      ImmutableSet.of("dfs.namenode.http-address", "dfs.namenode.https-address", "dfs.namenode.rpc-address");


    /**
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @param configType config type that contains this property
     * @param topology cluster topology instance
     *
     * @return true if the property should be included
     *         false if the property should not be included
     */
    @Override
    public boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology) {
      return !setOfHDFSPropertyNamesNonHA.contains(propertyName)
        || !isNameNodeHAEnabled(topology.getConfiguration().getFullProperties());
    }
  }

  /**
   * Filter implementation filters out a property depending on property value.
   */
  private static class ConditionalPropertyFilter implements PropertyFilter {

    private final String propertyName;
    private final String propertyValue;
    private final String configType;

    public ConditionalPropertyFilter(String configType, String propertyName, String propertyValue) {
      this.propertyName = propertyName;
      this.propertyValue = propertyValue;
      this.configType = configType;
    }

    /**
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @param configType config type that contains this property
     * @param topology cluster topology instance
     *
     * @return true if the property should be included
     *         false if the property should not be included
     */
    @Override
    public boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology) {
      if (configType.equals(this.configType) && propertyName.equals(this.propertyName) && propertyValue.equals(this
        .propertyValue)) {
        return false;
      }
      return true;
    }
  }

  /**
   * Filter implementation that scans for HAWQ HA properties that should be
   * removed/ignored when HAWQ HA is not enabled.
   */
  private static class HawqHAFilter implements PropertyFilter {

    /**
     * Set of HAWQ Property names that are only valid in a HA scenario.
     */
    private final Set<String> setOfHawqPropertyNamesNonHA =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(HAWQ_SITE_HAWQ_STANDBY_ADDRESS_HOST)));


    /**
     *
     * @param propertyName property name
     * @param propertyValue property value
     * @param configType config type that contains this property
     * @param topology cluster topology instance
     *
     * @return true if the property should be included
     *         false if the property should not be included
     */
    @Override
    public boolean isPropertyIncluded(String propertyName, String propertyValue, String configType, ClusterTopology topology) {
      int matchingGroupCount = topology.getHostGroupsForComponent(HAWQSTANDBY).size();
      if (matchingGroupCount == 0) {
        if (setOfHawqPropertyNamesNonHA.contains(propertyName)) {
          return false;
        }
      }

      return true;
    }
  }


}
