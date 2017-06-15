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
import org.apache.ambari.server.actionmanager.HostRoleCommandFactory;
import org.apache.ambari.server.actionmanager.HostRoleCommandFactoryImpl;
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
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
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
import org.apache.ambari.server.state.UpgradeContextFactory;
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
        install(new FactoryModuleBuilder().build(UpgradeContextFactory.class));
        install(new FactoryModuleBuilder().build(RoleGraphFactory.class));

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
        bind(HostRoleCommandFactory.class).to(HostRoleCommandFactoryImpl.class);
      }
    });
  }


  private void createResourcesTest(Authentication authentication) throws Exception {
    Injector injector = createInjector();

    UserEntity userEntity100 = createNiceMock(UserEntity.class);
    UserEntity userEntity200 = createNiceMock(UserEntity.class);

    Users users = injector.getInstance(Users.class);
    expect(users.createUser("User100", "User100", "User100", null))
        .andReturn(userEntity100)
        .once();
    expect(users.createUser("user200", "user200", "user200", null))
        .andReturn(userEntity200)
        .once();

    users.addLocalAuthentication(userEntity100, "password100");
    users.addLocalAuthentication(userEntity200, "password200");
    expectLastCall().once();

    // replay
    replayAll();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    ResourceProvider provider = getResourceProvider(injector);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<>();

    Map<String, Object> properties;

    properties = new LinkedHashMap<>();
    properties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User100");
    properties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password100");
    propertySet.add(properties);

    properties = new LinkedHashMap<>();
    properties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "user200");
    properties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password200");
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
      UserEntity userEntity1 = createMockUserEntity("User1");
      UserEntity userEntity10 = createMockUserEntity("User10");
      UserEntity userEntity100 = createMockUserEntity("User100");
      UserEntity userEntityAdmin = createMockUserEntity("admin");

      List<UserEntity> allUsers = Arrays.asList(userEntity1, userEntity10, userEntity100, userEntityAdmin);

      expect(users.getAllUserEntities()).andReturn(allUsers).once();
      expect(users.hasAdminPrivilege(userEntity1)).andReturn(false).once();
      expect(users.hasAdminPrivilege(userEntity10)).andReturn(false).once();
      expect(users.hasAdminPrivilege(userEntity100)).andReturn(false).once();
      expect(users.hasAdminPrivilege(userEntityAdmin)).andReturn(true).once();
    } else {

      UserEntity userEntity = createMockUserEntity("User1");
      expect(users.getUserEntity("User1")).andReturn(userEntity).once();
      expect(users.hasAdminPrivilege(userEntity)).andReturn(false).once();
    }

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    Set<String> propertyIds = new HashSet<>();
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

    UserEntity userEntity = createMockUserEntity(requestedUsername);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(requestedUsername)).andReturn(userEntity).once();
    expect(users.hasAdminPrivilege(userEntity)).andReturn(false).once();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    Set<String> propertyIds = new HashSet<>();
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

  private void updateResources_SetAdmin(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    UserEntity userEntity = createMockUserEntity(requestedUsername);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(requestedUsername)).andReturn(userEntity).once();

    if ("admin".equals(authentication.getName())) {
      users.grantAdminPrivilege(userEntity);
      expectLastCall().once();
    }

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, "true");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(request, createPredicate(requestedUsername));

    verifyAll();
  }

  private void updateResources_SetActive(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    UserEntity userEntity = createMockUserEntity(requestedUsername);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(requestedUsername)).andReturn(userEntity).once();

    if ("admin".equals(authentication.getName())) {
      users.setUserActive(userEntity, true);
      expectLastCall().once();
    }

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, "true");

    Request request = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(request, createPredicate(requestedUsername));

    verifyAll();
  }

  private void updateResources_SetPassword(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    UserEntity userEntity = createMockUserEntity(requestedUsername);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(requestedUsername)).andReturn(userEntity).once();
    users.modifyPassword(userEntity, "old_password", "new_password");
    expectLastCall().once();

    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(UserResourceProvider.USER_OLD_PASSWORD_PROPERTY_ID, "old_password");
    properties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "new_password");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    provider.updateResources(request, createPredicate(requestedUsername));

    verifyAll();
  }

  private void deleteResourcesTest(Authentication authentication, String requestedUsername) throws Exception {
    Injector injector = createInjector();

    UserEntity userEntity = createMockUserEntity(requestedUsername);

    Users users = injector.getInstance(Users.class);
    expect(users.getUserEntity(requestedUsername)).andReturn(userEntity).once();
    users.removeUser(userEntity);
    expectLastCall().atLeastOnce();

    // replay
    replayAll();

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    ambariMetaInfo.init();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    ResourceProvider provider = getResourceProvider(injector);

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

  private UserEntity createMockUserEntity(String username) {
    UserEntity userEntity = createMock(UserEntity.class);
    expect(userEntity.getUserId()).andReturn(username.hashCode()).anyTimes();
    expect(userEntity.getUserName()).andReturn(username).anyTimes();
    expect(userEntity.getActive()).andReturn(true).anyTimes();
    expect(userEntity.getAuthenticationEntities()).andReturn(Collections.<UserAuthenticationEntity>emptyList()).anyTimes();
    expect(userEntity.getMemberEntities()).andReturn(Collections.<MemberEntity>emptySet()).anyTimes();
    return userEntity;
  }

  private ResourceProvider getResourceProvider(Injector injector) {
    UserResourceProvider resourceProvider = new UserResourceProvider(
        PropertyHelper.getPropertyIds(Resource.Type.User),
        PropertyHelper.getKeyPropertyIds(Resource.Type.User),
        injector.getInstance(AmbariManagementController.class));

    injector.injectMembers(resourceProvider);
    return resourceProvider;
  }
}