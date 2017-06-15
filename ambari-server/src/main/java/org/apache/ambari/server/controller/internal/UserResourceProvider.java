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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.UserRequest;
import org.apache.ambari.server.controller.UserResponse;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourcePredicateEvaluator;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.entities.MemberEntity;
import org.apache.ambari.server.orm.entities.UserAuthenticationEntity;
import org.apache.ambari.server.orm.entities.UserEntity;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.security.authorization.UserAuthenticationType;
import org.apache.ambari.server.security.authorization.Users;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for user resources.
 */
public class UserResourceProvider extends AbstractControllerResourceProvider implements ResourcePredicateEvaluator {

  // ----- Property ID constants ---------------------------------------------

  // Users
  public static final String USER_USERNAME_PROPERTY_ID = PropertyHelper.getPropertyId("Users", "user_name");
  public static final String USER_PASSWORD_PROPERTY_ID = PropertyHelper.getPropertyId("Users", "password");
  public static final String USER_OLD_PASSWORD_PROPERTY_ID = PropertyHelper.getPropertyId("Users", "old_password");
  @Deprecated
  public static final String USER_LDAP_USER_PROPERTY_ID = PropertyHelper.getPropertyId("Users", "ldap_user");
  @Deprecated
  public static final String USER_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId("Users", "user_type");
  public static final String USER_ACTIVE_PROPERTY_ID = PropertyHelper.getPropertyId("Users", "active");
  public static final String USER_GROUPS_PROPERTY_ID = PropertyHelper.getPropertyId("Users", "groups");
  public static final String USER_ADMIN_PROPERTY_ID = PropertyHelper.getPropertyId("Users", "admin");

  private static Set<String> pkPropertyIds =
      new HashSet<>(Arrays.asList(new String[]{
          USER_USERNAME_PROPERTY_ID}));

  @Inject
  private Users users;

  /**
   * Create a new resource provider for the given management controller.
   */
  @AssistedInject
  UserResourceProvider(@Assisted Set<String> propertyIds,
                       @Assisted Map<Resource.Type, String> keyPropertyIds,
                       @Assisted AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);

    setRequiredCreateAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS));
    setRequiredDeleteAuthorizations(EnumSet.of(RoleAuthorization.AMBARI_MANAGE_USERS));
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    final Set<UserRequest> requests = new HashSet<>();
    for (Map<String, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    createResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        createUsers(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<UserRequest> requests = new HashSet<>();

    if (predicate == null) {
      requests.add(getRequest(null));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<UserResponse> responses = getResources(new Command<Set<UserResponse>>() {
      @Override
      public Set<UserResponse> invoke() throws AmbariException, AuthorizationException {
        return getUsers(requests);
      }
    });

    if (LOG.isDebugEnabled()) {
      LOG.debug("Found user responses matching get user request, userRequestSize={}, userResponseSize={}", requests.size(), responses.size());
    }

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();

    for (UserResponse userResponse : responses) {
      ResourceImpl resource = new ResourceImpl(Resource.Type.User);

      setResourceProperty(resource, USER_USERNAME_PROPERTY_ID,
          userResponse.getUsername(), requestedIds);

      // This is deprecated but here for backwards compatibility
      setResourceProperty(resource, USER_LDAP_USER_PROPERTY_ID,
          userResponse.isLdapUser(), requestedIds);

      // This is deprecated but here for backwards compatibility
      setResourceProperty(resource, USER_TYPE_PROPERTY_ID,
          userResponse.getAuthenticationType(), requestedIds);

      setResourceProperty(resource, USER_ACTIVE_PROPERTY_ID,
          userResponse.isActive(), requestedIds);

      setResourceProperty(resource, USER_GROUPS_PROPERTY_ID,
          userResponse.getGroups(), requestedIds);

      setResourceProperty(resource, USER_ADMIN_PROPERTY_ID,
          userResponse.isAdmin(), requestedIds);

      resources.add(resource);
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<UserRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      UserRequest req = getRequest(propertyMap);

      requests.add(req);
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException, AuthorizationException {
        updateUsers(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResourcesAuthorized(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    final Set<UserRequest> requests = new HashSet<>();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      UserRequest req = getRequest(propertyMap);

      requests.add(req);
    }

    modifyResources(new Command<Void>() {
      @Override
      public Void invoke() throws AmbariException {
        deleteUsers(requests);
        return null;
      }
    });

    return getRequestStatus(null);
  }

  /**
   * ResourcePredicateEvaluator implementation. If property type is User/user_name,
   * we do a case insensitive comparison so that we can return the retrieved
   * username when it differs only in case with respect to the requested username.
   *
   * @param predicate the predicate
   * @param resource  the resource
   * @return
   */
  @Override
  public boolean evaluate(Predicate predicate, Resource resource) {
    if (predicate instanceof EqualsPredicate) {
      EqualsPredicate equalsPredicate = (EqualsPredicate) predicate;
      String propertyId = equalsPredicate.getPropertyId();
      if (propertyId.equals(USER_USERNAME_PROPERTY_ID)) {
        return equalsPredicate.evaluateIgnoreCase(resource);
      }
    }
    return predicate.evaluate(resource);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private UserRequest getRequest(Map<String, Object> properties) {
    if (properties == null) {
      return new UserRequest(null);
    }

    UserRequest request = new UserRequest((String) properties.get(USER_USERNAME_PROPERTY_ID));

    request.setPassword((String) properties.get(USER_PASSWORD_PROPERTY_ID));
    request.setOldPassword((String) properties.get(USER_OLD_PASSWORD_PROPERTY_ID));

    if (null != properties.get(USER_ACTIVE_PROPERTY_ID)) {
      request.setActive(Boolean.valueOf(properties.get(USER_ACTIVE_PROPERTY_ID).toString()));
    }

    if (null != properties.get(USER_ADMIN_PROPERTY_ID)) {
      request.setAdmin(Boolean.valueOf(properties.get(USER_ADMIN_PROPERTY_ID).toString()));
    }

    return request;
  }


  /**
   * Creates users.
   *
   * @param requests the request objects which define the user.
   * @throws AmbariException when the user cannot be created.
   */
  private void createUsers(Set<UserRequest> requests) throws AmbariException {
    for (UserRequest request : requests) {
      String username = request.getUsername();
      String password = request.getPassword();

      if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
        throw new AmbariException("Username and password must be supplied.");
      }

      String displayName = StringUtils.defaultIfEmpty(request.getDisplayName(), username);
      String localUserName = StringUtils.defaultIfEmpty(request.getLocalUserName(), username);

      UserEntity userEntity = users.createUser(username, localUserName, displayName, request.isActive());
      if (userEntity != null) {
        users.addLocalAuthentication(userEntity, password);

        if (Boolean.TRUE.equals(request.isAdmin())) {
          users.grantAdminPrivilege(userEntity);
        }
      }
    }
  }

  /**
   * Updates the users specified.
   *
   * @param requests the users to modify
   * @throws AmbariException          if the resources cannot be updated
   * @throws IllegalArgumentException if the authenticated user is not authorized to update all of
   *                                  the requested properties
   */
  private void updateUsers(Set<UserRequest> requests) throws AmbariException, AuthorizationException {
    boolean isUserAdministrator = AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null,
        RoleAuthorization.AMBARI_MANAGE_USERS);
    String authenticatedUsername = AuthorizationHelper.getAuthenticatedName();

    for (UserRequest request : requests) {
      String requestedUsername = request.getUsername();

      // An administrator can modify any user, else a user can only modify themself.
      if (!isUserAdministrator && (!authenticatedUsername.equalsIgnoreCase(requestedUsername))) {
        throw new AuthorizationException();
      }

      UserEntity userEntity = users.getUserEntity(requestedUsername);
      if (null == userEntity) {
        continue;
      }

      if (null != request.isActive()) {
        // If this value is being set, make sure the authenticated user is an administrator before
        // allowing to change it. Only administrators should be able to change a user's active state
        if (!isUserAdministrator) {
          throw new AuthorizationException("The authenticated user is not authorized to update the requested resource property");
        }
        users.setUserActive(userEntity, request.isActive());
      }

      if (null != request.isAdmin()) {
        // If this value is being set, make sure the authenticated user is an administrator before
        // allowing to change it. Only administrators should be able to change a user's administrative
        // privileges
        if (!isUserAdministrator) {
          throw new AuthorizationException("The authenticated user is not authorized to update the requested resource property");
        }

        if (request.isAdmin()) {
          users.grantAdminPrivilege(userEntity);
        } else {
          users.revokeAdminPrivilege(userEntity);
        }
      }

      if (null != request.getOldPassword() && null != request.getPassword()) {
        users.modifyPassword(userEntity, request.getOldPassword(), request.getPassword());
      }
    }
  }

  /**
   * Deletes the users specified.
   *
   * @param requests the users to delete
   * @throws AmbariException if the resources cannot be deleted
   */
  private void deleteUsers(Set<UserRequest> requests)
      throws AmbariException {

    for (UserRequest r : requests) {
      String username = r.getUsername();
      if (!StringUtils.isEmpty(username)) {

        if (LOG.isDebugEnabled()) {
          LOG.debug("Received a delete user request, username= {}", username);
        }

        users.removeUser(users.getUserEntity(username));
      }
    }
  }

  /**
   * Gets the users identified by the given request objects.
   *
   * @param requests the request objects
   * @return a set of user responses
   * @throws AmbariException if the users could not be read
   */
  private Set<UserResponse> getUsers(Set<UserRequest> requests)
      throws AmbariException, AuthorizationException {

    Set<UserResponse> responses = new HashSet<>();

    for (UserRequest r : requests) {

      if (LOG.isDebugEnabled()) {
        LOG.debug("Received a getUsers request, userRequest={}", r.toString());
      }

      String requestedUsername = r.getUsername();
      String authenticatedUsername = AuthorizationHelper.getAuthenticatedName();

      // A user resource may be retrieved by an administrator or the same user.
      if (!AuthorizationHelper.isAuthorized(ResourceType.AMBARI, null, RoleAuthorization.AMBARI_MANAGE_USERS)) {
        if (null == requestedUsername) {
          // Since the authenticated user is not the administrator, force only that user's resource
          // to be returned
          requestedUsername = authenticatedUsername;
        } else if (!requestedUsername.equalsIgnoreCase(authenticatedUsername)) {
          // Since the authenticated user is not the administrator and is asking for a different user,
          // throw an AuthorizationException
          throw new AuthorizationException();
        }
      }

      // get them all
      if (null == requestedUsername) {
        for (UserEntity u : users.getAllUserEntities()) {
          responses.add(createUserResponse(u));
        }
      } else {

        UserEntity u = users.getUserEntity(requestedUsername);
        if (null == u) {
          if (requests.size() == 1) {
            // only throw exceptin if there is a single request
            // if there are multiple requests, this indicates an OR predicate
            throw new ObjectNotFoundException("Cannot find user '"
                + requestedUsername + "'");
          }
        } else {
          responses.add(createUserResponse(u));
        }
      }
    }

    return responses;
  }

  private UserResponse createUserResponse(UserEntity userEntity) {
    List<UserAuthenticationEntity> authenticationEntities = userEntity.getAuthenticationEntities();
    boolean isLdapUser = false;
    UserAuthenticationType userType = UserAuthenticationType.LOCAL;

      for (UserAuthenticationEntity authenticationEntity : authenticationEntities) {
        if (authenticationEntity.getAuthenticationType() == UserAuthenticationType.LDAP) {
          isLdapUser = true;
          userType = UserAuthenticationType.LDAP;
        } else if (authenticationEntity.getAuthenticationType() == UserAuthenticationType.PAM) {
          userType = UserAuthenticationType.PAM;
        }
    }

    Set<String> groups = new HashSet<>();
    for (MemberEntity memberEntity : userEntity.getMemberEntities()) {
      groups.add(memberEntity.getGroup().getGroupName());
    }

    boolean isAdmin = users.hasAdminPrivilege(userEntity);

    UserResponse userResponse = new UserResponse(userEntity.getUserName(), userType, isLdapUser, userEntity.getActive(), isAdmin);
    userResponse.setGroups(groups);
    return userResponse;
  }

}
