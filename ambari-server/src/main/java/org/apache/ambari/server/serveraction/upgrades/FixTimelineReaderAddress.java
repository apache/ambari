/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.serveraction.upgrades;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.join;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.ServiceComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The timeline reader bind address is initialized to localhost by default.
 * This upgrade action updates localhost to the current hostname of the timeline reader.
 */
public class FixTimelineReaderAddress extends AbstractUpgradeServerAction {
  private static final Logger LOG = LoggerFactory.getLogger(CreateAndConfigureAction.class);
  private static final String YARN_SITE = "yarn-site";
  private static final String TIMELINE_READER = "TIMELINE_READER";
  private static final String HTTP_ADDRESS = "yarn.timeline-service.reader.webapp.address";
  private static final String HTTPS_ADDRESS = "yarn.timeline-service.reader.webapp.https.address";
  private static final String[] HOST_PROPERTIES = new String[] {HTTP_ADDRESS, HTTPS_ADDRESS
  };

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext) throws AmbariException, InterruptedException {
    Cluster cluster = getClusters().getCluster(getExecutionCommand().getClusterName());
    List<String> updatedHosts = new ArrayList<>();
    try {
      for (String propertyName : HOST_PROPERTIES) {
        Config config = cluster.getDesiredConfigByType(YARN_SITE);
        if (config == null) {
          continue;
        }
        String oldHost = config.getProperties().get(propertyName);
        if (oldHost == null) {
          continue;
        }
        String newHost = replace(oldHost, hostNameOf(cluster, "YARN", TIMELINE_READER), defaultPort(propertyName));
        updatedHosts.add(newHost);
        updateConfig(cluster, propertyName, newHost, config);
      }
      return commandReport(String.format("Updated %s hosts to: %s", TIMELINE_READER, join(updatedHosts, ", ")));
    } catch (ObjectNotFoundException e) {
      return commandReport("Skipping " + this.getClass().getSimpleName() + ". Reason: " + e.getMessage());
    }
  }

  private int defaultPort(String propertyName) {
    switch (propertyName) {
      case HTTP_ADDRESS:  return 8198;
      case HTTPS_ADDRESS: return 8199;
      default: throw new IllegalArgumentException("Unknown property: " + propertyName);
    }
  }

  private String replace(String oldHost, String newHost, int defaultPort) {
    if (oldHost.contains(":")) {
      String hostPart = oldHost.split(":")[0];
      return oldHost.replace(hostPart, newHost);
    } else {
      return newHost + ":" + defaultPort;
    }
  }

  private void updateConfig(Cluster cluster, String propertyName, String propertyValue, Config config) throws AmbariException {
    Map<String, String> newProperties = new HashMap<>();
    newProperties.put(propertyName, propertyValue);
    config.updateProperties(newProperties);
    config.save();
    agentConfigsHolder.updateData(cluster.getClusterId(), cluster.getHosts().stream().map(Host::getHostId).collect(toList()));
  }

  /**
   * @return the host name of a given service component. One instance is expected.
   */
  private String hostNameOf(Cluster cluster, String serviceName, String componentName) throws AmbariException {
    ServiceComponent timelineReader = cluster.getService(serviceName).getServiceComponent(componentName);
    Set<String> allHosts = timelineReader.getServiceComponentHosts().keySet();
    if (allHosts.isEmpty()) {
      throw new ObjectNotFoundException("No " + componentName + " hosts found.");
    }
    if (allHosts.size() > 1) {
      LOG.warn("Expected one " + componentName + " host, found " + allHosts.size() + ". Using the first host.");
    }
    return allHosts.iterator().next();
  }

  private CommandReport commandReport(String message) {
    return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", message, "");
  }
}
