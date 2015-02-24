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

public class Utils {
  static void verifySuccess(TStatus status, String comment) throws HiveClientException {
    if (status.getStatusCode() != TStatusCode.SUCCESS_STATUS &&
        status.getStatusCode() != TStatusCode.SUCCESS_WITH_INFO_STATUS) {
      String message = (status.getErrorMessage() != null) ? status.getErrorMessage() : "";
      throw new HiveErrorStatusException(status.getStatusCode(), message + ": " + comment);
    }
  }

  public static class HiveAuthenticationParams {
    public static final String AUTH_TYPE = "auth";
    // We're deprecating this variable's name.
    public static final String AUTH_QOP_DEPRECATED = "sasl.qop";
    public static final String AUTH_QOP = "saslQop";
    public static final String AUTH_SIMPLE = "noSasl";
    public static final String AUTH_TOKEN = "delegationToken";
    public static final String AUTH_USER = "user";
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
  }
}
