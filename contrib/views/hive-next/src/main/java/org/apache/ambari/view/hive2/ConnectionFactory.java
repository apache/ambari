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
import com.google.common.collect.FluentIterable;
import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.hive2.client.ConnectionConfig;

import java.util.List;

public class ConnectionFactory {

  private static String ZK_HIVE_DYN_SERVICE_DISCOVERY_KEY = "hive.server2.support.dynamic.service.discovery";
  private static String ZK_HIVE_NAMESPACE_KEY = "hive.server2.zookeeper.namespace";
  private static String ZK_HIVE_QUORUM = "hive.zookeeper.quorum";

  private static String AMBARI_HIVE_SERVICE_NAME = "HIVE";
  private static String AMBARI_HIVESERVER_COMPONENT_NAME = "HIVE_SERVER";

  private static String HIVE_SITE = "hive-site";
  private static String HIVE_INTERACTIVE_SITE = "hive-interactive-site";

  private static String HIVE_JDBC_URL_KEY = "hive.jdbc.url";
  private static final String HIVE_SESSION_PARAMS = "hive.session.params";

  private static String BINARY_PORT_KEY = "hive.server2.thrift.port";
  private static String HTTP_PORT_KEY = "hive.server2.thrift.http.port";
  private static String HIVE_TRANSPORT_MODE_KEY = "hive.server2.transport.mode";
  private static String HTTP_PATH_KEY = "hive.server2.thrift.http.path";


  public static ConnectionConfig create(ViewContext context) {
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
    return new ConnectionConfig(userName, "", jdbcUrl);
  }

  private static String getFromHiveConfiguration(ViewContext context) {
    String transportMode = context.getCluster().getConfigurationValue(HIVE_SITE, HIVE_TRANSPORT_MODE_KEY);
    String binaryPort = context.getCluster().getConfigurationValue(HIVE_SITE, BINARY_PORT_KEY);
    String httpPort = context.getCluster().getConfigurationValue(HIVE_SITE, HTTP_PORT_KEY);
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
      .append(concatHostPorts)
      .append(";")
      .append(sessionParams);

    if (!isBinary) {
      builder.append(";").append("transportMode=http;httpPath=").append(pathKey);
    }

    return builder.toString();
  }

  private static String getFromClusterZookeeperConfig(ViewContext context) {
    String quorum = context.getCluster().getConfigurationValue(HIVE_SITE, ZK_HIVE_QUORUM);
    if (quorum == null) {
      quorum = context.getCluster().getConfigurationValue(HIVE_INTERACTIVE_SITE, ZK_HIVE_QUORUM);
    }

    String namespace = context.getCluster().getConfigurationValue(HIVE_SITE, ZK_HIVE_NAMESPACE_KEY);
    if (namespace == null) {
      namespace = context.getCluster().getConfigurationValue(HIVE_INTERACTIVE_SITE, ZK_HIVE_NAMESPACE_KEY);
    }
    return String.format("jdbc:hive2://%s/;serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=%s", quorum, namespace);
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
