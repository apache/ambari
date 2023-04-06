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

package org.apache.ambari.logsearch.config.api;

import java.util.List;
import java.util.Map;

import org.apache.ambari.logsearch.config.api.model.inputconfig.InputConfig;

public class LogSearchConfigServerClass2 implements LogSearchConfigServer {
  @Override
  public void init(Map<String, String> properties) {}

  @Override
  public boolean inputConfigExists(String clusterName, String serviceName) throws Exception {
    return false;
  }

  @Override
  public void createInputConfig(String clusterName, String serviceName, String inputConfig) throws Exception {}

  @Override
  public LogLevelFilterManager getLogLevelFilterManager() {
    return null;
  }

  @Override
  public void setLogLevelFilterManager(LogLevelFilterManager logLevelFilterManager) {
  }

  @Override
  public void setInputConfig(String clusterName, String serviceName, String inputConfig) throws Exception {}

  @Override
  public List<String> getServices(String clusterName) {
    return null;
  }

  @Override
  public String getGlobalConfigs(String clusterName) {
    return null;
  }

  @Override
  public InputConfig getInputConfig(String clusterName, String serviceName) {
    return null;
  }

  @Override
  public void close() {}
}