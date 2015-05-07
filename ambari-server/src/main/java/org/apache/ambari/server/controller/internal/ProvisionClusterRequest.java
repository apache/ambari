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

//todo: in which package does this belong?  For now it is co-located with resource providers because
//todo: it needs to understand the syntax of the associated resource provider request
package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.api.predicate.InvalidQueryException;
import org.apache.ambari.server.stack.NoSuchStackException;
import org.apache.ambari.server.topology.Blueprint;
import org.apache.ambari.server.topology.BlueprintFactory;
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.ConfigurationFactory;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.NoSuchBlueprintException;
import org.apache.ambari.server.topology.RequiredPasswordValidator;
import org.apache.ambari.server.topology.TopologyRequest;
import org.apache.ambari.server.topology.TopologyValidator;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request for provisioning a cluster.
 */
public class ProvisionClusterRequest implements TopologyRequest {

  private static BlueprintFactory blueprintFactory;
  private static ConfigurationFactory configurationFactory = new ConfigurationFactory();

  private String clusterName;
  private String defaultPassword;
  private Blueprint blueprint;
  private Configuration configuration;
  private Map<String, HostGroupInfo> hostGroupInfoMap = new HashMap<String, HostGroupInfo>();

  @SuppressWarnings("unchecked")
  public ProvisionClusterRequest(Map<String, Object> properties) throws InvalidTopologyTemplateException {
    this.clusterName = String.valueOf(properties.get(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID));

    //todo: constant
    if (properties.containsKey("default_password")) {
      defaultPassword = String.valueOf(properties.get("default_password"));
    }

    try {
      parseBlueprint(properties);
    } catch (NoSuchStackException e) {
      throw new InvalidTopologyTemplateException("The specified stack doesn't exist: " + e, e);
    } catch (NoSuchBlueprintException e) {
      throw new InvalidTopologyTemplateException("The specified blueprint doesn't exist: " + e, e);
    }
    this.configuration = configurationFactory.getConfiguration(
        (Collection<Map<String, String>>) properties.get("configurations"));
    this.configuration.setParentConfiguration(blueprint.getConfiguration());
    //parseConfiguration(properties);
    parseHostGroupInfo(properties);
  }

  //todo:
  public static void init(BlueprintFactory factory) {
    blueprintFactory = factory;
  }

  @Override
  public String getClusterName() {
    return clusterName;
  }

  @Override
  public Type getType() {
    return Type.PROVISION;
  }

  @Override
  public Blueprint getBlueprint() {
    return blueprint;
  }

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  //todo: return copy?
  public Map<String, HostGroupInfo> getHostGroupInfo() {
    return hostGroupInfoMap;
  }

  @Override
  public List<TopologyValidator> getTopologyValidators() {
    return Collections.<TopologyValidator>singletonList(new RequiredPasswordValidator(defaultPassword));
  }

  @Override
  public String getCommandDescription() {
    return String.format("Provision Cluster '%s'", clusterName);
  }

  private void parseBlueprint(Map<String, Object> properties) throws NoSuchStackException, NoSuchBlueprintException {
    String blueprintName = String.valueOf(properties.get(ClusterResourceProvider.BLUEPRINT_PROPERTY_ID));
    blueprint = blueprintFactory.getBlueprint(blueprintName);

    if (blueprint == null) {
      throw new NoSuchBlueprintException(blueprintName);
    }
  }

  @SuppressWarnings("unchecked")
  private void parseHostGroupInfo(Map<String, Object> properties) throws InvalidTopologyTemplateException {
    Collection<Map<String, Object>> hostGroups =
        (Collection<Map<String, Object>>) properties.get("host_groups");

    if (hostGroups == null || hostGroups.isEmpty()) {
      throw new InvalidTopologyTemplateException("'host_groups' element must be included in cluster create body");
    }

    // iterate over host groups provided in request body
    for (Map<String, Object> hostGroupProperties : hostGroups) {
      String name = String.valueOf(hostGroupProperties.get("name"));
      // String.valueOf() converts null to "null"
      if (name.equals("null") || name.isEmpty()) {
        throw new InvalidTopologyTemplateException("All host groups must contain a 'name' element");
      }

      Collection hosts = (Collection) hostGroupProperties.get("hosts");
      if (hosts == null || hosts.isEmpty()) {
        throw new InvalidTopologyTemplateException("Host group '" + name + "' must contain a 'hosts' element");
      }

      // blueprint was parsed already
      HostGroupInfo hostGroupInfo = new HostGroupInfo(name);
      hostGroupInfoMap.put(name, hostGroupInfo);

      for (Object oHost : hosts) {
        Map<String, String> hostProperties = (Map<String, String>) oHost;
        //add host information to host group
        String fqdn = hostProperties.get("fqdn");
        if (fqdn == null || fqdn.isEmpty()) {
          //todo: validate the host_name and host_predicate are not both specified for same group
          String predicate = hostProperties.get("host_predicate");
          if (predicate != null && ! predicate.isEmpty()) {
            try {
              hostGroupInfo.setPredicate(predicate);
            } catch (InvalidQueryException e) {
              throw new InvalidTopologyTemplateException(
                  String.format("Unable to compile host predicate '%s': %s", predicate, e), e);
            }
          }

          if (hostProperties.containsKey("host_count")) {
            hostGroupInfo.setRequestedCount(Integer.valueOf(hostProperties.get("host_count")));
          } else {
            throw new InvalidTopologyTemplateException(
                "Host group '" + name + "' hosts element must include at least one fqdn" +
                " or a host_count must be specified");
          }
        } else {
          hostGroupInfo.addHost(fqdn);
        }
      }
      // don't set the parent configuration
      hostGroupInfo.setConfiguration(configurationFactory.getConfiguration(
          (Collection<Map<String, String>>) hostGroupProperties.get("configurations")));
    }
  }
}
