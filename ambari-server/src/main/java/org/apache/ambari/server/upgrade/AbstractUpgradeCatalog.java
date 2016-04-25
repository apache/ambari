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

import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.configuration.Configuration.DatabaseType;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.kerberos.AbstractKerberosDescriptorContainer;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

public abstract class AbstractUpgradeCatalog implements UpgradeCatalog {
  @Inject
  protected DBAccessor dbAccessor;
  @Inject
  protected Configuration configuration;
  @Inject
  protected StackUpgradeUtil stackUpgradeUtil;

  protected Injector injector;

  // map and list with constants, for filtration like in stack advisor

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
  public static final String CONFIGURATION_TYPE_RANGER_HBASE_PLUGIN_PROPERTIES = "ranger-hbase-plugin-properties";
  public static final String CONFIGURATION_TYPE_RANGER_KNOX_PLUGIN_PROPERTIES = "ranger-knox-plugin-properties";

  private static final String PROPERTY_DFS_NAMESERVICES = "dfs.nameservices";
  private static final String PROPERTY_HIVE_SERVER2_AUTHENTICATION = "hive.server2.authentication";
  public static final String PROPERTY_RANGER_HBASE_PLUGIN_ENABLED = "ranger-hbase-plugin-enabled";
  public static final String PROPERTY_RANGER_KNOX_PLUGIN_ENABLED = "ranger-knox-plugin-enabled";

  private static final Logger LOG = LoggerFactory.getLogger
    (AbstractUpgradeCatalog.class);
  private static final Map<String, UpgradeCatalog> upgradeCatalogMap =
    new HashMap<String, UpgradeCatalog>();

  @Inject
  public AbstractUpgradeCatalog(Injector injector) {
    this.injector = injector;
    injector.injectMembers(this);
    registerCatalog(this);
  }

  protected AbstractUpgradeCatalog() {
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

  protected static Document convertStringToDocument(String xmlStr) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    Document doc = null;

    try
    {
      builder = factory.newDocumentBuilder();
      doc = builder.parse( new InputSource( new StringReader( xmlStr ) ) );
    } catch (Exception e) {
      LOG.error("Error during convertation from String \"" + xmlStr + "\" to Xml!", e);
    }
    return doc;
  }

  protected static boolean isConfigEnabled(Cluster cluster, String configType, String propertyName) {
    boolean isRangerPluginEnabled = false;
    if (cluster != null) {
      Config rangerPluginProperties = cluster.getDesiredConfigByType(configType);
      if (rangerPluginProperties != null) {
        String rangerPluginEnabled = rangerPluginProperties.getProperties().get(propertyName);
        if (StringUtils.isNotEmpty(rangerPluginEnabled)) {
          isRangerPluginEnabled =  "yes".equalsIgnoreCase(rangerPluginEnabled);
        }
      }
    }
    return isRangerPluginEnabled;
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
        upgradeCatalog2.getTargetVersion(), 4);
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
        // This is required because some of the  entities actively managed by
        // the persistence context will remain unaware of the actual changes
        // occurring at the database level. Some UpgradeCatalogs perform
        // update / delete using CriteriaBuilder directly.
        entityManager.getEntityManagerFactory().getCache().evictAll();
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
              if (property.getValue() == null || property.getPropertyTypes().contains(PropertyInfo.PropertyType.DONT_ADD_ON_UPGRADE)) {
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
   * This method returns Map of clusters.
   * Map can be empty or with some objects, but never be null.
   */
  protected Map<String, Cluster> getCheckedClusterMap(Clusters clusters) {
    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null) {
        return clusterMap;
      }
    }
    return new HashMap<>();
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
          if (oldConfig != null) {
            cr.setPropertiesAttributes(oldConfig.getPropertiesAttributes());
          }
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
   * Remove properties from the cluster
   * @param cluster cluster object
   * @param configType config to be updated
   * @param removePropertiesList properties to be removed. Could be <code>null</code>
   * @throws AmbariException
   */
  protected void removeConfigurationPropertiesFromCluster(Cluster cluster, String configType, Set<String> removePropertiesList)
      throws AmbariException {

    updateConfigurationPropertiesForCluster(cluster, configType, new HashMap<String, String>(), removePropertiesList, false, true);
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

  /**
   * Iterates through a collection of AbstractKerberosDescriptorContainers to find and update
   * identity descriptor references.
   *
   * @param descriptorMap    a String to AbstractKerberosDescriptorContainer map to iterate trough
   * @param referenceName    the reference name to change
   * @param newReferenceName the new reference name
   */
  protected void updateKerberosDescriptorIdentityReferences(Map<String, ? extends AbstractKerberosDescriptorContainer> descriptorMap,
                                                          String referenceName,
                                                          String newReferenceName) {
    if (descriptorMap != null) {
      for (AbstractKerberosDescriptorContainer kerberosServiceDescriptor : descriptorMap.values()) {
        updateKerberosDescriptorIdentityReferences(kerberosServiceDescriptor, referenceName, newReferenceName);

        if (kerberosServiceDescriptor instanceof KerberosServiceDescriptor) {
          updateKerberosDescriptorIdentityReferences(((KerberosServiceDescriptor) kerberosServiceDescriptor).getComponents(),
              referenceName, newReferenceName);
        }
      }
    }
  }

  /**
   * Given an AbstractKerberosDescriptorContainer, iterates through its contained identity descriptors
   * to find ones matching the reference name to change.
   * <p/>
   * If found, the reference name is updated to the new name.
   *
   * @param descriptorContainer the AbstractKerberosDescriptorContainer to update
   * @param referenceName       the reference name to change
   * @param newReferenceName    the new reference name
   */
  protected void updateKerberosDescriptorIdentityReferences(AbstractKerberosDescriptorContainer descriptorContainer,
                                                          String referenceName,
                                                          String newReferenceName) {
    if (descriptorContainer != null) {
      KerberosIdentityDescriptor identity = descriptorContainer.getIdentity(referenceName);
      if (identity != null) {
        identity.setName(newReferenceName);
      }
    }
  }

  /**
   * Update the stored Kerberos Descriptor artifacts to conform to the new structure.
   * <p/>
   * Finds the relevant artifact entities and iterates through them to process each independently.
   */
  protected void updateKerberosDescriptorArtifacts() throws AmbariException {
    ArtifactDAO artifactDAO = injector.getInstance(ArtifactDAO.class);
    List<ArtifactEntity> artifactEntities = artifactDAO.findByName("kerberos_descriptor");

    if (artifactEntities != null) {
      for (ArtifactEntity artifactEntity : artifactEntities) {
        updateKerberosDescriptorArtifact(artifactDAO, artifactEntity);
      }
    }
  }



  /**
   * Update the specified Kerberos Descriptor artifact to conform to the new structure.
   * <p/>
   * On ambari version update some of identities can be moved between scopes(e.g. from service to component), so
   * old identity need to be moved to proper place and all references for moved identity need to be updated.
   * <p/>
   * By default descriptor remains unchanged and this method must be overridden in child UpgradeCatalog to meet new
   * ambari version changes in kerberos descriptors.
   * <p/>
   * The supplied ArtifactEntity is updated in place a merged back into the database.
   *
   * @param artifactDAO    the ArtifactDAO to use to store the updated ArtifactEntity
   * @param artifactEntity the ArtifactEntity to update
   */
  protected void updateKerberosDescriptorArtifact(ArtifactDAO artifactDAO, ArtifactEntity artifactEntity) throws AmbariException {
    // NOOP
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
  }

  @Override
  public final void updateDatabaseSchemaVersion() {
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
