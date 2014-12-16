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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.utils.HTTPUtils;
import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.TypeToken;


public class MasterHostResolver {

  private static Logger LOG = LoggerFactory.getLogger(MasterHostResolver.class);

  private Cluster cluster;

  enum Service {
    HDFS,
    HBASE,
    YARN
  }

  /**
   * Union of status for several services.
   */
  enum Status {
    ACTIVE,
    STANDBY
  }

  public MasterHostResolver(Cluster cluster) {
    this.cluster = cluster;
  }

  /**
   * Get the master hostname of the given service and component.
   * @param serviceName Service
   * @param componentName Component
   * @return The hostname that is the master of the service and component if successful, null otherwise.
   */
  public HostsType getMasterAndHosts(String serviceName, String componentName) {
    HostsType hostsType = new HostsType();

    if (serviceName == null || componentName == null) {
      return null;
    }

    Set<String> componentHosts = cluster.getHosts(serviceName, componentName);
    if (0 == componentHosts.size()) {
      return null;
    }
    
    hostsType.hosts = componentHosts;

    Service s = null;
    try {
      s = Service.valueOf(serviceName.toUpperCase());
    } catch (Exception e) {
      // !!! nothing to do
      return hostsType;
    }

    switch (s) {
      case HDFS:
        if (componentName.equalsIgnoreCase("NAMENODE")) {
          Map<Status, String> pair = getNameNodePair(componentHosts);
          if (pair != null) {
            hostsType.master = pair.containsKey(Status.ACTIVE) ? pair.get(Status.ACTIVE) :  null;
            hostsType.secondary = pair.containsKey(Status.STANDBY) ? pair.get(Status.STANDBY) :  null;
          } else {
            hostsType.master = componentHosts.iterator().next();
          }
        }
        break;
      case YARN:
        if (componentName.equalsIgnoreCase("RESOURCEMANAGER")) {
          resolveResourceManagers(hostsType);
        }
        break;
      case HBASE:
        if (componentName.equalsIgnoreCase("HBASE_MASTER")) {
          resolveHBaseMasters(hostsType);
        }
        break;
    }
    return hostsType;
  }

  /**
   * Get mapping of the HDFS Namenodes from the state ("active" or "standby") to the hostname.
   * @param hosts Hosts to lookup.
   * @return Returns a map from the state ("active" or "standby" to the hostname with that state.
   */
  private Map<Status, String> getNameNodePair(Set<String> hosts) {
    Map<Status, String> stateToHost = new HashMap<Status, String>();
    
    if (hosts != null && hosts.size() == 2) {
      for (String hostname : hosts) {
        String state = queryJmxBeanValue(hostname, 50070,
            "Hadoop:service=NameNode,name=NameNodeStatus", "State", true);
        
        if (null != state &&
            (state.equalsIgnoreCase(Status.ACTIVE.toString()) ||
                state.equalsIgnoreCase(Status.STANDBY.toString()))) {
            Status status = Status.valueOf(state.toUpperCase());
            stateToHost.put(status, hostname);
          }
        }
        
      if (stateToHost.containsKey(Status.ACTIVE) && stateToHost.containsKey(Status.STANDBY) && !stateToHost.get(Status.ACTIVE).equalsIgnoreCase(stateToHost.get(Status.STANDBY))) {
        return stateToHost;
      }
    }

    return null;
  }
  
  private void resolveResourceManagers(HostsType hostType) {
    // !!! for RM, only the master returns jmx
    Set<String> orderedHosts = new LinkedHashSet<String>(hostType.hosts);
    
    for (String hostname : hostType.hosts) {
      
      String value = queryJmxBeanValue(hostname, 8088,
          "Hadoop:service=ResourceManager,name=RMNMInfo", "modelerType", true);
      
      if (null != value) {
        if (null == hostType.master) {
          hostType.master = hostname;
        }
        
        // !!! quick and dirty to make sure the master is last in the list
        orderedHosts.remove(hostname);
        orderedHosts.add(hostname);
      }
      
    }
    hostType.hosts = orderedHosts;
  }
  
  private void resolveHBaseMasters(HostsType hostsType) {
    
    for (String hostname : hostsType.hosts) {
      
      String value = queryJmxBeanValue(hostname, 60010,
          "Hadoop:service=HBase,name=Master,sub=Server", "tag.isActiveMaster", false);
      
      if (null != value) {
        Boolean bool = Boolean.valueOf(value);
        if (bool.booleanValue()) {
          hostsType.master = hostname;
        } else {
          hostsType.secondary = hostname;
        }
      }
      
    }
  }
  
  private String queryJmxBeanValue(String hostname, int port, String beanName, String attributeName,
      boolean asQuery) {
    
    String endPoint = asQuery ?
        String.format("http://%s:%s/jmx?qry=%s", hostname, port, beanName) :
          String.format("http://%s:%s/jmx?get=%s::%s", hostname, port, beanName, attributeName);
    
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
      LOG.info("Could not load JMX from {}/{} from {}", beanName, attributeName, hostname, e);
    }
    
    return null;
    
  }
  
}
