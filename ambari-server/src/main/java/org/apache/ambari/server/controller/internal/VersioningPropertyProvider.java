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

package org.apache.ambari.server.controller.internal;

import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A property provider that delegates to other Property providers based on the
 * metrics version of the cluster associated with the resources being populated.
 */
public class VersioningPropertyProvider extends BaseProvider implements PropertyProvider {

  private final Map<String, PropertyHelper.MetricsVersion> clusterVersions;
  private final Map<PropertyHelper.MetricsVersion, AbstractPropertyProvider> providers;
  private final AbstractPropertyProvider defaultProvider;
  private final String clusterNamePropertyId;

  /**
   * Create a version aware property provider.
   */
  public VersioningPropertyProvider(Map<String, PropertyHelper.MetricsVersion> clusterVersions,
                                    Map<PropertyHelper.MetricsVersion, AbstractPropertyProvider> providers,
                                    AbstractPropertyProvider defaultProvider,
                                    String clusterNamePropertyId) {
    super(getComponentMetrics(providers));

    this.clusterVersions       = clusterVersions;
    this.providers             = providers;
    this.defaultProvider       = defaultProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;
  }

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate)
      throws SystemException {

    Set<Resource> keepers = new HashSet<Resource>();

    // divide up the given resources according to their associated clusters
    Map<String, Set<Resource>> resourcesByCluster = new HashMap<String, Set<Resource>>();

    for (Resource resource : resources) {
      String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

      Set<Resource> resourceSet = resourcesByCluster.get(clusterName);

      if (resourceSet == null) {
        resourceSet = new HashSet<Resource>();
        resourcesByCluster.put(clusterName, resourceSet);
      }
      resourceSet.add(resource);
    }

    // give each set of resources to the underlying provider that matches the
    // metrics version of the associated cluster
    for (Map.Entry<String, Set<Resource>> entry : resourcesByCluster.entrySet()) {
      String                   clusterName = entry.getKey();
      Set<Resource>            resourceSet = entry.getValue();
      AbstractPropertyProvider provider    = null;

      if (clusterName == null) {
        provider = defaultProvider;
      } else {
        PropertyHelper.MetricsVersion version = clusterVersions.get(clusterName);

        if (version != null) {
          provider = providers.get(version);
        }
      }

      if (provider != null) {
        keepers.addAll(provider.populateResources(resourceSet, request, predicate));
      }
    }
    return keepers;
  }

  // ----- helper methods ----------------------------------------------------

  private static Set<String> getComponentMetrics(Map<PropertyHelper.MetricsVersion, AbstractPropertyProvider> providers) {

    Set<String> propertyIds = new HashSet<String>();

    for (AbstractPropertyProvider provider : providers.values()) {
      propertyIds.addAll(provider.getPropertyIds());
    }
    return propertyIds;
  }
}
