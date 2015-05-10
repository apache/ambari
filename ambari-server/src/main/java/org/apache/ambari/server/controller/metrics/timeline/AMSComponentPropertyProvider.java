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
package org.apache.ambari.server.controller.metrics.timeline;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PredicateHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AMSComponentPropertyProvider extends AMSPropertyProvider {

  public AMSComponentPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                                          StreamProvider streamProvider,
                                          ComponentSSLConfiguration configuration,
                                          MetricHostProvider hostProvider,
                                          String clusterNamePropertyId,
                                          String componentNamePropertyId) {

    super(updateComponentMetricsWithAggregateFunctionSupport(componentPropertyInfoMap),
      streamProvider, configuration, hostProvider,
      clusterNamePropertyId, null, componentNamePropertyId);
  }

  /**
   * This method adds supported propertyInfo for component metrics with
   * aggregate function ids. API calls with multiple aggregate functions
   * applied to a single metric need this support.
   *
   * Currently this support is added only for Component metrics,
   * this can be easily extended to all levels by moving this method to the
   * base class: @AMSPropertyProvider.
   */
  private static Map<String, Map<String, PropertyInfo>> updateComponentMetricsWithAggregateFunctionSupport(
        Map<String, Map<String, PropertyInfo>> componentMetrics) {

    if (componentMetrics == null || componentMetrics.isEmpty()) {
      return componentMetrics;
    }

    // For every component
    for (Map<String, PropertyInfo> componentMetricInfo : componentMetrics.values()) {
      Map<String, PropertyInfo> aggregateMetrics = new HashMap<String, PropertyInfo>();
      // For every metric
      for (Map.Entry<String, PropertyInfo> metricEntry : componentMetricInfo.entrySet()) {
        // For each aggregate function id
        for (String identifierToAdd : aggregateFunctionIdentifierMap.values()) {
          String metricInfoKey = metricEntry.getKey() + identifierToAdd;
          // This disallows metric key suffix of the form "._sum._sum" for
          // the sake of avoiding duplicates
          if (componentMetricInfo.containsKey(metricInfoKey)) {
            continue;
          }

          PropertyInfo propertyInfo = metricEntry.getValue();
          PropertyInfo metricInfoValue = new PropertyInfo(
            propertyInfo.getPropertyId() + identifierToAdd,
            propertyInfo.isTemporal(),
            propertyInfo.isPointInTime());
          metricInfoValue.setAmsHostMetric(propertyInfo.isAmsHostMetric());
          metricInfoValue.setAmsId(propertyInfo.getAmsId());
          metricInfoValue.setUnit(propertyInfo.getUnit());

          aggregateMetrics.put(metricInfoKey, metricInfoValue);
        }
      }
      componentMetricInfo.putAll(aggregateMetrics);
    }

    return componentMetrics;
  }

  @Override
  protected String getHostName(Resource resource) {
    return null;
  }

  @Override
  protected String getComponentName(Resource resource) {
    return (String) resource.getPropertyValue(componentNamePropertyId);
  }
}
