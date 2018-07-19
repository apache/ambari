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
import org.apache.ambari.logfeeder.docker.DockerContainerRegistry;
import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.input.InputConfigUploader;
import org.apache.ambari.logfeeder.input.InputManagerImpl;
import org.apache.ambari.logfeeder.loglevelfilter.LogLevelFilterHandler;
import org.apache.ambari.logfeeder.common.ConfigHandler;
import org.apache.ambari.logfeeder.metrics.MetricsManager;
import org.apache.ambari.logfeeder.metrics.StatsLogger;
import org.apache.ambari.logfeeder.output.OutputManagerImpl;
import org.apache.ambari.logfeeder.plugin.manager.InputManager;
import org.apache.ambari.logfeeder.plugin.manager.OutputManager;
import org.apache.ambari.logsearch.config.api.LogLevelFilterManager;
import org.apache.ambari.logsearch.config.api.LogLevelFilterUpdater;
import org.apache.ambari.logsearch.config.api.LogSearchConfigFactory;
import org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder;
import org.apache.ambari.logsearch.config.local.LogSearchConfigLogFeederLocal;
import org.apache.ambari.logsearch.config.solr.LogLevelFilterManagerSolr;
import org.apache.ambari.logsearch.config.solr.LogLevelFilterUpdaterSolr;
import org.apache.ambari.logsearch.config.zookeeper.LogSearchConfigLogFeederZK;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
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
    if (logFeederProps.isUseLocalConfigs()) {
      LogSearchConfigLogFeeder logfeederConfig = LogSearchConfigFactory.createLogSearchConfigLogFeeder(
        Maps.fromProperties(logFeederProps.getProperties()),
        logFeederProps.getClusterName(),
        LogSearchConfigLogFeederLocal.class, false);
      logfeederConfig.setLogLevelFilterManager(logLevelFilterManager());
      return logfeederConfig;
    } else {
      return LogSearchConfigFactory.createLogSearchConfigLogFeeder(
        Maps.fromProperties(logFeederProps.getProperties()),
        logFeederProps.getClusterName(),
        LogSearchConfigLogFeederZK.class, false);
    }
  }

  @Bean
  public LogLevelFilterManager logLevelFilterManager() {
    if (logFeederProps.isSolrFilterStorage()) {
      if (StringUtils.isNotEmpty(logFeederProps.getSolrZkConnectString())) {
        CloudSolrClient.Builder builder = new CloudSolrClient.Builder();
        builder.withZkHost(logFeederProps.getSolrZkConnectString());
        CloudSolrClient solrClient = builder.build();
        solrClient.setDefaultCollection("history");
        return new LogLevelFilterManagerSolr(solrClient);
      } else {
        return null; // TODO: use lb http client
      }
    } else { // no default filter manager
      return null;
    }
  }

  @Bean
  @DependsOn("logLevelFilterHandler")
  public LogLevelFilterUpdater logLevelFilterUpdater() throws Exception {
    if (logFeederProps.isSolrFilterStorage() && logFeederProps.isSolrFilterMonitor()) {
      LogLevelFilterUpdater logLevelFilterUpdater = new LogLevelFilterUpdaterSolr(
        "filter-updater-solr", logLevelFilterHandler(),
        30, (LogLevelFilterManagerSolr) logLevelFilterManager(), logFeederProps.getClusterName());
      logLevelFilterUpdater.start();
      return logLevelFilterUpdater;
    } else { // no default filter updater
      return null;
    }
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
  @DependsOn("containerRegistry")
  public InputManager inputManager() {
    return new InputManagerImpl();
  }

  @Bean
  public OutputManager outputManager() {
    return new OutputManagerImpl();
  }

  @Bean
  public DockerContainerRegistry containerRegistry() {
    if (logFeederProps.isDockerContainerRegistryEnabled()) {
      return DockerContainerRegistry.getInstance(logFeederProps.getProperties());
    } else {
      return null;
    }
  }
}
