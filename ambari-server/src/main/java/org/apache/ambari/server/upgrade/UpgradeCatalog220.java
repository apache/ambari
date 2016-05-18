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

import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessor.DBColumnInfo;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.ArtifactDAO;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.DaoUtils;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.ArtifactEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.alert.SourceType;
import org.apache.ambari.server.state.kerberos.KerberosComponentDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptor;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

/**
 * Upgrade catalog for version 2.2.0.
 */
public class UpgradeCatalog220 extends AbstractUpgradeCatalog {

  private static final String UPGRADE_TABLE = "upgrade";
  private static final String STORM_SITE = "storm-site";
  private static final String HDFS_SITE_CONFIG = "hdfs-site";
  private static final String TOPOLOGY_CONFIG = "topology";
  private static final String KAFKA_BROKER = "kafka-broker";
  private static final String KAFKA_ENV_CONFIG = "kafka-env";
  private static final String KAFKA_ENV_CONTENT_KERBEROS_PARAMS =
    "export KAFKA_KERBEROS_PARAMS={{kafka_kerberos_params}}";
  private static final String AMS_ENV = "ams-env";
  private static final String AMS_HBASE_ENV = "ams-hbase-env";
  private static final String AMS_SITE = "ams-site";
  private static final String AMS_HBASE_SITE = "ams-hbase-site";
  private static final String AMS_HBASE_SITE_ZK_TIMEOUT_PROPERTY =
    "zookeeper.session.timeout.localHBaseCluster";
  private static final String AMS_HBASE_SITE_NORMALIZER_ENABLED_PROPERTY = "hbase.normalizer.enabled";
  private static final String AMS_HBASE_SITE_NORMALIZER_PERIOD_PROPERTY = "hbase.normalizer.period";
  private static final String AMS_HBASE_SITE_NORMALIZER_CLASS_PROPERTY = "hbase.master.normalizer.class";
  private static final String TIMELINE_METRICS_HBASE_FIFO_COMPACTION_ENABLED = "timeline.metrics.hbase.fifo.compaction.enabled";
  private static final String HBASE_ENV_CONFIG = "hbase-env";
  private static final String FLUME_ENV_CONFIG = "flume-env";
  private static final String HIVE_SITE_CONFIG = "hive-site";
  private static final String HIVE_ENV_CONFIG = "hive-env";
  private static final String RANGER_ENV_CONFIG = "ranger-env";
  private static final String RANGER_UGSYNC_SITE_CONFIG = "ranger-ugsync-site";
  private static final String ZOOKEEPER_LOG4J_CONFIG = "zookeeper-log4j";
  private static final String NIMBS_MONITOR_FREQ_SECS_PROPERTY = "nimbus.monitor.freq.secs";
  private static final String STORM_METRICS_REPORTER = "metrics.reporter.register";
  private static final String HIVE_SERVER2_OPERATION_LOG_LOCATION_PROPERTY = "hive.server2.logging.operation.log.location";
  private static final String HADOOP_ENV_CONFIG = "hadoop-env";
  private static final String CONTENT_PROPERTY = "content";
  private static final String HADOOP_ENV_CONTENT_TO_APPEND = "\n{% if is_datanode_max_locked_memory_set %}\n" +
    "# Fix temporary bug, when ulimit from conf files is not picked up, without full relogin. \n" +
    "# Makes sense to fix only when runing DN as root \n" +
    "if [ \"$command\" == \"datanode\" ] && [ \"$EUID\" -eq 0 ] && [ -n \"$HADOOP_SECURE_DN_USER\" ]; then\n" +
    "  ulimit -l {{datanode_max_locked_memory}}\n" +
    "fi\n" +
    "{% endif %}\n";

  private static final String DOWNGRADE_ALLOWED_COLUMN = "downgrade_allowed";
  private static final String UPGRADE_SKIP_FAILURE_COLUMN = "skip_failures";
  private static final String UPGRADE_SKIP_SC_FAILURE_COLUMN = "skip_sc_failures";
  public static final String UPGRADE_PACKAGE_COL = "upgrade_package";
  public static final String UPGRADE_TYPE_COL = "upgrade_type";
  public static final String REPO_VERSION_TABLE = "repo_version";

  private static final String HOST_ROLE_COMMAND_TABLE = "host_role_command";
  private static final String HOST_ID_COL = "host_id";

  private static final String KERBEROS_DESCRIPTOR_TABLE = "kerberos_descriptor";
  private static final String KERBEROS_DESCRIPTOR_NAME_COLUMN = "kerberos_descriptor_name";
  private static final String KERBEROS_DESCRIPTOR_COLUMN = "kerberos_descriptor";
  private static final String RANGER_HDFS_PLUGIN_ENABLED_PROPERTY = "ranger-hdfs-plugin-enabled";
  private static final String RANGER_HIVE_PLUGIN_ENABLED_PROPERTY = "ranger-hive-plugin-enabled";
  private static final String RANGER_HBASE_PLUGIN_ENABLED_PROPERTY = "ranger-hbase-plugin-enabled";
  private static final String RANGER_STORM_PLUGIN_ENABLED_PROPERTY = "ranger-storm-plugin-enabled";
  private static final String RANGER_KNOX_PLUGIN_ENABLED_PROPERTY = "ranger-knox-plugin-enabled";
  private static final String RANGER_YARN_PLUGIN_ENABLED_PROPERTY = "ranger-yarn-plugin-enabled";
  private static final String RANGER_KAFKA_PLUGIN_ENABLED_PROPERTY = "ranger-kafka-plugin-enabled";

  private static final String RANGER_USERSYNC_SOURCE_IMPL_CLASS_PROPERTY = "ranger.usersync.source.impl.class";

  private static final String BLUEPRINT_TABLE = "blueprint";
  private static final String SECURITY_TYPE_COLUMN = "security_type";
  private static final String SECURITY_DESCRIPTOR_REF_COLUMN = "security_descriptor_reference";

  private static final String STAGE_TABLE = "stage";

