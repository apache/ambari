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

package org.apache.ambari.server.controller;

import static org.apache.ambari.server.controller.AddServiceRequest.COMPONENTS;
import static org.apache.ambari.server.controller.AddServiceRequest.CONFIG_RECOMMENDATION_STRATEGY;
import static org.apache.ambari.server.controller.AddServiceRequest.Component;
import static org.apache.ambari.server.controller.AddServiceRequest.OperationType.ADD_SERVICE;
import static org.apache.ambari.server.controller.AddServiceRequest.PROVISION_ACTION;
import static org.apache.ambari.server.controller.AddServiceRequest.SERVICES;
import static org.apache.ambari.server.controller.AddServiceRequest.STACK_NAME;
import static org.apache.ambari.server.controller.AddServiceRequest.STACK_VERSION;
import static org.apache.ambari.server.controller.AddServiceRequest.Service;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_AND_START;
import static org.apache.ambari.server.controller.internal.ProvisionAction.INSTALL_ONLY;
import static org.apache.ambari.server.serveraction.kerberos.KerberosServerAction.OPERATION_TYPE;
import static org.apache.ambari.server.topology.ConfigRecommendationStrategy.ALWAYS_APPLY;
import static org.apache.ambari.server.topology.ConfigRecommendationStrategy.NEVER_APPLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.internal.ProvisionAction;
import org.apache.ambari.server.topology.ConfigRecommendationStrategy;
import org.apache.ambari.server.topology.Configurable;
import org.apache.ambari.server.topology.Configuration;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

/**
 * Tests for {@link AddServiceRequest} serialization / deserialization / syntax validation
 */
public class AddServiceRequestTest {

  private static String REQUEST_ALL_FIELDS_SET;
  private static String REQUEST_MINIMAL_SERVICES_AND_COMPONENTS;
  private static String REQUEST_MINIMAL_SERVICES_ONLY;
  private static String REQUEST_MINIMAL_COMPONENTS_ONLY;
  private static String REQUEST_INVALID_NO_OPERATION_TYPE;
  private static String REQUEST_INVALID_NO_SERVICES_AND_COMPONENTS;
  private static String REQUEST_INVALID_INVALID_FIELD;


  ObjectMapper mapper = new ObjectMapper();

  @BeforeClass
  public static void setUpClass() {
    REQUEST_ALL_FIELDS_SET = read("add_service_api/request1.json");
    REQUEST_MINIMAL_SERVICES_AND_COMPONENTS = read("add_service_api/request2.json");
    REQUEST_MINIMAL_SERVICES_ONLY = read("add_service_api/request3.json");
    REQUEST_MINIMAL_COMPONENTS_ONLY = read("add_service_api/request4.json");
    REQUEST_INVALID_NO_OPERATION_TYPE = read("add_service_api/request_invalid_1.json");
    REQUEST_INVALID_NO_SERVICES_AND_COMPONENTS = read("add_service_api/request_invalid_2.json");
    REQUEST_INVALID_INVALID_FIELD = read("add_service_api/request_invalid_3.json");
  }

