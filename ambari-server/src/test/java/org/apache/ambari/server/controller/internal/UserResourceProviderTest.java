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

package org.apache.ambari.server.controller.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.ambari.server.actionmanager.ActionDBAccessor;
import org.apache.ambari.server.actionmanager.ActionManager;
import org.apache.ambari.server.actionmanager.RequestFactory;
import org.apache.ambari.server.actionmanager.StageFactory;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariManagementControllerImpl;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.UserResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.orm.entities.PermissionEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.scheduler.ExecutionScheduler;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.authorization.AmbariGrantedAuthority;
import org.apache.ambari.server.security.authorization.User;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.ambari.server.security.encryption.CredentialStoreService;
import org.apache.ambari.server.security.encryption.CredentialStoreServiceImpl;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactory;
import org.apache.ambari.server.stageplanner.RoleGraphFactoryImpl;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigFactory;
import org.apache.ambari.server.state.ServiceComponentFactory;
import org.apache.ambari.server.state.ServiceComponentHostFactory;
import org.apache.ambari.server.state.ServiceFactory;
import org.apache.ambari.server.state.configgroup.ConfigGroupFactory;
import org.apache.ambari.server.state.scheduler.RequestExecutionFactory;
import org.apache.ambari.server.state.stack.OsFamily;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

/**
 * UserResourceProvider tests.
 */
public class UserResourceProviderTest {
  @Test
  public void testCreateResources() throws Exception {
    Resource.Type type = Resource.Type.User;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    managementController.createUsers(AbstractResourceProviderTest.Matcher.getUserRequestSet("User100"));

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.  add more maps for multiple creates
    Set<Map<String, Object>> propertySet = new LinkedHashSet<Map<String, Object>>();

    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    // add properties to the request map
    properties.put(UserResourceProvider.USER_USERNAME_PROPERTY_ID, "User100");

    propertySet.add(properties);

    // create the request
    Request request = PropertyHelper.getCreateRequest(propertySet, null);

    provider.createResources(request);

    // verify
    verify(managementController, response);
  }

  @Test
  public void testGetResources() throws Exception {
    Resource.Type type = Resource.Type.User;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);

    Set<UserResponse> allResponse = new HashSet<UserResponse>();
    allResponse.add(new UserResponse("User100", false, true, false));

    // set expectations
    expect(managementController.getUsers(AbstractResourceProviderTest.Matcher.getUserRequestSet("User100"))).
        andReturn(allResponse).once();

    // replay
    replay(managementController);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Set<String> propertyIds = new HashSet<String>();

    propertyIds.add(UserResourceProvider.USER_USERNAME_PROPERTY_ID);
    propertyIds.add(UserResourceProvider.USER_PASSWORD_PROPERTY_ID);

    Predicate predicate = new PredicateBuilder().property(UserResourceProvider.USER_USERNAME_PROPERTY_ID).
        equals("User100").toPredicate();
    Request request = PropertyHelper.getReadRequest(propertyIds);
    Set<Resource> resources = provider.getResources(request, predicate);

    Assert.assertEquals(1, resources.size());
    for (Resource resource : resources) {
      String userName = (String) resource.getPropertyValue(UserResourceProvider.USER_USERNAME_PROPERTY_ID);
      Assert.assertEquals("User100", userName);
    }

