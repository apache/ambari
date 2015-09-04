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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.slider.clients.AmbariClient;
import org.apache.ambari.view.slider.clients.AmbariCluster;
import org.apache.ambari.view.slider.clients.AmbariClusterInfo;
import org.apache.ambari.view.slider.clients.AmbariHostComponent;
import org.apache.ambari.view.slider.clients.AmbariHostInfo;
import org.apache.ambari.view.slider.clients.AmbariService;
import org.apache.ambari.view.slider.clients.AmbariServiceInfo;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmbariHttpClient extends BaseHttpClient implements AmbariClient {

	private static final Logger logger = LoggerFactory.getLogger(AmbariHttpClient.class);

	public AmbariHttpClient(String url, String userId, String password,
			ViewContext viewContext) {
		super(url, userId, password, viewContext);
	}
	
    @SuppressWarnings("deprecation")
    private RuntimeException createRuntimeException(HttpException httpException) {
      String message = httpException.getMessage();
      try {
        JsonElement jsonElement = new JsonParser().parse(new JsonReader(new StringReader(httpException.getMessage())));
        if (jsonElement != null && jsonElement.getAsJsonObject().has("message")) {
          message = jsonElement.getAsJsonObject().get("message").getAsString();
        }
      } catch (Throwable t) {
      }
      if (httpException.getReasonCode() != HttpStatus.SC_OK) {
        message = httpException.getReasonCode() + " " + httpException.getReason() + ": " + message;
      }
      return new RuntimeException(message, httpException);
    }

    /**
     * Provides the first cluster defined on this Ambari server.
     * 
     * @return
     */
    public AmbariClusterInfo getClusterInfo() {
        try {
            JsonElement jsonElement = doGetJson("/api/v1/clusters");
            if(jsonElement==null) {
              return null;
            }
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
            throw createRuntimeException(e);
        } catch (IOException e) {
            logger.warn("Unable to determine Ambari clusters", e);
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Provides the first cluster defined on this Ambari server.
     * 
     * @return
     */
    public Map<String, String> getAmbariServerConfigs() {
      Map<String, String> configs = new HashMap<String, String>();
      try {
        JsonElement jsonElement = doGetJson("/api/v1/services/AMBARI/components/AMBARI_SERVER");
        if (jsonElement != null && jsonElement.getAsJsonObject().has("RootServiceComponents")
            && jsonElement.getAsJsonObject().get("RootServiceComponents").getAsJsonObject().has("properties")) {
          JsonObject ambariProperties = jsonElement.getAsJsonObject().get("RootServiceComponents").getAsJsonObject().get("properties").getAsJsonObject();
          for (Entry<String, JsonElement> entry : ambariProperties.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
              configs.put(entry.getKey(), entry.getValue().getAsString());
            }
          }
        }
      } catch (HttpException e) {
        logger.warn("Unable to determine Ambari clusters", e);
      } catch (IOException e) {
        logger.warn("Unable to determine Ambari clusters", e);
      }
      return configs;
    }

	public AmbariCluster getCluster(AmbariClusterInfo clusterInfo) {
		if (clusterInfo != null) {
			try {
				JsonElement jsonElement = doGetJson("/api/v1/clusters/"
				    + clusterInfo.getName() + "?fields=services/ServiceInfo,hosts,Clusters");
				if (jsonElement != null) {
					AmbariCluster cluster = new AmbariCluster();
					// desired configs
					Map<String, String> desiredConfigs = new HashMap<String, String>();
					JsonObject jsonObject = jsonElement.getAsJsonObject();
                    JsonObject clustersJsonObject = jsonObject.get("Clusters").getAsJsonObject();
                    JsonObject desiredConfigsObj = clustersJsonObject.get("desired_configs")
					    .getAsJsonObject();
					for (Map.Entry<String, JsonElement> entry : desiredConfigsObj
					    .entrySet()) {
						desiredConfigs.put(entry.getKey(), entry.getValue()
						    .getAsJsonObject().get("tag").getAsString());
					}
					cluster.setDesiredConfigs(desiredConfigs);
					cluster.setName(clustersJsonObject.get("cluster_name").getAsString());
					cluster.setVersion(clustersJsonObject.get("version").getAsString());
					// services
                    List<AmbariServiceInfo> services = new ArrayList<AmbariServiceInfo>();
                    for (JsonElement svcJson : jsonObject.get("services")
                        .getAsJsonArray()) {
                      AmbariServiceInfo si = new AmbariServiceInfo();
                      si.setId(svcJson.getAsJsonObject().get("ServiceInfo")
                          .getAsJsonObject().get("service_name").getAsString());
                      si.setStarted("STARTED".equals(svcJson.getAsJsonObject()
                          .get("ServiceInfo").getAsJsonObject().get("state")
                          .getAsString()));
                      services.add(si);
                    }
					cluster.setServices(services);
					// hosts
					List<AmbariHostInfo> hosts = new ArrayList<AmbariHostInfo>();
					for (JsonElement hostJson : jsonObject.get("hosts")
                        .getAsJsonArray()) {
                      AmbariHostInfo hi = new AmbariHostInfo();
                      hi.setHostName(hostJson.getAsJsonObject().get("Hosts")
                          .getAsJsonObject().get("host_name").getAsString());
                      hosts.add(hi);
                    }
					cluster.setHosts(hosts);
					return cluster;
				}
			} catch (IllegalStateException e) {
			  logger.warn("Unable to determine Ambari cluster details - "
			      + clusterInfo.getName(), e);
			  throw new RuntimeException(e.getMessage(), e);
			} catch (HttpException e) {
				logger.warn("Unable to determine Ambari cluster details - "
				    + clusterInfo.getName(), e);
				throw createRuntimeException(e);
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
				if (jsonElement!=null) {
				  JsonObject jsonObject = jsonElement.getAsJsonObject();
				  JsonArray configsArray = jsonObject.get("items").getAsJsonArray();
				  if (configsArray.size() > 0) {
				    JsonObject propertiesObj = configsArray.get(0).getAsJsonObject()
				        .get("properties").getAsJsonObject();
				    Map<String, String> properties = new HashMap<String, String>();
				    for (Map.Entry<String, JsonElement> entry : propertiesObj.entrySet()) {
                                      if (entry.getValue().isJsonPrimitive()) {
                                        properties.put(entry.getKey(), entry.getValue().getAsString());
                                      }
				    }
				    return properties;
				  }
				}
			} catch (HttpException e) {
				logger.warn("Unable to determine Ambari clusters", e);
				throw createRuntimeException(e);
			} catch (IOException e) {
				logger.warn("Unable to determine Ambari clusters", e);
				throw new RuntimeException(e.getMessage(), e);
			}
		}
		return null;
	}

	@Override
	public AmbariService getService(AmbariClusterInfo cluster, String serviceId) {
      if (cluster != null && serviceId != null) {
        try {
            JsonElement jsonElement = doGetJson("/api/v1/clusters/"
                + cluster.getName() + "/services/" + serviceId + "?fields=ServiceInfo,components/host_components/HostRoles");
            if (jsonElement == null) {
              return null;
            }
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            AmbariService svc = new AmbariService();
            JsonObject serviceInfoJsonObject = jsonObject.get("ServiceInfo").getAsJsonObject();
            svc.setId(serviceInfoJsonObject.get("service_name").getAsString());
            svc.setStarted("STARTED".equals(serviceInfoJsonObject.get("state").getAsString()));
            svc.setMaintenanceMode(!"OFF".equals(serviceInfoJsonObject.get("maintenance_state").getAsString()));
            Map<String, List<AmbariHostComponent>> componentsToHostComponentsMap = new HashMap<String, List<AmbariHostComponent>>();
            for(JsonElement ce: jsonObject.get("components").getAsJsonArray()){
              String componentName = ce.getAsJsonObject().get("ServiceComponentInfo").getAsJsonObject().get("component_name").getAsString();
              List<AmbariHostComponent> hcList = new ArrayList<AmbariHostComponent>();
              componentsToHostComponentsMap.put(componentName, hcList);
              JsonArray hcJsonArray = ce.getAsJsonObject().get("host_components").getAsJsonArray();
              for(JsonElement hce: hcJsonArray) {
                AmbariHostComponent hc = new AmbariHostComponent();
                JsonObject hcJsonObject = hce.getAsJsonObject().get("HostRoles").getAsJsonObject();
                hc.setHostName(hcJsonObject.get("host_name").getAsString());
                hc.setStarted("STARTED".equals(hcJsonObject.get("state").getAsString()));
                hc.setName(hcJsonObject.get("component_name").getAsString());
                hcList.add(hc);
              }
            }
            svc.setComponentsToHostComponentsMap(componentsToHostComponentsMap);
            return svc;
        } catch (HttpException e) {
            logger.warn("Unable to determine Ambari clusters", e);
            throw createRuntimeException(e);
        } catch (IOException e) {
            logger.warn("Unable to determine Ambari clusters", e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    return null;
}

}
