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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.persist.PersistService;
import com.google.inject.util.Modules;

/**
 * UpgradeResourceDefinition tests.
 */
public class UpgradeResourceProviderTest {

  private UpgradeDAO upgradeDao = null;
  private RepositoryVersionDAO repoVersionDao = null;
  private Injector m_injector;

  @Before
  public void before() {
    upgradeDao = createStrictMock(UpgradeDAO.class);
    repoVersionDao = createStrictMock(RepositoryVersionDAO.class);

    m_injector = Guice.createInjector(Modules.override(
        new InMemoryDefaultTestModule()).with(new Module() {

          @Override
          public void configure(Binder binder) {
            // TODO Auto-generated method stub
            binder.bind(UpgradeDAO.class).toInstance(upgradeDao);
            binder.bind(RepositoryVersionDAO.class).toInstance(repoVersionDao);
          }
        }));
    m_injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void after() {
    m_injector.getInstance(PersistService.class).stop();
    m_injector = null;
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCreateResources() throws Exception {
    AmbariManagementController amc = createMock(AmbariManagementController.class);
    Clusters clusters = createMock(Clusters.class);
    Cluster cluster = createCluster();

    expect(amc.getClusters()).andReturn(clusters).atLeastOnce();
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).atLeastOnce();

    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setUpgradePackage("upgrade_test");

    expect(repoVersionDao.findByStackAndVersion((String)anyObject(),(String)anyObject())).andReturn(repoVersionEntity).atLeastOnce();

    Capture<UpgradeEntity> entityCapture = new Capture<UpgradeEntity>();
    upgradeDao.create(capture(entityCapture));
    expectLastCall();

    replay(amc, clusters, cluster, upgradeDao, repoVersionDao);

    UpgradeResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.2.2");

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);

    assertTrue(entityCapture.hasCaptured());
    UpgradeEntity entity = entityCapture.getValue();
    assertNotNull(entity);
    assertEquals(Long.valueOf(1), entity.getClusterId());
    assertEquals(3, entity.getUpgradeItems().size());

    assertTrue(entity.getUpgradeItems().get(0).getText().contains("Preparing"));
    assertTrue(entity.getUpgradeItems().get(1).getText().contains("Restarting"));
    assertTrue(entity.getUpgradeItems().get(2).getText().contains("Finalizing"));

    verify(amc, clusters, cluster, upgradeDao);
  }


  /**
   * @param amc
   * @return the provider
   */
  private UpgradeResourceProvider createProvider(AmbariManagementController amc) {
    return new UpgradeResourceProvider(amc);
  }

  private Cluster createCluster() {
    Cluster cluster = createMock(Cluster.class);

    expect(cluster.getClusterId()).andReturn(Long.valueOf(1)).anyTimes();
    expect(cluster.getDesiredStackVersion()).andReturn(new StackId("HDP-2.1.1")).atLeastOnce();

    final ServiceComponentHost zk_server_host_comp = createStrictMock(ServiceComponentHost.class);
    Map<String, ServiceComponentHost> zk_host_comp_map = new HashMap<String, ServiceComponentHost>() {{
      put("h1", zk_server_host_comp);
    }};

    final ServiceComponent zk_server_comp = createStrictMock(ServiceComponent.class);
    expect(zk_server_comp.getServiceComponentHosts()).andReturn(zk_host_comp_map).atLeastOnce();

    Map<String, ServiceComponent> zk_comp_map = new HashMap<String, ServiceComponent>() {{
      put("ZOOKEEPER_SERVER", zk_server_comp);
    }};

    final Service zk = createStrictMock(Service.class);
    expect(zk.getServiceComponents()).andReturn(zk_comp_map).atLeastOnce();

    Map<String, Service> servicesMap = new HashMap<String, Service>() {{
      put("ZOOKEEPER", zk);
    }};
    expect(cluster.getServices()).andReturn(servicesMap).anyTimes();

    replay(zk_server_host_comp, zk_server_comp, zk);


    return cluster;
  }

}
