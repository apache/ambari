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
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.controller.utilities.StreamProvider;
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

  static {
    URL_TEMPLATES.put("NAGIOS_SERVER", "http://%s/ambarinagios/nagios/nagios_alerts.php?q1=alerts&alert_type=all");
    
    MAPPINGS.put("NAGIOS_SERVER", PropertyHelper.getPropertyId("HostRoles", "nagios_alerts"));
  }

  private final ComponentSSLConfiguration configuration;

  private StreamProvider streamProvider = null;
  // !!! not yet used, but make consistent
  private String clusterNamePropertyId = null;
  private String hostNamePropertyId = null;
  private String componentNamePropertyId = null;
  
  public HttpProxyPropertyProvider(
      StreamProvider stream,
      ComponentSSLConfiguration configuration,
      String clusterNamePropertyId,
      String hostNamePropertyId,
      String componentNamePropertyId) {

    super(new HashSet<String>(MAPPINGS.values()));
    this.streamProvider = stream;
    this.configuration = configuration;
    this.clusterNamePropertyId = clusterNamePropertyId;
    this.hostNamePropertyId = hostNamePropertyId;
    this.componentNamePropertyId = componentNamePropertyId;
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
      
      if (null != hostName && null != componentName &&
          MAPPINGS.containsKey(componentName.toString()) &&
          URL_TEMPLATES.containsKey(componentName.toString())) {
        
        String template = getTemplate(componentName.toString());
        String propertyId = MAPPINGS.get(componentName.toString());
        String url = String.format(template, hostName);
        
        getHttpResponse(resource, url, propertyId);
      }
    }
    
    return resources;
  }

  private String getTemplate(String key) {

    String template = URL_TEMPLATES.get(key);

    if (key.equals("NAGIOS_SERVER")) {
      if (configuration.isNagiosSSL()) {
        template = template.replace("http", "https");
      }
    }
    return template;
  }

  private void getHttpResponse(Resource r, String url, String propertyIdToSet) throws SystemException {
    InputStream in = null;
    try {
      in = streamProvider.readFrom(url);
      Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
      Map<String, Object> propertyValueFromJson = new Gson().fromJson(IOUtils.toString(in, "UTF-8"), mapType);
      r.setProperty(propertyIdToSet, propertyValueFromJson);
    }
    catch (IOException ioe) {
      LOG.error("Error reading HTTP response from " + url);
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
