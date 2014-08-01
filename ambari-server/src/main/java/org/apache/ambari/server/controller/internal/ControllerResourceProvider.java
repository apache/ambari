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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ControllerRequest;
import org.apache.ambari.server.controller.ControllerResponse;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.commons.lang.StringUtils;

/**
 * Resource provider for controller resource.
 */
class ControllerResourceProvider extends AbstractControllerResourceProvider {

  // ----- Property ID constants ---------------------------------------------

  protected static final String CONTROLLER_NAME_PROPERTY_ID               = PropertyHelper.getPropertyId("Controllers", "name");
  protected static final String CONTROLLER_LDAP_CONFIGURED_PROPERTY_ID    = PropertyHelper.getPropertyId("LDAP", "configured");
  protected static final String CONTROLLER_LDAP_USERS_PROPERTY_ID         = PropertyHelper.getPropertyId("LDAP", "users");
  protected static final String CONTROLLER_LDAP_GROUPS_PROPERTY_ID        = PropertyHelper.getPropertyId("LDAP", "groups");
  protected static final String CONTROLLER_LDAP_SYNCED_USERS_PROPERTY_ID  = PropertyHelper.getPropertyId("LDAP", "synced_users");
  protected static final String CONTROLLER_LDAP_SYNCED_GROUPS_PROPERTY_ID = PropertyHelper.getPropertyId("LDAP", "synced_groups");

  private static Set<String> pkPropertyIds = new HashSet<String>(
      Arrays.asList(new String[] { CONTROLLER_NAME_PROPERTY_ID }));

  private static Map<String, ControllerResponse> controllers = new HashMap<String, ControllerResponse>() {
    {
      for (ControllerType type: ControllerType.values()) {
        put(type.getName(), new ControllerResponse(type.getName()));
      }
    }
  };

  /**
   * Create a new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  ControllerResourceProvider(Set<String> propertyIds,
                       Map<Resource.Type, String> keyPropertyIds,
                       AmbariManagementController managementController) {
    super(propertyIds, keyPropertyIds, managementController);
  }

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException,
      UnsupportedPropertyException,
      ResourceAlreadyExistsException,
      NoSuchParentResourceException {
    // controllers can't be dynamically created
    return getRequestStatus(null);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ControllerRequest> requests = new HashSet<ControllerRequest>();

    if (predicate == null) {
      requests.add(getRequest(null));
    } else {
      for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
        requests.add(getRequest(propertyMap));
      }
    }

    Set<ControllerResponse> responses = getResources(new Command<Set<ControllerResponse>>() {
      @Override
      public Set<ControllerResponse> invoke() throws AmbariException {
        final Set<ControllerResponse> responses = new HashSet<ControllerResponse>();
        for (ControllerRequest request: requests) {
          if (request.getName() == null) {
            responses.addAll(controllers.values());
          } else {
            if (controllers.containsKey(request.getName())) {
              responses.add(controllers.get(request.getName()));
            }
          }
        }
        return responses;
      }
    });

    Set<String>   requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources    = new HashSet<Resource>();

    for (ControllerResponse controllerResponse : responses) {
      ResourceImpl resource = new ResourceImpl(Resource.Type.Controller);

      setResourceProperty(resource, CONTROLLER_NAME_PROPERTY_ID,
          controllerResponse.getName(), requestedIds);

      switch (ControllerType.getByName(controllerResponse.getName())) {
      case LDAP:
        final boolean ldapConfigured = getManagementController().checkLdapConfigured();
        setResourceProperty(resource, CONTROLLER_LDAP_CONFIGURED_PROPERTY_ID,
            ldapConfigured, requestedIds);
        if (ldapConfigured) {
          try {
            final List<String> allUsers = new ArrayList<String>();
            final List<String> syncedUsers = new ArrayList<String>();
            for (Entry<String, Boolean> user : getManagementController().getLdapUsersSyncInfo().entrySet()) {
              allUsers.add(user.getKey());
              if (user.getValue()) {
                syncedUsers.add(user.getKey());
              }
            }
            setResourceProperty(resource, CONTROLLER_LDAP_USERS_PROPERTY_ID,
                allUsers, requestedIds);
            setResourceProperty(resource, CONTROLLER_LDAP_SYNCED_USERS_PROPERTY_ID,
                syncedUsers, requestedIds);
            final List<String> allGroups = new ArrayList<String>();
            final List<String> syncedGroups = new ArrayList<String>();
            for (Entry<String, Boolean> group : getManagementController().getLdapGroupsSyncInfo().entrySet()) {
              allGroups.add(group.getKey());
              if (group.getValue()) {
                syncedGroups.add(group.getKey());
              }
            }
            setResourceProperty(resource, CONTROLLER_LDAP_GROUPS_PROPERTY_ID,
                allGroups, requestedIds);
            setResourceProperty(resource, CONTROLLER_LDAP_SYNCED_GROUPS_PROPERTY_ID,
                syncedGroups, requestedIds);
          } catch (AmbariException ex) {
            throw new SystemException("Can't retrieve data from external LDAP server", ex);
          }
        }
        break;
      }

      resources.add(resource);
    }

    return resources;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
    throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    final Set<ControllerRequest> requests = new HashSet<ControllerRequest>();

    for (Map<String, Object> propertyMap : getPropertyMaps(request.getProperties().iterator().next(), predicate)) {
      final ControllerRequest req = getRequest(propertyMap);
      requests.add(req);
    }

    // one request per each controller
    for (final ControllerRequest controllerRequest: requests) {
      modifyResources(new Command<Void>() {
        @Override
        public Void invoke() throws AmbariException {
          switch (ControllerType.getByName(controllerRequest.getName())) {
          case LDAP:
            Set<String> users = new HashSet<String>();
            if (controllerRequest.getPropertyMap().containsKey(CONTROLLER_LDAP_SYNCED_USERS_PROPERTY_ID)) {
              final String userCsv = (String) controllerRequest.getPropertyMap().get(CONTROLLER_LDAP_SYNCED_USERS_PROPERTY_ID);
              for (String user: userCsv.split(",")) {
                if (StringUtils.isNotEmpty(user)) {
                  users.add(user.toLowerCase());
                }
              }
            }
            Set<String> groups = new HashSet<String>();
            if (controllerRequest.getPropertyMap().containsKey(CONTROLLER_LDAP_SYNCED_GROUPS_PROPERTY_ID)) {
              final String groupCsv = (String) controllerRequest.getPropertyMap().get(CONTROLLER_LDAP_SYNCED_GROUPS_PROPERTY_ID);
              for (String group: groupCsv.split(",")) {
                if (StringUtils.isNotEmpty(group)) {
                  groups.add(group.toLowerCase());
                }
              }
            }
            getManagementController().synchronizeLdapUsersAndGroups(users, groups);
            break;
          }
          return null;
        }
      });
    }

    return getRequestStatus(null);
  }

  @Override
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    // controllers can't be removed
    return getRequestStatus(null);
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return pkPropertyIds;
  }

  private ControllerRequest getRequest(Map<String, Object> properties) {
    if (properties == null) {
      return new ControllerRequest(null, properties);
    }
    final ControllerRequest request = new ControllerRequest((String) properties.get(CONTROLLER_NAME_PROPERTY_ID), properties);
    return request;
  }
}
