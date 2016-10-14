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

package org.apache.ambari.server.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ServiceComponentResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.UpgradeDAO;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntityPK;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentHistoryEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.state.stack.upgrade.Direction;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import junit.framework.Assert;

public class ServiceComponentTest {

  private static Clusters clusters;
  private static Cluster cluster;
  private static Cluster cluster2;
  private static Service service;
  private static Service service2;
  private static String clusterName;
  private static String cluster2Name;
  private static String serviceName;
  private static Injector injector;
  private static ServiceFactory serviceFactory;
  private static ServiceComponentFactory serviceComponentFactory;
  private static ServiceComponentHostFactory serviceComponentHostFactory;
  private static AmbariMetaInfo metaInfo;
  private static OrmTestHelper helper;
  private static HostDAO hostDAO;

  @BeforeClass
  public static void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    serviceComponentFactory = injector.getInstance(
        ServiceComponentFactory.class);
    serviceComponentHostFactory = injector.getInstance(
        ServiceComponentHostFactory.class);
    helper = injector.getInstance(OrmTestHelper.class);
    hostDAO = injector.getInstance(HostDAO.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);

    clusterName = "foo";
    cluster2Name = "bar";
    serviceName = "HDFS";

    StackId stackId = new StackId("HDP-0.1");
    clusters.addCluster(clusterName, stackId);
    clusters.addCluster(cluster2Name, stackId);

