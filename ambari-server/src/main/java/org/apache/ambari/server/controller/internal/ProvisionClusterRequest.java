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
import org.apache.ambari.server.topology.Configuration;
import org.apache.ambari.server.topology.ConfigurationFactory;
import org.apache.ambari.server.topology.HostGroupInfo;
import org.apache.ambari.server.topology.InvalidTopologyTemplateException;
import org.apache.ambari.server.topology.NoSuchBlueprintException;
import org.apache.ambari.server.topology.RequiredPasswordValidator;
import org.apache.ambari.server.topology.TopologyValidator;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Request for provisioning a cluster.
 */
public class ProvisionClusterRequest extends BaseClusterRequest {

  /**
   * configuration factory
   */
  private static ConfigurationFactory configurationFactory = new ConfigurationFactory();

  /**
   * default password
   */
  private String defaultPassword;

  @SuppressWarnings("unchecked")
  /**
   * Constructor.
   *
   * @param properties  request properties
   */
  public ProvisionClusterRequest(Map<String, Object> properties) throws InvalidTopologyTemplateException {
    setClusterName(String.valueOf(properties.get(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID)));

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

    Configuration configuration = configurationFactory.getConfiguration(
        (Collection<Map<String, String>>) properties.get("configurations"));
    configuration.setParentConfiguration(blueprint.getConfiguration());
    setConfiguration(configuration);

    parseHostGroupInfo(properties);
  }

  @Override
  public Type getType() {
    return Type.PROVISION;
  }

  @Override
  public List<TopologyValidator> getTopologyValidators() {
    return Collections.<TopologyValidator>singletonList(new RequiredPasswordValidator(defaultPassword));
  }

  @Override
  public String getCommandDescription() {
    return String.format("Provision Cluster '%s'", clusterName);
  }

  /**
   * Parse blueprint.
   *
   * @param properties  request properties
   *
   * @throws NoSuchStackException     if specified stack doesn't exist
   * @throws NoSuchBlueprintException if specified blueprint doesn't exist
   */
  private void parseBlueprint(Map<String, Object> properties) throws NoSuchStackException, NoSuchBlueprintException {
    String blueprintName = String.valueOf(properties.get(ClusterResourceProvider.BLUEPRINT_PROPERTY_ID));
    // set blueprint field
    setBlueprint(getBlueprintFactory().getBlueprint(blueprintName));

    if (blueprint == null) {
      throw new NoSuchBlueprintException(blueprintName);
    }
  }

  /**
   * Parse host group information.
   *
   * @param properties  request properties
   * @throws InvalidTopologyTemplateException  if any validation checks on properties fail
   */
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

      HostGroupInfo hostGroupInfo = new HostGroupInfo(name);
      getHostGroupInfo().put(name, hostGroupInfo);

      for (Object oHost : hosts) {
        Map<String, String> hostProperties = (Map<String, String>) oHost;

        String hostName = hostProperties.get("fqdn");
        boolean containsHostCount = hostProperties.containsKey("host_count");
        boolean containsHostPredicate = hostProperties.containsKey("host_predicate");

        if (hostName != null && (containsHostCount || containsHostPredicate)) {
          throw new InvalidTopologyTemplateException(
              "Can't specify host_count or host_predicate if host_name is specified in hostgroup: " + name);
        }

        //add host information to host group
        if (hostName == null || hostName.isEmpty()) {
          //todo: validate the host_name and host_predicate are not both specified for same group
          String predicate = hostProperties.get("host_predicate");
          if (predicate != null && ! predicate.isEmpty()) {
            validateHostPredicateProperties(predicate);
            try {
              hostGroupInfo.setPredicate(predicate);
            } catch (InvalidQueryException e) {
              throw new InvalidTopologyTemplateException(
                  String.format("Unable to compile host predicate '%s': %s", predicate, e), e);
            }
          }

          if (containsHostCount) {
            hostGroupInfo.setRequestedCount(Integer.valueOf(hostProperties.get("host_count")));
          } else {
            throw new InvalidTopologyTemplateException(
                "Host group '" + name + "' hosts element must include at least one fqdn" +
                " or a host_count must be specified");
          }
        } else {
          hostGroupInfo.addHost(hostName);
        }
      }
      // don't set the parent configuration
      hostGroupInfo.setConfiguration(configurationFactory.getConfiguration(
          (Collection<Map<String, String>>) hostGroupProperties.get("configurations")));
    }
  }
}
