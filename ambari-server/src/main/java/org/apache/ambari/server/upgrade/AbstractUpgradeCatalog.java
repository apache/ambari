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
package org.apache.ambari.server.upgrade;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class AbstractUpgradeCatalog implements UpgradeCatalog {
  @Inject
  protected DBAccessor dbAccessor;
  @Inject
  protected Configuration configuration;
  @Inject
  protected StackUpgradeUtil stackUpgradeUtil;

  protected Injector injector;

  // map and list with constants, for filtration like in stack advisor
  protected Map<String,List<String>> hiveAuthPropertyValueDependencies = new HashMap<String, List<String>>();
  protected List<String> allHiveAuthPropertyValueDependecies = new ArrayList<String>();

  /**
   * Override variable in child's if table name was changed
   */
  protected String ambariSequencesTable = "ambari_sequences";

  /**
   * The user name to use as the authenticated user when perform authenticated tasks or operations
   * that require the name of the authenticated user
   */
  protected static final String AUTHENTICATED_USER_NAME = "ambari-upgrade";

  private static final String CONFIGURATION_TYPE_HIVE_SITE = "hive-site";
  private static final String CONFIGURATION_TYPE_HDFS_SITE = "hdfs-site";

  private static final String PROPERTY_DFS_NAMESERVICES = "dfs.nameservices";
  private static final String PROPERTY_HIVE_SERVER2_AUTHENTICATION = "hive.server2.authentication";

  private static final Logger LOG = LoggerFactory.getLogger
    (AbstractUpgradeCatalog.class);
  private static final Map<String, UpgradeCatalog> upgradeCatalogMap =
    new HashMap<String, UpgradeCatalog>();

  @Inject
  public AbstractUpgradeCatalog(Injector injector) {
    this.injector = injector;
    injector.injectMembers(this);
    registerCatalog(this);

    hiveAuthPropertyValueDependencies.put("ldap", Arrays.asList("hive.server2.authentication.ldap.url",
            "hive.server2.authentication.ldap.baseDN"));
    hiveAuthPropertyValueDependencies.put("kerberos", Arrays.asList("hive.server2.authentication.kerberos.keytab",
            "hive.server2.authentication.kerberos.principal"));
    hiveAuthPropertyValueDependencies.put("pam", Arrays.asList("hive.server2.authentication.pam.services"));
    hiveAuthPropertyValueDependencies.put("custom", Arrays.asList("hive.server2.custom.authentication.class"));

    for (List<String> dependencies : hiveAuthPropertyValueDependencies.values()) {
      allHiveAuthPropertyValueDependecies.addAll(dependencies);
    }
  }

  /**
   * Every subclass needs to register itself
   */
  protected void registerCatalog(UpgradeCatalog upgradeCatalog) {
    upgradeCatalogMap.put(upgradeCatalog.getTargetVersion(), upgradeCatalog);
  }

  /**
   * Add new sequence to <code>ambariSequencesTable</code>.
   * @param seqName name of sequence to be inserted
   * @param seqDefaultValue initial value for the sequence
   * @param ignoreFailure true to ignore insert sql errors
   * @throws SQLException
   */
   protected final void addSequence(String seqName, Long seqDefaultValue, boolean ignoreFailure) throws SQLException{
     // check if sequence is already in the database
     Statement statement = null;
     ResultSet rs = null;
     try {
       statement = dbAccessor.getConnection().createStatement();
       if (statement != null) {
         rs = statement.executeQuery(String.format("SELECT COUNT(*) from %s where sequence_name='%s'", ambariSequencesTable, seqName));

         if (rs != null) {
           if (rs.next() && rs.getInt(1) == 0) {
             dbAccessor.executeQuery(String.format("INSERT INTO %s(sequence_name, sequence_value) VALUES('%s', %d)", ambariSequencesTable, seqName, seqDefaultValue), ignoreFailure);
           } else {
             LOG.warn("Sequence {} already exists, skipping", seqName);
           }
         }
       }
     } finally {
       if (rs != null) {
         rs.close();
       }
       if (statement != null) {
         statement.close();
       }
     }
  }

  /**
   * Add several new sequences to <code>ambariSequencesTable</code>.
   * @param seqNames list of sequences to be inserted
   * @param seqDefaultValue initial value for the sequence
   * @param ignoreFailure true to ignore insert sql errors
   * @throws SQLException
   *
   */
  protected final void addSequences(List<String> seqNames, Long seqDefaultValue, boolean ignoreFailure) throws SQLException{
    // ToDo: rewrite function to use one SQL call per select/insert for all items
    for (String seqName: seqNames){
      addSequence(seqName, seqDefaultValue, ignoreFailure);
    }
  }

  @Override
  public String getSourceVersion() {
    return null;
  }

  protected static UpgradeCatalog getUpgradeCatalog(String version) {
    return upgradeCatalogMap.get(version);
  }

  protected static class VersionComparator implements Comparator<UpgradeCatalog> {

    @Override
    public int compare(UpgradeCatalog upgradeCatalog1,
                       UpgradeCatalog upgradeCatalog2) {
      //make sure FinalUpgradeCatalog runs last
      if (upgradeCatalog1.isFinal() ^ upgradeCatalog2.isFinal()) {
        return Boolean.compare(upgradeCatalog1.isFinal(), upgradeCatalog2.isFinal());
      }

      return VersionUtils.compareVersions(upgradeCatalog1.getTargetVersion(),
        upgradeCatalog2.getTargetVersion(), 3);
    }
  }

  /**
   * Update metainfo to new version.
   */
  @Transactional
  public int updateMetaInfoVersion(String version) {
    int rows = 0;
    if (version != null) {
      MetainfoDAO metainfoDAO = injector.getInstance(MetainfoDAO.class);

      MetainfoEntity versionEntity = metainfoDAO.findByKey("version");

      if (versionEntity != null) {
        versionEntity.setMetainfoValue(version);
        metainfoDAO.merge(versionEntity);
      } else {
        versionEntity = new MetainfoEntity();
        versionEntity.setMetainfoName("version");
        versionEntity.setMetainfoValue(version);
        metainfoDAO.create(versionEntity);
      }

    }

    return rows;
  }

  protected Provider<EntityManager> getEntityManagerProvider() {
    return injector.getProvider(EntityManager.class);
  }

  protected void executeInTransaction(Runnable func) {
    EntityManager entityManager = getEntityManagerProvider().get();
    if (entityManager.getTransaction().isActive()) { //already started, reuse
      func.run();
    } else {
      entityManager.getTransaction().begin();
      try {
        func.run();
        entityManager.getTransaction().commit();
      } catch (Exception e) {
        LOG.error("Error in transaction ", e);
        if (entityManager.getTransaction().isActive()) {
          entityManager.getTransaction().rollback();
        }
        throw new RuntimeException(e);
      }

    }
  }

  protected void changePostgresSearchPath() throws SQLException {
    String dbUser = configuration.getDatabaseUser();
    String schemaName = configuration.getServerJDBCPostgresSchemaName();

    if (null != dbUser && !dbUser.equals("") && null != schemaName && !schemaName.equals("")) {
      // Wrap username with double quotes to accept old username "ambari-server"
      if (!dbUser.contains("\"")) {
        dbUser = String.format("\"%s\"", dbUser);
      }

      dbAccessor.executeQuery(String.format("ALTER SCHEMA %s OWNER TO %s;", schemaName, dbUser));
      dbAccessor.executeQuery(String.format("ALTER ROLE %s SET search_path to '%s';", dbUser, schemaName));
    }
  }

  public void addNewConfigurationsFromXml() throws AmbariException {
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);

    Clusters clusters = controller.getClusters();
    if (clusters == null) {
      return;
    }
    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (Cluster cluster : clusterMap.values()) {
        Map<String, Set<String>> newProperties = new HashMap<String, Set<String>>();

        Set<PropertyInfo> stackProperties = configHelper.getStackProperties(cluster);
        for(String serviceName: cluster.getServices().keySet()) {
          Set<PropertyInfo> properties = configHelper.getServiceProperties(cluster, serviceName);

          if(properties == null) {
            continue;
          }
          properties.addAll(stackProperties);

          for(PropertyInfo property:properties) {
            String configType = ConfigHelper.fileNameToConfigType(property.getFilename());
            Config clusterConfigs = cluster.getDesiredConfigByType(configType);
            if(clusterConfigs == null || !clusterConfigs.getProperties().containsKey(property.getName())) {
              if (!checkAccordingToStackAdvisor(property, cluster)) {
                continue;
              }

              LOG.info("Config " + property.getName() + " from " + configType + " from xml configurations" +
                  " is not found on the cluster. Adding it...");

              if(!newProperties.containsKey(configType)) {
                newProperties.put(configType, new HashSet<String>());
              }
              newProperties.get(configType).add(property.getName());
            }
          }
        }



        for (Entry<String, Set<String>> newProperty : newProperties.entrySet()) {
          updateConfigurationPropertiesWithValuesFromXml(newProperty.getKey(), newProperty.getValue(), false, true);
        }
      }
    }
  }

  protected boolean checkAccordingToStackAdvisor(PropertyInfo property, Cluster cluster) {
    if (allHiveAuthPropertyValueDependecies.contains(property.getName())) {
      Config hiveSite = cluster.getDesiredConfigByType(CONFIGURATION_TYPE_HIVE_SITE);
      if (hiveSite != null) {
        String hiveAuthValue = hiveSite.getProperties().get(PROPERTY_HIVE_SERVER2_AUTHENTICATION);
        if (hiveAuthValue != null) {
          List<String> dependencies = hiveAuthPropertyValueDependencies.get(hiveAuthValue.toLowerCase());
          if (dependencies != null) {
            return dependencies.contains(property.getName());
          }
        }
      }
      return false;
    }
    return true;
  }

  protected boolean isNNHAEnabled(Cluster cluster) {
    Config hdfsSiteConfig = cluster.getDesiredConfigByType(CONFIGURATION_TYPE_HDFS_SITE);
    if (hdfsSiteConfig != null) {
      Map<String, String> properties = hdfsSiteConfig.getProperties();
      String nameServices = properties.get(PROPERTY_DFS_NAMESERVICES);
      if (!StringUtils.isEmpty(nameServices)) {
        String namenodes = properties.get(String.format("dfs.ha.namenodes.%s", nameServices));
        if (!StringUtils.isEmpty(namenodes)) {
          return (namenodes.split(",").length > 1);
        }
      }
    }
    return false;
  }

  /**
   * Create a new cluster scoped configuration with the new properties added
   * with the values from the coresponding xml files.
   *
   * If xml owner service is not in the cluster, the configuration won't be added.
   *
   * @param configType Configuration type. (hdfs-site, etc.)
   * @param propertyNames Set property names.
   */
  protected void updateConfigurationPropertiesWithValuesFromXml(String configType,
      Set<String> propertyNames, boolean updateIfExists, boolean createNewConfigType) throws AmbariException {
    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);

    Clusters clusters = controller.getClusters();
    if (clusters == null) {
      return;
    }
    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (Cluster cluster : clusterMap.values()) {
        Map<String, String> properties = new HashMap<String, String>();

        for(String propertyName:propertyNames) {
          String propertyValue = configHelper.getPropertyValueFromStackDefinitions(cluster, configType, propertyName);

          if(propertyValue == null) {
            LOG.info("Config " + propertyName + " from " + configType + " is not found in xml definitions." +
                "Skipping configuration property update");
            continue;
          }

          ServiceInfo propertyService = configHelper.getPropertyOwnerService(cluster, configType, propertyName);
          if(propertyService != null && !cluster.getServices().containsKey(propertyService.getName())) {
            LOG.info("Config " + propertyName + " from " + configType + " with value = " + propertyValue + " " +
                "Is not added due to service " + propertyService.getName() + " is not in the cluster.");
            continue;
          }

          properties.put(propertyName, propertyValue);
        }

        updateConfigurationPropertiesForCluster(cluster, configType,
            properties, updateIfExists, createNewConfigType);
      }
    }
  }

  /**
   * Update properties for the cluster
   * @param cluster cluster object
   * @param configType config to be updated
   * @param properties properties to be added or updated. Couldn't be <code>null</code>, but could be empty.
   * @param removePropertiesList properties to be removed. Could be <code>null</code>
   * @param updateIfExists
   * @param createNewConfigType
   * @throws AmbariException
   */
  protected void updateConfigurationPropertiesForCluster(Cluster cluster, String configType,
        Map<String, String> properties, Set<String> removePropertiesList, boolean updateIfExists,
        boolean createNewConfigType) throws AmbariException {
    AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);
    String newTag = "version" + System.currentTimeMillis();

    if (properties != null) {
      Map<String, Config> all = cluster.getConfigsByType(configType);
      if (all == null || !all.containsKey(newTag) || properties.size() > 0) {
        Map<String, String> oldConfigProperties;
        Config oldConfig = cluster.getDesiredConfigByType(configType);

        if (oldConfig == null && !createNewConfigType) {
          LOG.info("Config " + configType + " not found. Assuming service not installed. " +
              "Skipping configuration properties update");
          return;
        } else if (oldConfig == null) {
          oldConfigProperties = new HashMap<String, String>();
          newTag = "version1";
        } else {
          oldConfigProperties = oldConfig.getProperties();
        }

        Map<String, String> mergedProperties =
          mergeProperties(oldConfigProperties, properties, updateIfExists);

        if (removePropertiesList != null) {
          mergedProperties = removeProperties(mergedProperties, removePropertiesList);
        }

        if (!Maps.difference(oldConfigProperties, mergedProperties).areEqual()) {
          LOG.info("Applying configuration with tag '{}' to " +
            "cluster '{}'", newTag, cluster.getClusterName());

          ConfigurationRequest cr = new ConfigurationRequest();
          cr.setClusterName(cluster.getClusterName());
          cr.setVersionTag(newTag);
          cr.setType(configType);
          cr.setProperties(mergedProperties);
          controller.createConfiguration(cr);

          Config baseConfig = cluster.getConfig(cr.getType(), cr.getVersionTag());
          if (baseConfig != null) {
            String authName = AUTHENTICATED_USER_NAME;

            if (cluster.addDesiredConfig(authName, Collections.singleton(baseConfig)) != null) {
              String oldConfigString = (oldConfig != null) ? " from='" + oldConfig.getTag() + "'" : "";
              LOG.info("cluster '" + cluster.getClusterName() + "' "
                + "changed by: '" + authName + "'; "
                + "type='" + baseConfig.getType() + "' "
                + "tag='" + baseConfig.getTag() + "'"
                + oldConfigString);
            }
          }
        } else {
          LOG.info("No changes detected to config " + configType + ". Skipping configuration properties update");
        }
      }
    }
  }

  protected void updateConfigurationPropertiesForCluster(Cluster cluster, String configType,
        Map<String, String> properties, boolean updateIfExists, boolean createNewConfigType) throws AmbariException {
    updateConfigurationPropertiesForCluster(cluster, configType, properties, null, updateIfExists, createNewConfigType);
  }

  /**
   * Create a new cluster scoped configuration with the new properties added
   * to the existing set of properties.
   * @param configType Configuration type. (hdfs-site, etc.)
   * @param properties Map of key value pairs to add / update.
   */
  protected void updateConfigurationProperties(String configType,
        Map<String, String> properties, boolean updateIfExists, boolean createNewConfigType) throws
    AmbariException {
    AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);

    Clusters clusters = controller.getClusters();
    if (clusters == null) {
      return;
    }
    Map<String, Cluster> clusterMap = clusters.getClusters();

    if (clusterMap != null && !clusterMap.isEmpty()) {
      for (Cluster cluster : clusterMap.values()) {
        updateConfigurationPropertiesForCluster(cluster, configType,
            properties, updateIfExists, createNewConfigType);
      }
    }
  }

  private Map<String, String> mergeProperties(Map<String, String> originalProperties,
                               Map<String, String> newProperties,
                               boolean updateIfExists) {

    Map<String, String> properties = new HashMap<String, String>(originalProperties);
    for (Map.Entry<String, String> entry : newProperties.entrySet()) {
      if (!properties.containsKey(entry.getKey()) || updateIfExists) {
        properties.put(entry.getKey(), entry.getValue());
      }
    }
    return properties;
  }

  private Map<String, String> removeProperties(Map<String, String> originalProperties, Set<String> removeList){
    Map<String, String> properties = new HashMap<String, String>();
    properties.putAll(originalProperties);
    for (String removeProperty: removeList){
      if (originalProperties.containsKey(removeProperty)){
        properties.remove(removeProperty);
      }
    }
    return properties;
  }

  @Override
  public void upgradeSchema() throws AmbariException, SQLException {
    DatabaseType databaseType = configuration.getDatabaseType();

    if (databaseType == DatabaseType.POSTGRES) {
      changePostgresSearchPath();
    }

    executeDDLUpdates();
  }

  @Override
  public void preUpgradeData() throws AmbariException, SQLException {
    executePreDMLUpdates();
  }

  @Override
  public void upgradeData() throws AmbariException, SQLException {
    executeDMLUpdates();
    updateMetaInfoVersion(getTargetVersion());
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  protected abstract void executeDDLUpdates() throws AmbariException, SQLException;

  /**
   * Perform data insertion before running normal upgrade of data, requires started persist service
   * @throws AmbariException
   * @throws SQLException
   */
  protected abstract void executePreDMLUpdates() throws AmbariException, SQLException;

  protected abstract void executeDMLUpdates() throws AmbariException, SQLException;

  @Override
  public String toString() {
    return "{ upgradeCatalog: sourceVersion = " + getSourceVersion() + ", " +
      "targetVersion = " + getTargetVersion() + " }";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onPostUpgrade() throws AmbariException, SQLException {
    // NOOP
  }
}
