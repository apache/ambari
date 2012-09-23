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
 * Host instance formatter.
 */
public class HostInstanceFormatter extends BaseFormatter {
  /**
   * host_components collection.
   */
  public List<HrefEntry> host_components = new ArrayList<HrefEntry>();


  /**
   * Constructor.
   *
   * @param resourceDefinition the resource definition
   */
  public HostInstanceFormatter(ResourceDefinition resourceDefinition) {
    super(resourceDefinition);
  }

  /**
   * Add host_component href's.
   *
   * @param href the host_component href to add
   * @param r    the host_component resource being added
   */
  @Override
  public void addSubResource(BaseFormatter.HrefEntry href, Resource r) {
    host_components.add(href);
  }
}
