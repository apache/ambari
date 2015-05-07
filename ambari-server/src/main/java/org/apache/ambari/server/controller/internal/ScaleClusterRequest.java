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


package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.TopologyRequest;
import org.apache.ambari.server.topology.TopologyValidator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A request for a scaling an existing cluster.
 */
public class ScaleClusterRequest implements TopologyRequest {

  private static BlueprintFactory blueprintFactory;

  private String clusterName;

  private Blueprint blueprint;

  private Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<String, HostGroupInfo>();

  public static void init(BlueprintFactory factory) {
    blueprintFactory = factory;
  }

  public ScaleClusterRequest(Request request) throws InvalidTopologyTemplateException {
    for (Map<String, Object> properties : request.getProperties()) {
      // can only operate on a single cluster per logical request
      if (clusterName == null) {
        clusterName = String.valueOf(properties.get(HostResourceProvider.HOST_CLUSTER_NAME_PROPERTY_ID));
      }
      parseHostGroup(properties);
    }
  }

  @Override
  public String getClusterName() {
    return clusterName;
  }

  @Override
  public Type getType() {
    return Type.SCALE;
  }

  @Override
  public Blueprint getBlueprint() {
    return blueprint;
  }

  @Override
  public Configuration getConfiguration() {
    // currently don't allow cluster scoped configuration in scaling operation
    return new Configuration(Collections.<String, Map<String, String>>emptyMap(),
        Collections.<String, Map<String, Map<String, String>>>emptyMap());
  }

  @Override
  public Map<String, HostGroupInfo> getHostGroupInfo() {
    return hostGroupInfoMap;
  }

  @Override
  public List<TopologyValidator> getTopologyValidators() {
    return Collections.emptyList();
  }

  @Override
  public String getCommandDescription() {
    return String.format("Scale Cluster '%s' (+%s hosts)", clusterName, getTotalRequestedHostCount());
  }

  private void parseHostGroup(Map<String, Object> properties) throws InvalidTopologyTemplateException {
    String blueprintName = String.valueOf(properties.get(HostResourceProvider.BLUEPRINT_PROPERTY_ID));
    if (blueprint == null) {
      blueprint = parseBlueprint(blueprintName);
    } else if (! blueprintName.equals(blueprint.getName())) {
      throw new InvalidTopologyTemplateException(
          "Currently, a scaling request may only refer to a single blueprint");
    }

    String hgName = String.valueOf(properties.get(HostResourceProvider.HOSTGROUP_PROPERTY_ID));
    //todo: need to use fully qualified host group name.  For now, disregard name collisions across BP's
    HostGroupInfo hostGroupInfo = hostGroupInfoMap.get(hgName);

    if (hostGroupInfo == null) {
      if (blueprint.getHostGroup(hgName) == null) {
        throw new InvalidTopologyTemplateException("Invalid host group specified in request: " + hgName);
      }
      hostGroupInfo = new HostGroupInfo(hgName);
      hostGroupInfoMap.put(hgName, hostGroupInfo);
    }

    // specifying configuration is scaling request isn't permitted
    hostGroupInfo.setConfiguration(new Configuration(Collections.<String, Map<String, String>>emptyMap(),
        Collections.<String, Map<String, Map<String, String>>>emptyMap()));

    // process host_name and host_count
    if (properties.containsKey("host_count")) {
      //todo: validate the host_name and host_predicate are not both specified for same group
      //todo: validate that when predicate is specified that only a single host group entry is specified
      String predicate = String.valueOf(properties.get("host_predicate"));
      if (predicate != null && ! predicate.isEmpty()) {
        try {
          hostGroupInfo.setPredicate(predicate);
        } catch (InvalidQueryException e) {
          throw new InvalidTopologyTemplateException(
              String.format("Unable to compile host predicate '%s': %s", predicate, e), e);
        }
      }

      if (! hostGroupInfo.getHostNames().isEmpty()) {
        throw new InvalidTopologyTemplateException("Can't specify both host_name and host_count for the same hostgroup: " + hgName);
      }
      hostGroupInfo.setRequestedCount(Integer.valueOf(String.valueOf(properties.get("host_count"))));
    } else {
      if (hostGroupInfo.getRequestedHostCount() != hostGroupInfo.getHostNames().size()) {
        throw new InvalidTopologyTemplateException("Can't specify both host_name and host_count for the same hostgroup: " + hgName);
      }
      hostGroupInfo.addHost(getHostNameFromProperties(properties));
    }
  }

  private Blueprint parseBlueprint(String blueprintName) throws InvalidTopologyTemplateException  {
    Blueprint blueprint;
    try {
      blueprint = blueprintFactory.getBlueprint(blueprintName);
    } catch (NoSuchStackException e) {
      throw new InvalidTopologyTemplateException("Invalid stack specified in the blueprint: " + blueprintName);
    }

    if (blueprint == null) {
      throw new InvalidTopologyTemplateException("The specified blueprint doesn't exist: " + blueprintName);
    }
    return blueprint;
  }

  //todo: this was copied exactly from HostResourceProvider
  private String getHostNameFromProperties(Map<String, Object> properties) {
    String hostname = String.valueOf(properties.get(HostResourceProvider.HOST_NAME_PROPERTY_ID));

    return hostname != null ? hostname :
        String.valueOf(properties.get(HostResourceProvider.HOST_NAME_NO_CATEGORY_PROPERTY_ID));
  }

  private int getTotalRequestedHostCount() {
    int count = 0;
    for (HostGroupInfo groupInfo : getHostGroupInfo().values()) {
      count += groupInfo.getRequestedHostCount();
    }
    return count;
  }
}
