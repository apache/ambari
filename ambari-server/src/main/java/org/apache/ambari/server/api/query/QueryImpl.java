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
import org.apache.ambari.server.api.services.ResultImpl;
import org.apache.ambari.server.controller.internal.PropertyIdImpl;
import org.apache.ambari.server.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.BasePredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.api.util.TreeNode;

import java.util.*;

/**
 * Default read query.
 */
public class QueryImpl implements Query {
  /**
   * Resource definition of resource being operated on.
   */
  private ResourceDefinition m_resourceDefinition;

  /**
   * Properties of the query which make up the select portion of the query.
   */
  private Map<String, Set<String>> m_mapQueryProperties = new HashMap<String, Set<String>>();

  /**
   * All properties that are available for the resource.
   */
  private Map<String, Set<String>> m_mapAllProperties;

  /**
   * Sub-resources of the resource which is being operated on.
   */
  private Map<String, ResourceDefinition> m_mapSubResources = new HashMap<String, ResourceDefinition>();

  /**
   * The user supplied predicate.
   */
  private Predicate m_userPredicate;


  /**
   * Constructor.
   *
   * @param resourceDefinition the resource definition of the resource being operated on
   */
  public QueryImpl(ResourceDefinition resourceDefinition) {
    m_resourceDefinition = resourceDefinition;
    m_mapAllProperties = Collections.unmodifiableMap(getClusterController().
        getSchema(resourceDefinition.getType()).getCategories());
  }

  @Override
  public void addProperty(String path, String property) {
    if (path == null && property.equals("*")) {
      // wildcard
      addAllProperties();
    } else if (m_mapAllProperties.containsKey(path) && m_mapAllProperties.get(path).contains(property)) {
      // local property
      Set<String> setProps = m_mapQueryProperties.get(path);
      if (setProps == null) {
        setProps = new HashSet<String>();
        m_mapQueryProperties.put(path, setProps);
      }
      setProps.add(property);
    } else if (m_mapAllProperties.containsKey(property)) {
      // no path specified because path is provided as property
      //local category
      Set<String> setProps = m_mapQueryProperties.get(property);
      if (setProps == null) {
        setProps = new HashSet<String>();
        m_mapQueryProperties.put(property, setProps);
      }
      // add all props for category
      setProps.addAll(m_mapAllProperties.get(property));
    } else {
      // not a local category/property
      boolean success = addPropertyToSubResource(path, property);
      if (!success) {
        //TODO
        throw new RuntimeException("Attempted to add invalid property to resource.  Resource=" +
            m_resourceDefinition.getType() + ", Property: Category=" + path + " Field=" + property);
      }
    }
  }

  @Override
  public void addProperty(PropertyId property) {
    addProperty(property.getCategory(), property.getName());
  }

  @Override
  public Result execute() throws AmbariException {
    Result result = createResult();

    if (m_resourceDefinition.getId() == null) {
      // collection, add pk only
      Schema schema = getClusterController().getSchema(m_resourceDefinition.getType());
      addProperty(schema.getKeyPropertyId(m_resourceDefinition.getType()));
      result.getResultTree().setProperty("isCollection", "true");
    }

    if (m_mapQueryProperties.isEmpty() && m_mapSubResources.isEmpty()) {
      //Add sub resource properties for default case where no fields are specified.
      m_mapSubResources.putAll(m_resourceDefinition.getSubResources());
    }

    Predicate predicate = createPredicate(m_resourceDefinition);
    Iterable<Resource> iterResource = getClusterController().getResources(
        m_resourceDefinition.getType(), createRequest(), predicate);

    TreeNode<Resource> tree = result.getResultTree();
    for (Resource resource : iterResource) {
      TreeNode<Resource> node = tree.addChild(resource, null);

      for (Map.Entry<String, ResourceDefinition> entry : m_mapSubResources.entrySet()) {
        String subResCategory = entry.getKey();
        ResourceDefinition r = entry.getValue();

        r.setParentId(m_resourceDefinition.getType(), resource.getPropertyValue(
            getClusterController().getSchema(m_resourceDefinition.getType()).
                getKeyPropertyId(m_resourceDefinition.getType())));

        TreeNode<Resource> childResult = r.getQuery().execute().getResultTree();
        childResult.setName(subResCategory);
        childResult.setProperty("isCollection", "false");
        node.addChild(childResult);
      }
    }

    return result;
  }

  @Override
  public Predicate getInternalPredicate() {
    return createInternalPredicate(m_resourceDefinition);
  }

  @Override
  public Map<String, Set<String>> getProperties() {
    return Collections.unmodifiableMap(m_mapQueryProperties);
  }

  @Override
  public void setUserPredicate(Predicate predicate) {
    m_userPredicate = predicate;
  }

  private void addAllProperties() {
    m_mapQueryProperties.putAll(m_mapAllProperties);
    for (Map.Entry<String, ResourceDefinition> entry : m_resourceDefinition.getSubResources().entrySet()) {
      String name = entry.getKey();
      if (! m_mapSubResources.containsKey(name)) {
        m_mapSubResources.put(name, entry.getValue());
      }
    }
  }

  private boolean addPropertyToSubResource(String path, String property) {
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

    ResourceDefinition subResource = m_resourceDefinition.getSubResources().get(p);
    if (subResource != null) {
      m_mapSubResources.put(p, subResource);
      //todo: handle case of trailing '/' (for example fields=subResource/)

      if (property != null || !path.equals(p)) {
        //only add if a sub property is set or if a sub category is specified
        subResource.getQuery().addProperty(i == -1 ? null : path.substring(i + 1), property);
      }
      resourceAdded = true;
    }
    return resourceAdded;
  }

  private BasePredicate createInternalPredicate(ResourceDefinition resourceDefinition) {
    //todo: account for user predicates
    Resource.Type resourceType = resourceDefinition.getType();
    Map<Resource.Type, String> mapResourceIds = resourceDefinition.getResourceIds();
    Schema schema = getClusterController().getSchema(resourceType);

    Set<BasePredicate> setPredicates = new HashSet<BasePredicate>();
    for (Map.Entry<Resource.Type, String> entry : mapResourceIds.entrySet()) {
      //todo: null check is a hack for host_component and component queries where serviceId is not available for
      //todo: host_component queries and host is not available for component queries.
      //todo: this should be rectified when the data model is changed for host_component
      if (entry.getValue() != null) {
        setPredicates.add(new EqualsPredicate(schema.getKeyPropertyId(entry.getKey()), entry.getValue()));
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

  private Predicate createPredicate(ResourceDefinition resourceDefinition) {
    BasePredicate internalPredicate = createInternalPredicate(resourceDefinition);
    //todo: remove cast when predicate hierarchy is fixed
    return m_userPredicate == null ? internalPredicate :
        new AndPredicate((BasePredicate) m_userPredicate, internalPredicate);
  }

  ClusterController getClusterController() {
    return ClusterControllerHelper.getClusterController();
  }

  Request createRequest() {
    Set<PropertyId> setProperties = new HashSet<PropertyId>();

    for (Map.Entry<String, Set<String>> entry : m_mapQueryProperties.entrySet()) {
      String group = entry.getKey();
      for (String property : entry.getValue()) {
        setProperties.add(new PropertyIdImpl(property, group, false));
      }
    }
    return PropertyHelper.getReadRequest(setProperties);
  }

  Result createResult() {
    return new ResultImpl();
  }

}
