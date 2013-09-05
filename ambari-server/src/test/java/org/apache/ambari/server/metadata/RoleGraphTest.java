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

package org.apache.ambari.server.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.stageplanner.RoleGraphNode;
import org.junit.Test;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
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

public class RoleGraphTest {

  private Injector injector;

  public ClusterEntity createDummyData() {
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterName("test_cluster1");
    clusterEntity.setClusterInfo("test_cluster_info1");
    clusterEntity.setClusterId(1l);

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
    when(stateEntity.getDesiredStackVersion()).thenReturn(gson.toJson(new StackId("HDP-2.0.5"),
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
  public void testValidateOrder() {
    RoleCommandOrder rco = new RoleCommandOrder();
    ClusterEntity entity = createDummyData();
    ClusterImpl cluster = new ClusterImpl(entity, injector);
    rco.initialize(cluster);

    RoleGraphNode datanode_upgrade = new RoleGraphNode(Role.DATANODE, RoleCommand.UPGRADE);
    RoleGraphNode hdfs_client_upgrade = new RoleGraphNode(Role.HDFS_CLIENT, RoleCommand.UPGRADE);
    Assert.assertEquals(-1, rco.order(datanode_upgrade, hdfs_client_upgrade));
    Assert.assertEquals(1, rco.order(hdfs_client_upgrade, datanode_upgrade));

    RoleGraphNode namenode_upgrade = new RoleGraphNode(Role.NAMENODE, RoleCommand.UPGRADE);
    RoleGraphNode ganglia_server_upgrade = new RoleGraphNode(Role.GANGLIA_SERVER, RoleCommand.UPGRADE);
    Assert.assertEquals(1, rco.order(ganglia_server_upgrade, hdfs_client_upgrade));
    Assert.assertEquals(1, rco.order(ganglia_server_upgrade, datanode_upgrade));
    Assert.assertEquals(-1, rco.order(namenode_upgrade, ganglia_server_upgrade));

    RoleGraphNode datanode_start = new RoleGraphNode(Role.DATANODE, RoleCommand.START);
    RoleGraphNode datanode_install = new RoleGraphNode(Role.DATANODE, RoleCommand.INSTALL);
    RoleGraphNode jobtracker_start = new RoleGraphNode(Role.JOBTRACKER, RoleCommand.START);
    RoleGraphNode tasktracker_start = new RoleGraphNode(Role.TASKTRACKER, RoleCommand.START);
    Assert.assertEquals(1, rco.order(datanode_start, datanode_install));
    Assert.assertEquals(1, rco.order(jobtracker_start, datanode_start));
    Assert.assertEquals(0, rco.order(jobtracker_start, jobtracker_start));

    RoleGraphNode hive_client_install = new RoleGraphNode(Role.HIVE_CLIENT,
      RoleCommand.INSTALL);
    RoleGraphNode mapred_client_install = new RoleGraphNode(Role.MAPREDUCE_CLIENT,
      RoleCommand.INSTALL);
    RoleGraphNode hcat_client_install = new RoleGraphNode(Role.HCAT,
      RoleCommand.INSTALL);
    RoleGraphNode nagios_server_install = new RoleGraphNode(Role.NAGIOS_SERVER,
      RoleCommand.INSTALL);
    RoleGraphNode oozie_client_install = new RoleGraphNode(Role.OOZIE_CLIENT,
      RoleCommand.INSTALL);
    Assert.assertEquals(1, rco.order(nagios_server_install, hive_client_install));
    Assert.assertEquals(1, rco.order(nagios_server_install, mapred_client_install));
    Assert.assertEquals(1, rco.order(nagios_server_install, hcat_client_install));
    Assert.assertEquals(1, rco.order(nagios_server_install, oozie_client_install));

    RoleGraphNode pig_service_check = new RoleGraphNode(Role.PIG_SERVICE_CHECK, RoleCommand.EXECUTE);
    RoleGraphNode resourcemanager_start = new RoleGraphNode(Role.RESOURCEMANAGER, RoleCommand.START);
    Assert.assertEquals(-1, rco.order(resourcemanager_start, pig_service_check));

    RoleGraphNode hdfs_service_check = new RoleGraphNode(Role.HDFS_SERVICE_CHECK, RoleCommand.EXECUTE);
    RoleGraphNode snamenode_start = new RoleGraphNode(Role.SECONDARY_NAMENODE, RoleCommand.START);
    Assert.assertEquals(-1, rco.order(snamenode_start, hdfs_service_check));
    
    RoleGraphNode mapred2_service_check = new RoleGraphNode(Role.MAPREDUCE2_SERVICE_CHECK, RoleCommand.EXECUTE);
    RoleGraphNode rm_start = new RoleGraphNode(Role.RESOURCEMANAGER, RoleCommand.START);
    RoleGraphNode nm_start = new RoleGraphNode(Role.NODEMANAGER, RoleCommand.START);
    RoleGraphNode hs_start = new RoleGraphNode(Role.HISTORYSERVER, RoleCommand.START);
    RoleGraphNode nagios_start = new RoleGraphNode(Role.NAGIOS_SERVER, RoleCommand.START);
    Assert.assertEquals(-1, rco.order(rm_start, mapred2_service_check));
    Assert.assertEquals(-1, rco.order(nm_start, mapred2_service_check)); 
    Assert.assertEquals(-1, rco.order(hs_start, mapred2_service_check));
    Assert.assertEquals(-1, rco.order(hs_start, mapred2_service_check));
    Assert.assertEquals(1, rco.order(nm_start, rm_start));
    
    //Non-HA mode
    RoleGraphNode nn_start = new RoleGraphNode(Role.NAMENODE, RoleCommand.START);
    RoleGraphNode jn_start = new RoleGraphNode(Role.JOURNALNODE, RoleCommand.START);
    RoleGraphNode zk_server_start = new RoleGraphNode(Role.ZOOKEEPER_SERVER, RoleCommand.START);
    RoleGraphNode hbase_master_start = new RoleGraphNode(Role.HBASE_MASTER, RoleCommand.START);
    RoleGraphNode hbase_reg_srv_start = new RoleGraphNode(Role.HBASE_REGIONSERVER, RoleCommand.START);
    RoleGraphNode ganglia_server_start = new RoleGraphNode(Role.GANGLIA_SERVER, RoleCommand.START);
    RoleGraphNode ganglia_monitor_start = new RoleGraphNode(Role.GANGLIA_MONITOR, RoleCommand.START);
    RoleGraphNode hcat_start = new RoleGraphNode(Role.HCAT, RoleCommand.START);
    RoleGraphNode hive_srv_start = new RoleGraphNode(Role.HIVE_SERVER, RoleCommand.START);
    RoleGraphNode hive_ms_start = new RoleGraphNode(Role.HIVE_METASTORE, RoleCommand.START);
    RoleGraphNode hue_start = new RoleGraphNode(Role.HUE_SERVER, RoleCommand.START);
    RoleGraphNode mysql_start = new RoleGraphNode(Role.MYSQL_SERVER, RoleCommand.START);
    RoleGraphNode oozie_srv_start = new RoleGraphNode(Role.OOZIE_SERVER, RoleCommand.START);
    RoleGraphNode pig_start = new RoleGraphNode(Role.PIG, RoleCommand.START);
    RoleGraphNode sqoop_start = new RoleGraphNode(Role.SQOOP, RoleCommand.START);
    RoleGraphNode webhcat_srv_start = new RoleGraphNode(Role.WEBHCAT_SERVER, RoleCommand.START);
    RoleGraphNode flume_start = new RoleGraphNode(Role.FLUME_SERVER, RoleCommand.START);
    RoleGraphNode zkfc_start = new RoleGraphNode(Role.ZKFC, RoleCommand.START);
    
    Assert.assertEquals(0, rco.order(nn_start, jn_start));
    Assert.assertEquals(0, rco.order(nn_start, zk_server_start));
    Assert.assertEquals(0, rco.order(zkfc_start, nn_start));
    // Check that Nagios starts after other components
    Assert.assertEquals(1, rco.order(nagios_start, nn_start));
    Assert.assertEquals(1, rco.order(nagios_start, snamenode_start));
    Assert.assertEquals(1, rco.order(nagios_start, datanode_start));
    Assert.assertEquals(1, rco.order(nagios_start, resourcemanager_start));
    Assert.assertEquals(1, rco.order(nagios_start, nm_start));
    Assert.assertEquals(1, rco.order(nagios_start, hbase_master_start));
    Assert.assertEquals(1, rco.order(nagios_start, hbase_reg_srv_start));
    Assert.assertEquals(1, rco.order(nagios_start, ganglia_server_start));
    Assert.assertEquals(1, rco.order(nagios_start, ganglia_monitor_start));
    Assert.assertEquals(1, rco.order(nagios_start, hcat_start));
    Assert.assertEquals(1, rco.order(nagios_start, hs_start));
    Assert.assertEquals(1, rco.order(nagios_start, hive_srv_start));
    Assert.assertEquals(1, rco.order(nagios_start, hue_start));
    Assert.assertEquals(1, rco.order(nagios_start, jobtracker_start));
    Assert.assertEquals(1, rco.order(nagios_start, tasktracker_start));
    Assert.assertEquals(1, rco.order(nagios_start, zk_server_start));
    Assert.assertEquals(1, rco.order(nagios_start, mysql_start));
    Assert.assertEquals(1, rco.order(nagios_start, oozie_srv_start));
    Assert.assertEquals(1, rco.order(nagios_start, pig_start));
    Assert.assertEquals(1, rco.order(nagios_start, sqoop_start));
    Assert.assertEquals(1, rco.order(nagios_start, webhcat_srv_start));
    Assert.assertEquals(1, rco.order(nagios_start, flume_start));



    //Enable HA for cluster
    try {
      cluster.getService("HDFS").addServiceComponent("JOURNALNODE");
    } catch (AmbariException e) {
      Assert.fail("Failed to add journal node for cluster.");
    }
    rco.initialize(cluster);
    Assert.assertEquals(1, rco.order(nn_start, jn_start));
    Assert.assertEquals(1, rco.order(nn_start, zk_server_start));
    Assert.assertEquals(1, rco.order(zkfc_start, nn_start));
    Assert.assertEquals(1, rco.order(nagios_start, zkfc_start));
    Assert.assertEquals(1, rco.order(nagios_start, jn_start));



  }
}
