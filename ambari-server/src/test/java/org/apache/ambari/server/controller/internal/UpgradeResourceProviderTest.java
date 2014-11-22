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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.actionmanager.RequestStatus;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.orm.entities.UpgradeItemEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.UpgradeState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

/**
 * UpgradeResourceDefinition tests.
 */
public class UpgradeResourceProviderTest {

  private UpgradeDAO upgradeDao = null;
  private RepositoryVersionDAO repoVersionDao = null;
  private Injector injector;
  private Clusters clusters;

  @Before
  public void before() throws Exception {

    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);

    upgradeDao = injector.getInstance(UpgradeDAO.class);
    repoVersionDao = injector.getInstance(RepositoryVersionDAO.class);

    RepositoryVersionEntity repoVersionEntity = new RepositoryVersionEntity();
    repoVersionEntity.setDisplayName("My New Version");
    repoVersionEntity.setRepositories("");
    repoVersionEntity.setStack("HDP-2.1.1");
    repoVersionEntity.setUpgradePackage("upgrade_test");
    repoVersionEntity.setVersion("2.2.2.2");

    repoVersionDao.create(repoVersionEntity);

    clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1");
    Cluster cluster = clusters.getCluster("c1");
    StackId stackId = new StackId("HDP-2.1.1");
    cluster.setDesiredStackVersion(stackId);
    cluster.createClusterVersion(stackId.getStackName(), stackId.getStackVersion(), "admin", RepositoryVersionState.CURRENT);

    clusters.addHost("h1");
    Host host = clusters.getHost("h1");
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.3");
    host.setHostAttributes(hostAttributes);
    host.persist();

    clusters.mapHostToCluster("h1", "c1");

    // add a single ZK server
    Service service = cluster.addService("ZOOKEEPER");
    service.setDesiredStackVersion(cluster.getDesiredStackVersion());
    service.persist();

    ServiceComponent component = service.addServiceComponent("ZOOKEEPER_SERVER");
    component.addServiceComponentHost("h1");


  }

  @After
  public void after() {
    injector.getInstance(PersistService.class).stop();
    injector = null;
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCreateResources() throws Exception {

    Cluster cluster = clusters.getCluster("c1");

    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(0, upgrades.size());

    AmbariManagementController amc = injector.getInstance(AmbariManagementController.class);

    UpgradeResourceProvider provider = createProvider(amc);

    Map<String, Object> requestProps = new HashMap<String, Object>();
    requestProps.put(UpgradeResourceProvider.UPGRADE_CLUSTER_NAME, "c1");
    requestProps.put(UpgradeResourceProvider.UPGRADE_VERSION, "2.2.2.2");

    Request request = PropertyHelper.getCreateRequest(Collections.singleton(requestProps), null);
    provider.createResources(request);


    upgrades = upgradeDao.findUpgrades(cluster.getClusterId());
    assertEquals(1, upgrades.size());

    UpgradeEntity entity = upgrades.get(0);
    assertEquals(cluster.getClusterId(), entity.getClusterId().longValue());
    assertEquals(3, entity.getUpgradeItems().size());

    assertTrue(entity.getUpgradeItems().get(0).getText().contains("Preparing"));
    assertTrue(entity.getUpgradeItems().get(1).getText().contains("Restarting"));
    assertTrue(entity.getUpgradeItems().get(2).getText().contains("Finalizing"));

    ActionManager am = injector.getInstance(ActionManager.class);
    List<Long> requests = am.getRequestsByStatus(RequestStatus.IN_PROGRESS, 100, true);

    assertEquals(1, requests.size());
    assertEquals(requests.get(0), entity.getRequestId());


    List<Stage> stages = am.getRequestStatus(requests.get(0).longValue());
    assertEquals(3, stages.size());
    for (int i = 0; i < stages.size(); i++) {
      Stage stage = stages.get(i);
      UpgradeItemEntity upgradeItem = entity.getUpgradeItems().get(i);
      assertEquals(stage.getStageId(), upgradeItem.getStageId().longValue());
      assertEquals(UpgradeState.NONE, upgradeItem.getState());
    }


    List<HostRoleCommand> tasks = am.getRequestTasks(requests.get(0).longValue());
    assertEquals(3, tasks.size());
  }


  /**
   * @param amc
   * @return the provider
   */
  private UpgradeResourceProvider createProvider(AmbariManagementController amc) {
    return new UpgradeResourceProvider(amc);
  }


}
