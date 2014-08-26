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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.mockStaticPartial;
import static org.powermock.api.easymock.PowerMock.replayAll;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.ExecutionCommandWrapper;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.inject.Guice;
import com.google.inject.Injector;

@RunWith(PowerMockRunner.class)
@PrepareForTest(StageUtils.class)
@PowerMockIgnore("javax.management.*")
public class TestStageUtils {
  private static final String HOSTS_LIST = "all_hosts";

  private static final String STACK_ID = "HDP-1.3.1";

  private static Log LOG = LogFactory.getLog(TestStageUtils.class);

  private AmbariMetaInfo ambariMetaInfo;

  private Injector injector;

  static ServiceComponentHostFactory serviceComponentHostFactory;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    serviceComponentHostFactory = injector.getInstance(ServiceComponentHostFactory.class);
    ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

  }


  public static void addService(Cluster cl, List<String> hostList,
       Map<String, List<Integer>> topology, String serviceName,
       Injector injector) throws AmbariException {
    cl.setDesiredStackVersion(new StackId(STACK_ID));
    cl.addService(serviceName);
    
    for (Entry<String, List<Integer>> component : topology.entrySet()) {
      
      String componentName = component.getKey();
      cl.getService(serviceName).addServiceComponent(componentName);
      
      for (Integer hostIndex : component.getValue()) {
        cl.getService(serviceName)
        .getServiceComponent(componentName)
        .addServiceComponentHost(
            serviceComponentHostFactory.createNew(cl.getService(serviceName)
                .getServiceComponent(componentName), hostList.get(hostIndex)));
      }
    }
  }

  @Test
  @Ignore
  public void testGetATestStage() {
    Stage s = StageUtils.getATestStage(1, 2, "host2", "", "hostParamsStage");
    String hostname = s.getHosts().get(0);
    List<ExecutionCommandWrapper> wrappers = s.getExecutionCommands(hostname);
    for (ExecutionCommandWrapper wrapper : wrappers) {
      assertEquals("cluster1", wrapper.getExecutionCommand().getClusterName());
      assertEquals(StageUtils.getActionId(1, 2), wrapper.getExecutionCommand().getCommandId());
      assertEquals(hostname, wrapper.getExecutionCommand().getHostname());
    }
  }

  @Test
  @Ignore
  public void testJaxbToString() throws Exception {
    Stage s = StageUtils.getATestStage(1, 2, "host1", "", "hostParamsStage");
    String hostname = s.getHosts().get(0);
    List<ExecutionCommandWrapper> wrappers = s.getExecutionCommands(hostname);
    for (ExecutionCommandWrapper wrapper : wrappers) {
      LOG.info("Command is " + StageUtils.jaxbToString(wrapper.getExecutionCommand()));
    }
    assertEquals(StageUtils.getActionId(1, 2), s.getActionId());
  }

  @Test
  @Ignore
  public void testJasonToExecutionCommand() throws JsonGenerationException,
      JsonMappingException, JAXBException, IOException {
    Stage s = StageUtils.getATestStage(1, 2, "host1", "clusterHostInfo", "hostParamsStage");
    ExecutionCommand cmd = s.getExecutionCommands("host1").get(0).getExecutionCommand();    
    HashMap<String, Map<String,String>> configTags = new HashMap<String, Map<String,String>>();
    Map<String, String> globalTag = new HashMap<String, String>();
    globalTag.put("tag", "version1");
    configTags.put("global", globalTag );
    cmd.setConfigurationTags(configTags);
    String json = StageUtils.jaxbToString(cmd);
    ExecutionCommand cmdDes = StageUtils.stringToExecutionCommand(json);
    assertEquals(cmd.toString(), cmdDes.toString());
    assertEquals(cmd, cmdDes);
  }

  @Test
  @Ignore
  public void testGetClusterHostInfo() throws AmbariException, UnknownHostException {
    Clusters fsm = injector.getInstance(Clusters.class);
    String h0 = "h0";

    List<String> hostList = new ArrayList<String>();
    hostList.add("h1");
    hostList.add("h2");
    hostList.add("h3");
    hostList.add("h4");
    hostList.add("h5");
    hostList.add("h6");
    hostList.add("h7");
    hostList.add("h8");
    hostList.add("h9");
    hostList.add("h10");

    mockStaticPartial(StageUtils.class, "getHostName");
    expect(StageUtils.getHostName()).andReturn(h0).anyTimes();
    replayAll();

    List<Integer> pingPorts = Arrays.asList(StageUtils.DEFAULT_PING_PORT,
        StageUtils.DEFAULT_PING_PORT,
        StageUtils.DEFAULT_PING_PORT,
        8671,
        8671,
        null,
        8672,
        8672,
        null,
        8673);
    
    fsm.addCluster("c1");
    fsm.getCluster("c1").setDesiredStackVersion(new StackId(STACK_ID));
    
    int index = 0;
    
    for (String host: hostList) {
      fsm.addHost(host);
      
      Map<String, String> hostAttributes = new HashMap<String, String>();
      hostAttributes.put("os_family", "redhat");
      hostAttributes.put("os_release_version", "5.9");
      fsm.getHost(host).setHostAttributes(hostAttributes);
      
      fsm.getHost(host).setCurrentPingPort(pingPorts.get(index));
      fsm.getHost(host).persist();
      fsm.mapHostToCluster(host, "c1");
      index++;
    }

    //Add HDFS service
    Map<String, List<Integer>> hdfsTopology = new HashMap<String, List<Integer>>();
    hdfsTopology.put("NAMENODE", Collections.singletonList(0));
    hdfsTopology.put("SECONDARY_NAMENODE", Collections.singletonList(1));
    List<Integer> datanodeIndexes = Arrays.asList(0,1,2,3,5,7,8,9);
    hdfsTopology.put("DATANODE", new ArrayList<Integer>(datanodeIndexes));
    addService(fsm.getCluster("c1"), hostList, hdfsTopology , "HDFS", injector);
    
    //Add HBASE service
    Map<String, List<Integer>> hbaseTopology = new HashMap<String, List<Integer>>(); 
    hbaseTopology.put("HBASE_MASTER", Collections.singletonList(5));
    List<Integer> regionServiceIndexes = Arrays.asList(1,3,5,8,9);
    hbaseTopology.put("HBASE_REGIONSERVER", regionServiceIndexes);
    addService(fsm.getCluster("c1"), hostList, hbaseTopology , "HBASE", injector);
    
    //Add MAPREDUCE service
    Map<String, List<Integer>> mrTopology = new HashMap<String, List<Integer>>(); 
    mrTopology.put("JOBTRACKER", Collections.singletonList(5));
    List<Integer> taskTrackerIndexes = Arrays.asList(1,2,3,4,5,7,9);
    mrTopology.put("TASKTRACKER", taskTrackerIndexes);
    addService(fsm.getCluster("c1"), hostList, mrTopology , "MAPREDUCE", injector);
    
    
    //Add NONAME service
    Map<String, List<Integer>> nonameTopology = new HashMap<String, List<Integer>>(); 
    nonameTopology.put("NONAME_SERVER", Collections.singletonList(7));
    addService(fsm.getCluster("c1"), hostList, nonameTopology , "NONAME", injector);

    fsm.getCluster("c1").getService("MAPREDUCE").getServiceComponent("TASKTRACKER").getServiceComponentHost("h2")
        .setComponentAdminState(HostComponentAdminState.DECOMMISSIONED);
    fsm.getCluster("c1").getService("MAPREDUCE").getServiceComponent("TASKTRACKER").getServiceComponentHost("h3")
        .setComponentAdminState(HostComponentAdminState.DECOMMISSIONED);

    //Get cluster host info
    Map<String, Set<String>> info =
        StageUtils.getClusterHostInfo(fsm.getHostsForCluster("c1"), fsm.getCluster("c1"));

    //All hosts present in cluster host info
    Set<String> allHosts = info.get(HOSTS_LIST);
    ArrayList<String> allHostsList = new ArrayList<String>(allHosts);
    assertEquals(fsm.getHosts().size(), allHosts.size());
    for (Host host: fsm.getHosts()) {
      assertTrue(allHosts.contains(host.getHostName()));
    }
    
    
    //Check HDFS topology compression
    Map<String, String> hdfsMapping = new HashMap<String, String>();
    hdfsMapping.put("DATANODE", "slave_hosts");
    hdfsMapping.put("NAMENODE", "namenode_host");
    hdfsMapping.put("SECONDARY_NAMENODE", "snamenode_host");
    checkServiceCompression(info, hdfsMapping, hdfsTopology, hostList);
    
    
    //Check HBASE topology compression
    Map<String, String> hbaseMapping = new HashMap<String, String>();
    hbaseMapping.put("HBASE_MASTER", "hbase_master_hosts");
    hbaseMapping.put("HBASE_REGIONSERVER", "hbase_rs_hosts");
    checkServiceCompression(info, hbaseMapping, hbaseTopology, hostList);
    
    //Check MAPREDUCE topology compression
    Map<String, String> mrMapping = new HashMap<String, String>();
    mrMapping.put("JOBTRACKER", "jtnode_host");
    mrMapping.put("TASKTRACKER", "mapred_tt_hosts");
    checkServiceCompression(info, mrMapping, mrTopology, hostList);
    
    Set<String> actualPingPorts = info.get("all_ping_ports");
    
    if (pingPorts.contains(null))
      assertEquals(new HashSet<Integer>(pingPorts).size(), actualPingPorts.size() + 1);
    else
      assertEquals(new HashSet<Integer>(pingPorts).size(), actualPingPorts.size());
    
    List<Integer> pingPortsActual = getRangeMappedDecompressedSet(actualPingPorts);

    List<Integer> reindexedPorts = getReindexedList(pingPortsActual, new ArrayList<String>(allHosts), hostList);
    
    //Treat null values
    while (pingPorts.contains(null)) {
      int indexOfNull = pingPorts.indexOf(null);
      pingPorts.set(indexOfNull, StageUtils.DEFAULT_PING_PORT);
    }

    assertEquals(pingPorts, reindexedPorts);
    
    // check for no-name in the list
    assertTrue(info.containsKey("noname_server_hosts"));
    assertTrue(info.containsKey("decom_tt_hosts"));
    Set<String> decommissionedHosts = info.get("decom_tt_hosts");
    assertEquals(2, decommissionedHosts.toString().split(",").length);

    // check server hostname field
    assertTrue(info.containsKey(StageUtils.AMBARI_SERVER_HOST));
    Set<String> serverHost = info.get(StageUtils.AMBARI_SERVER_HOST);
    assertEquals(1, serverHost.size());
    assertEquals(h0, serverHost.iterator().next());
  }

  private void checkServiceCompression(Map<String, Set<String>> info,
      Map<String, String> serviceMapping, Map<String, List<Integer>> serviceTopology,
      List<String> hostList) {
    
    
    for (Entry<String, List<Integer>> component: serviceTopology.entrySet()) {
      
      String componentName = component.getKey();
      
      List<Integer> componentIndexesExpected = component.getValue();
      
      String roleName = serviceMapping.get(componentName);
      
      assertTrue("No mapping for " + componentName , roleName != null);
      
      Set<Integer> componentIndexesActual = getDecompressedSet(info.get(roleName));
      
      Set<String> expectedComponentHosts = new HashSet<String>();
      
      for (Integer i: componentIndexesExpected)
        expectedComponentHosts.add(hostList.get(i));
      
      Set<String> actualSlavesHosts = new HashSet<String>();
      
      for (Integer i: componentIndexesActual)
        actualSlavesHosts.add(new ArrayList<String>(info.get(HOSTS_LIST)).get(i));
        
      
      
      assertEquals(expectedComponentHosts, actualSlavesHosts);
    
    }
    
  }

  private Set<Integer> getDecompressedSet(Set<String> set) {

    Set<Integer> resultSet = new HashSet<Integer>();

    for (String index : set) {

      String[] ranges = index.split(",");

      for (String r : ranges) {

        String[] split = r.split("-");

        if (split.length == 2) {
          Integer start = Integer.valueOf(split[0]);
          Integer end = Integer.valueOf(split[1]);
          ContiguousSet<Integer> rangeSet =
          ContiguousSet.create(Range.closed(start, end), DiscreteDomain.integers()) ;

          for (Integer i : rangeSet) {
            resultSet.add(i);

          }

        } else {
          resultSet.add(Integer.valueOf(split[0]));
        }
      }

    }
    return resultSet;
  }
  
  private List<Integer> getRangeMappedDecompressedSet(Set<String> compressedSet) {

    SortedMap<Integer, Integer> resultMap = new TreeMap<Integer, Integer>();

    for (String token : compressedSet) {

      String[] split = token.split(":");

      if (split.length != 2)
        throw new RuntimeException("Broken data, expected format - m:r, got - "
            + token);

      Integer index = Integer.valueOf(split[0]);

      String rangeTokens = split[1];

      Set<String> rangeTokensSet =
          new HashSet<String>(Arrays.asList(rangeTokens.split(",")));

      Set<Integer> decompressedSet = getDecompressedSet(rangeTokensSet);

      for (Integer i : decompressedSet)
        resultMap.put(i, index);

    }

    List<Integer> resultList = new ArrayList<Integer>(resultMap.values());

    return resultList;

  }
  
  private List<Integer> getReindexedList(List<Integer> list,
      List<String> currentIndexes, List<String> desiredIndexes) {

    SortedMap<Integer, Integer> sortedMap = new TreeMap<Integer, Integer>();

    int index = 0;

    for (Integer value : list) {
      String currentIndexValue = currentIndexes.get(index);
      Integer desiredIndexValue = desiredIndexes.indexOf(currentIndexValue);
      sortedMap.put(desiredIndexValue, value);
      index++;
    }

    return new ArrayList<Integer>(sortedMap.values());
  }

}
