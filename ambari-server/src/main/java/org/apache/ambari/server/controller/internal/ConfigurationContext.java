/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.controller.internal;

import java.util.Map;

import org.apache.ambari.server.controller.StackV2;
import org.apache.ambari.server.topology.Configuration;

/**
 * Provides a context for configuration.
 */
public class ConfigurationContext  {

  private final Configuration configuration;

  private final StackV2 stack;

  public ConfigurationContext(StackV2 stack, Configuration configuration){
    this.stack = stack;
    this.configuration = configuration;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public StackV2 getStack() {
    return stack;
  }

  public boolean isNameNodeHAEnabled() {
    Map<String, Map<String, String>> configurationProperties = getConfiguration().getProperties();
    return configurationProperties.containsKey("hdfs-site") &&
      (configurationProperties.get("hdfs-site").containsKey("dfs.nameservices") ||
        configurationProperties.get("hdfs-site").containsKey("dfs.internal.nameservices"));
  }

  public boolean isYarnResourceManagerHAEnabled() {
    Map<String, Map<String, String>> configProperties = getConfiguration().getProperties();
    return configProperties.containsKey("yarn-site") && configProperties.get("yarn-site").containsKey("yarn.resourcemanager.ha.enabled")
      && configProperties.get("yarn-site").get("yarn.resourcemanager.ha.enabled").equals("true");
  }
}
