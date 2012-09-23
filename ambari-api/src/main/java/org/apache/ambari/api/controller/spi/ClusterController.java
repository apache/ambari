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
package org.apache.ambari.api.controller.spi;

import java.util.Set;

/**
 * The cluster controller is the main access point for getting properties
 * from the backend providers.  A cluster controller maintains a mapping of
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
   * @return an iterable object of the requested resources
   */
  public Iterable<Resource> getResources(Resource.Type type, Request request, Predicate predicate);

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
  // TODO
}
