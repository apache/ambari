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
package org.apache.ambari.server.api.services;

import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.ViewDataMigrationContextImpl;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.view.migration.ViewDataMigrationContext;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.ambari.view.migration.ViewDataMigrator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Service responsible for data migration between view instances.
 */
public class ViewDataMigrationService extends BaseService {
  /**
   * Logger.
   */
  private static final Log LOG = LogFactory.getLog(ViewDataMigrationService.class);

  /**
   * The current view name.
   */
  private final String viewName;

  /**
   * The current view version.
   */
  private final String viewVersion;

  /**
   * The current view instance name.
   */
  private final String instanceName;

  private ViewRegistry viewRegistry;

  /**
   * Constructor.
   *
   * @param viewName       the current view name
   * @param viewVersion    the current view version
   * @param instanceName   the current view instance name
   */
  public ViewDataMigrationService(String viewName, String viewVersion, String instanceName) {
    this.viewName = viewName;
    this.viewVersion = viewVersion;
    this.instanceName = instanceName;
    this.viewRegistry = ViewRegistry.getInstance();
  }

  /**
   * Migrates view instance persistence data from origin view instance
   * specified in the path params.
   *
   * @param originViewVersion    the origin view version
   * @param originInstanceName   the origin view instance name
   */
  @PUT
  @Path("{originVersion}/{originInstanceName}")
  public Response migrateData(@PathParam("originVersion") String originViewVersion,
                              @PathParam("originInstanceName") String originInstanceName)
      throws ViewDataMigrationException {

    if (!viewRegistry.checkAdmin()) {
      throw new WebApplicationException(Response.Status.FORBIDDEN);
    }

    LOG.info("Data Migration to view instance " + viewName + "/" + viewVersion + "/" + instanceName +
        " from " + viewName + "/" + originViewVersion + "/" + originInstanceName);

    ViewInstanceEntity instanceDefinition = getViewInstanceEntity(viewName, viewVersion, instanceName);
    ViewInstanceEntity originInstanceDefinition = getViewInstanceEntity(viewName, originViewVersion, originInstanceName);

    ViewDataMigrationContextImpl migrationContext = getViewDataMigrationContext(instanceDefinition, originInstanceDefinition);

    ViewDataMigrator dataMigrator = getViewDataMigrator(instanceDefinition, migrationContext);

    LOG.debug("Running before-migration hook");
    if (!dataMigrator.beforeMigration()) {
      String msg = "View " + viewName + "/" + viewVersion + "/" + instanceName + " canceled the migration process";

      LOG.error(msg);
      throw new ViewDataMigrationException(msg);
    }

    Map<String, Class> originClasses = migrationContext.getOriginEntityClasses();
    Map<String, Class> currentClasses = migrationContext.getCurrentEntityClasses();
    for (Map.Entry<String, Class> originEntity : originClasses.entrySet()) {
      LOG.debug("Migrating persistence entity " + originEntity.getKey());
      if (currentClasses.containsKey(originEntity.getKey())) {
        Class entity = currentClasses.get(originEntity.getKey());
        dataMigrator.migrateEntity(originEntity.getValue(), entity);
      } else {
        LOG.debug("Entity " + originEntity.getKey() + " not found in target view");
        dataMigrator.migrateEntity(originEntity.getValue(), null);
      }
    }

    LOG.debug("Migrating instance data");
    dataMigrator.migrateInstanceData();

    LOG.debug("Running after-migration hook");
    dataMigrator.afterMigration();

    LOG.debug("Copying user permissions");
    viewRegistry.copyPrivileges(originInstanceDefinition, instanceDefinition);

    Response.ResponseBuilder builder = Response.status(Response.Status.OK);
    return builder.build();
  }

  protected ViewDataMigrationContextImpl getViewDataMigrationContext(ViewInstanceEntity instanceDefinition, ViewInstanceEntity originInstanceDefinition) {
    return new ViewDataMigrationContextImpl(
        originInstanceDefinition, instanceDefinition);
  }

  protected ViewInstanceEntity getViewInstanceEntity(String viewName, String viewVersion, String instanceName) {
    return viewRegistry.getInstanceDefinition(viewName, viewVersion, instanceName);
  }

  /**
   * Get the migrator instance for view instance with injected migration context.
   * If versions of instances are same returns copy-all-data migrator.
   * If versions are different, loads the migrator from the current view (view should
   * contain ViewDataMigrator implementation, otherwise exception will be raised).
   *
   * @param currentInstanceDefinition    the current view instance definition
   * @param migrationContext             the migration context to inject into migrator
   * @throws ViewDataMigrationException  if view does not support migration
   * @return  the data migration instance
   */
  protected ViewDataMigrator getViewDataMigrator(ViewInstanceEntity currentInstanceDefinition,
                                                 ViewDataMigrationContextImpl migrationContext)
      throws ViewDataMigrationException {
    ViewDataMigrator dataMigrator;

    LOG.info("Migrating " + viewName + "/" + viewVersion + "/" + instanceName +
        " data from " + migrationContext.getOriginDataVersion() + " to " +
        migrationContext.getCurrentDataVersion() + " data version");

    if (migrationContext.getOriginDataVersion() == migrationContext.getCurrentDataVersion()) {

      LOG.info("Instances of same version, copying all data.");
      dataMigrator = new CopyAllDataMigrator(migrationContext);
    } else {
      try {
        dataMigrator = currentInstanceDefinition.getDataMigrator(migrationContext);
        if (dataMigrator == null) {
          throw new ViewDataMigrationException("A view instance " +
              viewName + "/" + viewVersion + "/" + instanceName + " does not support migration.");
        }
        LOG.debug("Data migrator loaded");
      } catch (ClassNotFoundException e) {
        String msg = "Caught exception loading data migrator of " + viewName + "/" + viewVersion + "/" + instanceName;

        LOG.error(msg, e);
        throw new RuntimeException(msg);
      }
    }
    return dataMigrator;
  }

  /**
   * The data migrator implementation that copies all data without modification.
   * Used to copy data between instances of same version.
   */
  public static class CopyAllDataMigrator implements ViewDataMigrator {
    private ViewDataMigrationContext migrationContext;

    public CopyAllDataMigrator(ViewDataMigrationContext migrationContext) {
      this.migrationContext = migrationContext;
    }

    @Override
    public boolean beforeMigration() {
      return true;
    }

    @Override
    public void afterMigration() {
    }

    @Override
    public void migrateEntity(Class originEntityClass, Class currentEntityClass)
        throws ViewDataMigrationException {
      migrationContext.copyAllObjects(originEntityClass, currentEntityClass);
    }

    @Override
    public void migrateInstanceData() {
      migrationContext.copyAllInstanceData();
    }
  }
}