  @Test
  public void testDeserialize_basic() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_ALL_FIELDS_SET, AddServiceRequest.class);

    assertEquals(ADD_SERVICE, request.getOperationType());
    assertEquals(ALWAYS_APPLY, request.getRecommendationStrategy());
    assertEquals(INSTALL_ONLY, request.getProvisionAction());
    assertEquals("HDP", request.getStackName());
    assertEquals("3.0", request.getStackVersion());

    Configuration configuration = request.getConfiguration();
    assertEquals(
      ImmutableMap.of("storm-site", ImmutableMap.of("final", ImmutableMap.of("fs.defaultFS", "true"))),
      configuration.getAttributes());
    assertEquals(
      ImmutableMap.of("storm-site", ImmutableMap.of("ipc.client.connect.max.retries", "50")),
      configuration.getProperties());

    assertEquals(
      ImmutableSet.of(Component.of("NIMBUS", "c7401.ambari.apache.org"), Component.of("BEACON_SERVER", "c7402.ambari.apache.org")),
      request.getComponents());

    assertEquals(
      ImmutableSet.of(Service.of("STORM"), Service.of("BEACON")),
      request.getServices());

  }

  @Test
  public void testDeserialize_defaultAndEmptyValues() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_MINIMAL_SERVICES_AND_COMPONENTS, AddServiceRequest.class);

    // filled-out values
    assertEquals(ADD_SERVICE, request.getOperationType());


    assertEquals(
      ImmutableSet.of(Component.of("NIMBUS", "c7401.ambari.apache.org"), Component.of("BEACON_SERVER", "c7402.ambari.apache.org")),
      request.getComponents());

    assertEquals(
      ImmutableSet.of(Service.of("STORM"), Service.of("BEACON")),
      request.getServices());

    // default / empty values
    assertEquals(NEVER_APPLY, request.getRecommendationStrategy());
    assertEquals(INSTALL_AND_START, request.getProvisionAction());
    assertNull(request.getStackName());
    assertNull(request.getStackVersion());

    Configuration configuration = request.getConfiguration();
    assertTrue(configuration.getFullAttributes().isEmpty());
    assertTrue(configuration.getFullProperties().isEmpty());
  }

  @Test
  public void testDeserialize_onlyServices() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_MINIMAL_SERVICES_ONLY, AddServiceRequest.class);

    // filled-out values
    assertEquals(ADD_SERVICE, request.getOperationType());

    assertEquals(
      ImmutableSet.of(Service.of("STORM"), Service.of("BEACON")),
      request.getServices());

    // default / empty values
    assertEquals(NEVER_APPLY, request.getRecommendationStrategy());
    assertEquals(INSTALL_AND_START, request.getProvisionAction());
    assertNull(request.getStackName());
    assertNull(request.getStackVersion());

    Configuration configuration = request.getConfiguration();
    assertTrue(configuration.getFullAttributes().isEmpty());
    assertTrue(configuration.getFullProperties().isEmpty());

    assertTrue(request.getComponents().isEmpty());
  }

  @Test
  public void testDeserialize_onlyComponents() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_MINIMAL_COMPONENTS_ONLY, AddServiceRequest.class);

    // filled-out values
    assertEquals(ADD_SERVICE, request.getOperationType());

    assertEquals(
      ImmutableSet.of(Component.of("NIMBUS", "c7401.ambari.apache.org"), Component.of("BEACON_SERVER", "c7402.ambari.apache.org")),
      request.getComponents());

    // default / empty values
    assertEquals(NEVER_APPLY, request.getRecommendationStrategy());
    assertEquals(INSTALL_AND_START, request.getProvisionAction());
    assertNull(request.getStackName());
    assertNull(request.getStackVersion());

    Configuration configuration = request.getConfiguration();
    assertTrue(configuration.getFullAttributes().isEmpty());
    assertTrue(configuration.getFullProperties().isEmpty());

    assertTrue(request.getServices().isEmpty());
  }

  @Test(expected = JsonProcessingException.class)
  public void testDeserialize_invalid_noOperationType() throws Exception {
    mapper.readValue(REQUEST_INVALID_NO_OPERATION_TYPE, AddServiceRequest.class);
  }

  @Test(expected = JsonProcessingException.class)
  public void testDeserialize_invalid_noServicesAndComponents() throws Exception {
    mapper.readValue(REQUEST_INVALID_NO_SERVICES_AND_COMPONENTS, AddServiceRequest.class);
  }

  @Test(expected = JsonProcessingException.class)
  public void testDeserialize_invalid_invalidField() throws Exception {
    mapper.readValue(REQUEST_INVALID_INVALID_FIELD, AddServiceRequest.class);
  }

  @Test
  public void testSerialize_basic() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_ALL_FIELDS_SET, AddServiceRequest.class);

    Map<String, ?> serialized = serialize(request);

    assertEquals(AddServiceRequest.OperationType.ADD_SERVICE.name(), serialized.get(OPERATION_TYPE));
    assertEquals(ConfigRecommendationStrategy.ALWAYS_APPLY.name(), serialized.get(CONFIG_RECOMMENDATION_STRATEGY));
    assertEquals(ProvisionAction.INSTALL_ONLY.name(), serialized.get(PROVISION_ACTION));
    assertEquals("HDP", serialized.get(STACK_NAME));
    assertEquals("3.0", serialized.get(STACK_VERSION));

    assertEquals(
      ImmutableSet.of(ImmutableMap.of(Service.NAME, "BEACON"), ImmutableMap.of(Service.NAME, "STORM")),
      ImmutableSet.copyOf((List<String>) serialized.get(SERVICES)) );

    assertEquals(
      ImmutableSet.of(
        ImmutableMap.of(Component.COMPONENT_NAME, "NIMBUS", Component.FQDN, "c7401.ambari.apache.org"),
        ImmutableMap.of(Component.COMPONENT_NAME, "BEACON_SERVER", Component.FQDN, "c7402.ambari.apache.org")),
      ImmutableSet.copyOf((List<String>) serialized.get(COMPONENTS)) );

    assertEquals(
      ImmutableList.of(
        ImmutableMap.of(
          "storm-site",
          ImmutableMap.of(
            "properties", ImmutableMap.of("ipc.client.connect.max.retries", "50"),
            "properties_attributes", ImmutableMap.of("final", ImmutableMap.of("fs.defaultFS", "true"))
          )
        )
      ),
      serialized.get(Configurable.CONFIGURATIONS)
    );
  }

  @Test
  public void testSerialize_EmptyOmitted() throws Exception {
    AddServiceRequest request = mapper.readValue(REQUEST_MINIMAL_SERVICES_ONLY, AddServiceRequest.class);
    Map<String, ?> serialized = serialize(request);

    assertEquals(AddServiceRequest.OperationType.ADD_SERVICE.name(), serialized.get(OPERATION_TYPE));
    assertEquals(ProvisionAction.INSTALL_AND_START.name(), serialized.get(PROVISION_ACTION));
    assertEquals(
      ImmutableSet.of(ImmutableMap.of(Service.NAME, "BEACON"), ImmutableMap.of(Service.NAME, "STORM")),
      ImmutableSet.copyOf((List<String>) serialized.get(SERVICES)) );

    assertFalse(serialized.containsKey(STACK_NAME));
    assertFalse(serialized.containsKey(STACK_VERSION));
    assertFalse(serialized.containsKey(Configurable.CONFIGURATIONS));
    assertFalse(serialized.containsKey(COMPONENTS));

  }

  private Map<String, ?> serialize(AddServiceRequest request) throws IOException {
    String serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
    return mapper.readValue(serialized, new TypeReference<Map<String, ?>>() {});
  }

  private static String read(String resourceName) {
    try {
      return Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}