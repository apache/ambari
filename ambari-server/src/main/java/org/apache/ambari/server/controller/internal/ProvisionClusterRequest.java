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
@SuppressWarnings("unchecked")
public class ProvisionClusterRequest extends BaseClusterRequest {
  /**
   * host groups property name
   */
  public static final String HOSTGROUPS_PROPERTY = "host_groups";

  /**
   * host group name property name
   */
  public static final String HOSTGROUP_NAME_PROPERTY = "name";

  /**
   * host group host count property name
   */
  public static final String HOSTGROUP_HOST_COUNT_PROPERTY = "host_count";

  /**
   * host group host predicate property name
   */
  public static final String HOSTGROUP_HOST_PREDICATE_PROPERTY = "host_predicate";

  /**
   * host group host fqdn property name
   */
  public static final String HOSTGROUP_HOST_FQDN_PROPERTY = "fqdn";

  /**
   * host group hosts property name
   */
  public static final String HOSTGROUP_HOSTS_PROPERTY = "hosts";

  /**
   * configurations property name
   */
  public static final String CONFIGURATIONS_PROPERTY = "configurations";

  /**
   * default password property name
   */
  public static final String DEFAULT_PASSWORD_PROPERTY = "default_password";

  /**
   * configuration factory
   */
  private static ConfigurationFactory configurationFactory = new ConfigurationFactory();

  /**
   * default password
   */
  private String defaultPassword;

  /**
   * Constructor.
   *
   * @param properties  request properties
   */
  public ProvisionClusterRequest(Map<String, Object> properties) throws InvalidTopologyTemplateException {
    setClusterName(String.valueOf(properties.get(
        ClusterResourceProvider.CLUSTER_NAME_PROPERTY_ID)));

    if (properties.containsKey(DEFAULT_PASSWORD_PROPERTY)) {
      defaultPassword = String.valueOf(properties.get(DEFAULT_PASSWORD_PROPERTY));
    }

    try {
      parseBlueprint(properties);
    } catch (NoSuchStackException e) {
      throw new InvalidTopologyTemplateException("The specified stack doesn't exist: " + e, e);
    } catch (NoSuchBlueprintException e) {
      throw new InvalidTopologyTemplateException("The specified blueprint doesn't exist: " + e, e);
    }

    Configuration configuration = configurationFactory.getConfiguration(
        (Collection<Map<String, String>>) properties.get(CONFIGURATIONS_PROPERTY));
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
  public String getDescription() {
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
   *
   * @throws InvalidTopologyTemplateException  if any validation checks on properties fail
   */
  private void parseHostGroupInfo(Map<String, Object> properties) throws InvalidTopologyTemplateException {
    Collection<Map<String, Object>> hostGroups =
        (Collection<Map<String, Object>>) properties.get(HOSTGROUPS_PROPERTY);

    if (hostGroups == null || hostGroups.isEmpty()) {
      throw new InvalidTopologyTemplateException("'host_groups' element must be included in cluster create body");
    }

    for (Map<String, Object> hostGroupProperties : hostGroups) {
      processHostGroup(hostGroupProperties);
    }
  }

  /**
   * Process host group properties.
   *
   * @param hostGroupProperties  host group properties
   *
   * @throws InvalidTopologyTemplateException if any validation checks on properties fail
   */
  private void processHostGroup(Map<String, Object> hostGroupProperties) throws InvalidTopologyTemplateException {
    String name = String.valueOf(hostGroupProperties.get(HOSTGROUP_NAME_PROPERTY));
    // String.valueOf() converts null to "null"
    if (name == null || name.equals("null") || name.isEmpty()) {
      throw new InvalidTopologyTemplateException("All host groups must contain a 'name' element");
    }

    HostGroupInfo hostGroupInfo = new HostGroupInfo(name);
    getHostGroupInfo().put(name, hostGroupInfo);

    processHostCountAndPredicate(hostGroupProperties, hostGroupInfo);
    processGroupHosts(name, (Collection<Map<String, String>>)
        hostGroupProperties.get(HOSTGROUP_HOSTS_PROPERTY), hostGroupInfo);

    // don't set the parent configuration
    hostGroupInfo.setConfiguration(configurationFactory.getConfiguration(
        (Collection<Map<String, String>>) hostGroupProperties.get(CONFIGURATIONS_PROPERTY)));
  }

  /**
   * Process host count and host predicate for a host group.
   *
   * @param hostGroupProperties  host group properties
   * @param hostGroupInfo        associated host group info instance
   *
   * @throws InvalidTopologyTemplateException  specified host group properties fail validation
   */
  private void processHostCountAndPredicate(Map<String, Object> hostGroupProperties, HostGroupInfo hostGroupInfo)
      throws InvalidTopologyTemplateException {

    if (hostGroupProperties.containsKey(HOSTGROUP_HOST_COUNT_PROPERTY)) {
      hostGroupInfo.setRequestedCount(Integer.valueOf(String.valueOf(
          hostGroupProperties.get(HOSTGROUP_HOST_COUNT_PROPERTY))));
    }

    if (hostGroupProperties.containsKey(HOSTGROUP_HOST_PREDICATE_PROPERTY)) {
      if (hostGroupInfo.getRequestedHostCount() == 0) {
        throw new InvalidTopologyTemplateException(String.format(
            "Host group '%s' must not specify 'host_predicate' without 'host_count'",
            hostGroupInfo.getHostGroupName()));
      }

      String hostPredicate = String.valueOf(hostGroupProperties.get(HOSTGROUP_HOST_PREDICATE_PROPERTY));
      validateHostPredicateProperties(hostPredicate);
      try {
        hostGroupInfo.setPredicate(hostPredicate);
      } catch (InvalidQueryException e) {
        throw new InvalidTopologyTemplateException(
            String.format("Unable to compile host predicate '%s': %s", hostPredicate, e), e);
      }
    }
  }

  /**
   * Process host group hosts.
   *
   * @param name           host group name
   * @param hosts          collection of host group host properties
   * @param hostGroupInfo  associated host group info instance
   *
   * @throws InvalidTopologyTemplateException specified host group properties fail validation
   */
  private void processGroupHosts(String name, Collection<Map<String, String>> hosts, HostGroupInfo hostGroupInfo)
      throws InvalidTopologyTemplateException {

    if (hosts != null) {
      if (hostGroupInfo.getRequestedHostCount() != 0) {
        throw new InvalidTopologyTemplateException(String.format(
            "Host group '%s' must not contain both a 'hosts' element and a 'host_count' value", name));
      }

      if (hostGroupInfo.getPredicate() != null) {
        throw new InvalidTopologyTemplateException(String.format(
            "Host group '%s' must not contain both a 'hosts' element and a 'host_predicate' value", name));
      }

      for (Map<String, String> hostProperties : hosts) {
        if (hostProperties.containsKey(HOSTGROUP_HOST_FQDN_PROPERTY)) {
          hostGroupInfo.addHost(hostProperties.get(HOSTGROUP_HOST_FQDN_PROPERTY));
        }
      }
    }

    if (hostGroupInfo.getRequestedHostCount() == 0) {
      throw new InvalidTopologyTemplateException(String.format(
          "Host group '%s' must contain at least one 'hosts/fqdn' or a 'host_count' value", name));
    }
  }
}
