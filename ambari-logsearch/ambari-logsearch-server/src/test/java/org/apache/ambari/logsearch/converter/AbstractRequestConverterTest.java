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
package org.apache.ambari.logsearch.converter;

import org.apache.ambari.logsearch.model.request.impl.BaseLogRequest;
import org.apache.ambari.logsearch.model.request.impl.CommonSearchRequest;

public class AbstractRequestConverterTest {

  public void fillBaseLogRequestWithTestData(BaseLogRequest request) {
    fillCommonRequestWithTestData(request);
    request.setFrom("2016-09-13T22:00:01.000Z");
    request.setTo("2016-09-14T22:00:01.000Z");
    request.setMustBe("logsearch_app,secure_log");
    request.setMustNot("hst_agent,system_message");
    request.setIncludeQuery("[{\"log_message\" : \"myincludemessage\"}]");
    request.setExcludeQuery("[{\"log_message\" : \"myexcludemessage\"}]");
  }

  public void fillCommonRequestWithTestData(CommonSearchRequest request) {
    request.setStartIndex("0");
    request.setPage("0");
    request.setPageSize("25");
    request.setClusters("cl1");
  }

}
