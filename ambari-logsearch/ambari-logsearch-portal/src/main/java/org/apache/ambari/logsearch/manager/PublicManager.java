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

package org.apache.ambari.logsearch.manager;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.logsearch.model.response.NameValueData;
import org.apache.ambari.logsearch.model.response.NameValueDataListResponse;
import org.apache.ambari.logsearch.web.security.LogsearchSimpleAuthenticationProvider;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class PublicManager extends JsonManagerBase {

  @Inject
  private LogsearchSimpleAuthenticationProvider simpleAuthenticationProvider;

  public String getGeneralConfig() {
    NameValueDataListResponse nameValueList = new NameValueDataListResponse();
    List<NameValueData> nameValues = new ArrayList<>();
    NameValueData nameValue = new NameValueData();
    nameValue.setName("simpleAuth");
    nameValue.setValue("" + simpleAuthenticationProvider.isEnable());
    nameValues.add(nameValue);
    nameValueList.setvNameValues(nameValues);
    return convertObjToString(nameValueList);
  }
}
