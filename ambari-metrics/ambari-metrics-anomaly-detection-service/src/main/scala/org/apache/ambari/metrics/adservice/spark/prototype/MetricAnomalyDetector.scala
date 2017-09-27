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

import java.io.{FileInputStream, IOException, InputStream}
import java.util
import java.util.Properties
import java.util.logging.LogManager

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.ambari.metrics.adservice.prototype.core.MetricsCollectorInterface
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._
import org.apache.ambari.metrics.adservice.prototype.methods.{AnomalyDetectionTechnique, MetricAnomaly}
import org.apache.ambari.metrics.adservice.prototype.methods.ema.{EmaModelLoader, EmaTechnique}
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics
import org.apache.log4j.Logger
import org.apache.spark.storage.StorageLevel

object MetricAnomalyDetector {

  /*
    Load current EMA model
    Filter step - Check if anomaly
    Collect / Write to AMS / Print.
   */

//  var brokers = "avijayan-ams-1.openstacklocal:2181,avijayan-ams-2.openstacklocal:2181,avijayan-ams-3.openstacklocal:2181"
//  var groupId = "ambari-metrics-group"
//  var topicName = "ambari-metrics-topic"
//  var numThreads = 1
//  val anomalyDetectionModels: Array[AnomalyDetectionTechnique] = Array[AnomalyDetectionTechnique]()
//
//  def readProperties(propertiesFile: String): Properties = try {
//    val properties = new Properties
//    var inputStream = ClassLoader.getSystemResourceAsStream(propertiesFile)
//    if (inputStream == null) inputStream = new FileInputStream(propertiesFile)
//    properties.load(inputStream)
//    properties
//  } catch {
//    case ioEx: IOException =>
//      null
//  }
//
//  def main(args: Array[String]): Unit = {
//
//    @transient
//    lazy val log = org.apache.log4j.LogManager.getLogger("MetricAnomalyDetectorLogger")
//
//    if (args.length < 1) {
//      System.err.println("Usage: MetricSparkConsumer <input-config-file>")
//      System.exit(1)
//    }
//
//    //Read properties
//    val properties = readProperties(propertiesFile = args(0))
//
//    //Load EMA parameters - w, n
//    val emaW = properties.getProperty("emaW").toDouble
//    val emaN = properties.getProperty("emaN").toDouble
//
//    //collector info
//    val collectorHost: String = properties.getProperty("collectorHost")
//    val collectorPort: String = properties.getProperty("collectorPort")
//    val collectorProtocol: String = properties.getProperty("collectorProtocol")
//    val anomalyMetricPublisher = new MetricsCollectorInterface(collectorHost, collectorProtocol, collectorPort)
//
//    //Instantiate Kafka stream reader
//    val sparkConf = new SparkConf().setAppName("AmbariMetricsAnomalyDetector")
//    val streamingContext = new StreamingContext(sparkConf, Duration(10000))
//
//    val topicsSet = topicName.toSet
//    val kafkaParams = Map[String, String]("metadata.broker.list" -> brokers)
////    val stream = KafkaUtils.createDirectStream()
//
//    val kafkaStream = KafkaUtils.createStream(streamingContext, zkQuorum, groupId, Map(topicName -> numThreads), StorageLevel.MEMORY_AND_DISK_SER_2)
//    kafkaStream.print()
//
//    var timelineMetricsStream = kafkaStream.map( message => {
//      val mapper = new ObjectMapper
//      val metrics = mapper.readValue(message._2, classOf[TimelineMetrics])
//      metrics
//    })
//    timelineMetricsStream.print()
//
//    var appMetricStream = timelineMetricsStream.map( timelineMetrics => {
//      (timelineMetrics.getMetrics.get(0).getAppId, timelineMetrics)
//    })
//    appMetricStream.print()
//
//    var filteredAppMetricStream = appMetricStream.filter( appMetricTuple => {
//      appIds.contains(appMetricTuple._1)
//    } )
//    filteredAppMetricStream.print()
//
//    filteredAppMetricStream.foreachRDD( rdd => {
//      rdd.foreach( appMetricTuple => {
//        val timelineMetrics = appMetricTuple._2
//        logger.info("Received Metric (1): " + timelineMetrics.getMetrics.get(0).getMetricName)
//        log.info("Received Metric (2): " + timelineMetrics.getMetrics.get(0).getMetricName)
//        for (timelineMetric <- timelineMetrics.getMetrics) {
//          var anomalies = emaModel.test(timelineMetric)
//          anomalyMetricPublisher.publish(anomalies)
//        }
//      })
//    })
//
//    streamingContext.start()
//    streamingContext.awaitTermination()
//  }
  }
