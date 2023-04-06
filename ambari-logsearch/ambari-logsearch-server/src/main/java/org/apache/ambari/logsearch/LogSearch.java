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

import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.data.solr.SolrRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;

@SpringBootApplication(
  scanBasePackages = {"org.apache.ambari.logsearch"},
  exclude = {
    RepositoryRestMvcAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    SolrAutoConfiguration.class,
    SolrRepositoriesAutoConfiguration.class
  }
)
public class LogSearch {

  public static void main(String[] args) {

    String pidFile = System.getenv("LOGSEARCH_PID_FILE") == null ? "logsearch.pid" : System.getenv("LOGSEARCH_PID_FILE");
    new SpringApplicationBuilder(LogSearch.class)
      .bannerMode(Banner.Mode.OFF)
      .listeners(new ApplicationPidFileWriter(pidFile))
      .web(WebApplicationType.SERVLET)
      .run(args);
  }

}
