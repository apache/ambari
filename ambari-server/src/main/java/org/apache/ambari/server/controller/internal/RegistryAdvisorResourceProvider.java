/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.registry.MpackEntry;
import org.apache.ambari.server.registry.RegistryAdvisor;
import org.apache.ambari.server.registry.RegistryAdvisorRequest;
import org.apache.ambari.server.registry.RegistryAdvisorRequest.RegistryAdvisorRequestBuilder;
import org.apache.ambari.server.registry.RegistryAdvisorRequest.RegistryAdvisorRequestType;

import com.google.inject.Inject;

/**
 * Abstract superclass for registry recommendations and validations.
 */
public abstract class RegistryAdvisorResourceProvider extends AbstractControllerResourceProvider {

  public static final String REGISTRY_ID =  PropertyHelper.getPropertyId("RegistryInfo", "registry_id");
  protected static final String SELECTED_SCENARIOS_PROPERTY_ID = "selected_scenarios";
  protected static final String SELECTED_MPACKS_PROPERTY_ID = "selected_mpacks";

  protected static RegistryAdvisor registryAdvisor;

  @Inject
  public static void init(RegistryAdvisor instance) {
    registryAdvisor = instance;
  }

  /**
   * Constructor
   * @param propertyIds           Property ids
   * @param keyPropertyIds        Key property ids
   * @param managementController  Ambari management controller instance
   */
  protected RegistryAdvisorResourceProvider(Set<String> propertyIds, Map<Resource.Type, String> keyPropertyIds,
    AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  /**
   * Get property containing {@link RegistryAdvisorRequestType}
   * @return  Property id
   */
  protected abstract String getRequestTypePropertyId();

  /**
   * Create registry advisor request instance
   * @param request {@link Request} input
   * @return  {@link RegistryAdvisorRequest} instance
   */
  protected RegistryAdvisorRequest createRegistryAdvisorRequest(Request request) {
    try {
      Long registryId = Long.valueOf((String) getRequestProperty(request, REGISTRY_ID));
      RegistryAdvisorRequestType requestType = RegistryAdvisorRequestType.fromString(
        (String) getRequestProperty(request, getRequestTypePropertyId()));
      List<String> selectedScenarios = (List<String>) getRequestProperty(request, SELECTED_SCENARIOS_PROPERTY_ID);
      List<MpackEntry> selectedMpacks = parseSelectedMpacksProperty(request);
      return RegistryAdvisorRequestBuilder.forRegistry(registryId)
        .ofType(requestType)
        .forScenarios(selectedScenarios)
        .forMpacks(selectedMpacks)
        .build();
    } catch (Exception e) {
      LOG.warn("Error occurred during preparation of registry advisor request", e);
      Response response = Response.status(Response.Status.BAD_REQUEST)
        .entity(String.format("Request body is not correct, error: %s", e.getMessage())).build();
      // TODO: Hosts and services must not be empty
      throw new WebApplicationException(response);
    }
  }

  /**
   * Parse selected mpacks property from the reuqest
   * @param request {@link Request} input
   * @return        List of selected mpacks
   */
  private List<MpackEntry> parseSelectedMpacksProperty(Request request) {
    List<MpackEntry> selectedMpacks = new LinkedList<>();request.getProperties();
    Set<Map<String, String>> selectedMpacksProperties =
      (Set<Map<String, String>>) getRequestProperty(request, SELECTED_MPACKS_PROPERTY_ID);
    if(selectedMpacksProperties != null) {
      for (Map<String, String> properties : selectedMpacksProperties) {
        String mpackName = properties.get("mpack_name");
        String mpackVersion = properties.get("mpack_version");
        MpackEntry mpackEntry = new MpackEntry(mpackName, mpackVersion);
        selectedMpacks.add(mpackEntry);
      }
    }
    return selectedMpacks;
  }

  /**
   * Get value of property in the request
   * @param request       {@link Request} input
   * @param propertyName  Property name
   * @return              Property value
   */
  protected Object getRequestProperty(Request request, String propertyName) {
    for (Map<String, Object> propertyMap : request.getProperties()) {
      if (propertyMap.containsKey(propertyName)) {
        return propertyMap.get(propertyName);
      }
    }
    return null;
  }

}
