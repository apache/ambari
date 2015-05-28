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

package org.apache.ambari.view.hive.resources.jobs.rm;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive.utils.ServiceFormattedException;
import org.apache.ambari.view.utils.ambari.AmbariApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RMParserFactory {
  protected final static Logger LOG =
      LoggerFactory.getLogger(RMParserFactory.class);

  public static final String HTTPS_ONLY = "HTTPS_ONLY";
  public static final String HTTP_ONLY = "HTTP_ONLY";
  public static final String YARN_SITE = "yarn-site";
  public static final String YARN_HTTP_POLICY = "yarn.http.policy";

  private ViewContext context;

  public RMParserFactory(ViewContext context) {
    this.context = context;
  }

  public RMParser getRMParser() {
    RMRequestsDelegate delegate = new RMRequestsDelegateImpl(context, getRMUrl());
    return new RMParser(delegate);
  }

  public String getRMUrl() {
    String url;

    AmbariApi ambariApi = new AmbariApi(context);

    if (ambariApi.isClusterAssociated()) {
      String httpPolicy = ambariApi.getCluster().getConfigurationValue("yarn-site", "yarn.http.policy");
      if (httpPolicy.equals(HTTPS_ONLY)) {
        url = ambariApi.getCluster().getConfigurationValue("yarn-site", "yarn.resourcemanager.webapp.https.address");
      } else {
        url = ambariApi.getCluster().getConfigurationValue("yarn-site", "yarn.resourcemanager.webapp.address");
        if (!httpPolicy.equals(HTTP_ONLY))
          LOG.error(String.format("R040 Unknown value %s of yarn-site/yarn.http.policy. HTTP_ONLY assumed.", httpPolicy));
      }

    } else {
      url = context.getProperties().get("yarn.resourcemanager.url");
    }
    return addProtocolIfMissing(url);
  }

  public String addProtocolIfMissing(String url) {
    if (!url.matches("^[^:]+://.*$")) {
      AmbariApi ambariApi = new AmbariApi(context);
      if (!ambariApi.isClusterAssociated()) {
        throw new ServiceFormattedException(
            "R030 View is not cluster associated. Resource Manager URL should contain protocol.");
      }

      String httpPolicy = ambariApi.getCluster().getConfigurationValue(YARN_SITE, YARN_HTTP_POLICY);
      if (httpPolicy.equals(HTTPS_ONLY)) {
        url = "https://" + url;
      } else {
        url = "http://" + url;
        if (!httpPolicy.equals(HTTP_ONLY))
          LOG.error(String.format("R050 Unknown value %s of yarn-site/yarn.http.policy. HTTP_ONLY assumed.", httpPolicy));
      }
    }
    return url;
  }
}
