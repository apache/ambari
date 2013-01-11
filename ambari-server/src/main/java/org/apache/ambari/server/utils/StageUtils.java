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

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;

public class StageUtils {
  private static Log LOG = LogFactory.getLog(StageUtils.class);
  
  private static Map<String, String> componentToClusterInfoKeyMap = 
      new HashMap<String, String>();
  
  static {
    componentToClusterInfoKeyMap.put("NAMENODE", "namenode_host");
    componentToClusterInfoKeyMap.put("JOBTRACKER", "jtnode_host");
    componentToClusterInfoKeyMap.put("SNAMENODE", "snamenode_host");
    componentToClusterInfoKeyMap.put("ZOOKEEPER_SERVER", "zookeeper_hosts");
    componentToClusterInfoKeyMap.put("HBASE_MASTER", "hbase_master_host");
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
    componentToClusterInfoKeyMap.put("TASKTRACKER", "slave_hosts");
    componentToClusterInfoKeyMap.put("HBASE_REGIONSERVER", "hbase_rs_hosts");
    componentToClusterInfoKeyMap.put("KERBEROS_SERVER", "kdc_host");
    componentToClusterInfoKeyMap.put("KERBEROS_ADMIN_CLIENT",
        "kerberos_adminclient_host");
  }
  
  public static String getActionId(long requestId, long stageId) {
    return requestId + "-" + stageId;
  }

  public static long[] getRequestStage(String actionId) {
    String [] fields = actionId.split("-");
    long[] requestStageIds = new long[2];
    requestStageIds[0] = Long.parseLong(fields[0]);
    requestStageIds[1] = Long.parseLong(fields[1]);
    return requestStageIds;
  }

  public static Stage getATestStage(long requestId, long stageId) {
    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      hostname = "host-dummy";
    }
    return getATestStage(requestId, stageId, hostname);
  }
  
  //For testing only
  public static Stage getATestStage(long requestId, long stageId, String hostname) {
    Stage s = new Stage(requestId, "/tmp", "cluster1");
    s.setStageId(stageId);
    long now = System.currentTimeMillis();
    s.addHostRoleExecutionCommand(hostname, Role.NAMENODE, RoleCommand.INSTALL,
        new ServiceComponentHostInstallEvent("NAMENODE", hostname, now, "HDP-1.2.0"),
        "cluster1", "HDFS");
    ExecutionCommand execCmd = s.getExecutionCommandWrapper(hostname, "NAMENODE").getExecutionCommand();
    execCmd.setCommandId(s.getActionId());
    Map<String, List<String>> clusterHostInfo = new TreeMap<String, List<String>>();
    List<String> slaveHostList = new ArrayList<String>();
    slaveHostList.add(hostname);
    slaveHostList.add("host2");
    clusterHostInfo.put("slave_hosts", slaveHostList);
    execCmd.setClusterHostInfo(clusterHostInfo);
    Map<String, String> hdfsSite = new TreeMap<String, String>();
    hdfsSite.put("dfs.block.size", "2560000000");
    Map<String, Map<String, String>> configurations =
        new TreeMap<String, Map<String, String>>();
    configurations.put("hdfs-site", hdfsSite);
    execCmd.setConfigurations(configurations);
    Map<String, String> params = new TreeMap<String, String>();
    params.put("jdklocation", "/x/y/z");
    execCmd.setHostLevelParams(params);
    Map<String, String> roleParams = new TreeMap<String, String>();
    roleParams.put("format", "false");
    execCmd.setRoleParams(roleParams);
    return s;
  }
  
  public static String jaxbToString(Object jaxbObj) throws JAXBException,
  JsonGenerationException, JsonMappingException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
    return mapper.writeValueAsString(jaxbObj);
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
  
  public static Map<String, List<String>> getClusterHostInfo(Cluster cluster) {
    Map<String, List<String>> info = new HashMap<String, List<String>>();
    if (cluster.getServices() != null) {
      for (String serviceName : cluster.getServices().keySet()) {
        if (cluster.getServices().get(serviceName) != null) {
          for (String componentName : cluster.getServices().get(serviceName)
              .getServiceComponents().keySet()) {
            String clusterInfoKey = componentToClusterInfoKeyMap
                .get(componentName);
            if (clusterInfoKey == null) {
              continue;
            }
            ServiceComponent scomp = cluster.getServices().get(serviceName)
                .getServiceComponents().get(componentName);
            if (scomp.getServiceComponentHosts() != null
                && !scomp.getServiceComponentHosts().isEmpty()) {
              List<String> hostList = new ArrayList<String>();
              for (String host: scomp.getServiceComponentHosts().keySet()) {
                hostList.add(host);
              }
              info.put(clusterInfoKey, hostList);
            }
            //Add ambari db server
            info.put("ambari_db_server_host", Arrays.asList(getHostName()));
          }
        }
      }
    }
    return info;
  }

  public static String getHostName() {
    try {
      return InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException e) {
      LOG.warn("Could not find canonical hostname ", e);
      return "localhost";
    }
  }
  
  public static String getHostsToDecommission(List<String> hosts) {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    boolean first = true;
    for (String host : hosts) {
      if (!first) {
        builder.append(",");
      } else {
        first = false;
      }
      builder.append("'");
      builder.append(host);
      builder.append("'");
    }
    return builder.toString();
  }
}
