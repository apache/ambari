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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class TestStagePlanner {

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
  public void testSingleStagePlan() throws AmbariException {
    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));
    
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
    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));
    rco.initialize(cluster);
    RoleGraph rg = new RoleGraph(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1");
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now), "cluster1", "HBASE");
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now), "cluster1", "ZOOKEEPER");
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
    RoleCommandOrder rco = injector.getInstance(RoleCommandOrder.class);
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));
    rco.initialize(cluster);
    RoleGraph rg = new RoleGraph(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1");
    stage.addHostRoleExecutionCommand("host11", Role.SECONDARY_NAMENODE,
        RoleCommand.START, new ServiceComponentHostStartEvent("SECONDARY_NAMENODE",
            "host11", now), "cluster1", "HDFS");
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now), "cluster1", "HBASE");
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now), "cluster1", "ZOOKEEPER");
    stage.addHostRoleExecutionCommand("host4", Role.DATANODE,
        RoleCommand.START, new ServiceComponentHostStartEvent("DATANODE",
            "host4", now), "cluster1", "HDFS");
    stage.addHostRoleExecutionCommand("host4", Role.HBASE_REGIONSERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_REGIONSERVER",
            "host4", now), "cluster1", "HBASE");
    stage.addHostRoleExecutionCommand("host4", Role.TASKTRACKER,
        RoleCommand.START, new ServiceComponentHostStartEvent("TASKTRACKER",
            "host4", now), "cluster1", "MAPREDUCE");
    stage.addHostRoleExecutionCommand("host5", Role.JOBTRACKER,
        RoleCommand.START, new ServiceComponentHostStartEvent("JOBTRACKER",
            "host5", now), "cluster1", "MAPREDUCE");
    stage.addHostRoleExecutionCommand("host6", Role.OOZIE_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("OOZIE_SERVER",
            "host6", now), "cluster1", "OOZIE");
    stage.addHostRoleExecutionCommand("host7", Role.WEBHCAT_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("WEBHCAT_SERVER",
            "host7", now), "cluster1", "WEBHCAT");
    stage.addHostRoleExecutionCommand("host8", Role.NAGIOS_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("NAGIOS_SERVER",
            "host8", now), "cluster1", "NAGIOS");
    stage.addHostRoleExecutionCommand("host4", Role.GANGLIA_MONITOR,
        RoleCommand.START, new ServiceComponentHostStartEvent("GANGLIA_MONITOR",
            "host4", now), "cluster1", "GANGLIA");
    stage.addHostRoleExecutionCommand("host9", Role.GANGLIA_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("GANGLIA_SERVER",
            "host9", now), "cluster1", "GANGLIA");
    System.out.println(stage.toString());
    rg.build(stage);
    System.out.println(rg.stringifyGraph());
    List<Stage> outStages = rg.getStages();
    for (Stage s: outStages) {
      System.out.println(s.toString());
    }
    assertEquals(5, outStages.size());
  }
}
