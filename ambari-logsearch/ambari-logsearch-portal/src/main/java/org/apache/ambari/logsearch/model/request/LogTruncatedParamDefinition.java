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

import static org.apache.ambari.logsearch.doc.DocConstants.ServiceDescriptions.ID_D;
import static org.apache.ambari.logsearch.doc.DocConstants.ServiceDescriptions.SCROLL_TYPE_D;
import static org.apache.ambari.logsearch.doc.DocConstants.ServiceDescriptions.NUMBER_ROWS_D;

public interface LogTruncatedParamDefinition {

  String getId();

  @ApiParam(value = ID_D, name = LogSearchConstants.REQUEST_PARAM_ID)
  void setId(String id);

  String getScrollType();

  @ApiParam(value = SCROLL_TYPE_D, name = LogSearchConstants.REQUEST_PARAM_SCROLL_TYPE)
  void setScrollType(String scrollType);

  String getNumberRows();

  @ApiParam(value = NUMBER_ROWS_D, name = LogSearchConstants.REQUEST_PARAM_NUMBER_ROWS)
  void setNumberRows(String numberRows);
}
