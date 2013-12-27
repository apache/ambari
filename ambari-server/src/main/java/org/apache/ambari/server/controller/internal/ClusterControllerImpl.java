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

import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

/**
 * Default cluster controller implementation.
 */
public class ClusterControllerImpl implements ClusterController {
  private final static Logger LOG =
      LoggerFactory.getLogger(ClusterControllerImpl.class);

  /**
   * Module of providers for this controller.
   */
  private final ProviderModule providerModule;

  /**
   * Map of resource providers keyed by resource type.
   */
  private final Map<Resource.Type, ResourceProvider> resourceProviders =
      new HashMap<Resource.Type, ResourceProvider>();

  /**
   * Map of property provider lists keyed by resource type.
   */
  private final Map<Resource.Type, List<PropertyProvider>> propertyProviders =
      new HashMap<Resource.Type, List<PropertyProvider>>();

  /**
   * Map of schemas keyed by resource type.
   */
  private final Map<Resource.Type, Schema> schemas =
      new HashMap<Resource.Type, Schema>();

  /**
   * Resource comparator.
   */
  private final ResourceComparator comparator = new ResourceComparator();

  /**
   * Predicate evaluator
   */
  private static final DefaultResourcePredicateEvaluator
    DEFAULT_RESOURCE_PREDICATE_EVALUATOR =
    new DefaultResourcePredicateEvaluator();

  // ----- Constructors ------------------------------------------------------

  public ClusterControllerImpl(ProviderModule providerModule) {
    this.providerModule = providerModule;
  }


  // ----- ClusterController -------------------------------------------------

  @Override
  public Set<Resource> getResources(Resource.Type type, Request request, Predicate predicate)
      throws UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException, SystemException {
    Set<Resource> resources;

    ResourceProvider provider = ensureResourceProvider(type);
    ensurePropertyProviders(type);

    if (provider == null) {
      resources = Collections.emptySet();
    } else {

      if (LOG.isDebugEnabled()) {
        LOG.debug("Using resource provider "
            + provider.getClass().getName()
            + " for request type " + type.toString());
      }
      // make sure that the providers can satisfy the request
      checkProperties(type, request, predicate);

      // get the resources
      resources = provider.getResources(request, predicate);
    }
    return resources;
  }

  @Override
  public Set<Resource> populateResources(Resource.Type type,
                                         Set<Resource> resources,
                                         Request request,
                                         Predicate predicate) throws SystemException {
    Set<Resource> keepers = resources;

    for (PropertyProvider propertyProvider : propertyProviders.get(type)) {
      if (providesRequestProperties(propertyProvider, request, predicate)) {
        keepers = propertyProvider.populateResources(keepers, request, predicate);
      }
    }
    return keepers;
  }

  @Override
  public Iterable<Resource> getIterable(Resource.Type type, Set<Resource> providerResources,
                                        Request request, Predicate predicate)
      throws NoSuchParentResourceException, UnsupportedPropertyException, NoSuchResourceException, SystemException {
    return getPage(type, providerResources, request, predicate, null).getIterable();
  }

  @Override
  public PageResponse getPage(Resource.Type type, Set<Resource> providerResources,
                              Request request, Predicate predicate, PageRequest pageRequest)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    Set<Resource> resources;
    ResourceProvider provider = ensureResourceProvider(type);

    ResourcePredicateEvaluator evaluator = provider instanceof ResourcePredicateEvaluator ?
        (ResourcePredicateEvaluator) provider : DEFAULT_RESOURCE_PREDICATE_EVALUATOR;

    if (!providerResources.isEmpty()) {
      Comparator<Resource> resourceComparator = pageRequest == null || pageRequest.getComparator() == null ?
          comparator : pageRequest.getComparator();

      TreeSet<Resource> sortedResources = new TreeSet<Resource>(resourceComparator);
      sortedResources.addAll(providerResources);

      if (pageRequest != null) {
        switch (pageRequest.getStartingPoint()) {
          case Beginning:
            return getPageFromOffset(pageRequest.getPageSize(), 0,
              sortedResources, predicate, evaluator);
          case End:
            return getPageToOffset(pageRequest.getPageSize(), -1,
                sortedResources, predicate, evaluator);
          case OffsetStart:
            return getPageFromOffset(pageRequest.getPageSize(),
              pageRequest.getOffset(), sortedResources, predicate, evaluator);
          case OffsetEnd:
            return getPageToOffset(pageRequest.getPageSize(),
              pageRequest.getOffset(), sortedResources, predicate, evaluator);
          // TODO : need to support the following cases for pagination
//          case PredicateStart:
//          case PredicateEnd:
        }
      }
      resources = sortedResources;
    } else {
      resources = providerResources;
    }

