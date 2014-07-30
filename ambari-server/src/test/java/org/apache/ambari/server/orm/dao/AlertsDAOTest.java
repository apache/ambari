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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.alert.Scope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * Tests {@link AlertsDAO}.
 */
public class AlertsDAOTest {

  static Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

  private Long clusterId;
  private Injector injector;
  private OrmTestHelper helper;
  private AlertsDAO dao;
  private AlertDefinitionDAO definitionDao;

  /**
   * 
   */
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    helper = injector.getInstance(OrmTestHelper.class);
    clusterId = helper.createCluster();
    dao = injector.getInstance(AlertsDAO.class);
    definitionDao = injector.getInstance(AlertDefinitionDAO.class);

    // create 5 definitions
    for (int i = 0; i < 5; i++) {
      AlertDefinitionEntity definition = new AlertDefinitionEntity();
      definition.setDefinitionName("Alert Definition " + i);
      definition.setServiceName("Service " + i);
      definition.setComponentName(null);
      definition.setClusterId(clusterId);
      definition.setHash(UUID.randomUUID().toString());
      definition.setScheduleInterval(60);
      definition.setScope(Scope.SERVICE);
      definition.setSource("Source " + i);
      definition.setSourceType("SCRIPT");
      definitionDao.create(definition);
    }

    List<AlertDefinitionEntity> definitions = definitionDao.findAll();
    assertNotNull(definitions);
    assertEquals(5, definitions.size());

    // create 10 historical alerts for each definition, 8 OK and 2 CRIT
    calendar.clear();
    calendar.set(2014, Calendar.JANUARY, 1);

    for (AlertDefinitionEntity definition : definitions) {
      for (int i = 0; i < 10; i++) {
        AlertHistoryEntity history = new AlertHistoryEntity();
        history.setServiceName(definition.getServiceName());
        history.setClusterId(clusterId);
        history.setAlertDefinition(definition);
        history.setAlertLabel(definition.getDefinitionName() + " " + i);
        history.setAlertText(definition.getDefinitionName() + " " + i);
        history.setAlertTimestamp(calendar.getTimeInMillis());

        history.setAlertState(AlertState.OK);
        if (i == 0 || i == 5) {
          history.setAlertState(AlertState.CRITICAL);
        }

        // increase the days for each
        calendar.add(Calendar.DATE, 1);

        dao.create(history);
      }
    }

    // for each definition, create a current alert
    for (AlertDefinitionEntity definition : definitions) {
      List<AlertHistoryEntity> alerts = dao.findAll();
      AlertHistoryEntity history = null;
      for (AlertHistoryEntity alert : alerts) {
        if (definition.equals(alert.getAlertDefinition())) {
          history = alert;
        }
      }

      assertNotNull(history);

      AlertCurrentEntity current = new AlertCurrentEntity();
      current.setAlertHistory(history);
      current.setLatestTimestamp(new Date().getTime());
      current.setOriginalTimestamp(new Date().getTime() - 10800000);
      current.setMaintenanceState(MaintenanceState.OFF);
      dao.create(current);
    }
  }

  /**
   * 
   */
  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }


  /**
   * 
   */
  @Test
  public void testFindAll() {
    List<AlertHistoryEntity> alerts = dao.findAll(clusterId);
    assertNotNull(alerts);
    assertEquals(50, alerts.size());
  }

  /**
   * 
   */
  @Test
  public void testFindAllCurrent() {
    List<AlertCurrentEntity> currentAlerts = dao.findCurrent();
    assertNotNull(currentAlerts);
    assertEquals(5, currentAlerts.size());
  }

  /**
   * 
   */
  @Test
  public void testFindCurrentByService() {
    List<AlertCurrentEntity> currentAlerts = dao.findCurrent();
    AlertCurrentEntity current = currentAlerts.get(0);
    AlertHistoryEntity history = current.getAlertHistory();
    
    assertNotNull(history);
    
    currentAlerts = dao.findCurrentByService(clusterId,
        history.getServiceName());

    assertNotNull(currentAlerts);
    assertEquals(1, currentAlerts.size());

    currentAlerts = dao.findCurrentByService(clusterId, "foo");

    assertNotNull(currentAlerts);
    assertEquals(0, currentAlerts.size());
  }

  /**
   * 
   */
  @Test
  public void testFindByState() {
    List<AlertState> allStates = new ArrayList<AlertState>();
    allStates.add(AlertState.OK);
    allStates.add(AlertState.WARNING);
    allStates.add(AlertState.CRITICAL);
        
    List<AlertHistoryEntity> history = dao.findAll(clusterId, allStates);
    assertNotNull(history);
    assertEquals(50, history.size());

    history = dao.findAll(clusterId, Collections.singletonList(AlertState.OK));
    assertNotNull(history);
    assertEquals(40, history.size());

    history = dao.findAll(clusterId,
        Collections.singletonList(AlertState.CRITICAL));
    assertNotNull(history);
    assertEquals(10, history.size());
    
    history = dao.findAll(clusterId,
        Collections.singletonList(AlertState.WARNING));
    assertNotNull(history);
    assertEquals(0, history.size());        
  }

  /**
   * 
   */
  @Test
  public void testFindByDate() {
    calendar.clear();
    calendar.set(2014, Calendar.JANUARY, 1);
    
    // on or after 1/1/2014
    List<AlertHistoryEntity> history = dao.findAll(clusterId,
        calendar.getTime(), null);

    assertNotNull(history);
    assertEquals(50, history.size());

    // on or before 1/1/2014
    history = dao.findAll(clusterId, null, calendar.getTime());
    assertNotNull(history);
    assertEquals(1, history.size());

    // between 1/5 and 1/10
    calendar.set(2014, Calendar.JANUARY, 5);
    Date startDate = calendar.getTime();

    calendar.set(2014, Calendar.JANUARY, 10);
    Date endDate = calendar.getTime();

    history = dao.findAll(clusterId, startDate, endDate);
    assertNotNull(history);
    assertEquals(6, history.size());

    // after 3/1
    calendar.set(2014, Calendar.MARCH, 5);
    history = dao.findAll(clusterId, calendar.getTime(), null);
    assertNotNull(history);
    assertEquals(0, history.size());

    history = dao.findAll(clusterId, endDate, startDate);
    assertNotNull(history);
    assertEquals(0, history.size());
  }
}
