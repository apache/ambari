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

import com.google.common.reflect.TypeToken;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.utils.HTTPUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.utils.StageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MasterHostResolver {

  private static Logger LOG = LoggerFactory.getLogger(MasterHostResolver.class);

  private Cluster cluster;

  enum Service {
    HDFS,
    HBASE
  }

  /**
   * Union of status for several services.
   */
  enum Status {
    ACTIVE,
    STANDBY
  }

  public MasterHostResolver() {
    ;
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

    Service s = Service.valueOf(serviceName.toUpperCase());

    Set<String> componentHosts = cluster.getHosts(serviceName, componentName);
    if (0 == componentHosts.size()) {
      return null;
    }

    hostsType.hosts = componentHosts;

    switch (s) {
      case HDFS:
        if (componentName.equalsIgnoreCase("NAMENODE")) {
          Map<Status, String> pair = getNameNodePair(componentHosts);
          if (pair != null) {
            hostsType.master = pair.containsKey(Status.ACTIVE) ? pair.get(Status.ACTIVE) :  null;
            hostsType.secondary = pair.containsKey(Status.STANDBY) ? pair.get(Status.STANDBY) :  null;
          }
        }
        break;
      case HBASE:
        if (componentName.equalsIgnoreCase("HBASE_REGIONSERVER")) {
          // TODO Rolling Upgrade, fill for this Component.
          ;
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
      Iterator iter = hosts.iterator();

      while(iter.hasNext()) {
        String hostname = (String) iter.next();
        try {
          // TODO Rolling Upgrade, don't hardcode jmx port number
          // E.g.,
          // dfs.namenode.http-address.dev.nn1 : c6401.ambari.apache.org:50070
          // dfs.namenode.http-address.dev.nn2 : c6402.ambari.apache.org:50070
          String endpoint = "http://" + hostname + ":50070/jmx?qry=Hadoop:service=NameNode,name=NameNodeStatus";
          String response = HTTPUtils.requestURL(endpoint);

          if (response != null && !response.isEmpty()) {
            Map<String, ArrayList<HashMap<String, String>>> nameNodeInfo = new HashMap<String, ArrayList<HashMap<String, String>>>();
            Type type = new TypeToken<Map<String, ArrayList<HashMap<String, String>>>>() {}.getType();
            nameNodeInfo = StageUtils.getGson().fromJson(response, type);

            try {
              String state = nameNodeInfo.get("beans").get(0).get("State");

              if (state.equalsIgnoreCase(Status.ACTIVE.toString()) || state.equalsIgnoreCase(Status.STANDBY.toString())) {
                Status status = Status.valueOf(state.toUpperCase());
                stateToHost.put(status, hostname);
              }
            } catch (Exception e) {
              throw new Exception("Response from endpoint " + endpoint + " was not formatted correctly. Value: " + response);
            }
          } else {
            throw new Exception("Response from endpoint " + endpoint + " was empty.");
          }
        } catch (Exception e) {
          LOG.warn("Failed to parse namenode jmx endpoint to get state for host " + hostname + ". Error: " + e.getMessage());
        }
      }

      if (stateToHost.containsKey(Status.ACTIVE) && stateToHost.containsKey(Status.STANDBY) && !stateToHost.get(Status.ACTIVE).equalsIgnoreCase(stateToHost.get(Status.STANDBY))) {
        return stateToHost;
      }
    }

    return null;
  }
}
