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
import org.apache.ambari.api.services.Result;
import org.apache.ambari.api.controller.spi.Resource;
import org.apache.ambari.api.controller.spi.Schema;

import javax.ws.rs.core.UriInfo;

/**
 * HostComponent instance formatter.
 */
public class HostComponentInstanceFormatter extends BaseFormatter {
  /**
   * Related host.
   */
  public HrefEntry host;

  /**
   * Related component.
   */
  public HrefEntry component;

  /**
   * Constructor.
   *
   * @param resourceDefinition the resource definition
   */
  public HostComponentInstanceFormatter(ResourceDefinition resourceDefinition) {
    super(resourceDefinition);
  }

  /**
   * Extends the base format to add the host.
   *
   * @param result  the result being formatted
   * @param uriInfo url info
   * @return the formatted hostComponent instance
   */
  @Override
  public Object format(Result result, UriInfo uriInfo) {
    Object o = super.format(result, uriInfo);
    host = new HrefEntry(href.substring(0, href.indexOf("/host_components/")));

    return o;
  }

  /**
   * Add component.
   *
   * @param href the href to add
   * @param r    the resource being added
   */
  @Override
  public void addSubResource(HrefEntry href, Resource r) {
    component = href;
  }

  /**
   * Build the component href.
   *
   * @param baseHref base url
   * @param schema   associated schema
   * @param relation the component resource
   * @return href for the associated component resource
   */
  @Override
  String buildRelationHref(String baseHref, Schema schema, Resource relation) {
    ResourceDefinition resourceDefinition = getResourceDefinition();
    String clusterId = resourceDefinition.getResourceIds().get(Resource.Type.Cluster);
    String serviceId = relation.getPropertyValue(schema.getKeyPropertyId(Resource.Type.Service));
    String componentId = resourceDefinition.getId();
    return href.substring(0, href.indexOf(clusterId) + clusterId.length() + 1) +
        "services/" + serviceId + "/components/" + componentId;
  }
}
