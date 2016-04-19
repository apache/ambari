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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import junit.framework.Assert;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.SecurePasswordHelper;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorFactory;
import org.apache.ambari.server.state.kerberos.KerberosDescriptorType;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptorTest;
import org.apache.ambari.server.state.kerberos.KerberosServiceDescriptorTest;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.easymock.EasyMock.*;

/**
 * ClusterKerberosDescriptorResourceProviderTest unit tests.
 */
@SuppressWarnings("unchecked")
public class ClusterKerberosDescriptorResourceProviderTest extends EasyMockSupport {

  private static final Map<String, Object> STACK_MAP =
      new HashMap<String, Object>() {
        {
          put("properties", new HashMap<String, Object>() {{
            put("realm", "EXAMPLE.COM");
            put("some.property", "Hello World");
          }});

          put(KerberosDescriptorType.AUTH_TO_LOCAL_PROPERTY.getDescriptorPluralName(), new ArrayList<String>() {{
            add("global.name.rules");
          }});

          put(KerberosDescriptorType.SERVICE.getDescriptorPluralName(), new ArrayList<Object>() {{
            add(KerberosServiceDescriptorTest.MAP_VALUE);
          }});
          put(KerberosDescriptorType.CONFIGURATION.getDescriptorPluralName(), new ArrayList<Map<String, Object>>() {{
            add(new HashMap<String, Object>() {
              {
                put("cluster-conf", new HashMap<String, String>() {
                  {
                    put("property1", "red");
                  }
                });
              }
            });
          }});
          put(KerberosDescriptorType.IDENTITY.getDescriptorPluralName(), new ArrayList<Object>() {{
            add(new HashMap<String, Object>() {
              {
                put("name", "shared");
                put("principal", new HashMap<String, Object>(KerberosPrincipalDescriptorTest.MAP_VALUE));
                put("keytab", new HashMap<String, Object>() {
                  {
                    put("file", "/etc/security/keytabs/subject.service.keytab");

                    put("owner", new HashMap<String, Object>() {{
                      put("name", "root");
                      put("access", "rw");
                    }});

                    put("group", new HashMap<String, Object>() {{
                      put("name", "hadoop");
                      put("access", "r");
                    }});

                    put("configuration", "service-site/service2.component.keytab.file");
                  }
                });
              }
            });
          }});
        }
      };

  private static final Map<String, Object> USER_MAP =
      new HashMap<String, Object>() {
        {
          put("properties", new HashMap<String, Object>() {{
            put("realm", "HWX.COM");
            put("some.property", "Hello World");
          }});

          put(KerberosDescriptorType.CONFIGURATION.getDescriptorPluralName(), new ArrayList<Map<String, Object>>() {{
            add(new HashMap<String, Object>() {
              {
                put("cluster-conf", new HashMap<String, String>() {
                  {
                    put("property1", "blue");
                    put("property2", "orange");
                  }
                });
              }
            });
          }});
          put(KerberosDescriptorType.IDENTITY.getDescriptorPluralName(), new ArrayList<Object>() {{
            add(new HashMap<String, Object>() {
              {
                put("name", "shared");
                put("principal", new HashMap<String, Object>(KerberosPrincipalDescriptorTest.MAP_VALUE));
                put("keytab", new HashMap<String, Object>() {
                  {
                    put("file", "/etc/security/keytabs/subject.service.keytab");

                    put("owner", new HashMap<String, Object>() {{
                      put("name", "root");
                      put("access", "rw");
                    }});

                    put("group", new HashMap<String, Object>() {{
                      put("name", "hadoop");
                      put("access", "r");
                    }});

                    put("configuration", "service-site/service2.component.keytab.file");
                  }
                });
              }
            });
          }});
        }
      };

  private static final Map<String, Object> COMPOSITE_MAP = new HashMap<String, Object>();

  static {
    COMPOSITE_MAP.putAll(STACK_MAP);
    COMPOSITE_MAP.putAll(USER_MAP);
  }

  private Injector injector;

