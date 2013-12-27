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

package org.apache.ambari.server.api.query;

import org.apache.ambari.server.api.resources.ResourceDefinition;
import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.resources.ResourceInstanceFactoryImpl;
import org.apache.ambari.server.api.resources.SubResourceDefinition;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.api.util.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Default read query.
 */
public class QueryImpl implements Query, ResourceInstance {

  /**
   * Definition for the resource type.  The definition contains all information specific to the
   * resource type.
   */
  private final ResourceDefinition resourceDefinition;

  /**
   * The cluster controller.
   */
  private final ClusterController clusterController;

  /**
   * Properties of the query which make up the select portion of the query.
   */
  private final Set<String> queryPropertySet = new HashSet<String>();

  /**
   * Map that associates categories with temporal data.
   */
  private final Map<String, TemporalInfo> temporalInfoMap = new HashMap<String, TemporalInfo>();

  /**
   * Map of primary and foreign key values.
   */
  private final Map<Resource.Type, String> keyValueMap = new HashMap<Resource.Type, String>();

  /**
   * Set of query results.
   */
  Map<Resource, QueryResult> queryResults = new LinkedHashMap<Resource, QueryResult>();

  /**
   * Sub-resources of the resource which is being operated on.
   */
  private final Map<String, QueryImpl> querySubResourceSet = new HashMap<String, QueryImpl>();

  /**
   * Sub-resource instances of this resource.
   * Map of resource name to resource instance.
   */
  private Map<String, QueryImpl> subResourceSet;

  /**
   * Indicates that the query should include all available properties.
   */
  private boolean allProperties = false;

  /**
   * The user supplied predicate.
   */
  private Predicate userPredicate;

  /**
   * The user supplied page request information.
   */
  private PageRequest pageRequest;

  /**
   * Indicates whether or not the response should be minimal.
   */
  private boolean minimal;

  /**
   * The sub resource properties referenced in the user predicate.
   */
  private final Set<String> subResourcePredicateProperties = new HashSet<String>();

  /**
   * The logger.
   */
  private final static Logger LOG =
      LoggerFactory.getLogger(QueryImpl.class);


  // ----- Constructor -------------------------------------------------------

  /**
   * Constructor
   *
   * @param keyValueMap         the map of key values
   * @param resourceDefinition  the resource definition
   * @param clusterController   the cluster controller
   */
  public QueryImpl(Map<Resource.Type, String> keyValueMap,
                   ResourceDefinition resourceDefinition,
                   ClusterController clusterController) {
    this.resourceDefinition = resourceDefinition;
    this.clusterController  = clusterController;
    setKeyValueMap(keyValueMap);
  }


  // ----- Query -------------------------------------------------------------

  @Override
  public void addProperty(String propertyId, TemporalInfo temporalInfo) {
    if (propertyId.equals("*")) {
      // wildcard
      addAllProperties(temporalInfo);
    } else{
      if (addPropertyToSubResource(propertyId, temporalInfo)){
        addKeyProperties(getResourceDefinition().getType(), !minimal);
      } else {
        if (propertyId.endsWith("/*")) {
          propertyId = propertyId.substring(0, propertyId.length() - 2);
        }
        addLocalProperty(propertyId);
        if (temporalInfo != null) {
          temporalInfoMap.put(propertyId, temporalInfo);
        }
      }
    }
  }

  @Override
  public void addLocalProperty(String property) {
    queryPropertySet.add(property);
  }

  @Override
  public Result execute()
      throws UnsupportedPropertyException,
             SystemException,
             NoSuchResourceException,
             NoSuchParentResourceException {

    queryForResources();
    return getResult(null);
  }

  @Override
  public Predicate getPredicate() {
    return createPredicate();
  }

  @Override
  public Set<String> getProperties() {
    return Collections.unmodifiableSet(queryPropertySet);
  }

  @Override
  public void setUserPredicate(Predicate predicate) {
    userPredicate = predicate;
  }

