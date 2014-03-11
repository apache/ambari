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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.*;
import org.apache.ambari.server.security.CertificateManager;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.utils.VersionUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.*;

import static org.junit.Assert.assertTrue;

public class UpgradeTest {
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeTest.class);

  private static String DDL_PATTERN = "ddl-scripts/Ambari-DDL-Derby-%s.sql";
  private static List<String> VERSIONS = Arrays.asList("1.4.4",
      "1.4.3", "1.4.2", "1.4.1", "1.4.0", "1.2.5", "1.2.4",
      "1.2.3"); //TODO add all
  private static String DROP_DERBY_URL = "jdbc:derby:memory:myDB/ambari;drop=true";
  private  Properties properties = new Properties();

  private Injector injector;

  public UpgradeTest() {
    properties.setProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY, "remote");
    properties.setProperty(Configuration.SERVER_JDBC_URL_KEY, Configuration.JDBC_IN_MEMORY_URL);
    properties.setProperty(Configuration.SERVER_JDBC_DRIVER_KEY, Configuration.JDBC_IN_MEMROY_DRIVER);
    properties.setProperty(Configuration.METADETA_DIR_PATH,
      "src/test/resources/stacks");
    properties.setProperty(Configuration.SERVER_VERSION_FILE,
      "target/version");
    properties.setProperty(Configuration.OS_VERSION_KEY,
      "centos5");
  }

  @Test
  public void testUpgrade() throws Exception {
    String targetVersion = getLastVersion();
    List<String> failedVersions = new ArrayList<String>();

    for (String version : VERSIONS) {
      injector = Guice.createInjector(new ControllerModule(properties));

      try {
        createSourceDatabase(version);

        performUpgrade(targetVersion);

        testUpgradedSchema();
      } catch (Exception e) {
        failedVersions.add(version);
        e.printStackTrace();
      }

      dropDatabase();

    }

    assertTrue("Upgrade test failed for version: " + failedVersions, failedVersions.isEmpty());


  }

  private void dropDatabase() throws ClassNotFoundException, SQLException {
    Class.forName(Configuration.JDBC_IN_MEMROY_DRIVER);
    try {
      DriverManager.getConnection(DROP_DERBY_URL);
    } catch (SQLNonTransientConnectionException ignored) {
      LOG.info("Database dropped ", ignored); //error 08006 expected
    }
  }

  private void testUpgradedSchema() throws Exception {
    injector = Guice.createInjector(new ControllerModule(properties));
    injector.getInstance(PersistService.class).start();

    //TODO join() in AmbariServer.run() prevents proper start testing, figure out

    //check dao selects
    //TODO generify DAOs for basic methods? deal with caching config group daos in such case
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    clusterDAO.findAll();
    BlueprintDAO blueprintDAO = injector.getInstance(BlueprintDAO.class);
    blueprintDAO.findAll();
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    clusterServiceDAO.findAll();
    injector.getInstance(ClusterStateDAO.class).findAll();
    injector.getInstance(ConfigGroupConfigMappingDAO.class).findAll();
    injector.getInstance(ConfigGroupDAO.class).findAll();
    injector.getInstance(ConfigGroupHostMappingDAO.class).findAll();
    injector.getInstance(ExecutionCommandDAO.class).findAll();
    injector.getInstance(HostComponentDesiredStateDAO.class).findAll();
    injector.getInstance(HostComponentStateDAO.class).findAll();
    injector.getInstance(HostConfigMappingDAO.class).findAll();
    injector.getInstance(HostDAO.class).findAll();
    injector.getInstance(HostRoleCommandDAO.class).findAll();
    injector.getInstance(HostStateDAO.class).findAll();
    injector.getInstance(KeyValueDAO.class).findAll();
    injector.getInstance(MetainfoDAO.class).findAll();
    RequestDAO requestDAO = injector.getInstance(RequestDAO.class);
    requestDAO.findAll();
    requestDAO.findAllResourceFilters();
    injector.getInstance(RequestScheduleBatchRequestDAO.class).findAll();
    injector.getInstance(RequestScheduleDAO.class).findAll();
    injector.getInstance(RoleDAO.class).findAll();
    injector.getInstance(RoleSuccessCriteriaDAO.class).findAll();
    injector.getInstance(ServiceComponentDesiredStateDAO.class).findAll();
    injector.getInstance(ServiceDesiredStateDAO.class).findAll();
    injector.getInstance(StageDAO.class).findAll();
    injector.getInstance(UserDAO.class).findAll();


    //TODO extend checks if needed
    injector.getInstance(PersistService.class).stop();


  }

  private void performUpgrade(String targetVersion) throws Exception {
    Injector injector = Guice.createInjector(new SchemaUpgradeHelper.UpgradeHelperModule(properties));
    SchemaUpgradeHelper schemaUpgradeHelper = injector.getInstance(SchemaUpgradeHelper.class);

    LOG.info("Upgrading schema to target version = " + targetVersion);

    UpgradeCatalog targetUpgradeCatalog = AbstractUpgradeCatalog
      .getUpgradeCatalog(targetVersion);

    LOG.debug("Target upgrade catalog. " + targetUpgradeCatalog);

    // Read source version from DB
    String sourceVersion = schemaUpgradeHelper.readSourceVersion();
    LOG.info("Upgrading schema from source version = " + sourceVersion);

    List<UpgradeCatalog> upgradeCatalogs =
      schemaUpgradeHelper.getUpgradePath(sourceVersion, targetVersion);

    schemaUpgradeHelper.executeUpgrade(upgradeCatalogs);

    schemaUpgradeHelper.startPersistenceService();

    schemaUpgradeHelper.executeDMLUpdates(upgradeCatalogs);

    LOG.info("Upgrade successful.");

    schemaUpgradeHelper.stopPersistenceService();

  }

  private String getLastVersion() throws Exception {
    Injector injector = Guice.createInjector(new SchemaUpgradeHelper.UpgradeHelperModule(properties));
    Set<UpgradeCatalog> upgradeCatalogs = injector.getInstance(Key.get(new TypeLiteral<Set<UpgradeCatalog>>() {
    }));
    String maxVersion = "1.2";
    for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
      String targetVersion = upgradeCatalog.getTargetVersion();
      if (VersionUtils.compareVersions(maxVersion, targetVersion) < 0) {
        maxVersion = targetVersion;
      }
    }
    return maxVersion;
  }

  private void createSourceDatabase(String version) throws IOException, SQLException {

    //create database
    String fileName = String.format(DDL_PATTERN, version);
    fileName = this.getClass().getClassLoader().getResource(fileName).getFile();
    DBAccessor dbAccessor = injector.getInstance(DBAccessor.class);
    dbAccessor.executeScript(fileName);

  }


}
