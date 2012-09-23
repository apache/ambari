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

import java.util.*;

/**
 * Formatter for cluster instance resources.
 */
public class ClusterInstanceFormatter extends BaseFormatter {

  /**
   * services collection
   */
  public List<HrefEntry> services = new ArrayList<HrefEntry>();

  /**
   * hosts collection
   */
  public List<HrefEntry> hosts = new ArrayList<HrefEntry>();

  /**
   * Constructor.
   *
   * @param resourceDefinition resource definition
   */
  public ClusterInstanceFormatter(ResourceDefinition resourceDefinition) {
    super(resourceDefinition);
  }

  /**
   * Add services and hosts href's.
   *
   * @param href the href to add
   * @param r    the resource being added
   */
  @Override
  public void addSubResource(HrefEntry href, Resource r) {
    if (r.getType() == Resource.Type.Service) {
      services.add(href);
    } else {
      hosts.add(href);
    }
  }
}
