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

import java.util.HashSet;
import java.util.Set;

/**
 * An abstract resource provider for a gsInstaller defined cluster.
 */
public abstract class GSInstallerResourceProvider implements ResourceProvider {

  private final ClusterDefinition clusterDefinition;

  private final Set<Resource> resources = new HashSet<Resource>();


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public GSInstallerResourceProvider(ClusterDefinition clusterDefinition) {
    this.clusterDefinition = clusterDefinition;
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
        resultSet.add(new ResourceImpl(resource));
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
  public RequestStatus deleteResources(Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    throw new UnsupportedOperationException("Management operations are not supported");
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    propertyIds = new HashSet<String>(propertyIds);
    propertyIds.removeAll(getPropertyIdsForSchema());
    return propertyIds;
  }


  // ----- accessors ---------------------------------------------------------

  /**
   * Get the configuration provider.
   *
   * @return the configuration provider
   */
  protected ClusterDefinition getClusterDefinition() {
    return clusterDefinition;
  }


  // ----- helper methods ----------------------------------------------------

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
    switch (type) {
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
