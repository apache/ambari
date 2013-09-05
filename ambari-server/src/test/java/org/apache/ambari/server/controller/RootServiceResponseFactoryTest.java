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

package org.apache.ambari.server.controller;

import java.util.Collections;
import java.util.Set;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.RootServiceResponseFactory.Components;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RootServiceResponseFactoryTest {

  private AbstractRootServiceResponseFactory responseFactory;
  private Injector injector;
  
  @Inject
  Configuration configs;
  
  @Inject
  AmbariMetaInfo ambariMetaInfo;
  
  public class MockModule extends AbstractModule {
    
    @Override
    protected void configure() {
      AmbariMetaInfo ambariMetaInfo = mock(AmbariMetaInfo.class);
      bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
    }
  }
  
  @Before
  public void setUp() throws Exception{
    injector = Guice.createInjector(new InMemoryDefaultTestModule(), new MockModule());
    injector.injectMembers(this);
    responseFactory = injector.getInstance(RootServiceResponseFactory.class);
    
    when(ambariMetaInfo.getServerVersion()).thenReturn("1.2.3");
  }

  @Test
  public void testGetRootServices() throws Exception {

    // Request a null service name
    RootServiceRequest request = new RootServiceRequest(null);
    Set<RootServiceResponse> rootServices =
        responseFactory.getRootServices(request);
    assertEquals(RootServiceResponseFactory.Services.values().length,
        rootServices.size());

    // null request
    request = null;
    rootServices = responseFactory.getRootServices(request);
    assertEquals(RootServiceResponseFactory.Services.values().length,
        rootServices.size());

    // Request nonexistent service
    try {
      request = new RootServiceRequest("XXX");
      ;
      rootServices = responseFactory.getRootServices(request);
    } catch (Exception e) {
      assertTrue(e instanceof ObjectNotFoundException);
    }

    // Request existent service
    request =
        new RootServiceRequest(
            RootServiceResponseFactory.Services.AMBARI.name());
    ;
    rootServices = responseFactory.getRootServices(request);
    assertEquals(1, rootServices.size());
    assertTrue(rootServices.contains(new RootServiceResponse(
        RootServiceResponseFactory.Services.AMBARI.name())));
  }

  @Test
  public void testGetRootServiceComponents() throws Exception {

    // Request null service name, null component name
    RootServiceComponentRequest request =
        new RootServiceComponentRequest(null, null);
    Set<RootServiceComponentResponse> rootServiceComponents;
    try {
      rootServiceComponents = responseFactory.getRootServiceComponents(request);
    } catch (Exception e) {
      assertTrue(e instanceof ObjectNotFoundException);
    }

    // Request null service name, not-null component name
    request =
        new RootServiceComponentRequest(null,
            RootServiceResponseFactory.Components.AMBARI_SERVER.name());

    try {
      rootServiceComponents = responseFactory.getRootServiceComponents(request);
    } catch (Exception e) {
      assertTrue(e instanceof ObjectNotFoundException);
    }

    // Request existent service name, null component name
    request =
        new RootServiceComponentRequest(
            RootServiceResponseFactory.Services.AMBARI.name(), null);
    rootServiceComponents = responseFactory.getRootServiceComponents(request);
    assertEquals(
        RootServiceResponseFactory.Services.AMBARI.getComponents().length,
        rootServiceComponents.size());

    for (int i = 0; i < RootServiceResponseFactory.Services.AMBARI
        .getComponents().length; i++) {
      Components component =
          RootServiceResponseFactory.Services.AMBARI.getComponents()[i];

      if (component.name().equals(
          RootServiceResponseFactory.Components.AMBARI_SERVER.name()))
        assertTrue(rootServiceComponents
            .contains(new RootServiceComponentResponse(component.name(),
                ambariMetaInfo.getServerVersion(),
                configs.getAmbariProperties())));
      else
        assertTrue(rootServiceComponents
            .contains(new RootServiceComponentResponse(component.name(),
                RootServiceResponseFactory.NOT_APPLICABLE,
                Collections.<String, String> emptyMap())));
    }

    // Request existent service name, existent component name
    request =
        new RootServiceComponentRequest(
            RootServiceResponseFactory.Services.AMBARI.name(),
            RootServiceResponseFactory.Services.AMBARI.getComponents()[0]
                .name());
    rootServiceComponents = responseFactory.getRootServiceComponents(request);
    assertEquals(1, rootServiceComponents.size());
    assertTrue(rootServiceComponents.contains(new RootServiceComponentResponse(
        RootServiceResponseFactory.Services.AMBARI.getComponents()[0].name(),
        ambariMetaInfo.getServerVersion(),
        configs.getAmbariProperties())));
    
    // Request existent service name, and component, not belongs to requested service
    request =
        new RootServiceComponentRequest(
            RootServiceResponseFactory.Services.AMBARI.name(),
            "XXX");
    try {
      rootServiceComponents = responseFactory.getRootServiceComponents(request);
    } catch (Exception e) {
      assertTrue(e instanceof ObjectNotFoundException);
    }
  }
}