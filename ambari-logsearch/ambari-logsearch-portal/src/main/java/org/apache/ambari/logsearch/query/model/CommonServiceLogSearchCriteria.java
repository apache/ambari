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
package org.apache.ambari.logsearch.query.model;

import org.apache.ambari.logsearch.query.SearchCriteriaConstants;

public class CommonServiceLogSearchCriteria extends CommonSearchCriteria {

  public String getLevel() {
    return getParam(SearchCriteriaConstants.PARAM_LEVEL, String.class);
  }

  public void setLevel(String level) {
    addParam(SearchCriteriaConstants.PARAM_LEVEL, level);
  }

  public String getSelectComp() {
    return getParam(SearchCriteriaConstants.PARAM_SELECT_COMP, String.class);
  }

  public void setSelectComp(String selectComp) {
    addParam(SearchCriteriaConstants.PARAM_SELECT_COMP, selectComp);
  }

  public String getBundleId() {
    return getParam(SearchCriteriaConstants.PARAM_BUNDLE_ID, String.class);
  }

  public void setBundleId(String bunldeId) {
    addParam(SearchCriteriaConstants.PARAM_BUNDLE_ID, bunldeId);
  }

  public String getFrom() {
    return getParam(SearchCriteriaConstants.PARAM_FROM ,String.class);
  }

  public void setFrom(String from) {
    addParam(SearchCriteriaConstants.PARAM_FROM, from);
  }

  public String getTo() {
    return getParam(SearchCriteriaConstants.PARAM_TO ,String.class);
  }

  public void setTo(String to) {
    addParam(SearchCriteriaConstants.PARAM_TO, to);
  }

  public String getHostName() {
    return getParam(SearchCriteriaConstants.PARAM_HOST_NAME ,String.class);
  }

  public void setHostName(String hostName) {
    addParam(SearchCriteriaConstants.PARAM_HOST_NAME, hostName);
  }

  public String getComponentName() {
    return getParam(SearchCriteriaConstants.PARAM_COMPONENT_NAME ,String.class);
  }

  public void setComponentName(String componentName) {
    addParam(SearchCriteriaConstants.PARAM_COMPONENT_NAME, componentName);
  }

  public String getFileName() {
    return getParam(SearchCriteriaConstants.PARAM_FILE_NAME ,String.class);
  }

  public void setFileName(String fileName) {
    addParam(SearchCriteriaConstants.PARAM_FILE_NAME, fileName);
  }
}
