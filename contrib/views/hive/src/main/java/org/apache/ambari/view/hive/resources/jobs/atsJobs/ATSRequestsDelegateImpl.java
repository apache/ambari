/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.view.hive.resources.jobs.atsJobs;

import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class ATSRequestsDelegateImpl implements ATSRequestsDelegate {
  protected final static Logger LOG =
      LoggerFactory.getLogger(ATSRequestsDelegateImpl.class);
  public static final String EMPTY_ENTITIES_JSON = "{ \"entities\" : [  ] }";

  private ViewContext context;
  private String atsUrl;

  public ATSRequestsDelegateImpl(ViewContext context, String atsUrl) {
    this.context = context;
    this.atsUrl = atsUrl;
  }

  @Override
  public JSONObject hiveQueryIdList(String username) {
    String hiveQueriesListUrl = atsUrl + "/ws/v1/timeline/HIVE_QUERY_ID?primaryFilter=requestuser:" + username;
    String response = readFromWithDefault(hiveQueriesListUrl, "{ \"entities\" : [  ] }");
    return (JSONObject) JSONValue.parse(response);
  }

  @Override
  public JSONObject hiveQueryIdByOperationId(String operationId) {
    String hiveQueriesListUrl = atsUrl + "/ws/v1/timeline/HIVE_QUERY_ID?primaryFilter=operationid:" + operationId;
    String response = readFromWithDefault(hiveQueriesListUrl, "{ \"entities\" : [  ] }");
    return (JSONObject) JSONValue.parse(response);
  }

  @Override
  public JSONObject tezDagByName(String name) {
    String tezDagUrl = atsUrl + "/ws/v1/timeline/TEZ_DAG_ID?primaryFilter=dagName:" + name;
    String response = readFromWithDefault(tezDagUrl, EMPTY_ENTITIES_JSON);
    return (JSONObject) JSONValue.parse(response);
  }

  protected String readFromWithDefault(String hiveQueriesListUrl, String defaultResponse) {
    String response;
    try {
      InputStream responseInputStream = context.getURLStreamProvider().readFrom(hiveQueriesListUrl, "GET",
          null, new HashMap<String, String>());
      response = IOUtils.toString(responseInputStream);
    } catch (IOException e) {
      LOG.error("Error while reading from ATS", e);
      response = defaultResponse;
    }
    return response;
  }

  public String getAtsUrl() {
    return atsUrl;
  }

  public void setAtsUrl(String atsUrl) {
    this.atsUrl = atsUrl;
  }
}
