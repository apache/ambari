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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourcePredicateEvaluator;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Resource provider for auth resources.
 */
public class AuthResourceProvider extends AbstractControllerResourceProvider implements ResourcePredicateEvaluator {

  private static final Logger LOG = LoggerFactory.getLogger(AuthResourceProvider.class);

  // ----- Property ID constants ---------------------------------------------

  public static final String AUTH_RESOURCE_CATEGORY = "Auth";

  public static final String USERNAME_PROPERTY_ID = "user_name";

  public static final String AUTH_USERNAME_PROPERTY_ID = AUTH_RESOURCE_CATEGORY + "/" + USERNAME_PROPERTY_ID;

  /**
   * The key property ids for a User resource.
   */
  private static Map<Resource.Type, String> keyPropertyIds = ImmutableMap.<Resource.Type, String>builder()
      .put(Resource.Type.Auth, AUTH_USERNAME_PROPERTY_ID)
      .build();

  private static Set<String> propertyIds = Sets.newHashSet(
      AUTH_USERNAME_PROPERTY_ID
  );

  /**
   * Create a new resource provider for the given management controller.
   */
  @AssistedInject
  AuthResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.Auth, propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResourcesAuthorized(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {

    Set<Resource> resources = new HashSet<>();

    ResourceProvider userResourceProvider =
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.User,
            getManagementController());
    ResourceProvider privilegeResourceProvider =
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.UserPrivilege,
            getManagementController());
    ResourceProvider userAuthenticationSourceResourceProvider =
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.UserAuthenticationSource,
            getManagementController());
    ResourceProvider activeWidgetLayoutResourceProvider =
        AbstractControllerResourceProvider.getResourceProvider(Resource.Type.ActiveWidgetLayout,
            getManagementController());

    for (Map<String, Object> requestProperties : request.getProperties()) {

      if (userResourceProvider != null) {
        try {
          Predicate predicate = getPredicate(requestProperties, UserResourceProvider.USER_USERNAME_PROPERTY_ID);
          resources.addAll(userResourceProvider.getResources(request, predicate));
        } catch (NoSuchResourceException e) {
          throw new SystemException("Error during users retrieving", e);
        }
      }
      if (privilegeResourceProvider != null) {
        try {
          Predicate predicate = getPredicate(requestProperties, UserPrivilegeResourceProvider.USER_NAME);
          resources.addAll(privilegeResourceProvider.getResources(request, predicate));
        } catch (NoSuchResourceException e) {
          throw new SystemException("Error during privileges retrieving", e);
        }
      }
      if (userAuthenticationSourceResourceProvider != null) {
        try {
          Predicate predicate = getPredicate(requestProperties,
              UserAuthenticationSourceResourceProvider.AUTHENTICATION_USER_NAME_PROPERTY_ID);
          resources.addAll(userAuthenticationSourceResourceProvider.getResources(request, predicate));
        } catch (NoSuchResourceException e) {
          throw new SystemException("Error during user authentication sources retrieving", e);
        }
      }
      if (activeWidgetLayoutResourceProvider != null) {
        try {
          Predicate predicate = getPredicate(requestProperties,
              ActiveWidgetLayoutResourceProvider.WIDGETLAYOUT_USERNAME_PROPERTY_ID);
          resources.addAll(activeWidgetLayoutResourceProvider.getResources(request, predicate));
        } catch (NoSuchResourceException e) {
          throw new SystemException("Error during active widgets layouts retrieving", e);
        }
      }
    }
    return getRequestStatus(null, resources);
  }

  /**
   * ResourcePredicateEvaluator implementation. If property type is Auth/user_name,
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
      if (propertyId.equals(AUTH_USERNAME_PROPERTY_ID)) {
        return equalsPredicate.evaluateIgnoreCase(resource);
      }
    }
    return predicate.evaluate(resource);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<>(keyPropertyIds.values());
  }

  private Predicate getPredicate(Map<String, Object> properties, String userNamePropertyId) {
    if (properties == null) {
      return null;
    }
    String userName = (String) properties.get(AUTH_USERNAME_PROPERTY_ID);
    if (userName == null) {
      return null;
    }
    return new EqualsPredicate<>(userNamePropertyId, userName);
  }
}
