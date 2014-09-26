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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import com.google.common.collect.Multimap;
import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.AgentEnv.Directory;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.controller.ServiceConfigVersionResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ClusterStateEntity;
import org.apache.ambari.server.state.AgentVersion;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ConfigImpl;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostHealthStatus;
import org.apache.ambari.server.state.HostState;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class ClusterTest {
  private static final Logger LOG = LoggerFactory.getLogger(ClusterTest.class);
  
  private Clusters clusters;
  private Cluster c1;
  private Injector injector;
  private ServiceFactory serviceFactory;
  private ServiceComponentFactory serviceComponentFactory;
  private ServiceComponentHostFactory serviceComponentHostFactory;
  private AmbariMetaInfo metaInfo;
  private ConfigFactory configFactory;
  private ConfigGroupFactory configGroupFactory;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
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
    metaInfo.init();
    clusters.addCluster("c1");
    c1 = clusters.getCluster("c1");
    Assert.assertEquals("c1", c1.getClusterName());
    Assert.assertEquals(1, c1.getClusterId());
    clusters.addHost("h1");
    Host host = clusters.getHost("h1");
    host.setIPv4("ipv4");
    host.setIPv6("ipv6");
    
    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");
    host.setHostAttributes(hostAttributes);
    
    host.persist();
    c1.setDesiredStackVersion(new StackId("HDP-0.1"));
    clusters.mapHostToCluster("h1", "c1");
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testAddHost() throws AmbariException {
    clusters.addHost("h2");

    try {
      clusters.addHost("h2");
      fail("Duplicate add should fail");
    }
    catch (AmbariException e) {
      // Expected
    }

  }


  @Test
  public void testGetHostState() throws AmbariException {
    Assert.assertEquals(HostState.INIT, clusters.getHost("h1").getState());
  }

  @Test
  public void testSetHostState() throws AmbariException {
    clusters.getHost("h1").setState(HostState.HEARTBEAT_LOST);
    Assert.assertEquals(HostState.HEARTBEAT_LOST,
        clusters.getHost("h1").getState());
  }

  @Test
  public void testHostEvent() throws AmbariException,
      InvalidStateTransitionException {
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
  public void testBasicClusterSetup() throws AmbariException {
    String clusterName = "c2";

    try {
      clusters.getCluster(clusterName);
      fail("Exception expected for invalid cluster");
    } catch (Exception e) {
      // Expected
    }

    clusters.addCluster(clusterName);
    Cluster c2 = clusters.getCluster(clusterName);

    Assert.assertNotNull(c2);

    Assert.assertEquals(clusterName, c2.getClusterName());

    c2.setClusterName("foo2");
    Assert.assertEquals("foo2", c2.getClusterName());

    Assert.assertNotNull(c2.getDesiredStackVersion());
    Assert.assertEquals("", c2.getDesiredStackVersion().getStackId());

    StackId stackVersion = new StackId("HDP-1.0");
    c2.setDesiredStackVersion(stackVersion);
    Assert.assertEquals("HDP-1.0", c2.getDesiredStackVersion().getStackId());
  }

  @Test
  public void testAddAndGetServices() throws AmbariException {
    // TODO write unit tests for
    // public void addService(Service service) throws AmbariException;
    // public Service getService(String serviceName) throws AmbariException;
    // public Map<String, Service> getServices();

    Service s1 = serviceFactory.createNew(c1, "HDFS");
    Service s2 = serviceFactory.createNew(c1, "MAPREDUCE");

    c1.addService(s1);
    c1.addService(s2);

    s1.persist();
    s2.persist();

    Service s3 = serviceFactory.createNew(c1, "MAPREDUCE");

    try {
      c1.addService(s3);
      fail("Expected error on adding dup service");
    } catch (Exception e) {
      // Expected
    }

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
  public void testGetServiceComponentHosts() throws AmbariException {
    // TODO write unit tests
    // public List<ServiceComponentHost> getServiceComponentHosts(String hostname);

    Service s = serviceFactory.createNew(c1, "HDFS");
    c1.addService(s);
    s.persist();
    ServiceComponent sc = serviceComponentFactory.createNew(s, "NAMENODE");
    s.addServiceComponent(sc);
    sc.persist();
    ServiceComponentHost sch =
        serviceComponentHostFactory.createNew(sc, "h1");
    sc.addServiceComponentHost(sch);
    sch.persist();

    List<ServiceComponentHost> scHosts = c1.getServiceComponentHosts("h1");
    Assert.assertEquals(1, scHosts.size());
    
    Iterator<ServiceComponentHost> iterator = scHosts.iterator();
    
    //Try to iterate on sch and modify it in loop
    try {
      while (iterator.hasNext()) {
        iterator.next();
        Service s1 = serviceFactory.createNew(c1, "PIG");
        c1.addService(s1);
        s1.persist();
        ServiceComponent sc1 = serviceComponentFactory.createNew(s1, "PIG");
        s1.addServiceComponent(sc1);
        sc1.persist();
        ServiceComponentHost sch1 = serviceComponentHostFactory.createNew(sc1, "h1");
        sc1.addServiceComponentHost(sch1);
        sch1.persist();
      }
    } catch (ConcurrentModificationException e ) {
      Assert.assertTrue("Failed to work concurrently with sch", false);
    }
    
    scHosts = c1.getServiceComponentHosts("h1");
    Assert.assertEquals(2, scHosts.size());
  }


  @Test
  public void testGetAndSetConfigs() {
    
    Map<String, Map<String, String>> c1PropAttributes = new HashMap<String, Map<String,String>>();
    c1PropAttributes.put("final", new HashMap<String, String>());
    c1PropAttributes.get("final").put("a", "true");
    Map<String, Map<String, String>> c2PropAttributes = new HashMap<String, Map<String,String>>();
    c2PropAttributes.put("final", new HashMap<String, String>());
    c2PropAttributes.get("final").put("x", "true");
    Config config1 = configFactory.createNew(c1, "global",
        new HashMap<String, String>() {{ put("a", "b"); }}, c1PropAttributes);
    config1.setTag("version1");
    
    Config config2 = configFactory.createNew(c1, "global",
        new HashMap<String, String>() {{ put("x", "y"); }}, c2PropAttributes);
    config2.setTag("version2");
    
    Config config3 = configFactory.createNew(c1, "core-site",
        new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());
    config3.setTag("version2");
    
    c1.addConfig(config1);
    c1.addConfig(config2);
    c1.addConfig(config3);
    
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
    Config config1 = configFactory.createNew(c1, "global",
        new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());
    config1.setTag("version1");
    
    Config config2 = configFactory.createNew(c1, "global",
        new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version2");
    
    Config config3 = configFactory.createNew(c1, "core-site",
        new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());
    config3.setTag("version2");
    
    c1.addConfig(config1);
    c1.addConfig(config2);
    c1.addConfig(config3);
    
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
  
  public ClusterEntity createDummyData() {
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1L);
    clusterEntity.setClusterName("test_cluster1");
    clusterEntity.setClusterInfo("test_cluster_info1");

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
    clusterEntity.setClusterConfigEntities(Collections.EMPTY_LIST);
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
        Collections.EMPTY_LIST);
    ServiceDesiredStateEntity stateEntity = mock(ServiceDesiredStateEntity.class);
    Gson gson = new Gson();
    when(stateEntity.getDesiredStackVersion()).thenReturn(gson.toJson(new StackId("HDP-0.1"),
        StackId.class));
    clusterServiceEntity.setServiceDesiredStateEntity(stateEntity);
    List<ClusterServiceEntity> clusterServiceEntities = new ArrayList<ClusterServiceEntity>();
    clusterServiceEntities.add(clusterServiceEntity);
    clusterEntity.setClusterServiceEntities(clusterServiceEntities);
    return clusterEntity;
  }
  
  @Test
  public void testClusterRecovery() throws AmbariException {
    ClusterEntity entity = createDummyData();
    ClusterStateEntity clusterStateEntity = new ClusterStateEntity();
    clusterStateEntity.setCurrentStackVersion("{\"stackName\":\"HDP\",\"stackVersion\":\"0.1\"}");
    entity.setClusterStateEntity(clusterStateEntity);
    ClusterImpl cluster = new ClusterImpl(entity, injector);
    Service service = cluster.getService("HDFS");
    /* make sure the services are recovered */
    Assert.assertEquals("HDFS",service.getName());
    Map<String, Service> services = cluster.getServices();
    Assert.assertNotNull(services.get("HDFS"));
  }
  

  @Test
  public void testConvertToResponse() throws AmbariException {
    ClusterResponse r = c1.convertToResponse();
    Assert.assertEquals(c1.getClusterId(), r.getClusterId().longValue());
    Assert.assertEquals(c1.getClusterName(), r.getClusterName());
    Assert.assertEquals(Integer.valueOf(1), r.getTotalHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getAlertStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getHealthyStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getUnhealthyStatusHosts());
    Assert.assertEquals(1, r.getClusterHealthReport().getUnknownStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getStaleConfigsHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getMaintenanceStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getHealthyStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getHeartbeatLostStateHosts());
    Assert.assertEquals(1, r.getClusterHealthReport().getInitStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getUnhealthyStateHosts());

    clusters.addHost("h2");
    Host host = clusters.getHost("h2");
    host.setIPv4("ipv4");
    host.setIPv6("ipv6");

    Map<String, String> hostAttributes = new HashMap<String, String>();
    hostAttributes.put("os_family", "redhat");
    hostAttributes.put("os_release_version", "5.9");
    host.setHostAttributes(hostAttributes);
    host.setState(HostState.HEALTHY);
    host.setHealthStatus(new HostHealthStatus(HostHealthStatus.HealthStatus.HEALTHY, ""));
    host.setStatus(host.getHealthStatus().getHealthStatus().name());
    host.persist();
    c1.setDesiredStackVersion(new StackId("HDP-2.0.6"));
    clusters.mapHostToCluster("h2", "c1");

    r = c1.convertToResponse();

    Assert.assertEquals(Integer.valueOf(2), r.getTotalHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getAlertStatusHosts());
    Assert.assertEquals(1, r.getClusterHealthReport().getHealthyStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getUnhealthyStatusHosts());
    Assert.assertEquals(1, r.getClusterHealthReport().getUnknownStatusHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getStaleConfigsHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getMaintenanceStateHosts());
    Assert.assertEquals(1, r.getClusterHealthReport().getHealthyStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getHeartbeatLostStateHosts());
    Assert.assertEquals(1, r.getClusterHealthReport().getInitStateHosts());
    Assert.assertEquals(0, r.getClusterHealthReport().getUnhealthyStateHosts());

    // TODO write unit tests for debug dump
    StringBuilder sb = new StringBuilder();
    c1.debugDump(sb);
  }

  @Test
  public void testDeleteService() throws Exception {
    c1.addService("MAPREDUCE").persist();

    Service hdfs = c1.addService("HDFS");
    hdfs.persist();
    ServiceComponent nameNode = hdfs.addServiceComponent("NAMENODE");
    nameNode.persist();


    assertEquals(2, c1.getServices().size());
    assertEquals(2, injector.getProvider(EntityManager.class).get().
        createQuery("SELECT service FROM ClusterServiceEntity service").getResultList().size());

    c1.deleteService("HDFS");

    assertEquals(1, c1.getServices().size());
    assertEquals(1, injector.getProvider(EntityManager.class).get().
        createQuery("SELECT service FROM ClusterServiceEntity service").getResultList().size());
  }

  @Test
  public void testGetHostsDesiredConfigs() throws Exception {
    Host host1 = clusters.getHost("h1");

    Map<String, Map<String, String>> propAttributes = new HashMap<String, Map<String,String>>();
    propAttributes.put("final", new HashMap<String, String>());
    propAttributes.get("final").put("test", "true");
    Config config = configFactory.createNew(c1, "hdfs-site", new HashMap<String, String>(){{
      put("test", "test");
    }}, propAttributes);
    config.setTag("1");

    host1.addDesiredConfig(c1.getClusterId(), true, "test", config);

    Map<String, Map<String, DesiredConfig>> configs = c1.getAllHostsDesiredConfigs();

    assertTrue(configs.containsKey("h1"));
    assertEquals(1, configs.get("h1").size());

    List<String> hostnames = new ArrayList<String>();
    hostnames.add("h1");

    configs = c1.getHostsDesiredConfigs(hostnames);

    assertTrue(configs.containsKey("h1"));
    assertEquals(1, configs.get("h1").size());
  }
  
  @Test
  public void testProvisioningState() throws AmbariException {
    c1.setProvisioningState(State.INIT);
    Assert.assertEquals(State.INIT,
        c1.getProvisioningState());
    
    c1.setProvisioningState(State.INSTALLED);
    Assert.assertEquals(State.INSTALLED,
        c1.getProvisioningState());    
  }
  
  @Test
  public void testAlertUpdates() {
    
    Set<Alert> alerts = new HashSet<Alert>();
    alerts.add(new Alert("alert_1", "instance1", "service", "component", "host1", AlertState.OK));
    alerts.add(new Alert("alert_1", "instance2", "service", "component", "host1", AlertState.OK));
    
    c1.addAlerts(alerts);
    
    Collection<Alert> result = c1.getAlerts();
    assertEquals(2, result.size());
    for (Alert a : result) {
      assertEquals("alert_1", a.getName());
    }
    
    // add an alert that removes the previous 2 with the same name
    alerts = new HashSet<Alert>();
    alerts.add(new Alert("alert_1", null, "service", "component", "host1", AlertState.WARNING));
    c1.addAlerts(alerts);
    
    result = c1.getAlerts();
    assertEquals(1, result.size());
    
    // add alerts that remove the old type, regardless of instance name
    alerts = new HashSet<Alert>();    
    alerts.add(new Alert("alert_1", "instance1", "service", "component", "host1", AlertState.OK));
    alerts.add(new Alert("alert_1", "instance2", "service", "component", "host1", AlertState.OK));
    c1.addAlerts(alerts);
    
    result = c1.getAlerts();
    assertEquals(2, result.size());
  }

  @Test
  public void testServiceConfigVersions() throws AmbariException {
    Config config1 = configFactory.createNew(c1, "hdfs-site",
      new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());
    config1.setTag("version1");

    Config config2 = configFactory.createNew(c1, "hdfs-site",
      new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version2");

    c1.addConfig(config1);
    c1.addConfig(config2);

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
  public void testSingleServiceVersionForMultipleConfigs() {
    Config config1 = configFactory.createNew(c1, "hdfs-site",
      new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());
    config1.setTag("version1");

    Config config2 = configFactory.createNew(c1, "core-site",
      new HashMap<String, String>() {{ put("x", "y"); }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version2");

    c1.addConfig(config1);
    c1.addConfig(config2);

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
  public void testServiceConfigVersionsForGroups() throws AmbariException {
    Config config1 = configFactory.createNew(c1, "hdfs-site",
      new HashMap<String, String>() {{ put("a", "b"); }}, new HashMap<String, Map<String,String>>());
    config1.setTag("version1");

    c1.addConfig(config1);

    ServiceConfigVersionResponse scvResponse =
      c1.addDesiredConfig("admin", Collections.singleton(config1));

    assertEquals("SCV 1 should be created", Long.valueOf(1), scvResponse.getVersion());

    Map<String, Collection<ServiceConfigVersionResponse>> activeServiceConfigVersions =
      c1.getActiveServiceConfigVersions();
    Assert.assertEquals("Only one scv should be active", 1, activeServiceConfigVersions.get("HDFS").size());

    //create config group
    Config config2 = configFactory.createNew(c1, "hdfs-site",
      new HashMap<String, String>() {{ put("a", "c"); }}, new HashMap<String, Map<String,String>>());
    config2.setTag("version2");

    ConfigGroup configGroup =
      configGroupFactory.createNew(c1, "test group", "HDFS", "descr", Collections.singletonMap("hdfs-site", config2),
        Collections.<String, Host>emptyMap());

    configGroup.persist();

    c1.addConfigGroup(configGroup);

    scvResponse = c1.createServiceConfigVersion("HDFS", "admin", "test note", configGroup);
    assertEquals("SCV 2 should be created", Long.valueOf(2), scvResponse.getVersion());

    //two scv active
    activeServiceConfigVersions = c1.getActiveServiceConfigVersions();
    Assert.assertEquals("Two service config versions should be active, for default and test groups",
      2, activeServiceConfigVersions.get("HDFS").size());

    Config config3 = configFactory.createNew(c1, "hdfs-site",
      new HashMap<String, String>() {{ put("a", "d"); }}, new HashMap<String, Map<String,String>>());

    configGroup.setConfigurations(Collections.singletonMap("hdfs-site", config3));

    configGroup.persist();
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

    Config config4 = new ConfigImpl("hdfs-site");
    config4.setProperties(new HashMap<String, String>() {{
      put("a", "b");
    }});

    ConfigGroup configGroup2 =
        configGroupFactory.createNew(c1, "test group 2", "HDFS", "descr", Collections.singletonMap("hdfs-site", config4),
            Collections.<String, Host>emptyMap());

    configGroup2.persist();
    c1.addConfigGroup(configGroup2);

    scvResponse = c1.createServiceConfigVersion("HDFS", "admin", "test note", configGroup2);
    assertEquals("SCV 5 should be created", Long.valueOf(5), scvResponse.getVersion());

    activeServiceConfigVersions = c1.getActiveServiceConfigVersions();
    Assert.assertEquals("Three service config versions should be active, for default and test groups",
        3, activeServiceConfigVersions.get("HDFS").size());
    assertEquals("Five total scvs", 5, c1.getServiceConfigVersions().size());

  }


}
