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

package org.apache.ambari.logfeeder.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;

import org.apache.ambari.logfeeder.LogFeeder;
import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * This class contains utility methods used by LogFeeder
 */
public class LogFeederPropertiesUtil {
  private static final Logger LOG = Logger.getLogger(LogFeederPropertiesUtil.class);

  public static final String LOGFEEDER_PROPERTIES_FILE = "logfeeder.properties";

  private static Properties props;
  public static Properties getProperties() {
    return props;
  }

  public static void loadProperties() throws Exception {
    loadProperties(LOGFEEDER_PROPERTIES_FILE);
  }
  
  /**
   * This method will read the properties from System, followed by propFile and finally from the map
   */
  public static void loadProperties(String propFile) throws Exception {
    LOG.info("Loading properties. propFile=" + propFile);
    props = new Properties(System.getProperties());
    boolean propLoaded = false;

    // First get properties file path from environment value
    String propertiesFilePath = System.getProperty("properties");
    if (StringUtils.isNotEmpty(propertiesFilePath)) {
      File propertiesFile = new File(propertiesFilePath);
      if (propertiesFile.exists() && propertiesFile.isFile()) {
        LOG.info("Properties file path set in environment. Loading properties file=" + propertiesFilePath);
        try (FileInputStream fis = new FileInputStream(propertiesFile)) {
          props.load(fis);
          propLoaded = true;
        } catch (Throwable t) {
          LOG.error("Error loading properties file. properties file=" + propertiesFile.getAbsolutePath());
        }
      } else {
        LOG.error("Properties file path set in environment, but file not found. properties file=" + propertiesFilePath);
      }
    }

    if (!propLoaded) {
      try (BufferedInputStream bis = (BufferedInputStream) LogFeeder.class.getClassLoader().getResourceAsStream(propFile)) {
        // Properties not yet loaded, let's try from class loader
        if (bis != null) {
          LOG.info("Loading properties file " + propFile + " from classpath");
          props.load(bis);
          propLoaded = true;
        } else {
          LOG.fatal("Properties file not found in classpath. properties file name= " + propFile);
        }
      }
    }

    if (!propLoaded) {
      LOG.fatal("Properties file is not loaded.");
      throw new Exception("Properties not loaded");
    }
  }

  public static String getStringProperty(String key) {
    return props == null ? null : props.getProperty(key);
  }

  public static String getStringProperty(String key, String defaultValue) {
    return props == null ? defaultValue : props.getProperty(key, defaultValue);
  }

