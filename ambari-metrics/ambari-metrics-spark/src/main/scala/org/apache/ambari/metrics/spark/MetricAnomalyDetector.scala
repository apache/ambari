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
package org.apache.ambari.metrics.spark


import java.util
import java.util.logging.LogManager

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.ambari.metrics.alertservice.common.{MetricAnomaly, TimelineMetrics}
import org.apache.ambari.metrics.alertservice.methods.MetricAnomalyModel
import org.apache.ambari.metrics.alertservice.methods.ema.{EmaModel, EmaModelLoader}
import org.apache.ambari.metrics.alertservice.spark.AnomalyMetricPublisher
import org.apache.log4j.Logger
import org.apache.spark.storage.StorageLevel

import scala.collection.JavaConversions._
import org.apache.logging.log4j.scala.Logging

object MetricAnomalyDetector extends Logging {


  var zkQuorum = "avijayan-ams-1.openstacklocal:2181,avijayan-ams-2.openstacklocal:2181,avijayan-ams-3.openstacklocal:2181"
  var groupId = "ambari-metrics-group"
  var topicName = "ambari-metrics-topic"
  var numThreads = 1
  val anomalyDetectionModels: Array[MetricAnomalyModel] = Array[MetricAnomalyModel]()

  def main(args: Array[String]): Unit = {

    @transient
    lazy val log: Logger = org.apache.log4j.LogManager.getLogger("MetricAnomalyDetectorLogger")

    if (args.length < 5) {
      System.err.println("Usage: MetricAnomalyDetector <method1,method2> <appid1,appid2> <collector_host> <port> <protocol>")
      System.exit(1)
    }

    for (method <- args(0).split(",")) {
      if (method == "ema") anomalyDetectionModels :+ new EmaModel()
    }

    val appIds = util.Arrays.asList(args(1).split(","))

    val collectorHost = args(2)
    val collectorPort = args(3)
    val collectorProtocol = args(4)

    val anomalyMetricPublisher: AnomalyMetricPublisher = new AnomalyMetricPublisher(collectorHost, collectorProtocol, collectorPort)

    val sparkConf = new SparkConf().setAppName("AmbariMetricsAnomalyDetector")

    val streamingContext = new StreamingContext(sparkConf, Duration(10000))

    val emaModel = new EmaModelLoader().load(streamingContext.sparkContext, "/tmp/model/ema")

    val kafkaStream = KafkaUtils.createStream(streamingContext, zkQuorum, groupId, Map(topicName -> numThreads), StorageLevel.MEMORY_AND_DISK_SER_2)
    kafkaStream.print()

    var timelineMetricsStream = kafkaStream.map( message => {
      val mapper = new ObjectMapper
      val metrics = mapper.readValue(message._2, classOf[TimelineMetrics])
      metrics
    })
    timelineMetricsStream.print()

    var appMetricStream = timelineMetricsStream.map( timelineMetrics => {
      (timelineMetrics.getMetrics.get(0).getAppId, timelineMetrics)
    })
    appMetricStream.print()

    var filteredAppMetricStream = appMetricStream.filter( appMetricTuple => {
      appIds.contains(appMetricTuple._1)
    } )
    filteredAppMetricStream.print()

    filteredAppMetricStream.foreachRDD( rdd => {
      rdd.foreach( appMetricTuple => {
        val timelineMetrics = appMetricTuple._2
        logger.info("Received Metric (1): " + timelineMetrics.getMetrics.get(0).getMetricName)
        log.info("Received Metric (2): " + timelineMetrics.getMetrics.get(0).getMetricName)
        for (timelineMetric <- timelineMetrics.getMetrics) {
          var anomalies = emaModel.test(timelineMetric)
          anomalyMetricPublisher.publish(anomalies)
          for (anomaly <- anomalies) {
            var an = anomaly : MetricAnomaly
            logger.info(an.getAnomalyAsString)
          }
        }
      })
    })

    streamingContext.start()
    streamingContext.awaitTermination()
  }
  }
