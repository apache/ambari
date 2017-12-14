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

import com.google.common.collect.Maps;
import org.apache.ambari.logfeeder.common.ConfigHandler;
import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.input.InputConfigUploader;
import org.apache.ambari.logfeeder.input.InputManager;
import org.apache.ambari.logfeeder.loglevelfilter.LogLevelFilterHandler;
import org.apache.ambari.logfeeder.metrics.MetricsManager;
import org.apache.ambari.logfeeder.metrics.StatsLogger;
import org.apache.ambari.logfeeder.output.OutputManager;
import org.apache.ambari.logsearch.config.api.LogSearchConfigFactory;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;
import org.apache.ambari.logsearch.config.zookeeper.LogSearchConfigLogFeederZK;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import javax.inject.Inject;

@Configuration
@PropertySource(value = {
  "classpath:" + LogFeederConstants.LOGFEEDER_PROPERTIES_FILE
})
public class ApplicationConfig {

  @Inject
  private LogFeederProps logFeederProps;

  @Bean
  public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
    return new PropertySourcesPlaceholderConfigurer();
  }

  @Bean
  public LogFeederSecurityConfig logFeederSecurityConfig() {
    return new LogFeederSecurityConfig();
  }

  @Bean
  @DependsOn("logSearchConfigLogFeeder")
  public ConfigHandler configHandler() throws Exception {
    return new ConfigHandler(logSearchConfigLogFeeder());
  }

  @Bean
  @DependsOn("logFeederSecurityConfig")
  public LogSearchConfigLogFeeder logSearchConfigLogFeeder() throws Exception {
    return LogSearchConfigFactory.createLogSearchConfigLogFeeder(
      Maps.fromProperties(logFeederProps.getProperties()),
      logFeederProps.getClusterName(),
      LogSearchConfigLogFeederZK.class,false);
  }

  @Bean
  public MetricsManager metricsManager() {
    return new MetricsManager();
  }

  @Bean
  @DependsOn("configHandler")
  public LogLevelFilterHandler logLevelFilterHandler() throws Exception {
    return new LogLevelFilterHandler(logSearchConfigLogFeeder());
  }

  @Bean
  @DependsOn({"configHandler", "logSearchConfigLogFeeder", "logLevelFilterHandler"})
  public InputConfigUploader inputConfigUploader() {
    return new InputConfigUploader();
  }

  @Bean
  @DependsOn("inputConfigUploader")
  public StatsLogger statsLogger() {
    return new StatsLogger();
  }

  @Bean
  public InputManager inputManager() {
    return new InputManager();
  }

  @Bean
  public OutputManager outputManager() {
    return new OutputManager();
  }
}
