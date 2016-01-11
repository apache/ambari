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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonSyntaxException;
import com.google.inject.Injector;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Property provider that is used to read HTTP data from another server.
 */
public class HttpProxyPropertyProvider extends BaseProvider implements PropertyProvider {

  protected final static Logger LOG =
      LoggerFactory.getLogger(HttpProxyPropertyProvider.class);

  private static final Map<String, String> URL_TEMPLATES = new HashMap<String, String>();
  private static final Map<String, String> MAPPINGS = new HashMap<String, String>();
  private static final Map<String, String> PROPERTIES_TO_FILTER = new HashMap<String, String>();

  private static final String COMPONENT_RESOURCEMANAGER = "RESOURCEMANAGER";
  private static final String CONFIG_YARN_SITE = "yarn-site";
  private static final String CONFIG_CORE_SITE = "core-site";
  private static final String PROPERTY_YARN_HTTP_POLICY = "yarn.http.policy";
  private static final String PROPERTY_HADOOP_SSL_ENABLED = "hadoop.ssl.enabled";
  private static final String PROPERTY_YARN_HTTP_POLICY_VALUE_HTTPS_ONLY = "HTTPS_ONLY";
  private static final String PROPERTY_HADOOP_SSL_ENABLED_VALUE_TRUE = "true";


  private static final String PROPERTY_RESOURCEMANAGER_WEBAPP_ADDRESS = "yarn.resourcemanager.webapp.address";
  private static final String PROPERTY_RESOURCEMANAGER_WEBAPP_HTTPS_ADDRESS = "yarn.resourcemanager.webapp.https.address";
  // resource manager HA properties

  private static final String PROPERTY_RESOURCEMANAGER_HA_RM_IDS = "yarn.resourcemanager.ha.rm-ids";
  private static final String PROPERTY_RESOURCEMANAGER_HOSTNAME_TEMPLATE = "yarn.resourcemanager.hostname.%s";
  private static final String PROPERTY_RESOURCEMANAGER_WEBAPP_ADDRESS_TEMPLATE = "yarn.resourcemanager.webapp.address.%s";
  private static final String PROPERTY_RESOURCEMANAGER_WEBAPP_HTTPS_ADDRESS_TEMPLATE = "yarn.resourcemanager.webapp.https.address.%s";

  static {
    URL_TEMPLATES.put(COMPONENT_RESOURCEMANAGER, "http://%s:%s/ws/v1/cluster/info");
    
    MAPPINGS.put(COMPONENT_RESOURCEMANAGER, PropertyHelper.getPropertyId("HostRoles", "ha_state"));

    PROPERTIES_TO_FILTER.put(COMPONENT_RESOURCEMANAGER, "clusterInfo/haState");
  }

  private final ComponentSSLConfiguration configuration;

  private StreamProvider streamProvider = null;
  // !!! not yet used, but make consistent
  private String clusterNamePropertyId = null;
  private String hostNamePropertyId = null;
  private String componentNamePropertyId = null;

  private Injector injector;
  private Clusters clusters;
  
  public HttpProxyPropertyProvider(
      StreamProvider stream,
      ComponentSSLConfiguration configuration,
      Injector inject,
      String clusterNamePropertyId,
      String hostNamePropertyId,
      String componentNamePropertyId) {

    super(new HashSet<String>(MAPPINGS.values()));
    this.streamProvider = stream;
    this.configuration = configuration;
    this.clusterNamePropertyId = clusterNamePropertyId;
    this.hostNamePropertyId = hostNamePropertyId;
    this.componentNamePropertyId = componentNamePropertyId;
    this.injector = inject;
    this.clusters = injector.getInstance(Clusters.class);
  }

  /**
   * This method only checks if an HTTP-type property should be fulfilled.  No
   * modification is performed on the resources.
   */
  @Override
  public Set<Resource> populateResources(Set<Resource> resources,
      Request request, Predicate predicate) throws SystemException {
    
    Set<String> ids = getRequestPropertyIds(request, predicate);
    
    if (0 == ids.size())
      return resources;

    for (Resource resource : resources) {
      
      Object hostName = resource.getPropertyValue(hostNamePropertyId);
      Object componentName = resource.getPropertyValue(componentNamePropertyId);
      Object clusterName = resource.getPropertyValue(clusterNamePropertyId);

      if (null != hostName && null != componentName &&
          MAPPINGS.containsKey(componentName.toString()) &&
          URL_TEMPLATES.containsKey(componentName.toString())) {
        
        String template = getTemplate(componentName.toString(), clusterName.toString(), hostName.toString());
        String propertyId = MAPPINGS.get(componentName.toString());
        String url = String.format(template, hostName);
        
        getHttpResponse(resource, url, propertyId);
      }
    }
    
    return resources;
  }