  public static boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = getStringProperty(key);
    return toBoolean(value, defaultValue);
  }

  private static boolean toBoolean(String value, boolean defaultValue) {
    if (StringUtils.isEmpty(value)) {
      return defaultValue;
    }
    
    return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
  }

  public static int getIntProperty(String key, int defaultValue) {
    return getIntProperty(key, defaultValue, null, null);
  }

  public static int getIntProperty(String key, int defaultValue, Integer minValue, Integer maxValue) {
    String value = getStringProperty(key);
    int retValue = LogFeederUtil.objectToInt(value, defaultValue, ", key=" + key);
    if (minValue != null && retValue < minValue) {
      LOG.info("Minimum rule was applied for " + key + ": " + retValue + " < " + minValue);
      retValue = minValue;
    }
    if (maxValue != null && retValue > maxValue) {
      LOG.info("Maximum rule was applied for " + key + ": " + retValue + " > " + maxValue);
      retValue = maxValue;
    }
    return retValue;
  }

  private static final String CLUSTER_NAME_PROPERTY = "cluster.name";

  @LogSearchPropertyDescription(
    name = CLUSTER_NAME_PROPERTY,
    description = "The name of the cluster the Log Feeder program runs in.",
    examples = {"cl1"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getClusterName() {
    return getStringProperty(CLUSTER_NAME_PROPERTY);
  }

  private static final String TMP_DIR_PROPERTY = "logfeeder.tmp.dir";
  private static final String DEFAULT_TMP_DIR = "/tmp/$username/logfeeder/";
  private static String logFeederTempDir = null;
  
  @LogSearchPropertyDescription(
    name = TMP_DIR_PROPERTY,
    description = "The tmp dir used for creating temporary files.",
    examples = {"/tmp/"},
    defaultValue = DEFAULT_TMP_DIR,
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public synchronized static String getLogFeederTempDir() {
    if (logFeederTempDir == null) {
      String tempDirValue = getStringProperty(TMP_DIR_PROPERTY, DEFAULT_TMP_DIR);
      HashMap<String, String> contextParam = new HashMap<String, String>();
      String username = System.getProperty("user.name");
      contextParam.put("username", username);
      logFeederTempDir = PlaceholderUtil.replaceVariables(tempDirValue, contextParam);
    }
    return logFeederTempDir;
  }

  public static final String CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY = "hadoop.security.credential.provider.path";

  @LogSearchPropertyDescription(
    name = CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY,
    description = "The jceks file that provides passwords.",
    examples = {"jceks://file/etc/ambari-logsearch-logfeeder/conf/logfeeder.jceks"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getCredentialStoreProviderPath() {
    return getStringProperty(CREDENTIAL_STORE_PROVIDER_PATH_PROPERTY);
  }

  private static final String CONFIG_FILES_PROPERTY = "logfeeder.config.files";

  @LogSearchPropertyDescription(
    name = CONFIG_FILES_PROPERTY,
    description = "Comma separated list of the config files containing global / output configurations.",
    examples = {"global.json,output.json", "/etc/ambari-logsearch-logfeeder/conf/global.json"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getConfigFiles() {
    return getStringProperty(CONFIG_FILES_PROPERTY);
  }

  private static final String CONFIG_DIR_PROPERTY = "logfeeder.config.dir";
  
  @LogSearchPropertyDescription(
    name = CONFIG_DIR_PROPERTY,
    description = "The directory where shipper configuration files are looked for.",
    examples = {"/etc/ambari-logsearch-logfeeder/conf"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getConfigDir() {
    return getStringProperty(CONFIG_DIR_PROPERTY);
  }

  public static final String CHECKPOINT_EXTENSION_PROPERTY = "logfeeder.checkpoint.extension";
  public static final String DEFAULT_CHECKPOINT_EXTENSION = ".cp";
  
  @LogSearchPropertyDescription(
    name = CHECKPOINT_EXTENSION_PROPERTY,
    description = "The extension used for checkpoint files.",
    examples = {"ckp"},
    defaultValue = DEFAULT_CHECKPOINT_EXTENSION,
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getCheckPointExtension() {
    return getStringProperty(CHECKPOINT_EXTENSION_PROPERTY, DEFAULT_CHECKPOINT_EXTENSION);
  }

  private static final String CHECKPOINT_FOLDER_PROPERTY = "logfeeder.checkpoint.folder";
  
  @LogSearchPropertyDescription(
    name = CHECKPOINT_FOLDER_PROPERTY,
    description = "The folder wher checkpoint files are stored.",
    examples = {"/etc/ambari-logsearch-logfeeder/conf/checkpoints"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getCheckpointFolder() {
    return getStringProperty(CHECKPOINT_FOLDER_PROPERTY);
  }

  private static final String CACHE_ENABLED_PROPERTY = "logfeeder.cache.enabled";
  private static final boolean DEFAULT_CACHE_ENABLED = false;
  
  @LogSearchPropertyDescription(
    name = CACHE_ENABLED_PROPERTY,
    description = "Enables the usage of a cache to avoid duplications.",
    examples = {"true"},
    defaultValue = DEFAULT_CACHE_ENABLED + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static boolean isCacheEnabled() {
    return getBooleanProperty(CACHE_ENABLED_PROPERTY, DEFAULT_CACHE_ENABLED);
  }

  private static final String CACHE_KEY_FIELD_PROPERTY = "logfeeder.cache.key.field";
  private static final String DEFAULT_CACHE_KEY_FIELD = "log_message";
  
  @LogSearchPropertyDescription(
    name = CACHE_KEY_FIELD_PROPERTY,
    description = "The field which's value should be cached and should be checked for repteitions.",
    examples = {"some_field_prone_to_repeating_value"},
    defaultValue = DEFAULT_CACHE_KEY_FIELD,
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getCacheKeyField() {
    return getStringProperty(CACHE_KEY_FIELD_PROPERTY, DEFAULT_CACHE_KEY_FIELD);
  }

  private static final String CACHE_SIZE_PROPERTY = "logfeeder.cache.size";
  private static final int DEFAULT_CACHE_SIZE = 100;
  
  @LogSearchPropertyDescription(
    name = CACHE_SIZE_PROPERTY,
    description = "The number of log entries to cache in order to avoid duplications.",
    examples = {"50"},
    defaultValue = DEFAULT_CACHE_SIZE + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static int getCacheSize() {
    return getIntProperty(CACHE_SIZE_PROPERTY, DEFAULT_CACHE_SIZE);
  }

  private static final String CACHE_LAST_DEDUP_ENABLED_PROPERTY = "logfeeder.cache.last.dedup.enabled";
  private static final boolean DEFAULT_CACHE_LAST_DEDUP_ENABLED = false;
  
  @LogSearchPropertyDescription(
    name = CACHE_LAST_DEDUP_ENABLED_PROPERTY,
    description = "Enable filtering directly repeating log entries irrelevant of the time spent between them.",
    examples = {"true"},
    defaultValue = DEFAULT_CACHE_LAST_DEDUP_ENABLED + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static boolean isCacheLastDedupEnabled() {
    return getBooleanProperty(CACHE_LAST_DEDUP_ENABLED_PROPERTY, DEFAULT_CACHE_LAST_DEDUP_ENABLED);
  }

  private static final String CACHE_DEDUP_INTERVAL_PROPERTY = "logfeeder.cache.dedup.interval";
  private static final long DEFAULT_CACHE_DEDUP_INTERVAL = 1000;
  
  @LogSearchPropertyDescription(
    name = CACHE_DEDUP_INTERVAL_PROPERTY,
    description = "Maximum number of milliseconds between two identical messages to be filtered out.",
    examples = {"500"},
    defaultValue = DEFAULT_CACHE_DEDUP_INTERVAL + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getCacheDedupInterval() {
    return getStringProperty(CACHE_DEDUP_INTERVAL_PROPERTY, String.valueOf(DEFAULT_CACHE_DEDUP_INTERVAL));
  }

  private static final String LOG_FILTER_ENABLE_PROPERTY = "logfeeder.log.filter.enable";
  private static final boolean DEFAULT_LOG_FILTER_ENABLE = false;
  
  @LogSearchPropertyDescription(
    name = LOG_FILTER_ENABLE_PROPERTY,
    description = "Enables the filtering of the log entries by log level filters.",
    examples = {"true"},
    defaultValue = DEFAULT_LOG_FILTER_ENABLE + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static boolean isLogFilterEnabled() {
    return getBooleanProperty(LOG_FILTER_ENABLE_PROPERTY, DEFAULT_LOG_FILTER_ENABLE);
  }

  private static final String INCLUDE_DEFAULT_LEVEL_PROPERTY = "logfeeder.include.default.level";
  
  @LogSearchPropertyDescription(
    name = INCLUDE_DEFAULT_LEVEL_PROPERTY,
    description = "Comma separtaed list of the default log levels to be enabled by the filtering.",
    examples = {"FATAL,ERROR,WARN"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getIncludeDefaultLevel() {
    return getStringProperty(INCLUDE_DEFAULT_LEVEL_PROPERTY);
  }

  private static final String DEFAULT_SOLR_JAAS_FILE = "/etc/security/keytabs/logsearch_solr.service.keytab";
  private static final String SOLR_JAAS_FILE_PROPERTY = "logfeeder.solr.jaas.file";
  
  @LogSearchPropertyDescription(
    name = SOLR_JAAS_FILE_PROPERTY,
    description = "The jaas file used for solr.",
    examples = {"/etc/ambari-logsearch-logfeeder/conf/logfeeder_jaas.conf"},
    defaultValue = DEFAULT_SOLR_JAAS_FILE,
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getSolrJaasFile() {
    return getStringProperty(SOLR_JAAS_FILE_PROPERTY, DEFAULT_SOLR_JAAS_FILE);
  }

  private static final String SOLR_KERBEROS_ENABLE_PROPERTY = "logfeeder.solr.kerberos.enable";
  private static final boolean DEFAULT_SOLR_KERBEROS_ENABLE = false;
  
  @LogSearchPropertyDescription(
    name = SOLR_KERBEROS_ENABLE_PROPERTY,
    description = "Enables using kerberos for accessing solr.",
    examples = {"true"},
    defaultValue = DEFAULT_SOLR_KERBEROS_ENABLE + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static boolean isSolrKerberosEnabled() {
    return getBooleanProperty(SOLR_KERBEROS_ENABLE_PROPERTY, DEFAULT_SOLR_KERBEROS_ENABLE);
  }

  private static final String METRICS_COLLECTOR_HOSTS_PROPERTY = "logfeeder.metrics.collector.hosts";
  
  @LogSearchPropertyDescription(
    name = METRICS_COLLECTOR_HOSTS_PROPERTY,
    description = "Comma separtaed list of metric collector hosts.",
    examples = {"c6401.ambari.apache.org"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getMetricsCollectorHosts() {
    return getStringProperty(METRICS_COLLECTOR_HOSTS_PROPERTY);
  }

  private static final String METRICS_COLLECTOR_PROTOCOL_PROPERTY = "logfeeder.metrics.collector.protocol";
  
  @LogSearchPropertyDescription(
    name = METRICS_COLLECTOR_PROTOCOL_PROPERTY,
    description = "The protocol used by metric collectors.",
    examples = {"http", "https"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getMetricsCollectorProtocol() {
    return getStringProperty(METRICS_COLLECTOR_PROTOCOL_PROPERTY);
  }

  private static final String METRICS_COLLECTOR_PORT_PROPERTY = "logfeeder.metrics.collector.port";
  
  @LogSearchPropertyDescription(
    name = METRICS_COLLECTOR_PORT_PROPERTY,
    description = "The port used by metric collectors.",
    examples = {"6188"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getMetricsCollectorPort() {
    return getStringProperty(METRICS_COLLECTOR_PORT_PROPERTY);
  }

  private static final String METRICS_COLLECTOR_PATH_PROPERTY = "logfeeder.metrics.collector.path";
  
  @LogSearchPropertyDescription(
    name = METRICS_COLLECTOR_PATH_PROPERTY,
    description = "The path used by metric collectors.",
    examples = {"/ws/v1/timeline/metrics"},
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getMetricsCollectorPath() {
    return getStringProperty(METRICS_COLLECTOR_PATH_PROPERTY);
  }

  private static final String SIMULATE_INPUT_NUMBER_PROPERTY = "logfeeder.simulate.input_number";
  private static final int DEFAULT_SIMULATE_INPUT_NUMBER = 0;

  @LogSearchPropertyDescription(
    name = SIMULATE_INPUT_NUMBER_PROPERTY,
    description = "The number of the simulator instances to run with. O means no simulation.",
    examples = {"10"},
    defaultValue = DEFAULT_SIMULATE_INPUT_NUMBER + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static int getSimulateInputNumber() {
    return getIntProperty(SIMULATE_INPUT_NUMBER_PROPERTY, DEFAULT_SIMULATE_INPUT_NUMBER);
  }
  
  private static final String SIMULATE_LOG_LEVEL_PROPERTY = "logfeeder.simulate.log_level";
  private static final String DEFAULT_SIMULATE_LOG_LEVEL = "WARN";

  @LogSearchPropertyDescription(
    name = SIMULATE_LOG_LEVEL_PROPERTY,
    description = "The log level to create the simulated log entries with.",
    examples = {"INFO"},
    defaultValue = DEFAULT_SIMULATE_LOG_LEVEL,
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getSimulateLogLevel() {
    return getStringProperty(SIMULATE_LOG_LEVEL_PROPERTY, DEFAULT_SIMULATE_LOG_LEVEL);
  }

  private static final String SIMULATE_NUMBER_OF_WORDS_PROPERTY = "logfeeder.simulate.number_of_words";
  private static final int DEFAULT_SIMULATE_NUMBER_OF_WORDS = 1000;

  @LogSearchPropertyDescription(
    name = SIMULATE_NUMBER_OF_WORDS_PROPERTY,
    description = "The size of the set of words that may be used to create the simulated log entries with.",
    examples = {"100"},
    defaultValue = DEFAULT_SIMULATE_NUMBER_OF_WORDS + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static int getSimulateNumberOfWords() {
    return getIntProperty(SIMULATE_NUMBER_OF_WORDS_PROPERTY, DEFAULT_SIMULATE_NUMBER_OF_WORDS, 50, 1000000);
  }

  private static final String SIMULATE_MIN_LOG_WORDS_PROPERTY = "logfeeder.simulate.min_log_words";
  private static final int DEFAULT_SIMULATE_MIN_LOG_WORDS = 5;

  @LogSearchPropertyDescription(
    name = SIMULATE_MIN_LOG_WORDS_PROPERTY,
    description = "The minimum number of words in a simulated log entry.",
    examples = {"3"},
    defaultValue = DEFAULT_SIMULATE_MIN_LOG_WORDS + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static int getSimulateMinLogWords() {
    return getIntProperty(SIMULATE_MIN_LOG_WORDS_PROPERTY, DEFAULT_SIMULATE_MIN_LOG_WORDS, 1, 10);
  }

  private static final String SIMULATE_MAX_LOG_WORDS_PROPERTY = "logfeeder.simulate.max_log_words";
  private static final int DEFAULT_SIMULATE_MAX_LOG_WORDS = 5;

  @LogSearchPropertyDescription(
    name = SIMULATE_MAX_LOG_WORDS_PROPERTY,
    description = "The maximum number of words in a simulated log entry.",
    examples = {"8"},
    defaultValue = DEFAULT_SIMULATE_MAX_LOG_WORDS + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static int getSimulateMaxLogWords() {
    return getIntProperty(SIMULATE_MAX_LOG_WORDS_PROPERTY, DEFAULT_SIMULATE_MAX_LOG_WORDS, 10, 20);
  }

  private static final String SIMULATE_SLEEP_MILLISECONDS_PROPERTY = "logfeeder.simulate.sleep_milliseconds";
  private static final int DEFAULT_SIMULATE_SLEEP_MILLISECONDS = 10000;

  @LogSearchPropertyDescription(
    name = SIMULATE_SLEEP_MILLISECONDS_PROPERTY,
    description = "The milliseconds to sleep between creating two simulated log entries.",
    examples = {"5000"},
    defaultValue = DEFAULT_SIMULATE_SLEEP_MILLISECONDS + "",
    sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static int getSimulateSleepMilliseconds() {
    return getIntProperty(SIMULATE_SLEEP_MILLISECONDS_PROPERTY, DEFAULT_SIMULATE_SLEEP_MILLISECONDS);
  }

  private static final String SIMULATE_LOG_IDS_PROPERTY = "logfeeder.simulate.log_ids";
  
  @LogSearchPropertyDescription(
      name = SIMULATE_LOG_IDS_PROPERTY,
      description = "The comma separated list of log ids for which to create the simulated log entries.",
      examples = {"ambari_server,zookeeper,infra_solr,logsearch_app"},
      defaultValue = "The log ids of the installed services in the cluster",
      sources = {LOGFEEDER_PROPERTIES_FILE}
  )
  public static String getSimulateLogIds() {
    return getStringProperty(SIMULATE_LOG_IDS_PROPERTY);
  }

}