    cluster = clusters.getCluster(clusterName);
    cluster2 = clusters.getCluster(cluster2Name);
    cluster.setDesiredStackVersion(stackId);
    cluster2.setCurrentStackVersion(stackId);
    Assert.assertNotNull(cluster);
    Assert.assertNotNull(cluster2);
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    cluster.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.INSTALLING);
    cluster2.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
      RepositoryVersionState.INSTALLING);

    Service s = serviceFactory.createNew(cluster, serviceName);
    cluster.addService(s);
    s.persist();
    service = cluster.getService(serviceName);
    Assert.assertNotNull(service);

    s = serviceFactory.createNew(cluster2, serviceName);
    cluster2.addService(s);
    s.persist();
    service2 = cluster2.getService(serviceName);
    Assert.assertNotNull(service2);
  }

  @AfterClass
  public static void teardown() throws AmbariException {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testCreateServiceComponent() throws AmbariException {
    String componentName = "DATANODE2";
    ServiceComponent component = serviceComponentFactory.createNew(service,
        componentName);
    service.addServiceComponent(component);
    component.persist();

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    Assert.assertEquals(componentName, sc.getName());
    Assert.assertEquals(serviceName, sc.getServiceName());
    Assert.assertEquals(cluster.getClusterId(),
        sc.getClusterId());
    Assert.assertEquals(cluster.getClusterName(),
        sc.getClusterName());
    Assert.assertEquals(State.INIT, sc.getDesiredState());
    Assert.assertFalse(
        sc.getDesiredStackVersion().getStackId().isEmpty());
  }


  @Test
  public void testGetAndSetServiceComponentInfo() throws AmbariException {
    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service,
        componentName);
    service.addServiceComponent(component);
    component.persist();

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);


    sc.setDesiredState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED, sc.getDesiredState());

    sc.setDesiredStackVersion(new StackId("HDP-1.2.0"));
    Assert.assertEquals("HDP-1.2.0", sc.getDesiredStackVersion().getStackId());

    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO =
        injector.getInstance(ServiceComponentDesiredStateDAO.class);

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    ServiceComponent sc1 = serviceComponentFactory.createExisting(service,
        serviceComponentDesiredStateEntity);
    Assert.assertNotNull(sc1);
    Assert.assertEquals(State.INSTALLED, sc1.getDesiredState());
    Assert.assertEquals("HDP-1.2.0",
        sc1.getDesiredStackVersion().getStackId());

  }

  @Test
  public void testGetAndSetConfigs() {
    // FIXME add unit tests for configs once impl done
    /*
      public Map<String, Config> getDesiredConfigs();
      public void updateDesiredConfigs(Map<String, Config> configs);
     */
  }

  private void addHostToCluster(String hostname,
      String clusterName) throws AmbariException {
    clusters.addHost(hostname);
    Host h = clusters.getHost(hostname);
    h.setIPv4(hostname + "ipv4");
    h.setIPv6(hostname + "ipv6");

    Map<String, String> hostAttributes = new HashMap<String, String>();
	hostAttributes.put("os_family", "redhat");
	hostAttributes.put("os_release_version", "6.3");
	h.setHostAttributes(hostAttributes);


    h.persist();
    clusters.mapHostToCluster(hostname, clusterName);
  }

  @Test
  public void testAddAndGetServiceComponentHosts() throws AmbariException {
    String componentName = "DATANODE";
    ServiceComponent component = serviceComponentFactory.createNew(service,
        componentName);
    service.addServiceComponent(component);
    component.persist();

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    Assert.assertTrue(sc.getServiceComponentHosts().isEmpty());

    try {
      serviceComponentHostFactory.createNew(sc, "h1");
      fail("Expected error for invalid host");
    } catch (Exception e) {
      // Expected
    }

    addHostToCluster("h1", service.getCluster().getClusterName());
    addHostToCluster("h2", service.getCluster().getClusterName());
    addHostToCluster("h3", service.getCluster().getClusterName());

    HostEntity hostEntity1 = hostDAO.findByName("h1");
    assertNotNull(hostEntity1);

    ServiceComponentHost sch1 =
        serviceComponentHostFactory.createNew(sc, "h1");
    ServiceComponentHost sch2 =
        serviceComponentHostFactory.createNew(sc, "h2");
    ServiceComponentHost failSch =
        serviceComponentHostFactory.createNew(sc, "h2");

    Map<String, ServiceComponentHost> compHosts =
        new HashMap<String, ServiceComponentHost>();
    compHosts.put("h1", sch1);
    compHosts.put("h2", sch2);
    compHosts.put("h3", failSch);

    try {
      sc.addServiceComponentHosts(compHosts);
      fail("Expected error for dups");
    } catch (Exception e) {
      // Expected
    }
    Assert.assertTrue(sc.getServiceComponentHosts().isEmpty());

    compHosts.remove("h3");
    sc.addServiceComponentHosts(compHosts);

    Assert.assertEquals(2, sc.getServiceComponentHosts().size());

    sch1.persist();
    sch2.persist();

    ServiceComponentHost schCheck = sc.getServiceComponentHost("h2");
    Assert.assertNotNull(schCheck);
    Assert.assertEquals("h2", schCheck.getHostName());

    ServiceComponentHost sch3 =
        serviceComponentHostFactory.createNew(sc, "h3");
    sc.addServiceComponentHost(sch3);
    sch3.persist();
    Assert.assertNotNull(sc.getServiceComponentHost("h3"));

    sch1.setDesiredStackVersion(new StackId("HDP-1.2.0"));
    sch1.setState(State.STARTING);
    sch1.setStackVersion(new StackId("HDP-1.2.0"));
    sch1.setDesiredState(State.STARTED);

    HostComponentDesiredStateDAO desiredStateDAO = injector.getInstance(
        HostComponentDesiredStateDAO.class);
    HostComponentStateDAO liveStateDAO = injector.getInstance(
        HostComponentStateDAO.class);

    HostComponentDesiredStateEntityPK dPK =
        new HostComponentDesiredStateEntityPK();

    dPK.setClusterId(cluster.getClusterId());
    dPK.setComponentName(componentName);
    dPK.setHostId(hostEntity1.getHostId());
    dPK.setServiceName(serviceName);

    HostComponentDesiredStateEntity desiredStateEntity =
        desiredStateDAO.findByPK(dPK);

    HostComponentStateEntity stateEntity = liveStateDAO.findByIndex(cluster.getClusterId(),
        serviceName, componentName, hostEntity1.getHostId());

    ServiceComponentHost sch = serviceComponentHostFactory.createExisting(sc,
        stateEntity, desiredStateEntity);
    Assert.assertNotNull(sch);
    Assert.assertEquals(State.STARTING, sch.getState());
    Assert.assertEquals(State.STARTED, sch.getDesiredState());
    Assert.assertEquals("HDP-1.2.0",
        sch.getStackVersion().getStackId());
    Assert.assertEquals("HDP-1.2.0",
        sch.getDesiredStackVersion().getStackId());
  }

  @Test
  public void testConvertToResponse() throws AmbariException {
    String componentName = "SECONDARY_NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service,
        componentName);
    service.addServiceComponent(component);
    component.persist();

    addHostToCluster("h12", service.getCluster().getClusterName());
    ServiceComponentHost sch =
      serviceComponentHostFactory.createNew(component, "h12");
    sch.setState(State.INSTALLED);

    Map<String, ServiceComponentHost> compHosts =
      new HashMap<String, ServiceComponentHost>();
    compHosts.put("h12", sch);
    component.addServiceComponentHosts(compHosts);
    Assert.assertEquals(1, component.getServiceComponentHosts().size());
    sch.persist();

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);
    sc.setDesiredState(State.INSTALLED);
    sc.setDesiredStackVersion(new StackId("HDP-1.2.0"));

    ServiceComponentResponse r = sc.convertToResponse();
    Assert.assertEquals(sc.getClusterName(), r.getClusterName());
    Assert.assertEquals(sc.getClusterId(), r.getClusterId().longValue());
    Assert.assertEquals(sc.getName(), r.getComponentName());
    Assert.assertEquals(sc.getServiceName(), r.getServiceName());
    Assert.assertEquals(sc.getDesiredStackVersion().getStackId(),
        r.getDesiredStackVersion());
    Assert.assertEquals(sc.getDesiredState().toString(),
        r.getDesiredState());
    int totalCount = r.getServiceComponentStateCount().get("totalCount");
    int startedCount = r.getServiceComponentStateCount().get("startedCount");
    int installedCount = r.getServiceComponentStateCount().get("installedCount");
    Assert.assertEquals(1, totalCount);
    Assert.assertEquals(0, startedCount);
    Assert.assertEquals(1, installedCount);

    // TODO check configs
    // r.getConfigVersions()

    // TODO test debug dump
    StringBuilder sb = new StringBuilder();
    sc.debugDump(sb);
    Assert.assertFalse(sb.toString().isEmpty());
  }

  @Test
  public void testCanBeRemoved() throws Exception {
    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service,
                                                                   componentName);
    addHostToCluster("h11", service.getCluster().getClusterName());
    ServiceComponentHost sch = serviceComponentHostFactory.createNew(component, "h1");
    component.addServiceComponentHost(sch);

    for (State state : State.values()) {
      component.setDesiredState(state);

      for (State hcState : State.values()) {
        sch.setDesiredState(hcState);
        sch.setState(hcState);

        if (hcState.isRemovableState()) {
          org.junit.Assert.assertTrue(component.canBeRemoved());
        } else {
          org.junit.Assert.assertFalse(component.canBeRemoved());
        }
      }
    }
  }

  @Test
  public void testHistoryCreation() throws AmbariException {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(
        ServiceComponentDesiredStateDAO.class);

    String componentName = "HDFS_CLIENT";
    ServiceComponent component = serviceComponentFactory.createNew(service, componentName);
    service.addServiceComponent(component);
    component.persist();

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    sc.setDesiredState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED, sc.getDesiredState());

    sc.setDesiredStackVersion(new StackId("HDP-2.2.0"));
    StackId stackId = sc.getDesiredStackVersion();
    Assert.assertEquals(new StackId("HDP", "2.2.0"), stackId);

    Assert.assertEquals("HDP-2.2.0", sc.getDesiredStackVersion().getStackId());

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    Assert.assertNotNull(serviceComponentDesiredStateEntity);

    UpgradeEntity upgradeEntity = createUpgradeEntity(cluster, "2.2.0.0", "2.2.0.1");
    ServiceComponentHistoryEntity history = new ServiceComponentHistoryEntity();
    history.setFromStack(serviceComponentDesiredStateEntity.getDesiredStack());
    history.setToStack(serviceComponentDesiredStateEntity.getDesiredStack());
    history.setUpgrade(upgradeEntity);

    serviceComponentDesiredStateEntity.addHistory(history);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.merge(
        serviceComponentDesiredStateEntity);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    assertEquals(1, serviceComponentDesiredStateEntity.getHistory().size());
    ServiceComponentHistoryEntity persistedHistory = serviceComponentDesiredStateEntity.getHistory().iterator().next();

    assertEquals(history.getFromStack(), persistedHistory.getFromStack());
    assertEquals(history.getToStack(), persistedHistory.getFromStack());
    assertEquals(history.getUpgrade(), persistedHistory.getUpgrade());
    assertEquals(history.getServiceComponentDesiredState(), persistedHistory.getServiceComponentDesiredState());
  }


  @Test
  public void testServiceComponentRemove() throws AmbariException {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(
        ServiceComponentDesiredStateDAO.class);

    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service2, componentName);
    service2.addServiceComponent(component);
    component.persist();

    ServiceComponent sc = service2.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    sc.setDesiredState(State.STARTED);
    Assert.assertEquals(State.STARTED, sc.getDesiredState());

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster2.getClusterId(), serviceName, componentName);

    Assert.assertNotNull(serviceComponentDesiredStateEntity);

    Assert.assertTrue(sc.getServiceComponentHosts().isEmpty());

    addHostToCluster("h21", service2.getCluster().getClusterName());
    addHostToCluster("h22", service2.getCluster().getClusterName());

    HostEntity hostEntity1 = hostDAO.findByName("h21");
    assertNotNull(hostEntity1);

    ServiceComponentHost sch1 =
        serviceComponentHostFactory.createNew(sc, "h21");
    ServiceComponentHost sch2 =
        serviceComponentHostFactory.createNew(sc, "h22");

    Map<String, ServiceComponentHost> compHosts =
        new HashMap<String, ServiceComponentHost>();
    compHosts.put("h21", sch1);
    compHosts.put("h22", sch2);
    sc.addServiceComponentHosts(compHosts);

    sch1.setState(State.STARTED);
    sch2.setState(State.STARTED);

    try {
      // delete the SC
      sc.delete();
      Assert.assertTrue("Delete must fail as some SCH are in STARTED state", false);
    }catch(AmbariException e) {
      // expected
    }

    sch1.setState(State.INSTALLED);
    sch2.setState(State.INSTALL_FAILED);
    sc.delete();

    // verify history is gone, too
    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster2.getClusterId(), serviceName, componentName);

    Assert.assertNull(serviceComponentDesiredStateEntity);
 }

  /**
   * Tests the CASCADE nature of removing a service component also removes the
   * history.
   *
   * @throws AmbariException
   */
  @Test
  public void testHistoryRemoval() throws AmbariException {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(
        ServiceComponentDesiredStateDAO.class);

    String componentName = "DATANODE";
    ServiceComponent component = serviceComponentFactory.createNew(service2, componentName);
    service2.addServiceComponent(component);
    component.persist();

    ServiceComponent sc = service2.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    sc.setDesiredState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED, sc.getDesiredState());

    sc.setDesiredStackVersion(new StackId("HDP-2.2.0"));
    StackId stackId = sc.getDesiredStackVersion();
    Assert.assertEquals(new StackId("HDP", "2.2.0"), stackId);

    Assert.assertEquals("HDP-2.2.0", sc.getDesiredStackVersion().getStackId());

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster2.getClusterId(), serviceName, componentName);


    Assert.assertNotNull(serviceComponentDesiredStateEntity);

    UpgradeEntity upgradeEntity = createUpgradeEntity(cluster2, "2.2.0.0", "2.2.0.1");
    ServiceComponentHistoryEntity history = new ServiceComponentHistoryEntity();
    history.setFromStack(serviceComponentDesiredStateEntity.getDesiredStack());
    history.setToStack(serviceComponentDesiredStateEntity.getDesiredStack());
    history.setUpgrade(upgradeEntity);
    history.setServiceComponentDesiredState(serviceComponentDesiredStateEntity);

    serviceComponentDesiredStateEntity.addHistory(history);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.merge(
        serviceComponentDesiredStateEntity);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster2.getClusterId(), serviceName, componentName);

    assertEquals(1, serviceComponentDesiredStateEntity.getHistory().size());

    // verify that we can retrieve the history directly
    List<ServiceComponentHistoryEntity> componentHistoryList = serviceComponentDesiredStateDAO.findHistory(
        sc.getClusterId(), sc.getServiceName(), sc.getName());

    assertEquals(1, componentHistoryList.size());

    // delete the SC
    sc.delete();

    // verify history is gone, too
    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster2.getClusterId(), serviceName, componentName);

    Assert.assertNull(serviceComponentDesiredStateEntity);

    // verify that we cannot retrieve the history directly
    componentHistoryList = serviceComponentDesiredStateDAO.findHistory(sc.getClusterId(),
        sc.getServiceName(), sc.getName());

    assertEquals(0, componentHistoryList.size());
  }

  @Test
  public void testVersionCreation() throws Exception {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(
        ServiceComponentDesiredStateDAO.class);

    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service, componentName);
    service.addServiceComponent(component);
    component.persist();

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    sc.setDesiredState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED, sc.getDesiredState());

    sc.setDesiredStackVersion(new StackId("HDP-2.2.0"));
    StackId stackId = sc.getDesiredStackVersion();
    Assert.assertEquals(new StackId("HDP", "2.2.0"), stackId);

    Assert.assertEquals("HDP-2.2.0", sc.getDesiredStackVersion().getStackId());

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    Assert.assertNotNull(serviceComponentDesiredStateEntity);

    RepositoryVersionEntity rve = new RepositoryVersionEntity(
        serviceComponentDesiredStateEntity.getDesiredStack(), "HDP-2.2.0", "2.2.0.1-1111", "[]");

    RepositoryVersionDAO repositoryDAO = injector.getInstance(RepositoryVersionDAO.class);
    repositoryDAO.create(rve);

    ServiceComponentVersionEntity version = new ServiceComponentVersionEntity();
    version.setState(RepositoryVersionState.CURRENT);
    version.setRepositoryVersion(rve);
    version.setUserName("user");
    serviceComponentDesiredStateEntity.addVersion(version);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.merge(
        serviceComponentDesiredStateEntity);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    assertEquals(1, serviceComponentDesiredStateEntity.getVersions().size());
    ServiceComponentVersionEntity persistedVersion = serviceComponentDesiredStateEntity.getVersions().iterator().next();

    assertEquals(RepositoryVersionState.CURRENT, persistedVersion.getState());
  }

  @Test
  public void testVersionRemoval() throws Exception {
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(
        ServiceComponentDesiredStateDAO.class);

    String componentName = "NAMENODE";
    ServiceComponent component = serviceComponentFactory.createNew(service, componentName);
    service.addServiceComponent(component);
    component.persist();

    ServiceComponent sc = service.getServiceComponent(componentName);
    Assert.assertNotNull(sc);

    sc.setDesiredState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED, sc.getDesiredState());

    sc.setDesiredStackVersion(new StackId("HDP-2.2.0"));
    StackId stackId = sc.getDesiredStackVersion();
    Assert.assertEquals(new StackId("HDP", "2.2.0"), stackId);

    Assert.assertEquals("HDP-2.2.0", sc.getDesiredStackVersion().getStackId());

    ServiceComponentDesiredStateEntity serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    Assert.assertNotNull(serviceComponentDesiredStateEntity);

    RepositoryVersionEntity rve = new RepositoryVersionEntity(
        serviceComponentDesiredStateEntity.getDesiredStack(), "HDP-2.2.0", "2.2.0.1-1111", "[]");

    RepositoryVersionDAO repositoryDAO = injector.getInstance(RepositoryVersionDAO.class);
    repositoryDAO.create(rve);

    ServiceComponentVersionEntity version = new ServiceComponentVersionEntity();
    version.setState(RepositoryVersionState.CURRENT);
    version.setRepositoryVersion(rve);
    version.setUserName("user");
    serviceComponentDesiredStateEntity.addVersion(version);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.merge(
        serviceComponentDesiredStateEntity);

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);

    assertEquals(1, serviceComponentDesiredStateEntity.getVersions().size());
    ServiceComponentVersionEntity persistedVersion = serviceComponentDesiredStateEntity.getVersions().iterator().next();

    assertEquals(RepositoryVersionState.CURRENT, persistedVersion.getState());

    sc.delete();

    serviceComponentDesiredStateEntity = serviceComponentDesiredStateDAO.findByName(
        cluster.getClusterId(), serviceName, componentName);
    Assert.assertNull(serviceComponentDesiredStateEntity);


    // verify versions are gone, too
    List<ServiceComponentVersionEntity> list = serviceComponentDesiredStateDAO.findVersions(cluster.getClusterId(), serviceName, componentName);
    assertEquals(0, list.size());
  }

  /**
   * Creates an upgrade entity, asserting it was created correctly.
   *
   * @param fromVersion
   * @param toVersion
   * @return
   */
  private UpgradeEntity createUpgradeEntity(Cluster cl, String fromVersion, String toVersion) {
    UpgradeDAO upgradeDao = injector.getInstance(UpgradeDAO.class);
    UpgradeEntity upgradeEntity = new UpgradeEntity();
    upgradeEntity.setClusterId(cl.getClusterId());
    upgradeEntity.setDirection(Direction.UPGRADE);
    upgradeEntity.setFromVersion(fromVersion);
    upgradeEntity.setToVersion(toVersion);
    upgradeEntity.setUpgradePackage("upgrade_test");
    upgradeEntity.setUpgradeType(UpgradeType.ROLLING);
    upgradeEntity.setRequestId(1L);

    upgradeDao.create(upgradeEntity);
    List<UpgradeEntity> upgrades = upgradeDao.findUpgrades(cl.getClusterId());
    assertEquals(1, upgrades.size());
    return upgradeEntity;
  }

}
