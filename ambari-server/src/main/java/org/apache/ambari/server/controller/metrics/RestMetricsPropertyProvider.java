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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.internal.PropertyInfo;
import org.apache.ambari.server.controller.internal.StackDefinedPropertyProvider;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * WARNING: Class should be thread-safe!
 * <p/>
 * Resolves metrics like api/cluster/summary/nimbus.uptime
 * For every metric, finds a relevant JSON value and returns is as
 * a resource property.
 */
public class RestMetricsPropertyProvider extends ThreadPoolEnabledPropertyProvider {

  protected final static Logger LOG =
      LoggerFactory.getLogger(RestMetricsPropertyProvider.class);

  private static Map<String, RestMetricsPropertyProvider> instances =
      new Hashtable<String, RestMetricsPropertyProvider>();

  @Inject
  private AmbariManagementController amc;

  @Inject
  private Clusters clusters;

  private final Map<String, String> metricsProperties;
  private final StreamProvider streamProvider;
  private final String clusterNamePropertyId;
  private final String componentNamePropertyId;
  private final String statePropertyId;
  private MetricHostProvider metricHostProvider;
  private final String componentName;

  private static final String DEFAULT_PORT_PROPERTY = "default_port";
  private static final String PORT_CONFIG_TYPE_PROPERTY = "port_config_type";
  private static final String PORT_PROPERTY_NAME_PROPERTY = "port_property_name";

  /**
   * Protocol to use when connecting
   */
  private static final String PROTOCOL_OVERRIDE_PROPERTY = "protocol";
  private static final String HTTP_PROTOCOL = "http";
  private static final String HTTPS_PROTOCOL = "https";
  private static final String DEFAULT_PROTOCOL = HTTP_PROTOCOL;


  /**
   * String that separates JSON URL from path inside JSON in metrics path
   */
  public static final String URL_PATH_SEPARATOR = "##";

  /**
   * Symbol that separates names of nested JSON sections in metrics path
   */
  public static final String DOCUMENT_PATH_SEPARATOR = "#";


  /**
   * Create a REST property provider.
   *
   * @param metricsProperties       the map of per-component metrics properties
   * @param componentMetrics        the map of supported metrics for component
   * @param streamProvider          the stream provider
   * @param metricHostProvider     metricsHostProvider instance
   * @param clusterNamePropertyId   the cluster name property id
   * @param hostNamePropertyId      the host name property id
   * @param componentNamePropertyId the component name property id
   * @param statePropertyId         the state property id
   */
  public RestMetricsPropertyProvider(
    Injector injector,
    Map<String, String> metricsProperties,
    Map<String, Map<String, PropertyInfo>> componentMetrics,
    StreamProvider streamProvider,
    MetricHostProvider metricHostProvider,
    String clusterNamePropertyId,
    String hostNamePropertyId,
    String componentNamePropertyId,
    String statePropertyId,
    String componentName){

    super(componentMetrics, hostNamePropertyId, metricHostProvider);
    this.metricsProperties = metricsProperties;
    this.streamProvider = streamProvider;
    this.clusterNamePropertyId = clusterNamePropertyId;
    this.componentNamePropertyId = componentNamePropertyId;
    this.statePropertyId = statePropertyId;
    this.metricHostProvider = metricHostProvider;
    injector.injectMembers(this);
    this.componentName = componentName;
  }

  // ----- MetricsProvider implementation ------------------------------------


