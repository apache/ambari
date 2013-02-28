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

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.BasePredicate;
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
public class QueryImpl implements Query {

  /**
   * Resource instance.
   */
  private ResourceInstance m_resource;

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
   * Sub-resources of the resource which is being operated on.
   */
  private Map<String, ResourceInstance> m_mapSubResources = new HashMap<String, ResourceInstance>();

  /**
   * The user supplied predicate.
   */
  private Predicate m_userPredicate;

  /**
   * The logger.
   */
  private final static Logger LOG =
      LoggerFactory.getLogger(QueryImpl.class);

  /**
   * Constructor.
   *
   * @param resource the resource being operated on
   */
  public QueryImpl(ResourceInstance resource) {
    m_resource = resource;
  }

  @Override
  public void addProperty(String category, String name, TemporalInfo temporalInfo) {
    if (category == null && name.equals("*")) {
      // wildcard
      addAllProperties(temporalInfo);
    } else{
      if (addPropertyToSubResource(category, name, temporalInfo)){
        // add pk/fk properties of the resource to this query
        Resource.Type resourceType = m_resource.getResourceDefinition().getType();
        Schema        schema       = getClusterController().getSchema(resourceType);

        for (Resource.Type type : m_resource.getIds().keySet()) {
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

    Result result = createResult();
    Resource.Type resourceType = m_resource.getResourceDefinition().getType();
    if (m_resource.getIds().get(resourceType) == null) {
      addCollectionProperties(resourceType);
      result.getResultTree().setProperty("isCollection", "true");
    }

    if (m_setQueryProperties.isEmpty() && m_mapSubResources.isEmpty()) {
      //Add sub resource properties for default case where no fields are specified.
      m_mapSubResources.putAll(m_resource.getSubResources());
    }

    if (LOG.isInfoEnabled()) {
      //todo: include predicate info.  Need to implement toString for all predicates.
      LOG.info("Executing resource query: " + m_resource.getIds());
    }

    Predicate predicate = createPredicate(m_resource);
    Iterable<Resource> iterResource = getClusterController().getResources(
        resourceType, createRequest(), predicate);

    TreeNode<Resource> tree = result.getResultTree();
    int count = 1;
    for (Resource resource : iterResource) {
      // add a child node for the resource and provide a unique name.  The name is never used.
      //todo: provide a more meaningful node name
      TreeNode<Resource> node = tree.addChild(resource, resource.getType() + ":" + count++);
      for (Map.Entry<String, ResourceInstance> entry : m_mapSubResources.entrySet()) {
        String subResCategory = entry.getKey();
        ResourceInstance r = entry.getValue();

        setParentIdsOnSubResource(resource, r);

        TreeNode<Resource> childResult = r.getQuery().execute().getResultTree();
        childResult.setName(subResCategory);
        childResult.setProperty("isCollection", "false");
        node.addChild(childResult);
      }
    }
    return result;
  }

  @Override
  public Predicate getPredicate() {
    //todo: create predicate once
    return createPredicate(m_resource);
  }

  @Override
  public Set<String> getProperties() {
    return Collections.unmodifiableSet(m_setQueryProperties);
  }

  @Override
  public void setUserPredicate(Predicate predicate) {
    m_userPredicate = predicate;
  }

  ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }

  private void addCollectionProperties(Resource.Type resourceType) {
    Schema schema = getClusterController().getSchema(resourceType);
    // add pk
    String property = schema.getKeyPropertyId(resourceType);
    addProperty(PropertyHelper.getPropertyCategory(property), PropertyHelper.getPropertyName(property), null);

    for (Resource.Type type : m_resource.getIds().keySet()) {
      // add fk's
      String keyPropertyId = schema.getKeyPropertyId(type);
      //todo: property id can be null in some cases such as host_component queries which obtain
      //todo: component sub-resources.  Component will not have host fk.
      //todo: refactor so that null check is not required.
      if (keyPropertyId != null) {
        addProperty(PropertyHelper.getPropertyCategory(keyPropertyId), PropertyHelper.getPropertyName(keyPropertyId), null);
      }
    }
  }

  private void addAllProperties(TemporalInfo temporalInfo) {
    allProperties = true;
    if (temporalInfo != null) {
      m_mapCategoryTemporalInfo.put(null, temporalInfo);
    }

    for (Map.Entry<String, ResourceInstance> entry : m_resource.getSubResources().entrySet()) {
      String name = entry.getKey();
      if (! m_mapSubResources.containsKey(name)) {
        m_mapSubResources.put(name, entry.getValue());
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

    ResourceInstance subResource = m_resource.getSubResources().get(p);
    if (subResource != null) {
      m_mapSubResources.put(p, subResource);
      //todo: handle case of trailing '/' (for example fields=subResource/)

      if (property != null || !path.equals(p)) {
        //only add if a sub property is set or if a sub category is specified
        subResource.getQuery().addProperty(i == -1 ? null : path.substring(i + 1), property, temporalInfo);
      }
      resourceAdded = true;
    }
    return resourceAdded;
  }

  private BasePredicate createInternalPredicate(ResourceInstance resource) {
    Resource.Type resourceType = resource.getResourceDefinition().getType();
    Map<Resource.Type, String> mapResourceIds = resource.getIds();
    Schema schema = getClusterController().getSchema(resourceType);

    Set<BasePredicate> setPredicates = new HashSet<BasePredicate>();
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
      return new AndPredicate(setPredicates.toArray(new BasePredicate[setPredicates.size()]));
    } else {
      return null;
    }
  }

  private Predicate createPredicate(ResourceInstance resource) {
    Predicate predicate = null;
    //todo: change reference type to Predicate when predicate hierarchy is fixed
    BasePredicate internalPredicate = createInternalPredicate(resource);
    if (internalPredicate == null) {
      if (m_userPredicate != null) {
        predicate = m_userPredicate;
      }
    } else {
      predicate = (m_userPredicate == null ? internalPredicate :
          new AndPredicate((BasePredicate) m_userPredicate, internalPredicate));
    }
    return predicate;
  }

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

  private void setParentIdsOnSubResource(Resource resource, ResourceInstance r) {
    Map<Resource.Type, String> mapParentIds = m_resource.getIds();
    Map<Resource.Type, String> mapResourceIds = new HashMap<Resource.Type, String>(mapParentIds.size());
    for (Map.Entry<Resource.Type, String> resourceIdEntry : mapParentIds.entrySet()) {
      Resource.Type type = resourceIdEntry.getKey();
      String value = resourceIdEntry.getValue();

      if (value == null) {
        Object o = resource.getPropertyValue(getClusterController().getSchema(type).getKeyPropertyId(type));
        value = o == null ? null : o.toString();
      }
      if (value != null) {
        mapResourceIds.put(type, value);
      }
    }
    String resourceKeyProp = getClusterController().getSchema(resource.getType()).
        getKeyPropertyId(resource.getType());
    //todo: shouldn't use toString here
    mapResourceIds.put(resource.getType(), resource.getPropertyValue(resourceKeyProp).toString());
    r.setIds(mapResourceIds);
  }

  Result createResult() {
    return new ResultImpl(true);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    QueryImpl that = (QueryImpl) o;

    return m_mapCategoryTemporalInfo.equals(that.m_mapCategoryTemporalInfo) &&
           m_mapPropertyTemporalInfo.equals(that.m_mapPropertyTemporalInfo) &&
           m_setQueryProperties.equals(that.m_setQueryProperties) &&
           m_mapSubResources.equals(that.m_mapSubResources) &&
           m_resource.equals(that.m_resource) &&
           m_userPredicate == null ? that.m_userPredicate == null : m_userPredicate.equals(that.m_userPredicate);
  }

  @Override
  public int hashCode() {
    int result = m_resource.hashCode();
    result = 31 * result + m_setQueryProperties.hashCode();
    result = 31 * result + m_mapPropertyTemporalInfo.hashCode();
    result = 31 * result + m_mapCategoryTemporalInfo.hashCode();
    result = 31 * result + m_mapSubResources.hashCode();
    result = 31 * result + (m_userPredicate != null ? m_userPredicate.hashCode() : 0);
    return result;
  }
}
