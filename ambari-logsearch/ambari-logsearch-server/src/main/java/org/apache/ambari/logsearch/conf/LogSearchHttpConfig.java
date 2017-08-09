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
public class LogSearchHttpConfig {

  @LogSearchPropertyDescription(
    name = "logsearch.http.port",
    description = "Log Search http port",
    examples = {"61888", "8888"},
    defaultValue = "61888",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.http.port:61888}")
  private int httpPort;

  @LogSearchPropertyDescription(
    name = "logsearch.https.port",
    description = "Log Search https port",
    examples = {"61889", "8889"},
    defaultValue = "61889",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.https.port:61889}")
  private int httpsPort;

  @LogSearchPropertyDescription(
    name = "logsearch.protocol",
    description = "Log Search Protocol (http or https)",
    examples = {"http", "https"},
    defaultValue = "http",
    sources = {LOGSEARCH_PROPERTIES_FILE}
  )
  @Value("${logsearch.protocol:http}")
  private String protocol;

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public int getHttpPort() {
    return httpPort;
  }

  public void setHttpPort(int httpPort) {
    this.httpPort = httpPort;
  }

  public int getHttpsPort() {
    return httpsPort;
  }

  public void setHttpsPort(int httpsPort) {
    this.httpsPort = httpsPort;
  }
}
