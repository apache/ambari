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
package org.apache.ambari.server.controller.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.spi.ExtendedResourceProvider;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.QueryResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.models.HostInfoSummary;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class HostInfoSummaryResourceProvider extends ReadOnlyResourceProvider implements ExtendedResourceProvider {

  private static final Logger LOG = LoggerFactory.getLogger(HostInfoSummaryResourceProvider.class);

  public static final String HOSTS_SUMMARY = "HostsSummary/hosts_summary";
  public static final String CLUSTER_NAME = "HostsSummary/cluster_name";

  private static final Set<String> PROPERTY_IDS = new HashSet<>();

  private static final Map<Resource.Type, String> KEY_PROPERTY_IDS = new HashMap<>();

  static {
    PROPERTY_IDS.add(CLUSTER_NAME);
    PROPERTY_IDS.add(HOSTS_SUMMARY);
    KEY_PROPERTY_IDS.put(Resource.Type.Cluster, CLUSTER_NAME);
    KEY_PROPERTY_IDS.put(Resource.Type.HostSummary, HOSTS_SUMMARY);
  }

  /**
   * Constructor.
   *
   * @param controller
   */
  HostInfoSummaryResourceProvider(AmbariManagementController controller) {
    super(Resource.Type.HostSummary, PROPERTY_IDS, KEY_PROPERTY_IDS, controller);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QueryResponse queryForResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException,
      NoSuchParentResourceException {

    return new QueryResponseImpl(getResources(request, predicate));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException,
      NoSuchResourceException, NoSuchParentResourceException {

    Set<String> requestPropertyIds = getRequestPropertyIds(request, predicate);

    // use a collection which preserves order since JPA sorts the results
    Set<Resource> results = new LinkedHashSet<>();

    HostInfoSummary hostInfoSummary = new HostInfoSummary();
    Resource resource = new ResourceImpl(Resource.Type.HostSummary);
    // predicate may or may not be there depending on how to query
    // if it has cluster name, the host summary is cluster scope
    // otherwise, the host summary covers all the hosts cross the clusters
    Set<Map<String, Object>> propertyMap = getPropertyMaps(predicate);
    String clusterName = null;
    if (propertyMap.size() != 0) {
      for (Map<String, Object> property : propertyMap) {
        clusterName = (String) property.get(CLUSTER_NAME);
        if (StringUtils.isNotBlank(clusterName)) {
          setResourceProperty(resource, CLUSTER_NAME, clusterName, requestPropertyIds);
          break;
        }
      }
    }
    hostInfoSummary = hostInfoSummary.getHostInfoSummary(clusterName);
    setResourceProperty(resource, HOSTS_SUMMARY, hostInfoSummary.getSummary(), requestPropertyIds);
    results.add(resource);

    return results;
  }


  @Override
  protected Set<String> getPKPropertyIds() {
    return new HashSet<String>();
  }
}
