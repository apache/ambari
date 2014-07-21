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

package org.apache.ambari.server.orm.dao;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.MaintenanceState;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests {@link AlertsDAO}.
 */
public class AlertsDAOTest {

  static Long clusterId;
  static Injector injector;
  AlertsDAO dao;

  @BeforeClass
  public static void beforeClass() {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusterId = injector.getInstance(OrmTestHelper.class).createCluster();
    AlertsDAO alertDAO = injector.getInstance(AlertsDAO.class);
    AlertDefinitionDAO alertDefinitionDAO = injector.getInstance(AlertDefinitionDAO.class);

    // create 5 definitions
    for (int i = 0; i < 5; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("Service " + i);
      definition.setComponentName(null);
      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60L);
      definition.setScope("SERVICE");
      definition.setSource("Source " + i);
      definition.setSourceType("SCRIPT");
      alertDefinitionDAO.create(definition);
    }

    List<AlertDefinitionEntity> definitions = alertDefinitionDAO.findAll();
    Assert.assertNotNull(definitions);
    Assert.assertEquals(5, definitions.size());

    // create 5 historical alerts for each definition
    for (AlertDefinitionEntity definition : definitions) {
      for (int i = 0; i < 5; i++) {
        AlertHistoryEntity history = new AlertHistoryEntity();
        history.setServiceName(definition.getServiceName());
        history.setClusterId(clusterId);
        history.setAlertDefinition(definition);
        history.setAlertLabel(definition.getDefinitionName() + " " + i);
        history.setAlertState(AlertState.OK);
        history.setAlertText(definition.getDefinitionName() + " " + i);
        history.setAlertTimestamp(new Date().getTime());
        alertDAO.create(history);
      }
    }

    // for each definition, create a current alert
    for (AlertDefinitionEntity definition : definitions) {
      List<AlertHistoryEntity> alerts = alertDAO.findAll();
      AlertHistoryEntity history = null;
      for (AlertHistoryEntity alert : alerts) {
        if (definition.equals(alert.getAlertDefinition())) {
          history = alert;
        }
      }

      Assert.assertNotNull(history);

      AlertCurrentEntity current = new AlertCurrentEntity();
      current.setAlertId(history.getAlertId());
      current.setAlertHistory(history);
      current.setLatestTimestamp(new Date().getTime());
      current.setOriginalTimestamp(new Date().getTime() - 10800000);
      current.setMaintenanceState(MaintenanceState.OFF);
      alertDAO.create(current);
    }
  }

  /**
   * 
   */
  @AfterClass
  public static void afterClass() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  /**
   * 
   */
  @Before
  public void setup() {
    dao = new AlertsDAO();
    injector.injectMembers(dao);
  }

  /**
   * 
   */
  @Test
  public void testFindAll() {
    List<AlertHistoryEntity> alerts = dao.findAll(clusterId);
    Assert.assertNotNull(alerts);
    Assert.assertEquals(25, alerts.size());
  }

  /**
   * 
   */
  @Test
  public void testFindAllCurrent() {
    List<AlertCurrentEntity> currentAlerts = dao.findCurrent();
    Assert.assertNotNull(currentAlerts);
    Assert.assertEquals(5, currentAlerts.size());
  }

  /**
   * 
   */
  @Test
  public void testFindCurrentByService() {
    List<AlertCurrentEntity> currentAlerts = dao.findCurrent();
    AlertCurrentEntity current = currentAlerts.get(0);
    AlertHistoryEntity history = current.getAlertHistory();
    
    Assert.assertNotNull(history);    
    
    currentAlerts = dao.findCurrentByService(clusterId,
        history.getServiceName());

    Assert.assertNotNull(currentAlerts);
    Assert.assertEquals(1, currentAlerts.size());

    currentAlerts = dao.findCurrentByService(clusterId, "foo");

    Assert.assertNotNull(currentAlerts);
    Assert.assertEquals(0, currentAlerts.size());
  }
}