  private static final String KNOX_SERVICE = "KNOX";

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCatalog220.class);

  private static final String OOZIE_SITE_CONFIG = "oozie-site";
  private static final String OOZIE_SERVICE_HADOOP_CONFIGURATIONS_PROPERTY_NAME = "oozie.service.HadoopAccessorService.hadoop.configurations";
  private static final String OLD_DEFAULT_HADOOP_CONFIG_PATH = "/etc/hadoop/conf";
  private static final String NEW_DEFAULT_HADOOP_CONFIG_PATH = "{{hadoop_conf_dir}}";

  @Inject
  DaoUtils daoUtils;

  @Inject
  private RepositoryVersionDAO repositoryVersionDAO;

  @Inject
  private ClusterDAO clusterDAO;

  // ----- Constructors ------------------------------------------------------

  /**
   * Don't forget to register new UpgradeCatalogs in {@link org.apache.ambari.server.upgrade.SchemaUpgradeHelper.UpgradeHelperModule#configure()}
   *
   * @param injector Guice injector to track dependencies and uses bindings to inject them.
   */
  @Inject
  public UpgradeCatalog220(Injector injector) {
    super(injector);
    this.injector = injector;
  }

  // ----- UpgradeCatalog ----------------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTargetVersion() {
    return "2.2.0";
  }

  // ----- AbstractUpgradeCatalog --------------------------------------------

  /**
   * {@inheritDoc}
   */
  @Override
  public String getSourceVersion() {
    return "2.1.2.1";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executeDDLUpdates() throws AmbariException, SQLException {
    executeUpgradeDDLUpdates();

    // Alter the host_role_command table to allow host_id to be nullable
    dbAccessor.alterColumn(HOST_ROLE_COMMAND_TABLE, new DBColumnInfo(HOST_ID_COL, Long.class, null, null, true));

    addKerberosDescriptorTable();
    executeBlueprintDDLUpdates();
    executeStageDDLUpdates();
  }

  protected void executeUpgradeDDLUpdates() throws AmbariException, SQLException {
    updateUpgradesDDL();
  }

  private void addKerberosDescriptorTable() throws SQLException {
    List<DBAccessor.DBColumnInfo> columns = new ArrayList<DBAccessor.DBColumnInfo>();
    columns.add(new DBAccessor.DBColumnInfo(KERBEROS_DESCRIPTOR_NAME_COLUMN, String.class, 255, null, false));
    columns.add(new DBAccessor.DBColumnInfo(KERBEROS_DESCRIPTOR_COLUMN, char[].class, null, null, false));

    LOG.debug("Creating table [ {} ] with columns [ {} ] and primary key: [ {} ]", KERBEROS_DESCRIPTOR_TABLE, columns, KERBEROS_DESCRIPTOR_NAME_COLUMN);
    dbAccessor.createTable(KERBEROS_DESCRIPTOR_TABLE, columns, KERBEROS_DESCRIPTOR_NAME_COLUMN);
  }

  private void executeBlueprintDDLUpdates() throws AmbariException, SQLException {
    dbAccessor.addColumn(BLUEPRINT_TABLE, new DBAccessor.DBColumnInfo(SECURITY_TYPE_COLUMN,
      String.class, 32, "NONE", false));
    dbAccessor.addColumn(BLUEPRINT_TABLE, new DBAccessor.DBColumnInfo(SECURITY_DESCRIPTOR_REF_COLUMN,
      String.class, null, null, true));
  }

  /**
   * Updates the {@code stage} table by:
   * <ul>
   * <li>Adding the {@code supports_auto_skip_failure} column</li>
   * </ul>
   *
   * @throws SQLException
   */
  protected void executeStageDDLUpdates() throws SQLException {
    dbAccessor.addColumn(STAGE_TABLE,
      new DBAccessor.DBColumnInfo("supports_auto_skip_failure", Integer.class, 1, 0, false));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void executePreDMLUpdates() throws AmbariException, SQLException {
    // execute DDL updates
    executeStackUpgradeDDLUpdates();

    // DDL and DML mixed code, double check here
    bootstrapRepoVersionForHDP21();

    // execute DML updates, no DDL things after this line
    executeUpgradePreDMLUpdates();
  }

  /**
   * Updates the following columns on the {@value #UPGRADE_TABLE} table to
   * default values:
   * <ul>
   * <li>{value {@link #DOWNGRADE_ALLOWED_COLUMN}}</li>
   * <li>{value {@link #UPGRADE_SKIP_FAILURE_COLUMN}}</li>
   * <li>{value {@link #UPGRADE_SKIP_SC_FAILURE_COLUMN}}</li>
   * </ul>
   *
   * @throws AmbariException
   * @throws SQLException
   */
  protected void executeUpgradePreDMLUpdates() throws AmbariException, SQLException {
    UpgradeDAO upgradeDAO = injector.getInstance(UpgradeDAO.class);
    List<UpgradeEntity> upgrades = upgradeDAO.findAll();
    for (UpgradeEntity upgrade: upgrades){
      if (upgrade.isDowngradeAllowed() == null) {
        upgrade.setDowngradeAllowed(true);
      }

      // ensure that these are set to false for existing upgrades
      upgrade.setAutoSkipComponentFailures(false);
      upgrade.setAutoSkipServiceCheckFailures(false);

      // apply changes
      upgradeDAO.merge(upgrade);

      LOG.info(String.format("Updated upgrade id %s, upgrade pack %s from version %s to %s",
        upgrade.getId(), upgrade.getUpgradePackage(), upgrade.getFromVersion(),
        upgrade.getToVersion()));
    }

    // make the columns nullable now that they have defaults
    dbAccessor.setColumnNullable(UPGRADE_TABLE, DOWNGRADE_ALLOWED_COLUMN, false);
    dbAccessor.setColumnNullable(UPGRADE_TABLE, UPGRADE_SKIP_FAILURE_COLUMN, false);
    dbAccessor.setColumnNullable(UPGRADE_TABLE, UPGRADE_SKIP_SC_FAILURE_COLUMN, false);
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    addNewConfigurationsFromXml();
    updateAlertDefinitions();
    updateStormConfigs();
    updateAMSConfigs();
    updateHDFSConfigs();
    updateHbaseEnvConfig();
    updateFlumeEnvConfig();
    updateHadoopEnv();
    updateKafkaConfigs();
    updateRangerEnvConfig();
    updateRangerUgsyncSiteConfig();
    updateZookeeperLog4j();
    updateHiveConfig();
    updateAccumuloConfigs();
    updateKerberosDescriptorArtifacts();
    updateKnoxTopology();
  }

  protected void updateKnoxTopology() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config topology = cluster.getDesiredConfigByType(TOPOLOGY_CONFIG);
      if (topology != null) {
        String content = topology.getProperties().get(CONTENT_PROPERTY);
        if (content != null) {
          Document topologyXml = convertStringToDocument(content);
          if (topologyXml != null) {
            Element root = topologyXml.getDocumentElement();
            if (root != null)  {
              NodeList providerNodes = root.getElementsByTagName("provider");
              boolean authorizationProviderExists = false;
              try {
                for (int i = 0; i < providerNodes.getLength(); i++) {
                  Node providerNode = providerNodes.item(i);
                  NodeList childNodes = providerNode.getChildNodes();
                  for (int k = 0; k < childNodes.getLength(); k++) {
                    Node child = childNodes.item(k);
                    child.normalize();
                    String childTextContent = child.getTextContent();
                    if (childTextContent != null && childTextContent.toLowerCase().equals("authorization")) {
                      authorizationProviderExists = true;
                      break;
                    }
                  }
                  if (authorizationProviderExists) {
                    break;
                  }
                }
              } catch(Exception e) {
                e.printStackTrace();
                LOG.error("Error occurred during check 'authorization' provider already exists in topology." + e);
                return;
              }
              if (!authorizationProviderExists) {
                NodeList nodeList = root.getElementsByTagName("gateway");
                if (nodeList != null && nodeList.getLength() > 0) {
                  boolean rangerPluginEnabled = isConfigEnabled(cluster,
                    AbstractUpgradeCatalog.CONFIGURATION_TYPE_RANGER_KNOX_PLUGIN_PROPERTIES,
                    AbstractUpgradeCatalog.PROPERTY_RANGER_KNOX_PLUGIN_ENABLED);

                  Node gatewayNode = nodeList.item(0);
                  Element newProvider = topologyXml.createElement("provider");

                  Element role = topologyXml.createElement("role");
                  role.appendChild(topologyXml.createTextNode("authorization"));
                  newProvider.appendChild(role);

                  Element name = topologyXml.createElement("name");
                  if (rangerPluginEnabled) {
                    name.appendChild(topologyXml.createTextNode("XASecurePDPKnox"));
                  } else {
                    name.appendChild(topologyXml.createTextNode("AclsAuthz"));
                  }
                  newProvider.appendChild(name);

                  Element enabled = topologyXml.createElement("enabled");
                  enabled.appendChild(topologyXml.createTextNode("true"));
                  newProvider.appendChild(enabled);


                  gatewayNode.appendChild(newProvider);

                  DOMSource topologyDomSource = new DOMSource(root);
                  StringWriter writer = new StringWriter();
                  try {
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "5");
                    transformer.transform(topologyDomSource, new StreamResult(writer));
                  } catch (TransformerConfigurationException e) {
                    e.printStackTrace();
                    LOG.error("Unable to create transformer instance, to convert Document(XML) to String. " + e);
                    return;
                  } catch (TransformerException e) {
                    e.printStackTrace();
                    LOG.error("Unable to transform Document(XML) to StringWriter. " + e);
                    return;
                  }

                  content = writer.toString();
                  Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, content);
                  updateConfigurationPropertiesForCluster(cluster, TOPOLOGY_CONFIG, updates, true, false);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Move the upgrade_package column from the repo_version table to the upgrade table as follows,
   * add column upgrade_package to upgrade table as String 255 and nullable
   * populate column in the upgrade table
   * drop the column in the repo_version table
   * make the column in the upgrade table non-nullable.
   * This has to be called as part of DML and not DDL since the persistence service has to be started.
   * @throws AmbariException
   * @throws SQLException
   */
  @Transactional
  protected void executeStackUpgradeDDLUpdates() throws SQLException, AmbariException {
    final Configuration.DatabaseType databaseType = configuration.getDatabaseType();

    // Add columns
    if (!dbAccessor.tableHasColumn(UPGRADE_TABLE, UPGRADE_PACKAGE_COL)) {
      LOG.info("Adding upgrade_package column to upgrade table.");
      dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_PACKAGE_COL, String.class, 255, null, true));
    }
    if (!dbAccessor.tableHasColumn(UPGRADE_TABLE, UPGRADE_TYPE_COL)) {
      LOG.info("Adding upgrade_type column to upgrade table.");
      dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_TYPE_COL, String.class, 32, null, true));
    }

    // Populate values in upgrade table.
    boolean success = populateUpgradeTable();

    if (!success) {
      throw new AmbariException("Errors found while populating the upgrade table with values for columns upgrade_type and upgrade_package.");
    }

    if (dbAccessor.tableHasColumn(REPO_VERSION_TABLE, UPGRADE_PACKAGE_COL)) {
      LOG.info("Dropping upgrade_package column from repo_version table.");
      dbAccessor.dropColumn(REPO_VERSION_TABLE, UPGRADE_PACKAGE_COL);

      // Now, make the added column non-nullable
      // Make the hosts id non-null after all the values are populated
      LOG.info("Making upgrade_package column in the upgrade table non-nullable.");
      if (databaseType == Configuration.DatabaseType.DERBY) {
        // This is a workaround for UpgradeTest.java unit test
        dbAccessor.executeQuery("ALTER TABLE " + UPGRADE_TABLE + " ALTER column " + UPGRADE_PACKAGE_COL + " NOT NULL");
      } else {
        dbAccessor.alterColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_PACKAGE_COL, String.class, 255, null, false));
      }
    }

    if (dbAccessor.tableHasColumn(REPO_VERSION_TABLE, UPGRADE_TYPE_COL)) {
      // Now, make the added column non-nullable
      // Make the hosts id non-null after all the values are populated
      LOG.info("Making upgrade_type column in the upgrade table non-nullable.");
      if (databaseType == Configuration.DatabaseType.DERBY) {
        // This is a workaround for UpgradeTest.java unit test
        dbAccessor.executeQuery("ALTER TABLE " + UPGRADE_TABLE + " ALTER column " + UPGRADE_TYPE_COL + " NOT NULL");
      } else {
        dbAccessor.alterColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_TYPE_COL, String.class, 32, null, false));
      }
    }
  }

  /**
   * Populate the upgrade table with values for the columns upgrade_type and upgrade_package.
   * The upgrade_type will default to {@code org.apache.ambari.server.state.stack.upgrade.UpgradeType.ROLLING}
   * whereas the upgrade_package will be calculated.
   * @return {@code} true on success, and {@code} false otherwise.
   */
  private boolean populateUpgradeTable() {
    boolean success = true;
    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        // Need to use SQL since the schema is changing and some of the columns have not yet been added..
        rs = statement.executeQuery("SELECT upgrade_id, cluster_id, from_version, to_version, direction, upgrade_package, upgrade_type FROM upgrade");
        if (rs != null) {
          try {
            while (rs.next()) {
              final long upgradeId = rs.getLong("upgrade_id");
              final long clusterId = rs.getLong("cluster_id");
              final String fromVersion = rs.getString("from_version");
              final String toVersion = rs.getString("to_version");
              final Direction direction = Direction.valueOf(rs.getString("direction"));
              // These two values are likely null.
              String upgradePackage = rs.getString("upgrade_package");
              String upgradeType = rs.getString("upgrade_type");

              LOG.info(MessageFormat.format("Populating rows for the upgrade table record with " +
                  "upgrade_id: {0,number,#}, cluster_id: {1,number,#}, from_version: {2}, to_version: {3}, direction: {4}",
                upgradeId, clusterId, fromVersion, toVersion, direction));

              // Set all upgrades that have been done so far to type "rolling"
              if (StringUtils.isEmpty(upgradeType)) {
                LOG.info("Updating the record's upgrade_type to " + UpgradeType.ROLLING);
                dbAccessor.executeQuery("UPDATE upgrade SET upgrade_type = '" + UpgradeType.ROLLING + "' WHERE upgrade_id = " + upgradeId);
              }

              if (StringUtils.isEmpty(upgradePackage)) {
                String version = null;
                StackEntity stack = null;

                if (direction == Direction.UPGRADE) {
                  version = toVersion;
                } else if (direction == Direction.DOWNGRADE) {
                  // TODO AMBARI-12698, this is going to be a problem.
                  // During a downgrade, the "to_version" is overwritten to the source version, but the "from_version"
                  // doesn't swap. E.g.,
                  //  upgrade_id | from_version |  to_version  | direction
                  // ------------+--------------+--------------+----------
                  //           1 | 2.2.6.0-2800 | 2.3.0.0-2557 | UPGRADE
                  //           2 | 2.2.6.0-2800 | 2.2.6.0-2800 | DOWNGRADE
                  version = fromVersion;
                }

                ClusterEntity cluster = clusterDAO.findById(clusterId);

                if (null != cluster) {
                  stack = cluster.getDesiredStack();
                  upgradePackage = calculateUpgradePackage(stack, version);
                } else {
                  LOG.error("Could not find a cluster with cluster_id " + clusterId);
                }

                if (!StringUtils.isEmpty(upgradePackage)) {
                  LOG.info("Updating the record's upgrade_package to " + upgradePackage);
                  dbAccessor.executeQuery("UPDATE upgrade SET upgrade_package = '" + upgradePackage + "' WHERE upgrade_id = " + upgradeId);
                } else {
                  success = false;
                  LOG.error("Unable to populate column upgrade_package for record in table upgrade with id " + upgradeId);
                }
              }
            }
          } catch (Exception e) {
            success = false;
            e.printStackTrace();
            LOG.error("Unable to populate the upgrade_type and upgrade_package columns of the upgrade table. " + e);
          }
        }
      }
    } catch (Exception e) {
      success = false;
      e.printStackTrace();
      LOG.error("Failed to retrieve records from the upgrade table to populate the upgrade_type and upgrade_package columns. Exception: " + e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
        if (statement != null) {
          statement.close();
        }
      } catch (SQLException e) {
        ;
      }
    }
    return success;
  }

  /**
   * Find the single Repo Version for the given stack and version, and return
   * its upgrade_package column. Because the upgrade_package column is going to
   * be removed from this entity, must use raw SQL instead of the entity class.
   * <p/>
   * It's possible that there is an invalid version listed in the upgrade table.
   * For example:
   *
   * <pre>
   * upgrade
   * 1 2 1295  2.2.0.0-2041  2.2.4.2-2     UPGRADE
   * 2 2 1296  2.2.0.0-2041  2.2.0.0-2041  DOWNGRADE
   * 3 2 1299  2.2.0.0-2041  2.2.4.2       UPGRADE
   *
   * repo_version
   * 1  2.2.0.0-2041  HDP-2.2.0.0-2041  upgrade-2.2
   * 2  2.2.4.2-2     HDP-2.2.4.2-2     upgrade-2.2
   * </pre>
   *
   * Notice that it's possible for the {@code upgrade} table to include entries
   * for a repo version which does not exist; {@code 2.2.4.2}. In these cases,
   * this method will attempt a "best match".
   *
   * @param stack
   *          Stack
   * @param version
   *          Stack version
   * @return The value of the upgrade_package column, or null if not found.
   */

  private String calculateUpgradePackage(StackEntity stack, String version) {
    String upgradePackage = null;
    // Find the corresponding repo_version, and extract its upgrade_package
    if (null != version && null != stack) {
      RepositoryVersionEntity repoVersion = repositoryVersionDAO.findByStackNameAndVersion(stack.getStackName(), version);

      // a null repoVersion means there's mismatch between the upgrade and repo_version table;
      // use a best-guess approach based on the Stack
      if( null == repoVersion ){
        List<RepositoryVersionEntity> bestMatches = repositoryVersionDAO.findByStack(new StackId(stack));
        if (!bestMatches.isEmpty()) {
          repoVersion = bestMatches.get(0);
        }
      }

      // our efforts have failed; we have no idea what to use; return null as per the contract of the method
      if( null == repoVersion ) {
        return null;
      }

      Statement statement = null;
      ResultSet rs = null;
      try {
        statement = dbAccessor.getConnection().createStatement();
        if (statement != null) {
          // Need to use SQL since the schema is changing and the entity will no longer have the upgrade_package column.
          rs = statement.executeQuery("SELECT upgrade_package FROM repo_version WHERE repo_version_id = " + repoVersion.getId());
          if (rs != null && rs.next()) {
            upgradePackage = rs.getString("upgrade_package");
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to retrieve upgrade_package for repo_version record with id " + repoVersion.getId() + ". Exception: " + e.getMessage());
      } finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (statement != null) {
            statement.close();
          }
        } catch (SQLException e) {
          ;
        }
      }
    }
    return upgradePackage;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void updateKerberosDescriptorArtifact(ArtifactDAO artifactDAO, ArtifactEntity artifactEntity) throws AmbariException {
    if (artifactEntity != null) {
      Map<String, Object> data = artifactEntity.getArtifactData();

      if (data != null) {
        final KerberosDescriptor kerberosDescriptor = new KerberosDescriptorFactory().createInstance(data);

        if (kerberosDescriptor != null) {
          KerberosServiceDescriptor hdfsService = kerberosDescriptor.getService("HDFS");
          if(hdfsService != null) {
            // before 2.2.0 hdfs indentity expected to be in HDFS service
            KerberosIdentityDescriptor hdfsIdentity = hdfsService.getIdentity("hdfs");
            if (hdfsIdentity != null) {
              KerberosComponentDescriptor namenodeComponent = hdfsService.getComponent("NAMENODE");
              hdfsIdentity.setName("hdfs");
              hdfsService.removeIdentity("hdfs");
              namenodeComponent.putIdentity(hdfsIdentity);
            }
          }
          updateKerberosDescriptorIdentityReferences(kerberosDescriptor, "/HDFS/hdfs", "/HDFS/NAMENODE/hdfs");
          updateKerberosDescriptorIdentityReferences(kerberosDescriptor.getServices(), "/HDFS/hdfs", "/HDFS/NAMENODE/hdfs");

          artifactEntity.setArtifactData(kerberosDescriptor.toMap());
          artifactDAO.merge(artifactEntity);
        }
      }
    }
  }

  /**
   * If still on HDP 2.1, then no repo versions exist, so need to bootstrap the HDP 2.1 repo version,
   * If still on HDP 2.1, then no repo versions exist, so need to bootstrap the HDP 2.1 repo version,
   * and mark it as CURRENT in the cluster_version table for the cluster, as well as the host_version table
   * for all hosts.
   */
  @Transactional
  public void bootstrapRepoVersionForHDP21() throws AmbariException, SQLException {
    final String hardcodedInitialVersion = "2.1.0.0-0001";
    AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = amc.getAmbariMetaInfo();
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    RepositoryVersionHelper repositoryVersionHelper = injector.getInstance(RepositoryVersionHelper.class);
    RepositoryVersionDAO repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    ClusterVersionDAO clusterVersionDAO = injector.getInstance(ClusterVersionDAO.class);
    HostVersionDAO hostVersionDAO = injector.getInstance(HostVersionDAO.class);

    Clusters clusters = amc.getClusters();
    if (clusters == null) {
      LOG.error("Unable to get Clusters entity.");
      return;
    }

    for (Cluster cluster : clusters.getClusters().values()) {
      ClusterEntity clusterEntity = clusterDAO.findByName(cluster.getClusterName());
      final StackId stackId = cluster.getCurrentStackVersion();
      LOG.info(MessageFormat.format("Analyzing cluster {0}, currently at stack {1} and version {2}",
        cluster.getClusterName(), stackId.getStackName(), stackId.getStackVersion()));

      if (stackId.getStackName().equalsIgnoreCase("HDP") && stackId.getStackVersion().equalsIgnoreCase("2.1")) {
        final StackInfo stackInfo = ambariMetaInfo.getStack(stackId.getStackName(), stackId.getStackVersion());
        StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

        LOG.info("Bootstrapping the versions since using HDP-2.1");

        // The actual value is not known, so use this.
        String displayName = stackId.getStackName() + "-" + hardcodedInitialVersion;

        // However, the Repo URLs should be correct.
        String operatingSystems = repositoryVersionHelper.serializeOperatingSystems(stackInfo.getRepositories());

        // Create the Repo Version if it doesn't already exist.
        RepositoryVersionEntity repoVersionEntity = repositoryVersionDAO.findByDisplayName(displayName);
        if (null != repoVersionEntity) {
          LOG.info(MessageFormat.format("A Repo Version already exists with Display Name: {0}", displayName));
        } else {
          final long repoVersionIdSeq = repositoryVersionDAO.findMaxId("id");
          // Safe to attempt to add the sequence if it doesn't exist already.
          addSequence("repo_version_id_seq", repoVersionIdSeq, false);

          repoVersionEntity = repositoryVersionDAO.create(
            stackEntity, hardcodedInitialVersion, displayName, operatingSystems);
          LOG.info(MessageFormat.format("Created Repo Version with ID: {0,number,#}\n, Display Name: {1}, Repo URLs: {2}\n",
            repoVersionEntity.getId(), displayName, operatingSystems));
        }

        // Create the Cluster Version if it doesn't already exist.
        ClusterVersionEntity clusterVersionEntity = clusterVersionDAO.findByClusterAndStackAndVersion(cluster.getClusterName(),
          stackId, hardcodedInitialVersion);

        if (null != clusterVersionEntity) {
          LOG.info(MessageFormat.format("A Cluster Version version for cluster: {0}, version: {1}, already exists; its state is {2}.",
            cluster.getClusterName(), clusterVersionEntity.getRepositoryVersion().getVersion(), clusterVersionEntity.getState()));

          // If there are not CURRENT cluster versions, make this one the CURRENT one.
          if (clusterVersionEntity.getState() != RepositoryVersionState.CURRENT &&
            clusterVersionDAO.findByClusterAndState(cluster.getClusterName(), RepositoryVersionState.CURRENT).isEmpty()) {
            clusterVersionEntity.setState(RepositoryVersionState.CURRENT);
            clusterVersionDAO.merge(clusterVersionEntity);
          }
        } else {
          final long clusterVersionIdSeq = clusterVersionDAO.findMaxId("id");
          // Safe to attempt to add the sequence if it doesn't exist already.
          addSequence("cluster_version_id_seq", clusterVersionIdSeq, false);

          clusterVersionEntity = clusterVersionDAO.create(clusterEntity, repoVersionEntity, RepositoryVersionState.CURRENT,
            System.currentTimeMillis(), System.currentTimeMillis(), "admin");
          LOG.info(MessageFormat.format("Created Cluster Version with ID: {0,number,#}, cluster: {1}, version: {2}, state: {3}.",
            clusterVersionEntity.getId(), cluster.getClusterName(), clusterVersionEntity.getRepositoryVersion().getVersion(),
            clusterVersionEntity.getState()));
        }

        // Create the Host Versions if they don't already exist.
        Collection<HostEntity> hosts = clusterEntity.getHostEntities();
        boolean addedAtLeastOneHost = false;
        if (null != hosts && !hosts.isEmpty()) {
          for (HostEntity hostEntity : hosts) {
            HostVersionEntity hostVersionEntity = hostVersionDAO.findByClusterStackVersionAndHost(cluster.getClusterName(),
              stackId, hardcodedInitialVersion, hostEntity.getHostName());

            if (null != hostVersionEntity) {
              LOG.info(MessageFormat.format("A Host Version version for cluster: {0}, version: {1}, host: {2}, already exists; its state is {3}.",
                cluster.getClusterName(), hostVersionEntity.getRepositoryVersion().getVersion(),
                hostEntity.getHostName(), hostVersionEntity.getState()));

              if (hostVersionEntity.getState() != RepositoryVersionState.CURRENT &&
                hostVersionDAO.findByClusterHostAndState(cluster.getClusterName(), hostEntity.getHostName(),
                  RepositoryVersionState.CURRENT).isEmpty()) {
                hostVersionEntity.setState(RepositoryVersionState.CURRENT);
                hostVersionDAO.merge(hostVersionEntity);
              }
            } else {
              // This should only be done the first time.
              if (!addedAtLeastOneHost) {
                final long hostVersionIdSeq = hostVersionDAO.findMaxId("id");
                // Safe to attempt to add the sequence if it doesn't exist already.
                addSequence("host_version_id_seq", hostVersionIdSeq, false);
                addedAtLeastOneHost = true;
              }

              hostVersionEntity = new HostVersionEntity(hostEntity, repoVersionEntity, RepositoryVersionState.CURRENT);
              hostVersionDAO.create(hostVersionEntity);
              LOG.info(MessageFormat.format("Created Host Version with ID: {0,number,#}, cluster: {1}, version: {2}, host: {3}, state: {4}.",
                hostVersionEntity.getId(), cluster.getClusterName(), hostVersionEntity.getRepositoryVersion().getVersion(),
                hostEntity.getHostName(), hostVersionEntity.getState()));
            }
          }
        } else {
          LOG.info(MessageFormat.format("Not inserting any Host Version records since cluster {0} does not have any hosts.",
            cluster.getClusterName()));
        }
      }
    }
  }

  /**
   * Adds the following columns to the {@value #UPGRADE_TABLE} table:
   * <ul>
   * <li>{@value #DOWNGRADE_ALLOWED_COLUMN}</li>
   * <li>{@value #UPGRADE_SKIP_FAILURE_COLUMN}</li>
   * <li>{@value #UPGRADE_SKIP_SC_FAILURE_COLUMN}</li>
   * </ul>
   *
   * @throws SQLException
   */
  protected void updateUpgradesDDL() throws SQLException{
    dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(DOWNGRADE_ALLOWED_COLUMN, Short.class, 1, null, true));
    dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_SKIP_FAILURE_COLUMN, Short.class, 1, null, true));
    dbAccessor.addColumn(UPGRADE_TABLE, new DBColumnInfo(UPGRADE_SKIP_SC_FAILURE_COLUMN, Short.class, 1, null, true));
  }

  /**
   * Modifies the JSON of some of the alert definitions which have changed
   * between Ambari versions.
   */
  protected void updateAlertDefinitions() {
    LOG.info("Updating alert definitions.");
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);
    Clusters clusters = ambariManagementController.getClusters();

    Map<String, Cluster> clusterMap = getCheckedClusterMap(clusters);
    for (final Cluster cluster : clusterMap.values()) {
      long clusterID = cluster.getClusterId();

      final AlertDefinitionEntity journalNodeProcessAlertDefinitionEntity = alertDefinitionDAO.findByName(
        clusterID, "journalnode_process");
      final AlertDefinitionEntity hostDiskUsageAlertDefinitionEntity = alertDefinitionDAO.findByName(
          clusterID, "ambari_agent_disk_usage");

      if (journalNodeProcessAlertDefinitionEntity != null) {
        String source = journalNodeProcessAlertDefinitionEntity.getSource();

        journalNodeProcessAlertDefinitionEntity.setSource(modifyJournalnodeProcessAlertSource(source));
        journalNodeProcessAlertDefinitionEntity.setSourceType(SourceType.WEB);
        journalNodeProcessAlertDefinitionEntity.setHash(UUID.randomUUID().toString());

        alertDefinitionDAO.merge(journalNodeProcessAlertDefinitionEntity);
        LOG.info("journalnode_process alert definition was updated.");
      }

      if (hostDiskUsageAlertDefinitionEntity != null) {
        hostDiskUsageAlertDefinitionEntity.setDescription("This host-level alert is triggered if the amount of disk space " +
            "used goes above specific thresholds. The default threshold values are 50% for WARNING and 80% for CRITICAL.");
        hostDiskUsageAlertDefinitionEntity.setLabel("Host Disk Usage");

        alertDefinitionDAO.merge(hostDiskUsageAlertDefinitionEntity);
        LOG.info("ambari_agent_disk_usage alert definition was updated.");
      }

    }
  }

  /**
   * Modifies type of the journalnode_process alert to WEB.
   * Changes reporting text and uri according to the WEB type.
   * Removes default_port property.
   */
  String modifyJournalnodeProcessAlertSource(String source) {
    JsonObject rootJson = new JsonParser().parse(source).getAsJsonObject();

    rootJson.remove("type");
    rootJson.addProperty("type", "WEB");

    rootJson.remove("default_port");

    rootJson.remove("uri");
    JsonObject uriJson = new JsonObject();
    uriJson.addProperty("http", "{{hdfs-site/dfs.journalnode.http-address}}");
    uriJson.addProperty("https", "{{hdfs-site/dfs.journalnode.https-address}}");
    uriJson.addProperty("kerberos_keytab", "{{hdfs-site/dfs.web.authentication.kerberos.keytab}}");
    uriJson.addProperty("kerberos_principal", "{{hdfs-site/dfs.web.authentication.kerberos.principal}}");
    uriJson.addProperty("https_property", "{{hdfs-site/dfs.http.policy}}");
    uriJson.addProperty("https_property_value", "HTTPS_ONLY");
    uriJson.addProperty("connection_timeout", 5.0);
    rootJson.add("uri", uriJson);

    rootJson.getAsJsonObject("reporting").getAsJsonObject("ok").remove("text");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("ok").addProperty(
      "text", "HTTP {0} response in {2:.3f}s");

    rootJson.getAsJsonObject("reporting").getAsJsonObject("warning").remove("text");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("warning").addProperty(
      "text", "HTTP {0} response from {1} in {2:.3f}s ({3})");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("warning").remove("value");

    rootJson.getAsJsonObject("reporting").getAsJsonObject("critical").remove("text");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("critical").addProperty("text",
      "Connection failed to {1} ({3})");
    rootJson.getAsJsonObject("reporting").getAsJsonObject("critical").remove("value");

    return rootJson.toString();
  }

  protected void updateHadoopEnv() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config hadoopEnvConfig = cluster.getDesiredConfigByType(HADOOP_ENV_CONFIG);
      if (hadoopEnvConfig != null) {
        String content = hadoopEnvConfig.getProperties().get(CONTENT_PROPERTY);
        if (content != null) {
          content += HADOOP_ENV_CONTENT_TO_APPEND;
          Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, content);
          updateConfigurationPropertiesForCluster(cluster, HADOOP_ENV_CONFIG, updates, true, false);
        }
      }
    }
  }

  protected void updateHDFSConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(
      AmbariManagementController.class);
    Map<String, Cluster> clusterMap = getCheckedClusterMap(ambariManagementController.getClusters());

    for (final Cluster cluster : clusterMap.values()) {
      // Remove dfs.namenode.rpc-address property when NN HA is enabled
      if (cluster.getDesiredConfigByType(HDFS_SITE_CONFIG) != null && isNNHAEnabled(cluster)) {
        Set<String> removePropertiesSet = new HashSet<>();
        removePropertiesSet.add("dfs.namenode.rpc-address");
        removeConfigurationPropertiesFromCluster(cluster, HDFS_SITE_CONFIG, removePropertiesSet);
      }
    }
  }

  protected void updateZookeeperLog4j() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config zookeeperLog4jConfig = cluster.getDesiredConfigByType(ZOOKEEPER_LOG4J_CONFIG);
      if (zookeeperLog4jConfig != null) {
        String content = zookeeperLog4jConfig.getProperties().get(CONTENT_PROPERTY);
        if (content != null) {
          content = content.replaceAll("[\n^]\\s*log4j\\.rootLogger\\s*=\\s*INFO\\s*,\\s*CONSOLE", "\nlog4j.rootLogger=INFO, ROLLINGFILE");
          Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, content);
          updateConfigurationPropertiesForCluster(cluster, ZOOKEEPER_LOG4J_CONFIG, updates, true, false);
        }
      }
    }
  }

  protected void updateStormConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config stormSiteProps = cluster.getDesiredConfigByType(STORM_SITE);
      if (stormSiteProps != null) {
        Map<String, String> updates = new HashMap<>();

        String nimbusMonitorFreqSecs = stormSiteProps.getProperties().get(NIMBS_MONITOR_FREQ_SECS_PROPERTY);
        if (nimbusMonitorFreqSecs != null && nimbusMonitorFreqSecs.equals("10")) {
          updates.put(NIMBS_MONITOR_FREQ_SECS_PROPERTY, "120");
        }

        Service amsService = null;
        try {
          amsService = cluster.getService("AMBARI_METRICS");
        } catch(AmbariException ambariException) {
          LOG.info("AMBARI_METRICS service not found in cluster while updating storm-site properties");
        }
        String metricsReporter = stormSiteProps.getProperties().get(STORM_METRICS_REPORTER);
        if (amsService != null && StringUtils.isEmpty(metricsReporter)) {
          updates.put(STORM_METRICS_REPORTER, "org.apache.hadoop.metrics2.sink.storm.StormTimelineMetricsReporter");
        }

        updateConfigurationPropertiesForCluster(cluster, STORM_SITE, updates, true, false);
      }
    }
  }

  protected void updateHiveConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config hiveSiteConfig = cluster.getDesiredConfigByType(HIVE_SITE_CONFIG);
      if (hiveSiteConfig != null) {
        String hiveServer2OperationLogLocation = hiveSiteConfig.getProperties().get(HIVE_SERVER2_OPERATION_LOG_LOCATION_PROPERTY);
        if (hiveServer2OperationLogLocation != null && hiveServer2OperationLogLocation.equals("${system:java.io.tmpdir}/${system:user.name}/operation_logs")) {
          Map<String, String> updates = Collections.singletonMap(HIVE_SERVER2_OPERATION_LOG_LOCATION_PROPERTY, "/tmp/hive/operation_logs");
          updateConfigurationPropertiesForCluster(cluster, HIVE_SITE_CONFIG, updates, true, false);
        }
      }
      StackId stackId = cluster.getCurrentStackVersion();
      boolean isStackNotLess23 = (stackId != null && stackId.getStackName().equals("HDP") &&
              VersionUtils.compareVersions(stackId.getStackVersion(), "2.3") >= 0);

      Config hiveEnvConfig = cluster.getDesiredConfigByType(HIVE_ENV_CONFIG);
      if (hiveEnvConfig != null) {
        Map<String, String> hiveEnvProps = new HashMap<String, String>();
        String content = hiveEnvConfig.getProperties().get(CONTENT_PROPERTY);
        // For HDP-2.3 we need to add hive heap size management to content,
        // for others we need to update content
        if(content != null) {
          if(isStackNotLess23) {
            content = updateHiveEnvContentHDP23(content);
          } else {
            content = updateHiveEnvContent(content);
          }
          hiveEnvProps.put(CONTENT_PROPERTY, content);
          updateConfigurationPropertiesForCluster(cluster, HIVE_ENV_CONFIG, hiveEnvProps, true, true);
        }
      }

    }
  }

  protected void updateHbaseEnvConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    boolean updateConfig = false;

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      StackId stackId = cluster.getCurrentStackVersion();
      Config hbaseEnvConfig = cluster.getDesiredConfigByType(HBASE_ENV_CONFIG);
      if (hbaseEnvConfig != null) {
        String content = hbaseEnvConfig.getProperties().get(CONTENT_PROPERTY);
        if (content != null) {
          if (!content.contains("-Djava.io.tmpdir")) {
            content += "\n\nexport HBASE_OPTS=\"${HBASE_OPTS} -Djava.io.tmpdir={{java_io_tmpdir}}\"";
            updateConfig = true;
          }
          if (stackId != null && stackId.getStackName().equals("HDP") &&
              VersionUtils.compareVersions(stackId.getStackVersion(), "2.2") >= 0) {
            if (!content.contains("MaxDirectMemorySize={{hbase_max_direct_memory_size}}m")) {
              String newPartOfContent = "\n\nexport HBASE_REGIONSERVER_OPTS=\"$HBASE_REGIONSERVER_OPTS {% if hbase_max_direct_memory_size %} -XX:MaxDirectMemorySize={{hbase_max_direct_memory_size}}m {% endif %}\"\n\n";
              content += newPartOfContent;
              updateConfig = true;
            }
            if (updateConfig) {
              Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, content);
              updateConfigurationPropertiesForCluster(cluster, HBASE_ENV_CONFIG, updates, true, false);
            }
          }
        }
      }
    }
  }

  protected void updateFlumeEnvConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config flumeEnvConfig = cluster.getDesiredConfigByType(FLUME_ENV_CONFIG);
      if (flumeEnvConfig != null) {
        String content = flumeEnvConfig.getProperties().get(CONTENT_PROPERTY);
        if (content != null && !content.contains("/usr/lib/flume/lib/ambari-metrics-flume-sink.jar")) {
          String newPartOfContent = "\n\n" +
            "# Note that the Flume conf directory is always included in the classpath.\n" +
            "# Add flume sink to classpath\n" +
            "if [ -e \"/usr/lib/flume/lib/ambari-metrics-flume-sink.jar\" ]; then\n" +
            "  export FLUME_CLASSPATH=$FLUME_CLASSPATH:/usr/lib/flume/lib/ambari-metrics-flume-sink.jar\n" +
            "fi\n";
          content += newPartOfContent;
          Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, content);
          updateConfigurationPropertiesForCluster(cluster, FLUME_ENV_CONFIG, updates, true, false);
        }
      }
    }
  }

  protected void updateAMSConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();

      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {

          Config amsHbaseEnv = cluster.getDesiredConfigByType(AMS_HBASE_ENV);
          if (amsHbaseEnv != null) {
            Map<String, String> amsHbaseEnvProperties = amsHbaseEnv.getProperties();
            String content = amsHbaseEnvProperties.get("content");
            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("content", updateAmsHbaseEnvContent(content));
            updateConfigurationPropertiesForCluster(cluster, AMS_HBASE_ENV, newProperties, true, true);
          }

          Config amsEnv = cluster.getDesiredConfigByType(AMS_ENV);
          if (amsEnv != null) {
            Map<String, String> amsEnvProperties = amsEnv.getProperties();
            String content = amsEnvProperties.get("content");
            Map<String, String> newProperties = new HashMap<>();
            newProperties.put("content", updateAmsEnvContent(content));
            updateConfigurationPropertiesForCluster(cluster, AMS_ENV, newProperties, true, true);
          }

          Config amsSite = cluster.getDesiredConfigByType(AMS_SITE);
          if (amsSite != null) {
            Map<String, String> currentAmsSiteProperties = amsSite.getProperties();
            Map<String, String> newProperties = new HashMap<>();

            //Changed AMS result set limit from 5760 to 15840.
            if(currentAmsSiteProperties.containsKey("timeline.metrics.service.default.result.limit") &&
              currentAmsSiteProperties.get("timeline.metrics.service.default.result.limit").equals(String.valueOf(5760))) {
              LOG.info("Updating timeline.metrics.service.default.result.limit to 15840");
              newProperties.put("timeline.metrics.service.default.result.limit", String.valueOf(15840));
            }

            //Interval
            newProperties.put("timeline.metrics.cluster.aggregator.second.interval", String.valueOf(120));
            newProperties.put("timeline.metrics.cluster.aggregator.minute.interval", String.valueOf(300));
            newProperties.put("timeline.metrics.host.aggregator.minute.interval", String.valueOf(300));

            //ttl
            newProperties.put("timeline.metrics.cluster.aggregator.second.ttl", String.valueOf(2592000));
            newProperties.put("timeline.metrics.cluster.aggregator.minute.ttl", String.valueOf(7776000));

            //checkpoint
            newProperties.put("timeline.metrics.cluster.aggregator.second.checkpointCutOffMultiplier", String.valueOf(2));

            //disabled
            newProperties.put("timeline.metrics.cluster.aggregator.second.disabled", String.valueOf(false));

            //Add compaction policy property
            newProperties.put(TIMELINE_METRICS_HBASE_FIFO_COMPACTION_ENABLED, String.valueOf(true));

            updateConfigurationPropertiesForCluster(cluster, AMS_SITE, newProperties, true, true);
          }

          Config amsHbaseSite = cluster.getDesiredConfigByType(AMS_HBASE_SITE);
          if (amsHbaseSite != null) {
            Map<String, String> amsHbaseSiteProperties = amsHbaseSite.getProperties();
            Map<String, String> newProperties = new HashMap<>();

            String zkTimeout = amsHbaseSiteProperties.get(AMS_HBASE_SITE_ZK_TIMEOUT_PROPERTY);
            // if old default, set new default
            if ("20000".equals(zkTimeout)) {
              newProperties.put(AMS_HBASE_SITE_ZK_TIMEOUT_PROPERTY, "120000");
            }

            //Adding hbase.normalizer.period to upgrade
            if(!amsHbaseSiteProperties.containsKey(AMS_HBASE_SITE_NORMALIZER_ENABLED_PROPERTY)) {
              LOG.info("Enabling " + AMS_HBASE_SITE_NORMALIZER_ENABLED_PROPERTY);
              newProperties.put(AMS_HBASE_SITE_NORMALIZER_ENABLED_PROPERTY, String.valueOf(true));
            }

            if(!amsHbaseSiteProperties.containsKey(AMS_HBASE_SITE_NORMALIZER_PERIOD_PROPERTY)) {
              LOG.info("Updating " + AMS_HBASE_SITE_NORMALIZER_PERIOD_PROPERTY);
              newProperties.put(AMS_HBASE_SITE_NORMALIZER_PERIOD_PROPERTY, String.valueOf(600000));
            }

            if(!amsHbaseSiteProperties.containsKey(AMS_HBASE_SITE_NORMALIZER_CLASS_PROPERTY)) {
              LOG.info("Updating " + AMS_HBASE_SITE_NORMALIZER_CLASS_PROPERTY);
              newProperties.put(AMS_HBASE_SITE_NORMALIZER_CLASS_PROPERTY,
                "org.apache.hadoop.hbase.master.normalizer.SimpleRegionNormalizer");
            }
            updateConfigurationPropertiesForCluster(cluster, AMS_HBASE_SITE, newProperties, true, true);
          }
        }
      }
    }

  }

  protected String updateAmsHbaseEnvContent(String content) {
    if (content == null) {
      return null;
    }
    String regSearch = "export HBASE_HEAPSIZE=";
    String replacement = "#export HBASE_HEAPSIZE=";
    content = content.replaceAll(regSearch, replacement);
    content += "\n" +
      "# The maximum amount of heap to use for hbase shell.\n" +
      "export HBASE_SHELL_OPTS=\"-Xmx256m\"\n";
    return content;
  }

  protected String updateAmsEnvContent(String content) {
    if (content == null) {
      return null;
    }
    if (!content.contains("AMS_COLLECTOR_GC_OPTS")) {
      content += "\n" +
        "# AMS Collector GC options\n" +
        "export AMS_COLLECTOR_GC_OPTS=\"-XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=70 " +
        "-XX:+UseCMSInitiatingOccupancyOnly -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps " +
        "-XX:+UseGCLogFileRotation -XX:GCLogFileSize=10M " +
        "-Xloggc:{{ams_collector_log_dir}}/collector-gc.log-`date +'%Y%m%d%H%M'`\"\n" +
        "export AMS_COLLECTOR_OPTS=\"$AMS_COLLECTOR_OPTS $AMS_COLLECTOR_GC_OPTS\"\n";
    }

    if (!content.contains("AMS_HBASE_NORMALIZER_ENABLED")) {
      content += "\n" +
        "# HBase normalizer enabled\n" +
        "export AMS_HBASE_NORMALIZER_ENABLED={{ams_hbase_normalizer_enabled}}\n";
    }

    if (!content.contains("AMS_HBASE_FIFO_COMPACTION_ENABLED")) {
      content += "\n" +
        "# HBase compaction policy enabled\n" +
        "export AMS_HBASE_FIFO_COMPACTION_ENABLED={{ams_hbase_fifo_compaction_enabled}}\n";
    }

    return content;
  }

  protected void updateKafkaConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    Clusters clusters = ambariManagementController.getClusters();

    if (clusters != null) {
      Map<String, Cluster> clusterMap = clusters.getClusters();
      if (clusterMap != null && !clusterMap.isEmpty()) {
        for (final Cluster cluster : clusterMap.values()) {
          Set<String> installedServices = cluster.getServices().keySet();
          Config kafkaBroker = cluster.getDesiredConfigByType(KAFKA_BROKER);
          if (kafkaBroker != null) {
            Map<String, String> newProperties = new HashMap<>();
            Map<String, String> kafkaBrokerProperties = kafkaBroker.getProperties();
            String kafkaMetricsReporters = kafkaBrokerProperties.get("kafka.metrics.reporters");
            if (kafkaMetricsReporters == null ||
              "{{kafka_metrics_reporters}}".equals(kafkaMetricsReporters)) {

              if (installedServices.contains("AMBARI_METRICS")) {
                newProperties.put("kafka.metrics.reporters", "org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter");
              } else if (installedServices.contains("GANGLIA")) {
                newProperties.put("kafka.metrics.reporters", "kafka.ganglia.KafkaGangliaMetricsReporter");
              } else {
                newProperties.put("kafka.metrics.reporters", " ");
              }

            }
            if (!newProperties.isEmpty()) {
              updateConfigurationPropertiesForCluster(cluster, KAFKA_BROKER, newProperties, true, true);
            }
          }

          Config kafkaEnv = cluster.getDesiredConfigByType(KAFKA_ENV_CONFIG);
          if (kafkaEnv != null) {
            String kafkaEnvContent = kafkaEnv.getProperties().get(CONTENT_PROPERTY);
            if (kafkaEnvContent != null && !kafkaEnvContent.contains(KAFKA_ENV_CONTENT_KERBEROS_PARAMS)) {
              kafkaEnvContent += "\n\nexport KAFKA_KERBEROS_PARAMS=\"$KAFKA_KERBEROS_PARAMS {{kafka_kerberos_params}}\"";
              Map<String, String> updates = Collections.singletonMap(CONTENT_PROPERTY, kafkaEnvContent);
              updateConfigurationPropertiesForCluster(cluster, KAFKA_ENV_CONFIG, updates, true, false);
            }
          }
        }
      }
    }
  }

  protected void updateRangerEnvConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Map<String, String> newRangerEnvProps = new HashMap<>();
      Config rangerHdfsPluginProperties = cluster.getDesiredConfigByType("ranger-hdfs-plugin-properties");
      if (rangerHdfsPluginProperties != null && rangerHdfsPluginProperties.getProperties().containsKey(RANGER_HDFS_PLUGIN_ENABLED_PROPERTY)) {
        newRangerEnvProps.put(RANGER_HDFS_PLUGIN_ENABLED_PROPERTY, rangerHdfsPluginProperties.getProperties().get(RANGER_HDFS_PLUGIN_ENABLED_PROPERTY));
      }
      Config hiveEnvProperties = cluster.getDesiredConfigByType("hive-env");
      if (hiveEnvProperties != null && hiveEnvProperties.getProperties().containsKey("hive_security_authorization")
              && hiveEnvProperties.getProperties().get("hive_security_authorization").toLowerCase().equals("ranger")) {
        newRangerEnvProps.put(RANGER_HIVE_PLUGIN_ENABLED_PROPERTY, "Yes");
      }
      Config rangerHbasePluginProperties = cluster.getDesiredConfigByType("ranger-hbase-plugin-properties");
      if (rangerHbasePluginProperties != null && rangerHbasePluginProperties.getProperties().containsKey(RANGER_HBASE_PLUGIN_ENABLED_PROPERTY)) {
        newRangerEnvProps.put(RANGER_HBASE_PLUGIN_ENABLED_PROPERTY, rangerHbasePluginProperties.getProperties().get(RANGER_HBASE_PLUGIN_ENABLED_PROPERTY));
      }

      Config rangerStormPluginProperties = cluster.getDesiredConfigByType("ranger-storm-plugin-properties");
      if (rangerStormPluginProperties != null && rangerStormPluginProperties.getProperties().containsKey(RANGER_STORM_PLUGIN_ENABLED_PROPERTY)) {
        newRangerEnvProps.put(RANGER_STORM_PLUGIN_ENABLED_PROPERTY, rangerStormPluginProperties.getProperties().get(RANGER_STORM_PLUGIN_ENABLED_PROPERTY));
      }
      Config rangerKnoxPluginProperties = cluster.getDesiredConfigByType("ranger-knox-plugin-properties");
      if (rangerKnoxPluginProperties != null && rangerKnoxPluginProperties.getProperties().containsKey(RANGER_KNOX_PLUGIN_ENABLED_PROPERTY)) {
        newRangerEnvProps.put(RANGER_KNOX_PLUGIN_ENABLED_PROPERTY, rangerKnoxPluginProperties.getProperties().get(RANGER_KNOX_PLUGIN_ENABLED_PROPERTY));
      }
      Config rangerYarnPluginProperties = cluster.getDesiredConfigByType("ranger-yarn-plugin-properties");
      if (rangerYarnPluginProperties != null && rangerYarnPluginProperties.getProperties().containsKey(RANGER_YARN_PLUGIN_ENABLED_PROPERTY)) {
        newRangerEnvProps.put(RANGER_YARN_PLUGIN_ENABLED_PROPERTY, rangerYarnPluginProperties.getProperties().get(RANGER_YARN_PLUGIN_ENABLED_PROPERTY));
      }
      Config rangerKafkaPluginProperties = cluster.getDesiredConfigByType("ranger-kafka-plugin-properties");
      if (rangerKafkaPluginProperties != null && rangerKafkaPluginProperties.getProperties().containsKey(RANGER_KAFKA_PLUGIN_ENABLED_PROPERTY)) {
        newRangerEnvProps.put(RANGER_KAFKA_PLUGIN_ENABLED_PROPERTY, rangerKafkaPluginProperties.getProperties().get(RANGER_KAFKA_PLUGIN_ENABLED_PROPERTY));
      }
      if (!newRangerEnvProps.isEmpty()) {
        updateConfigurationPropertiesForCluster(cluster, RANGER_ENV_CONFIG, newRangerEnvProps, true, true);
      }
    }
  }

  protected void updateRangerUgsyncSiteConfig() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);

    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      Config rangerUgsyncSiteProperties = cluster.getDesiredConfigByType(RANGER_UGSYNC_SITE_CONFIG);
      if (rangerUgsyncSiteProperties != null && rangerUgsyncSiteProperties.getProperties().containsKey(RANGER_USERSYNC_SOURCE_IMPL_CLASS_PROPERTY)) {
        String sourceClassValue = rangerUgsyncSiteProperties.getProperties().get(RANGER_USERSYNC_SOURCE_IMPL_CLASS_PROPERTY);
        if (sourceClassValue != null) {
          if ("ldap".equals(sourceClassValue)) {
            Map<String, String> updates = Collections.singletonMap(RANGER_USERSYNC_SOURCE_IMPL_CLASS_PROPERTY,
                "org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder");
            updateConfigurationPropertiesForCluster(cluster, RANGER_UGSYNC_SITE_CONFIG, updates, true, false);
          } else if ("unix".equals(sourceClassValue)) {
            Map<String, String> updates = Collections.singletonMap(RANGER_USERSYNC_SOURCE_IMPL_CLASS_PROPERTY,
                "org.apache.ranger.unixusersync.process.UnixUserGroupBuilder");
            updateConfigurationPropertiesForCluster(cluster, RANGER_UGSYNC_SITE_CONFIG, updates, true, false);
          } else if ("file".equals(sourceClassValue)) {
            Map<String, String> updates = Collections.singletonMap(RANGER_USERSYNC_SOURCE_IMPL_CLASS_PROPERTY,
                "org.apache.ranger.unixusersync.process.FileSourceUserGroupBuilder");
            updateConfigurationPropertiesForCluster(cluster, RANGER_UGSYNC_SITE_CONFIG, updates, true, false);
          }
        }
      }
    }
  }

  protected String updateHiveEnvContent(String hiveEnvContent) {
    if(hiveEnvContent == null) {
      return null;
    }
    // There are two cases here
    // We do not have "export HADOOP_CLIENT_OPTS" and we need to add it
    // We have "export HADOOP_CLIENT_OPTS" with wrong order
    String exportHadoopClientOpts = "(?s).*export\\s*HADOOP_CLIENT_OPTS.*";
    if (hiveEnvContent.matches(exportHadoopClientOpts)) {
      String oldHeapSizeRegex = "export\\s*HADOOP_CLIENT_OPTS=\"-Xmx\\$\\{HADOOP_HEAPSIZE\\}m\\s*\\$HADOOP_CLIENT_OPTS\"";
      String newHeapSizeRegex = "export HADOOP_CLIENT_OPTS=\"$HADOOP_CLIENT_OPTS  -Xmx${HADOOP_HEAPSIZE}m\"";
      return hiveEnvContent.replaceAll(oldHeapSizeRegex, Matcher.quoteReplacement(newHeapSizeRegex));
    } else {
      String oldHeapSizeRegex = "export\\s*HADOOP_HEAPSIZE\\s*=\\s*\"\\{\\{hive_heapsize\\}\\}\"\\.*\\n\\s*fi\\s*\\n";
      String newHeapSizeRegex = "export HADOOP_HEAPSIZE={{hive_heapsize}} # Setting for HiveServer2 and Client\n" +
              "fi\n" +
              "\n" +
              "export HADOOP_CLIENT_OPTS=\"$HADOOP_CLIENT_OPTS  -Xmx${HADOOP_HEAPSIZE}m\"\n";
      return hiveEnvContent.replaceAll(oldHeapSizeRegex, Matcher.quoteReplacement(newHeapSizeRegex));
    }
  }

  protected String updateHiveEnvContentHDP23(String hiveEnvContent) {
    if(hiveEnvContent == null) {
      return null;
    }
    String oldHeapSizeRegex = "# The heap size of the jvm stared by hive shell script can be controlled via:\\s*\\n";
    String newHeapSizeRegex = "# The heap size of the jvm stared by hive shell script can be controlled via:\n" +
            "\n" +
            "if [ \"$SERVICE\" = \"metastore\" ]; then\n" +
            "  export HADOOP_HEAPSIZE={{hive_metastore_heapsize}} # Setting for HiveMetastore\n" +
            "else\n" +
            "  export HADOOP_HEAPSIZE={{hive_heapsize}} # Setting for HiveServer2 and Client\n" +
            "fi\n" +
            "\n" +
            "export HADOOP_CLIENT_OPTS=\"$HADOOP_CLIENT_OPTS  -Xmx${HADOOP_HEAPSIZE}m\"\n" +
            "\n";
    return hiveEnvContent.replaceFirst(oldHeapSizeRegex, Matcher.quoteReplacement(newHeapSizeRegex));
  }

  protected void updateAccumuloConfigs() throws AmbariException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    for (final Cluster cluster : getCheckedClusterMap(ambariManagementController.getClusters()).values()) {
      // If security type is set to Kerberos, update Kerberos-related configs
      if(cluster.getSecurityType() == SecurityType.KERBEROS) {
        Config clientProps = cluster.getDesiredConfigByType("client");
        if (clientProps != null) {
          Map<String, String> properties = clientProps.getProperties();
          if (properties == null) {
            properties = new HashMap<String, String>();
          }
          // <2.2.0 did not account for a custom service principal.
          // Need to ensure that the client knows the server's principal (the primary) to properly authenticate.
          properties.put("kerberos.server.primary", "{{bare_accumulo_principal}}");
          updateConfigurationPropertiesForCluster(cluster, "client", properties, true, false);
        }
      } // else -- no special client-configuration is necessary.
    }
  }
}
