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

package org.apache.ambari.server.controller.jmx;

import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.spi.*;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Property provider implementation for JMX sources.
 */
public class JMXPropertyProvider implements PropertyProvider {

  private static final String NAME_KEY = "name";
  private static final String PORT_KEY = "tag.port";

  /**
   * Set of property ids supported by this provider.
   */
  private final Set<String> propertyIds;

  private final Map<String, Map<String, PropertyInfo>> componentMetrics;

  private final StreamProvider streamProvider;

  private final JMXHostProvider jmxHostProvider;

  private static final Map<String, String> JMX_PORTS = new HashMap<String, String>();

  private final String clusterNamePropertyId;

  private final String hostNamePropertyId;

  private final String componentNamePropertyId;


  static {
    JMX_PORTS.put("NAMENODE",     "50070");
    JMX_PORTS.put("DATANODE",     "50075");
    JMX_PORTS.put("JOBTRACKER",   "50030");
    JMX_PORTS.put("TASKTRACKER",  "50060");
    JMX_PORTS.put("HBASE_MASTER", "60010");
  }

  protected final static Logger LOG =
      LoggerFactory.getLogger(JMXPropertyProvider.class);


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a JMX property provider.
   *
   * @param componentMetrics         the map of supported metrics
   * @param streamProvider           the stream provider
   * @param jmxHostProvider          the host mapping
   * @param clusterNamePropertyId    the cluster name property id
   * @param hostNamePropertyId       the host name property id
   * @param componentNamePropertyId  the component name property id
   */
  public JMXPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics,
                             StreamProvider streamProvider,
                             JMXHostProvider jmxHostProvider,
                             String clusterNamePropertyId,
                             String hostNamePropertyId,
                             String componentNamePropertyId) {
    this.componentMetrics         = componentMetrics;
    this.streamProvider           = streamProvider;
    this.jmxHostProvider          = jmxHostProvider;
    this.clusterNamePropertyId    = clusterNamePropertyId;
    this.hostNamePropertyId       = hostNamePropertyId;
    this.componentNamePropertyId  = componentNamePropertyId;

    propertyIds = new HashSet<String>();
    for (Map.Entry<String, Map<String, PropertyInfo>> entry : componentMetrics.entrySet()) {
      propertyIds.addAll(entry.getValue().keySet());
    }
  }


  // ----- PropertyProvider --------------------------------------------------

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate)
      throws SystemException {


    Set<Resource> keepers = new HashSet<Resource>();
    for (Resource resource : resources) {
      if (populateResource(resource, request, predicate)) {
        keepers.add(resource);
      }
    }
    return keepers;
  }

  @Override
  public Set<String> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    if (!this.propertyIds.containsAll(propertyIds)) {
      Set<String> unsupportedPropertyIds = new HashSet<String>(propertyIds);
      unsupportedPropertyIds.removeAll(this.propertyIds);
      return unsupportedPropertyIds;
    }
    return Collections.emptySet();
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Populate a resource by obtaining the requested JMX properties.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   *
   * @return true if the resource was successfully populated with the requested properties
   */
  private boolean populateResource(Resource resource, Request request, Predicate predicate)
      throws SystemException {

    Set<String> ids = PropertyHelper.getRequestPropertyIds(propertyIds, request, predicate);
    if (ids.isEmpty()) {
      return true;
    }

    String clusterName   = (String) resource.getPropertyValue(clusterNamePropertyId);

    // TODO : what should we do if the host mapping is null?
    if (jmxHostProvider.getHostMapping(clusterName) == null) {
      return true;
    }

    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);
    String port          = JMX_PORTS.get(componentName);

    String hostName;
    if (hostNamePropertyId == null) {
      hostName = jmxHostProvider.getHostName(clusterName, componentName);
    }
    else {
      String name = (String) resource.getPropertyValue(hostNamePropertyId);
      hostName = jmxHostProvider.getHostMapping(clusterName).get(name);
    }

    Map<String, PropertyInfo> metrics = componentMetrics.get(componentName);

    if (metrics == null || hostName == null || port == null) {
      return true;
    }

    String spec = getSpec(hostName + ":" + port);

    try {
      JMXMetricHolder metricHolder = new ObjectMapper().readValue(streamProvider.readFrom(spec), JMXMetricHolder.class);

      Map<String, Map<String, Object>> categories = new HashMap<String, Map<String, Object>>();

      for (Map<String, Object> bean : metricHolder.getBeans()) {
        String category = getCategory(bean);
        if (category != null) {
          categories.put(category, bean);
        }
      }

      for (String propertyId : ids) {

        PropertyInfo propertyInfo = metrics.get(propertyId);

        if (propertyInfo != null && propertyInfo.isPointInTime()) {

          String property = propertyInfo.getPropertyId();
          String category = "";

          List<String> keyList = new LinkedList<String>();
          int keyStartIndex = property.indexOf('[', 0);
          int firstKeyIndex = keyStartIndex > -1 ? keyStartIndex : property.length();
          while (keyStartIndex > -1) {
            int keyEndIndex = property.indexOf(']', keyStartIndex);
            if (keyEndIndex > -1 & keyEndIndex > keyStartIndex) {
              keyList.add(property.substring(keyStartIndex + 1, keyEndIndex));
              keyStartIndex = property.indexOf('[', keyEndIndex);
            }
            else {
              keyStartIndex = -1;
            }
          }


          int dotIndex = property.lastIndexOf('.', firstKeyIndex - 1);
          if (dotIndex != -1){
            category = property.substring(0, dotIndex);
            property = property.substring(dotIndex + 1, firstKeyIndex);
          }

          Map<String, Object> properties = categories.get(category);
          if (properties != null && properties.containsKey(property)) {
            Object value = properties.get(property);
            if (keyList.size() > 0 && value instanceof Map) {
              Map map = (Map) value;
              for (String key : keyList) {
                value = map.get(key);
                if (value instanceof Map) {
                  map = (Map) value;
                }
                else {
                  break;
                }
              }
            }
            resource.setProperty(propertyId, value);
          }
        }
      }
    } catch (IOException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Caught exception getting JMX metrics : spec=" + spec, e);
      }
    }

    return true;
  }

  private String getCategory(Map<String, Object> bean) {
    if (bean.containsKey(NAME_KEY)) {
      String name = (String) bean.get(NAME_KEY);

      if (bean.containsKey(PORT_KEY)) {
        String port = (String) bean.get(PORT_KEY);
        name = name.replace("ForPort" + port, "");
      }
      return name;
    }
    return null;
  }

  /**
   * Get the spec to locate the JMX stream from the given source
   *
   * @param jmxSource  the source (host and port)
   *
   * @return the spec
   */
  protected String getSpec(String jmxSource) {
//    return "http://" + jmxSource + "/jmx?qry=Hadoop:*";
    return "http://" + jmxSource + "/jmx";
  }
}
