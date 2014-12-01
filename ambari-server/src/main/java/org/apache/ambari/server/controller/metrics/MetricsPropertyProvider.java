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
package org.apache.ambari.server.controller.metrics;

import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.internal.AbstractPropertyProvider;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.metrics.ganglia.GangliaComponentPropertyProvider;
import org.apache.ambari.server.controller.metrics.ganglia.GangliaHostComponentPropertyProvider;
import org.apache.ambari.server.controller.metrics.ganglia.GangliaHostPropertyProvider;
import org.apache.ambari.server.controller.metrics.timeline.AMSComponentPropertyProvider;
import org.apache.ambari.server.controller.metrics.timeline.AMSHostComponentPropertyProvider;
import org.apache.ambari.server.controller.metrics.timeline.AMSHostPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.apache.ambari.server.controller.metrics.MetricsPropertyProvider.MetricsService.TIMELINE_METRICS;

public abstract class MetricsPropertyProvider extends AbstractPropertyProvider {
  protected final static Logger LOG =
    LoggerFactory.getLogger(MetricsPropertyProvider.class);

  protected static final Pattern questionMarkPattern = Pattern.compile("\\?");

  protected final StreamProvider streamProvider;

  protected final MetricHostProvider hostProvider;

  protected final String clusterNamePropertyId;

  protected final String hostNamePropertyId;

  protected final String componentNamePropertyId;

  protected final ComponentSSLConfiguration configuration;

  /**
   * Enumeration to distinguish metrics service installed for a cluster
   */
  public enum MetricsService {
    GANGLIA,
    TIMELINE_METRICS
  }

  public MetricsPropertyProvider(Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
                                 StreamProvider streamProvider,
                                 ComponentSSLConfiguration configuration,
                                 MetricHostProvider hostProvider,
                                 String clusterNamePropertyId,
                                 String hostNamePropertyId,
                                 String componentNamePropertyId) {

    super(componentPropertyInfoMap);

    this.streamProvider           = streamProvider;
    this.configuration            = configuration;
    this.hostProvider             = hostProvider;
    this.clusterNamePropertyId    = clusterNamePropertyId;
    this.hostNamePropertyId       = hostNamePropertyId;
    this.componentNamePropertyId  = componentNamePropertyId;
  }

  public static MetricsPropertyProvider createInstance(
        MetricsService metricsService,
        Resource.Type type,
        Map<String, Map<String, PropertyInfo>> componentPropertyInfoMap,
        StreamProvider streamProvider,
        ComponentSSLConfiguration configuration,
        MetricHostProvider hostProvider,
        String clusterNamePropertyId,
        String hostNamePropertyId,
        String componentNamePropertyId) {

    if (type.isInternalType()) {
      switch (type.getInternalType()) {
        case Host:
          if (metricsService.equals(TIMELINE_METRICS)) {
            return new AMSHostPropertyProvider(componentPropertyInfoMap,
              streamProvider,
              configuration,
              hostProvider,
              clusterNamePropertyId,
              hostNamePropertyId);
          } else {
            return new GangliaHostPropertyProvider(
              componentPropertyInfoMap,
              streamProvider,
              configuration,
              hostProvider,
              clusterNamePropertyId,
              hostNamePropertyId);
          }
        case HostComponent:
          if (metricsService.equals(TIMELINE_METRICS)) {
            return new AMSHostComponentPropertyProvider(
              componentPropertyInfoMap,
              streamProvider,
              configuration,
              hostProvider,
              clusterNamePropertyId,
              hostNamePropertyId,
              componentNamePropertyId);
          } else {
            return new GangliaHostComponentPropertyProvider(
              componentPropertyInfoMap,
              streamProvider,
              configuration,
              hostProvider,
              clusterNamePropertyId,
              hostNamePropertyId,
              componentNamePropertyId);
          }
        case Component:
          if (metricsService.equals(TIMELINE_METRICS)) {
            return new AMSComponentPropertyProvider(
              componentPropertyInfoMap,
              streamProvider,
              configuration,
              hostProvider,
              clusterNamePropertyId,
              componentNamePropertyId);
          } else {
            return new GangliaComponentPropertyProvider(
              componentPropertyInfoMap,
              streamProvider,
              configuration,
              hostProvider,
              clusterNamePropertyId,
              componentNamePropertyId);
          }
        default:
          break;
      }
    }

    return null;
  }

  /**
   * Get the host name for the given resource.
   *
   * @param resource  the resource
   *
   * @return the host name
   */
  protected abstract String getHostName(Resource resource);

  /**
   * Get the component name for the given resource.
   *
   * @param resource  the resource
   *
   * @return the component name
   */
  protected abstract String getComponentName(Resource resource);

  /**
   * Get a comma delimited string from the given set of strings or
   * an empty string if the size of the given set is greater than
   * the given limit.
   *
   * @param set    the set of strings
   * @param limit  the upper size limit for the list
   *
   * @return a comma delimited string of strings
   */
  protected static String getSetString(Set<String> set, int limit) {
    StringBuilder sb = new StringBuilder();

    if (limit == -1 || set.size() <= limit) {
      for (String cluster : set) {
        if (sb.length() > 0) {
          sb.append(',');
        }
        sb.append(cluster);
      }
    }
    return sb.toString();
  }
}
