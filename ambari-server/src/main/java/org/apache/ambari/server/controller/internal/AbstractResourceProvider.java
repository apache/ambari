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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.DuplicateResourceException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.ParentObjectNotFoundException;
import org.apache.ambari.server.controller.ConfigurationRequest;
import org.apache.ambari.server.controller.RequestStatusResponse;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract resource provider implementation that maps to an Ambari management controller.
 */
public abstract class AbstractResourceProvider extends BaseProvider implements ResourceProvider, ObservableResourceProvider {

  /**
   * Key property mapping by resource type.
   */
  private final Map<Resource.Type, String> keyPropertyIds;

  /**
   * Observers of this observable resource provider.
   */
  private final Set<ResourceProviderObserver> observers = new HashSet<ResourceProviderObserver>();


  protected final static Logger LOG =
      LoggerFactory.getLogger(AbstractResourceProvider.class);

    // ----- Constructors ------------------------------------------------------
  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds           the property ids
   * @param keyPropertyIds        the key property ids
   */
  protected AbstractResourceProvider(Set<String> propertyIds,
                                     Map<Resource.Type, String> keyPropertyIds) {
    super(propertyIds);
    this.keyPropertyIds = keyPropertyIds;
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return keyPropertyIds;
  }


  // ----- ObservableResourceProvider ----------------------------------------

  @Override
  public void updateObservers(ResourceProviderEvent event) {
    for (ResourceProviderObserver observer : observers) {
      observer.update(event);
    }
  }

  @Override
  public void addObserver(ResourceProviderObserver observer) {
    observers.add(observer);
  }


  // ----- utility methods ---------------------------------------------------

  /**
   * Get the set of property ids that uniquely identify the resources
   * of this provider.
   *
   * @return the set of primary key properties
   */
  protected abstract Set<String> getPKPropertyIds();

  /**
   * Notify all listeners of a creation event.
   *
   * @param type     the type of the resources being created
   * @param request  the request used to create the resources
   */
  protected void notifyCreate(Resource.Type type, Request request) {
    updateObservers(new ResourceProviderEvent(type, ResourceProviderEvent.Type.Create, request, null));
  }

  /**
   * Notify all listeners of a update event.
   *
   * @param type       the type of the resources being updated
   * @param request    the request used to update the resources
   * @param predicate  the predicate used to update the resources
   */
  protected void notifyUpdate(Resource.Type type, Request request, Predicate predicate) {
    updateObservers(new ResourceProviderEvent(type, ResourceProviderEvent.Type.Update, request, predicate));
  }

  /**
   * Notify all listeners of a delete event.
   *
   * @param type       the type of the resources being deleted
   * @param predicate  the predicate used to delete the resources
   */
  protected void notifyDelete(Resource.Type type, Predicate predicate) {
    updateObservers(new ResourceProviderEvent(type, ResourceProviderEvent.Type.Delete, null, predicate));
  }

  /**
   * Get a set of properties from the given predicate.  The returned set of
   * property/value mappings is required to generate delete or get requests
   * to the back end which does not deal with predicates.  Note that the
   * single predicate can result in multiple backend requests.
   *
   * @param givenPredicate           the predicate
   *
   * @return the set of properties used to build request objects
   */
  protected Set<Map<String, Object>> getPropertyMaps(Predicate givenPredicate)
    throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {

    SimplifyingPredicateVisitor visitor = new SimplifyingPredicateVisitor(this);
    PredicateHelper.visit(givenPredicate, visitor);
    List<Predicate> predicates = visitor.getSimplifiedPredicates();

    Set<Map<String, Object>> propertyMaps = new HashSet<Map<String, Object>>();

    for (Predicate predicate : predicates) {
      propertyMaps.add(PredicateHelper.getProperties(predicate));
    }
    return propertyMaps;
  }

  /**
   * Get a set of properties from the given property map and predicate.  The
   * returned set of property/value mappings is required to generate update or create
   * requests to the back end which does not deal with predicates.  Note that
   * the single property map & predicate can result in multiple backend requests.
   *
   * @param requestPropertyMap  the request properties (for update)
   * @param givenPredicate      the predicate
   *
   * @return the set of properties used to build request objects
   */
  protected Set<Map<String, Object>> getPropertyMaps(Map<String, Object> requestPropertyMap, Predicate givenPredicate)
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Map<String, Object>> propertyMaps = new HashSet<Map<String, Object>>();

    Set<String> pkPropertyIds = getPKPropertyIds();
    if (requestPropertyMap != null && !pkPropertyIds.equals(PredicateHelper.getPropertyIds(givenPredicate))) {

      for (Resource resource : getResources(PropertyHelper.getReadRequest(pkPropertyIds), givenPredicate)) {
        Map<String, Object> propertyMap = new HashMap<String, Object>(PropertyHelper.getProperties(resource));
        propertyMap.putAll(requestPropertyMap);
        propertyMaps.add(propertyMap);
      }
    }
    else {
      Map<String, Object> propertyMap = new HashMap<String, Object>(PredicateHelper.getProperties(givenPredicate));
      propertyMap.putAll(requestPropertyMap);
      propertyMaps.add(propertyMap);
    }

