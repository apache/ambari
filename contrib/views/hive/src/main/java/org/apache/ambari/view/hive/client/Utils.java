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

package org.apache.ambari.view.hive.client;

import org.apache.hive.service.cli.thrift.TStatus;
import org.apache.hive.service.cli.thrift.TStatusCode;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import java.util.ArrayList;
import java.util.List;

public class Utils {
  // This value is set to true by the setServiceUnavailableRetryStrategy() when the server returns 401
  static final String HIVE_SERVER2_RETRY_KEY = "hive.server2.retryserver";
  static final String HIVE_SERVER2_RETRY_TRUE = "true";
  static final String HIVE_SERVER2_RETRY_FALSE = "false";

  static final String HIVE_COMPILE_ERROR_MSG = "Error while compiling statement:";

  static void verifySuccess(TStatus status, String comment) throws HiveClientException {
    if (status.getStatusCode() != TStatusCode.SUCCESS_STATUS &&
        status.getStatusCode() != TStatusCode.SUCCESS_WITH_INFO_STATUS) {
      String message = (status.getErrorMessage() != null) ? status.getErrorMessage() : "";

      // For schemantic exception Error code is between 10000-19999
      // https://issues.apache.org/jira/browse/HIVE-3001
      // https://issues.apache.org/jira/browse/HIVE-12867
      if((status.getErrorCode() >= 10000 && status.getErrorCode() <= 19999)|| message.contains(HIVE_COMPILE_ERROR_MSG)){
        throw new HiveInvalidQueryException(status.getStatusCode(),message);
      }
      throw new HiveErrorStatusException(status.getStatusCode(), comment + ". " + message);
    }
  }

  static boolean needToSendCredentials(CookieStore cookieStore, String cookieName, boolean isSSL) {
    if (cookieName == null || cookieStore == null) {
      return true;
    }

    List<Cookie> cookies = cookieStore.getCookies();

    for (Cookie c : cookies) {
      // If this is a secured cookie and the current connection is non-secured,
      // then, skip this cookie. We need to skip this cookie because, the cookie
      // replay will not be transmitted to the server.
      if (c.isSecure() && !isSSL) {
        continue;
      }
      if (c.getName().equals(cookieName)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Removes the empty strings and returns back only the strings with content
   */
  static String[] removeEmptyStrings(String[] strs) {
    List<String> nonEmptyStrings = new ArrayList<>();
    for(String str : strs) {
      if (!(str == null || str.trim().isEmpty())) {
        nonEmptyStrings.add(str.trim());
      }
    }
    return nonEmptyStrings.toArray(new String[] {});
  }

  public static class HiveAuthenticationParams {
    public static final String AUTH_TYPE = "auth";
    // We're deprecating this variable's name.
    public static final String AUTH_QOP_DEPRECATED = "sasl.qop";
    public static final String AUTH_QOP = "saslQop";
    public static final String AUTH_SIMPLE = "noSasl";
    public static final String AUTH_TOKEN = "delegationToken";
    public static final String AUTH_USER = "user";
    public static final String HS2_PROXY_USER = "hive.server2.proxy.user";
    public static final String AUTH_PRINCIPAL = "principal";
    public static final String AUTH_PASSWD = "password";
    public static final String AUTH_KERBEROS_AUTH_TYPE = "kerberosAuthType";
    public static final String AUTH_KERBEROS_AUTH_TYPE_FROM_SUBJECT = "fromSubject";
    public static final String ANONYMOUS_USER = "anonymous";
    public static final String ANONYMOUS_PASSWD = "anonymous";
    public static final String USE_SSL = "ssl";
    public static final String SSL_TRUST_STORE = "sslTrustStore";
    public static final String SSL_TRUST_STORE_PASSWORD = "trustStorePassword";
    // We're deprecating the name and placement of this in the parsed map (from hive conf vars to
    // hive session vars).
    public static final String TRANSPORT_MODE_DEPRECATED = "hive.server2.transport.mode";
    public static final String TRANSPORT_MODE = "transportMode";
    // We're deprecating the name and placement of this in the parsed map (from hive conf vars to
    // hive session vars).
    public static final String HTTP_PATH_DEPRECATED = "hive.server2.thrift.http.path";
    public static final String HTTP_PATH = "httpPath";
    public static final String SERVICE_DISCOVERY_MODE = "serviceDiscoveryMode";
    // Don't use dynamic service discovery
    public static final String SERVICE_DISCOVERY_MODE_NONE = "none";
    // Use ZooKeeper for indirection while using dynamic service discovery
    public static final String SERVICE_DISCOVERY_MODE_ZOOKEEPER = "zooKeeper";
    public static final String ZOOKEEPER_NAMESPACE = "zooKeeperNamespace";
    // Default namespace value on ZooKeeper.
    // This value is used if the param "zooKeeperNamespace" is not specified in the JDBC Uri.
    public static final String ZOOKEEPER_DEFAULT_NAMESPACE = "hiveserver2";
    // Non-configurable params:
    // Currently supports JKS keystore format
    public static final String SSL_TRUST_STORE_TYPE = "JKS";
    static final String COOKIE_AUTH = "cookieAuth";
    static final String COOKIE_AUTH_FALSE = "false";
    static final String COOKIE_NAME = "cookieName";
    // The default value of the cookie name when CookieAuth=true
    static final String DEFAULT_COOKIE_NAMES_HS2 = "hive.server2.auth";
    static final String HTTP_HEADER_PREFIX = "http.header.";
    // --------------- Begin 2 way ssl options -------------------------
    // Use two way ssl. This param will take effect only when ssl=true
    static final String USE_TWO_WAY_SSL = "twoWay";
    static final String TRUE = "true";
    static final String SSL_KEY_STORE = "sslKeyStore";
    static final String SSL_KEY_STORE_PASSWORD = "keyStorePassword";
    static final String SSL_KEY_STORE_TYPE = "JKS";
    static final String SUNX509_ALGORITHM_STRING = "SunX509";
    // --------------- End 2 way ssl options ----------------------------
    static final String SUNJSSE_ALGORITHM_STRING = "SunJSSE";
  }
}
