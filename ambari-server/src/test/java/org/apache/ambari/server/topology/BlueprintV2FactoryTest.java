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
package org.apache.ambari.server.topology;

import static org.easymock.EasyMock.anyString;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.controller.StackV2Factory;
import org.apache.ambari.server.state.StackId;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

public class BlueprintV2FactoryTest {

  static String BLUEPRINTV2_JSON;
  static String BLUEPRINTV2_2_JSON;

  BlueprintV2Factory blueprintFactory;

  @BeforeClass
  public static void setUpClass() throws Exception {
    BLUEPRINTV2_JSON = Resources.toString(Resources.getResource("blueprintv2/blueprintv2.json"), Charsets.UTF_8);
    BLUEPRINTV2_2_JSON = Resources.toString(Resources.getResource("blueprintv2/blueprintv2_2.json"), Charsets.UTF_8);
  }

  @Before
  public void setUp() throws Exception {
    StackV2Factory stackFactory = mock(StackV2Factory.class);
    when(stackFactory.create(any(StackId.class), anyString())).thenAnswer(invocation -> {
      StackId stackId = invocation.getArgumentAt(0, StackId.class);
      return new StackV2(stackId.getStackName(), stackId.getStackVersion(), invocation.getArgumentAt(1, String.class),
        new HashMap<>(), new HashMap<>(), new HashMap<>(),
        new HashMap<>(), new HashMap<>(), new HashMap<>(),
        new HashMap<>(), new HashMap<>(), new HashMap<>());
    });
    blueprintFactory = BlueprintV2Factory.create(stackFactory);
    blueprintFactory.setPrettyPrintJson(true);
  }

  @Test
  public void testSerialization_parseJsonAsBlueprint() throws Exception {
    BlueprintV2 bp = blueprintFactory.convertFromJson(BLUEPRINTV2_JSON);
    assertEquals(new StackId("HDPCORE", "3.0.0"),
      bp.getServiceGroups().iterator().next().getServices().iterator().next().getStack().getStackId());
    assertEquals(2, bp.getStackIds().size());
    assertEquals(7, bp.getAllServiceIds().size());
    assertEquals(2, bp.getServiceGroups().size());
  }

  @Test
  public void testSerialization_parseJsonAsMap() throws Exception {
    ObjectMapper mapper = blueprintFactory.getObjectMapper();
    Map<String, Object> blueprintAsMap = mapper.readValue(BLUEPRINTV2_JSON, HashMap.class);
    assertEquals(2, getAsMap(blueprintAsMap, "cluster_settings").size());
    assertEquals(2, getAsMap(blueprintAsMap, "Blueprints").size());
    assertEquals("blueprint-def", getByPath(blueprintAsMap,
      ImmutableList.of("Blueprints", "blueprint_name")));
    assertEquals(2, getAsList(blueprintAsMap, "service_groups").size());
    assertEquals("StreamSG", getByPath(blueprintAsMap,
      ImmutableList.of("service_groups", 1, "name")));
    assertEquals(2, getAsList(blueprintAsMap, "repository_versions").size());
    assertEquals(1, getAsList(blueprintAsMap, "host_groups").size());
    assertEquals("host_group_1", getByPath(blueprintAsMap,
      ImmutableList.of("host_groups", 0, "name")));
    System.out.println(blueprintAsMap);
  }

  @Test
  public void testSerialization_serializeBlueprint() throws Exception {
    BlueprintV2 bp = blueprintFactory.convertFromJson(BLUEPRINTV2_JSON);
    String serialized = blueprintFactory.convertToJson(bp);
    // Test that serialized blueprint can be read again
    bp = blueprintFactory.convertFromJson(serialized);
    assertEquals(2, bp.getStackIds().size());
    assertEquals(7, bp.getAllServiceIds().size());
    assertEquals(2, bp.getServiceGroups().size());
  }

  @Test
  public void testSerialization2_parseJsonAsBlueprint() throws Exception {
    BlueprintV2 bp = blueprintFactory.convertFromJson(BLUEPRINTV2_2_JSON);
    assertEquals(new StackId("HDP", "3.0.0"),
      bp.getServiceGroups().iterator().next().getServices().iterator().next().getStack().getStackId());
    assertEquals(1, bp.getStackIds().size());
    assertEquals(4, bp.getAllServiceIds().size());
    assertEquals(1, bp.getServiceGroups().size());
  }

  @Test
  public void testSerialization2_parseJsonAsMap() throws Exception {
    ObjectMapper mapper = blueprintFactory.getObjectMapper();
    Map<String, Object> blueprintAsMap = mapper.readValue(BLUEPRINTV2_2_JSON, HashMap.class);
    assertEquals(2, getAsMap(blueprintAsMap, "cluster_settings").size());
    assertEquals(2, getAsMap(blueprintAsMap, "Blueprints").size());
    assertEquals("blueprint-def", getByPath(blueprintAsMap,
      ImmutableList.of("Blueprints", "blueprint_name")));
    assertEquals(1, getAsList(blueprintAsMap, "service_groups").size());
    assertEquals("CoreSG", getByPath(blueprintAsMap,
      ImmutableList.of("service_groups", 0, "name")));
    assertEquals(1, getAsList(blueprintAsMap, "repository_versions").size());
    assertEquals(1, getAsList(blueprintAsMap, "host_groups").size());
    assertEquals("host_group_1", getByPath(blueprintAsMap,
      ImmutableList.of("host_groups", 0, "name")));
    System.out.println(blueprintAsMap);
  }

  @Test
  public void testSerialization2_serializeBlueprint() throws Exception {
    BlueprintV2 bp = blueprintFactory.convertFromJson(BLUEPRINTV2_2_JSON);
    String serialized = blueprintFactory.convertToJson(bp);
    // Test that serialized blueprint can be read again
    bp = blueprintFactory.convertFromJson(serialized);
    assertEquals(1, bp.getStackIds().size());
    assertEquals(4, bp.getAllServiceIds().size());
    assertEquals(1, bp.getServiceGroups().size());
  }

  private static Map<String, Object> getAsMap(Map<String, Object> parentMap, String key) {
    return (Map<String, Object>)parentMap.get(key);
  }

  private static List<Object> getAsList(Map<String, Object> parentMap, String key) {
    return (List<Object>)parentMap.get(key);
  }

  private static Object getByPath(Map<String, Object> initialMap, List<Object> path) {
    Object returnValue = initialMap;
    for(Object key: path) {
      if (key instanceof String) { // this element is a map
        returnValue = ((Map<String, Object>)returnValue).get(key);
        Preconditions.checkNotNull(returnValue, "No value for key: " + key);
      }
      else if (key instanceof Integer) { // this element is an arraylist
        returnValue = ((List<Object>)returnValue).get((Integer)key);
      }
      else {
        throw new IllegalArgumentException("Invalid path element: " + key);
      }
    }
    return returnValue;
  }

}