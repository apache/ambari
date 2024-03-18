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
package org.apache.ambari.logfeeder.conf;

import com.google.common.base.Splitter;
import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.PostConstruct;
import java.util.List;

@Configuration
@Lazy
public class MetricsCollectorConfig {

  @LogSearchPropertyDescription(
    name = LogFeederConstants.METRICS_COLLECTOR_HOSTS_PROPERTY,
    description = "Comma separtaed list of metric collector hosts.",
    examples = {"c6401.ambari.apache.org,c6402.ambari.apache.org"},
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.METRICS_COLLECTOR_HOSTS_PROPERTY + ":}")
  private String hostsString;

  private List<String> hosts;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.METRICS_COLLECTOR_PROTOCOL_PROPERTY,
    description = "The protocol used by metric collectors.",
    examples = {"http", "https"},
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.METRICS_COLLECTOR_PROTOCOL_PROPERTY + ":#{NULL}}")
  private String protocol;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.METRICS_COLLECTOR_PORT_PROPERTY,
    description = "The port used by metric collectors.",
    examples = {"6188"},
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.METRICS_COLLECTOR_PORT_PROPERTY + ":#{NULL}}")
  private String port;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.METRICS_COLLECTOR_PATH_PROPERTY,
    description = "The path used by metric collectors.",
    examples = {"/ws/v1/timeline/metrics"},
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.METRICS_COLLECTOR_PATH_PROPERTY + ":#{NULL}}")
  private String path;

  public List<String> getHosts() {
    return hosts;
  }

  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }

  public String getProtocol() {
    return protocol;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getHostsString() {
    return hostsString;
  }

  @PostConstruct
  public void init() {
    if (StringUtils.isNotBlank(hostsString)) {
      hosts = Splitter.on(',').splitToList(hostsString);
    } else {
      hosts = null;
    }
  }

}
