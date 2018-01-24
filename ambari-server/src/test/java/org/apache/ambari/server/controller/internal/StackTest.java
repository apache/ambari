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

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.ambari.server.controller.StackLevelConfigurationResponse;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.PropertyDependencyInfo;
import org.apache.ambari.server.state.PropertyInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.ValueAttributesInfo;
import org.apache.ambari.server.topology.Configuration;
import org.apache.commons.lang3.tuple.Pair;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Stack unit tests.
 */
public class StackTest {

  private static final String STACK_CONFIG_TYPE = "cluster-env";
  private static final String STACK_CONFIG_FILE = STACK_CONFIG_TYPE + ".xml";
  private static final String SERVICE_CONFIG_TYPE = "test-site";
  private static final String SERVICE_CONFIG_FILE = SERVICE_CONFIG_TYPE + ".xml";

  private StackInfo stackInfo;
  private ServiceInfo serviceInfo;
  private ComponentInfo componentInfo;
  private PropertyInfo serviceLevelProperty;
  private PropertyInfo stackLevelProperty;
  private PropertyInfo optionalServiceLevelProperty;
  private PropertyInfo passwordProperty;

  @Before
  public void setUp() {
    stackInfo = new StackInfo();
    stackInfo.setName("stack name");
    stackInfo.setVersion("1.0");

    serviceInfo = new ServiceInfo();
    serviceInfo.setName("some service");
    stackInfo.getServices().add(serviceInfo);

    componentInfo = new ComponentInfo();
    componentInfo.setName("some component");
    serviceInfo.getComponents().add(componentInfo);

    serviceLevelProperty = new PropertyInfo();
    serviceLevelProperty.setName("service_level");
    serviceLevelProperty.setValue("service-level value");
    serviceLevelProperty.setFilename(SERVICE_CONFIG_FILE);
    serviceLevelProperty.setRequireInput(true);
    serviceLevelProperty.getPropertyTypes().add(PropertyInfo.PropertyType.TEXT);
    serviceInfo.getProperties().add(serviceLevelProperty);

    passwordProperty = new PropertyInfo();
    passwordProperty.setName("a_password");
    passwordProperty.setValue("secret");
    passwordProperty.setFilename(SERVICE_CONFIG_FILE);
    passwordProperty.setRequireInput(true);
    passwordProperty.getPropertyTypes().add(PropertyInfo.PropertyType.PASSWORD);
    serviceInfo.getProperties().add(passwordProperty);

    optionalServiceLevelProperty = new PropertyInfo();
    optionalServiceLevelProperty.setName("optional_service_level");
    optionalServiceLevelProperty.setValue("service-level value (optional)");
    optionalServiceLevelProperty.setFilename(SERVICE_CONFIG_FILE);
    optionalServiceLevelProperty.setRequireInput(false);
    optionalServiceLevelProperty.getPropertyTypes().add(PropertyInfo.PropertyType.USER);
    serviceInfo.getProperties().add(optionalServiceLevelProperty);

    stackLevelProperty = new PropertyInfo();
    stackLevelProperty.setName("stack_level");
    stackLevelProperty.setValue("stack-level value");
    stackLevelProperty.setFilename(STACK_CONFIG_FILE);
    stackLevelProperty.setRequireInput(true);
    stackLevelProperty.getPropertyTypes().add(PropertyInfo.PropertyType.TEXT);
    stackInfo.getProperties().add(stackLevelProperty);
  }

  @Test
  public void stackHasCorrectNameAndVersion() throws Exception {
    // GIVEN
    Stack stack = new Stack(stackInfo);

    // THEN
    assertEquals(stackInfo.getName(), stack.getName());
    assertEquals(stackInfo.getVersion(), stack.getVersion());
  }

  @Test
  public void getServices() throws Exception {
    // GIVEN
    Stack stack = new Stack(stackInfo);

    // WHEN
    Collection<String> services = stack.getServices();

    // THEN
    assertEquals(ImmutableSet.of(serviceInfo.getName()), ImmutableSet.copyOf(services));
  }

  @Test
  public void configTypeOmitsFileExtension() throws Exception {
    // GIVEN
    Stack stack = new Stack(stackInfo);

    // WHEN
    Configuration configuration = stack.getConfiguration(singleton(serviceInfo.getName()));

    // THEN
    assertEquals(serviceLevelProperty.getValue(), configuration.getProperties().get(SERVICE_CONFIG_TYPE).get(serviceLevelProperty.getName()));
  }

  @Test
  public void getRequiredPropertiesForService() throws Exception {
    // GIVEN
    Stack stack = new Stack(stackInfo);

    // WHEN
    Collection<Stack.ConfigProperty> requiredConfigurationProperties = stack.getRequiredConfigurationProperties(serviceInfo.getName());

    // THEN
    // should include stack-level property
    // should exclude optional property
    Set<Pair<String, String>> actualRequiredProperties = convertToPropertySet(requiredConfigurationProperties);
    Set<Pair<String, String>> expected = ImmutableSet.of(
      Pair.of(STACK_CONFIG_TYPE, stackLevelProperty.getName()),
      Pair.of(SERVICE_CONFIG_TYPE, passwordProperty.getName()),
      Pair.of(SERVICE_CONFIG_TYPE, serviceLevelProperty.getName())
    );
    assertEquals(expected, actualRequiredProperties);
    assertEquals(expected.size(), requiredConfigurationProperties.size());
  }

