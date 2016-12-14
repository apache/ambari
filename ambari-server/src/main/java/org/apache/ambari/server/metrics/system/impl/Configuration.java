/**
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
package org.apache.ambari.server.metrics.system.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Configuration {
  public static final String CONFIG_FILE = "metrics.properties";

  private static Logger LOG = LoggerFactory.getLogger(Configuration.class);
  private Properties properties;

  public Configuration() {
      this(readConfigFile());
  }

  public Configuration(Properties properties) {
      this.properties = properties;
  }

  private static Properties readConfigFile() {
      Properties properties = new Properties();

      //Get property file stream from classpath
      InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

      if (inputStream == null) {
          throw new RuntimeException(CONFIG_FILE + " not found in classpath");
      }

      // load the properties
      try {
          properties.load(inputStream);
          inputStream.close();
      } catch (FileNotFoundException fnf) {
          LOG.info("No configuration file " + CONFIG_FILE + " found in classpath.", fnf);
      } catch (IOException ie) {
          throw new IllegalArgumentException("Can't read configuration file " +
                  CONFIG_FILE, ie);
      }

      return properties;
  }

  /**
   * Get the property value for the given key.
   *
   * @return the property value
   */
  public String getProperty(String key) {
    return properties.getProperty(key);
  }

  /**
   * Get the property value for the given key.
   *
   * @return the property value
   */
  public String getProperty(String key, String defaultValue) {
    return properties.getProperty(key, defaultValue);
  }
}
