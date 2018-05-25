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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorException;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequest;
import org.apache.ambari.server.api.services.mpackadvisor.MpackAdvisorRequestException;
import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse;
import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse.BindingHostGroup;
import org.apache.ambari.server.api.services.mpackadvisor.recommendations.MpackRecommendationResponse.HostGroup;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

public class MpackRecommendationResourceProvider extends MpackAdvisorResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(MpackRecommendationResourceProvider.class);

  protected static final String RECOMMENDATION_ID_PROPERTY_ID = PropertyHelper.getPropertyId(
      "MpackRecommendation", "id");

  protected static final String HOSTS_PROPERTY_ID = "hosts";
  protected static final String SERVICES_PROPERTY_ID = "services";
  protected static final String RECOMMEND_PROPERTY_ID = "recommend";
  protected static final String RECOMMENDATIONS_PROPERTY_ID = "recommendations";

  protected static final String BLUEPRINT_PROPERTY_ID = PropertyHelper
      .getPropertyId("recommendations", "blueprint");
  protected static final String BLUEPRINT_CONFIGURATIONS_PROPERTY_ID = PropertyHelper
      .getPropertyId("recommendations/blueprint", "configurations");

  protected static final String BLUEPRINT_HOST_GROUPS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "recommendations/blueprint", "host_groups");
  protected static final String BLUEPRINT_HOST_GROUPS_NAME_PROPERTY_ID = "name";
  protected static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY_ID = "components";

  //MpackInstances
  protected static final String BLUEPRINT_MPACK_INSTANCES_PROPERTY_ID = PropertyHelper.getPropertyId(
      "recommendations/blueprint", "mpack_instances");
  protected static final String BLUEPRINT_MPACK_INSTANCES_NAME_PROPERTY_ID = "name";
  protected static final String BLUEPRINT_MPACK_INSTANCES_VERSION_PROPERTY_ID = "version";

  // Service Instances
  protected static final String BLUEPRINT_MPACK_SERVICE_PROPERTY_ID = "service_instances";
  protected static final String BLUEPRINT_MPACK_SERVICE_NAME_PROPERTY_ID = "name";
  protected static final String BLUEPRINT_MPACK_SERVICE_TYPE_PROPERTY_ID = "type";
  protected static final String BLUEPRINT_MPACK_SERVICE_CONFIG_PROPERTY_ID = "configurations";
  protected static final String BLUEPRINT_MPACK_SERVICE_CONFIG_PROPERTIES_PROPERTY_ID = "properties";

  protected static final String BINDING_HOST_GROUPS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "recommendations/blueprint_cluster_binding", "host_groups");
  protected static final String BINDING_HOST_GROUPS_NAME_PROPERTY_ID = "name";
  protected static final String BINDING_HOST_GROUPS_HOSTS_PROPERTY_ID = "hosts";
  protected static final String BINDING_PROPERTY_ID = PropertyHelper
      .getPropertyId("recommendations", "blueprint_cluster_binding");


  /**
   * The key property ids for a Recommendation resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Type.MpackRecommendation, RECOMMENDATION_ID_PROPERTY_ID)
      .build();

  /**
   * The property ids for a Recommendation resource.
   */
  private static Set<String> propertyIds = Sets.newHashSet(
      RECOMMENDATION_ID_PROPERTY_ID,
      RECOMMEND_PROPERTY_ID,
      HOSTS_PROPERTY_ID,
      RECOMMENDATIONS_PROPERTY_ID,
      BLUEPRINT_PROPERTY_ID,
      BLUEPRINT_CONFIGURATIONS_PROPERTY_ID,
      BLUEPRINT_HOST_GROUPS_PROPERTY_ID,
      PropertyHelper.getPropertyId(BLUEPRINT_HOST_GROUPS_PROPERTY_ID, BLUEPRINT_HOST_GROUPS_NAME_PROPERTY_ID),
      PropertyHelper.getPropertyId(BLUEPRINT_HOST_GROUPS_PROPERTY_ID, BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY_ID),
      BINDING_PROPERTY_ID,
      BINDING_HOST_GROUPS_PROPERTY_ID,
      PropertyHelper.getPropertyId(BINDING_HOST_GROUPS_PROPERTY_ID, BINDING_HOST_GROUPS_NAME_PROPERTY_ID),
      PropertyHelper.getPropertyId(BINDING_HOST_GROUPS_PROPERTY_ID, BINDING_HOST_GROUPS_HOSTS_PROPERTY_ID),
      BINDING_HOST_GROUPS_NAME_PROPERTY_ID,
      BINDING_HOST_GROUPS_HOSTS_PROPERTY_ID);


  protected MpackRecommendationResourceProvider(AmbariManagementController managementController) {
    super(Type.MpackRecommendation, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  protected String getRequestTypePropertyId() {
    return RECOMMEND_PROPERTY_ID;
  }

  @Override
  public RequestStatus createResources(final Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    MpackAdvisorRequest recommendationRequest = prepareMpackAdvisorRequest(request);

    final MpackRecommendationResponse response;
    try {
      response = maHelper.recommend(recommendationRequest);
    } catch (MpackAdvisorRequestException e) {
      LOG.warn("Error occured during recommendation", e);
      throw new IllegalArgumentException(e.getMessage(), e);
    } catch (MpackAdvisorException e) {
      LOG.warn("Error occured during recommendation", e);
      throw new SystemException(e.getMessage(), e);
    }

    Resource recommendation = createResources(new Command<Resource>() {
      @Override
      public Resource invoke() throws AmbariException {

        Resource resource = new ResourceImpl(Resource.Type.MpackRecommendation);
        setResourceProperty(resource, RECOMMENDATION_ID_PROPERTY_ID, response.getId(), getPropertyIds());
        setResourceProperty(resource, HOSTS_PROPERTY_ID, response.getHosts(), getPropertyIds());
        setResourceProperty(resource, SERVICES_PROPERTY_ID, response.getServices(),
            getPropertyIds());
        setResourceProperty(resource, BLUEPRINT_CONFIGURATIONS_PROPERTY_ID, response
            .getRecommendations().getBlueprint().getConfigurations(), getPropertyIds());

        Set<HostGroup> hostGroups = response.getRecommendations().getBlueprint().getHostGroups();
        List<Map<String, Object>> listGroupProps = new ArrayList<>();
        if (hostGroups != null) {
          for (HostGroup hostGroup : hostGroups) {
            Map<String, Object> mapGroupProps = new HashMap<>();
            mapGroupProps.put(BLUEPRINT_HOST_GROUPS_NAME_PROPERTY_ID, hostGroup.getName());
            mapGroupProps
                .put(BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY_ID, hostGroup.getComponents());
            listGroupProps.add(mapGroupProps);
          }
        }
        setResourceProperty(resource, BLUEPRINT_HOST_GROUPS_PROPERTY_ID, listGroupProps,
            getPropertyIds());

        // Update Mpack Instances
        Set<MpackRecommendationResponse.MpackInstance> mpackInstances  = response.getRecommendations().getBlueprint().getMpackInstances();
        List<Map<String, Object>> listMpackInstances = new ArrayList<>();
        if (mpackInstances != null) {
          for (MpackRecommendationResponse.MpackInstance mpackInstance : mpackInstances) {
            Map<String, Object> mpackInstanceMap = new HashMap<>();
            mpackInstanceMap.put(BLUEPRINT_MPACK_INSTANCES_NAME_PROPERTY_ID, mpackInstance.getName());
            mpackInstanceMap.put(BLUEPRINT_MPACK_INSTANCES_VERSION_PROPERTY_ID, mpackInstance.getVersion());

            // Service Instances
            List<Map<String, Object>> listServiceInstances = new ArrayList<>();
            for (MpackRecommendationResponse.ServiceInstance serviceInstance : mpackInstance.getServiceInstances()) {
              Map<String, Object> serviceInstanceMap = new HashMap<>();
              serviceInstanceMap.put(BLUEPRINT_MPACK_SERVICE_NAME_PROPERTY_ID, serviceInstance.getName());
              serviceInstanceMap.put(BLUEPRINT_MPACK_SERVICE_TYPE_PROPERTY_ID, serviceInstance.getType());

              // Service Instance Configs
              Map<String, Object> serviceConfigsMap = new HashMap<>();
              Map<String, MpackRecommendationResponse.BlueprintConfigurations> readServiceConfigs = serviceInstance.getConfigurations();
              for (String configBag : readServiceConfigs.keySet()) {
                MpackRecommendationResponse.BlueprintConfigurations bpConfig = readServiceConfigs.get(configBag);
                Map<String, String> bpConfigProperties = bpConfig.getProperties();
                Map<String, Object> propertiesMap = new HashMap<>();
                propertiesMap.put(BLUEPRINT_MPACK_SERVICE_CONFIG_PROPERTIES_PROPERTY_ID, bpConfigProperties);
                serviceConfigsMap.put(configBag, propertiesMap);
              }
              serviceInstanceMap.put(BLUEPRINT_MPACK_SERVICE_CONFIG_PROPERTY_ID, readServiceConfigs);
              listServiceInstances.add(serviceInstanceMap);
            }
            mpackInstanceMap.put(BLUEPRINT_MPACK_SERVICE_PROPERTY_ID, listServiceInstances);
            listMpackInstances.add(mpackInstanceMap);
          }
        }
        setResourceProperty(resource, BLUEPRINT_MPACK_INSTANCES_PROPERTY_ID, listMpackInstances,
            getPropertyIds());


        Set<BindingHostGroup> bindingHostGroups = response.getRecommendations()
            .getBlueprintClusterBinding().getHostGroups();
        List<Map<String, Object>> listBindingGroupProps = new ArrayList<>();
        for (BindingHostGroup hostGroup : bindingHostGroups) {
          Map<String, Object> mapGroupProps = new HashMap<>();
          mapGroupProps.put(BINDING_HOST_GROUPS_NAME_PROPERTY_ID, hostGroup.getName());
          mapGroupProps.put(BINDING_HOST_GROUPS_HOSTS_PROPERTY_ID, hostGroup.getHosts());
          listBindingGroupProps.add(mapGroupProps);
        }
        setResourceProperty(resource, BINDING_HOST_GROUPS_PROPERTY_ID, listBindingGroupProps,
            getPropertyIds());

        return resource;
      }
    });

    notifyCreate(Resource.Type.MpackRecommendation, request);

    Set<Resource> resources = new HashSet<>(Arrays.asList(recommendation));
    return new RequestStatusImpl(null, resources);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

}