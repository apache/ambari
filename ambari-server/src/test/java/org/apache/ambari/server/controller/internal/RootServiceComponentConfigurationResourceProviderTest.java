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

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.ambari.server.api.services.RootServiceComponentConfigurationService;
import org.apache.ambari.server.configuration.Configuration;
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
import org.apache.ambari.server.ldap.domain.AmbariLdapConfigurationKeys;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.encryption.CredentialProvider;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import edu.emory.mathcs.backport.java.util.Collections;
import junit.framework.Assert;
import junit.framework.AssertionFailedError;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileUtils.class, AmbariServerConfigurationHandler.class})
public class RootServiceComponentConfigurationResourceProviderTest extends EasyMockSupport {

  private static final String CATEGORY_NAME_1 = "test-category-1";
  private static final String CATEGORY_NAME_2 = "test-category-2";
  private static final String LDAP_CONFIG_CATEGORY = AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName();

  private Injector injector;
  private Predicate predicate;
  private ResourceProvider resourceProvider;
  private RootServiceComponentConfigurationHandlerFactory factory;
  private Request request;
  private AmbariConfigurationDAO dao;
  private Configuration configuration;
  private AmbariEventPublisher publisher;
  private Map<String, String> properties = new HashMap<>();
  private Set<Map<String, Object>> propertySets = new HashSet<>();

  @Before
  public void init() throws Exception {
    injector = createInjector();
    resourceProvider = injector.getInstance(RootServiceComponentConfigurationResourceProvider.class);
    predicate = createPredicate(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1);
    request = createMock(Request.class);
    dao = injector.getInstance(AmbariConfigurationDAO.class);
    configuration = injector.getInstance(Configuration.class);
    factory = injector.getInstance(RootServiceComponentConfigurationHandlerFactory.class);
    publisher = injector.getInstance(AmbariEventPublisher.class);
    properties = new HashMap<>();
    propertySets = new HashSet<>();
  }

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
    properties.put(Configuration.AMBARI_PYTHON_WRAP.getKey(), "value1");
    properties.put(Configuration.AMBARI_DISPLAY_URL.getKey(), "value2");
    propertySets.add(toRequestProperties(CATEGORY_NAME_1, properties));

    Map<String, String> properties2 = new HashMap<>();
    if (opDirective == null) {
      properties2.put(Configuration.SSL_TRUSTSTORE_TYPE.getKey(), "value1");
      properties2.put(Configuration.SSL_TRUSTSTORE_PATH.getKey(), "value2");
      propertySets.add(toRequestProperties(CATEGORY_NAME_2, properties2));
    }

    Map<String, String> requestInfoProperties;
    if (opDirective == null) {
      requestInfoProperties = Collections.emptyMap();
    } else {
      requestInfoProperties = Collections.singletonMap(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION, opDirective);
    }

    expect(request.getProperties()).andReturn(propertySets).once();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();
    Capture<Map<String, String>> capturedProperties2 = newCapture();

    if (opDirective == null) {
      expect(dao.reconcileCategory(eq(CATEGORY_NAME_1), capture(capturedProperties1), eq(true)))
          .andReturn(true)
          .once();
      expect(dao.reconcileCategory(eq(CATEGORY_NAME_2), capture(capturedProperties2), eq(true)))
          .andReturn(true)
          .once();


      publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
      expectLastCall().times(2);
    }

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
      validateCapturedProperties(properties, capturedProperties1);
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
    expect(dao.removeByCategory(CATEGORY_NAME_1)).andReturn(1).once();

    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().once();

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
    expect(request.getPropertyIds()).andReturn(null).anyTimes();

    properties.put(AmbariLdapConfigurationKeys.ANONYMOUS_BIND.key(), "value1");
    properties.put(AmbariLdapConfigurationKeys.GROUP_MEMBER_ATTRIBUTE.key(), "value2");

