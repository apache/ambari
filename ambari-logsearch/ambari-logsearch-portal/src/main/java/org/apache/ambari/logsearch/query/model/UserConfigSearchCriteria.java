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

public class UserConfigSearchCriteria extends CommonSearchCriteria {

  public String getUserName() {
    return getParam(SearchCriteriaConstants.PARAM_USER_NAME, String.class);
  }

  public void setUserName(String userName) {
    addParam(SearchCriteriaConstants.PARAM_USER_NAME, userName);
  }

  public String getFilterName() {
    return getParam(SearchCriteriaConstants.PARAM_FILTER_NAME, String.class);
  }

  public void setFilterName(String filterName) {
    addParam(SearchCriteriaConstants.PARAM_FILTER_NAME, filterName);
  }

  public String getRowType() {
    return getParam(SearchCriteriaConstants.PARAM_ROW_TYPE, String.class);
  }

  public void setRowType(String rowType) {
    addParam(SearchCriteriaConstants.PARAM_ROW_TYPE, rowType);
  }
}
