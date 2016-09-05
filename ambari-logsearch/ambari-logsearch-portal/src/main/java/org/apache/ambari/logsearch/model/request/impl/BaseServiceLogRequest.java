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
package org.apache.ambari.logsearch.model.request.impl;

import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.BundleIdParamDefinition;
import org.apache.ambari.logsearch.model.request.DateRangeParamDefinition;
import org.apache.ambari.logsearch.model.request.ServiceLogParamDefinition;

import javax.ws.rs.QueryParam;

public class BaseServiceLogRequest extends BaseLogRequest
  implements ServiceLogParamDefinition, BundleIdParamDefinition, DateRangeParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_LEVEL)
  private String level;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_ADVANCED_SEARCH)
  private String advancedSearch;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_TREE_PARAMS)
  private String treeParams;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_E_MESSAGE)
  private String eMessage;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_G_MUST_NOT)
  private String gMustNot;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_HOST_NAME)
  private String hostName;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_COMPONENT_NAME)
  private String componentName;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_FILE_NAME)
  private String fileName;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_BUNDLE_ID)
  private String bundleId;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_FROM)
  private String from;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_TO)
  private String to;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_DATE_RANGE_LABEL)
  private String dateRangeLabel;

  @Override
  public String getLevel() {
    return level;
  }

  @Override
  public void setLevel(String level) {
    this.level = level;
  }

  @Override
  public String getAdvancedSearch() {
    return advancedSearch;
  }

  @Override
  public void setAdvancedSearch(String advancedSearch) {
    this.advancedSearch = advancedSearch;
  }

  @Override
  public String getTreeParams() {
    return treeParams;
  }

  @Override
  public void setTreeParams(String treeParams) {
    this.treeParams = treeParams;
  }

  @Override
  public String geteMessage() {
    return eMessage;
  }

  @Override
  public void seteMessage(String eMessage) {
    this.eMessage = eMessage;
  }

  @Override
  public String getgMustNot() {
    return gMustNot;
  }

  @Override
  public void setgMustNot(String gMustNot) {
    this.gMustNot = gMustNot;
  }

  @Override
  public String getHostName() {
    return hostName;
  }

  @Override
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @Override
  public String getComponentName() {
    return componentName;
  }

  @Override
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  @Override
  public String getFileName() {
    return fileName;
  }

  @Override
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  @Override
  public String getBundleId() {
    return bundleId;
  }

  @Override
  public void setBundleId(String bundleId) {
    this.bundleId = bundleId;
  }

  @Override
  public String getFrom() {
    return from;
  }

  @Override
  public void setFrom(String from) {
    this.from = from;
  }

  @Override
  public String getTo() {
    return to;
  }

  @Override
  public void setTo(String to) {
    this.to = to;
  }

  @Override
  public String getDateRangeLabel() {
    return dateRangeLabel;
  }

  @Override
  public void setDateRangeLabel(String dateRangeLabel) {
    this.dateRangeLabel = dateRangeLabel;
  }
}
