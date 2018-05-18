/*
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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.client.utils.URIBuilder;

public class AmbariConfig {

  private static final String JDK_RESOURCE_LOCATION = "/resources/";

  private final String masterHostname;
  private final Integer masterPort;
  private final String masterProtocol;

  private final String jdkResourceUrl;
  private final String javaHome;
  private final String jdkName;
  private final String jceName;
  private final String ojdbcUrl;
  private final String serverDB;
  private final String mysqljdbcUrl;

  public AmbariConfig(Configuration configs) throws UnknownHostException {
    this(configs, InetAddress.getLocalHost().getCanonicalHostName());
  }

  AmbariConfig(Configuration configs, String masterHostname) {
    this.masterHostname = masterHostname;
    if (configs != null) {
      if (configs.getApiSSLAuthentication()) {
        masterProtocol = "https";
        masterPort = configs.getClientSSLApiPort();
      } else {
        masterProtocol = "http";
        masterPort = configs.getClientApiPort();
      }

      javaHome = configs.getJavaHome();
      jdkName = configs.getJDKName();
      jceName = configs.getJCEName();
      serverDB = configs.getServerDBName();

      jdkResourceUrl = getAmbariServerURI(JDK_RESOURCE_LOCATION);
      ojdbcUrl = getAmbariServerURI(JDK_RESOURCE_LOCATION + configs.getOjdbcJarName());
      mysqljdbcUrl = getAmbariServerURI(JDK_RESOURCE_LOCATION + configs.getMySQLJarName());
    } else {
      masterProtocol = null;
      masterPort = null;

      jdkResourceUrl = null;
      javaHome = null;
      jdkName = null;
      jceName = null;
      ojdbcUrl = null;
      mysqljdbcUrl = null;
      serverDB = null;
    }
  }

  public String getJdkResourceUrl() {
    return jdkResourceUrl;
  }

  public String getJavaHome() {
    return javaHome;
  }

  public String getJDKName() {
    return jdkName;
  }

  public String getJCEName() {
    return jceName;
  }

  public String getServerDB() {
    return serverDB;
  }

  public String getOjdbcUrl() {
    return ojdbcUrl;
  }

  public String getMysqljdbcUrl() {
    return mysqljdbcUrl;
  }

  public String getAmbariServerURI(String path) {
    return (masterProtocol == null || masterHostname == null || masterPort == null)
      ? null
      : getAmbariServerURI(path, masterProtocol, masterHostname, masterPort);
  }

  static String getAmbariServerURI(String path, String masterProtocol, String masterHostname, Integer masterPort) {
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme(masterProtocol);
    uriBuilder.setHost(masterHostname);
    uriBuilder.setPort(masterPort);

    String[] parts = path.split("\\?");

    if (parts.length > 1) {
      uriBuilder.setPath(parts[0]);
      uriBuilder.setQuery(parts[1]);
    } else {
      uriBuilder.setPath(path);
    }

    return uriBuilder.toString();
  }

}
