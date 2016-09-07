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

import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_END_TIME;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_EXCLUDE_MESSAGE;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_EXCLUDE_QUERY;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_INCLUDE_MESSAGE;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_INCLUDE_QUERY;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_MUST_BE_STRING;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_MUST_NOT_STRING;
import static org.apache.ambari.logsearch.query.SearchCriteriaConstants.PARAM_START_TIME;

public class CommonSearchCriteria extends SearchCriteria {

  public String getIncludeMessage() {
    return getParam(PARAM_INCLUDE_MESSAGE, String.class);
  }

  public void setIncludeMessage(String includeMessage) {
    addParam(PARAM_INCLUDE_MESSAGE, includeMessage);
  }

  public String getExcludeMessage() {
    return getParam(PARAM_EXCLUDE_MESSAGE, String.class);
  }

  public void setExcludeMessage(String excludeMessage) {
    addParam(PARAM_EXCLUDE_MESSAGE, excludeMessage);
  }

  public String getMustBe() {
    return getParam(PARAM_MUST_BE_STRING, String.class);
  }

  public void setMustBe(String mustHave) {
    addParam(PARAM_MUST_BE_STRING, mustHave);
  }

  public String getMustNot() {
    return getParam(PARAM_MUST_NOT_STRING, String.class);
  }

  public void setMustNot(String mustNot) {
    addParam(PARAM_MUST_NOT_STRING, mustNot);
  }

  public String getIncludeQuery() {
    return getParam(PARAM_INCLUDE_QUERY, String.class);
  }

  public void setIncludeQuery(String includeQuery) {
    addParam(PARAM_INCLUDE_QUERY, includeQuery);
  }

  public String getExcludeQuery() {
    return getParam(PARAM_EXCLUDE_QUERY, String.class);
  }

  public void setExcludeQuery(String excludeQuery) {
    addParam(PARAM_EXCLUDE_QUERY, excludeQuery);
  }

  public String getStartTime() {
    return getParam(PARAM_START_TIME, String.class);
  }

  public void setStartTime(String startTime) {
    addParam(PARAM_START_TIME, startTime);
  }

  public String getEndTime() {
    return getParam(PARAM_END_TIME, String.class);
  }

  public void setEndTime(String endTime) {
    addParam(PARAM_END_TIME, endTime);
  }
}
