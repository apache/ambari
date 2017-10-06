/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.metrics.adservice.db

import java.sql.{Connection, SQLException}

import org.apache.ambari.metrics.adservice.common.{ADServiceConfiguration, PhoenixQueryConstants}
import org.apache.hadoop.hbase.util.RetryCounterFactory
import org.apache.hadoop.metrics2.sink.timeline.query.{DefaultPhoenixDataSource, PhoenixConnectionProvider}
import java.util.concurrent.TimeUnit.SECONDS

object PhoenixAnomalyStoreAccessor  {

  private var datasource: PhoenixConnectionProvider = _

  def initAnomalyMetricSchema(): Unit = {

    val datasource: PhoenixConnectionProvider = new DefaultPhoenixDataSource(ADServiceConfiguration.getHBaseConf)
    val retryCounterFactory = new RetryCounterFactory(10, SECONDS.toMillis(3).toInt)

    val ttl = ADServiceConfiguration.getAnomalyDataTtl
    try {
      var conn = datasource.getConnectionRetryingOnException(retryCounterFactory)
      var stmt = conn.createStatement

      val methodParametersSql = String.format(PhoenixQueryConstants.CREATE_METHOD_PARAMETERS_TABLE,
        PhoenixQueryConstants.METHOD_PARAMETERS_TABLE_NAME)
      stmt.executeUpdate(methodParametersSql)

      val pointInTimeAnomalySql = String.format(PhoenixQueryConstants.CREATE_PIT_ANOMALY_METRICS_TABLE_SQL,
        PhoenixQueryConstants.PIT_ANOMALY_METRICS_TABLE_NAME,
        ttl.asInstanceOf[Object])
      stmt.executeUpdate(pointInTimeAnomalySql)

      val trendAnomalySql = String.format(PhoenixQueryConstants.CREATE_TREND_ANOMALY_METRICS_TABLE_SQL,
        PhoenixQueryConstants.TREND_ANOMALY_METRICS_TABLE_NAME,
        ttl.asInstanceOf[Object])
      stmt.executeUpdate(trendAnomalySql)

      val snapshotSql = String.format(PhoenixQueryConstants.CREATE_MODEL_SNAPSHOT_TABLE,
        PhoenixQueryConstants.MODEL_SNAPSHOT)
      stmt.executeUpdate(snapshotSql)

      conn.commit()
    } catch {
      case e: SQLException => throw e
    }
  }

  @throws[SQLException]
  def getConnection: Connection = datasource.getConnection
}
