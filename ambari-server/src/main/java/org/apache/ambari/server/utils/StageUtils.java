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
import java.util.Collection;
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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.Stage;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.ActionExecutionContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostInstallEvent;
import org.apache.ambari.server.topology.TopologyManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.inject.Inject;

public class StageUtils {

  public static final Integer DEFAULT_PING_PORT = 8670;
  public static final String DEFAULT_RACK = "/default-rack";
  public static final String DEFAULT_IPV4_ADDRESS = "127.0.0.1";

  private static final Log LOG = LogFactory.getLog(StageUtils.class);
  protected static final String AMBARI_SERVER_HOST = "ambari_server_host";
  protected static final String HOSTS_LIST = "all_hosts";
  protected static final String PORTS = "all_ping_ports";
  protected static final String RACKS = "all_racks";
  protected static final String IPV4_ADDRESSES = "all_ipv4_ips";
  private static Map<String, String> componentToClusterInfoKeyMap =
      new HashMap<String, String>();
  private static Map<String, String> decommissionedToClusterInfoKeyMap =
      new HashMap<String, String>();
  private volatile static Gson gson;

  @Inject
  private static StageFactory stageFactory;

  @Inject
  private static TopologyManager topologyManager;

  @Inject
  public StageUtils(StageFactory stageFactory) {
    StageUtils.stageFactory = stageFactory;
  }

  private static String server_hostname;
  static {
    try {
      server_hostname = InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
    } catch (UnknownHostException e) {
      LOG.warn("Could not find canonical hostname ", e);
      server_hostname = "localhost";
    }
  }

  public static Gson getGson() {
    if (gson != null) {
      return gson;
    } else {
      synchronized (LOG) {
        if (gson == null) {
          gson = new Gson();
        }
        return gson;
      }
    }
  }

  public static void setGson(Gson gson) {
    if (gson == null) {
      StageUtils.gson = gson;
    }
  }

  //todo: proper static injection
  public static void setTopologyManager(TopologyManager topologyManager) {
    StageUtils.topologyManager = topologyManager;
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
    componentToClusterInfoKeyMap.put("FLUME_HANDLER", "flume_hosts");
    componentToClusterInfoKeyMap.put("HBASE_MASTER", "hbase_master_hosts");
    componentToClusterInfoKeyMap.put("HBASE_REGIONSERVER", "hbase_rs_hosts");
    componentToClusterInfoKeyMap.put("HIVE_SERVER", "hive_server_host");
    componentToClusterInfoKeyMap.put("HIVE_METASTORE", "hive_metastore_host");
    componentToClusterInfoKeyMap.put("OOZIE_SERVER", "oozie_server");
    componentToClusterInfoKeyMap.put("WEBHCAT_SERVER", "webhcat_server_host");
    componentToClusterInfoKeyMap.put("MYSQL_SERVER", "hive_mysql_host");
    componentToClusterInfoKeyMap.put("DASHBOARD", "dashboard_host");
    componentToClusterInfoKeyMap.put("GANGLIA_SERVER", "ganglia_server_host");
    componentToClusterInfoKeyMap.put("DATANODE", "slave_hosts");
    componentToClusterInfoKeyMap.put("TASKTRACKER", "mapred_tt_hosts");
    componentToClusterInfoKeyMap.put("HBASE_REGIONSERVER", "hbase_rs_hosts");
    componentToClusterInfoKeyMap.put("KERBEROS_SERVER", "kdc_host");
    componentToClusterInfoKeyMap.put("KERBEROS_ADMIN_CLIENT", "kerberos_adminclient_host");
    componentToClusterInfoKeyMap.put("ACCUMULO_MASTER", "accumulo_master_hosts");
    componentToClusterInfoKeyMap.put("ACCUMULO_MONITOR", "accumulo_monitor_hosts");
    componentToClusterInfoKeyMap.put("ACCUMULO_GC", "accumulo_gc_hosts");
    componentToClusterInfoKeyMap.put("ACCUMULO_TRACER", "accumulo_tracer_hosts");
    componentToClusterInfoKeyMap.put("ACCUMULO_TSERVER", "accumulo_tserver_hosts");
  }

