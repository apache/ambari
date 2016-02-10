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


import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.MaintenanceStateHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

public class UpgradeCatalog222Test {
  private Injector injector;
  private Provider<EntityManager> entityManagerProvider = createStrictMock(Provider.class);
  private EntityManager entityManager = createNiceMock(EntityManager.class);
  private UpgradeCatalogHelper upgradeCatalogHelper;
  private StackEntity desiredStackEntity;



  @Before
  public void init() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).anyTimes();
    replay(entityManagerProvider);
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    upgradeCatalogHelper = injector.getInstance(UpgradeCatalogHelper.class);
    // inject AmbariMetaInfo to ensure that stacks get populated in the DB
    injector.getInstance(AmbariMetaInfo.class);
    // load the stack entity
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    desiredStackEntity = stackDAO.find("HDP", "2.2.0");
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method addNewConfigurationsFromXml = AbstractUpgradeCatalog.class.getDeclaredMethod("addNewConfigurationsFromXml");
    Method updateAlerts = UpgradeCatalog222.class.getDeclaredMethod("updateAlerts");
    Method updateStormConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateStormConfigs");
    Method updateAMSConfigs = UpgradeCatalog222.class.getDeclaredMethod("updateAMSConfigs");


    UpgradeCatalog222 upgradeCatalog222 = createMockBuilder(UpgradeCatalog222.class)
            .addMockedMethod(addNewConfigurationsFromXml)
            .addMockedMethod(updateAlerts)
            .addMockedMethod(updateStormConfigs)
            .addMockedMethod(updateAMSConfigs)
            .createMock();

    upgradeCatalog222.addNewConfigurationsFromXml();
    expectLastCall().once();
    upgradeCatalog222.updateAlerts();
    expectLastCall().once();
    upgradeCatalog222.updateStormConfigs();
    expectLastCall().once();
    upgradeCatalog222.updateAMSConfigs();
    expectLastCall().once();

    replay(upgradeCatalog222);

    upgradeCatalog222.executeDMLUpdates();

    verify(upgradeCatalog222);
  }

  @Test
  public void testAmsSiteUpdateConfigs() throws Exception{

    Map<String, String> oldPropertiesAmsSite = new HashMap<String, String>() {
      {
        put("timeline.metrics.host.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(1));
        put("timeline.metrics.cluster.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(1));
        put("timeline.metrics.service.operation.mode", "distributed");
        put("timeline.metrics.host.aggregator.ttl", String.valueOf(86400));
        put("timeline.metrics.host.aggregator.minute.ttl", String.valueOf(604800));
        put("timeline.metrics.host.aggregator.hourly.ttl", String.valueOf(2592000));
        put("timeline.metrics.host.aggregator.daily.ttl", String.valueOf(31536000));
        put("timeline.metrics.cluster.aggregator.second.ttl", String.valueOf(21600)); //Less than 1 day
        put("timeline.metrics.cluster.aggregator.minute.ttl", String.valueOf(7776000));
        put("timeline.metrics.cluster.aggregator.hourly.ttl", String.valueOf(31536000));
        put("timeline.metrics.cluster.aggregator.daily.ttl", String.valueOf(63072000));
      }
    };
    Map<String, String> newPropertiesAmsSite = new HashMap<String, String>() {
      {
        put("timeline.metrics.host.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(2));
        put("timeline.metrics.cluster.aggregator.daily.checkpointCutOffMultiplier", String.valueOf(2));
        put("timeline.metrics.service.watcher.disabled", String.valueOf(false));
        put("timeline.metrics.host.aggregator.ttl", String.valueOf(7.0));
        put("timeline.metrics.host.aggregator.minute.ttl", String.valueOf(7.0));
        put("timeline.metrics.host.aggregator.hourly.ttl", String.valueOf(30.0));
        put("timeline.metrics.host.aggregator.daily.ttl", String.valueOf(365.0));
        put("timeline.metrics.cluster.aggregator.second.ttl", String.valueOf(0.25));
        put("timeline.metrics.cluster.aggregator.minute.ttl", String.valueOf(30.0));
        put("timeline.metrics.cluster.aggregator.hourly.ttl", String.valueOf(365.0));
        put("timeline.metrics.cluster.aggregator.daily.ttl", String.valueOf(730.0));
        put("timeline.metrics.service.operation.mode", "distributed");
      }
    };
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    Clusters clusters = easyMockSupport.createNiceMock(Clusters.class);
    final Cluster cluster = easyMockSupport.createNiceMock(Cluster.class);
    Config mockAmsSite = easyMockSupport.createNiceMock(Config.class);

    expect(clusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", cluster);
    }}).once();
    expect(cluster.getDesiredConfigByType("ams-site")).andReturn(mockAmsSite).atLeastOnce();
    expect(mockAmsSite.getProperties()).andReturn(oldPropertiesAmsSite).anyTimes();

    Injector injector = easyMockSupport.createNiceMock(Injector.class);
    expect(injector.getInstance(Gson.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(MaintenanceStateHelper.class)).andReturn(null).anyTimes();
    expect(injector.getInstance(KerberosHelper.class)).andReturn(createNiceMock(KerberosHelper.class)).anyTimes();

    replay(injector, clusters, mockAmsSite, cluster);

    AmbariManagementControllerImpl controller = createMockBuilder(AmbariManagementControllerImpl.class)
      .addMockedMethod("createConfiguration")
      .addMockedMethod("getClusters", new Class[] { })
      .withConstructor(createNiceMock(ActionManager.class), clusters, injector)
      .createNiceMock();

    Injector injector2 = easyMockSupport.createNiceMock(Injector.class);
    Capture<ConfigurationRequest> configurationRequestCapture = EasyMock.newCapture();
    ConfigurationResponse configurationResponseMock = easyMockSupport.createMock(ConfigurationResponse.class);

    expect(injector2.getInstance(AmbariManagementController.class)).andReturn(controller).anyTimes();
    expect(controller.getClusters()).andReturn(clusters).anyTimes();
    expect(controller.createConfiguration(capture(configurationRequestCapture))).andReturn(configurationResponseMock)
      .anyTimes();

    replay(controller, injector2, configurationResponseMock);
    new UpgradeCatalog222(injector2).updateAMSConfigs();
    easyMockSupport.verifyAll();

    ConfigurationRequest configurationRequest = configurationRequestCapture.getValue();
    Map<String, String> updatedProperties = configurationRequest.getProperties();
    assertTrue(Maps.difference(newPropertiesAmsSite, updatedProperties).areEqual());

  }

}
