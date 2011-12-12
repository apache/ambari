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
package org.apache.ambari.configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;

/**
 * Ambari configuration.
 * Reads properties from ambari.properties
 */
public class Configuration {
  private static final String AMBARI_CONF_VAR = "AMBARI_CONF_DIR";
  private static final String CONFIG_FILE = "ambari.properties";
  
  private static final Log LOG = LogFactory.getLog(Configuration.class);
  
  private final URI dataStore;
  
  @Inject
  Configuration() {
    this(readConfigFile());
  }
  
  protected Configuration(Properties properties) {
    // get the data store
    String dataStoreString = properties.getProperty("data.store", 
                                                    "zk://localhost:2181/");
    try {
      dataStore = new URI(dataStoreString);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Can't parse data.store: " + 
                                         dataStoreString, e);
    }    
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

  /**
   * Get the URI for the persistent data store.
   * @return the data store URI
   */
  public URI getDataStore() {
    return dataStore;
  }
}
