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
import org.apache.ambari.server.controller.metrics.MetricHostProvider;
import org.apache.ambari.server.controller.metrics.ThreadPoolEnabledPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Property provider implementation for JMX sources.
 */
public class JMXPropertyProvider extends ThreadPoolEnabledPropertyProvider {

  private static final String NAME_KEY = "name";
  private static final String PORT_KEY = "tag.port";
  private static final String DOT_REPLACEMENT_CHAR = "#";

  private final static ObjectReader jmxObjectReader;
  private final static ObjectReader stormObjectReader;

  private static final Map<String, String> DEFAULT_JMX_PORTS = new HashMap<String, String>();

  static {
    DEFAULT_JMX_PORTS.put("NAMENODE",           "50070");
    DEFAULT_JMX_PORTS.put("DATANODE",           "50075");
    DEFAULT_JMX_PORTS.put("HBASE_MASTER",       "60010");
    DEFAULT_JMX_PORTS.put("HBASE_REGIONSERVER", "60030");
    DEFAULT_JMX_PORTS.put("RESOURCEMANAGER",     "8088");
    DEFAULT_JMX_PORTS.put("HISTORYSERVER",      "19888");
    DEFAULT_JMX_PORTS.put("NODEMANAGER",         "8042");
    DEFAULT_JMX_PORTS.put("JOURNALNODE",         "8480");
    DEFAULT_JMX_PORTS.put("STORM_REST_API",      "8745");

    ObjectMapper jmxObjectMapper = new ObjectMapper();
    jmxObjectMapper.configure(DeserializationConfig.Feature.USE_ANNOTATIONS, false);
    jmxObjectReader = jmxObjectMapper.reader(JMXMetricHolder.class);

    TypeReference<HashMap<String,Object>> typeRef
            = new TypeReference<
            HashMap<String,Object>
            >() {};
    stormObjectReader = jmxObjectMapper.reader(typeRef);
  }

  protected final static Logger LOG =
      LoggerFactory.getLogger(JMXPropertyProvider.class);

  private static final Pattern dotReplacementCharPattern =
    Pattern.compile(DOT_REPLACEMENT_CHAR);

  private final StreamProvider streamProvider;

  private final JMXHostProvider jmxHostProvider;

  private final String clusterNamePropertyId;

  private final String hostNamePropertyId;

  private final String componentNamePropertyId;

  private final String statePropertyId;

  // ----- Constructors ------------------------------------------------------

  /**
   * Create a JMX property provider.
   *
   * @param componentMetrics         the map of supported metrics
   * @param streamProvider           the stream provider
   * @param jmxHostProvider          the JMX host mapping
   * @param metricHostProvider      the host mapping
   * @param clusterNamePropertyId    the cluster name property id
   * @param hostNamePropertyId       the host name property id
   * @param componentNamePropertyId  the component name property id
   * @param statePropertyId          the state property id
   */
  public JMXPropertyProvider(Map<String, Map<String, PropertyInfo>> componentMetrics,
                             StreamProvider streamProvider,
                             JMXHostProvider jmxHostProvider,
                             MetricHostProvider metricHostProvider,
                             String clusterNamePropertyId,
                             String hostNamePropertyId,
                             String componentNamePropertyId,
                             String statePropertyId) {

    super(componentMetrics, hostNamePropertyId, metricHostProvider);

    this.streamProvider           = streamProvider;
    this.jmxHostProvider          = jmxHostProvider;
    this.clusterNamePropertyId    = clusterNamePropertyId;
    this.hostNamePropertyId       = hostNamePropertyId;
    this.componentNamePropertyId  = componentNamePropertyId;
    this.statePropertyId          = statePropertyId;
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Populate a resource by obtaining the requested JMX properties.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   * @param ticket    a valid ticket
   *
   * @return the populated resource; null if the resource should NOT be part of the result set for the given predicate
   */
  @Override
  protected Resource populateResource(Resource resource, Request request, Predicate predicate, Ticket ticket)
      throws SystemException {

    Set<String> ids = getRequestPropertyIds(request, predicate);
    Set<String> unsupportedIds = new HashSet<String>();
    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);

    if (getComponentMetrics().get(componentName) == null) {
      // If there are no metrics defined for the given component then there is nothing to do.
      return resource;
    }

    for (String id : ids) {
      if (request.getTemporalInfo(id) != null) {
        unsupportedIds.add(id);
      }
      if (!isSupportedPropertyId(componentName, id)) {
        unsupportedIds.add(id);
      }
    }
    ids.removeAll(unsupportedIds);

    if (ids.isEmpty()) {
      // no properties requested
      return resource;
    }

    // Don't attempt to get the JMX properties if the resource is in an unhealthy state
    if (statePropertyId != null) {
      String state = (String) resource.getPropertyValue(statePropertyId);
      if (state != null && !healthyStates.contains(state)) {
        return resource;
      }
    }

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    String protocol = getJMXProtocol(clusterName, componentName);

    boolean httpsEnabled = false;

    if (protocol.equals("https")){
      httpsEnabled = true;
    }

    Set<String> hostNames = getHosts(resource, clusterName, componentName);
    if (hostNames == null || hostNames.isEmpty()) {
      LOG.warn("Unable to get JMX metrics.  No host name for " + componentName);
      return resource;
    }

    InputStream in = null;

    try {
      try {
        for (String hostName : hostNames) {
          try {
            String port = getPort(clusterName, componentName, hostName, httpsEnabled);
            if (port == null) {
              LOG.warn("Unable to get JMX metrics.  No port value for " + componentName);
              return resource;
            }
            if (LOG.isDebugEnabled()) {
              LOG.debug("Spec: " + getSpec(protocol, hostName, port, "/jmx"));
            }
            in = streamProvider.readFrom(getSpec(protocol, hostName, port, "/jmx"));
            // if the ticket becomes invalid (timeout) then bail out
            if (!ticket.isValid()) {
              return resource;
            }

            getHadoopMetricValue(in, ids, resource, request, ticket);

          } catch (IOException e) {
            logException(e);
          }
        }
      } finally {
        if (in != null) {
          in.close();
        }
      }
    } catch (IOException e) {
      logException(e);
    }
    return resource;
  }

