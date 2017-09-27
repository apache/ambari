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

package org.apache.ambari.metrics.adservice.spark.prototype

import org.apache.ambari.metrics.adservice.prototype.methods.ema.EmaTechnique
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

object SparkPhoenixReader {

  def main(args: Array[String]) {

    if (args.length < 6) {
      System.err.println("Usage: SparkPhoenixReader <metric_name> <appId> <hostname> <weight> <timessdev> <phoenixConnectionString> <model_dir>")
      System.exit(1)
    }

    var metricName = args(0)
    var appId = args(1)
    var hostname = args(2)
    var weight = args(3).toDouble
    var timessdev = args(4).toInt
    var phoenixConnectionString = args(5) //avijayan-ams-3.openstacklocal:61181:/ams-hbase-unsecure
    var modelDir = args(6)

    val conf = new SparkConf()
    conf.set("spark.app.name", "AMSAnomalyModelBuilder")
    //conf.set("spark.master", "spark://avijayan-ams-2.openstacklocal:7077")

    var sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    val currentTime = System.currentTimeMillis()
    val oneDayBack = currentTime - 24*60*60*1000

    val df = sqlContext.load("org.apache.phoenix.spark", Map("table" -> "METRIC_RECORD", "zkUrl" -> phoenixConnectionString))
    df.registerTempTable("METRIC_RECORD")
    val result = sqlContext.sql("SELECT METRIC_NAME, HOSTNAME, APP_ID, SERVER_TIME, METRIC_SUM, METRIC_COUNT FROM METRIC_RECORD " +
      "WHERE METRIC_NAME = '" + metricName + "' AND HOSTNAME = '" + hostname + "' AND APP_ID = '" + appId + "' AND SERVER_TIME < " + currentTime + " AND SERVER_TIME > " + oneDayBack)

    var metricValues = new java.util.TreeMap[java.lang.Long, java.lang.Double]
    result.collect().foreach(
      t => metricValues.put(t.getLong(3), t.getDouble(4) / t.getInt(5))
    )

    //val seriesName = result.head().getString(0)
    //val hostname = result.head().getString(1)
    //val appId = result.head().getString(2)

    val timelineMetric = new TimelineMetric()
    timelineMetric.setMetricName(metricName)
    timelineMetric.setAppId(appId)
    timelineMetric.setHostName(hostname)
    timelineMetric.setMetricValues(metricValues)

    var emaModel = new EmaTechnique(weight, timessdev)
    emaModel.test(timelineMetric)
    emaModel.save(sc, modelDir)

  }

}
