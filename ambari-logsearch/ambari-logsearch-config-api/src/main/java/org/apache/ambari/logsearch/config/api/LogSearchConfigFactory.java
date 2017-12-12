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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for LogSearchConfigServer and LogSearchConfigLogFeeder.
 */
public class LogSearchConfigFactory {
  private static final Logger LOG = LoggerFactory.getLogger(LogSearchConfigFactory.class);

  /**
   * Creates a Log Search Configuration instance for the Log Search Server that implements
   * {@link org.apache.ambari.logsearch.config.api.LogSearchConfigServer}.
   * 
   * @param properties The properties of the component for which the configuration is created. If the properties contain the
   *                  "logsearch.config.class" entry than the class defined there would be used instead of the default class.
   * @param defaultClass The default configuration class to use if not specified otherwise.
   * @param init initialize the properties and zookeeper client
   * @return The Log Search Configuration instance.
   * @throws Exception Throws exception if the defined class does not implement LogSearchConfigServer, or doesn't have an empty
   *                   constructor, or throws an exception in it's init method.
   */
  public static LogSearchConfigServer createLogSearchConfigServer(Map<String, String> properties,
      Class<? extends LogSearchConfigServer> defaultClass, boolean init) throws Exception {
    try {
      LogSearchConfigServer logSearchConfig = null;
      String configClassName = properties.get("logsearch.config.server.class");
      if (configClassName != null && !"".equals(configClassName.trim())) {
        Class<?> clazz = Class.forName(configClassName);
        if (LogSearchConfigServer.class.isAssignableFrom(clazz)) {
          logSearchConfig = (LogSearchConfigServer) clazz.newInstance();
        } else {
          throw new IllegalArgumentException("Class " + configClassName + " does not implement the interface " +
              LogSearchConfigServer.class.getName());
        }
      } else {
        logSearchConfig = defaultClass.newInstance();
      }
      if (init) {
        logSearchConfig.init(properties);
      }
      return logSearchConfig;
    } catch (Exception e) {
      LOG.error("Could not initialize logsearch config.", e);
      throw e;
    }
  }

  /**
   * Creates a Log Search Configuration instance for the Log Search Server that implements
   * {@link org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder}.
   * 
   * @param properties The properties of the component for which the configuration is created. If the properties contain the
   *                  "logsearch.config.class" entry than the class defined there would be used instead of the default class.
   * @param clusterName The name of the cluster.
   * @param defaultClass The default configuration class to use if not specified otherwise.
   * @param init initialize the properties and zookeeper client
   * @return The Log Search Configuration instance.
   * @throws Exception Throws exception if the defined class does not implement LogSearchConfigLogFeeder, or doesn't have an empty
   *                   constructor, or throws an exception in it's init method.
   */
  public static LogSearchConfigLogFeeder createLogSearchConfigLogFeeder(Map<String, String> properties, String clusterName,
      Class<? extends LogSearchConfigLogFeeder> defaultClass, boolean init) throws Exception {
    try {
      LogSearchConfigLogFeeder logSearchConfig = null;
      String configClassName = properties.get("logsearch.config.logfeeder.class");
      if (configClassName != null && !"".equals(configClassName.trim())) {
        Class<?> clazz = Class.forName(configClassName);
        if (LogSearchConfig.class.isAssignableFrom(clazz)) {
          logSearchConfig = (LogSearchConfigLogFeeder) clazz.newInstance();
        } else {
          throw new IllegalArgumentException("Class " + configClassName + " does not implement the interface " +
              LogSearchConfigLogFeeder.class.getName());
        }
      } else {
        logSearchConfig = defaultClass.newInstance();
      }
      if (init) {
        logSearchConfig.init(properties, clusterName);
      }
      return logSearchConfig;
    } catch (Exception e) {
      LOG.error("Could not initialize logsearch config.", e);
      throw e;
    }
  }

  /**
   * Creates a Log Search Configuration instance for the Log Search Server that implements
   * {@link org.apache.ambari.logsearch.config.api.LogSearchConfigServer}.
   *
   * @param properties The properties of the component for which the configuration is created. If the properties contain the
   *                  "logsearch.config.class" entry than the class defined there would be used instead of the default class.
   * @param defaultClass The default configuration class to use if not specified otherwise.
   * @return The Log Search Configuration instance.
   * @throws Exception Throws exception if the defined class does not implement LogSearchConfigServer, or doesn't have an empty
   *                   constructor, or throws an exception in it's init method.
   */
  public static LogSearchConfigServer createLogSearchConfigServer(Map<String, String> properties,
                                                                  Class<? extends LogSearchConfigServer> defaultClass) throws Exception {
    return createLogSearchConfigServer(properties, defaultClass, true);
  }

  /**
   * Creates a Log Search Configuration instance for the Log Search Server that implements
   * {@link org.apache.ambari.logsearch.config.api.LogSearchConfigLogFeeder}.
   *
   * @param properties The properties of the component for which the configuration is created. If the properties contain the
   *                  "logsearch.config.class" entry than the class defined there would be used instead of the default class.
   * @param clusterName The name of the cluster.
   * @param defaultClass The default configuration class to use if not specified otherwise.
   * @return The Log Search Configuration instance.
   * @throws Exception Throws exception if the defined class does not implement LogSearchConfigLogFeeder, or doesn't have an empty
   *                   constructor, or throws an exception in it's init method.
   */
  public static LogSearchConfigLogFeeder createLogSearchConfigLogFeeder(Map<String, String> properties, String clusterName,
      Class<? extends LogSearchConfigLogFeeder> defaultClass) throws Exception {
    return createLogSearchConfigLogFeeder(properties, clusterName, defaultClass, true);
  }
}
