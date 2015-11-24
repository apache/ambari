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
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.controller.ControllerModule;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.MetainfoDAO;
import org.apache.ambari.server.orm.entities.MetainfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.Transactional;

public class StackUpgradeHelper {
  private static final Logger LOG = LoggerFactory.getLogger
    (StackUpgradeHelper.class);

  private static final String STACK_ID_UPDATE_ACTION = "updateStackId";
  private static final String METAINFO_UPDATE_ACTION = "updateMetaInfo";
  private static final String STACK_ID_STACK_NAME_KEY = "stackName";

  @Inject
  private DBAccessor dbAccessor;
  @Inject
  private PersistService persistService;
  @Inject
  private MetainfoDAO metainfoDAO;
  @Inject
  private StackUpgradeUtil stackUpgradeUtil;

  private void startPersistenceService() {
    persistService.start();
  }

  private void stopPersistenceService() {
    persistService.stop();
  }

  /**
   * Add key value to the metainfo table.
   * @param data
   * @throws SQLException
   */
  @Transactional
  void updateMetaInfo(Map<String, String> data) throws SQLException {
    if (data != null && !data.isEmpty()) {
      for (Map.Entry<String, String> entry : data.entrySet()) {
        MetainfoEntity metainfoEntity = metainfoDAO.findByKey(entry.getKey());
        if (metainfoEntity != null) {
          metainfoEntity.setMetainfoName(entry.getKey());
          metainfoEntity.setMetainfoValue(entry.getValue());
          metainfoDAO.merge(metainfoEntity);
        } else {
          metainfoEntity = new MetainfoEntity();
          metainfoEntity.setMetainfoName(entry.getKey());
          metainfoEntity.setMetainfoValue(entry.getValue());
          metainfoDAO.create(metainfoEntity);
        }
      }
    }
  }

  /**
   * Change the stack id in the Ambari DB.
   * @param stackInfo
   * @throws SQLException
   */
  public void updateStackVersion(Map<String, String> stackInfo) throws Exception {
    if (stackInfo == null || stackInfo.isEmpty()) {
      throw new IllegalArgumentException("Empty stack id. " + stackInfo);
    }
    
    String repoUrl = stackInfo.remove("repo_url");
    String repoUrlOs = stackInfo.remove("repo_url_os");
    
    Iterator<Map.Entry<String, String>> stackIdEntry = stackInfo.entrySet().iterator();
    Map.Entry<String, String> stackEntry = stackIdEntry.next();

    String stackName = stackEntry.getKey();
    String stackVersion = stackEntry.getValue();

    LOG.info("Updating stack id, stackName = " + stackName + ", " +
      "stackVersion = "+ stackVersion);

    stackUpgradeUtil.updateStackDetails(stackName, stackVersion);
    
    if (null != repoUrl) {
      stackUpgradeUtil.updateLocalRepo(stackName, stackVersion, repoUrl, repoUrlOs);  
    }

    dbAccessor.updateTable("hostcomponentstate", "current_state", "INSTALLED", "where current_state = 'UPGRADING'");
  }

  private List<String> getValidActions() {
    return new ArrayList<String>() {{
      add(STACK_ID_UPDATE_ACTION);
      add(METAINFO_UPDATE_ACTION);
    }};
  }

  /**
   * Support changes need to support upgrade of Stack
   * @param args Simple key value json map
   */
  public static void main(String[] args) {
    try {
      if (args.length < 2) {
        throw new InputMismatchException("Need to provide action, " +
          "stack name and stack version.");
      }

      String action = args[0];
      String valueMap = args[1];

      Injector injector = Guice.createInjector(new ControllerModule());
      StackUpgradeHelper stackUpgradeHelper = injector.getInstance(StackUpgradeHelper.class);
      Gson gson = injector.getInstance(Gson.class);

      if (!stackUpgradeHelper.getValidActions().contains(action)) {
        throw new IllegalArgumentException("Unsupported action. Allowed " +
          "actions: " + stackUpgradeHelper.getValidActions());
      }

      
      stackUpgradeHelper.startPersistenceService();
      Map values = gson.fromJson(valueMap, Map.class);

      if (action.equals(STACK_ID_UPDATE_ACTION)) {
        stackUpgradeHelper.updateStackVersion(values);
        
      } else if (action.equals(METAINFO_UPDATE_ACTION)) {

        stackUpgradeHelper.updateMetaInfo(values);
      }

      stackUpgradeHelper.stopPersistenceService();

    } catch (Throwable t) {
      LOG.error("Caught exception on upgrade. Exiting...", t);
      System.exit(1);
    }
  }
}
