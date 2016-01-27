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
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.ambari.server.utils.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class SchemaUpgradeHelper {
  private static final Logger LOG = LoggerFactory.getLogger
    (SchemaUpgradeHelper.class);

  private Set<UpgradeCatalog> allUpgradeCatalogs;
  private PersistService persistService;
  private DBAccessor dbAccessor;
  private Configuration configuration;

  @Inject
  public SchemaUpgradeHelper(Set<UpgradeCatalog> allUpgradeCatalogs,
                             PersistService persistService,
                             DBAccessor dbAccessor,
                             Configuration configuration) {
    this.allUpgradeCatalogs = allUpgradeCatalogs;
    this.persistService = persistService;
    this.dbAccessor = dbAccessor;
    this.configuration = configuration;
  }

  public void startPersistenceService() {
    persistService.start();
  }

  public void stopPersistenceService() {
    persistService.stop();
  }

  public Set<UpgradeCatalog> getAllUpgradeCatalogs() {
    return allUpgradeCatalogs;
  }

  public String readSourceVersion() {

    Statement statement = null;
    ResultSet rs = null;
    try {
      statement = dbAccessor.getConnection().createStatement();
      if (statement != null) {
        rs = statement.executeQuery("SELECT " + dbAccessor.quoteObjectName("metainfo_value") +
          " from metainfo WHERE " + dbAccessor.quoteObjectName("metainfo_key") + "='version'");
        if (rs != null && rs.next()) {
          return rs.getString(1);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Unable to read database version", e);

    } finally {
      {
        if (rs != null) {
          try {
            rs.close();
          } catch (SQLException e) {
            throw new RuntimeException("Cannot close result set");
          }
        }
        if (statement != null) {
          try {
            statement.close();
          } catch (SQLException e) {
            throw new RuntimeException("Cannot close statement");
          }
        }
      }
    }
    //not found, assume oldest version
    //doesn't matter as there single upgrade catalog for 1.2.0 - 1.5.0 and 1.4.4 - 1.5.0 upgrades
    return "1.2.0";
  }

  /**
   * Read server version file
   * @return
   */
  protected String getAmbariServerVersion() {
    return configuration.getServerVersion();
  }

  /**
   * Return a set Upgrade catalogs to be applied to upgrade from
   * @sourceVersion to @targetVersion
   *
   * @param sourceVersion
   * @param targetVersion
   * @return
   * @throws org.apache.ambari.server.AmbariException
   */
  protected List<UpgradeCatalog> getUpgradePath(String sourceVersion,
                                                       String targetVersion) throws AmbariException {
    List<UpgradeCatalog> upgradeCatalogs = new ArrayList<UpgradeCatalog>();
    List<UpgradeCatalog> candidateCatalogs = new ArrayList<UpgradeCatalog>(allUpgradeCatalogs);

    Collections.sort(candidateCatalogs, new AbstractUpgradeCatalog.VersionComparator());

    for (UpgradeCatalog upgradeCatalog : candidateCatalogs) {
      if (sourceVersion == null || VersionUtils.compareVersions(sourceVersion,
        upgradeCatalog.getTargetVersion(), 4) < 0) {
        // catalog version is newer than source
        if (VersionUtils.compareVersions(upgradeCatalog.getTargetVersion(),
          targetVersion, 4) <= 0) {
          // catalog version is older or equal to target
          upgradeCatalogs.add(upgradeCatalog);
        }
      }
    }

    LOG.info("Upgrade path: " + upgradeCatalogs);

    return upgradeCatalogs;
  }

  /**
   * Extension of main controller module
   */
  public static class UpgradeHelperModule extends ControllerModule {

    public UpgradeHelperModule() throws Exception {
    }

    public UpgradeHelperModule(Properties properties) throws Exception {
      super(properties);
    }

    @Override
    protected void configure() {
      super.configure();
      // Add binding to each newly created catalog
      Multibinder<UpgradeCatalog> catalogBinder =
        Multibinder.newSetBinder(binder(), UpgradeCatalog.class);
      catalogBinder.addBinding().to(UpgradeCatalog150.class);
      catalogBinder.addBinding().to(UpgradeCatalog151.class);
      catalogBinder.addBinding().to(UpgradeCatalog160.class);
      catalogBinder.addBinding().to(UpgradeCatalog161.class);
      catalogBinder.addBinding().to(UpgradeCatalog170.class);
      catalogBinder.addBinding().to(UpgradeCatalog200.class);
      catalogBinder.addBinding().to(UpgradeCatalog210.class);
      catalogBinder.addBinding().to(UpgradeCatalog211.class);
      catalogBinder.addBinding().to(UpgradeCatalog212.class);
      catalogBinder.addBinding().to(UpgradeCatalog2121.class);
      catalogBinder.addBinding().to(UpgradeCatalog220.class);
      catalogBinder.addBinding().to(UpgradeCatalog221.class);
      catalogBinder.addBinding().to(UpgradeCatalog222.class);
      catalogBinder.addBinding().to(FinalUpgradeCatalog.class);

      EventBusSynchronizer.synchronizeAmbariEventPublisher(binder());
    }
  }

  public void executeUpgrade(List<UpgradeCatalog> upgradeCatalogs) throws AmbariException {
    LOG.info("Executing DDL upgrade...");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.upgradeSchema();
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  public void executePreDMLUpdates(List<UpgradeCatalog> upgradeCatalogs) throws AmbariException {
    LOG.info("Executing Pre-DML changes.");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.preUpgradeData();
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  public void executeDMLUpdates(List<UpgradeCatalog> upgradeCatalogs) throws AmbariException {
    LOG.info("Executing DML changes.");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.upgradeData();
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  public void executeOnPostUpgrade(List<UpgradeCatalog> upgradeCatalogs)
      throws AmbariException {
    LOG.info("Finalizing catalog upgrade.");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.onPostUpgrade();
          upgradeCatalog.updateDatabaseSchemaVersion();
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  public void resetUIState() throws AmbariException {
    LOG.info("Resetting UI state.");
    try {
      dbAccessor.updateTable("key_value_store", dbAccessor.quoteObjectName("value"),
          "{\"clusterState\":\"CLUSTER_STARTED_5\"}",
          "where " + dbAccessor.quoteObjectName("key") + "='CLUSTER_CURRENT_STATUS'");
    } catch (SQLException e) {
      throw new AmbariException("Unable to reset UI state", e);
    }
  }

  /**
   * Upgrade Ambari DB schema to the target version passed in as the only
   * argument.
   * @param args args[0] = target version to upgrade to.
   */
  public static void main(String[] args) throws Exception {
    try {
      // check java version to be higher then 1.6
      String[] splittedJavaVersion = System.getProperty("java.version").split("\\.");
      float javaVersion = Float.parseFloat(splittedJavaVersion[0] + "." + splittedJavaVersion[1]);
      if (javaVersion < Configuration.JDK_MIN_VERSION) {
        LOG.error(String.format("Oracle JDK version is lower than %.1f It can cause problems during upgrade process. Please," +
                " use 'ambari-server setup' command to upgrade JDK!", Configuration.JDK_MIN_VERSION));
        System.exit(1);
      }

      Injector injector = Guice.createInjector(new UpgradeHelperModule());
      SchemaUpgradeHelper schemaUpgradeHelper = injector.getInstance(SchemaUpgradeHelper.class);

      String targetVersion = schemaUpgradeHelper.getAmbariServerVersion();
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

      schemaUpgradeHelper.executePreDMLUpdates(upgradeCatalogs);

      schemaUpgradeHelper.executeDMLUpdates(upgradeCatalogs);

      schemaUpgradeHelper.executeOnPostUpgrade(upgradeCatalogs);

      schemaUpgradeHelper.resetUIState();

      LOG.info("Upgrade successful.");

      schemaUpgradeHelper.stopPersistenceService();
    } catch (Throwable e) {
      if (e instanceof AmbariException) {
        LOG.error("Exception occurred during upgrade, failed", e);
        throw (AmbariException)e;
      }else{
        LOG.error("Unexpected error, upgrade failed", e);
        throw new Exception("Unexpected error, upgrade failed", e);
      }
    }
  }
}