    return new PageResponseImpl(
        new ResourceIterable(resources, predicate, evaluator),
        0, null, null);
  }

  @Override
  public Schema getSchema(Resource.Type type) {
    Schema schema;

    synchronized (schemas) {
      schema = schemas.get(type);
      if (schema == null) {
        schema = new SchemaImpl(ensureResourceProvider(type));
        schemas.put(type, schema);
      }
    }
    return schema;
  }

  @Override
  public RequestStatus createResources(Resource.Type type, Request request)
      throws UnsupportedPropertyException,
             SystemException,
             ResourceAlreadyExistsException,
             NoSuchParentResourceException {

    ResourceProvider provider = ensureResourceProvider(type);
    if (provider != null) {

      checkProperties(type, request, null);

      return provider.createResources(request);
    }
    return null;
  }

  @Override
  public RequestStatus updateResources(Resource.Type type, Request request, Predicate predicate)
      throws UnsupportedPropertyException,
             SystemException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    ResourceProvider provider = ensureResourceProvider(type);
    if (provider != null) {

      if (!checkProperties(type, request, predicate)) {
        predicate = resolvePredicate(type, predicate);
        if (predicate == null) {
          return null;
        }
      }
        return provider.updateResources(request, predicate);
    }
    return null;
  }

  @Override
  public RequestStatus deleteResources(Resource.Type type, Predicate predicate)
      throws UnsupportedPropertyException,
             SystemException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    ResourceProvider provider = ensureResourceProvider(type);
    if (provider != null) {
      if (!checkProperties(type, null, predicate)) {
        predicate = resolvePredicate(type, predicate);
        if (predicate == null) {
          return null;
        }
      }
        return provider.deleteResources(predicate);
    }
    return null;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get an iterable set of resources filtered by the given request and
   * predicate objects.
   *
   * @param type       type of resources
   * @param request    the request
   * @param predicate  the predicate object which filters which resources are returned
   *
   * @return a page response representing the requested page of resources
   *
   * @throws UnsupportedPropertyException thrown if the request or predicate contain
   *                                      unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws NoSuchResourceException no matching resource(s) found
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  protected Iterable<Resource> getResourceIterable(Resource.Type type, Request request,
                                                   Predicate predicate)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchParentResourceException,
      NoSuchResourceException {
    return getResources(type, request, predicate, null).getIterable();
  }

  /**
   * Get a page of resources filtered by the given request, predicate objects and
   * page request.
   *
   * @param type         type of resources
   * @param request      the request
   * @param predicate    the predicate object which filters which resources are returned
   * @param pageRequest  the page request for a paginated response
   *
   * @return a page response representing the requested page of resources
   *
   * @throws UnsupportedPropertyException thrown if the request or predicate contain
   *                                      unsupported property ids
   * @throws SystemException an internal exception occurred
   * @throws NoSuchResourceException no matching resource(s) found
   * @throws NoSuchParentResourceException a specified parent resource doesn't exist
   */
  protected  PageResponse getResources(Resource.Type type, Request request, Predicate predicate,
                                       PageRequest pageRequest)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    Set<Resource> providerResources = getResources(type, request, predicate);
    populateResources(type, providerResources, request, predicate);
    return getPage(type, providerResources, request, predicate, pageRequest);
  }

  /**
   * Check to make sure that all the property ids specified in the given request and
   * predicate are supported by the resource provider or property providers for the
   * given type.
   *
   * @param type       the resource type
   * @param request    the request
   * @param predicate  the predicate
   *
   * @return true if all of the properties specified in the request and predicate are supported by
   *         the resource provider for the given type; false if any of the properties specified in
   *         the request and predicate are not supported by the resource provider but are supported
   *         by a property provider for the given type.
   *
   * @throws UnsupportedPropertyException thrown if any of the properties specified in the request
   *                                      and predicate are not supported by either the resource
   *                                      provider or a property provider for the given type
   */
  private boolean checkProperties(Resource.Type type, Request request, Predicate predicate)
      throws UnsupportedPropertyException {
    Set<String> requestPropertyIds = request == null ? new HashSet<String>() :
        PropertyHelper.getAssociatedPropertyIds(request);

    if (predicate != null) {
      requestPropertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }

    if (requestPropertyIds.size() > 0) {
      ResourceProvider provider = ensureResourceProvider(type);
      requestPropertyIds = provider.checkPropertyIds(requestPropertyIds);

      if (requestPropertyIds.size() > 0) {
        List<PropertyProvider> propertyProviders = ensurePropertyProviders(type);
        for (PropertyProvider propertyProvider : propertyProviders) {
          requestPropertyIds = propertyProvider.checkPropertyIds(requestPropertyIds);
          if (requestPropertyIds.size() == 0) {
            return false;
          }
        }
        throw new UnsupportedPropertyException(type, requestPropertyIds);
      }
    }
    return true;
  }

  /**
   * Check to see if any of the property ids specified in the given request and
   * predicate are handled by an associated property provider.  if so, then use
   * the given predicate to obtain a new predicate that can be completely
   * processed by an update or delete operation on a resource provider for
   * the given resource type.  This means that the new predicate should only
   * reference the key property ids for this type.
   *
   * @param type       the resource type
   * @param predicate  the predicate
   *
   * @return the given predicate if a new one is not required; a new predicate if required
   *
   * @throws UnsupportedPropertyException thrown if any of the properties specified in the request
   *                                      and predicate are not supported by either the resource
   *                                      provider or a property provider for the given type
   *
   * @throws SystemException thrown for internal exceptions
   * @throws NoSuchResourceException if the resource that is requested doesn't exist
   * @throws NoSuchParentResourceException if a parent resource of the requested resource doesn't exist
   */
  private Predicate resolvePredicate(Resource.Type type, Predicate predicate)
    throws UnsupportedPropertyException,
        SystemException,
        NoSuchResourceException,
        NoSuchParentResourceException{

    ResourceProvider provider = ensureResourceProvider(type);

    Set<String>  keyPropertyIds = new HashSet<String>(provider.getKeyPropertyIds().values());
    Request      readRequest    = PropertyHelper.getReadRequest(keyPropertyIds);

    Iterable<Resource> resources = getResourceIterable(type, readRequest, predicate);

    PredicateBuilder pb = new PredicateBuilder();
    PredicateBuilder.PredicateBuilderWithPredicate pbWithPredicate = null;

    for (Resource resource : resources) {
      if (pbWithPredicate != null) {
        pb = pbWithPredicate.or();
      }

      pb              = pb.begin();
      pbWithPredicate = null;

      for (String keyPropertyId : keyPropertyIds) {
        if (pbWithPredicate != null) {
          pb = pbWithPredicate.and();
        }
        pbWithPredicate =
            pb.property(keyPropertyId).equals((Comparable) resource.getPropertyValue(keyPropertyId));
      }
      if (pbWithPredicate != null) {
        pbWithPredicate = pbWithPredicate.end();
      }
    }
    return pbWithPredicate == null ? null : pbWithPredicate.toPredicate();
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
    Set<String> requestPropertyIds = new HashSet<String>(request.getPropertyIds());

    if (requestPropertyIds.size() == 0) {
      return true;
    }
    requestPropertyIds.addAll(PredicateHelper.getPropertyIds(predicate));

    int size = requestPropertyIds.size();

    return size > provider.checkPropertyIds(requestPropertyIds).size();
  }

  /**
   * Get the resource provider for the given type, creating it if required.
   *
   * @param type  the resource type
   *
   * @return the resource provider
   */
  private ResourceProvider ensureResourceProvider(Resource.Type type) {
    synchronized (resourceProviders) {
      if (!resourceProviders.containsKey(type)) {
        resourceProviders.put(type, providerModule.getResourceProvider(type));
      }
    }
    return resourceProviders.get(type);
  }

  /**
   * Get the list of property providers for the given type.
   *
   * @param type  the resource type
   *
   * @return the list of property providers
   */
  private List<PropertyProvider> ensurePropertyProviders(Resource.Type type) {
    synchronized (propertyProviders) {
      if (!propertyProviders.containsKey(type)) {
        propertyProviders.put(type, providerModule.getPropertyProviders(type));
      }
    }
    return propertyProviders.get(type);
  }

  /**
   * Get one page of resources from the given set of resources starting at the given offset.
   *
   * @param pageSize   the page size
   * @param offset     the offset
   * @param resources  the set of resources
   * @param predicate  the predicate
   *
   * @return a page response containing a page of resources
   */
  private PageResponse getPageFromOffset(int pageSize, int offset,
                                         NavigableSet<Resource> resources,
                                         Predicate predicate,
                                         ResourcePredicateEvaluator evaluator) {

    int                currentOffset = 0;
    Resource           previous      = null;
    Set<Resource>      pageResources = new LinkedHashSet<Resource>();
    Iterator<Resource> iterator      = resources.iterator();

    // skip till offset
    while (currentOffset < offset && iterator.hasNext()) {
      previous = iterator.next();
      ++currentOffset;
    }

    // get a page worth of resources
    for (int i = 0; i < pageSize && iterator.hasNext(); ++i) {
      pageResources.add(iterator.next());
    }

    return new PageResponseImpl(new ResourceIterable(pageResources,
        predicate, evaluator),
        currentOffset,
        previous,
        iterator.hasNext() ? iterator.next() : null);
  }

  /**
   * Get one page of resources from the given set of resources ending at the given offset.
   *
   * @param pageSize   the page size
   * @param offset     the offset; -1 indicates the end of the resource set
   * @param resources  the set of resources
   * @param predicate  the predicate
   *
   * @return a page response containing a page of resources
   */
  private PageResponse getPageToOffset(int pageSize, int offset,
                                       NavigableSet<Resource> resources,
                                       Predicate predicate,
                                       ResourcePredicateEvaluator evaluator) {

    int                currentOffset = resources.size() - 1;
    Resource           next          = null;
    List<Resource>     pageResources = new LinkedList<Resource>();
    Iterator<Resource> iterator      = resources.descendingIterator();

    if (offset != -1) {
      // skip till offset
      while (currentOffset > offset && iterator.hasNext()) {
        next = iterator.next();
        --currentOffset;
      }
    }

    // get a page worth of resources
    for (int i = 0; i < pageSize && iterator.hasNext(); ++i) {
      pageResources.add(0, iterator.next());
      --currentOffset;
    }

    return new PageResponseImpl(new ResourceIterable(new
        LinkedHashSet<Resource>(pageResources), predicate, evaluator),
        currentOffset + 1,
        iterator.hasNext() ? iterator.next() : null,
        next);
  }

  /**
   * Get the associated resource comparator.
   *
   * @return the resource comparator
   */
  protected Comparator<Resource> getComparator() {
    return comparator;
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

    /**
     * The predicate evaluator.
     */
    private final ResourcePredicateEvaluator evaluator;


    // ----- Constructors ----------------------------------------------------

    /**
     * Create a ResourceIterable.
     *
     * @param resources  the set of resources to iterate over
     * @param predicate  the predicate used to filter the set of resources
     * @param evaluator  the evaluator used to evaluate with the given predicate
     */
    private ResourceIterable(Set<Resource> resources, Predicate predicate,
                             ResourcePredicateEvaluator evaluator) {
      this.resources = resources;
      this.predicate = predicate;
      this.evaluator = evaluator;
    }

    // ----- Iterable --------------------------------------------------------

    @Override
    public Iterator<Resource> iterator() {
      return new ResourceIterator(resources, predicate, evaluator);
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

    /**
     * The predicate evaluator.
     */
    private final ResourcePredicateEvaluator evaluator;

    // ----- Constructors ----------------------------------------------------

    /**
     * Create a new ResourceIterator.
     *
     * @param resources  the set of resources to iterate over
     * @param predicate  the predicate used to filter the set of resources
     * @param evaluator  the evaluator used to evaluate with the given predicate
     */
    private ResourceIterator(Set<Resource> resources, Predicate predicate,
                             ResourcePredicateEvaluator evaluator) {
      this.iterator     = resources.iterator();
      this.predicate    = predicate;
      this.evaluator    = evaluator;
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
        if (predicate == null || evaluator.evaluate(predicate, next)) {
          return next;
        }
      }
      return null;
    }
  }

  // ----- ResourceComparator inner class ------------------------------------

  protected class ResourceComparator implements Comparator<Resource> {

    @Override
    public int compare(Resource resource1, Resource resource2) {
      Resource.Type resourceType = resource1.getType();

      // compare based on resource type
      int compVal = resourceType.compareTo(resource2.getType());

      if (compVal == 0) {
        Schema schema = getSchema(resourceType);

        // compare based on resource key properties
        for (Resource.Type type : Resource.Type.values()) {
          String keyPropertyId = schema.getKeyPropertyId(type);
          if (keyPropertyId != null) {
            compVal = compareValues(resource1.getPropertyValue(keyPropertyId),
                                    resource2.getPropertyValue(keyPropertyId));
            if (compVal != 0 ) {
              return compVal;
            }
          }
        }
      }

      // compare based on the resource strings
      return resource1.toString().compareTo(resource2.toString());
    }

    // compare two values and account for null
    private int compareValues(Object val1, Object val2) {

      if (val1 == null || val2 == null) {
        return val1 == null && val2 == null ? 0 : val1 == null ? -1 : 1;
      }

      if (val1 instanceof Comparable) {
        try {
          return ((Comparable)val1).compareTo(val2);
        } catch (ClassCastException e) {
          return 0;
        }
      }
      return 0;
    }
  }
}
