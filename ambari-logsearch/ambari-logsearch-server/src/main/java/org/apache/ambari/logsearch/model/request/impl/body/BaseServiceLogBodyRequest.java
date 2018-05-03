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
package org.apache.ambari.logsearch.model.request.impl.body;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.ambari.logsearch.common.LogSearchConstants;
import org.apache.ambari.logsearch.model.request.impl.BaseServiceLogRequest;

public class BaseServiceLogBodyRequest extends BaseLogBodyRequest implements BaseServiceLogRequest {
  @JsonProperty(LogSearchConstants.REQUEST_PARAM_LEVEL)
  private String level;

  @JsonProperty(LogSearchConstants.REQUEST_PARAM_HOST_NAME)
  private String hostName;

  @JsonProperty(LogSearchConstants.REQUEST_PARAM_COMPONENT_NAME)
  private String componentName;

  @JsonProperty(LogSearchConstants.REQUEST_PARAM_FILE_NAME)
  private String fileName;

  @JsonProperty(LogSearchConstants.REQUEST_PARAM_BUNDLE_ID)
  private String bundleId;

  @JsonProperty(LogSearchConstants.REQUEST_PARAM_HOSTS)
  private String hostList;

  @Override
  public String getLevel() {
    return level;
  }

  @Override
  public void setLevel(String level) {
    this.level = level;
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
  public String getHostList() {
    return hostList;
  }

  @Override
  public void setHostList(String hostList) {
    this.hostList = hostList;
  }
}
