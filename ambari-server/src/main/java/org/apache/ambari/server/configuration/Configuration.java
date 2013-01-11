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

import com.google.inject.Singleton;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.security.ClientSecurityType;
import org.apache.ambari.server.security.authorization.LdapServerProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * Ambari configuration.
 * Reads properties from ambari.properties
 */
@Singleton
public class Configuration {

  public static final String CONFIG_FILE = "ambari.properties";
  public static final String BOOTSTRAP_DIR = "bootstrap.dir";
  public static final String BOOTSTRAP_DIR_DEFAULT = "/var/run/ambari-server/bootstrap";
  public static final String WEBAPP_DIR = "webapp.dir";
  public static final String BOOTSTRAP_SCRIPT = "bootstrap.script";
    public static final String BOOTSTRAP_SCRIPT_DEFAULT =  "/usr/bin/ambari_bootstrap";
  public static final String BOOTSTRAP_SETUP_AGENT_SCRIPT = "bootstrap.setup_agent.script";
  public static final String BOOTSTRAP_SETUP_AGENT_PASSWORD = "bootstrap.setup_agent.password";
  public static final String BOOTSTRAP_MASTER_HOSTNAME = "bootstrap.master_host_name";
  public static final String API_AUTHENTICATE = "api.authenticate";
  public static final String API_USE_SSL = "api.ssl";
  public static final String SRVR_KSTR_DIR_KEY = "security.server.keys_dir";
  public static final String SRVR_CRT_NAME_KEY = "security.server.cert_name";
  public static final String SRVR_KEY_NAME_KEY = "security.server.key_name";
  public static final String KSTR_NAME_KEY =
      "security.server.keystore_name";
  public static final String SRVR_CRT_PASS_FILE_KEY =
      "security.server.crt_pass_file";
  public static final String SRVR_CRT_PASS_KEY = "security.server.crt_pass";
  public static final String SRVR_CRT_PASS_LEN_KEY = "security.server.crt_pass.len";
  public static final String PASSPHRASE_ENV_KEY =
      "security.server.passphrase_env_var";
  public static final String PASSPHRASE_KEY = "security.server.passphrase";
  public static final String RESOURCES_DIR_KEY = "resources.dir";
  public static final String METADETA_DIR_PATH = "metadata.path";


  public static final String CLIENT_SECURITY_KEY = "client.security";
  public static final String LDAP_USE_SSL_KEY = "authorization.ldap.useSSL";
  public static final String LDAP_PRIMARY_URL_KEY =
      "authorization.ldap.primaryUrl";
  public static final String LDAP_SECONDARY_URL_KEY =
      "authorization.ldap.secondaryUrl";
  public static final String LDAP_BASE_DN_KEY =
      "authorization.ldap.baseDn";
  public static final String LDAP_BIND_ANONYMOUSLY_KEY =
      "authorization.ldap.bindAnonymously";
  public static final String LDAP_MANAGER_DN_KEY =
      "authorization.ldap.managerDn";
  public static final String LDAP_MANAGER_PASSWORD_KEY =
      "authorization.ldap.managerPassword";
  public static final String LDAP_USERNAME_ATTRIBUTE_KEY =
      "authorization.ldap.usernameAttribute";

  public static final String USER_ROLE_NAME_KEY =
      "authorization.userRoleName";
  public static final String ADMIN_ROLE_NAME_KEY =
      "authorization.adminRoleName";

  public static final String PERSISTENCE_IN_MEMORY_KEY =
      "server.persistence.inMemory";
  public static final String SERVER_JDBC_USER_NAME_KEY =
      "server.jdbc.user.name";
  private static final String SERVER_JDBC_USER_NAME_DEFAULT =
      "ambari-server";
  public static final String SERVER_JDBC_USER_PASSWD_KEY =
      "server.jdbc.user.passwd";
  private static final String SERVER_JDBC_USER_PASSWD_DEFAULT =
      "bigdata";

  public static final String OS_VERSION_KEY =
      "server.os_type";

  private static final String SRVR_KSTR_DIR_DEFAULT = ".";
  public static final String SRVR_CRT_NAME_DEFAULT = "ca.crt";
  public static final String SRVR_KEY_NAME_DEFAULT = "ca.key";
  public static final String KSTR_NAME_DEFAULT = "keystore.p12";
  private static final String SRVR_CRT_PASS_FILE_DEFAULT ="pass.txt";
  private static final String SRVR_CRT_PASS_LEN_DEFAULT = "50";
  private static final String PASSPHRASE_ENV_DEFAULT = "AMBARI_PASSPHRASE";
  private static final String RESOURCES_DIR_DEFAULT =
      "/var/share/ambari/resources/";

  private static final String CLIENT_SECURITY_DEFAULT = "local";

  private static final String USER_ROLE_NAME_DEFAULT = "user";
  private static final String ADMIN_ROLE_NAME_DEFAULT = "admin";
  private static final String LDAP_BIND_ANONYMOUSLY_DEFAULT = "true";

