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
package org.apache.ambari.infra;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationPidFileWriter;

@SpringBootApplication(
  scanBasePackages = {"org.apache.ambari.infra"},
  exclude = {
    RepositoryRestMvcAutoConfiguration.class,
    WebMvcAutoConfiguration.class,
    BatchAutoConfiguration.class,
    SecurityAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    SolrAutoConfiguration.class
  }
)
public class InfraManager {

  public static void main(String[] args) {
    String pidFile = System.getenv("INFRA_MANAGER_PID_FILE") == null ? "infra-manager.pid" : System.getenv("INFRA_MANAGER_PID_FILE");
    new SpringApplicationBuilder(InfraManager.class)
      .bannerMode(Banner.Mode.OFF)
      .listeners(new ApplicationPidFileWriter(pidFile))
      .web(true)
      .run(args);
  }
}
