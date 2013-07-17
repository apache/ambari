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
package org.apache.ambari.server.stageplanner;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.state.HostState;
import org.apache.ambari.server.orm.entities.HostStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.state.StackId;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import com.google.inject.persist.PersistService;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.After;
import org.junit.Before;

public class TestStagePlanner {

  private Injector injector;

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
    clusterServiceEntity.setServiceConfigMappings(Collections.EMPTY_LIST);
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

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    /*
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
    */
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testSingleStagePlan() {
    RoleCommandOrder rco = new RoleCommandOrder();
    ClusterEntity entity = createDummyData();
    ClusterImpl cluster = new ClusterImpl(entity, injector);
    rco.initialize(cluster);

    RoleGraph rg = new RoleGraph(rco);
    String hostname = "dummy";
    Stage stage = StageUtils.getATestStage(1, 1, hostname);
    rg.build(stage);
    List<Stage> outStages = rg.getStages();
    for (Stage s: outStages) {
      System.out.println(s.toString());
    }
    assertEquals(1, outStages.size());
    assertEquals(stage.getExecutionCommands(hostname), outStages.get(0)
        .getExecutionCommands(hostname));
  }

  @Test
  public void testMultiStagePlan() {
    RoleCommandOrder rco = new RoleCommandOrder();
    ClusterEntity entity = createDummyData();
    ClusterImpl cluster = new ClusterImpl(entity, injector);
    rco.initialize(cluster);
    RoleGraph rg = new RoleGraph(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1");
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now, new HashMap<String, String>()), "cluster1", "HBASE");
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now, new HashMap<String, String>()), "cluster1", "ZOOKEEPER");
    System.out.println(stage.toString());

    rg.build(stage);
    System.out.println(rg.stringifyGraph());
    List<Stage> outStages = rg.getStages();
    for (Stage s: outStages) {
      System.out.println(s.toString());
    }
    assertEquals(3, outStages.size());
  }

  @Test
  public void testManyStages() {
    RoleCommandOrder rco = new RoleCommandOrder();
    ClusterEntity entity = createDummyData();
    ClusterImpl cluster = new ClusterImpl(entity, injector);
    rco.initialize(cluster);
    RoleGraph rg = new RoleGraph(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1");
    stage.addHostRoleExecutionCommand("host11", Role.SECONDARY_NAMENODE,
        RoleCommand.START, new ServiceComponentHostStartEvent("SECONDARY_NAMENODE",
            "host11", now, new HashMap<String, String>()), "cluster1", "HDFS");
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now, new HashMap<String, String>()), "cluster1", "HBASE");
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now, new HashMap<String, String>()), "cluster1", "ZOOKEEPER");
    stage.addHostRoleExecutionCommand("host4", Role.DATANODE,
        RoleCommand.START, new ServiceComponentHostStartEvent("DATANODE",
            "host4", now, new HashMap<String, String>()), "cluster1", "HDFS");
    stage.addHostRoleExecutionCommand("host4", Role.HBASE_REGIONSERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_REGIONSERVER",
            "host4", now, new HashMap<String, String>()), "cluster1", "HBASE");
    stage.addHostRoleExecutionCommand("host4", Role.TASKTRACKER,
        RoleCommand.START, new ServiceComponentHostStartEvent("TASKTRACKER",
            "host4", now, new HashMap<String, String>()), "cluster1", "MAPREDUCE");
    stage.addHostRoleExecutionCommand("host5", Role.JOBTRACKER,
        RoleCommand.START, new ServiceComponentHostStartEvent("JOBTRACKER",
            "host5", now, new HashMap<String, String>()), "cluster1", "MAPREDUCE");
    stage.addHostRoleExecutionCommand("host6", Role.OOZIE_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("OOZIE_SERVER",
            "host6", now, new HashMap<String, String>()), "cluster1", "OOZIE");
    stage.addHostRoleExecutionCommand("host7", Role.WEBHCAT_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("WEBHCAT_SERVER",
            "host7", now, new HashMap<String, String>()), "cluster1", "WEBHCAT");
    stage.addHostRoleExecutionCommand("host8", Role.NAGIOS_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("NAGIOS_SERVER",
            "host8", now, new HashMap<String, String>()), "cluster1", "NAGIOS");
    stage.addHostRoleExecutionCommand("host4", Role.GANGLIA_MONITOR,
        RoleCommand.START, new ServiceComponentHostStartEvent("GANGLIA_MONITOR",
            "host4", now, new HashMap<String, String>()), "cluster1", "GANGLIA");
    stage.addHostRoleExecutionCommand("host9", Role.GANGLIA_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("GANGLIA_SERVER",
            "host9", now, new HashMap<String, String>()), "cluster1", "GANGLIA");
    System.out.println(stage.toString());
    rg.build(stage);
    System.out.println(rg.stringifyGraph());
    List<Stage> outStages = rg.getStages();
    for (Stage s: outStages) {
      System.out.println(s.toString());
    }
    assertEquals(4, outStages.size());
  }
}
