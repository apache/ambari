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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestStageUtils {
  private static Log LOG = LogFactory.getLog(TestStageUtils.class);

  private AmbariMetaInfo ambariMetaInfo;

  private Injector injector;

//  ServiceComponentFactory serviceComponentFactory;
  static ServiceComponentHostFactory serviceComponentHostFactory;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    serviceComponentHostFactory = injector.getInstance(ServiceComponentHostFactory.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

  }


  public static void addHdfsService(Cluster cl, String [] hostList,
      Injector injector) throws AmbariException {
    cl.setDesiredStackVersion(new StackId("HDP-0.1"));
    cl.addService("HDFS");
    cl.getService("HDFS").addServiceComponent("NAMENODE");
    cl.getService("HDFS").addServiceComponent("DATANODE");
    cl.getService("HDFS").addServiceComponent("SECONDARY_NAMENODE");
    cl.getService("HDFS")
        .getServiceComponent("NAMENODE")
        .addServiceComponentHost(
            serviceComponentHostFactory.createNew(cl.getService("HDFS")
                .getServiceComponent("NAMENODE"), hostList[0], false));
    cl.getService("HDFS")
        .getServiceComponent("SECONDARY_NAMENODE")
        .addServiceComponentHost(
            serviceComponentHostFactory.createNew(cl.getService("HDFS")
                .getServiceComponent("SECONDARY_NAMENODE"), hostList[1], false)
        );
    for (int i = 1; i < hostList.length; i++) {
      cl.getService("HDFS")
          .getServiceComponent("DATANODE")
          .addServiceComponentHost(serviceComponentHostFactory.createNew(cl.getService("HDFS")
              .getServiceComponent("DATANODE"), hostList[i], false));
    }
  }

  public static void addHbaseService(Cluster cl, String [] hostList,
      Injector injector) throws AmbariException {
    cl.setDesiredStackVersion(new StackId("HDP-0.2"));
    cl.addService("HBASE");
    cl.getService("HBASE").addServiceComponent("HBASE_MASTER");
    cl.getService("HBASE").addServiceComponent("HBASE_REGIONSERVER");
    cl.getService("HBASE")
        .getServiceComponent("HBASE_MASTER")
        .addServiceComponentHost(
            serviceComponentHostFactory.createNew(cl.getService("HBASE")
                .getServiceComponent("HBASE_MASTER"), hostList[0], false));
    for (int i = 1; i < hostList.length; i++) {
      cl.getService("HBASE")
          .getServiceComponent("HBASE_REGIONSERVER")
          .addServiceComponentHost(
              serviceComponentHostFactory.createNew(cl.getService("HBASE")
                  .getServiceComponent("HBASE_REGIONSERVER"), hostList[i],
                  false));
    }
  }

  public static void addMapreduceService(Cluster cl, String [] hostList,
                                     Injector injector) throws AmbariException {
    cl.setDesiredStackVersion(new StackId("HDP-0.2"));
    cl.addService("MAPREDUCE");
    cl.getService("MAPREDUCE").addServiceComponent("JOBTRACKER");
    cl.getService("MAPREDUCE").addServiceComponent("TASKTRACKER");
    cl.getService("MAPREDUCE")
        .getServiceComponent("JOBTRACKER")
        .addServiceComponentHost(
            serviceComponentHostFactory.createNew(cl.getService("MAPREDUCE")
                .getServiceComponent("JOBTRACKER"), hostList[0], false));
    for (int i = 1; i < hostList.length; i++) {
      cl.getService("MAPREDUCE")
          .getServiceComponent("TASKTRACKER")
          .addServiceComponentHost(
              serviceComponentHostFactory.createNew(cl.getService("MAPREDUCE")
                  .getServiceComponent("TASKTRACKER"), hostList[i],
                  false));
    }
  }

  @Test
  public void testGetATestStage() {
    Stage s = StageUtils.getATestStage(1, 2, "host2");
    String hostname = s.getHosts().get(0);
    List<ExecutionCommandWrapper> wrappers = s.getExecutionCommands(hostname);
    for (ExecutionCommandWrapper wrapper : wrappers) {
      assertEquals("cluster1", wrapper.getExecutionCommand().getClusterName());
      assertEquals(StageUtils.getActionId(1, 2), wrapper.getExecutionCommand().getCommandId());
      assertEquals(hostname, wrapper.getExecutionCommand().getHostname());
    }
  }

  @Test
  public void testJaxbToString() throws Exception {
    Stage s = StageUtils.getATestStage(1, 2, "host1");
    String hostname = s.getHosts().get(0);
    List<ExecutionCommandWrapper> wrappers = s.getExecutionCommands(hostname);
    for (ExecutionCommandWrapper wrapper : wrappers) {
      LOG.info("Command is " + StageUtils.jaxbToString(wrapper.getExecutionCommand()));
    }
    assertEquals(StageUtils.getActionId(1, 2), s.getActionId());
  }

  @Test
  public void testJasonToExecutionCommand() throws JsonGenerationException,
      JsonMappingException, JAXBException, IOException {
    Stage s = StageUtils.getATestStage(1, 2, "host1", "clusterHostInfo");
    ExecutionCommand cmd = s.getExecutionCommands("host1").get(0).getExecutionCommand();    
    cmd.setConfigurationTags(new HashMap<String, Map<String,String>>() {{
      put("global", new HashMap<String, String>() {{ put("tag", "version1"); }});
    }});
    
    
    String json = StageUtils.jaxbToString(cmd);
    ExecutionCommand cmdDes = StageUtils.stringToExecutionCommand(json);
    assertEquals(cmd.toString(), cmdDes.toString());
    assertEquals(cmd, cmdDes);
  }

  @Test
  public void testGetClusterHostInfo() throws AmbariException, UnknownHostException {
    Clusters fsm = injector.getInstance(Clusters.class);
    fsm.addCluster("c1");
    fsm.addHost("h1");
    fsm.addHost("h2");
    fsm.addHost("h3");
    fsm.addHost("h4");
    fsm.getCluster("c1").setDesiredStackVersion(new StackId("HDP-0.1"));
    fsm.getHost("h1").setOsType("centos5");
    fsm.getHost("h2").setOsType("centos5");
    fsm.getHost("h3").setOsType("centos5");
    fsm.getHost("h4").setOsType("centos5");
    fsm.getHost("h1").setCurrentPingPort(8670);
    fsm.getHost("h2").setCurrentPingPort(null);
    fsm.getHost("h3").setCurrentPingPort(null);
    fsm.getHost("h4").setCurrentPingPort(8670);
    fsm.getHost("h1").persist();
    fsm.getHost("h2").persist();
    fsm.getHost("h3").persist();
    fsm.getHost("h4").persist();
    fsm.mapHostToCluster("h1", "c1");
    fsm.mapHostToCluster("h2", "c1");
    fsm.mapHostToCluster("h3", "c1");
    fsm.mapHostToCluster("h4", "c1");
    String [] hostList = {"h1", "h2", "h3" };
    addHdfsService(fsm.getCluster("c1"), hostList, injector);
    addHbaseService(fsm.getCluster("c1"), hostList, injector);
    addMapreduceService(fsm.getCluster("c1"), hostList, injector);
    Map<String, List<String>> info = StageUtils.getClusterHostInfo(fsm.getHostsForCluster("c1"),
        fsm.getCluster("c1"), new HostsMap(injector.getInstance(Configuration.class)),
        injector.getInstance(Configuration.class));
    assertEquals(2, info.get("slave_hosts").size());
    assertEquals(2, info.get("mapred_tt_hosts").size());
    assertEquals(2, info.get("hbase_rs_hosts").size());
    assertEquals(1, info.get("hbase_master_hosts").size());
    assertEquals(4, info.get("all_hosts").size());
    assertEquals(4, info.get("all_ping_ports").size());
    assertEquals("h1", info.get("hbase_master_hosts").get(0));
    assertEquals("8670", info.get("all_ping_ports").get(0));
    assertEquals("8670", info.get("all_ping_ports").get(1));
    assertEquals("8670", info.get("all_ping_ports").get(2));
    assertEquals("8670", info.get("all_ping_ports").get(3));

    assertFalse(info.get("ambari_db_rca_url").get(0).contains(Configuration.HOSTNAME_MACRO));
    String address = InetAddress.getLocalHost().getCanonicalHostName();
    assertTrue(info.get("ambari_db_rca_url").get(0).contains(address));

  }
}
