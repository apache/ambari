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

package org.apache.ambari.server.stack;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.UpgradeState;
import org.apache.ambari.server.utils.HTTPUtils;
import org.apache.ambari.server.utils.HostAndPort;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;


public class MasterHostResolver {

  private static Logger LOG = LoggerFactory.getLogger(MasterHostResolver.class);

  private Cluster m_cluster;
  private String m_version;
  private ConfigHelper m_configHelper;

  public enum Service {
    HDFS,
    HBASE,
    YARN,
    OTHER
  }

  /**
   * Union of status for several services.
   */
  protected enum Status {
    ACTIVE,
    STANDBY
  }

  /**
   * Create a resolver that does not consider HostComponents' version when
   * resolving hosts.  Common use case is creating an upgrade that should
   * include an entire cluster.
   * @param configHelper Configuration Helper
   * @param cluster the cluster
   */
  public MasterHostResolver(ConfigHelper configHelper, Cluster cluster) {
    this(configHelper, cluster, null);
  }

  /**
   * Create a resolver that compares HostComponents' version when calculating
   * hosts for the stage.  Common use case is for downgrades when only some
   * HostComponents need to be downgraded, and HostComponents already at the
   * correct version are skipped.
   * @param configHelper Configuration Helper
   * @param cluster the cluster
   * @param version the version, or {@code null} to not compare versions
   */
  public MasterHostResolver(ConfigHelper configHelper, Cluster cluster, String version) {
    m_configHelper = configHelper;
    m_cluster = cluster;
    m_version = version;
  }

  /**
   * Gets the cluster that this instance of the {@link MasterHostResolver} is
   * initialized with.
   *
   * @return the cluster (not {@code null}).
   */
  public Cluster getCluster() {
    return m_cluster;
  }

  /**
   * Get the master hostname of the given service and component.
   * @param serviceName Service
   * @param componentName Component
   * @return The hostname that is the master of the service and component if successful, null otherwise.
   */
  public HostsType getMasterAndHosts(String serviceName, String componentName) {

    if (serviceName == null || componentName == null) {
      return null;
    }

    Set<String> componentHosts = m_cluster.getHosts(serviceName, componentName);
    if (0 == componentHosts.size()) {
      return null;
    }

    HostsType hostsType = new HostsType();
    hostsType.hosts.addAll(componentHosts);

    Service s = Service.OTHER;
    try {
      s = Service.valueOf(serviceName.toUpperCase());
    } catch (Exception e) {
      // !!! nothing to do
    }

    try {
      switch (s) {
        case HDFS:
          if (componentName.equalsIgnoreCase("NAMENODE")) {
            if (componentHosts.size() != 2) {
              return filterHosts(hostsType, serviceName, componentName);
            }

            Map<Status, String> pair = getNameNodePair();
            if (pair != null) {
              hostsType.master = pair.containsKey(Status.ACTIVE) ? pair.get(Status.ACTIVE) :  null;
              hostsType.secondary = pair.containsKey(Status.STANDBY) ? pair.get(Status.STANDBY) :  null;
            } else {
              // !!! we KNOW we have 2 componentHosts if we're here.
              Iterator<String> iterator = componentHosts.iterator();
              hostsType.master = iterator.next();
              hostsType.secondary = iterator.next();

              LOG.warn("Could not determine the active/standby states from NameNodes {}. " +
                  "Using {} as active and {} as standby.",
                  StringUtils.join(componentHosts, ','), hostsType.master, hostsType.secondary);
            }
          }
          break;
        case YARN:
          if (componentName.equalsIgnoreCase("RESOURCEMANAGER")) {
            resolveResourceManagers(getCluster(), hostsType);
          }
          break;
        case HBASE:
          if (componentName.equalsIgnoreCase("HBASE_MASTER")) {
            resolveHBaseMasters(getCluster(), hostsType);
          }
          break;
        default:
          break;
      }
    } catch (Exception err) {
      LOG.error("Unable to get master and hosts for Component " + componentName + ". Error: " + err.getMessage(), err);
    }

    hostsType = filterHosts(hostsType, serviceName, componentName);

    return hostsType;
  }