  @Override
  public void setPageRequest(PageRequest pageRequest) {
    this.pageRequest = pageRequest;
  }

  @Override
  public void setMinimal(boolean minimal) {
    this.minimal = minimal;
  }


// ----- ResourceInstance --------------------------------------------------

  @Override
  public void setKeyValueMap(Map<Resource.Type, String> keyValueMap) {
    this.keyValueMap.putAll(keyValueMap);
  }

  @Override
  public Map<Resource.Type, String> getKeyValueMap() {
    return new HashMap<Resource.Type, String>((keyValueMap));
  }

  @Override
  public Query getQuery() {
    return this;
  }

  @Override
  public ResourceDefinition getResourceDefinition() {
    return resourceDefinition;
  }

  @Override
  public boolean isCollectionResource() {
    return getKeyValueMap().get(getResourceDefinition().getType()) == null;
  }

  @Override
  public Map<String, ResourceInstance> getSubResources() {
    return new HashMap<String, ResourceInstance>(ensureSubResources());
  }


  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    QueryImpl query = (QueryImpl) o;

    return clusterController.equals(query.clusterController) && !(pageRequest != null ?
        !pageRequest.equals(query.pageRequest) :
        query.pageRequest != null) && queryPropertySet.equals(query.queryPropertySet) &&
        resourceDefinition.equals(query.resourceDefinition) &&
        keyValueMap.equals(query.keyValueMap) && !(userPredicate != null ?
        !userPredicate.equals(query.userPredicate) :
        query.userPredicate != null);
  }

  @Override
  public int hashCode() {
    int result = resourceDefinition.hashCode();
    result = 31 * result + clusterController.hashCode();
    result = 31 * result + queryPropertySet.hashCode();
    result = 31 * result + keyValueMap.hashCode();
    result = 31 * result + (userPredicate != null ? userPredicate.hashCode() : 0);
    result = 31 * result + (pageRequest != null ? pageRequest.hashCode() : 0);
    return result;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Get the map of sub-resources.  Lazily create the map if required.  
   */
  protected Map<String, QueryImpl> ensureSubResources() {
    if (subResourceSet == null) {
      subResourceSet = new HashMap<String, QueryImpl>();
      Set<SubResourceDefinition> setSubResourceDefs =
          getResourceDefinition().getSubResourceDefinitions();

      ClusterController controller = clusterController;

      for (SubResourceDefinition subResDef : setSubResourceDefs) {
        Resource.Type type = subResDef.getType();
        Map<Resource.Type, String> valueMap = getKeyValueMap();
        QueryImpl resource =  new QueryImpl(valueMap,
            ResourceInstanceFactoryImpl.getResourceDefinition(type, valueMap),
            controller);
        resource.setMinimal(minimal);

        Schema schema = controller.getSchema(type);

        // ensure pk is returned
        resource.addLocalProperty(schema.getKeyPropertyId(type));

        if (!minimal) {
          // add additionally required fk properties
          for (Resource.Type fkType : subResDef.getAdditionalForeignKeys()) {
            resource.addLocalProperty(schema.getKeyPropertyId(fkType));
          }
        }

        String subResourceName = subResDef.isCollection() ?
            resource.getResourceDefinition().getPluralName() :
            resource.getResourceDefinition().getSingularName();

        subResourceSet.put(subResourceName, resource);
      }
    }
    return subResourceSet;
  }

  /**
   * Query the cluster controller for the top level resources.
   */
  private void queryForResources()
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    Set<Resource> providerResourceSet = new HashSet<Resource>();

    Resource.Type resourceType    = getResourceDefinition().getType();
    Request       request         = createRequest(!minimal);
    Request       qRequest        = createRequest(true);
    Predicate     queryPredicate  = createPredicate(getKeyValueMap(), processUserPredicate(userPredicate));
    Set<Resource> resourceSet     = new LinkedHashSet<Resource>();

    for (Resource queryResource : doQuery(resourceType, qRequest, queryPredicate)) {
      providerResourceSet.add(queryResource);
      resourceSet.add(queryResource);
    }
    queryResults.put(null,
        new QueryResult(request, queryPredicate, userPredicate, getKeyValueMap(), resourceSet));

    clusterController.populateResources(resourceType, providerResourceSet, qRequest, queryPredicate);
    queryForSubResources(userPredicate, hasSubResourcePredicate());
  }

  /**
   * Query the cluster controller for the sub-resources associated with 
   * this query object.
   */
  private void queryForSubResources(Predicate predicate, boolean hasSubResourcePredicate)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    for (Map.Entry<String, QueryImpl> entry : querySubResourceSet.entrySet()) {

      QueryImpl     subResource  = entry.getValue();
      Resource.Type resourceType = subResource.getResourceDefinition().getType();
      Request       request      = subResource.createRequest(!minimal);
      Request       qRequest     = subResource.createRequest(true);

      Predicate subResourcePredicate = hasSubResourcePredicate ?
          getSubResourcePredicate(predicate, entry.getKey()) : null;

      Predicate processedPredicate = hasSubResourcePredicate ? subResource.processUserPredicate(subResourcePredicate) : null;

      Set<Resource> providerResourceSet = new HashSet<Resource>();

      for (QueryResult queryResult : queryResults.values()) {
        for (Resource resource : queryResult.getProviderResourceSet()) {

          Map<Resource.Type, String> map = getKeyValueMap(resource, queryResult.getKeyValueMap());

          Predicate queryPredicate = subResource.createPredicate(map, processedPredicate);

          Set<Resource> resourceSet = new LinkedHashSet<Resource>();

          try {
            for (Resource queryResource : subResource.doQuery(resourceType, qRequest, queryPredicate)) {
              providerResourceSet.add(queryResource);
              resourceSet.add(queryResource);
            }
          } catch (NoSuchResourceException e) {
            // do nothing ...
          }
          subResource.queryResults.put(resource,
              new QueryResult(request, queryPredicate, subResourcePredicate, map, resourceSet));
        }
      }
      clusterController.populateResources(resourceType, providerResourceSet, qRequest, null);
      subResource.queryForSubResources(subResourcePredicate, hasSubResourcePredicate);
    }
  }

  /**
   * Query the cluster controller for the resources.
   */
  private Set<Resource> doQuery(Resource.Type type, Request request, Predicate predicate)
      throws UnsupportedPropertyException,
      SystemException,
      NoSuchResourceException,
      NoSuchParentResourceException {

    if (queryPropertySet.isEmpty() && querySubResourceSet.isEmpty()) {
      //Add sub resource properties for default case where no fields are specified.
      querySubResourceSet.putAll(ensureSubResources());
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Executing resource query: " + request + " where " + predicate);
    }
    return clusterController.getResources(type, request, predicate);
  }

  /**
   * Get a map of property sets keyed by the resources associated with this query.
   * The property sets should contain the joined sets of all of the requested
   * properties from each resource's sub-resources.
   *
   * For example, if this query is associated with the resources
   * AResource1, AResource1 and AResource3 as follows ...
   *
   * a_resources
   * │
   * └──AResource1 ─────────────AResource1 ─────────────AResource3
   *      │                       │                       │
   *      ├── b_resources         ├── b_resources         ├── BResources
   *      │   ├── BResource1      │   ├── BResource3      │    └── BResource5
   *      │   │     p1:1          │   │     p1:3          │          p1:5
   *      │   │     p2:5          │   │     p2:5          │          p2:5
   *      │   │                   │   │                   │
   *      │   └── BResource2      │   └── BResource4      └── c_resources
   *      │         p1:2          │         p1:4              └── CResource4
   *      │         p2:0          │         p2:0                    p3:4
   *      │                       │
   *      └── c_resources         └── c_resources
   *          ├── CResource1          └── CResource3
   *          │     p3:1                    p3:3
   *          │
   *          └── CResource2
   *                p3:2
   *
   * Given the following query ...
   *
   *     api/v1/a_resources?b_resources/p1>3&b_resources/p2=5&c_resources/p3=1
   *
   * The caller should pass the following property ids ...
   *
   *     b_resources/p1
   *     b_resources/p2
   *     c_resources/p3
   *
   * getJoinedResourceProperties should produce the following map of property sets
   * by making recursive calls on the sub-resources of each of this query's resources,
   * joining the resulting property sets, and adding them to the map keyed by the
   * resource ...
   *
   *  {
   *    AResource1=[{b_resources/p1=1, b_resources/p2=5, c_resources/p3=1},
   *                {b_resources/p1=2, b_resources/p2=0, c_resources/p3=1},
   *                {b_resources/p1=1, b_resources/p2=5, c_resources/p3=2},
   *                {b_resources/p1=2, b_resources/p2=0, c_resources/p3=2}],
   *    AResource2=[{b_resources/p1=3, b_resources/p2=5, c_resources/p3=3},
   *                {b_resources/p1=4, b_resources/p2=0, c_resources/p3=3}],
   *    AResource3=[{b_resources/p1=5, b_resources/p2=5, c_resources/p3=4}],
   *  }
   *
   * @param propertyIds     the requested properties
   * @param parentResource  the parent resource; may be null
   * @param category        the sub-resource category; may be null
   *
   * @return a map of property sets keyed by the resources associated with this query
   */
  protected Map<Resource, Set<Map<String, Object>>> getJoinedResourceProperties(Set<String> propertyIds,
                                                                                Resource parentResource,
                                                                                String category)
      throws SystemException, UnsupportedPropertyException, NoSuchParentResourceException, NoSuchResourceException {

    Map<Resource, Set<Map<String, Object>>> resourcePropertyMaps =
        new HashMap<Resource, Set<Map<String, Object>>>();

    Map<String, String> categoryPropertyIdMap =
        getPropertyIdsForCategory(propertyIds, category);

    for (Map.Entry<Resource, QueryResult> queryResultEntry : queryResults.entrySet()) {
      QueryResult queryResult         = queryResultEntry.getValue();
      Resource    queryParentResource = queryResultEntry.getKey();

      // for each resource for the given parent ...
      if (queryParentResource == parentResource) {

        Iterable<Resource> iterResource = clusterController.getIterable(
            resourceDefinition.getType(), queryResult.getProviderResourceSet(),
            queryResult.getRequest(), queryResult.getPredicate());

        for (Resource resource : iterResource) {
          // get the resource properties
          Map<String, Object> resourcePropertyMap = new HashMap<String, Object>();
          for (Map.Entry<String, String> categoryPropertyIdEntry : categoryPropertyIdMap.entrySet()) {
            Object value = resource.getPropertyValue(categoryPropertyIdEntry.getValue());
            if (value != null) {
              resourcePropertyMap.put(categoryPropertyIdEntry.getKey(), value);
            }
          }

          Set<Map<String, Object>> propertyMaps = new HashSet<Map<String, Object>>();

          // For each sub category get the property maps for the sub resources
          for (Map.Entry<String, QueryImpl> entry : querySubResourceSet.entrySet()) {
            String subResourceCategory = category == null ? entry.getKey() : category + "/" + entry.getKey();

            QueryImpl subResource = entry.getValue();

            Map<Resource, Set<Map<String, Object>>> subResourcePropertyMaps =
                subResource.getJoinedResourceProperties(propertyIds, resource, subResourceCategory);

            Set<Map<String, Object>> combinedSubResourcePropertyMaps = new HashSet<Map<String, Object>>();
            for (Set<Map<String, Object>> maps : subResourcePropertyMaps.values()) {
              combinedSubResourcePropertyMaps.addAll(maps);
            }
            propertyMaps = joinPropertyMaps(propertyMaps, combinedSubResourcePropertyMaps);
          }
          // add parent resource properties to joinedResources
          if (!resourcePropertyMap.isEmpty()) {
            if (propertyMaps.isEmpty()) {
              propertyMaps.add(resourcePropertyMap);
            } else {
              for (Map<String, Object> propertyMap : propertyMaps) {
                propertyMap.putAll(resourcePropertyMap);
              }
            }
          }
          resourcePropertyMaps.put(resource, propertyMaps);
        }
      }
    }
    return resourcePropertyMaps;
  }

  // Map the given set of property ids to corresponding property ids in the
  // given sub-resource category.
  private Map<String, String> getPropertyIdsForCategory(Set<String> propertyIds, String category) {
    Map<String, String> map = new HashMap<String, String>();

    for (String propertyId : propertyIds) {
      if (category == null || propertyId.startsWith(category)) {
        map.put(propertyId, category==null ? propertyId : propertyId.substring(category.length() + 1));
      }
    }
    return map;
  }

  // Join two sets of property maps into one.
  private static Set<Map<String, Object>> joinPropertyMaps(Set<Map<String, Object>> propertyMaps1,
                                                           Set<Map<String, Object>> propertyMaps2) {
    Set<Map<String, Object>> propertyMaps = new HashSet<Map<String, Object>>();

    if (propertyMaps1.isEmpty()) {
      return propertyMaps2;
    }
    if (propertyMaps2.isEmpty()) {
      return propertyMaps1;
    }

    for (Map<String, Object> map1 : propertyMaps1) {
      for (Map<String, Object> map2 : propertyMaps2) {
        Map<String, Object> joinedMap = new HashMap<String, Object>(map1);
        joinedMap.putAll(map2);
        propertyMaps.add(joinedMap);
      }
    }
    return propertyMaps;
  }

   // Get a result from this query.
  private Result getResult(Resource parentResource)
      throws UnsupportedPropertyException, SystemException, NoSuchResourceException, NoSuchParentResourceException {

    Result result = new ResultImpl(true);
    Resource.Type resourceType = getResourceDefinition().getType();
    TreeNode<Resource> tree = result.getResultTree();

    if (isCollectionResource()) {
      tree.setProperty("isCollection", "true");
    }

    QueryResult queryResult = queryResults.get(parentResource);

    if (queryResult != null) {
      Predicate queryPredicate     = queryResult.getPredicate();
      Predicate queryUserPredicate = queryResult.getUserPredicate();
      Request   queryRequest       = queryResult.getRequest();

      Set<Resource> providerResourceSet = queryResult.getProviderResourceSet();

      if (hasSubResourcePredicate() && queryUserPredicate != null) {
        queryPredicate = getExtendedPredicate(parentResource, queryUserPredicate);
      }

      Iterable<Resource> iterResource;

      if (pageRequest == null) {
        iterResource = clusterController.getIterable(
            resourceType, providerResourceSet, queryRequest, queryPredicate);
      } else {
        PageResponse pageResponse = clusterController.getPage(
            resourceType, providerResourceSet, queryRequest, queryPredicate, pageRequest);
        iterResource = pageResponse.getIterable();
      }

      Set<String> propertyIds = queryRequest.getPropertyIds();

      int count = 1;
      for (Resource resource : iterResource) {

        // add a child node for the resource and provide a unique name.  The name is never used.
        TreeNode<Resource> node = tree.addChild(
            minimal ? new ResourceImpl(resource, propertyIds) : resource,
            resource.getType() + ":" + count++);

        for (Map.Entry<String, QueryImpl> entry : querySubResourceSet.entrySet()) {
          String    subResCategory = entry.getKey();
          QueryImpl subResource    = entry.getValue();

          TreeNode<Resource> childResult = subResource.getResult(resource).getResultTree();
          childResult.setName(subResCategory);
          childResult.setProperty("isCollection", "false");
          node.addChild(childResult);
        }
      }
    }
    return result;
  }

  // Indicates whether or not this query has sub-resource elements
  // in its predicate.
  private boolean hasSubResourcePredicate() {
    return !subResourcePredicateProperties.isEmpty();
  }

  // Alter the given predicate so that the resources referenced by
  // the predicate will be extended to include the joined properties
  // of their sub-resources.
  private Predicate getExtendedPredicate(Resource parentResource,
                                         Predicate predicate)
      throws SystemException,
             UnsupportedPropertyException,
             NoSuchParentResourceException,
             NoSuchResourceException {

    Map<Resource, Set<Map<String, Object>>> joinedResources =
        getJoinedResourceProperties(subResourcePredicateProperties, parentResource, null);

    ExtendedResourcePredicateVisitor visitor =
        new ExtendedResourcePredicateVisitor(joinedResources);

    PredicateHelper.visit(predicate, visitor);
    return visitor.getExtendedPredicate();
  }

  private void addKeyProperties(Resource.Type resourceType, boolean includeFKs) {
    Schema schema = clusterController.getSchema(resourceType);

    if (includeFKs) {
      for (Resource.Type type : Resource.Type.values()) {
        // add fk's
        String propertyId = schema.getKeyPropertyId(type);
        if (propertyId != null) {
          addProperty(propertyId, null);
        }
      }
    } else {
      // add pk only
      String propertyId = schema.getKeyPropertyId(resourceType);
      addProperty(propertyId, null);
    }
  }

  private void addAllProperties(TemporalInfo temporalInfo) {
    allProperties = true;
    if (temporalInfo != null) {
      temporalInfoMap.put(null, temporalInfo);
    }

    for (Map.Entry<String, QueryImpl> entry : ensureSubResources().entrySet()) {
      String name = entry.getKey();
      if (! querySubResourceSet.containsKey(name)) {
        querySubResourceSet.put(name, entry.getValue());
      }
    }
  }

  private boolean addPropertyToSubResource(String propertyId, TemporalInfo temporalInfo) {
    int    index    = propertyId.indexOf("/");
    String category = index == -1 ? propertyId : propertyId.substring(0, index);

    Map<String, QueryImpl> subResources = ensureSubResources();

    QueryImpl subResource = subResources.get(category);
    if (subResource != null) {
      querySubResourceSet.put(category, subResource);

      //only add if a sub property is set or if a sub category is specified
      if (index != -1) {
        subResource.addProperty(propertyId.substring(index + 1), temporalInfo);
      }
      return true;
    }
    return false;
  }

  private Predicate createInternalPredicate(Map<Resource.Type, String> mapResourceIds) {
    Resource.Type resourceType = getResourceDefinition().getType();
    Schema schema = clusterController.getSchema(resourceType);

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

  private Predicate createPredicate() {
    return createPredicate(getKeyValueMap(), userPredicate);
  }

  private Predicate createPredicate(Map<Resource.Type, String> keyValueMap, Predicate predicate) {
    Predicate internalPredicate = createInternalPredicate(keyValueMap);

    if (internalPredicate == null) {
        return predicate;
    }
    return (predicate == null ? internalPredicate :
          new AndPredicate(predicate, internalPredicate));
  }

  // Get a sub-resource predicate from the given predicate.
  private Predicate getSubResourcePredicate(Predicate predicate, String category) {
    if (predicate == null) {
      return null;
    }

    SubResourcePredicateVisitor visitor = new SubResourcePredicateVisitor(category);
    PredicateHelper.visit(predicate, visitor);
    return visitor.getSubResourcePredicate();
  }

  // Process the given predicate to remove sub-resource elements.
  private Predicate processUserPredicate(Predicate predicate) {
    if (predicate == null) {
      return null;
    }
    ProcessingPredicateVisitor visitor = new ProcessingPredicateVisitor(this);
    PredicateHelper.visit(predicate, visitor);

    // add the sub-resource to the request
    Set<String> categories = visitor.getSubResourceCategories();
    for (String category : categories) {
      addPropertyToSubResource(category, null);
    }
    // record the sub-resource properties on this query
    subResourcePredicateProperties.addAll(visitor.getSubResourceProperties());

    return visitor.getProcessedPredicate();
  }

  private Request createRequest(boolean includeFKs) {
    
    if (allProperties) {
      return PropertyHelper.getReadRequest(Collections.<String>emptySet());
    }
    
    Set<String> setProperties = new HashSet<String>();

    Map<String, TemporalInfo> mapTemporalInfo    = new HashMap<String, TemporalInfo>();
    TemporalInfo              globalTemporalInfo = temporalInfoMap.get(null);
    Resource.Type             resourceType       = getResourceDefinition().getType();

    if (getKeyValueMap().get(resourceType) == null) {
      addKeyProperties(resourceType, includeFKs);
    }

    setProperties.addAll(queryPropertySet);
    
    for (String propertyId : setProperties) {
      TemporalInfo temporalInfo = temporalInfoMap.get(propertyId);
      if (temporalInfo != null) {
        mapTemporalInfo.put(propertyId, temporalInfo);
      } else if (globalTemporalInfo != null) {
        mapTemporalInfo.put(propertyId, globalTemporalInfo);
      }
    }
    return PropertyHelper.getReadRequest(setProperties, mapTemporalInfo);
  }


  // Get a key value map based on the given resource and an existing key value map
  private Map<Resource.Type, String> getKeyValueMap(Resource resource,
                                                    Map<Resource.Type, String> keyValueMap) {
    Map<Resource.Type, String> resourceKeyValueMap = new HashMap<Resource.Type, String>(keyValueMap.size());
    for (Map.Entry<Resource.Type, String> resourceIdEntry : keyValueMap.entrySet()) {
      Resource.Type type = resourceIdEntry.getKey();
      String value = resourceIdEntry.getValue();

      if (value == null) {
        Object o = resource.getPropertyValue(clusterController.getSchema(type).getKeyPropertyId(type));
        value = o == null ? null : o.toString();
      }
      if (value != null) {
        resourceKeyValueMap.put(type, value);
      }
    }
    String resourceKeyProp = clusterController.getSchema(resource.getType()).
        getKeyPropertyId(resource.getType());

    resourceKeyValueMap.put(resource.getType(), resource.getPropertyValue(resourceKeyProp).toString());
    return resourceKeyValueMap;
  }

  // ----- inner class : QueryResult -----------------------------------------

  /**
   * Maintain information about an individual query and its result.
   */
  private static class QueryResult {
    private final Request request;
    private final Predicate predicate;
    private final Predicate userPredicate;
    private final Map<Resource.Type, String> keyValueMap;
    private final Set<Resource> providerResourceSet;

    // ----- Constructor -----------------------------------------------------

    private QueryResult(Request request, Predicate predicate,
                        Predicate userPredicate, Map<Resource.Type, String> keyValueMap,
                        Set<Resource> providerResourceSet) {
      this.request             = request;
      this.predicate           = predicate;
      this.userPredicate       = userPredicate;
      this.keyValueMap         = keyValueMap;
      this.providerResourceSet = providerResourceSet;
    }

    // ----- accessors -------------------------------------------------------

    public Request getRequest() {
      return request;
    }

    public Predicate getPredicate() {
      return predicate;
    }

    public Predicate getUserPredicate() {
      return userPredicate;
    }

    public Map<Resource.Type, String> getKeyValueMap() {
      return keyValueMap;
    }

    public Set<Resource> getProviderResourceSet() {
      return providerResourceSet;
    }
  }
}
