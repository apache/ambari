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

package org.apache.ambari.server.controller.gsinstaller;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.RequestStatus;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;


/**
 * An abstract resource provider for a gsInstaller defined cluster.
 */
public abstract class GSInstallerResourceProvider implements ResourceProvider {

  private final ClusterDefinition clusterDefinition;

  private final Set<Resource> resources = new HashSet<Resource>();

  private final Resource.Type type;

  private final Set<String> propertyIds;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public GSInstallerResourceProvider(Resource.Type type, ClusterDefinition clusterDefinition) {
    this.type              = type;
    this.clusterDefinition = clusterDefinition;

    Set<String> propertyIds = PropertyHelper.getPropertyIds(type);
    this.propertyIds = new HashSet<String>(propertyIds);
    this.propertyIds.addAll(PropertyHelper.getCategories(propertyIds));
  }


  // ----- ResourceProvider --------------------------------------------------

  @Override
  public RequestStatus createResources(Request request)
      throws SystemException, UnsupportedPropertyException, ResourceAlreadyExistsException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Management operations are not supported");
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    Set<Resource> resultSet = new HashSet<Resource>();

    for (Resource resource : resources) {
      if (predicate == null || predicate.evaluate(resource)) {
        ResourceImpl newResource = new ResourceImpl(resource);
        updateProperties(newResource, request, predicate);
        resultSet.add(newResource);
      }
    }
    return resultSet;
  }

  @Override
  public RequestStatus updateResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Management operations are not supported");
  }

  @Override
  public RequestStatus deleteResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Management operations are not supported");
  }

  @Override
  public Map<Resource.Type, String> getKeyPropertyIds() {
    return PropertyHelper.getKeyPropertyIds(type);
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = new HashSet<String>(propertyIds);
    propertyIds.removeAll(this.propertyIds);
    return propertyIds;
  }


  // ----- GSInstallerResourceProvider ---------------------------------------

  /**
   * Update the resource with any properties handled by the resource provider.
   *
   * @param resource   the resource to update
   * @param request    the request
   * @param predicate  the predicate
   */
  public abstract void updateProperties(Resource resource, Request request, Predicate predicate);


  // ----- accessors ---------------------------------------------------------

  /**
   * Get the configuration provider.
   *
   * @return the configuration provider
   */
  protected ClusterDefinition getClusterDefinition() {
    return clusterDefinition;
  }

  /**
   * Get the resource provider type.
   *
   * @return the type
   */
  public Resource.Type getType() {
    return type;
  }


// ----- helper methods ----------------------------------------------------

  /**
   * Get the set of property ids required to satisfy the given request.
   *
   * @param request              the request
   * @param predicate            the predicate
   *
   * @return the set of property ids needed to satisfy the request
   */
  protected Set<String> getRequestPropertyIds(Request request, Predicate predicate) {
    Set<String> propertyIds  = request.getPropertyIds();

    // if no properties are specified, then return them all
    if (propertyIds == null || propertyIds.isEmpty()) {
      return new HashSet<String>(this.propertyIds);
    }

    propertyIds = new HashSet<String>(propertyIds);

    if (predicate != null) {
      propertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }
    return propertyIds;
  }

  /**
   * Check to see if the given set contains a property or category id that matches the given property id.
   *
   * @param ids         the set of property/category ids
   * @param propertyId  the property id
   *
   * @return true if the given set contains a property id or category that matches the given property id
   */
  protected static boolean contains(Set<String> ids, String propertyId) {
    boolean contains = ids.contains(propertyId);

    if (!contains) {
      String category = PropertyHelper.getPropertyCategory(propertyId);
      while (category != null && !contains) {
        contains = ids.contains(category);
        category = PropertyHelper.getPropertyCategory(category);
      }
    }
    return contains;
  }

  /**
  * Add a resource to the set of resources provided by this provider.
  *
  * @param resource  the resource to add
  */
  protected void addResource(Resource resource) {
    resources.add(resource);
  }

  /**
   * Factory method for obtaining a resource provider based on a given type.
   *
   * @param type               the resource type
   * @param clusterDefinition  the cluster definition
   *
   * @return a new resource provider
   */
  public static ResourceProvider getResourceProvider(Resource.Type type,
                                                     ClusterDefinition clusterDefinition) {
    switch (type.getInternalType()) {
      case Cluster:
        return new GSInstallerClusterProvider(clusterDefinition);
      case Service:
        return new GSInstallerServiceProvider(clusterDefinition);
      case Component:
        return new GSInstallerComponentProvider(clusterDefinition);
      case Host:
        return new GSInstallerHostProvider(clusterDefinition);
      case HostComponent:
        return new GSInstallerHostComponentProvider(clusterDefinition);
      default:
        return new GSInstallerNoOpProvider(type, clusterDefinition);
    }
  }
}
