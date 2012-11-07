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

import org.apache.ambari.server.AmbariException;

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
   * @throws AmbariException thrown if the resources cannot be obtained
   */
  public Iterable<Resource> getResources(Resource.Type type, Request request,
                                         Predicate predicate) throws AmbariException;

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
   * @throws AmbariException thrown if the resources cannot be created
   */
  public RequestStatus createResources(Resource.Type type, Request request) throws AmbariException;

  /**
   * Update the resources selected by the given predicate with the properties
   * from the given request object.
   *
   * @param type       the type of the resources
   * @param request    the request object which defines the set of properties
   *                   for the resources to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   resources are updated
   *
   * @throws AmbariException thrown if the resource cannot be updated
   */
  public RequestStatus updateResources(Resource.Type type, Request request,
                              Predicate predicate) throws AmbariException;

  /**
   * Delete the resources selected by the given predicate.
   *
   * @param type      the type of the resources
   * @param predicate the predicate object which can be used to filter which
   *                  resources are deleted
   *
   * @throws AmbariException thrown if the resource cannot be deleted
   */
  public RequestStatus deleteResources(Resource.Type type,
                              Predicate predicate) throws AmbariException;
}
