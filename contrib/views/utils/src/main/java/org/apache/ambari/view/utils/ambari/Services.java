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

import org.apache.ambari.view.ViewContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for specific Hadoop services and util functions for them
 */
public class Services {
  public static final String HTTPS_ONLY = "HTTPS_ONLY";
  public static final String HTTP_ONLY = "HTTP_ONLY";
  public static final String YARN_SITE = "yarn-site";
  public static final String YARN_HTTP_POLICY = "yarn.http.policy";
  public static final String YARN_RESOURCEMANAGER_HA_ENABLED = "yarn.resourcemanager.ha.enabled";

  private final AmbariApi ambariApi;
  private ViewContext context;

  protected final static Logger LOG = LoggerFactory.getLogger(Services.class);

  public Services(AmbariApi ambariApi, ViewContext context) {
    this.ambariApi = ambariApi;
    this.context = context;
  }

  /**
   * Returns URL to Resource Manager.
   * If cluster associated, returns HTTP or HTTPS address based on "yarn.http.policy" property value.
   * If not associated, retrieves RM URL from view instance properties by "yarn.resourcemanager.url" property.
   * @return url of RM
   */
  public String getRMUrl() {
    String url;

    if (ambariApi.isClusterAssociated()) {
      String protocol;

      String haEnabled = ambariApi.getCluster().getConfigurationValue(YARN_SITE, YARN_RESOURCEMANAGER_HA_ENABLED);
      String httpPolicy = ambariApi.getCluster().getConfigurationValue(YARN_SITE, YARN_HTTP_POLICY);
      if (httpPolicy.equals(HTTPS_ONLY)) {
        protocol = "https";
        url = ambariApi.getCluster().getConfigurationValue(YARN_SITE, "yarn.resourcemanager.webapp.https.address");

      } else {
        protocol = "http";
        url = ambariApi.getCluster().getConfigurationValue(YARN_SITE, "yarn.resourcemanager.webapp.address");
        if (!httpPolicy.equals(HTTP_ONLY))
          LOG.error(String.format("RA030 Unknown value %s of yarn-site/yarn.http.policy. HTTP_ONLY assumed.", httpPolicy));
      }

      url = addProtocolIfMissing(url, protocol);

      if (haEnabled != null && haEnabled.equals("true")) {
        url = getActiveRMUrl(url);
      }
    } else {
      url = context.getProperties().get("yarn.resourcemanager.url");
      if (!hasProtocol(url)) {
        throw new AmbariApiException(
            "RA070 View is not cluster associated. Resource Manager URL should contain protocol.");
      }
    }
    return removeTrailingSlash(url);
  }

  private String removeTrailingSlash(String url) {
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    return url;
  }

  public final Pattern refreshHeaderUrlPattern = Pattern.compile("^\\d+;\\s*url=(.*)$");

  /**
   * Returns active RM URL. Makes a request to RM passed as argument.
   * If response contains Refresh header then passed url was standby RM.
   * @param url url of random RM
   * @return url of active RM
   */
  private String getActiveRMUrl(String url) {
    String activeRMUrl = url;
    try {
      HttpURLConnection httpURLConnection = context.getURLConnectionProvider().
          getConnection(url, "GET", (String) null, new HashMap<String, String>());
      String refreshHeader = httpURLConnection.getHeaderField("Refresh");
      if (refreshHeader != null) { // we hit standby RM
        Matcher matcher = refreshHeaderUrlPattern.matcher(refreshHeader);
        if (matcher.find()) {
          activeRMUrl = matcher.group(1);
        }
      }
    } catch (IOException e) {
      throw new AmbariApiException("RA110 ResourceManager is not accessible");
    }
    return activeRMUrl;
  }

  /**
   * Returns URL to WebHCat in format like http://<hostname>:<port>/templeton/v1
   * @return url to WebHCat
   */
  public String getWebHCatURL() {
    String host = null;

    if (ambariApi.isClusterAssociated()) {
      List<String> hiveServerHosts = ambariApi.getHostsWithComponent("WEBHCAT_SERVER");

      if (!hiveServerHosts.isEmpty()) {
        host = hiveServerHosts.get(0);
        LOG.info("WEBHCAT_SERVER component was found on host " + host);
      } else {
        LOG.warn("No host was found with WEBHCAT_SERVER component. Using hive.host property to get hostname.");
      }
    }

    if (host == null) {
      host = context.getProperties().get("webhcat.hostname");
      if (host == null || host.isEmpty()) {
        throw new AmbariApiException(
            "RA080 Can't determine WebHCat hostname neither by associated cluster nor by webhcat.hostname property.");
      }
    }

    String port = context.getProperties().get("webhcat.port");
    if (port == null || port.isEmpty()) {
      throw new AmbariApiException(
          "RA090 Can't determine WebHCat port neither by associated cluster nor by webhcat.port property.");
    }

    return String.format("http://%s:%s/templeton/v1", host, port);
  }

  public static String addProtocolIfMissing(String url, String protocol) throws AmbariApiException {
    if (!hasProtocol(url)) {
      url = protocol + "://" + url;
    }
    return url;
  }

  /**
   * Checks if URL has the protocol
   * @param url url
   * @return true if protocol is present
   */
  public static boolean hasProtocol(String url) {
    return url.matches("^[^:]+://.*$");
  }
}
