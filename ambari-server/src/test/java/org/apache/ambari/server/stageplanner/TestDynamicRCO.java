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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TestDynamicRCO {

  private static Logger LOG = LoggerFactory.getLogger(TestDynamicRCO.class);

  private Injector injector;


    public ClusterEntity createDummyData(String stackString, String serviceString) {
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
    clusterServiceEntity.setServiceName(serviceString);
    clusterServiceEntity.setClusterEntity(clusterEntity);
    clusterServiceEntity.setServiceComponentDesiredStateEntities(
        Collections.EMPTY_LIST);
    clusterServiceEntity.setServiceConfigMappings(Collections.EMPTY_LIST);
    ServiceDesiredStateEntity stateEntity = mock(ServiceDesiredStateEntity.class);
    Gson gson = new Gson();
    when(stateEntity.getDesiredStackVersion()).thenReturn(gson.toJson(new StackId(stackString),
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
  }

  @After
  public void teardown() {
    injector.getInstance(PersistService.class).stop();
  }

  static private String toString(int v) {
    if (v == 0) { 
      return "True";
    } else {
      return "False";
    }
  }

  @Test
  public void testDynamicRCO() {

    RoleCommandOrder rcoHDFS1 = new RoleCommandOrder();
    RoleCommandOrder rcoHDFS2 = new RoleCommandOrder();
    RoleCommandOrder rcoHCFS = new RoleCommandOrder();

    ClusterEntity entityHDFS1 = createDummyData("HDP-1.3.0", "HDFS");
    ClusterEntity entityHDFS2 = createDummyData("HDP-1.3.1", "HDFS");
    ClusterEntity entityHCFS = createDummyData("HDP-1.3.1", "HCFS");

    ClusterImpl clusterHDFS1 = new ClusterImpl(entityHDFS1, injector);
    ClusterImpl clusterHDFS2 = new ClusterImpl(entityHDFS2, injector);   
    ClusterImpl clusterHCFS = new ClusterImpl(entityHCFS, injector);

    rcoHDFS1.initialize(clusterHDFS1);
    rcoHDFS2.initialize(clusterHDFS2);
    rcoHCFS.initialize(clusterHCFS);

    int c1, c2;
    c1 = rcoHDFS1.compareDeps(rcoHDFS2);
    LOG.debug("HDFS deps match for stacks HDP-1.3.0 and HDP-1.3.1 = " + toString(c1) + ", expected True");

    c2 = rcoHDFS2.compareDeps(rcoHCFS);
    LOG.debug("HDFS deps and HCFS deps match for stack HDP-1.3.1 = " + toString(c2) + ", expected False");

    assertEquals(c1, 0);
    assertFalse("HCFS deps should differ", c2 == 0);

  }

}
