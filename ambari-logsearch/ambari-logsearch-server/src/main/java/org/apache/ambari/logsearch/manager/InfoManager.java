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

import org.apache.ambari.logsearch.conf.AuthPropsConfig;
import org.apache.ambari.logsearch.common.PropertyDescriptionStorage;
import org.apache.ambari.logsearch.model.response.PropertyDescriptionData;

import javax.inject.Inject;
import javax.inject.Named;

@Named
public class InfoManager extends JsonManagerBase {

  @Inject
  private AuthPropsConfig authPropsConfig;

  @Inject
  private PropertyDescriptionStorage propertyDescriptionStore;

  public Map<String, Boolean> getAuthMap() {
    Map<String, Boolean> authMap = new HashMap<>();
    authMap.put("external", authPropsConfig.isAuthExternalEnabled());
    authMap.put("file", authPropsConfig.isAuthFileEnabled());
    authMap.put("jwt", authPropsConfig.isAuthJwtEnabled());
    authMap.put("ldap", authPropsConfig.isAuthLdapEnabled());
    authMap.put("simple", authPropsConfig.isAuthSimpleEnabled());
    return authMap;
  }

  public Map<String, List<PropertyDescriptionData>> getPropertyDescriptions() {
    return propertyDescriptionStore.getPropertyDescriptions();
  }

  public List<PropertyDescriptionData> getLogSearchPropertyDescriptions(String propertiesFile) {
    return getPropertyDescriptions().get(propertiesFile);
  }
}
