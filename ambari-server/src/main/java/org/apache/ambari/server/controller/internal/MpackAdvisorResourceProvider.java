/*
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorHelper;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest.MpackAdvisorRequestBuilder;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest.MpackAdvisorRequestType;
import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse.HostGroup;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.state.ChangedConfigInfo;
import org.apache.ambari.server.topology.MpackInstance;
import org.apache.ambari.server.topology.ServiceInstance;
import org.apache.commons.collections.bag.HashBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Abstract superclass for recommendations and validations.
 */
public abstract class MpackAdvisorResourceProvider extends ReadOnlyResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(StackAdvisorResourceProvider.class);

  private static final String HOST_PROPERTY = "hosts";
  private static final String RECOMMENDATIONS_PROPERTY = "recommendations";
  private static final String BLUEPRINT_PROPERTY = "blueprint";

  private static final String BLUEPRINT_MPACK_INSTANCES_PROPERTY = RECOMMENDATIONS_PROPERTY + "/" + BLUEPRINT_PROPERTY + "/mpack_instances";
  private static final String BLUEPRINT_MPACK_INSTANCES_NAME_PROPERTY = "name";
  private static final String BLUEPRINT_MPACK_INSTANCES_TYPE_PROPERTY = "type";
  private static final String BLUEPRINT_MPACK_INSTANCES_VERSION_PROPERTY = "version";

  // Service Instances related
  private static final String MPACK_SERVICE_INSTANCES_PROPERTY = "service_instances";
  private static final String MPACK_SERVICE_INSTANCE_NAME_PROPERTY = "name";
  private static final String MPACK_SERVICE_INSTANCE_TYPE_PROPERTY = "type";
  private static final String MPACK_SERVICE_INSTANCE_CONFIGURATION_PROPERTY = "configurations";

  // Blueprint -> host-group related
  private static final String BLUEPRINT_HOST_GROUPS_PROPERTY = RECOMMENDATIONS_PROPERTY + "/" + BLUEPRINT_PROPERTY + "/host_groups";

  private static final String BLUEPRINT_HOST_GROUPS_NAME_PROPERTY = "name";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY = "components";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY = "name";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_MPACK_INSTANCE_PROPERTY = "mpack_instance";
  private static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_SERVICE_INSTANCE_PROPERTY = "service_instance";

  private static final String CHANGED_CONFIGURATIONS_PROPERTY = "changed_configurations";
  private static final String OPERATION_PROPERTY = "operation";
  private static final String OPERATION_DETAILS_PROPERTY = "operation_details";

  // Blueprint -> blueprint_cluster_binding related
  private static final String BLUEPRINT_CLUSTER_BINDING_PROPERTY = "blueprint_cluster_binding";
  private static final String BINDING_HOST_GROUPS_PROPERTY = RECOMMENDATIONS_PROPERTY + "/" + BLUEPRINT_CLUSTER_BINDING_PROPERTY + "/host_groups";
  private static final String BINDING_HOST_GROUPS_NAME_PROPERTY = "name";
  private static final String BINDING_HOST_GROUPS_HOSTS_PROPERTY = "hosts";
  private static final String BINDING_HOST_GROUPS_HOSTS_NAME_PROPERTY = "fqdn";

  protected static final String USER_CONTEXT_OPERATION_PROPERTY = "user_context/operation";
  protected static final String USER_CONTEXT_OPERATION_DETAILS_PROPERTY = "user_context/operation_details";

  protected static Configuration configuration;
  protected static MpackAdvisorHelper maHelper;

  @Inject
  public static void init(MpackAdvisorHelper instance, Configuration serverConfig) {
    maHelper = instance;
    configuration = serverConfig;
  }

  protected MpackAdvisorResourceProvider(Resource.Type type, Set<String> propertyIds, Map<Type, String> keyPropertyIds,
                                         AmbariManagementController managementController) {
    super(type, propertyIds, keyPropertyIds, managementController);
  }

  protected abstract String getRequestTypePropertyId();

  @SuppressWarnings("unchecked")
  protected MpackAdvisorRequest prepareMpackAdvisorRequest(Request request) {
    try {
      MpackAdvisorRequestType requestType = MpackAdvisorRequestType
          .fromString((String) getRequestProperty(request, getRequestTypePropertyId()));

      /*
       * ClassCastException will occur if hosts or services are empty in the
       * request.
       *
       * @see JsonRequestBodyParser for arrays parsing
       */

      // Hosts Related parsing
      Object hostsObject = getRequestProperty(request, HOST_PROPERTY);
      if (hostsObject instanceof LinkedHashSet) {
        if (((LinkedHashSet)hostsObject).isEmpty()) {
          throw new Exception("Empty host list passed to recommendation service");
        }
      }
      List<String> hosts = (List<String>) hostsObject;

      Collection<MpackInstance> mpackInstances = parseMpackInstances(request);
      Collection<HostGroup> hgCompMap = prepareHostGroupComponentsMap(request);
      Map<String, Set<String>> hgHostsMap = calculateHostGroupHostsMap(request);
      Map<String, Map<String, Set<String>>> mpacksToComponentsHostsMap = populateComponentHostsMap(hgCompMap, hgHostsMap);
      Map<String, Map<String, Map<String, String>>> configurations = populateConfigurations(request);
      Map<String, String> userContext = readUserContext(request);
      Boolean gplLicenseAccepted = configuration.getGplLicenseAccepted();

      List<ChangedConfigInfo> changedConfigurations =
          requestType == MpackAdvisorRequest.MpackAdvisorRequestType.CONFIGURATION_DEPENDENCIES ?
              calculateChangedConfigurations(request) : Collections.emptyList();

      return MpackAdvisorRequestBuilder.
          forStack().
          ofType(requestType).
          forHosts(hosts).
          forMpackInstances(mpackInstances).
          forComponentHostsMap(hgCompMap).
          forHostsGroupBindings(hgHostsMap).
          withMpacksToComponentsHostsMap(mpacksToComponentsHostsMap).
          withConfigurations(configurations).
          withChangedConfigurations(changedConfigurations).
          withUserContext(userContext).
          withGPLLicenseAccepted(gplLicenseAccepted).
          build();
    } catch (Exception e) {
      LOG.warn("Error occurred during preparation of stack advisor request", e);
      Response response = Response.status(Status.BAD_REQUEST)
          .entity(String.format("Request body is not correct, error: %s", e.getMessage())).build();
      throw new WebApplicationException(response);
    }
  }

  private Collection<MpackInstance> parseMpackInstances(Request request) {
    Collection<MpackInstance> mpackInstancesBag = new HashBag();
    Set<Map<String, Object>> reqMpackInstances = (Set<Map<String, Object>>) getRequestProperty(
        request, BLUEPRINT_MPACK_INSTANCES_PROPERTY);
    if (reqMpackInstances != null) {
      for (Map<String, Object> reqMpackInstance : reqMpackInstances) {
        String mpackName = (String) reqMpackInstance.get(BLUEPRINT_MPACK_INSTANCES_NAME_PROPERTY);
        String mpackType = (String) reqMpackInstance.get(BLUEPRINT_MPACK_INSTANCES_TYPE_PROPERTY);
        String mpackVersion = (String) reqMpackInstance.get(BLUEPRINT_MPACK_INSTANCES_VERSION_PROPERTY);

        Collection<ServiceInstance> serviceInstances = new HashBag();
        Set<Map<String, Object>> reqServiceInstances =
            (Set<Map<String, Object>>) reqMpackInstance.get(MPACK_SERVICE_INSTANCES_PROPERTY);
        for (Map<String, Object> instance : reqServiceInstances) {
          ServiceInstance serviceInstance = new ServiceInstance();
          serviceInstance.setName((String) instance.get(MPACK_SERVICE_INSTANCE_NAME_PROPERTY));
          serviceInstance.setType((String) instance.get(MPACK_SERVICE_INSTANCE_TYPE_PROPERTY));
          // If type is not provided, set it to Name.
          if (serviceInstance.getType() == null) {
            serviceInstance.setType(serviceInstance.getName());
          }
          Map<String, Map<String, Map<String, String>>> serviceConfigsMap = getServiceConfigs(instance);
          // Putting configurations in attributes object, because of Map issues.
          org.apache.ambari.server.topology.Configuration serviceConfigsObj = new org.apache.ambari.server.topology.Configuration(null, serviceConfigsMap, null);
          serviceInstance.setConfiguration(serviceConfigsObj);
          serviceInstances.add(serviceInstance);
        }
        // Update mpackInstancesBag
        mpackInstancesBag.add(new MpackInstance(mpackName, mpackType, mpackVersion ,serviceInstances));
      }
    }
    return mpackInstancesBag;
  }


  public Map<String, Map<String, Map<String, String>>> getServiceConfigs(Map<String, Object> instance) {
    Map<String, Map<String, Map<String, String>>> configurations = new HashMap<>();
    for (String property : instance.keySet()) {
      if (property.startsWith(CONFIGURATIONS_PROPERTY_ID)) {
        try {
          String propertyEnd = property.substring(CONFIGURATIONS_PROPERTY_ID.length()); // /mapred-site/properties/yarn.app.mapreduce.am.resource.mb
          propertyEnd = propertyEnd.substring(1); // Remove the initial "/", now is : mapred-site/properties/yarn.app.mapreduce.am.resource.mb
          String[] propertyPath = propertyEnd.split("/"); // length == 3
          String siteName = propertyPath[0];
          String propertiesProperty = propertyPath[1];
          String propertyName = propertyPath[2];

          Map<String, Map<String, String>> siteMap = configurations.get(siteName);
          if (siteMap == null) {
            siteMap = new HashMap<>();
            configurations.put(siteName, siteMap);
          }


          Map<String, String> propertiesMap = siteMap.get(propertiesProperty);
          if (propertiesMap == null) {
            propertiesMap = new HashMap<>();
            siteMap.put(propertiesProperty, propertiesMap);
          }

          Object propVal = instance.get(property);
          if (propVal != null)
            propertiesMap.put(propertyName, propVal.toString());
          else
            LOG.info(String.format("No value specified for configuration property, name = %s ", property));

        } catch (Exception e) {
          LOG.debug(String.format("Error handling configuration property, name = %s", property), e);
          // do nothing
        }
      }
    }
    return configurations;
  }


  /**
   * Will prepare host-group names to components names map from the
   * recommendation blueprint host groups.
   *
   * @param request mpack advisor request
   * @return host-group to components map
   */
  @SuppressWarnings("unchecked")
  private Collection<HostGroup> prepareHostGroupComponentsMap(Request request) {
    Set<Map<String, Object>> hostGroups = (Set<Map<String, Object>>) getRequestProperty(request,
        BLUEPRINT_HOST_GROUPS_PROPERTY);

    Collection<HostGroup> hgCompMap = new HashBag();
    if (hostGroups != null) {
      for (Map<String, Object> hostGroup : hostGroups) {
        String hostGroupName = (String) hostGroup.get(BLUEPRINT_HOST_GROUPS_NAME_PROPERTY);

        Set<Map<String, String>> rcvdComponentsSet = (Set<Map<String, String>>) hostGroup
            .get(BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY);


        Set<Map<String, String>> componentSet = new HashSet();
        for (Map<String, String> component : rcvdComponentsSet) {
          Map<String, String> components = new HashMap<>();
          components.put(BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY,
              component.get(BLUEPRINT_HOST_GROUPS_COMPONENTS_NAME_PROPERTY));
          components.put(BLUEPRINT_HOST_GROUPS_COMPONENTS_MPACK_INSTANCE_PROPERTY,
              component.get(BLUEPRINT_HOST_GROUPS_COMPONENTS_MPACK_INSTANCE_PROPERTY));
          components.put(BLUEPRINT_HOST_GROUPS_COMPONENTS_SERVICE_INSTANCE_PROPERTY,
              component.get(BLUEPRINT_HOST_GROUPS_COMPONENTS_SERVICE_INSTANCE_PROPERTY));

          componentSet.add(components);
        }

        HostGroup hg = new HostGroup();
        hg.setComponents(componentSet);
        hg.setName(hostGroupName);
        hgCompMap.add(hg);
      }
    }
    return hgCompMap;
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
    Map<String, Set<String>> map = new HashMap<>();
    if (bindingHostGroups != null) {
      for (Map<String, Object> hostGroup : bindingHostGroups) {
        String hostGroupName = (String) hostGroup.get(BINDING_HOST_GROUPS_NAME_PROPERTY);

        Set<Map<String, Object>> hostsSet = (Set<Map<String, Object>>) hostGroup
            .get(BINDING_HOST_GROUPS_HOSTS_PROPERTY);

        Set<String> hosts = new HashSet<>();
        for (Map<String, Object> host : hostsSet) {
          hosts.add((String) host.get(BINDING_HOST_GROUPS_HOSTS_NAME_PROPERTY));
        }

        map.put(hostGroupName, hosts);
      }
    }

    return map;
  }

  protected static final String CLUSTER_CONFIGURATIONS_PROPERTY_ID = "recommendations/blueprint/configurations/";
  protected static final String CONFIGURATIONS_PROPERTY_ID = "configurations";
  protected Map<String, Map<String, Map<String, String>>> populateConfigurations(Request request) {
    Map<String, Map<String, Map<String, String>>>  configurations = new HashMap<>();
    Map<String, Object> properties = request.getProperties().iterator().next();
    //Map<String, Object> clusterLevelConfigs = (Map<String, Object>) getRequestProperty(
    //    request, CLUSTER_CONFIGURATIONS_PROPERTY_ID);
    if (properties != null) {
      for (String property : properties.keySet()) {
        if (property.startsWith(CLUSTER_CONFIGURATIONS_PROPERTY_ID)) {
          try {
            String propertyEnd = property.substring(CLUSTER_CONFIGURATIONS_PROPERTY_ID.length());
            //getRequestProperty(request, CLUSTER_CONFIGURATIONS_PROPERTY_ID);
            //String propertyEnd = configPropRelativePath.substring(CONFIGURATIONS_PROPERTY_ID.length()); // mapred-site/properties/yarn.app.mapreduce.am.resource.mb
            String[] propertyPath = propertyEnd.split("/"); // length == 3
            String siteName = propertyPath[0];
            String propertiesProperty = propertyPath[1];
            String propertyName = propertyPath[2];

            Map<String, Map<String, String>> siteMap = configurations.get(siteName);
            if (siteMap == null) {
              siteMap = new HashMap<>();
              configurations.put(siteName, siteMap);
            }

            Map<String, String> propertiesMap = siteMap.get(propertiesProperty);
            if (propertiesMap == null) {
              propertiesMap = new HashMap<>();
              siteMap.put(propertiesProperty, propertiesMap);
            }

            Object propVal = properties.get(property);
            if (propVal != null)
              propertiesMap.put(propertyName, propVal.toString());
            else
              LOG.info(String.format("No value specified for configuration property, name = %s ", property));

          } catch (Exception e) {
            LOG.debug(String.format("Error handling configuration property, name = %s", property), e);
            // do nothing
          }

        }
      }
    }
    return configurations;
  }

  protected List<ChangedConfigInfo> calculateChangedConfigurations(Request request) {
    List<ChangedConfigInfo> configs =
        new LinkedList<>();
    HashSet<HashMap<String, String>> changedConfigs =
        (HashSet<HashMap<String, String>>) getRequestProperty(request, CHANGED_CONFIGURATIONS_PROPERTY);
    for (HashMap<String, String> props: changedConfigs) {
      configs.add(new ChangedConfigInfo(props.get("type"), props.get("name"), props.get("old_value")));
    }

    return configs;
  }

  /**
   * Parse the user contex for the call. Typical structure
   * { "operation" : "createCluster" }
   * { "operation" : "addService", "services" : "Atlas,Slider" }
   * @param request
   * @return
   */
  protected Map<String, String> readUserContext(Request request) {
    HashMap<String, String> userContext = new HashMap<>();
    if (null != getRequestProperty(request, USER_CONTEXT_OPERATION_PROPERTY)) {
      userContext.put(OPERATION_PROPERTY,
          (String) getRequestProperty(request, USER_CONTEXT_OPERATION_PROPERTY));
    }
    if (null != getRequestProperty(request, USER_CONTEXT_OPERATION_DETAILS_PROPERTY)) {
      userContext.put(OPERATION_DETAILS_PROPERTY,
          (String) getRequestProperty(request, USER_CONTEXT_OPERATION_DETAILS_PROPERTY));
    }
    return userContext;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Map<String, Set<String>>> populateComponentHostsMap(Collection<HostGroup> hostGroups,
                                                              Map<String, Set<String>> bindingHostGroups) {
    /*
     * ClassCastException may occur in case of body inconsistency: property
     * missed, etc.
     */

    Map<String, Map<String, Set<String>>> mpacksToComponentsHostsMap = new HashMap<>();
    if (null != bindingHostGroups && null != hostGroups) {
      Iterator hgItr = hostGroups.iterator();
      while (hgItr.hasNext()) {
        HostGroup hostGrp = (HostGroup) hgItr.next();
        String hgName = hostGrp.getName();
        Set<Map<String, String>> components = hostGrp.getComponents();

        Set<String> hosts = bindingHostGroups.get(hgName);
        Iterator compItr = components.iterator();
        while (compItr.hasNext()) {
          Map<String, String> compValueMap = (Map<String, String>) compItr.next();
          String compName = compValueMap.get("name");
          String compMpackname = compValueMap.get("mpack_instance");
          Map<String, Set<String>> mpackToComponentsHostsMap = mpacksToComponentsHostsMap.get(compMpackname);
          if (mpackToComponentsHostsMap == null) {
            mpackToComponentsHostsMap = new HashMap<>();
            mpacksToComponentsHostsMap.put(compMpackname, mpackToComponentsHostsMap);
          }
          // Check if 'compName' exists. If exists, fetch and update to its existing hosts.
          // else, add the 'compName' along with its hosts.
          Set<String> updatedHosts = mpackToComponentsHostsMap.get(compName);
          if (updatedHosts == null || updatedHosts.isEmpty()) {
            mpackToComponentsHostsMap.put(compName, hosts);
          } else {
            // Fetch and update the existing host(s) Set.
            updatedHosts.addAll(hosts);
            mpackToComponentsHostsMap.put(compName, updatedHosts);
          }
        }
      }
    }

    return mpacksToComponentsHostsMap;
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