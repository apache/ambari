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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic resource provider implementation that maps to a management controller.
 */
public abstract class ResourceProviderImpl implements ResourceProvider {

  /**
   * The set of property ids supported by this resource provider.
   */
  private final Set<PropertyId> propertyIds;

  /**
   * The management controller to delegate to.
   */
  private final AmbariManagementController managementController;

  /**
   * Key property mapping by resource type.
   */
  private final Map<Resource.Type, PropertyId> keyPropertyIds;


  protected final static Logger LOG =
      LoggerFactory.getLogger(ResourceProviderImpl.class);

    // ----- Constructors ------------------------------------------------------
  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   * @param managementController  the management controller
   */
  protected ResourceProviderImpl(Set<PropertyId> propertyIds,
                               Map<Resource.Type, PropertyId> keyPropertyIds,
                               AmbariManagementController managementController) {
    this.propertyIds          = propertyIds;
    this.keyPropertyIds       = keyPropertyIds;
    this.managementController = managementController;
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public Set<PropertyId> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public Map<Resource.Type, PropertyId> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- accessors ---------------------------------------------------------

  /**
   * Get the associated management controller.
   *
   * @return the associated management controller
   */
  public AmbariManagementController getManagementController() {
    return managementController;
  }


  // ----- utility methods ---------------------------------------------------

  protected abstract Set<PropertyId> getPKPropertyIds();


  /**
   * Get a set of properties from the given property map and predicate.
   *
   * @param requestPropertyMap
   * @param predicate
   *
   * @return the set of properties
   *
   * @throws AmbariException
   */
  protected Set<Map<PropertyId, Object>> getPropertyMaps(Map<PropertyId, Object> requestPropertyMap,
                                                         Predicate predicate)
      throws AmbariException{

    Set<PropertyId>              pkPropertyIds       = getPKPropertyIds();
    Set<Map<PropertyId, Object>> properties          = new HashSet<Map<PropertyId, Object>>();
    Set<Map<PropertyId, Object>> predicateProperties = new HashSet<Map<PropertyId, Object>>();

    if (predicate != null && pkPropertyIds.equals(PredicateHelper.getPropertyIds(predicate))) {
      predicateProperties.add(getProperties(predicate));
    } else {
      for (Resource resource : getResources(PropertyHelper.getReadRequest(pkPropertyIds), predicate)) {
        predicateProperties.add(PropertyHelper.getProperties(resource));
      }
    }

    for (Map<PropertyId, Object> predicatePropertyMap : predicateProperties) {
      // get properties from the given request properties
      Map<PropertyId, Object> propertyMap = requestPropertyMap == null ?
          new HashMap<PropertyId, Object>():
          new HashMap<PropertyId, Object>(requestPropertyMap);
      // add the pk properties
      setProperties(propertyMap, predicatePropertyMap, pkPropertyIds);
      properties.add(propertyMap);
    }
    return properties;
  }

  /**
   * Get a request status
   *
   * @return the request status
   */
  protected RequestStatus getRequestStatus(RequestStatusResponse response) {

    if (response != null){
      Resource requestResource = new ResourceImpl(Resource.Type.Request);
      requestResource.setProperty(PropertyHelper.getPropertyId("id", "Requests"), response.getRequestId());
      // TODO : how do we tell what a request status is?
      // for now make everything InProgress
      requestResource.setProperty(PropertyHelper.getPropertyId("status", "Requests"), "InProgress");
      return new RequestStatusImpl(requestResource);
    }
    return new RequestStatusImpl(null);
  }

  /**
   * Get a map of property values from a given predicate.
   *
   * @param predicate  the predicate
   *
   * @return the map of properties
   */
  protected static Map<PropertyId, Object> getProperties(Predicate predicate) {
    if (predicate == null) {
      return Collections.emptyMap();
    }
    PropertyPredicateVisitor visitor = new PropertyPredicateVisitor();
    PredicateHelper.visit(predicate, visitor);
    return visitor.getProperties();
  }

  /**
   * Transfer property values from one map to another for the given list of property ids.
   *
   * @param to           the target map
   * @param from         the source map
   * @param propertyIds  the set of property ids
   */
  protected static void setProperties(Map<PropertyId, Object> to, Map<PropertyId, Object> from, Set<PropertyId> propertyIds) {
    for (PropertyId propertyId : propertyIds) {
      if (from.containsKey(propertyId)) {
        to.put(propertyId, from.get(propertyId));
      }
    }
  }

  /**
   * Set a property value on the given resource for the given id and value.
   * Make sure that the id is in the given set of requested ids.
   *
   * @param resource      the resource
   * @param propertyId    the property id
   * @param value         the value to set
   * @param requestedIds  the requested set of property ids
   */
  protected static void setResourceProperty(Resource resource, PropertyId propertyId, Object value, Set<PropertyId> requestedIds) {
    if (requestedIds.contains(propertyId)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Setting property for resource"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId.getName()
            + ", value=" + value);
      }
      resource.setProperty(propertyId, value);
    }
    else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Skipping property for resource as not in requestedIds"
            + ", resourceType=" + resource.getType()
            + ", propertyId=" + propertyId.getName()
            + ", value=" + value);
      }
    }
  }

  /**
   * Factory method for obtaining a resource provider based on a given type and management controller.
   *
   *
   * @param type                  the resource type
   * @param propertyIds           the property ids
   * @param managementController  the management controller
   *
   * @return a new resource provider
   */
  public static ResourceProvider getResourceProvider(Resource.Type type,
                                                     Set<PropertyId> propertyIds,
                                                     Map<Resource.Type, PropertyId> keyPropertyIds,
                                                     AmbariManagementController managementController) {
    switch (type) {
      case Cluster:
        return new ClusterResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Service:
        return new ServiceResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Component:
        return new ComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Host:
        return new HostResourceProvider(propertyIds, keyPropertyIds, managementController);
      case HostComponent:
        return new HostComponentResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Configuration:
        return new ConfigurationResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Action:
        return new ActionResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Request:
        return new RequestResourceProvider(propertyIds, keyPropertyIds, managementController);
      case Task:
        return new TaskResourceProvider(propertyIds, keyPropertyIds, managementController);
      case User:
        return new UserResourceProvider(propertyIds, keyPropertyIds, managementController);
      default:
        throw new IllegalArgumentException("Unknown type " + type);
    }
  }
}