  static {
    decommissionedToClusterInfoKeyMap.put("DATANODE", "decom_dn_hosts");
    decommissionedToClusterInfoKeyMap.put("TASKTRACKER", "decom_tt_hosts");
    decommissionedToClusterInfoKeyMap.put("NODEMANAGER", "decom_nm_hosts");
    decommissionedToClusterInfoKeyMap.put("HBASE_REGIONSERVER", "decom_hbase_rs_hosts");
  }

  public static String getActionId(long requestId, long stageId) {
    return requestId + "-" + stageId;
  }

  public static Map<String, String> getComponentToClusterInfoKeyMap() {
    return componentToClusterInfoKeyMap;
  }

  public static long[] getRequestStage(String actionId) {
    String[] fields = actionId.split("-");
    long[] requestStageIds = new long[2];
    requestStageIds[0] = Long.parseLong(fields[0]);
    requestStageIds[1] = Long.parseLong(fields[1]);
    return requestStageIds;
  }

  public static Stage getATestStage(long requestId, long stageId, String clusterHostInfo, String commandParamsStage, String hostParamsStage) {
    String hostname;
    try {
      hostname = InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      hostname = "host-dummy";
    }
    return getATestStage(requestId, stageId, hostname, clusterHostInfo, commandParamsStage, hostParamsStage);
  }

  //For testing only
  @Inject
  public static Stage getATestStage(long requestId, long stageId, String hostname, String clusterHostInfo, String commandParamsStage, String hostParamsStage) {
    Stage s = stageFactory.createNew(requestId, "/tmp", "cluster1", 1L, "context", clusterHostInfo, commandParamsStage, hostParamsStage);
    s.setStageId(stageId);
    long now = System.currentTimeMillis();
    s.addHostRoleExecutionCommand(hostname, Role.NAMENODE, RoleCommand.INSTALL,
        new ServiceComponentHostInstallEvent("NAMENODE", hostname, now, "HDP-1.2.0"), "cluster1",
        "HDFS", false, false);
    ExecutionCommand execCmd = s.getExecutionCommandWrapper(hostname, "NAMENODE").getExecutionCommand();

    execCmd.setRequestAndStage(s.getRequestId(), s.getStageId());
    List<String> slaveHostList = new ArrayList<String>();
    slaveHostList.add(hostname);
    slaveHostList.add("host2");
    Map<String, String> hdfsSite = new TreeMap<String, String>();
    hdfsSite.put("dfs.block.size", "2560000000");
    Map<String, Map<String, String>> configurations =
        new TreeMap<String, Map<String, String>>();
    configurations.put("hdfs-site", hdfsSite);
    execCmd.setConfigurations(configurations);
    Map<String, Map<String, Map<String, String>>> configurationAttributes =
        new TreeMap<String, Map<String, Map<String, String>>>();
    Map<String, Map<String, String>> hdfsSiteAttributes = new TreeMap<String, Map<String, String>>();
    Map<String, String> finalAttribute = new TreeMap<String, String>();
    finalAttribute.put("dfs.block.size", "true");
    hdfsSiteAttributes.put("final", finalAttribute);
    configurationAttributes.put("hdfsSite", hdfsSiteAttributes);
    execCmd.setConfigurationAttributes(configurationAttributes);
    Map<String, String> params = new TreeMap<String, String>();
    params.put("jdklocation", "/x/y/z");
    params.put("stack_version", "1.2.0");
    params.put("stack_name", "HDP");
    execCmd.setHostLevelParams(params);
    Map<String, String> roleParams = new TreeMap<String, String>();
    roleParams.put("format", "false");
    execCmd.setRoleParams(roleParams);
    Map<String, String> commandParams = new TreeMap<String, String>();
    commandParams.put(ExecutionCommand.KeyNames.COMMAND_TIMEOUT, "600");
    execCmd.setCommandParams(commandParams);
    return s;
  }

  public static String jaxbToString(Object jaxbObj) throws JAXBException,
      JsonGenerationException, JsonMappingException, IOException {
    return getGson().toJson(jaxbObj);
  }

