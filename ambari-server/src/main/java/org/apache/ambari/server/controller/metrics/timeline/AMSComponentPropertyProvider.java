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

    super(componentPropertyInfoMap, streamProvider, configuration, hostProvider,
      clusterNamePropertyId, null, componentNamePropertyId);
  }

  @Override
  protected Set<String> getRequestPropertyIds(Request request, Predicate predicate) {
    Set<String> supportedPropertyIds = super.getRequestPropertyIds(request, predicate);

    Set<String> unsupportedPropertyIds = new HashSet<String>(request.getPropertyIds());
    if (predicate != null) {
      unsupportedPropertyIds.addAll(PredicateHelper.getPropertyIds(predicate));
    }
    unsupportedPropertyIds.removeAll(supportedPropertyIds);
    // Allow for aggregate function names to be a part of metrics names
    if (!unsupportedPropertyIds.isEmpty()) {
      for (String propertyId : unsupportedPropertyIds) {
        String[] idWithFunctionArray = stripFunctionFromMetricName(propertyId);
        if (idWithFunctionArray != null) {
          propertyIdAggregateFunctionMap.put(idWithFunctionArray[0], idWithFunctionArray[1]);
          supportedPropertyIds.add(idWithFunctionArray[0]);
        }
      }
    }

    return supportedPropertyIds;
  }

  /**
   * Return array of function identifier and metricsName stripped of function
   * identifier for metricsNames with aggregate function identifiers as
   * trailing suffixes.
   */
  protected String[] stripFunctionFromMetricName(String propertyId) {
    for (Map.Entry<AGGREGATE_FUNCTION_IDENTIFIER, String> identifierEntry :
        aggregateFunctionIdentifierMap.entrySet()) {
      if (propertyId.endsWith(identifierEntry.getValue())) {
        String[] retVal = new String[2];
        retVal[0] = StringUtils.removeEnd(propertyId, identifierEntry.getValue());
        retVal[1] = identifierEntry.getValue();
        return retVal;
      }
    }
    return null;
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
