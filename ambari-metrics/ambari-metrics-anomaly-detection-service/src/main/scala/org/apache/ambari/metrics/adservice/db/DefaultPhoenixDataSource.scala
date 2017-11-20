/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package org.apache.ambari.metrics.adservice.db

import org.apache.commons.logging.LogFactory
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.client.ConnectionFactory
import org.apache.hadoop.hbase.client.HBaseAdmin
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

object DefaultPhoenixDataSource {
  private[db] val LOG = LogFactory.getLog(classOf[DefaultPhoenixDataSource])
  private val ZOOKEEPER_CLIENT_PORT = "hbase.zookeeper.property.clientPort"
  private val ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum"
  private val ZNODE_PARENT = "zookeeper.znode.parent"
  private val connectionUrl = "jdbc:phoenix:%s:%s:%s"
}

class DefaultPhoenixDataSource(var hbaseConf: Configuration) extends PhoenixConnectionProvider {

  val zookeeperClientPort: String = hbaseConf.getTrimmed(DefaultPhoenixDataSource.ZOOKEEPER_CLIENT_PORT, "2181")
  val zookeeperQuorum: String = hbaseConf.getTrimmed(DefaultPhoenixDataSource.ZOOKEEPER_QUORUM)
  val znodeParent: String = hbaseConf.getTrimmed(DefaultPhoenixDataSource.ZNODE_PARENT, "/ams-hbase-unsecure")
  final private var url : String = _

  if (zookeeperQuorum == null || zookeeperQuorum.isEmpty) {
    throw new IllegalStateException("Unable to find Zookeeper quorum to access HBase store using Phoenix.")
  }
  url = String.format(DefaultPhoenixDataSource.connectionUrl, zookeeperQuorum, zookeeperClientPort, znodeParent)


  /**
    * Get HBaseAdmin for table ops.
    *
    * @return @HBaseAdmin
    * @throws IOException
    */
  @throws[IOException]
  override def getHBaseAdmin: HBaseAdmin = ConnectionFactory.createConnection(hbaseConf).getAdmin.asInstanceOf[HBaseAdmin]

  /**
    * Get JDBC connection to HBase store. Assumption is that the hbase
    * configuration is present on the classpath and loaded by the caller into
    * the Configuration object.
    * Phoenix already caches the HConnection between the client and HBase
    * cluster.
    *
    * @return @java.sql.Connection
    */
  @throws[SQLException]
  override def getConnection: Connection = {
    DefaultPhoenixDataSource.LOG.debug("Metric store connection url: " + url)
    try DriverManager.getConnection(url)
    catch {
      case e: SQLException =>
        DefaultPhoenixDataSource.LOG.warn("Unable to connect to HBase store using Phoenix.", e)
        throw e
    }
  }

}