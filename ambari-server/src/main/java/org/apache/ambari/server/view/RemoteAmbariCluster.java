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

package org.apache.ambari.server.view;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.orm.entities.RemoteAmbariClusterEntity;
import org.apache.ambari.view.AmbariHttpException;
import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * View associated  Remote cluster implementation.
 */
public class RemoteAmbariCluster implements Cluster {

  private String name;

  private AmbariStreamProvider streamProvider;

  private final LoadingCache<String, JsonElement> configurationCache = CacheBuilder.newBuilder()
    .expireAfterWrite(10, TimeUnit.SECONDS)
    .build(new CacheLoader<String, JsonElement>() {
      @Override
      public JsonElement load(String url) throws Exception {
        return readFromUrlJSON(url);
      }
    });


  /**
   * Constructor for Remote Ambari Cluster
   *
   * @param remoteAmbariClusterEntity
   */
  public RemoteAmbariCluster(RemoteAmbariClusterEntity remoteAmbariClusterEntity, Configuration config) {
    String [] urlSplit = remoteAmbariClusterEntity.getUrl().split("/");

    // remoteAmbariClusterEntity.getName() is not the actual name of Remote Cluster
    // We need to extract the name from cluster url which is like. http://host:port/api/vi/clusters/${clusterName}
    this.name = urlSplit[urlSplit.length -1];
    
    this.streamProvider = new RemoteAmbariStreamProvider(
      remoteAmbariClusterEntity.getUrl(), remoteAmbariClusterEntity.getUsername(),
      remoteAmbariClusterEntity.getPassword(),config.getRequestConnectTimeout(),config.getRequestReadTimeout());
  }

  /**
   * Constructor for Remote Ambari Cluster
   *
   * @param name
   * @param streamProvider
   */
  public RemoteAmbariCluster(String name, AmbariStreamProvider streamProvider) {
    this.name = name;
    this.streamProvider = streamProvider;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getConfigurationValue(String type, String key) {
    JsonElement config = null;
    try {
      String desiredTag = getDesiredConfig(type);
      if(desiredTag != null){
        config = configurationCache.get(String.format("/configurations?(type=%s&tag=%s)", type, desiredTag));
      }
    } catch (ExecutionException e) {
      throw new RemoteAmbariConfigurationReadException("Can't retrieve configuration from Remote Ambari", e);
    }

    if(config == null || !config.isJsonObject()) return null;
    JsonElement items = config.getAsJsonObject().get("items");

    if(items == null || !items.isJsonArray()) return null;
    JsonElement item = items.getAsJsonArray().get(0);

    if(item == null || !item.isJsonObject()) return null;
    JsonElement properties = item.getAsJsonObject().get("properties");

    if(properties == null || !properties.isJsonObject()) return null;
    JsonElement property = properties.getAsJsonObject().get(key);

    if(property == null || !property.isJsonPrimitive()) return null;

    return property.getAsJsonPrimitive().getAsString();
  }

  @Override
  public List<String> getHostsForServiceComponent(String serviceName, String componentName) {
    String url = String.format("services/%s/components/%s?" +
      "fields=host_components/HostRoles/host_name",serviceName,componentName);

    List<String> hosts = new ArrayList<String>();

    try {
      JsonElement response = configurationCache.get(url);

      if(response == null || !response.isJsonObject()) return hosts;

      JsonElement hostComponents = response.getAsJsonObject().get("host_components");

      if(hostComponents == null || !hostComponents.isJsonArray()) return hosts;

      for (JsonElement element : hostComponents.getAsJsonArray()) {
        JsonElement hostRoles = element.getAsJsonObject().get("HostRoles");
        String hostName = hostRoles.getAsJsonObject().get("host_name").getAsString();
        hosts.add(hostName);
      }

    } catch (ExecutionException e) {
      throw new RemoteAmbariConfigurationReadException("Can't retrieve host information from Remote Ambari", e);
    }

    return hosts;
  }

  /**
   * Get list of services installed on the remote cluster
   *
   * @return list of services Available on cluster
   */
  public Set<String> getServices() throws IOException, AmbariHttpException {
    Set<String> services = new HashSet<String>();
    String path = "?fields=services/ServiceInfo/service_name";
    JsonElement config = configurationCache.getUnchecked(path);

    if(config != null && config.isJsonObject()){
      JsonElement items = config.getAsJsonObject().get("services");
      if(items != null && items.isJsonArray()){
        for (JsonElement item : items.getAsJsonArray()) {
          JsonElement serviceInfo =  item.getAsJsonObject().get("ServiceInfo");
          if(serviceInfo != null && serviceInfo.isJsonObject()){
            String serviceName = serviceInfo.getAsJsonObject().get("service_name").getAsString();
            services.add(serviceName);
          }
        }
      }
    }

    return services;
  }

  /**
   * Get the current tag for the config type
   *
   * @param type
   * @return
   * @throws ExecutionException
   */
  private String getDesiredConfig(String type) throws ExecutionException {
    JsonElement desiredConfigResponse = configurationCache.get("?fields=services/ServiceInfo,hosts,Clusters");

    if(desiredConfigResponse == null || !desiredConfigResponse.isJsonObject()) return null;
    JsonElement clusters = desiredConfigResponse.getAsJsonObject().get("Clusters");

    if(clusters == null || !clusters.isJsonObject()) return null;
    JsonElement desiredConfig = clusters.getAsJsonObject().get("desired_configs");

    if(desiredConfig == null || !desiredConfig.isJsonObject()) return null;
    JsonElement desiredConfigForType = desiredConfig.getAsJsonObject().get(type);

    if(desiredConfigForType == null || !desiredConfigForType.isJsonObject()) return null;
    JsonElement typeJson = desiredConfigForType.getAsJsonObject().get("tag");

    if( typeJson == null || !(typeJson.isJsonPrimitive())) return null;

    return typeJson.getAsJsonPrimitive().getAsString();
  }

  /**
   * Read the content of the url from remote cluster
   *
   * @param url
   * @return
   * @throws IOException
   * @throws AmbariHttpException
   */
  private JsonElement readFromUrlJSON(String url) throws IOException, AmbariHttpException {
    InputStream inputStream = streamProvider.readFrom(url, "GET", (String)null, null);
    String response = IOUtils.toString(inputStream);
    return new JsonParser().parse(response);
  }

}
