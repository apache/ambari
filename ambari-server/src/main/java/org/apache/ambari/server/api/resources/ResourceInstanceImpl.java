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


package org.apache.ambari.server.api.resources;

import org.apache.ambari.server.api.query.Query;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Schema;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Resource instance which contains request specific state.
 */
public class ResourceInstanceImpl implements ResourceInstance, Query{

  /**
   * Map of primary and foreign keys and values necessary to identify the resource.
   */
  private Map<Resource.Type, String> m_mapResourceIds = new HashMap<Resource.Type, String>();

  /**
   * Definition for the resource type.  The definition contains all information specific to the
   * resource type.
   */
  private ResourceDefinition m_resourceDefinition;

  /**
   * Sub-resource instances of this resource.
   * Map of resource resource name to resource instance.
   */
  private Map<String, ResourceInstanceImpl> m_mapSubResources;

  /**
   * Sub-resources of the resource which is being operated on.
   */
  private Map<String, ResourceInstanceImpl> m_mapQuerySubResources = new HashMap<String, ResourceInstanceImpl>();

  /**
   * Properties of the query which make up the select portion of the query.
   */
  private Set<String> m_setQueryProperties = new HashSet<String>();

  /**
   * Indicates that the query should include all available properties.
   */
  private boolean allProperties = false;

  /**
   * Map that associates each property set on the query to temporal data.
   */
  private Map<String, TemporalInfo> m_mapPropertyTemporalInfo = new HashMap<String, TemporalInfo>();

  /**
   * Map that associates categories with temporal data.
   */
  private Map<String, TemporalInfo> m_mapCategoryTemporalInfo = new HashMap<String, TemporalInfo>();

  /**
   * The user supplied predicate.
   */
  private Predicate m_userPredicate;

  /**
   * The associated cluster controller.
   */
  private final ClusterController m_controller;

  /**
   * The logger.
   */
  private final static Logger LOG =
      LoggerFactory.getLogger(ResourceInstanceImpl.class);


  // ----- Constructors ------------------------------------------------------

  public ResourceInstanceImpl(Map<Resource.Type, String> mapIds,
                              ResourceDefinition resourceDefinition,
                              ClusterController controller) {

    m_resourceDefinition = resourceDefinition;
    m_controller = controller;

    setIds(mapIds);
  }


  // ----- ResourceInstance --------------------------------------------------

  @Override
  public void setIds(Map<Resource.Type, String> mapIds) {
    m_mapResourceIds.putAll(mapIds);
  }

  @Override
  public Map<Resource.Type, String> getIds() {
    return new HashMap<Resource.Type, String>((m_mapResourceIds));
  }

  @Override
  public Query getQuery() {
    return this;
  }

  @Override
  public ResourceDefinition getResourceDefinition() {
    return m_resourceDefinition;
  }

  @Override
  public Map<String, ResourceInstance> getSubResources() {
    return new HashMap<String, ResourceInstance>(ensureSubResources());
  }

  @Override
  public boolean isCollectionResource() {
    return getIds().get(getResourceDefinition().getType()) == null;
  }


  // ----- Query -------------------------------------------------------------

  @Override
  public void addProperty(String category, String name, TemporalInfo temporalInfo) {
    if (category == null && name.equals("*")) {
      // wildcard
      addAllProperties(temporalInfo);
    } else{
      if (addPropertyToSubResource(category, name, temporalInfo)){
        // add pk/fk properties of the resource to this query
        Resource.Type resourceType = getResourceDefinition().getType();
        Schema schema              = getClusterController().getSchema(resourceType);

        for (Resource.Type type : getIds().keySet()) {
          addLocalProperty(schema.getKeyPropertyId(type));
        }
      } else {
        String propertyId = PropertyHelper.getPropertyId(category, name.equals("*") ? null : name);
        addLocalProperty(propertyId);
        if (temporalInfo != null) {
          m_mapCategoryTemporalInfo.put(propertyId, temporalInfo);
        }
      }
    }
  }

