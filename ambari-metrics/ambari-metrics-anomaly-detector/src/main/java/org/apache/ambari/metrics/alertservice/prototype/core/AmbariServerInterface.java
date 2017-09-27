/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.alertservice.prototype.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AmbariServerInterface implements Serializable{

  private static final Log LOG = LogFactory.getLog(AmbariServerInterface.class);

  private String ambariServerHost;
  private String clusterName;

  public AmbariServerInterface(String ambariServerHost, String clusterName) {
    this.ambariServerHost = ambariServerHost;
    this.clusterName = clusterName;
  }

  public int getPointInTimeSensitivity() {

    String url = constructUri("http", ambariServerHost, "8080", "/api/v1/clusters/" + clusterName + "/alert_definitions?fields=*");

    URL obj = null;
    BufferedReader in = null;

    try {
      obj = new URL(url);
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      con.setRequestMethod("GET");

      String encoded = Base64.getEncoder().encodeToString(("admin:admin").getBytes(StandardCharsets.UTF_8));
      con.setRequestProperty("Authorization", "Basic "+encoded);

      int responseCode = con.getResponseCode();
      LOG.info("Sending 'GET' request to URL : " + url);
      LOG.info("Response Code : " + responseCode);

      in = new BufferedReader(
        new InputStreamReader(con.getInputStream()));

      StringBuilder responseJsonSb = new StringBuilder();
      String line;
      while ((line = in.readLine()) != null) {
        responseJsonSb.append(line);
      }

      JSONObject jsonObject = new JSONObject(responseJsonSb.toString());
      JSONArray array = jsonObject.getJSONArray("items");
      for(int i = 0 ; i < array.length() ; i++){
        JSONObject alertDefn = array.getJSONObject(i).getJSONObject("AlertDefinition");
        if (alertDefn.get("name") != null && alertDefn.get("name").equals("point_in_time_metrics_anomalies")) {
          JSONObject sourceNode = alertDefn.getJSONObject("source");
          JSONArray params = sourceNode.getJSONArray("parameters");
          for(int j = 0 ; j < params.length() ; j++){
            JSONObject param = params.getJSONObject(j);
            if (param.get("name").equals("sensitivity")) {
              return param.getInt("value");
            }
          }
          break;
        }
      }

    } catch (Exception e) {
      LOG.error(e);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException e) {
          LOG.warn(e);
        }
      }
    }

    return -1;
  }

  private String constructUri(String protocol, String host, String port, String path) {
    StringBuilder sb = new StringBuilder(protocol);
    sb.append("://");
    sb.append(host);
    sb.append(":");
    sb.append(port);
    sb.append(path);
    return sb.toString();
  }

//  public static void main(String[] args) {
//    AmbariServerInterface ambariServerInterface = new AmbariServerInterface();
//    ambariServerInterface.getPointInTimeSensitivity("avijayan-ams-1.openstacklocal","c1");
//  }
}
