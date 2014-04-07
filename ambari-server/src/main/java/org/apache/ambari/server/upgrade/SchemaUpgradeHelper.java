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
import org.apache.ambari.server.utils.VersionUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

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

    ResultSet resultSet = null;
    try {
      resultSet = dbAccessor.executeSelect("SELECT \"metainfo_value\" from metainfo WHERE \"metainfo_key\"='version'");
      if (resultSet.next()) {
        return resultSet.getString(1);
      } else {
        //not found, assume oldest version
        //doesn't matter as there single upgrade catalog for 1.2.0 - 1.5.0 and 1.4.4 - 1.5.0 upgrades
        return "1.2.0";
      }
    } catch (SQLException e) {
      throw new RuntimeException("Unable to read database version", e);
    }finally {
      if (resultSet != null) {
        try {
          resultSet.close();
        } catch (SQLException e) {
          throw new RuntimeException("Cannot close result set");
        }
      }
    }

  }

  /**
   * Read server version file
   * @return
   */
  protected String getAmbariServerVersion() {
    String versionFilePath = configuration.getServerVersionFilePath();
    try {
      return FileUtils.readFileToString(new File(versionFilePath));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
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
        upgradeCatalog.getTargetVersion(), 3) < 0) {
        // catalog version is newer than source
        if (VersionUtils.compareVersions(upgradeCatalog.getTargetVersion(),
          targetVersion, 3) <= 0) {
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
    }
  }

  public void executeUpgrade(List<UpgradeCatalog> upgradeCatalogs) throws AmbariException {
    LOG.info("Executing DDL upgrade...");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.upgradeSchema();
        } catch (AmbariException e) {
          LOG.error("Upgrade failed. ", e);
          throw e;
        } catch (SQLException e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  public void executeDMLUpdates(List<UpgradeCatalog> upgradeCatalogs) throws AmbariException {
    LOG.info("Execution DML changes.");

    if (upgradeCatalogs != null && !upgradeCatalogs.isEmpty()) {
      for (UpgradeCatalog upgradeCatalog : upgradeCatalogs) {
        try {
          upgradeCatalog.upgradeData();
        } catch (AmbariException e) {
          LOG.error("Upgrade failed. ", e);
          throw e;
        } catch (SQLException e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage());
        } catch (Exception e) {
          LOG.error("Upgrade failed. ", e);
          throw new AmbariException(e.getMessage(), e);
        }
      }
    }
  }

  /**
   * Upgrade Ambari DB schema to the target version passed in as the only
   * argument.
   * @param args args[0] = target version to upgrade to.
   */
  public static void main(String[] args) throws Exception {
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

    schemaUpgradeHelper.executeDMLUpdates(upgradeCatalogs);

    LOG.info("Upgrade successful.");

    schemaUpgradeHelper.stopPersistenceService();
  }
}
