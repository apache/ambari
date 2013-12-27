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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class StageUtils {
  

  private static final Log LOG = LogFactory.getLog(StageUtils.class);

  private static Map<String, String> componentToClusterInfoKeyMap =
      new HashMap<String, String>();

  private volatile static Gson gson;
  public static final Integer DEFAULT_PING_PORT = 8670;

  private static final String HOSTS_LIST = "all_hosts";

  private static final String PORTS = "all_ping_ports";

  public static void setGson(Gson gson) {
    if (gson==null) {
      StageUtils.gson = gson;
    }
  }

  public static Gson getGson() {
    if (gson != null) {
      return gson;
    } else {
      synchronized (LOG) {
        if (gson==null) {
          gson = new Gson();
        }
        return gson;
      }
    }
  }


  static {
    componentToClusterInfoKeyMap.put("NAMENODE", "namenode_host");
    componentToClusterInfoKeyMap.put("JOBTRACKER", "jtnode_host");
    componentToClusterInfoKeyMap.put("SECONDARY_NAMENODE", "snamenode_host");
    componentToClusterInfoKeyMap.put("RESOURCEMANAGER", "rm_host");
    componentToClusterInfoKeyMap.put("NODEMANAGER", "nm_hosts");
    componentToClusterInfoKeyMap.put("HISTORYSERVER", "hs_host");
    componentToClusterInfoKeyMap.put("JOURNALNODE", "journalnode_hosts");
    componentToClusterInfoKeyMap.put("ZKFC", "zkfc_hosts");
    componentToClusterInfoKeyMap.put("ZOOKEEPER_SERVER", "zookeeper_hosts");
    componentToClusterInfoKeyMap.put("FLUME_SERVER", "flume_hosts");
    componentToClusterInfoKeyMap.put("HBASE_MASTER", "hbase_master_hosts");
    componentToClusterInfoKeyMap.put("HBASE_REGIONSERVER", "hbase_rs_hosts");
    componentToClusterInfoKeyMap.put("HIVE_SERVER", "hive_server_host");
    componentToClusterInfoKeyMap.put("OOZIE_SERVER", "oozie_server");
    componentToClusterInfoKeyMap.put("WEBHCAT_SERVER",
        "webhcat_server_host");
    componentToClusterInfoKeyMap.put(Role.MYSQL_SERVER.toString(),
        "hive_mysql_host");
    componentToClusterInfoKeyMap.put("DASHBOARD", "dashboard_host");
    componentToClusterInfoKeyMap.put("NAGIOS_SERVER", "nagios_server_host");
    componentToClusterInfoKeyMap.put("GANGLIA_SERVER",
        "ganglia_server_host");
    componentToClusterInfoKeyMap.put("DATANODE", "slave_hosts");
    componentToClusterInfoKeyMap.put("TASKTRACKER", "mapred_tt_hosts");
    componentToClusterInfoKeyMap.put("HBASE_REGIONSERVER", "hbase_rs_hosts");
    componentToClusterInfoKeyMap.put("KERBEROS_SERVER", "kdc_host");
    componentToClusterInfoKeyMap.put("KERBEROS_ADMIN_CLIENT",
        "kerberos_adminclient_host");
  }

  public static String getActionId(long requestId, long stageId) {
    return requestId + "-" + stageId;
  }

  public static long[] getRequestStage(String actionId) {
    String[] fields = actionId.split("-");
    long[] requestStageIds = new long[2];
    requestStageIds[0] = Long.parseLong(fields[0]);
    requestStageIds[1] = Long.parseLong(fields[1]);
    return requestStageIds;
  }

  public static Stage getATestStage(long requestId, long stageId, String clusterHostInfo) {
    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      hostname = "host-dummy";
    }
    return getATestStage(requestId, stageId, hostname, clusterHostInfo);
  }

  //For testing only
  public static Stage getATestStage(long requestId, long stageId, String hostname, String clusterHostInfo) {
    
    Stage s = new Stage(requestId, "/tmp", "cluster1", "context", clusterHostInfo);
    s.setStageId(stageId);
    long now = System.currentTimeMillis();
    s.addHostRoleExecutionCommand(hostname, Role.NAMENODE, RoleCommand.INSTALL,
        new ServiceComponentHostInstallEvent("NAMENODE", hostname, now, "HDP-1.2.0"),
        "cluster1", "HDFS");
    ExecutionCommand execCmd = s.getExecutionCommandWrapper(hostname, "NAMENODE").getExecutionCommand();
    execCmd.setCommandId(s.getActionId());
    List<String> slaveHostList = new ArrayList<String>();
    slaveHostList.add(hostname);
    slaveHostList.add("host2");
    Map<String, String> hdfsSite = new TreeMap<String, String>();
    hdfsSite.put("dfs.block.size", "2560000000");
    Map<String, Map<String, String>> configurations =
        new TreeMap<String, Map<String, String>>();
    configurations.put("hdfs-site", hdfsSite);
    execCmd.setConfigurations(configurations);
    Map<String, String> params = new TreeMap<String, String>();
    params.put("jdklocation", "/x/y/z");
    params.put("stack_version", "1.2.0");
    params.put("stack_name", "HDP");
    execCmd.setHostLevelParams(params);
    Map<String, String> roleParams = new TreeMap<String, String>();
    roleParams.put("format", "false");
    execCmd.setRoleParams(roleParams);
    return s;
  }

  public static String jaxbToString(Object jaxbObj) throws JAXBException,
  JsonGenerationException, JsonMappingException, IOException {
    return getGson().toJson(jaxbObj);
  }

  public static ExecutionCommand stringToExecutionCommand(String json)
      throws JsonParseException, JsonMappingException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
    InputStream is = new ByteArrayInputStream(json.getBytes(Charset.forName("UTF8")));
    return mapper.readValue(is, ExecutionCommand.class);
  }

  public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
    InputStream is = new ByteArrayInputStream(json.getBytes(Charset.forName("UTF8")));
    return mapper.readValue(is, clazz);
  }


  public static Map<String, Set<String>> getClusterHostInfo(
      Map<String, Host> allHosts, Cluster cluster, HostsMap hostsMap,
      Configuration configuration) throws AmbariException {

    Map<String, SortedSet<Integer>> hostRolesInfo = new HashMap<String, SortedSet<Integer>>();
    
    Map<String, Set<String>> clusterHostInfo = new HashMap<String, Set<String>>();

    //Fill hosts and ports lists
    Set<String> hostsSet = new LinkedHashSet<String>();
    List<Integer> portsList = new ArrayList<Integer>();
    
    for (Host host : allHosts.values()) {
      
      Integer currentPingPort = host.getCurrentPingPort() == null ?
          DEFAULT_PING_PORT : host.getCurrentPingPort();
      
      hostsSet.add(host.getHostName());
      portsList.add(currentPingPort);
    }
    
    List<String> hostsList = new ArrayList<String>(hostsSet);
    
    //Fill host roles
    for (Entry<String, Service> serviceEntry : cluster.getServices().entrySet()) { 
      
      Service service = serviceEntry.getValue();
      
      for (Entry<String, ServiceComponent> serviceComponentEntry : service.getServiceComponents().entrySet()) {
        
        ServiceComponent serviceComponent = serviceComponentEntry.getValue();
        String componentName = serviceComponent.getName();

        for (final String hostName:serviceComponent.getServiceComponentHosts().keySet()) {

          if (componentToClusterInfoKeyMap.containsKey(componentName)) {
            
            String roleName = componentToClusterInfoKeyMap.get(componentName);
            SortedSet<Integer> hostsForComponentsHost = hostRolesInfo.get(roleName);
            
            if (hostsForComponentsHost == null) {
              hostsForComponentsHost = new TreeSet<Integer>();
              hostRolesInfo.put(roleName, hostsForComponentsHost);
            }

            int hostIndex = hostsList.indexOf(hostName);
            //Add index of host to current host role
            hostsForComponentsHost.add(hostIndex);
          }
          else
            LOG.warn("Component " + componentName + " doesn't have mapped role name for cluster host info");
        }
      }
    }
    
    for (String roleName : componentToClusterInfoKeyMap.values()) {
      if (hostRolesInfo.containsKey(roleName)) {

        TreeSet<Integer> sortedSet =
            new TreeSet<Integer>(hostRolesInfo.get(roleName));

        Set<String> replacedRangesSet = replaceRanges(sortedSet);

        clusterHostInfo.put(roleName, replacedRangesSet);

      }
    }

    clusterHostInfo.put(HOSTS_LIST, hostsSet);
    clusterHostInfo.put(PORTS, replaceMappedRanges(portsList));
    
    return clusterHostInfo;
  }
  
  
  /**
   * Finds ranges in sorted set and replaces ranges by compact notation
   * 
   * <p>For example, suppose <tt>set</tt> comprises<tt> [1, 2, 3, 4, 7]</tt>.
   * After invoking <tt>rangedSet = StageUtils.replaceRanges(set)</tt> 
   * <tt>rangedSet</tt> will comprise
   * <tt>["1-4", "7"]</tt>..
   *
   * @param  set  the source set to be ranged
   */
  public static Set<String> replaceRanges(SortedSet<Integer> set) {
    
    if (set == null)
      return null;
    
    Set<String> rangedSet = new HashSet<String>();
    
    Integer prevElement = null;
    Integer startOfRange = set.first();
    
    for (Integer i : set) {
      if (prevElement != null && (i - prevElement) > 1 ) {
        String rangeItem = getRangedItem(startOfRange, prevElement);
        rangedSet.add(rangeItem);
        startOfRange = i;
      }
      prevElement = i;
    }
    
    rangedSet.add(getRangedItem(startOfRange, prevElement));
    
    return rangedSet;
  }
  
  /**
   * Finds ranges in list and replaces ranges by compact notation
   * 
   * <p>For example, suppose <tt>list</tt> comprises<tt> [1, 1, 2, 2, 1, 3]</tt>.
   * After invoking <tt>rangedMappedSet = StageUtils.replaceMappedRanges(list)</tt> 
   * <tt>rangedMappedSet</tt> will comprise
   * <tt>["1:0-1,4", "2:2-3", "3:5"]</tt>..
   *
   * @param  list  the source list to be ranged
   */
  public static Set<String> replaceMappedRanges(List<Integer> values) {

    Map<Integer, SortedSet<Integer>> convolutedValues = new HashMap<Integer, SortedSet<Integer>>();

    int valueIndex = 0;

    for (Integer value : values) {

      SortedSet<Integer> correspValues = convolutedValues.get(value);

      if (correspValues == null) {
        correspValues = new TreeSet<Integer>();
        convolutedValues.put(value, correspValues);
      }
      correspValues.add(valueIndex);
      valueIndex++;
    }

    Set<String> result = new HashSet<String>();

    for (Entry<Integer, SortedSet<Integer>> entry : convolutedValues.entrySet()) {
      Set<String> replacedRanges = replaceRanges(entry.getValue());
      result.add(entry.getKey() + ":" + Joiner.on(",").join(replacedRanges));
    }

    return result;
  }

  private static String getRangedItem(Integer startOfRange, Integer endOfRange) {
    
    String separator = (endOfRange - startOfRange) > 1 ? "-" : ",";
    
    String rangeItem = endOfRange.equals(startOfRange) ? 
        endOfRange.toString() :
          startOfRange + separator + endOfRange;
    return rangeItem;
  }

  public static String getHostName() {
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      LOG.warn("Could not find canonical hostname ", e);
      return "localhost";
    }
  }
}