  /**
   * Filters the supplied list of hosts in the following ways:
   * <ul>
   * <li>Compares the versions of a HostComponent to the version for the
   * resolver. Only versions that do not match are retained.</li>
   * <li>Removes unhealthy hosts in maintenance mode from the list of healthy
   * hosts</li>
   * </ul>
   *
   * @param hostsType
   *          the hosts to resolve
   * @param service
   *          the service name
   * @param component
   *          the component name
   * @return the modified hosts instance with filtered and unhealthy hosts
   *         filled
   */
  private HostsType filterHosts(HostsType hostsType, String service, String component) {
    try {
      org.apache.ambari.server.state.Service svc = m_cluster.getService(service);
      ServiceComponent sc = svc.getServiceComponent(component);

      // !!! not really a fan of passing these around
      List<ServiceComponentHost> unhealthyHosts = new ArrayList<ServiceComponentHost>();
      LinkedHashSet<String> upgradeHosts = new LinkedHashSet<String>();

      for (String hostName : hostsType.hosts) {
        ServiceComponentHost sch = sc.getServiceComponentHost(hostName);
        Host host = sch.getHost();
        MaintenanceState maintenanceState = host.getMaintenanceState(sch.getClusterId());

        // !!! FIXME: only rely on maintenance state once the upgrade endpoint
        // is using the pre-req endpoint for determining if an upgrade is
        // possible
        if (maintenanceState != MaintenanceState.OFF) {
          unhealthyHosts.add(sch);
        } else if (null == m_version || null == sch.getVersion() ||
            !sch.getVersion().equals(m_version) ||
            sch.getUpgradeState() == UpgradeState.FAILED) {
          upgradeHosts.add(hostName);
        }
      }

      hostsType.unhealthy = unhealthyHosts;
      hostsType.hosts = upgradeHosts;

      return hostsType;
    } catch (AmbariException e) {
      // !!! better not
      LOG.warn("Could not determine host components to upgrade. Defaulting to saved hosts.", e);
      return hostsType;
    }
  }

  /**
   * Determine if HDFS is present and it has NameNode High Availability.
   * @return true if has NameNode HA, otherwise, false.
   */
  public boolean isNameNodeHA() throws AmbariException {
    Map<String, org.apache.ambari.server.state.Service> services = m_cluster.getServices();
    if (services != null && services.containsKey("HDFS")) {

      Set<String> secondaryNameNodeHosts = m_cluster.getHosts("HDFS", "SECONDARY_NAMENODE");
      Set<String> nameNodeHosts = m_cluster.getHosts("HDFS", "NAMENODE");

      if (secondaryNameNodeHosts.size() == 1 && nameNodeHosts.size() == 1) {
        return false;
      }
      if (nameNodeHosts.size() > 1) {
        return true;
      }

      throw new AmbariException("Unable to determine if cluster has NameNode HA.");
    }
    return false;
  }

  /**
   * Get mapping of the HDFS Namenodes from the state ("active" or "standby") to the hostname.
   * @return Returns a map from the state ("active" or "standby" to the hostname with that state if exactly
   * one active and one standby host were found, otherwise, return null.
   * The hostnames are returned in lowercase.
   */
  private Map<Status, String> getNameNodePair() {
    Map<Status, String> stateToHost = new HashMap<Status, String>();
    Cluster cluster = getCluster();

    String nameService = m_configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.HDFS_SITE, "dfs.internal.nameservices");
    if (nameService == null || nameService.isEmpty()) {
      return null;
    }

