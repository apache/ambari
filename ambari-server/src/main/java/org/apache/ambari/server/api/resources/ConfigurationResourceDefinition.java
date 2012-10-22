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

import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

/**
 * Service resource definition.
 */
public class ConfigurationResourceDefinition extends BaseResourceDefinition {

  /**
   * value of cluster id foreign key
   */
  private String m_clusterId;

  /**
   * Constructor.
   *
   * @param id        service id value
   * @param clusterId cluster id value
   */
  public ConfigurationResourceDefinition(String configType, String configTag, String clusterId) {
    super(Resource.Type.Configuration, configType);
    m_clusterId = clusterId;
    setResourceId(Resource.Type.Cluster, m_clusterId);
    
    if (null != configTag)
      setProperty(PropertyHelper.getPropertyId("tag", "Config"), configTag);
  }

  @Override
  public String getPluralName() {
    return "configurations";
  }

  @Override
  public String getSingularName() {
    return "configuration";
  }

  @Override
  public Map<String, ResourceDefinition> getSubResources() {
    return new HashMap<String, ResourceDefinition>();
  }
}
