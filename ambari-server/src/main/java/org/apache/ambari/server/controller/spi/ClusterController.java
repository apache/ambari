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
package org.apache.ambari.server.controller.spi;

import java.util.Set;

/**
 * The cluster controller is the main access point for accessing resources
 * from the backend sources.  A cluster controller maintains a mapping of
 * resource providers keyed by resource types.
 */
public interface ClusterController {


  // ----- Monitoring ------------------------------------------------------

  /**
   * Get the resources of the given type filtered by the given request and
   * predicate objects.
   *
   * @param type      the type of the requested resources
   * @param request   the request object which defines the desired set of properties
   * @param predicate the predicate object which filters which resources are returned
   *
   * @return an iterable object of the requested resources
   *
   * @throws UnsupportedPropertyException thrown if the request or predicate contain
   *                                      unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws NoSuchResourceException no matching resource(s) found
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  Set<Resource> getResources(Resource.Type type, Request request, Predicate predicate)
      throws UnsupportedPropertyException,
      NoSuchResourceException,
      NoSuchParentResourceException,
      SystemException;

  /**
   * Populate the given resources from the associated property providers.  This
   * method may filter the resources based on the predicate and return a subset
   * of the given resources.
   *
   * @param type       the resource type
   * @param resources  the resources to be populated
   * @param request    the request
   * @param predicate  the predicate
   *
   * @return the set of resources that were successfully populated
   *
   * @throws SystemException if unable to populate the resources
   */
  public Set<Resource> populateResources(Resource.Type type,
                                            Set<Resource> resources,
                                            Request request,
                                            Predicate predicate) throws SystemException;

  /**
   * Get an iterable set of resources from the given set of resources filtered by the
   * given request and predicate objects.
   *
   * @param type               type of resources
   * @param providerResources  set of populated Resources
   * @param request            the request
   * @param predicate          the predicate object which filters which resources are returned
   *
   * @return a page response representing the requested page of resources
   *
   * @throws UnsupportedPropertyException thrown if the request or predicate contain
   *                                      unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws NoSuchResourceException no matching resource(s) found
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  Iterable<Resource> getIterable(Resource.Type type, Set<Resource> providerResources,
                                 Request request, Predicate predicate)
      throws NoSuchParentResourceException,
      UnsupportedPropertyException,
      NoSuchResourceException,
      SystemException;

  /**
   * Get a page of resources from the given set filtered by the given request,
   * predicate objects and page request.
   *
   * @param type               type of resources
   * @param providerResources  set of populated Resources
   * @param request            the request
   * @param predicate          the predicate object which filters which resources are returned
   * @param pageRequest        the page request for a paginated response
   *
   * @return a page response representing the requested page of resources
   *
   * @throws UnsupportedPropertyException thrown if the request or predicate contain
   *                                      unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws NoSuchResourceException no matching resource(s) found
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  PageResponse getPage(Resource.Type type, Set<Resource> providerResources,
                       Request request, Predicate predicate, PageRequest pageRequest)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException;

  /**
   * Get the {@link Schema schema} for the given resource type.  The schema
   * for a given resource type describes the properties and categories provided
   * by that type of resource.
   *
   * @param type the resource type
   * @return the schema object for the given resource
   */
  public Schema getSchema(Resource.Type type);


  // ----- Management -------------------------------------------------------

  /**
   * Create the resources defined by the properties in the given request object.
   *
   * @param type     the type of the resources
   * @param request  the request object which defines the set of properties
   *                 for the resources to be created
   *
   * @throws UnsupportedPropertyException thrown if the request contains
   *                                      unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws ResourceAlreadyExistsException attempted to create a resource that already exists
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  public RequestStatus createResources(Resource.Type type, Request request)
      throws UnsupportedPropertyException,
             SystemException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException;

  /**
   * Update the resources selected by the given predicate with the properties
   * from the given request object.
   *
   *
   * @param type       the type of the resources
   * @param request    the request object which defines the set of properties
   *                   for the resources to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   resources are updated
   *
   * @throws UnsupportedPropertyException thrown if the request or predicate
   *                                      contain unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws NoSuchResourceException no matching resource(s) found
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  public RequestStatus updateResources(Resource.Type type,
                                       Request request,
                                       Predicate predicate)
      throws UnsupportedPropertyException,
             SystemException,
             NoSuchResourceException,
             NoSuchParentResourceException;

  /**
   * Delete the resources selected by the given predicate.
   *
   * @param type      the type of the resources
   * @param predicate the predicate object which can be used to filter which
   *                  resources are deleted
   *
   * @throws UnsupportedPropertyException thrown if the predicate contains
   *                                      unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws NoSuchResourceException no matching resource(s) found
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  public RequestStatus deleteResources(Resource.Type type, Predicate predicate)
      throws UnsupportedPropertyException,
             SystemException,
             NoSuchResourceException,
             NoSuchParentResourceException ;
}
