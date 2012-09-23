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

package org.apache.ambari.api.services.formatters;


import org.apache.ambari.api.controller.utilities.ClusterControllerHelper;
import org.apache.ambari.api.resource.ResourceDefinition;
import org.apache.ambari.api.services.Result;
import org.apache.ambari.api.controller.spi.Resource;
import org.apache.ambari.api.controller.spi.Schema;

import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base result formatter.
 */
public abstract class BaseFormatter implements ResultFormatter {
  /**
   * Request url.
   */
  public String href;

  /**
   * properties collection
   */
  public Map<String, Map<String, String>> properties = new HashMap<String, Map<String, String>>();

  /**
   * Resource definition.
   */
  private ResourceDefinition m_resourceDefinition;

  /**
   * Constructor.
   *
   * @param resourceDefinition the resource definition
   */
  BaseFormatter(ResourceDefinition resourceDefinition) {
    m_resourceDefinition = resourceDefinition;
  }

  /**
   * Format the given result to a format expected by client.
   *
   * @param result  internal result       the internal result
   * @param uriInfo URL info for request  url info for the request
   * @return the formatted result
   */
  @Override
  public Object format(Result result, UriInfo uriInfo) {
    href = uriInfo.getAbsolutePath().toString();

    Map<String, List<Resource>> mapResults = result.getResources();
    handleProperties(mapResults);

    String baseHref = href.endsWith("/") ? href : href + '/';
    handleResources(result.getResources().get("/"), baseHref);
    handleChildren(m_resourceDefinition.getChildren(), mapResults, baseHref);
    handleRelations(m_resourceDefinition.getRelations(), mapResults, baseHref);

    return this;
  }

  /**
   * Handle resource properties.
   *
   * @param mapResults results
   */
  void handleProperties(Map<String, List<Resource>> mapResults) {
    List<Resource> listProperties = mapResults.get("/");
    Resource propsResource = listProperties.get(0);
    properties = propsResource.getCategories();
  }

  /**
   * Handle collection resources
   *
   * @param listResources list of resources in collection
   * @param baseHref      the base url
   */
  void handleResources(List<Resource> listResources, String baseHref) {
    // only format resources for collection resources
    if (m_resourceDefinition.getId() != null) return;
    Schema schema = ClusterControllerHelper.getClusterController().getSchema(m_resourceDefinition.getType());
    for (Resource r : listResources) {
      addSubResource(new HrefEntry(baseHref + r.getPropertyValue(schema.getKeyPropertyId(
          m_resourceDefinition.getType()))), r);
    }
  }

  /**
   * Handle child resources.
   *
   * @param setChildren set of child resources
   * @param mapResults  results
   * @param baseHref    base url
   */
  void handleChildren(Set<ResourceDefinition> setChildren, Map<String, List<Resource>> mapResults, String baseHref) {
    for (ResourceDefinition resource : setChildren) {
      String resourceName = resource.getPluralName();
      List<Resource> listResources = mapResults.get(resourceName);
      if (listResources != null) {
        for (Resource r : listResources) {
          handleChild(r, baseHref, resourceName);
        }
      }
    }
  }

  /**
   * Handle a single child resource.
   *
   * @param child        child resource
   * @param baseHref     base url
   * @param resourceName child resource name
   */
  void handleChild(Resource child, String baseHref, String resourceName) {
    Schema schema = ClusterControllerHelper.getClusterController().getSchema(child.getType());
    addSubResource(new HrefEntry(baseHref + resourceName + '/' +
        child.getPropertyValue(schema.getKeyPropertyId(child.getType()))), child);
  }

  /**
   * Handle relation resources.  These are associated resources that are not children.
   *
   * @param setRelations set of related resources
   * @param mapResults   results
   * @param baseHref     base url
   */
  void handleRelations(Set<ResourceDefinition> setRelations, Map<String, List<Resource>> mapResults, String baseHref) {
    for (ResourceDefinition resourceDef : setRelations) {
      String resourceName = resourceDef.getSingularName();
      List<Resource> listResources = mapResults.get(resourceName);
      if (listResources != null) {
        for (Resource r : listResources) {
          handleRelation(r, baseHref);
        }
      }
    }
  }

  /**
   * Handle a single relation resource.
   *
   * @param relation relation resource
   * @param baseHref base url
   */
  void handleRelation(Resource relation, String baseHref) {
    Schema schema = ClusterControllerHelper.getClusterController().getSchema(relation.getType());
    String relationUrl = buildRelationHref(baseHref, schema, relation);

    addSubResource(new HrefEntry(relationUrl), relation);
  }

  /**
   * Build a relation href.
   *
   * @param baseHref the base href
   * @param schema   associated schema
   * @param relation the relation resource
   * @return href for the given relation resource
   */
  String buildRelationHref(String baseHref, Schema schema, Resource relation) {
    //todo: should not be called in this class
    return "";
  }

  /**
   * Get the resource definition.
   *
   * @return the resource definition
   */
  ResourceDefinition getResourceDefinition() {
    return m_resourceDefinition;
  }

  /**
   * Add a sub resource.  This can be a child, relation or resource in the collection.
   *
   * @param href the href to add
   * @param r    the resource being added
   */
  public abstract void addSubResource(HrefEntry href, Resource r);

  /**
   * An href.
   */
  public static class HrefEntry {
    public String href;

    /**
     * Constructor.
     *
     * @param href the href
     */
    public HrefEntry(String href) {
      this.href = href;
    }
  }
}
