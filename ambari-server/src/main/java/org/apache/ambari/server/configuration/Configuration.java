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
package org.apache.ambari.server.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Ambari configuration.
 * Reads properties from ambari.properties
 */
public class Configuration {
  private static final String AMBARI_CONF_VAR = "AMBARI_CONF_DIR";
  private static final String CONFIG_FILE = "ambari.properties";
  private static final String BOOTSTRAP_DIR = "bootstrap.dir";
  private static final String BOOTSTRAP_SCRIPT = "bootstrap.script";

  private static final Log LOG = LogFactory.getLog(Configuration.class);

  private Properties properties;
  
  Configuration() {
    this(readConfigFile());
  }

  /**
   * For Testing only. This is to be able to create Configuration object 
   * for testing.
   * @param properties properties to use for testing using the Conf object.
   */
  public Configuration(Properties properties) {
    this.properties = properties;
  }

  /**
   * Find, read, and parse the configuration file.
   * @return the properties that were found or empty if no file was found
   */
  private static Properties readConfigFile() {
    Properties properties = new Properties();

    // get the configuration directory and filename
    String confDir = System.getenv(AMBARI_CONF_VAR);
    if (confDir == null) {
      confDir = "/etc/ambari";
    }
    String filename = confDir + "/" + CONFIG_FILE;

    // load the properties
    try {
      properties.load(new FileInputStream(filename));
    } catch (FileNotFoundException fnf) {
      LOG.info("No configuration file " + filename + " found.", fnf);
    } catch (IOException ie) {
      throw new IllegalArgumentException("Can't read configuration file " +
                                         filename, ie);
    }
    return properties;
  }
  
  public File getBootStrapDir() {
    String fileName = properties.getProperty(BOOTSTRAP_DIR);
    if (fileName == null) {
      return new File("/var/run/ambari/bootstrap");
    }
    return new File(fileName);
  }
  
  public String getBootStrapScript() {
    String bootscript = properties.getProperty(BOOTSTRAP_SCRIPT);
    if (bootscript == null) {
      return "/usr/bin/ambari_bootstrap";
    }
    return bootscript;
  }
}
