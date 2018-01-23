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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class RootServiceResponseFactoryTest {

  @Inject
  private RootServiceResponseFactory responseFactory;
  private Injector injector;

  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testGetRootServices() throws Exception {
    // Request a null service name
    RootServiceRequest request = new RootServiceRequest(null);
    Set<RootServiceResponse> rootServices = responseFactory.getRootServices(request);
    assertEquals(RootService.values().length,
        rootServices.size());

    // null request
    request = null;
    rootServices = responseFactory.getRootServices(request);
    assertEquals(RootService.values().length,
        rootServices.size());

    // Request nonexistent service
    try {
      request = new RootServiceRequest("XXX");
      rootServices = responseFactory.getRootServices(request);
    } catch (Exception e) {
      assertTrue(e instanceof ObjectNotFoundException);
    }

    // Request existent service
    request = new RootServiceRequest(
        RootService.AMBARI.name());

    rootServices = responseFactory.getRootServices(request);
    assertEquals(1, rootServices.size());
    assertTrue(rootServices.contains(new RootServiceResponse(
        RootService.AMBARI.name())));
  }

  @Test
  public void testGetRootServiceComponents() throws Exception {
    // Request null service name, null component name
    RootServiceComponentRequest request = new RootServiceComponentRequest(null,
        null);

    Set<RootServiceComponentResponse> rootServiceComponents;
    try {
      rootServiceComponents = responseFactory.getRootServiceComponents(request);
    } catch (Exception e) {
      assertTrue(e instanceof ObjectNotFoundException);
    }

    RootComponent ambariServerComponent = RootComponent.AMBARI_SERVER;

    // Request null service name, not-null component name
    request = new RootServiceComponentRequest(null, ambariServerComponent.name());

    try {
      rootServiceComponents = responseFactory.getRootServiceComponents(request);
    } catch (Exception e) {
      assertTrue(e instanceof ObjectNotFoundException);
    }

    // Request existent service name, null component name
    String serviceName = RootService.AMBARI.name();
    request = new RootServiceComponentRequest(serviceName, null);

    rootServiceComponents = responseFactory.getRootServiceComponents(request);
    assertEquals(
        RootService.AMBARI.getComponents().length,
        rootServiceComponents.size());

    String ambariVersion = ambariMetaInfo.getServerVersion();

    for (int i = 0; i < RootService.AMBARI.getComponents().length; i++) {
      RootComponent component = RootService.AMBARI.getComponents()[i];

      if (component.name().equals(ambariServerComponent.name())) {
        for (RootServiceComponentResponse response : rootServiceComponents) {
          if (response.getComponentName().equals(ambariServerComponent.name())) {
            assertEquals(ambariVersion, response.getComponentVersion());
            assertEquals(1, response.getProperties().size(), 1);
            assertTrue(response.getProperties().containsKey("jdk_location"));
          }
        }
      } else {
        assertTrue(rootServiceComponents.contains(new RootServiceComponentResponse(
            serviceName, component.name(), RootServiceResponseFactory.NOT_APPLICABLE,
            Collections.emptyMap())));
      }
    }

    // Request existent service name, existent component name
    request = new RootServiceComponentRequest(
        RootService.AMBARI.name(),
        RootService.AMBARI.getComponents()[0].name());

    rootServiceComponents = responseFactory.getRootServiceComponents(request);
    assertEquals(1, rootServiceComponents.size());
    for (RootServiceComponentResponse response : rootServiceComponents) {
      if (response.getComponentName().equals(
          RootService.AMBARI.getComponents()[0].name())) {
        assertEquals(ambariVersion, response.getComponentVersion());
        assertEquals(2, response.getProperties().size());
        assertTrue(response.getProperties().containsKey("jdk_location"));
        assertTrue(response.getProperties().containsKey("java.version"));
      }
    }

    // Request existent service name, and component, not belongs to requested
    // service
    request = new RootServiceComponentRequest(
        RootService.AMBARI.name(), "XXX");
    
    try {
      rootServiceComponents = responseFactory.getRootServiceComponents(request);
    } catch (Exception e) {
      assertTrue(e instanceof ObjectNotFoundException);
    }
  }
}
