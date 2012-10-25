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
package org.apache.ambari.server.utils;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestStageUtils {
  private static Log LOG = LogFactory.getLog(TestStageUtils.class);
  
  public static void addHdfsService(Cluster cl, String [] hostList,
      Injector injector) throws AmbariException {
    cl.addService("HDFS");
    cl.getService("HDFS").addServiceComponent("NAMENODE");
    cl.getService("HDFS").addServiceComponent("DATANODE");
    cl.getService("HDFS").addServiceComponent("SNAMENODE");
    cl.getService("HDFS")
        .getServiceComponent("NAMENODE")
        .addServiceComponentHost(
            new ServiceComponentHostImpl(cl.getService("HDFS")
                .getServiceComponent("NAMENODE"), hostList[0], false,
                injector));
    cl.getService("HDFS")
        .getServiceComponent("SNAMENODE")
        .addServiceComponentHost(
            new ServiceComponentHostImpl(cl.getService("HDFS")
                .getServiceComponent("SNAMENODE"), hostList[1], false,
                injector));
    for (int i = 1; i < hostList.length; i++) {
      cl.getService("HDFS")
          .getServiceComponent("DATANODE")
          .addServiceComponentHost(
              new ServiceComponentHostImpl(cl.getService("HDFS")
                  .getServiceComponent("DATANODE"), hostList[i], false,
                  injector));
    }
  }
  
  public static void addHbaseService(Cluster cl, String [] hostList,
      Injector injector) throws AmbariException {
    cl.addService("HBASE");
    cl.getService("HBASE").addServiceComponent("HBASE_MASTER");
    cl.getService("HBASE").addServiceComponent("HBASE_REGIONSERVER");
    cl.getService("HBASE")
        .getServiceComponent("HBASE_MASTER")
        .addServiceComponentHost(
            new ServiceComponentHostImpl(cl.getService("HBASE")
                .getServiceComponent("HBASE_MASTER"), hostList[0], false,
                injector));
    for (int i = 1; i < hostList.length; i++) {
      cl.getService("HBASE")
          .getServiceComponent("HBASE_REGIONSERVER")
          .addServiceComponentHost(
              new ServiceComponentHostImpl(cl.getService("HBASE")
                  .getServiceComponent("HBASE_REGIONSERVER"), hostList[i], false,
                  injector));
    }
  }
  
  @Test
  public void testGetATestStage() {
    Stage s = StageUtils.getATestStage(1, 2, "host2");
    String hostname = s.getHosts().get(0);
    List<ExecutionCommand> cmds = s.getExecutionCommands(hostname);
    for (ExecutionCommand cmd : cmds) {
      assertEquals("cluster1", cmd.getClusterName());
      assertEquals(StageUtils.getActionId(1, 2), cmd.getCommandId());
      assertEquals(hostname, cmd.getHostname());
    }
  }
  
  @Test
  public void testJaxbToString() throws Exception {
    Stage s = StageUtils.getATestStage(1, 2, "host1");
    String hostname = s.getHosts().get(0);
    List<ExecutionCommand> cmds = s.getExecutionCommands(hostname);
    for (ExecutionCommand cmd: cmds) {
      LOG.info("Command is " + StageUtils.jaxbToString(cmd));
    }
    assertEquals(StageUtils.getActionId(1, 2), s.getActionId());
  }

  @Test
  public void testJasonToExecutionCommand() throws JsonGenerationException,
      JsonMappingException, JAXBException, IOException {
    Stage s = StageUtils.getATestStage(1, 2, "host1");
    ExecutionCommand cmd = s.getExecutionCommands("host1").get(0);
    String json = StageUtils.jaxbToString(cmd);
    ExecutionCommand cmdDes = StageUtils.stringToExecutionCommand(json);
    assertEquals(cmd.toString(), cmdDes.toString());
    assertEquals(cmd, cmdDes);
  }
  
  @Test
  public void testGetClusterHostInfo() throws AmbariException {
    Injector injector;
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    Clusters fsm = new ClustersImpl(injector);
    fsm.addCluster("c1");
    fsm.addHost("h1");
    fsm.addHost("h2");
    fsm.addHost("h3");
    fsm.mapHostToCluster("h1", "c1");
    fsm.mapHostToCluster("h2", "c1");
    fsm.mapHostToCluster("h3", "c1");
    String [] hostList = {"h1", "h2", "h3" };
    fsm.getHost("h1").persist();
    fsm.getHost("h2").persist();
    fsm.getHost("h3").persist();    
    addHdfsService(fsm.getCluster("c1"), hostList, injector);
    addHbaseService(fsm.getCluster("c1"), hostList, injector);
    Map<String, List<String>> info = StageUtils.getClusterHostInfo(fsm
        .getCluster("c1"));
    assertEquals(2, info.get("slave_hosts").size());
    assertEquals(1, info.get("hbase_master_host").size());
    assertEquals("h1", info.get("hbase_master_host").get(0));
  }
}
