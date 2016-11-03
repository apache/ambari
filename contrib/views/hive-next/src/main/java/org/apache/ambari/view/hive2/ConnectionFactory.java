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

package org.apache.ambari.view.hive2;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.client.ConnectionConfig;

import java.util.List;

public class ConnectionFactory {

  private static final String ZK_HIVE_DYN_SERVICE_DISCOVERY_KEY = "hive.server2.support.dynamic.service.discovery";
  private static final String ZK_HIVE_NAMESPACE_KEY = "hive.server2.zookeeper.namespace";
  private static final String ZK_HIVE_QUORUM = "hive.zookeeper.quorum";

  private static final String AMBARI_HIVE_SERVICE_NAME = "HIVE";
  private static final String AMBARI_HIVESERVER_COMPONENT_NAME = "HIVE_SERVER";

  private static final String HIVE_SITE = "hive-site";
  private static final String HIVE_INTERACTIVE_SITE = "hive-interactive-site";

  private static final String HIVE_JDBC_URL_KEY = "hive.jdbc.url";
  private static final String HIVE_SESSION_PARAMS = "hive.session.params";
  private static final String HIVE_LDAP_CONFIG = "hive.ldap.configured";

  private static final String BINARY_PORT_KEY = "hive.server2.thrift.port";
  private static final String HIVE_AUTH_MODE = "hive.server2.authentication";
  private static final String HTTP_PORT_KEY = "hive.server2.thrift.http.port";
  private static final String HIVE_TRANSPORT_MODE_KEY = "hive.server2.transport.mode";
  private static final String HTTP_PATH_KEY = "hive.server2.thrift.http.path";
  private static final String HS2_PROXY_USER = "hive.server2.proxy.user";
  private static final String USE_HIVE_INTERACTIVE_MODE = "use.hive.interactive.mode";

  public static boolean isLdapEnabled(ViewContext context){
    if (context.getCluster() == null) {
      return context.getProperties().get(HIVE_LDAP_CONFIG).equalsIgnoreCase("true");
    }
    return context.getCluster().getConfigurationValue(HIVE_SITE,HIVE_AUTH_MODE).equalsIgnoreCase("ldap");
  }

  public static ConnectionConfig create(ViewContext context)  {

    String jdbcUrl;
    if (context.getCluster() == null) {
      jdbcUrl = getConnectFromCustom(context);
    } else {
      if (zookeeperConfigured(context)) {
        jdbcUrl = getFromClusterZookeeperConfig(context);
      } else {
        jdbcUrl = getFromHiveConfiguration(context);
      }
    }

    String userName = context.getUsername();
    if(isLdapEnabled(context)){
      Optional<String> opPassword = ConnectionSystem.getInstance().getPassword(context);
      if(opPassword.isPresent()){
        return new ConnectionConfig(userName, opPassword.get(), jdbcUrl);
      }
    }
    return new ConnectionConfig(userName, "", jdbcUrl);
  }


  private static String getFromHiveConfiguration(ViewContext context) {
    boolean useLLAP = Boolean.valueOf(context.getProperties().get(USE_HIVE_INTERACTIVE_MODE));
    String transportMode = context.getCluster().getConfigurationValue(HIVE_SITE, HIVE_TRANSPORT_MODE_KEY);
    String binaryPort = context.getCluster().getConfigurationValue(HIVE_SITE, BINARY_PORT_KEY);
    String httpPort = context.getCluster().getConfigurationValue(HIVE_SITE, HTTP_PORT_KEY);
    if (useLLAP) {
      binaryPort = context.getCluster().getConfigurationValue(HIVE_INTERACTIVE_SITE, BINARY_PORT_KEY);
      httpPort = context.getCluster().getConfigurationValue(HIVE_INTERACTIVE_SITE, HTTP_PORT_KEY);
    }


    String pathKey = context.getCluster().getConfigurationValue(HIVE_SITE, HTTP_PATH_KEY);
    List<String> hiveHosts = context.getCluster().getHostsForServiceComponent(AMBARI_HIVE_SERVICE_NAME, AMBARI_HIVESERVER_COMPONENT_NAME);
    String sessionParams = context.getProperties().get(HIVE_SESSION_PARAMS);

    boolean isBinary = transportMode.equalsIgnoreCase("binary");
    final String port = isBinary ? binaryPort : httpPort;

    List<String> hostPorts = FluentIterable.from(hiveHosts).transform(new Function<String, String>() {
      @Override
      public String apply(String input) {
        return input + ":" + port;
      }
    }).toList();

    String concatHostPorts = Joiner.on(",").join(hostPorts);

    StringBuilder builder = new StringBuilder();
    builder.append("jdbc:hive2://")
        .append(concatHostPorts);
    if(!Strings.isNullOrEmpty(sessionParams)) {
      builder.append(";").append(sessionParams);
    }

    if (!isBinary) {
      builder.append(";").append("transportMode=http;httpPath=").append(pathKey);
    }

    return builder.toString();
  }

  private static String getFromClusterZookeeperConfig(ViewContext context) {
    boolean useLLAP = Boolean.valueOf(context.getProperties().get(USE_HIVE_INTERACTIVE_MODE));
    String quorum = context.getCluster().getConfigurationValue(HIVE_SITE, ZK_HIVE_QUORUM);

    String namespace = context.getCluster().getConfigurationValue(HIVE_SITE, ZK_HIVE_NAMESPACE_KEY);
    if (useLLAP) {
      namespace = context.getCluster().getConfigurationValue(HIVE_INTERACTIVE_SITE, ZK_HIVE_NAMESPACE_KEY);
    }

    String sessionParams = context.getProperties().get(HIVE_SESSION_PARAMS);

    String formatted = String.format("jdbc:hive2://%s/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=%s", quorum, namespace);
    if (Strings.isNullOrEmpty(sessionParams)) {
      sessionParams = "";
    }

    if (!sessionParams.contains(HS2_PROXY_USER)) {
      if (!sessionParams.isEmpty()) {
        sessionParams += ";";
      }
      sessionParams = sessionParams + HS2_PROXY_USER + "=" + context.getUsername();
    }

    if (sessionParams.isEmpty()) {
      return formatted;
    }
    return formatted + ";" + sessionParams;
  }

  private static boolean zookeeperConfigured(ViewContext context) {
    boolean fromHiveSite = Boolean.valueOf(context.getCluster().getConfigurationValue(HIVE_SITE, ZK_HIVE_DYN_SERVICE_DISCOVERY_KEY));
    boolean fromHiveInteractiveSite = Boolean.valueOf(context.getCluster().getConfigurationValue(HIVE_INTERACTIVE_SITE, ZK_HIVE_DYN_SERVICE_DISCOVERY_KEY));
    return fromHiveInteractiveSite || fromHiveSite;
  }

  private static String getConnectFromCustom(ViewContext context) {
    String jdbcUrl = context.getProperties().get(HIVE_JDBC_URL_KEY);
    String hiveSessionParams = context.getProperties().get(HIVE_SESSION_PARAMS);
    return jdbcUrl + ";" + hiveSessionParams;
  }
}
