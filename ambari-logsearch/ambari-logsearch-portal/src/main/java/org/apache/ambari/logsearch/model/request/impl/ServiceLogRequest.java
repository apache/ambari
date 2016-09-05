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
import org.apache.ambari.logsearch.model.request.LastPageParamDefinition;
import org.apache.ambari.logsearch.model.request.ServiceLogSearchParamDefinition;

import javax.ws.rs.QueryParam;

public class ServiceLogRequest extends ServiceLogFileRequest implements ServiceLogSearchParamDefinition, LastPageParamDefinition {

  @QueryParam(LogSearchConstants.REQUEST_PARAM_KEYWORD)
  private String keyWord;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_SOURCE_LOG_ID)
  private String sourceLogId;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_KEYWORD_TYPE)
  private String keywordType;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_TOKEN)
  private String token;

  @QueryParam(LogSearchConstants.REQUEST_PARAM_LAST_PAGE)
  private boolean isLastPage;

  @Override
  public String getKeyWord() {
    return keyWord;
  }

  @Override
  public void setKeyWord(String keyWord) {
    this.keyWord = keyWord;
  }

  @Override
  public String getSourceLogId() {
    return sourceLogId;
  }

  @Override
  public void setSourceLogId(String sourceLogId) {
    this.sourceLogId = sourceLogId;
  }

  @Override
  public String getKeywordType() {
    return keywordType;
  }

  @Override
  public void setKeywordType(String keywordType) {
    this.keywordType = keywordType;
  }

  @Override
  public String getToken() {
    return token;
  }

  @Override
  public void setToken(String token) {
    this.token = token;
  }

  @Override
  public boolean isLastPage() {
    return isLastPage;
  }

  @Override
  public void setLastPage(boolean lastPage) {
    isLastPage = lastPage;
  }
}
