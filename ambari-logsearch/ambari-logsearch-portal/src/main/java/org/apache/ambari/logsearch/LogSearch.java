/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import org.apache.ambari.logsearch.common.ManageStartEndTime;
import org.apache.ambari.logsearch.solr.metrics.SolrMetricsLoader;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.http.HttpServer2;
import org.apache.log4j.Logger;

public class LogSearch {
  private static final Logger logger = Logger.getLogger(LogSearch.class);

  private static final String KEYSTORE_LOCATION_ARG = "javax.net.ssl.keyStore";
  private static final String KEYSTORE_PASSWORD_ARG = "javax.net.ssl.keyStorePassword";
  private static final String KEYSTORE_TYPE_ARG = "javax.net.ssl.keyStoreType";
  private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
  private static final String TRUSTSTORE_LOCATION_ARG = "javax.net.ssl.trustStore";
  private static final String TRUSTSTORE_PASSWORD_ARG = "javax.net.ssl.trustStorePassword";
  private static final String TRUSTSTORE_TYPE_ARG = "javax.net.ssl.trustStoreType";
  private static final String DEFAULT_TRUSTSTORE_TYPE = "JKS";

  private static final String LOGSEARCH_PROTOCOL_PROP = "logsearch.protocol";
  private static final String HTTPS_PROTOCOL = "https";
  private static final String HTTP_PROTOCOL = "http";
  private static final String HTTPS_PORT = "61889";
  private static final String HTTP_PORT = "61888";

  public static void main(String[] argv) {
    HttpServer2.Builder builder = new HttpServer2.Builder();
    builder.setName("app");

    URI logsearchURI = addUri(argv, builder);
    builder.setFindPort(false);
    List<String> pathList = new ArrayList<String>();
    pathList.add("/*");
    builder.setPathSpec(pathList.toArray(new String[0]));
    builder.needsClientAuth(false);
    Timer timer = new Timer();
    timer.schedule(new ManageStartEndTime(), 0, 40000);
    try {
      logger.info("Starting logsearch server URI=" + logsearchURI);
      HttpServer2 server = builder.build();
      server.start();
      ConfigUtil.initializeApplicationConfig();
      logger.info(server.toString());
    } catch (Throwable e) {
      logger.error("Error running logsearch server", e);
    }

    SolrMetricsLoader.startSolrMetricsLoaderTasks();
  }

  private static URI addUri(String[] argv, HttpServer2.Builder builder) {
    boolean portSpecified = argv.length > 0;
    String port = portSpecified ? argv[0] : HTTP_PORT;
    String protocol = HTTP_PROTOCOL;

    String protcolProperty = PropertiesUtil.getProperty(LOGSEARCH_PROTOCOL_PROP);
    if (HTTPS_PROTOCOL.equals(protcolProperty)) {
      String keystoreLocation = System.getProperty(KEYSTORE_LOCATION_ARG);
      String keystorePassword = System.getProperty(KEYSTORE_PASSWORD_ARG);
      String keystoreType = System.getProperty(KEYSTORE_TYPE_ARG, DEFAULT_KEYSTORE_TYPE);

      String trustoreLocation = System.getProperty(TRUSTSTORE_LOCATION_ARG);
      String trustorePassword = System.getProperty(TRUSTSTORE_PASSWORD_ARG);
      String truststoreType = System.getProperty(TRUSTSTORE_TYPE_ARG, DEFAULT_TRUSTSTORE_TYPE);

      if (!StringUtils.isEmpty(keystoreLocation) && !StringUtils.isEmpty(keystorePassword)) {
        builder.keyPassword(keystorePassword);
        builder.keyStore(keystoreLocation, keystorePassword, keystoreType);
        
        if (!StringUtils.isEmpty(trustoreLocation) && !StringUtils.isEmpty(trustorePassword)) {
          builder.trustStore(trustoreLocation, trustorePassword, truststoreType);
        }

        protocol = HTTPS_PROTOCOL;
        if (!portSpecified) {
          port = HTTPS_PORT;
        }
      } else{
        logger.warn("starting logsearch in with http protocol as keystore location or password was not present");
      }
    }

    URI logsearchURI = URI.create(String.format("%s://0.0.0.0:%s", protocol, port));
    builder.addEndpoint(logsearchURI);

    return logsearchURI;
  }
}
