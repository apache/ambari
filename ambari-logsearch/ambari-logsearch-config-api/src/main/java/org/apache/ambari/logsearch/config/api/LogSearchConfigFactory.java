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

import java.util.Map;

import org.apache.ambari.logsearch.config.api.LogSearchConfig.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for LogSearchConfig.
 */
public class LogSearchConfigFactory {
  private static final Logger LOG = LoggerFactory.getLogger(LogSearchConfigFactory.class);

  /**
   * Creates a Log Search Configuration instance that implements {@link org.apache.ambari.logsearch.config.api.LogSearchConfig}.
   * 
   * @param component The component of the Log Search Service to create the configuration for (SERVER/LOGFEEDER).
   * @param properties The properties of the component for which the configuration is created. If the properties contain the
   *                  "logsearch.config.class" entry than the class defined there would be used instead of the default class.
   * @param clusterName The name of the cluster, only need to be specified in LOGFEEDER mode (null for SERVER mode).
   * @param defaultClass The default configuration class to use if not specified otherwise.
   * @return The Log Search Configuration instance.
   * @throws Exception Throws exception if the defined class does not implement LogSearchConfig, or doesn't have an empty
   *                   constructor, or throws an exception in it's init method.
   */
  public static LogSearchConfig createLogSearchConfig(Component component, Map<String, String> properties, String clusterName,
      Class<? extends LogSearchConfig> defaultClass) throws Exception {
    try {
      LogSearchConfig logSearchConfig = null;
      String configClassName = properties.get("logsearch.config.class");
      if (configClassName != null && !"".equals(configClassName.trim())) {
        Class<?> clazz = Class.forName(configClassName);
        if (LogSearchConfig.class.isAssignableFrom(clazz)) {
          logSearchConfig = (LogSearchConfig) clazz.newInstance();
        } else {
          throw new IllegalArgumentException("Class " + configClassName + " does not implement the interface " +
              LogSearchConfig.class.getName());
        }
      } else {
        logSearchConfig = defaultClass.newInstance();
      }
      
      logSearchConfig.init(component, properties, clusterName);
      return logSearchConfig;
    } catch (Exception e) {
      LOG.error("Could not initialize logsearch config.", e);
      throw e;
    }
  }
}