    return propertyMaps;
  }

  /**
   * Get a request status
   *
   * @return the request status
   */
  protected RequestStatus getRequestStatus(RequestStatusResponse response, Set<Resource> associatedResources) {
    if (response != null){
      Resource requestResource = new ResourceImpl(Resource.Type.Request);
      requestResource.setProperty(PropertyHelper.getPropertyId("Requests", "id"), response.getRequestId());
      requestResource.setProperty(PropertyHelper.getPropertyId("Requests", "status"), "InProgress");
      return new RequestStatusImpl(requestResource, associatedResources);
    }
    return new RequestStatusImpl(null, associatedResources);
  }

  /**
   * Get a request status
   *
   * @return the request status
   */
  protected RequestStatus getRequestStatus(RequestStatusResponse response) {
    return getRequestStatus(response, null) ;
  }

  /**
   * Invoke a command against the Ambari backend to create resources and map
   * any {@link AmbariException} to the types appropriate for the
   * {@link ResourceProvider} interface.
   *
   * @param command  the command to invoke
   * @param <T>      the type of the response
   *
   * @return the response
   *
   * @throws SystemException                thrown if a system exception occurred
   * @throws ResourceAlreadyExistsException thrown if a resource already exists
   * @throws NoSuchParentResourceException  thrown if a parent of a resource doesn't exist
   */
  protected <T> T createResources(Command<T> command)
      throws SystemException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    try {
      return command.invoke();
    } catch (ParentObjectNotFoundException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    } catch (DuplicateResourceException e) {
      throw new ResourceAlreadyExistsException(e.getMessage());
    } catch (AmbariException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught AmbariException when creating a resource", e);
      }
      throw new SystemException("An internal system exception occurred: " + e.getMessage(), e);
    }
  }

  /**
   * Invoke a command against the Ambari backend to get resources and map
   * any {@link AmbariException} to the types appropriate for the
   * {@link ResourceProvider} interface.
   *
   * @param command  the command to invoke
   * @param <T>      the type of the response
   *
   * @return the response
   *
   * @throws SystemException                thrown if a system exception occurred
   * @throws NoSuchParentResourceException  thrown if a parent of a resource doesn't exist
   */
  protected <T> T getResources (Command<T> command)
      throws SystemException, NoSuchResourceException, NoSuchParentResourceException {
    try {
      return command.invoke();
    } catch (ObjectNotFoundException e) {
      throw new NoSuchResourceException("The requested resource doesn't exist: " + e.getMessage(), e);
    } catch (ParentObjectNotFoundException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    } catch (AmbariException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught AmbariException when getting a resource", e);
      }
      throw new SystemException("An internal system exception occurred: " + e.getMessage(), e);
    }
  }

  /**
   * Invoke a command against the Ambari backend to modify resources and map
   * any {@link AmbariException} to the types appropriate for the
   * {@link ResourceProvider} interface.
   *
   * @param command  the command to invoke
   * @param <T>      the type of the response
   *
   * @return the response
   *
   * @throws SystemException                thrown if a system exception occurred
   * @throws NoSuchParentResourceException  thrown if a parent of a resource doesn't exist
   */
  protected <T> T modifyResources (Command<T> command)
      throws SystemException, NoSuchResourceException, NoSuchParentResourceException {
    try {
      return command.invoke();
    } catch (ObjectNotFoundException e) {
      throw new NoSuchResourceException("The specified resource doesn't exist: " + e.getMessage(), e);
    } catch (ParentObjectNotFoundException e) {
      throw new NoSuchParentResourceException(e.getMessage(), e);
    } catch (AmbariException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Caught AmbariException when modifying a resource", e);
      }
      throw new SystemException("An internal system exception occurred: " + e.getMessage(), e);
    }
  }

  /**
   * Helper method to get a configuration request, if one exists.
   * @param parentCategory  the parent category name.  Checks for a property
   *    whose category is the parent and marked as a desired config.
   * @param properties  the properties on the request.
   */
  protected ConfigurationRequest getConfigurationRequest(String parentCategory, Map<String, Object> properties) {

    ConfigurationRequest config = null;

    // as a convenience, allow consumers to specify name/value overrides in this
    // call instead of forcing a cluster call to do that work
    for (Entry<String, Object> entry : properties.entrySet()) {
      String absCategory = PropertyHelper.getPropertyCategory(entry.getKey());
      String propName = PropertyHelper.getPropertyName(entry.getKey());

      if (absCategory.startsWith(parentCategory + "/desired_config")) {
        config = (null == config) ? new ConfigurationRequest() : config;

        if (propName.equals("type"))
          config.setType(entry.getValue().toString());
        else if (propName.equals("tag"))
          config.setVersionTag(entry.getValue().toString());
        else if (propName.equals("selected")) {
          config.setSelected(Boolean.parseBoolean(entry.getValue().toString()));
        }
        else if (absCategory.endsWith("/properties")) {
          config.getProperties().put(propName, entry.getValue().toString());
        }
      }
    }

    return config;
  }

  // ----- Inner interface ---------------------------------------------------

  /**
   * Command to invoke against the Ambari backend.
   *
   * @param <T>  the response type
   */
  protected interface Command<T> {
    /**
     * Invoke this command.
     *
     * @return  the response
     *
     * @throws AmbariException thrown if a problem occurred during invocation
     */
    public T invoke() throws AmbariException;
  }
}
