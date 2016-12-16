/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.hooks.HookContextFactory;
import org.apache.ambari.server.hooks.HookService;
import org.apache.ambari.server.metadata.CachedRoleCommandOrderProvider;
import org.apache.ambari.server.metadata.RoleCommandOrderProvider;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.dao.HostRoleCommandDAO;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.UserType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;

/**
 * UserResourceProvider tests.
 */
public class UserResourceProviderTest extends EasyMockSupport {

  @Before
  public void resetMocks() {
    resetAll();
  }

  @After
  public void clearAuthentication() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testCreateResources_Administrator() throws Exception {
    createResourcesTest(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResources_NonAdministrator() throws Exception {
    createResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  @Test
  public void testGetResources_Administrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createAdministrator("admin"));
  }

  @Test
  public void testGetResources_NonAdministrator() throws Exception {
    getResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L));
  }

  @Test
  public void testGetResource_Administrator_Self() throws Exception {
    getResourceTest(TestAuthenticationFactory.createAdministrator("admin"), "admin");
  }

  @Test
  public void testGetResource_Administrator_Other() throws Exception {
    getResourceTest(TestAuthenticationFactory.createAdministrator("admin"), "User1");
  }

  @Test
  public void testGetResource_NonAdministrator_Self() throws Exception {
    getResourceTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testGetResource_NonAdministrator_Other() throws Exception {
    getResourceTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_SetAdmin_Administrator_Self() throws Exception {
    updateResources_SetAdmin(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test
  public void testUpdateResources_SetAdmin_Administrator_Other() throws Exception {
    updateResources_SetAdmin(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_SetAdmin_NonAdministrator_Self() throws Exception {
    updateResources_SetAdmin(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_SetAdmin_NonAdministrator_Other() throws Exception {
    updateResources_SetAdmin(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_SetActive_Administrator_Self() throws Exception {
    updateResources_SetActive(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test
  public void testUpdateResources_SetActive_Administrator_Other() throws Exception {
    updateResources_SetActive(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_SetActive_NonAdministrator_Self() throws Exception {
    updateResources_SetActive(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_SetActive_NonAdministrator_Other() throws Exception {
    updateResources_SetActive(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testUpdateResources_SetPassword_Administrator_Self() throws Exception {
    updateResources_SetPassword(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test
  public void testUpdateResources_SetPassword_Administrator_Other() throws Exception {
    updateResources_SetPassword(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test
  public void testUpdateResources_SetPassword_NonAdministrator_Self() throws Exception {
    updateResources_SetPassword(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResources_SetPassword_NonAdministrator_Other() throws Exception {
    updateResources_SetPassword(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  @Test
  public void testDeleteResource_Administrator_Self() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test
  public void testDeleteResource_Administrator_Other() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createAdministrator("admin"), "User100");
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResource_NonAdministrator_Self() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User1");
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResource_NonAdministrator_Other() throws Exception {
    deleteResourcesTest(TestAuthenticationFactory.createClusterAdministrator("User1", 2L), "User100");
  }

  private Injector createInjector() throws Exception {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(ActionDBAccessor.class).toInstance(createNiceMock(ActionDBAccessor.class));
        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(AmbariMetaInfo.class).toInstance(createMock(AmbariMetaInfo.class));
        bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
        bind(RequestFactory.class).toInstance(createNiceMock(RequestFactory.class));
        bind(RequestExecutionFactory.class).toInstance(createNiceMock(RequestExecutionFactory.class));
        bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
        install(new FactoryModuleBuilder().build(RoleGraphFactory.class));
        bind(Clusters.class).toInstance(createNiceMock(Clusters.class));
        bind(AbstractRootServiceResponseFactory.class).toInstance(createNiceMock(AbstractRootServiceResponseFactory.class));
        bind(StackManagerFactory.class).toInstance(createNiceMock(StackManagerFactory.class));
        bind(ConfigFactory.class).toInstance(createNiceMock(ConfigFactory.class));
        bind(ConfigGroupFactory.class).toInstance(createNiceMock(ConfigGroupFactory.class));
        bind(ServiceFactory.class).toInstance(createNiceMock(ServiceFactory.class));
        bind(ServiceComponentFactory.class).toInstance(createNiceMock(ServiceComponentFactory.class));
        bind(ServiceComponentHostFactory.class).toInstance(createNiceMock(ServiceComponentHostFactory.class));
        bind(PasswordEncoder.class).toInstance(createNiceMock(PasswordEncoder.class));
        bind(KerberosHelper.class).toInstance(createNiceMock(KerberosHelper.class));
        bind(Users.class).toInstance(createMock(Users.class));
        bind(AmbariManagementController.class).to(AmbariManagementControllerImpl.class);
        bind(RoleCommandOrderProvider.class).to(CachedRoleCommandOrderProvider.class);
        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
        bind(HostRoleCommandDAO.class).toInstance(createMock(HostRoleCommandDAO.class));
        bind(HookService.class).toInstance(createMock(HookService.class));
        bind(HookContextFactory.class).toInstance(createMock(HookContextFactory.class));
      }
    });
  }


  private void createResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    Users users = injector.getInstance(Users.class);
    users.createUser("User100", "password", UserType.LOCAL, (Boolean) null, null);
    expectLastCall().atLeastOnce();

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User100");
    properties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verifyAll();
  }

  private void getResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    Users users = injector.getInstance(Users.class);

    if ("admin".equals(authentication.getName())) {
      List<User> allUsers = Arrays.asList(
          createMockUser("User1"),
          createMockUser("User10"),
          createMockUser("User100"),
          createMockUser("admin")
      );
      expect(users.getAllUsers()).andReturn(allUsers).atLeastOnce();
    } else {
      expect(users.getAnyUser("User1")).andReturn(createMockUser("User1")).atLeastOnce();
    }

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(managementController);

    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(UserResourceProvider.USER_USERNAME_PROPERTY_ID);
    propertyIds.add(UserResourceProvider.USER_PASSWORD_PROPERTY_ID);

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> resources = provider.getResources(request, null);

    if ("admin".equals(authentication.getName())) {
      List<String> expectedList = Arrays.asList("User1", "User10", "User100", "admin");
      Assert.assertEquals(4, resources.size());
      for (Resource resource : resources) {
        String userName = (String) resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID);
        Assert.assertTrue(expectedList.contains(userName));
      }
    } else {
      Assert.assertEquals(1, resources.size());
      for (Resource resource : resources) {
        Assert.assertEquals("User1", resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID));
      }
    }

    verifyAll();
  }

  private void getResourceTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    Users users = injector.getInstance(Users.class);
    expect(users.getAnyUser(requestedUsername)).andReturn(createMockUser(requestedUsername)).atLeastOnce();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(managementController);

    Set<String> propertyIds = new HashSet<String>();
    propertyIds.add(UserResourceProvider.USER_USERNAME_PROPERTY_ID);
    propertyIds.add(UserResourceProvider.USER_PASSWORD_PROPERTY_ID);

    Request request = PropertyHelper.getReadRequest(propertyIds);

    Set<Resource> resources = provider.getResources(request, createPredicate(requestedUsername));

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String userName = (String) resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID);
      Assert.assertEquals(requestedUsername, userName);
    }

    verifyAll();
  }

  public void updateResources_SetAdmin(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    Users users = injector.getInstance(Users.class);
    expect(users.getAnyUser(requestedUsername)).andReturn(createMockUser(requestedUsername)).once();

    if ("admin".equals(authentication.getName())) {
      users.grantAdminPrivilege(requestedUsername.hashCode());
      expectLastCall().once();
    }

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, "true");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(request, createPredicate(requestedUsername));

    verifyAll();
  }

  public void updateResources_SetActive(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    Users users = injector.getInstance(Users.class);
    expect(users.getAnyUser(requestedUsername)).andReturn(createMockUser(requestedUsername)).once();

    if ("admin".equals(authentication.getName())) {
      users.setUserActive(requestedUsername, true);
      expectLastCall().once();
    }

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, "true");

    Request request = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(request, createPredicate(requestedUsername));

    verifyAll();
  }

  public void updateResources_SetPassword(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    Users users = injector.getInstance(Users.class);
    expect(users.getAnyUser(requestedUsername)).andReturn(createMockUser(requestedUsername)).once();
    users.modifyPassword(requestedUsername, "old_password", "new_password");
    expectLastCall().once();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();
    properties.put(UserResourceProvider.USER_OLD_PASSWORD_PROPERTY_ID, "old_password");
    properties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "new_password");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(request, createPredicate(requestedUsername));

    verifyAll();
  }

  private void deleteResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    User user = createMockUser(requestedUsername);

    Users users = injector.getInstance(Users.class);
    expect(users.getAnyUser(requestedUsername)).andReturn(user).atLeastOnce();
    users.removeUser(user);
    expectLastCall().atLeastOnce();

    // replay
    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    ResourceProvider provider = getResourceProvider(managementController);

    provider.deleteResources(new RequestImpl(null, null, null, null), createPredicate(requestedUsername));

    // verify
    verifyAll();
  }


  private Predicate createPredicate(String requestedUsername) {
    return new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID)
        .equals(requestedUsername)
        .toPredicate();
  }

  private User createMockUser(String username) {
    User user = createMock(User.class);
    expect(user.getUserId()).andReturn(username.hashCode()).anyTimes();
    expect(user.getUserName()).andReturn(username).anyTimes();
    expect(user.getUserType()).andReturn(UserType.LOCAL).anyTimes();
    expect(user.isLdapUser()).andReturn(false).anyTimes();
    expect(user.isActive()).andReturn(true).anyTimes();
    expect(user.isAdmin()).andReturn(false).anyTimes();
    expect(user.getGroups()).andReturn(Collections.<String>emptyList()).anyTimes();

    return user;
  }

  private ResourceProvider getResourceProvider(AmbariManagementController managementController) {
    return AbstractControllerResourceProvider.getResourceProvider(
        Resource.Type.User,
        PropertyHelper.getPropertyIds(Resource.Type.User),
        PropertyHelper.getKeyPropertyIds(Resource.Type.User),
        managementController);
  }
}