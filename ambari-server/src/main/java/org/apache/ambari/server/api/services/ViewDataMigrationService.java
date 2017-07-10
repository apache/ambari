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

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.view.ViewDataMigrationUtility;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.view.migration.ViewDataMigrationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

  /**
   * The singleton view registry.
   */
  ViewRegistry viewRegistry;

  /**
   * The view data migration utility.
   */
  private ViewDataMigrationUtility viewDataMigrationUtility;

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
   * @param originViewVersion  the origin view version
   * @param originInstanceName the origin view instance name
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

    ViewInstanceEntity instanceDefinition = viewRegistry.getInstanceDefinition(
        viewName, viewVersion, instanceName);
    ViewInstanceEntity originInstanceDefinition = viewRegistry.getInstanceDefinition(
        viewName, originViewVersion, originInstanceName);

    getViewDataMigrationUtility().migrateData(instanceDefinition, originInstanceDefinition, false);

    Response.ResponseBuilder builder = Response.status(Response.Status.OK);
    return builder.build();
  }

  protected ViewDataMigrationUtility getViewDataMigrationUtility() {
    if (viewDataMigrationUtility == null) {
      viewDataMigrationUtility = new ViewDataMigrationUtility(viewRegistry);
    }
    return viewDataMigrationUtility;
  }

  protected void setViewDataMigrationUtility(ViewDataMigrationUtility viewDataMigrationUtility) {
    this.viewDataMigrationUtility = viewDataMigrationUtility;
  }
}
