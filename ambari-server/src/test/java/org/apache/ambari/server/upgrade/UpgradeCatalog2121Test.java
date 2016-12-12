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

import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;

@PrepareForTest(UpgradeCatalog2121.class)
public class UpgradeCatalog2121Test {
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
    desiredStackEntity = stackDAO.find("PHD", "3.0.0");
  }

  @After
  public void tearDown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testExecuteDMLUpdates() throws Exception {
    Method updatePHDConfigs = UpgradeCatalog2121.class.getDeclaredMethod("updatePHDConfigs");
    Method updateOozieConfigs = UpgradeCatalog2121.class.getDeclaredMethod("updateOozieConfigs");

    UpgradeCatalog2121 upgradeCatalog2121 = createMockBuilder(UpgradeCatalog2121.class)
        .addMockedMethod(updatePHDConfigs)
        .addMockedMethod(updateOozieConfigs)
        .createMock();

    upgradeCatalog2121.updatePHDConfigs();
    expectLastCall().once();
    upgradeCatalog2121.updateOozieConfigs();
    expectLastCall().once();

    replay(upgradeCatalog2121);

    upgradeCatalog2121.executeDMLUpdates();

    verify(upgradeCatalog2121);
  }

  @Test
  public void testUpdateOozieConfigs() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final Clusters mockClusters = easyMockSupport.createStrictMock(Clusters.class);
    final Cluster mockClusterExpected = easyMockSupport.createNiceMock(Cluster.class);
    final Map<String, String> propertiesOozieSite = new HashMap<String, String>() {{
      put("oozie.authentication.kerberos.name.rules", "\n ");
    }};
    final Config oozieSiteConf = easyMockSupport.createNiceMock(Config.class);
    final Injector mockInjector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(Clusters.class).toInstance(mockClusters);
        bind(EntityManager.class).toInstance(entityManager);

        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
      }
    });
    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(new HashMap<String, Cluster>() {{
      put("normal", mockClusterExpected);
    }}).atLeastOnce();
    expect(mockClusterExpected.getDesiredConfigByType("oozie-site")).andReturn(oozieSiteConf).atLeastOnce();

    expect(oozieSiteConf.getProperties()).andReturn(propertiesOozieSite).once();

    UpgradeCatalog2121 upgradeCatalog2121 = createMockBuilder(UpgradeCatalog2121.class)
            .withConstructor(Injector.class)
            .withArgs(mockInjector)
            .addMockedMethod("updateConfigurationPropertiesForCluster", Cluster.class, String.class,
                    Map.class, Set.class, boolean.class, boolean.class)
            .createMock();
    upgradeCatalog2121.updateConfigurationPropertiesForCluster(mockClusterExpected,
            "oozie-site", new HashMap<String, String>(), Collections.singleton("oozie.authentication.kerberos.name.rules"),
            true, false);
    expectLastCall().once();

    easyMockSupport.replayAll();
    replay(upgradeCatalog2121);
    upgradeCatalog2121.updateOozieConfigs();
    easyMockSupport.verifyAll();

  }
}

