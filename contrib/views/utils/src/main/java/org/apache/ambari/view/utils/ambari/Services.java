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

import java.util.List;

/**
 * Utilities for specific Hadoop services and util functions for them
 */
public class Services {
  public static final String HTTPS_ONLY = "HTTPS_ONLY";
  public static final String HTTP_ONLY = "HTTP_ONLY";
  public static final String YARN_SITE = "yarn-site";
  public static final String YARN_HTTP_POLICY = "yarn.http.policy";

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
    } else {
      url = context.getProperties().get("yarn.resourcemanager.url");
      if (!hasProtocol(url)) {
        throw new AmbariApiException(
            "RA070 View is not cluster associated. Resource Manager URL should contain protocol.");
      }
    }
    return url;
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