  @Before
  public void setUp() throws Exception {
    injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        Properties properties = new Properties();

        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(SecurePasswordHelper.class).toInstance(new SecurePasswordHelper());
        bind(Configuration.class).toInstance(new Configuration(properties));
        bind(KerberosDescriptorFactory.class).toInstance(new KerberosDescriptorFactory());
      }
    });
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test(expected = SystemException.class)
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = SystemException.class)
  public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = SystemException.class)
  public void testCreateResourcesAsServiceAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testCreateResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    ClusterKerberosDescriptorResourceProvider resourceProvider = new ClusterKerberosDescriptorResourceProvider(managementController);
    injector.injectMembers(resourceProvider);

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        PropertyHelper.getPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        PropertyHelper.getKeyPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        managementController);

    AbstractResourceProviderTest.TestObserver observer = new AbstractResourceProviderTest.TestObserver();
    ((ObservableResourceProvider) provider).addObserver(observer);

    provider.createResources(request);

    verifyAll();
  }

  @Test
  public void testGetResourcesAsAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesAsClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesAsServiceAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResources(Authentication authentication) throws Exception {

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).atLeastOnce();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).atLeastOnce();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        PropertyHelper.getPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        PropertyHelper.getKeyPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        managementController);

    Predicate predicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();

    Set<Resource> results = provider.getResources(request, predicate);
    Assert.assertEquals(3, results.size());

    verifyAll();
  }

  @Test
  public void testGetResourcesWithPredicateAsAdministrator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesWithPredicateAsClusterAdministrator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesWithPredicateAsServiceAdministrator() throws Exception {
    testGetResourcesWithPredicate(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResourcesWithPredicate(Authentication authentication) throws Exception {

    StackId stackVersion = createMock(StackId.class);
    expect(stackVersion.getStackName()).andReturn("stackName").atLeastOnce();
    expect(stackVersion.getStackVersion()).andReturn("stackVersion").atLeastOnce();

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).atLeastOnce();
    expect(cluster.getCurrentStackVersion()).andReturn(stackVersion).atLeastOnce();
    expect(cluster.getClusterName()).andReturn("c1").anyTimes();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).atLeastOnce();

    KerberosDescriptorFactory kerberosDescriptorFactory = injector.getInstance(KerberosDescriptorFactory.class);
    KerberosDescriptor kerberosDescriptor = kerberosDescriptorFactory.createInstance(STACK_MAP);

    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
    expect(metaInfo.getKerberosDescriptor("stackName", "stackVersion")).andReturn(kerberosDescriptor).atLeastOnce();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();
    expect(managementController.getAmbariMetaInfo()).andReturn(metaInfo).atLeastOnce();

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).atLeastOnce();

    Map<String, Map<String, Object>> artifactPropertyMap = new HashMap<>();
    artifactPropertyMap.put(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY, USER_MAP);
    artifactPropertyMap.put(ArtifactResourceProvider.ARTIFACT_DATA_PROPERTY + "/properties", null);

    Resource artifactResource = createMock(Resource.class);
    expect(artifactResource.getPropertiesMap()).andReturn(artifactPropertyMap).atLeastOnce();

    ResourceProvider artifactResourceProvider = createStrictMock(ArtifactResourceProvider.class);
    expect(artifactResourceProvider.getResources(anyObject(Request.class), anyObject(Predicate.class)))
        .andReturn(Collections.singleton(artifactResource)).atLeastOnce();

    ClusterController clusterController = createStrictMock(ClusterController.class);
    expect(clusterController.ensureResourceProvider(Resource.Type.Artifact)).andReturn(artifactResourceProvider).atLeastOnce();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        PropertyHelper.getPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        PropertyHelper.getKeyPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        managementController);

    setClusterController(provider, clusterController);
    setKerberosDescriptorFactory(provider, kerberosDescriptorFactory);

    Predicate clusterPredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate typePredicate;
    Set<Resource> results;

    // --------------
    // Get the STACK Kerberos Descriptor
    typePredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("STACK")
        .toPredicate();

    results = provider.getResources(request, new AndPredicate(clusterPredicate, typePredicate));
    Assert.assertEquals(1, results.size());

    for (Resource result : results) {
      Assert.assertEquals("c1", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("STACK", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID));

      // Reconstruct the deconstructed Kerberos Descriptor
      Map partial1 = result.getPropertiesMap().get(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID);
      Map partial2 = result.getPropertiesMap().get("KerberosDescriptor/kerberos_descriptor/properties");
      partial1.put("properties", partial2);

      Assert.assertEquals(STACK_MAP, partial1);
    }

    // --------------
    // Get the USER Kerberos Descriptor
    typePredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("USER")
        .toPredicate();

    results = provider.getResources(request, new AndPredicate(clusterPredicate, typePredicate));
    Assert.assertEquals(1, results.size());

    for (Resource result : results) {
      Assert.assertEquals("c1", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("USER", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID));

      // Reconstruct the deconstructed Kerberos Descriptor
      Map partial1 = result.getPropertiesMap().get(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID);
      Map partial2 = result.getPropertiesMap().get("KerberosDescriptor/kerberos_descriptor/properties");
      partial1.put("properties", partial2);

      Assert.assertEquals(USER_MAP, partial1);
    }

    // --------------
    // Get the COMPOSITE Kerberos Descriptor
    typePredicate = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("COMPOSITE")
        .toPredicate();

    results = provider.getResources(request, new AndPredicate(clusterPredicate, typePredicate));
    Assert.assertEquals(1, results.size());

    for (Resource result : results) {
      Assert.assertEquals("c1", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID));
      Assert.assertEquals("COMPOSITE", result.getPropertyValue(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID));

      // Reconstruct the deconstructed Kerberos Descriptor
      Map partial1 = result.getPropertiesMap().get(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID);
      Map partial2 = result.getPropertiesMap().get("KerberosDescriptor/kerberos_descriptor/properties");
      partial1.put("properties", partial2);

      Assert.assertEquals(COMPOSITE_MAP, partial1);
    }

    verifyAll();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetResourcesWithInvalidKerberosDescriptorTypeAsAdministrator() throws Exception {
    testGetResourcesWithInvalidKerberosDescriptorType(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetResourcesWithInvalidKerberosDescriptorTypeAsClusterAdministrator() throws Exception {
    testGetResourcesWithInvalidKerberosDescriptorType(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesWithInvalidKerberosDescriptorTypeAsServiceAdministrator() throws Exception {
    testGetResourcesWithInvalidKerberosDescriptorType(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResourcesWithInvalidKerberosDescriptorType(Authentication authentication) throws Exception {

    StackId stackVersion = createMock(StackId.class);
    expect(stackVersion.getStackName()).andReturn("stackName").atLeastOnce();
    expect(stackVersion.getStackVersion()).andReturn("stackVersion").atLeastOnce();

    Cluster cluster = createMock(Cluster.class);
    expect(cluster.getResourceId()).andReturn(4L).atLeastOnce();
    expect(cluster.getCurrentStackVersion()).andReturn(stackVersion).atLeastOnce();

    Clusters clusters = createMock(Clusters.class);
    expect(clusters.getCluster("c1")).andReturn(cluster).atLeastOnce();

    KerberosDescriptor kerberosDescriptor = createMock(KerberosDescriptor.class);
    expect(kerberosDescriptor.toMap()).andReturn(STACK_MAP).atLeastOnce();

    AmbariMetaInfo metaInfo = createMock(AmbariMetaInfo.class);
    expect(metaInfo.getKerberosDescriptor("stackName", "stackVersion")).andReturn(kerberosDescriptor).atLeastOnce();

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();
    expect(managementController.getAmbariMetaInfo()).andReturn(metaInfo).atLeastOnce();

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        PropertyHelper.getPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        PropertyHelper.getKeyPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        managementController);

    Predicate predicate1 = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate predicate2 = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("BOGUS")
        .toPredicate();
    Predicate predicate = new AndPredicate(predicate1, predicate2);

    try {
      provider.getResources(request, predicate);
      Assert.fail("Expected NoSuchResourceException not thrown");
    } catch (NoSuchResourceException e) {
      // expected
    }

    verifyAll();
  }

  @Test
  public void testGetResourcesWithoutPredicateAsAdministrator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResourcesWithoutPredicateAsClusterAdministrator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResourcesWithoutPredicateAsServiceAdministrator() throws Exception {
    testGetResourcesWithoutPredicate(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testGetResourcesWithoutPredicate(Authentication authentication) throws Exception {

    Clusters clusters = createMock(Clusters.class);

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    expect(managementController.getClusters()).andReturn(clusters).atLeastOnce();

    Request request = createMock(Request.class);
    expect(request.getPropertyIds()).andReturn(null).once();

    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        PropertyHelper.getPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        PropertyHelper.getKeyPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        managementController);


    Set<Resource> results = provider.getResources(request, null);
    Assert.assertTrue(results.isEmpty());

    verifyAll();
  }

  @Test(expected = SystemException.class)
  public void testUpdateResourcesAsAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = SystemException.class)
  public void testUpdateResourcesAsClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = SystemException.class)
  public void testUpdateResourcesAsServiceAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testUpdateResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Request request = createMock(Request.class);

    ClusterKerberosDescriptorResourceProvider resourceProvider = new ClusterKerberosDescriptorResourceProvider(managementController);
    injector.injectMembers(resourceProvider);

    replayAll();
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        PropertyHelper.getPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        PropertyHelper.getKeyPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        managementController);

    provider.createResources(request);

    verifyAll();
  }

  @Test(expected = SystemException.class)
  public void testDeleteResourcesAsAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = SystemException.class)
  public void testDeleteResourcesAsClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  @Test(expected = SystemException.class)
  public void testDeleteResourcesAsServiceAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createServiceAdministrator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    ClusterKerberosDescriptorResourceProvider resourceProvider = new ClusterKerberosDescriptorResourceProvider(managementController);
    injector.injectMembers(resourceProvider);

    replayAll();
    // end expectations

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.ClusterKerberosDescriptor,
        PropertyHelper.getPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        PropertyHelper.getKeyPropertyIds(Resource.Type.ClusterKerberosDescriptor),
        managementController);

    Predicate predicate1 = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID).equals("c1")
        .toPredicate();
    Predicate predicate2 = new PredicateBuilder()
        .property(ClusterKerberosDescriptorResourceProvider.CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID).equals("alias1")
        .toPredicate();
    Predicate predicate = new AndPredicate(predicate1, predicate2);

    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    verifyAll();
  }

  private void setClusterController(ResourceProvider provider, ClusterController clusterController) throws Exception {
    Class<?> c = provider.getClass();

    Field f = c.getDeclaredField("clusterController");
    f.setAccessible(true);
    f.set(provider, clusterController);
  }

  private void setKerberosDescriptorFactory(ResourceProvider provider, KerberosDescriptorFactory factory) throws Exception {
    Class<?> c = provider.getClass();

    Field f = c.getDeclaredField("kerberosDescriptorFactory");
    f.setAccessible(true);
    f.set(provider, factory);
  }

}