  @Override
  public void addLocalProperty(String property) {
    m_setQueryProperties.add(property);
  }

  @Override
  public Result execute()
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {

    QueryResult queryResult = queryForResources();

    return applyResult(queryResult, null);
  }

  @Override
  public Predicate getPredicate() {
    Predicate internalPredicate = createInternalPredicate(this);
    if (internalPredicate == null) {
      return m_userPredicate == null ? null :m_userPredicate;

    } else {

      return m_userPredicate == null ?
          internalPredicate :
          new AndPredicate(m_userPredicate, internalPredicate);
    }
  }

  @Override
  public Set<String> getProperties() {
    return Collections.unmodifiableSet(m_setQueryProperties);
  }

  @Override
  public void setUserPredicate(Predicate predicate) {
    m_userPredicate = predicate;
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ResourceInstanceImpl that = (ResourceInstanceImpl) o;

    return m_mapResourceIds.equals(that.m_mapResourceIds) &&
        m_resourceDefinition.equals(that.m_resourceDefinition) &&
        m_mapSubResources == null ? that.m_mapSubResources == null :
            m_mapSubResources.equals(that.m_mapSubResources) &&
        m_mapQuerySubResources == null ? that.m_mapQuerySubResources == null :
            m_mapQuerySubResources.equals(that.m_mapQuerySubResources) &&
        m_mapCategoryTemporalInfo.equals(that.m_mapCategoryTemporalInfo) &&
        m_mapPropertyTemporalInfo.equals(that.m_mapPropertyTemporalInfo) &&
        m_setQueryProperties.equals(that.m_setQueryProperties) &&
        m_userPredicate == null ? that.m_userPredicate == null : m_userPredicate.equals(that.m_userPredicate);
  }

  @Override
  public int hashCode() {
    int result = 13;
    result = 31 * result + m_mapResourceIds.hashCode();
    result = 31 * result + m_resourceDefinition.hashCode();
    result = 31 * result + (m_mapSubResources != null ? m_mapSubResources.hashCode() : 0);
    result = 31 * result + (m_mapQuerySubResources != null ? m_mapQuerySubResources.hashCode() : 0);
    result = 31 * result + m_setQueryProperties.hashCode();
    result = 31 * result + m_mapPropertyTemporalInfo.hashCode();
    result = 31 * result + m_mapCategoryTemporalInfo.hashCode();
    result = 31 * result + (m_userPredicate != null ? m_userPredicate.hashCode() : 0);

    return result;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get the associated cluster controller.
   */
  protected ClusterController getClusterController() {
    return m_controller;
  }

  /**
   * Query the cluster controller for the associated resources.
   */
  private QueryResult queryForResources()
      throws UnsupportedPropertyException,
             SystemException,
             NoSuchResourceException,
             NoSuchParentResourceException {
    Set<Resource> resources = new LinkedHashSet<Resource>();

    prepForQuery();

    for (Resource resource : doQuery(getPredicate())) {
      resources.add(resource);
    }

    return new QueryResult(resources, queryForSubResources(resources), this);
  }

  /**
   * Query the cluster controller for the sub-resources associated with the given resources.
   */
  private Map<Resource.Type, QueryResult> queryForSubResources(Set<Resource> resources)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    Map<Resource.Type, QueryResult> subResourceMap = new HashMap<Resource.Type, QueryResult>();
    Map<String, Predicate>          predicateMap   = getSubResourcePredicates(resources);

    for (Map.Entry<String, ResourceInstanceImpl> entry : m_mapQuerySubResources.entrySet()) {
      ResourceInstanceImpl subResource  = entry.getValue();
      Set<Resource>        subResources = new LinkedHashSet<Resource>();

      prepForQuery();

      for (Resource resource : subResource.doQuery(predicateMap.get(entry.getKey()))) {
        subResources.add(resource);
      }

      subResourceMap.put(subResource.getResourceDefinition().getType(),
          new QueryResult(subResources, subResource.queryForSubResources(subResources), subResource));
    }
    return subResourceMap;
  }

