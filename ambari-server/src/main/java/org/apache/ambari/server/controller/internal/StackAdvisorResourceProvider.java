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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestBuilder;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import com.google.inject.Inject;

/**
 * Abstract superclass for recommendations and validations.
 */
public abstract class StackAdvisorResourceProvider extends ReadOnlyResourceProvider {

  protected static final String STACK_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("Versions",
      "stack_name");
  protected static final String STACK_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId(
      "Versions", "stack_version");

  private static final String HOST_PROPERTY = "hosts";
  private static final String SERVICES_PROPERTY = "services";

  private static final String BLUEPRINT_HOST_GROUPS_PROPERTY = "recommendations/blueprint/host_groups";
  private static final String BINDING_HOST_GROUPS_PROPERTY = "recommendations/blueprint_cluster_binding/host_groups";

  private static final String BLUEPRINT_HOST_GROUPS_NAME_PROPERTY = "name";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY = "components";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY = "name";

  private static final String BINDING_HOST_GROUPS_NAME_PROPERTY = "name";
  private static final String BINDING_HOST_GROUPS_HOSTS_PROPERTY = "hosts";
  private static final String BINDING_HOST_GROUPS_HOSTS_NAME_PROPERTY = "fqdn";

  protected static StackAdvisorHelper saHelper;

  @Inject
  public static void init(StackAdvisorHelper instance) {
    saHelper = instance;
  }

  protected StackAdvisorResourceProvider(Set<String> propertyIds, Map<Type, String> keyPropertyIds,
      AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @SuppressWarnings("unchecked")
  protected StackAdvisorRequest prepareStackAdvisorRequest(Request request) {
    try {
      String stackName = (String) getRequestProperty(request, STACK_NAME_PROPERTY_ID);
      String stackVersion = (String) getRequestProperty(request, STACK_VERSION_PROPERTY_ID);

      /*
       * ClassCastException will occur if hosts or services are empty in the
       * request.
       * 
       * @see JsonRequestBodyParser for arrays parsing
       */
      List<String> hosts = (List<String>) getRequestProperty(request, HOST_PROPERTY);
      List<String> services = (List<String>) getRequestProperty(request, SERVICES_PROPERTY);
      Map<String, Set<String>> componentHostsMap = calculateComponentHostsMap(request);

      StackAdvisorRequest saRequest = StackAdvisorRequestBuilder.forStack(stackName, stackVersion)
          .forHosts(hosts).forServices(services).withComponentHostsMap(componentHostsMap).build();

      return saRequest;
    } catch (Exception e) {
      LOG.warn("Error occured during preparation of stack advisor request", e);
      Response response = Response.status(Status.BAD_REQUEST).entity("Request body is not correct")
          .build();
      // TODO: Hosts and services must not be empty
      throw new WebApplicationException(response);
    }
  }

  /**
   * Will prepare host-group names to components names map from the
   * recommendation blueprint host groups.
   * 
   * @param hostGroups the blueprint host groups
   * @return host-group to components map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateHostGroupComponentsMap(
      Set<Map<String, Object>> hostGroups) {
    Map<String, Set<String>> map = new HashMap<String, Set<String>>();
    for (Map<String, Object> hostGroup : hostGroups) {
      String hostGroupName = (String) hostGroup.get(BLUEPRINT_HOST_GROUPS_NAME_PROPERTY);

      Set<Map<String, Object>> componentsSet = (Set<Map<String, Object>>) hostGroup
          .get(BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY);

      Set<String> components = new HashSet<String>();
      for (Map<String, Object> component : componentsSet) {
        components.add((String) component.get(BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY));
      }

      map.put(hostGroupName, components);
    }

    return map;
  }

  /**
   * Will prepare host-group names to hosts names map from the recommendation
   * binding host groups.
   * 
   * @param bindingHostGroups the binding host groups
   * @return host-group to hosts map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateHostGroupHostsMap(
      Set<Map<String, Object>> bindingHostGroups) {
    Map<String, Set<String>> map = new HashMap<String, Set<String>>();

    for (Map<String, Object> hostGroup : bindingHostGroups) {
      String hostGroupName = (String) hostGroup.get(BINDING_HOST_GROUPS_NAME_PROPERTY);

      Set<Map<String, Object>> hostsSet = (Set<Map<String, Object>>) hostGroup
          .get(BINDING_HOST_GROUPS_HOSTS_PROPERTY);

      Set<String> hosts = new HashSet<String>();
      for (Map<String, Object> host : hostsSet) {
        hosts.add((String) host.get(BINDING_HOST_GROUPS_HOSTS_NAME_PROPERTY));
      }

      map.put(hostGroupName, hosts);
    }

    return map;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateComponentHostsMap(Request request) {
    /*
     * ClassCastException may occur in case of body inconsistency: property
     * missed, etc.
     */
    Set<Map<String, Object>> hostGroups = (Set<Map<String, Object>>) getRequestProperty(request,
        BLUEPRINT_HOST_GROUPS_PROPERTY);
    Set<Map<String, Object>> bindingHostGroups = (Set<Map<String, Object>>) getRequestProperty(
        request, BINDING_HOST_GROUPS_PROPERTY);

    Map<String, Set<String>> componentHostsMap = new HashMap<String, Set<String>>();
    if (null != bindingHostGroups && null != hostGroups) {
      Map<String, Set<String>> hgComponentsMap = calculateHostGroupComponentsMap(hostGroups);
      Map<String, Set<String>> hgHostsMap = calculateHostGroupHostsMap(bindingHostGroups);

      for (Map.Entry<String, Set<String>> hgComponents : hgComponentsMap.entrySet()) {
        String hgName = hgComponents.getKey();
        Set<String> components = hgComponents.getValue();

        Set<String> hosts = hgHostsMap.get(hgName);
        for (String component : components) {
          Set<String> componentHosts = componentHostsMap.get(component);
          if (componentHosts == null) { // if was not initialized
            componentHosts = new HashSet<String>();
            componentHostsMap.put(component, componentHosts);
          }
          componentHosts.addAll(hosts);
        }
      }
    }

    return componentHostsMap;
  }

  protected Object getRequestProperty(Request request, String propertyName) {
    for (Map<String, Object> propertyMap : request.getProperties()) {
      if (propertyMap.containsKey(propertyName)) {
        return propertyMap.get(propertyName);
      }
    }
    return null;
  }

}
