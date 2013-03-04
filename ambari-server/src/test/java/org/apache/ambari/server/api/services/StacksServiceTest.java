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

package org.apache.ambari.server.api.services;

import org.apache.ambari.server.api.handlers.RequestHandler;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.junit.Test;

import javax.ws.rs.core.Response;

/**
 * Unit tests for StacksService.
 */
public class StacksServiceTest extends BaseServiceTest {

  private static final String STACK_NAME = "stackName";
  private static final String STACK_VERSION = "stackVersion";
  private static final String OS_TYPE = "osType";
  private static final String REPO_ID = "repoId";
  private static final String SERVICE_NAME = "serviceName";
  private static final String PROPERTY_NAME = "propertyName";
  private static final String COMPONENT_NAME = "componentName";

  @Test
  public void testGetStacks() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStacks(getHttpHeaders(), getUriInfo());
    verifyResults(response, 200);
  }

  @Test
  public void testGetStacks__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStacks(getHttpHeaders(), getUriInfo());
    verifyResults(response, 500);
  }

  @Test
  public void testGetStack() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStack(getHttpHeaders(), getUriInfo(),
        STACK_NAME);
    verifyResults(response, 200);
  }

  @Test
  public void testGetStack__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStack(getHttpHeaders(), getUriInfo(),
        STACK_NAME);
    verifyResults(response, 500);
  }

  @Test
  public void testGetStackVersion() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getStackVersion(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION);
    verifyResults(response, 200);
  }

  @Test
  public void testGetStackVersion__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStackVersion(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION);
    verifyResults(response, 500);
  }

  @Test
  public void testGetStackVersions() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getStackVersions(getHttpHeaders(),
        getUriInfo(), STACK_NAME);
    verifyResults(response, 200);
  }

  @Test
  public void testGetStackVersions__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStackVersions(getHttpHeaders(),
        getUriInfo(), STACK_NAME);
    verifyResults(response, 500);
  }

  @Test
  public void testGetRepositories() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getRepositories(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_NAME, OS_TYPE);
    verifyResults(response, 200);
  }

  @Test
  public void testGetRepositories__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getRepositories(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, OS_TYPE);
    verifyResults(response, 500);
  }

  @Test
  public void testGetRepository() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getRepository(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_NAME, OS_TYPE, REPO_ID);
    verifyResults(response, 200);
  }

  @Test
  public void testGetRepository__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getRepository(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, OS_TYPE, REPO_ID);
    verifyResults(response, 500);
  }

  @Test
  public void testGetStackServices() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getStackServices(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION);
    verifyResults(response, 200);
  }

  @Test
  public void testGetStackServices__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStackServices(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION);
    verifyResults(response, 500);
  }

  @Test
  public void testGetStackService() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getStackService(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME);
    verifyResults(response, 200);
  }

  @Test
  public void testGetStackService__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStackService(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME);
    verifyResults(response, 500);
  }

  @Test
  public void testGetStackConfigurations() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getStackConfigurations(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME);
    verifyResults(response, 200);
  }

  @Test
  public void testGetStackConfigurations__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStackConfigurations(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME);
    verifyResults(response, 500);
  }

  @Test
  public void testGetStackConfiguration() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getStackConfiguration(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME, PROPERTY_NAME);
    verifyResults(response, 200);
  }

  @Test
  public void testGetStackConfiguration__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStackConfiguration(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME, PROPERTY_NAME);
    verifyResults(response, 500);
  }

  @Test
  public void testGetServiceComponent() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getServiceComponent(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME, COMPONENT_NAME);
    verifyResults(response, 200);
  }

  @Test
  public void testGetServiceComponent__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getServiceComponent(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME, COMPONENT_NAME);
    verifyResults(response, 500);
  }

  @Test
  public void testGetServiceComponents() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getStackConfiguration(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME, PROPERTY_NAME);
    verifyResults(response, 200);
  }

  @Test
  public void testGetServiceComponents__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getStackConfiguration(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, SERVICE_NAME, PROPERTY_NAME);
    verifyResults(response, 500);
  }

  @Test
  public void testGetOperatingSystems() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getOperatingSystems(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION);
    verifyResults(response, 200);
  }

  @Test
  public void testGetOperatingSystems__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getOperatingSystems(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION);
    verifyResults(response, 500);
  }

  @Test
  public void testGetOperatingSystem() {

    registerExpectations(Request.Type.GET, null, 200, false);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());

    Response response = stacksService.getOperatingSystem(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, OS_TYPE);
    verifyResults(response, 200);
  }

  @Test
  public void testGetOperatingSystem__ErrorState() {

    registerExpectations(Request.Type.GET, null, 500, true);
    replayMocks();

    // test
    StacksService stacksService = new TestStacksService(getResource(),
        getRequestFactory(), getRequestHandler());
    Response response = stacksService.getOperatingSystem(getHttpHeaders(),
        getUriInfo(), STACK_NAME, STACK_VERSION, OS_TYPE);
    verifyResults(response, 500);
  }

  private class TestStacksService extends StacksService {
    private RequestFactory m_requestFactory;
    private RequestHandler m_requestHandler;
    private ResourceInstance m_resourceDef;

    private TestStacksService(ResourceInstance resourceDef,
        RequestFactory requestFactory, RequestHandler handler) {
      m_resourceDef = resourceDef;
      m_requestFactory = requestFactory;
      m_requestHandler = handler;
    }

    @Override
    ResourceInstance createStackResource(String stackName) {
      return m_resourceDef;
    }

    @Override
    ResourceInstance createStackVersionResource(String stackName,
        String stackVersion) {
      return m_resourceDef;
    }

    @Override
    ResourceInstance createRepositoryResource(String stackName,
        String stackVersion, String osType, String repoId) {
      return m_resourceDef;
    }

    @Override
    ResourceInstance createStackServiceResource(String stackName,
        String stackVersion, String serviceName) {
      return m_resourceDef;
    }

    ResourceInstance createStackConfigurationResource(String stackName,
        String stackVersion, String serviceName, String propertyName) {
      return m_resourceDef;
    }

    ResourceInstance createStackServiceComponentResource(String stackName,
        String stackVersion, String serviceName, String componentName) {
      return m_resourceDef;
    }

    ResourceInstance createOperatingSystemResource(String stackName,
        String stackVersion, String osType) {
      return m_resourceDef;
    }

    @Override
    RequestFactory getRequestFactory() {
      return m_requestFactory;
    }

    @Override
    RequestHandler getRequestHandler(Request.Type requestType) {
      return m_requestHandler;
    }
  }

  // todo: test getHostHandler, getServiceHandler, getHostComponentHandler
}
