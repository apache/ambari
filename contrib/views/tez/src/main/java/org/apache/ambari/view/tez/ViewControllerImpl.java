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

package org.apache.ambari.view.tez;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implementation of controller to handle requests for the tez View.
 */
@Singleton
public class ViewControllerImpl implements ViewController {

  private static final Logger LOG = LoggerFactory.getLogger(ViewControllerImpl.class);

  private final static String TYPE_YARN_SITE = "yarn-site";
  private final String YARN_HTTP_POLICY_PROPERTY = "yarn.http.policy";
  private final static Map<String, String> HTTP_PROPERTY_MAP = new HashMap<String, String>();
  private final static Map<String, String> HTTPS_PROPERTY_MAP = new HashMap<String, String>();
  private final static String YARN_TIMELINE_WEBAPP_HTTP_ADDRESS_PROPERTY =
      "yarn.timeline-service.webapp.address";
  private final static String YARN_RM_WEBAPP_HTTP_ADDRESS_PROPERTY =
      "yarn.resourcemanager.webapp.address";
  private final static String YARN_TIMELINE_WEBAPP_HTTPS_ADDRESS_PROPERTY =
      "yarn.timeline-service.webapp.https.address";
  private final static String YARN_RM_WEBAPP_HTTPS_ADDRESS_PROPERTY =
      "yarn.resourcemanager.webapp.https.address";
  private final static String YARN_HTTPS_ONLY = "HTTPS_ONLY";

  static {
    HTTP_PROPERTY_MAP.put(ViewController.PARAM_YARN_ATS_URL,
        YARN_TIMELINE_WEBAPP_HTTP_ADDRESS_PROPERTY);
    HTTP_PROPERTY_MAP.put(ViewController.PARAM_YARN_RESOURCEMANAGER_URL,
        YARN_RM_WEBAPP_HTTP_ADDRESS_PROPERTY);
    HTTPS_PROPERTY_MAP.put(ViewController.PARAM_YARN_ATS_URL,
        YARN_TIMELINE_WEBAPP_HTTPS_ADDRESS_PROPERTY);
    HTTPS_PROPERTY_MAP.put(ViewController.PARAM_YARN_RESOURCEMANAGER_URL,
        YARN_RM_WEBAPP_HTTPS_ADDRESS_PROPERTY);
  }

  @Inject
  private ViewContext viewContext;

  /**
   * Because only an admin user is allowed to see the properties in
   * api/v1/views/tez/versions/{version_number}/instances/{instance_name}/ ,
   * we need a way for all users to see the basic properties of the tez View, primarily
   * the YARN ATS URL and YARN Resource Manager URL
   * @return Get the properties that any user is allowed to see, even non-admin users.
   */
  @Override
  public ViewStatus getViewStatus() {
    ViewStatus status = new ViewStatus();
    Map<String, String> parameters = new HashMap<String, String>();
    parameters.put(ViewController.PARAM_YARN_ATS_URL, getViewParameterValue(ViewController.PARAM_YARN_ATS_URL));
    parameters.put(ViewController.PARAM_YARN_RESOURCEMANAGER_URL, getViewParameterValue(ViewController.PARAM_YARN_RESOURCEMANAGER_URL));
    status.setParameters(parameters);
    return status;
  }

  /**
   * @param parameterName Parameter to get the value for
   * @return Returns the value of the given parameter
   */
  private String getViewParameterValue(String parameterName) {
    String value = null;
    Cluster cluster = viewContext.getCluster();
    if (cluster == null) {
      value = viewContext.getProperties().get(parameterName);
    } else {
      if (!parameterName.equals(ViewController.PARAM_YARN_ATS_URL)
          && !parameterName.equals(ViewController.PARAM_YARN_RESOURCEMANAGER_URL)) {
        throw new RuntimeException("Requested configured value for unknown parameter: "
            + parameterName);
      }

      String httpPolicy = cluster.getConfigurationValue(TYPE_YARN_SITE, YARN_HTTP_POLICY_PROPERTY);
      if (httpPolicy != null && httpPolicy.equalsIgnoreCase(YARN_HTTPS_ONLY)) {
        String addr = cluster.getConfigurationValue(TYPE_YARN_SITE,
            HTTPS_PROPERTY_MAP.get(parameterName));
        if (!addr.startsWith("https")) {
          value = "https://" + addr;
        }
      } else {
        String addr = cluster.getConfigurationValue(TYPE_YARN_SITE,
            HTTP_PROPERTY_MAP.get(parameterName));
        if (!addr.startsWith("http")) {
          value = "http://" + addr;
        }
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("ViewControllerImpl, paramName=" + parameterName
          + ", value=" + value);
    }
    if ("null".equals(value)) {
      return null;
    }
    return value;
  }
}
