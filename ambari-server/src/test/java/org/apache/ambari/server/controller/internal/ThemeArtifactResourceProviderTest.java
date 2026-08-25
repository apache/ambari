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

import static org.apache.ambari.server.controller.internal.ThemeArtifactResourceProvider.STACK_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.ThemeArtifactResourceProvider.STACK_SERVICE_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.ThemeArtifactResourceProvider.STACK_VERSION_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.ThemeArtifactResourceProvider.THEME_DATA_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.ThemeArtifactResourceProvider.THEME_DEFAULT_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.ThemeArtifactResourceProvider.THEME_FILE_NAME_PROPERTY_ID;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.stack.ThemeModule;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.ThemeInfo;
import org.apache.ambari.server.state.theme.Theme;
import org.junit.Before;
import org.junit.Test;

public class ThemeArtifactResourceProviderTest {

  private static final String STACK_NAME = "TEST";
  private static final String STACK_VERSION = "1.0";

  private final Request request = PropertyHelper.getReadRequest(ThemeArtifactResourceProvider.propertyIds);

  private ThemeArtifactResourceProvider provider;

  @Before
  public void setUp() throws Exception {
    ServiceInfo hdfs = service("HDFS",
      theme("alternate.json", false, "alternate"),
      theme("default.json", true, "default"));
    ServiceInfo yarn = service("YARN", theme("yarn.json", true, "yarn-default"));
    ServiceInfo empty = service("EMPTY");

    StackInfo stack = new StackInfo();
    stack.setName(STACK_NAME);
    stack.setVersion(STACK_VERSION);
    stack.setServices(Arrays.asList(hdfs, yarn, empty));

    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
    expect(metaInfo.getStack(anyString(), anyString())).andAnswer(() -> {
      Object[] arguments = getCurrentArguments();
      if (STACK_NAME.equals(arguments[0]) && STACK_VERSION.equals(arguments[1])) {
        return stack;
      }
      throw new AmbariException("Stack not found");
    }).anyTimes();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getAmbariMetaInfo()).andReturn(metaInfo).anyTimes();
    replay(metaInfo, managementController);

