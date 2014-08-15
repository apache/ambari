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
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType;
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

  protected abstract String getRequestTypePropertyId();

  @SuppressWarnings("unchecked")
  protected StackAdvisorRequest prepareStackAdvisorRequest(Request request) {
    try {
      String stackName = (String) getRequestProperty(request, STACK_NAME_PROPERTY_ID);
      String stackVersion = (String) getRequestProperty(request, STACK_VERSION_PROPERTY_ID);
      StackAdvisorRequestType requestType = StackAdvisorRequestType
          .fromString((String) getRequestProperty(request, getRequestTypePropertyId()));

      /*
       * ClassCastException will occur if hosts or services are empty in the
       * request.
       * 
       * @see JsonRequestBodyParser for arrays parsing
       */
      List<String> hosts = (List<String>) getRequestProperty(request, HOST_PROPERTY);
      List<String> services = (List<String>) getRequestProperty(request, SERVICES_PROPERTY);
      Map<String, Set<String>> hgComponentsMap = calculateHostGroupComponentsMap(request);
      Map<String, Set<String>> hgHostsMap = calculateHostGroupHostsMap(request);
      Map<String, Set<String>> componentHostsMap = calculateComponentHostsMap(hgComponentsMap,
          hgHostsMap);
      Map<String, Map<String, Map<String, String>>> configurations = calculateConfigurations(request);

      StackAdvisorRequest saRequest = StackAdvisorRequestBuilder.forStack(stackName, stackVersion)
          .ofType(requestType).forHosts(hosts).forServices(services)
          .forHostComponents(hgComponentsMap).forHostsGroupBindings(hgHostsMap)
          .withComponentHostsMap(componentHostsMap).withConfigurations(configurations).build();

      return saRequest;
    } catch (Exception e) {
      LOG.warn("Error occured during preparation of stack advisor request", e);
      Response response = Response.status(Status.BAD_REQUEST)
          .entity(String.format("Request body is not correct, error: %s", e.getMessage())).build();
      // TODO: Hosts and services must not be empty
      throw new WebApplicationException(response);
    }
  }

  /**
   * Will prepare host-group names to components names map from the
   * recommendation blueprint host groups.
   * 
   * @param request stack advisor request
   * @return host-group to components map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateHostGroupComponentsMap(Request request) {
    Set<Map<String, Object>> hostGroups = (Set<Map<String, Object>>) getRequestProperty(request,
        BLUEPRINT_HOST_GROUPS_PROPERTY);
    Map<String, Set<String>> map = new HashMap<String, Set<String>>();
    if (hostGroups != null) {
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
    }

    return map;
  }

  /**
   * Will prepare host-group names to hosts names map from the recommendation
   * binding host groups.
   * 
   * @param request stack advisor request
   * @return host-group to hosts map
   */
  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateHostGroupHostsMap(Request request) {
    Set<Map<String, Object>> bindingHostGroups = (Set<Map<String, Object>>) getRequestProperty(
        request, BINDING_HOST_GROUPS_PROPERTY);
    Map<String, Set<String>> map = new HashMap<String, Set<String>>();
    if (bindingHostGroups != null) {
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
    }

    return map;
  }

  private static final String CONFIGURATIONS_PROPERTY_ID = "recommendations/blueprint/configurations/";

  private Map<String, Map<String, Map<String, String>>> calculateConfigurations(Request request) {
    Map<String, Map<String, Map<String, String>>> configurations = new HashMap<String, Map<String, Map<String, String>>>();
    Map<String, Object> properties = request.getProperties().iterator().next();
    for (String property : properties.keySet()) {
      if (property.startsWith(CONFIGURATIONS_PROPERTY_ID)) {
        try {
          String propertyEnd = property.substring(CONFIGURATIONS_PROPERTY_ID.length()); // mapred-site/properties/yarn.app.mapreduce.am.resource.mb
          String[] propertyPath = propertyEnd.split("/"); // length == 3
          String siteName = propertyPath[0];
          String propertiesProperty = propertyPath[1];
          String propertyName = propertyPath[2];

          Map<String, Map<String, String>> siteMap = configurations.get(siteName);
          if (siteMap == null) {
            siteMap = new HashMap<String, Map<String, String>>();
            configurations.put(siteName, siteMap);
          }

          Map<String, String> propertiesMap = siteMap.get(propertiesProperty);
          if (propertiesMap == null) {
            propertiesMap = new HashMap<String, String>();
            siteMap.put(propertiesProperty, propertiesMap);
          }

          String value = (String) properties.get(property);
          propertiesMap.put(propertyName, value);
        } catch (Exception e) {
          LOG.debug(String.format("Error handling configuration property, name = %s", property), e);
          // do nothing
        }
      }
    }
    return configurations;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Set<String>> calculateComponentHostsMap(Map<String, Set<String>> hostGroups,
      Map<String, Set<String>> bindingHostGroups) {
    /*
     * ClassCastException may occur in case of body inconsistency: property
     * missed, etc.
     */

    Map<String, Set<String>> componentHostsMap = new HashMap<String, Set<String>>();
    if (null != bindingHostGroups && null != hostGroups) {
      for (Map.Entry<String, Set<String>> hgComponents : hostGroups.entrySet()) {
        String hgName = hgComponents.getKey();
        Set<String> components = hgComponents.getValue();

        Set<String> hosts = bindingHostGroups.get(hgName);
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
