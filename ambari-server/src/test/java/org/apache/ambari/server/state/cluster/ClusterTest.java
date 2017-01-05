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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.AgentEnv.Directory;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.events.ClusterConfigChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.OrmTestHelper;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterVersionDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.ClusterConfigEntity;
import org.apache.ambari.server.orm.entities.ClusterConfigMappingEntity;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.ClusterVersionEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.server.state.configgroup.ConfigGroup;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.apache.ambari.server.state.stack.upgrade.UpgradeType;
import org.apache.ambari.server.utils.EventBusSynchronizer;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import com.google.inject.util.Modules;

import junit.framework.Assert;

public class ClusterTest {

  private static final EnumSet<RepositoryVersionState> TERMINAL_VERSION_STATES =
      EnumSet.of(RepositoryVersionState.CURRENT, RepositoryVersionState.INSTALLED);

  private Clusters clusters;
  private Cluster c1;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private ServiceComponentFactory serviceComponentFactory;
  private ServiceComponentHostFactory serviceComponentHostFactory;
  private AmbariMetaInfo metaInfo;
  private ConfigFactory configFactory;
  private ConfigGroupFactory configGroupFactory;
  private OrmTestHelper helper;
  private StackDAO stackDAO;
  private ClusterDAO clusterDAO;
  private HostDAO hostDAO;
  private ClusterVersionDAO clusterVersionDAO;
  private HostVersionDAO hostVersionDAO;
  private HostComponentStateDAO hostComponentStateDAO;
  private RepositoryVersionDAO repositoryVersionDAO;
  private Gson gson;

  @Singleton
  static class ClusterVersionDAOMock extends ClusterVersionDAO {
    static boolean failOnCurrentVersionState;
    static List<ClusterVersionEntity> mockedClusterVersions;

    @Override
    @Transactional
    public ClusterVersionEntity merge(ClusterVersionEntity entity) {
      if (!failOnCurrentVersionState || entity.getState() != RepositoryVersionState.CURRENT) {
        return super.merge(entity);
      } else {
        throw new RollbackException();
      }
    }

    @Override
    @Transactional
    public List<ClusterVersionEntity> findByCluster(String clusterName) {
      if (mockedClusterVersions == null) {
        return super.findByCluster(clusterName);
      } else {
        return mockedClusterVersions;
      }
    }
  }

