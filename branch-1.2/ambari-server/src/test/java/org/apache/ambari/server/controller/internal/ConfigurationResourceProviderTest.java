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

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ConfigurationResponse;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * Tests for the configuration resource provider.
 */
public class ConfigurationResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createConfiguration(AbstractResourceProviderTest.Matcher.getConfigurationRequest(
        "Cluster100", "type", "tag", new HashMap<String, String>()));

    // replay
    replay(managementController, response);

    ConfigurationResourceProvider provider = new ConfigurationResourceProvider(
        PropertyHelper.getPropertyIds(Resource.Type.Configuration ),
        PropertyHelper.getKeyPropertyIds(Resource.Type.Configuration),
        managementController);

    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(ConfigurationResourceProvider.CONFIGURATION_CLUSTER_NAME_PROPERTY_ID, "Cluster100");
    properties.put(ConfigurationResourceProvider.CONFIGURATION_CONFIG_TAG_PROPERTY_ID, "tag");
    properties.put(ConfigurationResourceProvider.CONFIGURATION_CONFIG_TYPE_PROPERTY_ID, "type");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.Configuration;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<ConfigurationResponse> allResponse = new HashSet<ConfigurationResponse>();
    allResponse.add(new ConfigurationResponse("Cluster100", "type", "tag1", null));
    allResponse.add(new ConfigurationResponse("Cluster100", "type", "tag2", null));
    allResponse.add(new ConfigurationResponse("Cluster100", "type", "tag3", null));

    // set expectations
    expect(managementController.getConfigurations(
        AbstractResourceProviderTest.Matcher.getConfigurationRequestSet(
            "Cluster100", null, null, Collections.<String, String>emptyMap()))).andReturn(allResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(ConfigurationResourceProvider.CONFIGURATION_CLUSTER_NAME_PROPERTY_ID);
    propertyIds.add(ConfigurationResourceProvider.CONFIGURATION_CONFIG_TAG_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(
        ConfigurationResourceProvider.CONFIGURATION_CLUSTER_NAME_PROPERTY_ID).equals("Cluster100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(3, resources.size());
    Set<String> tags = new HashSet<String>();
    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(
          ConfigurationResourceProvider.CONFIGURATION_CLUSTER_NAME_PROPERTY_ID);
      Assert.assertEquals("Cluster100", clusterName);
      tags.add((String) resource.getPropertyValue(
          ConfigurationResourceProvider.CONFIGURATION_CONFIG_TAG_PROPERTY_ID));
    }
    // Make sure that all of the response objects got moved into resources
    for (ConfigurationResponse response : allResponse ) {
      Assert.assertTrue(tags.contains(response.getVersionTag()));
    }

    // verify
    verify(managementController);

  }

  @Test
  public void testUpdateResources() throws Exception {
    Resource.Type type = Resource.Type.Configuration;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties);

    Predicate predicate = new PredicateBuilder().property(
        ConfigurationResourceProvider.CONFIGURATION_CONFIG_TAG_PROPERTY_ID).equals("Configuration100").toPredicate();

    try {
      provider.updateResources(request, predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(managementController, response);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.Configuration;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Predicate predicate = new PredicateBuilder().property(
        ConfigurationResourceProvider.CONFIGURATION_CONFIG_TAG_PROPERTY_ID).equals("Configuration100").toPredicate();
    try {
      provider.deleteResources(predicate);
      Assert.fail("Expected an UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }

    // verify
    verify(managementController);
  }
}
