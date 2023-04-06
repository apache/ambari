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

import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.LOG_ID_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.SHIPPER_CONFIG_D;
import static org.apache.ambari.logsearch.doc.DocConstants.CommonDescriptions.TEST_ENTRY_D;

public interface ShipperConfigTestParams {

  String getShipperConfig();

  @ApiParam(value = SHIPPER_CONFIG_D, name = LogSearchConstants.REQUEST_PARAM_SHIPPER_CONFIG, required = true)
  void setShipperConfig(String shipperConfig);

  String getLogId();

  @ApiParam(value = LOG_ID_D, name = LogSearchConstants.REQUEST_PARAM_LOG_ID, required = true)
  void setLogId(String logId);

  String getTestEntry();

  @ApiParam(value = TEST_ENTRY_D, name = LogSearchConstants.REQUEST_PARAM_TEST_ENTRY, required = true)
  void setTestEntry(String testEntry);

}