  /**
   * Populate a resource by obtaining the requested REST properties.
   *
   * @param resource  the resource to be populated
   * @param request   the request
   * @param predicate the predicate
   * @return the populated resource; null if the resource should NOT be
   *         part of the result set for the given predicate
   */
  @Override
  protected Resource populateResource(Resource resource,
                                      Request request, Predicate predicate, Ticket ticket)
      throws SystemException {

    // Remove request properties that request temporal information
    Set<String> ids = getRequestPropertyIds(request, predicate);
    Set<String> temporalIds = new HashSet<String>();
    String resourceComponentName = (String) resource.getPropertyValue(componentNamePropertyId);

    if (!componentName.equals(resourceComponentName)) {
      return resource;
    }

    for (String id : ids) {
      if (request.getTemporalInfo(id) != null) {
        temporalIds.add(id);
      }
    }
    ids.removeAll(temporalIds);

    if (ids.isEmpty()) {
      // no properties requested
      return resource;
    }

    // Don't attempt to get REST properties if the resource is in
    // an unhealthy state
    if (statePropertyId != null) {
      String state = (String) resource.getPropertyValue(statePropertyId);
      if (state != null && !healthyStates.contains(state)) {
        return resource;
      }
    }

    Map<String, PropertyInfo> propertyInfos =
        getComponentMetrics().get(StackDefinedPropertyProvider.WRAPPED_METRICS_KEY);
    if (propertyInfos == null) {
      // If there are no metrics defined for the given component then there is nothing to do.
      return resource;
    }
    String protocol = resolveProtocol();
    String port = "-1";
    String hostname = null;
    try {
      String clusterName = (String) resource.getPropertyValue(clusterNamePropertyId);
      Cluster cluster = clusters.getCluster(clusterName);
      hostname = getHost(resource, clusterName, resourceComponentName);
      if (hostname == null) {
        String msg = String.format("Unable to get component REST metrics. " +
            "No host name for %s.", resourceComponentName);
        LOG.warn(msg);
        return resource;
      }
      port = resolvePort(cluster, hostname, resourceComponentName, metricsProperties);
    } catch (Exception e) {
      rethrowSystemException(e);
    }

    Set<String> resultIds = new HashSet<String>();
    for (String id : ids){
      for (String metricId : propertyInfos.keySet()){
        if (metricId.startsWith(id)){
          resultIds.add(metricId);
        }
      }
    }

    // Extract set of URLs for metrics
    HashMap<String, Set<String>> urls = extractPropertyURLs(resultIds, propertyInfos);

    for (String url : urls.keySet()) {
      try {
        InputStream in = streamProvider.readFrom(getSpec(protocol, hostname, port, url));
        if (!ticket.isValid()) {
          if (in != null) {
            in.close();
          }
          return resource;
        }       
        try {
          extractValuesFromJSON(in, urls.get(url), resource, propertyInfos);
        } finally {
            in.close();
          }
      } catch (IOException e) {
        logException(e);
      }
    }
    return resource;
  }

  @Override
  public Set<String> checkPropertyIds(Set<String> propertyIds) {
    Set<String> unsupported = new HashSet<String>();
    for (String propertyId : propertyIds) {
      if (!getComponentMetrics().
          get(StackDefinedPropertyProvider.WRAPPED_METRICS_KEY).
          containsKey(propertyId)) {
        unsupported.add(propertyId);
      }
    }
    return unsupported;
  }

  // ----- helper methods ----------------------------------------------------

  /**
   * Uses port_config_type, port_property_name, default_port parameters from
   * metricsProperties to find out right port value for service
   *
   * @return determines REST port for service
   */
  private String resolvePort(Cluster cluster, String hostname, String componentName,
                          Map<String, String> metricsProperties)
      throws AmbariException {
    String portConfigType = null;
    String portPropertyName = null;
    if (metricsProperties.containsKey(PORT_CONFIG_TYPE_PROPERTY) &&
        metricsProperties.containsKey(PORT_PROPERTY_NAME_PROPERTY)) {
      portConfigType = metricsProperties.get(PORT_CONFIG_TYPE_PROPERTY);
      portPropertyName = metricsProperties.get(PORT_PROPERTY_NAME_PROPERTY);
    }
    String portStr = null;
    if (portConfigType != null && portPropertyName != null) {
      try {
        Map<String, Map<String, String>> configTags =
            amc.findConfigurationTagsWithOverrides(cluster, hostname);
        if (configTags.containsKey(portConfigType)) {
          Map<String, String> config = configTags.get(portConfigType);
          if (config.containsKey(portPropertyName)) {
            portStr = config.get(portPropertyName);
          }
        }
      } catch (AmbariException e) {
        String message = String.format("Can not extract config tags for " +
            "cluster = %s, hostname = %s", componentName, hostname);
        LOG.warn(message);
      }
      if (portStr == null) {
        String message = String.format(
            "Can not extract REST port for " +
                "component %s from configurations. " +
                "Config tag = %s, config key name = %s, " +
                "hostname = %s. Probably metrics.json file for " +
                "service is misspelled. Trying default port",
            componentName, portConfigType,
            portPropertyName, hostname);
        LOG.debug(message);
      }
    }
    if (portStr == null && metricsProperties.containsKey(DEFAULT_PORT_PROPERTY)) {
      if (metricsProperties.containsKey(DEFAULT_PORT_PROPERTY)) {
        portStr = metricsProperties.get(DEFAULT_PORT_PROPERTY);
      } else {
        String message = String.format("Can not determine REST port for " +
            "component %s. " +
            "Default REST port property %s is not defined at metrics.json " +
            "file for service, and there is no any other available ways " +
            "to determine port information.",
            componentName, DEFAULT_PORT_PROPERTY);
        throw new AmbariException(message);
      }
    }
      return portStr;
  }


