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

import org.apache.ambari.server.controller.spi.PropertyId;
import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.utilities.PredicateHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Default cluster controller implementation.
 */
public class ClusterControllerImpl implements ClusterController {

  /**
   * Module of providers for this controller.
   */
  private final ProviderModule providerModule;

  /**
   * Map of resource providers keyed by resource type.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders;

  /**
   * Map of property provider lists keyed by resource type.
   */
  private final Map<Resource.Type, List<PropertyProvider>> propertyProviders;

  /**
   * Map of schemas keyed by resource type.
   */
  private final Map<Resource.Type, Schema> schemas;


  // ----- Constructors ------------------------------------------------------

  public ClusterControllerImpl(ProviderModule providerModule) {
    this.providerModule = providerModule;
    resourceProviders   = new HashMap<Resource.Type, ResourceProvider>();
    propertyProviders   = new HashMap<Resource.Type, List<PropertyProvider>>();
    schemas             = new HashMap<Resource.Type, Schema>();

    setProviders();
  }


  // ----- ClusterController -------------------------------------------------

  @Override
  public Iterable<Resource> getResources(Resource.Type type, Request request, Predicate predicate)
      throws AmbariException{
    ResourceProvider provider = resourceProviders.get(type);
    Set<Resource> resources;

    if (provider == null) {
      resources = Collections.emptySet();
    } else {
      resources = provider.getResources(request, predicate);
      resources = populateResources(type, resources, request, predicate);
    }
    
    return new ResourceIterable(resources, predicate);
  }

  @Override
  public Schema getSchema(Resource.Type type) {
    Schema schema;

    synchronized (schemas) {
      schema = schemas.get(type);
      if (schema == null) {
        schema = new SchemaImpl(resourceProviders.get(type), propertyProviders.get(type));
        schemas.put(type, schema);
      }
    }
    return schema;
  }

  @Override
  public void createResources(Resource.Type type, Request request) throws AmbariException {
    ResourceProvider provider = resourceProviders.get(type);
    if (provider != null) {
      provider.createResources(request);
    }
  }

  @Override
  public void updateResources(Resource.Type type, Request request, Predicate predicate) throws AmbariException {
    ResourceProvider provider = resourceProviders.get(type);
    if (provider != null) {
      provider.updateResources(request, predicate);
    }
  }

  @Override
  public void deleteResources(Resource.Type type, Predicate predicate) throws AmbariException {
    ResourceProvider provider = resourceProviders.get(type);
    if (provider != null) {
      provider.deleteResources(predicate);
    }
  }


  // ----- helper methods ----------------------------------------------------

  private Set<Resource> populateResources(Resource.Type type,
                                          Set<Resource> resources,
                                          Request request,
                                          Predicate predicate) throws AmbariException{
    Set<Resource> keepers = resources;

    for (PropertyProvider propertyProvider : propertyProviders.get(type)) {
      if (providesRequestProperties(propertyProvider, request, predicate)) {
        keepers = propertyProvider.populateResources(keepers, request, predicate);
      }
    }
    return keepers;
  }

  /**
   * Indicates whether or not the given property provider can service the given request.
   *
   * @param provider   the property provider
   * @param request    the request
   * @param predicate  the predicate
   *
   * @return true if the given provider can service the request
   */
  private boolean providesRequestProperties(PropertyProvider provider, Request request, Predicate predicate) {
    Set<PropertyId> requestPropertyIds = new HashSet<PropertyId>(request.getPropertyIds());

    if (requestPropertyIds.size() == 0) {
      return true;
    }
    requestPropertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    requestPropertyIds.retainAll(provider.getPropertyIds());

    return !requestPropertyIds.isEmpty();
  }

  private void setProviders() {
    resourceProviders.put(Resource.Type.Cluster, providerModule.getResourceProvider(Resource.Type.Cluster));
    resourceProviders.put(Resource.Type.Service, providerModule.getResourceProvider(Resource.Type.Service));
    resourceProviders.put(Resource.Type.Host, providerModule.getResourceProvider(Resource.Type.Host));
    resourceProviders.put(Resource.Type.Component, providerModule.getResourceProvider(Resource.Type.Component));
    resourceProviders.put(Resource.Type.HostComponent, providerModule.getResourceProvider(Resource.Type.HostComponent));
    resourceProviders.put(Resource.Type.Configuration, providerModule.getResourceProvider(Resource.Type.Configuration));

    propertyProviders.put(Resource.Type.Cluster, providerModule.getPropertyProviders(Resource.Type.Cluster));
    propertyProviders.put(Resource.Type.Service, providerModule.getPropertyProviders(Resource.Type.Service));
    propertyProviders.put(Resource.Type.Host, providerModule.getPropertyProviders(Resource.Type.Host));
    propertyProviders.put(Resource.Type.Component, providerModule.getPropertyProviders(Resource.Type.Component));
    propertyProviders.put(Resource.Type.HostComponent, providerModule.getPropertyProviders(Resource.Type.HostComponent));
    propertyProviders.put(Resource.Type.Configuration, providerModule.getPropertyProviders(Resource.Type.Configuration));
  }


  // ----- ResourceIterable inner class --------------------------------------

  private static class ResourceIterable implements Iterable<Resource> {

    /**
     * The resources to iterate over.
     */
    private final Set<Resource> resources;

    /**
     * The predicate used to filter the set.
     */
    private final Predicate predicate;

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a ResourceIterable.
     *
     * @param resources  the set of resources to iterate over
     * @param predicate  the predicate used to filter the set of resources
     */
    private ResourceIterable(Set<Resource> resources, Predicate predicate) {
      this.resources = resources;
      this.predicate = predicate;
    }

    // ----- Iterable --------------------------------------------------------

    @Override
    public Iterator<Resource> iterator() {
      return new ResourceIterator(resources, predicate);
    }
  }


  // ----- ResourceIterator inner class --------------------------------------

  private static class ResourceIterator implements Iterator<Resource> {

    /**
     * The underlying iterator.
     */
    private final Iterator<Resource> iterator;

    /**
     * The predicate used to filter the resource being iterated over.
     */
    private final Predicate predicate;

    /**
     * The next resource.
     */
    private Resource nextResource;


    // ----- Constructors ----------------------------------------------------

    /**
     * Create a new ResourceIterator.
     *
     * @param resources  the set of resources to iterate over
     * @param predicate  the predicate used to filter the set of resources
     */
    private ResourceIterator(Set<Resource> resources, Predicate predicate) {
      this.iterator     = resources.iterator();
      this.predicate    = predicate;
      this.nextResource = getNextResource();
    }

    // ----- Iterator --------------------------------------------------------

    @Override
    public boolean hasNext() {
      return nextResource != null;
    }

    @Override
    public Resource next() {
      if (nextResource == null) {
        throw new NoSuchElementException("Iterator has no more elements.");
      }

      Resource currentResource = nextResource;
      this.nextResource = getNextResource();

      return currentResource;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported.");
    }

    // ----- helper methods --------------------------------------------------

    /**
     * Get the next resource.
     *
     * @return the next resource.
     */
    private Resource getNextResource() {
      while (iterator.hasNext()) {
        Resource next = iterator.next();
        
        if (predicate == null || predicate.evaluate(next)) {
          return next;
        }
      }
      return null;
    }
  }
}