  private String getTemplate(String componentName, String clusterName, String hostName) throws SystemException {
    String template = URL_TEMPLATES.get(componentName);

    if (componentName.equals(COMPONENT_RESOURCEMANAGER)) {
      try {
        Cluster cluster = this.clusters.getCluster(clusterName);
        Map<String, String> yarnConfigProperties = cluster.getDesiredConfigByType(CONFIG_YARN_SITE).getProperties();
        Map<String, String> coreConfigProperties = cluster.getDesiredConfigByType(CONFIG_CORE_SITE).getProperties();
        String yarnHttpPolicy = yarnConfigProperties.get(PROPERTY_YARN_HTTP_POLICY);
        String hadoopSslEnabled = coreConfigProperties.get(PROPERTY_HADOOP_SSL_ENABLED);
        boolean useHttps = (yarnHttpPolicy != null && yarnHttpPolicy.equals(PROPERTY_YARN_HTTP_POLICY_VALUE_HTTPS_ONLY)) ||
            hadoopSslEnabled != null && hadoopSslEnabled.equals(PROPERTY_HADOOP_SSL_ENABLED_VALUE_TRUE);
        if (useHttps) {
          template = template.replace("http", "https");
        }
        // determine correct port, we don't want to use hardcoded one
        String port;
        if(!yarnConfigProperties.containsKey(PROPERTY_RESOURCEMANAGER_HA_RM_IDS)) {
          //non ha mode
          if(useHttps) {
            port = getPortFromProperty(yarnConfigProperties, PROPERTY_RESOURCEMANAGER_WEBAPP_HTTPS_ADDRESS, "8090");
          } else {
            port = getPortFromProperty(yarnConfigProperties, PROPERTY_RESOURCEMANAGER_WEBAPP_ADDRESS, "8088");
          }
        } else {
          // ha mode
          String rmId = null;
          for(String id : yarnConfigProperties.get(PROPERTY_RESOURCEMANAGER_HA_RM_IDS).split(",")) {
            String hostNameProperty = String.format(PROPERTY_RESOURCEMANAGER_HOSTNAME_TEMPLATE, id);
            String hostNameById =  yarnConfigProperties.get(hostNameProperty);
            if(hostNameById.equals(hostName)){
              rmId = id;
              break;
            }
          }
          if(useHttps) {
            String httpsAddressPoperty = String.format(PROPERTY_RESOURCEMANAGER_WEBAPP_HTTPS_ADDRESS_TEMPLATE, rmId);
            port = getPortFromProperty(yarnConfigProperties, httpsAddressPoperty, "8090");
          } else {
            String httpAddressPoperty = String.format(PROPERTY_RESOURCEMANAGER_WEBAPP_ADDRESS_TEMPLATE, rmId);
            port = getPortFromProperty(yarnConfigProperties, httpAddressPoperty, "8088");
          }

        }
        template = String.format(template, "%s", port);
      } catch (AmbariException e) {
          LOG.debug(String.format("Could not load cluster with name %s. %s", clusterName, e.getMessage()));
          throw new SystemException(String.format("Could not load cluster with name %s.", clusterName),e);
      }
    }
    return template;
  }

  private String getPortFromProperty(Map<String, String> propertyMap, String property, String defaultValue) {
    if(propertyMap.containsKey(property))
      return propertyMap.get(property).split(":")[1];
    else
      return defaultValue;
  }

  private Object getPropertyValueToSet(Map<String, Object> propertyValueFromJson, Object componentName) throws SystemException {
    Object result = propertyValueFromJson;
    //TODO need refactoring for universalization
    try {
      if (PROPERTIES_TO_FILTER.get(componentName) != null) {
        for (String key : PROPERTIES_TO_FILTER.get(componentName).split("/")) {
          result = ((Map)result).get(key);
        }
      }
    } catch (ClassCastException e) {
        LOG.error(String.format("Error getting property value for %s. %s", PROPERTIES_TO_FILTER.get(componentName),
              e.getMessage()));
        throw new SystemException(String.format("Error getting property value for %s.",
                PROPERTIES_TO_FILTER.get(componentName)),e);
    }
    return result;
  }

  private void getHttpResponse(Resource r, String url, String propertyIdToSet) throws SystemException {
    InputStream in = null;
    try {
      in = streamProvider.readFrom(url);
      Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
      Map<String, Object> propertyValueFromJson = new Gson().fromJson(IOUtils.toString(in, "UTF-8"), mapType);
      Object propertyValueToSet = getPropertyValueToSet(propertyValueFromJson,
              r.getPropertyValue(componentNamePropertyId));
      r.setProperty(propertyIdToSet, propertyValueToSet);
    }
    catch (IOException ioe) {
      LOG.debug("Error reading HTTP response from " + url);
      r.setProperty(propertyIdToSet, null);
    } catch (JsonSyntaxException jse) {
      LOG.error("Error parsing HTTP response from " + url);
      r.setProperty(propertyIdToSet, null);
    } finally {
      if (in != null) {
        try {
          in.close();
        }
        catch (IOException ioe) {
          LOG.error("Error closing HTTP response stream " + url);
        }
      }
    }
    
  }

}
