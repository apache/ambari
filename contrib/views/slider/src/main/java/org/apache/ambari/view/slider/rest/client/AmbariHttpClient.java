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

package org.apache.ambari.view.slider.rest.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.view.slider.clients.AmbariClient;
import org.apache.ambari.view.slider.clients.AmbariCluster;
import org.apache.ambari.view.slider.clients.AmbariClusterInfo;
import org.apache.ambari.view.slider.clients.AmbariHostInfo;
import org.apache.ambari.view.slider.clients.AmbariServiceInfo;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AmbariHttpClient extends BaseHttpClient implements AmbariClient {

	private static final Logger logger = Logger.getLogger(AmbariHttpClient.class);

	public AmbariHttpClient(String url, String userId, String password) {
		super(url, userId, password);
	}

	/**
	 * Provides the first cluster defined on this Ambari server.
	 *
	 * @return
	 */
	public AmbariClusterInfo getClusterInfo() {
		try {
			JsonElement jsonElement = doGetJson("/api/v1/clusters");
			JsonObject jsonObject = jsonElement.getAsJsonObject();
			JsonArray clustersArray = jsonObject.get("items").getAsJsonArray();
			if (clustersArray.size() > 0) {
				AmbariClusterInfo cluster = new AmbariClusterInfo();
				JsonObject clusterObj = clustersArray.get(0).getAsJsonObject()
				    .get("Clusters").getAsJsonObject();
				cluster.setName(clusterObj.get("cluster_name").getAsString());
				cluster.setVersion(clusterObj.get("version").getAsString());
				return cluster;
			}
		} catch (HttpException e) {
			logger.warn("Unable to determine Ambari clusters", e);
			throw new RuntimeException(e.getMessage(), e);
		} catch (IOException e) {
			logger.warn("Unable to determine Ambari clusters", e);
			throw new RuntimeException(e.getMessage(), e);
		}
		return null;
	}

	public AmbariCluster getCluster(AmbariClusterInfo clusterInfo) {
		if (clusterInfo != null) {
			try {
				JsonElement jsonElement = doGetJson("/api/v1/clusters/"
				    + clusterInfo.getName());
				if (jsonElement != null) {
					AmbariCluster cluster = new AmbariCluster();
					// desired configs
					Map<String, String> desiredConfigs = new HashMap<String, String>();
					JsonObject desiredConfigsObj = jsonElement.getAsJsonObject()
					    .get("Clusters").getAsJsonObject().get("desired_configs")
					    .getAsJsonObject();
					for (Map.Entry<String, JsonElement> entry : desiredConfigsObj
					    .entrySet()) {
						desiredConfigs.put(entry.getKey(), entry.getValue()
						    .getAsJsonObject().get("tag").getAsString());
					}
					cluster.setDesiredConfigs(desiredConfigs);
					// services
					List<AmbariServiceInfo> services = new ArrayList<AmbariServiceInfo>();
					cluster.setServices(services);
					// hosts
					List<AmbariHostInfo> hosts = new ArrayList<AmbariHostInfo>();
					cluster.setHosts(hosts);
					return cluster;
				}
			} catch (HttpException e) {
				logger.warn("Unable to determine Ambari cluster details - "
				    + clusterInfo.getName(), e);
				throw new RuntimeException(e.getMessage(), e);
			} catch (IOException e) {
				logger.warn("Unable to determine Ambari cluster details - "
				    + clusterInfo.getName(), e);
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return null;
	}

	public Map<String, String> getConfiguration(AmbariClusterInfo cluster,
	    String configType, String configTag) {
		if (cluster != null && configType != null && configTag != null) {
			try {
				JsonElement jsonElement = doGetJson("/api/v1/clusters/"
				    + cluster.getName() + "/configurations?type=" + configType
				    + "&tag=" + configTag);
				JsonObject jsonObject = jsonElement.getAsJsonObject();
				JsonArray configsArray = jsonObject.get("items").getAsJsonArray();
				if (configsArray.size() > 0) {
					JsonObject propertiesObj = configsArray.get(0).getAsJsonObject()
					    .get("properties").getAsJsonObject();
					Map<String, String> properties = new HashMap<String, String>();
					for (Map.Entry<String, JsonElement> entry : propertiesObj.entrySet()) {
						properties.put(entry.getKey(), entry.getValue().getAsString());
					}
					return properties;
				}
			} catch (HttpException e) {
				logger.warn("Unable to determine Ambari clusters", e);
				throw new RuntimeException(e.getMessage(), e);
			} catch (IOException e) {
				logger.warn("Unable to determine Ambari clusters", e);
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return null;
	}

}