    String nnUniqueIDstring = m_configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.HDFS_SITE, "dfs.ha.namenodes." + nameService);
    if (nnUniqueIDstring == null || nnUniqueIDstring.isEmpty()) {
      return null;
    }

    String[] nnUniqueIDs = nnUniqueIDstring.split(",");
    if (nnUniqueIDs == null || nnUniqueIDs.length != 2) {
      return null;
    }

    String policy = m_configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.HDFS_SITE, "dfs.http.policy");
    boolean encrypted = (policy != null && policy.equalsIgnoreCase(ConfigHelper.HTTPS_ONLY));

    String namenodeFragment = "dfs.namenode." + (encrypted ? "https-address" : "http-address") + ".{0}.{1}";

    for (String nnUniqueID : nnUniqueIDs) {
      String key = MessageFormat.format(namenodeFragment, nameService, nnUniqueID);
      String value = m_configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.HDFS_SITE, key);

      try {
        HostAndPort hp = HTTPUtils.getHostAndPortFromProperty(value);
        if (hp == null) {
          throw new MalformedURLException("Could not parse host and port from " + value);
        }

        String state = queryJmxBeanValue(hp.host, hp.port, "Hadoop:service=NameNode,name=NameNodeStatus", "State", true, encrypted);

        if (null != state && (state.equalsIgnoreCase(Status.ACTIVE.toString()) || state.equalsIgnoreCase(Status.STANDBY.toString()))) {
          Status status = Status.valueOf(state.toUpperCase());
          stateToHost.put(status, hp.host.toLowerCase());
        } else {
          LOG.error(String.format("Could not retrieve state for NameNode %s from property %s by querying JMX.", hp.host, key));
        }
      } catch (MalformedURLException e) {
        LOG.error(e.getMessage());
      }
    }

    if (stateToHost.containsKey(Status.ACTIVE) && stateToHost.containsKey(Status.STANDBY) && !stateToHost.get(Status.ACTIVE).equalsIgnoreCase(stateToHost.get(Status.STANDBY))) {
      return stateToHost;
    }
    return null;
  }

  /**
   * Resolve the name of the Resource Manager master and convert the hostname to lowercase.
   * @param cluster Cluster
   * @param hostType RM hosts
   * @throws MalformedURLException
   */
  private void resolveResourceManagers(Cluster cluster, HostsType hostType) throws MalformedURLException {
    LinkedHashSet<String> orderedHosts = new LinkedHashSet<String>(hostType.hosts);

    // IMPORTANT, for RM, only the master returns jmx
    String rmWebAppAddress = m_configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.YARN_SITE, "yarn.resourcemanager.webapp.address");
    HostAndPort hp = HTTPUtils.getHostAndPortFromProperty(rmWebAppAddress);
    if (hp == null) {
      throw new MalformedURLException("Could not parse host and port from " + rmWebAppAddress);
    }

    for (String hostname : hostType.hosts) {
      String value = queryJmxBeanValue(hostname, hp.port,
          "Hadoop:service=ResourceManager,name=RMNMInfo", "modelerType", true);

      if (null != value) {
        if (null == hostType.master) {
          hostType.master = hostname.toLowerCase();
        }

        // Quick and dirty to make sure the master is last in the list
        orderedHosts.remove(hostname.toLowerCase());
        orderedHosts.add(hostname.toLowerCase());
      }

    }
    hostType.hosts = orderedHosts;
  }

  /**
   * Resolve the HBASE master and convert the hostname to lowercase.
   * @param cluster Cluster
   * @param hostsType HBASE master host.
   * @throws AmbariException
   */
  private void resolveHBaseMasters(Cluster cluster, HostsType hostsType) throws AmbariException {
    String hbaseMasterInfoPortProperty = "hbase.master.info.port";
    String hbaseMasterInfoPortValue = m_configHelper.getValueFromDesiredConfigurations(cluster, ConfigHelper.HBASE_SITE, hbaseMasterInfoPortProperty);

    if (hbaseMasterInfoPortValue == null || hbaseMasterInfoPortValue.isEmpty()) {
      throw new AmbariException("Could not find property " + hbaseMasterInfoPortProperty);
    }

    final int hbaseMasterInfoPort = Integer.parseInt(hbaseMasterInfoPortValue);
    for (String hostname : hostsType.hosts) {
      String value = queryJmxBeanValue(hostname, hbaseMasterInfoPort,
          "Hadoop:service=HBase,name=Master,sub=Server", "tag.isActiveMaster", false);

      if (null != value) {
        Boolean bool = Boolean.valueOf(value);
        if (bool.booleanValue()) {
          hostsType.master = hostname.toLowerCase();
        } else {
          hostsType.secondary = hostname.toLowerCase();
        }
      }
    }
  }

  protected String queryJmxBeanValue(String hostname, int port, String beanName, String attributeName,
                                  boolean asQuery) {
    return queryJmxBeanValue(hostname, port, beanName, attributeName, asQuery, false);
  }

  /**
   * Query the JMX attribute at http(s)://$server:$port/jmx?qry=$query or http(s)://$server:$port/jmx?get=$bean::$attribute
   * @param hostname host name
   * @param port port number
   * @param beanName if asQuery is false, then search for this bean name
   * @param attributeName if asQuery is false, then search for this attribute name
   * @param asQuery whether to search bean or query
   * @param encrypted true if using https instead of http.
   * @return The jmx value.
   */
  protected String queryJmxBeanValue(String hostname, int port, String beanName, String attributeName,
                                  boolean asQuery, boolean encrypted) {

    String protocol = encrypted ? "https://" : "http://";
    String endPoint = protocol + (asQuery ?
        String.format("%s:%s/jmx?qry=%s", hostname, port, beanName) :
        String.format("%s:%s/jmx?get=%s::%s", hostname, port, beanName, attributeName));

    String response = HTTPUtils.requestURL(endPoint);

    if (null == response || response.isEmpty()) {
      return null;
    }

    Type type = new TypeToken<Map<String, ArrayList<HashMap<String, String>>>>() {}.getType();

    try {
      Map<String, ArrayList<HashMap<String, String>>> jmxBeans =
          StageUtils.getGson().fromJson(response, type);

      return jmxBeans.get("beans").get(0).get(attributeName);
    } catch (Exception e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Could not load JMX from {}/{} from {}", beanName, attributeName, hostname, e);
      } else {
        LOG.debug("Could not load JMX from {}/{} from {}", beanName, attributeName, hostname);
      }
    }

    return null;
  }
}