  /**
   * Populate the properties and sub-resource sets of this resource prior
   * to running a query.
   */
  private void prepForQuery() {

    Resource.Type resourceType = getResourceDefinition().getType();

    if (isCollectionResource()) {
      addCollectionProperties(resourceType);
    }
    Map<String, ResourceInstanceImpl> mapSubResources = m_mapQuerySubResources;

    if (m_setQueryProperties.isEmpty() && mapSubResources.isEmpty()) {
      //Add sub resource properties for default case where no fields are specified.
      mapSubResources.putAll(ensureSubResources());
    }
  }

  /**
   * Query the cluster controller for the resources.
   */
  private Iterable<Resource> doQuery(Predicate predicate)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    Resource.Type resourceType = getResourceDefinition().getType();
    Request       request      = createRequest();

    if (LOG.isDebugEnabled()) {
      LOG.debug("Executing resource query: " + request + " where " + predicate);
    }
    return getClusterController().getResources(resourceType, request, predicate);
  }

  /**
   * Apply the query result to this resource.
   */
  private Result applyResult(QueryResult queryResult, Predicate parentPredicate) {
    Result result = new ResultImpl(true);

    Map<String, ResourceInstanceImpl> mapSubResources  = queryResult.getResourceInstance().m_mapQuerySubResources;
    ResourceInstanceImpl              resourceInstance = queryResult.getResourceInstance();

    if (isCollectionResource()) {
      result.getResultTree().setProperty("isCollection", "true");
    }

    TreeNode<Resource> tree = result.getResultTree();

    int count = 1;
    for (Resource resource : queryResult.getResources()) {
      if (parentPredicate == null || parentPredicate.evaluate(resource)) {
        // Add a child node for the resource and provide a unique name
        TreeNode<Resource> node = tree.addChild(resource, resource.getType() + ":" + count++);
        for (Map.Entry<String, ResourceInstanceImpl> entry : mapSubResources.entrySet()) {
          String subResCategory = entry.getKey();
          ResourceInstanceImpl subResourceInstance = entry.getValue();

          Resource.Type subResourceType = subResourceInstance.getResourceDefinition().getType();

          QueryResult subQueryResult = queryResult.getSubResources().get(subResourceType);

          if (subQueryResult != null) {

            resourceInstance.setParentIdsOnSubResource(resource, subResourceInstance);

            Predicate subPredicate = subResourceInstance.getQuery().getPredicate();

            TreeNode<Resource> childResult = subResourceInstance.applyResult(subQueryResult, subPredicate).getResultTree();
            childResult.setName(subResCategory);
            childResult.setProperty("isCollection", "false");
            node.addChild(childResult);
          }

        }
      }
    }
    return result;
  }

  /**
   * Get the map of sub-resources.  Lazily construct it if necessary.
   */
  private Map<String, ResourceInstanceImpl> ensureSubResources() {
    if (m_mapSubResources == null) {
      m_mapSubResources = new HashMap<String, ResourceInstanceImpl>();
      Set<SubResourceDefinition> setSubResourceDefs = getResourceDefinition().getSubResourceDefinitions();

      for (SubResourceDefinition subResDef : setSubResourceDefs) {

        Map<Resource.Type, String> mapIds = getIds();

        ResourceInstanceImpl resource = new ResourceInstanceImpl(mapIds,
            ResourceInstanceFactoryImpl.createResourceDefinition(subResDef.getType(), mapIds), m_controller);

        // ensure pk is returned
        resource.getQuery().addLocalProperty(getClusterController().getSchema(
            subResDef.getType()).getKeyPropertyId(subResDef.getType()));
        // add additionally required fk properties
        for (Resource.Type fkType : subResDef.getAdditionalForeignKeys()) {
          resource.getQuery().addLocalProperty(getClusterController().getSchema(subResDef.getType()).getKeyPropertyId(fkType));
        }

        String subResourceName = subResDef.isCollection() ? resource.getResourceDefinition().getPluralName() :
            resource.getResourceDefinition().getSingularName();

        m_mapSubResources.put(subResourceName, resource);
      }
    }
    return m_mapSubResources;
  }

  private void addCollectionProperties(Resource.Type resourceType) {
    Schema schema = getClusterController().getSchema(resourceType);
    // add pk
    String property = schema.getKeyPropertyId(resourceType);
    addProperty(PropertyHelper.getPropertyCategory(property), PropertyHelper.getPropertyName(property), null);

    for (Resource.Type type : getIds().keySet()) {
      // add fk's
      String keyPropertyId = schema.getKeyPropertyId(type);
      if (keyPropertyId != null) {
        addProperty(PropertyHelper.getPropertyCategory(keyPropertyId), 
            PropertyHelper.getPropertyName(keyPropertyId), null);
      }
    }
  }

  private void addAllProperties(TemporalInfo temporalInfo) {
    allProperties = true;
    if (temporalInfo != null) {
      m_mapCategoryTemporalInfo.put(null, temporalInfo);
    }

    for (Map.Entry<String, ResourceInstanceImpl> entry : ensureSubResources().entrySet()) {
      String name = entry.getKey();
      if (! m_mapQuerySubResources.containsKey(name)) {
        m_mapQuerySubResources.put(name, entry.getValue());
      }
    }
  }

  private boolean addPropertyToSubResource(String path, String property, TemporalInfo temporalInfo) {
    // cases:
    // - path is null, property is path (all sub-resource props will have a path)
    // - path is single token and prop in non null
    //      (path only will presented as above case with property only)
    // - path is multi level and prop is non null

    boolean resourceAdded = false;
    if (path == null) {
      path = property;
      property = null;
    }

    int i = path.indexOf("/");
    String p = i == -1 ? path : path.substring(0, i);

    ResourceInstanceImpl subResource = ensureSubResources().get(p);
    if (subResource != null) {
      m_mapQuerySubResources.put(p, subResource);
      //todo: handle case of trailing '/' (for example fields=subResource/)

      if (property != null || !path.equals(p)) {
        //only add if a sub property is set or if a sub category is specified
        subResource.getQuery().addProperty(i == -1 ? null : path.substring(i + 1), property, temporalInfo);
      }
      resourceAdded = true;
    }
    return resourceAdded;
  }

  private Predicate createInternalPredicate(ResourceInstance resource) {
    Resource.Type resourceType = resource.getResourceDefinition().getType();
    Map<Resource.Type, String> mapResourceIds = resource.getIds();
    Schema schema = getClusterController().getSchema(resourceType);

    Set<Predicate> setPredicates = new HashSet<Predicate>();
    for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
      if (entry.getValue() != null) {
        String keyPropertyId = schema.getKeyPropertyId(entry.getKey());
        if (keyPropertyId != null) {
          setPredicates.add(new EqualsPredicate<String>(keyPropertyId, entry.getValue()));
        }
      }
    }

    if (setPredicates.size() == 1) {
      return setPredicates.iterator().next();
    } else if (setPredicates.size() > 1) {
      return new AndPredicate(setPredicates.toArray(new Predicate[setPredicates.size()]));
    } else {
      return null;
    }
  }

  /**
   * Get a map of predicates for the given resource's sub-resources keyed by resource type.
   */
  private Map<String, Predicate> getSubResourcePredicates(Set<Resource> resources) {
    Map<String, Predicate> predicateMap = new HashMap<String, Predicate>();

    for (Resource resource : resources) {
      for (Map.Entry<String, ResourceInstanceImpl> entry : m_mapQuerySubResources.entrySet()) {
        ResourceInstanceImpl subResourceInstance = entry.getValue();

        String    subResCategory = entry.getKey();
        Predicate predicate      = predicateMap.get(subResCategory);

        setParentIdsOnSubResource(resource, subResourceInstance);

        predicateMap.put(subResCategory, predicate == null ?
            subResourceInstance.getQuery().getPredicate() :
            new OrPredicate(predicate, subResourceInstance.getQuery().getPredicate()));
      }
    }
    return predicateMap;
  }

  /**
   * Set the parent id values from the given resource on the given sub resource instance.
   */
  private void setParentIdsOnSubResource(Resource resource,
                                         ResourceInstanceImpl subResourceInstance) {

    Map<Resource.Type, String> mapParentIds   = getIds();
    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>(mapParentIds.size());
    ClusterController          controller     = getClusterController();

    for (Map.Entry<Resource.Type, String> resourceIdEntry : mapParentIds.entrySet()) {
      Resource.Type type  = resourceIdEntry.getKey();
      String        value = resourceIdEntry.getValue();

      if (value == null) {
        Object o = resource.getPropertyValue(controller.getSchema(type).getKeyPropertyId(type));
        value = o == null ? null : o.toString();
      }
      if (value != null) {
        mapResourceIds.put(type, value);
      }
    }
    String resourceKeyProp = controller.getSchema(resource.getType()).
        getKeyPropertyId(resource.getType());
    //todo: shouldn't use toString here
    mapResourceIds.put(resource.getType(), resource.getPropertyValue(resourceKeyProp).toString());
    subResourceInstance.setIds(mapResourceIds);
  }

  /**
   * Create a request object for this resource. 
   */
  private Request createRequest() {
    Set<String> setProperties = new HashSet<String>();

    Map<String, TemporalInfo> mapTemporalInfo    = new HashMap<String, TemporalInfo>();
    TemporalInfo              globalTemporalInfo = m_mapCategoryTemporalInfo.get(null);

    for (String group : m_setQueryProperties) {
      TemporalInfo temporalInfo = m_mapCategoryTemporalInfo.get(group);
      if (temporalInfo != null) {
        mapTemporalInfo.put(group, temporalInfo);
      } else if (globalTemporalInfo != null) {
        mapTemporalInfo.put(group, globalTemporalInfo);
      }
      setProperties.add(group);
    }

    return PropertyHelper.getReadRequest(allProperties ? Collections.<String>emptySet() : setProperties, mapTemporalInfo);
  }


  // ----- inner classes -----------------------------------------------------

  /**
   * Holder for the results of a query for resources.
   */
  private static class QueryResult {

    /**
     * The associated ResourceInstanceImpl.
     */
    private  final ResourceInstanceImpl resourceInstance;

    /**
     * The set of resources returned from the query for the associated ResourceInstanceImpl.
     */
    private final Set<Resource> resources;

    /**
     * The set of QueryResults for the sub resources.
     */
    private final Map<Resource.Type, QueryResult> subResourceMap;

    private QueryResult(Set<Resource> resources,
                        Map<Resource.Type, QueryResult> subResourceMap,
                        ResourceInstanceImpl resourceInstance) {
      this.resources        = resources;
      this.subResourceMap   = subResourceMap;
      this.resourceInstance = resourceInstance;
    }

    /**
     * Get the associated ResourceInstanceImpl.
     *
     * @return the assocaited ResourceInstanceImpl
     */
    public ResourceInstanceImpl getResourceInstance() {
      return resourceInstance;
    }

    /**
     * Get the resources returned from the query.
     *
     * @return the resources returned from the query
     */
    public Set<Resource> getResources() {
      return resources;
    }

    /**
     * Get the set of QueryResults for the sub resources.
     *
     * @return theset of QueryResults for the sub resources
     */
    public Map<Resource.Type, QueryResult> getSubResources() {
      return subResourceMap;
    }
  }
}
