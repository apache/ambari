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
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
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

    managementController.createConfiguration(AbstractResourceProviderTest.Matchers.configurationRequest(
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
}