    expect(dao.findByCategory(CATEGORY_NAME_1)).andReturn(createEntities(CATEGORY_NAME_1, properties)).once();

    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), CATEGORY_NAME_1))
        .andReturn(new AmbariServerLDAPConfigurationHandler())
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
    Assert.assertEquals(3, propertiesMap.size());

    Assert.assertEquals(CATEGORY_NAME_1, propertiesMap.get(RootServiceComponentConfigurationResourceProvider.RESOURCE_KEY).get("category"));

    Map<String, Object> retrievedProperties = propertiesMap.get(RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTIES_PROPERTY_ID);
    Assert.assertEquals(2, retrievedProperties.size());

    for (Map.Entry<String, String> entry : properties.entrySet()) {
      Assert.assertEquals(entry.getValue(), retrievedProperties.get(entry.getKey()));
    }

    Map<String, Object> retrievedPropertyTypes = propertiesMap.get(RootServiceComponentConfigurationResourceProvider.CONFIGURATION_PROPERTY_TYPES_PROPERTY_ID);
    Assert.assertEquals(2, retrievedPropertyTypes.size());
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
    properties.put(Configuration.AMBARI_DISPLAY_URL.getKey(), "value1");
    properties.put(Configuration.BOOTSTRAP_MASTER_HOSTNAME.getKey(), "value2");
    propertySets.add(toRequestProperties(CATEGORY_NAME_1, properties));

    Map<String, String> requestInfoProperties;
    if (opDirective == null) {
      requestInfoProperties = Collections.emptyMap();
    } else {
      requestInfoProperties = Collections.singletonMap(RootServiceComponentConfigurationService.DIRECTIVE_OPERATION, opDirective);
    }

    expect(request.getProperties()).andReturn(propertySets).once();
    expect(request.getRequestInfoProperties()).andReturn(requestInfoProperties).once();

    Capture<Map<String, String>> capturedProperties1 = newCapture();

    if (opDirective == null) {
      expect(dao.reconcileCategory(eq(CATEGORY_NAME_1), capture(capturedProperties1), eq(false)))
          .andReturn(true)
          .once();

      publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
      expectLastCall().times(1);
    }

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
      validateCapturedProperties(properties, capturedProperties1);
    } else {
      Assert.assertFalse(capturedProperties1.hasCaptured());
    }
  }

  @Test
  public void shouldNotUpdatePasswordIfItHasNotBeenChanged() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    properties.put(AmbariLdapConfigurationKeys.BIND_PASSWORD.key(), "passwd");
    propertySets.add(toRequestProperties(LDAP_CONFIG_CATEGORY, properties));
    setupBasicExpectations(properties);
    expect(configuration.isSecurityPasswordEncryptionEnabled()).andThrow(new AssertionFailedError()).anyTimes(); //this call should never have never been hit

    replayAll();
    resourceProvider.updateResources(request, predicate);
    verifyAll();
  }

  @Test
  public void shouldUpdatePasswordFileIfSecurityPasswordEncryptionIsDisabled() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    properties.put(AmbariLdapConfigurationKeys.BIND_PASSWORD.key(), "newPasswd");
    propertySets.add(toRequestProperties(LDAP_CONFIG_CATEGORY, properties));
    Map<String, String> expectedProperties = new HashMap<>();
    expectedProperties.put(AmbariLdapConfigurationKeys.BIND_PASSWORD.key(), "currentPasswd");
    setupBasicExpectations(expectedProperties);
    expect(configuration.isSecurityPasswordEncryptionEnabled()).andReturn(false).once();
    PowerMock.mockStatic(FileUtils.class);
    FileUtils.writeStringToFile(new File("currentPasswd"), "newPasswd", Charset.defaultCharset());
    PowerMock.expectLastCall().once();
    PowerMock.replay(FileUtils.class);
    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().once();

    replayAll();
    resourceProvider.updateResources(request, predicate);
    verifyAll();
  }

  @Test
  public void shouldUpdatePasswordInCredentialStoreIfSecurityPasswordEncryptionIsEnabled() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createAdministrator());
    properties.put(AmbariLdapConfigurationKeys.BIND_PASSWORD.key(), "newPasswd");
    propertySets.add(toRequestProperties(LDAP_CONFIG_CATEGORY, properties));
    Map<String, String> expectedProperties = new HashMap<>();
    expectedProperties.put(AmbariLdapConfigurationKeys.BIND_PASSWORD.key(), "currentPasswd");
    setupBasicExpectations(expectedProperties);
    expect(configuration.isSecurityPasswordEncryptionEnabled()).andReturn(true).once();

    File masterKeyLocation = createNiceMock(File.class);
    File masterKeyStoreLocation = createNiceMock(File.class);
    expect(configuration.getMasterKeyLocation()).andReturn(masterKeyLocation).once();
    expect(configuration.isMasterKeyPersisted()).andReturn(false).once();
    expect(configuration.getMasterKeyStoreLocation()).andReturn(masterKeyStoreLocation).once();
    CredentialProvider credentialProvider = PowerMock.createMock(CredentialProvider.class);
    PowerMock.expectNew(CredentialProvider.class, null, (String) null, masterKeyLocation, false, masterKeyStoreLocation).andReturn(credentialProvider);
    credentialProvider.addAliasToCredentialStore("currentPasswd", "newPasswd");
    PowerMock.expectLastCall().once();
    PowerMock.replay(credentialProvider, CredentialProvider.class);

    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().once();

    replayAll();
    resourceProvider.updateResources(request, predicate);
    verifyAll();
    PowerMock.verify(credentialProvider, CredentialProvider.class);
  }

  private void setupBasicExpectations(Map<String, String> expectedProperties) {
    expect(request.getProperties()).andReturn(propertySets).once();
    expect(request.getRequestInfoProperties()).andReturn(new HashMap<>());
    expect(dao.findByCategory(LDAP_CONFIG_CATEGORY)).andReturn(createEntities(AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName(), expectedProperties)).once();
    expect(factory.getInstance(RootService.AMBARI.name(), RootComponent.AMBARI_SERVER.name(), LDAP_CONFIG_CATEGORY)).andReturn(new AmbariServerLDAPConfigurationHandler()).once();
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
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Configuration.class).toInstance(createNiceMock(Configuration.class));
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(AmbariConfigurationDAO.class).toInstance(createMock(AmbariConfigurationDAO.class));
        bind(AmbariEventPublisher.class).toInstance(createMock(AmbariEventPublisher.class));
        bind(RootServiceComponentConfigurationHandlerFactory.class).toInstance(createMock(RootServiceComponentConfigurationHandlerFactory.class));

        binder().requestStaticInjection(AmbariServerConfigurationHandler.class);
      }
    });
  }
}