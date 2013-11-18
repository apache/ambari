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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import com.google.gson.Gson;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
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
  private static final String DEFAULT_PING_PORT = "8670";

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
    componentToClusterInfoKeyMap.put("SNAMENODE", "snamenode_host");
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


  public static Map<String, List<String>> getClusterHostInfo(
      Map<String, Host> allHosts, Cluster cluster, HostsMap hostsMap,
      Configuration configuration) throws AmbariException {
    Map<String, List<String>> info = new HashMap<String, List<String>>();
    if (cluster.getServices() != null) {
      String hostName = getHostName();
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
                String mappedHost = hostsMap.getHostMap(host);
                hostList.add(mappedHost);
              }
              info.put(clusterInfoKey, hostList);
            }
            //Set up ambari-rca connection properties, is this a hack?
            //info.put("ambari_db_server_host", Arrays.asList(hostsMap.getHostMap(getHostName())));
            String url = configuration.getRcaDatabaseUrl();
            if (url.contains(Configuration.HOSTNAME_MACRO)) {
              url = url.replace(Configuration.HOSTNAME_MACRO, hostsMap.getHostMap(hostName));
            }
            info.put("ambari_db_rca_url", Arrays.asList(url));
            info.put("ambari_db_rca_driver", Arrays.asList(configuration.getRcaDatabaseDriver()));
            info.put("ambari_db_rca_username", Arrays.asList(configuration.getRcaDatabaseUser()));
            info.put("ambari_db_rca_password", Arrays.asList(configuration.getRcaDatabasePassword()));

          }
        }
      }
    }

    // Add a lists of all hosts and all ping ports for agents and hosts monitoring
    List<String> allHostNames = new ArrayList<String>();
    List<String> allHostPingPorts = new ArrayList<String>();
    for (Host host : allHosts.values()) {
      allHostNames.add(host.getHostName());
      allHostPingPorts.add(host.getCurrentPingPort() == null ?
        DEFAULT_PING_PORT : host.getCurrentPingPort().toString());
    }
    info.put("all_hosts", allHostNames);
    info.put("all_ping_ports", allHostPingPorts);
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
