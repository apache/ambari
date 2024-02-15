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
package org.apache.ambari.logsearch.conf;

import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_PROPERTIES_FILE;

@Configuration
public class LogSearchHttpHeaderConfig {

  @Value("${logsearch.http.header.access-control-allow-origin:*}")
  @LogSearchPropertyDescription(
    name = "logsearch.http.header.access-control-allow-origin",
    description = "Access-Control-Allow-Origin header for Log Search Server.",
    examples = {"*", "http://c6401.ambari.apache.org"},
    defaultValue = "*",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String accessControlAllowOrigin;

  @Value("${logsearch.http.header.access-control-allow-headers:origin, content-type, accept, authorization}")
  @LogSearchPropertyDescription(
    name = "logsearch.http.header.access-control-allow-headers",
    description = "Access-Control-Allow-Headers header for Log Search Server.",
    examples = {"content-type, authorization"},
    defaultValue = "origin, content-type, accept, authorization",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String accessControlAllowHeaders;

  @Value("${logsearch.http.header.access-control-allow-credentials:true}")
  @LogSearchPropertyDescription(
    name = "logsearch.http.header.access-control-allow-credentials",
    description = "Access-Control-Allow-Credentials header for Log Search Server.",
    examples = {"true", "false"},
    defaultValue = "true",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String accessControlAllowCredentials;

  @Value("${logsearch.http.header.access-control-allow-methods:GET, POST, PUT, DELETE, OPTIONS, HEAD}")
  @LogSearchPropertyDescription(
    name = "logsearch.http.header.access-control-allow-methods",
    description = "Access-Control-Allow-Methods header for Log Search Server.",
    examples = {"GET, POST"},
    defaultValue = "GET, POST, PUT, DELETE, OPTIONS, HEAD",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  private String accessControlAllowMethods;

  public String getAccessControlAllowOrigin() {
    return accessControlAllowOrigin;
  }

  public void setAccessControlAllowOrigin(String accessControlAllowOrigin) {
    this.accessControlAllowOrigin = accessControlAllowOrigin;
  }

  public String getAccessControlAllowHeaders() {
    return accessControlAllowHeaders;
  }

  public void setAccessControlAllowHeaders(String accessControlAllowHeaders) {
    this.accessControlAllowHeaders = accessControlAllowHeaders;
  }

  public String getAccessControlAllowCredentials() {
    return accessControlAllowCredentials;
  }

  public void setAccessControlAllowCredentials(String accessControlAllowCredentials) {
    this.accessControlAllowCredentials = accessControlAllowCredentials;
  }

  public String getAccessControlAllowMethods() {
    return accessControlAllowMethods;
  }

  public void setAccessControlAllowMethods(String accessControlAllowMethods) {
    this.accessControlAllowMethods = accessControlAllowMethods;
  }
}
