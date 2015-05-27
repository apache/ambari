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

import org.apache.ambari.view.AmbariStreamProvider;
import org.apache.ambari.view.URLStreamProvider;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.cluster.Cluster;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides API to Ambari. Supports both Local and Remote cluster association.
 * Also provides API to get cluster topology (determine what node contains specific service)
 * on both local and remote cluster.
 */
public class AmbariApi {
  public static final String AMBARI_SERVER_URL_INSTANCE_PROPERTY = "ambari.server.url";
  public static final String AMBARI_SERVER_USERNAME_INSTANCE_PROPERTY = "ambari.server.username";
  public static final String AMBARI_SERVER_PASSWORD_INSTANCE_PROPERTY = "ambari.server.password";

  private Cluster cluster;
  private ViewContext context;
  private String remoteUrl;
  private String remoteUsername;
  private String remotePassword;

  /**
   * Constructor for Ambari API based on ViewContext
   * @param context View Context
   */
  public AmbariApi(ViewContext context) {
    this.context = context;

    remoteUrl = context.getProperties().get(AMBARI_SERVER_URL_INSTANCE_PROPERTY);
    remoteUsername = context.getProperties().get(AMBARI_SERVER_USERNAME_INSTANCE_PROPERTY);
    remotePassword = context.getProperties().get(AMBARI_SERVER_PASSWORD_INSTANCE_PROPERTY);
  }

  /**
   * Provides ability to get cluster topology
   * @param requestComponent name of component
   * @return list of hostnames with component
   * @throws AmbariApiException
   */
  public List<String> getHostsWithComponent(String requestComponent) throws AmbariApiException {
    String method = "hosts?fields=Hosts/public_host_name,host_components/HostRoles/component_name";
    String response = readFromAmbari(method);

    List<String> foundHosts = new ArrayList<String>();

    JSONObject jsonObject = (JSONObject) JSONValue.parse(response);
    JSONArray hosts = (JSONArray) jsonObject.get("items");
    for (Object host : hosts) {
      JSONObject hostJson = (JSONObject) host;
      JSONArray hostComponents = (JSONArray) hostJson.get("host_components");
      for (Object component : hostComponents) {
        JSONObject componentJson = (JSONObject) component;
        JSONObject hostRoles = (JSONObject) componentJson.get("HostRoles");
        String componentName = (String) hostRoles.get("component_name");
        if (componentName.equals(requestComponent)) {
          foundHosts.add((String) hostRoles.get("host_name"));
        }
      }
    }
    return foundHosts;
  }

  /**
   * Request to Ambari REST API. Supports both local and remote cluster
   * @param method REST API path, e.g. /api/v1/clusters/mycluster?...
   * @return response
   * @throws AmbariApiException IO error or not associated with cluster
   */
  public String readFromAmbari(String method) throws AmbariApiException {
    String response;

    try {
      InputStream inputStream;

      if (isLocalCluster()) {
        AmbariStreamProvider ambariStreamProvider = context.getAmbariStreamProvider();
        String url = String.format("/api/v1/clusters/%s/%s", getCluster().getName(), method);
        inputStream = ambariStreamProvider.readFrom(url, "GET", (String) null, null, true);

      } else if (isRemoteCluster()) {
        URLStreamProvider urlStreamProvider = getUrlStreamProviderBasicAuth();
        String url = String.format("%s/%s", remoteUrl, method);
        inputStream = urlStreamProvider.readFrom(url, "GET", (String) null, null);

      } else {
        throw new NoClusterAssociatedException(
            "RA030 View is not associated with any cluster. No way to request Ambari.");
      }

      response = IOUtils.toString(inputStream);
    } catch (IOException e) {
      throw new AmbariApiException("RA040 I/O error while requesting Ambari", e);
    }
    return response;
  }

  /**
   * Check if associated with local or remote cluster
   * @return true if associated
   */
  public boolean isClusterAssociated() {
    try {
      getCluster();
      return true;
    } catch (NoClusterAssociatedException e) {
      return false;
    }
  }

  /**
   * Cluster object that provides access for Ambari configuration
   * @return cluster if locally associated or RemoteCluster
   * @throws NoClusterAssociatedException
   */
  public Cluster getCluster() throws NoClusterAssociatedException {
    if (cluster == null) {
      if (isLocalCluster()) {
        cluster = context.getCluster();

      } else if (isRemoteCluster()) {
        cluster = getRemoteCluster();

      } else {
        throw new NoClusterAssociatedException(
            "RA050 View is not associated with any cluster. No way to request Ambari.");
      }
    }
    return cluster;
  }

  /**
   * Is associated with local cluster
   * @return true if associated
   */
  public boolean isLocalCluster() {
    return context.getCluster() != null;
  }

  /**
   * Is associated with remote cluster
   * @return true if associated
   */
  public boolean isRemoteCluster() {
    return remoteUrl != null && !remoteUrl.isEmpty();
  }

  /**
   * Build RemoteCluster instance based on viewContext properties
   * @return RemoteCluster instance
   */
  public RemoteCluster getRemoteCluster() {
    if (!isRemoteCluster())
      return null;

    URLStreamProvider urlStreamProviderBasicAuth = getUrlStreamProviderBasicAuth();
    return new RemoteCluster(remoteUrl, urlStreamProviderBasicAuth);
  }

  /**
   * Build URLStreamProvider with Basic Authentication for Remote Cluster
   * @return URLStreamProvider
   */
  public URLStreamProvider getUrlStreamProviderBasicAuth() {
    if (remoteUsername == null || remoteUsername.isEmpty() ||
        remotePassword == null || remotePassword.isEmpty()) {
      throw new AmbariApiException("RA020 Remote Ambari username and password are not filled");
    }

    URLStreamProvider urlStreamProvider = context.getURLStreamProvider();

    return new URLStreamProviderBasicAuth(urlStreamProvider, remoteUsername, remotePassword);
  }
}
