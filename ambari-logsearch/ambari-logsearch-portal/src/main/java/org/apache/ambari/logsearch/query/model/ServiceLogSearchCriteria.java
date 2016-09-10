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

public class ServiceLogSearchCriteria extends CommonServiceLogSearchCriteria {

  public void setKeyword(String keyword) {
    addParam(SearchCriteriaConstants.PARAM_KEYWORD, keyword);
  }

  public String getKeyword() {
    return getParam(SearchCriteriaConstants.PARAM_KEYWORD, String.class);
  }

  public void setKeywordType(String keywordType) {
    addParam(SearchCriteriaConstants.PARAM_KEYWORD_TYPE, keywordType);
  }

  public String getKeywordType() {
    return getParam(SearchCriteriaConstants.PARAM_KEYWORD_TYPE, String.class);
  }

  public void setSourceLogId(String sourceLogId) {
    addParam(SearchCriteriaConstants.PARAM_SOURCE_LOG_ID, sourceLogId);
  }

  public String getSourceLogId() {
    return getParam(SearchCriteriaConstants.PARAM_SOURCE_LOG_ID, String.class);
  }

  public void setToken(String token) {
    addParam(SearchCriteriaConstants.PARAM_TOKEN, token);
  }

  public String getToken() {
    return getParam(SearchCriteriaConstants.PARAM_TOKEN, String.class);
  }

  public void setLastPage(boolean lastPage) {
    addParam(SearchCriteriaConstants.PARAM_IS_LAST_PAGE, lastPage);
  }

  public boolean isLastPage() {
    return getParam(SearchCriteriaConstants.PARAM_IS_LAST_PAGE, Boolean.class);
  }

}
