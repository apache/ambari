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

import org.apache.ambari.logsearch.common.Marker;

import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_LOG_FILE_COMPONENT;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_LOG_FILE_HOST;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_LOG_FILE_TYPE;

public class LogFileSearchCriteria extends SearchCriteria {

  public String getLogFileComponent() {
    return getParam(PARAM_LOG_FILE_COMPONENT, String.class);
  }

  public void setLogFileComponent(String logFileComponent) {
    addParam(PARAM_LOG_FILE_COMPONENT, logFileComponent);
  }

  public String getLogFileHost() {
    return getParam(PARAM_LOG_FILE_HOST, String.class);
  }

  public void setLogFileHost(String logFileHost) {
    addParam(PARAM_LOG_FILE_HOST, logFileHost);
  }

  public String getLogType() {
    return getParam(PARAM_LOG_FILE_TYPE, String.class);
  }

  public void setLogType(String logType) {
    addParam(PARAM_LOG_FILE_TYPE, logType);
  }
}
