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
package org.apache.hadoop.yarn.server.applicationhistoryservice.metrics.timeline.query;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.HBaseAdmin;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DefaultPhoenixDataSource implements PhoenixConnectionProvider {

  static final Log LOG = LogFactory.getLog(DefaultPhoenixDataSource.class);
  private static final String ZOOKEEPER_CLIENT_PORT ="hbase.zookeeper.property.clientPort";
  private static final String ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";
  private static final String ZNODE_PARENT = "zookeeper.znode.parent";

  private static final String connectionUrl = "jdbc:phoenix:%s:%s:%s";
  private final String url;

  private Configuration hbaseConf;

  public DefaultPhoenixDataSource(Configuration hbaseConf) {
    this.hbaseConf = hbaseConf;
    String zookeeperClientPort = hbaseConf.getTrimmed(ZOOKEEPER_CLIENT_PORT, "2181");
    String zookeeperQuorum = hbaseConf.getTrimmed(ZOOKEEPER_QUORUM);
    String znodeParent = hbaseConf.getTrimmed(ZNODE_PARENT, "/ams-hbase-unsecure");
    if (zookeeperQuorum == null || zookeeperQuorum.isEmpty()) {
      throw new IllegalStateException("Unable to find Zookeeper quorum to " +
        "access HBase store using Phoenix.");
    }

    url = String.format(connectionUrl,
      zookeeperQuorum,
      zookeeperClientPort,
      znodeParent);
  }

  /**
   * Get HBaseAdmin for table ops.
   * @return @HBaseAdmin
   * @throws IOException
   */
  public HBaseAdmin getHBaseAdmin() throws IOException {
    return (HBaseAdmin) ConnectionFactory.createConnection(hbaseConf).getAdmin();
  }

  /**
   * Get JDBC connection to HBase store. Assumption is that the hbase
   * configuration is present on the classpath and loaded by the caller into
   * the Configuration object.
   * Phoenix already caches the HConnection between the client and HBase
   * cluster.
   *
   * @return @java.sql.Connection
   */
  public Connection getConnection() throws SQLException {

    LOG.debug("Metric store connection url: " + url);
    try {
      return DriverManager.getConnection(url);
    } catch (SQLException e) {
      LOG.warn("Unable to connect to HBase store using Phoenix.", e);

      throw e;
    }
  }

}