  @Test
  public void getRequiredPropertiesForServiceAndType() throws Exception {
    // GIVEN
    Stack stack = new Stack(stackInfo);

    // WHEN
    Collection<Stack.ConfigProperty> requiredConfigurationProperties = stack.getRequiredConfigurationProperties(serviceInfo.getName(), PropertyInfo.PropertyType.TEXT);

    // THEN
    Set<Pair<String, String>> actualRequiredProperties = convertToPropertySet(requiredConfigurationProperties);
    Set<Pair<String, String>> expected = ImmutableSet.of(
      Pair.of(STACK_CONFIG_TYPE, stackLevelProperty.getName()),
      Pair.of(SERVICE_CONFIG_TYPE, serviceLevelProperty.getName())
    );
    assertEquals(expected, actualRequiredProperties);
    assertEquals(expected.size(), requiredConfigurationProperties.size());
  }

  @Test
  public void testConfigPropertyReadsInDependencies() throws Exception {
    // FIXME get rid of mock
    EasyMockSupport mockSupport = new EasyMockSupport();

    Set<PropertyDependencyInfo> setOfDependencyInfo = new HashSet<>();

    StackLevelConfigurationResponse mockResponse = mockSupport.createMock(StackLevelConfigurationResponse.class);
    expect(mockResponse.getPropertyName()).andReturn("test-property-one");
    expect(mockResponse.getPropertyValue()).andReturn("test-value-one");
    expect(mockResponse.getPropertyAttributes()).andReturn(Collections.emptyMap());
    expect(mockResponse.getPropertyType()).andReturn(Collections.emptySet());
    expect(mockResponse.getType()).andReturn("test-type-one");
    expect(mockResponse.getDependsOnProperties()).andReturn(setOfDependencyInfo);
    expect(mockResponse.getPropertyValueAttributes()).andReturn(new ValueAttributesInfo());

    mockSupport.replayAll();

    Stack.ConfigProperty configProperty =
      new Stack.ConfigProperty(mockResponse);

    assertSame("DependencyInfo was not properly parsed from the stack response object",
          setOfDependencyInfo, configProperty.getDependsOnProperties());


    mockSupport.verifyAll();

  }

  @Test
  public void getAllConfigurationTypesReturnsExcludedOnesToo() throws Exception {
    // GIVEN
    serviceInfo.setExcludedConfigTypes(ImmutableSet.of(SERVICE_CONFIG_TYPE));
    Stack stack = new Stack(stackInfo);

    // WHEN
    Collection<String> allConfigurationTypes = stack.getAllConfigurationTypes(serviceInfo.getName());

    // THEN
    Set<String> expected = ImmutableSet.of(SERVICE_CONFIG_TYPE, STACK_CONFIG_TYPE);
    assertEquals(expected, allConfigurationTypes);
  }

  @Test
  public void getConfigurationTypesOmitsExcludedOnes() throws Exception {
    // GIVEN
    serviceInfo.setExcludedConfigTypes(ImmutableSet.of(SERVICE_CONFIG_TYPE));
    Stack stack = new Stack(stackInfo);

    // WHEN
    Collection<String> allConfigurationTypes = stack.getConfigurationTypes(serviceInfo.getName());

    // THEN
    Set<String> expected = ImmutableSet.of(STACK_CONFIG_TYPE);
    assertEquals(expected, allConfigurationTypes);
  }

  @Test
  public void findsServiceForValidConfigType() {
    // GIVEN
    Stack stack = new Stack(stackInfo);

    // WHEN
    String service = stack.getServiceForConfigType(SERVICE_CONFIG_TYPE);

    // THEN
    assertEquals(serviceInfo.getName(), service);
  }

  @Test(expected = IllegalArgumentException.class) // THEN
  public void serviceIsNotFoundForExcludedConfigType() {
    // GIVEN
    serviceInfo.setExcludedConfigTypes(ImmutableSet.of(SERVICE_CONFIG_TYPE));
    Stack stack = new Stack(stackInfo);

    // WHEN
    stack.getServiceForConfigType(SERVICE_CONFIG_TYPE);
  }

  @Test(expected = IllegalArgumentException.class) // THEN
  public void serviceIsNotFoundForUnknownConfigType() {
    // GIVEN
    Stack stack = new Stack(stackInfo);

    // WHEN
    stack.getServiceForConfigType("no_such_config_type");
  }

  @Test
  public void findsAllServicesForValidConfigType() {
    // GIVEN
    ServiceInfo otherMatchingService = new ServiceInfo();
    otherMatchingService.setName("matches");
    Stack stack = new Stack(stackInfo);

    // WHEN
    Stream<String> services = stack.getServicesForConfigType(SERVICE_CONFIG_TYPE);

    // THEN
    Set<String> expected = ImmutableSet.of(serviceInfo.getName());
    assertEquals(expected, services.collect(toSet()));
  }

  @Test
  public void noServiceFoundForExcludedConfigType() {
    // GIVEN
    serviceInfo.setExcludedConfigTypes(ImmutableSet.of(SERVICE_CONFIG_TYPE));
    Stack stack = new Stack(stackInfo);

    // WHEN
    Stream<String> services = stack.getServicesForConfigType(SERVICE_CONFIG_TYPE);

    // THEN
    assertEquals(emptySet(), services.collect(toSet()));
  }

  @Test
  public void noServiceFoundForUnknownConfigType() {
    // GIVEN
    Stack stack = new Stack(stackInfo);

    // WHEN
    Stream<String> services = stack.getServicesForConfigType("no_such_config_type");

    // THEN
    assertEquals(emptySet(), services.collect(toSet()));
  }

  private static Set<Pair<String, String>> convertToPropertySet(Collection<Stack.ConfigProperty> requiredConfigurationProperties) {
    return requiredConfigurationProperties
      .stream().map(p -> Pair.of(p.getType(), p.getName())).collect(toSet());
  }
}
