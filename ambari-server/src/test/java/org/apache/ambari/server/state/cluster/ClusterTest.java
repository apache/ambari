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
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.agent.AgentEnv;
import org.apache.ambari.server.agent.AgentEnv.Directory;
import org.apache.ambari.server.agent.DiskInfo;
import org.apache.ambari.server.agent.HostInfo;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ClusterResponse;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.entities.*;
import org.apache.ambari.server.state.*;
import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.host.HostHealthyHeartbeatEvent;
import org.apache.ambari.server.state.host.HostRegistrationRequestEvent;
import org.junit.After;
import org.junit.Before;
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

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    clusters = injector.getInstance(Clusters.class);
    serviceFactory = injector.getInstance(ServiceFactory.class);
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
    host.setOsType("centos5");
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
        serviceComponentHostFactory.createNew(sc, "h1", false);
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
        ServiceComponentHost sch1 = serviceComponentHostFactory.createNew(sc1, "h1", false);
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
    Config config1 = configFactory.createNew(c1, "global",
        new HashMap<String, String>() {{ put("a", "b"); }});
    config1.setVersionTag("version1");
    
    Config config2 = configFactory.createNew(c1, "global",
        new HashMap<String, String>() {{ put("x", "y"); }});
    config2.setVersionTag("version2");
    
    Config config3 = configFactory.createNew(c1, "core-site",
        new HashMap<String, String>() {{ put("x", "y"); }});
    config3.setVersionTag("version2");
    
    c1.addConfig(config1);
    c1.addConfig(config2);
    c1.addConfig(config3);
    
    c1.addDesiredConfig("_test", config1);
    Config res = c1.getDesiredConfigByType("global");
    Assert.assertNotNull("Expected non-null config", res);
    
    res = c1.getDesiredConfigByType("core-site");
    Assert.assertNull("Expected null config", res);
    
    c1.addDesiredConfig("_test", config2);
    res = c1.getDesiredConfigByType("global");
    Assert.assertEquals("Expected version tag to be 'version2'", "version2", res.getVersionTag());
    
  }
  
  @Test
  public void testDesiredConfigs() throws Exception {
    Config config1 = configFactory.createNew(c1, "global",
        new HashMap<String, String>() {{ put("a", "b"); }});
    config1.setVersionTag("version1");
    
    Config config2 = configFactory.createNew(c1, "global",
        new HashMap<String, String>() {{ put("x", "y"); }});
    config2.setVersionTag("version2");
    
    Config config3 = configFactory.createNew(c1, "core-site",
        new HashMap<String, String>() {{ put("x", "y"); }});
    config3.setVersionTag("version2");
    
    c1.addConfig(config1);
    c1.addConfig(config2);
    c1.addConfig(config3);
    
    try {
      c1.addDesiredConfig(null, config1);
      fail("Cannot set a null user with config");
    }
    catch (Exception e) {
      // test failure
    }
    
    c1.addDesiredConfig("_test1", config1);
    c1.addDesiredConfig("_test3", config3);
    
    Map<String, DesiredConfig> desiredConfigs = c1.getDesiredConfigs();
    Assert.assertFalse("Expect desired config not contain 'mapred-site'", desiredConfigs.containsKey("mapred-site"));
    Assert.assertTrue("Expect desired config contain " + config1.getType(), desiredConfigs.containsKey("global"));
    Assert.assertTrue("Expect desired config contain " + config3.getType(), desiredConfigs.containsKey("core-site"));
    Assert.assertEquals("Expect desired config for global should be " + config1.getVersionTag(),
        config1.getVersionTag(), desiredConfigs.get(config1.getType()).getVersion());
    Assert.assertEquals("_test1", desiredConfigs.get(config1.getType()).getUser());
    Assert.assertEquals("_test3", desiredConfigs.get(config3.getType()).getUser());
    DesiredConfig dc = desiredConfigs.get(config1.getType());
    Assert.assertTrue("Expect no host-level overrides",
        (null == dc.getHostOverrides() || dc.getHostOverrides().size() == 0));
    
    c1.addDesiredConfig("_test2", config2);
    Assert.assertEquals("_test2", c1.getDesiredConfigs().get(config2.getType()).getUser());
    
    c1.addDesiredConfig("_test1", config1);

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
    Assert.assertEquals(1, r.getHostNames().size());

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

    Config config = configFactory.createNew(c1, "hdfs-site", new HashMap<String, String>(){{
      put("test", "test");
    }});
    config.setVersionTag("1");

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
}
