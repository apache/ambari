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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Singleton;


/**
 * Ambari configuration.
 * Reads properties from ambari.properties
 */
@Singleton
public class Configuration {

  private static final String AMBARI_CONF_VAR = "AMBARI_CONF_DIR";
  private static final String CONFIG_FILE = "ambari.properties";
  public static final String BOOTSTRAP_DIR = "bootstrap.dir";
  public static final String BOOTSTRAP_SCRIPT = "bootstrap.script";
  public static final String SRVR_KSTR_DIR_KEY = "security.server.keys_dir";
  public static final String SRVR_CRT_NAME_KEY = "security.server.cert_name";
  public static final String SRVR_KEY_NAME_KEY = "security.server.key_name";
  public static final String KSTR_NAME_KEY = "security.server.keystore_name";
  public static final String SRVR_CRT_PASS_FILE_KEY = "security.server.crt_pass_file";
  public static final String SRVR_CRT_PASS_KEY = "security.server.crt_pass";
  public static final String CLIENT_SECURITY_KEY = "client.security";
  private static final String SRVR_KSTR_DIR_DEFAULT = ".";
  private static final String SRVR_CRT_NAME_DEFAULT = "ca.crt";
  private static final String SRVR_KEY_NAME_DEFAULT = "ca.key";
  private static final String KSTR_NAME_DEFAULT = "keystore.p12";
  private static final String SRVR_CRT_PASS_FILE_DEFAULT ="pass.txt";
  private static final String CLIENT_SECURITY_DEFAULT = "local";



  
  private static final Log LOG = LogFactory.getLog(Configuration.class);

  private static Configuration instance;

  private Properties properties;


  private Map<String, String> configsMap;


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

    configsMap = new HashMap<String, String>();
    configsMap.put(SRVR_KSTR_DIR_KEY, properties.getProperty(SRVR_KSTR_DIR_KEY, SRVR_KSTR_DIR_DEFAULT));
    configsMap.put(SRVR_KSTR_DIR_KEY, properties.getProperty(SRVR_KSTR_DIR_KEY, SRVR_KSTR_DIR_DEFAULT));
    configsMap.put(SRVR_CRT_NAME_KEY, properties.getProperty(SRVR_CRT_NAME_KEY, SRVR_CRT_NAME_DEFAULT));
    configsMap.put(SRVR_KEY_NAME_KEY, properties.getProperty(SRVR_KEY_NAME_KEY, SRVR_KEY_NAME_DEFAULT));
    configsMap.put(KSTR_NAME_KEY, properties.getProperty(KSTR_NAME_KEY, KSTR_NAME_DEFAULT));
    configsMap.put(SRVR_CRT_PASS_FILE_KEY, properties.getProperty(SRVR_CRT_PASS_FILE_KEY, SRVR_CRT_PASS_FILE_DEFAULT));
    configsMap.put(CLIENT_SECURITY_KEY, properties.getProperty(CLIENT_SECURITY_KEY, CLIENT_SECURITY_DEFAULT));

    try {
        File passFile = new File(configsMap.get(SRVR_KSTR_DIR_KEY) + File.separator 
            + configsMap.get(SRVR_CRT_PASS_FILE_KEY));
        if (passFile.exists()) {
          String srvrCrtPass = FileUtils.readFileToString(passFile);
          configsMap.put(SRVR_CRT_PASS_KEY, srvrCrtPass.trim());
        } else {
          LOG.info("Not found pass file at " + passFile);
        }
      } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException("Error reading certificate password from file");
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
  
  /**
   * Get the map with server config parameters.
   * Keys - public constants of this class
   * @return the map with server config parameters
   */
  public Map<String, String> getConfigsMap() {
    return configsMap;
  }

}
