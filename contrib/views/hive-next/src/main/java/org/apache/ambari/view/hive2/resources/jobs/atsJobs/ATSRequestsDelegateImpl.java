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

package org.apache.ambari.view.hive2.resources.jobs.atsJobs;

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
    this.atsUrl = addProtocolIfMissing(atsUrl);
  }

  private String addProtocolIfMissing(String atsUrl) {
    if (!atsUrl.matches("^[^:]+://.*$"))
      atsUrl = "http://" + atsUrl;
    return atsUrl;
  }

  @Override
  public String hiveQueryIdDirectUrl(String entity) {
    return atsUrl + "/ws/v1/timeline/HIVE_QUERY_ID/" + entity;
  }

  @Override
  public String hiveQueryIdOperationIdUrl(String operationId) {
    // ATS parses operationId started with digit as integer and not returns the response.
    // Quotation prevents this.
    return atsUrl + "/ws/v1/timeline/HIVE_QUERY_ID?primaryFilter=operationid:%22" + operationId + "%22";
  }

  @Override
  public String tezDagDirectUrl(String entity) {
    return atsUrl + "/ws/v1/timeline/TEZ_DAG_ID/" + entity;
  }

  @Override
  public String tezDagNameUrl(String name) {
    return atsUrl + "/ws/v1/timeline/TEZ_DAG_ID?primaryFilter=dagName:" + name;
  }

  @Override
  public String tezVerticesListForDAGUrl(String dagId) {
    return atsUrl + "/ws/v1/timeline/TEZ_VERTEX_ID?primaryFilter=TEZ_DAG_ID:" + dagId;
  }

  @Override
  public JSONObject hiveQueryIdsForUser(String username) {
    String hiveQueriesListUrl = atsUrl + "/ws/v1/timeline/HIVE_QUERY_ID?primaryFilter=requestuser:" + username;
    String response = readFromWithDefault(hiveQueriesListUrl, "{ \"entities\" : [  ] }");
    return (JSONObject) JSONValue.parse(response);
  }

  @Override
  public JSONObject hiveQueryIdByOperationId(String operationId) {
    String hiveQueriesListUrl = hiveQueryIdOperationIdUrl(operationId);
    String response = readFromWithDefault(hiveQueriesListUrl, EMPTY_ENTITIES_JSON);
    return (JSONObject) JSONValue.parse(response);
  }

  @Override
  public JSONObject tezDagByName(String name) {
    String tezDagUrl = tezDagNameUrl(name);
    String response = readFromWithDefault(tezDagUrl, EMPTY_ENTITIES_JSON);
    return (JSONObject) JSONValue.parse(response);
  }

  @Override
  public JSONObject tezDagByEntity(String entity) {
    String tezDagEntityUrl = tezDagEntityUrl(entity);
    String response = readFromWithDefault(tezDagEntityUrl, EMPTY_ENTITIES_JSON);
    return (JSONObject) JSONValue.parse(response);
  }

  /**
   * fetches the HIVE_QUERY_ID from ATS for given user between given time period
   * @param username: username for which to fetch hive query IDs
   * @param startTime: time in miliseconds, inclusive
   * @param endTime: time in miliseconds, exclusive
   * @return
   */
  @Override
  public JSONObject hiveQueryIdsForUserByTime(String username, long startTime, long endTime) {
    StringBuilder url = new StringBuilder();
    url.append(atsUrl).append("/ws/v1/timeline/HIVE_QUERY_ID?")
      .append("windowStart=").append(startTime)
      .append("&windowEnd=").append(endTime)
      .append("&primaryFilter=requestuser:").append(username);
    String hiveQueriesListUrl = url.toString();

    String response = readFromWithDefault(hiveQueriesListUrl, EMPTY_ENTITIES_JSON);
    return (JSONObject) JSONValue.parse(response);
  }

  @Override
  public JSONObject hiveQueryEntityByEntityId(String hiveEntityId) {
    StringBuilder url = new StringBuilder();
    url.append(atsUrl).append("/ws/v1/timeline/HIVE_QUERY_ID/").append(hiveEntityId);
    String hiveQueriesListUrl = url.toString();
    String response = readFromWithDefault(hiveQueriesListUrl, EMPTY_ENTITIES_JSON);
    return (JSONObject) JSONValue.parse(response);
  }

  private String tezDagEntityUrl(String entity) {
    return atsUrl + "/ws/v1/timeline/TEZ_DAG_ID?primaryFilter=callerId:" + entity;
  }

  public boolean checkATSStatus() throws IOException {
    String url = atsUrl + "/ws/v1/timeline/";
    InputStream responseInputStream = context.getURLStreamProvider().readAsCurrent(url, "GET",
            (String)null, new HashMap<String, String>());
     IOUtils.toString(responseInputStream);
    return true;
  }

  @Override
  public JSONObject tezVerticesListForDAG(String dagId) {
    String response = readFromWithDefault(tezVerticesListForDAGUrl(dagId), "{ \"entities\" : [  ] }");
    return (JSONObject) JSONValue.parse(response);
  }



  protected String readFromWithDefault(String atsUrl, String defaultResponse) {
    String response;
    try {
      InputStream responseInputStream = context.getURLStreamProvider().readAsCurrent(atsUrl, "GET",
          (String)null, new HashMap<String, String>());
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
