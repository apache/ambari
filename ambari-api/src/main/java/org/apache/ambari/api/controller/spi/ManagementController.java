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
 * Management controller interface.
 */
public interface ManagementController {


  // ----- Create -----------------------------------------------------------

  /**
   * Create the clusters defined by the properties in the given request object.
   *
   * @param request  the request object which defines the set of properties
   *                 for the clusters to be created
   */
  public void createClusters(Request request);

  /**
   * Create the services defined by the properties in the given request object.
   *
   * @param request  the request object which defines the set of properties
   *                 for the services to be created
   */
  public void createServices(Request request);

  /**
   * Create the components defined by the properties in the given request object.
   *
   * @param request  the request object which defines the set of properties
   *                 for the components to be created
   */
  public void createComponents(Request request);

  /**
   * Create the hosts defined by the properties in the given request object.
   *
   * @param request  the request object which defines the set of properties
   *                 for the hosts to be created
   */
  public void createHosts(Request request);

  /**
   * Create the host components defined by the properties in the given request object.
   *
   * @param request  the request object which defines the set of properties
   *                 for the host components to be created
   */
  public void createHostComponents(Request request);


  // ----- Read -------------------------------------------------------------

  /**
   * Get a set of cluster {@link Resource resources} based on the given request and predicate
   * information.
   *
   * @param request    the request object which defines the desired set of properties
   * @param predicate  the predicate object which can be used to filter which
   *                   clusters are returned
   * @return a set of cluster resources based on the given request and predicate information
   */
  public Set<Resource> getClusters(Request request, Predicate predicate);

  /**
   * Get a set of service {@link Resource resources} based on the given request and predicate
   * information.
   *
   * @param request    the request object which defines the desired set of properties
   * @param predicate  the predicate object which can be used to filter which
   *                   services are returned
   * @return a set of service resources based on the given request and predicate information
   */
  public Set<Resource> getServices(Request request, Predicate predicate);

  /**
   * Get a set of component {@link Resource resources} based on the given request and predicate
   * information.
   *
   * @param request    the request object which defines the desired set of properties
   * @param predicate  the predicate object which can be used to filter which
   *                   components are returned
   * @return a set of component resources based on the given request and predicate information
   */
  public Set<Resource> getComponents(Request request, Predicate predicate);

  /**
   * Get a set of host {@link Resource resources} based on the given request and predicate
   * information.
   *
   * @param request    the request object which defines the desired set of properties
   * @param predicate  the predicate object which can be used to filter which
   *                   hosts are returned
   * @return a set of host resources based on the given request and predicate information
   */
  public Set<Resource> getHosts(Request request, Predicate predicate);

  /**
   * Get a set of host component {@link Resource resources} based on the given request and predicate
   * information.
   *
   * @param request    the request object which defines the desired set of properties
   * @param predicate  the predicate object which can be used to filter which
   *                   host components are returned
   * @return a set of host component resources based on the given request and predicate information
   */
  public Set<Resource> getHostComponents(Request request, Predicate predicate);


  // ----- Update -----------------------------------------------------------

  /**
   * Update the clusters selected by the given predicate with the properties
   * from the given request object.
   *
   * @param request    the request object which defines the set of properties
   *                   for the clusters to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   clusters are updated
   */
  public void updateClusters(Request request, Predicate predicate);

  /**
   * Update the services selected by the given predicate with the properties
   * from the given request object.
   *
   * @param request    the request object which defines the set of properties
   *                   for the services to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   services are updated
   */
  public void updateServices(Request request, Predicate predicate);

  /**
   * Update the components selected by the given predicate with the properties
   * from the given request object.
   *
   * @param request    the request object which defines the set of properties
   *                   for the components to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   components are updated
   */
  public void updateComponents(Request request, Predicate predicate);

  /**
   * Update the hosts selected by the given predicate with the properties
   * from the given request object.
   *
   * @param request    the request object which defines the set of properties
   *                   for the hosts to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   hosts are updated
   */
  public void updateHosts(Request request, Predicate predicate);

  /**
   * Update the host components selected by the given predicate with the properties
   * from the given request object.
   *
   * @param request    the request object which defines the set of properties
   *                   for the host components to be updated
   * @param predicate  the predicate object which can be used to filter which
   *                   host components are updated
   */
  public void updateHostComponents(Request request, Predicate predicate);


  // ----- Delete -----------------------------------------------------------

  /**
   * Delete the clusters selected by the given predicate.
   *
   * @param predicate the predicate object which can be used to filter which
   *                  clusters are deleted
   */
  public void deleteClusters(Predicate predicate);

  /**
   * Delete the services selected by the given predicate.
   *
   * @param predicate the predicate object which can be used to filter which
   *                  services are deleted
   */
  public void deleteServices(Predicate predicate);

  /**
   * Delete the components selected by the given predicate.
   *
   * @param predicate the predicate object which can be used to filter which
   *                  components are deleted
   */
  public void deleteComponents(Predicate predicate);

  /**
   * Delete the hosts selected by the given predicate.
   *
   * @param predicate the predicate object which can be used to filter which
   *                  hosts are deleted
   */
  public void deleteHosts(Predicate predicate);

  /**
   * Delete the host components selected by the given predicate.
   *
   * @param predicate the predicate object which can be used to filter which
   *                  host components are deleted
   */
  public void deleteHostComponents(Predicate predicate);
}
