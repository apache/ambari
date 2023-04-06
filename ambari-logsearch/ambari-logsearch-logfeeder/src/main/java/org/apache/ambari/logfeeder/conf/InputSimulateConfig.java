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
import org.apache.ambari.logsearch.config.api.LogSearchPropertyDescription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@Lazy
public class InputSimulateConfig {

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SIMULATE_INPUT_NUMBER_PROPERTY,
    description = "The number of the simulator instances to run with. O means no simulation.",
    examples = {"10"},
    defaultValue = LogFeederConstants.DEFAULT_SIMULATE_INPUT_NUMBER + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${"+ LogFeederConstants.SIMULATE_INPUT_NUMBER_PROPERTY + ":0}")
  private Integer simulateInputNumber;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SIMULATE_LOG_LEVEL_PROPERTY,
    description = "The log level to create the simulated log entries with.",
    examples = {"INFO"},
    defaultValue = LogFeederConstants.DEFAULT_SIMULATE_LOG_LEVEL,
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${"+ LogFeederConstants.SIMULATE_LOG_LEVEL_PROPERTY + ":"+ LogFeederConstants.DEFAULT_SIMULATE_LOG_LEVEL + "}")
  private String simulateLogLevel;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SIMULATE_NUMBER_OF_WORDS_PROPERTY,
    description = "The size of the set of words that may be used to create the simulated log entries with.",
    examples = {"100"},
    defaultValue = LogFeederConstants.DEFAULT_SIMULATE_NUMBER_OF_WORDS + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.SIMULATE_NUMBER_OF_WORDS_PROPERTY + ":" + LogFeederConstants.DEFAULT_SIMULATE_NUMBER_OF_WORDS + "}")
  private Integer simulateNumberOfWords;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SIMULATE_MIN_LOG_WORDS_PROPERTY,
    description = "The minimum number of words in a simulated log entry.",
    examples = {"3"},
    defaultValue = LogFeederConstants.DEFAULT_SIMULATE_MIN_LOG_WORDS + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.SIMULATE_MIN_LOG_WORDS_PROPERTY + ":" + LogFeederConstants.DEFAULT_SIMULATE_MIN_LOG_WORDS + "}")
  private Integer simulateMinLogWords;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SIMULATE_MAX_LOG_WORDS_PROPERTY,
    description = "The maximum number of words in a simulated log entry.",
    examples = {"8"},
    defaultValue = LogFeederConstants.DEFAULT_SIMULATE_MAX_LOG_WORDS + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.SIMULATE_MAX_LOG_WORDS_PROPERTY + ":" + LogFeederConstants.DEFAULT_SIMULATE_MAX_LOG_WORDS + "}")
  private Integer simulateMaxLogWords;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SIMULATE_SLEEP_MILLISECONDS_PROPERTY,
    description = "The milliseconds to sleep between creating two simulated log entries.",
    examples = {"5000"},
    defaultValue = LogFeederConstants.DEFAULT_SIMULATE_SLEEP_MILLISECONDS + "",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.SIMULATE_SLEEP_MILLISECONDS_PROPERTY + ":" + LogFeederConstants.DEFAULT_SIMULATE_SLEEP_MILLISECONDS + "}")
  private Integer simulateSleepMilliseconds;

  @LogSearchPropertyDescription(
    name = LogFeederConstants.SIMULATE_LOG_IDS_PROPERTY,
    description = "The comma separated list of log ids for which to create the simulated log entries.",
    examples = {"ambari_server,zookeeper,infra_solr,logsearch_app"},
    defaultValue = "The log ids of the installed services in the cluster",
    sources = {LogFeederConstants.LOGFEEDER_PROPERTIES_FILE}
  )
  @Value("${" + LogFeederConstants.SIMULATE_LOG_IDS_PROPERTY + ":}")
  private String simulateLogIds;

  public Integer getSimulateInputNumber() {
    return simulateInputNumber;
  }

  public void setSimulateInputNumber(Integer simulateInputNumber) {
    this.simulateInputNumber = simulateInputNumber;
  }

  public String getSimulateLogLevel() {
    return simulateLogLevel;
  }

  public void setSimulateLogLevel(String simulateLogLevel) {
    this.simulateLogLevel = simulateLogLevel;
  }

  public Integer getSimulateNumberOfWords() {
    return simulateNumberOfWords;
  }

  public void setSimulateNumberOfWords(Integer simulateNumberOfWords) {
    this.simulateNumberOfWords = simulateNumberOfWords;
  }

  public Integer getSimulateMinLogWords() {
    return simulateMinLogWords;
  }

  public void setSimulateMinLogWords(Integer simulateMinLogWords) {
    this.simulateMinLogWords = simulateMinLogWords;
  }

  public Integer getSimulateMaxLogWords() {
    return simulateMaxLogWords;
  }

  public void setSimulateMaxLogWords(Integer simulateMaxLogWords) {
    this.simulateMaxLogWords = simulateMaxLogWords;
  }

  public Integer getSimulateSleepMilliseconds() {
    return simulateSleepMilliseconds;
  }

  public void setSimulateSleepMilliseconds(Integer simulateSleepMilliseconds) {
    this.simulateSleepMilliseconds = simulateSleepMilliseconds;
  }

  public String getSimulateLogIds() {
    return simulateLogIds;
  }

  public void setSimulateLogIds(String simulateLogIds) {
    this.simulateLogIds = simulateLogIds;
  }
}
