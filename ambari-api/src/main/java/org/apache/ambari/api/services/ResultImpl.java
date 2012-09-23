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

package org.apache.ambari.api.services;


import org.apache.ambari.api.controller.spi.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
//todo: at the moment only supports one level of nesting.
//todo: need to allow arbitrary nesting depth for expansion.
//todo: consider building a tree structure.
public class ResultImpl implements Result {

  private Map<String, List<Resource>> m_mapResources = new HashMap<String, List<Resource>>();

  @Override
  public void addResources(String groupName, List<Resource> listResources) {
    List<Resource> resources = m_mapResources.get(groupName);
    if (resources == null) {
      m_mapResources.put(groupName, listResources);
    } else {
      resources.addAll(listResources);
    }
  }

  @Override
  public Map<String, List<Resource>> getResources() {
    return m_mapResources;
  }
}

