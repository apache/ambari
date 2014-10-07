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
package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMORY_URL;
import static org.apache.ambari.server.configuration.Configuration.JDBC_IN_MEMROY_DRIVER;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.captureLong;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.DBAccessorImpl;
import org.apache.ambari.server.orm.dao.AlertsDAO;
import org.apache.ambari.server.orm.entities.AlertCurrentEntity;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.AlertHistoryEntity;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Test the AlertResourceProvider class
 */
public class AlertResourceProviderTest {

  private static final Long ALERT_VALUE_ID = Long.valueOf(1000L);
  private static final String ALERT_VALUE_LABEL = "My Label";
  private static final Long ALERT_VALUE_TIMESTAMP = Long.valueOf(1L);
  private static final String ALERT_VALUE_TEXT = "My Text";
  private static final String ALERT_VALUE_COMPONENT = "component";
  private static final String ALERT_VALUE_HOSTNAME = "host";
  private static final String ALERT_VALUE_SERVICE = "service";

  private AlertsDAO m_dao;
//  private Injector m_injector;
  private AmbariManagementController m_amc;

  @Before
  @SuppressWarnings("boxing")  
  public void before() throws Exception {
    Injector m_injector = Guice.createInjector(new MockModule());

    m_amc = m_injector.getInstance(AmbariManagementController.class);

    Cluster cluster = EasyMock.createMock(Cluster.class);
    Clusters clusters = m_injector.getInstance(Clusters.class);

    expect(m_amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster(capture(new Capture<String>()))).andReturn(cluster).atLeastOnce();
    expect(cluster.getClusterId()).andReturn(Long.valueOf(1L));

    replay(m_amc, clusters, cluster);
    
    m_dao = m_injector.getInstance(AlertsDAO.class);
    
    AlertResourceProvider.init(m_injector);
  }


  /**
   * @throws Exception
   */
  @Test
  public void testGetCluster() throws Exception {
    expect(m_dao.findCurrentByCluster(
        captureLong(new Capture<Long>()))).andReturn(getClusterMockEntities()).anyTimes();
    
    replay(m_dao);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_NAME,
        AlertResourceProvider.ALERT_LABEL);
    
    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").toPredicate();
    
    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    assertEquals("c1", r.getPropertyValue(AlertResourceProvider.ALERT_CLUSTER_NAME));
    
    verify(m_dao);
  }
  
  /**
   * Test for service
   */
  @Test
  public void testGetService() throws Exception {
    expect(m_dao.findCurrentByService(captureLong(new Capture<Long>()),
        capture(new Capture<String>()))).andReturn(getClusterMockEntities()).anyTimes();
    
    replay(m_dao);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_NAME,
        AlertResourceProvider.ALERT_LABEL);
    
    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").and()
        .property(AlertResourceProvider.ALERT_SERVICE).equals(ALERT_VALUE_SERVICE).toPredicate();
    
    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    assertEquals("c1", r.getPropertyValue(AlertResourceProvider.ALERT_CLUSTER_NAME));
    assertEquals(ALERT_VALUE_SERVICE, r.getPropertyValue(AlertResourceProvider.ALERT_SERVICE));
    
    verify(m_dao);
  }  

  /**
   * Test for service
   */
  @Test
  public void testGetHost() throws Exception {
    expect(m_dao.findCurrentByHost(captureLong(new Capture<Long>()),
        capture(new Capture<String>()))).andReturn(getClusterMockEntities()).anyTimes();
    
    replay(m_dao);

    Request request = PropertyHelper.getReadRequest(
        AlertResourceProvider.ALERT_ID,
        AlertResourceProvider.ALERT_NAME,
        AlertResourceProvider.ALERT_LABEL);
    
    Predicate predicate = new PredicateBuilder().property(
        AlertResourceProvider.ALERT_CLUSTER_NAME).equals("c1").and()
        .property(AlertResourceProvider.ALERT_HOST).equals(ALERT_VALUE_HOSTNAME).toPredicate();
    
    AlertResourceProvider provider = createProvider();
    Set<Resource> results = provider.getResources(request, predicate);

    assertEquals(1, results.size());

    Resource r = results.iterator().next();
    assertEquals("c1", r.getPropertyValue(AlertResourceProvider.ALERT_CLUSTER_NAME));
    assertEquals(ALERT_VALUE_HOSTNAME, r.getPropertyValue(AlertResourceProvider.ALERT_HOST));
    
    verify(m_dao);
  }  

  
  
  private AlertResourceProvider createProvider() {
    return new AlertResourceProvider(
        PropertyHelper.getPropertyIds(Resource.Type.Alert),
        PropertyHelper.getKeyPropertyIds(Resource.Type.Alert),
        m_amc);
  }

  /**
   * @return
   */
  private List<AlertCurrentEntity> getClusterMockEntities() throws Exception {
    AlertCurrentEntity current = new AlertCurrentEntity();
    current.setAlertId(Long.valueOf(1000L));
    current.setLatestTimestamp(Long.valueOf(1L));
    current.setOriginalTimestamp(Long.valueOf(2L));
    
    AlertHistoryEntity history = new AlertHistoryEntity();
    history.setAlertId(ALERT_VALUE_ID);
    history.setAlertInstance(null);
    history.setAlertLabel(ALERT_VALUE_LABEL);
    history.setAlertState(AlertState.OK);
    history.setAlertText(ALERT_VALUE_TEXT);
    history.setAlertTimestamp(ALERT_VALUE_TIMESTAMP);
    history.setClusterId(Long.valueOf(1L));
    history.setComponentName(ALERT_VALUE_COMPONENT);
    history.setHostName(ALERT_VALUE_HOSTNAME);
    history.setServiceName(ALERT_VALUE_SERVICE);
    
    AlertDefinitionEntity definition = new AlertDefinitionEntity();
    
    history.setAlertDefinition(definition);
    current.setAlertHistory(history);
    
    return Arrays.asList(current);
  }


  /**
  *
  */
  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(EntityManager.class).toInstance(EasyMock.createMock(EntityManager.class));
      binder.bind(AlertsDAO.class).toInstance(EasyMock.createMock(AlertsDAO.class));
      binder.bind(AmbariManagementController.class).toInstance(createMock(AmbariManagementController.class));
      binder.bind(DBAccessor.class).to(DBAccessorImpl.class);
      
      Clusters clusters = EasyMock.createNiceMock(Clusters.class);
      Configuration configuration = EasyMock.createMock(Configuration.class);
      
      binder.bind(Clusters.class).toInstance(clusters);
      binder.bind(Configuration.class).toInstance(configuration);

      expect(configuration.getDatabaseUrl()).andReturn(JDBC_IN_MEMORY_URL).anyTimes();
      expect(configuration.getDatabaseDriver()).andReturn(JDBC_IN_MEMROY_DRIVER).anyTimes();
      expect(configuration.getDatabaseUser()).andReturn("test").anyTimes();
      expect(configuration.getDatabasePassword()).andReturn("test").anyTimes();
      replay(configuration);
    }
  }
}
