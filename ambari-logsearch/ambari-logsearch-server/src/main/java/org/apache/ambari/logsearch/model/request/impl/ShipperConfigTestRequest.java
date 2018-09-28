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

import org.apache.ambari.logsearch.model.request.ShipperConfigTestParams;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

import javax.ws.rs.FormParam;

import static org.apache.ambari.logsearch.common.LogSearchConstants.REQUEST_PARAM_LOG_ID;
import static org.apache.ambari.logsearch.common.LogSearchConstants.REQUEST_PARAM_SHIPPER_CONFIG;
import static org.apache.ambari.logsearch.common.LogSearchConstants.REQUEST_PARAM_TEST_ENTRY;

public class ShipperConfigTestRequest implements ShipperConfigTestParams {

  @NotBlank
  @FormParam(REQUEST_PARAM_LOG_ID)
  private String logId;

  @NotBlank
  @FormParam(REQUEST_PARAM_TEST_ENTRY)
  private String testEntry;

  @NotEmpty
  @FormParam(REQUEST_PARAM_SHIPPER_CONFIG)
  private String shipperConfig;

  @Override
  public String getLogId() {
    return logId;
  }

  @Override
  public void setLogId(String logId) {
    this.logId = logId;
  }

  @Override
  public String getShipperConfig() {
    return shipperConfig;
  }

  @Override
  public void setShipperConfig(String shipperConfig) {
    this.shipperConfig = shipperConfig;
  }

  @Override
  public String getTestEntry() {
    return testEntry;
  }

  public void setTestEntry(String testEntry) {
    this.testEntry = testEntry;
  }
}
