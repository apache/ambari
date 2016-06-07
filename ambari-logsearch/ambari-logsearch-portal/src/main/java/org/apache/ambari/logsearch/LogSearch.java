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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import org.apache.ambari.logsearch.common.ManageStartEndTime;
import org.apache.ambari.logsearch.solr.metrics.SolrMetricsLoader;
import org.apache.ambari.logsearch.util.ConfigUtil;
import org.apache.ambari.logsearch.util.PropertiesUtil;
import org.apache.hadoop.http.HttpServer2;
import org.apache.log4j.Logger;

public class LogSearch {
  static Logger logger = Logger.getLogger(LogSearch.class);
  
  private static final String LOGSEARCH_PROTOCOL_PROP = "logsearch.protocol";
  private static final String KEYSTORE_LOCATION_PROP = "logsearch.https.keystore";
  private static final String TRUSTORE_LOCATION_PROP = "logsearch.https.trustore";
  private static final String KEYSTORE_PASSWORD_PROP = "logsearch.https.keystore.password";
  private static final String TRUSTORE_PASSWORD_PROP = "logsearch.https.trustore.password";
  private static final String DEFAULT_KEYSTORE_TYPE = "JKS";
  private static final String KEYSTORE_TYPE_PROP = "logsearch.https.keystore.type";
  private static final String HTTPS_PROTOCOL = "https";
  private static final String HTTPS_PORT = "61889";
  private static final String HTTP_PORT = "61888";

  public static void main(String argv[]) {
    String port = (argv.length > 0) ? argv[0] : HTTP_PORT;
    HttpServer2.Builder builder = new HttpServer2.Builder();
    builder.setName("app");
    
    String keystorePassword = PropertiesUtil.getProperty(KEYSTORE_PASSWORD_PROP);
    String trustorePassword = PropertiesUtil.getProperty(TRUSTORE_PASSWORD_PROP);
    String keystoreType = PropertiesUtil.getProperty(KEYSTORE_TYPE_PROP);
    
    String protcol = PropertiesUtil.getProperty(LOGSEARCH_PROTOCOL_PROP);
    String keystoreLocation = PropertiesUtil.getProperty(KEYSTORE_LOCATION_PROP);   
    
    String trustoreLocation = PropertiesUtil.getProperty(TRUSTORE_LOCATION_PROP);
    URI logsearchURI = URI.create("http://0.0.0.0:" + port);
 
    
    if (HTTPS_PROTOCOL.equals(protcol)) {
      if(keystoreType == null || keystoreType.isEmpty()){
        keystoreType = DEFAULT_KEYSTORE_TYPE;
      }

      if (keystoreLocation != null && !keystoreLocation.isEmpty()
          && keystorePassword != null && !keystorePassword.isEmpty()) {
        
        builder.keyPassword(keystorePassword);
        builder.keyStore(keystoreLocation, keystorePassword, keystoreType);
        
        if (trustoreLocation != null && !trustoreLocation.isEmpty()
            && trustorePassword != null && !trustorePassword.isEmpty()) {
          
          builder.trustStore(trustoreLocation, trustorePassword, keystoreType);
        }
        
        if(HTTP_PORT.equals(port)){
          port = HTTPS_PORT;
        }
        logsearchURI = URI.create("https://0.0.0.0:" + port);
      } else{
        logger.warn("starting logsearch in with http protocol as keystore location or password was not present");
      }
    }
    builder.addEndpoint(logsearchURI);
    builder.setFindPort(false);
    List<String> pathList = new ArrayList<String>();
    pathList.add("/*");
    builder.setPathSpec(pathList.toArray(new String[0]));
    builder.needsClientAuth(false);
    Timer timer = new Timer();
    timer.schedule(new ManageStartEndTime(), 0, 40000);
    try {
      logger.info("Starting logsearch server URI="+logsearchURI);
      HttpServer2 server = builder.build();
      server.start();
      ConfigUtil.initializeApplicationConfig();
      logger.info(server.toString());
    } catch (Throwable e) {
      logger.error("Error running logsearch server", e);
    }

    SolrMetricsLoader.startSolrMetricsLoaderTasks();
  }
}
