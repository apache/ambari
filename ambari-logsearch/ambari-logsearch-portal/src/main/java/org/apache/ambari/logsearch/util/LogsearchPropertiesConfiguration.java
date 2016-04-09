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
package org.apache.ambari.logsearch.util;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

public class LogsearchPropertiesConfiguration extends PropertiesConfiguration {

  Logger logger = Logger.getLogger(LogsearchPropertiesConfiguration.class);

  public LogsearchPropertiesConfiguration() {
    super();
  }


  public static LogsearchPropertiesConfiguration getInstance() {
    return new LogsearchPropertiesConfiguration();
  }

  public void load(File file) {
    if (!file.exists()) {
      logger.error("File :" + file.getAbsolutePath() + " not exists");
      return;
    }
    try {
      super.load(file);
    } catch (ConfigurationException e) {
      logger.error(e);
    }
  }

  public void load(String fileAbsolutePath) {
    File file = new File(fileAbsolutePath);
    load(file);
  }

  /**
   * Load from classPath
   *
   * @param fileName
   */
  public void loadFromClasspath(String fileName) {
    logger.debug("loading config properties : " + fileName);
    // load file from classpath
    try {
      URL fileCompleteUrl = Thread.currentThread()
        .getContextClassLoader().getResource(fileName);
      logger.debug("File Complete URI :" + fileCompleteUrl);
      File file = new File(fileCompleteUrl.toURI());
      load(file);
    } catch (Exception e) {
      logger.error(e);
    }
  }

  public HashMap<String, Object> getPropertyMap() {
    HashMap<String, Object> propertyMap = new HashMap<String, Object>();
    Iterator<String> keys = this.getKeys();
    while (keys.hasNext()) {
      String key = keys.next();
      propertyMap.put(key, this.getProperty(key));
    }
    return propertyMap;
  }

}