  /**
   * Extracts protocol type from metrics properties. If no protocol is defined,
   * uses default protocol.
   */
  private String resolveProtocol() {
    String protocol = DEFAULT_PROTOCOL;
    if (metricsProperties.containsKey(PROTOCOL_OVERRIDE_PROPERTY)) {
      protocol = metricsProperties.get(PROTOCOL_OVERRIDE_PROPERTY).toLowerCase();
      if (!protocol.equals(HTTP_PROTOCOL) && !protocol.equals(HTTPS_PROTOCOL)) {
        String message = String.format(
            "Unsupported protocol type %s, falling back to %s",
            protocol, DEFAULT_PROTOCOL);
        LOG.warn(message);
        protocol = DEFAULT_PROTOCOL;
      }
    } else {
      protocol = DEFAULT_PROTOCOL;
    }
    return protocol;
  }


  /**
   * Extracts JSON URL from metricsPath
   */
  private String extractMetricsURL(String metricsPath)
      throws IllegalArgumentException {
    return validateAndExtractPathParts(metricsPath)[0];
  }

  /**
   * Extracts part of metrics path that contains path through nested
   * JSON sections
   */
  private String extractDocumentPath(String metricsPath)
      throws IllegalArgumentException {
    return validateAndExtractPathParts(metricsPath)[1];
  }

  /**
   * Returns [MetricsURL, DocumentPath] or throws an exception
   * if metricsPath is invalid.
   */
  private String[] validateAndExtractPathParts(String metricsPath)
      throws IllegalArgumentException {
    String[] pathParts = metricsPath.split(URL_PATH_SEPARATOR);
    if (pathParts.length == 2) {
      return pathParts;
    } else {
      // This warning is expected to occur only on development phase
      String message = String.format(
          "Metrics path %s does not contain or contains" +
              "more than one %s sequence. That probably " +
              "means that the mentioned metrics path is misspelled. " +
              "Please check the relevant metrics.json file",
          metricsPath, URL_PATH_SEPARATOR);
      throw new IllegalArgumentException(message);
    }
  }


  /**
   * Returns a map <document_url, requested_property_ids>.
   * requested_property_ids contain a set of property IDs
   * that should be fetched for this URL. Doing
   * that allows us to extract document only once when getting few properties
   * from this document.
   *
   * @param ids set of property IDs that should be fetched
   */
  private HashMap<String, Set<String>> extractPropertyURLs(Set<String> ids,
                                                           Map<String, PropertyInfo> propertyInfos) {
    HashMap<String, Set<String>> result = new HashMap<String, Set<String>>();
    for (String requestedPropertyId : ids) {
      PropertyInfo propertyInfo = propertyInfos.get(requestedPropertyId);

      String metricsPath = propertyInfo.getPropertyId();
      String url = extractMetricsURL(metricsPath);
      Set<String> set;
      if (!result.containsKey(url)) {
        set = new HashSet<String>();
        result.put(url, set);
      } else {
        set = result.get(url);
      }
      set.add(requestedPropertyId);
    }
    return result;
  }


  /**
   * Extracts requested properties from a given JSON input stream into
   * resource.
   *
   * @param jsonStream           input stream that contains JSON
   * @param requestedPropertyIds a set of property IDs
   *                             that should be fetched for this URL
   * @param resource             all extracted values are placed into resource
   */
  private void extractValuesFromJSON(InputStream jsonStream,
                                     Set<String> requestedPropertyIds,
                                     Resource resource,
                                     Map<String, PropertyInfo> propertyInfos)
      throws IOException {
    Gson gson = new Gson();
    Type type = new TypeToken<Map<Object, Object>>() {
    }.getType();
    JsonReader jsonReader = new JsonReader(
        new BufferedReader(new InputStreamReader(jsonStream)));
    Map<String, String> jsonMap = gson.fromJson(jsonReader, type);
    for (String requestedPropertyId : requestedPropertyIds) {
      PropertyInfo propertyInfo = propertyInfos.get(requestedPropertyId);
      String metricsPath = propertyInfo.getPropertyId();
      String documentPath = extractDocumentPath(metricsPath);
      String[] docPath = documentPath.split(DOCUMENT_PATH_SEPARATOR);
      Map<String, String> subMap = jsonMap;
      for (int i = 0; i < docPath.length; i++) {
        String pathElement = docPath[i];
        if (!subMap.containsKey(pathElement)) {
          String message = String.format(
              "Can not fetch %dth element of document path (%s) " +
                  "from json. Wrong metrics path: %s",
              i, pathElement, metricsPath);
          throw new IOException(message);
        }
        Object jsonSubElement = jsonMap.get(pathElement);
        if (i == docPath.length - 1) { // Reached target document section
          // Extract property value
          resource.setProperty(requestedPropertyId, jsonSubElement);
        } else { // Navigate to relevant document section
          subMap = gson.fromJson((JsonElement) jsonSubElement, type);
        }
      }
    }
  }

}