    // verify
    verify(managementController);
  }

  @Test
  public void testUpdateResources_SetAdmin_AsAdminUser() throws Exception {
    Resource.Type type = Resource.Type.User;
    Injector injector = createInjector();

    SecurityHelper securityHelper = injector.getInstance(SecurityHelper.class);
    Users users = injector.getInstance(Users.class);
    User user = createMock(User.class);
    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Collection<? extends GrantedAuthority> currentAuthorities = Collections.singleton(new AmbariGrantedAuthority(privilegeEntity));

    // set expectations
    expect(users.getAnyUser("User100")).andReturn(user).once();

    users.grantAdminPrivilege(1000);
    expectLastCall().once();

    expect(user.getUserId()).andReturn(1000).once();

    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).once();
    expect(permissionEntity.getId()).andReturn(PermissionEntity.AMBARI_ADMIN_PERMISSION).once();

    securityHelper.getCurrentAuthorities();
    expectLastCall().andReturn(currentAuthorities).once();

    // replay
    replay(securityHelper, user, users, privilegeEntity, permissionEntity, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, "true");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID)
        .equals("User100")
        .toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(securityHelper, user, users, privilegeEntity, permissionEntity, response);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUpdateResources_SetAdmin_AsNonAdminUser() throws Exception {
    Resource.Type type = Resource.Type.User;
    Injector injector = createInjector();

    SecurityHelper securityHelper = injector.getInstance(SecurityHelper.class);
    Users users = injector.getInstance(Users.class);
    User user = createMock(User.class);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    expect(users.getAnyUser("User100")).andReturn(user).once();

    securityHelper.getCurrentAuthorities();
    expectLastCall().andReturn(Collections.emptyList()).once();

    // replay
    replay(securityHelper, user, users, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(UserResourceProvider.USER_ADMIN_PROPERTY_ID, "true");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID)
        .equals("User100")
        .toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(securityHelper, user, users, response);
  }

  @Test
  public void testUpdateResources_SetActive_AsAdminUser() throws Exception {
    Resource.Type type = Resource.Type.User;
    Injector injector = createInjector();

    SecurityHelper securityHelper = injector.getInstance(SecurityHelper.class);
    Users users = injector.getInstance(Users.class);
    User user = createMock(User.class);
    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Collection<? extends GrantedAuthority> currentAuthorities = Collections.singleton(new AmbariGrantedAuthority(privilegeEntity));

    // set expectations
    expect(users.getAnyUser("User100")).andReturn(user).once();
    
    users.setUserActive("User100", false);
    expectLastCall().once();

    expect(user.getUserName()).andReturn("User100").once();

    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).once();
    expect(permissionEntity.getId()).andReturn(PermissionEntity.AMBARI_ADMIN_PERMISSION).once();

    securityHelper.getCurrentAuthorities();
    expectLastCall().andReturn(currentAuthorities).once();

    // replay
    replay(securityHelper, user, users, privilegeEntity, permissionEntity, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, "false");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID)
        .equals("User100")
        .toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(securityHelper, user, users, privilegeEntity, permissionEntity, response);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUpdateResources_SetActive_AsNonActiveUser() throws Exception {
    Resource.Type type = Resource.Type.User;
    Injector injector = createInjector();

    SecurityHelper securityHelper = injector.getInstance(SecurityHelper.class);
    Users users = injector.getInstance(Users.class);
    User user = createMock(User.class);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    expect(users.getAnyUser("User100")).andReturn(user).once();

    securityHelper.getCurrentAuthorities();
    expectLastCall().andReturn(Collections.emptyList()).once();

    // replay
    replay(securityHelper, user, users, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(UserResourceProvider.USER_ACTIVE_PROPERTY_ID, "false");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID)
        .equals("User100")
        .toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(securityHelper, user, users, response);
  }

  @Test
  public void testUpdateResources_SetPassword_AsAdminUser() throws Exception {
    Resource.Type type = Resource.Type.User;
    Injector injector = createInjector();

    SecurityHelper securityHelper = injector.getInstance(SecurityHelper.class);
    Users users = injector.getInstance(Users.class);
    User user = createMock(User.class);
    PrivilegeEntity privilegeEntity = createMock(PrivilegeEntity.class);
    PermissionEntity permissionEntity = createMock(PermissionEntity.class);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    Collection<? extends GrantedAuthority> currentAuthorities = Collections.singleton(new AmbariGrantedAuthority(privilegeEntity));

    // set expectations
    expect(users.getAnyUser("User100")).andReturn(user).once();

    users.modifyPassword("User100", "old_password", "password");
    expectLastCall().once();

    expect(user.getUserName()).andReturn("User100").once();

    expect(privilegeEntity.getPermission()).andReturn(permissionEntity).anyTimes();
    expect(permissionEntity.getId()).andReturn(PermissionEntity.AMBARI_ADMIN_PERMISSION).anyTimes();

    securityHelper.getCurrentAuthorities();
    expectLastCall().andReturn(currentAuthorities).anyTimes();

    // replay
    replay(securityHelper, user, users, privilegeEntity, permissionEntity, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password");
    properties.put(UserResourceProvider.USER_OLD_PASSWORD_PROPERTY_ID, "old_password");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID)
        .equals("User100")
        .toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(securityHelper, user, users, privilegeEntity, permissionEntity, response);
  }

  @Test
  public void testUpdateResources_SetPassword_AsNonActiveUser() throws Exception {
    Resource.Type type = Resource.Type.User;
    Injector injector = createInjector();

    SecurityHelper securityHelper = injector.getInstance(SecurityHelper.class);
    Users users = injector.getInstance(Users.class);
    User user = createMock(User.class);

    AmbariManagementController managementController = injector.getInstance(AmbariManagementController.class);

    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    expect(users.getAnyUser("User100")).andReturn(user).once();

    users.modifyPassword("User100", "old_password", "password");
    expectLastCall().once();

    expect(user.getUserName()).andReturn("User100").once();

    securityHelper.getCurrentAuthorities();
    expectLastCall().andReturn(Collections.emptyList()).anyTimes();

    // replay
    replay(securityHelper, user, users, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    // add the property map to a set for the request.
    Map<String, Object> properties = new LinkedHashMap<String, Object>();

    properties.put(UserResourceProvider.USER_PASSWORD_PROPERTY_ID, "password");
    properties.put(UserResourceProvider.USER_OLD_PASSWORD_PROPERTY_ID, "old_password");

    // create the request
    Request request = PropertyHelper.getUpdateRequest(properties, null);

    Predicate predicate = new PredicateBuilder()
        .property(UserResourceProvider.USER_USERNAME_PROPERTY_ID)
        .equals("User100")
        .toPredicate();
    provider.updateResources(request, predicate);

    // verify
    verify(securityHelper, user, users, response);
  }

  @Test
  public void testDeleteResources() throws Exception {
    Resource.Type type = Resource.Type.User;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    RequestStatusResponse response = createNiceMock(RequestStatusResponse.class);

    // set expectations
    managementController.deleteUsers(AbstractResourceProviderTest.Matcher.getUserRequestSet("User100"));

    // replay
    replay(managementController, response);

    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        PropertyHelper.getPropertyIds(type),
        PropertyHelper.getKeyPropertyIds(type),
        managementController);

    Predicate predicate = new PredicateBuilder().property(UserResourceProvider.USER_USERNAME_PROPERTY_ID).
        equals("User100").toPredicate();
    provider.deleteResources(predicate);

    // verify
    verify(managementController, response);
  }

  private Injector createInjector() {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(DBAccessor.class).toInstance(createNiceMock(DBAccessor.class));
        bind(ActionDBAccessor.class).toInstance(createNiceMock(ActionDBAccessor.class));
        bind(ExecutionScheduler.class).toInstance(createNiceMock(ExecutionScheduler.class));
        bind(SecurityHelper.class).toInstance(createMock(SecurityHelper.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(AmbariMetaInfo.class).toInstance(createMock(AmbariMetaInfo.class));
        bind(ActionManager.class).toInstance(createNiceMock(ActionManager.class));
        bind(RequestFactory.class).toInstance(createNiceMock(RequestFactory.class));
        bind(RequestExecutionFactory.class).toInstance(createNiceMock(RequestExecutionFactory.class));
        bind(StageFactory.class).toInstance(createNiceMock(StageFactory.class));
        bind(RoleGraphFactory.class).to(RoleGraphFactoryImpl.class);
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
        bind(CredentialStoreService.class).to(CredentialStoreServiceImpl.class);
      }
    });
  }
}
