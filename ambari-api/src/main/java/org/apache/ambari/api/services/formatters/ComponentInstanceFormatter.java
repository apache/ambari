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

import org.apache.ambari.api.resource.ResourceDefinition;
import org.apache.ambari.api.controller.spi.Resource;
import org.apache.ambari.api.controller.spi.Schema;

import java.util.*;

/**
 * Component instance formatter.
 */
public class ComponentInstanceFormatter extends BaseFormatter {

  /**
   * host_components collection
   */
  public List<HrefEntry> host_components = new ArrayList<HrefEntry>();

  /**
   * Constructor.
   *
   * @param resourceDefinition resource definition
   */
  public ComponentInstanceFormatter(ResourceDefinition resourceDefinition) {
    super(resourceDefinition);
  }

  /**
   * Add host_components href's.
   *
   * @param href the href to add
   * @param r    the type of resource being added
   */
  @Override
  public void addSubResource(HrefEntry href, Resource r) {
    host_components.add(href);
  }

  /**
   * Build hosts_component href's.
   *
   * @param baseHref the base URL
   * @param schema   associated schema
   * @param relation the host_component resource
   * @return href for a host_component
   */
  @Override
  String buildRelationHref(String baseHref, Schema schema, Resource relation) {
    ResourceDefinition resourceDefinition = getResourceDefinition();
    String clusterId = resourceDefinition.getResourceIds().get(Resource.Type.Cluster);
    return baseHref.substring(0, baseHref.indexOf(clusterId) + clusterId.length() + 1) +
        "hosts/" + relation.getPropertyValue(schema.getKeyPropertyId(Resource.Type.Host)) +
        "/host_components/" + resourceDefinition.getId();
  }
}
