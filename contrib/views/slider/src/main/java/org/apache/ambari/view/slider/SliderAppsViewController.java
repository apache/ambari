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

package org.apache.ambari.view.slider;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.yarn.exceptions.YarnException;

import com.google.gson.JsonObject;
import com.google.inject.ImplementedBy;

@ImplementedBy(SliderAppsViewControllerImpl.class)
public interface SliderAppsViewController {

  public static final String PARAM_AMBARI_CLUSTER_API = "ambari.server.url";
  public static final String PARAM_AMBARI_USERNAME = "ambari.server.username";
  public static final String PARAM_AMBARI_PASSWORD = "ambari.server.password";
  public static final String PARAM_SLIDER_USER = "slider.user";
  public static final String PARAM_VIEW_PRINCIPAL = "view.kerberos.principal";
  public static final String PARAM_VIEW_PRINCIPAL_KEYTAB= "view.kerberos.principal.keytab";

  public static final String PROPERTY_SLIDER_ZK_QUORUM = "slider.zookeeper.quorum";
  public static final String PROPERTY_METRICS_SERVER_HOSTNAME = "site.global.metric_collector_host";
  public static final String PROPERTY_METRICS_SERVER_PORT = "site.global.metric_collector_port";
  public static final String PROPERTY_METRICS_LIBRARY_PATH = "site.global.metric_collector_lib";
  public static final String PROPERTY_YARN_RM_WEBAPP_URL = "yarn.rm.webapp.url";
  public static final String PROPERTY_SLIDER_USER = "view.slider.user";
  public static final String PROPERTY_JAVA_HOME = "java.home";
  public static final String PROPERTY_SLIDER_SECURITY_ENABLED = "slider.security.enabled";

  public static final String METRICS_API_NAME = "Metrics API";

  public ViewStatus getViewStatus();

  /**
   * Provides information about requested Slider App.
   * 
   * @param applicationId
   * @param properties
   *          Identifies specific properties to show up. Provide
   *          <code>null</code> for default properties.
   * @return
   * @throws YarnException
   * @throws IOException
   * @throws InterruptedException 
   */
  public SliderApp getSliderApp(String applicationId, Set<String> properties)
      throws YarnException, IOException, InterruptedException;

  /**
   * Provides list of Slider apps with requested properties populated.
   * 
   * @param properties
   *          Identifies specific properties to show up. Provide
   *          <code>null</code> for default properties.
   * @return
   * @throws YarnException
   * @throws IOException
   * @throws InterruptedException 
   */
  public List<SliderApp> getSliderApps(Set<String> properties)
      throws YarnException, IOException, InterruptedException;

  /**
   * Attempts to delete a Slider app. An unsuccessful attempt will result in
   * exception.
   * 
   * @param applicationId
   * @throws YarnException
   * @throws IOException
   * @throws InterruptedException 
   */
  public void deleteSliderApp(String applicationId) throws YarnException,
      IOException, InterruptedException;

  public SliderAppType getSliderAppType(String appTypeId, Set<String> properties);

  public List<SliderAppType> getSliderAppTypes(Set<String> properties);

  public String createSliderApp(JsonObject requestJson) throws IOException,
      YarnException, InterruptedException;

  public void freezeApp(String appId) throws YarnException, IOException,
      InterruptedException;

  public void thawApp(String appId) throws YarnException, IOException,
      InterruptedException;

  public void flexApp(String appId, Map<String, Integer> componentsMap)
      throws YarnException, IOException, InterruptedException;

  public boolean appExists(String string) throws IOException, InterruptedException, YarnException;
}
