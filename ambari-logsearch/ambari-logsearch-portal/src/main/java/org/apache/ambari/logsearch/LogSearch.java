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
import org.apache.hadoop.http.HttpServer2;
import org.apache.log4j.Logger;

public class LogSearch {
  static Logger logger = Logger.getLogger(LogSearch.class);

  public static void main(String argv[]) {
    String port = (argv.length > 0) ? argv[0] : "61888";
    HttpServer2.Builder builder = new HttpServer2.Builder();
    builder.setName("app");
    builder.addEndpoint(URI.create("http://0.0.0.0:" + port));
    builder.setFindPort(false);
    List<String> pathList = new ArrayList<String>();
    pathList.add("/*");
    builder.setPathSpec(pathList.toArray(new String[0]));
    builder.needsClientAuth(false);
    Timer timer = new Timer();
    timer.schedule(new ManageStartEndTime(), 0, 40000);
    try {
      logger.info("Starting logsearch server...");
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
