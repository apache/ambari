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

/**
 * Configuration for SSL on components (Ganglia & Nagios).
 */
public class ComponentSSLConfiguration {

  /**
   * Configuration
   */
  private String truststorePath;
  private String truststorePassword;
  private String truststoreType;
  private boolean gangliaSSL;
  private boolean nagiosSSL;

  /**
   * The singleton.
   */
  private static ComponentSSLConfiguration singleton = new ComponentSSLConfiguration();


  // ----- Constructors ------------------------------------------------------

  /**
   * Singleton constructor.
   */
  protected ComponentSSLConfiguration() {
  }


  // ----- ComponentSSLConfiguration -----------------------------------------

  /**
   * Initialize with the given configuration.
   *
   * @param configuration  the configuration
   */
  public void init(Configuration configuration) {
    truststorePath     = configuration.getProperty(Configuration.SSL_TRUSTSTORE_PATH_KEY);
    truststorePassword = getPassword(configuration);
    truststoreType     = configuration.getProperty(Configuration.SSL_TRUSTSTORE_TYPE_KEY);
    gangliaSSL         = Boolean.parseBoolean(configuration.getProperty(Configuration.GANGLIA_HTTPS_KEY));
    nagiosSSL          = Boolean.parseBoolean(configuration.getProperty(Configuration.NAGIOS_HTTPS_KEY));
  }


  // ----- accessors ---------------------------------------------------------

  /**
   * Get the truststore path.
   *
   * @return the truststore path
   */
  public String getTruststorePath() {
    return truststorePath;
  }

  /**
   * Get the truststore password.
   *
   * @return the truststore password
   */
  public String getTruststorePassword() {
    return truststorePassword;
  }

  /**
   * Get the truststore type.
   *
   * @return the truststore type; may be null
   */
  public String getTruststoreType() {
    return truststoreType;
  }

  /**
   * Indicates whether or not Ganglia is setup for SSL.
   *
   * @return true if Ganglia is setup for SSL
   */
  public boolean isGangliaSSL() {
    return gangliaSSL;
  }

  /**
   * Indicates whether or not Nagios is setup for SSL.
   *
   * @return true if Nagios is setup for SSL
   */
  public boolean isNagiosSSL() {
    return nagiosSSL;
  }

  /**
   * Get the singleton instance.
   *
   * @return the singleton instance
   */
  public static ComponentSSLConfiguration instance() {
    return singleton;
  }


  // -----helper methods -----------------------------------------------------

  private String getPassword(Configuration configuration) {
    String rawPassword = configuration.getProperty(Configuration.SSL_TRUSTSTORE_PASSWORD_KEY);
    String password    = configuration.readPasswordFromStore(rawPassword);

    return password == null ? rawPassword : password;
  }
}
