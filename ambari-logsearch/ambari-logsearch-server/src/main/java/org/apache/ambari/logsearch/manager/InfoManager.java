/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ambari.logsearch.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.ambari.logsearch.common.PropertyDescriptionStorage;
import org.apache.ambari.logsearch.common.ShipperConfigDescriptionStorage;
import org.apache.ambari.logsearch.conf.LogSearchConfigApiConfig;
import org.apache.ambari.logsearch.model.response.PropertyDescriptionData;
import org.apache.ambari.logsearch.model.response.ShipperConfigDescriptionData;
import org.springframework.beans.factory.annotation.Value;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InfoManager extends JsonManagerBase {

  @Value("${logsearch.app.version:}")
  private String logsearchAppVersion;

  @Value("${logsearch.solr.version:}")
  private String logsearchSolrVersion;

  @Value("${java.runtime.version}")
  private String javaRuntimeVersion;

  @Inject
  private AuthPropsConfig authPropsConfig;

  @Inject
  private LogSearchConfigApiConfig logSearchConfigApiConfig;

  @Inject
  private PropertyDescriptionStorage propertyDescriptionStore;

  @Inject
  private ShipperConfigDescriptionStorage shipperConfigDescriptionStore;

  public Map<String, String> getApplicationInfo() {
    Map<String, String> appMap = new HashMap<>();
    appMap.put("application.version", logsearchAppVersion);
    appMap.put("solr.version", logsearchSolrVersion);
    appMap.put("java.runtime.version", javaRuntimeVersion);
    return appMap;
  }

  public Map<String, Boolean> getAuthMap() {
    Map<String, Boolean> authMap = new HashMap<>();
    authMap.put("external", authPropsConfig.isAuthExternalEnabled());
    authMap.put("file", authPropsConfig.isAuthFileEnabled());
    authMap.put("jwt", authPropsConfig.isAuthJwtEnabled());
    authMap.put("ldap", authPropsConfig.isAuthLdapEnabled());
    authMap.put("simple", authPropsConfig.isAuthSimpleEnabled());
    return authMap;
  }

  public Map<String, Object> getFeaturesMap() {
    Map<String, Object> featuresMap = new HashMap<>();
    featuresMap.put(LogSearchConstants.AUTH_FEATURE_KEY, getAuthMap());
    featuresMap.put(LogSearchConstants.SHIPPER_CONFIG_API_KEY, logSearchConfigApiConfig.isConfigApiEnabled());
    return featuresMap;
  }

  public Map<String, List<PropertyDescriptionData>> getPropertyDescriptions() {
    return propertyDescriptionStore.getPropertyDescriptions();
  }

  public List<PropertyDescriptionData> getLogSearchPropertyDescriptions(String propertiesFile) {
    return getPropertyDescriptions().get(propertiesFile);
  }
  
  public List<ShipperConfigDescriptionData> getLogSearchShipperConfigDescription() {
    return shipperConfigDescriptionStore.getShipperConfigDescription();
  }
}
