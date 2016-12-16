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
package org.apache.ambari.server.checks;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.apache.ambari.server.state.SecurityState;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

public class DatabaseConsistencyCheckHelper {

  static Logger LOG = LoggerFactory.getLogger(DatabaseConsistencyCheckHelper.class);

  @Inject
  private static Injector injector;

  private static MetainfoDAO metainfoDAO;
  private static Connection connection;
  private static AmbariMetaInfo ambariMetaInfo;
  private static DBAccessor dbAccessor;


  private static boolean errorsFound = false;
  private static boolean warningsFound = false;

  public static boolean ifErrorsFound() {
    return errorsFound;
  }

  public static boolean ifWarningsFound() {
    return warningsFound;
  }

  public static void resetErrorWarningFlags() {
    errorsFound = false;
    warningsFound = false;
  }

  public static void setInjector(Injector injector) {
    DatabaseConsistencyCheckHelper.injector = injector;
    // Clean up: new injector means static fields should be reinitalized, though in real life it only occurs during testing
    closeConnection();
    connection = null;
    metainfoDAO = null;
    ambariMetaInfo = null;
    dbAccessor = null;
  }

  public static void setConnection(Connection connection) {
    DatabaseConsistencyCheckHelper.connection = connection;
  }

  /*
    * method to close connection
    * */
  public static void closeConnection() {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException e) {
      LOG.error("Exception occurred during connection close procedure: ", e);
    }
  }


  public static void fixDatabaseConsistency() {
    fixHostComponentStatesCountEqualsHostComponentsDesiredStates();
    fixClusterConfigsNotMappedToAnyService();
  }

  public static void runAllDBChecks() {
    LOG.info("******************************* Check database started *******************************");
    checkSchemaName();
    checkMySQLEngine();
    checkForConfigsNotMappedToService();
    checkForNotMappedConfigsToCluster();
    checkForConfigsSelectedMoreThanOnce();
    checkForHostsWithoutState();
    checkHostComponentStates();
    checkServiceConfigs();
    LOG.info("******************************* Check database completed *******************************");
  }

  public static void checkDBVersionCompatible() throws AmbariException {
    LOG.info("Checking DB store version");

    if (metainfoDAO == null) {
      metainfoDAO = injector.getInstance(MetainfoDAO.class);
    }

    MetainfoEntity schemaVersionEntity = metainfoDAO.findByKey(Configuration.SERVER_VERSION_KEY);
    String schemaVersion = null;

    if (schemaVersionEntity != null) {
      schemaVersion = schemaVersionEntity.getMetainfoValue();
    }

    Configuration conf = injector.getInstance(Configuration.class);
    File versionFile = new File(conf.getServerVersionFilePath());
    if (!versionFile.exists()) {
      throw new AmbariException("Server version file does not exist.");
    }
    String serverVersion = null;
    try (Scanner scanner = new Scanner(versionFile)) {
      serverVersion = scanner.useDelimiter("\\Z").next();

    } catch (IOException ioe) {
      throw new AmbariException("Unable to read server version file.");
    }

    if (schemaVersionEntity==null || VersionUtils.compareVersions(schemaVersion, serverVersion, 3) != 0) {
      String error = "Current database store version is not compatible with " +
              "current server version"
              + ", serverVersion=" + serverVersion
              + ", schemaVersion=" + schemaVersion;
      LOG.error(error);
      throw new AmbariException(error);
    }

    LOG.info("DB store version is compatible");
  }

  public static void checkForNotMappedConfigsToCluster() {
    LOG.info("Checking for configs not mapped to any cluster");

    String GET_NOT_MAPPED_CONFIGS_QUERY = "select type_name from clusterconfig where type_name not in (select type_name from clusterconfigmapping)";
    Set<String> nonSelectedConfigs = new HashSet<>();
    ResultSet rs = null;
    Statement statement = null;

    if (connection == null) {
      if (dbAccessor == null) {
        dbAccessor = injector.getInstance(DBAccessor.class);
      }
      connection = dbAccessor.getConnection();
    }

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(GET_NOT_MAPPED_CONFIGS_QUERY);
      if (rs != null) {
        while (rs.next()) {
          nonSelectedConfigs.add(rs.getString("type_name"));
        }
      }
      if (!nonSelectedConfigs.isEmpty()) {
        LOG.warn("You have config(s): {} that is(are) not mapped (in clusterconfigmapping table) to any cluster!", StringUtils.join(nonSelectedConfigs, ","));
        warningsFound = true;
      }
    } catch (SQLException e) {
      LOG.error("Exception occurred during check for not mapped configs to cluster procedure: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }
  }

  /**
  * This method checks if any config type in clusterconfigmapping table, has
  * more than one versions selected. If config version is selected(in selected column = 1),
  * it means that this version of config is actual. So, if any config type has more
  * than one selected version it's a bug and we are showing error message for user.
  * */
  public static void checkForConfigsSelectedMoreThanOnce() {
    LOG.info("Checking for configs selected more than once");

    String GET_CONFIGS_SELECTED_MORE_THAN_ONCE_QUERY = "select c.cluster_name, ccm.type_name from clusterconfigmapping ccm " +
            "join clusters c on ccm.cluster_id=c.cluster_id " +
            "group by c.cluster_name, ccm.type_name " +
            "having sum(selected) > 1";
    Multimap<String, String> clusterConfigTypeMap = HashMultimap.create();
    ResultSet rs = null;
    Statement statement = null;

    if (connection == null) {
      if (dbAccessor == null) {
        dbAccessor = injector.getInstance(DBAccessor.class);
      }
      connection = dbAccessor.getConnection();
    }

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(GET_CONFIGS_SELECTED_MORE_THAN_ONCE_QUERY);
      if (rs != null) {
        while (rs.next()) {
          clusterConfigTypeMap.put(rs.getString("cluster_name"), rs.getString("type_name"));
        }

        for (String clusterName : clusterConfigTypeMap.keySet()) {
          LOG.error("You have config(s), in cluster {}, that is(are) selected more than once in clusterconfigmapping table: {}",
                  clusterName ,StringUtils.join(clusterConfigTypeMap.get(clusterName), ","));
          errorsFound = true;
        }
      }

    } catch (SQLException e) {
      LOG.error("Exception occurred during check for config selected more than ones procedure: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }
  }

  /**
  * This method checks if all hosts from hosts table
  * has related host state info in hoststate table.
  * If not then we are showing error.
  * */
  public static void checkForHostsWithoutState() {
    LOG.info("Checking for hosts without state");

    String GET_HOSTS_WITHOUT_STATUS_QUERY = "select host_name from hosts where host_id not in (select host_id from hoststate)";
    Set<String> hostsWithoutStatus = new HashSet<>();
    ResultSet rs = null;
    Statement statement = null;

    if (connection == null) {
      if (dbAccessor == null) {
        dbAccessor = injector.getInstance(DBAccessor.class);
      }
      connection = dbAccessor.getConnection();
    }

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(GET_HOSTS_WITHOUT_STATUS_QUERY);
      if (rs != null) {
        while (rs.next()) {
          hostsWithoutStatus.add(rs.getString("host_name"));
        }

        if (!hostsWithoutStatus.isEmpty()) {
          LOG.error("You have host(s) without state (in hoststate table): " + StringUtils.join(hostsWithoutStatus, ","));
          errorsFound = true;
        }
      }

    } catch (SQLException e) {
      LOG.error("Exception occurred during check for host without state procedure: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }
  }

  /**
  * This method checks if count of host component states equals count
  * of desired host component states. According to ambari logic these
  * two tables should have the same count of rows. If not then we are
  * showing error for user.
  *
  * One more, we are checking if all components has only one host
  * component state. If some component has more, it can cause issues
  * */
  public static void checkHostComponentStates() {
    LOG.info("Checking host component states count equals host component desired states count");

    String GET_HOST_COMPONENT_STATE_COUNT_QUERY = "select count(*) from hostcomponentstate";
    String GET_HOST_COMPONENT_DESIRED_STATE_COUNT_QUERY = "select count(*) from hostcomponentdesiredstate";
    String GET_MERGED_TABLE_ROW_COUNT_QUERY = "select count(*) FROM hostcomponentstate hcs " +
            "JOIN hostcomponentdesiredstate hcds ON hcs.service_name=hcds.service_name AND hcs.component_name=hcds.component_name AND hcs.host_id=hcds.host_id";
    String GET_HOST_COMPONENT_STATE_DUPLICATES_QUERY = "select component_name, host_id from hostcomponentstate group by component_name, host_id having count(component_name) > 1";
    int hostComponentStateCount = 0;
    int hostComponentDesiredStateCount = 0;
    Map<String, String> hostComponentStateDuplicates = new HashMap<>();
    int mergedCount = 0;
    ResultSet rs = null;
    Statement statement = null;

    if (connection == null) {
      if (dbAccessor == null) {
        dbAccessor = injector.getInstance(DBAccessor.class);
      }
      connection = dbAccessor.getConnection();
    }

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

      rs = statement.executeQuery(GET_HOST_COMPONENT_STATE_COUNT_QUERY);
      if (rs != null) {
        while (rs.next()) {
          hostComponentStateCount = rs.getInt(1);
        }
      }

      rs = statement.executeQuery(GET_HOST_COMPONENT_DESIRED_STATE_COUNT_QUERY);
      if (rs != null) {
        while (rs.next()) {
          hostComponentDesiredStateCount = rs.getInt(1);
        }
      }

      rs = statement.executeQuery(GET_MERGED_TABLE_ROW_COUNT_QUERY);
      if (rs != null) {
        while (rs.next()) {
          mergedCount = rs.getInt(1);
        }
      }

      if (hostComponentStateCount != hostComponentDesiredStateCount || hostComponentStateCount != mergedCount) {
        LOG.error("Your host component states (hostcomponentstate table) count not equals host component desired states (hostcomponentdesiredstate table) count!");
        errorsFound = true;
      }


      rs = statement.executeQuery(GET_HOST_COMPONENT_STATE_DUPLICATES_QUERY);
      if (rs != null) {
        while (rs.next()) {
          hostComponentStateDuplicates.put(rs.getString("component_name"), rs.getString("host_id"));
        }
      }

      for (Map.Entry<String, String> component : hostComponentStateDuplicates.entrySet()) {
        LOG.error("Component {} on host with id {}, has more than one host component state (hostcomponentstate table)!", component.getKey(), component.getValue());
        errorsFound = true;
      }

    } catch (SQLException e) {
      LOG.error("Exception occurred during check for same count of host component states and host component desired states: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }

  }

  /**
  * Remove configs that are not mapped to any service.
  */
  @Transactional
  public static void fixClusterConfigsNotMappedToAnyService() {
    LOG.info("Checking for configs not mapped to any Service");
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    List<ClusterConfigEntity> notMappedClusterConfigs = getNotMappedClusterConfigsToService();

    for (ClusterConfigEntity clusterConfigEntity : notMappedClusterConfigs){
      List<String> types = new ArrayList<>();
      String type = clusterConfigEntity.getType();
      types.add(type);
      LOG.error("Removing cluster config mapping of type {} that is not mapped to any service", type);
      clusterDAO.removeClusterConfigMappingEntityByTypes(clusterConfigEntity.getClusterId(),types);
      LOG.error("Removing config that is not mapped to any service", clusterConfigEntity);
      clusterDAO.removeConfig(clusterConfigEntity);
    }
  }


  /**
   * Find ClusterConfigs that are not mapped to Service
   * @return ClusterConfigs that are not mapped to Service
   */
  private static List<ClusterConfigEntity> getNotMappedClusterConfigsToService() {
    Provider<EntityManager> entityManagerProvider = injector.getProvider(EntityManager.class);
    EntityManager entityManager = entityManagerProvider.get();

    String queryName = "ClusterConfigEntity.findNotMappedClusterConfigsToService";
    TypedQuery<ClusterConfigEntity> query = entityManager.createNamedQuery(queryName, ClusterConfigEntity.class);

    return query.getResultList();
  }

  /**
   * Look for configs that are not mapped to any service.
   */
  public static void checkForConfigsNotMappedToService() {
    LOG.info("Checking for configs that are not mapped to any service");
    List<ClusterConfigEntity> notMappedClasterConfigs = getNotMappedClusterConfigsToService();

    Set<String> nonMappedConfigs = new HashSet<>();
    for (ClusterConfigEntity clusterConfigEntity : notMappedClasterConfigs) {
      nonMappedConfigs.add(clusterConfigEntity.getType() + '-' + clusterConfigEntity.getTag());
    }
    if (!notMappedClasterConfigs.isEmpty()){
      LOG.warn("You have config(s): {} that is(are) not mapped (in serviceconfigmapping table) to any service!", StringUtils.join(nonMappedConfigs, ","));
      warningsFound = true;
    }
  }

  /**
  * This method checks if count of host component states equals count
  * of desired host component states. According to ambari logic these
  * two tables should have the same count of rows. If not then we are
  * adding missed host components.
  */
  @Transactional
  public static void fixHostComponentStatesCountEqualsHostComponentsDesiredStates() {
    LOG.info("Checking that there are the same number of actual and desired host components");

    HostComponentStateDAO hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
    HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);

    List<HostComponentDesiredStateEntity> hostComponentDesiredStates = hostComponentDesiredStateDAO.findAll();
    List<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findAll();

    Set<HostComponentDesiredStateEntity> missedHostComponentDesiredStates = new HashSet<>();
    missedHostComponentDesiredStates.addAll(hostComponentDesiredStates);
    Set<HostComponentStateEntity> missedHostComponentStates = new HashSet<>();
    missedHostComponentStates.addAll(hostComponentStates);

    for (Iterator<HostComponentStateEntity> stateIterator = missedHostComponentStates.iterator(); stateIterator.hasNext();){
      HostComponentStateEntity hostComponentStateEntity = stateIterator.next();
      for (Iterator<HostComponentDesiredStateEntity> desiredStateIterator = missedHostComponentDesiredStates.iterator(); desiredStateIterator.hasNext();) {
        HostComponentDesiredStateEntity hostComponentDesiredStateEntity = desiredStateIterator.next();
        if (hostComponentStateEntity.getComponentName().equals(hostComponentDesiredStateEntity.getComponentName()) &&
            hostComponentStateEntity.getServiceName().equals(hostComponentDesiredStateEntity.getServiceName()) &&
            hostComponentStateEntity.getHostId().equals(hostComponentDesiredStateEntity.getHostId())){
          desiredStateIterator.remove();
          stateIterator.remove();
          break;
        }
      }
    }

    for (HostComponentDesiredStateEntity hostComponentDesiredStateEntity : missedHostComponentDesiredStates) {
      HostComponentStateEntity stateEntity = new HostComponentStateEntity();
      stateEntity.setClusterId(hostComponentDesiredStateEntity.getClusterId());
      stateEntity.setComponentName(hostComponentDesiredStateEntity.getComponentName());
      stateEntity.setServiceName(hostComponentDesiredStateEntity.getServiceName());
      stateEntity.setVersion(State.UNKNOWN.toString());
      stateEntity.setHostEntity(hostComponentDesiredStateEntity.getHostEntity());
      stateEntity.setCurrentState(State.UNKNOWN);
      stateEntity.setUpgradeState(UpgradeState.NONE);
      stateEntity.setCurrentStack(hostComponentDesiredStateEntity.getDesiredStack());
      stateEntity.setSecurityState(SecurityState.UNKNOWN);
      stateEntity.setServiceComponentDesiredStateEntity(hostComponentDesiredStateEntity.getServiceComponentDesiredStateEntity());

      LOG.error("Trying to add missing record in hostcomponentstate: {}", stateEntity);
      hostComponentStateDAO.create(stateEntity);
    }

    for (HostComponentStateEntity missedHostComponentState : missedHostComponentStates) {

      HostComponentDesiredStateEntity stateEntity = new HostComponentDesiredStateEntity();
      stateEntity.setClusterId(missedHostComponentState.getClusterId());
      stateEntity.setComponentName(missedHostComponentState.getComponentName());
      stateEntity.setServiceName(missedHostComponentState.getServiceName());
      stateEntity.setHostEntity(missedHostComponentState.getHostEntity());
      stateEntity.setDesiredState(State.UNKNOWN);
      stateEntity.setDesiredStack(missedHostComponentState.getCurrentStack());
      stateEntity.setServiceComponentDesiredStateEntity(missedHostComponentState.getServiceComponentDesiredStateEntity());

      LOG.error("Trying to add missing record in hostcomponentdesiredstate: {}", stateEntity);
      hostComponentDesiredStateDAO.create(stateEntity);
    }
  }

  /**
  * This method checks db schema name for Postgres.
  * */
  public static void checkSchemaName () {
    Configuration conf = injector.getInstance(Configuration.class);
    if(conf.getDatabaseType()!=Configuration.DatabaseType.POSTGRES) {
      return;
    }
    LOG.info("Ensuring that the schema set for Postgres is correct");
    if (connection == null) {
      if (dbAccessor == null) {
        dbAccessor = injector.getInstance(DBAccessor.class);
      }
      connection = dbAccessor.getConnection();
    }
    ResultSet rs = null;
    try {
      DatabaseMetaData databaseMetaData = connection.getMetaData();

      rs = databaseMetaData.getSchemas();

      boolean ambariSchemaPresent = false;
      if (rs != null) {
        while (rs.next()) {
          if(StringUtils.equals(rs.getString("TABLE_SCHEM"),conf.getDatabaseSchema())){
            ambariSchemaPresent = true;
            break;
          }
        }
      }
      if (!ambariSchemaPresent){
        LOG.error("The schema %s defined for Ambari from ambari.properties has not been found in the database. " +
          "This means that the Ambari tables are stored under the public schema which can lead to problems.", conf.getDatabaseSchema());
        warningsFound = true;
      }

    } catch (SQLException e) {
      LOG.error("Exception occurred during checking db schema name.: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }
    }
  }

  /**
  * This method checks tables engine type to be innodb for MySQL.
  * */
  public static void checkMySQLEngine () {
    Configuration conf = injector.getInstance(Configuration.class);
    if(conf.getDatabaseType()!=Configuration.DatabaseType.MYSQL) {
      return;
    }
    LOG.info("Checking to ensure that the MySQL DB engine type is set to InnoDB");
    if (connection == null) {
      if (dbAccessor == null) {
        dbAccessor = injector.getInstance(DBAccessor.class);
      }
      connection = dbAccessor.getConnection();
    }

    String GET_INNODB_ENGINE_SUPPORT = "select TABLE_NAME, ENGINE from information_schema.tables where TABLE_SCHEMA = '%s' and LOWER(ENGINE) != 'innodb';";

    ResultSet rs = null;
    Statement statement;

    try {
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
      rs = statement.executeQuery(String.format(GET_INNODB_ENGINE_SUPPORT, conf.getDatabaseSchema()));
      if (rs != null) {
        List<String> tablesInfo = new ArrayList<>();
        while (rs.next()) {
          errorsFound = true;
          tablesInfo.add(rs.getString("TABLE_NAME"));
        }
        if (!tablesInfo.isEmpty()){
          LOG.error("Found tables with engine type that is not InnoDB : %s", StringUtils.join(tablesInfo, ','));
        }
      }
    } catch (SQLException e) {
      LOG.error("Exception occurred during checking MySQL engine to be innodb: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }
    }
  }

  /**
  * This method checks several potential problems for services:
  * 1) Check if we have services in cluster which doesn't have service config id(not available in serviceconfig table).
  * 2) Check if service has no mapped configs to it's service config id.
  * 3) Check if service has all required configs mapped to it.
  * 4) Check if service has config which is not selected(has no actual config version) in clusterconfigmapping table.
  * If any issue was discovered, we are showing error message for user.
  * */
  public static void checkServiceConfigs()  {
    LOG.info("Checking services and their configs");

    String GET_SERVICES_WITHOUT_CONFIGS_QUERY = "select c.cluster_name, service_name from clusterservices cs " +
            "join clusters c on cs.cluster_id=c.cluster_id " +
            "where service_name not in (select service_name from serviceconfig sc where sc.cluster_id=cs.cluster_id and sc.service_name=cs.service_name and sc.group_id is null)";
    String GET_SERVICE_CONFIG_WITHOUT_MAPPING_QUERY = "select c.cluster_name, sc.service_name, sc.version from serviceconfig sc " +
            "join clusters c on sc.cluster_id=c.cluster_id " +
            "where service_config_id not in (select service_config_id from serviceconfigmapping) and group_id is null";
    String GET_STACK_NAME_VERSION_QUERY = "select c.cluster_name, s.stack_name, s.stack_version from clusters c " +
            "join stack s on c.desired_stack_id = s.stack_id";
    String GET_SERVICES_WITH_CONFIGS_QUERY = "select c.cluster_name, cs.service_name, cc.type_name, sc.version from clusterservices cs " +
            "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
            "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
            "join clusterconfig cc on scm.config_id=cc.config_id and sc.cluster_id=cc.cluster_id " +
            "join clusters c on cc.cluster_id=c.cluster_id and sc.stack_id=c.desired_stack_id " +
            "where sc.group_id is null and sc.service_config_id=(select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
            "group by c.cluster_name, cs.service_name, cc.type_name, sc.version";
    String GET_NOT_SELECTED_SERVICE_CONFIGS_QUERY = "select c.cluster_name, cs.service_name, cc.type_name from clusterservices cs " +
            "join serviceconfig sc on cs.service_name=sc.service_name and cs.cluster_id=sc.cluster_id " +
            "join serviceconfigmapping scm on sc.service_config_id=scm.service_config_id " +
            "join clusterconfig cc on scm.config_id=cc.config_id and cc.cluster_id=sc.cluster_id " +
            "join clusterconfigmapping ccm on cc.type_name=ccm.type_name and cc.version_tag=ccm.version_tag and cc.cluster_id=ccm.cluster_id " +
            "join clusters c on ccm.cluster_id=c.cluster_id " +
            "where sc.group_id is null and sc.service_config_id = (select max(service_config_id) from serviceconfig sc2 where sc2.service_name=sc.service_name and sc2.cluster_id=sc.cluster_id) " +
            "group by c.cluster_name, cs.service_name, cc.type_name " +
            "having sum(ccm.selected) < 1";
    Multimap<String, String> clusterServiceMap = HashMultimap.create();
    Map<String, Map<String, String>>  clusterStackInfo = new HashMap<>();
    Map<String, Multimap<String, String>> clusterServiceVersionMap = new HashMap<>();
    Map<String, Multimap<String, String>> clusterServiceConfigType = new HashMap<>();
    ResultSet rs = null;
    Statement statement = null;

    if (connection == null) {
      if (dbAccessor == null) {
        dbAccessor = injector.getInstance(DBAccessor.class);
      }
      connection = dbAccessor.getConnection();
    }

    LOG.info("Getting ambari metainfo instance");
    if (ambariMetaInfo == null) {
      ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    }

    try {
      LOG.info("Executing query 'GET_SERVICES_WITHOUT_CONFIGS'");
      statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);

      rs = statement.executeQuery(GET_SERVICES_WITHOUT_CONFIGS_QUERY);
      if (rs != null) {
        while (rs.next()) {
          clusterServiceMap.put(rs.getString("cluster_name"), rs.getString("service_name"));
        }

        for (String clusterName : clusterServiceMap.keySet()) {
          LOG.warn("Service(s): {}, from cluster {} has no config(s) in serviceconfig table!", StringUtils.join(clusterServiceMap.get(clusterName), ","), clusterName);
          warningsFound = true;
        }

      }
      LOG.info("Executing query 'GET_SERVICE_CONFIG_WITHOUT_MAPPING'");
      rs = statement.executeQuery(GET_SERVICE_CONFIG_WITHOUT_MAPPING_QUERY);
      if (rs != null) {
        String serviceName = null, version = null, clusterName = null;
        while (rs.next()) {
          serviceName = rs.getString("service_name");
          clusterName = rs.getString("cluster_name");
          version = rs.getString("version");

          if (clusterServiceVersionMap.get(clusterName) != null) {
            Multimap<String, String> serviceVersion = clusterServiceVersionMap.get(clusterName);
            serviceVersion.put(serviceName, version);
          } else {
            Multimap<String, String> serviceVersion = HashMultimap.create();;
            serviceVersion.put(serviceName, version);
            clusterServiceVersionMap.put(clusterName, serviceVersion);
          }
        }

        for (String clName : clusterServiceVersionMap.keySet()) {
          Multimap<String, String> serviceVersion = clusterServiceVersionMap.get(clName);
          for (String servName : serviceVersion.keySet()) {
            LOG.error("In cluster {}, service config mapping is unavailable (in table serviceconfigmapping) for service {} with version(s) {}! ", clName, servName, StringUtils.join(serviceVersion.get(servName), ","));
            errorsFound = true;
          }
        }

      }

      //get stack info from db
      LOG.info("Getting stack info from database");
      rs = statement.executeQuery(GET_STACK_NAME_VERSION_QUERY);
      if (rs != null) {
        while (rs.next()) {
          Map<String, String> stackInfoMap = new HashMap<>();
          stackInfoMap.put(rs.getString("stack_name"), rs.getString("stack_version"));
          clusterStackInfo.put(rs.getString("cluster_name"), stackInfoMap);
        }
      }


      Set<String> serviceNames = new HashSet<>();
      Map<String, Map<Integer, Multimap<String, String>>> dbClusterServiceVersionConfigs = new HashMap<>();
      Multimap<String, String> stackServiceConfigs = HashMultimap.create();

      LOG.info("Executing query 'GET_SERVICES_WITH_CONFIGS'");
      rs = statement.executeQuery(GET_SERVICES_WITH_CONFIGS_QUERY);
      if (rs != null) {
        String serviceName = null, configType = null, clusterName = null;
        Integer serviceVersion = null;
        while (rs.next()) {
          clusterName = rs.getString("cluster_name");
          serviceName = rs.getString("service_name");
          configType = rs.getString("type_name");
          serviceVersion = rs.getInt("version");

          serviceNames.add(serviceName);

          //collect data about mapped configs to services from db
          if (dbClusterServiceVersionConfigs.get(clusterName) != null) {
            Map<Integer, Multimap<String, String>> dbServiceVersionConfigs = dbClusterServiceVersionConfigs.get(clusterName);

            if (dbServiceVersionConfigs.get(serviceVersion) != null) {
              dbServiceVersionConfigs.get(serviceVersion).put(serviceName, configType);
            } else {
              Multimap<String, String> dbServiceConfigs = HashMultimap.create();
              dbServiceConfigs.put(serviceName, configType);
              dbServiceVersionConfigs.put(serviceVersion, dbServiceConfigs);
            }
          } else {
            Map<Integer, Multimap<String, String>> dbServiceVersionConfigs = new HashMap<>();
            Multimap<String, String> dbServiceConfigs = HashMultimap.create();
            dbServiceConfigs.put(serviceName, configType);
            dbServiceVersionConfigs.put(serviceVersion, dbServiceConfigs);
            dbClusterServiceVersionConfigs.put(clusterName, dbServiceVersionConfigs);
          }
        }
      }

      //compare service configs from stack with configs that we got from db
      LOG.info("Comparing service configs from stack with configs that we got from db");
      for (Map.Entry<String, Map<String, String>> clusterStackInfoEntry : clusterStackInfo.entrySet()) {
        //collect required configs for all services from stack
        String clusterName = clusterStackInfoEntry.getKey();
        Map<String, String> stackInfo = clusterStackInfoEntry.getValue();
        String stackName = stackInfo.keySet().iterator().next();
        String stackVersion = stackInfo.get(stackName);
        LOG.info("Getting services from metainfo");
        Map<String, ServiceInfo> serviceInfoMap = ambariMetaInfo.getServices(stackName, stackVersion);
        for (String serviceName : serviceNames) {
          LOG.info("Processing {}-{} / {}", stackName, stackVersion, serviceName);
          ServiceInfo serviceInfo = serviceInfoMap.get(serviceName);
          if (serviceInfo != null) {
            Set<String> configTypes = serviceInfo.getConfigTypeAttributes().keySet();
            for (String configType : configTypes) {
              stackServiceConfigs.put(serviceName, configType);
            }
          } else {
            LOG.warn("Service {} is not available for stack {} in cluster {}",
                    serviceName, stackName + "-" + stackVersion, clusterName);
            warningsFound = true;
          }
        }

        //compare required service configs from stack with mapped service configs from db
        LOG.info("Comparing required service configs from stack with mapped service configs from db");
        Map<Integer, Multimap<String, String>> dbServiceVersionConfigs = dbClusterServiceVersionConfigs.get(clusterName);
        if (dbServiceVersionConfigs != null) {
          for (Integer serviceVersion : dbServiceVersionConfigs.keySet()) {
            Multimap<String, String> dbServiceConfigs = dbServiceVersionConfigs.get(serviceVersion);
            if (dbServiceConfigs != null) {
              for (String serviceName : dbServiceConfigs.keySet()) {
                Collection<String> serviceConfigsFromStack = stackServiceConfigs.get(serviceName);
                Collection<String> serviceConfigsFromDB = dbServiceConfigs.get(serviceName);
                if (serviceConfigsFromDB != null && serviceConfigsFromStack != null) {
                  serviceConfigsFromStack.removeAll(serviceConfigsFromDB);
                  if (!serviceConfigsFromStack.isEmpty()) {
                    LOG.error("Required config(s): {} is(are) not available for service {} with service config version {} in cluster {}",
                            StringUtils.join(serviceConfigsFromStack, ","), serviceName, Integer.toString(serviceVersion), clusterName);
                    errorsFound = true;
                  }
                }
              }
            }
          }
        }
      }

      //getting services which has mapped configs which are not selected in clusterconfigmapping
      LOG.info("Getting services which has mapped configs which are not selected in clusterconfigmapping");
      rs = statement.executeQuery(GET_NOT_SELECTED_SERVICE_CONFIGS_QUERY);
      if (rs != null) {
        String serviceName = null, configType = null, clusterName = null;
        while (rs.next()) {
          clusterName = rs.getString("cluster_name");
          serviceName = rs.getString("service_name");
          configType = rs.getString("type_name");


          if (clusterServiceConfigType.get(clusterName) != null) {
            Multimap<String, String> serviceConfigs = clusterServiceConfigType.get(clusterName);
            serviceConfigs.put(serviceName, configType);
          } else {

            Multimap<String, String> serviceConfigs = HashMultimap.create();
            serviceConfigs.put(serviceName, configType);
            clusterServiceConfigType.put(clusterName, serviceConfigs);

          }

        }
      }

      for (String clusterName : clusterServiceConfigType.keySet()) {
        Multimap<String, String> serviceConfig = clusterServiceConfigType.get(clusterName);
        for (String serviceName : serviceConfig.keySet()) {
          LOG.error("You have non selected configs: {} for service {} from cluster {}!", StringUtils.join(serviceConfig.get(serviceName), ","), serviceName, clusterName);
          errorsFound = true;
        }
      }
    } catch (SQLException e) {
      LOG.error("Exception occurred during complex service check procedure: ", e);
    } catch (AmbariException e) {
      LOG.error("Exception occurred during complex service check procedure: ", e);
    } finally {
      if (rs != null) {
        try {
          rs.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during result set closing procedure: ", e);
        }
      }

      if (statement != null) {
        try {
          statement.close();
        } catch (SQLException e) {
          LOG.error("Exception occurred during statement closing procedure: ", e);
        }
      }
    }

  }


}