    provider = new ThemeArtifactResourceProvider(managementController);
  }

  @Test
  public void testGetNamedThemeProjectsExactThemeData() throws Exception {
    Set<Resource> resources = provider.getResources(request,
      serviceThemePredicate("HDFS", "default.json"));

    assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();
    assertEquals("default.json", resource.getPropertyValue(THEME_FILE_NAME_PROPERTY_ID));
    assertEquals(true, resource.getPropertyValue(THEME_DEFAULT_PROPERTY_ID));
    assertEquals(STACK_NAME, resource.getPropertyValue(STACK_NAME_PROPERTY_ID));
    assertEquals(STACK_VERSION, resource.getPropertyValue(STACK_VERSION_PROPERTY_ID));
    assertEquals("HDFS", resource.getPropertyValue(STACK_SERVICE_NAME_PROPERTY_ID));

    Theme theme = getLogicalTheme(resource);
    assertEquals("default", theme.getName());
    assertSame(theme,
      resource.getPropertiesMap().get(THEME_DATA_PROPERTY_ID).get(ThemeModule.THEME_KEY));
  }

  @Test
  public void testGetServiceThemesUsesFileKeysWithoutAssumingOrder() throws Exception {
    Set<Resource> resources = provider.getResources(request, servicePredicate("HDFS"));

    Map<String, Resource> resourcesByFile = resources.stream().collect(Collectors.toMap(
      resource -> (String) resource.getPropertyValue(THEME_FILE_NAME_PROPERTY_ID),
      Function.identity()));

    assertEquals(2, resourcesByFile.size());
    assertEquals(false, resourcesByFile.get("alternate.json").getPropertyValue(THEME_DEFAULT_PROPERTY_ID));
    assertEquals(true, resourcesByFile.get("default.json").getPropertyValue(THEME_DEFAULT_PROPERTY_ID));
    assertEquals("alternate", getLogicalTheme(resourcesByFile.get("alternate.json")).getName());
    assertEquals("default", getLogicalTheme(resourcesByFile.get("default.json")).getName());
  }

  @Test
  public void testDefaultTruePredicateReturnsOnlyDefaultTheme() throws Exception {
    Set<Resource> resources = provider.getResources(request, defaultPredicate("HDFS", true));

    assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();
    assertEquals("default.json", resource.getPropertyValue(THEME_FILE_NAME_PROPERTY_ID));
    assertEquals(true, resource.getPropertyValue(THEME_DEFAULT_PROPERTY_ID));
  }

  @Test
  public void testDefaultFalsePredicateReturnsOnlyNonDefaultTheme() throws Exception {
    Set<Resource> resources = provider.getResources(request, defaultPredicate("HDFS", false));

    assertEquals(1, resources.size());
    Resource resource = resources.iterator().next();
    assertEquals("alternate.json", resource.getPropertyValue(THEME_FILE_NAME_PROPERTY_ID));
    assertEquals(false, resource.getPropertyValue(THEME_DEFAULT_PROPERTY_ID));
  }

  @Test
  public void testGetThemesAcrossServicesFromBatchPredicate() throws Exception {
    Predicate predicate = new PredicateBuilder()
      .begin()
        .property(STACK_NAME_PROPERTY_ID).equals(STACK_NAME).and()
        .property(STACK_VERSION_PROPERTY_ID).equals(STACK_VERSION).and()
        .property(STACK_SERVICE_NAME_PROPERTY_ID).equals("HDFS")
      .end().or().begin()
        .property(STACK_NAME_PROPERTY_ID).equals(STACK_NAME).and()
        .property(STACK_VERSION_PROPERTY_ID).equals(STACK_VERSION).and()
        .property(STACK_SERVICE_NAME_PROPERTY_ID).equals("YARN")
      .end().toPredicate();

    Set<Resource> resources = provider.getResources(request, predicate);
    Set<String> serviceAndFileKeys = resources.stream()
      .map(resource -> resource.getPropertyValue(STACK_SERVICE_NAME_PROPERTY_ID) + "/"
        + resource.getPropertyValue(THEME_FILE_NAME_PROPERTY_ID))
      .collect(Collectors.toSet());

    assertEquals(3, resources.size());
    assertEquals(Set.of("HDFS/alternate.json", "HDFS/default.json", "YARN/yarn.json"), serviceAndFileKeys);
  }

  @Test
  public void testGetThemesAcrossWholeStackRetainsValidServices() throws Exception {
    Predicate predicate = new PredicateBuilder()
      .property(STACK_NAME_PROPERTY_ID).equals(STACK_NAME).and()
      .property(STACK_VERSION_PROPERTY_ID).equals(STACK_VERSION)
      .toPredicate();

    Set<Resource> resources = provider.getResources(request, predicate);
    Set<String> services = resources.stream()
      .map(resource -> (String) resource.getPropertyValue(STACK_SERVICE_NAME_PROPERTY_ID))
      .collect(Collectors.toSet());

    assertEquals(3, resources.size());
    assertEquals(Set.of("HDFS", "YARN"), services);
    assertFalse(services.contains("EMPTY"));
  }

  @Test(expected = NoSuchResourceException.class)
  public void testEmptyServiceReturnsNoResource() throws Exception {
    provider.getResources(request, servicePredicate("EMPTY"));
  }

  @Test(expected = NoSuchParentResourceException.class)
  public void testMissingStackReturnsNoSuchParent() throws Exception {
    Predicate predicate = new PredicateBuilder()
      .property(STACK_NAME_PROPERTY_ID).equals("MISSING").and()
      .property(STACK_VERSION_PROPERTY_ID).equals(STACK_VERSION)
      .toPredicate();

    provider.getResources(request, predicate);
  }

  @Test(expected = NoSuchParentResourceException.class)
  public void testMissingServiceReturnsNoSuchParent() throws Exception {
    provider.getResources(request, servicePredicate("MISSING"));
  }

  @Test(expected = NoSuchResourceException.class)
  public void testMissingNamedThemeReturnsNoSuchResource() throws Exception {
    provider.getResources(request, serviceThemePredicate("HDFS", "missing.json"));
  }

  @Test
  public void testPrimaryKeyIsThemeFileName() {
    assertEquals(Collections.singleton(THEME_FILE_NAME_PROPERTY_ID), provider.getPKPropertyIds());
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testCreateIsUnsupported() throws Exception {
    provider.createResources(request);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testUpdateIsUnsupported() throws Exception {
    provider.updateResources(request, servicePredicate("HDFS"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testDeleteIsUnsupported() throws Exception {
    provider.deleteResources(request, servicePredicate("HDFS"));
  }

  private Predicate servicePredicate(String serviceName) {
    return new PredicateBuilder()
      .property(STACK_NAME_PROPERTY_ID).equals(STACK_NAME).and()
      .property(STACK_VERSION_PROPERTY_ID).equals(STACK_VERSION).and()
      .property(STACK_SERVICE_NAME_PROPERTY_ID).equals(serviceName)
      .toPredicate();
  }

  private Predicate serviceThemePredicate(String serviceName, String fileName) {
    return new PredicateBuilder()
      .property(STACK_NAME_PROPERTY_ID).equals(STACK_NAME).and()
      .property(STACK_VERSION_PROPERTY_ID).equals(STACK_VERSION).and()
      .property(STACK_SERVICE_NAME_PROPERTY_ID).equals(serviceName).and()
      .property(THEME_FILE_NAME_PROPERTY_ID).equals(fileName)
      .toPredicate();
  }

  private Predicate defaultPredicate(String serviceName, boolean isDefault) {
    return new PredicateBuilder()
      .property(STACK_NAME_PROPERTY_ID).equals(STACK_NAME).and()
      .property(STACK_VERSION_PROPERTY_ID).equals(STACK_VERSION).and()
      .property(STACK_SERVICE_NAME_PROPERTY_ID).equals(serviceName).and()
      .property(THEME_DEFAULT_PROPERTY_ID).equals(isDefault)
      .toPredicate();
  }

  private Theme getLogicalTheme(Resource resource) {
    String logicalThemePropertyId = PropertyHelper.getPropertyId(THEME_DATA_PROPERTY_ID, ThemeModule.THEME_KEY);
    return (Theme) resource.getPropertyValue(logicalThemePropertyId);
  }

  private ServiceInfo service(String name, ThemeInfo... themes) {
    ServiceInfo service = new ServiceInfo();
    service.setName(name);
    Map<String, ThemeInfo> themesByFile = new LinkedHashMap<>();
    for (ThemeInfo theme : themes) {
      themesByFile.put(theme.getFileName(), theme);
    }
    service.setThemesMap(themesByFile);
    return service;
  }

  private ThemeInfo theme(String fileName, boolean isDefault, String logicalName) {
    Theme theme = new Theme();
    theme.setName(logicalName);

    ThemeInfo themeInfo = new ThemeInfo();
    themeInfo.setFileName(fileName);
    themeInfo.setIsDefault(isDefault);
    themeInfo.setThemeMap(Collections.singletonMap(ThemeModule.THEME_KEY, theme));
    return themeInfo;
  }
}
