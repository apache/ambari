/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_CATEGORY_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_COMPONENT_NAME_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTIES_PROPERTY_ID;
import static org.apache.ambari.server.controller.internal.RootServiceComponentConfigurationResourceProvider.CONFIGURATION_SERVICE_NAME_PROPERTY_ID;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.services.RootServiceComponentConfigurationService;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import edu.emory.mathcs.backport.java.util.Collections;
import junit.framework.Assert;

public class RootServiceComponentConfigurationResourceProviderTest extends EasyMockSupport {

  private static final String CATEGORY_NAME_1 = "test-category-1";
  private static final String CATEGORY_NAME_2 = "test-category-2";

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources_Administrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_ServiceOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceOperator(), null);
  }

  @Test
  public void testCreateResourcesWithDirective_Administrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ClusterOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterOperator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesWithDirective_ServiceOperator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceOperator(), "test-directive");
  }

  private void testCreateResources(Authentication authentication, String opDirective) throws Exception {
    Injector injector = createInjector();

    ResourceProvider resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);

    Set<Map<String, Object>> propertySets = new HashSet<>();

    Map<String, String> properties1 = new HashMap<>();
    properties1.put("property1a", "value1");
    properties1.put("property2a", "value2");
    propertySets.add(toRequestProperties(CATEGORY_NAME_1, properties1));

    Map<String, String> properties2 = new HashMap<>();
    if (opDirective == null) {
      properties2.put("property1b", "value1");
      properties2.put("property2b", "value2");
      propertySets.add(toRequestProperties(CATEGORY_NAME_2, properties2));
    }

    Map<String, String> requestInfoProperties;
    if (opDirective == null) {
      requestInfoProperties = Collections.emptyMap();
    } else {
      requestInfoProperties = Collections.singletonMap(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION, opDirective);
    }

    Request request = createMock(Request.class);
    expect(request.getProperties()).andReturn(propertySets).once();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();
    Capture<Map<String, String>> capturedProperties2 = newCapture();

    if (opDirective == null) {
      AmbariConfigurationDAO dao = injector.getInstance(AmbariConfigurationDAO.class);
      expect(dao.reconcileCategory(eq(CATEGORY_NAME_1), capture(capturedProperties1), eq(true)))
          .andReturn(true)
          .once();
      expect(dao.reconcileCategory(eq(CATEGORY_NAME_2), capture(capturedProperties2), eq(true)))
          .andReturn(true)
          .once();


      AmbariEventPublisher publisher = injector.getInstance(AmbariEventPublisher.class);
      publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
      expectLastCall().times(2);
    }

    RootServiceComponentConfigurationHandlerFactory factory = injector.getInstance(RootServiceComponentConfigurationHandlerFactory.class);
    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1))
        .andReturn(new AmbariServerConfigurationHandler())
        .once();
    if (opDirective == null) {
      expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_2))
          .andReturn(new AmbariServerConfigurationHandler())
          .once();
    }

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      resourceProvider.createResources(request);
      if (opDirective != null) {
        Assert.fail("Expected SystemException to be thrown");
      }
    } catch (AuthorizationException e) {
      throw e;
    } catch (SystemException e) {
      if (opDirective == null) {
        Assert.fail("Unexpected exception: " + e.getMessage());
      } else {
        Assert.assertEquals("The requested operation is not supported for this category: " + CATEGORY_NAME_1, e.getMessage());
      }
    }

    verifyAll();

    if (opDirective == null) {
      validateCapturedProperties(properties1, capturedProperties1);
      validateCapturedProperties(properties2, capturedProperties2);
    } else {
      Assert.assertFalse(capturedProperties1.hasCaptured());
      Assert.assertFalse(capturedProperties2.hasCaptured());
    }
  }

  @Test
  public void testDeleteResources_Administrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ClusterOperator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResources_ServiceOperator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceOperator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    ResourceProvider resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);

    Predicate predicate = createPredicate(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1);

    Request request = createMock(Request.class);

    AmbariConfigurationDAO dao = injector.getInstance(AmbariConfigurationDAO.class);
    expect(dao.removeByCategory(CATEGORY_NAME_1)).andReturn(1).once();

    AmbariEventPublisher publisher = injector.getInstance(AmbariEventPublisher.class);
    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().once();

    RootServiceComponentConfigurationHandlerFactory factory = injector.getInstance(RootServiceComponentConfigurationHandlerFactory.class);
    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1))
        .andReturn(new AmbariServerConfigurationHandler())
        .once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    resourceProvider.deleteResources(request, predicate);

    verifyAll();
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ClusterOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterOperator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResources_ServiceOperator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceOperator());
  }

  private void testGetResources(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    ResourceProvider resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);

    Predicate predicate = createPredicate(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1);

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).anyTimes();

    Map<String, String> properties = new HashMap<>();
    properties.put("property1a", "value1");
    properties.put("property2a", "value2");

    AmbariConfigurationDAO dao = injector.getInstance(AmbariConfigurationDAO.class);
    expect(dao.findByCategory(CATEGORY_NAME_1)).andReturn(createEntities(CATEGORY_NAME_1, properties)).once();

    RootServiceComponentConfigurationHandlerFactory factory = injector.getInstance(RootServiceComponentConfigurationHandlerFactory.class);
    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1))
        .andReturn(new AmbariServerConfigurationHandler())
        .once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    Set<Resource> response = resourceProvider.getResources(request, predicate);

    verifyAll();

    Assert.assertNotNull(response);
    Assert.assertEquals(1, response.size());

    Resource resource = response.iterator().next();
    Assert.assertEquals(Resource.Type.RootServiceComponentConfiguration, resource.getType());

    Map<String, Map<String, Object>> propertiesMap = resource.getPropertiesMap();
    Assert.assertEquals(2, propertiesMap.size());

    Assert.assertEquals(CATEGORY_NAME_1, propertiesMap.get(RootServiceComponentConfigurationResourceProvider.RESOURCE_KEY).get("category"));

    Map<String, Object> retrievedProperties = propertiesMap.get(RootServiceComponentConfigurationResourceProvider.RESOURCE_KEY + "/properties");
    Assert.assertEquals(2, retrievedProperties.size());

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      Assert.assertEquals(entry.getValue(), retrievedProperties.get(entry.getKey()));
    }
  }

  @Test
  public void testUpdateResources_Administrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ClusterOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterOperator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator(), null);
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_ServiceOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceOperator(), null);
  }

  @Test
  public void testUpdateResourcesWithDirective_Administrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ClusterOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterOperator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator(), "test-directive");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesWithDirective_ServiceOperator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceOperator(), "test-directive");
  }

  private void testUpdateResources(Authentication authentication, String opDirective) throws Exception {
    Injector injector = createInjector();

    ResourceProvider resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);

    Predicate predicate = createPredicate(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1);

    Set<Map<String, Object>> propertySets = new HashSet<>();

    Map<String, String> properties1 = new HashMap<>();
    properties1.put("property1a", "value1");
    properties1.put("property2a", "value2");
    propertySets.add(toRequestProperties(CATEGORY_NAME_1, properties1));

    Map<String, String> requestInfoProperties;
    if (opDirective == null) {
      requestInfoProperties = Collections.emptyMap();
    } else {
      requestInfoProperties = Collections.singletonMap(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION, opDirective);
    }

    Request request = createMock(Request.class);
    expect(request.getProperties()).andReturn(propertySets).once();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();

    if (opDirective == null) {
      AmbariConfigurationDAO dao = injector.getInstance(AmbariConfigurationDAO.class);
      expect(dao.reconcileCategory(eq(CATEGORY_NAME_1), capture(capturedProperties1), eq(false)))
          .andReturn(true)
          .once();

      AmbariEventPublisher publisher = injector.getInstance(AmbariEventPublisher.class);
      publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
      expectLastCall().times(1);
    }

    RootServiceComponentConfigurationHandlerFactory factory = injector.getInstance(RootServiceComponentConfigurationHandlerFactory.class);
    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1))
        .andReturn(new AmbariServerConfigurationHandler())
        .once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    try {
      resourceProvider.updateResources(request, predicate);

      if (opDirective != null) {
        Assert.fail("Expected SystemException to be thrown");
      }
    } catch (AuthorizationException e) {
      throw e;
    } catch (SystemException e) {
      if (opDirective == null) {
        Assert.fail("Unexpected exception: " + e.getMessage());
      } else {
        Assert.assertEquals("The requested operation is not supported for this category: " + CATEGORY_NAME_1, e.getMessage());
      }
    }

    verifyAll();

    if (opDirective == null) {
      validateCapturedProperties(properties1, capturedProperties1);
    } else {
      Assert.assertFalse(capturedProperties1.hasCaptured());
    }
  }

  private Predicate createPredicate(String serviceName, String componentName, String categoryName) {
    Predicate predicateService = new PredicateBuilder()
        .property(CONFIGURATION_SERVICE_NAME_PROPERTY_ID)
        .equals(serviceName)
        .toPredicate();
    Predicate predicateComponent = new PredicateBuilder()
        .property(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID)
        .equals(componentName)
        .toPredicate();
    Predicate predicateCategory = new PredicateBuilder()
        .property(CONFIGURATION_CATEGORY_PROPERTY_ID)
        .equals(categoryName)
        .toPredicate();
    return new AndPredicate(predicateService, predicateComponent, predicateCategory);
  }

  private List<AmbariConfigurationEntity> createEntities(String categoryName, Map<String, String> properties) {
    List<AmbariConfigurationEntity> entities = new ArrayList<>();

    for (Map.Entry<String, String> property : properties.entrySet()) {
      AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
      entity.setCategoryName(categoryName);
      entity.setPropertyName(property.getKey());
      entity.setPropertyValue(property.getValue());
      entities.add(entity);
    }

    return entities;
  }

  private Map<String, Object> toRequestProperties(String categoryName1, Map<String, String> properties) {
    Map<String, Object> requestProperties = new HashMap<>();
    requestProperties.put(CONFIGURATION_SERVICE_NAME_PROPERTY_ID, "AMBARI");
    requestProperties.put(CONFIGURATION_COMPONENT_NAME_PROPERTY_ID, "AMBARI_SERVER");
    requestProperties.put(CONFIGURATION_CATEGORY_PROPERTY_ID, categoryName1);
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      requestProperties.put(CONFIGURATION_PROPERTIES_PROPERTY_ID + "/" + entry.getKey(), entry.getValue());
    }
    return requestProperties;
  }

  private void validateCapturedProperties(Map<String, String> expectedProperties, Capture<Map<String, String>> capturedProperties) {
    Assert.assertTrue(capturedProperties.hasCaptured());

    Map<String, String> properties = capturedProperties.getValue();
    Assert.assertNotNull(properties);

    // Convert the Map to a TreeMap to help with comparisons
    expectedProperties = new TreeMap<>(expectedProperties);
    properties = new TreeMap<>(properties);
    Assert.assertEquals(expectedProperties, properties);
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(AmbariConfigurationDAO.class).toInstance(createMock(AmbariConfigurationDAO.class));
        bind(AmbariEventPublisher.class).toInstance(createMock(AmbariEventPublisher.class));
        bind(RootServiceComponentConfigurationHandlerFactory.class).toInstance(createMock(RootServiceComponentConfigurationHandlerFactory.class));

        binder().requestStaticInjection(AmbariServerConfigurationHandler.class);
      }
    });
  }
}