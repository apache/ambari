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

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.UserRequest;
import org.apache.ambari.server.controller.UserResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource provider for user resources.
 */
class UserResourceProvider extends ResourceProviderImpl{

  // ----- Property ID constants ---------------------------------------------

  // Users
  protected static final PropertyId USER_USERNAME_PROPERTY_ID     = PropertyHelper.getPropertyId("user_name", "Users");
  protected static final PropertyId USER_ROLES_PROPERTY_ID        = PropertyHelper.getPropertyId("roles", "Users");
  protected static final PropertyId USER_PASSWORD_PROPERTY_ID     = PropertyHelper.getPropertyId("password", "Users");
  protected static final PropertyId USER_OLD_PASSWORD_PROPERTY_ID = PropertyHelper.getPropertyId("old_password", "Users");

  private static Set<PropertyId> pkPropertyIds =
      new HashSet<PropertyId>(Arrays.asList(new PropertyId[]{
          USER_USERNAME_PROPERTY_ID}));

  /**
   * Create a new resource provider for the given management controller.
   */
  UserResourceProvider(Set<PropertyId> propertyIds,
                       Map<Resource.Type, PropertyId> keyPropertyIds,
                       AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(Request request)
      throws AmbariException {


    Set<UserRequest> requests = new HashSet<UserRequest>();
    for (Map<PropertyId, Object> propertyMap : request.getProperties()) {
      requests.add(getRequest(propertyMap));
    }

    getManagementController().createUsers(requests);

    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws AmbariException {

    UserRequest userRequest = getRequest(getProperties(predicate));

    Set<UserResponse> responses = getManagementController().getUsers(
        Collections.singleton(userRequest));

    Set<Resource> resources = new HashSet<Resource>();
    for (UserResponse userResponse : responses) {
      ResourceImpl resource = new ResourceImpl(Resource.Type.User);

      resource.setProperty(USER_USERNAME_PROPERTY_ID, userResponse.getUsername());

      // TODO support arrays/sets in the JsonSerializer
      if (userResponse.getRoles().size() > 0) {
        int i = 0;
        StringBuilder sb = new StringBuilder();
        for (String role : userResponse.getRoles()) {
          if ((i++) != 0)
            sb.append(',');
          sb.append(role);
        }
        resource.setProperty(USER_ROLES_PROPERTY_ID, sb.toString());
      }

      resources.add(resource);
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws AmbariException {

    Set<UserRequest> requests = new HashSet<UserRequest>();

    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      UserRequest req = getRequest(propertyMap);

      requests.add(req);
    }

    getManagementController().updateUsers(requests);

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws AmbariException {
    Set<UserRequest> requests = new HashSet<UserRequest>();

    for (Map<PropertyId, Object> propertyMap : getPropertyMaps(null, predicate)) {
      UserRequest req = getRequest(propertyMap);

      requests.add(req);

    }
    getManagementController().deleteUsers(requests);

    return getRequestStatus(null);
  }

  @Override
  protected Set<PropertyId> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private UserRequest getRequest(Map<PropertyId, Object> properties) {
    UserRequest request = new UserRequest ((String) properties.get(USER_USERNAME_PROPERTY_ID));

    request.setPassword((String) properties.get(USER_PASSWORD_PROPERTY_ID));
    request.setOldPassword((String) properties.get(USER_OLD_PASSWORD_PROPERTY_ID));

    // TODO - support array/sets directly out of the request
    if (null != properties.get(USER_ROLES_PROPERTY_ID)) {
      HashSet<String> roles = new HashSet<String>();

      Collections.addAll(roles, ((String) properties.get(USER_ROLES_PROPERTY_ID)).split(","));

      request.setRoles(roles);
    }

    return request;
  }
}
