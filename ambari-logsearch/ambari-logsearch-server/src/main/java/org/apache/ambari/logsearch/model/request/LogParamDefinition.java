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
package org.apache.ambari.logsearch.model.request;

import io.swagger.annotations.ApiParam;
import org.apache.ambari.logsearch.common.LogSearchConstants;

import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.E_MESSAGE_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.I_MESSAGE_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.MUST_BE_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.MUST_NOT_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.INCLUDE_QUERY_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.EXCLUDE_QUERY_D;

public interface LogParamDefinition {

  String getIncludeMessage();

  @ApiParam(value = I_MESSAGE_D, name = LogSearchConstants.REQUEST_PARAM_I_MESSAGE)
  void setIncludeMessage(String includeMessage);

  String getExcludeMessage();

  @ApiParam(value = E_MESSAGE_D, name = LogSearchConstants.REQUEST_PARAM_E_MESSAGE)
  void setExcludeMessage(String excludeMessage);

  String getMustBe();

  @ApiParam(value = MUST_BE_D, name = LogSearchConstants.REQUEST_PARAM_MUST_BE)
  void setMustBe(String mustBe);

  String getMustNot();

  @ApiParam(value = MUST_NOT_D, name = LogSearchConstants.REQUEST_PARAM_MUST_NOT)
  void setMustNot(String mustNot);

  String getIncludeQuery();

  @ApiParam(value = INCLUDE_QUERY_D, name = LogSearchConstants.REQUEST_PARAM_INCLUDE_QUERY)
  void setIncludeQuery(String includeQuery);

  String getExcludeQuery();

  @ApiParam(value = EXCLUDE_QUERY_D, name = LogSearchConstants.REQUEST_PARAM_EXCLUDE_QUERY)
  void setExcludeQuery(String excludeQuery);
}
