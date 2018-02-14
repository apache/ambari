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

import java.sql.SQLException;

import javax.inject.Inject;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.state.Clusters;

import com.google.inject.Injector;

/**
 * Updates script-based alert definitions with paths from the stack.
 */
public class UpdateAlertScriptPaths extends AbstractFinalUpgradeCatalog {

  @Inject
  public UpdateAlertScriptPaths(Injector injector) {
    super(injector);
  }

  @Override
  protected void executeDMLUpdates() throws AmbariException, SQLException {
    AmbariManagementController ambariManagementController = injector.getInstance(AmbariManagementController.class);
    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    Clusters clusters = ambariManagementController.getClusters();
    ambariMetaInfo.reconcileAlertDefinitions(clusters, true);
  }
}