  //TODO For embedded server only - should be removed later
  private static final String LDAP_PRIMARY_URL_DEFAULT = "localhost:33389";
  private static final String LDAP_BASE_DN_DEFAULT = "dc=ambari,dc=apache,dc=org";
  private static final String LDAP_USERNAME_ATTRIBUTE_DEFAULT = "uid";

  //TODO for development purposes only, should be changed to 'false'
  private static final String PERSISTENCE_IN_MEMORY_DEFAULT = "true";


  private static final Logger LOG = LoggerFactory.getLogger(
      Configuration.class);

  private Properties properties;


  private Map<String, String> configsMap;


  public Configuration() {
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
    configsMap.put(SRVR_KSTR_DIR_KEY, properties.getProperty(
        SRVR_KSTR_DIR_KEY, SRVR_KSTR_DIR_DEFAULT));
    configsMap.put(SRVR_KSTR_DIR_KEY, properties.getProperty(
        SRVR_KSTR_DIR_KEY, SRVR_KSTR_DIR_DEFAULT));
    configsMap.put(SRVR_CRT_NAME_KEY, properties.getProperty(
        SRVR_CRT_NAME_KEY, SRVR_CRT_NAME_DEFAULT));
    configsMap.put(SRVR_KEY_NAME_KEY, properties.getProperty(
        SRVR_KEY_NAME_KEY, SRVR_KEY_NAME_DEFAULT));
    configsMap.put(KSTR_NAME_KEY, properties.getProperty(
        KSTR_NAME_KEY, KSTR_NAME_DEFAULT));
    configsMap.put(SRVR_CRT_PASS_FILE_KEY, properties.getProperty(
     SRVR_CRT_PASS_FILE_KEY, SRVR_CRT_PASS_FILE_DEFAULT));
    configsMap.put(PASSPHRASE_ENV_KEY, properties.getProperty(
        PASSPHRASE_ENV_KEY, PASSPHRASE_ENV_DEFAULT));
    configsMap.put(PASSPHRASE_KEY, System.getenv(configsMap.get(
        PASSPHRASE_ENV_KEY)));
    configsMap.put(USER_ROLE_NAME_KEY, properties.getProperty(
        USER_ROLE_NAME_KEY, USER_ROLE_NAME_DEFAULT));
    configsMap.put(ADMIN_ROLE_NAME_KEY, properties.getProperty(
        ADMIN_ROLE_NAME_KEY, ADMIN_ROLE_NAME_DEFAULT));
    configsMap.put(RESOURCES_DIR_KEY, properties.getProperty(
        RESOURCES_DIR_KEY, RESOURCES_DIR_DEFAULT));
    configsMap.put(SRVR_CRT_PASS_LEN_KEY, properties.getProperty(
     SRVR_CRT_PASS_LEN_KEY, SRVR_CRT_PASS_LEN_DEFAULT));

    File passFile = new File(configsMap.get(SRVR_KSTR_DIR_KEY) + File.separator
        + configsMap.get(SRVR_CRT_PASS_FILE_KEY));
    String randStr = null;

    if (!passFile.exists()) {
      LOG.info("Generation of file with password");
      try {
        randStr = RandomStringUtils.randomAlphanumeric(Integer
            .parseInt(configsMap.get(SRVR_CRT_PASS_LEN_KEY)));
        FileUtils.writeStringToFile(passFile, randStr);

      } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException(
            "Error reading certificate password from file");
      }
    } else {
        LOG.info("Reading password from existing file");
      try {
        randStr = FileUtils.readFileToString(passFile);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    configsMap.put(SRVR_CRT_PASS_KEY, randStr);
  }


  /**
   * Find, read, and parse the configuration file.
   * @return the properties that were found or empty if no file was found
   */
  private static Properties readConfigFile() {
    Properties properties = new Properties();

    //Get property file stream from classpath
    InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(CONFIG_FILE);

    if (inputStream == null)
      throw new RuntimeException(CONFIG_FILE + " not found in classpath");


    // load the properties
    try {
      properties.load(inputStream);
    } catch (FileNotFoundException fnf) {
      LOG.info("No configuration file " + CONFIG_FILE + " found in classpath.", fnf);
    } catch (IOException ie) {
      throw new IllegalArgumentException("Can't read configuration file " +
       CONFIG_FILE, ie);
    }

    return properties;
  }

  public File getBootStrapDir() {
    String fileName = properties.getProperty(BOOTSTRAP_DIR);
    if (fileName == null) {
        fileName = BOOTSTRAP_DIR_DEFAULT;
    }
    return new File(fileName);
  }

  public String getBootStrapScript() {
    String bootscript = properties.getProperty(BOOTSTRAP_SCRIPT);
    if (bootscript == null) {
      return BOOTSTRAP_SCRIPT_DEFAULT;
    }
    return bootscript;
  }

  public String getBootSetupAgentScript() {
    return properties.getProperty(BOOTSTRAP_SETUP_AGENT_SCRIPT,
        "/usr/lib/python2.6/site-packages/ambari_server/setupAgent.py");
  }

  public String getBootSetupAgentPassword() {
    String pass = configsMap.get(PASSPHRASE_KEY);

    if (null != pass)
      return pass;

    // fallback
    return properties.getProperty(BOOTSTRAP_SETUP_AGENT_PASSWORD, "password");
  }

  /**
   * Get the map with server config parameters.
   * Keys - public constants of this class
   * @return the map with server config parameters
   */
  public Map<String, String> getConfigsMap() {
    return configsMap;
  }

  /**
   * Gets client security type
   * @return appropriate ClientSecurityType
   */
  public ClientSecurityType getClientSecurityType() {
    return ClientSecurityType.fromString(properties.getProperty(CLIENT_SECURITY_KEY));
  }

  public void setClientSecurityType(ClientSecurityType type) {
    properties.setProperty(CLIENT_SECURITY_KEY, type.toString());
  }

  public String getWebAppDir() {
    LOG.info("Web App DIR test " + properties.getProperty(WEBAPP_DIR));
    return properties.getProperty(WEBAPP_DIR, "web");
  }

  /**
   * Gets ambari stack-path
   * @return String
   */
  public String getMetadataPath() {
    return properties.getProperty(METADETA_DIR_PATH);
  }

  /**
   * Check to see if the API should be authenticated or not
   * @return false if not, true if the authentication is enabled.
   */
  public boolean getApiAuthentication() {
    return ("true".equals(properties.getProperty(API_AUTHENTICATE, "false")));
  }

  /**
   * Check to see if the API should be authenticated via ssl or not
   * @return false if not, true if ssl needs to be used.
   */
  public boolean getApiSSLAuthentication() {
    return ("true".equals(properties.getProperty(API_USE_SSL, "false")));
  }


  public PersistenceType getPersistenceType() {
    String value = properties.getProperty(PERSISTENCE_IN_MEMORY_KEY, PERSISTENCE_IN_MEMORY_DEFAULT);
    if ("true".equalsIgnoreCase(value)) {
      return PersistenceType.IN_MEMORY;
    } else {
      return PersistenceType.POSTGRES;
    }
  }

  public String getDatabaseUser() {
    return properties.getProperty(SERVER_JDBC_USER_NAME_KEY, SERVER_JDBC_USER_NAME_DEFAULT);
  }

  public String getDatabasePassword() {
    String filePath = properties.getProperty(SERVER_JDBC_USER_PASSWD_KEY);
    if (filePath == null) {
      LOG.debug("DB password file not specified - using default");
      return SERVER_JDBC_USER_PASSWD_DEFAULT;
    } else {
      LOG.debug("Reading password from file {}", filePath);
      String password;
      try {
        password = FileUtils.readFileToString(new File(filePath));
      } catch (IOException e) {
        throw new RuntimeException("Unable to read database password", e);
      }
      return password;
    }
  }

  /**
   * Gets parameters of LDAP server to connect to
   * @return LdapServerProperties object representing connection parameters
   */
  public LdapServerProperties getLdapServerProperties() {
    LdapServerProperties ldapServerProperties = new LdapServerProperties();

    ldapServerProperties.setPrimaryUrl(properties.getProperty(
        LDAP_PRIMARY_URL_KEY, LDAP_PRIMARY_URL_DEFAULT));
    ldapServerProperties.setSecondaryUrl(properties.getProperty(
        LDAP_SECONDARY_URL_KEY));
    ldapServerProperties.setUseSsl("true".equalsIgnoreCase(properties.
        getProperty(LDAP_USE_SSL_KEY)));
    ldapServerProperties.setAnonymousBind("true".
        equalsIgnoreCase(properties.getProperty(LDAP_BIND_ANONYMOUSLY_KEY,
            LDAP_BIND_ANONYMOUSLY_DEFAULT)));
    ldapServerProperties.setManagerDn(properties.getProperty(
        LDAP_MANAGER_DN_KEY));
    ldapServerProperties.setManagerPassword(properties.getProperty(
        LDAP_MANAGER_PASSWORD_KEY));
    ldapServerProperties.setBaseDN(properties.getProperty
        (LDAP_BASE_DN_KEY, LDAP_BASE_DN_DEFAULT));
    ldapServerProperties.setUsernameAttribute(properties.
        getProperty(LDAP_USERNAME_ATTRIBUTE_KEY, LDAP_USERNAME_ATTRIBUTE_DEFAULT));

    return ldapServerProperties;
  }

  public String getServerOsType() {
    return properties.getProperty(OS_VERSION_KEY, "");
  }

  public String getMasterHostname(String defaultValue) {
    return properties.getProperty(BOOTSTRAP_MASTER_HOSTNAME, defaultValue);
  }

}
