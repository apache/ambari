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

public class ServiceLogTruncatedSearchCriteria extends ServiceLogFileSearchCriteria {

  public String getId() {
    return getParam(SearchCriteriaConstants.PARAM_ID, String.class);
  }

  public void setId(String id) {
    addParam(SearchCriteriaConstants.PARAM_ID, id);
  }

  public String getScrollType() {
    return getParam(SearchCriteriaConstants.PARAM_SCROLL_TYPE, String.class);
  }

  public void setScrollType(String scrollType) {
    addParam(SearchCriteriaConstants.PARAM_SCROLL_TYPE, scrollType);
  }

  public String getNumberRows() {
    return getParam(SearchCriteriaConstants.PARAM_NUMBER_ROWS, String.class);
  }

  public void setNumberRows(String numberRows) {
    addParam(SearchCriteriaConstants.PARAM_NUMBER_ROWS, numberRows);
  }
}