  public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
    InputStream is = new ByteArrayInputStream(json.getBytes(Charset.forName("UTF8")));
    return mapper.readValue(is, clazz);
  }

  public static Map<String, String> getCommandParamsStage(ActionExecutionContext actionExecContext) throws AmbariException {
    return actionExecContext.getParameters() != null ? actionExecContext.getParameters() : new TreeMap<String, String>();
  }

  public static Map<String, Set<String>> getClusterHostInfo(Cluster cluster) throws AmbariException {
    //Fill hosts and ports lists
    Set<String>   hostsSet  = new LinkedHashSet<String>();
    List<Integer> portsList = new ArrayList<Integer>();
    List<String>  rackList  = new ArrayList<String>();
    List<String>  ipV4List  = new ArrayList<String>();

    Collection<Host> allHosts = cluster.getHosts();
    for (Host host : allHosts) {

      hostsSet.add(host.getHostName());

      Integer currentPingPort = host.getCurrentPingPort();
      portsList.add(currentPingPort == null ? DEFAULT_PING_PORT : currentPingPort);

      String rackInfo = host.getRackInfo();
      rackList.add(StringUtils.isEmpty(rackInfo) ? DEFAULT_RACK : rackInfo );

      String iPv4 = host.getIPv4();
      ipV4List.add(StringUtils.isEmpty(iPv4) ? DEFAULT_IPV4_ADDRESS : iPv4 );
    }

    // add hosts from topology manager
    Map<String, Collection<String>> pendingHostComponents = topologyManager.getPendingHostComponents();
    for (String hostname : pendingHostComponents.keySet()) {
      if (!hostsSet.contains(hostname)) {
        hostsSet.add(hostname);
        portsList.add(DEFAULT_PING_PORT);
        rackList.add(DEFAULT_RACK);
        ipV4List.add(DEFAULT_IPV4_ADDRESS);
      }
    }

    List<String> hostsList = new ArrayList<String>(hostsSet);
    Map<String, String> additionalComponentToClusterInfoKeyMap = new HashMap<String, String>();

    // Fill hosts for services
    Map<String, SortedSet<Integer>> hostRolesInfo = new HashMap<String, SortedSet<Integer>>();
    for (Map.Entry<String, Service> serviceEntry : cluster.getServices().entrySet()) {

      Service service = serviceEntry.getValue();

      for (Map.Entry<String, ServiceComponent> serviceComponentEntry : service.getServiceComponents().entrySet()) {

        ServiceComponent serviceComponent = serviceComponentEntry.getValue();
        String componentName = serviceComponent.getName();

        String roleName = componentToClusterInfoKeyMap.get(componentName);
        if(null == roleName) {
          roleName = additionalComponentToClusterInfoKeyMap.get(componentName);
        }
        if (null == roleName && !serviceComponent.isClientComponent()) {
          roleName = componentName.toLowerCase() + "_hosts";
          additionalComponentToClusterInfoKeyMap.put(componentName, roleName);
        }

        String decomRoleName = decommissionedToClusterInfoKeyMap.get(componentName);

        if (roleName == null && decomRoleName == null) {
          continue;
        }

        for (String hostName : serviceComponent.getServiceComponentHosts().keySet()) {

          if (roleName != null) {
            SortedSet<Integer> hostsForComponentsHost = hostRolesInfo.get(roleName);

            if (hostsForComponentsHost == null) {
              hostsForComponentsHost = new TreeSet<Integer>();
              hostRolesInfo.put(roleName, hostsForComponentsHost);
            }

            int hostIndex = hostsList.indexOf(hostName);
            //Add index of host to current host role
            hostsForComponentsHost.add(hostIndex);
          }

          if (decomRoleName != null) {
            ServiceComponentHost scHost = serviceComponent.getServiceComponentHost(hostName);
            if (scHost.getComponentAdminState() == HostComponentAdminState.DECOMMISSIONED) {
              SortedSet<Integer> hostsForComponentsHost = hostRolesInfo.get(decomRoleName);

              if (hostsForComponentsHost == null) {
                hostsForComponentsHost = new TreeSet<Integer>();
                hostRolesInfo.put(decomRoleName, hostsForComponentsHost);
              }

              int hostIndex = hostsList.indexOf(hostName);
              //Add index of host to current host role
              hostsForComponentsHost.add(hostIndex);
            }
          }
        }
      }
    }

    // add components from topology manager
    for (Map.Entry<String, Collection<String>> entry : pendingHostComponents.entrySet()) {
      String hostname = entry.getKey();
      Collection<String> hostComponents = entry.getValue();

      for (String hostComponent : hostComponents) {
        String roleName = componentToClusterInfoKeyMap.get(hostComponent);
        if (null == roleName) {
          roleName = additionalComponentToClusterInfoKeyMap.get(hostComponent);
        }
        if (null == roleName) {
          // even though all mappings are being added, componentToClusterInfoKeyMap is
          // a higher priority lookup
          for (Service service : cluster.getServices().values()) {
            for (ServiceComponent sc : service.getServiceComponents().values()) {
              if (!sc.isClientComponent() && sc.getName().equals(hostComponent)) {
                roleName = hostComponent.toLowerCase() + "_hosts";
                additionalComponentToClusterInfoKeyMap.put(hostComponent, roleName);
              }
            }
          }
        }

        if (roleName != null) {
          SortedSet<Integer> hostsForComponentsHost = hostRolesInfo.get(roleName);

          if (hostsForComponentsHost == null) {
            hostsForComponentsHost = new TreeSet<Integer>();
            hostRolesInfo.put(roleName, hostsForComponentsHost);
          }

          int hostIndex = hostsList.indexOf(hostname);
          if (hostIndex != -1) {
            if (!hostsForComponentsHost.contains(hostIndex)) {
              hostsForComponentsHost.add(hostIndex);
            }
          } else {
            //todo: I don't think that this can happen
            //todo: determine if it can and if so, handle properly
            //todo: if it 'cant' should probably enforce invariant
            throw new RuntimeException("Unable to get host index for host: " + hostname);
          }
        }
      }
    }

    Map<String, Set<String>> clusterHostInfo = new HashMap<String, Set<String>>();

    for (Map.Entry<String, SortedSet<Integer>> entry : hostRolesInfo.entrySet()) {
      TreeSet<Integer> sortedSet = new TreeSet<Integer>(entry.getValue());

      Set<String> replacedRangesSet = replaceRanges(sortedSet);

      clusterHostInfo.put(entry.getKey(), replacedRangesSet);
    }

    clusterHostInfo.put(HOSTS_LIST, hostsSet);
    clusterHostInfo.put(PORTS, replaceMappedRanges(portsList));
    clusterHostInfo.put(IPV4_ADDRESSES, replaceMappedRanges(ipV4List));
    clusterHostInfo.put(RACKS, replaceMappedRanges(rackList));

    // Fill server host
    /*
     * Note: We don't replace server host name by an index (like we do
     * with component hostnames), because if ambari-agent is not installed
     * at ambari-server host, then allHosts map will not contain
     * ambari-server hostname.
     */
    TreeSet<String> serverHost = new TreeSet<String>();
    serverHost.add(getHostName());
    clusterHostInfo.put(AMBARI_SERVER_HOST, serverHost);

    return clusterHostInfo;
  }

  /**
   * Given a clusterHostInfo map, replaces host indexes with the mapped host names.
   * <p/>
   * If all_hosts was <code>["host1", "host2", "host3", "host4", "host5"]</code>, then a value of
   * <code>["1-3", "5"]</code> for a given component would be converted to
   * <code>["host1", "host2", "host3", "host5"]</code>.
   * <p/>
   * Operations are performed inplace, meaning a new clusterHostInfo map is not created and updated.
   *
   * @param clusterHostInfo the cluster host info map to perform the substitutions within
   * @return the updated cluster host info map.
   * @throws AmbariException if an index fails to map to a host name
   */
  public static Map<String, Set<String>> substituteHostIndexes(Map<String, Set<String>> clusterHostInfo) throws AmbariException {
    Set<String> keysToSkip = new HashSet<String>(Arrays.asList(HOSTS_LIST, PORTS, AMBARI_SERVER_HOST, RACKS, IPV4_ADDRESSES));
    String[] allHosts = {};
    if (clusterHostInfo.get(HOSTS_LIST) != null) {
      allHosts = clusterHostInfo.get(HOSTS_LIST).toArray(new String[clusterHostInfo.get(HOSTS_LIST).size()]);
    }
    Set<String> keys = clusterHostInfo.keySet();
    for (String key : keys) {
      if (keysToSkip.contains(key)) {
        continue;
      }
      Set<String> hosts = new HashSet<String>();
      Set<String> currentHostsIndexes = clusterHostInfo.get(key);
      if (currentHostsIndexes == null) {
        continue;
      }
      for (String hostIndexRange : currentHostsIndexes) {
        for (Integer hostIndex : rangeToSet(hostIndexRange)) {
          try {
            hosts.add(allHosts[hostIndex]);
          } catch (ArrayIndexOutOfBoundsException ex) {
            throw new AmbariException("Failed to fill cluster host info  ", ex);
          }
        }
      }
      clusterHostInfo.put(key, hosts);
    }
    return clusterHostInfo;
  }

  /**
   * Finds ranges in sorted set and replaces ranges by compact notation
   * <p/>
   * <p>For example, suppose <tt>set</tt> comprises<tt> [1, 2, 3, 4, 7]</tt>.
   * After invoking <tt>rangedSet = StageUtils.replaceRanges(set)</tt>
   * <tt>rangedSet</tt> will comprise
   * <tt>["1-4", "7"]</tt>..
   *
   * @param set the source set to be ranged
   */
  public static Set<String> replaceRanges(SortedSet<Integer> set) {

    if (set == null) {
      return null;
    }

    Set<String> rangedSet = new HashSet<String>();

    Integer prevElement = null;
    Integer startOfRange = set.first();

    for (Integer i : set) {
      if (prevElement != null && (i - prevElement) > 1) {
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
   * <p/>
   * <p>For example, suppose <tt>list</tt> comprises<tt> [1, 1, 2, 2, 1, 3]</tt>.
   * After invoking <tt>rangedMappedSet = StageUtils.replaceMappedRanges(list)</tt>
   * <tt>rangedMappedSet</tt> will comprise
   * <tt>["1:0-1,4", "2:2-3", "3:5"]</tt>..
   *
   * @param values the source list to be ranged
   */
  public static <T> Set<String> replaceMappedRanges(List<T> values) {

    Map<T, SortedSet<Integer>> convolutedValues = new HashMap<T, SortedSet<Integer>>();

    int valueIndex = 0;

    for (T value : values) {

      SortedSet<Integer> correspValues = convolutedValues.get(value);

      if (correspValues == null) {
        correspValues = new TreeSet<Integer>();
        convolutedValues.put(value, correspValues);
      }
      correspValues.add(valueIndex);
      valueIndex++;
    }

    Set<String> result = new HashSet<String>();

    for (Entry<T, SortedSet<Integer>> entry : convolutedValues.entrySet()) {
      Set<String> replacedRanges = replaceRanges(entry.getValue());
      result.add(entry.getKey() + ":" + Joiner.on(",").join(replacedRanges));
    }

    return result;
  }

  public static String getHostName() {
    return server_hostname;
  }

  /**
   * Splits a range to its explicit set of values.
   * <p/>
   * For example if the range is "1-5", the result will be [1, 2, 3, 4, 5]
   *
   * @param range the range to split
   * @return a set of integers representing the original range
   */
  private static Set<Integer> rangeToSet(String range) {
    Set<Integer> indexSet = new HashSet<Integer>();
    int startIndex;
    int endIndex;
    if (range.contains("-")) {
      startIndex = Integer.parseInt(range.split("-")[0]);
      endIndex = Integer.parseInt(range.split("-")[1]);
    } else if (range.contains(",")) {
      startIndex = Integer.parseInt(range.split(",")[0]);
      endIndex = Integer.parseInt(range.split(",")[1]);
    } else {
      startIndex = endIndex = Integer.parseInt(range);
    }
    for (int i = startIndex; i <= endIndex; i++) {
      indexSet.add(i);
    }
    return indexSet;
  }

  private static String getRangedItem(Integer startOfRange, Integer endOfRange) {

    String separator = (endOfRange - startOfRange) > 1 ? "-" : ",";

    String rangeItem = endOfRange.equals(startOfRange) ?
        endOfRange.toString() :
        startOfRange + separator + endOfRange;
    return rangeItem;
  }
}
