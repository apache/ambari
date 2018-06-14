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
package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Map;

import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.MpackDAO;
import org.apache.ambari.server.orm.dao.ServiceGroupDAO;
import org.apache.ambari.server.orm.dao.UpgradePlanDAO;
import org.apache.ambari.server.orm.entities.MpackEntity;
import org.apache.ambari.server.orm.entities.ServiceGroupEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanDetailEntity;
import org.apache.ambari.server.orm.entities.UpgradePlanEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

/**
 * UpgradePlanResourceProvider tests.
 */
public class UpgradePlanResourceProviderTest {

  private Injector injector;
  private UpgradePlanDAO planDAO;

  private AmbariManagementController amc;
  private MpackDAO mpackDAO;
  private ServiceGroupDAO serviceGroupDAO;

  @Before
  public void before() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);
    expect(cluster.getClusterName()).andReturn("c1").atLeastOnce();
    expect(cluster.getClusterId()).andReturn(1L).anyTimes();

    Clusters clusters = createNiceMock(Clusters.class);
    expect(clusters.getCluster(anyString())).andReturn(cluster).atLeastOnce();

    amc = createNiceMock(AmbariManagementController.class);
    mpackDAO = createNiceMock(MpackDAO.class);
    serviceGroupDAO = createNiceMock(ServiceGroupDAO.class);

    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();

    replay(cluster, clusters, amc);

    // Create an injector which will inject the mocks
    injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);

    planDAO = injector.getInstance(UpgradePlanDAO.class);
  }

  @After
  public void after() throws Exception {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;
  }

  @Test
  public void testCreateResources() throws Exception {
    ServiceGroupEntity randomServiceGroup = createNiceMock(ServiceGroupEntity.class);
    expect(serviceGroupDAO.findByPK(EasyMock.anyLong())).andReturn(randomServiceGroup).atLeastOnce();

    MpackEntity randomMpack = createNiceMock(MpackEntity.class);
    expect(mpackDAO.findById(EasyMock.anyLong())).andReturn(randomMpack).atLeastOnce();

    replay(serviceGroupDAO, mpackDAO);

    UpgradePlanResourceProvider provider = createProvider(amc);

    Map<String, Object> serviceGroups = ImmutableMap.<String, Object>builder()
        .put("service_group_id", 4L)
        .put("mpack_target_id", 2L)
        .put("upgrade_pack", "up.xml")
        .build();

    Map<String, Object> requestMap = ImmutableMap.<String, Object>builder()
        .put(UpgradePlanResourceProvider.UPGRADE_PLAN_CLUSTER_NAME, "c1")
        .put(UpgradePlanResourceProvider.UPGRADE_PLAN_TYPE, UpgradeType.ROLLING.name())
        .put(UpgradePlanResourceProvider.UPGRADE_PLAN_DIRECTION, Direction.UPGRADE.name())
        .put(UpgradePlanResourceProvider.UPGRADE_PLAN_SERVICE_GROUPS, Sets.newHashSet(serviceGroups))
        .build();

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestMap), null);

    RequestStatus requestStatus = provider.createResourcesAuthorized(request);

    assertEquals(1, requestStatus.getAssociatedResources().size());

    Resource resource = requestStatus.getAssociatedResources().iterator().next();
    Long id = (Long) resource.getPropertyValue(UpgradePlanResourceProvider.UPGRADE_PLAN_ID);
    assertNotNull(id);

    UpgradePlanEntity entity = planDAO.findByPK(id);
    assertNotNull(entity);
    assertEquals(1, entity.getDetails().size());

    UpgradePlanDetailEntity detail = entity.getDetails().iterator().next();
    assertEquals(4L, detail.getServiceGroupId());
    assertEquals(2L, detail.getMpackTargetId());
    assertEquals("up.xml", detail.getUpgradePack());
    assertNotNull(detail.getConfigChanges());
    // !!! TODO when more thorough code is added, we'll be able to test more assertions
    assertEquals(0, detail.getConfigChanges().size());
  }


  /**
   * @param amc
   * @return the provider
   */
  private UpgradePlanResourceProvider createProvider(AmbariManagementController amc) {
    return new UpgradePlanResourceProvider(amc);
  }

  /**
   * Mock module that will bind UpgradeHelper to a mock instance.
   */
  private class MockModule implements Module {
    @Override
    public void configure(Binder binder) {
      binder.bind(AmbariManagementController.class).toInstance(amc);
      binder.bind(MpackDAO.class).toInstance(mpackDAO);
      binder.bind(ServiceGroupDAO.class).toInstance(serviceGroupDAO);
    }
  }
}