  /**
   * Hadoop-specific metrics fetching
   */
  private void getHadoopMetricValue(InputStream in, Set<String> ids,
                       Resource resource, Request request, Ticket ticket) throws IOException {
    JMXMetricHolder metricHolder = jmxObjectReader.readValue(in);

    Map<String, Map<String, Object>> categories = new HashMap<String, Map<String, Object>>();
    String componentName = (String) resource.getPropertyValue(componentNamePropertyId);

    String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);

    for (Map<String, Object> bean : metricHolder.getBeans()) {
      String category = getCategory(bean, clusterName, componentName);
      if (category != null) {
        categories.put(category, bean);
      }
    }

    for (String propertyId : ids) {
      Map<String, PropertyInfo> propertyInfoMap = getPropertyInfoMap(componentName, propertyId);

      String requestedPropertyId = propertyId;

      for (Map.Entry<String, PropertyInfo> entry : propertyInfoMap.entrySet()) {

        PropertyInfo propertyInfo = entry.getValue();
        propertyId = entry.getKey();

        if (propertyInfo.isPointInTime()) {

          String property = propertyInfo.getPropertyId();
          String category = "";

          List<String> keyList = new LinkedList<String>();

          int keyStartIndex = property.indexOf('[');
          if (-1 != keyStartIndex) {
            int keyEndIndex = property.indexOf(']', keyStartIndex);
            if (-1 != keyEndIndex && keyEndIndex > keyStartIndex) {
              keyList.add(property.substring(keyStartIndex+1, keyEndIndex));
            }
          }

          if (!containsArguments(propertyId)) {
            int dotIndex = property.indexOf('.', property.indexOf('='));
            if (-1 != dotIndex) {
              category = property.substring(0, dotIndex);
              property = (-1 == keyStartIndex) ?
                      property.substring(dotIndex+1) :
                      property.substring(dotIndex+1, keyStartIndex);
            }
          } else {
            int firstKeyIndex = keyStartIndex > -1 ? keyStartIndex : property.length();
            int dotIndex = property.lastIndexOf('.', firstKeyIndex);

            if (dotIndex != -1) {
              category = property.substring(0, dotIndex);
              property = property.substring(dotIndex + 1, firstKeyIndex);
            }
          }

          if (containsArguments(propertyId)) {
            Pattern pattern = Pattern.compile(category);

            // find all jmx categories that match the regex
            for (String jmxCat : categories.keySet()) {
              Matcher matcher = pattern.matcher(jmxCat);
              if (matcher.matches()) {
                String newPropertyId = propertyId;
                for (int i = 0; i < matcher.groupCount(); i++) {
                  newPropertyId = substituteArgument(newPropertyId, "$" + (i + 1), matcher.group(i + 1));
                }
                // We need to do the final filtering here, after the argument substitution
                if (isRequestedPropertyId(newPropertyId, requestedPropertyId, request)) {
                  if (!ticket.isValid()) {
                    return;
                  }
                  setResourceValue(resource, categories, newPropertyId, jmxCat, property, keyList);
                }
              }
            }
          } else {
            if (!ticket.isValid()) {
              return;
            }
            setResourceValue(resource, categories, propertyId, category, property, keyList);
          }
        }
      }
    }
  }

  private void setResourceValue(Resource resource, Map<String, Map<String, Object>> categories, String propertyId,
                                String category, String property, List<String> keyList) {
    Map<String, Object> properties = categories.get(category);
    if (property.contains(DOT_REPLACEMENT_CHAR)) {
      property = dotReplacementCharPattern.matcher(property).replaceAll(".");
    }
    if (properties != null && properties.containsKey(property)) {
      Object value = properties.get(property);
      if (keyList.size() > 0 && value instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) value;
        for (String key : keyList) {
          value = map.get(key);
          if (value instanceof Map) {
            map = (Map<?, ?>) value;
          }
          else {
            break;
          }
        }
      }
      resource.setProperty(propertyId, value);
    }
  }

  private String getPort(String clusterName, String componentName, String hostName, boolean httpsEnabled) throws SystemException {
    String port = jmxHostProvider.getPort(clusterName, componentName, hostName, httpsEnabled);
    return port == null ? DEFAULT_JMX_PORTS.get(componentName) : port;
  }

  private String getJMXProtocol(String clusterName, String componentName) {
    return jmxHostProvider.getJMXProtocol(clusterName, componentName);
  }
  
  private Set<String> getHosts(Resource resource, String clusterName, String componentName) {
    return hostNamePropertyId == null ?
            jmxHostProvider.getHostNames(clusterName, componentName) :
            Collections.singleton((String) resource.getPropertyValue(hostNamePropertyId));
  }

  private String getCategory(Map<String, Object> bean, String clusterName, String componentName) {
    if (bean.containsKey(NAME_KEY)) {
      String name = (String) bean.get(NAME_KEY);

      if (bean.containsKey(PORT_KEY)) {
        String port = (String) bean.get(PORT_KEY);
        String tag = jmxHostProvider.getJMXRpcMetricTag(clusterName, componentName, port);
        name = name.replace("ForPort" + port, tag == null ? "" : ",tag=" + tag);
      }
      return name;
    }
    return null;
  }
}
