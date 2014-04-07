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


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import junit.framework.Assert;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.stageplanner.RoleGraphNode;
import org.junit.After;
import org.junit.Test;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.Before;

public class RoleGraphTest {

  
  private Injector injector;
  
  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);    
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  @Test
  public void testValidateOrder() throws AmbariException {
    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);
    ClusterImpl cluster = mock(ClusterImpl.class);
    
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));
    
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

    RoleGraphNode pig_service_check = new RoleGraphNode(Role.PIG_SERVICE_CHECK, RoleCommand.SERVICE_CHECK);
    RoleGraphNode resourcemanager_start = new RoleGraphNode(Role.RESOURCEMANAGER, RoleCommand.START);
    Assert.assertEquals(-1, rco.order(resourcemanager_start, pig_service_check));

    RoleGraphNode hdfs_service_check = new RoleGraphNode(Role.HDFS_SERVICE_CHECK, RoleCommand.SERVICE_CHECK);
    RoleGraphNode snamenode_start = new RoleGraphNode(Role.SECONDARY_NAMENODE, RoleCommand.START);
    Assert.assertEquals(-1, rco.order(snamenode_start, hdfs_service_check));
    
    RoleGraphNode mapred2_service_check = new RoleGraphNode(Role.MAPREDUCE2_SERVICE_CHECK, RoleCommand.SERVICE_CHECK);
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
    RoleGraphNode flume_start = new RoleGraphNode(Role.FLUME_HANDLER, RoleCommand.START);
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
    Service hdfsServiceMock = mock(Service.class);
    ServiceComponent jnComponentMock = mock(ServiceComponent.class);
    when(cluster.getService("HDFS")).thenReturn(hdfsServiceMock);
    when(hdfsServiceMock.getServiceComponent("JOURNALNODE")).thenReturn(jnComponentMock);
    
    rco.initialize(cluster);
    Assert.assertEquals(1, rco.order(nn_start, jn_start));
    Assert.assertEquals(1, rco.order(nn_start, zk_server_start));
    Assert.assertEquals(1, rco.order(zkfc_start, nn_start));
    Assert.assertEquals(1, rco.order(nagios_start, zkfc_start));
    Assert.assertEquals(1, rco.order(nagios_start, jn_start));
  }
}
