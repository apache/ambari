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
package org.apache.ambari.server.state.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.UUID;

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.AlertDefinitionDAO;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.alert.Scope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests the alert manager.
 */
public class AlertDataManagerTest {
  
  private static final String ALERT_DEFINITION = "Alert Definition 1";
  private static final String SERVICE = "service1";
  private static final String COMPONENT = "component1";
  private static final String HOST1 = "h1";
  private static final String HOST2 = "h2";
  private static final String ALERT_LABEL = "My Label";
  
  private Long clusterId;
  private Injector injector;
  private OrmTestHelper helper;
  private AlertsDAO dao;
  
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();
    dao = injector.getInstance(AlertsDAO.class);
    AlertDefinitionDAO definitionDao = injector.getInstance(AlertDefinitionDAO.class);

    // create 5 definitions
    for (int i = 0; i < 5; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("Service " + i);
      definition.setComponentName(null);
      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(Integer.valueOf(60));
      definition.setScope(Scope.SERVICE);
      definition.setSource("Source " + i);
      definition.setSourceType("SCRIPT");
      definitionDao.create(definition);
    }
  
  }
  
  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }
  
  @Test
  public void testAlertRecords() {
    
    Alert alert1 = new Alert(ALERT_DEFINITION, null, SERVICE, COMPONENT, HOST1, AlertState.OK);
    alert1.setLabel(ALERT_LABEL);
    alert1.setText("Component component1 is OK");
    alert1.setTimestamp(1L);

    Alert alert2 = new Alert(ALERT_DEFINITION, null, SERVICE, COMPONENT, HOST2, AlertState.CRITICAL);
    alert2.setLabel(ALERT_LABEL);
    alert2.setText("Component component2 is not OK");
    
    AlertDataManager am = injector.getInstance(AlertDataManager.class);
    
    am.add(clusterId.longValue(), alert1);
    am.add(clusterId.longValue(), alert2);
    
    List<AlertCurrentEntity> allCurrent = dao.findCurrentByService(clusterId.longValue(), SERVICE);
    assertEquals(2, allCurrent.size());
    
    List<AlertHistoryEntity> allHistory = dao.findAll(clusterId.longValue());
    assertEquals(2, allHistory.size());
    
    AlertCurrentEntity current = dao.findCurrentByHostAndName(clusterId.longValue(), HOST1, ALERT_DEFINITION);
    assertNotNull(current);
    assertEquals(HOST1, current.getAlertHistory().getHostName());
    assertEquals(ALERT_DEFINITION, current.getAlertHistory().getAlertDefinition().getDefinitionName());
    assertEquals(ALERT_LABEL, current.getAlertHistory().getAlertLabel());
    assertEquals("Component component1 is OK", current.getAlertHistory().getAlertText());
    assertEquals(current.getAlertHistory().getAlertState(), AlertState.OK);
    assertEquals(1L, current.getOriginalTimestamp().longValue());
    assertEquals(1L, current.getLatestTimestamp().longValue());
    
    Long currentId = current.getAlertId();
    Long historyId = current.getAlertHistory().getAlertId();
    
    // no new history since the state is the same
    Alert alert3 = new Alert(ALERT_DEFINITION, null, SERVICE, COMPONENT, HOST1, AlertState.OK);
    alert3.setLabel(ALERT_LABEL);
    alert3.setText("Component component1 is OK");
    alert3.setTimestamp(2L);
    am.add(clusterId.longValue(), alert3);
    
    current = dao.findCurrentByHostAndName(clusterId.longValue(), HOST1, ALERT_DEFINITION);
    assertNotNull(current);
    assertEquals(currentId, current.getAlertId());
    assertEquals(historyId, current.getAlertHistory().getAlertId());
    assertEquals(HOST1, current.getAlertHistory().getHostName());
    assertEquals(ALERT_DEFINITION, current.getAlertHistory().getAlertDefinition().getDefinitionName());
    assertEquals(ALERT_LABEL, current.getAlertHistory().getAlertLabel());
    assertEquals("Component component1 is OK", current.getAlertHistory().getAlertText());
    assertEquals(current.getAlertHistory().getAlertState(), AlertState.OK);
    assertEquals(1L, current.getOriginalTimestamp().longValue());
    assertEquals(2L, current.getLatestTimestamp().longValue());
   
    allCurrent = dao.findCurrentByService(clusterId.longValue(), SERVICE);
    assertEquals(2, allCurrent.size());
    
    allHistory = dao.findAll(clusterId.longValue());
    assertEquals(2, allHistory.size());
    
    // change to warning
    Alert alert4 = new Alert(ALERT_DEFINITION, null, SERVICE, COMPONENT, HOST1, AlertState.WARNING);
    alert4.setLabel(ALERT_LABEL);
    alert4.setText("Component component1 is about to go down");
    alert4.setTimestamp(3L);
    am.add(clusterId.longValue(), alert4);

    current = dao.findCurrentByHostAndName(clusterId.longValue(), HOST1, ALERT_DEFINITION);
    assertNotNull(current);
    assertEquals(current.getAlertId(), currentId);
    assertFalse(historyId.equals(current.getAlertHistory().getAlertId()));    
    assertEquals(HOST1, current.getAlertHistory().getHostName());
    assertEquals(ALERT_DEFINITION, current.getAlertHistory().getAlertDefinition().getDefinitionName());
    assertEquals(ALERT_LABEL, current.getAlertHistory().getAlertLabel());
    assertEquals("Component component1 is about to go down", current.getAlertHistory().getAlertText());
    assertEquals(current.getAlertHistory().getAlertState(), AlertState.WARNING);
    assertEquals(3L, current.getOriginalTimestamp().longValue());
    assertEquals(3L, current.getLatestTimestamp().longValue());

    allCurrent = dao.findCurrentByService(clusterId.longValue(), SERVICE);
    assertEquals(2, allCurrent.size());
    
    allHistory = dao.findAll(clusterId.longValue());
    assertEquals(3, allHistory.size());
  }
}