  private static class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(ClusterVersionDAO.class).to(ClusterVersionDAOMock.class);
      EventBusSynchronizer.synchronizeAmbariEventPublisher(binder());
    }
  }

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(Modules.override(new InMemoryDefaultTestModule()).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
    configGroupFactory = injector.getInstance(ConfigGroupFactory.class);
    serviceComponentFactory = injector.getInstance(
        ServiceComponentFactory.class);
    serviceComponentHostFactory = injector.getInstance(
        ServiceComponentHostFactory.class);
    configFactory = injector.getInstance(ConfigFactory.class);
    metaInfo = injector.getInstance(AmbariMetaInfo.class);
    helper = injector.getInstance(OrmTestHelper.class);
    stackDAO = injector.getInstance(StackDAO.class);
    clusterDAO = injector.getInstance(ClusterDAO.class);
    hostDAO = injector.getInstance(HostDAO.class);
    clusterVersionDAO = injector.getInstance(ClusterVersionDAO.class);
    hostVersionDAO = injector.getInstance(HostVersionDAO.class);
    hostComponentStateDAO = injector.getInstance(HostComponentStateDAO.class);
    repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    gson = injector.getInstance(Gson.class);
    injector.getInstance(UnitOfWork.class).begin();
  }

  @After
  public void teardown() {
    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(PersistService.class).stop();
  }

  private void createDefaultCluster() throws Exception {
    // TODO, use common function
    StackId stackId = new StackId("HDP", "0.1");
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
    org.junit.Assert.assertNotNull(stackEntity);

    String clusterName = "c1";

    clusters.addCluster(clusterName, stackId);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");

    Set<String> hostNames = new HashSet<String>() {{ add("h1"); add("h2"); }};
    for (String hostName : hostNames) {
      clusters.addHost(hostName);

      HostEntity hostEntity = hostDAO.findByName(hostName);
      hostEntity.setIpv4("ipv4");
      hostEntity.setIpv6("ipv6");
      hostEntity.setHostAttributes(gson.toJson(hostAttributes));
      hostDAO.merge(hostEntity);
    }

    clusters.mapAndPublishHostsToCluster(hostNames, clusterName);
    c1 = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    c1.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.INSTALLING);
    c1.transitionClusterVersion(stackId, stackId.getStackVersion(),
        RepositoryVersionState.CURRENT);

    ClusterVersionDAOMock.failOnCurrentVersionState = false;
  }

  public ClusterEntity createDummyData() {
    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("0.1");

    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1L);
    clusterEntity.setClusterName("test_cluster1");
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setDesiredStack(stackEntity);

    HostEntity host1 = new HostEntity();
    HostEntity host2 = new HostEntity();
    HostEntity host3 = new HostEntity();

    host1.setHostName("test_host1");
    host2.setHostName("test_host2");
    host3.setHostName("test_host3");
    host1.setIpv4("192.168.0.1");
    host2.setIpv4("192.168.0.2");
    host3.setIpv4("192.168.0.3");

    List<HostEntity> hostEntities = new ArrayList<HostEntity>();
    hostEntities.add(host1);
    hostEntities.add(host2);

    clusterEntity.setHostEntities(hostEntities);
    clusterEntity.setClusterConfigEntities(Collections.<ClusterConfigEntity>emptyList());
    //both sides of relation should be set when modifying in runtime
    host1.setClusterEntities(Arrays.asList(clusterEntity));
    host2.setClusterEntities(Arrays.asList(clusterEntity));

    HostStateEntity hostStateEntity1 = new HostStateEntity();
    hostStateEntity1.setCurrentState(HostState.HEARTBEAT_LOST);
    hostStateEntity1.setHostEntity(host1);
    HostStateEntity hostStateEntity2 = new HostStateEntity();
    hostStateEntity2.setCurrentState(HostState.HEALTHY);
    hostStateEntity2.setHostEntity(host2);
    host1.setHostStateEntity(hostStateEntity1);
    host2.setHostStateEntity(hostStateEntity2);

    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setServiceName("HDFS");
    clusterServiceEntity.setClusterEntity(clusterEntity);
    clusterServiceEntity.setServiceComponentDesiredStateEntities(
        Collections.<ServiceComponentDesiredStateEntity>emptyList());

    ServiceDesiredStateEntity stateEntity = mock(ServiceDesiredStateEntity.class);

    when(stateEntity.getDesiredStack()).thenReturn(stackEntity);

    clusterServiceEntity.setServiceDesiredStateEntity(stateEntity);
    List<ClusterServiceEntity> clusterServiceEntities = new ArrayList<ClusterServiceEntity>();
    clusterServiceEntities.add(clusterServiceEntity);
    clusterEntity.setClusterServiceEntities(clusterServiceEntities);
    return clusterEntity;
  }

  private void checkStackVersionState(StackId stackId, String version, RepositoryVersionState state) {
    Collection<ClusterVersionEntity> allClusterVersions = c1.getAllClusterVersions();
    for (ClusterVersionEntity entity : allClusterVersions) {
      StackId repoVersionStackId = new StackId(entity.getRepositoryVersion().getStack());
      if (repoVersionStackId.equals(stackId)
          && repoVersionStackId.getStackVersion().equals(version)) {
        assertEquals(state, entity.getState());
      }
    }
  }

  private void assertStateException(StackId stackId, String version,
      RepositoryVersionState transitionState,
                                    RepositoryVersionState stateAfter) {
    try {
      c1.transitionClusterVersion(stackId, version, transitionState);
      Assert.fail();
    } catch (AmbariException e) {}
    checkStackVersionState(stackId, version, stateAfter);
    assertNotNull(c1.getCurrentClusterVersion());
  }

  /**
   * For Rolling Upgrades, create a cluster with the following components
   * HDFS: NameNode, DataNode, HDFS Client
   * ZK: Zookeeper Server, Zookeeper Monitor
   * Ganglia: Ganglia Server, Ganglia Monitor
   *
   * Further, 3 hosts will be added.
   * Finally, verify that only the Ganglia components do not need to advertise a version.
   * @param clusterName Cluster Name
   * @param stackId Stack to set for the cluster
   * @param hostAttributes Host attributes to use for 3 hosts (h-1, h-2, h-3)
   * @throws Exception
   * @return Cluster that was created
   */
  private Cluster createClusterForRU(String clusterName, StackId stackId, Map<String, String> hostAttributes) throws Exception {
    clusters.addCluster(clusterName, stackId);
    Cluster cluster = clusters.getCluster(clusterName);
    Assert.assertEquals(clusterName, cluster.getClusterName());
    Assert.assertEquals(1, cluster.getClusterId());

    // Add Hosts
    List<String> hostNames = new ArrayList<String>() {{ add("h-1"); add("h-2"); add("h-3"); }};
    for(String hostName : hostNames) {
      addHost(hostName, hostAttributes);
    }

    // Add stack and map Hosts to cluster
    cluster.setDesiredStackVersion(stackId);
    cluster.setCurrentStackVersion(stackId);
    for(String hostName : hostNames) {
      clusters.mapHostToCluster(hostName, clusterName);
    }

    // Transition all hosts to HEALTHY state
    for (Host host : cluster.getHosts()) {
      host.setState(HostState.HEALTHY);
    }

    // Add Services
    Service s1 = serviceFactory.createNew(cluster, "HDFS");
    Service s2 = serviceFactory.createNew(cluster, "ZOOKEEPER");
    Service s3 = serviceFactory.createNew(cluster, "GANGLIA");
    cluster.addService(s1);
    cluster.addService(s2);
    cluster.addService(s3);

    // Add HDFS components
    ServiceComponent sc1CompA = serviceComponentFactory.createNew(s1, "NAMENODE");
    ServiceComponent sc1CompB = serviceComponentFactory.createNew(s1, "DATANODE");
    ServiceComponent sc1CompC = serviceComponentFactory.createNew(s1, "HDFS_CLIENT");
    s1.addServiceComponent(sc1CompA);
    s1.addServiceComponent(sc1CompB);
    s1.addServiceComponent(sc1CompC);

    // Add ZK
    ServiceComponent sc2CompA = serviceComponentFactory.createNew(s2, "ZOOKEEPER_SERVER");
    ServiceComponent sc2CompB = serviceComponentFactory.createNew(s2, "ZOOKEEPER_CLIENT");
    s2.addServiceComponent(sc2CompA);
    s2.addServiceComponent(sc2CompB);

    // Add Ganglia
    ServiceComponent sc3CompA = serviceComponentFactory.createNew(s3, "GANGLIA_SERVER");
    ServiceComponent sc3CompB = serviceComponentFactory.createNew(s3, "GANGLIA_MONITOR");
    s3.addServiceComponent(sc3CompA);
    s3.addServiceComponent(sc3CompB);

    // Host 1 will have all components
    ServiceComponentHost schHost1Serv1CompA = serviceComponentHostFactory.createNew(sc1CompA, "h-1");
    ServiceComponentHost schHost1Serv1CompB = serviceComponentHostFactory.createNew(sc1CompB, "h-1");
    ServiceComponentHost schHost1Serv1CompC = serviceComponentHostFactory.createNew(sc1CompC, "h-1");
    ServiceComponentHost schHost1Serv2CompA = serviceComponentHostFactory.createNew(sc2CompA, "h-1");
    ServiceComponentHost schHost1Serv2CompB = serviceComponentHostFactory.createNew(sc2CompB, "h-1");
    ServiceComponentHost schHost1Serv3CompA = serviceComponentHostFactory.createNew(sc3CompA, "h-1");
    ServiceComponentHost schHost1Serv3CompB = serviceComponentHostFactory.createNew(sc3CompB, "h-1");
    sc1CompA.addServiceComponentHost(schHost1Serv1CompA);
    sc1CompB.addServiceComponentHost(schHost1Serv1CompB);
    sc1CompC.addServiceComponentHost(schHost1Serv1CompC);
    sc2CompA.addServiceComponentHost(schHost1Serv2CompA);
    sc2CompB.addServiceComponentHost(schHost1Serv2CompB);
    sc3CompA.addServiceComponentHost(schHost1Serv3CompA);
    sc3CompB.addServiceComponentHost(schHost1Serv3CompB);

    // Host 2 will have ZK_CLIENT and GANGLIA_MONITOR
    ServiceComponentHost schHost2Serv2CompB = serviceComponentHostFactory.createNew(sc2CompB, "h-2");
    ServiceComponentHost schHost2Serv3CompB = serviceComponentHostFactory.createNew(sc3CompB, "h-2");
    sc2CompB.addServiceComponentHost(schHost2Serv2CompB);
    sc3CompB.addServiceComponentHost(schHost2Serv3CompB);

    // Host 3 will have GANGLIA_MONITOR
    ServiceComponentHost schHost3Serv3CompB = serviceComponentHostFactory.createNew(sc3CompB, "h-3");
    sc3CompB.addServiceComponentHost(schHost3Serv3CompB);

    // Verify count of components
    List<ServiceComponentHost> scHost1 = cluster.getServiceComponentHosts("h-1");
    Assert.assertEquals(7, scHost1.size());

    List<ServiceComponentHost> scHost2 = cluster.getServiceComponentHosts("h-2");
    Assert.assertEquals(2, scHost2.size());

    List<ServiceComponentHost> scHost3 = cluster.getServiceComponentHosts("h-3");
    Assert.assertEquals(1, scHost3.size());

    //<editor-fold desc="Validate Version Advertised">
    /*
    For some reason this still uses the metainfo.xml files for these services
    from HDP-2.0.5 stack instead of the provided Stack Id
    */
    HashMap<String, Set<String>> componentsThatAdvertiseVersion = new HashMap<String, Set<String>>();
    HashMap<String, Set<String>> componentsThatDontAdvertiseVersion = new HashMap<String, Set<String>>();

    Set<String> hdfsComponents = new HashSet<String>() {{ add("NAMENODE"); add("DATANODE"); add("HDFS_CLIENT"); }};
    Set<String> zkComponents = new HashSet<String>() {{ add("ZOOKEEPER_SERVER"); add("ZOOKEEPER_CLIENT"); }};
    Set<String> gangliaComponents = new HashSet<String>() {{ add("GANGLIA_SERVER"); add("GANGLIA_MONITOR"); }};

    componentsThatAdvertiseVersion.put("HDFS", hdfsComponents);
    componentsThatAdvertiseVersion.put("ZOOKEEPER", zkComponents);
    componentsThatDontAdvertiseVersion.put("GANGLIA", gangliaComponents);

    for(String service : componentsThatAdvertiseVersion.keySet())  {
      Set<String> components = componentsThatAdvertiseVersion.get(service);
      for(String componentName : components) {
        ComponentInfo component = metaInfo.getComponent(stackId.getStackName(), stackId.getStackVersion(), service, componentName);
        Assert.assertTrue(component.isVersionAdvertised());
      }
    }

    for(String service : componentsThatDontAdvertiseVersion.keySet())  {
      Set<String> components = componentsThatDontAdvertiseVersion.get(service);
      for(String componentName : components) {
        ComponentInfo component = metaInfo.getComponent(stackId.getStackName(), stackId.getStackVersion(), service, componentName);
        Assert.assertFalse(component.isVersionAdvertised());
      }
    }
    //</editor-fold>

    return cluster;
  }

  /**
   * Add a host to the system with the provided attributes.
   * @param hostName Host Name
   * @param hostAttributes Host Attributes
   * @throws Exception
   */
  private void addHost(String hostName, Map<String, String> hostAttributes) throws Exception {
    clusters.addHost(hostName);
    Host host = clusters.getHost(hostName);
    host.setIPv4("ipv4");
    host.setIPv6("ipv6");
    host.setHostAttributes(hostAttributes);
  }

  /**
   * For the provided collection of HostComponentStates, set the version to {@paramref version} if the Component
   * can advertise a version. Then, simulate the {@link org.apache.ambari.server.events.listeners.upgrade.StackVersionListener}
   * by calling methods to transition the HostVersion, and recalculate the ClusterVersion.
   * @param stackId Stack ID to retrieve the ComponentInfo
   * @param version Version to set
   * @param cluster Cluster to retrieve services from
   * @param hostComponentStates Collection to set the version for
   * @throws Exception
   */
  private void simulateStackVersionListener(StackId stackId, String version, Cluster cluster, List<HostComponentStateEntity> hostComponentStates) throws Exception {
    for(int i = 0; i < hostComponentStates.size(); i++) {
      HostComponentStateEntity hce = hostComponentStates.get(i);
      ComponentInfo compInfo = metaInfo.getComponent(
          stackId.getStackName(), stackId.getStackVersion(),
          hce.getServiceName(),
          hce.getComponentName());

      if (compInfo.isVersionAdvertised()) {
        hce.setVersion(version);
        hostComponentStateDAO.merge(hce);
      }

      RepositoryVersionEntity rv = helper.getOrCreateRepositoryVersion(stackId, version);

      // Simulate the StackVersionListener during the installation
      Service svc = cluster.getService(hce.getServiceName());
      ServiceComponent svcComp = svc.getServiceComponent(hce.getComponentName());
      ServiceComponentHost scHost = svcComp.getServiceComponentHost(hce.getHostName());

      scHost.recalculateHostVersionState();
      cluster.recalculateClusterVersionState(rv);
    }
  }

  @Test
  public void testAddHost() throws Exception {
    createDefaultCluster();
    clusters.addHost("h3");

    try {
      clusters.addHost("h3");
      fail("Duplicate add should fail");
    }
    catch (AmbariException e) {
      // Expected
    }
  }

  @Test
  public void testGetHostState() throws Exception {
    createDefaultCluster();

    Assert.assertEquals(HostState.INIT, clusters.getHost("h1").getState());
  }

  @Test
  public void testSetHostState() throws Exception {
    createDefaultCluster();

    clusters.getHost("h1").setState(HostState.HEARTBEAT_LOST);
    Assert.assertEquals(HostState.HEARTBEAT_LOST,
        clusters.getHost("h1").getState());
  }

  @Test
  public void testHostEvent() throws Exception,
      InvalidStateTransitionException {
    createDefaultCluster();

    HostInfo hostInfo = new HostInfo();
    hostInfo.setHostName("h1");
    hostInfo.setInterfaces("fip_4");
    hostInfo.setArchitecture("os_arch");
    hostInfo.setOS("os_type");
    hostInfo.setMemoryTotal(10);
    hostInfo.setMemorySize(100);
    hostInfo.setProcessorCount(10);
    List<DiskInfo> mounts = new ArrayList<DiskInfo>();
    mounts.add(new DiskInfo("/dev/sda", "/mnt/disk1",
        "5000000", "4000000", "10%", "size", "fstype"));
    hostInfo.setMounts(mounts);

    AgentEnv agentEnv = new AgentEnv();

    Directory dir1 = new Directory();
    dir1.setName("/etc/hadoop");
    dir1.setType("not_exist");
    Directory dir2 = new Directory();
    dir2.setName("/var/log/hadoop");
    dir2.setType("not_exist");
    agentEnv.setStackFoldersAndFiles(new Directory[] { dir1, dir2 });

    AgentVersion agentVersion = new AgentVersion("0.0.x");
    long currentTime = 1001;

    clusters.getHost("h1").handleEvent(new HostRegistrationRequestEvent(
        "h1", agentVersion, currentTime, hostInfo, agentEnv));

    Assert.assertEquals(HostState.WAITING_FOR_HOST_STATUS_UPDATES,
        clusters.getHost("h1").getState());

    clusters.getHost("h1").setState(HostState.HEARTBEAT_LOST);

    try {
      clusters.getHost("h1").handleEvent(
          new HostHealthyHeartbeatEvent("h1", currentTime, null, null));
      fail("Exception should be thrown on invalid event");
    }
    catch (InvalidStateTransitionException e) {
      // Expected
    }

  }

  @Test
  public void testBasicClusterSetup() throws Exception {
    StackId stackVersion = new StackId("HDP-1.2.0");

    createDefaultCluster();

    String clusterName = "c2";

    try {
      clusters.getCluster(clusterName);
      fail("Exception expected for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster(clusterName, stackVersion);
    Cluster c2 = clusters.getCluster(clusterName);

    Assert.assertNotNull(c2);

    Assert.assertEquals(clusterName, c2.getClusterName());

    c2.setClusterName("foo2");
    Assert.assertEquals("foo2", c2.getClusterName());

    Assert.assertNotNull(c2.getDesiredStackVersion());
    Assert.assertEquals("HDP-1.2.0", c2.getDesiredStackVersion().getStackId());
  }

  @Test
  public void testAddAndGetServices() throws Exception {
    createDefaultCluster();

    // TODO write unit tests for
    // public void addService(Service service) throws AmbariException;
    // public Service getService(String serviceName) throws AmbariException;
    // public Map<String, Service> getServices();

    Service s1 = serviceFactory.createNew(c1, "HDFS");
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE");

    Service s = c1.getService("HDFS");
    Assert.assertNotNull(s);
    Assert.assertEquals("HDFS", s.getName());
    Assert.assertEquals(c1.getClusterId(), s.getClusterId());

    try {
      c1.getService("HBASE");
      fail("Expected error for unknown service");
    } catch (Exception e) {
      // Expected
    }

    Map<String, Service> services = c1.getServices();
    Assert.assertEquals(2, services.size());
    Assert.assertTrue(services.containsKey("HDFS"));
    Assert.assertTrue(services.containsKey("MAPREDUCE"));
  }

  @Test
  public void testGetServiceComponentHosts() throws Exception {
    createDefaultCluster();

    // TODO write unit tests
    // public List<ServiceComponentHost> getServiceComponentHosts(String hostname);

    Service s = serviceFactory.createNew(c1, "HDFS");
    c1.addService(s);
    ServiceComponent sc = serviceComponentFactory.createNew(s, "NAMENODE");
    s.addServiceComponent(sc);

    ServiceComponentHost sch =
        serviceComponentHostFactory.createNew(sc, "h1");
    sc.addServiceComponentHost(sch);

    List<ServiceComponentHost> scHosts = c1.getServiceComponentHosts("h1");
    Assert.assertEquals(1, scHosts.size());

    Iterator<ServiceComponentHost> iterator = scHosts.iterator();

    //Try to iterate on sch and modify it in loop
    try {
      while (iterator.hasNext()) {
        iterator.next();
        Service s1 = serviceFactory.createNew(c1, "PIG");
        c1.addService(s1);
        ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "PIG");
        s1.addServiceComponent(sc1);
        ServiceComponentHost sch1 = serviceComponentHostFactory.createNew(sc1, "h1");
        sc1.addServiceComponentHost(sch1);
      }
    } catch (ConcurrentModificationException e ) {
      Assert.assertTrue("Failed to work concurrently with sch", false);
    }

    scHosts = c1.getServiceComponentHosts("h1");
    Assert.assertEquals(2, scHosts.size());
  }

  @Test
  public void testGetServiceComponentHosts_ForService() throws Exception {
    createDefaultCluster();

    Service s = serviceFactory.createNew(c1, "HDFS");
    c1.addService(s);

    ServiceComponent scNN = serviceComponentFactory.createNew(s, "NAMENODE");
    s.addServiceComponent(scNN);
    ServiceComponentHost schNNH1 = serviceComponentHostFactory.createNew(scNN, "h1");
    scNN.addServiceComponentHost(schNNH1);

    ServiceComponent scDN = serviceComponentFactory.createNew(s, "DATANODE");
    s.addServiceComponent(scDN);
    ServiceComponentHost scDNH1 = serviceComponentHostFactory.createNew(scDN, "h1");
    scDN.addServiceComponentHost(scDNH1);
    ServiceComponentHost scDNH2 = serviceComponentHostFactory.createNew(scDN, "h2");
    scDN.addServiceComponentHost(scDNH2);

    List<ServiceComponentHost> scHosts;

    scHosts = c1.getServiceComponentHosts("HDFS", null);
    Assert.assertEquals(3, scHosts.size());

    scHosts = c1.getServiceComponentHosts("UNKNOWN SERVICE", null);
    Assert.assertEquals(0, scHosts.size());
  }

  @Test
  public void testGetServiceComponentHosts_ForServiceComponent() throws Exception {
    createDefaultCluster();

    Service s = serviceFactory.createNew(c1, "HDFS");
    c1.addService(s);

    ServiceComponent scNN = serviceComponentFactory.createNew(s, "NAMENODE");
    s.addServiceComponent(scNN);
    ServiceComponentHost schNNH1 = serviceComponentHostFactory.createNew(scNN, "h1");
    scNN.addServiceComponentHost(schNNH1);

    ServiceComponent scDN = serviceComponentFactory.createNew(s, "DATANODE");
    s.addServiceComponent(scDN);
    ServiceComponentHost scDNH1 = serviceComponentHostFactory.createNew(scDN, "h1");
    scDN.addServiceComponentHost(scDNH1);
    ServiceComponentHost scDNH2 = serviceComponentHostFactory.createNew(scDN, "h2");
    scDN.addServiceComponentHost(scDNH2);

    List<ServiceComponentHost> scHosts;

    scHosts = c1.getServiceComponentHosts("HDFS", "DATANODE");
    Assert.assertEquals(2, scHosts.size());

    scHosts = c1.getServiceComponentHosts("HDFS", "UNKNOWN COMPONENT");
    Assert.assertEquals(0, scHosts.size());

    scHosts = c1.getServiceComponentHosts("UNKNOWN SERVICE", "DATANODE");
    Assert.assertEquals(0, scHosts.size());

    scHosts = c1.getServiceComponentHosts("UNKNOWN SERVICE", "UNKNOWN COMPONENT");
    Assert.assertEquals(0, scHosts.size());
  }

  @Test
  public void testGetServiceComponentHostMap() throws Exception {
    createDefaultCluster();

    Service s = serviceFactory.createNew(c1, "HDFS");
    c1.addService(s);

    ServiceComponent scNN = serviceComponentFactory.createNew(s, "NAMENODE");
    s.addServiceComponent(scNN);
    ServiceComponentHost schNNH1 = serviceComponentHostFactory.createNew(scNN, "h1");
    scNN.addServiceComponentHost(schNNH1);

    ServiceComponent scDN = serviceComponentFactory.createNew(s, "DATANODE");
    s.addServiceComponent(scDN);
    ServiceComponentHost scDNH1 = serviceComponentHostFactory.createNew(scDN, "h1");
    scDN.addServiceComponentHost(scDNH1);
    ServiceComponentHost scDNH2 = serviceComponentHostFactory.createNew(scDN, "h2");
    scDN.addServiceComponentHost(scDNH2);

    Map<String, Set<String>> componentHostMap;

    componentHostMap = c1.getServiceComponentHostMap(null, null);
    Assert.assertEquals(2, componentHostMap.size());

    Assert.assertEquals(1, componentHostMap.get("NAMENODE").size());
    Assert.assertTrue(componentHostMap.get("NAMENODE").contains("h1"));

    Assert.assertEquals(2, componentHostMap.get("DATANODE").size());
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h1"));
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h2"));
  }

  @Test
  public void testGetServiceComponentHostMap_ForService() throws Exception {
    createDefaultCluster();

    Service sfHDFS = serviceFactory.createNew(c1, "HDFS");
    c1.addService(sfHDFS);

    Service sfMR = serviceFactory.createNew(c1, "MAPREDUCE");
    c1.addService(sfMR);

    ServiceComponent scNN = serviceComponentFactory.createNew(sfHDFS, "NAMENODE");
    sfHDFS.addServiceComponent(scNN);
    ServiceComponentHost schNNH1 = serviceComponentHostFactory.createNew(scNN, "h1");
    scNN.addServiceComponentHost(schNNH1);

    ServiceComponent scDN = serviceComponentFactory.createNew(sfHDFS, "DATANODE");
    sfHDFS.addServiceComponent(scDN);
    ServiceComponentHost scDNH1 = serviceComponentHostFactory.createNew(scDN, "h1");
    scDN.addServiceComponentHost(scDNH1);
    ServiceComponentHost scDNH2 = serviceComponentHostFactory.createNew(scDN, "h2");
    scDN.addServiceComponentHost(scDNH2);

    ServiceComponent scJT = serviceComponentFactory.createNew(sfMR, "JOBTRACKER");
    sfMR.addServiceComponent(scJT);
    ServiceComponentHost schJTH1 = serviceComponentHostFactory.createNew(scJT, "h1");
    scJT.addServiceComponentHost(schJTH1);

    Map<String, Set<String>> componentHostMap;

    componentHostMap = c1.getServiceComponentHostMap(null, Collections.singleton("HDFS"));
    Assert.assertEquals(2, componentHostMap.size());
    Assert.assertEquals(1, componentHostMap.get("NAMENODE").size());
    Assert.assertTrue(componentHostMap.get("NAMENODE").contains("h1"));
    Assert.assertEquals(2, componentHostMap.get("DATANODE").size());
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h1"));
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h2"));

    componentHostMap = c1.getServiceComponentHostMap(null, Collections.singleton("MAPREDUCE"));
    Assert.assertEquals(1, componentHostMap.size());
    Assert.assertEquals(1, componentHostMap.get("JOBTRACKER").size());
    Assert.assertTrue(componentHostMap.get("JOBTRACKER").contains("h1"));

    componentHostMap = c1.getServiceComponentHostMap(null, new HashSet<String>(Arrays.asList("HDFS", "MAPREDUCE")));
    Assert.assertEquals(3, componentHostMap.size());
    Assert.assertEquals(1, componentHostMap.get("NAMENODE").size());
    Assert.assertTrue(componentHostMap.get("NAMENODE").contains("h1"));
    Assert.assertEquals(2, componentHostMap.get("DATANODE").size());
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h1"));
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h2"));
    Assert.assertEquals(1, componentHostMap.get("JOBTRACKER").size());
    Assert.assertTrue(componentHostMap.get("JOBTRACKER").contains("h1"));

    componentHostMap = c1.getServiceComponentHostMap(null, Collections.singleton("UNKNOWN"));
    Assert.assertEquals(0, componentHostMap.size());
  }

  @Test
  public void testGetServiceComponentHostMap_ForHost() throws Exception {
    createDefaultCluster();

    Service sfHDFS = serviceFactory.createNew(c1, "HDFS");
    c1.addService(sfHDFS);

    Service sfMR = serviceFactory.createNew(c1, "MAPREDUCE");
    c1.addService(sfMR);

    ServiceComponent scNN = serviceComponentFactory.createNew(sfHDFS, "NAMENODE");
    sfHDFS.addServiceComponent(scNN);
    ServiceComponentHost schNNH1 = serviceComponentHostFactory.createNew(scNN, "h1");
    scNN.addServiceComponentHost(schNNH1);

    ServiceComponent scDN = serviceComponentFactory.createNew(sfHDFS, "DATANODE");
    sfHDFS.addServiceComponent(scDN);
    ServiceComponentHost scDNH1 = serviceComponentHostFactory.createNew(scDN, "h1");
    scDN.addServiceComponentHost(scDNH1);
    ServiceComponentHost scDNH2 = serviceComponentHostFactory.createNew(scDN, "h2");
    scDN.addServiceComponentHost(scDNH2);

    ServiceComponent scJT = serviceComponentFactory.createNew(sfMR, "JOBTRACKER");
    sfMR.addServiceComponent(scJT);
    ServiceComponentHost schJTH1 = serviceComponentHostFactory.createNew(scJT, "h1");
    scJT.addServiceComponentHost(schJTH1);

    Map<String, Set<String>> componentHostMap;

    componentHostMap = c1.getServiceComponentHostMap(Collections.singleton("h1"), null);
    Assert.assertEquals(3, componentHostMap.size());
    Assert.assertEquals(1, componentHostMap.get("NAMENODE").size());
    Assert.assertTrue(componentHostMap.get("NAMENODE").contains("h1"));
    Assert.assertEquals(1, componentHostMap.get("DATANODE").size());
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h1"));
    Assert.assertEquals(1, componentHostMap.get("JOBTRACKER").size());
    Assert.assertTrue(componentHostMap.get("JOBTRACKER").contains("h1"));

    componentHostMap = c1.getServiceComponentHostMap(Collections.singleton("h2"), null);
    Assert.assertEquals(1, componentHostMap.size());
    Assert.assertEquals(1, componentHostMap.get("DATANODE").size());
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h2"));

    componentHostMap = c1.getServiceComponentHostMap(new HashSet<String>(Arrays.asList("h1", "h2", "h3")), null);
    Assert.assertEquals(3, componentHostMap.size());
    Assert.assertEquals(1, componentHostMap.get("NAMENODE").size());
    Assert.assertTrue(componentHostMap.get("NAMENODE").contains("h1"));
    Assert.assertEquals(2, componentHostMap.get("DATANODE").size());
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h1"));
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h2"));
    Assert.assertEquals(1, componentHostMap.get("JOBTRACKER").size());
    Assert.assertTrue(componentHostMap.get("JOBTRACKER").contains("h1"));

    componentHostMap = c1.getServiceComponentHostMap(Collections.singleton("unknown"), null);
    Assert.assertEquals(0, componentHostMap.size());
  }

  @Test
  public void testGetServiceComponentHostMap_ForHostAndService() throws Exception {
    createDefaultCluster();

    Service sfHDFS = serviceFactory.createNew(c1, "HDFS");
    c1.addService(sfHDFS);

    Service sfMR = serviceFactory.createNew(c1, "MAPREDUCE");
    c1.addService(sfMR);

    ServiceComponent scNN = serviceComponentFactory.createNew(sfHDFS, "NAMENODE");
    sfHDFS.addServiceComponent(scNN);
    ServiceComponentHost schNNH1 = serviceComponentHostFactory.createNew(scNN, "h1");
    scNN.addServiceComponentHost(schNNH1);

    ServiceComponent scDN = serviceComponentFactory.createNew(sfHDFS, "DATANODE");
    sfHDFS.addServiceComponent(scDN);
    ServiceComponentHost scDNH1 = serviceComponentHostFactory.createNew(scDN, "h1");
    scDN.addServiceComponentHost(scDNH1);
    ServiceComponentHost scDNH2 = serviceComponentHostFactory.createNew(scDN, "h2");
    scDN.addServiceComponentHost(scDNH2);

    ServiceComponent scJT = serviceComponentFactory.createNew(sfMR, "JOBTRACKER");
    sfMR.addServiceComponent(scJT);
    ServiceComponentHost schJTH1 = serviceComponentHostFactory.createNew(scJT, "h1");
    scJT.addServiceComponentHost(schJTH1);

    Map<String, Set<String>> componentHostMap;

    componentHostMap = c1.getServiceComponentHostMap(Collections.singleton("h1"), Collections.singleton("HDFS"));
    Assert.assertEquals(2, componentHostMap.size());
    Assert.assertEquals(1, componentHostMap.get("DATANODE").size());
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h1"));
    Assert.assertEquals(1, componentHostMap.get("NAMENODE").size());
    Assert.assertTrue(componentHostMap.get("NAMENODE").contains("h1"));

    componentHostMap = c1.getServiceComponentHostMap(Collections.singleton("h2"), Collections.singleton("HDFS"));
    Assert.assertEquals(1, componentHostMap.size());
    Assert.assertEquals(1, componentHostMap.get("DATANODE").size());
    Assert.assertTrue(componentHostMap.get("DATANODE").contains("h2"));

    componentHostMap = c1.getServiceComponentHostMap(Collections.singleton("h3"), Collections.singleton("HDFS"));
    Assert.assertEquals(0, componentHostMap.size());
  }

  @Test
  public void testGetAndSetConfigs() throws Exception {
    createDefaultCluster();

    Map<String, Map<String, String>> c1PropAttributes = new HashMap<String, Map<String,String>>();
    c1PropAttributes.put("final", new HashMap<String, String>());
    c1PropAttributes.get("final").put("a", "true");
    Map<String, Map<String, String>> c2PropAttributes = new HashMap<String, Map<String,String>>();
    c2PropAttributes.put("final", new HashMap<String, String>());
    c2PropAttributes.get("final").put("x", "true");
    Config config1 = configFactory.createNew(c1, "global", "version1",
        new HashMap<String, String>() {{ put("a", "b"); }}, c1PropAttributes);

    Config config2 = configFactory.createNew(c1, "global", "version2",
        new HashMap<String, String>() {{ put("x", "y"); }}, c2PropAttributes);

    Config config3 = configFactory.createNew(c1, "core-site", "version2",
        new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());

    c1.addDesiredConfig("_test", Collections.singleton(config1));
    Config res = c1.getDesiredConfigByType("global");
    Assert.assertNotNull("Expected non-null config", res);
    Assert.assertEquals("true", res.getPropertiesAttributes().get("final").get("a"));

    res = c1.getDesiredConfigByType("core-site");
    Assert.assertNull("Expected null config", res);

    c1.addDesiredConfig("_test", Collections.singleton(config2));
    res = c1.getDesiredConfigByType("global");
    Assert.assertEquals("Expected version tag to be 'version2'", "version2", res.getTag());
    Assert.assertEquals("true", res.getPropertiesAttributes().get("final").get("x"));
  }

  @Test
  public void testDesiredConfigs() throws Exception {
    createDefaultCluster();

    Config config1 = configFactory.createNew(c1, "global", "version1",
        new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());

    Config config2 = configFactory.createNew(c1, "global", "version2",
        new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());

    Config config3 = configFactory.createNew(c1, "core-site", "version2",
        new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());

    try {
      c1.addDesiredConfig(null, Collections.singleton(config1));
      fail("Cannot set a null user with config");
    }
    catch (Exception e) {
      // test failure
    }

    c1.addDesiredConfig("_test1", Collections.singleton(config1));
    c1.addDesiredConfig("_test3", Collections.singleton(config3));

    Map<String, DesiredConfig> desiredConfigs = c1.getDesiredConfigs();
    Assert.assertFalse("Expect desired config not contain 'mapred-site'", desiredConfigs.containsKey("mapred-site"));
    Assert.assertTrue("Expect desired config contain " + config1.getType(), desiredConfigs.containsKey("global"));
    Assert.assertTrue("Expect desired config contain " + config3.getType(), desiredConfigs.containsKey("core-site"));
    Assert.assertEquals("Expect desired config for global should be " + config1.getTag(),
      config1.getTag(), desiredConfigs.get(config1.getType()).getTag());
    Assert.assertEquals("_test1", desiredConfigs.get(config1.getType()).getUser());
    Assert.assertEquals("_test3", desiredConfigs.get(config3.getType()).getUser());
    DesiredConfig dc = desiredConfigs.get(config1.getType());
    Assert.assertTrue("Expect no host-level overrides",
      (null == dc.getHostOverrides() || dc.getHostOverrides().size() == 0));

    c1.addDesiredConfig("_test2", Collections.singleton(config2));
    Assert.assertEquals("_test2", c1.getDesiredConfigs().get(config2.getType()).getUser());

    c1.addDesiredConfig("_test1", Collections.singleton(config1));

    // setup a host that also has a config override
    Host host = clusters.getHost("h1");
    host.addDesiredConfig(c1.getClusterId(), true, "_test2", config2);

    desiredConfigs = c1.getDesiredConfigs();
    dc = desiredConfigs.get(config1.getType());

    Assert.assertNotNull("Expect host-level overrides", dc.getHostOverrides());
    Assert.assertEquals("Expect one host-level override", 1, dc.getHostOverrides().size());
  }

  @Test
  public void testConvertToResponse() throws Exception {
    createDefaultCluster();

    ClusterResponse r = c1.convertToResponse();
    Assert.assertEquals(c1.getClusterId(), r.getClusterId().longValue());
    Assert.assertEquals(c1.getClusterName(), r.getClusterName());
    Assert.assertEquals(Integer.valueOf(2), r.getTotalHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getAlertStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getHealthyStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getUnhealthyStatusHosts());
    Assert.assertEquals(2, r.getClusterHealthReport().getUnknownStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getStaleConfigsHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getMaintenanceStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getHealthyStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getHeartbeatLostStateHosts());
    Assert.assertEquals(2, r.getClusterHealthReport().getInitStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getUnhealthyStateHosts());

    clusters.addHost("h3");
    Host host = clusters.getHost("h3");
    host.setIPv4("ipv4");
    host.setIPv6("ipv6");

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");
    host.setHostAttributes(hostAttributes);
    host.setState(HostState.HEALTHY);
    host.setHealthStatus(new HostHealthStatus(HostHealthStatus.HealthStatus.HEALTHY, ""));
    host.setStatus(host.getHealthStatus().getHealthStatus().name());
    c1.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    clusters.mapHostToCluster("h3", "c1");

    r = c1.convertToResponse();

    Assert.assertEquals(Integer.valueOf(3), r.getTotalHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getAlertStatusHosts());
    Assert.assertEquals(1, r.getClusterHealthReport().getHealthyStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getUnhealthyStatusHosts());
    Assert.assertEquals(2, r.getClusterHealthReport().getUnknownStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getStaleConfigsHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getMaintenanceStateHosts());
    Assert.assertEquals(1, r.getClusterHealthReport().getHealthyStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getHeartbeatLostStateHosts());
    Assert.assertEquals(2, r.getClusterHealthReport().getInitStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getUnhealthyStateHosts());

    // TODO write unit tests for debug dump
    StringBuilder sb = new StringBuilder();
    c1.debugDump(sb);
  }

  @Test
  public void testDeleteService() throws Exception {
    createDefaultCluster();

    c1.addService("MAPREDUCE");

    Service hdfs = c1.addService("HDFS");
    ServiceComponent nameNode = hdfs.addServiceComponent("NAMENODE");

    assertEquals(2, c1.getServices().size());
    assertEquals(2, injector.getProvider(EntityManager.class).get().
        createQuery("SELECT service FROM ClusterServiceEntity service").getResultList().size());

    c1.deleteService("HDFS");

    assertEquals(1, c1.getServices().size());
    assertEquals(1, injector.getProvider(EntityManager.class).get().
        createQuery("SELECT service FROM ClusterServiceEntity service").getResultList().size());
  }

  @Test
  public void testDeleteServiceWithConfigHistory() throws Exception {
    createDefaultCluster();

    c1.addService("HDFS");

    Config config1 = configFactory.createNew(c1, "hdfs-site", "version1",
      new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());

    Config config2 = configFactory.createNew(c1, "core-site", "version2",
      new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());

    Set<Config> configs = new HashSet<Config>();
    configs.add(config1);
    configs.add(config2);

    c1.addDesiredConfig("admin", configs);
    List<ServiceConfigVersionResponse> serviceConfigVersions = c1.getServiceConfigVersions();
    Assert.assertNotNull(serviceConfigVersions);
    // Single serviceConfigVersion for multiple configs
    Assert.assertEquals(1, serviceConfigVersions.size());
    Assert.assertEquals(Long.valueOf(1), serviceConfigVersions.get(0).getVersion());
    Assert.assertEquals(2, c1.getDesiredConfigs().size());
    Assert.assertEquals("version1", c1.getDesiredConfigByType("hdfs-site").getTag());
    Assert.assertEquals("version2", c1.getDesiredConfigByType("core-site").getTag());

    Map<String, Collection<ServiceConfigVersionResponse>> activeServiceConfigVersions =
      c1.getActiveServiceConfigVersions();
    Assert.assertEquals(1, activeServiceConfigVersions.size());

    c1.deleteService("HDFS");

    Assert.assertEquals(0, c1.getServices().size());
    Assert.assertEquals(0, c1.getServiceConfigVersions().size());

    EntityManager em = injector.getProvider(EntityManager.class).get();

    // ServiceConfig
    Assert.assertEquals(0,
      em.createQuery("SELECT serviceConfig from ServiceConfigEntity serviceConfig").getResultList().size());
    // ClusterConfig
    Assert.assertEquals(2,
      em.createQuery("SELECT config from ClusterConfigEntity config").getResultList().size());
    // ClusterConfigMapping
    List<ClusterConfigMappingEntity> configMappingEntities =
        em.createQuery("SELECT configmapping from ClusterConfigMappingEntity configmapping",
        ClusterConfigMappingEntity.class).getResultList();

    Assert.assertEquals(2, configMappingEntities.size());

    for (ClusterConfigMappingEntity configMappingEntity : configMappingEntities) {
      if (StringUtils.equals(configMappingEntity.getType(), "core-site")) {
        assertEquals("core-site is not part of HDFS in test stack, should remain mapped to cluster",
            1, configMappingEntity.isSelected());
      }
      if (StringUtils.equals(configMappingEntity.getType(), "hdfs-site")) {
        assertEquals("hdfs-site should be unmapped from cluster when HDFS service is removed",
            0, configMappingEntity.isSelected());
      }
    }

    // ServiceConfigMapping
    Assert.assertEquals(0,
      em.createNativeQuery("SELECT * from serviceconfigmapping").getResultList().size());
  }

  @Test
  public void testGetHostsDesiredConfigs() throws Exception {
    createDefaultCluster();

    Host host1 = clusters.getHost("h1");
    HostEntity hostEntity1 = hostDAO.findByName("h1");

    Map<String, Map<String, String>> propAttributes = new HashMap<String, Map<String,String>>();
    propAttributes.put("final", new HashMap<String, String>());
    propAttributes.get("final").put("test", "true");
    Config config = configFactory.createNew(c1, "hdfs-site", "1", new HashMap<String, String>(){{
      put("test", "test");
    }}, propAttributes);

    host1.addDesiredConfig(c1.getClusterId(), true, "test", config);

    Map<Long, Map<String, DesiredConfig>> configs = c1.getAllHostsDesiredConfigs();

    assertTrue(configs.containsKey(hostEntity1.getHostId()));
    assertEquals(1, configs.get(hostEntity1.getHostId()).size());

    List<Long> hostIds = new ArrayList<Long>();
    hostIds.add(hostEntity1.getHostId());

    configs = c1.getHostsDesiredConfigs(hostIds);

    assertTrue(configs.containsKey(hostEntity1.getHostId()));
    assertEquals(1, configs.get(hostEntity1.getHostId()).size());
  }

  @Test
  public void testProvisioningState() throws Exception {
    createDefaultCluster();

    c1.setProvisioningState(State.INIT);
    Assert.assertEquals(State.INIT,
        c1.getProvisioningState());

    c1.setProvisioningState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED,
        c1.getProvisioningState());
  }

  @Test
  public void testServiceConfigVersions() throws Exception {
    createDefaultCluster();

    Config config1 = configFactory.createNew(c1, "hdfs-site", "version1",
      new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());

    Config config2 = configFactory.createNew(c1, "hdfs-site", "version2",
      new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());

    c1.addDesiredConfig("admin", Collections.singleton(config1));
    List<ServiceConfigVersionResponse> serviceConfigVersions =
      c1.getServiceConfigVersions();
    Assert.assertNotNull(serviceConfigVersions);
    Assert.assertEquals(1, serviceConfigVersions.size());
    Map<String, Collection<ServiceConfigVersionResponse>> activeServiceConfigVersions =
      c1.getActiveServiceConfigVersions();
    Assert.assertEquals(1, activeServiceConfigVersions.size());
    ServiceConfigVersionResponse hdfsResponse =
      activeServiceConfigVersions.get("HDFS").iterator().next();

    Assert.assertEquals("HDFS", hdfsResponse.getServiceName());
    Assert.assertEquals("c1", hdfsResponse.getClusterName());
    Assert.assertEquals("admin", hdfsResponse.getUserName());
    Assert.assertEquals("Default", hdfsResponse.getGroupName());
    Assert.assertEquals(Long.valueOf(-1), hdfsResponse.getGroupId());
    Assert.assertEquals(Long.valueOf(1), hdfsResponse.getVersion());

    c1.addDesiredConfig("admin", Collections.singleton(config2));
    serviceConfigVersions = c1.getServiceConfigVersions();
    Assert.assertNotNull(serviceConfigVersions);
    // created new ServiceConfigVersion
    Assert.assertEquals(2, serviceConfigVersions.size());

    activeServiceConfigVersions = c1.getActiveServiceConfigVersions();
    Assert.assertEquals(1, activeServiceConfigVersions.size());
    hdfsResponse = activeServiceConfigVersions.get("HDFS").iterator().next();
    Assert.assertEquals("HDFS", hdfsResponse.getServiceName());
    Assert.assertEquals("c1", hdfsResponse.getClusterName());
    Assert.assertEquals("admin", hdfsResponse.getUserName());
    assertEquals(Long.valueOf(2), hdfsResponse.getVersion());

    // Rollback , clonning version1 config, created new ServiceConfigVersion
    c1.setServiceConfigVersion("HDFS", 1L, "admin", "test_note");
    serviceConfigVersions = c1.getServiceConfigVersions();
    Assert.assertNotNull(serviceConfigVersions);
    // created new ServiceConfigVersion
    Assert.assertEquals(3, serviceConfigVersions.size());
    // active version still 1
    activeServiceConfigVersions = c1.getActiveServiceConfigVersions();
    Assert.assertEquals(1, activeServiceConfigVersions.size());
    hdfsResponse = activeServiceConfigVersions.get("HDFS").iterator().next();
    Assert.assertEquals("HDFS", hdfsResponse.getServiceName());
    Assert.assertEquals("c1", hdfsResponse.getClusterName());
    Assert.assertEquals("admin", hdfsResponse.getUserName());
    assertEquals(Long.valueOf(3), hdfsResponse.getVersion());
  }

  @Test
  public void testSingleServiceVersionForMultipleConfigs() throws Exception {
    createDefaultCluster();

    Config config1 = configFactory.createNew(c1, "hdfs-site", "version1",
      new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());

    Config config2 = configFactory.createNew(c1, "core-site", "version2",
      new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());

    Set<Config> configs = new HashSet<Config>();
    configs.add(config1);
    configs.add(config2);

    c1.addDesiredConfig("admin", configs);
    List<ServiceConfigVersionResponse> serviceConfigVersions =
      c1.getServiceConfigVersions();
    Assert.assertNotNull(serviceConfigVersions);
    // Single serviceConfigVersion for multiple configs
    Assert.assertEquals(1, serviceConfigVersions.size());
    Assert.assertEquals(Long.valueOf(1), serviceConfigVersions.get(0).getVersion());
    Assert.assertEquals(2, c1.getDesiredConfigs().size());
    Assert.assertEquals("version1", c1.getDesiredConfigByType("hdfs-site").getTag());
    Assert.assertEquals("version2", c1.getDesiredConfigByType("core-site").getTag());

    Map<String, Collection<ServiceConfigVersionResponse>> activeServiceConfigVersions =
      c1.getActiveServiceConfigVersions();
    Assert.assertEquals(1, activeServiceConfigVersions.size());
  }

  @Test
  public void testServiceConfigVersionsForGroups() throws Exception {
    createDefaultCluster();

    Config config1 = configFactory.createNew(c1, "hdfs-site", "version1",
      new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());

    ServiceConfigVersionResponse scvResponse =
      c1.addDesiredConfig("admin", Collections.singleton(config1));

    assertEquals("SCV 1 should be created", Long.valueOf(1), scvResponse.getVersion());

    Map<String, Collection<ServiceConfigVersionResponse>> activeServiceConfigVersions =
      c1.getActiveServiceConfigVersions();
    Assert.assertEquals("Only one scv should be active", 1, activeServiceConfigVersions.get("HDFS").size());

    //create config group
    Config config2 = configFactory.createNew(c1, "hdfs-site", "version2",
      new HashMap<String, String>() {{ put("a", "c"); }}, new HashMap<String, Map<String,String>>());

    ConfigGroup configGroup =
      configGroupFactory.createNew(c1, "test group", "HDFS", "descr", Collections.singletonMap("hdfs-site", config2),
        Collections.<Long, Host>emptyMap());

    c1.addConfigGroup(configGroup);

    scvResponse = c1.createServiceConfigVersion("HDFS", "admin", "test note", configGroup);
    assertEquals("SCV 2 should be created", Long.valueOf(2), scvResponse.getVersion());

    //two scv active
    activeServiceConfigVersions = c1.getActiveServiceConfigVersions();
    Assert.assertEquals("Two service config versions should be active, for default and test groups",
      2, activeServiceConfigVersions.get("HDFS").size());

    Config config3 = configFactory.createNew(c1, "hdfs-site", "version3",
      new HashMap<String, String>() {{ put("a", "d"); }}, new HashMap<String, Map<String,String>>());

    configGroup.setConfigurations(Collections.singletonMap("hdfs-site", config3));

    scvResponse = c1.createServiceConfigVersion("HDFS", "admin", "test note", configGroup);
    assertEquals("SCV 3 should be created", Long.valueOf(3), scvResponse.getVersion());

    //still two scv active, 3 total
    activeServiceConfigVersions = c1.getActiveServiceConfigVersions();
    Assert.assertEquals("Two service config versions should be active, for default and test groups",
      2, activeServiceConfigVersions.get("HDFS").size());

    assertEquals(3, c1.getServiceConfigVersions().size());

    //rollback group

    scvResponse = c1.setServiceConfigVersion("HDFS", 2L, "admin", "group rollback");
    assertEquals("SCV 4 should be created", Long.valueOf(4), scvResponse.getVersion());

    configGroup = c1.getConfigGroups().get(configGroup.getId()); //refresh?

    //still two scv active, 4 total
    activeServiceConfigVersions = c1.getActiveServiceConfigVersions();
    Assert.assertEquals("Two service config versions should be active, for default and test groups",
      2, activeServiceConfigVersions.get("HDFS").size());
    assertEquals(4, c1.getServiceConfigVersions().size());

    //check properties rolled back
    Map<String, String> configProperties = configGroup.getConfigurations().get("hdfs-site").getProperties();

    assertEquals("Configurations should be rolled back to a:c ", "c", configProperties.get("a"));

    //check config with empty cluster

    Config config4 = configFactory.createReadOnly("hdfs-site", "version4",
        Collections.singletonMap("a", "b"), null);

    ConfigGroup configGroup2 =
        configGroupFactory.createNew(c1, "test group 2", "HDFS", "descr",
            new HashMap<>(Collections.singletonMap("hdfs-site", config4)),
            Collections.<Long, Host>emptyMap());

    c1.addConfigGroup(configGroup2);

    scvResponse = c1.createServiceConfigVersion("HDFS", "admin", "test note", configGroup2);
    assertEquals("SCV 5 should be created", Long.valueOf(5), scvResponse.getVersion());

    activeServiceConfigVersions = c1.getActiveServiceConfigVersions();
    Assert.assertEquals("Three service config versions should be active, for default and test groups",
        3, activeServiceConfigVersions.get("HDFS").size());
    assertEquals("Five total scvs", 5, c1.getServiceConfigVersions().size());
  }

  @Test
  public void testAllServiceConfigVersionsWithConfigGroups() throws Exception {
    // Given
    createDefaultCluster();

    Config hdfsSiteConfigV1 = configFactory.createNew(c1, "hdfs-site", "version1",
        ImmutableMap.of("p1", "v1"), ImmutableMap.<String, Map<String,String>>of());

    ServiceConfigVersionResponse hdfsSiteConfigResponseV1 = c1.addDesiredConfig("admin", Collections.singleton(hdfsSiteConfigV1));
    List<ConfigurationResponse> configResponsesDefaultGroup =  Collections.singletonList(
      new ConfigurationResponse(c1.getClusterName(), hdfsSiteConfigV1.getStackId(),
        hdfsSiteConfigV1.getType(), hdfsSiteConfigV1.getTag(), hdfsSiteConfigV1.getVersion(),
        hdfsSiteConfigV1.getProperties(), hdfsSiteConfigV1.getPropertiesAttributes(), hdfsSiteConfigV1.getPropertiesTypes())
    );

    hdfsSiteConfigResponseV1.setConfigurations(configResponsesDefaultGroup);

    Config hdfsSiteConfigV2 = configFactory.createNew(c1, "hdfs-site", "version2",
        ImmutableMap.of("p1", "v2"), ImmutableMap.<String, Map<String,String>>of());

    ConfigGroup configGroup = configGroupFactory.createNew(c1, "configGroup1", "version1", "test description", ImmutableMap.of(hdfsSiteConfigV2.getType(), hdfsSiteConfigV2), ImmutableMap.<Long, Host>of());

    c1.addConfigGroup(configGroup);
    ServiceConfigVersionResponse hdfsSiteConfigResponseV2 = c1.createServiceConfigVersion("HDFS", "admin", "test note", configGroup);
    hdfsSiteConfigResponseV2.setConfigurations(Collections.singletonList(
      new ConfigurationResponse(c1.getClusterName(), hdfsSiteConfigV2.getStackId(),
        hdfsSiteConfigV2.getType(), hdfsSiteConfigV2.getTag(), hdfsSiteConfigV2.getVersion(),
        hdfsSiteConfigV2.getProperties(), hdfsSiteConfigV2.getPropertiesAttributes(), hdfsSiteConfigV2.getPropertiesTypes())
    ));
    hdfsSiteConfigResponseV2.setIsCurrent(true); // this is the active config in 'configGroup1' config group as it's the solely service config

    // hdfs config v3
    ServiceConfigVersionResponse hdfsSiteConfigResponseV3 = c1.createServiceConfigVersion("HDFS", "admin", "new config in default group", null);
    hdfsSiteConfigResponseV3.setConfigurations(configResponsesDefaultGroup);
    hdfsSiteConfigResponseV3.setIsCurrent(true); // this is the active config in default config group as it's more recent than V1



    // When
    List<ServiceConfigVersionResponse> expectedServiceConfigResponses = ImmutableList.of(hdfsSiteConfigResponseV1, hdfsSiteConfigResponseV2, hdfsSiteConfigResponseV3);
    List<ServiceConfigVersionResponse> allServiceConfigResponses = c1.getServiceConfigVersions();


    Collections.sort(
      allServiceConfigResponses,
      new Comparator<ServiceConfigVersionResponse>() {
        @Override
        public int compare(ServiceConfigVersionResponse o1, ServiceConfigVersionResponse o2) {
          return o1.getVersion().compareTo(o2.getVersion());
        }
      }
    );

    // Then
    assertThat(
      allServiceConfigResponses,
      is(expectedServiceConfigResponses));
  }

  @Test
  public void testAllServiceConfigVersionsWithDeletedConfigGroups() throws Exception {
    // Given
    createDefaultCluster();

    Config hdfsSiteConfigV1 = configFactory.createNew(c1, "hdfs-site", "version1",
        ImmutableMap.of("p1", "v1"), ImmutableMap.<String, Map<String,String>>of());

    ServiceConfigVersionResponse hdfsSiteConfigResponseV1 = c1.addDesiredConfig("admin", Collections.singleton(hdfsSiteConfigV1));
    List<ConfigurationResponse> configResponsesDefaultGroup =  Collections.singletonList(
      new ConfigurationResponse(c1.getClusterName(), hdfsSiteConfigV1.getStackId(),
        hdfsSiteConfigV1.getType(), hdfsSiteConfigV1.getTag(), hdfsSiteConfigV1.getVersion(),
        hdfsSiteConfigV1.getProperties(), hdfsSiteConfigV1.getPropertiesAttributes(), hdfsSiteConfigV1.getPropertiesTypes())
    );

    hdfsSiteConfigResponseV1.setConfigurations(configResponsesDefaultGroup);

    Config hdfsSiteConfigV2 = configFactory.createNew(c1, "hdfs-site", "version2",
        ImmutableMap.of("p1", "v2"), ImmutableMap.<String, Map<String,String>>of());

    ConfigGroup configGroup = configGroupFactory.createNew(c1, "configGroup1", "version1", "test description", ImmutableMap.of(hdfsSiteConfigV2.getType(), hdfsSiteConfigV2), ImmutableMap.<Long, Host>of());

    c1.addConfigGroup(configGroup);
    ServiceConfigVersionResponse hdfsSiteConfigResponseV2 = c1.createServiceConfigVersion("HDFS", "admin", "test note", configGroup);
    hdfsSiteConfigResponseV2.setConfigurations(Collections.singletonList(
      new ConfigurationResponse(c1.getClusterName(), hdfsSiteConfigV2.getStackId(),
        hdfsSiteConfigV2.getType(), hdfsSiteConfigV2.getTag(), hdfsSiteConfigV2.getVersion(),
        hdfsSiteConfigV2.getProperties(), hdfsSiteConfigV2.getPropertiesAttributes(), hdfsSiteConfigV2.getPropertiesTypes())
    ));

    // delete the config group
    c1.deleteConfigGroup(configGroup.getId());


    // hdfs config v3
    ServiceConfigVersionResponse hdfsSiteConfigResponseV3 = c1.createServiceConfigVersion("HDFS", "admin", "new config in default group", null);
    hdfsSiteConfigResponseV3.setConfigurations(configResponsesDefaultGroup);
    hdfsSiteConfigResponseV3.setIsCurrent(true); // this is the active config in default config group as it's more recent than V1



    // When

    List<ServiceConfigVersionResponse> allServiceConfigResponses = c1.getServiceConfigVersions();

    Collections.sort(
      allServiceConfigResponses,
      new Comparator<ServiceConfigVersionResponse>() {
        @Override
        public int compare(ServiceConfigVersionResponse o1, ServiceConfigVersionResponse o2) {
          return o1.getVersion().compareTo(o2.getVersion());
        }
      }
    );


    // Then

    assertEquals(3, allServiceConfigResponses.size());

    // all configs that was created as member of config group 'configGroup1' should be marked as 'not current'
    // as the parent config group has been deleted

    // default group
    assertEquals(false, allServiceConfigResponses.get(0).getIsCurrent());
    assertEquals(ServiceConfigVersionResponse.DEFAULT_CONFIG_GROUP_NAME, allServiceConfigResponses.get(0).getGroupName());

    assertEquals(true, allServiceConfigResponses.get(2).getIsCurrent());
    assertEquals(ServiceConfigVersionResponse.DEFAULT_CONFIG_GROUP_NAME, allServiceConfigResponses.get(2).getGroupName());

    // deleted group
    assertEquals(false, allServiceConfigResponses.get(1).getIsCurrent());
    assertEquals(ServiceConfigVersionResponse.DELETED_CONFIG_GROUP_NAME, allServiceConfigResponses.get(1).getGroupName());



  }

  @Test
  public void testTransitionClusterVersion() throws Exception {
    createDefaultCluster();

    String stack = "HDP";
    String version = "0.2";

    StackId stackId = new StackId(stack, version);

    helper.getOrCreateRepositoryVersion(stackId, version);
    c1.createClusterVersion(stackId, version, "admin",
        RepositoryVersionState.INSTALLING);

    assertStateException(stackId, version, RepositoryVersionState.CURRENT,
        RepositoryVersionState.INSTALLING);

    c1.transitionClusterVersion(stackId, version,
        RepositoryVersionState.INSTALL_FAILED);
    checkStackVersionState(stackId, version,
        RepositoryVersionState.INSTALL_FAILED);

    assertStateException(stackId, version, RepositoryVersionState.CURRENT,
        RepositoryVersionState.INSTALL_FAILED);
    assertStateException(stackId, version, RepositoryVersionState.INSTALLED,
        RepositoryVersionState.INSTALL_FAILED);
    assertStateException(stackId, version, RepositoryVersionState.OUT_OF_SYNC,
        RepositoryVersionState.INSTALL_FAILED);

    c1.transitionClusterVersion(stackId, version,
        RepositoryVersionState.INSTALLING);
    checkStackVersionState(stackId, version, RepositoryVersionState.INSTALLING);

    c1.transitionClusterVersion(stackId, version,
        RepositoryVersionState.INSTALLED);
    checkStackVersionState(stackId, version, RepositoryVersionState.INSTALLED);

    assertStateException(stackId, version,
        RepositoryVersionState.INSTALL_FAILED, RepositoryVersionState.INSTALLED);

    c1.transitionClusterVersion(stackId, version,
        RepositoryVersionState.OUT_OF_SYNC);
    checkStackVersionState(stackId, version, RepositoryVersionState.OUT_OF_SYNC);

    assertStateException(stackId, version, RepositoryVersionState.CURRENT,
        RepositoryVersionState.OUT_OF_SYNC);
    assertStateException(stackId, version, RepositoryVersionState.INSTALLED,
        RepositoryVersionState.OUT_OF_SYNC);
    assertStateException(stackId, version,
        RepositoryVersionState.INSTALL_FAILED,
        RepositoryVersionState.OUT_OF_SYNC);

    c1.transitionClusterVersion(stackId, version,
        RepositoryVersionState.INSTALLING);
    checkStackVersionState(stackId, version, RepositoryVersionState.INSTALLING);

    c1.transitionClusterVersion(stackId, version,
        RepositoryVersionState.INSTALLED);
    checkStackVersionState(stackId, version, RepositoryVersionState.INSTALLED);

    c1.setDesiredStackVersion(stackId);
    c1.transitionClusterVersion(stackId, version,
        RepositoryVersionState.CURRENT);

    checkStackVersionState(stackId, version, RepositoryVersionState.CURRENT);

    checkStackVersionState(new StackId("HDP", "0.1"), "0.1",
        RepositoryVersionState.INSTALLED);

    // The only CURRENT state should not be changed
    assertStateException(stackId, version, RepositoryVersionState.INSTALLED,
        RepositoryVersionState.CURRENT);
  }

  @Test
  public void testTransitionClusterVersionTransactionFail() throws Exception {
    createDefaultCluster();

    StackId stackId = new StackId("HDP", "0.2");
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());
    c1.createClusterVersion(stackId, "0.2", "admin",
        RepositoryVersionState.INSTALLING);
    c1.transitionClusterVersion(stackId, "0.2",
        RepositoryVersionState.INSTALLED);
    try {
      ClusterVersionDAOMock.failOnCurrentVersionState = true;
      c1.transitionClusterVersion(stackId, "0.2",
          RepositoryVersionState.CURRENT);
      Assert.fail();
    } catch (AmbariException e) {

    } finally {
      ClusterVersionDAOMock.failOnCurrentVersionState = false;
    }

    // There must be CURRENT state for cluster
    assertNotNull(c1.getCurrentClusterVersion());
  }

  /**
   * Tests that hosts can be correctly transitioned into the "INSTALLING" state.
   * This method also tests that hosts in MM will not be transitioned, as per
   * the contract of
   * {@link Cluster#transitionHostsToInstalling(ClusterVersionEntity)}.
   *
   * @throws Exception
   */
  @Test
  public void testTransitionHostVersions() throws Exception {
    createDefaultCluster();

    StackId stackId = new StackId("HDP", "0.2");
    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    c1.createClusterVersion(stackId, "0.2", "admin",
        RepositoryVersionState.INSTALLING);

    ClusterVersionEntity entityHDP2 = null;
    for (ClusterVersionEntity entity : c1.getAllClusterVersions()) {
      StackEntity repoVersionStackEntity = entity.getRepositoryVersion().getStack();
      StackId repoVersionStackId = new StackId(repoVersionStackEntity);

      if (repoVersionStackId.getStackName().equals("HDP")
          && repoVersionStackId.getStackVersion().equals("0.2")) {
        entityHDP2 = entity;
        break;
      }
    }

    assertNotNull(entityHDP2);

    List<HostVersionEntity> hostVersionsH1Before = hostVersionDAO.findByClusterAndHost("c1", "h1");
    assertEquals(1, hostVersionsH1Before.size());

    c1.transitionHosts(entityHDP2, RepositoryVersionState.INSTALLING);

    List<HostVersionEntity> hostVersionsH1After = hostVersionDAO.findByClusterAndHost("c1", "h1");
    assertEquals(2, hostVersionsH1After.size());

    boolean checked = false;
    for (HostVersionEntity entity : hostVersionsH1After) {
      StackEntity repoVersionStackEntity = entity.getRepositoryVersion().getStack();
      if (repoVersionStackEntity.getStackName().equals("HDP")
          && repoVersionStackEntity.getStackVersion().equals("0.2")) {
        assertEquals(RepositoryVersionState.INSTALLING, entity.getState());
        checked = true;
        break;
      }
    }

    assertTrue(checked);

    // Test for update of existing host stack version
    c1.transitionHosts(entityHDP2, RepositoryVersionState.INSTALLING);

    hostVersionsH1After = hostVersionDAO.findByClusterAndHost("c1", "h1");
    assertEquals(2, hostVersionsH1After.size());

    checked = false;
    for (HostVersionEntity entity : hostVersionsH1After) {
      StackEntity repoVersionStackEntity = entity.getRepositoryVersion().getStack();
      if (repoVersionStackEntity.getStackName().equals("HDP")
          && repoVersionStackEntity.getStackVersion().equals("0.2")) {
        assertEquals(RepositoryVersionState.INSTALLING, entity.getState());
        checked = true;
        break;
      }
    }

    assertTrue(checked);

    // reset all to INSTALL_FAILED
    List<HostVersionEntity> hostVersionEntities = hostVersionDAO.findAll();
    for (HostVersionEntity hostVersionEntity : hostVersionEntities) {
      hostVersionEntity.setState(RepositoryVersionState.INSTALL_FAILED);
      hostVersionDAO.merge(hostVersionEntity);
    }

    // verify they have been transition to INSTALL_FAILED
    hostVersionEntities = hostVersionDAO.findAll();
    for (HostVersionEntity hostVersionEntity : hostVersionEntities) {
      assertEquals(RepositoryVersionState.INSTALL_FAILED, hostVersionEntity.getState());
    }

    // put 1 host in maintenance mode
    Collection<Host> hosts = c1.getHosts();
    Iterator<Host> iterator = hosts.iterator();
    Host hostInMaintenanceMode = iterator.next();
    Host hostNotInMaintenanceMode = iterator.next();
    hostInMaintenanceMode.setMaintenanceState(c1.getClusterId(), MaintenanceState.ON);

    // transition host versions to INSTALLING
    c1.transitionHosts(entityHDP2, RepositoryVersionState.INSTALLING);

    List<HostVersionEntity> hostInMaintModeVersions = hostVersionDAO.findByClusterAndHost("c1",
        hostInMaintenanceMode.getHostName());

    List<HostVersionEntity> otherHostVersions = hostVersionDAO.findByClusterAndHost("c1",
        hostNotInMaintenanceMode.getHostName());

    // verify the MM host has moved to OUT_OF_SYNC
    for (HostVersionEntity hostVersionEntity : hostInMaintModeVersions) {
      StackEntity repoVersionStackEntity = hostVersionEntity.getRepositoryVersion().getStack();
      if (repoVersionStackEntity.getStackName().equals("HDP")
          && repoVersionStackEntity.getStackVersion().equals("0.2")) {
        assertEquals(RepositoryVersionState.OUT_OF_SYNC, hostVersionEntity.getState());
      }
    }

    // verify the other host is in INSTALLING
    for (HostVersionEntity hostVersionEntity : otherHostVersions) {
      StackEntity repoVersionStackEntity = hostVersionEntity.getRepositoryVersion().getStack();
      if (repoVersionStackEntity.getStackName().equals("HDP")
          && repoVersionStackEntity.getStackVersion().equals("0.2")) {
      assertEquals(RepositoryVersionState.INSTALLING, hostVersionEntity.getState());
      }
    }
  }

  @Test
  public void testRecalculateClusterVersionState() throws Exception {
    createDefaultCluster();

    Host h1 = clusters.getHost("h1");
    h1.setState(HostState.HEALTHY);

    Host h2 = clusters.getHost("h2");
    h2.setState(HostState.HEALTHY);

    // Phase 1: Install bits during distribution
    StackId stackId = new StackId("HDP-0.1");
    final String stackVersion = "0.1-1000";
    RepositoryVersionEntity repositoryVersionEntity = helper.getOrCreateRepositoryVersion(
        stackId,
        stackVersion);
    // Because the cluster already has a Cluster Version, an additional stack must init with INSTALLING
    c1.createClusterVersion(stackId, stackVersion, "admin",
        RepositoryVersionState.INSTALLING);
    c1.setCurrentStackVersion(stackId);

    HostVersionEntity hv1 = helper.createHostVersion("h1", repositoryVersionEntity, RepositoryVersionState.INSTALLING);
    HostVersionEntity hv2 = helper.createHostVersion("h2", repositoryVersionEntity, RepositoryVersionState.INSTALLING);

    c1.recalculateClusterVersionState(repositoryVersionEntity);
    //Should remain in its current state
    checkStackVersionState(stackId, stackVersion,
        RepositoryVersionState.INSTALLING);

    h2.setState(HostState.UNHEALTHY);
    c1.recalculateClusterVersionState(repositoryVersionEntity);
    // In order for the states to be accurately reflected, the host health status should not impact the status
    // of the host_version.
    checkStackVersionState(stackId, stackVersion,
        RepositoryVersionState.INSTALLING);
    // Retry by going back to INSTALLING
    c1.transitionClusterVersion(stackId, stackVersion,
        RepositoryVersionState.INSTALLING);

    // Installation on one host fails (other is continuing)
    hv1.setState(RepositoryVersionState.INSTALL_FAILED);
    hostVersionDAO.merge(hv1);
    // Check that cluster version is still in a non-final state
    c1.recalculateClusterVersionState(repositoryVersionEntity);
    checkStackVersionState(stackId, stackVersion,
      RepositoryVersionState.INSTALLING);

    h2.setState(HostState.HEALTHY);
    hv2.setState(RepositoryVersionState.INSTALLED);
    hostVersionDAO.merge(hv2);
    // Now both cluster versions are in a final state, so
    // cluster version state changes to final state
    c1.recalculateClusterVersionState(repositoryVersionEntity);
    checkStackVersionState(stackId, stackVersion,
        RepositoryVersionState.INSTALL_FAILED);

    // Retry by going back to INSTALLING
    c1.transitionClusterVersion(stackId, stackVersion,
      RepositoryVersionState.INSTALLING);

    h2.setState(HostState.HEALTHY);
    hv2.setState(RepositoryVersionState.INSTALLED);
    hostVersionDAO.merge(hv2);
    c1.recalculateClusterVersionState(repositoryVersionEntity);
    checkStackVersionState(stackId, stackVersion,
      RepositoryVersionState.INSTALLING);

    // Make the last host fail
    hv1.setState(RepositoryVersionState.INSTALL_FAILED);
    hostVersionDAO.merge(hv1);
    c1.recalculateClusterVersionState(repositoryVersionEntity);
    checkStackVersionState(stackId, stackVersion,
        RepositoryVersionState.INSTALL_FAILED);
    // Retry by going back to INSTALLING
    c1.transitionClusterVersion(stackId, stackVersion,
        RepositoryVersionState.INSTALLING);

    // Now, all hosts are in INSTALLED
    hv1.setState(RepositoryVersionState.INSTALLED);
    hostVersionDAO.merge(hv1);
    c1.recalculateClusterVersionState(repositoryVersionEntity);
    checkStackVersionState(stackId, stackVersion,
        RepositoryVersionState.INSTALLED);

    // Set both hosts to CURRENT
    hv1.setState(RepositoryVersionState.CURRENT);
    hostVersionDAO.merge(hv1);
    hv2.setState(RepositoryVersionState.CURRENT);
    hostVersionDAO.merge(hv2);
    c1.recalculateClusterVersionState(repositoryVersionEntity);
    checkStackVersionState(stackId, stackVersion,
        RepositoryVersionState.CURRENT);
  }

  @Test
  public void testRecalculateClusterVersionStateWithNotRequired() throws Exception {
    createDefaultCluster();

    Host h1 = clusters.getHost("h1");
    h1.setState(HostState.HEALTHY);

    Host h2 = clusters.getHost("h2");
    h2.setState(HostState.HEALTHY);

    // Phase 1: Install bits during distribution
    StackId stackId = new StackId("HDP-0.1");
    final String stackVersion = "0.1-1000";
    RepositoryVersionEntity repositoryVersionEntity = helper.getOrCreateRepositoryVersion(
        stackId,
        stackVersion);
    // Because the cluster already has a Cluster Version, an additional stack must init with INSTALLING
    c1.createClusterVersion(stackId, stackVersion, "admin",
        RepositoryVersionState.INSTALLING);
    c1.setCurrentStackVersion(stackId);

    HostVersionEntity hv1 = helper.createHostVersion("h1", repositoryVersionEntity, RepositoryVersionState.INSTALLED);
    HostVersionEntity hv2 = helper.createHostVersion("h2", repositoryVersionEntity, RepositoryVersionState.NOT_REQUIRED);

    c1.recalculateClusterVersionState(repositoryVersionEntity);
    //Should remain in its current state
    checkStackVersionState(stackId, stackVersion,
        RepositoryVersionState.INSTALLED);
  }


  @Test
  public void testRecalculateAllClusterVersionStates() throws Exception {
    createDefaultCluster();

    Host h1 = clusters.getHost("h1");
    h1.setState(HostState.HEALTHY);

    Host h2 = clusters.getHost("h2");
    h2.setState(HostState.HEALTHY);

    StackId stackId = new StackId("HDP-0.1");
    RepositoryVersionEntity repositoryVersionEntity = helper.getOrCreateRepositoryVersion(
        stackId,
        "0.1-1000");
    c1.createClusterVersion(stackId, "0.1-1000", "admin",
        RepositoryVersionState.INSTALLING);
    c1.setCurrentStackVersion(stackId);
    c1.recalculateAllClusterVersionStates();
    checkStackVersionState(stackId, "0.1-1000",
        RepositoryVersionState.INSTALLING);
    checkStackVersionState(stackId, "0.1-2086", RepositoryVersionState.CURRENT);

    HostVersionEntity hv1 = helper.createHostVersion("h1", repositoryVersionEntity, RepositoryVersionState.INSTALLING);
    HostVersionEntity hv2 = helper.createHostVersion("h2", repositoryVersionEntity, RepositoryVersionState.INSTALLING);

    c1.recalculateAllClusterVersionStates();
    checkStackVersionState(stackId, "0.1-1000",
        RepositoryVersionState.INSTALLING);
    checkStackVersionState(stackId, "1.0-2086", RepositoryVersionState.CURRENT);

    hv1.setState(RepositoryVersionState.INSTALL_FAILED);
    hostVersionDAO.merge(hv1);
    c1.recalculateAllClusterVersionStates();
    checkStackVersionState(stackId, "0.1-1000",
        RepositoryVersionState.INSTALL_FAILED);
    checkStackVersionState(stackId, "0.1-2086", RepositoryVersionState.CURRENT);
    // Retry by going back to INSTALLING
    c1.transitionClusterVersion(stackId, "0.1-1000",
        RepositoryVersionState.INSTALLING);

    hv1.setState(RepositoryVersionState.CURRENT);
    hostVersionDAO.merge(hv1);
    c1.recalculateAllClusterVersionStates();
    checkStackVersionState(stackId, "0.1-1000",
        RepositoryVersionState.OUT_OF_SYNC);
    checkStackVersionState(stackId, "0.1-2086", RepositoryVersionState.CURRENT);
  }

  /**
   * Comprehensive test for transitionHostVersion and recalculateClusterVersion.
   * It creates a cluster with 3 hosts and 3 services, one of which does not advertise a version.
   * It then verifies that all 3 hosts have a version of CURRENT, and so does the cluster.
   * It then adds one more host with a component, so its HostVersion will initialize in CURRENT.
   * Next, it distributes a repo so that it is INSTALLED on the 4 hosts.
   * It then adds one more host, whose HostVersion will be OUT_OF_SYNC for the new repo.
   * After redistributing bits again, it simulates an RU.
   * Finally, some of the hosts will end up with a HostVersion in UPGRADED, and others still in INSTALLED.
   * @throws Exception
   */
  @Test
  public void testTransitionHostVersionAdvanced() throws Exception {
    String clusterName = "c1";
    String v1 = "2.2.0-123";
    StackId stackId = new StackId("HDP-2.2.0");

    RepositoryVersionEntity rv1 = helper.getOrCreateRepositoryVersion(stackId, v1);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.4");

    Cluster cluster = createClusterForRU(clusterName, stackId, hostAttributes);

    // Begin install by starting to advertise versions
    // Set the version for the HostComponentState objects
    int versionedComponentCount = 0;
    List<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findAll();
    for(int i = 0; i < hostComponentStates.size(); i++) {
      HostComponentStateEntity hce = hostComponentStates.get(i);
      ComponentInfo compInfo = metaInfo.getComponent(
          stackId.getStackName(), stackId.getStackVersion(),
          hce.getServiceName(),
          hce.getComponentName());

      if (compInfo.isVersionAdvertised()) {
        hce.setVersion(v1);
        hostComponentStateDAO.merge(hce);
        versionedComponentCount++;
      }

      // Simulate the StackVersionListener during the installation of the first Stack Version
      Service svc = cluster.getService(hce.getServiceName());
      ServiceComponent svcComp = svc.getServiceComponent(hce.getComponentName());
      ServiceComponentHost scHost = svcComp.getServiceComponentHost(hce.getHostName());

      scHost.recalculateHostVersionState();
      cluster.recalculateClusterVersionState(rv1);

      Collection<ClusterVersionEntity> clusterVersions = cluster.getAllClusterVersions();

      if (versionedComponentCount > 0) {
        // On the first component with a version, a RepoVersion should have been created
        RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByStackAndVersion(stackId, v1);
        Assert.assertNotNull(repositoryVersion);
        Assert.assertTrue(clusterVersions != null && clusterVersions.size() == 1);

        // Last component to report a version should cause the ClusterVersion to go to CURRENT
        if (i == hostComponentStates.size() - 1) {
          Assert.assertEquals(clusterVersions.iterator().next().getState(), RepositoryVersionState.CURRENT);
        }
      }
    }

    // Add another Host with components ZK Server, ZK Client, and Ganglia Monitor.
    // This host should get a HostVersion in CURRENT, and the ClusterVersion should stay in CURRENT
    addHost("h-4", hostAttributes);
    clusters.mapHostToCluster("h-4", clusterName);

    Service svc2 = cluster.getService("ZOOKEEPER");
    Service svc3 = cluster.getService("GANGLIA");

    ServiceComponent sc2CompA = svc2.getServiceComponent("ZOOKEEPER_SERVER");
    ServiceComponent sc2CompB = svc2.getServiceComponent("ZOOKEEPER_CLIENT");
    ServiceComponent sc3CompB = svc3.getServiceComponent("GANGLIA_MONITOR");

    ServiceComponentHost schHost4Serv2CompA = serviceComponentHostFactory.createNew(sc2CompA, "h-4");
    ServiceComponentHost schHost4Serv2CompB = serviceComponentHostFactory.createNew(sc2CompB, "h-4");
    ServiceComponentHost schHost4Serv3CompB = serviceComponentHostFactory.createNew(sc3CompB, "h-4");
    sc2CompA.addServiceComponentHost(schHost4Serv2CompA);
    sc2CompB.addServiceComponentHost(schHost4Serv2CompB);
    sc3CompB.addServiceComponentHost(schHost4Serv3CompB);

    simulateStackVersionListener(stackId, v1, cluster, hostComponentStateDAO.findByHost("h-4"));

    Collection<HostVersionEntity> hostVersions = hostVersionDAO.findAll();
    Assert.assertEquals(hostVersions.size(), clusters.getHosts().size());
    HostVersionEntity h4Version1 = hostVersionDAO.findByClusterStackVersionAndHost(clusterName, stackId, v1, "h-4");
    Assert.assertNotNull(h4Version1);
    Assert.assertEquals(h4Version1.getState(), RepositoryVersionState.CURRENT);

    // Distribute bits for a new repo
    String v2 = "2.2.0-456";
    RepositoryVersionEntity rv2 = helper.getOrCreateRepositoryVersion(stackId, v2);
    for(String hostName : clusters.getHostsForCluster(clusterName).keySet()) {
      HostEntity host = hostDAO.findByName(hostName);
      HostVersionEntity hve = new HostVersionEntity(host, rv2, RepositoryVersionState.INSTALLED);
      hostVersionDAO.create(hve);
    }
    cluster.createClusterVersion(stackId, v2, "admin",
        RepositoryVersionState.INSTALLING);
    cluster.transitionClusterVersion(stackId, v2,
        RepositoryVersionState.INSTALLED);

    ClusterVersionEntity cv2 = clusterVersionDAO.findByClusterAndStackAndVersion(clusterName, stackId, v2);
    Assert.assertNotNull(cv2);
    Assert.assertEquals(cv2.getState(), RepositoryVersionState.INSTALLED);

    // Add one more Host, with only Ganglia on it. It should have a HostVersion in OUT_OF_SYNC for v2
    addHost("h-5", hostAttributes);
    clusters.mapAndPublishHostsToCluster(Collections.singleton("h-5"), clusterName);
    ServiceComponentHost schHost5Serv3CompB = serviceComponentHostFactory.createNew(sc3CompB, "h-5");
    sc3CompB.addServiceComponentHost(schHost5Serv3CompB);

    // Host 5 will be in OUT_OF_SYNC, so redistribute bits to it so that it reaches a state of INSTALLED
    HostVersionEntity h5Version2 = hostVersionDAO.findByClusterStackVersionAndHost(clusterName, stackId, v2, "h-5");
    Assert.assertNotNull(h5Version2);
    Assert.assertEquals(h5Version2.getState(), RepositoryVersionState.OUT_OF_SYNC);

    h5Version2.setState(RepositoryVersionState.INSTALLED);
    hostVersionDAO.merge(h5Version2);

    // Perform an RU.
    // Verify that on first component with the new version, the ClusterVersion transitions to UPGRADING.
    // For hosts with only components that advertise a version, they HostVersion should be in UPGRADING.
    // For the remaining hosts, the HostVersion should stay in INSTALLED.
    versionedComponentCount = 0;
    hostComponentStates = hostComponentStateDAO.findAll();
    for(int i = 0; i < hostComponentStates.size(); i++) {
      HostComponentStateEntity hce = hostComponentStates.get(i);
      ComponentInfo compInfo = metaInfo.getComponent(
          stackId.getStackName(), stackId.getStackVersion(),
          hce.getServiceName(),
          hce.getComponentName());

      if (compInfo.isVersionAdvertised()) {
        hce.setVersion(v2);
        hostComponentStateDAO.merge(hce);
        versionedComponentCount++;
      }

      // Simulate the StackVersionListener during the installation of the first Stack Version
      Service svc = cluster.getService(hce.getServiceName());
      ServiceComponent svcComp = svc.getServiceComponent(hce.getComponentName());
      ServiceComponentHost scHost = svcComp.getServiceComponentHost(hce.getHostName());

      scHost.recalculateHostVersionState();
      cluster.recalculateClusterVersionState(rv2);

      Collection<ClusterVersionEntity> clusterVersions = cluster.getAllClusterVersions();

      if (versionedComponentCount > 0) {
        // On the first component with a version, a RepoVersion should have been created
        RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByStackAndVersion(stackId, v2);
        Assert.assertNotNull(repositoryVersion);
        Assert.assertTrue(clusterVersions != null && clusterVersions.size() == 2);
      }
    }

    Collection<HostVersionEntity> v2HostVersions = hostVersionDAO.findByClusterStackAndVersion(clusterName, stackId, v2);
    Assert.assertEquals(v2HostVersions.size(), clusters.getHostsForCluster(clusterName).size());
    for (HostVersionEntity hve : v2HostVersions) {
      Assert.assertTrue(TERMINAL_VERSION_STATES.contains(hve.getState()));
    }
  }

  @Test
  public void testBootstrapHostVersion() throws Exception {
    String clusterName = "c1";
    String v1 = "2.2.0-123";
    StackId stackId = new StackId("HDP-2.2.0");

    RepositoryVersionEntity rv1 = helper.getOrCreateRepositoryVersion(stackId, v1);

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "6.4");

    Cluster cluster = createClusterForRU(clusterName, stackId, hostAttributes);

    // Make one host unhealthy
    Host deadHost = cluster.getHosts().iterator().next();
    deadHost.setState(HostState.UNHEALTHY);

    // Begin bootstrap by starting to advertise versions
    // Set the version for the HostComponentState objects
    int versionedComponentCount = 0;
    List<HostComponentStateEntity> hostComponentStates = hostComponentStateDAO.findAll();
    for(int i = 0; i < hostComponentStates.size(); i++) {
      HostComponentStateEntity hce = hostComponentStates.get(i);
      ComponentInfo compInfo = metaInfo.getComponent(
              stackId.getStackName(), stackId.getStackVersion(),
              hce.getServiceName(),
              hce.getComponentName());

      if (hce.getHostName().equals(deadHost.getHostName())) {
        continue; // Skip setting version
      }

      if (compInfo.isVersionAdvertised()) {
        hce.setVersion(v1);
        hostComponentStateDAO.merge(hce);
        versionedComponentCount++;
      }

      // Simulate the StackVersionListener during the installation of the first Stack Version
      Service svc = cluster.getService(hce.getServiceName());
      ServiceComponent svcComp = svc.getServiceComponent(hce.getComponentName());
      ServiceComponentHost scHost = svcComp.getServiceComponentHost(hce.getHostName());

      scHost.recalculateHostVersionState();
      cluster.recalculateClusterVersionState(rv1);

      Collection<ClusterVersionEntity> clusterVersions = cluster.getAllClusterVersions();

      if (versionedComponentCount > 0) {
        // On the first component with a version, a RepoVersion should have been created
        RepositoryVersionEntity repositoryVersion = repositoryVersionDAO.findByStackAndVersion(stackId, v1);
        Assert.assertNotNull(repositoryVersion);
        Assert.assertTrue(clusterVersions != null && clusterVersions.size() == 1);

        // Since host 2 is dead, and host 3 contains only components that dont report a version,
        // cluster version transitions to CURRENT after first component on host 1 reports it's version
        if (versionedComponentCount == 1 && i < (hostComponentStates.size() - 1)) {
          Assert.assertEquals(clusterVersions.iterator().next().getState(), RepositoryVersionState.CURRENT);
        }
      }
    }
  }

  @Test
  public void testTransitionNonReportableHost() throws Exception {
    StackId stackId = new StackId("HDP-2.0.5");

    String clusterName = "c1";
    clusters.addCluster(clusterName, stackId);
    Cluster c1 = clusters.getCluster(clusterName);
    Assert.assertEquals(clusterName, c1.getClusterName());
    Assert.assertEquals(1, c1.getClusterId());

    clusters.addHost("h-1");
    clusters.addHost("h-2");
    clusters.addHost("h-3");

    for (String hostName : new String[] { "h-1", "h-2", "h-3" }) {
      Host h = clusters.getHost(hostName);
      h.setIPv4("ipv4");
      h.setIPv6("ipv6");

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "5.9");
      h.setHostAttributes(hostAttributes);
    }

    String v1 = "2.0.5-1";
    String v2 = "2.0.5-2";
    c1.setDesiredStackVersion(stackId);
    RepositoryVersionEntity rve1 = helper.getOrCreateRepositoryVersion(stackId,
        v1);
    RepositoryVersionEntity rve2 = helper.getOrCreateRepositoryVersion(stackId,
        v2);

    c1.setCurrentStackVersion(stackId);
    c1.createClusterVersion(stackId, v1, "admin",
        RepositoryVersionState.INSTALLING);
    c1.transitionClusterVersion(stackId, v1, RepositoryVersionState.CURRENT);

    clusters.mapHostToCluster("h-1", clusterName);
    clusters.mapHostToCluster("h-2", clusterName);
    clusters.mapHostToCluster("h-3", clusterName);
    ClusterVersionDAOMock.failOnCurrentVersionState = false;

    Service service = c1.addService("ZOOKEEPER");
    ServiceComponent sc = service.addServiceComponent("ZOOKEEPER_SERVER");
    sc.addServiceComponentHost("h-1");
    sc.addServiceComponentHost("h-2");

    service = c1.addService("SQOOP");
    sc = service.addServiceComponent("SQOOP");
    sc.addServiceComponentHost("h-3");

    List<HostVersionEntity> entities = hostVersionDAO.findByClusterAndHost(clusterName, "h-3");
    assertTrue("Expected no host versions", null == entities || 0 == entities.size());

    c1.createClusterVersion(stackId, v2, "admin",
        RepositoryVersionState.INSTALLING);
    c1.transitionClusterVersion(stackId, v2, RepositoryVersionState.INSTALLED);
    c1.transitionClusterVersion(stackId, v2, RepositoryVersionState.CURRENT);

    entities = hostVersionDAO.findByClusterAndHost(clusterName, "h-3");

    assertEquals(1, entities.size());
  }

  @Test
  public void testTransitionHostVersionState_OutOfSync_BlankCurrent() throws Exception {
    /**
     * Checks case when there are 2 cluster stack versions present (CURRENT and OUT_OF_SYNC),
     * and we add a new host to cluster. On a new host, both CURRENT and OUT_OF_SYNC host
     * versions should be present
     */
    StackId stackId = new StackId("HDP-2.0.5");
    String clusterName = "c1";
    clusters.addCluster(clusterName, stackId);
    final Cluster c1 = clusters.getCluster(clusterName);
    Assert.assertEquals(clusterName, c1.getClusterName());
    Assert.assertEquals(1, c1.getClusterId());

    clusters.addHost("h-1");
    clusters.addHost("h-2");
    String h3 = "h-3";
    clusters.addHost(h3);

    for (String hostName : new String[] { "h-1", "h-2", h3}) {
      Host h = clusters.getHost(hostName);
      h.setIPv4("ipv4");
      h.setIPv6("ipv6");

      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "5.9");
      h.setHostAttributes(hostAttributes);
    }

    String v1 = "2.0.5-1";
    String v2 = "2.0.5-2";
    c1.setDesiredStackVersion(stackId);
    RepositoryVersionEntity rve1 = helper.getOrCreateRepositoryVersion(stackId,
        v1);
    RepositoryVersionEntity rve2 = helper.getOrCreateRepositoryVersion(stackId,
        v2);

    c1.setCurrentStackVersion(stackId);
    c1.createClusterVersion(stackId, v1, "admin",
        RepositoryVersionState.INSTALLING);
    c1.transitionClusterVersion(stackId, v1, RepositoryVersionState.CURRENT);

    clusters.mapHostToCluster("h-1", clusterName);
    clusters.mapHostToCluster("h-2", clusterName);

    ClusterVersionDAOMock.failOnCurrentVersionState = false;

    Service service = c1.addService("ZOOKEEPER");
    ServiceComponent sc = service.addServiceComponent("ZOOKEEPER_SERVER");
    sc.addServiceComponentHost("h-1");
    sc.addServiceComponentHost("h-2");

    c1.createClusterVersion(stackId, v2, "admin",
        RepositoryVersionState.INSTALLING);
    c1.transitionClusterVersion(stackId, v2, RepositoryVersionState.INSTALLED);
    c1.transitionClusterVersion(stackId, v2, RepositoryVersionState.OUT_OF_SYNC);

    clusters.mapHostToCluster(h3, clusterName);

    // This method is usually called when we receive heartbeat from new host
    HostEntity hostEntity3 = mock(HostEntity.class);
    when(hostEntity3.getHostName()).thenReturn(h3);

    // HACK: to workaround issue with NullPointerException at
    // org.eclipse.persistence.internal.sessions.MergeManager.registerObjectForMergeCloneIntoWorkingCopy(MergeManager.java:1037)
    // during hostVersionDAO.merge()
    HostVersionDAO hostVersionDAOMock = mock(HostVersionDAO.class);
    Field field = ClusterImpl.class.getDeclaredField("hostVersionDAO");
    field.setAccessible(true);
    field.set(c1, hostVersionDAOMock);

    ArgumentCaptor<HostVersionEntity> hostVersionCaptor = ArgumentCaptor.forClass(HostVersionEntity.class);

    ClusterVersionDAOMock.mockedClusterVersions = new ArrayList<ClusterVersionEntity>() {{
      addAll(c1.getAllClusterVersions());
    }};

    c1.transitionHostVersionState(hostEntity3, rve1, stackId);

    // Revert fields of static instance
    ClusterVersionDAOMock.mockedClusterVersions = null;

    verify(hostVersionDAOMock).merge(hostVersionCaptor.capture());
    assertEquals(hostVersionCaptor.getValue().getState(), RepositoryVersionState.CURRENT);
  }

  /**
   * Tests that an existing configuration can be successfully updated without
   * creating a new version.
   *
   * @throws Exception
   */
  @Test
  public void testClusterConfigMergingWithoutNewVersion() throws Exception {
    createDefaultCluster();

    Cluster cluster = clusters.getCluster("c1");
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");
    assertEquals(0, clusterEntity.getClusterConfigEntities().size());

    final Config originalConfig = configFactory.createNew(cluster, "foo-site", "version3",
        new HashMap<String, String>() {
          {
            put("one", "two");
          }
        }, new HashMap<String, Map<String, String>>());

    ConfigGroup configGroup = configGroupFactory.createNew(cluster, "g1", "t1", "",
        new HashMap<String, Config>() {
          {
            put("foo-site", originalConfig);
          }
        }, Collections.<Long, Host> emptyMap());

    cluster.addConfigGroup(configGroup);

    clusterEntity = clusterDAO.findByName("c1");
    assertEquals(1, clusterEntity.getClusterConfigEntities().size());

    Map<String, Config> configsByType = cluster.getConfigsByType("foo-site");
    Config config = configsByType.entrySet().iterator().next().getValue();

    Map<String, String> properties = config.getProperties();
    properties.put("three", "four");
    config.setProperties(properties);
    config.save();

    clusterEntity = clusterDAO.findByName("c1");
    assertEquals(1, clusterEntity.getClusterConfigEntities().size());
    ClusterConfigEntity clusterConfigEntity = clusterEntity.getClusterConfigEntities().iterator().next();
    assertTrue(clusterConfigEntity.getData().contains("one"));
    assertTrue(clusterConfigEntity.getData().contains("two"));
    assertTrue(clusterConfigEntity.getData().contains("three"));
    assertTrue(clusterConfigEntity.getData().contains("four"));

    cluster.refresh();

    clusterEntity = clusterDAO.findByName("c1");
    assertEquals(1, clusterEntity.getClusterConfigEntities().size());
    clusterConfigEntity = clusterEntity.getClusterConfigEntities().iterator().next();
    assertTrue(clusterConfigEntity.getData().contains("one"));
    assertTrue(clusterConfigEntity.getData().contains("two"));
    assertTrue(clusterConfigEntity.getData().contains("three"));
    assertTrue(clusterConfigEntity.getData().contains("four"));
  }

  /**
   * Tests that {@link Cluster#applyLatestConfigurations(StackId)} sets the
   * right configs to enabled.
   *
   * @throws Exception
   */
  @Test
  public void testApplyLatestConfigurations() throws Exception {
    createDefaultCluster();
    Cluster cluster = clusters.getCluster("c1");
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");
    StackId stackId = cluster.getCurrentStackVersion();
    StackId newStackId = new StackId("HDP-2.0.6");

    StackEntity currentStack = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
    StackEntity newStack = stackDAO.find(newStackId.getStackName(), newStackId.getStackVersion());

    Assert.assertFalse( stackId.equals(newStackId) );

    String configType = "foo-type";

    ClusterConfigEntity clusterConfig = new ClusterConfigEntity();
    clusterConfig.setClusterEntity(clusterEntity);
    clusterConfig.setConfigId(1L);
    clusterConfig.setStack(currentStack);
    clusterConfig.setTag("version-1");
    clusterConfig.setData("{}");
    clusterConfig.setType(configType);
    clusterConfig.setTimestamp(1L);
    clusterConfig.setVersion(1L);

    clusterDAO.createConfig(clusterConfig);
    clusterEntity.getClusterConfigEntities().add(clusterConfig);
    clusterEntity = clusterDAO.merge(clusterEntity);

    ClusterConfigEntity newClusterConfig = new ClusterConfigEntity();
    newClusterConfig.setClusterEntity(clusterEntity);
    newClusterConfig.setConfigId(2L);
    newClusterConfig.setStack(newStack);
    newClusterConfig.setTag("version-2");
    newClusterConfig.setData("{}");
    newClusterConfig.setType(configType);
    newClusterConfig.setTimestamp(2L);
    newClusterConfig.setVersion(2L);

    clusterDAO.createConfig(newClusterConfig);
    clusterEntity.getClusterConfigEntities().add(newClusterConfig);
    clusterEntity = clusterDAO.merge(clusterEntity);

    // config mapping set to 1
    ClusterConfigMappingEntity configMapping = new ClusterConfigMappingEntity();
    configMapping.setClusterEntity(clusterEntity);
    configMapping.setCreateTimestamp(1L);
    configMapping.setSelected(1);
    configMapping.setTag("version-1");
    configMapping.setType(configType);
    configMapping.setUser("admin");

    // new config mapping set to 0
    ClusterConfigMappingEntity newConfigMapping = new ClusterConfigMappingEntity();
    newConfigMapping.setClusterEntity(clusterEntity);
    newConfigMapping.setCreateTimestamp(2L);
    newConfigMapping.setSelected(0);
    newConfigMapping.setTag("version-2");
    newConfigMapping.setType(configType);
    newConfigMapping.setUser("admin");

    clusterDAO.persistConfigMapping(configMapping);
    clusterDAO.persistConfigMapping(newConfigMapping);
    clusterEntity.getConfigMappingEntities().add(configMapping);
    clusterEntity.getConfigMappingEntities().add(newConfigMapping);
    clusterEntity = clusterDAO.merge(clusterEntity);

    // check that the original mapping is enabled
    Collection<ClusterConfigMappingEntity> clusterConfigMappings = clusterEntity.getConfigMappingEntities();
    Assert.assertEquals(2, clusterConfigMappings.size());
    for (ClusterConfigMappingEntity clusterConfigMapping : clusterConfigMappings) {
      if (clusterConfigMapping.getTag().equals("version-1")) {
        Assert.assertEquals(1, clusterConfigMapping.isSelected());
      } else {
        Assert.assertEquals(0, clusterConfigMapping.isSelected());
      }
    }

    cluster.applyLatestConfigurations(newStackId);
    clusterEntity = clusterDAO.findByName("c1");

    // now check that the new config mapping is enabled
    clusterConfigMappings = clusterEntity.getConfigMappingEntities();
    Assert.assertEquals(2, clusterConfigMappings.size());
    for (ClusterConfigMappingEntity clusterConfigMapping : clusterConfigMappings) {
      if (clusterConfigMapping.getTag().equals("version-1")) {
        Assert.assertEquals(0, clusterConfigMapping.isSelected());
      } else {
        Assert.assertEquals(1, clusterConfigMapping.isSelected());
      }
    }
  }

  /**
   * Tests that {@link Cluster#applyLatestConfigurations(StackId)} sets the
   * right configs to enabled when there are duplicate mappings for type/tag.
   * Only the most recent should be enabled.
   *
   * @throws Exception
   */
  @Test
  public void testApplyLatestConfigurationsWithMultipleMappings() throws Exception {
    createDefaultCluster();
    Cluster cluster = clusters.getCluster("c1");
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");
    StackId stackId = cluster.getCurrentStackVersion();

    StackEntity currentStack = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());

    String configType = "foo-type";
    String configTag = "version-1";

    // create the config for the mappings
    ClusterConfigEntity clusterConfig = new ClusterConfigEntity();
    clusterConfig.setClusterEntity(clusterEntity);
    clusterConfig.setConfigId(1L);
    clusterConfig.setStack(currentStack);
    clusterConfig.setTag(configTag);
    clusterConfig.setData("{}");
    clusterConfig.setType(configType);
    clusterConfig.setTimestamp(1L);
    clusterConfig.setVersion(1L);

    clusterDAO.createConfig(clusterConfig);
    clusterEntity.getClusterConfigEntities().add(clusterConfig);
    clusterEntity = clusterDAO.merge(clusterEntity);

    // create 3 mappings for the same type/tag, each with a different time

    // config mapping 1
    ClusterConfigMappingEntity configMapping = new ClusterConfigMappingEntity();
    configMapping.setClusterEntity(clusterEntity);
    configMapping.setCreateTimestamp(1L);
    configMapping.setSelected(0);
    configMapping.setTag(configTag);
    configMapping.setType(configType);
    configMapping.setUser("admin");
    clusterDAO.persistConfigMapping(configMapping);
    clusterEntity.getConfigMappingEntities().add(configMapping);

    // config mapping 2
    configMapping = new ClusterConfigMappingEntity();
    configMapping.setClusterEntity(clusterEntity);
    configMapping.setCreateTimestamp(2L);
    configMapping.setSelected(0);
    configMapping.setTag(configTag);
    configMapping.setType(configType);
    configMapping.setUser("admin");
    clusterDAO.persistConfigMapping(configMapping);
    clusterEntity.getConfigMappingEntities().add(configMapping);

    // config mapping 3
    configMapping = new ClusterConfigMappingEntity();
    configMapping.setClusterEntity(clusterEntity);
    configMapping.setCreateTimestamp(3L);
    configMapping.setSelected(0);
    configMapping.setTag(configTag);
    configMapping.setType(configType);
    configMapping.setUser("admin");
    clusterDAO.persistConfigMapping(configMapping);
    clusterEntity.getConfigMappingEntities().add(configMapping);

    clusterEntity = clusterDAO.merge(clusterEntity);

    // check all 3 mappings are disabled
    Collection<ClusterConfigMappingEntity> clusterConfigMappings = clusterEntity.getConfigMappingEntities();
    Assert.assertEquals(3, clusterConfigMappings.size());
    for (ClusterConfigMappingEntity clusterConfigMapping : clusterConfigMappings) {
      Assert.assertEquals(0, clusterConfigMapping.isSelected());
    }

    // apply configurations and check to see we've set the one with the latest
    // timestamp ONLY
    cluster.applyLatestConfigurations(cluster.getCurrentStackVersion());
    clusterEntity = clusterDAO.findByName("c1");

    // now check that the new config mapping is enabled
    clusterConfigMappings = clusterEntity.getConfigMappingEntities();
    Assert.assertEquals(3, clusterConfigMappings.size());
    for (ClusterConfigMappingEntity clusterConfigMapping : clusterConfigMappings) {
      if (clusterConfigMapping.getCreateTimestamp() < 3) {
        Assert.assertEquals(0, clusterConfigMapping.isSelected());
      } else {
        Assert.assertEquals(1, clusterConfigMapping.isSelected());
      }
    }
  }

  /**
   * Tests that applying configurations for a given stack correctly sets
   * {@link DesiredConfig}s.
   */
  @Test
  public void testDesiredConfigurationsAfterApplyingLatestForStack() throws Exception {
    createDefaultCluster();
    Cluster cluster = clusters.getCluster("c1");
    StackId stackId = cluster.getCurrentStackVersion();
    StackId newStackId = new StackId("HDP-2.2.0");

    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);

    // make sure the stacks are different
    Assert.assertFalse(stackId.equals(newStackId));

    Map<String, String> properties = new HashMap<String, String>();
    Map<String, Map<String, String>> propertiesAttributes = new HashMap<String, Map<String, String>>();

    // foo-type for v1 on current stack
    properties.put("foo-property-1", "foo-value-1");
    Config c1 = configFactory.createNew(cluster, "foo-type", "version-1", properties, propertiesAttributes);

    // make v1 "current"
    cluster.addDesiredConfig("admin", Sets.newHashSet(c1), "note-1");

    // bump the stack
    cluster.setDesiredStackVersion(newStackId);

    // save v2
    // foo-type for v2 on new stack
    properties.put("foo-property-2", "foo-value-2");
    Config c2 = configFactory.createNew(cluster, "foo-type", "version-2", properties, propertiesAttributes);

    // make v2 "current"
    cluster.addDesiredConfig("admin", Sets.newHashSet(c2), "note-2");

    // check desired config
    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    DesiredConfig desiredConfig = desiredConfigs.get("foo-type");
    desiredConfig = desiredConfigs.get("foo-type");
    assertNotNull(desiredConfig);
    assertEquals(Long.valueOf(2), desiredConfig.getVersion());
    assertEquals("version-2", desiredConfig.getTag());

    String hostName = cluster.getHosts().iterator().next().getHostName();

    // {foo-type={tag=version-2}}
    Map<String, Map<String, String>> effectiveDesiredTags = configHelper.getEffectiveDesiredTags(
        cluster, hostName);

    assertEquals("version-2", effectiveDesiredTags.get("foo-type").get("tag"));

    // move the stack back to the old stack
    cluster.setDesiredStackVersion(stackId);

    // apply the configs for the old stack
    cluster.applyLatestConfigurations(stackId);

    // {foo-type={tag=version-1}}
    effectiveDesiredTags = configHelper.getEffectiveDesiredTags(cluster, hostName);
    assertEquals("version-1", effectiveDesiredTags.get("foo-type").get("tag"));

    desiredConfigs = cluster.getDesiredConfigs();
    desiredConfig = desiredConfigs.get("foo-type");
    assertNotNull(desiredConfig);
    assertEquals(Long.valueOf(1), desiredConfig.getVersion());
    assertEquals("version-1", desiredConfig.getTag());
  }

  /**
   * Tests removing configurations and configuration mappings by stack.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveConfigurations() throws Exception {
    createDefaultCluster();
    Cluster cluster = clusters.getCluster("c1");
    ClusterEntity clusterEntity = clusterDAO.findByName("c1");
    StackId stackId = cluster.getCurrentStackVersion();
    StackId newStackId = new StackId("HDP-2.0.6");

    StackEntity currentStack = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
    StackEntity newStack = stackDAO.find(newStackId.getStackName(), newStackId.getStackVersion());

    Assert.assertFalse(stackId.equals(newStackId));

    String configType = "foo-type";

    ClusterConfigEntity clusterConfig = new ClusterConfigEntity();
    clusterConfig.setClusterEntity(clusterEntity);
    clusterConfig.setConfigId(1L);
    clusterConfig.setStack(currentStack);
    clusterConfig.setTag("version-1");
    clusterConfig.setData("{}");
    clusterConfig.setType(configType);
    clusterConfig.setTimestamp(1L);
    clusterConfig.setVersion(1L);

    clusterDAO.createConfig(clusterConfig);
    clusterEntity.getClusterConfigEntities().add(clusterConfig);
    clusterEntity = clusterDAO.merge(clusterEntity);

    ClusterConfigEntity newClusterConfig = new ClusterConfigEntity();
    newClusterConfig.setClusterEntity(clusterEntity);
    newClusterConfig.setConfigId(2L);
    newClusterConfig.setStack(newStack);
    newClusterConfig.setTag("version-2");
    newClusterConfig.setData("{}");
    newClusterConfig.setType(configType);
    newClusterConfig.setTimestamp(2L);
    newClusterConfig.setVersion(2L);

    clusterDAO.createConfig(newClusterConfig);
    clusterEntity.getClusterConfigEntities().add(newClusterConfig);
    clusterEntity = clusterDAO.merge(clusterEntity);

    // config mapping set to 1
    ClusterConfigMappingEntity configMapping = new ClusterConfigMappingEntity();
    configMapping.setClusterEntity(clusterEntity);
    configMapping.setCreateTimestamp(1L);
    configMapping.setSelected(1);
    configMapping.setTag("version-1");
    configMapping.setType(configType);
    configMapping.setUser("admin");

    // new config mapping set to 0
    ClusterConfigMappingEntity newConfigMapping = new ClusterConfigMappingEntity();
    newConfigMapping.setClusterEntity(clusterEntity);
    newConfigMapping.setCreateTimestamp(2L);
    newConfigMapping.setSelected(0);
    newConfigMapping.setTag("version-2");
    newConfigMapping.setType(configType);
    newConfigMapping.setUser("admin");

    clusterDAO.persistConfigMapping(configMapping);
    clusterDAO.persistConfigMapping(newConfigMapping);
    clusterEntity.getConfigMappingEntities().add(configMapping);
    clusterEntity.getConfigMappingEntities().add(newConfigMapping);
    clusterEntity = clusterDAO.merge(clusterEntity);

    // get back the cluster configs for the new stack
    List<ClusterConfigEntity> clusterConfigs = clusterDAO.getAllConfigurations(
        cluster.getClusterId(), newStackId);

    Assert.assertEquals(1, clusterConfigs.size());

    // remove the configs
    cluster.removeConfigurations(newStackId);

    clusterConfigs = clusterDAO.getAllConfigurations(cluster.getClusterId(), newStackId);
    Assert.assertEquals(0, clusterConfigs.size());
  }

  /**
   * Tests that properties request from {@code cluster-env} are correctly cached
   * and invalidated.
   *
   * @throws Exception
   */
  @Test
  public void testCachedClusterProperties() throws Exception {
    EventBusSynchronizer.synchronizeAmbariEventPublisher(injector);
    AmbariEventPublisher publisher = injector.getInstance(AmbariEventPublisher.class);

    createDefaultCluster();
    Cluster cluster = clusters.getCluster("c1");

    assertFalse(((ClusterImpl) cluster).isClusterPropertyCached("foo"));

    String property = cluster.getClusterProperty("foo", "bar");
    assertEquals("bar", property);

    assertTrue(((ClusterImpl) cluster).isClusterPropertyCached("foo"));

    // cause a cache invalidation
    ClusterConfigChangedEvent event = new ClusterConfigChangedEvent(cluster.getClusterName(),
        ConfigHelper.CLUSTER_ENV, null, 1L);

    publisher.publish(event);

    assertFalse(((ClusterImpl) cluster).isClusterPropertyCached("foo"));
  }

  /**
   * Tests that the {@link ClusterVersionEntity} can be created initially with a
   * state of {@link RepositoryVersionState#INSTALLED}. This state is needed for
   * {@link UpgradeType#HOST_ORDERED}.
   *
   * @throws Exception
   */
  @Test
  public void testClusterVersionCreationWithInstalledState() throws Exception {
    StackId stackId = new StackId("HDP", "0.1");
    StackEntity stackEntity = stackDAO.find(stackId.getStackName(), stackId.getStackVersion());
    org.junit.Assert.assertNotNull(stackEntity);

    String clusterName = "c1";
    clusters.addCluster(clusterName, stackId);
    c1 = clusters.getCluster(clusterName);

    helper.getOrCreateRepositoryVersion(stackId, stackId.getStackVersion());

    c1.createClusterVersion(stackId, stackId.getStackVersion(), "admin",
        RepositoryVersionState.INSTALLED);
  }
}
