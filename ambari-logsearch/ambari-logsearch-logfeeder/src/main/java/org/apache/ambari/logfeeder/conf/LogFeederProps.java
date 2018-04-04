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

import org.apache.ambari.logfeeder.common.LogFeederConstants;
import org.apache.ambari.logfeeder.plugin.common.LogFeederProperties;
import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.support.ResourcePropertySource;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

@Configuration
public class LogFeederProps implements LogFeederProperties {

  @Inject
  private Environment env;

  private Properties properties;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CLUSTER_NAME_PROPERTY,
    description = "The name of the cluster the Log Feeder program runs in.",
    examples = {"cl1"},
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.CLUSTER_NAME_PROPERTY + "}")
  private String clusterName;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.TMP_DIR_PROPERTY,
    description = "The tmp dir used for creating temporary files.",
    examples = {"/tmp/"},
    defaultValue = "java.io.tmpdir",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${"+ LogFeederConstants.TMP_DIR_PROPERTY + ":#{systemProperties['java.io.tmpdir']}}")
  private String tmpDir;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.LOG_FILTER_ENABLE_PROPERTY,
    description = "Enables the filtering of the log entries by log level filters.",
    examples = {"true"},
    defaultValue = "false",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${"+ LogFeederConstants.LOG_FILTER_ENABLE_PROPERTY + "}")
  private boolean logLevelFilterEnabled;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SOLR_IMPLICIT_ROUTING_PROPERTY,
    description = "Use implicit routing for Solr Collections.",
    examples = {"true"},
    defaultValue = "false",
    sources = {LogFeederConstants.SOLR_IMPLICIT_ROUTING_PROPERTY}
  )
  @Value("${"+ LogFeederConstants.SOLR_IMPLICIT_ROUTING_PROPERTY + ":false}")
  private boolean solrImplicitRouting;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.INCLUDE_DEFAULT_LEVEL_PROPERTY,
    description = "Comma separated list of the default log levels to be enabled by the filtering.",
    examples = {"FATAL,ERROR,WARN"},
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("#{'${" + LogFeederConstants.INCLUDE_DEFAULT_LEVEL_PROPERTY + ":}'.split(',')}")
  private List<String> includeDefaultLogLevels;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CONFIG_DIR_PROPERTY,
    description = "The directory where shipper configuration files are looked for.",
    examples = {"/usr/lib/ambari-logsearch-logfeeder/conf"},
    defaultValue = "/usr/lib/ambari-logsearch-logfeeder/conf",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${"+ LogFeederConstants.CONFIG_DIR_PROPERTY + ":/usr/lib/ambari-logsearch-logfeeder/conf}")
  private String confDir;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CONFIG_FILES_PROPERTY,
    description = "Comma separated list of the config files containing global / output configurations.",
    examples = {"global.json,output.json", "/usr/lib/ambari-logsearch-logfeeder/conf/global.config.json"},
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${"+ LogFeederConstants.CONFIG_FILES_PROPERTY + ":}")
  private String configFiles;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CHECKPOINT_EXTENSION_PROPERTY,
    description = "The extension used for checkpoint files.",
    examples = {"ckp"},
    defaultValue = LogFeederConstants.DEFAULT_CHECKPOINT_EXTENSION,
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.CHECKPOINT_EXTENSION_PROPERTY + ":" + LogFeederConstants.DEFAULT_CHECKPOINT_EXTENSION + "}")
  private String checkPointExtension;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.CHECKPOINT_FOLDER_PROPERTY,
    description = "The folder where checkpoint files are stored.",
    examples = {"/usr/lib/ambari-logsearch-logfeeder/conf/checkpoints"},
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.CHECKPOINT_FOLDER_PROPERTY + ":/usr/lib/ambari-logsearch-logfeeder/conf/checkpoints}")
  public String checkpointFolder;

  @Inject
  private LogEntryCacheConfig logEntryCacheConfig;

  @Inject
  private InputSimulateConfig inputSimulateConfig;

  @Inject
  private LogFeederSecurityConfig logFeederSecurityConfig;

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public Properties getProperties() {
    return properties;
  }

  public String getTmpDir() {
    return tmpDir;
  }

  public boolean isLogLevelFilterEnabled() {
    return logLevelFilterEnabled;
  }

  public List<String> getIncludeDefaultLogLevels() {
    return includeDefaultLogLevels;
  }

  public String getConfDir() {
    return confDir;
  }

  public void setConfDir(String confDir) {
    this.confDir = confDir;
  }

  public String getConfigFiles() {
    return configFiles;
  }

  public void setConfigFiles(String configFiles) {
    this.configFiles = configFiles;
  }

  public LogEntryCacheConfig getLogEntryCacheConfig() {
    return logEntryCacheConfig;
  }

  public void setLogEntryCacheConfig(LogEntryCacheConfig logEntryCacheConfig) {
    this.logEntryCacheConfig = logEntryCacheConfig;
  }

  public InputSimulateConfig getInputSimulateConfig() {
    return inputSimulateConfig;
  }

  public void setInputSimulateConfig(InputSimulateConfig inputSimulateConfig) {
    this.inputSimulateConfig = inputSimulateConfig;
  }

  public LogFeederSecurityConfig getLogFeederSecurityConfig() {
    return logFeederSecurityConfig;
  }

  public void setLogFeederSecurityConfig(LogFeederSecurityConfig logFeederSecurityConfig) {
    this.logFeederSecurityConfig = logFeederSecurityConfig;
  }

  public String getCheckPointExtension() {
    return checkPointExtension;
  }

  public void setCheckPointExtension(String checkPointExtension) {
    this.checkPointExtension = checkPointExtension;
  }

  public String getCheckpointFolder() {
    return checkpointFolder;
  }

  public void setCheckpointFolder(String checkpointFolder) {
    this.checkpointFolder = checkpointFolder;
  }

  public boolean isSolrImplicitRouting() {
    return solrImplicitRouting;
  }

  public void setSolrImplicitRouting(boolean solrImplicitRouting) {
    this.solrImplicitRouting = solrImplicitRouting;
  }

  @PostConstruct
  public void init() {
    properties = new Properties();
    MutablePropertySources propSrcs = ((AbstractEnvironment) env).getPropertySources();
    ResourcePropertySource propertySource = (ResourcePropertySource) propSrcs.get("class path resource [" +
      LogFeederConstants.LOGFEEDER_PROPERTIES_FILE + "]");
    if (propertySource != null) {
      Stream.of(propertySource)
        .map(MapPropertySource::getPropertyNames)
        .flatMap(Arrays::<String>stream)
        .forEach(propName -> properties.setProperty(propName, env.getProperty(propName)));
    } else {
      throw new IllegalArgumentException("Cannot find logfeeder.properties on the classpath");
    }
  }
}
