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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequestException;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.BindingHostGroup;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.HostGroup;
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

public class RecommendationResourceProvider extends StackAdvisorResourceProvider {

  protected static final String RECOMMENDATION_ID_PROPERTY_ID = PropertyHelper.getPropertyId(
      "Recommendation", "id");

  protected static final String HOSTS_PROPERTY_ID = "hosts";
  protected static final String SERVICES_PROPERTY_ID = "services";
  protected static final String RECOMMEND_PROPERTY_ID = "recommend";

  protected static final String CONFIG_GROUPS_PROPERTY_ID = PropertyHelper
      .getPropertyId("recommendations", "config-groups");

  protected static final String BLUEPRINT_CONFIGURATIONS_PROPERTY_ID = PropertyHelper
      .getPropertyId("recommendations/blueprint", "configurations");

  protected static final String BLUEPRINT_HOST_GROUPS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "recommendations/blueprint", "host_groups");
  protected static final String BLUEPRINT_HOST_GROUPS_NAME_PROPERTY_ID = "name";
  protected static final String BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY_ID = "components";

  protected static final String BINDING_HOST_GROUPS_PROPERTY_ID = PropertyHelper.getPropertyId(
      "recommendations/blueprint_cluster_binding", "host_groups");
  protected static final String BINDING_HOST_GROUPS_NAME_PROPERTY_ID = "name";
  protected static final String BINDING_HOST_GROUPS_HOSTS_PROPERTY_ID = "hosts";

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[] { RECOMMENDATION_ID_PROPERTY_ID }));

  protected RecommendationResourceProvider(Set<String> propertyIds,
      Map<Type, String> keyPropertyIds, AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  protected String getRequestTypePropertyId() {
    return RECOMMEND_PROPERTY_ID;
  }

  @Override
  public RequestStatus createResources(final Request request) throws SystemException,
      UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    StackAdvisorRequest recommendationRequest = prepareStackAdvisorRequest(request);

    final RecommendationResponse response;
    try {
      response = saHelper.recommend(recommendationRequest);
    } catch (StackAdvisorRequestException e) {
      LOG.warn("Error occured during recommendation", e);
      throw new IllegalArgumentException(e.getMessage(), e);
    } catch (StackAdvisorException e) {
      LOG.warn("Error occured during recommendation", e);
      throw new SystemException(e.getMessage(), e);
    }

    Resource recommendation = createResources(new Command<Resource>() {
      @Override
      public Resource invoke() throws AmbariException {

        Resource resource = new ResourceImpl(Resource.Type.Recommendation);
        setResourceProperty(resource, RECOMMENDATION_ID_PROPERTY_ID, response.getId(), getPropertyIds());
        setResourceProperty(resource, STACK_NAME_PROPERTY_ID, response.getVersion().getStackName(),
            getPropertyIds());
        setResourceProperty(resource, STACK_VERSION_PROPERTY_ID, response.getVersion()
            .getStackVersion(), getPropertyIds());
        setResourceProperty(resource, HOSTS_PROPERTY_ID, response.getHosts(), getPropertyIds());
        setResourceProperty(resource, SERVICES_PROPERTY_ID, response.getServices(),
            getPropertyIds());
        setResourceProperty(resource, CONFIG_GROUPS_PROPERTY_ID,
          response.getRecommendations().getConfigGroups(), getPropertyIds());
        setResourceProperty(resource, BLUEPRINT_CONFIGURATIONS_PROPERTY_ID, response
            .getRecommendations().getBlueprint().getConfigurations(), getPropertyIds());

        Set<HostGroup> hostGroups = response.getRecommendations().getBlueprint().getHostGroups();
        List<Map<String, Object>> listGroupProps = new ArrayList<Map<String, Object>>();
        for (HostGroup hostGroup : hostGroups) {
          Map<String, Object> mapGroupProps = new HashMap<String, Object>();
          mapGroupProps.put(BLUEPRINT_HOST_GROUPS_NAME_PROPERTY_ID, hostGroup.getName());
          mapGroupProps
              .put(BLUEPRINT_HOST_GROUPS_COMPONENTS_PROPERTY_ID, hostGroup.getComponents());
          listGroupProps.add(mapGroupProps);
        }
        setResourceProperty(resource, BLUEPRINT_HOST_GROUPS_PROPERTY_ID, listGroupProps,
            getPropertyIds());

        Set<BindingHostGroup> bindingHostGroups = response.getRecommendations()
            .getBlueprintClusterBinding().getHostGroups();
        List<Map<String, Object>> listBindingGroupProps = new ArrayList<Map<String, Object>>();
        for (BindingHostGroup hostGroup : bindingHostGroups) {
          Map<String, Object> mapGroupProps = new HashMap<String, Object>();
          mapGroupProps.put(BINDING_HOST_GROUPS_NAME_PROPERTY_ID, hostGroup.getName());
          mapGroupProps.put(BINDING_HOST_GROUPS_HOSTS_PROPERTY_ID, hostGroup.getHosts());
          listBindingGroupProps.add(mapGroupProps);
        }
        setResourceProperty(resource, BINDING_HOST_GROUPS_PROPERTY_ID, listBindingGroupProps,
            getPropertyIds());

        return resource;
      }
    });
    notifyCreate(Resource.Type.Recommendation, request);

    Set<Resource> resources = new HashSet<Resource>(Arrays.asList(recommendation));
    return new RequestStatusImpl(null, resources);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

}
