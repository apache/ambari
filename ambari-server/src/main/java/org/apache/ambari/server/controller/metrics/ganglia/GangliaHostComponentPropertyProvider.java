/*
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

package org.apache.ambari.server.controller.metrics.ganglia;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.AmbariServer;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.URLStreamProvider;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.spi.Resource;

/**
 * Ganglia property provider implementation for host component resources.
 */
public class GangliaHostComponentPropertyProvider extends GangliaPropertyProvider {
  // ----- Constructors ------------------------------------------------------

  public GangliaHostComponentPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                                              URLStreamProvider streamProvider,
                                              ComponentSSLConfiguration configuration,
                                              MetricHostProvider hostProvider,
                                              String clusterNamePropertyId,
                                              String hostNamePropertyId,
                                              String componentIdPropertyId) {

    super(componentPropertyInfoMap, streamProvider, configuration, hostProvider,
        clusterNamePropertyId, hostNamePropertyId, componentIdPropertyId);
  }


  // ----- GangliaPropertyProvider -------------------------------------------

  @Override
  protected String getHostName(Resource resource) {
    return (String) resource.getPropertyValue(getHostNamePropertyId());
  }

  @Override
  protected Set<String> getGangliaClusterNames(Resource resource, String clusterName) {
    Long componentId = getComponentId(resource);
    
    return new HashSet<>(GANGLIA_CLUSTER_NAME_MAP.containsKey(componentId) ?
      GANGLIA_CLUSTER_NAME_MAP.get(componentId) :
      Collections.emptyList());
  }
}
