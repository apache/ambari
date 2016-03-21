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

package org.apache.ambari.view.utils.ambari;

import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * Class that provides same interface as local Cluster, but
 * is able to retrieve configuration values by REST API
 */
public class RemoteCluster implements Cluster {
  protected String name;
  protected String baseUrl;
  protected URLStreamProvider urlStreamProvider;
  protected Map<String, JSONObject> configurationCache;

  /**
   * Constructor for RemoteCluster
   * @param ambariClusterUrl Ambari Server Cluster REST API URL (for example: http://ambari.server:8080/api/v1/clusters/c1)
   * @param urlStreamProvider stream provider with authorization support
   */
  public RemoteCluster(String ambariClusterUrl, URLStreamProvider urlStreamProvider) {
    this.baseUrl = ambariClusterUrl;
    this.urlStreamProvider = urlStreamProvider;

    String[] parts = ambariClusterUrl.split("/");
    this.name = parts[parts.length-1];
    PassiveExpiringMap<String, JSONObject> configurations = new PassiveExpiringMap<String, JSONObject>(10000L);  // keep cache for 10 seconds
    configurationCache = Collections.synchronizedMap(configurations);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getConfigurationValue(String type, String key) {
    JSONObject config;
    try {
      String desiredTag = getDesiredConfig(type);
      config = readFromUrlJSON(String.format("%s/configurations?(type=%s&tag=%s)", baseUrl, type, desiredTag));
    } catch (IOException e) {
      throw new AmbariApiException("RA010 Can't retrieve configuration from Remote Ambari", e);
    }

    JSONObject items = (JSONObject) ((JSONArray) config.get("items")).get(0);
    JSONObject properties = (JSONObject) items.get("properties");
    return (properties == null ? null : (String) properties.get(key));
  }

  private String getDesiredConfig(String type) throws IOException {
    JSONObject desiredConfigResponse = readFromUrlJSON(
        String.format("%s?fields=services/ServiceInfo,hosts,Clusters", baseUrl));
    JSONObject clusters = (JSONObject) (desiredConfigResponse.get("Clusters"));
    JSONObject desiredConfig = (JSONObject) (clusters.get("desired_configs"));
    JSONObject desiredConfigForType = (JSONObject) desiredConfig.get(type);

    return (String) desiredConfigForType.get("tag");
  }

  private JSONObject readFromUrlJSON(String url) throws IOException {
    JSONObject jsonObject = configurationCache.get(url);
    if (jsonObject == null) {
      InputStream inputStream = urlStreamProvider.readFrom(url, "GET", (String)null, null);
      String response = IOUtils.toString(inputStream);
      jsonObject = (JSONObject) JSONValue.parse(response);

      configurationCache.put(url, jsonObject);
    }
    return jsonObject;
  }
}
