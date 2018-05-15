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

package org.apache.ambari.view.hive2.resources.jobs.rm;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.utils.ServiceFormattedException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class RMRequestsDelegateImpl implements RMRequestsDelegate {
  protected final static Logger LOG =
      LoggerFactory.getLogger(RMRequestsDelegateImpl.class);
  public static final String EMPTY_ENTITIES_JSON = "{ \"entities\" : [  ] }";

  private ViewContext context;
  private String rmUrl;

  public RMRequestsDelegateImpl(ViewContext context, String rmUrl) {
    this.context = context;
    this.rmUrl = rmUrl;
  }

  @Override
  public String dagProgressUrl(String appId, String dagIdx) {
    return rmUrl + String.format("/proxy/%s/ws/v1/tez/dagProgress?dagID=%s", appId, dagIdx);
  }

  @Override
  public String verticesProgressUrl(String appId, String dagIdx, String vertices) {
    return rmUrl + String.format("/proxy/%s/ws/v1/tez/vertexProgresses?dagID=%s&vertexID=%s", appId, dagIdx, vertices);
  }

  @Override
  public JSONObject dagProgress(String appId, String dagIdx) {
    String url = dagProgressUrl(appId, dagIdx);
    String response;
    try {
      InputStream responseInputStream = context.getURLStreamProvider().readFrom(url, "GET",
          (String)null, new HashMap<String, String>());
      response = IOUtils.toString(responseInputStream);
    } catch (IOException e) {
      throw new ServiceFormattedException(
          String.format("R010 DAG %s in app %s not found or ResourceManager is unreachable", dagIdx, appId));
    }
    return (JSONObject) JSONValue.parse(response);
  }

  @Override
  public JSONObject verticesProgress(String appId, String dagIdx, String commaSeparatedVertices) {
    String url = verticesProgressUrl(appId, dagIdx, commaSeparatedVertices);
    String response;
    try {
      InputStream responseInputStream = context.getURLStreamProvider().readFrom(url, "GET",
          (String)null, new HashMap<String, String>());
      response = IOUtils.toString(responseInputStream);
    } catch (IOException e) {
      throw new ServiceFormattedException(
          String.format("R020 DAG %s in app %s not found or ResourceManager is unreachable", dagIdx, appId));
    }
    return (JSONObject) JSONValue.parse(response);
  }

  protected String readFromWithDefault(String url, String defaultResponse) {
    String response;
    try {
      InputStream responseInputStream = context.getURLStreamProvider().readFrom(url, "GET",
          (String)null, new HashMap<String, String>());
      response = IOUtils.toString(responseInputStream);
    } catch (IOException e) {
      LOG.error("Error while reading from RM", e);
      response = defaultResponse;
    }
    return response;
  }

}
