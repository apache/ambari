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

import static org.apache.ambari.server.controller.internal.StackAdvisorResourceProvider.CONFIGURATIONS_PROPERTY_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.junit.Test;

public class StackAdvisorResourceProviderTest {

  @Test
  public void testCalculateConfigurations() throws Exception {

    Map<Resource.Type, String> keyPropertyIds = Collections.emptyMap();
    Set<String> propertyIds = Collections.emptySet();
    AmbariManagementController ambariManagementController = mock(AmbariManagementController.class);
    RecommendationResourceProvider provider = new RecommendationResourceProvider(propertyIds,
        keyPropertyIds, ambariManagementController);

    Request request = mock(Request.class);
    Set<Map<String, Object>> propertiesSet = new HashSet<Map<String, Object>>();
    Map<String, Object> propertiesMap = new HashMap<String, Object>();
    propertiesMap.put(CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", "string");
    List<Object> array = new ArrayList<Object>();
    array.add("array1");
    array.add("array2");
    propertiesMap.put(CONFIGURATIONS_PROPERTY_ID + "site/properties/array_prop", array);
    propertiesSet.add(propertiesMap);

    doReturn(propertiesSet).when(request).getProperties();

    Map<String, Map<String, Map<String, String>>> calculatedConfigurations = provider.calculateConfigurations(request);

    assertNotNull(calculatedConfigurations);
    assertEquals(1, calculatedConfigurations.size());
    Map<String, Map<String, String>> site = calculatedConfigurations.get("site");
    assertNotNull(site);
    assertEquals(1, site.size());
    Map<String, String> properties = site.get("properties");
    assertNotNull(properties);
    assertEquals(2, properties.size());
    assertEquals("string", properties.get("string_prop"));
    assertEquals("[array1, array2]", properties.get("array_prop"));
  }

  @Test
  public void testCalculateConfigurationsWithNullPropertyValues() throws Exception {

    Map<Resource.Type, String> keyPropertyIds = Collections.emptyMap();
    Set<String> propertyIds = Collections.emptySet();
    AmbariManagementController ambariManagementController = mock(AmbariManagementController.class);
    RecommendationResourceProvider provider = new RecommendationResourceProvider(propertyIds,
      keyPropertyIds, ambariManagementController);

    Request request = mock(Request.class);
    Set<Map<String, Object>> propertiesSet = new HashSet<Map<String, Object>>();
    Map<String, Object> propertiesMap = new HashMap<String, Object>();
    propertiesMap.put(CONFIGURATIONS_PROPERTY_ID + "site/properties/string_prop", null); //null value means no value specified for the property
    List<Object> array = new ArrayList<Object>();
    array.add("array1");
    array.add("array2");
    propertiesMap.put(CONFIGURATIONS_PROPERTY_ID + "site/properties/array_prop", array);
    propertiesSet.add(propertiesMap);

    doReturn(propertiesSet).when(request).getProperties();

    Map<String, Map<String, Map<String, String>>> calculatedConfigurations = provider.calculateConfigurations(request);

    assertNotNull(calculatedConfigurations);
    assertEquals(1, calculatedConfigurations.size());
    Map<String, Map<String, String>> site = calculatedConfigurations.get("site");
    assertNotNull(site);
    assertEquals(1, site.size());
    Map<String, String> properties = site.get("properties");
    assertNotNull(properties);

    assertEquals("[array1, array2]", properties.get("array_prop"));


    // config properties with null values should be ignored
    assertFalse(properties.containsKey("string_prop"));

  }
}
