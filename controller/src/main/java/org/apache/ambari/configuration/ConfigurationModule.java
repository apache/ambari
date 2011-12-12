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
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

/**
 * Load a given property file into Guice as named properties.
 */
public class ConfigurationModule extends AbstractModule {

  private static final Log LOG = LogFactory.getLog(ConfigurationModule.class);
  private static final String AMBARI_CONF_VAR = "AMBARI_CONF_DIR";
  private static final String CONFIG_FILE = "ambari.properties";

  @Override
  protected void configure() {
    // set up default properties
    Properties properties = new Properties();
    properties.put("data.store", "zk://localhost:2181/");
    
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
      throw new RuntimeException("Can't read configuration file " + filename,
                                 ie);
    }
    Names.bindProperties(binder(), properties);
  }

}
