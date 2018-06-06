/*
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

import java.sql.SQLException;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.CommandExecutionType;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.metadata.RoleCommandOrder;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClusterImpl;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostStartEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import junit.framework.Assert;

public class TestStagePlanner {
  private static final Logger log = LoggerFactory.getLogger(TestStagePlanner.class);

  private Injector injector;

  @Inject
  private StageFactory stageFactory;
  @Inject
  private RoleCommandOrderProvider roleCommandOrderProvider;
  @Inject
  private StageUtils stageUtils;
  @Inject
  private RoleGraphFactory roleGraphFactory;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testSingleStagePlan() throws AmbariException {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);

    RoleGraph rg = roleGraphFactory.createNew(rco);
    String hostname = "dummy";
    Stage stage = StageUtils.getATestStage(1, 1, hostname, "", "");
    rg.build(stage);
    List<Stage> outStages = rg.getStages();
    for (Stage s: outStages) {
      log.info(s.toString());
    }
    assertEquals(1, outStages.size());
    assertEquals(stage.getExecutionCommands(hostname), outStages.get(0)
      .getExecutionCommands(hostname));
  }

  @Test
  public void testSCCInGraphDetectedShort() {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6.1"));

    Service hbaseService = mock(Service.class);
    when(hbaseService.getStackId()).thenReturn(new StackId("HDP-2.0.6.1"));
    Service zkService = mock(Service.class);
    when(zkService.getStackId()).thenReturn(new StackId("HDP-2.0.6.1"));

    when(cluster.getServices()).thenReturn(ImmutableSet.of(hbaseService, zkService));

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph rg = roleGraphFactory.createNew(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1", "", "");
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now), "cluster1", 1L, "core", "HBASE", false, false);
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now), "cluster1", 1L, "core", "ZOOKEEPER", false, false);
    log.info("Build and ready to detect circular dependencies - short chain");
    rg.build(stage);
    boolean exceptionThrown = false;
    try {
      List<Stage> outStages = rg.getStages();
    } catch (AmbariException e) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  @Test
  public void testSCCInGraphDetectedLong() {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6.1"));

    Service hbaseService = mock(Service.class);
    when(hbaseService.getStackId()).thenReturn(new StackId("HDP-2.0.6.1"));
    Service zkService = mock(Service.class);
    when(zkService.getStackId()).thenReturn(new StackId("HDP-2.0.6.1"));
    Service yarnService = mock(Service.class);
    when(yarnService.getStackId()).thenReturn(new StackId("HDP-2.0.6.1"));

    when(cluster.getServices()).thenReturn(ImmutableSet.of(hbaseService, zkService, yarnService));

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph rg = roleGraphFactory.createNew(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1", "", "");
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.STOP, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now), "cluster1", 1L, "core", "HBASE", false, false);
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.STOP, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now), "cluster1", 1L, "core", "ZOOKEEPER", false, false);
    stage.addHostRoleExecutionCommand("host2", Role.RESOURCEMANAGER,
        RoleCommand.STOP, new ServiceComponentHostStartEvent("RESOURCEMANAGER",
            "host4", now), "cluster1", 1L, "core", "YARN", false, false);

    log.info("Build and ready to detect circular dependencies - long chain");
    rg.build(stage);
    boolean exceptionThrown = false;
    try {
      List<Stage> outStages = rg.getStages();
    } catch (AmbariException e) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  @Test
  public void testSCCInGraphDetectedLongTwo() {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6.1"));

    Service hbaseService = mock(Service.class);
    when(hbaseService.getStackId()).thenReturn(new StackId("HDP-2.0.6.1"));
    Service zkService = mock(Service.class);
    when(zkService.getStackId()).thenReturn(new StackId("HDP-2.0.6.1"));

    when(cluster.getServices()).thenReturn(ImmutableSet.of(hbaseService, zkService));

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph rg = roleGraphFactory.createNew(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1", "", "");
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.UPGRADE, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now), "cluster1", 1L, "core", "HBASE", false, false);
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.UPGRADE, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now), "cluster1", 1L, "core", "ZOOKEEPER", false, false);
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_REGIONSERVER,
        RoleCommand.UPGRADE, new ServiceComponentHostStartEvent("HBASE_REGIONSERVER",
            "host4", now), "cluster1", 1L, "core", "HBASE", false, false);

    log.info("Build and ready to detect circular dependencies - long chain");
    rg.build(stage);
    boolean exceptionThrown = false;
    try {
      List<Stage> outStages = rg.getStages();
    } catch (AmbariException e) {
      exceptionThrown = true;
    }
    Assert.assertTrue(exceptionThrown);
  }

  @Test
  public void testNoSCCInGraphDetected() {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));

    Service hbaseService = mock(Service.class);
    when(hbaseService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));
    Service zkService = mock(Service.class);
    when(zkService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    when(cluster.getServices()).thenReturn(ImmutableSet.of(hbaseService, zkService));

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph rg = roleGraphFactory.createNew(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1", "", "");
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.STOP, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now), "cluster1", 1L, "core", "HBASE", false, false);
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.STOP, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now), "cluster1", 1L, "core", "ZOOKEEPER", false, false);
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_REGIONSERVER,
        RoleCommand.STOP, new ServiceComponentHostStartEvent("HBASE_REGIONSERVER",
            "host4", now), "cluster1", 1L, "core", "HBASE", false, false);
    log.info("Build and ready to detect circular dependencies");
    rg.build(stage);
    boolean exceptionThrown = false;
    try {
      List<Stage> outStages = rg.getStages();
    } catch (AmbariException e) {
      exceptionThrown = true;
    }
    Assert.assertFalse(exceptionThrown);
  }

  @Test
  public void testMultiStagePlan() throws Throwable {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));

    Service hbaseService = mock(Service.class);
    when(hbaseService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));
    Service zkService = mock(Service.class);
    when(zkService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    when(cluster.getServices()).thenReturn(ImmutableSet.of(hbaseService, zkService));


    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph rg = roleGraphFactory.createNew(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1", "", "");
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now), "cluster1", 1L, "core", "HBASE", false, false);
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now), "cluster1", 1L, "core", "ZOOKEEPER", false, false);
    log.info(stage.toString());

    rg.build(stage);
    log.info(rg.stringifyGraph());
    List<Stage> outStages = rg.getStages();
    for (Stage s: outStages) {
      log.info(s.toString());
    }
    assertEquals(3, outStages.size());
  }

  @Test
  public void testRestartStagePlan() throws Throwable {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));

    Service hiveService = mock(Service.class);
    when(hiveService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    when(cluster.getServices()).thenReturn(ImmutableSet.of(hiveService));

    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph rg = roleGraphFactory.createNew(rco);

    Stage stage = stageFactory.createNew(1, "/tmp", "cluster1", 1L, "execution command wrapper test",
      "commandParamsStage", "hostParamsStage");
    stage.setStageId(1);
    stage.addServerActionCommand("RESTART", null, Role.HIVE_METASTORE,
      RoleCommand.CUSTOM_COMMAND, "cluster1",
      new ServiceComponentHostServerActionEvent("host2", System.currentTimeMillis()),
      null, "command detail", null, null, false, false);
    stage.addServerActionCommand("RESTART", null, Role.MYSQL_SERVER,
      RoleCommand.CUSTOM_COMMAND, "cluster1",
      new ServiceComponentHostServerActionEvent("host2", System.currentTimeMillis()),
      null, "command detail", null, null, false, false);
    log.info(stage.toString());

    rg.build(stage);
    log.info(rg.stringifyGraph());
    List<Stage> outStages = rg.getStages();
    for (Stage s: outStages) {
      log.info(s.toString());
    }
    assertEquals(2, outStages.size());
  }

  @Test
  public void testManyStages() throws Throwable {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));

    Service hdfsService = mock(Service.class);
    when(hdfsService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    Service hbaseService = mock(Service.class);
    when(hbaseService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    Service zkService = mock(Service.class);
    when(zkService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    Service mrService = mock(Service.class);
    when(mrService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    Service oozieService = mock(Service.class);
    when(oozieService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    Service webhcatService = mock(Service.class);
    when(webhcatService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    Service gangliaService = mock(Service.class);
    when(gangliaService.getStackId()).thenReturn(new StackId("HDP-2.0.6"));

    when(cluster.getServices()).thenReturn(ImmutableSet.of(hdfsService, hbaseService, zkService, mrService, oozieService, webhcatService, gangliaService));


    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph rg = roleGraphFactory.createNew(rco);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1", "", "");
    stage.addHostRoleExecutionCommand("host11", Role.SECONDARY_NAMENODE,
        RoleCommand.START, new ServiceComponentHostStartEvent("SECONDARY_NAMENODE",
            "host11", now), "cluster1", 1L, "core", "HDFS", false, false);
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_MASTER",
            "host2", now), "cluster1", 1L, "core", "HBASE", false, false);
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
            "host3", now), "cluster1", 1L, "core", "ZOOKEEPER", false, false);
    stage.addHostRoleExecutionCommand("host4", Role.DATANODE,
        RoleCommand.START, new ServiceComponentHostStartEvent("DATANODE",
            "host4", now), "cluster1", 1L, "core", "HDFS", false, false);
    stage.addHostRoleExecutionCommand("host4", Role.HBASE_REGIONSERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_REGIONSERVER",
            "host4", now), "cluster1", 1L, "core", "HBASE", false, false);
    stage.addHostRoleExecutionCommand("host4", Role.TASKTRACKER,
        RoleCommand.START, new ServiceComponentHostStartEvent("TASKTRACKER",
            "host4", now), "cluster1", 1L, "core", "MAPREDUCE", false, false);
    stage.addHostRoleExecutionCommand("host5", Role.JOBTRACKER,
        RoleCommand.START, new ServiceComponentHostStartEvent("JOBTRACKER",
            "host5", now), "cluster1", 1L, "core", "MAPREDUCE", false, false);
    stage.addHostRoleExecutionCommand("host6", Role.OOZIE_SERVER,
        RoleCommand.START, new ServiceComponentHostStartEvent("OOZIE_SERVER",
            "host6", now), "cluster1", 1L, "core", "OOZIE", false, false);
    stage.addHostRoleExecutionCommand("host7", Role.WEBHCAT_SERVER,
      RoleCommand.START, new ServiceComponentHostStartEvent("WEBHCAT_SERVER",
        "host7", now), "cluster1", 1L, "core", "WEBHCAT", false, false);
    stage.addHostRoleExecutionCommand("host4", Role.GANGLIA_MONITOR,
      RoleCommand.START, new ServiceComponentHostStartEvent("GANGLIA_MONITOR",
        "host4", now), "cluster1", 1L, "core", "GANGLIA", false, false);
    stage.addHostRoleExecutionCommand("host9", Role.GANGLIA_SERVER,
      RoleCommand.START, new ServiceComponentHostStartEvent("GANGLIA_SERVER",
        "host9", now), "cluster1", 1L, "core", "GANGLIA", false, false);

    log.info(stage.toString());
    rg.build(stage);
    log.info(rg.stringifyGraph());
    List<Stage> outStages = rg.getStages();
    for (Stage s : outStages) {
      log.info(s.toString());
    }
    assertEquals(4, outStages.size());
  }

  @Test
  public void testDependencyOrderedStageCreate() throws Throwable {
    ClusterImpl cluster = mock(ClusterImpl.class);
    when(cluster.getCurrentStackVersion()).thenReturn(new StackId("HDP-2.0.6"));
    RoleCommandOrder rco = roleCommandOrderProvider.getRoleCommandOrder(cluster);
    RoleGraph rg = roleGraphFactory.createNew(rco);
    rg.setCommandExecutionType(CommandExecutionType.DEPENDENCY_ORDERED);
    long now = System.currentTimeMillis();
    Stage stage = StageUtils.getATestStage(1, 1, "host1", "", "");
    stage.addHostRoleExecutionCommand("host11", Role.SECONDARY_NAMENODE,
      RoleCommand.START, new ServiceComponentHostStartEvent("SECONDARY_NAMENODE",
        "host11", now), "cluster1", 1L, "core", "HDFS", false, false);
    stage.addHostRoleExecutionCommand("host2", Role.HBASE_MASTER,
      RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_MASTER",
        "host2", now), "cluster1", 1L, "core", "HBASE", false, false);
    stage.addHostRoleExecutionCommand("host3", Role.ZOOKEEPER_SERVER,
      RoleCommand.START, new ServiceComponentHostStartEvent("ZOOKEEPER_SERVER",
        "host3", now), "cluster1", 1L, "core", "ZOOKEEPER", false, false);
    stage.addHostRoleExecutionCommand("host4", Role.DATANODE,
      RoleCommand.START, new ServiceComponentHostStartEvent("DATANODE",
        "host4", now), "cluster1", 1L, "core", "HDFS", false, false);
    stage.addHostRoleExecutionCommand("host4", Role.HBASE_REGIONSERVER,
      RoleCommand.START, new ServiceComponentHostStartEvent("HBASE_REGIONSERVER",
        "host4", now), "cluster1", 1L, "core", "HBASE", false, false);
    stage.addHostRoleExecutionCommand("host4", Role.TASKTRACKER,
      RoleCommand.START, new ServiceComponentHostStartEvent("TASKTRACKER",
        "host4", now), "cluster1", 1L, "core", "MAPREDUCE", false, false);
    stage.addHostRoleExecutionCommand("host5", Role.JOBTRACKER,
      RoleCommand.START, new ServiceComponentHostStartEvent("JOBTRACKER",
        "host5", now), "cluster1", 1L, "core", "MAPREDUCE", false, false);
    stage.addHostRoleExecutionCommand("host6", Role.OOZIE_SERVER,
      RoleCommand.START, new ServiceComponentHostStartEvent("OOZIE_SERVER",
        "host6", now), "cluster1", 1L, "core", "OOZIE", false, false);
    stage.addHostRoleExecutionCommand("host7", Role.WEBHCAT_SERVER,
      RoleCommand.START, new ServiceComponentHostStartEvent("WEBHCAT_SERVER",
        "host7", now), "cluster1", 1L, "core", "WEBHCAT", false, false);
    stage.addHostRoleExecutionCommand("host4", Role.GANGLIA_MONITOR,
      RoleCommand.START, new ServiceComponentHostStartEvent("GANGLIA_MONITOR",
        "host4", now), "cluster1", 1L, "core", "GANGLIA", false, false);
    stage.addHostRoleExecutionCommand("host9", Role.GANGLIA_SERVER,
      RoleCommand.START, new ServiceComponentHostStartEvent("GANGLIA_SERVER",
        "host9", now), "cluster1", 1L, "core", "GANGLIA", false, false);
    log.info(stage.toString());
    rg.build(stage);
    log.info(rg.stringifyGraph());
    List<Stage> outStages = rg.getStages();
    for (Stage s : outStages) {
      log.info(s.toString());
    }
    assertEquals(1, outStages.size());
  }
}
